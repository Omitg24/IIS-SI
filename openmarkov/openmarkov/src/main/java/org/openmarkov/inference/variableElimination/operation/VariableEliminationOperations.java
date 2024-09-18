/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.inference.variableElimination.operation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.potential.Potential;
import org.openmarkov.core.model.network.potential.TablePotential;

public class VariableEliminationOperations {

	/**
	 * @param utilityPotentialsVariable
	 * @param decisionsUntilEnd
	 * @return List of ordered TablePotentials
	 */
	public static List<TablePotential> orderPotentialsByPartialOrder(List<TablePotential> utilityPotentialsVariable,
			List<Variable> decisionsUntilEnd) {

		List<TablePotential> orderedPotentials = new ArrayList<>();
		Set<TablePotential> setOfInputPotentials = new HashSet<>();

		for (Potential utilityPotentialVariable : utilityPotentialsVariable) {
			setOfInputPotentials.add((TablePotential) utilityPotentialVariable);
		}

		Set<TablePotential> potentialsWithoutIntervention = new HashSet<>();
		//Remove from inputPotentials the potentials without Interventions
		for (TablePotential inputPotential : setOfInputPotentials) {
			if (!VariableEliminationOperations.containsInterventions(inputPotential)) {
				potentialsWithoutIntervention.add(inputPotential);
			}
		}

		if (decisionsUntilEnd != null) {
			for (Variable decision : decisionsUntilEnd) {
				Set<TablePotential> potentialsWithDecisionInIntervention;
				potentialsWithDecisionInIntervention = VariableEliminationOperations.
						getPotentialsWithDecisionInIntervention(decision, setOfInputPotentials);
				setOfInputPotentials.removeAll(potentialsWithDecisionInIntervention);
				orderedPotentials.addAll(potentialsWithDecisionInIntervention);
			}
		}

		orderedPotentials.addAll(potentialsWithoutIntervention);
		setOfInputPotentials.removeAll(potentialsWithoutIntervention);
		orderedPotentials.addAll(setOfInputPotentials);

		return orderedPotentials;
	}

	/**
	 * @param tablePotential
	 * @return if the TablePotential contains one or more interventions.
	 */
	public static boolean containsInterventions(TablePotential tablePotential) {
		boolean containsInterventions = false;
		if (tablePotential.strategyTrees != null) {
			for (int i = 0; !containsInterventions && i < tablePotential.strategyTrees.length; i++) {
				containsInterventions |= tablePotential.strategyTrees[i] != null;
			}
		}
		return containsInterventions;
	}

	/**
	 * @param decision
	 * @param potentials
	 * @return
	 */
	public static Set<TablePotential> getPotentialsWithDecisionInIntervention(Variable decision,
			Collection<TablePotential> potentials) {

		Set<TablePotential> potentialsWithDecisionInIntervention = new HashSet<TablePotential>();
		for (TablePotential potential : potentials) {
			if (VariableEliminationOperations.hasDecisionInIntervention(decision, potential)) {
				potentialsWithDecisionInIntervention.add(potential);
			}
		}
		return potentialsWithDecisionInIntervention;
	}

	/**
	 * @param decision
	 * @param tablePotential
	 * @return
	 */
	public static boolean hasDecisionInIntervention(Variable decision, TablePotential tablePotential) {
		return containsInterventions(tablePotential) && tablePotential.strategyTrees[0]
				.hasInterventionForDecision(decision);
	}

}
