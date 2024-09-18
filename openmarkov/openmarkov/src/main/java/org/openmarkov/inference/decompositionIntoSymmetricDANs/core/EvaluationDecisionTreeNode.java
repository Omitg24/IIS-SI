package org.openmarkov.inference.decompositionIntoSymmetricDANs.core;

import java.text.DecimalFormat;

import org.openmarkov.core.dt.DecisionTreeBranch;
import org.openmarkov.core.dt.DecisionTreeElement;
import org.openmarkov.core.dt.DecisionTreeNode;
import org.openmarkov.core.model.network.Node;
import org.openmarkov.core.model.network.NodeType;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.potential.TablePotential;

public class EvaluationDecisionTreeNode extends DecisionTreeNode<Double> {
	
	//protected double utility = Double.NEGATIVE_INFINITY;
	
	public EvaluationDecisionTreeNode(Node node) {
		super(node);
		// TODO Auto-generated constructor stub
	}

	
	public EvaluationDecisionTreeNode(Node node, ProbNet dan) {
		super(node,dan);
	}




	public EvaluationDecisionTreeNode(Variable variable, ProbNet dan) {
		super(variable,dan);
	}


	@Override
	public boolean isBestDecision(DecisionTreeBranch branch) {
		boolean isBestDecision = false;
		if (nodeType == NodeType.DECISION) {
			isBestDecision = true;
			double thisUtility = (double) branch.getUtility();
			for (DecisionTreeElement otherBranch : children) {
				isBestDecision &= thisUtility >= (double)((DecisionTreeBranch) otherBranch).getUtility();
			}
		}
		return isBestDecision;
	}


	@Override
	public void setOnlyValueForUtility(TablePotential tablePotential) {
		this.setUtility(DANOperations.getOnlyValuePotential(tablePotential));
	}


	@Override
	public String formatUtility(DecimalFormat df, boolean addSlashPrefixIfItAddsContent) {
		double utility = getUtility();
		if (Double.isNaN(utility)) {
			utility = 0.0;
		}		
		String auxStr = "";
		if (addSlashPrefixIfItAddsContent) {
			auxStr += " / ";
		}		
		auxStr += " U=" + df.format(utility);
		return auxStr;
	}


}
