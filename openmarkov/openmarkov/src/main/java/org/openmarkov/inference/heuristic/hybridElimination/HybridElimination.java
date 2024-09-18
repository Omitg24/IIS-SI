/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.inference.heuristic.hybridElimination;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.event.UndoableEditEvent;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openmarkov.core.exception.IncompatibleEvidenceException;
import org.openmarkov.core.exception.NodeNotFoundException;
import org.openmarkov.core.exception.NonProjectablePotentialException;
import org.openmarkov.core.exception.WrongCriterionException;
import org.openmarkov.core.inference.heuristic.EliminationHeuristic;
import org.openmarkov.core.model.graph.Link;
import org.openmarkov.core.model.network.EvidenceCase;
import org.openmarkov.core.model.network.Node;
import org.openmarkov.core.model.network.NodeType;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.potential.TablePotential;

/**
 * Implements the heuristic triangulation algorithm for Simple Markov Models
 *
 * @author mkpalacio
 * @version 1.0
 * @since OpenMarkov 1.0
 */
public class HybridElimination extends EliminationHeuristic {

	// Attributes
	/**
	 * The <code>Graph</code> triangulated.
	 */
	private ProbNet triangulatedNet;

	private Logger logger;

	// Constructor

	/**
	 * @param probNet              <code>ProbNet</code>.
	 * @param variablesToEliminate <code>ArrayList</code> of
	 *                             <code>Variable</code>
	 */
	public HybridElimination(ProbNet probNet, List<List<Variable>> variablesToEliminate) {
		super(probNet, variablesToEliminate);
		// Creates an undirected graph to be used for triangulation
		triangulatedNet = probNet.copy();
		//triangulatedNet.removeConstraint(NetworkTypeConstraint.class);
		//triangulatedNet.removeConstraint(OnlyDirectedLinks.class);
		removeSuperValueNodes();
		/*nodesToEliminate = new ArrayList<Node>();
		for (Variable variable : variablesToEliminate) {
			nodesToEliminate.add(triangulatedNet.getNode(variable));
		}*/
		this.logger = LogManager.getLogger(HybridElimination.class);
	}

	/**
	 * @param collection is a generic <code>Collection</code>
	 *                   of <code>Node</code>s.
	 * @return A <code>long</code> integer. Clique size = the product of the
	 * cardinalities of the variables stored in the nodes.
	 */
	public static long cliqueSize(Collection<Node> collection) {
		int result = 1;
		for (Node node : collection) {
			result *= node.getVariable().getNumStates();
		}
		return result;
	}

	/**
	 * @param network network
	 * @param evidence evidence
	 * @return markovNetworkInference
	 * @throws IncompatibleEvidenceException IncompatibleEvidenceException
	 */
	public static ProbNet projectTablesAndBuildMarkovDecisionNetwork(ProbNet network, EvidenceCase evidence)
			throws IncompatibleEvidenceException {
		ProbNet markovNetworkInference = null;
		List<TablePotential> returnedProjectedPotentials;

		try {
			returnedProjectedPotentials = network.tableProjectPotentials(evidence);
		} catch (NonProjectablePotentialException | WrongCriterionException e1) {
			throw new IncompatibleEvidenceException("Unexpected inference exception :" + e1.getMessage());
		}
		List<TablePotential> projectedPotentials = new ArrayList<>();

		for (TablePotential potential : returnedProjectedPotentials) {
			if (potential.getVariables().size() != 0) {
				projectedPotentials.add(potential);
			} else {
				if (potential.isAdditive()) {
					// It is a utility potential
					if (potential.values[0] != 0) {
						projectedPotentials.add(potential);
					}
				} else {
					// It is a probability potential
					if (potential.values[0] != 1) {
						projectedPotentials.add(potential);
					}
				}
			}
		}

		markovNetworkInference = network.buildMarkovDecisionNetwork(projectedPotentials);

		return markovNetworkInference;
	}

