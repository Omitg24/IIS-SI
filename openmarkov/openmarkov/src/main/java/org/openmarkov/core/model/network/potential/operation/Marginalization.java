/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.model.network.potential.operation;

import java.util.Collection;

import org.openmarkov.core.model.network.potential.Potential;
import org.openmarkov.core.model.network.potential.TablePotential;

public abstract class Marginalization {

	private TablePotential utility;

	private TablePotential probability;

	/**
	 * Classifies potential from the first list between probability and utility and stores them
	 * in the second and third list
	 *
	 * @param potentials        {@code List} of {@code TablePotential}
	 * @param probPotentials    {@code List} of {@code TablePotential}
	 * @param utilityPotentials {@code List} of {@code TablePotential}
	 */
	protected void classifyProbAndUtilityPotentials(Collection<? extends Potential> potentials,
			Collection<TablePotential> probPotentials, Collection<TablePotential> utilityPotentials) {
		for (Potential potential : potentials) {
			if (potential.isAdditive()) {
				utilityPotentials.add((TablePotential) potential);
			} else {
				probPotentials.add((TablePotential) potential);
			}
		}
	}

	public TablePotential getUtility() {
		return utility;
	}

	protected void setUtility(TablePotential utility) {
		this.utility = utility;
	}

	public TablePotential getProbability() {
		return probability;
	}

	protected void setProbability(TablePotential probability) {
		this.probability = probability;
	}

}
