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
import org.openmarkov.core.model.network.constraint.annotation.Constraint;

/**
 * When a network is first time created if it is multiagent then would be initialize
 * with two agents Agent 1 and Agent 2 that could be modified by the user, if the network
 * is not multiagent then no agents would have been created.
 *
 * @author myebra
 */
@Constraint(name = "OnlyOneAgent", defaultBehavior = ConstraintBehavior.YES) public class OnlyOneAgent
		extends PNConstraint {

	@Override public boolean checkProbNet(ProbNet probNet) {

		List<Variable> variables = probNet.getVariables();
		for (Variable variable : variables) {
			if (variable.getAgent() != null) {
				return false;
			}
		}
		if (probNet.getAgents() != null) {

			return false;
		}

		return true;
	}

	@Override public boolean checkEdit(ProbNet probNet, PNEdit edit)
			throws NonProjectablePotentialException, WrongCriterionException {

		List<PNEdit> edits = UtilConstraints.getSimpleEditsByType(edit, AddNodeEdit.class);
		for (PNEdit simpleEdit : edits) {
			Variable variable = ((AddNodeEdit) simpleEdit).getVariable();
			if (variable.getAgent() != null) {
				return false;
			}
		}
		return true;
	}

	@Override protected String getMessage() {
		// TODO Auto-generated method stub
		return "There can be no more than one agent.";
	}

}
