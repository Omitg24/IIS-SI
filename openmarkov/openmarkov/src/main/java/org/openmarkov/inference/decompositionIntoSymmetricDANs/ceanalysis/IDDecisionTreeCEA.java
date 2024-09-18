package org.openmarkov.inference.decompositionIntoSymmetricDANs.ceanalysis;

import org.openmarkov.core.exception.NotEvaluableNetworkException;
import org.openmarkov.core.model.network.EvidenceCase;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.inference.decompositionIntoSymmetricDANs.core.IDDecisionTreeInference;

public class IDDecisionTreeCEA extends DANDecisionTreeCEA {
	

	public IDDecisionTreeCEA(ProbNet network, boolean computeDecisionTreeForGUI) throws NotEvaluableNetworkException {
		inferenceProcess = new IDDecisionTreeInference(network, computeDecisionTreeForGUI, true);
	}

	public IDDecisionTreeCEA(ProbNet probNet, int depth, boolean computeDecisionTreeForGUI)
			throws NotEvaluableNetworkException {
		inferenceProcess = new IDDecisionTreeInference(probNet, depth, computeDecisionTreeForGUI);
	}

	/**
	 * @param probNet
	 * @param depth                     Number of levels expanded in the decision
	 *                                  tree (the rest of levels are summarized as
	 *                                  utilities in the leaves)
	 * @param computeDecisionTreeForGUI
	 * @param evidence
	 * @throws NotEvaluableNetworkException
	 */
	public IDDecisionTreeCEA(ProbNet probNet, int depth, boolean computeDecisionTreeForGUI, EvidenceCase evidence)
			throws NotEvaluableNetworkException {		
		inferenceProcess = new IDDecisionTreeInference(probNet, evidence, depth, computeDecisionTreeForGUI, true);
	}

}
