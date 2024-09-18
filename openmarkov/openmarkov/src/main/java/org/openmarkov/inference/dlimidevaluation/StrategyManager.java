/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.inference.dlimidevaluation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openmarkov.core.exception.NodeNotFoundException;
import org.openmarkov.core.exception.NonProjectablePotentialException;
import org.openmarkov.core.exception.WrongCriterionException;
import org.openmarkov.core.model.network.Node;
import org.openmarkov.core.model.network.NodeType;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.ProbNetOperations;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.potential.Potential;
import org.openmarkov.core.model.network.potential.PotentialRole;
import org.openmarkov.core.model.network.potential.TablePotential;
import org.openmarkov.core.model.network.potential.operation.AuxiliaryOperations;
import org.openmarkov.core.model.network.potential.operation.DiscretePotentialOperations;

/**
 * This class acts as a manager to generate the possible startegies given a net and a horizon
 * @author IagoParis - 28/11/2018
 **/
public class StrategyManager {

    private List<List<Node>> orderedNodesBySlice;

    // The next three lists has the same length.
    private List<Node> decisionNodes; // This holds the node
    private int[] nStates;            // This its number of states
    private int[] nParentConfigs;    // This its number of parent configurations

    private int horizon; // The further slice index

    /**
     * This variable holds the current strategy of the strategy manager.
     *
     * The strategy is coded as this integer vector of options chosen where each element refers to a parent
     * configuration of a decision of a slice. The decisions are concatenated so after every parent configuration
     * of the first decision comes the parent configurations of the second one, and so on.
     **/
    private int[] options;
    private int[] maximumOptions;     // Maximum option for each element

    private Logger logger;


    /** Constructs the startegy manager and initializes a random strategy **/
    public StrategyManager(ProbNet probNet, int horizon) {

        this.logger = LogManager.getLogger(StrategyManager.class.getName());


        this.horizon = horizon;
        orderedNodesBySlice = orderNodesBySlice(probNet);

        // Separate the decision nodes
        decisionNodes = new ArrayList<>();
        for (List<Node> slice : orderedNodesBySlice) {
            for (Node node : slice) {
                if (node.getNodeType() == NodeType.DECISION) {
                    decisionNodes.add(node);
                }
            }
        }

        // As parallel arrays, get the number of parent configs and number of states for each decision node
        nStates = new int[decisionNodes.size()];
        nParentConfigs = new int[decisionNodes.size()];
        for (int d = 0; d < decisionNodes.size(); d++) {

            int nParentConfigs = 1;
            for (Node parent : decisionNodes.get(d).getParents()) {
                nParentConfigs *= parent.getVariable().getNumStates();
            }
            this.nParentConfigs[d] = nParentConfigs;
            nStates[d] = decisionNodes.get(d).getVariable().getNumStates();
        }

        // Compute the size of the strategy: one element for each decision parent configuration
        int valuesSize = 0;
        for (int d = 0; d < decisionNodes.size(); d++) {
            valuesSize += nParentConfigs[d];
        }

        // Initialize a random strategy and the maximum options for each element in the strategy
        this.options = new int[valuesSize];
        maximumOptions = new int[valuesSize];

        int v = 0;
        for (int d = 0; d < decisionNodes.size(); d++) {
            for (int parentConfig = 0; parentConfig < nParentConfigs[d]; parentConfig++) {
                options[v] = (int) (Math.random() * nStates[d]);
                maximumOptions[v] = nStates[d];
                v++;
            }
        }

    }

    /*"********************
     * Constructor helper *
     **********************/

    // Orders nodes by slice using that the name of a node is nodeName[nSlice]. This is a fragile (not-robust) approach
    // and better alternatives may exist in other places of OpenMarkov.
    private List<List<Node>> orderNodesBySlice(ProbNet probNet) {
        orderedNodesBySlice = new ArrayList<>();

        // Get a blueprint of the nodes of each slice for a correct ordering (higher slices doesn't get ordered
        // so well. This only works for first-order temporal nets.
        ArrayList<Node> sliceNodes = new ArrayList<>();
        for (Node node : ProbNetOperations.sortTopologically(probNet)) {
            if (node.getVariable().getTimeSlice() == 0) {
                sliceNodes.add(node);
            }
        }
        orderedNodesBySlice.add(sliceNodes);

        try {
            for (int slice = 1; slice <= horizon; slice++) {
                sliceNodes = new ArrayList<>();
                for (Node reference : orderedNodesBySlice.get(0)) {
                    // Use of the blueprint here
                    String name = reference.getName().replaceFirst(" \\[\\d+]",
                            " [" + slice + "]");
                    sliceNodes.add(probNet.getNode(name));
                }
                orderedNodesBySlice.add(sliceNodes);
            }
        } catch (NodeNotFoundException e) {
            logger.error("This should never happen." +
                    " If it occurs is because slice naming (i.e. nodename[i]) has changed its format", e);
        }
        return orderedNodesBySlice;
    }

