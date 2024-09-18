/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.model.network.potential;

import java.util.ArrayList;
import java.util.List;

import org.openmarkov.core.exception.NonProjectablePotentialException;
import org.openmarkov.core.exception.WrongCriterionException;
import org.openmarkov.core.inference.InferenceOptions;
import org.openmarkov.core.model.network.EvidenceCase;
import org.openmarkov.core.model.network.Node;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.VariableType;
import org.openmarkov.core.model.network.modelUncertainty.ExactFunction;
import org.openmarkov.core.model.network.modelUncertainty.ProbDensFunction;
import org.openmarkov.core.model.network.modelUncertainty.ProbDensFunctionManager;
import org.openmarkov.core.model.network.modelUncertainty.ProbDensFunctionType;
import org.openmarkov.core.model.network.modelUncertainty.UncertainValue;
import org.openmarkov.core.model.network.potential.plugin.PotentialType;

@PotentialType(name = "UnivariateDistr") public class UnivariateDistrPotential extends Potential {

	public static final String PSEUDO_VARIABLE = "pseudoVariableDistributionName";
	private static String INITIALIZATION_VALUE = "1";
	protected AugmentedTable distributionTable;
	/**
	 * finiteStateVariables contains the node variable (Numeric) and the finite-states parents
	 */
	protected List<Variable> finiteStatesVariables;
	/**
	 * parameterStateVariables contains the numeric  parents
	 */
	protected List<Variable> parameterVariables;
	protected Variable pseudoVariableDistribution;
	/**
	 * Represents the probability distribution of the values of the table. It is internally described as a finite-states
	 * variable whose states are given by the parameters of the distribution
	 */
	private Class<? extends ProbDensFunction> probDensFunctionClass;
	private String probDensFunctionName;
	private String probDensFunctionUnivariateName;
	private String probDensFunctionParametrizationName;
	private String[] probDensFunctionParametersName;
	private ProbDensFunctionManager probDensFunctionManager;

	/**
	 * Constructor
	 * @param variables List of variables
	 * @param role Potential role
	 */

	public UnivariateDistrPotential(List<Variable> variables, PotentialRole role) {
		super(variables, role);
		if (this.role == null) {
			this.role = PotentialRole.CONDITIONAL_PROBABILITY;
		}
		finiteStatesVariables = new ArrayList<Variable>();
		parameterVariables = new ArrayList<Variable>();

		for (Variable variable : variables.subList(1, variables.size())) {
			if ((variable.getVariableType() == VariableType.FINITE_STATES) || (
					variable.getVariableType() == VariableType.DISCRETIZED
			)) {
				finiteStatesVariables.add(variable);
			} else {
				parameterVariables.add(variable);
			}
		}

		setProbDensFunctionClass(ExactFunction.class);
		setDistributionTable();

	}

	/**
	 * Constructor
	 *
	 * @param variables List of variables
	 * @param name Name
	 * @param parametrization Parametrization
	 * @param role Potential role
	 * @throws InstantiationException InstantiationException
	 */
	public UnivariateDistrPotential(List<Variable> variables, String name, String parametrization, PotentialRole role)
			throws InstantiationException {

		this(variables, role);
		setProbDensFunctionClass(getProbDensFunction(name, parametrization));
		setDistributionTable();
	}

	/**
	 * Constructor
	 *
	 * @param variables List of variables
	 * @param probDensFunctionClass Class of the probability density function
	 * @param role Potential role
	 */
	public UnivariateDistrPotential(List<Variable> variables, Class<? extends ProbDensFunction> probDensFunctionClass,
			PotentialRole role) {
		this(variables, role);
		setProbDensFunctionClass(probDensFunctionClass);
		setDistributionTable();
	}

	/**
	 * Constructor
	 * @param potential Univariate distribution potential
	 */
	public UnivariateDistrPotential(UnivariateDistrPotential potential) {

		super(potential);
		finiteStatesVariables = potential.getFiniteStatesVariables();
		parameterVariables = potential.getParameterVariables();

		setProbDensFunctionClass(potential.getProbDensFunctionClass());
		setDistributionTable((AugmentedTable) (potential.getDistributionTable()).copy());

	}

	/**
	 * Constructor
	 * @param variables List of variables
	 */
	public UnivariateDistrPotential(List<Variable> variables) {
		this(variables, PotentialRole.CONDITIONAL_PROBABILITY);
	}

