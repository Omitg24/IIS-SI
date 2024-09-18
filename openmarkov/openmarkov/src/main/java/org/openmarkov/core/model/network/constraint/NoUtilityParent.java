/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.model.network.constraint;

import java.util.List;

import org.openmarkov.core.action.AddLinkEdit;
import org.openmarkov.core.action.PNEdit;
import org.openmarkov.core.exception.NonProjectablePotentialException;
import org.openmarkov.core.exception.WrongCriterionException;
import org.openmarkov.core.model.network.Node;
import org.openmarkov.core.model.network.NodeType;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.constraint.annotation.Constraint;

@Constraint(name = "NoUtilityParent", defaultBehavior = ConstraintBehavior.YES) public class NoUtilityParent
		extends PNConstraint {

	@Override public boolean checkProbNet(ProbNet probNet) {
		List<Node> utilityNodes = probNet.getNodes(NodeType.UTILITY);
		for (Node utilNode : utilityNodes) {
			List<Node> children = probNet.getChildren(utilNode);
			for (Node child : children) {
				if (child.getNodeType() != NodeType.UTILITY) {
					return false;
				}
			}
		}
		return true;
	}

	@Override public boolean checkEdit(ProbNet probNet, PNEdit edit)
			throws NonProjectablePotentialException, WrongCriterionException {

		List<PNEdit> edits = UtilConstraints.getSimpleEditsByType(edit, AddLinkEdit.class);

		for (PNEdit simpleEdit : edits) {
			if (((AddLinkEdit) simpleEdit).isDirected()) {
				Variable variable1 = ((AddLinkEdit) simpleEdit).getVariable1();
				Node node1 = probNet.getNode(variable1);
				if (node1.getNodeType() == NodeType.UTILITY) {
					Variable variable2 = ((AddLinkEdit) simpleEdit).getVariable2();
					Node node2 = probNet.getNode(variable2);
					if (node2.getNodeType() != NodeType.UTILITY) {
						return false;
					}
				}
			}
		}
		return true;
	}

	@Override protected String getMessage() {
		return "Utility nodes only can have utility children";
	}

}
