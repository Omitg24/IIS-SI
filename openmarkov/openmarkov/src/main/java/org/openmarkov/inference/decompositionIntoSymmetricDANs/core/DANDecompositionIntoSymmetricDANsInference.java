/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.inference.decompositionIntoSymmetricDANs.core;

import java.util.ArrayList;
import java.util.List;

import org.openmarkov.core.exception.IncompatibleEvidenceException;
import org.openmarkov.core.exception.InvalidStateException;
import org.openmarkov.core.exception.NotEvaluableNetworkException;
import org.openmarkov.core.model.network.EvidenceCase;
import org.openmarkov.core.model.network.Node;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.State;
import org.openmarkov.core.model.network.Variable;

public class DANDecompositionIntoSymmetricDANsInference extends DANInference {

	public DANDecompositionIntoSymmetricDANsInference(ProbNet probNet, boolean isCEA) throws NotEvaluableNetworkException {
		this(probNet, null, isCEA);
	}

	public DANDecompositionIntoSymmetricDANsInference(ProbNet probNet, EvidenceCase evidence, boolean isCEA)
			throws NotEvaluableNetworkException {
		this(probNet, null, evidence, isCEA);
	}

	public DANDecompositionIntoSymmetricDANsInference(ProbNet dan, List<Variable> conditioningVariablesList,
			EvidenceCase evidenceCase, boolean isCEA) throws NotEvaluableNetworkException {
		super(dan,isCEA);
		List<Variable> alwaysObservedVariables = getAlwaysObservedVariables(dan, conditioningVariablesList, evidenceCase);

		List<Variable> asymmetricObservedVariables = DANOperations.getAsymmetricObservableVariables(dan);
		List<Variable> symmetricObservedVariables = new ArrayList<>();
		symmetricObservedVariables.addAll(alwaysObservedVariables);
		symmetricObservedVariables.removeAll(asymmetricObservedVariables);
		List<Variable> newConditioningVariablesList = DANOperations
				.join(conditioningVariablesList, symmetricObservedVariables);
		if (DANOperations.isSymmetric(dan, evidenceCase)) {//The DAN is symmetric		
			DANConditionalSymmetricInference evaluation = new DANConditionalSymmetricInference(dan,
					newConditioningVariablesList, evidenceCase, isCEA);
			setProbabilityAndUtilityFromEvaluation(evaluation);
		} else {// The DAN is asymmetric
			if (!asymmetricObservedVariables
					.isEmpty()) { // If O_A is not empty, then some always-observed variable introduces asymmetries
				Variable x = DANOperations.selectVariableWithoutAncestorsInVariables(asymmetricObservedVariables, dan);
				for (State state : x.getStates()) {
					ProbNet dan_x = DANOperations.instantiate(dan, x, state);
					try {
						childEvaluationDecompositionIntoSymmetricDANs(dan_x, newConditioningVariablesList,
								DANOperations.extendEvidenceCase(evidenceCase, x, state));
					} catch (InvalidStateException | IncompatibleEvidenceException e) {
						e.printStackTrace();
					}
				}
				conditionEliminateChanceAndSetProbabilityAndUtility(dan, x);
			} else {// If O_A is empty, then some variable introduces asymmetries
				Variable rootDecision;
				List<Node> nextDecisions = DANOperations.getNextDecisions(dan);
				if (nextDecisions.size() == 1) { // If exactly one decision D can be made first
					rootDecision = nextDecisions.get(0).getVariable();
					for (State state : rootDecision.getStates()) {
						ProbNet dan_x = DANOperations.instantiate(dan, rootDecision, state);
						childEvaluationDecompositionIntoSymmetricDANs(dan_x, newConditioningVariablesList,
								evidenceCase);
					}
				} else {// Several decisions can be made first
					// Prioritize each decision
					rootDecision = DANOperations.createDummyVariableOfOrder(nextDecisions);
					nextDecisions.forEach(decision -> prioritizeDANAndChildEvaluationDecompositionIntoSymmetricDANs(dan, evidenceCase,
								newConditioningVariablesList, decision));
				}
				conditionMaximizeAndSetProbabilityAndUtility(dan, rootDecision);
			}
		}

		for (Variable auxSymVariable : symmetricObservedVariables) {
			eliminateChanceVariable(dan, auxSymVariable, this.getProbability(), this.getUtility());
		}
	}

	private void prioritizeDANAndChildEvaluationDecompositionIntoSymmetricDANs(ProbNet dan, EvidenceCase evidenceCase,
			List<Variable> newConditioningVariablesList, Node decision) {
		ProbNet prioritizedDAN = DANOperations.prioritize(dan, decision);
		childEvaluationDecompositionIntoSymmetricDANs(prioritizedDAN, newConditioningVariablesList,
				evidenceCase);
	}

	public static boolean isEmpty(List<Variable> list) {
		return ((list == null) || (list.size() == 0));
	}

	/**
	 * @param dan                   ProbNet corresponding to a Decision Analysis Network (DAN)
	 * @param conditioningVariables
	 * @param evidenceCase          This method (1) executes a recursive (child) evaluation with
	 *                              parameters 'dan', 'conditioningVariables', and 'evidenceCase',
	 *                              (2) adds the results of the child evaluation to the attributes
	 *                              of this object 'childrenProbability' and 'childrenUtility'
	 */
	private void childEvaluationDecompositionIntoSymmetricDANs(ProbNet dan, List<Variable> conditioningVariables,
			EvidenceCase evidenceCase) {
		DANDecompositionIntoSymmetricDANsInference auxEval = null;
		try {
			auxEval = new DANDecompositionIntoSymmetricDANsInference(dan, conditioningVariables, evidenceCase,isCEAnalysis);
		} catch (NotEvaluableNetworkException e) {
			e.printStackTrace();
		}
		addResultsOfChildEvaluation(auxEval);

	}

	/*
	 * @Override public StrategyTree getOptimalStrategyTree() throws
	 * UnexpectedInferenceException, IncompatibleEvidenceException,
	 * NotEvaluableNetworkException { return null; }
	 * 
	 * @Override public void setPreResolutionEvidence(EvidenceCase
	 * preresolutionEvidence) {
	 * 
	 * }
	 * 
	 * @Override public void setConditioningVariables(List<Variable>
	 * conditioningVariables) {
	 * 
	 * }
	 */
}

