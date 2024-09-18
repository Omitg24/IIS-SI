/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.inference.variableElimination.operation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.openmarkov.core.exception.CostEffectivenessException;
import org.openmarkov.core.model.network.CEP;
import org.openmarkov.core.model.network.Criterion;
import org.openmarkov.core.model.network.Criterion.CECriterion;
import org.openmarkov.core.model.network.Node;
import org.openmarkov.core.model.network.State;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.VariableType;
import org.openmarkov.core.model.network.potential.DeltaPotential;
import org.openmarkov.core.model.network.potential.Potential;
import org.openmarkov.core.model.network.potential.StrategyTree;
import org.openmarkov.core.model.network.potential.operation.DiscretePotentialOperations;
import org.openmarkov.core.model.network.potential.treeadd.TreeADDBranch;
import org.openmarkov.core.model.network.potential.treeadd.TreeADDPotential;

/**
 * @author Manuel Arias
 */
public class CEBaseOperations {

	// Constants
	private static final double defaultMaxThreshold = Double.POSITIVE_INFINITY;

	private static final double defaultMinThreshold = 0;
	private static final double SMALLEST_INTERVAL_FRACTION = 1000; // 1/1.000
	// Global variables
	public static double maxThreshold = defaultMaxThreshold;

	// Static methods
	public static double minThreshold = defaultMinThreshold;

	/**
	 * Algorithm 3: Maximize a set of CEPartitions performing a deterministic CEA (Algorithm 1)
	 * in each sub-interval. In the process it creates more intervals and also joins contiguous
	 * intervals with the same intervention.
	 *
	 * @param decisionVariable <code>Variable</code>
	 * @param partitions       <code>List</code> of <code>CEPartitionPotential</code>
	 * @throws CostEffectivenessException CostEffectivenessException
	 * @return <code>CEPartitionPotential</code>
	 */
	public static CEP optimalCEP(Variable decisionVariable, List<CEP> partitions) throws CostEffectivenessException {
		CEP result;
		int i;
		for (i = 0; i < partitions.size() && partitions.get(i).isZero(); i++)
			;
		if (i == partitions.size()) {
			result = CEP.getZeroPartition();
		} else {
			double[] allThresholds = getUnionThresholds(partitions, null);
			int numIntervals = allThresholds.length + 1;
			double[][] costs = new double[numIntervals][];
			double[][] effectivities = new double[numIntervals][];
			StrategyTree[][] strategyTrees = new StrategyTree[numIntervals][];
			double leftLambdaThreshold = minThreshold;
			double rightLambdaThreshold, middleLambda;
			int numPartitions = partitions.size();
			CEP[] partitionsFromDeterministicCEA = new CEP[numIntervals];
			for (int interval = 0; interval < numIntervals; interval++) {// For each interval
				// Create parameters to invoke deterministicCEA
				if (interval < allThresholds.length) {
					rightLambdaThreshold = allThresholds[interval];
				} else {
					rightLambdaThreshold = partitions.get(0).getMaxThreshold();
				}
				middleLambda = (leftLambdaThreshold + rightLambdaThreshold) / 2;
				strategyTrees[interval] = new StrategyTree[numPartitions];
				costs[interval] = new double[numPartitions];
				effectivities[interval] = new double[numPartitions];
				// For each partition in interval, get the cost, effectivities and interventions to do a deterministic CEA
				for (int subPartitionIndex = 0; subPartitionIndex < numPartitions; subPartitionIndex++) {
					CEP thisPartition = partitions.get(subPartitionIndex);
					// subPartitionIndex is the decision value of each branch
					StrategyTree strategyTree = thisPartition.getIntervention(middleLambda);
					if (strategyTree == null) { // First decision. Create a DeltaPotential
						List<Variable> variables = new ArrayList<Variable>(1);
						variables.add(decisionVariable);
						strategyTree = new StrategyTree(decisionVariable,
								decisionVariable.getStates()[subPartitionIndex]);
					} else { // There is a next decision, referenced by a TreeADDPotential with a single TreeADDBranch
						List<State> branchStates = new ArrayList<State>(1);
						branchStates.add(decisionVariable.getStates()[subPartitionIndex]);
						List<Variable> variables = new ArrayList<Variable>(strategyTree.getVariables());
						variables.add(decisionVariable);
						TreeADDBranch branch = new TreeADDBranch(branchStates, decisionVariable, strategyTree,
								variables);
						StrategyTree newStrategyTree = new StrategyTree(decisionVariable);
						newStrategyTree.addBranch(branch);
						strategyTree = newStrategyTree;
					}
					strategyTrees[interval][subPartitionIndex] = strategyTree;
					costs[interval][subPartitionIndex] = thisPartition.getCost(middleLambda);
					effectivities[interval][subPartitionIndex] = thisPartition.getEffectiveness(middleLambda);
				}
				// Deterministic CEA
				CEP intervalPartition = deterministicCEA(strategyTrees[interval], costs[interval],
						effectivities[interval]);
				partitionsFromDeterministicCEA[interval] = cutPartition(intervalPartition, leftLambdaThreshold,
						rightLambdaThreshold);
				leftLambdaThreshold = rightLambdaThreshold;
			}
			result = joinContiguousIntervals(partitionsFromDeterministicCEA, allThresholds);
		}

		result = removeZeroLengthIntervals(result);
		return result;
	}

