
/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.action;

import java.util.ArrayList;
import java.util.List;

import org.openmarkov.core.exception.DoEditException;
import org.openmarkov.core.model.network.Criterion;
import org.openmarkov.core.model.network.ProbNet;

@SuppressWarnings("serial") public class DecisionCriteriaEdit extends SimplePNEdit {
	private StateAction stateAction;
	private List<Criterion> lastCriteria;
	private Criterion modifiedCriterion;
	private String newName;

	public DecisionCriteriaEdit(ProbNet probnet, StateAction stateAction, Criterion modifiedCriterion, String newName) {
		super(probnet);
		this.modifiedCriterion = modifiedCriterion;

		if (stateAction.equals(StateAction.ADD)) {
			this.newName = modifiedCriterion.getCriterionName();
		} else if (stateAction.equals(StateAction.RENAME)) {
			this.newName = newName;
		}
		this.stateAction = stateAction;
		this.lastCriteria = new ArrayList<>(probnet.getDecisionCriteria());
	}

	@Override public void doEdit() throws DoEditException {
		List<Criterion> criteria = probNet.getDecisionCriteria();
		switch (stateAction) {
		case ADD:
			criteria.add(modifiedCriterion);
			break;
		case REMOVE:

			criteria.remove(modifiedCriterion);

			if (criteria.size() == 0) {
				criteria = null;
			}
			break;
		case DOWN:

			int criterionIndex = criteria.indexOf(modifiedCriterion);
			Criterion swapDown = criteria.get(criterionIndex);
			criteria.set(criterionIndex, criteria.get(criterionIndex + 1));
			criteria.set(criterionIndex + 1, swapDown);

			break;
		case UP:
			criterionIndex = criteria.indexOf(modifiedCriterion);

			Criterion swapUp = criteria.get(criterionIndex);
			criteria.set(criterionIndex, criteria.get(criterionIndex - 1));
			criteria.set(criterionIndex - 1, swapUp);

			break;
		case RENAME:
			String oldName = modifiedCriterion.getCriterionName();
			modifiedCriterion.setCriterionName(newName);
			break;
		default:
			break;
		}
	}

	public String getNewName() {
		return newName;
	}

	public StateAction getStateAction() {
		return stateAction;
	}

	public List<Criterion> getLastCriteria() {
		return lastCriteria;
	}

	@Override public void undo() {
		super.undo();
		probNet.setDecisionCriteria(lastCriteria);

	}
}
