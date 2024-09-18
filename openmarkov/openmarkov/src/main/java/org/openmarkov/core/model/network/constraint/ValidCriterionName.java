/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.model.network.constraint;

import java.util.List;

import org.openmarkov.core.action.DecisionCriteriaEdit;
import org.openmarkov.core.action.PNEdit;
import org.openmarkov.core.exception.NonProjectablePotentialException;
import org.openmarkov.core.exception.WrongCriterionException;
import org.openmarkov.core.model.network.Criterion;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.constraint.annotation.Constraint;

@Constraint(name = "NoValidCriterionName", defaultBehavior = ConstraintBehavior.YES) public class ValidCriterionName
		extends PNConstraint {

	// Constants for possible errors
	private final int IS_EMPTY_NAME = 0;
	private final int IS_NAME_ALREADY_EXIST = 1;
	// Flag of the error
	private int type_error;

	@Override public boolean checkEdit(ProbNet probNet, PNEdit edit)
			throws NonProjectablePotentialException, WrongCriterionException {
		// DecisionCriteriaEdit
		List<PNEdit> edits = UtilConstraints.getSimpleEditsByType(edit, DecisionCriteriaEdit.class);
		for (PNEdit simpleEdit : edits) {
			String name = ((DecisionCriteriaEdit) simpleEdit).getNewName();

			if (name != null) {
				// Get the trim and lowerCase state
				name = name.trim();
				name = name.toLowerCase();
			}

			switch (((DecisionCriteriaEdit) simpleEdit).getStateAction()) {
			case ADD:
			case RENAME:
				if ((name == null) || (name.contentEquals(""))) {
					type_error = IS_EMPTY_NAME;
					return false;
				}

				for (Criterion criterion : ((DecisionCriteriaEdit) simpleEdit).getLastCriteria()) {
					if (criterion.getCriterionName().trim().toLowerCase().equals(name)) {
						type_error = IS_NAME_ALREADY_EXIST;
						return false;
					}
				}

				break;

			default:
				break;
			}
		}
		return true;
	}

	@Override public boolean checkProbNet(ProbNet probNet) {
		
		/*
		List<Criterion> criteria = probNet.getDecisionCriteria();
		
		for(int i = 0; i < criteria.size(); i++){
			Criterion criterion = criteria.get(i);
			
			if(criterion.getCriterionName().equals("")){
				type_error = IS_EMPTY_NAME;
				return false;
			} else {
				for(int j = i+1; j < criteria.size(); j++){
					if(criterion.getCriterionName().equals(criteria.get(j).getCriterionName())){
						type_error = IS_NAME_ALREADY_EXIST;
						return false;
					}
				}
			}
		}*/

		return true;
	}

	@Override protected String getMessage() {
		switch (type_error) {
		case IS_EMPTY_NAME:
			return "there should be no empty names";
		case IS_NAME_ALREADY_EXIST:
			return "There is already a criterion with that name in the net.";
		default:
			return "Unknown problem";

		}
	}

}
