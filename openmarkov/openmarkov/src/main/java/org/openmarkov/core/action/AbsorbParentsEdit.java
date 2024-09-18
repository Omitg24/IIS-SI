package org.openmarkov.core.action;

import java.util.List;

import org.openmarkov.core.exception.NonProjectablePotentialException;
import org.openmarkov.core.exception.WrongCriterionException;
import org.openmarkov.core.inference.BasicOperations;
import org.openmarkov.core.model.network.Node;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.potential.Potential;

public class AbsorbParentsEdit extends CompoundPNEdit {
	Node node;	

	public AbsorbParentsEdit(ProbNet probNet, Node node) {
		super(probNet);
		this.node = node;
	}

	@Override
	public void generateEdits() throws NonProjectablePotentialException, WrongCriterionException {
		// gets neighbors of this node
		Variable nodeVariable = node.getVariable();
		List<Node> parents = probNet.getParents(node);
		Potential potential = BasicOperations.buildPotentialByAbsorbingParents(node, null);
		for (Node x : parents) {
			PNEdit newEdit;
			if (x.getChildren().size() > 1) {
				newEdit = new RemoveLinkEdit(probNet, x.getVariable(), nodeVariable, true, false);
			} else {// x.getChildren().size() == 1
				newEdit = new CRemoveNodeEdit(probNet, x);
			}
			edits.add(newEdit);
		}

		for (Variable variable : potential.getVariables()) {
			if (variable != nodeVariable) {
				edits.add(new AddLinkEdit(probNet, variable, nodeVariable, true, false));
			}
		}
		edits.add(new SetPotentialEdit(node, potential));
	}
}
