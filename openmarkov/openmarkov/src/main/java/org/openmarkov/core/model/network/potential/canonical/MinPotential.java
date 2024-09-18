/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.model.network.potential.canonical;

import java.util.ArrayList;
import java.util.List;

import org.openmarkov.core.model.network.Node;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.VariableType;
import org.openmarkov.core.model.network.potential.Potential;
import org.openmarkov.core.model.network.potential.PotentialRole;
import org.openmarkov.core.model.network.potential.TablePotential;
import org.openmarkov.core.model.network.potential.plugin.PotentialType;

@PotentialType(name = "AND / MIN", family = "ICI") public class MinPotential extends MinMaxPotential {

	/**
	 * @param modelType {@code ICIModel}.
	 * @param variables {@code ArrayList} of {@code Variable}.
	 */
	public MinPotential(ICIModelType modelType, List<Variable> variables) {
		super(modelType, variables);
	}

	/**
	 * Constructor for MinPotential that assumes the ICIModelType is GENERAL_MIN
	 *
	 * @param variables List of variables
	 */
	public MinPotential(List<Variable> variables) {
		this(ICIModelType.GENERAL_MIN, variables);
	}

	public MinPotential(MinPotential potential) {
		super(potential);
	}

	/**
	 * Returns if an instance of a certain Potential type makes sense given
	 * the variables and the potential role.
	 *
	 * @param node      {@code Node}
	 * @param variables {@code ArrayList} of {@code Variable}.
	 * @param role      {@code PotentialRole}.
	 * @return True if it is valid
	 */
	public static boolean validate(Node node, List<Variable> variables, PotentialRole role) {
		boolean valid = ICIPotential.validate(node, variables, role) && (
				(role == PotentialRole.CONDITIONAL_PROBABILITY) || (role == PotentialRole.POLICY)
		);
		int i = 0;

		while (valid && i < variables.size()) {
			valid &= variables.get(i).getVariableType() == VariableType.FINITE_STATES
					|| variables.get(i).getVariableType() == VariableType.DISCRETIZED;
			++i;
		}
		return valid;
	}

	/**
	 * @return A {@code TablePotential} with two variables: {@code conditionedVariable} and {@code pseudoVariable}.
	 */
	@Override
	public TablePotential getDeltaPotential() {
		Variable conditionedVariable = variables.get(0);
		ArrayList<Variable> deltaVariables = new ArrayList<>();
		deltaVariables.add(pseudoVariable);
		deltaVariables.add(conditionedVariable);
		TablePotential deltaPotential = new TablePotential(deltaVariables, PotentialRole.CONDITIONAL_PROBABILITY);
		int numStatesConditioned = conditionedVariable.getNumStates();
		int numStatesPseudo = pseudoVariable.getNumStates(); // same number
		int actualConfiguration = 0;
		for (int i = 0; i < numStatesConditioned; i++) {
			for (int j = 0; j < numStatesPseudo; j++) {
				if (i == j) {
					deltaPotential.values[actualConfiguration] = 1;
				} else if (j == (i + 1)) {
					deltaPotential.values[actualConfiguration] = -1;
				} else {
					deltaPotential.values[actualConfiguration] = 0;
				}
				actualConfiguration++;
			}
		}
		return deltaPotential;
	}

	/**
	 * @param subPotential {@code TablePotential}
	 * In general it will be the conditional probability associated with
	 * a link of the ICI model (i.e., a conditional probability of the child
	 * node given the parent node) or the leak probability.
	 *
	 * @return The accrued potential. {@code TablePotential}. I.e., if
	 * subPotential is P(y) then the accrued potential is P(Y&#62;=y), and if
	 * the subPotential is P(y|x) then the accrued potential is P(Y&#62;=y|x).
	 *
	 * reference Efficient computation for the Noisy MAX
	 *
	 * Condition: subPotential is a probability table of one variable
	 * or a probability table of one variable given another variable.
	 */
	@Override
	protected TablePotential getAccruedPotential(
			TablePotential subPotential) {
		// TODO Revisar este metodo para el caso de un potential proyectado
		List<Variable> subPotentialVariables = subPotential.getVariables();
		// Create a new TablePotential with the same variables,
		// except the first one, which is replaced by the pseudovariable
		List<Variable> accruedPotentialVariables = new ArrayList<>(subPotentialVariables);
		accruedPotentialVariables.set(0, pseudoVariable);
		TablePotential accruedPotential = new TablePotential(accruedPotentialVariables,
				PotentialRole.CONDITIONAL_PROBABILITY);

		// number of states in the pseudovariable
		int numStates = variables.get(0).getNumStates();

		double accumulator = 0;
		for (int i = subPotential.values.length - 1; i >= 0; i--) {
			accumulator += subPotential.values[i];
			accruedPotential.values[i] = accumulator;
			if (i % numStates == 0) {
				accumulator = 0;
			}
		}

		return accruedPotential;
	}

