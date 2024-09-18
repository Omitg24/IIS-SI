/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.action;

import java.util.ArrayList;

import org.openmarkov.core.exception.DoEditException;
import org.openmarkov.core.model.network.Node;
import org.openmarkov.core.model.network.NodeType;
import org.openmarkov.core.model.network.PolicyType;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.potential.Potential;

public class RemovePolicyEdit extends SimplePNEdit {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	private Potential lastPotential;
	private Variable variable;
	private PolicyType lastPolicyType;

	/**
	 * @param node Node
	 */
	public RemovePolicyEdit(Node node) {
		super(node.getProbNet());
		this.variable = node.getVariable();
		if (node.getNodeType() == NodeType.DECISION && node.getPolicyType() != PolicyType.OPTIMAL) {
			lastPotential = node.getPotentials().get(0);
			lastPolicyType = node.getPolicyType();
		}

	}

	@Override public void doEdit() throws DoEditException {
		ArrayList<Potential> potentials = new ArrayList<>();
		if (probNet.getNode(variable).getNodeType() == NodeType.DECISION && lastPolicyType != PolicyType.OPTIMAL) {
			probNet.getNode(variable).setPolicyType(PolicyType.OPTIMAL);
			probNet.getNode(variable).setPotentials(potentials);
		}
	}

	public void undo() {
		super.undo();
		ArrayList<Potential> potentials = new ArrayList<>();
		if (probNet.getNode(variable).getNodeType() == NodeType.DECISION && lastPolicyType != PolicyType.OPTIMAL) {
			potentials.add(lastPotential);
			probNet.getNode(variable).setPotentials(potentials);
			probNet.getNode(variable).setPolicyType(lastPolicyType);
		}
	}
}