	private static CEP removeZeroLengthIntervals(CEP result) throws CostEffectivenessException {
		CEP newResult = result;
		// Get intervals whose length is 0 or close to 0

		double[] thresholds = result.getThresholds();
		if (thresholds == null) {
			return result;
		}
		double closeToZeroIntervalLength = getCloseToZeroIntervalLength(thresholds, result.getMinThreshold(),
				result.getMaxThreshold());
		boolean[] intervalsToRemove = getIntervalsToRemove(thresholds, result.getMinThreshold(),
				result.getMaxThreshold(), closeToZeroIntervalLength);
		int numIntervalsToRemove = countIntervalsToRemove(intervalsToRemove);
		int numNewThresholds = thresholds.length - numIntervalsToRemove;
		int numNewIntervals = numNewThresholds + 1;
		if (numIntervalsToRemove > 0) { // Create new CEP
			StrategyTree[] newStrategyTrees = new StrategyTree[numNewIntervals];
			StrategyTree[] oldStrategyTrees = result.getStrategyTrees();
			double[] newCosts = new double[numNewIntervals];
			double[] newEffectivities = new double[numNewIntervals];
			double[] newThresholds = new double[numNewThresholds];
			int newInterval = 0;
			int numOldIntervals = thresholds.length + 1;
			int actualThreshold = 0;
			for (int oldInterval = 0; oldInterval < numOldIntervals; oldInterval++) {
				if (!intervalsToRemove[oldInterval]) {
					newCosts[newInterval] = result.getCost(oldInterval);
					newEffectivities[newInterval] = result.getEffectiveness(oldInterval);
					newStrategyTrees[newInterval] = oldStrategyTrees[oldInterval];
					if (numNewThresholds - actualThreshold > 0) {
						newThresholds[actualThreshold] = thresholds[oldInterval];
						actualThreshold++;
					}
					newInterval++;
				}
			}
			newResult = new CEP(newStrategyTrees, newCosts, newEffectivities, newThresholds, result.getMinThreshold(),
					result.getMaxThreshold());
		}
		return newResult;
	}

	/**
	 * Given an intervention and two limits for the lambda thresholds (left and right),
	 * cuts the sub-intervals outside the limits.
	 *
	 * @param unCuttedPartition <code>CEPartitionPotential</code>
	 * @param minThreshold      <code>double</code>
	 * @param maxThreshold      <code>double</code>
	 * @throws CostEffectivenessException
	 */
	private static CEP cutPartition(CEP unCuttedPartition, double minThreshold, double maxThreshold)
			throws CostEffectivenessException {
		// Get the first interval: threshold[firstInterval] > leftLambdaThresholds
		int firstInterval = 0;
		int lastInterval = 0;
		double[] thresholdsUncutted = unCuttedPartition.getThresholds();
		if (thresholdsUncutted.length > 0) {
			while (firstInterval < thresholdsUncutted.length && thresholdsUncutted[firstInterval] < minThreshold) {
				firstInterval++;
			}
			// Get the last interval
			lastInterval = firstInterval;
			while (lastInterval < thresholdsUncutted.length && thresholdsUncutted[lastInterval] < maxThreshold) {
				lastInterval++;
			}
		}
		// Copy costs, effectivities, interventions and thresholds into smaller partition.
		double[] costs;
		double[] effectivities;
		int numIntervals = lastInterval - firstInterval + 1;
		StrategyTree[] strategyTrees = new StrategyTree[numIntervals];
		double[] thresholds = new double[numIntervals - 1];
		int numIntervalsUncuttedPartition = unCuttedPartition.getNumIntervals();
		CEP partition;
		if (numIntervalsUncuttedPartition > numIntervals) {
			costs = new double[numIntervals];
			effectivities = new double[numIntervals];
			strategyTrees = new StrategyTree[numIntervals];
			for (int i = 0; i < numIntervals; i++) {
				costs[i] = unCuttedPartition.getCost(firstInterval + i);
				effectivities[i] = unCuttedPartition.getEffectiveness(firstInterval + i);
				strategyTrees[i] = unCuttedPartition.getIntervention(firstInterval + i);
			}
			if (numIntervals > 1) {
				for (int i = 0; i < thresholds.length; i++) {
					thresholds[i] = thresholdsUncutted[firstInterval + i];
				}
			}
			partition = new CEP(strategyTrees, costs, effectivities, thresholds, minThreshold, maxThreshold);
		} else {
			partition = new CEP(unCuttedPartition.getStrategyTrees(), unCuttedPartition.getCosts(),
					unCuttedPartition.getEffectivities(), unCuttedPartition.getThresholds(), minThreshold,
					maxThreshold);
		}

		return partition;
	}

