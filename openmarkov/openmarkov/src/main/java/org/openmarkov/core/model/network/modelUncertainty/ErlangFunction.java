/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.model.network.modelUncertainty;

import java.util.Random;

public class ErlangFunction extends ProbDensFunction {
	private int k;
	private double lambda;
	private ExponentialFunction exponentialFunction;

	/**
	 * @param k k
	 * @param lambda Lambda
	 */
	public ErlangFunction(int k, double lambda) {
		this.k = k;
		this.lambda = lambda;
		this.exponentialFunction = new ExponentialFunction(lambda);
	}

	public ErlangFunction() {
		this(0, 0.0);
	}

	public ErlangFunction(ErlangFunction erlangFunction) {
		super();
		this.k = erlangFunction.k;
		this.lambda = erlangFunction.lambda;
		if (erlangFunction.exponentialFunction != null) {
			this.exponentialFunction = (ExponentialFunction) erlangFunction.exponentialFunction.copy();
		}
	}

	@Override public double[] getParameters() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override public void setParameters(double[] args) {
		k = (int) Math.round(args[0]);
		lambda = args[1];
		exponentialFunction = new ExponentialFunction(lambda);
	}

	@Override public boolean verifyParametersDomain(boolean isChanceVariable) {
		return (k >= 0) && (lambda > 0);
	}

	@Override public double getMean() {
		return k / lambda;
	}

	@Override public double getMaximum() {
		return Double.POSITIVE_INFINITY;
	}

	@Override public double getSample(Random randomGenerator) {
		double sumSamples;
		sumSamples = 0.0;
		for (int i = 0; i < k; i++) {
			sumSamples = sumSamples + exponentialFunction.getSample(randomGenerator);
		}
		return sumSamples;
	}

	@Override public double getVariance() {
		return k / Math.pow(lambda, 2.0);
	}

	@Override public double getMinimum() {
		return 0;
	}

	@Override public DomainInterval getInterval(double p) {
		GammaFunction auxGamma = new GammaFunction(k, 1.0 / lambda);
		return auxGamma.getInterval(p);
	}

	@Override public ProbDensFunction copy() {
		return new ErlangFunction(this);
	}
}
