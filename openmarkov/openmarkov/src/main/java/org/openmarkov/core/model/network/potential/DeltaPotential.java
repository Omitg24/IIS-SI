/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.model.network.potential;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.openmarkov.core.exception.IncompatibleEvidenceException;
import org.openmarkov.core.exception.InvalidStateException;
import org.openmarkov.core.exception.NonProjectablePotentialException;
import org.openmarkov.core.exception.WrongCriterionException;
import org.openmarkov.core.inference.InferenceOptions;
import org.openmarkov.core.model.network.EvidenceCase;
import org.openmarkov.core.model.network.Finding;
import org.openmarkov.core.model.network.Node;
import org.openmarkov.core.model.network.PartitionedInterval;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.State;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.VariableType;
import org.openmarkov.core.model.network.potential.plugin.PotentialType;

@PotentialType(name = "Delta") public class DeltaPotential extends Potential {

	// state and stateIndex are used for finite states variables
	private State state = null;
	private int stateIndex = -1;
	// numericValue is used for numeric variables
	private double numericValue = Double.NaN;

	public DeltaPotential(List<Variable> variables, PotentialRole role, double numericValue) {
		this(variables, role);
		initNumeric(getConditionedVariable(), numericValue);
	}

	public DeltaPotential(List<Variable> variables, PotentialRole role, State state) {
		this(variables, role);
		initFiniteStates(getConditionedVariable(), state);
	}

	public DeltaPotential(List<Variable> variables, PotentialRole role) {
		super(variables, role);
		// set default values
		Variable conditionedVariable = getConditionedVariable();
		if (conditionedVariable.getVariableType() != VariableType.FINITE_STATES) {
			PartitionedInterval variableDomain = conditionedVariable.getPartitionedInterval();
			double numericValue = variableDomain.isLeftClosed() ?
					variableDomain.getMin() :
					variableDomain.getMin() + conditionedVariable.getPrecision();
			initNumeric(conditionedVariable, numericValue);
		} else {
			initFiniteStates(conditionedVariable, conditionedVariable.getStates()[0]);
		}
	}

	//    public DeltaPotential(Variable utilityVariable, List<Variable> variables)
	//    {
	//    	this(variables, PotentialRole.UTILITY);
	//    	this.utilityVariable = utilityVariable;
	//    }

	public DeltaPotential(DeltaPotential potential) {
		super(potential);
		state = potential.state;
		stateIndex = potential.stateIndex;
		numericValue = potential.numericValue;
	}

	/**
	 * Returns whether this type of Potential is suitable for the list of
	 * variables and the potential role given.
	 *
	 * @param node      . {@code Node}
	 * @param variables . {@code List} of {@code Variable}.
	 * @param role      . {@code PotentialRole}.
	 * @return True if it is valid
	 */
	public static boolean validate(Node node, List<Variable> variables, PotentialRole role) {
		return (
				(variables.size() <= 1 || role == PotentialRole.POLICY) || (
						variables.size() > 1 && role == PotentialRole.CONDITIONAL_PROBABILITY
								&& node.getVariable().getVariableType() == VariableType.NUMERIC
				)
		);
	}

	private void initFiniteStates(Variable conditionedVariable, State state) {
		this.state = state;
		stateIndex = conditionedVariable.getStateIndex(state);
	}

	private void initNumeric(Variable conditionedVariable, double numericValue) {
		this.numericValue = numericValue;
		if (conditionedVariable.getVariableType() == VariableType.DISCRETIZED) {
			try {
				stateIndex = conditionedVariable.getStateIndex(numericValue);
			} catch (InvalidStateException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	@Override public List<TablePotential> tableProject(EvidenceCase evidenceCase, InferenceOptions inferenceOptions,
			List<TablePotential> projectedPotentials) throws NonProjectablePotentialException, WrongCriterionException {
		TablePotential projectedPotential;
		if (state != null) {
			// finite states variable
			Variable conditionedVariable = getConditionedVariable();
			if (evidenceCase.contains(conditionedVariable)) {
				projectedPotential = new TablePotential(new ArrayList<Variable>(),
						PotentialRole.CONDITIONAL_PROBABILITY);
				projectedPotential.values[0] = 1;
			} else {
				projectedPotential = new TablePotential(Arrays.asList(conditionedVariable),
						PotentialRole.CONDITIONAL_PROBABILITY);
				for (int i = 0; i < projectedPotential.values.length; ++i) {
					projectedPotential.values[i] = (i == stateIndex) ? 1 : 0;
				}
			}
		} else {
			// numeric variable
			projectedPotential = new TablePotential(new ArrayList<Variable>(), PotentialRole.CONDITIONAL_PROBABILITY);
			projectedPotential.values[0] = numericValue;
		}
		return Arrays.asList(projectedPotential);
	}

	@Override public Potential copy() {
		return new DeltaPotential(this);
	}

	@Override public boolean isUncertain() {
		return false;
	}

	public State getState() {
		return state;
	}

	public double getNumericValue() {
		return numericValue;
	}

	public int getStateIndex() {
		return stateIndex;
	}

	public void setValue(State state) {
		this.state = state;
		stateIndex = getConditionedVariable().getStateIndex(state);
	}

	public void setValue(double numericValue) {
		this.numericValue = numericValue;
	}

	@Override public String toString() {
		return variables.get(0) + " = " + (state != null ? state.getName() : numericValue);
	}

	@Override public Collection<Finding> getInducedFindings(EvidenceCase evidenceCase)
			throws IncompatibleEvidenceException, WrongCriterionException {
		Finding inducedFinding = null;
		if (getConditionedVariable().getVariableType() == VariableType.FINITE_STATES) {
			inducedFinding = new Finding(getConditionedVariable(), state);
		} else {
			inducedFinding = new Finding(getConditionedVariable(), numericValue);
		}
		return Arrays.asList(inducedFinding);
	}

	@Override public void scalePotential(double scale) throws UnsupportedOperationException {
		this.numericValue *= scale;
//		throw new UnsupportedOperationException();
	}

	@Override public Potential deepCopy(ProbNet copyNet) {
		DeltaPotential potential = (DeltaPotential) super.deepCopy(copyNet);

		potential.numericValue = this.numericValue;

		if (this.state != null) {
			potential.setValue(new State(this.state));
		}
		potential.stateIndex = this.stateIndex;

		return potential;

	}

}
