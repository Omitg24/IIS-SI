/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.model.network.potential;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.openmarkov.core.exception.NonProjectablePotentialException;
import org.openmarkov.core.exception.WrongCriterionException;
import org.openmarkov.core.inference.InferenceOptions;
import org.openmarkov.core.model.network.EvidenceCase;
import org.openmarkov.core.model.network.Node;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.VariableType;
import org.openmarkov.core.model.network.potential.operation.DiscretePotentialOperations;
import org.openmarkov.core.model.network.potential.plugin.PotentialType;

/**
 * Potential associated to supervalue node to indicate that the utility is a
 * sum of the utilities of its parents.
 *
 * @author mkpalacio
 * @version 1.0
 */
@PotentialType(name = "Sum", family = "Utility") public class SumPotential extends Potential {

	// Constructor
	//	/**
	//	 *
	//	 * @param variables
	//	 * @param utilityVariable
	//	 */
	//	public SumPotential(Variable utilityVariable, List<Variable> variables) {
	//		super(utilityVariable, variables);
	//	}

	/**
	 * @param variables List of variables
	 * @param role Potential role
	 */
	public SumPotential(List<Variable> variables, PotentialRole role) {
		super(variables, role);
	}
	
	/**
	 * @param variables List of variables
	 */
	public SumPotential(List<Variable> variables) {
		this(variables, PotentialRole.CONDITIONAL_PROBABILITY);
	}

	public SumPotential(SumPotential potential) {
		super(potential);
	}

	// Methods

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
		boolean suitable = (
				role == PotentialRole.CONDITIONAL_PROBABILITY || role == PotentialRole.POLICY
		) && variables.get(0).getVariableType() == VariableType.NUMERIC;

		return suitable || (role == PotentialRole.UNSPECIFIED && node.isSuperValueNode());
	}

	@Override
	public List<TablePotential> tableProject(
			EvidenceCase evidenceCase, InferenceOptions inferenceOptions, List<TablePotential> projectedPotentials)
			throws NonProjectablePotentialException, WrongCriterionException {
		List<Variable> parentVariables = new ArrayList<>(variables);
		parentVariables.remove(getConditionedVariable());
		List<TablePotential> parentPotentials = new ArrayList<>();
		for (Variable parentVariable : parentVariables) {
			parentPotentials.add(findPotentialByVariable(parentVariable, projectedPotentials));
		}
		TablePotential sumPotential = DiscretePotentialOperations.sum(parentPotentials);
		return Arrays.asList(sumPotential);
	}

	@Override public Potential copy() {
		return new SumPotential(this);
	}

	public double getUtility(HashMap<Variable, Integer> sampledStateIndexes, HashMap<Variable, Double> utilities) {
		double sum = 0.0;
		for (Variable variable : getVariables()) {
			sum += utilities.get(variable);
		}
		return sum;
	}

	public Potential addVariable(Variable variable) {
		variables.add(variable);
		return this;
	}

	/**
	 * Removes variable to a potential implemented in each child class
	 */
	public Potential removeVariable(Variable variable) {
		variables.remove(variable);
		return this;
	}

	@Override public boolean isUncertain() {
		return false;
	}

	@Override public void scalePotential(double scale) {

	}

	@Override public Potential deepCopy(ProbNet copyNet) {
		return (SumPotential) super.deepCopy(copyNet);
	}
}


