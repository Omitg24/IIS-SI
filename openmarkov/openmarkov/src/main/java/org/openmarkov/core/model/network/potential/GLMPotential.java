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

import org.openmarkov.core.exception.NodeNotFoundException;
import org.openmarkov.core.exception.NonProjectablePotentialException;
import org.openmarkov.core.exception.WrongCriterionException;
import org.openmarkov.core.inference.InferenceOptions;
import org.openmarkov.core.model.network.EvidenceCase;
import org.openmarkov.core.model.network.Finding;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.VariableType;
import org.openmarkov.core.model.network.modelUncertainty.NormalFunction;
import org.openmarkov.core.model.network.modelUncertainty.XORShiftRandom;

/**
 * Generalised Linear Model potential
 */
public abstract class GLMPotential extends Potential {
	protected static final String CONSTANT = "Constant";
	/**
	 * Covariates
	 */
	protected String[] processedCovariates;
	/**
	 * Coefficients for the parameters of the function
	 */
	protected double[] coefficients;
	/**
	 * Sampled coefficients
	 */
	protected double[] sampledCoefficients;
	/**
	 * Covariance matrix
	 */
	protected double[] covarianceMatrix = null;
	/**
	 * Colesky decomposition
	 */
	protected double[] choleskyDecomposition = null;
	public GLMPotential(List<Variable> variables, PotentialRole role) {
		super(variables, role);
		this.sampledCoefficients = null;
		setCovariates(getDefaultCovariates(variables, role));
		setCoefficients(new double[processedCovariates.length]);
	}

	public GLMPotential(List<Variable> variables, PotentialRole role, String[] covariates, double[] coefficients) {
		super(variables, role);
		this.sampledCoefficients = null;
		setCoefficients(coefficients);
		setCovariates(covariates);
	}

	public GLMPotential(List<Variable> variables, PotentialRole role, String[] covariates, double[] coefficients,
			double[] uncertaintyMatrix, MatrixType matrixType) {
		this(variables, role, covariates, coefficients);
		if (matrixType == MatrixType.COVARIANCE) {
			this.covarianceMatrix = uncertaintyMatrix;
			this.choleskyDecomposition = calculateCholesky(uncertaintyMatrix);
		} else {
			this.choleskyDecomposition = uncertaintyMatrix;
		}
	}

	public GLMPotential(List<Variable> variables, PotentialRole role, String[] covariates, double[] coefficients,
			double[] covarianceMatrix) {
		this(variables, role, covariates, coefficients, covarianceMatrix, MatrixType.COVARIANCE);
	}

	public GLMPotential(GLMPotential potential) {
		super(potential);
		setCovariates(potential.processedCovariates.clone());
		setCoefficients(potential.coefficients.clone());
		if (potential.covarianceMatrix != null) {
			setCovarianceMatrix(potential.covarianceMatrix.clone());
		} else if (potential.choleskyDecomposition != null) {
			setCholeskyDecomposition(potential.choleskyDecomposition.clone());
		}
		sampledCoefficients = potential.sampledCoefficients;

	}

	private static double[] calculateCholesky(double[] covarianceMatrix) {
		double[] cholesky = null;
		if (covarianceMatrix != null) {
			// Cholesky decomposition using the the Cholesky–Banachiewicz
			// algorithm
			cholesky = new double[covarianceMatrix.length];
			// Solve quadratic equation to get n, the number of coefficients
			int n = (int) (Math.sqrt(covarianceMatrix.length * 8 + 1) - 1) / 2;
			double[] diagonals = new double[n];
			int[] firstIndexOfRow = new int[n];
			int index = 0;
			for (int i = 0; i < n; ++i) {
				double sumOfSquares = 0.0;
				firstIndexOfRow[i] = index;
				for (int j = 0; j <= i; ++j) {
					if (i == j) {
						diagonals[i] = Math.sqrt(covarianceMatrix[index] - sumOfSquares);
						cholesky[index] = diagonals[i];
					} else {
						double sumOfMul = 0.0;
						for (int k = 0; k < j; ++k) {
							sumOfMul += cholesky[firstIndexOfRow[i] + k] * cholesky[firstIndexOfRow[j] + k];
						}
						cholesky[index] = (covarianceMatrix[index] - sumOfMul) / diagonals[j];
					}
					sumOfSquares += Math.pow(cholesky[index], 2);
					++index;
				}
			}
		}
		return cholesky;
	}