	/**
	 * Joins contiguous intervals with the same intervention.
	 *
	 * @param partitions    is a list of contiguous partitions. <code>CEPartitionPotential[]</code>
	 * @param allThresholds <code>double[]</code>
	 * @return <code>CEPartitionPotential</code>
	 * @throws CostEffectivenessException
	 */
	private static CEP joinContiguousIntervals(CEP[] partitions, double[] allThresholds)
			throws CostEffectivenessException {
		CEP unionPartition = joinPartitions(partitions, allThresholds);
		int numIntervalsUnionPartition = unionPartition.getNumIntervals();
		boolean[] thresholdsToRemove = new boolean[numIntervalsUnionPartition - 1];
		int numThresholdsToRemove = 0;

		// Get a list of thresholds to remove, that is, intervals that can be joined.
		double previousCost = unionPartition.getCost(0);
		double previousEffectiveness = unionPartition.getEffectiveness(0);
		StrategyTree previousStrategyTree = unionPartition.getIntervention(0);
		for (int i = 1; i < numIntervalsUnionPartition; i++) {
			double actualCost = unionPartition.getCost(i);
			double actualEffectiveness = unionPartition.getEffectiveness(i);
			StrategyTree actualStrategyTree = unionPartition.getIntervention(i);
			if (DiscretePotentialOperations.almostEqual(actualCost, previousCost) && DiscretePotentialOperations
					.almostEqual(actualEffectiveness, previousEffectiveness) && (
					actualStrategyTree == previousStrategyTree | (
							actualStrategyTree != null && previousStrategyTree != null && actualStrategyTree
									.equals(previousStrategyTree)
					)
			)) {
				thresholdsToRemove[i - 1] = true;
				numThresholdsToRemove++;
			} else {
				previousCost = actualCost;
				previousEffectiveness = actualEffectiveness;
				previousStrategyTree = actualStrategyTree;
			}
		}

		// Create new partition in two steps
		// 1) Create data
		int numIntervalsJoinedPartition = numIntervalsUnionPartition - numThresholdsToRemove;
		double[] costs = new double[numIntervalsJoinedPartition];
		double[] effectivities = new double[numIntervalsJoinedPartition];
		StrategyTree[] strategyTrees = new StrategyTree[numIntervalsJoinedPartition];
		double[] thresholds = new double[numIntervalsJoinedPartition - 1];

		// 2) Fill data
		int indexJoinedPartition = 0;
		for (int i = 0; i < numIntervalsUnionPartition; i++) {
			costs[indexJoinedPartition] = unionPartition.getCost(i);
			effectivities[indexJoinedPartition] = unionPartition.getEffectiveness(i);
			strategyTrees[indexJoinedPartition] = unionPartition.getIntervention(i);
			if (i < numIntervalsUnionPartition - 1) {
				if (!thresholdsToRemove[i]) {
					thresholds[indexJoinedPartition] = unionPartition.getThreshold(i);
					indexJoinedPartition++;
				}
			}
		}

		return new CEP(strategyTrees, costs, effectivities, thresholds);
	}

	/**
	 * Creates a single partition from several contiguous partitions.
	 *
	 * @param partitions    <code>CEPartitionPotential[]</code>
	 * @param allThresholds marks the end of partition i and the beginning of partition i+1. <code>double[]</code>
	 * @return <code>CEPartitionPotential</code>
	 * @throws CostEffectivenessException
	 */
	private static CEP joinPartitions(CEP[] partitions, double[] allThresholds) throws CostEffectivenessException {
		int numIntervals = getNumIntervals(partitions, allThresholds);
		// Get intervals whose length is 0 or close to 0
		//        double closeToZeroIntervalLength = getCloseToZeroIntervalLength(allThresholds);
		//        boolean[] thresholdToRemove = getThresholdsToRemove(allThresholds, closeToZeroIntervalLength);
		//        int numThresholdsToRemove = countThresholdsToRemove(thresholdToRemove);
		// Create CEP
		//        numIntervals -= numThresholdsToRemove;
		double[] costs = new double[numIntervals];
		double[] effectivities = new double[numIntervals];
		StrategyTree[] strategyTrees = new StrategyTree[numIntervals];
		double[] thresholds = new double[numIntervals - 1];
		CEP partition = new CEP(strategyTrees, costs, effectivities, thresholds);
		// Fill CEP
		int partitionInterval = 0;
		int allPartitionsIndex = 0;
		for (int partitionIndex = 0; partitionIndex < partitions.length; partitionIndex++) {
			CEP subPartition = partitions[partitionIndex];
			int numIntervalsSubPartition = subPartition.getNumIntervals();
			for (int subIntervalIndex = 0; subIntervalIndex < numIntervalsSubPartition; subIntervalIndex++) {
				//			    if (allPartitionsIndex == 0 || !thresholdToRemove[allPartitionsIndex-1]) {
				costs[partitionInterval] = subPartition.getCost(subIntervalIndex);
				effectivities[partitionInterval] = subPartition.getEffectiveness(subIntervalIndex);
				strategyTrees[partitionInterval] = subPartition.getIntervention(subIntervalIndex);
				if (subIntervalIndex < numIntervalsSubPartition - 1) {
					thresholds[partitionInterval] = subPartition.getThreshold(subIntervalIndex);
				}
				partitionInterval++;
			}
			//                allPartitionsIndex++;
			//			}
			if (partitionIndex < partitions.length - 1) {
				thresholds[partitionInterval - 1] = allThresholds[partitionIndex];
			}
		}
		return partition;
	}

