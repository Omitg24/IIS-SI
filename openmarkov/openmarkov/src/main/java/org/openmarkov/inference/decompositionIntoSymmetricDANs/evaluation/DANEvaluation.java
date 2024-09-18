/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.inference.decompositionIntoSymmetricDANs.evaluation;

import java.util.List;

import org.openmarkov.core.exception.IncompatibleEvidenceException;
import org.openmarkov.core.exception.NotEvaluableNetworkException;
import org.openmarkov.core.exception.UnexpectedInferenceException;
import org.openmarkov.core.inference.tasks.Evaluation;
import org.openmarkov.core.model.network.EvidenceCase;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.potential.StrategyTree;
import org.openmarkov.core.model.network.potential.TablePotential;
import org.openmarkov.inference.decompositionIntoSymmetricDANs.core.DANInference;

public abstract class DANEvaluation implements Evaluation {
	
	DANInference inferenceProcess;
	
		
	public static boolean hasIncorrectProbability(TablePotential pot) {
		boolean isCorrect = true;
		if (pot != null) {
			double[] values = pot.values;
			for (int i = 0; i < values.length && isCorrect; i++) {
				double value = values[i];
				isCorrect = value >= 0.0 && value <= 1.0;
			}
		}
		return !isCorrect;
	}

	public static boolean containsValue(TablePotential pot, double v) {
		boolean containsValue = false;
		if (pot != null) {
			double[] values = pot.values;
			for (int i = 0; i < values.length && !containsValue; i++) {
				containsValue = values[i] == v;
			}
		}
		return containsValue;
	}


	public TablePotential getProbability()
			throws IncompatibleEvidenceException, UnexpectedInferenceException, NotEvaluableNetworkException {
		return inferenceProcess.getProbability();
	}

	public TablePotential getUtility() {
		return inferenceProcess.getUtility();
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
	public StrategyTree getOptimalStrategyTree()
			throws UnexpectedInferenceException, IncompatibleEvidenceException, NotEvaluableNetworkException {
		// TODO Auto-generated method stub
		return null;
	}


	
}
