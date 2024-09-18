/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.model.network;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.openmarkov.core.exception.CostEffectivenessException;
import org.openmarkov.core.model.network.potential.StrategyTree;

/**
 * A CEP is a set of <b>n</b> intervals, each one with a cost, an effectiveness and possibly, an intervention.
 * Intervals are separated by <b>n-1</b> thresholds. The whole partition is delimited by the minimum and the maximum
 * threshold.
 *
 * @author Manuel Arias
 */
public class CEP {

	/**
	 * Used to save memory and time in case of partitions corresponding to configurations with zero probability.
	 */
	private static CEP zeroPartition;
	private final double defaultMinimalThreshold = 0.0;
	private final double defaultMaximalThreshold = Double.POSITIVE_INFINITY;
	DecimalFormat decimalFormat3afterComa = new DecimalFormat("#.###");
	DecimalFormat decimalFormat2afterComa = new DecimalFormat("#.##");
	DecimalFormat decimalFormat1afterComa = new DecimalFormat("#.#");
	DecimalFormat decimalFormatNoDecimalsAfterComa = new DecimalFormat("#");
	// Attributes
	private double[] costs;
	private double[] effectivities;
	/**
	 * An intervention is a potential. If it is a decision, its value is a {@code DeltaPotential}, otherwise,
	 * a {@code TreeADDPotential}
	 */
	private StrategyTree[] strategyTrees;

	// Constructors
	/**
	 * Divisions between intervals.
	 */
	private double[] thresholds;
	private double minThreshold;
	private double maxThreshold;
	/**
	 * When true, this partition must not be taken in consideration.
	 */
	private boolean zeroProbability;

	// Methods
	// From this point: set of attributes and methods related with method toString()
	private String indent = "";
	private int indentLevel; // TODO Remove?

	/**
	 * @param strategyTrees {@code Intervention[]}
	 * @param costs         {@code double[]}
	 * @param effectivities {@code double[]}
	 * @param thresholds    {@code double[]}
	 * @throws CostEffectivenessException CostEffectivenessException
	 */
	public CEP(StrategyTree[] strategyTrees, double[] costs, double[] effectivities, double[] thresholds)
			throws CostEffectivenessException {

		if (costs.length == effectivities.length && costs.length == strategyTrees.length) {
			if (costs.length == 1 && thresholds == null) {
				this.thresholds = new double[0];
			} else {
				if (thresholds.length != (costs.length - 1)) {
					throw new CostEffectivenessException("Number of thresholds must be minor in 1 than number of "
							+ "costs, effectivities and interventions.\nNumber of thresholds = " + thresholds.length
							+ "\nNumber of costs, effectivities and interventions = " + costs.length);
				}
				this.thresholds = thresholds;
			}
			this.costs = costs;
			this.effectivities = effectivities;
			this.strategyTrees = strategyTrees;
		} else {
			throw new CostEffectivenessException(
					"Number of cost, effectivities and interventions must be equal.\n" + "Number of cost = "
							+ costs.length + "\nNumber of effectivities = " + effectivities.length
							+ "\nNumber of interventions = " + strategyTrees.length);
		}
		minThreshold = defaultMinimalThreshold;
		maxThreshold = defaultMaximalThreshold;
	}

	/**
	 * Creates a partition with only one interval
	 *
	 * @param strategyTree  {@code Potential}
	 * @param cost          {@code double}
	 * @param effectiveness {@code double}
	 * @param minThreshold  {@code double}
	 * @param maxThreshold  {@code double}
	 * @throws CostEffectivenessException CostEffectivenessException
	 */
	public CEP(StrategyTree strategyTree, double cost, double effectiveness, double minThreshold, double maxThreshold)
			throws CostEffectivenessException {

		costs = new double[] { cost };
		effectivities = new double[] { effectiveness };
		thresholds = new double[0];
		if (strategyTree != null) {
			this.strategyTrees = new StrategyTree[] { strategyTree };
		}
		this.minThreshold = minThreshold;
		this.maxThreshold = maxThreshold;
	}

	/**
	 * @param strategyTrees {@code Potential[]}
	 * @param costs         {@code double[]}
	 * @param effectivities {@code double[]}
	 * @param thresholds    {@code double[]}
	 * @param minThreshold  {@code double}
	 * @param maxThreshold  {@code double}
	 * @throws CostEffectivenessException CostEffectivenessException
	 */
	public CEP(StrategyTree[] strategyTrees, double[] costs, double[] effectivities, double[] thresholds,
			double minThreshold, double maxThreshold) throws CostEffectivenessException {

		this(strategyTrees, costs, effectivities, thresholds);
		this.minThreshold = minThreshold;
		this.maxThreshold = maxThreshold;
	}

	/**
	 * Creates a CEPartition with zero probability
	 */
	private CEP() {
		zeroProbability = true;
	}

