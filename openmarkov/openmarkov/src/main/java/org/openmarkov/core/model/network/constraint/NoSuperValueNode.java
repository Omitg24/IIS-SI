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
import org.openmarkov.core.model.network.constraint.annotation.Constraint;

@Constraint(name = "NoSuperValueNodes", defaultBehavior = ConstraintBehavior.OPTIONAL) public class NoSuperValueNode
		extends PNConstraint {

	@Override public boolean checkProbNet(ProbNet probNet) {
		List<Node> nodes = probNet.getNodes();
		for (Node node : nodes) {
			if (node.isSuperValueNode()) {
				return false;
			}
		}
		return true;
	}

	@Override public boolean checkEdit(ProbNet probNet, PNEdit edit)
			throws NonProjectablePotentialException, WrongCriterionException {
		// AddLinkEdit
		List<PNEdit> edits = UtilConstraints.getSimpleEditsByType(edit, AddLinkEdit.class);
		for (PNEdit simpleEdit : edits) {
			Node node = ((AddLinkEdit) simpleEdit).getNode1();
			if (node.getNodeType() == NodeType.UTILITY) {
				return false;
			}
		}
		return true;
	}

	@Override protected String getMessage() {
		return "adding a super value node is not allowed";
	}

}
