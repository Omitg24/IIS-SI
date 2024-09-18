/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.inference.decompositionIntoSymmetricDANs.evaluation;

import org.openmarkov.core.exception.NotEvaluableNetworkException;
import org.openmarkov.core.model.network.EvidenceCase;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.inference.decompositionIntoSymmetricDANs.core.DANDecompositionIntoSymmetricDANsInference;

public class DANDecompositionIntoSymmetricDANsEvaluation extends DANEvaluation {

	public DANDecompositionIntoSymmetricDANsEvaluation(ProbNet network) throws NotEvaluableNetworkException {

		inferenceProcess = new DANDecompositionIntoSymmetricDANsInference(network, false);
	}

	public DANDecompositionIntoSymmetricDANsEvaluation(ProbNet probNet, EvidenceCase evidenceCase)
			throws NotEvaluableNetworkException {
		inferenceProcess = new DANDecompositionIntoSymmetricDANsInference(probNet, evidenceCase, false);

	}


}
