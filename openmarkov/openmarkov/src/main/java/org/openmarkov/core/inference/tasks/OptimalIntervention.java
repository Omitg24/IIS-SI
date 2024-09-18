/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.inference.tasks;

import org.openmarkov.core.exception.IncompatibleEvidenceException;
import org.openmarkov.core.exception.NotEvaluableNetworkException;
import org.openmarkov.core.exception.UnexpectedInferenceException;
import org.openmarkov.core.model.network.potential.StrategyTree;

/**
 * @author jperez-martin
 * @author artasom
 */
public interface OptimalIntervention extends Task {

	/**
	 * @return The optimal intervention
	 * @throws IncompatibleEvidenceException IncompatibleEvidenceException
	 * @throws UnexpectedInferenceException UnexpectedInferenceException
	 * @throws NotEvaluableNetworkException NotEvaluableNetworkException
	 */
	StrategyTree getOptimalIntervention()
			throws IncompatibleEvidenceException, UnexpectedInferenceException, NotEvaluableNetworkException;

}