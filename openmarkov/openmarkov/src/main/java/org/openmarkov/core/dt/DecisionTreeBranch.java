/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */
package org.openmarkov.core.dt;

import java.util.LinkedList;
import java.util.List;

import org.openmarkov.core.exception.IncompatibleEvidenceException;
import org.openmarkov.core.exception.InvalidStateException;
import org.openmarkov.core.model.network.EvidenceCase;
import org.openmarkov.core.model.network.Finding;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.State;
import org.openmarkov.core.model.network.Variable;

public class DecisionTreeBranch implements DecisionTreeElement {


	protected double scenarioProbability = Double.NEGATIVE_INFINITY;
	private Variable branchVariable;
	private State branchState;
	private DecisionTreeNode parent;
	private DecisionTreeNode child;
	private ProbNet probNet;
	private EvidenceCase scenarioEvidence = null;
	
	
	public DecisionTreeBranch(ProbNet probNet, Variable branchVariable, State branchState) {
		this.probNet = probNet;
		this.branchState = branchState;
		this.branchVariable = branchVariable;
	}
	public DecisionTreeBranch(ProbNet probNet) {
		this(probNet, null, null);
	}
	

	public List<DecisionTreeElement> getChildren() {
		List<DecisionTreeElement> children = new LinkedList<>();
		children.add(child);
		return children;
	}



	public double getBranchProbability() {
		double parentScenarioProb = parent.getScenarioProbability();
		return (parentScenarioProb != 0) ? getScenarioProbability() / parentScenarioProb : 0;
	}

	public EvidenceCase getBranchStates() {
		if (scenarioEvidence == null) {
			scenarioEvidence = (parent != null) ? new EvidenceCase(parent.getBranchStates()) : new EvidenceCase();
			if (branchVariable != null) {
				try {
					scenarioEvidence.addFinding(new Finding(branchVariable, branchState));
				} catch (InvalidStateException | IncompatibleEvidenceException e) {
					e.printStackTrace();
				}
			}
		}
		return scenarioEvidence;
	}

	/**
	 * Returns the branchVariable.
	 *
	 * @return the branchVariable.
	 */
	public Variable getBranchVariable() {
		return branchVariable;
	}

	/**
	 * Returns the branch state
	 *
	 * @return the branch state
	 */
	public State getBranchState() {
		return branchState;
	}

	public double getScenarioProbability() {
		//TODO Manolo> I'm testing if everything is calculated correctly by inference modules
		/*if (scenarioProbability == Double.NEGATIVE_INFINITY) {
			scenarioProbability = 1;
			if (child.getNodeType() == NodeType.UTILITY) {
				EvidenceCase evidenceCase = getBranchStates();
				for (Finding finding : evidenceCase.getFindings()) {
					Node node = probNet.getNode(finding.getVariable());
					if (node != null && node.getNodeType() == NodeType.CHANCE) {
						Potential potential = node.getPotentials().get(0);
						scenarioProbability *= potential.getProbability(evidenceCase);
					}
				}
			} else {
				scenarioProbability = child.getScenarioProbability();
			}
		}*/
		return scenarioProbability;
	}

	public void setScenarioProbability(double scenarioProbability) {
		this.scenarioProbability = scenarioProbability;
	}

	public DecisionTreeNode getChild() {
		return child;
	}

	/**
	 * Sets the child.
	 *
	 * @param child the child to set.
	 */
	public void setChild(DecisionTreeNode child) {
		this.child = child;
		child.setParent(this);
	}

	@Override public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("DecisionTreeBranch [branchVariable=").append(branchVariable).append(", branchState=")
				.append(branchState).append("]");
		return builder.toString();
	}

	public DecisionTreeNode getParent() {
		return parent;
	}

	@Override public void setParent(DecisionTreeElement parent) {
		this.parent = (DecisionTreeNode) parent;
	}
	
	public Object getUtility() {
		return child.getUtility();
	}
	
	
}
