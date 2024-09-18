/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.model.network.constraint;

import java.util.List;

import org.openmarkov.core.action.AddNodeEdit;
import org.openmarkov.core.action.NodeNameEdit;
import org.openmarkov.core.action.PNEdit;
import org.openmarkov.core.exception.NonProjectablePotentialException;
import org.openmarkov.core.exception.WrongCriterionException;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.constraint.annotation.Constraint;

@Constraint(name = "NoEmptyName", defaultBehavior = ConstraintBehavior.YES) public class NoEmptyName
		extends PNConstraint {

	@Override public boolean checkEdit(ProbNet probNet, PNEdit edit)
			throws NonProjectablePotentialException, WrongCriterionException {
		// AddVariableEdit
		List<PNEdit> edits = UtilConstraints.getSimpleEditsByType(edit, AddNodeEdit.class);
		for (PNEdit simpleEdit : edits) {
			String name = ((AddNodeEdit) simpleEdit).getVariable().getName();
			if ((name == null) || (name.contentEquals(""))) {
				return false;
			}
		}
		// NodeNameEdit
		edits = UtilConstraints.getSimpleEditsByType(edit, NodeNameEdit.class);
		for (PNEdit simpleEdit : edits) {
			String name = ((NodeNameEdit) simpleEdit).getNewName();
			if ((name == null) || (name.contentEquals(""))) {
				return false;
			}
		}
		return true;
	}

	@Override public boolean checkProbNet(ProbNet probNet) {
		List<Variable> variables = probNet.getVariables();
		for (Variable variable : variables) {
			String name = variable.getName();
			if ((name == null) || (name.contentEquals(""))) {
				return false;
			}
		}
		return true;
	}

	@Override protected String getMessage() {
		return "there should be no empty names";
	}

}
