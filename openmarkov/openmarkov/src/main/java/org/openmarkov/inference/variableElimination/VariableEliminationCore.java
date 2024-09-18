/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.inference.variableElimination;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.openmarkov.core.action.PNESupport;
import org.openmarkov.core.action.RemoveNodeEdit;
import org.openmarkov.core.exception.CostEffectivenessException;
import org.openmarkov.core.exception.DoEditException;
import org.openmarkov.core.exception.NonProjectablePotentialException;
import org.openmarkov.core.exception.PotentialOperationException;
import org.openmarkov.core.exception.UnexpectedInferenceException;
import org.openmarkov.core.exception.WrongCriterionException;
import org.openmarkov.core.inference.heuristic.EliminationHeuristic;
import org.openmarkov.core.model.network.Criterion;
import org.openmarkov.core.model.network.NodeType;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.potential.GTablePotential;
import org.openmarkov.core.model.network.potential.Potential;
import org.openmarkov.core.model.network.potential.PotentialRole;
import org.openmarkov.core.model.network.potential.TablePotential;
import org.openmarkov.core.model.network.potential.operation.DiscretePotentialOperations;
import org.openmarkov.inference.variableElimination.action.CreatePotentialUtility;

/**
 * Essential variable elimination algorithm for Bayesian networks and influence diagrams.
 *
 * @author Manuel Arias
 */
public class VariableEliminationCore {

	// Attributes
	/**
	 * Minimum threshold by default.
	 */
	protected static final double defLambdaMin = 0.0;

	/**
	 * Higher threshold by default.
	 */
	protected static final double defLambdaMax = Double.POSITIVE_INFINITY;

	/**
	 * Minimum threshold applicable to this case.
	 */
	protected double lambdaMin;

	/**
	 * Higher threshold applicable to this case.
	 */
	protected double lambdaMax;

	private ProbNet markovDecisionNetwork;

	private boolean isUnicriterion;

	private EliminationHeuristic heuristic;

	/**
	 * Decision variables with their policies.
	 */
	private Map<Variable, TablePotential> optimalPolicies;

	/**
	 * Whether the utility potentials have been joined into a single CEP potential,
	 * which will be of type <code>GTablePotential</code>. This operation is performed
	 * in the case of cost-effectiveness analysis, just before eliminating the first decision.
	 */
	private boolean thereIsCEPPotential;

    private PNESupport pneSupport;

	// Constructors

	/**
	 * Initialize data structures and executes the algorithm.
	 *
	 * @param markovDecisionNetwork <code>ProbNet</code>
	 * @param heuristic             <code>EliminationHeuristic</code>
	 * @param isUnicriterion        <code>boolean</code>
	 * @throws UnexpectedInferenceException
	 */
	public VariableEliminationCore(ProbNet markovDecisionNetwork, EliminationHeuristic heuristic,
			boolean isUnicriterion) throws UnexpectedInferenceException {

		initialize(markovDecisionNetwork, heuristic, isUnicriterion);
		performVariableElimination();
	}

	/**
	 * Initialize data structures and executes the algorithm.
	 * This constructor must be used only in bi-criteria analysis.
	 *
	 * @param markovDecisionNetwork <code>ProbNet</code>
	 * @param heuristic             <code>EliminationHeuristic</code>
	 * @param isUnicriterion        <code>boolean</code>
	 * @param lambdaMin             <code>double</code>
	 * @param lambdaMax             <code>double</code>
	 * @throws UnexpectedInferenceException
	 */
	public VariableEliminationCore(ProbNet markovDecisionNetwork, EliminationHeuristic heuristic,
			boolean isUnicriterion, double lambdaMin, double lambdaMax) throws UnexpectedInferenceException {

		this.lambdaMin = lambdaMin;
		this.lambdaMax = lambdaMax;
		initialize(markovDecisionNetwork, heuristic, isUnicriterion);
		performVariableElimination();
	}

	// Methods

	/**
	 * @throws UnexpectedInferenceException
	 */
	private void performVariableElimination() throws UnexpectedInferenceException {
		Variable variableToDelete;
		while ((variableToDelete = heuristic.getVariableToDelete()) != null) {
			try {
				eliminateVariable(variableToDelete);
			} catch (UnexpectedInferenceException | PotentialOperationException | CostEffectivenessException | NonProjectablePotentialException | DoEditException | WrongCriterionException e) {
				throw new UnexpectedInferenceException(e.getMessage());
			}
		}
	}

