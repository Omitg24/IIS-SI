/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.inference.heuristic.minimalCliqueSize;

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
import org.openmarkov.core.model.network.potential.Potential;

/**
 * Hello world!
 */
public class minimalCliqueSize extends EliminationHeuristic {

	// Attributes
	/**
	 * Stores a copy of the <code>Node</code> <code>Graph</code>
	 */
	private ProbNet graph;

	/**
	 * Localize each <code>Node</code> in the <code>Graph</code> given a <code>Variable</code>
	 */
	private HashMap<Variable, Node> variablesNodes;

	// Constructor
	public minimalCliqueSize(ProbNet probNet, List<List<Variable>> variablesToEliminate) {
		super(probNet, variablesToEliminate);
		graph = probNet.copy();
		variablesNodes = new HashMap<>();
		List<Node> nodes = graph.getNodes();
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
			List<Node> candidateNodes = new ArrayList<>();
			Node candidateNode;

			for (Variable candidateVariable : candidateVariables) {
				candidateNodes.add(variablesNodes.get(candidateVariable));
			}

			int minimalCliqueSize = Integer.MAX_VALUE;
			int minimalCliquePotentialSize = 0;

			Node minNode = null;
			int numCandidateNodes = candidateNodes.size();
			for (int i = 0; i < numCandidateNodes && minimalCliqueSize > 0; i++) {
				candidateNode = candidateNodes.get(i);
				int cliqueSize = getCliqueSize(candidateNode);
				// If the cliqueSize is smaller than the minimal obtained so far,
				// or if they are equal, but the new one will create a smaller clique size regarding the potentials
				if ((cliqueSize < minimalCliqueSize) || (
						cliqueSize == minimalCliqueSize
								&& getCliquePotentialSize(candidateNode) < minimalCliquePotentialSize
				)) {
					minimalCliqueSize = cliqueSize;
					minNode = candidateNode;
					minimalCliquePotentialSize = getCliquePotentialSize(minNode);
				}
			}

			if (minNode != null) {
				variable = minNode.getVariable();
			}
		}

/*        // As the authors are using all the LIMID version of the ID to select the following variable
        variablesToEliminate.get(0).remove(variable);*/

		return variable;
	}

	/**
	 * @param node <code>Node</code>
	 * @return Size of the clique where the node would be included
	 * Variant one: choose a node with the least number of neighbours to be eliminated next
	 * (the original proposal , U. Kjærulff.
	 * Aspects of Efficiency Improvements in Bayesian Networks. PhD thesis, Aalborg University, 1993.)
	 * <p>
	 * This is the option implemented, just adding one to the number of neighbours of the node
	 * <p>
	 * <p>
	 * Variant two: multiplying the states of all the nodes that will be part of the clique
	 * (http://projekter.aau.dk/projekter/files/61072889/1212572184.pdf)
	 */
	private int getCliqueSize(Node node) {
		// The nodes that will belong to the clique are the neighbours of the clique plus the node itself
		return node.getNeighbors().size() + 1;
	}

	private int getCliquePotentialSize(Node node) {
		// The nodes that will belong to the clique are the neighbours of the clique plus the node itself
		List<Node> cliqueNodes = new ArrayList<>(node.getNeighbors());
		cliqueNodes.add(node);
		int potentialSize = 0;
		for (Node cliqueNode : cliqueNodes) {
			for (Potential potential : cliqueNode.getPotentials()) {
				for (Variable variable : potential.getVariables()) {
					potentialSize += variable.getNumStates();
				}
			}
		}
		return potentialSize;
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
					graph.addLink(neighborI, neighborJ, false);
				}
			}
		}

		// Update internal data structures
		graph.removeNode(variablesNodes.get(variable));
		variablesNodes.remove(variable);
	}

	public void undoEditHappened(UndoableEditEvent event) {
	}

}
