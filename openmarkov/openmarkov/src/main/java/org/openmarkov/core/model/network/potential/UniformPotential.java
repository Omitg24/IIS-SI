/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.model.network.potential;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.openmarkov.core.exception.NonProjectablePotentialException;
import org.openmarkov.core.exception.OpenMarkovExceptionConstants;
import org.openmarkov.core.inference.InferenceOptions;
import org.openmarkov.core.model.network.EvidenceCase;
import org.openmarkov.core.model.network.Node;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.VariableType;
import org.openmarkov.core.model.network.potential.plugin.PotentialType;

/**
 * Potential with discrete and/or continuous variables.
 *
 * @author marias
 * @version 1.0
 */
@PotentialType(name = "Uniform") public class UniformPotential extends Potential {
	// Attributes
	/**
	 * Value of a potential configuration when all the variables are discrete.
	 */
	private double discreteValue = 0.0;

	// Constructors

	/**
	 * @param variables {@code ArrayList} of {@code Variable}
	 * @param role      {@code PotentialRole}
	 */
	public UniformPotential(List<Variable> variables, PotentialRole role) {
		super(variables, role);
		if (allVariablesAreDiscrete(variables)) {
			discreteValue = calculateDiscreteValue(variables);
		}
	}

	//    /**
	//     * @param variables <code>ArrayList</code> of <code>Variable</code>
	//     * @param utilityVariable <code>Variable</code>
	//     */
	//    public UniformPotential (Variable utilityVariable, List<Variable> variables)
	//    {
	//        super (utilityVariable, variables);
	//        if (allVariablesAreDiscrete (variables))
	//        {
	//            discreteValue = calculateDiscreteValue (variables);
	//        }
	//    }

	/**
	 * @param role      {@code PotentialRole}
	 * @param variables {@code Variable}
	 */
	public UniformPotential(PotentialRole role, Variable... variables) {
		this(toList(variables), role);
	}

	/**
	 * Copy constructor for UniformPotential
	 *
	 * @param potential Uniform potential
	 */
	public UniformPotential(UniformPotential potential) {
		super(potential);
		if (allVariablesAreDiscrete(variables)) {
			discreteValue = calculateDiscreteValue(variables);
		}
	}

	// Methods

	/**
	 * Returns if an instance of a certain Potential type makes sense given the
	 * variables and the potential role
	 *
	 * @param node      {@code Node}
	 * @param variables {@code ArrayList} of {@code Variable}
	 * @param role      {@code PotentialRole}
	 * @return True if it is valid
	 */
	public static boolean validate(Node node, List<Variable> variables, PotentialRole role) {
		// TODO
		return true;
	}

	// Methods
	/**
	 * @param evidenceCase {@code evidenceCase}
	 * @param projectedPotentials List of projected potentials
	 *
	 * @return If this is a utility potential, it represents the case in
	 * which all the utilities are zero; therefore, it suffices to return
	 * an empty list. If this is a conditional probability P(Y|X1,...,Xn), it
	 * returns a TablePotential that is uniform potential P(y).
	 * If this is a joint probability, P(X1,...,Xn), it returns a
	 * TablePotential that is equal to this potential.
	 * In all cases, the argument evidenceCase is irrelevant.
	 *
	 * @throws NonProjectablePotentialException when this is a conditional probability potential and the conditioned variable is numeric.
	 */
	@Override
	public List<TablePotential> tableProject(
			EvidenceCase evidenceCase, InferenceOptions inferenceOptions, List<TablePotential> projectedPotentials)
			throws NonProjectablePotentialException {
		List<TablePotential> newProjectedPotentials = new ArrayList<>();
		switch (role) {
		case CONDITIONAL_PROBABILITY:
		case JOINT_PROBABILITY:
		case POLICY:
			TablePotential projectedPotential = null;
			Variable conditionedVariable = variables.get(0);
			if (evidenceCase != null && evidenceCase.contains(conditionedVariable)) {
				if (conditionedVariable.getVariableType() == VariableType.NUMERIC) {
					// returns an empty list of potentials
					return new ArrayList<>();
				} else {
					// returns a constant
					projectedPotential = new TablePotential(new ArrayList<Variable>(), role);
					projectedPotential.values[0] = 1.0 / conditionedVariable.getNumStates();
				}
			} else {
				// the conditioned variable does not make part of the
				// evidence
				if (conditionedVariable.getVariableType() == VariableType.NUMERIC) {
					throw new NonProjectablePotentialException(OpenMarkovExceptionConstants.NonProjectablePotentialException_UniformNumeric, conditionedVariable.getName());

				} else {
					// returns a uniform potential
					List<Variable> potentialVariables = new ArrayList<>(variables);
					if (evidenceCase != null) {
						potentialVariables.removeAll(evidenceCase.getVariables());
					}
					projectedPotential = new TablePotential(potentialVariables, getPotentialRole());
				}
			}
			newProjectedPotentials.add(projectedPotential);
			break;
		default:
			break;
		} // end of switch/case statement
		return newProjectedPotentials;
	}

	/**
	 * @param variables {@code ArrayList} of {@code Variable}
	 * @return {@code true} if all the variables are FINITE_STATES.
	 */
	private boolean allVariablesAreDiscrete(List<Variable> variables) {
		for (Variable variable : variables) {
			if (variable.getVariableType() != VariableType.FINITE_STATES) {
				return false;
			}
		}
		return true;
	}

	/**
	 * @param variables {@code ArrayList} of {@code Variable}
	 * @return 1 / multiplication of the number of states of conditioning
	 * variables.
	 */
	private double calculateDiscreteValue(List<Variable> variables) {
		int statesSpace = 1;
		for (int i = 1; i < variables.size(); i++) {
			statesSpace *= variables.get(i).getNumStates();
		}
		return 1 / new Double(statesSpace);
	}

	/**
	 * @return discreteValue. {@code double}
	 */
	public double getDiscreteValue() {
		return discreteValue;
	}

	/**
	 * Used to apply discount rates in cost effectiveness analysis for utility
	 * variables has no sense in chance nodes
	 * @param discreteValue Discrete value
	 */
	public void setDiscreteValue(double discreteValue) {
		this.discreteValue = discreteValue;
	}

	@Override public Potential copy() {
		return new UniformPotential(this);
	}

	@Override public int sampleConditionedVariable(Random randomGenerator, Map<Variable, Integer> parentStateIndexes) {
		return randomGenerator.nextInt(variables.get(0).getNumStates());
	}

	public double getProbability(HashMap<Variable, Integer> sampledStateIndexes) {
		return 1.0 / variables.get(0).getNumStates();
	}

	@Override public boolean isUncertain() {
		return false;
	}

	@Override public String toString() {
		return super.toString() + " = Uniform";
	}

	@Override public void scalePotential(double scale) {

	}

	@Override public Potential deepCopy(ProbNet copyNet) {
		UniformPotential potential = (UniformPotential) super.deepCopy(copyNet);

		potential.setDiscreteValue(this.getDiscreteValue());
		return potential;

	}
}