	/**
	 * Singleton for partitions with probability zero
	 *
	 * @return CEPartition
	 */
	public static CEP getZeroPartition() {
		if (zeroPartition == null) {
			zeroPartition = new CEP();
		}
		return zeroPartition;
	}

	/**
	 * @param lambda {@code double}
	 * @return Interval index corresponding to lambda. {@code int}
	 */
	public int index(double lambda) {
		int numThresholds = costs.length - 1;
		int i = 0;
		while (i < numThresholds && lambda > thresholds[i])
			i++;
		return i;
	}

	/**
	 * Multiplies costs and effectivities per factor.
	 *
	 * @param factor {@code double}
	 */
	public void multiply(double factor) {
		if (!zeroProbability) {
			for (int i = 0; i < costs.length; i++) {
				costs[i] *= factor;
				effectivities[i] *= factor;
			}
		}
	}

	/**
	 * Divides costs and effectivities per factor.
	 *
	 * @param factor {@code double}
	 */
	public void divide(double factor) {
		if (!zeroProbability) {
			for (int i = 0; i < costs.length; i++) {
				costs[i] /= factor;
				effectivities[i] /= factor;
			}
		}
	}

	/**
	 * Change the indentation in {@code toString()}. Used for nested interventions.
	 *
	 * @param indentLevel {@code int}
	 */
	public void setIndentLevel(int indentLevel) {
		this.indentLevel = indentLevel;
		if (indentLevel == 0) {
			indent = "";
		} else {
			indent = " ";
			for (int i = 1; i < indentLevel; i++)
				indent = indent + " ";
		}
	}

	/**
	 * @return The intervention corresponding to this CEP with a discretized continuous variable on root called lambda
	 */
	public StrategyTree getIntervention() {
		Variable lambda = getLambda();
		return new StrategyTree(lambda, getListOfStates(lambda), getListOfInterventions());
	}

	public int getNumIntervals() {
		return strategyTrees.length;
	}

	/**
	 * @param lambda {@code double}
	 * @return Cost corresponding to lambda. {@code double}
	 */
	public double getCost(double lambda) {
		return costs[index(lambda)];
	}

	/**
	 * @param interval {@code int}
	 * @return Cost corresponding to interval. {@code double}
	 */
	public double getCost(int interval) {
		return costs[interval];
	}

	/**
	 * @param lambda {@code double}
	 * @return Effectiveness corresponding to lambda. {@code double}
	 */
	public double getEffectiveness(double lambda) {
		return effectivities[index(lambda)];
	}

	/**
	 * @param interval {@code int}
	 * @return Effectiveness corresponding to interval. {@code double}
	 */
	public double getEffectiveness(int interval) {
		return effectivities[interval];
	}

	/**
	 * @param lambda {@code double}
	 * @return TreeADDPotential corresponding to lambda. {@code TreeADDPotential}
	 */
	public StrategyTree getIntervention(double lambda) {
		return strategyTrees[index(lambda)];
	}

	/**
	 * @param interval {@code int}
	 * @return TreeADDPotential corresponding to interval. {@code TreeADDPotential}
	 */
	public StrategyTree getIntervention(int interval) {
		return strategyTrees[interval];
	}

	/**
	 * @return All the interventions. {@code Intervention[]}
	 */
	public StrategyTree[] getStrategyTrees() {
		return strategyTrees;
	}

	/**
	 * @return {@code boolean}
	 */
	public boolean isZero() {
		return zeroProbability;
	}

	public void setZero() {
		zeroProbability = true;
	}

	/**
	 * @param interval {@code int}
	 * @return threshold between interval and interval + 1. {@code double}
	 */
	public double getThreshold(int interval) {
		return thresholds[interval];
	}

	/**
	 * @return {@code double[]}
	 */
	public double[] getThresholds() {
		return thresholds;
	}

	/**
	 * @param cost     {@code double}
	 * @param interval {@code int}
	 */
	public void setCost(double cost, int interval) {
		costs[interval] = cost;
	}

	/**
	 * @param eff      {@code double}
	 * @param interval {@code int}
	 */
	public void setEffectiveness(double eff, int interval) {
		effectivities[interval] = eff;
	}

	/**
	 * @return minThreshold. {@code double}
	 */
	public double getMinThreshold() {
		return minThreshold;
	}

	/**
	 * @return maxThreshold. {@code double}
	 */
	public double getMaxThreshold() {
		return maxThreshold;
	}

	/**
	 * @return effectivities. {@code double[]}
	 */
	public double[] getEffectivities() {
		return effectivities;
	}

	/**
	 * @return costs. {@code double[]}
	 */
	public double[] getCosts() {
		return costs;
	}

	private List<StrategyTree> getListOfInterventions() {
		List<StrategyTree> listOfStrategyTrees = new ArrayList<StrategyTree>(strategyTrees.length);
		for (int i = 0; i < strategyTrees.length; i++) {
			listOfStrategyTrees.add(strategyTrees[i]);
		}
		return listOfStrategyTrees;
	}

