package org.openmarkov.inference.decompositionIntoSymmetricDANs.core;

import java.util.List;

import org.openmarkov.core.exception.IncompatibleEvidenceException;
import org.openmarkov.core.exception.NotEvaluableNetworkException;
import org.openmarkov.core.exception.UnexpectedInferenceException;
import org.openmarkov.core.model.network.EvidenceCase;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.potential.TablePotential;
import org.openmarkov.core.model.network.potential.operation.DiscretePotentialOperations;
import org.openmarkov.inference.variableElimination.tasks.VECEAnalysis;
import org.openmarkov.inference.variableElimination.tasks.VEEvaluation;
import org.openmarkov.inference.variableElimination.tasks.VariableElimination;

public class DANConditionalSymmetricInference extends DANInference {

	public DANConditionalSymmetricInference(ProbNet network, boolean isCEA) {
		super(network, isCEA);
		// TODO Auto-generated constructor stub
	}
	
	public DANConditionalSymmetricInference(ProbNet dan, List<Variable> conditioningVariables,
			EvidenceCase evidenceCase, boolean isCEA) throws NotEvaluableNetworkException {
		super(dan, isCEA);
		VariableElimination ver = null;
		TablePotential probability = null;
		TablePotential utility = null;
		boolean callInference = true;
		try {

			ver = (!isCEAnalysis? new VEEvaluation(dan):new VECEAnalysis(dan));
			ver.setPreResolutionEvidence(DANOperations.translateEvidenceTo(dan, evidenceCase));
			ver.setConditioningVariables(conditioningVariables);
		} catch (UnexpectedInferenceException e) {
			e.printStackTrace();
		} catch (IncompatibleEvidenceException e) {
			probability = DiscretePotentialOperations.createZeroProbabilityPotential();
			utility = DiscretePotentialOperations.createZeroUtilityPotential(dan);
			callInference = false;
		} catch (NotEvaluableNetworkException e) {
			throw e;
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (callInference) {
			try {
				if (!isCEAnalysis) {
					VEEvaluation auxVer = (VEEvaluation)ver;
					probability = auxVer.getProbability();
					utility = auxVer.getUtility();
				}
				else {
					VECEAnalysis auxVer = (VECEAnalysis)ver;
					probability = auxVer.getProbability();
					utility = auxVer.getUtility();
				}
				
			} catch (IncompatibleEvidenceException | UnexpectedInferenceException e) {
				e.printStackTrace();
			}
		}
		setProbability(probability);
		setUtility(utility);
	}

	


}