    /*"*****************
     * Strategy output *
     *******************/

    /**
     * Returns a strategy in a format that OpenMarkov can show and understand
     * @return a strategy as a list of decisionPotentials
     */
    public List<Potential> getPotentialForm() {
        List<Potential> strategy = new ArrayList<>();

        for (int d = 0; d < decisionNodes.size(); d++) {

            int nCells = nParentConfigs[d] * nStates[d];
            double[] tablePotentialValues = new double[nCells];
            for (int parentConfig = 0; parentConfig < nParentConfigs[d]; parentConfig++) {
                tablePotentialValues[(nStates[d] * parentConfig) + /* Which column */
                        + (options[d* nParentConfigs[d] + parentConfig]) /* Which row */] = 1;
            }

            // Create a potential that says: "I took those random options"
            List<Variable> oldVariables = new ArrayList<>();
            oldVariables.add(decisionNodes.get(d).getVariable());
            for (Node parent : decisionNodes.get(d).getParents()) {
                oldVariables.add(parent.getVariable());
            }

            Potential decisionPotential = new TablePotential(oldVariables, PotentialRole.POLICY, tablePotentialValues);
            strategy.add(decisionPotential);
        }

        return strategy;
    }


    /**
     * Returns a strategy is a easy to use format. It the equivalent of a evolutionary computation genotype
     * @return a strategy as a vector of options chosen for each parent configuration of decisions in a slice.
     * See {@link #options}.
     */
    public int[] getCompressedForm() {
        return options;
    }


    /*"********************
     * Brute force method *
     **********************/

    /**
     * Evaluates all the strategies of a expanded temporal net. The best strategy is saved in compressed form
     * and returned in potential form.
     * @param limit A computational limit. No more strategies than the limit will be evaluated.
     * @return The strategy in its potential form
     */
    public List<Potential> bruteForce(int limit) {

        logger.info("");
        logger.info("[Beginning brute force]");
        long startTime = System.currentTimeMillis();
        this.createBaseStrategy();
        double bestUtility = this.evaluate();
        int nStrategies = 1;
        List<Potential> bestStrategy = null;

        double utility;
        int[] bestOptions = this.options;
        try { // An IndexOutOfBoundsException will break this loop

            for (int i = 0; i < limit + 1; i++) { // This limits the evaluation to maximumEvaluated strategies
                this.next();
                utility = this.evaluate();
                if (utility > bestUtility) {
                    bestUtility = utility;
                    bestOptions = this.options.clone(); // Save compressed form of best strategy found
                    bestStrategy = this.getPotentialForm();
                    logger.info("A new best utility was found: " + bestUtility);
                }
                nStrategies++;

                if (nStrategies % (limit / 20) == 0) {
                    logger.info((nStrategies) + " strategies evaluated");
                }
            }
        } catch (IndexOutOfBoundsException e) {
            // End of while. Last strategy reached.
        }

        long timeElapsed = System.currentTimeMillis() - startTime;
        logger.info("Best utility of the " + nStrategies + " strategies evaluated: " + bestUtility);
        for (Potential policy : bestStrategy) {
            logger.info(policy.toString());
        }
        logger.info("Time elapsed: " + timeElapsed + " ms");
        options = bestOptions; // Reset de best options found to the strategy manager
        return bestStrategy;
    }

    /**
     * Set the current strategy of the manager as the one with the first option (index 0)
     * for every parent configuration of every decision.
     */
    private void createBaseStrategy() {
        Arrays.fill(this.options, 0);
    }

    /*
     * Converts this strategy into the next (changing from the first to the last). You can use this to run through
     * every possible strategy. When trying to go beyond the last one (maximum option index for every decision)
     * it will throw IndexOutOfBoundsException. Use it as a termination condition.
     */
    private void next() throws IndexOutOfBoundsException {
        tick(0);
    }

    /* Is like a clock tick. Changes a set of options to the next so that:
     * "01110" turns to "11110"
     * If then 1 were the maximum option of the first parent configuration of the first decision, the tick will
     * change the next and then return the previous to 0:
     * "11110" turns to "02110"
     */
    private void tick(int decision) throws IndexOutOfBoundsException {

            options[decision]++;
        // Test if option is out of bounds
        if (options[decision] == maximumOptions[decision]) {
            options[decision] = 0;
            tick(decision + 1);
        }

    }

    /*"*************
     * Random walk *
     ***************/