	private static int countIntervalsToRemove(boolean[] intervalsToRemove) {
		int count = 0;
		for (int i = 0; i < intervalsToRemove.length; i++) {
			if (intervalsToRemove[i]) {
				count++;
			}
		}
		return count;
	}

	private static double getCloseToZeroIntervalLength(double[] allThresholds, double minThreshold,
			double maxThreshold) {
		//	    double max = minThreshold,
		//                min = maxThreshold;
		//        max = minThreshold > max && minThreshold < Double.POSITIVE_INFINITY ? minThreshold : max;
		//        min = minThreshold < min && minThreshold > Double.NEGATIVE_INFINITY ? minThreshold : min;
		//	    for (int i = 0; i < allThresholds.length; i++) {
		//	        max = allThresholds[i] > max && allThresholds[i] < Double.POSITIVE_INFINITY ? allThresholds[i] : max;
		//            min = allThresholds[i] < min && allThresholds[i] > Double.NEGATIVE_INFINITY ? allThresholds[i] : min;
		//        }
		//        max = maxThreshold > max && maxThreshold < Double.POSITIVE_INFINITY ? maxThreshold : max;
		//        min = maxThreshold < min && maxThreshold > Double.NEGATIVE_INFINITY ? maxThreshold : min;
		double length = 1.0 / SMALLEST_INTERVAL_FRACTION;
		//        if (max != Double.NEGATIVE_INFINITY && min != Double.POSITIVE_INFINITY && max != Double.POSITIVE_INFINITY && min != Double.NEGATIVE_INFINITY) {
		//        	length = (max - min) / SMALLEST_INTERVAL_FRACTION;
		//		}
		return length;
	}

	private static boolean[] getIntervalsToRemove(double[] allThresholds, double minThreshold, double maxThreshold,
			double closeToZeroIntervalLength) {
		int numIntervals = allThresholds.length + 1;
		boolean[] intervalsToRemove = new boolean[numIntervals];
		if (numIntervals > 1) {
			intervalsToRemove[0] = (allThresholds[0] - minThreshold) < closeToZeroIntervalLength;
			for (int i = 1; i < numIntervals - 1; i++) {
				intervalsToRemove[i] = (allThresholds[i] - allThresholds[i - 1]) < closeToZeroIntervalLength;
			}
			intervalsToRemove[numIntervals - 1] = (maxThreshold - allThresholds[allThresholds.length - 1])
					< closeToZeroIntervalLength;
		}
		return intervalsToRemove;
	}

	/**
	 * Given a set of contiguous partitions, calculates the number of intervals.
	 *
	 * @param partitions    <code>CEPartitionPotential[]</code>
	 * @param allThresholds marks the end of partition i and the beginning of partition i+1. <code>double[]</code>
	 * @return <code>int</code>
	 */
	private static int getNumIntervals(CEP[] partitions, double[] allThresholds) {
		int numIntervals = 0;
		for (int i = 0; i < partitions.length; i++) {
			numIntervals += partitions[i].getNumIntervals();
		}
		return numIntervals;
	}

	/**
	 * Algorithm 1: creates a CEP given a set of interventions with its costs and effectivities
	 *
	 * @param strategyTrees
	 * @param costs
	 * @param effectivities
	 * @param minThreshold
	 * @param maxThreshold
	 * @return A Cost-Effectiveness Partition.
	 * The size of arrays should be the same for all the arrays.
	 */
	public static CEP deterministicCEA(StrategyTree[] strategyTrees, double[] costs, double[] effectivities,
			double minThreshold, double maxThreshold) throws CostEffectivenessException {
		CEP partition = deterministicCEA(strategyTrees, costs, effectivities);
		return cutPartition(partition, minThreshold, maxThreshold);
	}

