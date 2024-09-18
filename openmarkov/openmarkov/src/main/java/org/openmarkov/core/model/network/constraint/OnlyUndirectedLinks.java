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
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.constraint.annotation.Constraint;

@Constraint(name = "OnlyUndirectedLinks", defaultBehavior = ConstraintBehavior.NO) public class OnlyUndirectedLinks
		extends PNConstraint {

	// Attributes.
	private String explanation;

	@Override public boolean checkProbNet(ProbNet probNet) {
		List<Node> nodes = probNet.getNodes();
		for (Node node : nodes) {
			// Only check children because with this is enough
			// to look for directed links
			if (probNet.getNumChildren(node) != 0) {
				return false;
			}
		}
		return true;
	}

	@Override public boolean checkEdit(ProbNet probNet, PNEdit edit)
			throws NonProjectablePotentialException, WrongCriterionException {
		List<PNEdit> edits = UtilConstraints.getSimpleEditsByType(edit, AddLinkEdit.class);
		for (PNEdit simpleEdit : edits) {
			if (((AddLinkEdit) simpleEdit).isDirected()) {
				AddLinkEdit addLink = (AddLinkEdit) simpleEdit;
				explanation = new String(addLink.getVariable1() + " --> " + addLink.getVariable2());
				return false;
			}
		}
		return true;
	}

	@Override protected String getMessage() {
		return explanation + ". no directed links allowed";
	}

}
