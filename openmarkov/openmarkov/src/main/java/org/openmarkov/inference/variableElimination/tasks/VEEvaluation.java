/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.inference.variableElimination.tasks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.openmarkov.core.exception.IncompatibleEvidenceException;
import org.openmarkov.core.exception.NotEvaluableNetworkException;
import org.openmarkov.core.exception.UnexpectedInferenceException;
import org.openmarkov.core.inference.heuristic.EliminationHeuristic;
import org.openmarkov.core.inference.tasks.Evaluation;
import org.openmarkov.core.inference.tasks.OptimalPolicies;
import org.openmarkov.core.inference.tasks.TaskUtilities;
import org.openmarkov.core.model.network.NodeType;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.potential.Potential;
import org.openmarkov.core.model.network.potential.PotentialRole;
import org.openmarkov.core.model.network.potential.StrategyTree;
import org.openmarkov.core.model.network.potential.TablePotential;
import org.openmarkov.core.model.network.potential.UniformPotential;
import org.openmarkov.inference.variableElimination.VariableEliminationCore;

/**
 * Task: Evaluation
 * <p>
 * Input: a symmetric network (usually containing decisions and utility nodes)
 * Optional input: pre-resolution evidence (E), imposed policies,
 * and observable variables (O) [for the evaluation of DANs]
 * </p>
 * Output: the global expected utility U(E,O), the probability P(E,O),
 * and the optimal policies (a table for each decision)
 *
 * @author mluque
 * @author fjdiez
 * @author marias
 * @author jperez-martin
 * @author artasom
 */

public class VEEvaluation extends VariableElimination implements Evaluation, OptimalPolicies {

	private VariableEliminationCore variableEliminationCore = null;
	private Variable decisionVariable;

	public VEEvaluation(ProbNet network) throws NotEvaluableNetworkException {
		super(network);
	}

	/**
	 * Preprocess the network and run the algorithm
	 *
	 * @throws IncompatibleEvidenceException Incompatible evidence exception
	 * @throws UnexpectedInferenceException  Unexpected inference execption
	 * @throws NotEvaluableNetworkException  Not evaluable network exception
	 */
	private void resolve()
			throws IncompatibleEvidenceException, UnexpectedInferenceException, NotEvaluableNetworkException {
		LogManager.getLogger(getClass()).trace("Resolving VEEvaluation");

		generalPreprocessing();
		unicriterionPreprocess();
		exactAlgorithmsPreprocessing();

		ProbNet markovNetworkInference = TaskUtilities
				.projectTablesAndBuildMarkovDecisionNetwork(probNet, getPreResolutionEvidence());

		// Build list of variables to eliminate
		List<Variable> variablesToEliminate = markovNetworkInference.getChanceAndDecisionVariables();

		// If the user sets a decision variable, remove from variables to eliminate
		if (decisionVariable != null) {
			variablesToEliminate.remove(decisionVariable);
		}

		// Create heuristic instance
		EliminationHeuristic heuristic = heuristicFactory(probNet, new ArrayList<Variable>(),
				getPreResolutionEvidence().getVariables(), getConditioningVariables(), variablesToEliminate);

		variableEliminationCore = new VariableEliminationCore(markovNetworkInference, heuristic, true);
	}

	@Override public TablePotential getProbability()
			throws IncompatibleEvidenceException, UnexpectedInferenceException, NotEvaluableNetworkException {
		if (variableEliminationCore == null) {
			resolve();
		}
		return variableEliminationCore.getProbability();
	}

	@Override public TablePotential getUtility()
			throws UnexpectedInferenceException, IncompatibleEvidenceException, NotEvaluableNetworkException {
		if (variableEliminationCore == null) {
			resolve();
		}
		return variableEliminationCore.getUtility();
	}

	@Override public StrategyTree getOptimalStrategyTree()
			throws UnexpectedInferenceException, IncompatibleEvidenceException, NotEvaluableNetworkException {
		if (variableEliminationCore == null) {
			resolve();
		}
		return variableEliminationCore.getUtility().strategyTrees[0];
	}

	@Override public HashMap<Variable, Potential> getOptimalPolicies()
			throws UnexpectedInferenceException, NotEvaluableNetworkException, IncompatibleEvidenceException {
		if (variableEliminationCore == null) {
			resolve();
		}

		//noinspection unchecked
		HashMap<Variable, Potential> optimalPolicies = (HashMap<Variable, Potential>) variableEliminationCore.getOptimalPolicies();
		
		// Set uniform policy to those nodes without policy calculated by the inference algorithm (and without imposed policy)
		for (Variable dec: probNet.getVariables(NodeType.DECISION)) {
			if (!optimalPolicies.containsKey(dec) && !TaskUtilities.hasImposedPolicy(probNet, dec)) {
				Potential pot = new UniformPotential(Arrays.asList(dec),PotentialRole.CONDITIONAL_PROBABILITY);
				optimalPolicies.put(dec, pot);
			}
		}
		
		return optimalPolicies;
	}

	@Override public Potential getOptimalPolicy(Variable decision)
			throws UnexpectedInferenceException, NotEvaluableNetworkException, IncompatibleEvidenceException {
		getOptimalPolicies();
		return variableEliminationCore.getOptimalPolicy(decision);
	}

	public void setDecisionVariable(Variable decisionVariable) {
		this.decisionVariable = decisionVariable;
	}

}