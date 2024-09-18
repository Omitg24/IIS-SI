/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.model.network.potential.operation;

import java.util.ArrayList;
import java.util.List;

import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.potential.PotentialRole;
import org.openmarkov.core.model.network.potential.StrategyTree;
import org.openmarkov.core.model.network.potential.TablePotential;

/**
 * From a probability and one or more additive potentials, that contains a given variable,
 * this class builds a new potential, first adding the additive potential into only one, then
 * by multiplying the probability and the additive potential and finally, maximizing
 * the resulting potential according to the variable.
 */
public class MaxOutVariable {

	private TablePotential utility;

	private TablePotential policy;

	/**
	 * Adds the additive potentials in order to have only one utility, then uses the next constructor.
	 * @param decisionVariable Decision variable
	 * @param probability Table potential
	 * @param inputAdditivePotentials Input additive potentials
	 */
	public MaxOutVariable(Variable decisionVariable, TablePotential probability, List<TablePotential> inputAdditivePotentials) {
		this(decisionVariable, probability, DiscretePotentialOperations.sum(inputAdditivePotentials));
	}

	/**
	 * Calculates the new utility and the policy
	 * @param decisionVariable Decision variable
	 * @param probability Table potential
	 * @param inputAdditivePotential Input additive potential
	 */
	public MaxOutVariable(Variable decisionVariable, TablePotential probability, TablePotential inputAdditivePotential) {
		TablePotential additivePotentialToMaximize = DiscretePotentialOperations.multiply(probability, inputAdditivePotential);
		List<Variable> additivePotentialToMaximizeVariables = additivePotentialToMaximize.getVariables();

		// initialize the output utility potential
		List<Variable> outputVariables = additivePotentialToMaximize.getVariables();
		outputVariables.remove(decisionVariable);
		TablePotential outputUtility = new TablePotential(outputVariables, PotentialRole.UNSPECIFIED);
		outputUtility.strategyTrees = new StrategyTree[outputUtility.values.length];
		outputUtility.setCriterion(additivePotentialToMaximize.getCriterion());

		// Accumulated offsets algorithm
		List<Variable> allVariables = new ArrayList<>(outputVariables.size() + 1);
		allVariables.add(decisionVariable);
		allVariables.addAll(outputVariables); // All variables = (decision) variable to maximize + outputVariables
		int numVariables = allVariables.size();
		int[] allVariablesDimensions = TablePotential.calculateDimensions(allVariables);

		// constants for the iterations
		int decisionVariableSize = decisionVariable.getNumStates();
		int[] accOffsetsInputUtilityPotential = TablePotential
				.getAccumulatedOffsets(allVariables, additivePotentialToMaximizeVariables);

		// auxiliary variables that may change in every iteration
		int[] allVariablesCoordinate = new int[numVariables];
		int outputUtilityPotentialPosition = 0;
		int inputUtilityPotentialPosition = 0;
		int increasedVariable = 0;

		double max;
		ArrayList<Integer> optimalStatesIndexes = new ArrayList<>(decisionVariableSize);

		double[] utilities = new double[decisionVariableSize];
		StrategyTree[] strategyTrees = new StrategyTree[decisionVariableSize];

		// initialize the policy
		TablePotential policyPotential = new TablePotential(allVariables, PotentialRole.POLICY);
		double[] policyValues = policyPotential.values;

		// outer iterations correspond to the variables to in the
		// outputUtilityPotential
		int outputPotentialSize = TablePotential.computeTableSize(outputVariables);
		for (int outerIteration = 0; outerIteration < outputPotentialSize; outerIteration++) {
			// reset auxiliary variables before entering the loop
			max = Double.NEGATIVE_INFINITY;
			optimalStatesIndexes.clear();
			// inner iterations correspond to the decision variable to eliminate
			for (int innerIteration = 0; innerIteration < decisionVariableSize; innerIteration++) {
				double auxInputUtilityPotentialValue = additivePotentialToMaximize.values[inputUtilityPotentialPosition];
				if (auxInputUtilityPotentialValue >= max) {
					if (auxInputUtilityPotentialValue > max) {
						max = auxInputUtilityPotentialValue;
						optimalStatesIndexes.clear();
					}
					optimalStatesIndexes.add(innerIteration);
				}
				utilities[innerIteration] = auxInputUtilityPotentialValue;
				if (additivePotentialToMaximize.strategyTrees != null) {
					strategyTrees[innerIteration] = additivePotentialToMaximize.strategyTrees[inputUtilityPotentialPosition];
				}

				// find the next configuration and the index of the increased variable
				increasedVariable = DiscretePotentialOperations
						.findNextConfigurationAndIndexIncreasedVariable(allVariablesDimensions, allVariablesCoordinate,
								increasedVariable);

				// Update coordinates
				inputUtilityPotentialPosition += accOffsetsInputUtilityPotential[increasedVariable];
			}

			outputUtility.values[outputUtilityPotentialPosition] = max;
			// TODO In testing phase it is easier to assume that there are no
			// ties between interventions
			outputUtility.strategyTrees[outputUtilityPotentialPosition] = StrategyTree
					.optimalInterventionTakingAllOptimal(decisionVariable, utilities, strategyTrees);

			// set the values of policyPotential
			int policyPotentialPosition = outputUtilityPotentialPosition * decisionVariableSize;
			double probForOptimalStates = 1.0 / optimalStatesIndexes.size();
			for (int i = 0; i < decisionVariableSize; i++) {
				policyValues[policyPotentialPosition + i] = (optimalStatesIndexes.contains(i)) ?
						probForOptimalStates :
						0.0;
			}
			outputUtilityPotentialPosition++;

		} // end of the outer loop

		// Return the utility potential if some of its values is different from
		// 0.0
		// or if any of the interventions is not null
		// TODO Manolo> Deberíamos considerar devolver siempre algo en el
		// atributo utility. Por ejemplo, al siguiente "if"
		// podríamos añadir un "else" y guardar un potencial 0.
		if (thereAreInterventionsInOutputUtilityPotential(outputUtility) ||
				DiscretePotentialOperations.thereAreRelevantUtilities(outputUtility)) {
			utility = outputUtility;
		} else {
			utility = DiscretePotentialOperations.createZeroUtilityPotential(null);
		}

		utility.setCriterion(inputAdditivePotential.getCriterion());

		// Store output policy
		policy = policyPotential;  // policy potential contains (decision) variable to remove + additivePotential variables
	}

