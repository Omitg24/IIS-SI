/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.inference;

import java.util.List;

import org.openmarkov.core.action.PNESupport;
import org.openmarkov.core.inference.heuristic.EliminationHeuristic;
import org.openmarkov.core.inference.heuristic.HeuristicFactory;
import org.openmarkov.core.model.network.EvidenceCase;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.Variable;

/**
 * @author mluque
 * @author marias
 * @author fjdiez
 */
public abstract class InferenceAlgorithmScheme {
	public EvidenceCase evidence;
	public List<Variable> variablesToEliminate;
	/**
	 * This is a copy of the {@code ProbNet} received.
	 */
	protected ProbNet probNet;
	/**
	 * For undo/redo operations.
	 */
	protected PNESupport pNESupport;
	/**
	 * Elimination heuristic factory
	 **/
	protected HeuristicFactory heuristicFactory;

	/*
	 * Policies set by the user. The optimal policy would only be calculated for the decisions
	 * without imposed policies.
	 * Each policy is stochastic, which implies it is a probability potential whose domain
	 * contains the decision.
	 */
	// TODO: remove???
	// private List<TablePotential> imposedPolicies;
	/**
	 * Variables that will not be eliminated during the inference, and therefore all the results
	 * contain these variables in the domain.
	 */
	protected List<Variable> conditioningVariables;
	protected EliminationHeuristic heuristic;
	/**
	 * Evidence introduced before the network is resolved.
	 * In influence diagrams this is Ezawa's evidence.
	 */
	private EvidenceCase preResolutionEvidence;
	/**
	 * Evidence when the network has been resolved.
	 * In influence diagrams this is Luque and Diez's evidence.
	 */
	private EvidenceCase postResolutionEvidence;

	/**
	 * @param probNet The network used in the inference
	 */
	public InferenceAlgorithmScheme(ProbNet probNet) {
		this.probNet = probNet.copy();
		preResolutionEvidence = new EvidenceCase();
		postResolutionEvidence = new EvidenceCase();
	}

	/**
	 * @return The post-resolution evidence.
	 */
	public EvidenceCase getPostResolutionEvidence() {
		return postResolutionEvidence;
	}

	/**
	 * @param postResolutionEvidence Post-resolution evidence
	 */
	public void setPostResolutionEvidence(EvidenceCase postResolutionEvidence) {
		this.postResolutionEvidence = postResolutionEvidence;
	}

	/**
	 * @return The pre-resolution evidence
	 */
	public EvidenceCase getPreResolutionEvidence() {
		return preResolutionEvidence;
	}

	/**
	 * @param preResolutionEvidence The pre-resolution evidence to set
	 */
	public void setPreResolutionEvidence(EvidenceCase preResolutionEvidence) {
		this.preResolutionEvidence = preResolutionEvidence;
	}

	/**
	 * @return The conditioning variables
	 */
	public List<Variable> getConditioningVariables() {
		return conditioningVariables;
	}

	/**
	 * @param conditioningVariables The conditioning variables to set
	 */
	public void setConditioningVariables(List<Variable> conditioningVariables) {
		this.conditioningVariables = conditioningVariables;
	}
}