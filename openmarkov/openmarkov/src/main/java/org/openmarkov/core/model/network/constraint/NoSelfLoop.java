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
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.constraint.annotation.Constraint;

@Constraint(name = "NoSelfLoops", defaultBehavior = ConstraintBehavior.YES) public class NoSelfLoop
		extends PNConstraint {

	@Override public boolean checkEdit(ProbNet probNet, PNEdit edit)
			throws NonProjectablePotentialException, WrongCriterionException {
		List<PNEdit> edits = UtilConstraints.getSimpleEditsByType(edit, AddLinkEdit.class);
		for (PNEdit simpleEdit : edits) {
			Variable variable1 = ((AddLinkEdit) simpleEdit).getVariable1();
			Variable variable2 = ((AddLinkEdit) simpleEdit).getVariable2();
			if (variable1.equals(variable2)) {
				return false;
			}
		}
		return true;
	}

	@Override public boolean checkProbNet(ProbNet probNet) {
		for (Node node : probNet.getNodes()) {
			if (probNet.isChild(node, node) || probNet.isSibling(node, node)) {
				return false;
			}
		}
		return true;
	}

	@Override protected String getMessage() {
		return "no self loops allowed";
	}
}