	public static String[] getMandatoryCovariates() {
		return new String[] { CONSTANT };
	}

	protected static String[] getDefaultCovariates(List<Variable> variables, PotentialRole role) {
		return getDefaultCovariates(variables, role, getMandatoryCovariates());
	}

	protected static String[] getDefaultCovariates(List<Variable> variables, PotentialRole role,
			String[] mandatoryCovariates) {
		int firstParentIndex = 1;
		String[] covariates = new String[mandatoryCovariates.length + variables.size() - firstParentIndex];

		int j = 0;
		while (j < mandatoryCovariates.length) {
			covariates[j] = mandatoryCovariates[j];
			++j;
		}
		for (int i = firstParentIndex; i < variables.size(); ++i) {
			covariates[j++] = variables.get(i).getName();
		}
		return covariates;
	}

	public String[] getCovariates() {
		return unprocessCovariates(variables, processedCovariates);
	}

	public void setCovariates(String[] covariates) {
		this.processedCovariates = processCovariates(variables, covariates);
	}

	public double[] getCoefficients() {
		return coefficients;
	}

	public void setCoefficients(double[] coefficients) {
		this.coefficients = coefficients;
	}

	public double getConstant() {
		return coefficients[getConstantIndex(processedCovariates)];
	}

	public void setConstant(double constant) {
		this.coefficients[getConstantIndex(processedCovariates)] = constant;
	}

	public double[] getCovarianceMatrix() {
		return covarianceMatrix;
	}

	public void setCovarianceMatrix(double[] covarianceMatrix) {
		this.covarianceMatrix = covarianceMatrix;
		this.choleskyDecomposition = calculateCholesky(covarianceMatrix);
	}

	public double[] getCholeskyDecomposition() {
		return choleskyDecomposition;
	}

	public void setCholeskyDecomposition(double[] choleskyDecomposition) {
		this.choleskyDecomposition = choleskyDecomposition;
	}

	@Override public boolean isUncertain() {
		return this.covarianceMatrix != null || this.choleskyDecomposition != null;
	}

	@Override public List<TablePotential> tableProject(EvidenceCase evidenceCase, InferenceOptions inferenceOptions,
			List<TablePotential> projectedPotentials) throws NonProjectablePotentialException, WrongCriterionException {
		double[] coefficients = (sampledCoefficients == null) ? this.coefficients : this.sampledCoefficients;
		List<Variable> evidencelessVariables = new ArrayList<>();
		Map<String, String> variableValues = new HashMap<>();
		int firstParentVariableIndex = 1;
		for (int i = firstParentVariableIndex; i < variables.size(); ++i) {
			Variable variable = variables.get(i);
			if (evidenceCase == null || !evidenceCase.contains(variable)) {
				if (variable.getVariableType() == VariableType.NUMERIC) {
					throw new NonProjectablePotentialException(
							"Can not project potential with numeric variable " + variable.getName());
				}
				evidencelessVariables.add(variable);
				variableValues.put("v" + i, "0.0");
			} else {
				double numericValue = 0;
				Finding finding = evidenceCase.getFinding(variable);
				if (variable.getVariableType() == VariableType.NUMERIC
						|| variable.getVariableType() == VariableType.DISCRETIZED) {
					numericValue = finding.getNumericalValue();
				} else {
					int index = evidenceCase.getFinding(variable).getStateIndex();
					numericValue = index;
					try {
						numericValue = Double.parseDouble(variable.getStates()[index].getName());
					} catch (NumberFormatException e) {
						// ignore
					}
				}
				variableValues.put("v" + i, String.valueOf(numericValue));
			}
		}
		return tableProject(evidenceCase, inferenceOptions, coefficients, processedCovariates, evidencelessVariables,
				variableValues);
	}

	protected abstract List<TablePotential> tableProject(EvidenceCase evidenceCase, InferenceOptions inferenceOptions,
			double[] coefficients, String[] covariates, List<Variable> evidencelessVariables,
			Map<String, String> variableValues) throws NonProjectablePotentialException, WrongCriterionException;

