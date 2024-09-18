/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.inference.heuristic.minimalFillIn;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.event.UndoableEditEvent;

import org.openmarkov.core.exception.ConstraintViolationException;
import org.openmarkov.core.exception.NonProjectablePotentialException;
import org.openmarkov.core.exception.WrongCriterionException;
import org.openmarkov.core.inference.heuristic.EliminationHeuristic;
import org.openmarkov.core.model.network.Node;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.Variable;

/**
 * Chooses the node that adds less links in network when eliminated.
 *
 * @author marias
 */
public class MinimalFillIn extends EliminationHeuristic {

	// Attributes
	/**
	 * Stores a copy of the <code>Node</code> <code>Graph</code>
	 */
	private ProbNet probNetCopy;

	/**
	 * Localize each <code>Node</code> in the <code>Graph</code> given a <code>Variable</code>
	 */
	private HashMap<Variable, Node> variablesNodes;

	// Constructor
	public MinimalFillIn(ProbNet probNet, List<List<Variable>> variablesToEliminate) {
		super(probNet, variablesToEliminate);
		probNetCopy = this.probNet.copy();
		variablesNodes = new HashMap<Variable, Node>();
		List<Node> nodes = probNetCopy.getNodes();
		for (Node node : nodes) {
			// Sets the variable as object
			Variable variable = node.getVariable();
			variablesNodes.put(variable, node);
		}
	}

	// Methods

	/**
	 * @return Variable with minimal fill-in.
	 */
	public Variable getVariableToDelete() {
		Variable variable = null;
		int variableListIndex = variablesToEliminate.size() - 1;

		while (variableListIndex >= 0 && variablesToEliminate.get(variableListIndex).size() == 0) {
			--variableListIndex;
		}
		if (variableListIndex >= 0) {
			List<Variable> candidateVariables = variablesToEliminate.get(variableListIndex);
			List<Node> candidateNodes = new ArrayList<Node>();
			for (Variable candidateVariable : candidateVariables) {
				candidateNodes.add(variablesNodes.get(candidateVariable));
			}

			int minimalFillIn = Integer.MAX_VALUE;
			Node minNode = null;
			int numCandidateNodes = candidateNodes.size();
			for (int i = 0; i < numCandidateNodes && minimalFillIn > 0; i++) {
				int fillIn = getFillIn(candidateNodes.get(i));
				if (fillIn < minimalFillIn) {
					minimalFillIn = fillIn;
					minNode = candidateNodes.get(i);
				}
			}

			if (minNode != null) {
				variable = minNode.getVariable();
			}
		}
		return variable;
	}

	/**
	 * @param node <code>Node</code>
	 * @return Fill-in of node = number of links that need to be added to the
	 * graph due to its elimination.
	 */
	private int getFillIn(Node node) {
		List<Node> neighbors = node.getNeighbors();
		int fillIn = 0;
		int numNeighbors = neighbors.size();
		for (int i = 0; i < numNeighbors - 1; i++) {
			Node nodeI = neighbors.get(i);
			for (int j = i + 1; j < numNeighbors; j++) {
				Node nodeJ = neighbors.get(j);
				if (!nodeI.isNeighbor(nodeJ)) {
					fillIn++;
				}
			}
		}
		return fillIn;
	}

	@Override public void undoableEditWillHappen(UndoableEditEvent event)
			throws ConstraintViolationException, NonProjectablePotentialException,
			WrongCriterionException {
		// Does nothing
	}

	@Override public void undoableEditHappened(UndoableEditEvent event) {
		super.undoableEditHappened(event);
		Variable variable = getEventVariable(event);
		// Create links in graph
		Node nodeToRemove = variablesNodes.get(variable);
		List<Node> neighborsOfNodeToRemove = nodeToRemove.getNeighbors();
		int numNeighbors = neighborsOfNodeToRemove.size();
		for (int i = 0; i < numNeighbors - 1; i++) {
			Node neighborI = neighborsOfNodeToRemove.get(i);
			for (int j = i + 1; j < numNeighbors; j++) {
				Node neighborJ = neighborsOfNodeToRemove.get(j);
				if (!neighborI.isNeighbor(neighborJ)) {
					probNetCopy.addLink(neighborI, neighborJ, false);
				}
			}
		}
		// Update internal data structures
		probNetCopy.removeNode(variablesNodes.get(variable));
		variablesNodes.remove(variable);
	}

	public void undoEditHappened(UndoableEditEvent event) {
	}

}
