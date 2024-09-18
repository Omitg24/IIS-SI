/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */
package org.openmarkov.core.dt;


import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import org.openmarkov.core.model.network.EvidenceCase;
import org.openmarkov.core.model.network.Node;
import org.openmarkov.core.model.network.NodeType;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.potential.Potential;
import org.openmarkov.core.model.network.potential.TablePotential;

public abstract class DecisionTreeNode<T> implements DecisionTreeElement {

	protected double scenarioProbability = Double.NEGATIVE_INFINITY;
	private Variable variable = null;
	protected NodeType nodeType = null;
	protected List<DecisionTreeElement> children = null;
	private DecisionTreeElement parent = null;
	private ProbNet network;
	/**
	 * This is attribute represents what in the past was the utility for uni-criteria decision trees, but now it is generalized
	 * to use the same decision tree structure in the case of cost-effectiveness analysis decision trees.
	 */
	protected T utility;

	
	public void setUtility(T utility) {
		this.utility = utility;
	}


	
	public T getUtility() {
		return utility;
	}


	public DecisionTreeNode(Node node) {
		this.variable = node.getVariable();
		this.nodeType = node.getNodeType();
		List<Potential> potentials = node.getPotentials();
		children = new ArrayList<>();
	}
	
	
	public DecisionTreeNode(Node node, ProbNet network) {
		this(node);
		this.network = network;
	}

	public DecisionTreeNode(Variable variable, ProbNet probNet) {
		this(probNet.getNode(variable), probNet);
	}

	/**
	 * Returns the variable.
	 *
	 * @return the Variable.
	 */
	public Variable getVariable() {
		return variable;
	}

	public NodeType getNodeType() {
		return nodeType;
	}

	/**
	 * Returns the children.
	 *
	 * @return the children.
	 */
	public List<DecisionTreeElement> getChildren() {
		return children;
	}



	public EvidenceCase getBranchStates() {
		return (parent != null) ? parent.getBranchStates() : new EvidenceCase();
	}

	
	public double getScenarioProbability() {
		return scenarioProbability;
	}

	public void setScenarioProbability(double scenarioProbability) {
		this.scenarioProbability = scenarioProbability;
	}

	public void addChild(DecisionTreeElement child) {
		child.setParent(this);
		children.add(child);
	}

	@Override public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("DecisionTreeNode [variable=");
		builder.append(variable.getName());
		builder.append(", children=").append(children);
		builder.append("]");
		return builder.toString();
	}

	@Override public void setParent(DecisionTreeElement parent) {
		this.parent = parent;
	}
	
	public ProbNet getNetwork() {
		return network;
	}
	
	public void copy(DecisionTreeNode<T> node) {
		utility = node.utility;
		scenarioProbability = node.scenarioProbability;
		variable = node.variable;
		nodeType = node.nodeType;
		children = node.children;
		parent = node.parent;
		network = node.network;
	}


	public abstract boolean isBestDecision(DecisionTreeBranch treeBranch);
	
	public abstract void setOnlyValueForUtility(TablePotential tablePotential);
	
	public abstract String formatUtility(DecimalFormat df, boolean addSlashPrefixIfItAddsContent);
	

}
