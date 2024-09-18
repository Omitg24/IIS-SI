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

//UNCLEAR I think this potential does not belong to the GLM Family
@PotentialType(name = "Binomial")

public class BinomialPotential extends Potential {

	//Number of cases
	private int N;
	//Probability of success
	private double theta;

	//UNCLEAR What role should I use--&gt; Suppose CONDITIONAL_PROBABILITY
	public BinomialPotential(List<Variable> variables, PotentialRole role, int NValue, double thetaValue) {
		this(variables, role);
		//UNCLEAR --&gt; Where do I control N is integer and p is between 0 and 1?
		this.N = NValue;
		this.theta = thetaValue;
	}

	/*
	 * From core.model.network.VariableType
	 * FINITE_STATES(0, "finiteStates"), NUMERIC(1, "numeric"),DISCRETIZED(2, "discretized");
	 */

	/* UNCLEAR--&gt;Where do I have to control that the variable is numeric, the probability is between 0 and 1
	 * and the number of cases is a positive integer?
	 */
	public BinomialPotential(List<Variable> variables, PotentialRole role) {
		super(variables, role);
		this.N = 1;
		this.theta = 0.5;

	}

	public BinomialPotential(BinomialPotential potential) {
		super(potential);
		this.N = potential.getN();
		this.theta = potential.gettheta();

	}

	/**
	 * Returns if an instance of a certain Potential type makes sense given the
	 * variables and the potential role.
	 *
	 * @param node      . {@code Node}
	 * @param variables . {@code List} of {@code Variable}.
	 * @param role      . {@code PotentialRole}.
	 * @return if an instance of a certain Potential type makes sense given the
	 * 	 variables and the potential role.
	 */
	public static boolean validate(Node node, List<Variable> variables, PotentialRole role) {
		return role == PotentialRole.CONDITIONAL_PROBABILITY && (!variables.isEmpty()
																		 && variables.get(0).getVariableType()
				== VariableType.NUMERIC
		);
	}

	public int getN() {
		return N;
	}

	public void setN(int NValue) {
		this.N = NValue;
	}

	public double gettheta() {
		return theta;
	}

	public void settheta(double thetaValue) {
		this.theta = thetaValue;
	}

	@Override public List<TablePotential> tableProject(EvidenceCase evidenceCase, InferenceOptions inferenceOptions,
			List<TablePotential> alreadyProjectedPotentials)
			throws NonProjectablePotentialException, WrongCriterionException {
		throw new NonProjectablePotentialException("Cannot convert numeric variable to a table");
	}

	protected List<TablePotential> tableProject(EvidenceCase evidenceCase, InferenceOptions inferenceOptions,
			double[] coefficients, String[] covariates, List<Variable> evidencelessVariables,
			Map<String, String> variableValues) throws NonProjectablePotentialException, WrongCriterionException {

		throw new NonProjectablePotentialException("Cannot convert numeric variable to a table");

	}

	@Override public Potential copy() {
		return new BinomialPotential(this);
	}

	@Override public String toString() {
		return super.toString() + " = Binomial";
	}

	@Override
	//UNCLEAR--&gt; What is this
	public void scalePotential(double scale) {

		throw new UnsupportedOperationException();

	}

	/* UNCLEAR What is this? 
	@Override
	 // From Delta
	 
	    public Collection<Finding> getInducedFindings(EvidenceCase evidenceCase)
	            throws IncompatibleEvidenceException, WrongCriterionException {
	        Finding inducedFinding = null;
	        if(getConditionedVariable().getVariableType() == VariableType.NUMERIC)
	        {
	            inducedFinding = new Finding(getConditionedVariable(), state);
	        }else
	        {
	            inducedFinding = new Finding(getConditionedVariable(), numericValue);
	        }
	        return Arrays.asList(inducedFinding);
	    }   
	     
	*/
	@Override public boolean isUncertain() {
		return false;
	}

	@Override public Potential deepCopy(ProbNet copyNet) {
		BinomialPotential potential = (BinomialPotential) super.deepCopy(copyNet);

		potential.N = this.N;
		potential.theta = this.theta;

		return potential;

	}
}
