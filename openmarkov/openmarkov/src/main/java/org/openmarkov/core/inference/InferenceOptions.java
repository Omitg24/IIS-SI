/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.inference;

import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.State;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.VariableType;

/**
 * Stores attributes for use in {@code SimpleMarkovEvaluation}
 */
public class InferenceOptions {

	// Attributes

	/** */
	public Variable simulationIndexVariable;

	/** */
	public double discountRate = 1.0;

	public ProbNet probNet;

	private MulticriteriaOptions multiCriteriaOptions;

	private TemporalOptions temporalOptions;

	// Constructor
	public InferenceOptions(ProbNet probNet, Variable simulationIndexVariable) {
		this.probNet = probNet;
		this.simulationIndexVariable = simulationIndexVariable;
	}

	public InferenceOptions() {
		this.multiCriteriaOptions = new MulticriteriaOptions();
		this.temporalOptions = new TemporalOptions();
	}

	public InferenceOptions(InferenceOptions inferenceOptions) {
		this.multiCriteriaOptions = new MulticriteriaOptions(inferenceOptions.getMultiCriteriaOptions());
		this.temporalOptions = new TemporalOptions(inferenceOptions.getTemporalOptions());
	}

	/**
	 * Sets the attribute simulationIndexVariable and returns the variable.
	 * If numSimulations = 0, it returns null.
	 * @param numSimulations Number of simulations
	 * @return Variable
	 */
	public static Variable setNumSimulations(int numSimulations) {
		Variable newVariable;
		if (numSimulations == 0) {
			newVariable = null;
		} else {
			newVariable = new Variable("###SimulationIndexes###", numSimulations);
		}
		return newVariable;
	}

	public MulticriteriaOptions getMultiCriteriaOptions() {
		return multiCriteriaOptions;
	}

	public void setMultiCriteriaOptions(MulticriteriaOptions multiCriteriaOptions) {
		this.multiCriteriaOptions = multiCriteriaOptions;
	}

	public TemporalOptions getTemporalOptions() {
		return temporalOptions;
	}

	// Methods

	public void setTemporalOptions(TemporalOptions temporalOptions) {
		this.temporalOptions = temporalOptions;
	}

	/**
	 * Prints decision criteria, simulation indices and discount rate
	 */
	public String toString() {
		StringBuilder buffer = new StringBuilder();
		if (simulationIndexVariable != null) {
			buffer.append("Simulation indices: ");
			printVariable(buffer, simulationIndexVariable);
		} else {
			buffer.append("No simulation indices.\n");
		}
		buffer.append("Discount rate = " + discountRate);
		return buffer.toString();
	}

	/**
	 * Inserts in buffer the name and states of the received variable
	 */
	private void printVariable(StringBuilder buffer, Variable variable) {
		buffer.append(variable.getName());
		if (variable.getVariableType() != VariableType.NUMERIC) {
			buffer.append("(");
			State[] states = variable.getStates();
			for (int i = 0; i < states.length - 1; i++) {
				buffer.append(states[i].getName() + ", ");
			}
			buffer.append(states[states.length - 1].getName() + ")\n");
		} else {
			buffer.append("Continuous variable!\n");
		}
	}

}
