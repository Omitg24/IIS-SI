/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.inference.heuristic.canoAndMoral;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.event.UndoableEditEvent;

import org.openmarkov.core.inference.heuristic.EliminationHeuristic;
import org.openmarkov.core.model.graph.Link;
import org.openmarkov.core.model.network.Node;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.Variable;

/**
 * Implements the heuristic triangulation algorithm defined by <cite>Andres
 * Cano and Serafin Moral</cite> in <cite>Heuristic Algorithms for the  Triangulation of Graphs</cite>
 *
 * @author Manuel arias
 * @version 1.0
 * @since OpenMarkov 1.0
 */
public class CanoMoralElimination extends EliminationHeuristic {

	// Attributes
	/**
	 * Stores a copy of the <code>Node</code> <code>Graph</code>
	 */
	private ProbNet graph;

	/**
	 * Localize each <code>Node</code> in the <code>Graph</code> given a <code>Variable</code>
	 */
	private List<Set<Node>> listOfSetsOfNodes;

	// Constructor
	public CanoMoralElimination(ProbNet probNet, List<List<Variable>> variablesToEliminate) {
		super(probNet, variablesToEliminate);
		graph = probNet.copy();
		listOfSetsOfNodes = new ArrayList<Set<Node>>();
		for (Collection<Variable> listOfVariables : variablesToEliminate) {
			Set<Node> nodes = new HashSet<Node>();
			listOfSetsOfNodes.add(nodes);
			for (Variable variable : listOfVariables) {
				nodes.add(graph.getNode(variable));
			}
		}
	}

	// Methods

	/**
	 * @return Set of Nodes to explore
	 */
	private Set<Node> getLastNonEmptySetOfNodes() {
		int indexSetsOfNodes = listOfSetsOfNodes.size() - 1;
		while (indexSetsOfNodes >= 0 && listOfSetsOfNodes.get(indexSetsOfNodes).isEmpty()) {
			indexSetsOfNodes--;
		}
		return indexSetsOfNodes >= 0 ? listOfSetsOfNodes.get(indexSetsOfNodes) : null;
	}

	@Override
	/** @return Next <code>Variable</code> to delete. */ public Variable getVariableToDelete() {
		Set<Node> setOfNodes = getLastNonEmptySetOfNodes();
		Node toDeleteNode = null;
		// Heuristic algorithm from Andres Cano and Serafin Moral
		double H6 = Double.MAX_VALUE;
		if (setOfNodes != null) {
			for (Node node : setOfNodes) {
				double createdCliqueSize = createdCliqueSize(node);
				double sumCliqueSizes = sumCliqueSizes(node);
				double auxH6 = sumCliqueSizes != 0 ?
						createdCliqueSize / sumCliqueSizes :
						createdCliqueSize > 0 ? Double.MAX_VALUE : 1.0;
				if (auxH6 <= H6 && setOfNodes.contains(node)) {
					H6 = auxH6;
					toDeleteNode = node;
				}
			}
		}
		return toDeleteNode != null ? toDeleteNode.getVariable() : null;
	}

	/**
	 * Calculates the heuristic metric <i>S(i)</i>: size of the clique created
	 * by deleting the <i>i</i> node
	 *
	 * @param toDeleteNode <code>Node</code>
	 * @return The heuristic metric <i>S(i)</i>. <code>long</code>.
	 */
	private long createdCliqueSize(Node toDeleteNode) {
		Set<Node> neighbors = new HashSet<Node>(toDeleteNode.getNeighbors());
		HashSet<Node> allNodes = new HashSet<Node>(graph.getNodes());
		allNodes.remove(toDeleteNode);
		return getCliqueSize(getMaxClique(neighbors, allNodes));
	}

	/**
	 * @param seed     Set of nodes that must be expanded to find the maximum clique
	 *                 that contains the seed. <code>Set</code> of <code>Node</code>
	 * @param allNodes Set of nodes among which maximum clique will be searched. <code>Set</code> of <code>Node</code>
	 * @return Maximum clique that contains <code>seed</code>. <code>Set</code> of <code>Node</code>
	 */
	private Set<Node> getMaxClique(Set<Node> seed, Set<Node> allNodes) {
		// 1. Get seed neighbors 
		Set<Node> neighbors = new HashSet<Node>();
		for (Node node : seed) {
			neighbors.addAll(node.getNeighbors());
		}

		// 2. Remove nodes outside allNodes and nodes in seed
		Set<Node> toDelete = new HashSet<Node>(seed);
		for (Node node : neighbors) {
			if (!allNodes.contains(node)) {
				toDelete.add(node);
			}
		}
		neighbors.removeAll(toDelete);

		// 3. Get nodes in neighbors whose neighbors contains all the nodes in seed 
		Set<Node> candidates = new HashSet<Node>();
		for (Node node : neighbors) {
			Set<Node> nodeNeighbors = new HashSet<Node>(node.getNeighbors());
			nodeNeighbors.add(node);
			if (nodeNeighbors.containsAll(seed)) {
				candidates.add(node);
			}
		}

		// 4. Expand seed recursively
		Set<Node> maxClique = null;
		long maxCliqueSize = 0;
		for (Node node : candidates) {
			Set<Node> expandedSeed = new HashSet<Node>(seed);
			expandedSeed.add(node);
			Set<Node> maxCliqueCandidate = getMaxClique(expandedSeed, allNodes);
			long maxCliqueCandidateSize = getCliqueSize(maxCliqueCandidate);
			if (maxCliqueCandidateSize > maxCliqueSize) {
				maxCliqueSize = maxCliqueCandidateSize;
				maxClique = maxCliqueCandidate;
			}
		}

		return maxClique;
	}

