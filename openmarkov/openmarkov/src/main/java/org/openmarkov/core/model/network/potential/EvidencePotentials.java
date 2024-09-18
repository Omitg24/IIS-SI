/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.model.network.potential;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

//import java.lang.Thread.State;

import org.openmarkov.core.model.network.EvidenceCase;
import org.openmarkov.core.model.network.Variable;

public class EvidencePotentials {

	/**
	 * For each {@code variableOfInterest} contained in
	 * {@code evidence}, this method generates an evidence potential. This
	 * potential contains only one variable and all the values of the potential
	 * are 0.0 except the value corresponding to the finding.<p>
	 * Finally the method inserts the new potential in
	 * {@code individualProbabilities}.
	 *
	 * @param individualProbabilities {@code HashMap} of key = String
	 *                                with variable name and value = Potential.
	 * @param variablesOfInterest     {@code ArrayList} of
	 *                                {@code Variable}
	 * @param evidence                {@code EvidenceCase}
	 * @return Evidence potentials
	 */
	public static HashMap<Variable, TablePotential> addEvidencePotentials(
			HashMap<Variable, TablePotential> individualProbabilities, List<Variable> variablesOfInterest,
			EvidenceCase evidence) {

		// Creates a fast structure for consultation with evidence variables
		if ((evidence != null) && (!evidence.isEmpty())) {
			HashSet<Variable> evidenceVariables = new HashSet<>(evidence.getVariables());

			for (Variable variable : variablesOfInterest) {
				if (evidenceVariables.contains(variable)) {
					// Creates a potential with the evidence variable
					List<Variable> potentialVariables = new ArrayList<>(1);
					potentialVariables.add(variable);
					TablePotential potential = null;
					potential = new TablePotential(potentialVariables, PotentialRole.CONDITIONAL_PROBABILITY);
					int indexStateEvidence = evidence.getState(variable);
					for (int indexState = 0; indexState < variable.getStates().length; indexState++) {
						// Sets potential table configurations
						potential.values[indexState] = (indexState == indexStateEvidence) ? 1.0 : 0.0;
					}
					// Inserts potential in individualProbabilities
					individualProbabilities.put(variable, potential);
				}
			}
		}
		return individualProbabilities;
	}

}
