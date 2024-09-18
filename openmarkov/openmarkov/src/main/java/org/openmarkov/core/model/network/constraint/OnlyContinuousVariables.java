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
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.VariableType;
import org.openmarkov.core.model.network.constraint.annotation.Constraint;

/**
 * Only continuous variables constraint.
 *
 * @author mpalacios based in OnlyDiscreteVariables class.
 * @version 1.0
 */

@Constraint(name = "OnlyContinuousVariables", defaultBehavior = ConstraintBehavior.OPTIONAL) public class OnlyContinuousVariables
		extends PNConstraint {

	@Override public boolean checkEdit(ProbNet probNet, PNEdit edit)
			throws NonProjectablePotentialException, WrongCriterionException {
		List<PNEdit> edits = UtilConstraints.getSimpleEditsByType(edit, AddNodeEdit.class);
		for (PNEdit simpleEdit : edits) {
			Variable variable = ((AddNodeEdit) simpleEdit).getVariable();
			if (variable.getVariableType() != VariableType.NUMERIC) {
				return false;
			}
		}
		return true;
	}

	@Override public boolean checkProbNet(ProbNet probNet) {
		List<Variable> variables = probNet.getVariables();
		for (Variable variable : variables) {
			if (variable.getVariableType() != VariableType.NUMERIC) {
				return false;
			}
		}
		return true;
	}

	@Override protected String getMessage() {
		return "all variables must be continuous";
	}

}
