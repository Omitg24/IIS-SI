/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.action;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openmarkov.core.exception.DoEditException;
import org.openmarkov.core.exception.NonProjectablePotentialException;
import org.openmarkov.core.exception.WrongCriterionException;
import org.openmarkov.core.model.graph.Link;
import org.openmarkov.core.model.network.Node;
import org.openmarkov.core.model.network.NodeType;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.potential.ExactDistrPotential;
import org.openmarkov.core.model.network.potential.Potential;
import org.openmarkov.core.model.network.potential.TablePotential;
import org.openmarkov.core.model.network.potential.operation.AuxiliaryOperations;
import org.openmarkov.core.model.network.potential.operation.DiscretePotentialOperations;

/**
 * This edit makes the net absorb a node, merging all the utility children into one and updating its potential.
 * @author iagoparis - summer 2018
 * @version 1.0
 * @since OpenMarkov 0.3
 */

@SuppressWarnings("serial") public class AbsorbNodeEdit extends SimplePNEdit{

    // Logger
    protected Logger logger;


    // Both node and variable attributes are created for convenience but one could be extracted from the other
    private Variable absorbedVariable;

    private Node absorbedNode;

    /* Undo attributes */
    // Un-absorb node
    private List<Link<Node>> linksDeleted;

    private List<Link<Node>> newParentLinks;

    private List<Potential> oldUtilityPotentials;

    // Unmerge utility node
    private boolean utilityNodesMerged;

    private List<Node> oldUtilityChildren;

    private ArrayList<Link<Node>> oldChildrenLinks;

    /* Redo attributes */
    private List<Potential> newPotentials;

    private Node mergedUtility;

    private Set<Node> mergedParents;
    private TablePotential maximizedPotential2;

    // Constructor

    /**
     * @param probNet    {@code ProbNet}
     * @param absorbedVariable  {@code Variable}
     */
    public AbsorbNodeEdit(ProbNet probNet, Variable absorbedVariable) {
        super(probNet);
        this.absorbedVariable = absorbedVariable;
        this.absorbedNode = probNet.getNode(absorbedVariable);
        this.linksDeleted = new ArrayList<>();
        this.newParentLinks = new ArrayList<>();

        this.logger = LogManager.getLogger(AbsorbNodeEdit.class.getName());
    }

    @Override
    public void doEdit() throws DoEditException {

        // If there are more than one utility children, merge them into one node
        if (absorbedNode.getChildren().size() > 1) {
            mergeUtilityChildren();
            utilityNodesMerged = true;
        } else {
            utilityNodesMerged = false;
        }

        Node child = absorbedNode.getChildren().get(0);
        oldUtilityPotentials = child.getPotentials();
        newPotentials = new ArrayList<>();

        /* Chance parent */
        if (absorbedNode.getNodeType() == NodeType.CHANCE) {
            for (Potential potential : oldUtilityPotentials) {

                // Potentials to multiply
                List<TablePotential> utilityAndChance = new ArrayList<>();
                try {
                    utilityAndChance.add(potential.getCPT()); //Utility
                    utilityAndChance.add(absorbedNode.getPotentials().get(0).getCPT()); //Chance

                } catch (NonProjectablePotentialException | WrongCriterionException e) {
                    e.printStackTrace();
                    logger.error("Potential not convertible to table or wrong criterion");
                    throw new DoEditException("Potential not convertible to table or wrong criterion");

                }

                /* Obtain parameters to invoke multiplyAndMarginalize */
                // All variables from chance parent and utility child potentials
                List<Variable> unionVariables = AuxiliaryOperations.getUnionVariables(utilityAndChance);

                List<Variable> variablesToKeep = new ArrayList<>(unionVariables);
                variablesToKeep.remove(absorbedVariable);

                List<Variable> variablesToEliminate = new ArrayList<>();
                variablesToEliminate.add(absorbedVariable);

                // Discrete operation is valid because all parents are discrete
                TablePotential marginalizedPotential = DiscretePotentialOperations.
                        multiplyAndMarginalize(utilityAndChance, variablesToKeep, variablesToEliminate);

                // Convert to utility potential
                ExactDistrPotential exactDistrPotential = new ExactDistrPotential(variablesToKeep);
                exactDistrPotential.setValues(marginalizedPotential.values);

                newPotentials.add(exactDistrPotential);
            }

            // Parents of chance node are now parents of utility node
            for (Node parent : absorbedNode.getParents() ) {
                Link<Node> link = probNet.getLink(parent, child, true);
                if (link == null) {
                    // creating the Link saves it in the graph
                    newParentLinks.add(probNet.addLink(parent, child, true));

                }
            }

        /* Decision parent */
        } else if (absorbedNode.getNodeType() == NodeType.DECISION) {
            for (Potential potential : oldUtilityPotentials) {
                TablePotential utilityPotential;

                try {
                    utilityPotential = potential.getCPT();
                } catch (NonProjectablePotentialException | WrongCriterionException e) {
                    logger.error("Potential not convertible to table or wrong criterion");
                    throw new DoEditException("Potential not convertible to table or wrong criterion");
                }

                // Discrete operation is valid because all parents are discrete
                TablePotential maximizedPotential = (TablePotential) DiscretePotentialOperations.
                        maximize(utilityPotential, absorbedVariable)[0];
                List<Variable> newVariables = new ArrayList<>(potential.getVariables());
                newVariables.remove(absorbedVariable);


                // Convert to utility potential
                ExactDistrPotential exactDistrPotential = new ExactDistrPotential(newVariables);
                exactDistrPotential.setValues(maximizedPotential.values);

                newPotentials.add(exactDistrPotential);
            }
            // Parents of decision node don't turn into parents of utility node
        }
        child.setPotentials(newPotentials);


        // Links saved for the undo()
        linksDeleted = getLinksWithNode(absorbedNode);
        probNet.removeNode(absorbedNode);
    }

