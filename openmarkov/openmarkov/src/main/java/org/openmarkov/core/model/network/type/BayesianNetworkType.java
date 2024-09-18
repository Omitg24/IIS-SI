/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.model.network.type;

import org.openmarkov.core.model.network.constraint.ConstraintBehavior;
import org.openmarkov.core.model.network.constraint.OnlyChanceNodes;
import org.openmarkov.core.model.network.type.plugin.ProbNetType;

@ProbNetType(name = "BayesianNetwork") public class BayesianNetworkType extends NetworkType {
	private static BayesianNetworkType instance = null;

	// Constructor
	private BayesianNetworkType() {
		super();
		overrideConstraintBehavior(OnlyChanceNodes.class, ConstraintBehavior.YES);
	}

	// Methods
	public static BayesianNetworkType getUniqueInstance() {
		if (instance == null) {
			instance = new BayesianNetworkType();
		}
		return instance;
	}

	/**
	 * @return String "BayesianNetwork".
	 */
	public String toString() {
		return "BAYESIAN_NET";
	}

}
