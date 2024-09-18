/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.inference.huginPropagation.action;

import java.util.ArrayList;
import java.util.List;

import org.openmarkov.core.action.SimplePNEdit;
import org.openmarkov.core.action.UsesVariable;
import org.openmarkov.core.exception.DoEditException;
import org.openmarkov.core.model.network.Node;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.Variable;

@SuppressWarnings("serial") public class RemoveMarkovNetNodeEdit extends SimplePNEdit implements UsesVariable {
	private Node nodeToDelete;

	public RemoveMarkovNetNodeEdit(ProbNet probNet, Node nodeToDelete) {
		super(probNet);
		this.nodeToDelete = nodeToDelete;
	}

	@Override public Variable getVariable() {
		return nodeToDelete.getVariable();
	}

	@Override public void doEdit() throws DoEditException {
		List<Node> siblings = new ArrayList<Node>(nodeToDelete.getSiblings());
		Variable variableToDelete = nodeToDelete.getVariable();
		for (Node sibling : siblings) {
			probNet.removeLink(nodeToDelete, sibling, false);
		}
		probNet.removeNode(probNet.getNode(variableToDelete));
		// marry siblings
		probNet.marry(new ArrayList<Node>(siblings));
	}
}