	/**
	 * Algorithm 1: creates a CEP given a set of interventions.
	 *
	 * @param strategyTrees <code>StrategyTree[]</code>
	 * @param costs         <code>double[]</code>
	 * @param effectivities <code>double[]</code>
	 * @return A Cost-Effectiveness Partition. <code>CEPartitionPotential</code>
	 * All the arrays should have the same size.
	 */
	public static CEP deterministicCEA(StrategyTree[] strategyTrees, double[] costs, double[] effectivities)
			throws CostEffectivenessException {
		int sigmaIminus1 = 0, sigmaI;
		double minCost = Double.POSITIVE_INFINITY;

		// Get sigma[0] (we call it sigmaIminus1)
		for (int i = 0; i < costs.length; i++) {
			if (costs[i] < minCost || (
					costs[i] == minCost && (
							i > 0 || effectivities[i] > effectivities[sigmaIminus1]
					)
			)) {
				minCost = costs[i];
				sigmaIminus1 = i;
			}
		}

		// Get possible interventions (effectiveness_i > effectiveness_[sigma0])
		boolean[] possibleInterventions = new boolean[strategyTrees.length];
		for (int i = 0; i < strategyTrees.length; i++) {
			if (effectivities[i] > effectivities[sigmaIminus1]) {
				possibleInterventions[i] = true;
			}
		}
		List<Integer> sigmasList = new ArrayList<Integer>();
		sigmasList.add(sigmaIminus1);
		double costSigmaIminus1 = costs[sigmaIminus1];
		double effectivenessSigmaIminus1 = effectivities[sigmaIminus1];

		while (atLeastOneTrue(possibleInterventions)) {
			sigmaI = argMinICER(costSigmaIminus1, effectivenessSigmaIminus1, costs, effectivities,
					possibleInterventions);
			sigmasList.add(sigmaI);
			removeDominatedInterventions(possibleInterventions, effectivities, sigmaI);
			sigmaIminus1 = sigmaI;
			costSigmaIminus1 = costs[sigmaIminus1];
			effectivenessSigmaIminus1 = effectivities[sigmaIminus1];
		}

		// Create CEP partition
		int numInterventions = sigmasList.size();
		StrategyTree[] interventionsCEP = new StrategyTree[numInterventions];
		double[] costsCEP = new double[numInterventions];
		double[] effectivitiesCEP = new double[numInterventions];
		double[] thresholds = new double[numInterventions - 1];
		int sigmasElement = sigmasList.get(0);
		interventionsCEP[0] = strategyTrees[sigmasElement];
		costsCEP[0] = costs[sigmasElement];
		effectivitiesCEP[0] = effectivities[sigmasElement];
		for (int i = 1; i < numInterventions; i++) {
			sigmasElement = sigmasList.get(i);
			interventionsCEP[i] = strategyTrees[sigmasElement];
			costsCEP[i] = costs[sigmasElement];
			effectivitiesCEP[i] = effectivities[sigmasElement];
			thresholds[i - 1] = ICER(costsCEP[i - 1], effectivitiesCEP[i - 1], costsCEP[i], effectivitiesCEP[i]);
		}
		return new CEP(interventionsCEP, costsCEP, effectivitiesCEP, thresholds);
	}

	/**
	 * @param booleanVector <code>Array</code> of <code>boolean[]</code>
	 * @return <code>true</code> if at least one boolean in the array is true. <code>boolean</code>
	 */
	private static boolean atLeastOneTrue(boolean[] booleanVector) {
		int i = 0;
		boolean truthValue;
		do {
			truthValue = booleanVector[i++];
		} while (!truthValue && i < booleanVector.length);
		return truthValue;
	}

	/**
	 * @param costSigmaMinus1          <code>double</code>
	 * @param effectivenessSigmaMinus1 <code>double</code>
	 * @param costs                    <code>double[]</code>
	 * @param effectivities            <code>double[]</code>
	 * @param possibleInterventions    <code>boolean[]</code>
	 * @return intervention number with minimum ICER regarding the intervention parameter. <code>int</code>
	 */
	private static int argMinICER(double costSigmaMinus1, double effectivenessSigmaMinus1, double[] costs,
			double[] effectivities, boolean[] possibleInterventions) {
		int argMinICER = -1;
		double minICER = Double.POSITIVE_INFINITY;
		for (int i = 0; i < possibleInterventions.length; i++) {
			if (possibleInterventions[i]) {
				double ICER = ICER(costSigmaMinus1, effectivenessSigmaMinus1, costs[i], effectivities[i]);
				if (ICER < minICER) {
					minICER = ICER;
					argMinICER = i;
				}
			}
		}
		return argMinICER;
	}

