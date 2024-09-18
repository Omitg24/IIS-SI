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
import org.openmarkov.core.model.network.potential.Potential;
import org.openmarkov.core.model.network.potential.PotentialRole;
import org.openmarkov.core.model.network.potential.TablePotential;
import org.openmarkov.core.model.network.potential.plugin.PotentialType;

/**
 * Implements the tuning canonical model, first designed for its use in the
 * Optifox project It is limited to variables with only 3 possible values
 *
 * @author Iñigo
 */
@PotentialType(name = "Tuning", family = "ICI") public class TuningPotential extends ICIPotential {
	/**
	 * The canonical model is limited to a child with only tree states
	 */
	private static final int NUM_STATES = 3;

	/**
	 * Constructor for TuningModelPotential.
	 *
	 * @param variables List of variables
	 */
	public TuningPotential(List<Variable> variables) {
		super(ICIModelType.TUNING, variables);
	}

	/**
	 * Copy constructor
	 *
	 * @param tuningPotential Tuning potential
	 */
	public TuningPotential(TuningPotential tuningPotential) {
		super(tuningPotential);
	}

	public TuningPotential(Variable... variables) {
		this(toList(variables));
	}

	/**
	 * Returns if an instance of a certain Potential type makes sense given the variables and the potential role
	 * @param node Node
	 * @param variables List of variables
	 * @param role Potential role
	 * @return True if it is valid
	 */
	public static boolean validate(Node node, List<Variable> variables, PotentialRole role) {
		boolean valid = ICIPotential.validate(node, variables, role) && role
				.equals(PotentialRole.CONDITIONAL_PROBABILITY);
		for (Variable variable : variables) {
			valid &= variable.getNumStates() == 3;
		}
		return valid;
	}

	/**
	 * Adds a parent to the family with its corresponding parameters
	 *
	 * @param parent Parent variable
	 * @param parameters the four parameters that define the link in the
	 *                   following order: c<sub><i>i</i></sub><sup>++</sup>,
	 *                   c<sub><i>i</i></sub><sup>+-</sup>,
	 *                   c<sub><i>i</i></sub><sup>-+</sup>,
	 *                   c<sub><i>i</i></sub><sup>--</sup>
	 */
	public void setNoisyParameters(Variable parent, double[] parameters) {
		double[] values = null;
		if (parameters.length == 4) {
			// Construct table given the parameters
			values = new double[9];
			values[0] = parameters[3]; // c--
			values[1] = 1 - parameters[2] - parameters[3]; // 1 - c-+ - c--
			values[2] = parameters[2]; // c-+
			values[3] = 0.0;
			values[4] = 1.0;
			values[5] = 0.0;
			values[6] = parameters[1]; // c+-
			values[7] = 1 - parameters[2] - parameters[3]; // 1 - c++ - c+-
			values[8] = parameters[0]; // c++
		} else if (parameters.length == 9) {
			values = parameters;
		} else {
			throw new IllegalArgumentException("Parameters' size must be either 4 or 9");
		}

		super.setNoisyParameters(parent, values);
	}

	/**
	 * Creates a table potential to compute the outcome of the f(tuning)
	 * function
	 *
	 * @return a TablePotential containing the probabilities of the tuning function
	 */
	public TablePotential getFFunctionPotential() {
		// Build the list of variables: child node first, z variables
		List<Variable> tuningFunctionVariables = new ArrayList<>(getAuxiliaryVariables());
		tuningFunctionVariables.add(0, variables.get(0));
		tuningFunctionVariables.add(getLeakyVariable());
		TablePotential tablePotential = new TablePotential(tuningFunctionVariables, role);
		int numParents = tuningFunctionVariables.size() - 1;
		// Set the values for the deterministic tuning function
		for (int i = 0; i < tablePotential.values.length; i += NUM_STATES) {
			int index = i / NUM_STATES;
			int netNumIncr = 0;
			for (int j = 0; j < numParents; ++j) {
				// netNumIncr = -1 if v-, netNumIncr = 0 if v0, netNumIncr = 1
				// if v+
				netNumIncr += (index % NUM_STATES) - 1;
				index /= 3;
			}
			// tuning function
			tablePotential.values[i] = (netNumIncr < 0) ? 1.0 : 0.0;
			tablePotential.values[i + 1] = (netNumIncr == 0) ? 1.0 : 0.0;
			tablePotential.values[i + 2] = (netNumIncr > 0) ? 1.0 : 0.0;
		}
		return tablePotential;
	}

	@Override public double[] getDefaultLeakyParameters(int numStates) {
		double[] leakyParameters = new double[numStates];

		for (int i = 0; i < numStates; ++i) {
			leakyParameters[i] = 0.0;
		}
		leakyParameters[numStates / 2] = 1.0;

		return leakyParameters;
	}

	@Override public Potential copy() {
		return new TuningPotential(this);
	}

	@Override public Potential addVariable(Variable newVariable) {
		List<Variable> newVariables = new ArrayList<>(variables);
		newVariables.add(newVariable);
		TuningPotential newICIPotential = new TuningPotential(newVariables);

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
		ArrayList<Variable> newVariables = new ArrayList<>();
		for (int i = 0; i < variables.size(); i++) {
			if (variable == variables.get(i)) {
				continue;
			} else {
				newVariables.add(variables.get(i));
			}
		}

		TuningPotential newICIPotential = new TuningPotential(newVariables);

		for (int i = 1; i < newVariables.size(); i++) {
			double[] noisyParameters = this.getNoisyParameters(newVariables.get(i));
			newICIPotential.setNoisyParameters(newVariables.get(i), noisyParameters);
		}
		newICIPotential.setLeakyParameters(getLeakyParameters());
		return newICIPotential;
	}

	@Override protected int computeFFunction(int[] parentStates) {
		int netNumIncr = 0;
		for (int parentState : parentStates) {
			netNumIncr += parentState - 1;
		}
		int resultingState = 1;
		if (netNumIncr > 0) {
			resultingState = 2;
		} else if (netNumIncr < 0) {
			resultingState = 0;
		}
		return resultingState;
	}

	@Override public boolean isUncertain() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override public void scalePotential(double scale) throws UnsupportedOperationException {
		throw new UnsupportedOperationException();

	}

	@Override public Potential deepCopy(ProbNet copyNet) {
		return (TuningPotential) super.deepCopy(copyNet);
	}
}
