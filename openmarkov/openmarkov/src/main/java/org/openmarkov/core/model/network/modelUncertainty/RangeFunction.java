/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.model.network.modelUncertainty;

@ProbDensFunctionType(name = "Range", parameters = { "lower bound", "upper bound" }) public class RangeFunction
		extends ProbDensFunctionWithKnownInverseCDF {
	private double lowerBound;
	private double upperBound;

	public RangeFunction() {
		this(0.0, 1.0);
	}

	/**
	 * @param lowerBound Lower bound
	 * @param upperBound Upper bound
	 */
	public RangeFunction(double lowerBound, double upperBound) {
		this.lowerBound = lowerBound;
		this.upperBound = upperBound;
	}

	public RangeFunction(RangeFunction rangeFunction) {
		super();
		this.lowerBound = rangeFunction.lowerBound;
		this.upperBound = rangeFunction.upperBound;
	}

	@Override public boolean verifyParametersDomain(boolean isChanceVariable) {
		return ((0 <= lowerBound) && (lowerBound < upperBound) && (upperBound <= 1) && isChanceVariable) || (
				(lowerBound < upperBound) && !isChanceVariable
		);
	}

	@Override public double[] getParameters() {
		return new double[] { lowerBound, upperBound };
	}

	@Override public void setParameters(double[] params) {
		lowerBound = params[0];
		upperBound = params[1];
	}

	@Override public double getMaximum() {
		return upperBound;
	}

	@Override public double getMean() {
		return (lowerBound + upperBound) / 2;
	}

	@Override public double getInverseCumulativeDistributionFunction(double y) {
		return lowerBound + (upperBound - lowerBound) * y;
	}

	@Override public double getVariance() {
		return Math.pow(upperBound - lowerBound, 2.0) / 12;
	}

	@Override public double getMinimum() {
		return lowerBound;
	}

	@Override public ProbDensFunction copy() {
		return new RangeFunction(this);
	}
}
