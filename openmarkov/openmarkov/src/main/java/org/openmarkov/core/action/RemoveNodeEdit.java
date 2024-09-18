/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.action;

import org.openmarkov.core.exception.DoEditException;
import org.openmarkov.core.model.network.Node;
import org.openmarkov.core.model.network.NodeType;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.Variable;

@SuppressWarnings("serial") public class RemoveNodeEdit extends SimplePNEdit implements UsesVariable {

	// Attributes
	protected Variable variable;
	/**
	 * Node associated to variable
	 */
	private Node node;

	// Constructor

	/**
	 * @param probNet {@code ProbNet}
	 * @param node    {@code Node}
	 */
	public RemoveNodeEdit(ProbNet probNet, Node node) {
		super(probNet);
		this.variable = node.getVariable();
		this.node = node;
	}

	public RemoveNodeEdit(ProbNet probNet, Variable variable) {
		super(probNet);
		this.variable = variable;
		this.node = probNet.getNode(variable);
	}

	// Methods
	@Override public void doEdit() throws DoEditException {
		if (node == null) {
			throw new DoEditException("Trying to access a null node");
		}
		probNet.removeNode(node);

	}

	public void undo() {
		super.undo();
		probNet.addNode(node);
	}

	/**
	 * @return nodeType {@code NodeType}
	 */
	public NodeType getNodeType() {
		return node.getNodeType();
	}

	/**
	 * @return variable {@code Variable}
	 */
	public Variable getVariable() {
		return variable;
	}

	public String toString() {
		StringBuilder buffer = new StringBuilder("RemoveNodeEdit: ");
		if (variable == null) {
			buffer.append("null");
		} else {
			buffer.append(variable.getName());
		}
		return buffer.toString();
	}

}
