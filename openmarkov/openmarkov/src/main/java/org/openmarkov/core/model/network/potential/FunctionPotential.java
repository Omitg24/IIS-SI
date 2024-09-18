/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */
package org.openmarkov.core.model.network.potential;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.openmarkov.core.exception.NonProjectablePotentialException;
import org.openmarkov.core.exception.WrongCriterionException;
import org.openmarkov.core.inference.InferenceOptions;
import org.openmarkov.core.model.network.EvidenceCase;
import org.openmarkov.core.model.network.Node;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.VariableType;
import org.openmarkov.core.model.network.potential.plugin.PotentialType;

import net.sourceforge.jeval.EvaluationException;
import net.sourceforge.jeval.Evaluator;

/**
 * This class implements a function potential consisting of a GLMPotential
 * with only one covariate with coefficient=1
 *
 * @author carmenyago
 * @version 1.0 2016
 */
@PotentialType(name = "Function") public class FunctionPotential extends GLMPotential {

	/**
	 * The default function
	 */
	public static final String DEFAULT_FUNCTION = "0";

	/**
	 * The coefficient
	 */
	protected static final double COEFFICIENT = 1;

	/**
	 * Creates a Function potential with the function by default
	 *
	 * @param variables - list with the node variable and their parents
	 * @param role Potential role
	 */
	public FunctionPotential(List<Variable> variables, PotentialRole role) {
		super(variables, role, new String[] { DEFAULT_FUNCTION }, new double[] { COEFFICIENT });
	}

	/**
	 * Creates a Function potential with the function given by {@code function}
	 *
	 * @param variables - list with the node variable and their parents
	 * @param role      - the role of the potential
	 * @param function  - A string representing the function
	 */
	public FunctionPotential(List<Variable> variables, PotentialRole role, String function) {
		super(variables, role, new String[] { function }, new double[] { COEFFICIENT });
	}

	/**
	 * Creates a Function potential equal to {@code potential}
	 *
	 * @param potential - potential copied
	 */
	public FunctionPotential(FunctionPotential potential) {
		super(potential);
	}

	/**
	 * Returns if an instance of a certain Potential type makes sense given the
	 * variables and the potential role.
	 * UNCLEAR--&#62; Should the parents be numeric
	 *
	 * @param node      . {@code Node}
	 * @param variables . {@code ArrayList} of {@code Variable}.
	 * @param role      . {@code PotentialRole}.
	 * @return True if it is valid
	 */

	public static boolean validate(Node node, List<Variable> variables, PotentialRole role) {
		return (
				!variables.isEmpty() && variables.get(0).getVariableType() == VariableType.NUMERIC
		);
	}

	/**
	 * Gets the unprocessed function of FunctionPotential
	 *
	 * @return the function contained in the FunctionPotential
	 */
	public String getFunction() {
		return unprocessCovariates(variables, processedCovariates)[0];
	}

	/**
	 * Process and sets  {codefunction}
	 *
	 * @param function - The function (unprocessed) to be set
	 */

	public void setFunction(String function) {
		setCovariates(new String[] { function });
	}

	/**
	 * Only throws NonProjectablePotentialException because this potential cannot be projected to a table
	 *
	 * @throws NonProjectablePotentialException NonProjectablePotentialException
	 * @throws WrongCriterionException WrongCriterionException
	 */
	@Override public List<TablePotential> tableProject(EvidenceCase evidenceCase, InferenceOptions inferenceOptions,
			List<TablePotential> projectedPotentials) throws NonProjectablePotentialException, WrongCriterionException {
		throw new NonProjectablePotentialException("Function potential cannot be projected to a table");
	}

	/**
	 * Only throws NonProjectablePotentialException because this potential cannot be projected to a table
	 *
	 * @throws NonProjectablePotentialException NonProjectablePotentialException
	 * @throws WrongCriterionException WrongCriterionException
	 */
	@Override protected List<TablePotential> tableProject(EvidenceCase evidenceCase, InferenceOptions inferenceOptions,
			double[] coefficients, String[] covariates, List<Variable> evidencelessVariables,
			Map<String, String> variableValues) throws NonProjectablePotentialException, WrongCriterionException {
		throw new NonProjectablePotentialException("Function potential cannot be projected to a table");

	}

	@Override public Potential copy() {
		return new FunctionPotential(this);
	}

	/**
	 * Multiplies function by {@code scale}
	 *
	 * @param scale - the scale factor
	 */
	@Override public void scalePotential(double scale) {
		String scaleString = new Double(scale).toString();
		String function = scaleString.concat("*").concat(processedCovariates[0]);
		processedCovariates[0] = function;
	}

	/**
	 * Adds the variable to the new potential. The function does not change
	 *
	 * @param variable - the variable to be added
	 * @return a FunctionPotential with the new variabla
	 */
	@Override public Potential addVariable(Variable variable) {
		FunctionPotential newPotential = null;
		if (!variables.contains(variable)) {
			List<Variable> newVariables = new ArrayList<>(variables);
			newVariables.add(variable);
			newPotential = new FunctionPotential(newVariables, this.role);
			newPotential.setCovariates(processedCovariates);
			newPotential.setCoefficients(new double[] { 1 });
		} else {
			newPotential = new FunctionPotential(this);
		}
		return newPotential;
	}

	/**
	 * Removes a variable from FunctionPotential. If the function does not use the variable,
	 * the function does not change, otherwise the function is set to its default value
	 *
	 * @param variable - the variable to be removed
	 * @return a FunctionPotential without the variable
	 */
	@Override public Potential removeVariable(Variable variable) {
		if (variables.contains(variable)) {
			List<Variable> newVariables = new ArrayList<>(variables);
			newVariables.remove(variable);
			int index = variables.indexOf(variable);
			String variableToRemove = "#{v" + index + "}";
			if (processedCovariates[0].contains(variableToRemove)) {
				return new FunctionPotential(newVariables, this.role);
			}
		}
		return new FunctionPotential(this);
	}

	@Override public Potential deepCopy(ProbNet copyNet) {
		return super.deepCopy(copyNet);
	}

	@Override public String toString() {
		return unprocessCovariates(variables, processedCovariates)[0];
	}

	/**
	 * Always returns false because there is no uncertainty
	 */

	@Override public boolean isUncertain() {
		return false;
	}
	
	
	/**
	 * @param values Values
	 * @return The value obtained by evaluation the function for the assignment of variables given by 'values'
	 * @throws EvaluationException EvaluationException
	 */
	public String getValue(Map<String,String> values) throws EvaluationException {
		Evaluator evaluator = new Evaluator();
		evaluator.setVariables(values);		
		return evaluator.evaluate(this.processedCovariates[0]);
	}

}
