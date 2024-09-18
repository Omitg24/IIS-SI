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
import org.openmarkov.core.inference.tasks.OptimalIntervention;
import org.openmarkov.core.model.network.EvidenceCase;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.ProbNetOperations;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.constraint.NoMixedParents;
import org.openmarkov.core.model.network.constraint.PNConstraint;
import org.openmarkov.core.model.network.potential.StrategyTree;
import org.openmarkov.core.model.network.type.BayesianNetworkType;
import org.openmarkov.core.model.network.type.DecisionAnalysisNetworkType;
import org.openmarkov.core.model.network.type.InfluenceDiagramType;
import org.openmarkov.core.model.network.type.MIDType;
import org.openmarkov.core.model.network.type.NetworkType;

/**
 * @author jperez-martin
 */
public class VEOptimalIntervention implements OptimalIntervention {

	private VEEvaluation veEvaluation = null;
	private ProbNet probNet;

	/**
	 * @param probNet a network (usually containing decisions and utility nodes)
	 * @throws NotEvaluableNetworkException
	 */
	public VEOptimalIntervention(ProbNet probNet) throws NotEvaluableNetworkException, IncompatibleEvidenceException {
		this(probNet, new EvidenceCase());
	}

	/**
	 * @param network               a network (usually containing decisions and utility nodes)
	 * @param preResolutionEvidence pre-resolution evidence
	 * @throws NotEvaluableNetworkException
	 */
	public VEOptimalIntervention(ProbNet network, EvidenceCase preResolutionEvidence)
			throws NotEvaluableNetworkException, IncompatibleEvidenceException {
		probNet = network.copy();

		veEvaluation = new VEEvaluation(network);
		veEvaluation.setPreResolutionEvidence(preResolutionEvidence);
	}

	@Override public StrategyTree getOptimalIntervention()
			throws IncompatibleEvidenceException, UnexpectedInferenceException, NotEvaluableNetworkException {
		return veEvaluation.getUtility().strategyTrees[0];
	}

	/**
	 * @return A new <code>ArrayList</code> of <code>PNConstraint</code>.
	 */
	public List<PNConstraint> initializeAdditionalConstraints() {
		List<PNConstraint> constraints = new ArrayList<>();
		constraints.add(new NoMixedParents());
		//constraints.add(new NoSuperValueNode());
		return constraints;
	}

	/**
	 * @return An <code>ArrayList</code> of <code>NetworkType</code> where the
	 * algorithm can be applied: Bayesian networks and influence
	 * diagrams.
	 */
	public List<NetworkType> initializeNetworkTypesApplicable() {
		List<NetworkType> networkTypes = new ArrayList<>();
		networkTypes.add(BayesianNetworkType.getUniqueInstance());
		networkTypes.add(InfluenceDiagramType.getUniqueInstance());
		networkTypes.add(MIDType.getUniqueInstance());
		if (probNet.getNetworkType().equals(DecisionAnalysisNetworkType.getUniqueInstance())) {
			if (!ProbNetOperations.hasOrderAsymmetry(probNet) && !ProbNetOperations.hasStructuralAsymmetry(probNet)) {
				networkTypes.add(DecisionAnalysisNetworkType.getUniqueInstance());
			}
		}
		return networkTypes;
	}

	@Override public void setPreResolutionEvidence(EvidenceCase preresolutionEvidence) {

	}

	@Override public void setConditioningVariables(List<Variable> conditioningVariables) {

	}
}
