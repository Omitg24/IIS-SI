/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.inference.variableElimination.action;

import java.util.ArrayList;

import org.openmarkov.core.exception.CostEffectivenessException;
import org.openmarkov.core.model.network.CEP;
import org.openmarkov.core.model.network.Criterion;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.VariableType;
import org.openmarkov.core.model.network.potential.GTablePotential;
import org.openmarkov.core.model.network.potential.StrategyTree;
import org.openmarkov.core.model.network.potential.TablePotential;

/**
 * Given an influence diagram with several cost and effectiveness nodes, creates a new influence diagram
 * with a single utility node, which has an associated cost-effectiveness potential.
 *
 * @author Manuel Arias
 */
public class CreatePotentialUtility {

	public final static String defaultCEVariableName = "CostEffectiveness";

	/**
	 * Creates a <code>GeneralizedTablePotential</code> of
	 * <code>CEPartition</code> using the cost and effectiveness potentials.
	 *
	 * @param costPotential          <code>TablePotential</code>.
	 * @param effectivenessPotential <code>TablePotential</code>.
	 * @param lambdaMin              used in <code>PartitionLCE</code>.
	 * @param lambdaMax              used in <code>PartitionLCE</code>.
	 * @throws CostEffectivenessException CostEffectivenessException
	 * <code>fsVariables</code> must be equal to <code>effectiveness.getVariables()</code> union
	 * <code>cost.getVariables()</code>.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" }) public static GTablePotential createCEPotential(
			TablePotential costPotential, TablePotential effectivenessPotential, double lambdaMin, double lambdaMax)
			throws CostEffectivenessException {

		// Gets the union of the variables of cost and effectiveness potential
		ArrayList<Variable> ceVariables = new ArrayList<Variable>(costPotential.getVariables());

		ArrayList<Variable> effectivenessVariables = new ArrayList<Variable>(effectivenessPotential.getVariables());
		for (Variable effectivenessVariable : effectivenessVariables) {
			if (!ceVariables.contains(effectivenessVariable)) {
				ceVariables.add(effectivenessVariable);
			}
		}

		// Get cost and effectiveness potential
		GTablePotential gPotential = new GTablePotential(new ArrayList<Variable>(ceVariables));
		gPotential.setCriterion(new Criterion());

		if (ceVariables.size() == 0) {
			// Cost and effectiveness potentials are constants, so also the cost-effectiveness potential
			StrategyTree[] strategyTrees = new StrategyTree[1];
			CEP partition = new CEP(strategyTrees, costPotential.values, effectivenessPotential.values, null, lambdaMin,
					lambdaMax);
			gPotential.elementTable.add(partition);
		} else {
			int[] dimensionsResult = gPotential.getDimensions();

			// Offsets accumulate algorithm
			// 1. Set up variables
			int[] coordinate = new int[dimensionsResult.length];
			int[][] accumulatedOffsets = new int[2][];
			accumulatedOffsets[0] = gPotential.getAccumulatedOffsets(costPotential.getVariables());
			accumulatedOffsets[1] = gPotential.getAccumulatedOffsets(effectivenessPotential.getVariables());
			int[] positions = { costPotential.getInitialPosition(), effectivenessPotential.getInitialPosition() };
			int increasedVariable;
			int tableSize = gPotential.getTableSize();

			// 2. Potential initialization operation
			for (int i = 0; i < tableSize; i++) {
				double[] costs = { costPotential.values[positions[0]] };
				double[] effectivities = { effectivenessPotential.values[positions[1]] };
				StrategyTree[] strategyTrees = new StrategyTree[1];
				CEP partition = new CEP(strategyTrees, costs, effectivities, null, lambdaMin, lambdaMax);
				gPotential.elementTable.add(partition);
				// calculate next position using accumulated offsets
				increasedVariable = 0;
				for (int j = 0; j < coordinate.length; j++) {
					coordinate[j]++;
					if (coordinate[j] < dimensionsResult[j]) {
						increasedVariable = j;
						break;
					}
					coordinate[j] = 0;
				}

				// 3. Update the positions of the potentials we are multiplying
				for (int j = 0; j < positions.length; j++) {
					positions[j] += accumulatedOffsets[j][increasedVariable];
				}
			}
		}

		Variable ceVariable = new Variable(defaultCEVariableName);
		ceVariable.setVariableType(VariableType.NUMERIC);
		gPotential.setCriterion(ceVariable.getDecisionCriterion());
		return gPotential;
	}

	//	/** Get potentials attached to a variable whose criterion is cost and sums them.
	//	 * @param influenceDiagram <code>ProbNet</code>
	//	 * @return <code>TablePotential</code>
	//	 * @throws CostEffectivenessException   */
	//	private static TablePotential getCostPotential(ProbNet influenceDiagram)
	//			throws CostEffectivenessException {
	//
	//		List<Variable> variables = influenceDiagram.getVariables();
	//		ArrayList<TablePotential> costPotentials = new ArrayList<TablePotential>();
	//		for (Variable variable : variables) {
	//			if (CEBaseOperations.isCostVariable(variable))	{
	//				Collection<Potential> potentialsVariable = influenceDiagram.getPotentials(variable);
	//				for (Potential potential : potentialsVariable) {
	//					if (potential.isAdditive()) {
	//						try {
	//							costPotentials.add(potential.getCPT());
	//						} catch (NonProjectablePotentialException | WrongCriterionException e) {
	//							throw new CostEffectivenessException(e.getMessage());
	//						}
	//					}
	//				}
	//			}
	//		}
	//		TablePotential costPotential = DiscretePotentialOperations.sum(costPotentials);
	//		return costPotential;
	//	}
	//
	//	/** Get potentials attached to a variable whose criterion is effectiveness and adds them.
	//	 * @param influenceDiagram <code>ProbNet</code>
	//	 * @return <code>TablePotential</code>
	//	 * @throws CostEffectivenessException   */
	//	private static TablePotential getEffectivenessPotential(ProbNet influenceDiagram)
	//			throws CostEffectivenessException {
	//		List<Variable> variables = influenceDiagram.getVariables();
	//		ArrayList<TablePotential> effectivenessPotentials = new ArrayList<TablePotential>();
	//		for (Variable variable : variables) {
	//			if (CEBaseOperations.isEffectivenessVariable(variable)) {
	//				Collection<Potential> potentialsVariable = influenceDiagram.getPotentials(variable);
	//				for (Potential potential : potentialsVariable) {
	//					if (potential.isAdditive()) {
	//						try {
	//							effectivenessPotentials.add(potential.getCPT());
	//						} catch (NonProjectablePotentialException | WrongCriterionException e) {
	//							throw new CostEffectivenessException(e.getMessage());
	//						}
	//					}
	//				}
	//			}
	//		}
	//		TablePotential effectivenessPotential = DiscretePotentialOperations.sum(effectivenessPotentials);
	//		return effectivenessPotential;
	//	}

}
