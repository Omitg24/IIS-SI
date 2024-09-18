/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.inference.tasks;

import java.util.HashMap;

import org.openmarkov.core.exception.IncompatibleEvidenceException;
import org.openmarkov.core.exception.NotEvaluableNetworkException;
import org.openmarkov.core.exception.UnexpectedInferenceException;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.potential.Potential;

/**
 * @author jperez-martin
 * @author artasom
 */
public interface OptimalPolicies extends Task {

	HashMap<Variable, Potential> getOptimalPolicies()
			throws UnexpectedInferenceException, NotEvaluableNetworkException, IncompatibleEvidenceException;

	Potential getOptimalPolicy(Variable decision)
			throws UnexpectedInferenceException, NotEvaluableNetworkException, IncompatibleEvidenceException;

}
