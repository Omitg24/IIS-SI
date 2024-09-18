/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.inference;

import java.util.Hashtable;

import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.potential.TablePotential;

public class StrategyUtilities {

	Hashtable<Variable, TablePotential> utilities;

	/**
	 *
	 */
	public StrategyUtilities() {
		super();
		utilities = new Hashtable<>();
	}

	/**
	 * @return the utilities
	 */
	public Hashtable<Variable, TablePotential> getUtilities() {
		return utilities;
	}

	/**
	 * @param utilities the utilities to set
	 */
	public void setUtilities(Hashtable<Variable, TablePotential> utilities) {
		this.utilities = utilities;
	}

	/**
	 * @param decision Decision variable
	 * @return the utilities
	 */
	public TablePotential getUtilities(Variable decision) {
		return utilities.get(decision);
	}

	public void assignUtilityTable(Variable decision, TablePotential globalUtilityTable) {
		utilities.put(decision, globalUtilityTable);

	}

}
