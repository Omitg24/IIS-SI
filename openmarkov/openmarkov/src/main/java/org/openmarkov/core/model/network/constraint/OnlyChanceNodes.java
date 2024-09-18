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
import org.openmarkov.core.exception.NonProjectablePotentialException;
import org.openmarkov.core.exception.WrongCriterionException;
import org.openmarkov.core.model.network.Node;
import org.openmarkov.core.model.network.NodeType;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.constraint.annotation.Constraint;

@Constraint(name = "OnlyChanceNodes", defaultBehavior = ConstraintBehavior.NO) public class OnlyChanceNodes
		extends PNConstraint {

	@Override public boolean checkProbNet(ProbNet probNet) {
		List<Node> nodes = probNet.getNodes();
		for (Node node : nodes) {
			if (node.getNodeType() != NodeType.CHANCE) {
				return false;
			}
		}
		return true;
	}

	@Override public boolean checkEdit(ProbNet probNet, PNEdit edit)
			throws NonProjectablePotentialException, WrongCriterionException {
		List<PNEdit> edits = UtilConstraints.getSimpleEditsByType(edit, AddNodeEdit.class);
		for (PNEdit simpleEdit : edits) {
			if (((AddNodeEdit) simpleEdit).getNodeType() != NodeType.CHANCE) {
				return false;
			}
		}
		return true;
	}

	@Override protected String getMessage() {
		return "only chance nodes allowed";
	}

}