	private void removeSuperValueNodes() {
		List<Variable> utilityVariables = triangulatedNet.getVariables(NodeType.UTILITY);
		List<Node> nodes = new ArrayList<Node>(0);
		// add links between supervalue nodes and the next chance nodes
		for (Variable utilityVariable : utilityVariables) {
			if (isSuperValueNode(utilityVariable)) {
				getChanceAncestors(utilityVariable, nodes);
				for (Node ancestor : nodes) {
					try {
						triangulatedNet.addLink(ancestor.getVariable(), utilityVariable, true);
					} catch (NodeNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				nodes.clear();
			}
		}
		//remove links between supervalue nodes and  their utility parents
		for (Variable utilityVariable : utilityVariables) {
			if (isSuperValueNode(utilityVariable)) {
				Node utilityNode = triangulatedNet.getNode(utilityVariable);
				//ArrayList<Node> parent =  utilityNode.getParents();
				for (Node parent : utilityNode.getParents()) {
					if (parent.getNodeType() == NodeType.UTILITY) {
						triangulatedNet.removeLink(parent.getVariable(), utilityVariable, true);

					}
				}
			}
		}

	}

	private void getChanceAncestors(Variable utilityVariable, List<Node> parents) {
		Node utilityNode = triangulatedNet.getNode(utilityVariable);

		for (Node parent : utilityNode.getParents()) {
			if ((parent.getNodeType() == NodeType.UTILITY) && (isSuperValueNode(parent.getVariable()))) {
				getChanceAncestors(parent.getVariable(), parents);
			} else if (parent.getNodeType() == NodeType.UTILITY) {
				parents.addAll(parent.getParents());
			} else {
				parents.add(parent);
			}
		}

	}

	// Methods

	@SuppressWarnings("unused") private Node getNoUtilityAncestor(Node node) {
		if (node.getNodeType() == NodeType.UTILITY) {
			return getNoUtilityAncestor(node);
		} else
			return node;
	}

	/**
	 * Gets if the variable parameter is a supervalue node
	 *
	 * @param utilityVariable the variable to test
	 * @return true if the variable is a supervalue node. False if does not
	 */
	private boolean isSuperValueNode(Variable utilityVariable) {
		Node utilityNode = triangulatedNet.getNode(utilityVariable);
		int numOfUtilityParents = 0;
		for (Node parent : utilityNode.getParents()) {
			if (parent.getNodeType() == NodeType.UTILITY) {
				//if the node has two or more utility parents then is a super value node
				if ((numOfUtilityParents++) >= 2)
					;
				return true;
			}
		}
		return false;
	}

	/**
	 * Applies the algorithm
	 *
	 * @return A triangulated undirected graph. <code>Graph</code>.
	 */
	public ProbNet getTriangulatedGraph() {
		List<Node> deletionSequence = getDeletionSequence();
		triangulate(deletionSequence);
		return triangulatedNet;
	}

	/**
	 * It does nothing.
	 *
	 * @param event <code>UndoableEditEvent</code>.
	 */
	public void undoableEditWillHappen(UndoableEditEvent event) {
	}

	/**
	 * @param event (<code>UndoableEditEvent</code>) that contains the edit
	 *              with the <code>variableToDelete</code>.
	 */
	public void undoableEditHappened(UndoableEditEvent event) {
		super.undoableEditHappened(event);
		Variable variableToDelete = getEventVariable(event);
		if (variableToDelete != null) {
			Node nodeToDelete = triangulatedNet.getNode(variableToDelete);
			// Create a clique with the siblings of this node
			List<Node> siblings = nodeToDelete.getSiblings();
			createClique(siblings);

			// Eliminate the node and its links
			triangulatedNet.removeLinks(nodeToDelete);
			triangulatedNet.removeNode(nodeToDelete);
		}
	}

	/**
	 * Triangulate the graph given the deletion sequence
	 *
	 * @param deletionSequence <code>ArrayList</code> of <code>Node</code>s.
	 */
	public void triangulate(List<Node> deletionSequence) {
		// Creates an array of booleans for each node in the deletion sequence
		List<Node> nodeList = triangulatedNet.getNodes();
		int nodeListSize = nodeList.size();
		Map<Integer, Node> eliminated = new HashMap<Integer, Node>(nodeListSize);

		for (Node toDelete : deletionSequence) {
			// Collect the neighbors nodes to each node in the deletion sequence
			List<Node> notDeletedNeighbors = collectNeighbors(eliminated, toDelete);

			// Adds links between the collected filtered nodes
			createClique(notDeletedNeighbors);
		}
	}

	@Override
	/** @return Next <code>Variable</code> to delete. */ public Variable getVariableToDelete() {
		Node toDeleteNode = null;
		// Heuristic algorithm from Andres Cano and Serafin Moral
		double H6 = Double.MAX_VALUE, aux;
		for (int i = nodesToEliminate.size() - 1; i >= 0 && toDeleteNode == null; i--) {
			for (Node node : nodesToEliminate.get(i)) {
				double createdCliqueSize = createdCliqueSize(node);
				double sumCliqueSizes = sumCliqueSizes(node);
				aux = createdCliqueSize / sumCliqueSizes;
				if (aux < H6) {
					H6 = aux;
					toDeleteNode = (Node) node;
				}
			}
		}
		if (toDeleteNode != null) {
			return toDeleteNode.getVariable();
		} else
			return null;
	}

	/**
	 * Collect the neighbors nodes of a node that belogns to the deletion
	 * sequence excluding the nodes of the eliminated array.
	 *
	 * @param eliminated   (<code>HashMap</code> with key = <code>Integer</code>
	 *                     and value = <code>Node</code>)
	 * @param nodeToDelete <code>Node</code>.
	 * @return Not deleted neighbors (an <code>ArrayList</code> of
	 * <code>node</code>s)
	 */
	public List<Node> collectNeighbors(Map<Integer, Node> eliminated, Node nodeToDelete) {
		List<Node> neighbors = nodeToDelete.getNeighbors();
		eliminated.put(nodeToDelete.hashCode(), nodeToDelete);//Delete this node
		List<Node> notDeletedNeighbors = new ArrayList<Node>();
		for (Node node : neighbors) { // Filter the collected nodes
			if (!eliminated.containsValue(node)) {
				notDeletedNeighbors.add(node);
			}
		}
		return notDeletedNeighbors;
	}

	/**
	 * Creates undirected links between each pair of nodes in the collection
	 * received if them does not exists.
	 *
	 * @param nodes <code>Collection</code> of <code>? extends Node</code>s.
	 */
	public void createClique(Collection<? extends Node> nodes) {
		for (Node node1 : nodes) {
			for (Node node2 : nodes) {
				if ((node1 != node2) && (!node1.isSibling(node2))) {
					try { // Adds an undirected link
						new Link<Node>(node1, node2, false);
					} catch (Exception exception) {
						logger.fatal(exception);
					}
				}
			}
		}
	}

	/**
	 * @return An ordered list of nodes in an <code>List</code>
	 */
	private List<Node> getDeletionSequence() {
		List<Node> nodesUndirected = triangulatedNet.getNodes();
		int numNodes = nodesUndirected.size();
		int[] ordering = new int[numNodes];
		List<Node> deletionSequence = new ArrayList<Node>(numNodes);

		List<Variable> variablesUndirected = new ArrayList<Variable>(numNodes);
		for (Node node : nodesUndirected) {
			variablesUndirected.add(node.getVariable());
		}

		// Builds the deletion sequence
		for (int i = 0; i < numNodes; i++) {
			Node toDeleteNode = triangulatedNet.getNode(getVariableToDelete());

			// Store the node
			ordering[i] = variablesUndirected.indexOf(toDeleteNode.getVariable());

			// Create a clique with the siblings of this node
			List<Node> siblings = toDeleteNode.getSiblings();
			createClique(siblings);

			// Eliminate the node and its links
			triangulatedNet.removeLinks(toDeleteNode);
		}
		try {
			triangulatedNet = projectTablesAndBuildMarkovDecisionNetwork(probNet, null);
		} catch (IncompatibleEvidenceException e) {
			e.printStackTrace();
		}

		// Builds the pointers array to the deletion sequence from the ordering
		nodesUndirected = triangulatedNet.getNodes();
		for (int i = 0; i < numNodes; i++) {
			deletionSequence.add(nodesUndirected.get(ordering[i]));
		}
		return deletionSequence;
	}

	/**
	 * Calculates the heuristic metric <i>S(i)</i>: size of the clique created
	 * by deleting the <i>i</i> node
	 *
	 * @param toDeleteNode <code>Node</code>
	 * @return The heuristic metric <i>S(i)</i>. <code>long</code>.
	 */
	private long createdCliqueSize(Node toDeleteNode) {
		return cliqueSize(toDeleteNode.getNeighbors());
	}

	/**
	 * Calculates the heuristic metric <i>C(i)</i>: sum of clique sizes in the
	 * subgraph of <i>X<sub>i</sub></i> and its adjacent nodes.
	 *
	 * @param center <code>Node</code>
	 */
	private long sumCliqueSizes(Node center) {
		// Obtains the subgraph
		List<Node> subgraph = center.getNeighbors();
		List<Set<Node>> cliques = getSubgraphCliques(center, subgraph);
		long sum = 0;
		for (Set<Node> clique : cliques) {
			sum += cliqueSize(clique);
		}
		return sum;
	}

	/**
	 * A node (called center) and its neighbors constitute a subgraph.
	 * This method calculates the subgraph cliques.
	 *
	 * @param center center
	 * @param subgraph The <code>center</code> neighbors
	 * @return An <code>ArrayList</code> with the cliques in
	 * <code>HashSet</code>s
	 */
	private List<Set<Node>> getSubgraphCliques(Node center, List<Node> subgraph) {
		List<Set<Node>> cliques = new ArrayList<Set<Node>>();
		List<Set<Node>> newCliques;
		// First clique
		Set<Node> firstClique = new HashSet<Node>();
		firstClique.add(center);
		cliques.add(firstClique);

		// For each node adds that node to each clique (if it is possible).
		// Otherwise creates a new clique with the intersection of the neighbors
		// of that node with the clique
		for (Node node : subgraph) {
			newCliques = new ArrayList<Set<Node>>();
			Set<Node> siblingsNode = new HashSet<Node>(node.getSiblings());
			for (Set<Node> clique : cliques) {
				if (siblingsNode.containsAll(clique)) {
					clique.add(node);
				} else {
					Set<Node> newClique = new HashSet<Node>(clique);
					newClique.retainAll(siblingsNode);
					newClique.add(node);
					newCliques.add(newClique);
				}
			}

			// Elimination of redundant cliques
			int numNewCliques = newCliques.size();
			boolean[] eliminated = new boolean[numNewCliques];
			for (int i = 0; i < numNewCliques; i++) {
				eliminated[i] = false;
			}
			List<Set<Node>> toEliminate = new ArrayList<Set<Node>>();
			for (int i = 0; i < numNewCliques - 1; i++) {
				if (!eliminated[i]) {
					Set<Node> clique_i = newCliques.get(i);
					for (int j = i + 1; j < numNewCliques; j++) {
						if (!eliminated[j]) {
							Set<Node> clique_j = newCliques.get(j);
							if (clique_i.containsAll(clique_j)) {
								toEliminate.add(clique_j);
								eliminated[j] = true;
							} else {
								if (clique_j.containsAll(clique_i)) {
									toEliminate.add(clique_i);
									eliminated[i] = true;
								}
							}
						}
					}
				}
			}
			for (Set<Node> clique : toEliminate) {
				newCliques.remove(clique);
			}

			// Elimination of newCliques included in Cliques or vice versa
			numNewCliques = newCliques.size();
			int numCliques = cliques.size();
			eliminated = new boolean[numCliques];
			boolean[] eliminatedNew = new boolean[numNewCliques];
			for (int i = 0; i < numCliques; i++) {
				eliminated[i] = false;
			}
			for (int i = 0; i < numNewCliques; i++) {
				eliminatedNew[i] = false;
			}
			toEliminate = new ArrayList<Set<Node>>();
			List<Set<Node>> toEliminateNew = new ArrayList<Set<Node>>();
			// Comparison between new and old cliques
			for (int i = 0; i < numNewCliques; i++) {
				if (!eliminatedNew[i]) {
					Set<Node> newClique = newCliques.get(i);
					for (int j = 0; j < numCliques; j++) {
						if (!eliminated[j]) {
							Set<Node> clique = cliques.get(j);
							if (newClique.containsAll(clique)) {
								eliminated[j] = true;
								toEliminate.add(clique);
							} else {
								if (clique.containsAll(newClique)) {
									eliminatedNew[i] = true;
									toEliminateNew.add(newClique);
									j = numCliques;
								}
							}
						}
					}
				}
			}

			// Eliminate redundant cliques
			for (Set<Node> clique : toEliminate) {
				cliques.remove(clique);
			}
			for (Set<Node> clique : toEliminateNew) {
				newCliques.remove(clique);
			}

			// Adds the new cliques to cliques collection
			cliques.addAll(newCliques);
		}

		return cliques;
	}

	public void undoEditHappened(UndoableEditEvent event) {
		// TODO Auto-generated method stub

	}

	public String toString() {
		return nodesToEliminate.toString();
	}
}
