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
import org.openmarkov.core.inference.tasks.ExpectedUtilityDecision;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.ProbNetOperations;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.potential.TablePotential;
import org.openmarkov.core.model.network.potential.operation.DiscretePotentialOperations;

/**
 * Task: resolution
 * <p>
 * Input: a symmetric network (usually containing decisions and utility nodes)
 * Optional input: pre-resolution evidence (E), imposed policies,
 * and observable variables (O) [for the evaluation of DANs]
 * <p>
 * Output: the global expected utility U(E,O), the probability P(E,O),
 * and the optimal policies (a table for each decision)
 *
 * @author mluque
 * @author fjdiez
 * @author marias
 * @author jperez-martin
 * @author artasom
 */

public class VEExpectedUtilityDecision extends VariableElimination implements ExpectedUtilityDecision {

	private Variable decision;

	private TablePotential result;

	/**
	 * @param network  Probabilistic network to be resolved
	 * @param decision The variable for which the expected utility is required
	 */
	public VEExpectedUtilityDecision(ProbNet network, Variable decision) throws NotEvaluableNetworkException {
		super(network);
		this.decision = decision;
	}

	private void resolve()
			throws NotEvaluableNetworkException, UnexpectedInferenceException, IncompatibleEvidenceException {
		List<Variable> informationalPredecesors = ProbNetOperations.getInformationalPredecessors(probNet, decision);
		List<Variable> orderedVariables = new ArrayList<>(informationalPredecesors);
		orderedVariables.remove(decision);
		orderedVariables.add(0, decision);

		VEEvaluation veEvaluation = new VEEvaluation(probNet);
		veEvaluation.setConditioningVariables(informationalPredecesors);

		result = DiscretePotentialOperations.reorder(veEvaluation.getUtility(), orderedVariables);
	}

	@Override public TablePotential getExpectedUtility()
			throws UnexpectedInferenceException, NotEvaluableNetworkException, IncompatibleEvidenceException {
		if (result == null) {
			resolve();
		}

		return result;
	}
}