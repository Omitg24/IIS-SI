/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.model.network.constraint;

import java.util.ArrayList;
import java.util.List;

import org.openmarkov.core.action.AddNodeEdit;
import org.openmarkov.core.action.NodeNameEdit;
import org.openmarkov.core.action.PNEdit;
import org.openmarkov.core.action.TimeSliceEdit;
import org.openmarkov.core.exception.NonProjectablePotentialException;
import org.openmarkov.core.exception.OpenMarkovExceptionConstants;
import org.openmarkov.core.exception.WrongCriterionException;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.constraint.annotation.Constraint;

@Constraint(name = "DistinctVariableNames", defaultBehavior = ConstraintBehavior.YES) public class DistinctVariableNames
		extends PNConstraint {

	// Constants for possible errors
	private final int IS_SAME_NAME = 0;
	private final int IS_SAME_TIME_SLICE = 1;
	// Flag of the error
	private int typeError;

	@Override public boolean checkEdit(ProbNet probNet, PNEdit edit)
			throws NonProjectablePotentialException, WrongCriterionException {
		List<PNEdit> edits = UtilConstraints.getSimpleEditsByType(edit, AddNodeEdit.class);
		List<Variable> variablesProbNet = probNet.getVariables();
		List<String> variablesProbNetNames = new ArrayList<>();
		for (Variable variable : variablesProbNet) {
			variablesProbNetNames.add(variable.getName());
		}

		// get new variables names
		List<String> newVariablesNames = new ArrayList<>();
		for (PNEdit simpleEdit : edits) {
			newVariablesNames.add(((AddNodeEdit) simpleEdit).getVariable().getName());
		}

		// check that new variables have distinct names
		int numNewVariables = newVariablesNames.size();
		for (int i = 0; i < numNewVariables - 1; i++) {
			for (int j = i + 1; j < numNewVariables; j++) {
				if (newVariablesNames.get(i).compareTo(newVariablesNames.get(j)) == 0) {
					typeError = IS_SAME_NAME;
					return false;
				}
			}
		}

		// check that new variables names are distinct than probNet variables
		// names
		for (String newVariableName : newVariablesNames) {
			for (String variableProbNetName : variablesProbNetNames) {
				if (variableProbNetName.compareTo(newVariableName) == 0) {
					typeError = IS_SAME_NAME;
					return false;
				}
			}
		}

		// NodeNameEdit
		edits = UtilConstraints.getSimpleEditsByType(edit, NodeNameEdit.class);
		for (PNEdit simpleEdit : edits) {
			String newName = ((NodeNameEdit) simpleEdit).getNewName();
			for (String variableProbNetName : variablesProbNetNames) {
				if ((newName.contentEquals(variableProbNetName))) {
					typeError = IS_SAME_NAME;
					return false;
				}
			}
		}

        /*
        Fixing issue 203
        https://bitbucket.org/cisiad/org.openmarkov.issues/issue/203/two-time-related-variables-with-identical
        We have to check prevent two variables of having the same time slice
        */
		// TimeSliceEdit
		edits = UtilConstraints.getSimpleEditsByType(edit, TimeSliceEdit.class);
		for (PNEdit simpleEdit : edits) {
			// We gent the new name
			String newName = ((TimeSliceEdit) simpleEdit).getNewName();
			// We remove the previous name of the variable from the forbidden names,
			// just in case the user wants to reselect it
			variablesProbNetNames.remove(((TimeSliceEdit) simpleEdit).getPreviousName());
			// We go through the remaining variables names
			for (String variableProbNetName : variablesProbNetNames) {
				// and if we find a match, it means there is another variable
				// in the same time slice
				if ((newName.contentEquals(variableProbNetName))) {
					typeError = IS_SAME_TIME_SLICE;
					return false;
				}
			}
		}

		return true;
	}

	@Override public boolean checkProbNet(ProbNet probNet) {
		List<Variable> variablesProbNet = probNet.getVariables();
		List<String> variablesProbNetNames = new ArrayList<>();
		for (Variable variable : variablesProbNet) {
			variablesProbNetNames.add(variable.getName());
		}

		// check that new variables have distinct names
		int numVariables = variablesProbNetNames.size();
		for (int i = 0; i < numVariables - 1; i++) {
			for (int j = i + 1; j < numVariables; j++) {
				if (variablesProbNetNames.get(i).compareTo(variablesProbNetNames.get(j)) == 0) {
					typeError = IS_SAME_NAME;
					return false;
				}
			}
		}

		return true;
	}

	@Override protected String getMessage() {
		switch (typeError) {
		case IS_SAME_NAME:
			return OpenMarkovExceptionConstants.InvalidVariableNameExistingException;
		case IS_SAME_TIME_SLICE:
			return OpenMarkovExceptionConstants.InvalidVariableNameExistingTimeSliceException;
		default:
			return OpenMarkovExceptionConstants.GenericException;
		}
	}

}
