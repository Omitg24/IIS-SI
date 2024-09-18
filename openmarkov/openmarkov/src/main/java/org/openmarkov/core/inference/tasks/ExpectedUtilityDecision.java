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
import org.openmarkov.core.model.network.potential.TablePotential;

/**
 * @author jperez-martin
 * @author artasom
 */
public interface ExpectedUtilityDecision extends Task {

	TablePotential getExpectedUtility()
			throws UnexpectedInferenceException, NotEvaluableNetworkException, IncompatibleEvidenceException;

}