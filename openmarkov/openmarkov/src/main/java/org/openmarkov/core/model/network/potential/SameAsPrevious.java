/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.model.network.potential;

import java.util.List;

import org.openmarkov.core.exception.NodeNotFoundException;
import org.openmarkov.core.exception.NonProjectablePotentialException;
import org.openmarkov.core.exception.WrongCriterionException;
import org.openmarkov.core.inference.InferenceOptions;
import org.openmarkov.core.model.network.EvidenceCase;
import org.openmarkov.core.model.network.Node;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.potential.plugin.PotentialType;

/**
 * @author marias
 * @version 1.0
 */
@PotentialType(name = "Same as previous", family = "Temporal") public class SameAsPrevious extends Potential {
	// Constructors

	/**
	 * @param variables List of variables
	 */
	public SameAsPrevious(List<Variable> variables) {
		super(variables, PotentialRole.CONDITIONAL_PROBABILITY);
	}

	//    /**
	//     * Utility constructor
	//     * @param variable
	//     * @throws NodeNotFoundException
	//     * @throws NodeNotFoundException
	//     */
	//    public SameAsPrevious (Variable variable)
	//     {
	//         super (variable, new ArrayList<Variable>());
	//     }

	/**
	 * Copy constructor
	 *
	 * @param potential Potential
	 */
	public SameAsPrevious(SameAsPrevious potential) {
		super(potential);
	}

	/**
	 * Returns if an instance of a certain Potential type makes sense given the
	 * variables and the potential role
	 * @param node Node
	 * @param variables List of variables
	 * @param role Potential role
	 * @return True if it is valid
	 */
	public static boolean validate(Node node, List<Variable> variables, PotentialRole role) {
		return node.getVariable().isTemporal() && node.getVariable().getTimeSlice() > 0;
	}

	// Methods
	@Override public List<TablePotential> tableProject(EvidenceCase evidenceCase, InferenceOptions inferenceOptions,
			List<TablePotential> projectedPotentials) throws NonProjectablePotentialException, WrongCriterionException {
		throw new NonProjectablePotentialException("SameAsPrevious potentials cannot be projected");
	}

	public Potential getOriginalPotential(ProbNet probNet) {
		Potential originalPotential = null;
		try {
			originalPotential = getOriginalPotential(probNet, getConditionedVariable());
		} catch (NodeNotFoundException e) {
			e.printStackTrace();
		}
		return originalPotential;
	}

	public Potential sample() {
		throw new IllegalArgumentException("SameAsPrevious potentials cannot be sampled.");
	}

	private Potential getOriginalPotential(ProbNet probNet, Variable variable) throws NodeNotFoundException {
		Potential previousPotential = null;
		if (variable.isTemporal()) {
			int timeSlice = variable.getTimeSlice();
			Variable previousVariable = null;
			while (timeSlice > 0 && previousVariable == null) {
				try {
					previousVariable = probNet.getVariable(variable.getBaseName(), --timeSlice);
					previousPotential = probNet.getNode(previousVariable).getPotentials().get(0);
					if (previousPotential instanceof SameAsPrevious) {
						previousVariable = null;
					}
				} catch (NodeNotFoundException e) {
				}
			}
			if (previousVariable == null) {// There is no previous variable
				throw new NodeNotFoundException(probNet,
						"It does not exists a " + "previous variable called: " + variable.getName()
								+ " in this probNet");
			}
		} else {
			throw new NodeNotFoundException(probNet, "Variable has not a temporal " + "type name: varName[number].");
		}
		return previousPotential;
	}

	@Override public Potential copy() {
		return new SameAsPrevious(this);
	}

	@Override public String toString() {
		return super.toString() + " = SameAsPrevious";
	}

	@Override public void replaceNumericVariable(Variable convertedParentVariable) {
		super.replaceNumericVariable(convertedParentVariable);
	}

	@Override public boolean isUncertain() {
		throw new IllegalArgumentException("There is no way to know whether SameAsPrevious potentials are uncertain");
	}

	@Override public void scalePotential(double scale) {

	}

	@Override public Potential deepCopy(ProbNet copyNet) {
		return (SameAsPrevious) super.deepCopy(copyNet);
	}

}
