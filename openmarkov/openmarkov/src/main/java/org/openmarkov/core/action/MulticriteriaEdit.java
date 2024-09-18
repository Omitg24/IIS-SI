/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.action;

import java.util.ArrayList;
import java.util.List;

import javax.swing.undo.CannotUndoException;

import org.openmarkov.core.exception.DoEditException;
import org.openmarkov.core.inference.MulticriteriaOptions;
import org.openmarkov.core.model.network.Criterion;
import org.openmarkov.core.model.network.ProbNet;

public class MulticriteriaEdit extends SimplePNEdit {

	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 1166925687725877620L;
	private List<Criterion> oldDecisionCriteria;
	private List<Criterion> newDecisionCriteria;
	private MulticriteriaOptions oldMulticriteriaOptions;
	private MulticriteriaOptions newMulticriteriaOptions;

	public MulticriteriaEdit(ProbNet probNet, List<Criterion> decisionCriteria, MulticriteriaOptions options) {
		super(probNet);
		if (probNet.getDecisionCriteria() != null && !probNet.getDecisionCriteria().isEmpty()) {
			this.oldDecisionCriteria = new ArrayList<>();
			for (Criterion criterion : probNet.getDecisionCriteria()) {
				this.oldDecisionCriteria.add(criterion.clone());
			}
		}
		this.oldMulticriteriaOptions = probNet.getInferenceOptions().getMultiCriteriaOptions().clone();
		this.newDecisionCriteria = decisionCriteria;
		this.newMulticriteriaOptions = options;
	}

	@Override public void doEdit() throws DoEditException {
		//probNet.setDecisionCriteria(this.newDecisionCriteria);
		// Set the new data at the probNet criteria 
		for (Criterion oldCriterion : probNet.getDecisionCriteria()) {
			for (Criterion newCriterion : this.newDecisionCriteria) {
				if (oldCriterion.getCriterionName().equals(newCriterion.getCriterionName())) {
					oldCriterion.copy(newCriterion);
				}
			}
		}

		probNet.getInferenceOptions().setMultiCriteriaOptions(this.newMulticriteriaOptions);

	}

	@Override public void undo() throws CannotUndoException {
		super.undo();
		//probNet.setDecisionCriteria(oldDecisionCriteria);
		for (Criterion oldCriterion : probNet.getDecisionCriteria()) {
			for (Criterion newCriterion : this.oldDecisionCriteria) {
				if (oldCriterion.getCriterionName().equals(newCriterion.getCriterionName())) {
					oldCriterion.copy(newCriterion);
				}
			}
		}
		probNet.getInferenceOptions().setMultiCriteriaOptions(this.oldMulticriteriaOptions);
	}

	@Override public void redo() {
		super.redo();
		try {
			doEdit();
		} catch (DoEditException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
