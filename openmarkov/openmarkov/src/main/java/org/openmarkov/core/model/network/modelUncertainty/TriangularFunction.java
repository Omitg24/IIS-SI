/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.model.network.modelUncertainty;

@ProbDensFunctionType(name = "Triangular", parameters = { "minimum", "maximum",
		"mode" }) public class TriangularFunction extends ProbDensFunctionWithKnownInverseCDF {
	/**
	 * Minimum
	 */
	private double minimum;
	/**
	 * Maximum
	 */
	private double maximum;
	/**
	 * Mode
	 */
	private double mode;

	public TriangularFunction() {
		this(0.0, 1.0, 0.5);
	}

	public TriangularFunction(double minimum, double maximum, double mode) {
		this.minimum = minimum;
		this.maximum = maximum;
		this.mode = mode;
	}

	public TriangularFunction(TriangularFunction triangularFunction) {
		super();
		this.maximum = triangularFunction.maximum;
		this.minimum = triangularFunction.minimum;
		this.mode = triangularFunction.mode;
	}

	@Override public boolean verifyParametersDomain(boolean isChanceVariable) {
		return (
				((0 <= minimum) && (minimum <= mode) && (mode <= maximum) && (maximum <= 1) && (minimum < maximum))
						&& isChanceVariable
		) || ((minimum <= mode) && (mode <= maximum) && (minimum < maximum) && !isChanceVariable);
	}

	@Override public double[] getParameters() {
		return new double[] { minimum, maximum, mode };
	}

	@Override public void setParameters(double[] params) {
		minimum = params[0];
		maximum = params[1];
		mode = params[2];
	}

	@Override public double getMaximum() {
		return maximum;
	}

	@Override public double getMean() {
		return (minimum + maximum + mode) / 3;
	}

	@Override public double getInverseCumulativeDistributionFunction(double y) {
		double sample;
		double diffBA;
		double ratioCABA;
		diffBA = maximum - minimum;
		double diffBC = maximum - mode;
		double diffCA = mode - minimum;
		ratioCABA = diffCA / diffBA;
		// if (x<ratioCABA){
		if (y < ratioCABA) {
			// if (x<c){
			sample = minimum + Math.sqrt(y * diffBA * diffCA);
		} else {
			sample = maximum - Math.sqrt((1 - y) * diffBA * diffBC);
		}
		return sample;
	}

	@Override public double getVariance() {
		return (
				Tools.square(minimum) + Tools.square(maximum) + Tools.square(mode) - minimum * maximum - minimum * mode
						- maximum * mode
		) / 18;
	}

	@Override public double getMinimum() {
		return minimum;
	}

	@Override public ProbDensFunction copy() {
		return new TriangularFunction(this);
	}
}
