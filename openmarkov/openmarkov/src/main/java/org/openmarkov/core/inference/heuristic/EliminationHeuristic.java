/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.inference.heuristic;

import java.util.ArrayList;
import java.util.List;

import javax.swing.event.UndoableEditEvent;
import javax.swing.undo.UndoableEdit;

import org.openmarkov.core.action.PNUndoableEditListener;
import org.openmarkov.core.action.UsesVariable;
import org.openmarkov.core.model.network.Node;
import org.openmarkov.core.model.network.NodeType;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.Variable;

/**
 * Here we define the skeleton (an abstract class) of a heuristic algorithm
 * that eliminates variables from a {@code MarkovNet}
 *
 * @author manuel
 * @author fjdiez
 * @version 1.0
 * @since OpenMarkov 1.0
 */
public abstract class EliminationHeuristic implements PNUndoableEditListener {

	// Attributes
	/**
	 * A pointer to the received {@code ProbNet}.
	 */
	protected ProbNet probNet;

	/**
	 * A set of nodes that points to variables that are nor query variables nor
	 * observed variables.
	 */
	protected List<List<Variable>> variablesToEliminate;

	/**
	 * A set of nodes that points to variables that are nor query variables nor
	 * observed variables.
	 */
	protected List<List<Node>> nodesToEliminate;

	/**
	 * {@code Variable} that the heuristic propose to eliminate.
	 */
	protected Variable variableProposed;

	// Constructor

	/**
	 * Variables will be eliminated from the last array to the first.
	 *
	 * @param probNet              {@code probNet} it's a network that can contain decisions. {@code ProbNet}
	 * @param variablesToEliminate {@code ArrayList} of {@code ArrayList} of {@code Variable}
	 */
	public EliminationHeuristic(ProbNet probNet, List<List<Variable>> variablesToEliminate) {
		// TODO Revisar todas las heuristicas que suponian que trabajaban con una copia
		this.probNet = probNet;

		// this.variablesToEliminate = variablesToEliminate;
		// Make a deep copy of variablesToEliminate
		this.variablesToEliminate = new ArrayList<>(variablesToEliminate.size());
		for (List<Variable> list : variablesToEliminate) {
			List<Variable> listOfVariables = new ArrayList<>(list.size());
			for (Variable variable : list) {
				if (probNet.containsVariable(variable)) {
					listOfVariables.add(variable);
				}
			}
			// Solving issue #227
			// https://bitbucket.org/cisiad/org.openmarkov.issues/issue/227/inconsistency-between-the-lists
			// Making consistent the way both lists are created
			if (listOfVariables.size() > 0) {
				this.variablesToEliminate.add(listOfVariables);
			}
		}

		this.nodesToEliminate = new ArrayList<>(variablesToEliminate.size());
		for (List<Variable> variables : this.variablesToEliminate) {
			List<Node> nodes = new ArrayList<>(variables.size());
			for (Variable variable : variables) {
				nodes.add(probNet.getNode(variable));
			}
			if (nodes.size() > 0) {
				this.nodesToEliminate.add(nodes);
			}
		}
		variableProposed = null;
	}

	// Methods

	/**
	 * @return The {@code Variable} the heuristic suggests to
	 * eliminate.
	 */
	public abstract Variable getVariableToDelete();

	public void undoableEditHappened(UndoableEditEvent event) {
		Variable removedVariable = getEventVariable(event);

		if (removedVariable != null) {
			int listOfListsIndex = variablesToEliminate.size() - 1;
			if (listOfListsIndex >= 0) {
				int listIndex = variablesToEliminate.get(listOfListsIndex).indexOf(removedVariable);
				if (listIndex >= 0) {
					variablesToEliminate.get(listOfListsIndex).remove(listIndex);
				}
				if (variablesToEliminate.get(listOfListsIndex).isEmpty()) {
					variablesToEliminate.remove(listOfListsIndex);
				}
				if (listIndex >= 0) {
					nodesToEliminate.get(listOfListsIndex).remove(listIndex);
				}
				// Solving issue #227
				// https://bitbucket.org/cisiad/org.openmarkov.issues/issue/227/inconsistency-between-the-lists
				// Making consistent the way the undoableEditHappened is executed in both lists
				if (nodesToEliminate.get(listOfListsIndex).isEmpty()) {
					nodesToEliminate.remove(listOfListsIndex);
				}
			}
		}
	}

	/**
	 * @param event {@code UndoableEditEvent}
	 * @return node ({@code Node}) in the heuristic
	 * {@code ProbNet} that will be removed
	 */
	protected Variable getEventVariable(UndoableEditEvent event) {
		Variable variable = null;
		UndoableEdit pNEdit = event.getEdit();

		if (pNEdit instanceof UsesVariable) {
			variable = ((UsesVariable) pNEdit).getVariable();
		}
		return variable;
	}

	/**
	 * @return The class name + list of list of variables to eliminate.
	 */
	public String toString() {
		String string = new String(this.getClass().getSimpleName() + " : ");
		int numLists = variablesToEliminate.size();
		for (int i = numLists - 1; i >= 0; i--) {
			List<Variable> variables = variablesToEliminate.get(i);
			boolean decision = false;
			if (variables.size() == 1) {
				decision = probNet.getNode(variables.get(0)).getNodeType() == NodeType.DECISION;
			}
			if (decision) {
				string += "{";
			} else {
				string += "[";
			}
			for (int j = 0; j < variables.size(); j++) {
				string += variables.get(j).getName();
				if (j < variables.size() - 1) {
					string += ",";
				}
			}
			if (decision) {
				string += "}";
			} else {
				string += "]";
			}
		}
		return string;
	}

	public int getNumVariablesToEliminate() {
		return variablesToEliminate.size();
	}

}
