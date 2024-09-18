/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.model.graph;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openmarkov.core.model.network.Node;
import org.openmarkov.core.model.network.PartitionedInterval;
import org.openmarkov.core.model.network.State;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.VariableType;
import org.openmarkov.core.model.network.potential.Potential;
import org.openmarkov.core.model.network.potential.PotentialRole;
import org.openmarkov.core.model.network.potential.TablePotential;

/**
 * This class implements explicit links.
 *
 * @author manuel
 * @author fjdiez
 * @version 1.0
 * @see Node
 * @see Graph
 * @since OpenMarkov 1.0
 */
public class Link<T> {

	// Attributes
	/**
	 * The first node. If the link is directed, this node is the parent.
	 */
	private T node1;

	/**
	 * The second node. If the link is directed, this node is the child.
	 */
	private T node2;

	/**
	 * If true, the link is directed. Otherwise, it is an undirected link.
	 */
	private boolean directed;

	/****
	 * Potential that contains the value of compatibility for the combinations
	 * of the variables of node1 and node2
	 */
	private TablePotential restrictionsPotential;

	/*****
	 * List of revealing values of type state
	 */
	private List<State> revealingStates;

	/*****
	 * List of revealing values of type interval
	 */
	private List<PartitionedInterval> revealingIntervals;

	// Constructors

	/**
	 * Creates an unlabelled link and sets the cross references in the nodes.
	 * This constructor should be called only from the {@code addLink}
	 * function in the class Graph. Both nodes must belong to the same graph.
	 *
	 * @param node1    {@code Node}.
	 * @param node2    {@code Node}.
	 * @param directed {@code boolean}.
	 *
	 */
	public Link(T node1, T node2, boolean directed) {
		this.node1 = node1;
		this.node2 = node2;
		this.directed = directed;
		revealingStates = new ArrayList<>();
		revealingIntervals = new ArrayList<>();

	}

	// Methods

	/**
	 * @return The parent (if the link is directed) or the first node (if the
	 * link is undirected).
	 */
	public T getNode1() {
		return node1;
	}

	/**
	 * @return The child (if the link is directed) or the second node (if the
	 * link is undirected).
	 */
	public T getNode2() {
		return node2;
	}

	/**
	 * @param node {@code Node}.
	 * @return {@code true} if the link contains {@code node}.
	 */
	public boolean contains(T node) {
		return ((node1 == node) || (node2 == node));
	}

	/**
	 * @return {@code true} if the link is directed, false if it is
	 * undirected
	 */
	public boolean isDirected() {
		return directed;
	}

	/******
	 * @return {@code true} if the link has a linkRestriction
	 *                          associates,false otherwise
	 */
	public boolean hasRestrictions() {
		return restrictionsPotential != null;
	}

	/****
	 * @return {@code true} if a value of the first variable makes all
	 *                          values of the second variable impossible.
	 *
	 */
	public boolean hasTotalRestriction() {
		boolean totalRestriction = false;
		if (hasRestrictions()) {
			int numStates = restrictionsPotential.getVariables().get(0).getNumStates();
			int valuesSize = restrictionsPotential.getValues().length;

			for (int index = 0; index < numStates && !totalRestriction; index++) {
				boolean valueRestrictsVariable = true;
				int i = index;
				while (i < valuesSize && valueRestrictsVariable) {
					if (restrictionsPotential.getValues()[i] == 1) {
						valueRestrictsVariable = false;

					}
					i += numStates;
				}

				if (valueRestrictsVariable) {
					totalRestriction = true;
				}
			}
		}

		return totalRestriction;

	}

	/****
	 * @return {@code true} if a value of the first variable makes all
	 *                          values of the second variable impossible.
	 *
	 */
	public Set<State> getStatesRestrictTotally() {

		Set<State> statesRestrictTotally = new HashSet<>();

		if (hasRestrictions()) {
			Variable parentVariable = restrictionsPotential.getVariables().get(0);
			int numStates = parentVariable.getNumStates();
			int valuesSize = restrictionsPotential.getValues().length;

			for (int index = 0; index < numStates; index++) {
				boolean totalRestriction = true;
				int i = index;
				while (i < valuesSize && totalRestriction) {
					totalRestriction = (restrictionsPotential.getValues()[i] != 1);
					i += numStates;
				}

				if (totalRestriction) {
					statesRestrictTotally.add(parentVariable.getStates()[index]);
				}
			}
		}

		return statesRestrictTotally;

	}

	/**
	 * Initializes a TablePotential for the variable associated to node1 and
	 * node2, whose values are all 1.
	 */
	public void initializesRestrictionsPotential() {
		List<Variable> variables = new ArrayList<>();
		variables.add(((Node) node1).getVariable());
		variables.add(((Node) node2).getVariable());
		restrictionsPotential = new TablePotential(variables, PotentialRole.LINK_RESTRICTION);
		for (int i = 0; i < restrictionsPotential.getValues().length; i++) {
			restrictionsPotential.getValues()[i] = 1;
		}

	}

