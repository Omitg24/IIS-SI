/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.action;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openmarkov.core.model.graph.Link;
import org.openmarkov.core.model.network.Node;
import org.openmarkov.core.model.network.PartitionedInterval;
import org.openmarkov.core.model.network.State;
import org.openmarkov.core.model.network.VariableType;
import org.openmarkov.core.model.network.potential.Potential;
import org.openmarkov.core.model.network.potential.TablePotential;
import org.openmarkov.core.model.network.potential.UniformPotential;

/**
 * {@code NodeReplaceStatesEdit} is a simple edit that allows modify the
 * states of node
 *
 * @author Miguel Palacios
 * @version 1.0 10/05/2011
 */

@SuppressWarnings("serial") public class NodeReplaceStatesEdit extends SimplePNEdit {

	// Default increment between discretized intervals
	private final int increment = 2;

	/**
	 * The current default states of the network
	 */
	private State[] lastStates;

	/**
	 * The new default states of the network
	 */
	private State[] newStates;

	private Node node;

	private List<Potential> lastPotential;

	private List<Potential> childrenLastPotential = new ArrayList<>();

	private PartitionedInterval currentPartitionedInterval;

	private Map<Link<Node>, double[]> linkRestrictionMap;

	/***
	 * Map with the revelation condition list for each link.
	 */
	private Map<Link<Node>, List> revelationConditionMap;

	/**
	 * Creates a {@code NodeReplaceStatesEdit} with the node and new states
	 * specified for replace.
	 *
	 * @param node      the node that will be modified.
	 * @param newStates the new states.
	 */
	public NodeReplaceStatesEdit(Node node, State[] newStates) {
		super(node.getProbNet());
		this.node = node;

		this.lastStates = node.getVariable().getStates();
		this.lastPotential = node.getPotentials();

		this.currentPartitionedInterval = node.getVariable().getPartitionedInterval();

		this.newStates = newStates;
		this.linkRestrictionMap = new HashMap<>();
		this.revelationConditionMap = new HashMap<>();
	}

	// Methods
	@Override public void doEdit() {
		if (newStates != null) {
			List<Node> nodes;
			node.getVariable().setStates(newStates);
			List<Potential> newPotentials = new ArrayList<>();
			// set uniform potential for the edited node and children if the
			// new number of states is different that the last states
			if (newStates.length != lastStates.length) {

				if (lastPotential.size() != 0) {//decision nodes without imposed policy has no potential
					UniformPotential newPotential = new UniformPotential(lastPotential.get(0).getVariables(),
							lastPotential.get(0).getPotentialRole());
					newPotentials.add(newPotential);
					node.setPotentials(newPotentials);
				}

				UniformPotential childLastPotential;
				nodes = probNet.getChildren(node);

				for (Node child : nodes) {
					if (child.getPotentials().size() != 0) {
						List<Potential> container = new ArrayList<>();
						childrenLastPotential.add(child.getPotentials().get(0));
						childLastPotential = new UniformPotential(child.getPotentials().get(0).getVariables(),
								child.getPotentials().get(0).getPotentialRole());
						// child.setUniformPotential();
						container.add(childLastPotential);
						child.setPotentials(container);
					}
				}
				resetLink(node);
			}

			if (node.getVariable().getVariableType() == VariableType.DISCRETIZED) {

				node.getVariable().setPartitionedInterval(new PartitionedInterval(
						node.getVariable().getDefaultInterval(node.getVariable().getNumStates()),
						node.getVariable().getDefaultBelongs(node.getVariable().getNumStates())));

			}
		}
	}

	@SuppressWarnings("unchecked") public void undo() {
		super.undo();
		if (lastStates != null) {
			node.getVariable().setStates(lastStates);
			if (lastStates.length != newStates.length) {
				node.setPotentials(lastPotential);
				List<Node> nodes = probNet.getChildren(node);
				for (Node child : nodes) {
					child.setPotential(childrenLastPotential.get(0));
				}
			}
		}
		for (Link<Node> link : linkRestrictionMap.keySet()) {
			link.initializesRestrictionsPotential();
			TablePotential restrictionPotential = (TablePotential) link.getRestrictionsPotential();
			restrictionPotential.setValues(linkRestrictionMap.get(link));
		}
		for (Link<Node> link : revelationConditionMap.keySet()) {
			VariableType varType = link.getNode1().getVariable().getVariableType();
			if ((varType == VariableType.NUMERIC)) {
				link.setRevealingIntervals(revelationConditionMap.get(link));
			} else {
				link.setRevealingStates(revelationConditionMap.get(link));
			}

		}
	}

	private PartitionedInterval getNewPartitionedInterval() {
		double limits[] = currentPartitionedInterval.getLimits();
		double newLimits[] = new double[limits.length + 1];
		boolean belongsToLeftSide[] = currentPartitionedInterval.getBelongsToLeftSide();
		boolean newBelongsToLeftSide[] = new boolean[limits.length + 1];
		for (int i = 0; i < limits.length; i++) {
			newLimits[i] = limits[i];
			newBelongsToLeftSide[i] = belongsToLeftSide[i];
		}
		newLimits[limits.length] = currentPartitionedInterval.getMax() + increment;
		newBelongsToLeftSide[limits.length] = false;
		return new PartitionedInterval(newLimits, newBelongsToLeftSide);
	}

	/****
	 * This method resets the link restrictions and revelation conditions of the
	 * links of the node
	 *
	 * @param node Node
	 */
	private void resetLink(Node node) {

		for (Link<Node> link : probNet.getLinks(node)) {
			if (link.hasRestrictions()) {
				double[] lastPotential = (
						(TablePotential) link.getRestrictionsPotential()
				).values.clone();
				linkRestrictionMap.put(link, lastPotential);
				link.setRestrictionsPotential(null);
			}
		}
		List<Node> children = probNet.getChildren(node);
		for (Node child : children) {
			Link<Node> link = probNet.getLink(node, child, true);
			if (link.hasRevealingConditions()) {
				VariableType varType = link.getNode1().getVariable().getVariableType();
				if (varType == VariableType.NUMERIC) {
					this.revelationConditionMap.put(link, link.getRevealingIntervals());
					link.setRevealingIntervals(new ArrayList<PartitionedInterval>());
				} else {
					this.revelationConditionMap.put(link, link.getRevealingStates());
					link.setRevealingStates(new ArrayList<State>());
				}
			}
		}
	}

}
