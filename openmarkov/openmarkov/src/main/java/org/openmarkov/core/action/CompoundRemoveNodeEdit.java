/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.action;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openmarkov.core.model.network.Node;
import org.openmarkov.core.model.network.NodeType;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.potential.Potential;
import org.openmarkov.core.model.network.potential.TablePotential;
import org.openmarkov.core.model.network.potential.operation.PotentialOperations;

/**
 * Removes a node performing this steps:<ol>
 * <li>Collect all potentials with this node variable
 * <li>Multiply and eliminates the variable
 * <li>Removes the collected potentials
 * <li>Adds to the {@code probNet} the new potential
 * <li>Adds links between the node siblings
 * <li>Remove links between the node and its children, parents and siblings
 * <li>Removes the node
 * </ol>
 */
@SuppressWarnings("serial") public class CompoundRemoveNodeEdit extends CompoundPNEdit {

	// Attributes
	protected Variable variable;

	protected NodeType nodeType;

	protected List<Node> parents;

	protected List<Node> children;

	protected List<Node> siblings;

	protected List<Potential> marginalizedPotentials;

	protected List<Potential> allPotentials;

	private Logger logger;

	// Constructor

	/**
	 * @param probNet  {@code ProbNet}
	 * @param variable {@code Variable}
	 */
	public CompoundRemoveNodeEdit(ProbNet probNet, Variable variable) {
		super(probNet);
		this.variable = variable;
		this.nodeType = probNet.getNode(variable).getNodeType();
		this.logger = LogManager.getLogger(CompoundPNEdit.class);
	}

	public void generateEdits() {
		Node node = probNet.getNode(variable);

		// gets neighbors of this node
		parents = probNet.getParents(node);
		children = probNet.getChildren(node);
		siblings = probNet.getSiblings(node);

		// collect potentials of this node ...
		List<TablePotential> potentialsVariable = new ArrayList<>();

		for (Potential pot : probNet.getPotentials(variable)) {
			potentialsVariable.add((TablePotential) pot);
		}

		Potential newPotential = null;
		try {
			// ... multiply and eliminate the variable
			newPotential = PotentialOperations.multiplyAndEliminate(potentialsVariable, variable);
		} catch (Exception e) {
			logger.fatal(e);
		}

		List<Variable> variablesNewPotential = newPotential.getVariables();
		if (variablesNewPotential != null && variablesNewPotential.size() > 0) {
			edits.add(new AddPotentialEdit(probNet, newPotential));
		}
		for (Potential potential : potentialsVariable) {
			edits.add(new RemovePotentialEdit(probNet, potential));
		}

		// add a link between the siblings of the removed node
		for (Node node1 : siblings) {
			for (Node node2 : siblings) {
				if ((node1 != node2) && (!probNet.isSibling(node1, node2))) {
					addEdit(new AddLinkEdit(probNet, node1.getVariable(), node2.getVariable(), false));
				}
			}
		}

		// remove links between node and its parents, children and siblings
		for (Node parent : parents) {
			addEdit(new RemoveLinkEdit(probNet, parent.getVariable(), node.getVariable(), true));
		}
		for (Node child : children) {
			addEdit(new RemoveLinkEdit(probNet, node.getVariable(), child.getVariable(), true));
		}
		for (Node sibling : siblings) {
			addEdit(new RemoveLinkEdit(probNet, sibling.getVariable(), node.getVariable(), false));
		}

		// generate edit related to remove the variable
		addEdit(new RemoveNodeEdit(probNet, variable));
	}

	public void undo() {
		super.undo();
	}

	/**
	 * @return variable {@code Variable}
	 */
	public Variable getVariable() {
		return variable;
	}

	/**
	 * @return {@code String}
	 */
	public String toString() {
		return new String("CompoundRemoveNodeEdit: " + variable);
	}

}
