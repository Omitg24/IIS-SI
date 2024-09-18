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
import org.openmarkov.core.model.network.Node;
import org.openmarkov.core.model.network.NodeType;
import org.openmarkov.core.model.network.PolicyType;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.potential.CycleLengthShift;
import org.openmarkov.core.model.network.potential.Potential;
import org.openmarkov.core.model.network.potential.PotentialRole;
import org.openmarkov.core.model.network.potential.TablePotential;
import org.openmarkov.core.model.network.potential.operation.LinkRestrictionPotentialOperations;
import org.openmarkov.core.model.network.potential.plugin.PotentialManager;
import org.openmarkov.core.model.network.potential.plugin.PotentialType;

@SuppressWarnings("serial") public class SetPotentialEdit extends SimplePNEdit {
	// unused - private PotentialType lastPotentialType;
	private Potential lastPotential;
	private String newPotentialType;
	// private ICIModelType newICIModelType;
	private Variable variable;
	private Potential newPotential = null;
	private Node node;

	/**
	 * Creates a new SetPotentialEdit object that sets the a new potential with
	 * the type specified for the node object.
	 *
	 * @param node             The node that contains the potential to modify
	 * @param newPotentialType The potential type of the new potential to be created
	 */
	public SetPotentialEdit(Node node, String newPotentialType) {
		super(node.getProbNet());
		this.node = node;
		this.variable = node.getVariable();
		//if (!(node.getNodeType() == NodeType.DECISION && node
		//	.getPolicyType() == PolicyType.OPTIMAL)) {
		lastPotential = node.getPotentials().get(0);
		//	}

		this.newPotentialType = newPotentialType;

	}

	/**
	 * SetPotentialEdit object that changes the last Potential with the
	 * potential specified for the node object.
	 *
	 * @param node      The node that contains the potential to set.
	 * @param potential The new potential object
	 */
	public SetPotentialEdit(Node node, Potential potential) {
		super(node.getProbNet());
		this.node = node;
		this.variable = node.getVariable();
		if (node.getPotentials().size() != 0) {// if node is a decision node it could not have a potential assigned yet
			lastPotential = node.getPotentials().get(0);
		}

		newPotential = potential;
		this.newPotentialType = newPotential.getClass().getAnnotation(PotentialType.class).name();
	}

	// TODO al asignar un potencial tener en cuenta a los padres y a los
	// predecesores informativos que me los va a dar Manolo invocando a una
	// funcion
	@Override public void doEdit() throws DoEditException {
		List<Variable> variables;
		PotentialRole role;
		variables = lastPotential.getVariables();
		role = lastPotential.getPotentialRole();

		List<Potential> potentials = new ArrayList<>();
		if (newPotential == null) {
			PotentialManager relationTypeManager = new PotentialManager();

			if (newPotentialType.equals(PotentialManager.getPotentialName(CycleLengthShift.class))) {
				newPotential = relationTypeManager
						.getByName(newPotentialType, variables, role, probNet.getCycleLength());
			} else {
				newPotential = relationTypeManager.getByName(newPotentialType, variables, role);
			}
		}

		if (!(node.getNodeType() == NodeType.DECISION && node.getPolicyType() == PolicyType.OPTIMAL)) {
			//	probNet.getNode(variable).setPolicyType(PolicyType.PROBABILISTIC);
			node.setPolicyType(PolicyType.PROBABILISTIC);
		}

		potentials.add(newPotential);
		//probNet.getNode(variable).setPotentials(potentials);
		node.setPotentials(potentials);
		// update potential with link restriction
		if (newPotential instanceof TablePotential && node.getNodeType() != NodeType.DECISION) {
			newPotential = LinkRestrictionPotentialOperations.updatePotentialByLinkRestrictions(node);
			potentials = new ArrayList<>();
			potentials.add(newPotential);
			node.setPotentials(potentials);
			//probNet.getNode(variable).setPotentials(potentials);
		}
	}

	public void undo() {
		super.undo();
		Node node = probNet.getNode(variable);
		List<Potential> potentials = new ArrayList<>();
		if (lastPotential != null) {
			potentials.add(lastPotential);
		} else if (node.getNodeType() == NodeType.DECISION) {
			node.setPolicyType(PolicyType.OPTIMAL);

		}
		node.setPotentials(potentials);
	}

	public Potential getNewPotential() {
		return newPotential;
	}

	public String getNewPotentialType() {
		return newPotentialType;
	}

	public Node getNode() {
		return node;
	}

}
