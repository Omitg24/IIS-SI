/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.inference.variableElimination.tasks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.openmarkov.core.exception.IncompatibleEvidenceException;
import org.openmarkov.core.exception.NodeNotFoundException;
import org.openmarkov.core.exception.NotEvaluableNetworkException;
import org.openmarkov.core.exception.UnexpectedInferenceException;
import org.openmarkov.core.inference.tasks.SensAnTornadoSpider;
import org.openmarkov.core.model.network.EvidenceCase;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.modelUncertainty.AxisVariation;
import org.openmarkov.core.model.network.modelUncertainty.SystematicSampling;
import org.openmarkov.core.model.network.modelUncertainty.UncertainParameter;
import org.openmarkov.core.model.network.potential.TablePotential;
import org.openmarkov.core.model.network.potential.operation.DiscretePotentialOperations;

/**
 * @author jperez-martin
 */
public class VESensAnTornadoSpider implements SensAnTornadoSpider {

	/**
	 * Potentials of each <code> UncertainParameter </code> conditioned on a dummy variable
	 */
	private HashMap<UncertainParameter, TablePotential> uncertainParametersPotentials;
	private ProbNet probNet;

	public VESensAnTornadoSpider(ProbNet network, EvidenceCase preResolutionEvidence,
			List<UncertainParameter> uncertainParameters, AxisVariation axisVariation, int numberOfIntervals)
			throws NotEvaluableNetworkException, IncompatibleEvidenceException {
		this(network, preResolutionEvidence, uncertainParameters, axisVariation, numberOfIntervals, null);
	}

	public VESensAnTornadoSpider(ProbNet network, EvidenceCase preResolutionEvidence,
			List<UncertainParameter> uncertainParameters, AxisVariation axisVariation, int numberOfIntervals,
			Variable decisionVariable) throws NotEvaluableNetworkException, IncompatibleEvidenceException {
		this.probNet = network.copy();
		uncertainParametersPotentials = new HashMap<>();

		String iterationVariableName = "***Iteration***";
		for (UncertainParameter uncertainParameter : uncertainParameters) {
			VEEvaluation veEvaluation = null;
			double hMin = axisVariation.getMinValue(uncertainParameter);
			double hMax = axisVariation.getMaxValue(uncertainParameter);
			ProbNet sampledProbNet = SystematicSampling
					.sampleNetwork(this.probNet, uncertainParameter, hMin, hMax, numberOfIntervals,
							iterationVariableName);

			List<Variable> conditioningVariables = new ArrayList<>();
			Variable conditionedVariable;
			try {
				conditionedVariable = sampledProbNet.getVariable(iterationVariableName);
				conditioningVariables.add(conditionedVariable);
			} catch (NodeNotFoundException e) {
				e.printStackTrace();
			}

			if (decisionVariable != null) {
				conditioningVariables.add(decisionVariable);
			}

			veEvaluation = new VEEvaluation(sampledProbNet);
			veEvaluation.setPreResolutionEvidence(preResolutionEvidence);
			veEvaluation.setConditioningVariables(conditioningVariables);

			// Collect the conditional potential
			TablePotential globalUtility = null;

			try {
				if (veEvaluation != null) {
					globalUtility = veEvaluation.getUtility();
				}

				if (decisionVariable != null && globalUtility != null) {
					globalUtility = DiscretePotentialOperations.reorder(globalUtility, conditioningVariables);
				}
			} catch (UnexpectedInferenceException e) {
				e.printStackTrace();
			}

			uncertainParametersPotentials.put(uncertainParameter, globalUtility);
		}
	}

	public HashMap<UncertainParameter, TablePotential> getUncertainParametersPotentials() {
		return uncertainParametersPotentials;
	}

	@Override public void setPreResolutionEvidence(EvidenceCase preresolutionEvidence) {

	}

	@Override public void setConditioningVariables(List<Variable> conditioningVariables) {

	}
}
