/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.model.network.modelUncertainty;

import java.util.Random;

public abstract class ProbDensFunction {
	public abstract double[] getParameters();

	public abstract void setParameters(double[] args);

	//CMI
	//For Univariate
	public void verifyParameters(double[] parameters) throws IllegalArgumentException {
		throw new IllegalArgumentException("verifyParameters not implemented in " + this.getClass().getName());
	}
	//CMF

	public abstract boolean verifyParametersDomain(boolean isChanceVariable);

	public abstract double getMean();

	public final double getStandardDeviation() {
		return Math.sqrt(getVariance());
	}

	public abstract double getVariance();

	public abstract double getMaximum();

	public abstract double getMinimum();

	public abstract double getSample(Random randomGenerator);

	@Override public String toString() {
		StringBuilder sb = new StringBuilder();
		ProbDensFunctionType probDensAnnotation = getClass().getAnnotation(ProbDensFunctionType.class);
		if (probDensAnnotation != null) {
			sb.append(probDensAnnotation.name());
			sb.append(" :");
		}
		for (double parameter : getParameters()) {
			sb.append(parameter + " ");
		}
		return sb.toString();
	}

	public abstract DomainInterval getInterval(double p);

	public abstract ProbDensFunction copy();
}
