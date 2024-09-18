/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.model.network.modelUncertainty;

import java.util.Random;

import org.apache.commons.math3.distribution.GammaDistribution;

import cern.jet.random.Gamma;

public abstract class GammaAbstract extends ProbDensFunction {
	protected double kAbstract;
	protected double thetaAbstract;

	public final double getMaximum() {
		return Double.POSITIVE_INFINITY;
	}

	@Override public final double getMinimum() {
		return 0.0;
	}

	@Override public final double getMean() {
		return (kAbstract * thetaAbstract);
	}

	@Override public final double getSample(Random randomGenerator) {
		return Gamma.staticNextDouble(kAbstract, 1.0 / thetaAbstract);
	}

	public boolean isAnErlangFunction(double epsilon) {
		return (Math.abs(kAbstract - Math.ceil(kAbstract))) < epsilon;
	}

	@Override public final double getVariance() {
		return kAbstract * Math.pow(thetaAbstract, 2.0);
	}

	@Override public DomainInterval getInterval(double p) {
		GammaDistribution auxGammaDist = new GammaDistribution(kAbstract, thetaAbstract);
		double halfP = p / 2;
		return new DomainInterval(auxGammaDist.inverseCumulativeProbability(0.5 - halfP),
				auxGammaDist.inverseCumulativeProbability(0.5 + halfP));
	}

}
