/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.model.network;

import java.util.ArrayList;
import java.util.List;

import org.openmarkov.core.model.network.potential.Potential;

public class UtilityOperations {

	/**
	 * Transform a multicriteria net into an unicriterion net. All the utility
	 * potentials are adjusted according to the scales defined in their
	 * criteria.
	 *
	 * @param probNet Network
	 */
	public static void transformToUnicriterion(ProbNet probNet) {
//		Criterion globalCriterion = new Criterion(Criterion.C_GLOBALCRITERION);

		for (Node utilityNode : probNet.getNodes(NodeType.UTILITY)) {
			Criterion decisionCriterion = utilityNode.getVariable().getDecisionCriterion();
			if (decisionCriterion != null) {
                double scale = decisionCriterion.getUnicriterizationScale();
				// Get the actual criterion scale
				List<Potential> utilityPotentials = utilityNode.getPotentials();

				if (!utilityPotentials.isEmpty()) {
                    List<Potential> scaledPotentials = new ArrayList<>();
					for (Potential potential : utilityPotentials) {
							Potential scaledPotential = potential.deepCopy(probNet);
							scaledPotential.scalePotential(scale);
                        scaledPotentials.add(scaledPotential);
					}
                    utilityNode.setPotentials(scaledPotentials);

				} else {
					// TODO - Check if we must remove the potential and node
					// Remove the potential and the node
					probNet.removePotentials(utilityPotentials);
					probNet.removeNode(utilityNode);
				}
//				utilityNode.getVariable().setDecisionCriterion(globalCriterion);
			}
		}
	}

	/**
	 * Transform the probNet scaling the utility potentials with the cost-effectiveness scale
	 *
	 * @param probNet transformed probNet
	 */
	public static void applyCEUtilityScaling(ProbNet probNet) {
		for (Node utilityNode : probNet.getNodes(NodeType.UTILITY)) {
			if (utilityNode.getVariable().getDecisionCriterion() != null) {
				// Save the actual criterion scale
				double scale = utilityNode.getVariable().getDecisionCriterion().getCeScale();
				if (scale != 0) {
					// Transform the potential with the scale
					Potential potential = utilityNode.getPotentials().get(0).deepCopy(probNet);
					potential.scalePotential(scale);
					utilityNode.setPotential(potential);
				} else {
					// Remove the potential and the node
					probNet.removePotentials(utilityNode.getPotentials());
					probNet.removeNode(utilityNode);
				}
			}
		}
	}

	/**
	 * Left Riemann sum defined in Elbasha 2016
	 *
	 * @param values        Array of values where each value represents the value of the cycle defined by the index in the array
	 * @param lenghtOfCycle Number of cycles in a year (For example, with monthly cycles, n = 12)
	 * @return result of applying the Left Riemann sum
	 */
	public static double applyLeftRiemannSum(double[] values, int lenghtOfCycle) {
		double[] newValues = new double[values.length];
		double summatory = 0;
		int numberOfCycles = values.length / lenghtOfCycle;
		for (int k = 1; k < numberOfCycles; k++) {
			newValues[k] = values[k - 1];
			summatory += newValues[k];
		}

		summatory = (1.0 / lenghtOfCycle) * summatory;
		return summatory;
	}

	/**
	 * Right Riemann sum defined in Elbasha 2016
	 *
	 * @param values        Array of values where each value represents the value of the cycle defined by the index in the array
	 * @param lenghtOfCycle Number of cycles in a year (For example, with monthly cycles, n = 12)
	 * @return result of applying the Right Riemann sum
	 */
	public static double applyRightRiemannSum(double[] values, int lenghtOfCycle) {
		double[] newValues = new double[values.length];
		double summatory = 0;
		int numberOfCycles = values.length / lenghtOfCycle;
		for (int k = 1; k < numberOfCycles; k++) {
			newValues[k] = values[k];
			summatory += values[k];
		}

		summatory = (1.0 / lenghtOfCycle) * summatory;
		return summatory;
	}

	/**
	 * Half-cycle correction: Trapezoidal rule defined in Elbasha 2016 (is the same than the life-table method)
	 *
	 * @param values        Array of values where each value represents the value of the cycle defined by the index in the array
	 * @param lenghtOfCycle Number of cycles in a year (For example, with monthly cycles, n = 12)
	 * @return result of applying the trapezoidal rule
	 */
	public static double applyTrapezoidalRule(double[] values, int lenghtOfCycle) {
		double[] newValues = new double[values.length];
		double summatory = 0;
		int numberOfCycles = values.length / lenghtOfCycle;
		for (int k = 1; k < numberOfCycles; k++) {
			newValues[k] = (1.0 / (2 * lenghtOfCycle)) * (values[k - 1] + values[k]);
			summatory += newValues[k];
		}
		return summatory;
	}

	/**
	 * Half-cycle correction: Composite Simpson’s 1/3rd Rule defined in Elbasha 2016
	 * This correction requires that the total number of subintervals or time horizon to be even.
	 *
	 * @param values        Array of values where each value represents the value of the cycle defined by the index in the array
	 * @param lenghtOfCycle Number of cycles in a year (For example, with monthly cycles, n = 12)
	 * @return result of applying the Composite Simpson’s 1/3rd Rule
	 * @throws Exception Exception
	 */
	public static double applyCompositeSimpsonsOneThirdRule(double[] values, int lenghtOfCycle) throws Exception {
		if ((values.length * lenghtOfCycle) % 2 == 0 || (values.length * lenghtOfCycle) <= 1) {
			throw new Exception("The total number of subintervals or time horizon is not even.");
		}
		double[] newValues = new double[values.length];
		double summatory = 0;
		int numberOfCycles = values.length / lenghtOfCycle;
		for (int k = 1; k < numberOfCycles; k += 2) {
			newValues[k] = (1.0 / (3 * lenghtOfCycle)) * (values[k - 1] + 4 * values[k] + values[k + 1]);
			summatory += newValues[k];
		}
		return summatory;
	}

	/**
	 * Half-cycle correction: Composite Simpson’s 3/8th Rule defined in Elbasha 2016
	 * This correction requires that the total number of subintervals is multiple of three.
	 *
	 * @param values        Array of values where each value represents the value of the cycle defined by the index in the array
	 * @param lenghtOfCycle Number of cycles in a year (For example, with monthly cycles, n = 12)
	 * @return result of applying the Composite Simpson’s 3/8th Rule
	 * @throws Exception Exception
	 */
	public static double applyCompositeSimpsonsThreeEighthsRule(double[] values, int lenghtOfCycle) throws Exception {
		if ((values.length * lenghtOfCycle - 1) % 3 != 0) {
			throw new Exception("The total number of subintervals or time horizon is not multiple of three.");
		}
		double[] newValues = new double[values.length];
		double summatory = 0;
		int numberOfCycles = values.length / lenghtOfCycle;
		for (int k = 0; k < numberOfCycles - 3; k += 3) {
			newValues[k] = (3.0 / (8 * lenghtOfCycle)) * (
					values[k] + 3 * values[k + 1] + 3 * values[k + 2] + values[k + 3]
			);
			summatory += newValues[k];
		}
		return summatory;
	}

}
