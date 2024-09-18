/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.model.network.constraint;

import java.util.List;

import org.openmarkov.core.action.NodeStateEdit;
import org.openmarkov.core.action.PNEdit;
import org.openmarkov.core.action.StateAction;
import org.openmarkov.core.exception.NonProjectablePotentialException;
import org.openmarkov.core.exception.OpenMarkovExceptionConstants;
import org.openmarkov.core.exception.WrongCriterionException;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.State;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.constraint.annotation.Constraint;

@Constraint(name = "NoValidStateName", defaultBehavior = ConstraintBehavior.YES) public class ValidStateName
		extends PNConstraint {

	// Constants for possible errors
	private final int IS_EMPTY_NAME = 0;
	private final int IS_NAME_ALREADY_EXIST = 1;
	// Flag of the error
	private int type_error;

	@Override
	public boolean checkEdit(ProbNet probNet, PNEdit edit)
			throws NonProjectablePotentialException, WrongCriterionException {
		// NodeStateEdit
		List<PNEdit> edits = UtilConstraints.getSimpleEditsByType(edit,
				NodeStateEdit.class);
		for (PNEdit simpleEdit : edits) {
			NodeStateEdit nodeStateEdit = (NodeStateEdit) simpleEdit;
			StateAction stateAction = nodeStateEdit.getStateAction();
			String name = (stateAction != StateAction.RENAME) ? nodeStateEdit
					.getNewState().getName() : nodeStateEdit.getNewName();

			// Get the trim and lowerCase state


			switch (stateAction) {
			case ADD:
				String trimmedLowerName;
				trimmedLowerName = name.trim();
				trimmedLowerName = name.toLowerCase();

				if ((trimmedLowerName == null) || (trimmedLowerName.contentEquals(""))) {
					type_error = IS_EMPTY_NAME;
					return false;
				}
				if (!nodeStateEdit.getNode().getVariable().chekNewStateName(trimmedLowerName)) {
					type_error = IS_NAME_ALREADY_EXIST;
					return false;
				}
				break;
			case RENAME:
				String trimmedName;
				trimmedName = name.trim();

				if ((trimmedName == null) || (trimmedName.contentEquals(""))) {
					type_error = IS_EMPTY_NAME;
					return false;
				}
				if (!nodeStateEdit.getNode().getVariable().chekNewStateName(trimmedName)) {
					type_error = IS_NAME_ALREADY_EXIST;
					return false;
				}
				break;
			default:
				break;
			}
		}
		return true;
	}

	@Override public boolean checkProbNet(ProbNet probNet) {
		List<Variable> variables = probNet.getVariables();
		for (Variable variable : variables) {
			State[] states = variable.getStates();
			for (State state : states) {
				String name = state.getName();
				if ((name == null) || (name.contentEquals(""))) {
					type_error = IS_EMPTY_NAME;
					return false;
				} else if (!variable.chekNewStateName(name)) {
					type_error = IS_NAME_ALREADY_EXIST;
					return false;
				}
			}

		}
		return true;
	}

	@Override protected String getMessage() {
		switch (type_error) {
		case IS_EMPTY_NAME:
			return OpenMarkovExceptionConstants.InvalidStateNameEmptyException;
		case IS_NAME_ALREADY_EXIST:
			return OpenMarkovExceptionConstants.InvalidStateNameDuplicatedException;
		default:
			return OpenMarkovExceptionConstants.GenericException;

		}
	}

}