	/**
	 * It is true when there is at least one strategy tree
	 * @param outputUtilityPotential Output utility potential
	 * @return True if there are interventions in the output utility potential
	 */
	private static boolean thereAreInterventionsInOutputUtilityPotential(TablePotential outputUtilityPotential) {
		boolean thereAreInterventions = false;
		if (outputUtilityPotential.strategyTrees != null) {
			for (int i = 0; i < outputUtilityPotential.strategyTrees.length && !thereAreInterventions; i++) {
				thereAreInterventions = outputUtilityPotential.strategyTrees[i] != null;
			}
		}
		return thereAreInterventions;
	}



	/*
	 * @param decisionVariable <code>Variable</code>
	 * @param potentials <code>List</code> of <code>TablePotential</code>
	 * @param sdagInterventions
	 * @return A <code>List</code> of <code>TablePotential</code>, with these, some of them optional, potentials:
	 * <ol>
	 * <li>if there are probability potentials in <code>potentials</code>, a join probability potential.
	 * <li>if there are interventions or the utility is different than 0 in the sum of the utility potentials of
	 * <code>potentials</code>, that sum of utility potentials.
	 * <li>a policy potential.
	 * </ol>
	 * OJO: Había otro método maxOutVariable que llamaba a éste, y con Javadoc:
	 *
	 * @param decisionVariable <code>Variable</code>
	 * @param potentials <code>List</code> of <code>TablePotential</code>
	 * @return A <code>List</code> with two <code>TablePotential</code>, marginal probability and new utility in this order.
	 *
	 */
	/*public MaxOutVariable(Variable decisionVariable, Collection<TablePotential> potentials) {
		// Get probability and utility potentials
		List<TablePotential> probPotentials = new ArrayList<>();
		List<TablePotential> utilityPotentials = new ArrayList<>();
		Marginalization.classifyProbAndUtilityPotentials(potentials, probPotentials, utilityPotentials);

		// if there are probability potentials depending on decisionVariable,
		// its product does not depend on decisionVariable and should be
		// projected
		boolean thereAreProbabilities = probPotentials.size() > 0;
		
		TablePotential inputUtilityPotential = DiscretePotentialOperations.sum(utilityPotentials);
		List<Variable> inputUtilityVariables = inputUtilityPotential.getVariables();

		// initialize the output utility potential
		List<Variable> outputUtilityVariables = inputUtilityPotential.getVariables();
		outputUtilityVariables.remove(decisionVariable);
		TablePotential outputUtilityPotential = new TablePotential(outputUtilityVariables, PotentialRole.UNSPECIFIED);
		outputUtilityPotential.interventions = new Intervention[outputUtilityPotential.values.length];

		outputUtilityPotential.setCriterion(inputUtilityPotential.getCriterion());

		// in allVariables, the first variable is decisionVariable
		List<Variable> allVariables = new ArrayList<>(outputUtilityVariables.size() + 1);
		allVariables.add(decisionVariable);
		allVariables.addAll(outputUtilityVariables);
		int numVariables = allVariables.size();
		int[] allVariablesDimensions = TablePotential.calculateDimensions(allVariables);

		// constants for the iterations
		int decisionVariableSize = decisionVariable.getNumStates();
		int[] accOffsetsInputUtilityPotential = TablePotential.getAccumulatedOffsets(allVariables,
				inputUtilityVariables);

		// auxiliary variables that may change in every iteration
		int[] allVariablesCoordinate = new int[numVariables];
		int outputUtilityPotentialPosition = 0;
		int inputUtilityPotentialPosition = 0;
		int increasedVariable = 0;

		double max;
		ArrayList<Integer> optimalStatesIndices = new ArrayList<>(decisionVariableSize);

		double[] utilities = new double[decisionVariableSize];
		Intervention[] interventions = new Intervention[decisionVariableSize];

		// initialize the policy
		TablePotential policyPotential = new TablePotential(allVariables, PotentialRole.POLICY);
		double[] policyValues = policyPotential.values;

		// outer iterations correspond to the variables to in the
		// outputUtilityPotential
		for (int outerIteration = 0; outerIteration < TablePotential
				.computeTableSize(outputUtilityVariables); outerIteration++) {
			// reset auxiliary variables before entering the loop
			max = Double.NEGATIVE_INFINITY;
			optimalStatesIndices.clear();
			// inner iterations correspond to the decision variable to eliminate
			for (int innerIteration = 0; innerIteration < decisionVariableSize; innerIteration++) {
				double auxInputUtilityPotentialValue = inputUtilityPotential.values[inputUtilityPotentialPosition];
				if (auxInputUtilityPotentialValue >= max) {
					if (auxInputUtilityPotentialValue > max) {
						max = auxInputUtilityPotentialValue;
						optimalStatesIndices.clear();
					}
					optimalStatesIndices.add(innerIteration);
				}
				utilities[innerIteration] = auxInputUtilityPotentialValue;
				if (inputUtilityPotential.interventions != null) {
					interventions[innerIteration] = inputUtilityPotential.interventions[inputUtilityPotentialPosition];
				}

				// find the next configuration and the index of the increased variable
				increasedVariable = DiscretePotentialOperations.findNextConfigurationAndIndexIncreasedVariable(
						allVariablesDimensions, allVariablesCoordinate, increasedVariable);

				// Update coordinates
				inputUtilityPotentialPosition += accOffsetsInputUtilityPotential[increasedVariable];
			}

			outputUtilityPotential.values[outputUtilityPotentialPosition] = max;
			// TODO In testing phase it is easier to assume that there are no
			// ties between interventions
			outputUtilityPotential.interventions[outputUtilityPotentialPosition] = Intervention
					.optimalInterventionTakingAllOptimal(decisionVariable, utilities, interventions);

			// set the values of policyPotential
			int policyPotentialPosition = outputUtilityPotentialPosition * decisionVariableSize;
			double probForOptimalStates = 1.0 / optimalStatesIndices.size();
			for (int i = 0; i < decisionVariableSize; i++) {
				policyValues[policyPotentialPosition + i] = (optimalStatesIndices.contains(i)) ? probForOptimalStates
						: 0.0;
			}
			outputUtilityPotentialPosition++;

		} // end of the outer loop

		// Return the utility potential if some of its values is different from
		// 0.0
		// or if any of the interventions is not null
		// TODO Manolo> Deberíamos considerar devolver siempre algo en el
		// atributo utility. Por ejemplo, al siguiente "if"
		// podríamos añadir un "else" y guardar un potencial 0.
		if (thereAreInterventionsInOutputUtilityPotential(outputUtilityPotential)
				|| DiscretePotentialOperations.thereAreRelevantUtilities(outputUtilityPotential)) {
			// Store output utility potential
			utility = outputUtilityPotential;
		}

		// Store output policy
		setPolicy(policyPotential);
	}
	
	
    */

	public TablePotential getUtility() {
		return utility;
	}

	public TablePotential getPolicy() {
		return policy;
	}

	protected void setPolicy(TablePotential policy) {
		this.policy = policy;
	}

}
