package org.openmarkov.inference.decompositionIntoSymmetricDANs.ceanalysis;

import org.openmarkov.core.exception.NotEvaluableNetworkException;
import org.openmarkov.core.model.network.EvidenceCase;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.inference.decompositionIntoSymmetricDANs.core.DANDecompositionIntoSymmetricDANsInference;

public class DANDecompositionIntoSymmetricDANsCEA extends DANCEAnalysis {

	public DANDecompositionIntoSymmetricDANsCEA(ProbNet network) {
		this(network,null);
	}

	public DANDecompositionIntoSymmetricDANsCEA(ProbNet probNet, EvidenceCase evidenceCase) {
		try {
			inferenceProcess = new DANDecompositionIntoSymmetricDANsInference(probNet,true);
		} catch (NotEvaluableNetworkException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
