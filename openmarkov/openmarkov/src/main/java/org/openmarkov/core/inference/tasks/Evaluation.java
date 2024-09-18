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
import org.openmarkov.core.model.network.potential.TablePotential;

/**
 * @author jperez-martin
 * @author artasom
 */
public interface Evaluation extends Task {

	/**
	 * @return The global expected utility
	 * @throws UnexpectedInferenceException UnexpectedInferenceException
	 * @throws IncompatibleEvidenceException IncompatibleEvidenceException
	 * @throws NotEvaluableNetworkException  defined over the conditioning variables.
	 */
	TablePotential getProbability()
			throws IncompatibleEvidenceException, UnexpectedInferenceException, NotEvaluableNetworkException;

	/**
	 * @return The global expected utility
	 * defined over the conditioning variables.
	 * @throws UnexpectedInferenceException UnexpectedInferenceException
	 * @throws IncompatibleEvidenceException IncompatibleEvidenceException
	 * @throws NotEvaluableNetworkException NotEvaluableNetworkException
	 */
	TablePotential getUtility()
			throws UnexpectedInferenceException, IncompatibleEvidenceException, NotEvaluableNetworkException;

	/**
	 * @return The optimal strategy tree
	 * @throws UnexpectedInferenceException UnexpectedInferenceException
	 * @throws IncompatibleEvidenceException IncompatibleEvidenceException
	 * @throws NotEvaluableNetworkException NotEvaluableNetworkException
	 */
	StrategyTree getOptimalStrategyTree()
			throws UnexpectedInferenceException, IncompatibleEvidenceException, NotEvaluableNetworkException;

}