	/**
	 * Now it is always true
	 * Returns if an instance of a certain Potential type makes sense given the
	 * variables and the potential role.
	 *
	 * @param node      . {@code Node}
	 * @param variables . {@code List} of {@code Variable}.
	 * @param role      . {@code PotentialRole}.
	 * @return True if it is valid
	 */
	public static boolean validate(Node node, List<Variable> variables, PotentialRole role) {
		return (node.getVariable().getVariableType() == VariableType.NUMERIC);

	}

	/**
	 * @return the probDensFunctionManager
	 */
	public ProbDensFunctionManager getProbDensFunctionManager() {
		if (probDensFunctionManager == null) {
			probDensFunctionManager = ProbDensFunctionManager.getUniqueInstance();
		}
		return probDensFunctionManager;
	}

	/**
	 * @param probDensFunctionManager the probDensFunctionManager to set
	 */
	public void setProbDensFunctionManager(ProbDensFunctionManager probDensFunctionManager) {
		this.probDensFunctionManager = probDensFunctionManager;
	}

	public Class<? extends ProbDensFunction> getProbDensFunction(String univariateName, String parametrization)
			throws InstantiationException {

		return getProbDensFunctionManager().getProbDensFunctionClass(univariateName, parametrization);
	}

	/**
	 * @return the distribution
	 */
	public Class<? extends ProbDensFunction> getProbDensFunctionClass() {
		return probDensFunctionClass;
	}

	/**
	 * @param distributionClass the distribution to set
	 */
	public void setProbDensFunctionClass(Class<? extends ProbDensFunction> distributionClass) {
		this.probDensFunctionClass = distributionClass;
		ProbDensFunctionType annotation = distributionClass.getAnnotation(ProbDensFunctionType.class);
		probDensFunctionName = annotation.name();
		probDensFunctionUnivariateName = annotation.univariateName();
		if (probDensFunctionUnivariateName.equals("default")) {
			probDensFunctionUnivariateName = probDensFunctionName;
		}
		probDensFunctionParametersName = annotation.parameters();
		setProbDensFunctionParametrizationName(probDensFunctionParametersName[0]);
		for (int i = 1; i < probDensFunctionParametersName.length; i++) {
			setProbDensFunctionParametrizationName(
					getProbDensFunctionParametrizationName() + ", " + probDensFunctionParametersName[i]);
		}
		translateDistributionIntoPseudoVariable(probDensFunctionParametersName);
	}

	/**
	 * @return the probDensFunctionName
	 */
	public String getProbDensFunctionName() {
		return probDensFunctionName;
	}

	/**
	 * @param probDensFunctionName the probDensFunctionName to set
	 */
	public void setProbDensFunctionName(String probDensFunctionName) {
		this.probDensFunctionName = probDensFunctionName;
	}

	/**
	 * @return the probDensUnivariateParameters
	 */
	public String[] getProbDensFunctionParametersName() {
		return probDensFunctionParametersName;
	}

	/**
	 * @param probDensFunctionParametersName the probDensUnivariateParameters to set
	 */
	public void setProbDensFunctionParametersName(String[] probDensFunctionParametersName) {
		this.probDensFunctionParametersName = probDensFunctionParametersName;
	}

	/**
	 * @return the probDensUnivariateName
	 */
	public String getProbDensFunctionUnivariateName() {
		return probDensFunctionUnivariateName;
	}

	/**
	 * @param probDensUnivariateName the probDensUnivariateName to set
	 */
	public void setProbDensFunctionUnivariateName(String probDensUnivariateName) {
		this.probDensFunctionUnivariateName = probDensUnivariateName;
	}

	public String getProbDensFunctionParametrizationName() {
		return probDensFunctionParametrizationName;
	}

	public void setProbDensFunctionParametrizationName(String probDensFunctionParametrizationName) {
		this.probDensFunctionParametrizationName = probDensFunctionParametrizationName;
	}

	/**
	 * @return the finiteStatesVariables
	 */
	public List<Variable> getFiniteStatesVariables() {
		return finiteStatesVariables;
	}

	/**
	 * @param finiteStatesVariables the finiteStatesVariables to set
	 */
	public void setFiniteStatesVariables(List<Variable> finiteStatesVariables) {
		this.finiteStatesVariables = finiteStatesVariables;
	}

	/**
	 * @return the parameterVariables
	 */
	public List<Variable> getParameterVariables() {
		return parameterVariables;
	}

	/**
	 * @param parameterVariables the parameterVariables to set
	 */
	public void setParameterVariables(List<Variable> parameterVariables) {
		this.parameterVariables = parameterVariables;
	}

