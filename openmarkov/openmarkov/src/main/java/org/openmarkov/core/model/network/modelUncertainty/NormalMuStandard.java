/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.model.network.modelUncertainty;

@ProbDensFunctionType(name = "NormalMuStandardFunction", univariateName = "Normal", isValidForProbabilities = false, isValidForNumeric = false, parameters = {
		"mu", "standard" }) public class NormalMuStandard extends NormalFunction {
	private double mu;
	private double standard;

	public NormalMuStandard() {
		super();
		setMu(0);
		setStandard(1);
	}

	public NormalMuStandard(double mu, double standard) {
		super(mu, standard * standard);
		this.setMu(mu);
		this.setStandard(standard);
	}

	public NormalMuStandard(NormalMuStandard normalMuStandardFunction) {
		this(normalMuStandardFunction.getMu(), normalMuStandardFunction.getStandard());
	}

	//CMI
	//For Univariate

	/**
	 * @param parameters - parameters[1]= mu and parameters[0] = standard deviation
	 * @throws IllegalArgumentException - thrown if standard&#60;0
	 */
	@Override public void verifyParameters(double[] parameters) {
		if (!(parameters[0] > 0)) {
			throw new IllegalArgumentException("Wrong parameters" + this.getClass().getName());
		}
	}
	//CMF

	@Override public double[] getParameters() {
		double[] a = new double[2];
		a[0] = getMu();
		a[1] = getStandard();
		return a;
	}

	public double getMu() {
		return mu;
	}

	public void setMu(double mu) {
		this.mu = mu;
	}

	public double getStandard() {
		return standard;
	}

	public void setStandard(double standard) {
		this.standard = standard;
	}

	@Override public ProbDensFunction copy() {
		return new NormalMuStandard(this);
	}

}
