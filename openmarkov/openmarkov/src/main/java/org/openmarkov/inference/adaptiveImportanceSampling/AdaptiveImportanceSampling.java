/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */
package org.openmarkov.inference.adaptiveImportanceSampling;

import java.util.List;

import org.openmarkov.core.exception.NotEvaluableNetworkException;
import org.openmarkov.core.inference.InferenceAlgorithm;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.constraint.PNConstraint;
import org.openmarkov.core.model.network.type.NetworkType;

public class AdaptiveImportanceSampling extends InferenceAlgorithm {
	public AdaptiveImportanceSampling(ProbNet probNet) throws NotEvaluableNetworkException {
		super(probNet);
	}

	@Override protected List<NetworkType> getPossibleNetworkTypes() {
		return null;
	}

	@Override protected List<PNConstraint> getAdditionalConstraints() {
		return null;
	}
}
