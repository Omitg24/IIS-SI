/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.model.network.modelUncertainty;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class FamilyDistribution {

	protected List<UncertainValue> family;

	public FamilyDistribution() {
		family = null;
	}

	public FamilyDistribution(List<UncertainValue> uncertainValues) {
		family = uncertainValues;
	}

	protected static List<UncertainValue> filterByFunction(Class<?> functionClass,
			List<UncertainValue> uncertainValues) {
		List<UncertainValue> filteredValues = new ArrayList<>();
		for (UncertainValue uncertainValue : uncertainValues) {
			if (functionClass.isAssignableFrom(uncertainValue.getProbDensFunction().getClass())) {
				filteredValues.add(uncertainValue);
			}
		}
		return filteredValues;
	}

	public List<UncertainValue> getFamily() {
		return family;
	}

	public void setFamily(List<UncertainValue> family) {
		this.family = family;
	}

	public double[] getMean() {
		double[] mean;
		int size = family.size();
		mean = new double[size];
		for (int i = 0; i < size; i++) {
			mean[i] = family.get(i).getProbDensFunction().getMean();
		}
		return mean;
	}

	public double[] getMaximum() {
		double[] max;
		int size = family.size();
		max = new double[size];
		for (int i = 0; i < size; i++) {
			max[i] = family.get(i).getProbDensFunction().getMaximum();
		}
		return max;
	}

	public double[] getVariance() {
		return new double[family.size()];
	}

	public double[] getStandardDeviation() {
		double[] stDeviation;

		stDeviation = new double[family.size()];
		double[] variance = getVariance();
		for (int i = 0; i < family.size(); i++) {
			stDeviation[i] = Math.sqrt(variance[i]);
		}
		return stDeviation;
	}

	public void remove(UncertainValue child) {
		family.remove(child);
	}

	public double[] getSample(Random randomGenerator) {
		double[] mean;
		int size = family.size();
		mean = new double[size];
		for (int i = 0; i < size; i++) {
			mean[i] = family.get(i).getProbDensFunction().getSample(randomGenerator);
		}
		return mean;
	}
}