	/**
	 * @param probDensFunctionParametersName Probability function parameters name
	 */
	protected void translateDistributionIntoPseudoVariable(String[] probDensFunctionParametersName) {
		pseudoVariableDistribution = new Variable(PSEUDO_VARIABLE, probDensFunctionParametersName);
	}

	/**
	 * @return the pseudoVariableDistribution
	 */
	public Variable getPseudoVariableDistribution() {
		return pseudoVariableDistribution;
	}

	/**
	 * @param pseudoVariableDistribution the pseudoVariableDistribution to set
	 */
	public void setPseudoVariableDistribution(Variable pseudoVariableDistribution) {
		this.pseudoVariableDistribution = pseudoVariableDistribution;
	}

	public AugmentedTable getAugmentedTable() {
		return distributionTable;
	}

	public AugmentedTable getDistributionTable() {
		return distributionTable;
	}

	public void setDistributionTable(AugmentedTable tableDistr) {
		this.distributionTable = tableDistr;
	}

	public void setDistributionTable() {
		List<Variable> vDistributionTable = new ArrayList<Variable>(finiteStatesVariables);
		vDistributionTable.add(0, pseudoVariableDistribution);
		setDistributionTable(new AugmentedTable(vDistributionTable, role));
		initializeAugmentedTable();
	}

	protected void initializeAugmentedTable() {
		String[] functionValues = distributionTable.getFunctionValues();
		for (int i = 0; i < functionValues.length; i++) {
			functionValues[i] = INITIALIZATION_VALUE;
		}
	}

	public void checkDistributionValues(double[] values) throws IllegalArgumentException {
		ProbDensFunction p = getProbDensFunctionManager().newInstance(probDensFunctionName, values);
		try {
			p.verifyParameters(values);
		} catch (IllegalArgumentException e) {
			throw (e);
		}

	}

	@Override public List<TablePotential> tableProject(EvidenceCase evidenceCase, InferenceOptions inferenceOptions)
			throws NonProjectablePotentialException, WrongCriterionException {
		return null;
	}

	@Override public UnivariateDistrPotential project(EvidenceCase evidenceCase)
			throws WrongCriterionException, NonProjectablePotentialException {
		return null;
	}

	@Override public List<TablePotential> tableProject(EvidenceCase evidenceCase, InferenceOptions inferenceOptions,
			List<TablePotential> alreadyProjectedPotentials)
			throws NonProjectablePotentialException, WrongCriterionException {
		return null;
	}

	@Override public Potential copy() {
		return new UnivariateDistrPotential(this);
	}

	@Override public boolean isUncertain() {
		return false;
	}

	/**
	 * UNCLEAR -- Makes sense??
	 */

	@Override public void scalePotential(double scale) {
		this.getDistributionTable().scalePotential(scale);
	}

	public Variable getChildVariable() {
		return this.getVariable(0);
	}

	public void setChildVariable(Variable childVariable) {
		this.getVariables().set(0, childVariable);
	}

	public UncertainValue[] getUncertainValues() {
		return getDistributionTable().getUncertainValues();
	}

	public void setUncertainValues(UncertainValue[] uncertainValues) {
		getDistributionTable().setUncertainValues(uncertainValues);
	}

	public double[] getValues() {
		return getDistributionTable().getValues();
	}

	public void setValues(double[] values) {
		this.getDistributionTable().values = values;
	}

	@Override public String toString() {
		StringBuilder buffer = new StringBuilder();
		buffer.append(variables.get(0).getName());
		if (variables.size() == 1) {
			buffer.append(" = ");
		} else if (variables.size() > 1) {
			buffer.append(" | ");
			// Print variables
			for (int i = 1; i < variables.size() - 1; i++) {
				buffer.append(variables.get(i));
				buffer.append(", ");
			}
			buffer.append(variables.get(variables.size() - 1));
			buffer.append(" = ");
		}
		buffer.append("UnivariteName" + probDensFunctionUnivariateName + " " + "Parametrization"
				+ probDensFunctionParametrizationName + " ");

		if (getDistributionTable().values.length == 1) {
			buffer.append(getDistributionTable().values[0]);
		} else if (getDistributionTable().values.length > 1) {
			buffer.append("{");
			for (int i = 0; i < getDistributionTable().values.length; i++) {
				buffer.append(getDistributionTable().values[i]);
				if (i != getDistributionTable().values.length - 1) {
					buffer.append(",");
				}
			}
			buffer.append("}");
		}
		return buffer.toString();
	}

}
