/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.model.network.modelUncertainty;

import java.util.Random;

@ProbDensFunctionType(name = "Dirichlet", isValidForNumeric = false, parameters = {
		"alpha" }) public class DirichletFunction extends ProbDensFunction {
	private double alpha;

	private GammaFunction gammaForSampling;

	public DirichletFunction() {
		this.alpha = 0;
		initializePdfForSampling();
	}

	public DirichletFunction(double alpha) {
		this.alpha = alpha;
		initializePdfForSampling();
	}

	public DirichletFunction(DirichletFunction dirichletFunction) {
		super();
		this.alpha = dirichletFunction.alpha;
		if (dirichletFunction.gammaForSampling != null) {
			this.gammaForSampling = (GammaFunction) dirichletFunction.gammaForSampling.copy();
		}
	}

	@Override public boolean verifyParametersDomain(boolean isChanceVariable) {
		return (alpha > 0);
	}

	@Override public double[] getParameters() {
		double[] a = new double[1];
		a[0] = alpha;
		return a;
	}

	@Override public void setParameters(double[] params) {
		alpha = params[0];
		initializePdfForSampling();
	}

	@Override public double getMaximum() {
		return 1;
	}

	@Override public double getMean() {
		return alpha;
	}

	@Override public double getSample(Random randomGenerator) {
		return gammaForSampling.getSample(randomGenerator);
	}

	@Override public double getVariance() {
		return 0;
	}

	/**
	 * Returns the alpha.
	 *
	 * @return the alpha.
	 */
	public double getAlpha() {
		return alpha;
	}

	/**
	 * Sets the alpha.
	 *
	 * @param alpha the alpha to set.
	 */
	public void setAlpha(double alpha) {
		this.alpha = alpha;
		initializePdfForSampling();
	}

	@Override public double getMinimum() {
		return 0;
	}

	@Override public DomainInterval getInterval(double p) {
		return null;
	}

	private void initializePdfForSampling() {
		gammaForSampling = new GammaFunction(alpha, 1.0);
	}

	@Override public ProbDensFunction copy() {
		return new DirichletFunction(this);
	}
}
