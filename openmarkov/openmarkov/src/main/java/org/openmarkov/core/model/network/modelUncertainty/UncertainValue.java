/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.model.network.modelUncertainty;

import java.util.Random;

/**
 * An {@code UncertainValue} is a value of a table of potentials which is
 * used for sensitivity analysis.
 *
 * @author Manuel Luque
 * @author Elena Almaraz
 * @author Javier Diez
 * @version 1.0
 * @since OpenMarkov 1.0
 */
public class UncertainValue {
	// Attributes
	/**
	 * Probability density function.
	 */
	protected ProbDensFunction probDensFunction;
	/**
	 * Name of the parameter.
	 */
	protected String name;

	public UncertainValue(double value) {
		name = null;
		probDensFunction = new ExactFunction(value);
	}

	public UncertainValue(ProbDensFunction probDensFunction, String name) {
		this.name = name;
		this.probDensFunction = probDensFunction;
	}

	public UncertainValue(ProbDensFunction probDensFunction) {
		this(probDensFunction, null);
	}

	public UncertainValue(UncertainValue uncertainValue) {
		super();
		if (uncertainValue.name != null) {
			this.name = new String(uncertainValue.name);
		}

		if (uncertainValue.probDensFunction != null) {
			this.probDensFunction = uncertainValue.probDensFunction.copy();
		}
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public ProbDensFunction getProbDensFunction() {
		return probDensFunction;
	}

	public boolean verifyParametersDomain(boolean isChanceVariable) {
		return probDensFunction.verifyParametersDomain(isChanceVariable);
	}

	public double getSample(Random randomGenerator) {
		return probDensFunction.getSample(randomGenerator);
	}

	@Override public String toString() {
		StringBuilder sb = new StringBuilder();
		if (name != null && name.isEmpty()) {
			sb.append(name);
			sb.append(": ");
		}
		sb.append(probDensFunction.toString());
		return sb.toString();
	}

	public boolean hasName() {
		return name != null && name.length() > 0;
	}

	public UncertainValue copy() {
		return new UncertainValue(this);
	}

}
