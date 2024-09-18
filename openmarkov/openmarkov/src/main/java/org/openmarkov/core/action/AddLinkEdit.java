/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.action;

import java.util.ArrayList;
import java.util.List;

import org.openmarkov.core.exception.DoEditException;
import org.openmarkov.core.exception.NodeNotFoundException;
import org.openmarkov.core.model.graph.Link;
import org.openmarkov.core.model.network.Node;
import org.openmarkov.core.model.network.NodeType;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.potential.Potential;
import org.openmarkov.core.model.network.potential.SumPotential;

/**
 * Creates a directed or undirected link between two nodes associated to two
 * variables in a <code>ProbNet</code>
 */
@SuppressWarnings("serial") public class AddLinkEdit extends BaseLinkEdit {

	/**
	 * Resulting link of addition or removal.
	 */
	protected Link<Node> link;
	/**
	 * The new <code>Potential</code> of the second node
	 */
	protected List<Potential> newPotentials = new ArrayList<>();
	/**
	 * parent node
	 */
	protected Node node1;
	/**
	 * child node
	 */
	protected Node node2;
	private boolean updatePotentials;
	/**
	 * The last <code>Potential</code> of the second node before the edition
	 */
	private List<Potential> oldPotentials;

	// Constructor

	/**
	 * @param probNet    <code>ProbNet</code>
	 * @param variable1  <code>Variable</code>
	 * @param variable2  <code>Variable</code>
	 * @param isDirected <code>boolean</code>
	 */
	public AddLinkEdit(ProbNet probNet, Variable variable1, Variable variable2, boolean isDirected,
			boolean updatePotentials) {
		super(probNet, variable1, variable2, isDirected);

		node1 = probNet.getNode(variable1);
		node2 = probNet.getNode(variable2);
		this.updatePotentials = updatePotentials;
		this.link = null;
	}

	public AddLinkEdit(ProbNet probNet, Variable variable1, Variable variable2, boolean isDirected) {
		this(probNet, variable1, variable2, isDirected, true);
	}

	// Methods
	@Override public void doEdit() throws DoEditException {
		probNet.addLink(node1, node2, isDirected);
		this.link = probNet.getLink(node1, node2, isDirected);
		if (updatePotentials) {
			this.oldPotentials = node2.getPotentials();
			// TODO Check if this UTILITY label is outdated
			if (node2.getNodeType() == NodeType.UTILITY && node2.onlyNumericalParents()) {
				// Add a default Sum potential to utility supervalue nodes
				for (Potential oldPotential : oldPotentials) {
					// Update potential
					List<Variable> variables = oldPotential.getVariables();
					if (!variables.contains(node1.getVariable())) {
						variables.add(node1.getVariable());
					}
					Potential newPotential = new SumPotential(variables, oldPotential.getPotentialRole());
					newPotentials.add(newPotential);
				}
			} else {
				for (Potential oldPotential : oldPotentials) {
					// Update potential
					Potential newPotential = oldPotential.addVariable(node1.getVariable());
					newPotentials.add(newPotential);
				}
			}
			node2.setPotentials(newPotentials);
		}
	}

	public void undo() {
		super.undo();

		try {
			node2 = probNet.getNode(variable2.getName());
		} catch (NodeNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (updatePotentials) {
			node2.setPotentials(oldPotentials);
		}
		probNet.removeLink(variable1, variable2, isDirected);
	}

	/**
	 * Method to compare two AddLinkEdits comparing the names of
	 * the source and destination variable alphabetically.
	 *
	 * @param obj AddLinkEdit to be compared
	 * @return result of the comparison
	 */
	public int compareTo(AddLinkEdit obj) {
		int result;

		if ((
				result = variable1.getName().compareTo(obj.getVariable1().
						getName())
		) != 0)
			return result;
		if ((
				result = variable2.getName().compareTo(obj.getVariable2().
						getName())
		) != 0)
			return result;
		else
			return 0;
	}

	@Override public String getOperationName() {
		return "Add link";
	}

	/**
	 * Gets the first <code>Node</code> object in the link.
	 *
	 * @return the first <code>Node</code> object in the link.
	 */
	public Node getNode1() {
		return node1;
	}

	/**
	 * Gets the second <code>Node</code> object in the link.
	 *
	 * @return the second <code>Node</code> object in the link.
	 */
	public Node getNode2() {
		return node2;
	}

	/**
	 * Returns the link.
	 *
	 * @return the link.
	 */
	public Link<Node> getLink() {
		return link;
	}

	@Override public BaseLinkEdit getUndoEdit() {
		return new RemoveLinkEdit(getProbNet(), getVariable1(), getVariable2(), isDirected());
	}
}