	private List<State> getListOfStates(Variable variable) {
		State[] statesVariable = variable.getStates();
		int numStates = statesVariable.length;
		List<State> listOfStates = new ArrayList<State>(numStates);
		for (int i = 0; i < numStates; i++) {
			listOfStates.add(statesVariable[i]);
		}
		return listOfStates;
	}

	private Variable getLambda() {
		State[] states = new State[strategyTrees.length];
		double[] limits = new double[strategyTrees.length + 1];
		limits[0] = minThreshold;
		boolean[] belongsToLeftSide = new boolean[strategyTrees.length + 2];
		belongsToLeftSide[belongsToLeftSide.length - 1] = false;
		belongsToLeftSide[0] = true;
		// build state names: state[i] = lambda in (min, max)
		for (int i = 0; i < states.length; i++) {
			if (i < thresholds.length) {
				limits[i + 1] = thresholds[i];
			}
			belongsToLeftSide[i + 1] = true;
			String stateName = new String("\u03BB in [");
			if (i == 0) {
				stateName = stateName + minThreshold;
			} else {
				stateName = stateName + thresholds[i - 1];
			}
			stateName = stateName + " , ";
			if (i == states.length - 1) {
				stateName = stateName + maxThreshold;
			} else {
				stateName = stateName + thresholds[i];
			}
			stateName = stateName + ") Cost = " + costs[i] + " Effectiveness = " + effectivities[i];
			states[i] = new State(stateName);
		}
		limits[limits.length - 1] = maxThreshold;
		return new Variable("lambda", states, new PartitionedInterval(limits, belongsToLeftSide), 0.1);
	}

	public String toString() {
		StringBuilder strBuffer = new StringBuilder();
		if (zeroProbability) {
			strBuffer.append(indent);
			strBuffer.append("CEP with probability zero.\n");
		} else {
			strBuffer.append(indent);
			strBuffer.append("Number of intervals: ");
			strBuffer.append(thresholds.length + 1);
			for (int i = 0; i < thresholds.length + 1; i++) {
				strBuffer.append("\nInterval ");
				strBuffer.append(i);
				strBuffer.append(":\n");
				strBuffer.append(indent);
				strBuffer.append("lambda in (");
				if (i == 0) {
					appendNumberToStrBuffer(strBuffer, minThreshold);
				} else {
					appendNumberToStrBuffer(strBuffer, thresholds[i - 1]);
				}
				strBuffer.append(" and ");
				if (i == thresholds.length) {
					if (maxThreshold == Double.POSITIVE_INFINITY || maxThreshold > 1E300) {
						strBuffer.append("+");
						strBuffer.append("Infinity");
					} else {
						strBuffer.append(maxThreshold);
					}
				} else {
					appendNumberToStrBuffer(strBuffer, thresholds[i]);
				}
				strBuffer.append(")");
				strBuffer.append(" Cost: ");
				appendNumberToStrBuffer(strBuffer, costs[i]);
				strBuffer.append("  ");

				strBuffer.append(" Eff: ");
				appendNumberToStrBuffer(strBuffer, effectivities[i]);
				strBuffer.append("\n");

				strBuffer.append(indent);
				strBuffer.append("optimal intervention:");
				if (strategyTrees[i] != null) {
					strategyTrees[i].setIndentLevel(indentLevel + 2);
					strBuffer.append("\n");
				} else {
					strBuffer.append(" ");
				}
				strBuffer.append(strategyTrees[i]);
			}
		}
		return strBuffer.toString();
	}

	private void appendNumberToStrBuffer(StringBuilder strBuffer, double number) {
		if (Math.abs(number) < 10.0) {
			strBuffer.append(decimalFormat3afterComa.format(number));
		} else if (Math.abs(number) < 100.0) {
			strBuffer.append(decimalFormat2afterComa.format(number));
		} else if (Math.abs(number) < 1000.0) {
			strBuffer.append(decimalFormat1afterComa.format(number));
		} else {
			strBuffer.append(decimalFormatNoDecimalsAfterComa.format(number));
		}
	}

	/**
	 * Calculate net monetary benefit given the willigness to pay (lambda)
	 *
	 * @param lambda {@code double}
	 * @return net monetary benefit given lambda
	 */
	public double getNetMonetaryBenefit(double lambda) {
		return getEffectiveness(lambda) * lambda - getCost(lambda);
	}

	public boolean equals(CEP cep) {
		if (cep != null) {
			boolean areEquals = true;
			areEquals = Arrays.equals(this.thresholds, cep.thresholds) && Arrays.equals(this.costs, cep.costs) && Arrays
					.equals(this.effectivities, cep.effectivities);
			return areEquals;
		} else {
			return false;
		}
	}
}