	/**
	 * @param variableToDelete
	 * @throws UnexpectedInferenceException
	 * @throws PotentialOperationException
	 * @throws CostEffectivenessException
	 * @throws DoEditException
	 * @throws NonProjectablePotentialException
	 * @throws WrongCriterionException
	 */
	@SuppressWarnings("rawtypes") private void eliminateVariable(Variable variableToDelete)
			throws UnexpectedInferenceException, PotentialOperationException, CostEffectivenessException,
			DoEditException, NonProjectablePotentialException, WrongCriterionException {
		NodeType nodeType = markovDecisionNetwork.getNode(variableToDelete).getNodeType();

		if (!isUnicriterion && nodeType == NodeType.DECISION && !thereIsCEPPotential) {
			createCEPPotential();
		}

		// Extract the potentials that depend on the variable
		List<TablePotential> probPotentials = new ArrayList<>();
		for (Potential potential : markovDecisionNetwork.getProbPotentials(variableToDelete)) {
			probPotentials.add((TablePotential) potential);
			markovDecisionNetwork.removePotential(potential);
		}
		List<TablePotential> utilityPotentials = new ArrayList<>();
		for (Potential potential : markovDecisionNetwork.getUtilityPotentials(variableToDelete)) {
			utilityPotentials.add((TablePotential) potential);
			markovDecisionNetwork.removePotential(potential);
		}
		RemoveNodeEdit removeNodeEdit = new RemoveNodeEdit(markovDecisionNetwork, variableToDelete);
		pneSupport.doEdit(removeNodeEdit);
		if (nodeType == NodeType.CHANCE) {
			ChanceVariableElimination elimination = new ChanceVariableElimination(variableToDelete, probPotentials,
					utilityPotentials);
			markovDecisionNetwork.addPotential(elimination.getMarginalProbability());
			for (Potential potential : elimination.getUtilityPotentials()) {
				markovDecisionNetwork.addPotential(potential);
			}

		} else {
			DecisionVariableElimination elimination = new DecisionVariableElimination(variableToDelete, probPotentials,
					utilityPotentials);
			markovDecisionNetwork.addPotential(elimination.getProjectedProbability());
			markovDecisionNetwork.addPotential(elimination.getUtility());
			optimalPolicies.put(variableToDelete, elimination.getOptimalPolicy());
		}
	}

	//TODO Revisar quitar (Manolo)
	/*private void addNewUtilityPotentialToMarkovDecisionNetwork(
			TablePotential utilityPotential,
			Potential newPotential) {
		newPotential.setCriterion(utilityPotential.getCriterion()); // Set as utility potential
		markovDecisionNetwork.addPotential(newPotential);
	}
*/

	/**
	 * Collects all the utility potentials, removes them from the network, and joins them
	 * into a GTablePotential of <code>CEP</code>s, which is added to the network and returned.
	 *
	 * @return a <code>GTablePotential</code> of <code>CEP</code>s
	 */
	@SuppressWarnings("rawtypes") public GTablePotential createCEPPotential() throws UnexpectedInferenceException {
		// Join all the utility potentials into a GTablePotential of CEPs
		ArrayList<TablePotential> costPotentials = new ArrayList<TablePotential>();
		ArrayList<TablePotential> effectivenessPotentials = new ArrayList<TablePotential>();

		for (Potential potential : markovDecisionNetwork.getAdditivePotentials()) {
			markovDecisionNetwork.removePotential(potential);
			if (potential.getCriterion().getCECriterion().equals(Criterion.CECriterion.Cost)) {
				costPotentials.add((TablePotential) potential);
			} else {
				effectivenessPotentials.add((TablePotential) potential);
			}
		}

		TablePotential costPotential = DiscretePotentialOperations.sum(costPotentials);
		TablePotential effectivenessPotential = DiscretePotentialOperations.sum(effectivenessPotentials);

		GTablePotential cepUtilityPotential;
		try {
			cepUtilityPotential = CreatePotentialUtility
					.createCEPotential(costPotential, effectivenessPotential, lambdaMin, lambdaMax);
			cepUtilityPotential.setCriterion(new Criterion("#{COST-EFFECTIVENESS}#")); // Set as utility potential
			markovDecisionNetwork.addPotential(cepUtilityPotential);
		} catch (CostEffectivenessException e) {
			throw new UnexpectedInferenceException(e.getMessage());
		}
		thereIsCEPPotential = true;
		return cepUtilityPotential;
	}

