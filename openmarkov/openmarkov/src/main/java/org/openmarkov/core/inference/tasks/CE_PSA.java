/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.inference.tasks;

import java.util.Collection;

import org.openmarkov.core.exception.IncompatibleEvidenceException;
import org.openmarkov.core.exception.NotEvaluableNetworkException;
import org.openmarkov.core.exception.UnexpectedInferenceException;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.potential.GTablePotential;

/**
 * @author jperez-martin
 */
public interface CE_PSA extends Task {

	Collection<GTablePotential> getCEPPotentials()
			throws NotEvaluableNetworkException, IncompatibleEvidenceException, UnexpectedInferenceException;

	void setDecisionVariable(Variable decisionSelected);
}
