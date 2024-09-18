/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.model.network.modelUncertainty;

@ProbDensFunctionType(name = "BetaAlphaNFunction", univariateName = "Beta", isValidForProbabilities = false, isValidForNumeric = false, parameters = {
		"alpha", "N" }) public class BetaAlphaNFunction extends BetaFunction {
	private double alpha;
	private double n;

	public BetaAlphaNFunction() {
		super();
		alpha = 1;
		setN(1);
	}

	public BetaAlphaNFunction(double alpha, double n) {
		super(alpha, n - alpha);
		this.alpha = alpha;
		this.setN(n);
		verifyParameters(new double[] { 1, 2 });
	}

	public BetaAlphaNFunction(BetaAlphaNFunction betaFunction) {
		this(betaFunction.getAlpha(), betaFunction.getN());
	}

	/**
	 *
	 */
	@Override public void verifyParameters(double[] parameters) throws IllegalArgumentException {
		if ((parameters[0] > 0) && (parameters[1] > 0) && (parameters[1] > parameters[0])) {
			throw new IllegalArgumentException("N should be greater than alpha " + this.getClass().getName());
		}
	}

	@Override public double[] getParameters() {
		double[] a = new double[2];
		a[0] = alpha;
		a[1] = n;
		return a;
	}

	@Override public void setParameters(double[] params) {
		alpha = params[0];
		n = params[1];

		super.setParameters(new double[] { alpha, n - alpha });
	}

	/**
	 * @return the n
	 */
	public double getN() {
		return n;
	}

	/**
	 * @param n the n to set
	 */
	public void setN(double n) {
		this.n = n;
	}

	@Override public ProbDensFunction copy() {
		return new BetaAlphaNFunction(this);
	}

}
