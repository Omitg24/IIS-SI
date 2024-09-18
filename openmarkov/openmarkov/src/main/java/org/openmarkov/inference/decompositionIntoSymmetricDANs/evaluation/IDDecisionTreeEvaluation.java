package org.openmarkov.inference.decompositionIntoSymmetricDANs.evaluation;

import org.openmarkov.core.exception.NotEvaluableNetworkException;
import org.openmarkov.core.model.network.EvidenceCase;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.inference.decompositionIntoSymmetricDANs.core.IDDecisionTreeInference;

public class IDDecisionTreeEvaluation extends DANDecisionTreeEvaluation {

		
	public IDDecisionTreeEvaluation(ProbNet network, boolean computeDecisionTreeForGUI) throws NotEvaluableNetworkException {
			inferenceProcess = new IDDecisionTreeInference(network, computeDecisionTreeForGUI, false);		
	}

	public IDDecisionTreeEvaluation(ProbNet probNet, EvidenceCase evidenceCase) throws NotEvaluableNetworkException {
			inferenceProcess = new IDDecisionTreeInference(probNet, evidenceCase, false);		
	}

	public IDDecisionTreeEvaluation(ProbNet probNet, int depth, boolean computeDecisionTreeForGUI) throws NotEvaluableNetworkException {
			inferenceProcess = new IDDecisionTreeInference(probNet, depth, computeDecisionTreeForGUI, false);		
	}
	
	/**
	 * @param probNet
	 * @param depth Number of levels expanded in the decision tree (the rest of levels are summarized as utilities in the leaves)
	 * @param computeDecisionTreeForGUI
	 * @param evidence
	 * @throws NotEvaluableNetworkException
	 */
	public IDDecisionTreeEvaluation(ProbNet probNet, int depth, boolean computeDecisionTreeForGUI, EvidenceCase evidence) throws NotEvaluableNetworkException {
		inferenceProcess = new IDDecisionTreeInference(probNet, depth, computeDecisionTreeForGUI, evidence);		
}

	


public IDDecisionTreeEvaluation() {
	// TODO Auto-generated constructor stub
}

}
