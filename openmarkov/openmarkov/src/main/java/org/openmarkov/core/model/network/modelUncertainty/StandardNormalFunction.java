/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.model.network.modelUncertainty;

public class StandardNormalFunction extends ProbDensFunctionWithKnownInverseCDF {
	// Odeh and Evans' coefficients
	private static final double p[] = new double[] { -0.322232431088, -1.0, -0.342242088547, -0.0204231210245,
			-0.453642210148E-4 };
	private static final double q[] = new double[] { 0.0993484626060, 0.588581570495, 0.531103462366, 0.103537752850,
			0.38560700634E-2 };
	// Polynomials for the approximation
	private Polynomial numerator = new Polynomial(p, 4);
	private Polynomial denominator = new Polynomial(q, 4);

	public StandardNormalFunction() {
	}

	public StandardNormalFunction(StandardNormalFunction standardNormalFunction) {
		super();

		if (standardNormalFunction.numerator != null) {
			this.numerator = new Polynomial(standardNormalFunction.numerator.coeff.clone(),
					standardNormalFunction.numerator.deg);
		}

		if (standardNormalFunction.denominator != null) {
			this.denominator = new Polynomial(standardNormalFunction.denominator.coeff.clone(),
					standardNormalFunction.denominator.deg);
		}

	}

	@Override public double[] getParameters() {
		return new double[0];
	}

	@Override public void setParameters(double[] args) {
		// ignore
	}

	@Override public boolean verifyParametersDomain(boolean isChanceVariable) {
		return true;
	}

	@Override public double getMean() {
		return 0;
	}

	@Override public double getMaximum() {
		return Double.POSITIVE_INFINITY;
	}

	/**
	 * We use the method proposed by Odeh and Evan (1974). It is an
	 * approximation for the inverse of the cumulative distribution function of
	 * the standard normal.
	 *
	 * @param beta beta
	 * @return Inverse cumulative distribution function of the standard normal
	 */
	@Override public double getInverseCumulativeDistributionFunction(double beta) {
		double inverse;
		if (beta < 0.5) {
			inverse = odehAndEvansApproximation(beta);
		} else {
			inverse = -odehAndEvansApproximation(1 - beta);
		}
		return inverse;
	}

	private double odehAndEvansApproximation(double beta) {
		double y = Math.sqrt(-2.0 * Math.log(1.0 - beta));
		return -y - numerator.evaluate(y) / denominator.evaluate(y);
	}

	@Override public double getVariance() {
		return 1.0;
	}

	@Override public double getMinimum() {
		return Double.NEGATIVE_INFINITY;
	}

	@Override public DomainInterval getInterval(double p) {
		double halfP = p / 2.0;
		double value = Math.abs(getInverseCumulativeDistributionFunction(0.5 - halfP));
		return new DomainInterval(-value, value);
	}

	@Override public ProbDensFunction copy() {
		return new StandardNormalFunction(this);
	}

	public class Polynomial {
		// Coefficients
		double[] coeff;
		// Degree
		int deg;

		public Polynomial(double[] p, int i) {
			coeff = p;
			deg = i;
		}

		public double evaluate(double x) {
			double p = 0;
			for (int i = deg; i >= 0; i--)
				p = coeff[i] + (x * p);
			return p;
		}
	}
}