	@Override public double[] getDefaultLeakyParameters(int numStates) {
		double[] leakyParameters = new double[numStates];

		leakyParameters[numStates - 1] = 1.0;
		for (int i = 0; i < numStates - 1; ++i) {
			leakyParameters[i] = 0.0;
		}
		return leakyParameters;
	}

	@Override public Potential copy() {
		return new MinPotential(this);
	}

	@Override public Potential addVariable(Variable newVariable) {
		List<Variable> newVariables = new ArrayList<>(variables);
		newVariables.add(newVariable);
		MinPotential newICIPotential = new MinPotential(this.modelType, newVariables);

		for (int i = 1; i < variables.size(); i++) {
			double[] noisyParameters = this.getNoisyParameters(variables.get(i));
			newICIPotential.setNoisyParameters(variables.get(i), noisyParameters);
		}

		Variable conditionedVariable = variables.get(0);
		double[] noisyParameters = newICIPotential.initializeNoisyParameters(conditionedVariable, newVariable);
		newICIPotential.setNoisyParameters(newVariable, noisyParameters);

		newICIPotential.setLeakyParameters(getLeakyParameters());
		return newICIPotential;
	}

	@Override public Potential removeVariable(Variable variable) {
		List<Variable> newVariables = new ArrayList<>();
		for (int i = 0; i < variables.size(); i++) {
			if (variable == variables.get(i)) {
				continue;
			} else {
				newVariables.add(variables.get(i));
			}
		}

		MinPotential newICIPotential = new MinPotential(this.modelType, newVariables);

		for (int i = 1; i < newVariables.size(); i++) {
			double[] noisyParameters = this.getNoisyParameters(newVariables.get(i));
			newICIPotential.setNoisyParameters(newVariables.get(i), noisyParameters);
		}
		newICIPotential.setLeakyParameters(getLeakyParameters());
		return newICIPotential;
	}

	@Override protected int computeFFunction(int[] parentStates) {
		int resultingState = variables.get(0).getNumStates() - 1;
		for (Integer parentState : parentStates) {
			if (parentState < resultingState) {
				resultingState = parentState;
			}
		}
		return resultingState;
	}

	@Override public boolean isUncertain() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override public TablePotential getFFunctionPotential() {
		// Build the list of variables: child node first, z variables
		List<Variable> functionVariables = new ArrayList<>(getAuxiliaryVariables());
		functionVariables.add(0, variables.get(0));
		functionVariables.add(getLeakyVariable());
		TablePotential tablePotential = new TablePotential(functionVariables, role);
		int numParents = functionVariables.size() - 1;
		int numStates = variables.get(0).getNumStates();
		// Set the values for the deterministic f function
		for (int i = 0; i < tablePotential.values.length; i += numStates) {
			int index = i / numStates;
			int min = 0;
			for (int j = 0; j < numParents; ++j) {
				min = (index % numStates) < min ? (index % numStates) : min;
				index /= variables.get(j + 1).getNumStates();
			}
			// min function
			for (int j = 0; j < numParents; ++j) {
				tablePotential.values[j] = j == min ? 1.0 : 0.0;
			}
		}
		return tablePotential;
	}

	@Override public void scalePotential(double scale) throws UnsupportedOperationException {
		throw new UnsupportedOperationException();

	}

	@Override public Potential deepCopy(ProbNet copyNet) {
		return (MinPotential) super.deepCopy(copyNet);
	}

}
