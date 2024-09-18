/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.model.network.modelUncertainty;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DirichletFamily extends FamilyDistribution {
	public DirichletFamily(List<UncertainValue> uncertainValues) {
		super(filterByFunction(DirichletFunction.class, uncertainValues));
	}

	public DirichletFamily(double[] alphas) {
		family = new ArrayList<>();
		for (Double alpha : alphas) {
			family.add(new UncertainValue(new DirichletFunction(alpha)));
		}
	}

	public double[] getMean() {
		return Tools.normalize(super.getMean());
	}

	public double[] getSample(Random randomGenerator) {
		return Tools.normalize(super.getSample(randomGenerator));
	}

	@Override public double[] getVariance() {
		double[] variance;
		double sumAlpha;

		double[] alpha = super.getMean();
		sumAlpha = Tools.sum(alpha);
		variance = new double[alpha.length];
		for (int i = 0; i < alpha.length; i++) {
			double alphaI = alpha[i];
			variance[i] = alphaI * (sumAlpha - alphaI) / (Math.pow(sumAlpha, 2.0) * (sumAlpha + 1.0));
		}
		return variance;
	}
}
