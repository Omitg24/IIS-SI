/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.inference.decompositionIntoSymmetricDANs.ceanalysis;

import java.util.List;

import org.openmarkov.core.exception.IncompatibleEvidenceException;
import org.openmarkov.core.exception.NotEvaluableNetworkException;
import org.openmarkov.core.exception.UnexpectedInferenceException;
import org.openmarkov.core.inference.tasks.CEAnalysis;
import org.openmarkov.core.model.network.CEP;
import org.openmarkov.core.model.network.EvidenceCase;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.potential.GTablePotential;
import org.openmarkov.core.model.network.potential.TablePotential;
import org.openmarkov.inference.decompositionIntoSymmetricDANs.core.DANInference;

public abstract class DANCEAnalysis implements CEAnalysis {
	
	DANInference inferenceProcess;

	@Override public CEP getCEP()
			throws NotEvaluableNetworkException, IncompatibleEvidenceException, UnexpectedInferenceException {
		return (CEP) getUtility().elementTable.get(0);
	}

	@Override public GTablePotential getUtility()
			throws UnexpectedInferenceException, NotEvaluableNetworkException, IncompatibleEvidenceException {
		return (GTablePotential) inferenceProcess.getUtility();
	}

	@Override public TablePotential getProbability()
			throws UnexpectedInferenceException, NotEvaluableNetworkException, IncompatibleEvidenceException {
		return inferenceProcess.getProbability();
	}

	@Override
	public void setPreResolutionEvidence(EvidenceCase preresolutionEvidence) throws IncompatibleEvidenceException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setConditioningVariables(List<Variable> conditioningVariables) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setDecisionVariable(Variable decisionVariable) {
		// TODO Auto-generated method stub
		
	}

}
