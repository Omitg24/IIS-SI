/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

/*
 * Licence, version 1.1 (EUPL) Unless required by applicable law, this code is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.inference.huginPropagation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.openmarkov.core.exception.ConstraintViolationException;
import org.openmarkov.core.exception.DoEditException;
import org.openmarkov.core.exception.NonProjectablePotentialException;
import org.openmarkov.core.exception.WrongCriterionException;
import org.openmarkov.core.inference.heuristic.EliminationHeuristic;
import org.openmarkov.core.model.network.Node;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.ProbNetOperations;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.potential.Potential;
import org.openmarkov.core.model.network.potential.TablePotential;
import org.openmarkov.inference.huginPropagation.action.RemoveMarkovNetNodeEdit;

/**
 * This class implements an algorithm to create a Hugin tree from a Markov
 * network.
 *
 * @author marias
 * @author fjdiez
 * @version 1.0
 * @see ClusterForest
 * @see org.openmarkov.inference.huginPropagation.HuginClique
 * @see org.openmarkov.inference.heuristic.simpleElimination.SimpleElimination
 * @since OpenMarkov 1.0
 */
public class HuginForest extends ClusterForest {
	// Attributes
	/**
	 * It is a variable that exists during the construction of the
	 * <code>HuginForest</code>. It is the collection of the cliques waiting to
	 * be assigned a parent in the <code>HuginForest</code>. Each clique comes
	 * with its separator to the variables remaining in the MarkovNet. It is
	 * implemented as a <code>HashMap</code> using the first separator variable
	 * as a key.
	 */
	protected Map<Variable, List<HuginClique>> orphanCliques;
	/**
	 * Used to build the <code>ClusterForest</code>.
	 */
	protected ProbNet markovNet;


	// Constructors

	/**
	 * Create a <code>HuginForest</code> from a Markov network and an heuristic.
	 * The Markov network received will be destroyed.
	 *
	 * @param markovNet  <code>MarkovNet</code>.
	 * @param heuristic  <code>EliminationHeuristic</code>.
	 * @param queryNodes <code>ArrayList</code> of <code>Node</code>. <code>queryNodes</code>
	 *                      must contain at least one element.
	 * @throws WrongCriterionException WrongCriterionException
	 * @throws NonProjectablePotentialException NonProjectablePotentialException
	 * @throws ConstraintViolationException ConstraintViolationException
	 * @throws DoEditException DoEditException
	 */
	/// Why this constructor have not PNEsupport?
	public HuginForest(ProbNet markovNet, EliminationHeuristic heuristic, List<Node> queryNodes)
			throws ConstraintViolationException, NonProjectablePotentialException,
			WrongCriterionException, DoEditException {
		super();
		this.markovNet = markovNet;
		markovNet.marry(queryNodes);
		orphanCliques = new HashMap<Variable, List<HuginClique>>();
		// eliminate variables
		Variable variableToDelete = heuristic.getVariableToDelete();
		while (variableToDelete != null) {
			removeMarkovNetNode(variableToDelete);
			variableToDelete = heuristic.getVariableToDelete();
		}
		// If the query variables form a clique (containing no other variables)
		// they will remain in the graph when all the non-query variables
		// have been eliminated
		if (markovNet.getNumNodes() > 0) {
			removeMarkovNetNode(queryNodes.get(0).getVariable());
		}
	}
	/**
	 * Creates a HuginForest for a Markov network
	 * The <code>MarkovNet</code> received will be destroyed.
	 *
	 * @param markovNet <code>MarkovNet</code>.
	 * @param heuristic <code>EliminationHeuristic</code>.
	 * @throws WrongCriterionException WrongCriterionException
	 * @throws NonProjectablePotentialException NonProjectablePotentialException
	 * @throws DoEditException DoEditException
	 */
	public HuginForest(ProbNet markovNet, EliminationHeuristic heuristic)
			throws DoEditException, NonProjectablePotentialException, WrongCriterionException {
		super();
		this.markovNet = markovNet;
		orphanCliques = new HashMap<Variable, List<HuginClique>>();
		// eliminate variables
		Variable variableToDelete = heuristic.getVariableToDelete();
		// register the created heuristic as listener
		this.markovNet.getPNESupport().addUndoableEditListener(heuristic);
		while (variableToDelete != null) {
			removeMarkovNetNode(variableToDelete);
			variableToDelete = heuristic.getVariableToDelete();
		}
		// unregister heuristic
		this.markovNet.getPNESupport().removeUndoableEditListener(heuristic);
	}

