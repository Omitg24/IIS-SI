/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.inference.variableElimination;

import java.util.List;

import org.openmarkov.core.exception.PotentialOperationException;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.potential.GTablePotential;
import org.openmarkov.core.model.network.potential.TablePotential;
import org.openmarkov.core.model.network.potential.operation.DiscretePotentialOperations;
import org.openmarkov.core.model.network.potential.operation.MaxOutVariable;
import org.openmarkov.inference.variableElimination.operation.CEPotentialOperation;

/**
 * Class used to maximize a set of potentials for a decision variable
 */
public class DecisionVariableElimination {

	TablePotential projectedProbability;
	TablePotential utility;
	TablePotential optimalPolicy;

	public DecisionVariableElimination(Variable variableToDelete, List<TablePotential> probPotentials,
			List<TablePotential> inputUtilityPotentials) throws PotentialOperationException {
		// all the potentials have the same criterion
		TablePotential totalUtility = DiscretePotentialOperations.sum(inputUtilityPotentials);
		TablePotential jointProbability = null;

		if (!probPotentials.isEmpty()) {
			jointProbability = DiscretePotentialOperations.multiply(probPotentials);
			projectedProbability = DiscretePotentialOperations.projectOutVariable(variableToDelete, jointProbability);
		} else {
			jointProbability = DiscretePotentialOperations.createUnityProbabilityPotential();
			projectedProbability = DiscretePotentialOperations.createUnityProbabilityPotential();
		}

		// maximize the utility potentials
		if (totalUtility instanceof GTablePotential) {
			utility = CEPotentialOperation.ceMaximize((GTablePotential) totalUtility, variableToDelete);

		} else {
			MaxOutVariable max = new MaxOutVariable(variableToDelete,
					DiscretePotentialOperations.createUnityProbabilityPotential(), totalUtility);
			utility = max.getUtility();
			optimalPolicy = max.getPolicy();
		}
	}

	public TablePotential getProjectedProbability() {
		return projectedProbability;
	}

	public TablePotential getUtility() {
		return utility;
	}

	public TablePotential getOptimalPolicy() {
		return optimalPolicy;
	}

}
