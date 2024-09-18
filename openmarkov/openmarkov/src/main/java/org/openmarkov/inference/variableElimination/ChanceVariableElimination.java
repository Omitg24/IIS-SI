/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.inference.variableElimination;

import java.util.ArrayList;
import java.util.List;

import org.openmarkov.core.exception.CostEffectivenessException;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.potential.GTablePotential;
import org.openmarkov.core.model.network.potential.TablePotential;
import org.openmarkov.core.model.network.potential.operation.DiscretePotentialOperations;
import org.openmarkov.inference.variableElimination.operation.CEPotentialOperation;

/**
 * Class used to sum out a chance variable given a set of potentials
 */
public class ChanceVariableElimination {

	TablePotential marginalProbability;
	List<TablePotential> utilityPotentials;

	public ChanceVariableElimination(Variable variableToDelete, List<TablePotential> probPotentials,
			List<TablePotential> inputUtilityPotentials) throws CostEffectivenessException {

		List<TablePotential> utilityPotentialsByCriterion = DiscretePotentialOperations
				.sumByCriterion(inputUtilityPotentials);

		utilityPotentials = new ArrayList<>();

		if (inputUtilityPotentials.isEmpty()) {
			// add the marginal probability to the network
			marginalProbability = DiscretePotentialOperations.multiplyAndMarginalize(probPotentials, variableToDelete);
		} else {
			TablePotential jointProbability = DiscretePotentialOperations.multiply(probPotentials);
			marginalProbability = DiscretePotentialOperations.marginalize(jointProbability, variableToDelete);
			TablePotential conditionalProbability = DiscretePotentialOperations
					.divide(jointProbability, marginalProbability);
			//Set to 0 every NaN cell in conditionalProbability
			double[] conditionalProbabilityValues = conditionalProbability.values;
			for (int i = 0; i < conditionalProbabilityValues.length; i++) {
				if (Double.isNaN(conditionalProbabilityValues[i])) {
					conditionalProbabilityValues[i] = 0.0;
				}
			}

			List<Variable> orderedVariables = new ArrayList<>();
			orderedVariables.add(variableToDelete);
			for (Variable variable : conditionalProbability.getVariables()) {
				if (variable != variableToDelete) {
					orderedVariables.add(variable);
				}
			}
			if (conditionalProbability.getVariables().size() > 0) {
				conditionalProbability = DiscretePotentialOperations.reorder(conditionalProbability, orderedVariables);
			}

			// eliminate the chance variable as in the unicriterion case
			for (TablePotential utilityPotential : utilityPotentialsByCriterion) {
				if (utilityPotential instanceof GTablePotential) {
					utilityPotentials.add(CEPotentialOperation
							.multiplyAndMarginalize(conditionalProbability, (GTablePotential) utilityPotential,
									variableToDelete));
				} else {
					utilityPotentials.add(DiscretePotentialOperations
							.multiplyAndMarginalize(conditionalProbability, utilityPotential, variableToDelete));
				}
			}
		}
	}

	public static boolean hasIncorrectProbability(TablePotential pot) {
		boolean isCorrect = true;
		if (pot != null) {
			double[] values = pot.values;
			for (int i = 0; i < values.length && isCorrect; i++) {
				double value = values[i];
				isCorrect = value >= 0.0 && value <= 1.0;
			}
		}
		return !isCorrect;
	}

	/**
	 * @return the marginal probability
	 */
	public TablePotential getMarginalProbability() {
		return marginalProbability;
	}

	/**
	 * @return a utility potential for each criterion
	 */
	public List<TablePotential> getUtilityPotentials() {
		return utilityPotentials;
	}

}