	// Methods

	/**
	 * @return The set of cliques = rootCliques + its children (recursively) +
	 * orphan cliques (<code>ArrayList</code> of <code>Node</code>s).
	 */
	public List<ClusterOfVariables> getNodes() {
		// rootCliques + its children (recursively)
		List<ClusterOfVariables> nodes = super.getNodes();
		// orphan cliques. recovers them in a HashMap because there are stored
		// in orphanCliques: a HashMap<Variable, ArrayList<HuginClique>>
		Set<HuginClique> auxOrphanCliques = new HashSet<HuginClique>();
		for (List<HuginClique> array : orphanCliques.values()) {
			auxOrphanCliques.addAll(array);
		}
		nodes.addAll(auxOrphanCliques);
		return nodes;
	}

	/**
	 * Change the <code>ClusterForest</code> structure to convert the given
	 * <code>HuginClique</code> in rootCluster
	 *
	 * @param newRoot <code>HuginClique</code>.
	 */
	public void convertToRoot(ClusterOfVariables newRoot) {
		Stack<ClusterOfVariables> cliquesStack = new Stack<>();
		ClusterOfVariables actualClique = rootClusters.get(0);
		// Store cliques from newRoot to the actual root in a Stack
		List<ClusterOfVariables> parents = actualClique.getParents();
		while (parents != null) {
			actualClique = parents.get(0);
			cliquesStack.add(actualClique);
			parents = actualClique.getParents();
		}
		// Create the new cliques and redirect links
		ClusterOfVariables root = cliquesStack.pop();
		ClusterOfVariables nextClique = cliquesStack.pop();
		while (nextClique != null) {
			// removes old link
			removeLink(root, nextClique, true);
			// create new link
			addLink(nextClique, root, true);
			root.setSeparatorVariables(nextClique.getSeparatorVariables());
			root = nextClique;
			nextClique = cliquesStack.pop();
		}
		// The root has no separator
		newRoot.setSeparatorVariables(new ArrayList<Variable>());
	}

	/**
	 * @param huginClique <code>HuginClique</code>.
	 */
	private void addOrphanClique(HuginClique huginClique) {
		// use the first separator variable as a key
		Variable key = huginClique.getSeparatorVariables().get(0);
		List<HuginClique> cliqueList = orphanCliques.get(key);
		if (cliqueList == null) {
			cliqueList = new ArrayList<HuginClique>();
			orphanCliques.put(key, cliqueList);
		}
		cliqueList.add(huginClique);
	}

	/**
	 * @param markovNet  This <code>markovNet</code> will be destroyed.
	 * @param heuristic  <code>EliminationHeuristic</code>.
	 * @param queryNodes <code>ArrayList</code> of <code>Node</code>s.
	 * @throws Exception Exception
	 */
	@SuppressWarnings("unused") private ClusterForest createForest(ProbNet markovNet, EliminationHeuristic heuristic,
			ArrayList<Node> queryNodes) throws Exception {
		return new HuginForest(markovNet, heuristic, queryNodes);
	}

	/**
	 * When adding a clique to the <code>HuginForest</code> looks for the orphan
	 * cliques than can be connected as children of that clique
	 *
	 * @param clique          A Hugin clique to be added to the <code>HuginForest</code>
	 * @param cliqueVariables cliqueVariables
	 */
	private void relocateOrphanCliques(HuginClique clique, List<Variable> cliqueVariables) {
		List<HuginClique> auxOrphanCliques;
		// Examines all the orphan cliques associated (in the hashMap)
		// with some of the variables in the clique
		for (Variable variable : cliqueVariables) {
			auxOrphanCliques = orphanCliques.get(variable);
			if (auxOrphanCliques != null) {
				for (ClusterOfVariables orphanClique : new ArrayList<HuginClique>(auxOrphanCliques)) {
					// If all the separator variables in the orphan clique
					// are contained in the (new) current clique,
					// then the orphan clique turns into a
					// child of the current clique
					if (clique.containsAll(orphanClique.getSeparatorVariables())) {
						addLink(clique, orphanClique, true);
						auxOrphanCliques.remove(orphanClique);
					}
				}
			}
		}
	}

