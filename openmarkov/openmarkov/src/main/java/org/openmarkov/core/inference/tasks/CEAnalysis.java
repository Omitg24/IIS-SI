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
import org.openmarkov.core.model.network.CEP;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.potential.GTablePotential;
import org.openmarkov.core.model.network.potential.TablePotential;

/**
 * @author jperez-martin
 */
public interface CEAnalysis extends Task {

	GTablePotential getUtility()
			throws UnexpectedInferenceException, NotEvaluableNetworkException, IncompatibleEvidenceException;

	TablePotential getProbability()
			throws UnexpectedInferenceException, NotEvaluableNetworkException, IncompatibleEvidenceException;

	void setDecisionVariable(Variable decisionVariable);

	CEP getCEP() throws NotEvaluableNetworkException, IncompatibleEvidenceException, UnexpectedInferenceException;
}
