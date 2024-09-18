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

import org.openmarkov.core.exception.InvalidStateException;
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

@PotentialType(name = "Linear combination", family = "GLM", altNames = {
		"Linear regression" }) public class LinearCombinationPotential extends GLMPotential {

	public LinearCombinationPotential(List<Variable> variables, PotentialRole role) {
		super(variables, role, getDefaultCovariates(variables, role), new double[variables.size()]);
	}

	//    public LinearCombinationPotential(Variable utilityVariable, List<Variable> variables) {
	//        super(variables, PotentialRole.UTILITY, getDefaultCovariates(variables, PotentialRole.UTILITY), new double[variables.size()+1]);
	//        this.utilityVariable = utilityVariable;
	//    }

	public LinearCombinationPotential(List<Variable> variables, PotentialRole role, String[] covariates,
			double[] coefficients) {
		super(variables, role, covariates, coefficients);
	}

	public LinearCombinationPotential(LinearCombinationPotential potential) {
		super(potential);
	}

	/**
	 * Returns if an instance of a certain Potential type makes sense given the
	 * variables and the potential role.
	 *
	 * @param node      . {@code Node}
	 * @param variables . {@code ArrayList} of {@code Variable}.
	 * @param role      . {@code PotentialRole}.
	 * @return True if it is valid
	 */
	public static boolean validate(Node node, List<Variable> variables, PotentialRole role) {
		return role == PotentialRole.UNSPECIFIED || (
				!variables.isEmpty() && variables.get(0).getVariableType() == VariableType.NUMERIC
		);
	}

	@Override protected List<TablePotential> tableProject(EvidenceCase evidenceCase, InferenceOptions inferenceOptions,
			double[] coefficients, String[] covariates, List<Variable> evidencelessVariables,
			Map<String, String> variableValues) throws NonProjectablePotentialException, WrongCriterionException {
		Variable conditionedVariable = getConditionedVariable();
		int numStates = conditionedVariable.getNumStates();
		Evaluator evaluator = new Evaluator();
		// Fill arrays numericValues and evidencelessVariables

		int constantIndex = getConstantIndex(covariates);

		List<Variable> projectedPotentialVariables = new ArrayList<>(evidencelessVariables);
		TablePotential projectedPotential = null;
		projectedPotentialVariables.add(0, variables.get(0));
		projectedPotential = new TablePotential(projectedPotentialVariables, role);

		int[] offsets = projectedPotential.getOffsets();
		int[] dimensions = projectedPotential.getDimensions();
		int firstParentIndex = 1;
		for (int i = 0; i < projectedPotential.values.length; i += numStates) {
			// Set the values of variables without evidence
			for (int j = firstParentIndex; j < projectedPotentialVariables.size(); ++j) {
				Variable variable = projectedPotentialVariables.get(j);
				int index = (i / offsets[j]) % dimensions[j];
				double value = index;
				try {
					value = Double.parseDouble(variable.getStates()[index].getName());
				} catch (NumberFormatException e) {
					// ignore
				}
				variableValues.put("v" + variables.indexOf(variable), String.valueOf(value));
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
			try {
				if (getConditionedVariable().getVariableType() == VariableType.NUMERIC) {
					projectedPotential.values[i] = regression;
				} else {
					int stateIndex = getConditionedVariable().getStateIndex(regression);
					for (int j = 0; j < numStates; ++j) {
						projectedPotential.values[i + j] = (j == stateIndex) ? 1 : 0;
					}
				}
			} catch (InvalidStateException e) {
				e.printStackTrace();
			}
		}
		return Arrays.asList(projectedPotential);
	}

	@Override public Potential copy() {
		return new LinearCombinationPotential(this);
	}

	@Override public void scalePotential(double scale) {
		// Multiply all the coefficients by the scale
		for (int i = 0; i < coefficients.length; i++) {
			coefficients[i] *= scale;
		}

	}

	@Override public Potential addVariable(Variable variable) {
		LinearCombinationPotential newPotential = null;
		if (!variables.contains(variable)) {
			List<Variable> newVariables = new ArrayList<>(variables);
			newVariables.add(variable);
			newPotential = new LinearCombinationPotential(newVariables, this.role);
			String[] newCovariates = new String[processedCovariates.length + 1];
			for (int i = 0; i < processedCovariates.length; ++i)
				newCovariates[i] = processedCovariates[i];
			newCovariates[processedCovariates.length] = processCovariate(variable.getName(), newVariables);
			newPotential.setCovariates(newCovariates);

			double[] newCoefficients = new double[coefficients.length + 1];
			for (int i = 0; i < coefficients.length; ++i)
				newCoefficients[i] = coefficients[i];
			newCoefficients[coefficients.length] = 0.0;
			newPotential.setCoefficients(newCoefficients);
		} else {
			newPotential = new LinearCombinationPotential(this);
		}
		return newPotential;

	}

	@Override public Potential removeVariable(Variable variable) {
		LinearCombinationPotential newPotential = null;
		if (variables.contains(variable)) {
			List<Variable> newVariables = new ArrayList<>(variables);
			newVariables.remove(variable);
			newPotential = new LinearCombinationPotential(newVariables, this.role);
			List<String> newCovariates = new ArrayList<>();
			List<Double> newCoefficients = new ArrayList<>();
			removeVariableFromCovariates(variables, variable, processedCovariates, coefficients, newCovariates,
					newCoefficients);

			String[] newCovariatesArray = new String[newCovariates.size()];
			double[] newCoefficientsArray = new double[newCoefficients.size()];
			for (int i = 0; i < newCoefficients.size(); ++i) {
				newCoefficientsArray[i] = newCoefficients.get(i);
				newCovariatesArray[i] = newCovariates.get(i);
			}
			newPotential.setCovariates(newCovariatesArray);
			newPotential.setCoefficients(newCoefficientsArray);
		} else {
			newPotential = new LinearCombinationPotential(this);
		}
		return newPotential;
	}

	@Override public Potential deepCopy(ProbNet copyNet) {
		return super.deepCopy(copyNet);
	}

	@Override public String toString() {
		StringBuffer sb = new StringBuffer(super.toString() + " = ");
		String[] covariates = unprocessCovariates(variables, processedCovariates);
		boolean first = true;
		for (int i = 0; i < covariates.length; ++i) {
			if (this.coefficients[i] != 0.0) {
				if (!first)
					sb.append(" + ");
				first = false;
				if (this.coefficients[i] != 1.0)
					sb.append(this.coefficients[i] + "*");
				sb.append(covariates[i]);
			}
		}
		return sb.toString();
	}

}
