/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.action;

import java.util.List;

import org.openmarkov.core.exception.NodeNotFoundException;
import org.openmarkov.core.model.network.Node;
import org.openmarkov.core.model.network.NodeType;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.potential.Potential;

/**
 * {@code CRemoveNodeEdit} is an compound edit that removes a node
 * performing this steps:
 * <ol>
 * <li>Remove links between the node and its children
 * <li> Remove links between the node and its children
 * <li> Removes the node
 * </ol>
 */
@SuppressWarnings("serial") public class CRemoveNodeEdit extends CompoundPNEdit { //implements UsesVariable{

	// Attributes

	protected Node node;

	protected NodeType nodeType;

	protected List<Node> parents;

	protected List<Node> children;

	protected List<Node> siblings;

	protected List<Potential> marginalizedPotentials;

	protected List<Potential> allPotentials;

	// Constructor

	/**
	 * @param probNet {@code ProbNet}
	 * @param node    {@code Node}
	 */
	public CRemoveNodeEdit(ProbNet probNet, Node node) {
		super(probNet);
		this.probNet = probNet;
		this.node = node;
		this.nodeType = node.getNodeType();
	}

	public void generateEdits() {
		// gets neighbors of this node
		parents = probNet.getParents(node);
		children = probNet.getChildren(node);

		for (Node parent : parents) {
			String name = parent.getName();
			try {
				addEdit(new RemoveLinkEdit(node.getProbNet(), probNet.getVariable(name),
						probNet.getVariable(node.getName()), true));
			} catch (NodeNotFoundException e) {
				System.err.println(e.getMessage());
				e.printStackTrace(System.err);
			}
		}
		for (Node child : children) {
			try {
				addEdit(new RemoveLinkEdit(node.getProbNet(), probNet.getVariable(node.getName()),
						probNet.getVariable(child.getName()), true));
			} catch (NodeNotFoundException e) {
				System.err.println(e.getMessage());
				e.printStackTrace(System.err);
			}
		}

		// add edit to remove the variable
		addEdit(new RemoveNodeEdit(probNet, node));

		// add edit to add the new potential
		//edits.add(new AddPotentialEdit(probNet, newPotential));
	}

	public void undo() {
		super.undo();
	}

	/**
	 * @return variable {@code Variable}
	 */
	public Variable getVariable() {
		return node.getVariable();
	}

	/**
	 * @return {@code String}
	 */
	public String toString() {
		return new String("CompoundRemoveNodeEdit: " + node.getName());
	}

}