/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */
package org.openmarkov.core.model.network.potential;

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

@PotentialType(name = "Hazard (Exponential)", family = "GLM")
public class ExponentialHazardPotential extends WeibullHazardPotential {

	protected static final String[] MANDATORY_COVARIATES = new String[] { CONSTANT };

	public ExponentialHazardPotential(List<Variable> variables, PotentialRole role) {
		super(variables, role, getDefaultCovariates(variables, role, MANDATORY_COVARIATES),
				new double[variables.size()]);
	}

	public ExponentialHazardPotential(List<Variable> variables, PotentialRole role, double[] coefficients) {
		this(variables, role, getDefaultCovariates(variables, role, MANDATORY_COVARIATES), null, null);
	}

	public ExponentialHazardPotential(List<Variable> variables, PotentialRole role, String[] covariates,
			double[] coefficients, double[] covarianceMatrix) {
		super(variables, role, covariates, coefficients, covarianceMatrix);
	}

	public ExponentialHazardPotential(List<Variable> variables, PotentialRole role, double[] coefficients,
			double[] covarianceMatrix) {
		super(variables, role, getDefaultCovariates(variables, role, MANDATORY_COVARIATES), coefficients,
				covarianceMatrix);
	}

	public ExponentialHazardPotential(List<Variable> variables, PotentialRole role, String[] covariates,
			double[] coefficients, double[] uncertaintyMatrix, MatrixType matrixType) {
		super(variables, role, covariates, coefficients, uncertaintyMatrix, matrixType);
	}

	public ExponentialHazardPotential(List<Variable> variables, PotentialRole role, double[] coefficients,
			double[] uncertaintyMatrix, MatrixType matrixType) {
		super(variables, role, getDefaultCovariates(variables, role, MANDATORY_COVARIATES), coefficients,
				uncertaintyMatrix, matrixType);
	}

	public ExponentialHazardPotential(ExponentialHazardPotential potential) {
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
		return !variables.isEmpty() && variables.get(0).getVariableType() == VariableType.FINITE_STATES
				&& variables.get(0).getNumStates() == 2;
	}

	public static String[] getMandatoryCovariates() {
		return new String[] { CONSTANT };
	}

	@Override public List<TablePotential> tableProject(EvidenceCase evidenceCase, InferenceOptions inferenceOptions,
			double[] coefficients, String[] covariates, List<Variable> evidencelessVariables,
			Map<String, String> variableValues) throws NonProjectablePotentialException, WrongCriterionException {
		double[] weibullCoeficients = new double[coefficients.length + 1];
		String[] weibullCovariates = new String[covariates.length + 1];
		// The exponential is a special case of Weibull where k=1 (gamma= ln(k));
		weibullCoeficients[0] = 0;
		weibullCovariates[0] = GAMMA;
		for (int i = 0; i < coefficients.length; ++i) {
			weibullCoeficients[i + 1] = coefficients[i];
			weibullCovariates[i + 1] = covariates[i];
		}
		return super.tableProject(evidenceCase, inferenceOptions, weibullCoeficients, weibullCovariates,
				evidencelessVariables, variableValues);
	}

	@Override public Potential copy() {
		return new ExponentialHazardPotential(this);
	}

	@Override public String toString() {
		return super.toShortString() + " = Hazard (Exponential)";
	}

	@Override public Potential deepCopy(ProbNet copyNet) {
		return (ExponentialHazardPotential) super.deepCopy(copyNet);
	}
}
