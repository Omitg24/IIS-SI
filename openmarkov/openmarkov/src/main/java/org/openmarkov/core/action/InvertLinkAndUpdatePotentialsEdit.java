/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.action;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openmarkov.core.exception.DoEditException;
import org.openmarkov.core.exception.NodeNotFoundException;
import org.openmarkov.core.exception.NonProjectablePotentialException;
import org.openmarkov.core.exception.WrongCriterionException;
import org.openmarkov.core.model.graph.Link;
import org.openmarkov.core.model.network.Node;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.potential.Potential;
import org.openmarkov.core.model.network.potential.TablePotential;
import org.openmarkov.core.model.network.potential.operation.DiscretePotentialOperations;

/**
 * @author artasom
 * @author iagoparis - summer 2018
 *
 * Inverts the arc between two nodes.
 *
 * Being X -&#62; Y the link that is going to be inverted and being:
 * A: the group of nodes that are parents of X and are not parents of Y,
 * C: the group of nodes that are parents of Y (except X) and are not parents of X, and
 * B the group of parents that X and Y share,
 *
 * The process takes five steps:
 *
 * 1. Invert the arc.
 *
 * 2. Share parents between the nodes.
 *
 * 3. 	Calculate P(x, y|a, b, c) through P(x, y|a, b, c) = P(x|a, b) · P(y|x, b, c)
 * Meaning: P(x, y|a, b, c) = pot(x) · pot(y)
 *
 * 4. Calculate P(y|a, b, c) through P(y|a, b, c) = Σ(x) P(x, y|a, b, c) and assign to node Y this probability.
 *
 * 5. Calculate P(x|a, b, c, y) through P(x|a, b, c, y) = P(x, y|a, b, c) / P(y|a, b, c) and assign to node X this probability.
 */
@SuppressWarnings("serial")
public class InvertLinkAndUpdatePotentialsEdit extends BaseLinkEdit {

    // Logger
    protected Logger logger;

	// x (parent) node
	private final Node x;
	// y (child) node
	private final Node y;
	// In case of undo, this list will keep the links created so they can be deleted
	private final List<Link> linksToUndo = new ArrayList<>();
	// Parent node's old potentials
	private List<Potential> parentsOldPotentials;
	// Child node's old potentials
	private List<Potential> childsOldPotentials;
    // Parent node's new potentials
    private TablePotential xNewPotential;
    // Child node's new potentials
    private TablePotential yNewPotential;

	// Constructor

	/**
	 * @param probNet   {@code ProbNet}
	 * @param variable1 {@code Variable}
	 * @param variable2 {@code Variable}
	 */
	public InvertLinkAndUpdatePotentialsEdit(ProbNet probNet, Variable variable1, Variable variable2) {

		super(probNet, variable1, variable2, true);
		x = probNet.getNode(variable1);
		y = probNet.getNode(variable2);

		logger = LogManager.getLogger(InvertLinkAndUpdatePotentialsEdit.class.getName());

	}

	// Methods

	/**
	 *
	 * @throws DoEditException DoEditException
	 */
	@Override public void doEdit() throws DoEditException {

		// The parents of x are retrieved
		List<Node> xParents = x.getParents();
		// The parents of y are retrieved
		List<Node> yParents = y.getParents();
		// The nodes will share their parents
		List<Node> newParents;

		// 1. Invert the arc.
		// The link between i and j can be removed
		probNet.removeLink(x, y, true);
		// and the link between j and i can be created
		probNet.addLink(y, x, true);

		// 2. Share parents between the nodes.
		// The list of created links is emptied
		linksToUndo.clear();
		// {C(x) \ C(y)} must be parents of y
		// The new parents of y will be those nodes that are parents of x,
		newParents = xParents;
		// and weren't already parents of y
		newParents.removeAll(yParents);

		// The new links are created
		for (Node newParent : newParents) {
			// creating the Link is creating a Link in the node and thus in the graph
			linksToUndo.add(new Link(newParent, y, true));
			//probNet.addLink(probNet.getNode(newParent), y, true);
		}

		// {C(y) \ C(x) \ x}
		// The new parents of x will be those nodes that are parents of y,
		newParents = yParents;
		// and weren't already parents of i
		newParents.removeAll(xParents);
		// excluding also the i node itself
		newParents.remove(x);

		// The new links are created
		for (Node newParent : newParents) {
			// creating the Link is creating a Link in the node and thus in the graph
			linksToUndo.add(new Link(newParent, x, true));
			//probNet.addLink(probNet.getNode(newParent), x, true);
		}

		List<TablePotential> xyPotentials = new ArrayList<>();

		parentsOldPotentials = x.getPotentials();
		childsOldPotentials = y.getPotentials();


		// 3. 	Calculate P(x, y|a, b, c) through P(x, y|a, b, c) = P(x|a, b) · P(y|x, b, c)
		// Meaning: P(x, y|a, b, c) = pot(x) · pot(y)

		// pot(x) are added to xyPotentials
		for (Potential parentsOldPotential : parentsOldPotentials) {

			try {
				xyPotentials.add(parentsOldPotential.getCPT());
			} catch (NonProjectablePotentialException | WrongCriterionException e) {
			    logger.error("Potential not convertible to table or wrong criterion on the old parent of the inverted link");
				e.printStackTrace();
				throw new DoEditException("Parent");
			}
		}

		// pot(y) are added to xyPotentials
		for (Potential childOldPotential : childsOldPotentials) {
			try {
				xyPotentials.add(childOldPotential.getCPT());
			} catch (NonProjectablePotentialException | WrongCriterionException e) {
			    logger.error("Potential not convertible to table or wrong criterion on the old child of the inverted link");
				e.printStackTrace();
				throw new DoEditException("Child");
			}
		}

		// Correct order of variables
		Set<Variable> variables = new LinkedHashSet<>();
		// The first variable from each factor should go before the others
		for (Potential potential: xyPotentials) {
			if (potential.getNumVariables() > 0) {
				variables.add(potential.getVariable(0));
			}
		}
		for (Potential potential : xyPotentials) {
			variables.addAll(potential.getVariables());
		}
		List<Variable> orderedVariables = new ArrayList<>(variables);

		// xyPotentials are multiplied
		TablePotential xyPotentialMultiplied = DiscretePotentialOperations.multiply(xyPotentials);

		// Apply the correction of the order of the variables: Σ(x) P(x, y|a, b, c) = P(a|b, y, c) to Σ(x) P(x, y|a, b, c) = P(y|a, b, c)
		xyPotentialMultiplied = DiscretePotentialOperations.reorder(xyPotentialMultiplied,
				new ArrayList<>(orderedVariables));

		// 4. Calculate P(y|a, b, c) through P(y|a, b, c) = Σ(x) P(x, y|a, b, c) and assign to node Y this probability.
		yNewPotential = DiscretePotentialOperations.marginalize(xyPotentialMultiplied, x.getVariable());
		y.setPotential(yNewPotential);

		// 5. Calculate P(x|a, b, c, y) through P(x|a, b, c, y) = P(x, y|a, b, c) / P(y|a, b, c) and assign to node X this probability.
		xNewPotential = DiscretePotentialOperations.divide(xyPotentialMultiplied, yNewPotential);
        xNewPotential = DiscretePotentialOperations.imposeOtherDistributionWhenDistributionIsZero(xNewPotential);
		x.setPotential(xNewPotential);

		for (Link link : linksToUndo) {
			probNet.addLink((Node) link.getNode1(), (Node) link.getNode2(), true);
		}
	}