	/*****
	 * Assigns a null value to the restrictionsPotential if the restrictions
	 * potential does not contain restrictions
	 *
	 */
	public void resetRestrictionsPotential() {
		boolean hasRestriction = false;
		double[] restrictions = this.restrictionsPotential.getValues();

		for (int i = 0; i < restrictions.length && !hasRestriction; i++) {
			if (restrictions[i] == 0) {
				hasRestriction = true;
			}
		}
		if (!hasRestriction) {
			restrictionsPotential = null;
		}
	}

	/*****
	 * Assigns the value of the parameter compatibility to the combination of
	 * the variables state1 and state2.
	 *
	 * @param state1
	 *            state of the variable of node1
	 * @param state2
	 *            state of the variable of node2
	 * @param compatibility
	 *            value of compatibility
	 */
	public void setCompatibilityValue(State state1, State state2, int compatibility) {
		if (this.restrictionsPotential == null) {
			this.initializesRestrictionsPotential();
		}
		int[] indexes = new int[2];
		indexes[0] = restrictionsPotential.getVariable(0).getStateIndex(state1);
		indexes[1] = restrictionsPotential.getVariable(1).getStateIndex(state2);
		List<Variable> variables = restrictionsPotential.getVariables();
		restrictionsPotential.setValue(variables, indexes, compatibility);
	}

	/******
	 * Returns the compatibility value of the combination of state1 and state2.
	 *
	 * @param state1
	 *            state of the variable of node1.
	 * @param state2
	 *            state of the variable of node2.
	 * @return the value 1 for compatibility and 0 for incompatibility.
	 */

	public int areCompatible(State state1, State state2) {
		if (this.restrictionsPotential == null) {
			return 1;
		}
		int[] indexes = new int[2];
		indexes[0] = restrictionsPotential.getVariable(0).getStateIndex(state1);
		indexes[1] = restrictionsPotential.getVariable(1).getStateIndex(state2);
		List<Variable> variables = restrictionsPotential.getVariables();

		return (int) restrictionsPotential.getValue(variables, indexes);

	}

	/****
	 *
	 * @return the potential of the the link restriction.
	 */
	public Potential getRestrictionsPotential() {
		return restrictionsPotential;
	}

	/****
	 * Assigns the potential to the restrictionPotential of the link
	 *
	 * @param potential Potential
	 */

	public void setRestrictionsPotential(Potential potential) {
		this.restrictionsPotential = (TablePotential) potential;
	}

	/**
	 * @return String
	 */
	public String toString() {
		StringBuilder buffer = new StringBuilder(node1.toString());
		if (!directed) {
			buffer.append(" --- ");
		} else {
			buffer.append(" --> ");
		}
		buffer.append(node2.toString());
		return buffer.toString();
	}

	/*****
	 * This method indicates whether there are revealing conditions for the
	 * link.
	 *
	 * @return {@code true} if there exist revealing conditions.
	 */
	public boolean hasRevealingConditions() {

		VariableType varType = ((Node) node1).getVariable().getVariableType();

		if (varType.equals(VariableType.NUMERIC)) {
			return !revealingIntervals.isEmpty();
		} else {
			return !revealingStates.isEmpty();
		}
	}

	/**
	 * @return the revealingStates
	 */
	public List<State> getRevealingStates() {
		return revealingStates;
	}

	/**
	 * @param revealingStates the revealingStates to set
	 */
	public void setRevealingStates(List<State> revealingStates) {
		this.revealingStates = revealingStates;
	}

	/**
	 * @return the revealingIntervals
	 */
	public List<PartitionedInterval> getRevealingIntervals() {
		return revealingIntervals;
	}

	/**
	 * @param revealingIntervals the revealingIntervals to set
	 */
	public void setRevealingIntervals(List<PartitionedInterval> revealingIntervals) {
		this.revealingIntervals = revealingIntervals;
	}

	/*****
	 * Adds the state to the revealing condition list.
	 *
	 * @param state State
	 */
	public void addRevealingState(State state) {

		revealingStates.add(state);
	}

	/*****
	 * Removes the revealing state from the revealing condition list.
	 *
	 * @param state State
	 */
	public void removeRevealingState(State state) {
		revealingStates.remove(state);

	}

	/*****
	 * Adds the interval to the revealing condition list.
	 *
	 * @param interval Interval
	 */
	public void addRevealingInterval(PartitionedInterval interval) {
		this.revealingIntervals.add(interval);
	}

	/********
	 * Removes the interval from the revealing condition list.
	 *
	 * @param interval Interval
	 */
	public void removeRevealingInterval(PartitionedInterval interval) {
		this.revealingIntervals.remove(interval);
	}

}
