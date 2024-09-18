package org.openmarkov.inference.decompositionIntoSymmetricDANs.core;

import java.text.DecimalFormat;

import org.openmarkov.core.dt.DecisionTreeBranch;
import org.openmarkov.core.dt.DecisionTreeNode;
import org.openmarkov.core.model.network.CEP;
import org.openmarkov.core.model.network.Node;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.potential.GTablePotential;
import org.openmarkov.core.model.network.potential.TablePotential;

public class CEADecisionTreeNode extends DecisionTreeNode<CEP> {
	
	public CEADecisionTreeNode(Node node) {
		super(node);
		// TODO Auto-generated constructor stub
	}

	public CEADecisionTreeNode(Node node, ProbNet dan) {
		super(node,dan);
	}

	public CEADecisionTreeNode(Variable variable, ProbNet dan) {
		super(variable,dan);
	}

	@Override
	public boolean isBestDecision(DecisionTreeBranch treeBranch) {
		// TODO Auto-generated method stub
		return false;
	}
	

	@Override
	public void setOnlyValueForUtility(TablePotential tablePotential) {
		setUtility(DANOperations.getOnlyValuePotentialCEP((GTablePotential) tablePotential));		
	}

	@Override
	public String formatUtility(DecimalFormat df, boolean addSlashPrefixIfItAddsContent) {
		CEP cep = getUtility();
		String str;
		if (cep.getNumIntervals() == 1) {
			str = "";
			if (addSlashPrefixIfItAddsContent) {
				str += " /";
			}
			str += " Cost=" + df.format(cep.getCost(0));
			str += " / Effectiveness=" + df.format(cep.getEffectiveness(0));
		} else {
			str = " ";
		}
		return str;
	}


}
