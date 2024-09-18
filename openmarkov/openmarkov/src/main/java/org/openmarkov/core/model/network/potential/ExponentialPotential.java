/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */
package org.openmarkov.core.model.network.potential;

import java.util.ArrayList;
import java.util.Arrays;
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

@PotentialType(name = "Exponential", family = "GLM") public class ExponentialPotential extends GLMPotential {

	public ExponentialPotential(List<Variable> variables, PotentialRole role) {
		super(variables, role);
	}

	//	public ExponentialPotential(Variable utilityVariable, List<Variable> variables) {
	//		this(variables, PotentialRole.UTILITY);
	//		this.utilityVariable = utilityVariable;
	//	}

	public ExponentialPotential(List<Variable> variables, PotentialRole role, String[] covariates,
			double[] coefficients) {
		super(variables, role, covariates, coefficients);
	}

	public ExponentialPotential(ExponentialPotential potential) {
		super(potential);
	}

	/**
	 * Returns if an instance of a certain Potential type makes sense given the
	 * variables and the potential role.
	 *
	 * @param node      . {@code Node}
	 * @param variables . {@code List} of {@code Variable}.
	 * @param role      . {@code PotentialRole}.
	 * @return True if it is valid
	 */
	public static boolean validate(Node node, List<Variable> variables, PotentialRole role) {
		return role == PotentialRole.UNSPECIFIED || (!variables.isEmpty() && variables.get(0).getVariableType()
				== VariableType.NUMERIC
		);
	}

	@Override protected List<TablePotential> tableProject(EvidenceCase evidenceCase, InferenceOptions inferenceOptions,
			double[] coefficients, String[] covariates, List<Variable> evidencelessVariables,
			Map<String, String> variableValues) throws NonProjectablePotentialException, WrongCriterionException {
		// Fill arrays numericValues and evidencelessVariables
		int constantIndex = getConstantIndex(covariates);

		List<Variable> projectedPotentialVariables = new ArrayList<>(evidencelessVariables);
		TablePotential projectedPotential = null;
		projectedPotentialVariables.add(0, variables.get(0));
		projectedPotential = new TablePotential(projectedPotentialVariables, role);
		Variable conditionedVariable = getConditionedVariable();
		int numStates = conditionedVariable.getNumStates();
		int parentFirstIndex = (conditionedVariable == projectedPotentialVariables.get(0)) ? 1 : 0;
		int[] offsets = projectedPotential.getOffsets();
		int[] dimensions = projectedPotential.getDimensions();
		Evaluator evaluator = new Evaluator();
		for (int i = 0; i < projectedPotential.values.length; i += numStates) {
			// Set the values of variables without evidence
			for (int j = parentFirstIndex; j < projectedPotentialVariables.size(); ++j) {
				Variable variable = projectedPotentialVariables.get(j);
				int index = (i / offsets[j]) % dimensions[j];
				double value = index;
				try {
					value = Double.parseDouble(variable.getStates()[index].getName());
				} catch (NumberFormatException e) {
					// ignore
				}
				variableValues.put("v" + j, String.valueOf(value));
			}
			evaluator.setVariables(variableValues);
			double regression = coefficients[constantIndex];
			for (int j = 0; j < coefficients.length; ++j) {
				double covariateValue = 0.0;
				if (j != constantIndex) {
					try {
						covariateValue = Double.parseDouble(evaluator.evaluate(covariates[j]));
					} catch (NumberFormatException | EvaluationException e) {
						throw new NonProjectablePotentialException(e.getMessage());
					}
					regression += covariateValue * coefficients[j];
				}
			}
			projectedPotential.values[i] = Math.exp(regression);
		}
		return Arrays.asList(projectedPotential);
	}

	@Override public Potential copy() {
		return new ExponentialPotential(this);
	}

	@Override public String toString() {
		return super.toString() + " = Exponential";
	}

	@Override public void scalePotential(double scale) {
		/*
		 * Add ln(scale) to the first coefficient (constant covariate) is the same as
		 * multiply all the exponential potential by the scale
		 */
		coefficients[0] += Math.log(scale);

	}

	@Override public Potential deepCopy(ProbNet copyNet) {
		return super.deepCopy(copyNet);
	}

}
