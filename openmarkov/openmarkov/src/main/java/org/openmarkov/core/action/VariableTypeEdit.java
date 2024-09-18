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
import org.openmarkov.core.model.graph.Link;
import org.openmarkov.core.model.network.Node;
import org.openmarkov.core.model.network.NodeType;
import org.openmarkov.core.model.network.PartitionedInterval;
import org.openmarkov.core.model.network.State;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.VariableType;
import org.openmarkov.core.model.network.potential.Potential;
import org.openmarkov.core.model.network.potential.PotentialRole;
import org.openmarkov.core.model.network.potential.SumPotential;
import org.openmarkov.core.model.network.potential.UniformPotential;
import org.openmarkov.core.model.network.potential.operation.Util;

@SuppressWarnings("serial") public class VariableTypeEdit extends SimplePNEdit {
	// private ProbNet probNet;
	private Node node;
	private VariableType newType;
	private VariableType currentType;
	private State[] currentStates;

	public VariableTypeEdit(Node node, VariableType newType) {
		super(node.getProbNet());
		this.node = node;
		this.newType = newType;
		this.currentType = node.getVariable().getVariableType();

	}

	@Override public void doEdit() throws DoEditException {
		// We save the current states
		currentStates = node.getVariable().getStates();

		node.getVariable().setVariableType(newType);

		// We restore the current states
		if (currentStates.length == 1) {
			node.getVariable().setStates(node.getProbNet().getDefaultStates());
		} else {
			node.getVariable().setStates(currentStates);
		}

		if (currentType != newType) {

			switch (currentType) {
			case FINITE_STATES:
				if (newType.equals(VariableType.DISCRETIZED)) {
					// DO NOTHING
					break;
				} else if (newType.equals(VariableType.NUMERIC)) {
					setPotentialsNodeAndChildren();
				}
				break;
			case DISCRETIZED:
				if (newType.equals(VariableType.FINITE_STATES)) {
					// DO NOTHING
					break;
				} else if (newType.equals(VariableType.NUMERIC)) {
					setPotentialsNodeAndChildren();
				}
				break;

			case NUMERIC:

				if (newType.equals(VariableType.FINITE_STATES)) {
					setPotentialsNodeAndChildren();
				} else if (newType.equals(VariableType.DISCRETIZED)) {
					setPotentialsNodeAndChildren();
					// If we only have one interval we need to set up at least
					// the default intervals for the states
					if (node.getVariable().getPartitionedInterval().getNumSubintervals() == 1) {
						PartitionedInterval interval = new PartitionedInterval(
								node.getVariable().getDefaultInterval(node.getVariable().getNumStates()),
								node.getVariable().getDefaultBelongs(node.getVariable().getNumStates()));
						node.getVariable().setPartitionedInterval(interval);
					}

				}

				List<Variable> variables = new ArrayList<>();
				if (node.getNodeType() != NodeType.UTILITY) {
					variables.add(node.getVariable());
				}
				for (Node parent : probNet.getParents(node)) {
					variables.add(parent.getVariable());
				}
				UniformPotential uniformPotential = new UniformPotential(variables,
						node.getPotentials().get(0).getPotentialRole());
				List<Potential> potentials = new ArrayList<>(1);
				potentials.add(uniformPotential);
				node.setPotentials(potentials);
				node.setUniformPotential();

				break;
			default:
				break;
			}
		}

		resetLink(node);

	}

	@Override public void undo() {
		node.getVariable().setVariableType(currentType);
		node.getVariable().setStates(currentStates);
	}

	public VariableType getNewVariableType() {
		return newType;
	}

	public Node getNode() {

		return this.node;
	}

	/****
	 * This method resets the link restriction of the links of the node
	 *
	 * @param node Node
	 */
	private void resetLink(Node node) {

		List<Node> children = probNet.getChildren(node);
		for (Node child : children) {
			Link<Node> link = probNet.getLink(node, child, true);
			if (link.hasRevealingConditions()) {
				link.setRevealingIntervals(new ArrayList<PartitionedInterval>());
				link.setRevealingStates(new ArrayList<State>());
			}
		}

		for (Link<Node> link : probNet.getLinks(node)) {
			if (link.hasRestrictions()) {
				link.setRestrictionsPotential(null);
			}

		}
	}

	public void setUniformPotential2Node(Node node) {

		List<Potential> newListPotentials = new ArrayList<>();
		List<Variable> variables = new ArrayList<>();
		Variable thisVariable;
		List<Potential> potentials = node.getPotentials();
		PotentialRole role = potentials.get(0).getPotentialRole();
		// first, this variable. The potentials is not null
		thisVariable = potentials.get(0).getVariable(0);
		variables.add(thisVariable);

		int numOfCellsInTable = thisVariable.getNumStates();
		double initialValue = Util.round(1 / ((double) numOfCellsInTable), "0.01");
		// add now all the parents

		for (Node parent : node.getParents()) {
			// TODO Revisar, ¿Solo se agrega/elimina un padre a la vez?
			// mpalacios
			// the set of variables could be changed, so , have to be updated.
			variables.add(parent.getVariable());
			numOfCellsInTable *= parent.getVariable().getNumStates();
		}
		// sets a new table with new columns and with all the same values
		double[] table = new double[numOfCellsInTable];
		for (int i = 0; i < numOfCellsInTable; i++) {
			table[i] = initialValue;
		}
		// and finally, create the potential and the list of potentials

		// TODO Comprobar que efectivamente es un CONDITIONAL_PROBABILITY
		UniformPotential uniformPotential = new UniformPotential(variables, role);

		newListPotentials.add(uniformPotential);

		node.setPotentials(newListPotentials);

	}

	private void setPotentialsNodeAndChildren() {
		// from numeric to finite states or discretized or vice versa
		// if child is utility to potential to be set depends on the
		// type of the other parents
		// it is not always uniform
		setUniformPotential2Node(node);
		for (Node child : probNet.getChildren(node)) {
			if (child.getNodeType() == NodeType.UTILITY) {
				List<Potential> newPotentials = new ArrayList<>();
				if (child.onlyNumericalParents()) {// utility and
					// numerical parents
					// sum
					for (Potential oldPotential : child.getPotentials()) {
						// Update potential
						Potential newPotential = new SumPotential(oldPotential.getVariables(),
								oldPotential.getPotentialRole());
						newPotentials.add(newPotential);
					}
				} else if (!child.onlyNumericalParents()) {// mixture of
					// finite
					// states
					// and
					// numerical
					// Uniform
					for (Potential oldPotential : child.getPotentials()) {
						// Update potential
						Potential newPotential = new UniformPotential(oldPotential.getVariables(),
								oldPotential.getPotentialRole());
						newPotentials.add(newPotential);
					}
				}
				child.setPotentials(newPotentials);
			} else {
				// if child is not utility always change potential to Uniform
				// 25/11/2014
				// If there are any potentials in the child
				// Example. In the "ID-decide-test" network, if you change the Domain of Result of test variable,
				// no potential should be set to Therapy
				if (child.getPotentials() != null) {
					if (child.getPotentials().size() > 0) {
						setUniformPotential2Node(child);
					}
				}
			}
		}
	}

}
