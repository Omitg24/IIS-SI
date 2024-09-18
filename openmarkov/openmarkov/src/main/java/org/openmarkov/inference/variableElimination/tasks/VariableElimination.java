/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.inference.variableElimination.tasks;

import java.util.ArrayList;
import java.util.List;

import org.openmarkov.core.exception.NotEvaluableNetworkException;
import org.openmarkov.core.inference.BasicOperations;
import org.openmarkov.core.inference.InferenceAlgorithm;
import org.openmarkov.core.inference.heuristic.EliminationHeuristic;
import org.openmarkov.core.inference.heuristic.HeuristicFactory;
import org.openmarkov.core.inference.tasks.TaskUtilities;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.constraint.NoMixedParents;
import org.openmarkov.core.model.network.constraint.OnlyAtemporalVariables;
import org.openmarkov.core.model.network.constraint.PNConstraint;
import org.openmarkov.core.model.network.type.BayesianNetworkType;
import org.openmarkov.core.model.network.type.DecisionAnalysisNetworkType;
import org.openmarkov.core.model.network.type.InfluenceDiagramType;
import org.openmarkov.core.model.network.type.MIDType;
import org.openmarkov.core.model.network.type.NetworkType;
import org.openmarkov.inference.heuristic.simpleElimination.SimpleElimination;

public abstract class VariableElimination extends InferenceAlgorithm {

	/**
	 * Elimination heuristic factory
	 **/
	private HeuristicFactory heuristicFactory;

	/*
	 * Policies set by the user. The optimal policy would only be calculated for the decisions
	 * without imposed policies.
	 * Each policy is stochastic, which implies it is a probability potential whose domain
	 * contains the decision.
	 */
	//private List<TablePotential> imposedPolicies;

	/**
	 * @param network The network used in the inference
	 */
	public VariableElimination(ProbNet network) throws NotEvaluableNetworkException {
		super(network);

		setHeuristicFactory(new HeuristicFactory() {
			@Override public EliminationHeuristic getHeuristic(ProbNet probNet, List<List<Variable>> variables) {
				return new SimpleElimination(probNet, variables);
			}
		});
	}

	/**
	 * This operations transform a PGM into another PGM of the same type that can be evaluated by the algorithm
	 */
	void generalPreprocessing() {
		boolean isTemporal = !probNet.hasConstraint(OnlyAtemporalVariables.class);

		// 2. If the network has temporal nodes, expand the network to the specified horizon.
		if (isTemporal) {
			probNet = TaskUtilities.expandNetwork(probNet, true);
		}

		//TODO - Check 3. Convert the decision nodes with imposed policies into chance nodes
		probNet = TaskUtilities.imposePolicies(probNet);

		// 4. Extend the pre-resolution evidence (the post-resolution evidence is not taken into account). Some potentials may generate pre-resolution findings; for example, a delta potential or a table in which the probability of one of the values (states) of the variables is 1.
		probNet = TaskUtilities.extendPreResolutionEvidence(probNet, getPreResolutionEvidence());

		// 5. If the network has temporal nodes, apply discounts.
		if (isTemporal) {
			probNet = TaskUtilities.applyDiscounts(probNet, true);
			probNet = TaskUtilities.applyTransitionTime(probNet, true);
		}
	}

	/**
	 * This procedure is used by all exact algorithms: variable elimination, clustering, arc reversal, etc. It consists of three steps:
	 * <p>
	 * 1. Absorb intermediate numeric nodes recursively, when possible.
	 * 2. Discretize the non-observed numeric variables.
	 * 3. Extend of the pre-resolution evidence for the new potentials.
	 */
	void exactAlgorithmsPreprocessing() {
		probNet = TaskUtilities.discretizeNonObservedNumericVariables(probNet, getPreResolutionEvidence());
		probNet = TaskUtilities.absorbAllIntermediateNumericNodes(probNet, getPreResolutionEvidence());
		// TODO - Implement: Extend ALL the evidence for the new potentials.

	}

	/**
	 * When the network has more than one criterion, multiply each criterion by its transformation factor
	 * (at the end all variables will have the same measuring units)
	 */
	void unicriterionPreprocess() {
		probNet = TaskUtilities.scaleUtilitiesUnicriterion(probNet);
	}

	/**
	 * Multiply each criterion by its cost or effectiveness scale, at the end all utilities will have only two
	 * measuring units (one for the costs and the other for the effectiveness)
	 */
	void bicriteriaPreprocess() {
		probNet = TaskUtilities.scaleUtilitiesCostEffectiveness(probNet);
	}

	@Override protected List<NetworkType> getPossibleNetworkTypes() {
		List<NetworkType> possibleNetworkTypes = new ArrayList<>();
		possibleNetworkTypes.add(BayesianNetworkType.getUniqueInstance());
		possibleNetworkTypes.add(InfluenceDiagramType.getUniqueInstance());
		possibleNetworkTypes.add(MIDType.getUniqueInstance());
		possibleNetworkTypes.add(DecisionAnalysisNetworkType.getUniqueInstance());
		return possibleNetworkTypes;
	}

	@Override protected List<PNConstraint> getAdditionalConstraints() {
		List<PNConstraint> constraints = new ArrayList<>();
		constraints.add(new NoMixedParents());
		//constraints.add(new NoSuperValueNode());
		return constraints;
	}

	private void setHeuristicFactory(HeuristicFactory heuristicFactory) {
		this.heuristicFactory = heuristicFactory;
	}

	protected EliminationHeuristic heuristicFactory(ProbNet markovNetworkInference, List<Variable> queryVariables,
			List<Variable> evidenceVariables, List<Variable> conditioningVariables,
			List<Variable> variablesToEliminate) {
		List<List<Variable>> projectedOrderVariables = BasicOperations
				.projectPartialOrder(this.probNet, queryVariables, evidenceVariables, conditioningVariables,
						variablesToEliminate);
		return heuristicFactory.getHeuristic(markovNetworkInference, projectedOrderVariables);
	}

}
