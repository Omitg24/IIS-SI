/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.model.network.potential.operation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.potential.PotentialRole;
import org.openmarkov.core.model.network.potential.StrategyTree;
import org.openmarkov.core.model.network.potential.TablePotential;

public class SumOutVariable extends Marginalization {

	/**
	 * Classify the potentials into probability and additive, if there are probabilities, produces a new probability potential;
	 * if there are additive potentials, produces a new additive potential. Both without the received variable
	 *
	 * @param variable {@code Variable}
	 * @param potentials     {@code List} of {@code TablePotential}
	 * marginal probability and new utility in this order.
	 */
	public SumOutVariable(Variable variable, Collection<TablePotential> potentials) {
		// Get probability and utility potentials
		List<TablePotential> probPotentials = new ArrayList<>();
		List<TablePotential> additivePotentials = new ArrayList<>();
		classifyProbAndUtilityPotentials(potentials, probPotentials, additivePotentials);
		int numAdditivePotentials = additivePotentials.size();
		List<TablePotential> intermediateAdditivePotentials = new ArrayList<>(numAdditivePotentials);
		TablePotential marginalProb = DiscretePotentialOperations.multiplyAndMarginalize(probPotentials, variable);
		// Do not return the probability potential if it depends on no variables
		// and its value is 1

		boolean thereAreAdditivePotentials = numAdditivePotentials > 0;
		if (thereAreAdditivePotentials) {
			// build the marginal and conditional probabilities
			TablePotential joinProb = DiscretePotentialOperations.multiply(probPotentials);
			if (joinProb == null) {
				joinProb = new TablePotential(new ArrayList<Variable>(), PotentialRole.CONDITIONAL_PROBABILITY);
			}
			TablePotential conditionalProb = DiscretePotentialOperations.divide(joinProb, marginalProb);

			for (TablePotential additivePotential : additivePotentials) {
				List<Variable> additiveVariables = additivePotential.getVariables();
				boolean thereAreInterventions = additivePotential.strategyTrees != null;

				// Initialize the output utility potential.

				// Set of variables = marginalProb variables + additiveVariables not already included
				List<Variable> outputAdditiveVariables = marginalProb.getVariables();
				for (Variable additiveVariable : additivePotential.getVariables()) {
					if (additiveVariable != variable && !outputAdditiveVariables.contains(additiveVariable)) {
						outputAdditiveVariables.add(additiveVariable);
					}
				}
				// Create potential
				TablePotential outputAdditivePotential = new TablePotential(outputAdditiveVariables, PotentialRole.UNSPECIFIED);
				outputAdditivePotential.setCriterion(additivePotential.getCriterion());
				if (thereAreInterventions) {
					outputAdditivePotential.strategyTrees = new StrategyTree[outputAdditivePotential.values.length];
				}

				List<Variable> allVariables = new ArrayList<>(outputAdditiveVariables.size() + 1);
				allVariables.add(variable);
				allVariables.addAll(outputAdditiveVariables);
				int numVariables = allVariables.size();
				int[] allVariablesDimensions = TablePotential.calculateDimensions(allVariables);

				// constants for the iterations
				int chanceVariableSize = variable.getNumStates();
				int[] accOffsetsConditionalProbPotential = TablePotential
						.getAccumulatedOffsets(allVariables, conditionalProb.getVariables());
				int[] accOffsetsInputUtilityPotential = TablePotential
						.getAccumulatedOffsets(allVariables, additiveVariables);

				// auxiliary variables that may change in every iteration
				int[] allVariablesCoordinate = new int[numVariables];
				int outputUtilityPotentialPosition = 0;
				int conditionalProbPotentialPosition = 0;
				int inputUtilityPotentialPosition = 0;
				int increasedVariable = 0;

				double[] probabilities = new double[chanceVariableSize];
				StrategyTree[] strategyTrees = new StrategyTree[chanceVariableSize];

				// outer iterations correspond to the variables in the outputAdditivePotential
				int tableSize = TablePotential.computeTableSize(outputAdditiveVariables);
				for (int outerIteration = 0; outerIteration < tableSize; outerIteration++) {
					double sum = 0;
					// inner iterations correspond to the chance variable to eliminate
					for (int innerIteration = 0; innerIteration < chanceVariableSize; innerIteration++) {
						double auxProb = conditionalProb.values[conditionalProbPotentialPosition];
						// This "if" is to ensure 0*(-Infinity) = 0
						if (auxProb > 0) {
							sum += auxProb * additivePotential.values[inputUtilityPotentialPosition];
						}
						if (thereAreInterventions) {
							probabilities[innerIteration] = auxProb;
							strategyTrees[innerIteration] = additivePotential.strategyTrees[inputUtilityPotentialPosition];
						}

						// find the next configuration and the index of the
						// increased variable
						increasedVariable = DiscretePotentialOperations
								.findNextConfigurationAndIndexIncreasedVariable(allVariablesDimensions,
										allVariablesCoordinate, increasedVariable);

						// Update coordinates
						conditionalProbPotentialPosition += accOffsetsConditionalProbPotential[increasedVariable];
						inputUtilityPotentialPosition += accOffsetsInputUtilityPotential[increasedVariable];
					}

					outputAdditivePotential.values[outputUtilityPotentialPosition] = sum;
					if (thereAreInterventions) {
						outputAdditivePotential.strategyTrees[outputUtilityPotentialPosition] = StrategyTree
								.averageOfInterventions(variable, probabilities, strategyTrees);
					}

					outputUtilityPotentialPosition++;

				} // end of outer loop

				// Return the additive potential if there are interventions or
				// if any of its values is different from 0.0
				if (thereAreInterventions || DiscretePotentialOperations
						.thereAreRelevantUtilities(outputAdditivePotential)) {
					boolean criteriaFound = false;
					for (int i = 0; i < intermediateAdditivePotentials.size(); i++) {
						if (intermediateAdditivePotentials.get(i).getCriterion() == outputAdditivePotential.getCriterion()) {
							intermediateAdditivePotentials.set(i,
									DiscretePotentialOperations.sum(intermediateAdditivePotentials.get(i), outputAdditivePotential));
							criteriaFound = true;
							break;
						}
					}
					if (!criteriaFound) {
						intermediateAdditivePotentials.add(outputAdditivePotential);
					}
				}

			} // end of additivePotentials loop
		} // end of if (!thereAreAdditivePotentials)

		if (marginalProb.getNumVariables() > 0 ||
				!DiscretePotentialOperations.almostEqual(marginalProb.values[0], 1.0)) {
			marginalProb.setPotentialRole(PotentialRole.JOINT_PROBABILITY);
			setProbability(marginalProb);
		}
		setUtility(DiscretePotentialOperations.sum(intermediateAdditivePotentials));
	}
}