    /**
     * Evaluates strategiesEvaluated random strategies of a expanded temporal net. The best strategy is saved in compressed form
     * and returned in potential form. Note that it will repeat evaluations if the total number of strategies is lower
     * than {@code strategiesEvaluated}
     * @param strategiesEvaluated The number of strategies it evaluates.
     * @return The strategy in its potential form
     */
    public List<Potential> randomWalk(int strategiesEvaluated) {

        // Evaluate strategiesEvaluated random strategies ()
        logger.info("");
        logger.info("[Random walk]");
        long startTime = System.currentTimeMillis();
        double bestUtility = Double.NEGATIVE_INFINITY;
        int nStrategies = 0;
        List<Potential> bestStrategy = null;

        double utility;
        int[] bestOptions = this.options;
        for (int i = 0; i < strategiesEvaluated; i++) {
            this.createRandomStrategy();
            utility = this.evaluate();
            if (utility > bestUtility) {
                bestUtility = utility;
                bestOptions = this.options.clone(); // Save compressed form of best strategy found
                bestStrategy = this.getPotentialForm();
                logger.info("A new best utility was found: " + bestUtility);
            }
            nStrategies++;
            if (nStrategies % (strategiesEvaluated / 20) == 0) {
                logger.info((nStrategies) + " strategies evaluated");
            }
        }

        long timeElapsed = System.currentTimeMillis() - startTime;
        logger.info("Best utility of the " + nStrategies + " strategies evaluated: " + bestUtility);
        for (Potential policy : bestStrategy) {
            logger.info(policy.toString());
        }
        logger.info("Time elapsed: " + timeElapsed + " ms");
        options = bestOptions; // Reset de best options found to the strategy manager
        return bestStrategy;

    }

    /**
     * Sets the current strategy of the strategy manager to a new random strategy
     **/
    private void createRandomStrategy() {
        int v = 0;
        for (int d = 0; d < decisionNodes.size(); d++) {
            for (int parentConfig = 0; parentConfig < nParentConfigs[d]; parentConfig++) {
                options[v] = (int) (Math.random() * nStates[d]);
                v++;
            }
        }
    }

    //------------------------------ End evaluation methods -----------------------------------

    /* Evaluation */
    /**
     * Evaluates the current strategy of the strategy manager, using backwards evaluation [Dlimid paper]
     * @return the utility for said strategy
     */
    public double evaluate() {

        TablePotential result = null;

        for (int slice = horizon - 1; slice >= 0; slice--) {

            List<Variable> variablesToRemove = new ArrayList<>(); // Variables of the current slice
            List<TablePotential> slicePotentials = new ArrayList<>();
            try {
                for (Node sliceNode : orderedNodesBySlice.get(slice)) {

                    // Add its potential
                    if (sliceNode.getNodeType() == NodeType.CHANCE) {
                        variablesToRemove.add(sliceNode.getVariable());
                        slicePotentials.add(sliceNode.getPotentials().get(0).getCPT());
                    } else if (sliceNode.getNodeType() == NodeType.DECISION) {
                        // The decision potential comes from the strategy
                        variablesToRemove.add(this.getPotentialForm().get(slice).getCPT().getVariable(0));
                        slicePotentials.add(this.getPotentialForm().get(slice).getCPT());
                    } else {
                        // Correct the utility with the previous slice potential if any
                        TablePotential slicePotential;
                        if (slice == horizon - 1) { // Last slice hasn't correction
                            slicePotential = sliceNode.getPotentials().get(0).getCPT();
                        } else {
                            slicePotential = DiscretePotentialOperations.sum(sliceNode.getPotentials().get(0).getCPT(),
                                    result);
                        }
                        slicePotentials.add(slicePotential);

                    }
                }

            } catch (NonProjectablePotentialException | WrongCriterionException e) {
                logger.error("The potential can't be converted to table", e);
            }

            // Fill variablesToKeep for marginalization
            List<Variable> variablesToKeep = AuxiliaryOperations.getUnionVariables(slicePotentials);
            for (Variable variable : AuxiliaryOperations.getUnionVariables(slicePotentials)) {
                if (variablesToRemove.contains(variable)) {
                    variablesToKeep.remove(variable);
                }
            }

            // Multiply the potentials and marginalize the variables of this slice
            result = DiscretePotentialOperations.multiplyAndMarginalize(slicePotentials, variablesToKeep,
                    variablesToRemove);
            result.setPotentialRole(PotentialRole.JOINT_PROBABILITY);

        }
        return result.getValues()[0];

    }



    public List<List<Node>> getOrderedNodesBySlice() {
        return orderedNodesBySlice;
    }

}