	@Override public Potential sample() {
		if (choleskyDecomposition != null) {
			if (this.sampledCoefficients == null) {
				this.sampledCoefficients = new double[coefficients.length];
			}

			Random randomGenerator = new XORShiftRandom();
			NormalFunction normalDistribution = new NormalFunction(0, 1);
			double[] normalSamples = new double[coefficients.length];
			for (int i = 0; i < normalSamples.length; ++i) {
				double sample = normalDistribution.getSample(randomGenerator);
				normalSamples[i] = sample;
			}

			int index = 0;
			for (int i = 0; i < coefficients.length; ++i) {
				double value = 0.0;
				for (int j = 0; j <= i; ++j) {
					value += choleskyDecomposition[index] * normalSamples[j];
					index++;
				}
				sampledCoefficients[i] = value + coefficients[i];
			}
		}
		return this;
	}

	protected String[] processCovariates(List<Variable> variables, String[] covariates) {
		String[] processedCovariates = new String[covariates.length];

		for (int i = 0; i < covariates.length; ++i) {
			processedCovariates[i] = processCovariate(covariates[i], variables);
		}
		return processedCovariates;
	}

	protected String processCovariate(String covariate, List<Variable> variables) {
		String processedCovariate = covariate;
		for (int i = 0; i < variables.size(); ++i) {
			Variable variable = variables.get(i);
			if (processedCovariate.contains("{" + variable.getName() + "}")) {
				processedCovariate = processedCovariate.replace("{" + variable.getName() + "}", "#{v" + i + "}");
			}
			if (processedCovariate.contains(variable.getName())) {
				processedCovariate = processedCovariate.replace(variable.getName(), "#{v" + i + "}");
			}
		}
		return processedCovariate;
	}

	protected String[] unprocessCovariates(List<Variable> variables, String[] processedCovariates) {
		String[] covariates = processedCovariates.clone();
		for (int j = 0; j < covariates.length; ++j) {
			for (int i = 0; i < variables.size(); ++i) {
				String processedVariableName = "#{v" + i + "}";
				if (covariates[j].equals(processedVariableName)) {
					covariates[j] = variables.get(i).getName();
				} else if (covariates[j].contains(processedVariableName)) {
					covariates[j] = covariates[j]
							.replace(processedVariableName, "{" + variables.get(i).getName() + "}");
				}
			}
		}
		return covariates;
	}

	protected void removeVariableFromCovariates(List<Variable> variables, Variable variable, String[] covariates,
			double[] coefficients, List<String> newCovariates, List<Double> newCoefficients) {
		int index = variables.indexOf(variable);
		String variableToRemove = "#{v" + index + "}";
		for (int i = 0; i < covariates.length; ++i) {
			if (!covariates[i].contains(variableToRemove)) {
				String newCovariate = covariates[i];
				for (int j = index + 1; j < variables.size(); ++j) {
					if (newCovariate.contains("#{v" + j + "}"))
						newCovariate = newCovariate.replace("#{v" + j + "}", "#{v" + (j - 1) + "}");
				}
				newCovariates.add(newCovariate);
				newCoefficients.add(coefficients[i]);
			}
		}
	}

	protected int getConstantIndex(String[] covariates) {
		int constantIndex = -1;
		int i = 0;
		while (i < covariates.length && constantIndex == -1) {
			if (covariates[i].equals(CONSTANT)) {
				constantIndex = i;
			}
			++i;
		}
		return constantIndex;
	}

	@Override public void shift(ProbNet probNet, int timeDifference) throws NodeNotFoundException {
		super.shift(probNet, timeDifference);
	}

	@Override public Potential deepCopy(ProbNet copyNet) {
		GLMPotential potential = (GLMPotential) super.deepCopy(copyNet);
		if (this.choleskyDecomposition != null) {
			potential.choleskyDecomposition = this.choleskyDecomposition.clone();
		}

		if (this.coefficients != null) {
			potential.coefficients = this.coefficients.clone();
		}

		if (this.covarianceMatrix != null) {
			potential.covarianceMatrix = this.covarianceMatrix.clone();
		}

		if (this.processedCovariates != null) {
			potential.processedCovariates = this.processedCovariates.clone();
		}

		if (sampledCoefficients != null) {
			potential.sampledCoefficients = this.sampledCoefficients.clone();
		}

		return potential;
	}

	public enum MatrixType {
		COVARIANCE, CHOLESKY
	}
}