    private void mergeUtilityChildren() throws DoEditException {

        // Save the old children for undoing
        oldUtilityChildren = absorbedNode.getChildren();
        oldChildrenLinks = new ArrayList<>();
        for (Node child : oldUtilityChildren) {
            oldChildrenLinks.addAll(getLinksWithNode(child));
        }

        /* Create the merged node */
        // Create the name
        StringBuilder mergedName = new StringBuilder();
        for (Node child : oldUtilityChildren) {
            mergedName.append(child.getName());
            mergedName.append(" + ");
        }

        int lastPlus = mergedName.lastIndexOf(" + ");
        mergedName.delete(lastPlus, lastPlus + 3); // Delete the extra plus added at the end

        // Get the position for the new node, the gravity center of children (which is the average by coordinate).
        double x = 0;
        double y = 0;
        for (Node child : oldUtilityChildren) {
            x += child.getCoordinateX();
            y += child.getCoordinateY();
        }

        x /= oldUtilityChildren.size(); // Division by 0 is tested in the Validator class.
        y /= oldUtilityChildren.size();

        // Gather all parents of every component node merged
        mergedParents = new HashSet<>();
        for (Node child : oldUtilityChildren) {
            mergedParents.addAll(child.getParents());
        }

        // Create the node
        Variable mergedVariable = new Variable(mergedName.toString());
        mergedUtility = new Node(probNet, mergedVariable, NodeType.UTILITY);
        mergedUtility.setCoordinateX(x);
        mergedUtility.setCoordinateY(y);
        probNet.addNode(mergedUtility);

        // Create the links
        for (Node parent : mergedParents) {
            probNet.addLink(parent, mergedUtility, true);
        }

        /* Create the potential */

        // Get the potential table of every component child
        List<TablePotential> utilityChildrenPotentials = new ArrayList<>();
        try {
            for (Node child : oldUtilityChildren) {
                // Change the variable of the component potentials to the merged variable
                TablePotential componentPotential = child.getPotentials().get(0).getCPT();
                componentPotential.replaceVariable(componentPotential.getVariable(0), mergedVariable);
                // Add the potential to the list to be summed
                utilityChildrenPotentials.add(componentPotential);
            }
        } catch (NonProjectablePotentialException | WrongCriterionException e) {
            logger.error("Potential not convertible to table or wrong criterion.");
            e.printStackTrace();
            throw new DoEditException(e.getLocalizedMessage());
        }

        // Sum the component potentials
        TablePotential sumPotential = DiscretePotentialOperations.sum(utilityChildrenPotentials);
        mergedUtility.setPotential(sumPotential); // Set the potential to the node

        // Remove the children that merged into the new utility node
        for (Node child : oldUtilityChildren) {
            probNet.removeNode(child);
        }

    }

    public void undo() {
        super.undo();
        probNet.addNode(absorbedNode);
        // Restore deleted links
        if (linksDeleted.size() != 0) {
            for (Link<Node> link : linksDeleted) {
                probNet.addLink(link.getNode1(), link.getNode2(), true);
            }
        }

        absorbedNode.getChildren().get(0).setPotentials(oldUtilityPotentials);
        // Destroy created utility links
        if (newParentLinks.size() != 0) {
            for (Link<Node> link : newParentLinks) {
                probNet.removeLink(link.getNode1(), link.getNode2(), true);
            }
        }

        // If utility children were merged, oldUtilityPotentials contains the potential of the merged utility children
        // and not its component potentials, however, restoring the component nodes will restore their respective
        // potentials ignoring the merged one.
        if (utilityNodesMerged) {
            probNet.removeNode(absorbedNode.getChildren().get(0));
            // Restore merged nodes
            for (Node utilityChild : oldUtilityChildren) {
                probNet.addNode(utilityChild);
            }
            // Restore their links
            for (Link<Node> link : oldChildrenLinks) {
                probNet.addLink(link.getNode1(), link.getNode2(), true);
            }

        }


    }

    public void redo() {
        setTypicalRedo(false);
        super.redo();
        if (utilityNodesMerged) {
            probNet.addNode(mergedUtility);
            // Re-create the links //
            for (Node parent : mergedParents) {
                probNet.addLink(parent, mergedUtility, true);
            }
            // Remove the children that merged into the new utility node
            for (Node child : oldUtilityChildren) {
                probNet.removeNode(child);
            }
        }

        // Re-create utility links
        if (newParentLinks.size() != 0) {
            for (Link<Node> link : newParentLinks) {
                probNet.addLink(link.getNode1(), link.getNode2(), true);
            }
        }
        absorbedNode.getChildren().get(0).setPotentials(newPotentials);

        probNet.removeNode(absorbedNode);

    }

    /*
     * Returns all the incoming and outcoming links of a given node.
     */
    private ArrayList<Link<Node>> getLinksWithNode(Node node) {
        ArrayList<Link<Node>> links = new ArrayList<>();
        for (Link<Node> link : probNet.getLinks() ) {
            if (link.contains(node)) {
                links.add(link);
            }
        }
        return links;
    }

}
