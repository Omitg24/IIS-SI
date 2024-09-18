/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.model.network.modelUncertainty;

import org.openmarkov.core.model.network.potential.Potential;
import org.openmarkov.core.model.network.potential.PotentialRole;
import org.openmarkov.core.model.network.potential.TablePotential;

public class UncertainParameter {

	UncertainValue uncertainValue;
	Potential potential;
	TablePotential subPotential;
	int configuration;
	public UncertainParameter(Potential potential, UncertainValue uncertainValue, TablePotential subPotential,
			int configuration) {
		this.uncertainValue = uncertainValue;
		this.potential = potential;
		this.subPotential = subPotential;
		this.configuration = configuration;
	}

	public double getBaseLineValue() {
		return getProbDensFunction().getMean();
	}

	private ProbDensFunction getProbDensFunction() {
		return uncertainValue.getProbDensFunction();
	}

	public double min(double p) {
		return getInterval(p).min();
	}

	public double max(double p) {
		return getInterval(p).max();
	}

	private DomainInterval getInterval(double p) {
		return getProbDensFunction().getInterval(p);
	}

	/**
	 * @return True if the parameter is of kind probability, and False if it is of kind utility.
	 */
	public boolean isProbabilityParameter() {
		return this.potential.getPotentialRole() != PotentialRole.UNSPECIFIED;
	}

	/**
	 * @return True if the parameter is of kind utility, and False if it is of kind probability.
	 */
	public boolean isUtilityParameter() {
		return !isProbabilityParameter();
	}

	public String getName() {
		return uncertainValue.getName();
	}

	public boolean hasName() {
		return uncertainValue.hasName();
	}

}
