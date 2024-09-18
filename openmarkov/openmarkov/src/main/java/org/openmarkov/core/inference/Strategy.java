/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.inference;

import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.potential.GTablePotential;
import org.openmarkov.core.model.network.potential.TablePotential;
import org.openmarkov.core.model.network.potential.operation.DiscretePotentialOperations;

public class Strategy {

	Hashtable<Variable, Policy> strategy;

	/**
	 * @param stratUtil constructs a strategy by maximizing over the utility tables
	 */
	public Strategy(StrategyUtilities stratUtil) {
		this();
		Set<Variable> decisions = stratUtil.getUtilities().keySet();

		for (Variable dec : decisions) {
			setPolicy(dec, new Policy(dec, stratUtil.getUtilities(dec)));
		}

	}

	public Strategy() {
		strategy = new Hashtable<>();
	}

	public List<Variable> getDomainOfPolicy(Variable varDecision) {
		return getPolicy(varDecision).getDomain();
	}

	public Policy getPolicy(Variable varDecision) {
		return strategy.get(varDecision);
	}

	private void setPolicy(Variable dec, Policy policy) {
		strategy.put(dec, policy);

	}

	public class Policy {

		GTablePotential potential;

		@SuppressWarnings("unchecked") public Policy(Variable dec, TablePotential utilities) {
			this();
			potential = (GTablePotential) DiscretePotentialOperations.maximize(utilities, dec)[1];
		}

		public Policy() {
			// TODO Auto-generated constructor stub
		}

		public GTablePotential getPotential() {

			return potential;
		}

		public List<Variable> getDomain() {
			return potential.getVariables();
		}

	}

}