	/**
	 * @param clique <code>Collection</code> of <code>Node</code>
	 * @return clique size = product of the number of states of each variable. <code>long</code>
	 */
	public long getCliqueSize(Collection<Node> clique) {
		long cliqueSize;
		if (clique == null) {
			cliqueSize = 0;
		} else {
			cliqueSize = 1;
			for (Node node : clique) {
				cliqueSize *= node.getVariable().getNumStates();
			}
		}
		return cliqueSize;
	}

	/**
	 * Calculates the heuristic metric <i>C(i)</i>: sum of clique sizes in the
	 * subgraph of <i>X<sub>i</sub></i> and its adjacent nodes.
	 *
	 * @param centerNode <code>Node</code>
	 */
	private long sumCliqueSizes(Node centerNode) {

		// 1. Initialize. Create a candidate to clique per each neighbor to the received node
		List<Node> neighbors = centerNode.getNeighbors();
		List<Set<Node>> candidateCliques = new ArrayList<Set<Node>>();
		for (Node neighborNode : neighbors) {
			Set<Node> neighborSet = new HashSet<Node>();
			neighborSet.add(centerNode);
			neighborSet.add(neighborNode);
			candidateCliques.add(neighborSet);
		}

		// 2. Get the maximal clique per each candidate
		// Set in which the maximal clique will be searched:
		Set<Node> allNodes = new HashSet<Node>(neighbors);
		allNodes.add(centerNode);

		// Set of maximal cliques
		List<Set<Node>> maximalCliques = new ArrayList<Set<Node>>();

		for (int numCandidate = 0; numCandidate < candidateCliques.size(); numCandidate++) {
			Set<Node> maximalClique = getMaxClique(candidateCliques.get(numCandidate), allNodes);
			// Check if this maximal clique is already included in the maximal cliques collection
			int numMaximalCliques = maximalCliques.size(), i = 0;
			for (; i < numMaximalCliques && !maximalCliques.get(i).containsAll(maximalClique); i++)
				;
			if (i < numMaximalCliques) {
				maximalCliques.add(maximalClique);
			}
		}

		long sumCliqueSizes = 0;
		for (Set<Node> clique : maximalCliques) {
			sumCliqueSizes += getCliqueSize(clique);
		}

		return sumCliqueSizes;
	}

	public String toString() {
		return nodesToEliminate.toString();
	}

	/**
	 * @param event (<code>UndoableEditEvent</code>) that contains the edit
	 *              with the <code>variableToDelete</code>.
	 */
	public void undoableEditHappened(UndoableEditEvent event) {
		super.undoableEditHappened(event);
		Variable variableToDelete = getEventVariable(event);
		if (variableToDelete != null) {
			Node nodeToDelete = graph.getNode(variableToDelete);
			// Create a clique with the siblings of this node
			List<Node> neighbors = nodeToDelete.getNeighbors();
			createClique(neighbors);

			graph.removeNode(nodeToDelete); // Eliminate the node and its links
			for (Set<Node> set : listOfSetsOfNodes) {
				if (set.contains(nodeToDelete)) {
					set.remove(nodeToDelete);
				}
			}
		}
	}

	/**
	 * @param nodes <code>ArrayList</code> of <code>Node</code>
	 */
	private void createClique(List<Node> nodes) {
		int numSiblings = nodes.size();
		for (int i = 0; i < numSiblings - 1; i++) {
			Node node1 = nodes.get(i);
			for (int j = i + 1; j < numSiblings; j++) {
				Node node2 = nodes.get(j);
				if (!node1.isSibling(node2)) {
					new Link(node1, node2, false);
				}
			}
		}
	}

	public void undoEditHappened(UndoableEditEvent event) {
	}

	/**
	 * It does nothing.
	 *
	 * @param event <code>UndoableEditEvent</code>.
	 */
	public void undoableEditWillHappen(UndoableEditEvent event) {
	}

}
