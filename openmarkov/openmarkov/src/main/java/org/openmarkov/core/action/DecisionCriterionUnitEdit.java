/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.action;

import org.openmarkov.core.exception.DoEditException;
import org.openmarkov.core.model.network.Criterion;
import org.openmarkov.core.model.network.ProbNet;

/**
 * {@code DecisionCriterionUnitEdit} is a simple edit that allow modify the unit
 * of a criterion
 *
 * @author Jorge
 */
public class DecisionCriterionUnitEdit extends SimplePNEdit {

	/**
	 * Default serial version uid
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * New unit name
	 */
	private String newUnit;
	/**
	 * Old unit name
	 */
	private String oldUnit;
	/**
	 * Criterion to be modified
	 */
	private String criterionName;
	/**
	 * Criterion in the net
	 */
	private Criterion criterion;

	public DecisionCriterionUnitEdit(ProbNet probnet, String criterionName, String newUnit) {
		super(probnet);
		this.criterionName = criterionName;
		this.newUnit = newUnit;

		// Search the criterion in where we want set the new unit of measure
		for (Criterion criterion : probnet.getDecisionCriteria()) {
			if (criterion.getCriterionName() != null && criterion.getCriterionName().equals(criterionName)) {
				this.criterion = criterion;
				break;
			}
		}

		this.oldUnit = this.criterion.getCriterionUnit();
	}

	@Override public void doEdit() throws DoEditException {
		this.criterion.setCriterionUnit(newUnit);
	}

	@Override public void undo() {
		super.undo();
		this.criterion.setCriterionUnit(oldUnit);
	}
}