	/**
	 * Calculates de Incremental Cost-Effectiveness Ratio between two interventions (1 and 2) given their costs and
	 * effectivities.
	 *
	 * @param cost1          <code>double</code>
	 * @param effectiveness1 <code>double</code>
	 * @param cost2          <code>double</code>
	 * @param effectiveness2 <code>double</code>
	 */
	private static double ICER(double cost1, double effectiveness1, double cost2, double effectiveness2) {
		double ICER;
		if (effectiveness1 == effectiveness2) {
			if (cost2 - cost1 > 0)
				ICER = Double.POSITIVE_INFINITY;
			else
				ICER = Double.NEGATIVE_INFINITY; // Unreachable
		} else {
			ICER = (cost2 - cost1) / (effectiveness2 - effectiveness1);
		}
		return ICER;
	}

	/**
	 * Remove interventions, modifying a boolean vector, whose effectiveness is smaller than other intervention
	 *
	 * @param possibleInterventions <code>boolean[]</code>
	 * @param effectivities         <code>double[]</code>
	 * @param sigmaI                <code>int</code>
	 */
	private static void removeDominatedInterventions(boolean[] possibleInterventions, double[] effectivities,
			int sigmaI) {
		possibleInterventions[sigmaI] = false;
		double effectivenessSigmaI = effectivities[sigmaI];
		for (int i = 0; i < possibleInterventions.length; i++) {
			if (possibleInterventions[i] && effectivenessSigmaI >= effectivities[i]) {
				possibleInterventions[i] = false;
			}
		}
	}

	/**
	 * Algorithm 2: Adds a collection of CEPs into a single one.
	 * Each partition has a weight in the probabilities parameter.
	 *
	 * @param partitions     <code>List</code> of <code>CEPartitionPotential</code>
	 * @param chanceVariable <code>Variable</code>
	 * @param probabilities  <code>double[]</code>
	 * @return Weighted CEPartitionPotential. <code>CEPartitionPotential</code>
	 * @throws CostEffectivenessException When the number of states in <code>variable</code>
	 *                                    is distinct than the length of <code>probabilities</code>.
	 */
	public static CEP weightedAverage(List<CEP> partitions, Variable chanceVariable, double[] probabilities)
			throws CostEffectivenessException {
		int numPartitions = partitions.size();
		boolean[] takeIntoAccount = new boolean[numPartitions];
		for (int i = 0; i < numPartitions; i++) {
			if (probabilities == null || (probabilities != null && probabilities[i] != 0.0)) {
				takeIntoAccount[i] = true;
			}
		}
		double[] thresholds = getUnionThresholds(partitions, probabilities);
		double[] costs = new double[thresholds.length + 1];
		double[] effectivities = new double[thresholds.length + 1];
		StrategyTree[] strategyTrees = new StrategyTree[thresholds.length + 1];
		double previousThreshold = minThreshold;
		for (int interval = 0; interval < thresholds.length + 1; interval++) {
			double nextThreshold;
			if (interval < thresholds.length) {
				nextThreshold = thresholds[interval];
			} else {
				if (thresholds.length > 0) {
					nextThreshold = thresholds[interval - 1] + 1.0;
				} else {
					nextThreshold = previousThreshold + 1.0;
				}
			}
			double medium = (previousThreshold + nextThreshold) / 2;
			StrategyTree lastStrategyTree = null;
			boolean distinctInterventions = false;
			StrategyTree[] newStrategyTrees = new StrategyTree[takeIntoAccount.length];
			double accumulatedCost = 0;
			double accumulatedEff = 0;
			for (int i = 0; i < takeIntoAccount.length; i++) {
				CEP partition = partitions.get(i);
				if (takeIntoAccount[i]) {// Remove partitions with zero probability
					newStrategyTrees[i] = partition.getIntervention(medium);
					double cost = partition.getCost(medium);
					double effectiveness = partition.getEffectiveness(medium);
					accumulatedCost += cost * probabilities[i];
					accumulatedEff += effectiveness * probabilities[i];
					if (lastStrategyTree == null) {
						lastStrategyTree = newStrategyTrees[i];
					} else {
						distinctInterventions |= !lastStrategyTree.equals(newStrategyTrees[i]);
					}
				}
			}
			costs[interval] = accumulatedCost;
			effectivities[interval] = accumulatedEff;
			if (!distinctInterventions) {
				strategyTrees[interval] = lastStrategyTree;
			} else { // Create a TreeADDPotential
				// All the interventions in a CEP have distinct values but the same chance variable
				StrategyTree newStrategyTree = StrategyTree
						.averageOfInterventions(chanceVariable, probabilities, newStrategyTrees);
				strategyTrees[interval] = newStrategyTree;
			}
			previousThreshold = nextThreshold;
		}

		return new CEP(strategyTrees, costs, effectivities, thresholds);
	}

