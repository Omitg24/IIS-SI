/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.inference.tasks;

import java.util.HashMap;
import java.util.List;

import org.openmarkov.core.exception.IncompatibleEvidenceException;
import org.openmarkov.core.exception.NotEvaluableNetworkException;
import org.openmarkov.core.exception.UnexpectedInferenceException;
import org.openmarkov.core.model.network.EvidenceCase;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.potential.TablePotential;

/**
 * @author jorgepmartin
 * @author artasom
 */
public interface Propagation extends Task {

	HashMap<Variable, TablePotential> getPosteriorValues()
			throws IncompatibleEvidenceException, UnexpectedInferenceException, NotEvaluableNetworkException;

	void setPostResolutionEvidence(EvidenceCase postResolutionEvidence);

	void setVariablesOfInterest(List<Variable> variablesOfInterest);

}