/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.inference.variableElimination.tasks;

import java.util.ArrayList;
import java.util.List;

import org.openmarkov.core.exception.IncompatibleEvidenceException;
import org.openmarkov.core.exception.NotEvaluableNetworkException;
import org.openmarkov.core.exception.UnexpectedInferenceException;
import org.openmarkov.core.inference.heuristic.EliminationHeuristic;
import org.openmarkov.core.inference.tasks.CEAnalysis;
import org.openmarkov.core.inference.tasks.TaskUtilities;
import org.openmarkov.core.model.network.CEP;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.potential.GTablePotential;
import org.openmarkov.core.model.network.potential.TablePotential;
import org.openmarkov.inference.variableElimination.VariableEliminationCore;

/**
 * @author jperez-martin
 */
public class VECEAnalysis extends VariableElimination implements CEAnalysis {

	private Variable decision;

	private GTablePotential utility;

	private TablePotential probability;

	/**
	 * @param network a symmetric network having at least two criteria (and usually decisions and utility nodes)
	 */
	public VECEAnalysis(ProbNet network) throws NotEvaluableNetworkException {
		super(network);
	}

	private void resolve() throws IncompatibleEvidenceException, UnexpectedInferenceException {
		generalPreprocessing();
		bicriteriaPreprocess();
		exactAlgorithmsPreprocessing();

		ProbNet markovNetworkInference = TaskUtilities
				.projectTablesAndBuildMarkovDecisionNetwork(probNet, getPreResolutionEvidence());

		// Build list of variables to eliminate
		List<Variable> variablesToEliminate = probNet.getChanceAndDecisionVariables();
		// And remove the received decision from them
		if (decision != null) {
			variablesToEliminate.remove(decision);
		}

		// Create heuristic instance
		EliminationHeuristic heuristic = heuristicFactory(markovNetworkInference, new ArrayList<Variable>(),
				getPreResolutionEvidence().getVariables(), getConditioningVariables(), variablesToEliminate);

		VariableEliminationCore variableEliminationCore = new VariableEliminationCore(markovNetworkInference, heuristic,
				false);

		utility = (GTablePotential) variableEliminationCore.getUtility();	
		probability = variableEliminationCore.getProbability();
	}

	@Override public GTablePotential getUtility() throws UnexpectedInferenceException, IncompatibleEvidenceException {
		if (utility == null) {
			resolve();
			return utility;
		}
		return utility;
	}

	@Override public TablePotential getProbability()
			throws UnexpectedInferenceException, NotEvaluableNetworkException, IncompatibleEvidenceException {
		if (probability == null) {
			resolve();
			return probability;
		}
		return probability;
	}

	@Override public void setDecisionVariable(Variable decisionVariable) {
		this.decision = decisionVariable;
	}

	@Override public CEP getCEP() throws IncompatibleEvidenceException, UnexpectedInferenceException {
		return (CEP) getUtility().elementTable.get(0);

	}
}
