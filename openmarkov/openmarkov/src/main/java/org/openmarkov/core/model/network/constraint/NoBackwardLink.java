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
import org.openmarkov.core.exception.NodeNotFoundException;
import org.openmarkov.core.exception.NonProjectablePotentialException;
import org.openmarkov.core.exception.WrongCriterionException;
import org.openmarkov.core.model.network.Node;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.constraint.annotation.Constraint;

@Constraint(name = "NoBackwardLinks", defaultBehavior = ConstraintBehavior.YES) public class NoBackwardLink
		extends PNConstraint {

	@Override public boolean checkProbNet(ProbNet probNet) {
		List<Node> probNetNodes = probNet.getNodes();
		int nodeTimeSlice;
		try {
			for (Node node : probNetNodes) {
				// If the node is temporal
				if (probNet.getVariable(node.getName()).isTemporal()) {
					// We retrieve its children
					List<Node> children = probNet.getChildren(node);
					// and we iterate over them
					for (Node child : children) {
						// checking if there is any not allowed link
						if (!allowedLink(probNet.getVariable(node.getName()), probNet.getVariable(child.getName()))) {
							return false;
						}
					}
				}

			}
		} catch (NodeNotFoundException e) {
			e.printStackTrace();
		}
		// If we have reached this point, there is no forbidden backward link
		return true;
	}

	@Override public boolean checkEdit(ProbNet probNet, PNEdit edit)
			throws NonProjectablePotentialException, WrongCriterionException {
		List<PNEdit> edits = UtilConstraints.getSimpleEditsByType(edit, AddLinkEdit.class);
		for (PNEdit simpleEdit : edits) {
			if (!allowedLink(((AddLinkEdit) simpleEdit).getVariable1(), ((AddLinkEdit) simpleEdit).getVariable2())) {
				return false;
			}
		}
		return true;
	}

	private boolean allowedLink(Variable variable1, Variable variable2) {
		boolean allowed = true;
		// If both variables are temporal, the second must not belong to a previous time slices
		// And the first is temporal and the second is not, the former must belong to the zeroth slice
		if ((
				variable1.isTemporal() && variable2.isTemporal() && variable2.getTimeSlice() < variable1.getTimeSlice()
		) || (
				variable1.isTemporal() && !variable2.isTemporal() && variable1.getTimeSlice() != 0
		)) {
			allowed = false;
		}
		return allowed;
	}

	@Override protected String getMessage() {
		// TODO Auto-generated method stub
		return "Links can only be drawn to future slices or from nodes in slice 0 towards atemporal nodes";
	}

}
