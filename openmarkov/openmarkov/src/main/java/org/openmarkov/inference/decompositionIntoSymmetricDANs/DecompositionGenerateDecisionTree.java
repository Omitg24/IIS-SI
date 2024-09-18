package org.openmarkov.inference.decompositionIntoSymmetricDANs;

import java.util.List;

import org.openmarkov.core.dt.DecisionTreeNode;
import org.openmarkov.core.exception.IncompatibleEvidenceException;
import org.openmarkov.core.exception.NotEvaluableNetworkException;
import org.openmarkov.core.inference.MulticriteriaOptions;
import org.openmarkov.core.inference.tasks.GenerateDecisionTree;
import org.openmarkov.core.model.network.EvidenceCase;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.type.InfluenceDiagramType;
import org.openmarkov.core.model.network.type.NetworkType;
import org.openmarkov.inference.decompositionIntoSymmetricDANs.ceanalysis.DANDecisionTreeCEA;
import org.openmarkov.inference.decompositionIntoSymmetricDANs.ceanalysis.IDDecisionTreeCEA;
import org.openmarkov.inference.decompositionIntoSymmetricDANs.evaluation.DANDecisionTreeEvaluation;
import org.openmarkov.inference.decompositionIntoSymmetricDANs.evaluation.IDDecisionTreeEvaluation;

public class DecompositionGenerateDecisionTree implements GenerateDecisionTree {
	
	private ProbNet probNet;
	EvidenceCase preresolutionEvidence;
	private int depth;
	private boolean isUnicriterion;

	@Override
	public void setPreResolutionEvidence(EvidenceCase preresolutionEvidence) throws IncompatibleEvidenceException {
		this.preresolutionEvidence = preresolutionEvidence;
		
	}

	@Override
	public void setConditioningVariables(List<Variable> conditioningVariables) {
		// TODO Auto-generated method stub
		
	}
	
	/**
	 * @param probNet a network (usually containing decisions and utility nodes)
	 */
	public DecompositionGenerateDecisionTree(ProbNet probNet)  {
		 this(probNet,5);
	}
	
	
	/**
	 * @param probNet a network (usually containing decisions and utility nodes)
	 */
	public DecompositionGenerateDecisionTree(ProbNet probNet, int depth)  {
		 this.probNet = probNet.copy();
		 this.depth = depth;
		 
		this.isUnicriterion = probNet.getInferenceOptions().getMultiCriteriaOptions().getMulticriteriaType()
					.equals(MulticriteriaOptions.Type.UNICRITERION);
	}

	

	@Override
	public DecisionTreeNode getDecisionTree() throws NotEvaluableNetworkException {
		NetworkType networkType = probNet.getNetworkType();
						
		DecisionTreeComputation computation;
		
		boolean isInfluenceDiagram = networkType instanceof InfluenceDiagramType;
		
		if (isUnicriterion) {
			computation = isInfluenceDiagram?new IDDecisionTreeEvaluation(probNet,depth,true,preresolutionEvidence):
				new DANDecisionTreeEvaluation(probNet,depth,true,preresolutionEvidence);
		}
		else {
			computation = isInfluenceDiagram?new IDDecisionTreeCEA(probNet,depth,true,preresolutionEvidence):
				new DANDecisionTreeCEA(probNet,depth,true,preresolutionEvidence);
		}
		 			
		
		return computation.getDecisionTree();
	}

}
