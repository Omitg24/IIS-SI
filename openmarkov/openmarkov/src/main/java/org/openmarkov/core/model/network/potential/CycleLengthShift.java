/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.model.network.potential;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.openmarkov.core.exception.NonProjectablePotentialException;
import org.openmarkov.core.inference.InferenceOptions;
import org.openmarkov.core.model.network.CycleLength;
import org.openmarkov.core.model.network.EvidenceCase;
import org.openmarkov.core.model.network.Finding;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.VariableType;
import org.openmarkov.core.model.network.potential.plugin.PotentialType;

/**
 * Potential identical to another but moved to another temporal slice.
 *
 * @author marias
 * @version 1.0
 */
@PotentialType(name = "CycleLengthShift", family = "Temporal") public class CycleLengthShift extends Potential {

	private CycleLength cycleLength;

	// Constructor

	/**
	 * @param variables   list of variables
	 * @param cycleLength cycle length of the potential
	 */
	public CycleLengthShift(List<Variable> variables, CycleLength cycleLength) {
		super(variables, PotentialRole.CONDITIONAL_PROBABILITY);
		this.cycleLength = cycleLength;
	}

	// public CycleLengthShift(Potential potential) {
	// super(potential);
	// }

	public CycleLengthShift(CycleLengthShift potential) {
		super(potential);
		this.cycleLength = potential.cycleLength;

	}

	public CycleLength getCycleLength() {
		return cycleLength;
	}

	/**
	 * Returns if an instance of a certain Potential type makes sense given the
	 * variables and the potential role
	 *
	 * @param variables List of variables
	 * @param role      PotentialRole
	 * @return True if it is valid
	 */
	public static boolean validate(List<Variable> variables, PotentialRole role) {
		return role == PotentialRole.CONDITIONAL_PROBABILITY && variables.size() == 2
				// child = variables.get (0)
				// parent = variables.get (1)
				&& variables.get(0).isTemporal() && variables.get(1).isTemporal() && variables.get(0).getBaseName()
				.equals(variables.get(1).getBaseName())
				&& variables.get(0).getTimeSlice() == variables.get(1).getTimeSlice() + 1;
	}

	// Methods
	@Override public List<TablePotential> tableProject(EvidenceCase evidenceCase, InferenceOptions inferenceOptions,
			List<TablePotential> projectedPotentials) throws NonProjectablePotentialException {
		Variable conditionedVariable = getConditionedVariable();
		Variable conditioningVariable = variables.get((conditionedVariable == variables.get(0)) ? 1 : 0);
		TablePotential projectedPotential;
		if (conditionedVariable.getVariableType() == VariableType.NUMERIC) {
			for (Variable variable : variables) {
				if (!variable.equals(conditionedVariable) && !evidenceCase.contains(variable)) {
					throw new NonProjectablePotentialException(
							"Variable " + variable.getName() + " is not included in EvidenceCase.");
				}
			}
			projectedPotential = new TablePotential(new ArrayList<Variable>(), role);
			projectedPotential.values[0] = evidenceCase.getNumericalValue(conditioningVariable) + cycleLength
					.getValue();
		} else {
			// Build projected potential based on parent's potential
			TablePotential projectedParentPotential = findPotentialByVariable(conditioningVariable,
					projectedPotentials);
			List<Variable> projectedVariables = projectedParentPotential.getVariables();
			// replace parent variable with child variable in the list of
			// variables of the projected potential
			projectedVariables.remove(conditioningVariable);
			projectedVariables.add(0, conditionedVariable);
			projectedPotential = new TablePotential(projectedVariables, role);

			int numStates = conditionedVariable.getNumStates();
			int numStatesParent = conditioningVariable.getNumStates();
			int configurationIndex = 0;
			// Copy values from parent's projected potential, shifting values
			// one state
			for (int i = 0; i < projectedParentPotential.values.length; i += numStatesParent) {
				projectedPotential.values[configurationIndex * numStates] = 0;
				for (int j = 0; j < numStatesParent; ++j) {
					projectedPotential.values[configurationIndex * numStates + j + 1] = projectedParentPotential.values[
							i + j];
				}
				configurationIndex++;
			}
		}
		return Collections.singletonList(projectedPotential);
	}

	@Override public Collection<Finding> getInducedFindings(EvidenceCase evidenceCase) {
		Variable conditionedVariable = getConditionedVariable();
		Variable conditioningVariable = variables.get((conditionedVariable == variables.get(0)) ? 1 : 0);
		List<Finding> inducedFindings = new ArrayList<>();
		if (evidenceCase.contains(conditioningVariable) && !evidenceCase.contains(conditionedVariable)) {
			double numericalValue = evidenceCase.getFinding(conditioningVariable).getNumericalValue() + cycleLength
					.getValue();
			inducedFindings.add(new Finding(conditionedVariable, numericalValue));
		}
		return inducedFindings;
	}

	@Override public Potential copy() {
		List<Variable> copiedVariables = null;
		if (this.getVariables() != null && this.getVariables().size() != 0) {
			copiedVariables = new ArrayList<>(this.getVariables());
		}
		CycleLengthShift cycleLengthShift = new CycleLengthShift(copiedVariables, this.cycleLength);
		cycleLengthShift.comment = this.comment;
		return cycleLengthShift;

	}

	@Override public boolean isUncertain() {
		return false;
	}

	@Override public String toString() {
		return super.toString() + " = CycleLengthShift";
	}

	@Override public void scalePotential(double scale) throws UnsupportedOperationException {
		throw new UnsupportedOperationException();

	}

	@Override public Potential deepCopy(ProbNet copyNet) {
		CycleLengthShift potential = (CycleLengthShift) super.deepCopy(copyNet);
		potential.cycleLength = this.cycleLength.clone();
		return potential;
	}

}