	public void undo() {
		super.undo();
		try {
			// Delete link Y -> X
			probNet.removeLink(variable2, variable1, isDirected);
			// Re-create link X -> Y
			probNet.addLink(variable1, variable2, isDirected);
			// Delete the links created when the nodes shared their fathers
			for (Link<Node> undoLink : linksToUndo) {
				probNet.removeLink(undoLink.getNode1(), undoLink.getNode2(), true);
			}
			// The potentials of X are restored to the original ones
			x.setPotentials(parentsOldPotentials);
			// The potentials of Y are restored to the original ones
			y.setPotentials(childsOldPotentials);
		} catch (NodeNotFoundException e) {
			logger.error("Node not found in link from " + variable1.getName() + " to " + variable2.getName());
			e.printStackTrace();
		}
	}


    public void redo() {
	    setTypicalRedo(false);
        super.redo();
        try {
            // Re-remove link X -> Y
            probNet.removeLink(variable1, variable2, isDirected);
            // Recreate link Y -> X
            probNet.addLink(variable2, variable1, isDirected);
            // Re-created the links of shared fathers
            for (Link<Node> linkToRedo : linksToUndo) {
                probNet.addLink(linkToRedo.getNode1(), linkToRedo.getNode2(), true);
            }
            // The potentials of X are restored to the original ones. I convert the only potential to a list of one
            // element to use the same method in undo() and redo(). Using setPotential() (withous s) will modify the
            // parentsOldPotentials and childOldPotential objects, making the next undo()'s useless.
            List<Potential> xNewPotentials= new ArrayList<>();
            xNewPotentials.add(xNewPotential);
            x.setPotentials(xNewPotentials);
            // The potentials of Y are restored to the original ones
            List<Potential> yNewPotentials= new ArrayList<>();
            xNewPotentials.add(yNewPotential);
            y.setPotentials(yNewPotentials);

        } catch (NodeNotFoundException e) {
            logger.error("Node not found in link from " + variable2.getName() + " to " + variable1.getName());
            e.printStackTrace();
        }
    }

	/**
	 * Method to compare two InvertLinkEdits comparing the names of
	 * the source and destination variable alphabetically.
	 *
	 * @param obj InvertLinkAndUpdatePotentialsEdit
	 * @return result of the comparison
	 */
	public int compareTo(InvertLinkAndUpdatePotentialsEdit obj) {
		int result;

		if ((
				result = variable1.getName().compareTo(obj.getVariable1().
						getName())
		) != 0)
			return result;
		if ((
				result = variable2.getName().compareTo(obj.getVariable2().
						getName())
		) != 0)
			return result;
		else
			return 0;
	}

	@Override public String getOperationName() {
		return "Invert link and update potentials";
	}

	public String toString() {
		return "Invert link and update potentials: " + variable1 + "-->" + variable2 + " ==> " + variable1 + "<--" + variable2;
	}

	@Override public BaseLinkEdit getUndoEdit() {
		return new InvertLinkAndUpdatePotentialsEdit(getProbNet(), getVariable2(),
				getVariable1());
	}

}