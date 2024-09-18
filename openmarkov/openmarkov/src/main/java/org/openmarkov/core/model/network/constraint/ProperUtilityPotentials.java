/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.model.network.constraint;

import java.util.List;

import org.openmarkov.core.action.AddNodeEdit;
import org.openmarkov.core.action.PNEdit;
import org.openmarkov.core.action.RemoveNodeEdit;
import org.openmarkov.core.exception.NonProjectablePotentialException;
import org.openmarkov.core.exception.WrongCriterionException;
import org.openmarkov.core.model.network.Node;
import org.openmarkov.core.model.network.NodeType;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.constraint.annotation.Constraint;
import org.openmarkov.core.model.network.potential.Potential;

@Constraint(name = "ProperUtilityPotentials", defaultBehavior = ConstraintBehavior.OPTIONAL) public class ProperUtilityPotentials
		extends PNConstraint {

	public boolean checkProbNet(ProbNet probNet) {
		List<Node> utilityNodes = probNet.getNodes(NodeType.UTILITY);
		if (utilityNodes.size() == 0) {
			return false;
		}
		for (Node utilityNode : utilityNodes) {
			List<Potential> utilityPotentials = utilityNode.getPotentials();
			if ((utilityPotentials == null) || (utilityPotentials.size() == 0)) {
				return false;
			}
		}
		return true;
	}

	public boolean checkEdit(ProbNet probNet, PNEdit edit)
			throws NonProjectablePotentialException, WrongCriterionException {
		List<PNEdit> edits = UtilConstraints.getSimpleEditsByType(edit, AddNodeEdit.class);
		int numUtilities = probNet.getNumNodes(NodeType.UTILITY);
		for (PNEdit simpleEdit : edits) {
			if (((AddNodeEdit) simpleEdit).getNodeType() == NodeType.UTILITY) {
				numUtilities = numUtilities + 1;
			}
		}
		edits = UtilConstraints.getSimpleEditsByType(edit, RemoveNodeEdit.class);
		for (PNEdit simpleEdit : edits) {
			if (((RemoveNodeEdit) simpleEdit).getNodeType() == NodeType.UTILITY) {
				numUtilities = numUtilities - 1;
			}
		}
		return (numUtilities > 0);
	}

	public String toString() {
		return this.getClass().getName();
	}

	@Override protected String getMessage() {
		return "there is at least one utility variable without "
				+ "utility potential or there are no utility potentials";
	}

}