	/**
	 * @param partitions    <code>List</code> of <code>CEPartitionPotential</code>
	 * @param probabilities This array can be <code>null</code>. <code>double[]</code>.
	 * @return The union of all the thresholds removing being equal. <code>double[]</code>
	 */
	public static double[] getUnionThresholds(List<CEP> partitions, double[] probabilities) {
		int[] indexes = new int[partitions.size()];
		double[][] thresholds = new double[indexes.length][];
		for (int i = 0; i < indexes.length; i++) {
			// When probabilities = null, all partitions must be taken into account.
			if (probabilities == null || probabilities[i] != 0.0) {
				thresholds[i] = partitions.get(i).getThresholds();
			} else {
				indexes[i] = Integer.MAX_VALUE;
			}
		}
		List<Double> listUnionThresholds = new ArrayList<Double>();
		double threshold;
		while ((threshold = getMinThreshold(thresholds, indexes, probabilities)) != maxThreshold) {
			listUnionThresholds.add(threshold);
		}
		double[] unionThresholds = new double[listUnionThresholds.size()];
		for (int i = 0; i < unionThresholds.length; i++)
			unionThresholds[i] = listUnionThresholds.get(i);
		return unionThresholds;
	}

	/**
	 * Given a list of ordered lists of thresholds, and a list of pointers (indexes) to elements of each list,
	 * the method chooses the minimum of the pointed elements and increments the corresponding index. In case that
	 * exists one or more elements equal to the selected one in other lists, it increases also the indexes of those
	 * lists.
	 *
	 * @param thresholds    List of lists of thresholds, each list corresponds to a <code>CEPartitionPotential</code>. <code>double[][]</code>
	 * @param indexes       List of indexes to the thresholds lists. In each list the elements on the left. <code>int[]</code>
	 * @param probabilities <code>double[]</code>
	 * @return Minimal threshold <code>double</code>
	 */
	private static double getMinThreshold(double[][] thresholds, int[] indexes, double[] probabilities) {
		double minThreshold = Double.POSITIVE_INFINITY;
		int thresholdIndex = -1;
		for (int i = 0; i < indexes.length; i++) {
			if ((probabilities == null || probabilities[i] != 0.0)
					&& indexes[i] < thresholds[i].length) {// Threshold list not empty
				double candidate = thresholds[i][indexes[i]];
				if (candidate < minThreshold) {
					thresholdIndex = i;
					minThreshold = candidate;
				} else if (candidate == minThreshold && thresholdIndex != -1) {    // Other equal threshold.
					indexes[thresholdIndex] = indexes[thresholdIndex] + 1;// Remove it 
					thresholdIndex = i;
				}
			}
		}
		if (thresholdIndex != -1) {
			indexes[thresholdIndex] = indexes[thresholdIndex] + 1;// Remove the repeated threshold
		}
		return minThreshold;
	}

	/**
	 * Compares the content of two potentials.
	 *
	 * @param firstPotential  <code>Potential</code>
	 * @param secondPotential <code>Potential</code>
	 * @return boolean
	 * @throws CostEffectivenessException
	 */
	public static boolean samePotentials(Potential firstPotential, Potential secondPotential)
			throws CostEffectivenessException {
		boolean same = false;
		if (firstPotential == secondPotential) {
			same = true;
		} else {
			if (firstPotential.getClass() == secondPotential.getClass()) {
				if (firstPotential.getClass() == DeltaPotential.class) { // Compare two DeltaPotentials
					// To avoid castings
					DeltaPotential firstDelta = (DeltaPotential) firstPotential;
					DeltaPotential secondDelta = (DeltaPotential) secondPotential;
					if (firstDelta.getState() == secondDelta.getState() && firstDelta.getNumVariables() == secondDelta
							.getNumVariables() && firstDelta.getVariables().containsAll(secondDelta.getVariables())) {
						same = true;
					}
				} else if (firstPotential.getClass() == TreeADDPotential.class) { // Compare two TreeADDPotentials
					// To avoid castings
					TreeADDPotential firstTree = (TreeADDPotential) firstPotential;
					TreeADDPotential secondTree = (TreeADDPotential) secondPotential;
					if (firstTree.getRootVariable() == secondTree.getRootVariable()) {
						List<TreeADDBranch> firstBranches = firstTree.getBranches();
						List<TreeADDBranch> secondBranches = secondTree.getBranches();
						if (firstBranches.size() == firstBranches.size()) {
							List<Variable> firstVariables = firstTree.getVariables();
							List<Variable> secondVariables = secondTree.getVariables();
							if (firstVariables.size() == secondVariables.size() && firstVariables
									.containsAll(secondVariables)) {
								same = sameBranchesCollections(firstBranches, secondBranches);
							}
						}
					}
				} else { // Add here more potentials and let the exception in the next else.
					throw new CostEffectivenessException("Unsupported potential type.");
				}
			}
		}
		return same;
	}

