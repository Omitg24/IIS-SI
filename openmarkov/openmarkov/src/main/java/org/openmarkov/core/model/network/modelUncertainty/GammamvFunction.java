/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.model.network.modelUncertainty;

@ProbDensFunctionType(name = "Gamma-mv", isValidForProbabilities = false, parameters = { "mean",
		"standard deviation" }) public class GammamvFunction extends GammaAbstract {
	private double mu;
	private double sigma;

	public GammamvFunction() {
		this(0.0, 0.0);
	}

	public GammamvFunction(double mu, double sigma) {
		setParameters(new double[] { mu, sigma });
	}

	public GammamvFunction(GammamvFunction gammamvFunction) {
		super();
		this.kAbstract = gammamvFunction.kAbstract;
		this.thetaAbstract = gammamvFunction.thetaAbstract;
		this.mu = gammamvFunction.mu;
		this.sigma = gammamvFunction.sigma;
	}

	@Override public double[] getParameters() {
		return new double[] { mu, sigma };
	}

	@Override public void setParameters(double[] parameters) {
		mu = parameters[0];
		sigma = parameters[1];
		this.kAbstract = Math.pow(mu / sigma, 2);
		this.thetaAbstract = Math.pow(sigma, 2) / mu;
	}

	@Override public boolean verifyParametersDomain(boolean isChanceVariable) {
		return (mu > 0) && (sigma > 0);
	}

	@Override public ProbDensFunction copy() {
		return new GammamvFunction(this);
	}
}
