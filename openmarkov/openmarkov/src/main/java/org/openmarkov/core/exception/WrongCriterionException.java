/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.exception;

import org.openmarkov.core.model.network.Variable;

@SuppressWarnings("serial") public class WrongCriterionException extends OpenMarkovException {

	public WrongCriterionException(Variable utilityVariable, String criterion, Variable decisionCriteria) {
		super("The criterion for the utility variable " + utilityVariable.getName() + ", which is " + criterion
				+ ", does not match any of the values of the decisionCriteria" + "variable.");
	}

}