	/**
	 * Compare two collection of branches
	 *
	 * @param firstBranches  <code>List</code> of <code>TreeADDBranch</code>
	 * @param secondBranches <code>List</code> of <code>TreeADDBranch</code>
	 * @return <code>true</code> if both list are equal.
	 * @throws CostEffectivenessException
	 */
	private static boolean sameBranchesCollections(List<TreeADDBranch> firstBranches,
			List<TreeADDBranch> secondBranches) throws CostEffectivenessException {
		boolean foundEqualBranches = true;
		int numBranches = firstBranches.size();
		for (int i = 0; i < numBranches && foundEqualBranches; i++) {
			TreeADDBranch firstBranch = firstBranches.get(i);
			boolean sameBranch = false;
			for (TreeADDBranch secondBranch : secondBranches) {
				sameBranch |= sameBranches(firstBranch, secondBranch);
			}
			if (!sameBranch) {
				foundEqualBranches = false;
			}
		}
		return foundEqualBranches;
	}

	/**
	 * Compare two branches.
	 *
	 * @param firstBranch   <code>TreeADDBranch</code>
	 * @param secondBbranch <code>TreeADDBranch</code>
	 * @return <code>true</code> if both branches are equal.
	 * @throws CostEffectivenessException
	 */
	private static boolean sameBranches(TreeADDBranch firstBranch, TreeADDBranch secondBbranch)
			throws CostEffectivenessException {
		boolean same = true;
		Variable rootVariable = firstBranch.getRootVariable();
		same &= rootVariable == secondBbranch.getRootVariable();
		same &= firstBranch.getReference() == secondBbranch.getReference();
		if (same && rootVariable.getVariableType() == VariableType.NUMERIC) {
			same &= firstBranch.getLowerBound() == secondBbranch.getLowerBound()
					&& firstBranch.getUpperBound() == secondBbranch.getUpperBound();
		}
		if (same) {
			List<Variable> firstVariables = firstBranch.getParentVariables();
			List<Variable> secondVariables = secondBbranch.getParentVariables();
			same &= firstVariables.size() == secondVariables.size() && firstVariables.containsAll(secondVariables);
			if (same) {
				List<State> firstStates = firstBranch.getBranchStates();
				List<State> secondStates = secondBbranch.getBranchStates();
				same &= firstStates.size() == secondStates.size() && firstStates.containsAll(secondStates);
				if (same) {
					Potential firstPotential = firstBranch.getPotential();
					Potential secondPotential = secondBbranch.getPotential();
					same &= (
							(firstPotential != null && secondPotential != null) || (
									firstPotential == null && secondPotential == null
							)
					);
					same &= firstPotential != null && firstPotential.equals(secondPotential);
				}
			}
		}
		return same;
	}

	public static List<Node> getNodesWithGivenCriterion(Collection<Node> nodes, CECriterion criterion) {
		List<Node> nodesWithGivenCriterion = new ArrayList<>();
		for (Node node : nodes) {
			Criterion nodeCriterion = node.getVariable().getDecisionCriterion();
			if (nodeCriterion != null) {
				if (nodeCriterion.getCECriterion() == criterion) {
					nodesWithGivenCriterion.add(node);
				}
			}
		}
		return nodesWithGivenCriterion;
	}

	/**
	 * @param nodes Collection of nodes
	 * @return List of nodes that are cost nodes
	 */
	public static List<Node> getCostNodes(Collection<Node> nodes) {
		List<Node> costNodes = new ArrayList<Node>();
		for (Node node : nodes) {
			if (isCostNode(node)) {
				costNodes.add(node);
			}
		}
		return costNodes;
	}

	public static boolean isCostNode(Node node) {
		return isCostVariable(node.getVariable());
	}

	public static boolean isCostVariable(Variable variable) {
		Criterion criterion = variable.getDecisionCriterion();
		return criterion != null ? criterion.getCECriterion() == CECriterion.Cost : false;
	}

	public static List<Node> getEffectivenessNodes(List<Node> nodes) {
		List<Node> effectivenessNodes = new ArrayList<Node>();
		for (Node node : nodes) {
			if (isEffectivenessNode(node)) {
				effectivenessNodes.add(node);
			}
		}
		return effectivenessNodes;
	}

	public static boolean isEffectivenessNode(Node node) {
		return isEffectivenessVariable(node.getVariable());
	}

	public static boolean isEffectivenessVariable(Variable variable) {
		Criterion criterion = variable.getDecisionCriterion();
		return criterion != null ? criterion.getCECriterion() == CECriterion.Effectiveness : false;
	}

}