	/**
	 * @return <code>Map</code> with key = <code>Variable</code> and value = <code>Potential</code>
	 */
	public Map<? extends Variable, ? extends Potential> getOptimalPolicies() {
		return optimalPolicies;
	}

	/**
	 * @param decisionVariable
	 * @return The policy of <code>decisionVariable</code>. <code>Potential</code>
	 */
	public Potential getOptimalPolicy(Variable decisionVariable) {
		return optimalPolicies.get(decisionVariable);
	}

	
	/**
	 * @throws UnexpectedInferenceException
	 * @return TablePotential
	 */
	public TablePotential getUtility() throws UnexpectedInferenceException {

 		List<Potential> utilityPotentials = markovDecisionNetwork.getAdditivePotentials();
		TablePotential utility = null;
		int numUtilityPotentials = utilityPotentials.size();

		if (isUnicriterion) {
		    if (numUtilityPotentials == 0) {
                utility = new TablePotential(null, PotentialRole.UNSPECIFIED);
                utility.setCriterion(new Criterion()); // Set this potential as additive with the "Default" criterion type.
            } else if (numUtilityPotentials == 1) {
                utility = (TablePotential) utilityPotentials.get(0);
            } else {
                List<TablePotential> utilityTablePotentials = new ArrayList<>();
                for (Potential potential : utilityPotentials) {
                    utilityTablePotentials.add((TablePotential) potential);
                }
                utility = DiscretePotentialOperations.sum(utilityTablePotentials);
            }
        } else {
		    if (numUtilityPotentials == 1) {
                TablePotential firstPotential = (TablePotential) utilityPotentials.get(0);
                utility = firstPotential instanceof GTablePotential ? firstPotential : createCEPPotential();
            } else { // No potentials or several
		        utility = createCEPPotential();
            }
        }

		return utility;
	}
	
	
	/**
	 * @throws UnexpectedInferenceException
	 * @return <code>TablePotential</code>
	 */
	public TablePotential getUtilityOld() throws UnexpectedInferenceException {
		List<Potential> utilityPotentials = markovDecisionNetwork.getAdditivePotentials();

		TablePotential utility;
		if (utilityPotentials.isEmpty()) {
			utility = isUnicriterion ? new TablePotential(null, PotentialRole.UNSPECIFIED): createCEPPotential();
			utility.setCriterion(new Criterion()); // Set this potential as additive with the "Default" criterion type.			
		} else {
			if (isUnicriterion) {
				// sum the utility potentials
				List<TablePotential> utilityTablePotentials = new ArrayList<>();
				for (Potential potential : utilityPotentials) {
					utilityTablePotentials.add((TablePotential) potential);
				}
				utility = DiscretePotentialOperations.sum(utilityTablePotentials);
			} else {
				utility = thereIsCEPPotential ? (TablePotential) utilityPotentials.get(0): createCEPPotential();
			}
		}
		return utility;
	}

	/**
	 * @return A <code>TablePotential</code> that is the result of the multiplication
	 * of all the probability potentials.
	 */
	public TablePotential getProbability() {
		List<TablePotential> probPotentials = new ArrayList<>();
		List<Potential> allPotentials = markovDecisionNetwork.getPotentials();
		for (Potential potential : allPotentials) {
			if (!potential.isAdditive()) {
				probPotentials.add((TablePotential) potential);
			}
		}
		return DiscretePotentialOperations.multiply(probPotentials);
	}

	/**
	 * Common code of the two constructors.
	 *
	 * @param markovDecisionNetwork <code>ProbNet</code>
	 * @param heuristic             <code>EliminationHeuristic</code>
	 * @param isUnicriterion
	 */
	private void initialize(ProbNet markovDecisionNetwork, EliminationHeuristic heuristic, boolean isUnicriterion) {

		thereIsCEPPotential = false;
		this.markovDecisionNetwork = markovDecisionNetwork;
		this.heuristic = heuristic;

		pneSupport = markovDecisionNetwork.getPNESupport();
		pneSupport.addUndoableEditListener(heuristic);

		this.isUnicriterion = isUnicriterion;

		if (!isUnicriterion) {
			lambdaMin = defLambdaMin;
			lambdaMax = defLambdaMax;
		}
		optimalPolicies = new LinkedHashMap<Variable, TablePotential>();
		// The iterators through LinkedHashMap follows the insertion order, which can be useful later
	}

	public ProbNet getMarkovDecisionNetwork() {
		return this.markovDecisionNetwork;
	}

}