	/**
	 * <code>variables2Cluster</code> associates a variable with a clique that is
	 * the root clique or that contains that variable in its separator.
	 *
	 * @param clique clique
	 */
	private void updateVariables2Clusters(ClusterOfVariables clique) {
		List<Variable> cliqueVariables = clique.getVariables();
		for (Variable variable : cliqueVariables) {
			ClusterOfVariables cliqueVariable = variables2Clusters.get(variable);
			if (cliqueVariable == null) {
				variables2Clusters.put(variable, clique);
			} else {
				if ((cliqueVariable.separatorVariables != null) && (
						!cliqueVariable.separatorVariables.contains(variable)
				) && ((clique.separatorVariables == null) || (clique.separatorVariables.contains(variable)))) {
					variables2Clusters.put(variable, clique);
				}
			}
		}
	}

	/**
	 * @param variable <code>Variable</code>
	 */
	protected void removeMarkovNetNode(Variable variable)
			throws DoEditException, NonProjectablePotentialException, WrongCriterionException {
		Node markovNetNode = markovNet.getNode(variable);
		List<Node> neighborNodes = markovNetNode.getNeighbors();
		// cliqueNodes = node + neighborNodes
		List<Node> cliqueNodes = new ArrayList<>(neighborNodes);
		cliqueNodes.add(markovNetNode);
		List<Variable> cliqueVariables = new ArrayList<Variable>();
		for (Node cliqueNode : cliqueNodes) {
			cliqueVariables.add(cliqueNode.getVariable());
		}
		// separatorNodes = nodes having neighbors outside the clique
		List<Node> separatorNodes = new ArrayList<Node>();
		List<Variable> separatorVariables = new ArrayList<Variable>();
		// nodesToDelete = cliqueNodes that are not in the separator
		List<Node> nodesToDelete = new ArrayList<Node>();
		List<Variable> variablesToDelete = new ArrayList<Variable>();
		// classifies the nodes into separator nodes and nodes to be deleted
		for (Node neighbor : cliqueNodes) {
			if (ProbNetOperations.hasNeighborsOutside(markovNet, neighbor, cliqueNodes)) {
				separatorNodes.add(neighbor);
				separatorVariables.add(neighbor.getVariable());
			} else {
				nodesToDelete.add(neighbor);
				variablesToDelete.add(neighbor.getVariable());
			}
		}
		HuginClique clique = new HuginClique(this, cliqueVariables, separatorVariables);
		addNode(clique);
		updateVariables2Clusters(clique);
		// extracts the potentials whose variables are all in the clique
		// (examines first the separator nodes) and assigns those potentials
		// to the clique
		List<Potential> potentialsToRemove;
		for (Node separatorNode : separatorNodes) {
			potentialsToRemove = new ArrayList<Potential>();
			for (Potential auxPot : separatorNode.getPotentials()) {
				TablePotential potential = (TablePotential) auxPot;
				List<Variable> potentialVariables = potential.getVariables();
				if (clique.containsAll(potentialVariables)) {
					potentialsToRemove.add(potential);
					clique.addPriorPotential(potential);
				}
			}
			for (Potential potentialToRemove : potentialsToRemove) {
				separatorNode.removePotential(potentialToRemove);
			}
		}
		// extracts the potentials from the nodes to be deleted,
		// assigns those potentials to the clique and deletes those nodes
		for (Node nodeToDelete : nodesToDelete) {
			for (Potential auxPot : nodeToDelete.getPotentials()) {
				TablePotential potential = (TablePotential) auxPot;
				clique.addPriorPotential(potential);
			}
			RemoveMarkovNetNodeEdit deleteMarkovNetNodeEdit = new RemoveMarkovNetNodeEdit(markovNet, nodeToDelete);
			try {
				markovNet.doEdit(deleteMarkovNetNodeEdit);
			} catch (ConstraintViolationException e) {
				throw new DoEditException(e);
			}
		}
		relocateOrphanCliques(clique, cliqueVariables);
		if (separatorVariables.isEmpty()) {
			setClusterAsRoot(clique);
		} else {
			addOrphanClique(clique);
		}
	}

	/**
	 * Returns the orphanCliques.
	 *
	 * @return the orphanCliques.
	 */
	public Map<Variable, List<HuginClique>> getOrphanCliques() {
		return orphanCliques;
	}
}
