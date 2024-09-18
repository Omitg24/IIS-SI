/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.inference;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;

import org.openmarkov.core.exception.NodeNotFoundException;
import org.openmarkov.core.exception.NonProjectablePotentialException;
import org.openmarkov.core.exception.WrongCriterionException;
import org.openmarkov.core.model.graph.Link;
import org.openmarkov.core.model.network.EvidenceCase;
import org.openmarkov.core.model.network.Node;
import org.openmarkov.core.model.network.NodeType;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.ProbNetOperations;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.VariableType;
import org.openmarkov.core.model.network.potential.ExactDistrPotential;
import org.openmarkov.core.model.network.potential.FunctionPotential;
import org.openmarkov.core.model.network.potential.Potential;
import org.openmarkov.core.model.network.potential.ProductPotential;
import org.openmarkov.core.model.network.potential.SumPotential;
import org.openmarkov.core.model.network.potential.TablePotential;
import org.openmarkov.core.model.network.potential.operation.DiscretePotentialOperations;
import org.openmarkov.core.model.network.type.DecisionAnalysisNetworkType;

import net.sourceforge.jeval.EvaluationException;

public class BasicOperations {
	/**
	 * The source probNet
	 * @param evidence Evidence
	 * @param node Node
	 * @return potential
	 */
	public static Potential buildPotentialByAbsorbingParents(Node node, EvidenceCase evidence) {
		Potential newPotential = null;
		
		List<TablePotential> tablePotentials = new ArrayList<>();
		List<Variable> parentVariable = new ArrayList<>();
		for (Node parent:node.getParents()) {
			TablePotential auxTablePot = null;
			try {
				auxTablePot = parent.getPotentials().get(0).tableProject(evidence, null).get(0);
			} catch (NonProjectablePotentialException | WrongCriterionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			tablePotentials.add(auxTablePot);
			parentVariable.add(parent.getVariable());
		}
		Potential nodePotential = node.getPotentials().get(0);
		if (nodePotential instanceof SumPotential) {
			newPotential = DiscretePotentialOperations.sum(tablePotentials);
		} else if (nodePotential instanceof ProductPotential) {
			newPotential = DiscretePotentialOperations.multiply(tablePotentials);
		} else { // FunctionPotential
			try {
				newPotential = DiscretePotentialOperations.evaluateFunctionPotential(
						(FunctionPotential) nodePotential, tablePotentials, parentVariable);
			} catch (NumberFormatException | EvaluationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		// }
		return buildExactDistrPotentialUtility(node.getVariable(), (TablePotential) newPotential);
	}

	private static ExactDistrPotential buildExactDistrPotentialUtility(Variable variable, TablePotential pot) {
		List<Variable> variables = new ArrayList<>();
		variables.add(variable);
		variables.addAll(pot.getVariables());
		ExactDistrPotential exact = new ExactDistrPotential(variables);
		exact.setTablePotential(pot);
		return exact;
	}

	private static boolean isSumSuperValueNode(ProbNet network, Variable utilityVariable) {
		List<Potential> potentials = network.getNode(utilityVariable).getPotentials();
		return (!potentials.isEmpty() && potentials.get(0) instanceof SumPotential);
	}

	/**
	 * @param network Network from which we extract terminal utility variables
	 * @return A list of utility nodes that have no children
	 */
	public static List<Variable> getTerminalUtilityVariables(ProbNet network) {
		List<Variable> utilityVariables = network.getVariables(NodeType.UTILITY);
		List<Variable> terminalUtilityNodes = new ArrayList<>();
		for (Variable utilityVariable : utilityVariables) {
			Node utilityNode = network.getNode(utilityVariable);
			if (network.getNumChildren(utilityNode) == 0) {
				terminalUtilityNodes.add(utilityVariable);
			}
		}
		return terminalUtilityNodes;
	}

	static boolean isNumeric(Variable v) {
		return v.getVariableType() == VariableType.NUMERIC;
	}

	/**
	 * @param network Network from which we extract terminal numeric variables
	 * @return A list of utility nodes that have no children
	 */
	public static List<Variable> getNumericVariablesWithoutNumericChildren(ProbNet network) {
		return network.getVariables().stream().filter(v -> (isNumeric(v) && !hasNumericChildren(network, v)))
				.collect(Collectors.toList());
	}

	private static boolean hasNumericChildren(ProbNet network, Variable v) {
		List<Node> children = network.getChildren(network.getNode(v));
		return children.stream().anyMatch(x -> isNumeric(x.getVariable()));
	}

	/*
	 * @param network Network from which we extract terminal utility nodes
	 * @return A list of utility nodes that have no children
	 *//*
		 * public static List<Node> getTerminalUtilityNodes(ProbNet network) {
		 * List<Node> utilityNodes = network.getNodes(NodeType.UTILITY); List<Node>
		 * terminalUtilityNodes = new ArrayList<>(); for (Node utilityNode :
		 * utilityNodes) { if (network.getNumChildren(utilityNode) == 0) {
		 * terminalUtilityNodes.add(utilityNode); } } return terminalUtilityNodes; }
		 */
	/*
	 * @param sourceProbNet Network from which we remove utility nodes
	 * @return A copy of the probNet after removing utility nodes.
	 */
	/*
	 * public static ProbNet removeUtilityNodes(ProbNet sourceProbNet) { ProbNet
	 * network = sourceProbNet.copy(); for (Variable utilityVariable :
	 * network.getVariables(NodeType.UTILITY)) { Node node =
	 * network.getNode(utilityVariable); network.removeNode(node); } return network;
	 * }
	 */

	public static ProbNet absorbAllIntermediateNumericNodes(ProbNet sourceProbNet, EvidenceCase evidence) {
		ProbNet network = sourceProbNet.copy();
		
		// Get numerical variables without numerical children to start the recursion
		List<Variable> terminalNumericVariables = getNumericVariablesWithoutNumericChildren(network);
		Set<Variable> processed = new HashSet<>();
		terminalNumericVariables.forEach(v -> absorbAllIntermediateNumericNodes(network,v,evidence,processed));
		return network;
		
	}
	
	private static void absorbAllIntermediateNumericNodes(ProbNet network, Variable variable, EvidenceCase evidence,
			Set<Variable> processed) {
	
		Node node = network.getNode(variable);
		List<Node> parents = node.getParents();
		for (Node parent : parents) {
			Variable parentVariable = parent.getVariable();
			if (!processed.contains(parentVariable) && isNumeric(parentVariable)) {
				absorbAllIntermediateNumericNodes(network, parentVariable, evidence, processed);
			}
		}
		if (haveParentsAndAreAllAbsorbable(node)) {
			absorbParents(network, node, evidence);
		}
		processed.add(variable);	
	}
	
	
	/**
	 * @param node Node
	 * @return true iff the node has parents and are all observable
	 */
	public static boolean haveParentsAndAreAllAbsorbable(Node node) {
		return !node.getParents().isEmpty() && areAllItsParentsAbsorbable(node);
	}

	

	public static void absorbParents(ProbNet network, Node node, EvidenceCase evidence) {
		List<Node> parents = network.getParents(node);
		
		Variable nodeVariable = node.getVariable();
		Potential potential = buildPotentialByAbsorbingParents(node, evidence);

		for (Node parent : parents) {
			network.removeLink(parent.getVariable(), nodeVariable, true);
		}
		for (Variable variable : potential.getVariables()) {
			if (variable != nodeVariable) {
				try {
					network.addLink(variable, nodeVariable, true);
				} catch (NodeNotFoundException e) {
					e.printStackTrace();
				}
			}
		}
		// sets the new potential
		List<Potential> newPotentials = new ArrayList<>();
		newPotentials.add(potential);
		network.getNode(nodeVariable).setPotentials(newPotentials);
		parents.forEach(x -> network.removeNode(x));
	}



	/**
	 * Assumes the structure of super value verifies that there are no more than one
	 * path between two utility nodes.
	 *
	 * @param sourceProbNet Network from which we extract the utility nodes
	 * @return A list of utility nodes that must be kept when we want to have a set
	 *         of utility nodes with an implicit sum
	 */
	private static List<Variable> getUtilityNodesToKeepImplicitSum(ProbNet sourceProbNet) {
		List<Variable> nodesToKeep = getTerminalUtilityVariables(sourceProbNet);
		while (thereAreSumNodesInTheList(sourceProbNet, nodesToKeep)) {
			removeASumNode(sourceProbNet, nodesToKeep);
		}
		return nodesToKeep;
	}

	/**
	 * @param sourceProbNet Network in which we test if there are sum nodes
	 * @param nodesToKeep   List of variables (of the nodes to keep)
	 * @return true if there are some sum node in the list 'nodesToKeep'
	 */
	private static boolean thereAreSumNodesInTheList(ProbNet sourceProbNet, List<Variable> nodesToKeep) {
		boolean thereAre = false;
		for (int i = 0; (i < nodesToKeep.size()) && !thereAre; i++) {
			Variable auxVar = nodesToKeep.get(i);
			thereAre = (isSumSuperValueNode(sourceProbNet, auxVar));
		}
		return thereAre;
	}

	/**
	 * @param sourceProbNet source network
	 * @param nodesToKeep   list of variables of the nodes to keep Removes a sum
	 *                      node of the list and add its parents to the list
	 */
	private static void removeASumNode(ProbNet sourceProbNet, List<Variable> nodesToKeep) {
		boolean removed = false;
		for (int i = 0; (i < nodesToKeep.size()) && !removed; i++) {
			Variable auxVar = nodesToKeep.get(i);
			removed = (isSumSuperValueNode(sourceProbNet, auxVar));
			if (removed) {
				nodesToKeep.remove(auxVar);
				List<Node> parentNodes = sourceProbNet.getParents(sourceProbNet.getNode(auxVar));
				nodesToKeep.addAll(ProbNet.getVariables(parentNodes));
			}
		}
	}

	/**
	 * 
	 * @param node the node to test
	 * @return true if all the parents of a node can be absorbed. It must be fulfill
	 *         three conditions: 1) It is a numeric node 2) Its parents are all
	 *         numeric 3) Its grandparents are all discrete
	 */
	public static boolean areAllItsParentsAbsorbable(Node node) {
		boolean areAbsorbable = true;

		// Verify condition 1)
		areAbsorbable = getVariableType(node) == VariableType.NUMERIC;

		if (areAbsorbable) {
			// Verify condition 2)
			List<Node> parents = node.getParents();
			areAbsorbable = areAllVariablesOfType(parents, VariableType.NUMERIC);

			if (areAbsorbable) {
				// Verify condition 3)
				Set<Node> grandParents = new HashSet<>();
				parents.forEach(x -> grandParents.addAll(x.getParents()));
				List<Node> grandParentsList = new ArrayList<>();
				grandParentsList.addAll(grandParents);
				areAbsorbable = areAllVariablesOfType(grandParentsList, VariableType.FINITE_STATES);
			}
		}

		return areAbsorbable;
	}

	private static boolean areAllVariablesOfType(List<Node> nodes, VariableType type) {
		boolean areAll = true;

		for (int i = 0; i < nodes.size() && areAll; i++) {
			areAll = getVariableType(nodes.get(i)) == type;
		}
		return areAll;
	}

	private static VariableType getVariableType(Node node) {
		return node.getVariable().getVariableType();
	}

	/*
	 *************************
	 *************************
	 *************************
	 *************************
	 * // TODO: check this... PARTIAL ORDER OPERATIONS. SOME OF THEM TO BE REMOVED
	 *************************
	 *************************
	 *************************
	 *************************
	 * 
	 */

	/**
	 * @param probNet Network
	 * @return {@code List} of {@code List} of {@code Variable}s
	 */
	public static List<List<Variable>> getOrder(ProbNet probNet) {
		List<List<Variable>> copyOfOrder = new ArrayList<>();
		for (List<Variable> list : calculatePartialOrder(probNet)) {
			copyOfOrder.add(new ArrayList<>(list));
		}
		return copyOfOrder;
	}

	/**
	 * @param probNet A probabilistic network of which the partial order will be
	 *                calculated
	 * @return {@code ArrayList} of {@code ArrayList} of
	 *         {@code Variables} with the partial order of the received probNet
	 */
	public static List<List<Variable>> calculatePartialOrder(ProbNet probNet) {
		ProbNet idCopy = probNet.copy(); // Copy influence diagram

		/* A partial order is a list of lists of variables. */
		List<List<Variable>> partialOrder;

		// Get decisions (only) in elimination order
		Stack<Variable> decisions = getSequenceOfDecisions(idCopy);

		// Create elimination order adding chance nodes
		partialOrder = new ArrayList<>();
		List<Node> chanceNodes = probNet.getNodes(NodeType.CHANCE);
		HashSet<Variable> chanceVariables = new HashSet<>();
		for (Node chanceNode : chanceNodes) {
			chanceVariables.add(chanceNode.getVariable());
		}
		List<Variable> decisionsList = new ArrayList<>(decisions);
		Collections.reverse(decisionsList);

		for (int i = 0; i < decisionsList.size(); i++) {
			Variable decision = decisionsList.get(i);
			Node decisionNode = probNet.getNode(decision);
			// Get nodes of the decision parents
			List<Node> parentDecisionNodes = new ArrayList<>();
			List<Node> parentsCandidates;
			if (probNet.getNetworkType() != DecisionAnalysisNetworkType.getUniqueInstance()) {
				parentsCandidates = probNet.getParents(decisionNode);
			} else {
				parentsCandidates = (i == 0) ? ProbNetOperations.getAlwaysObservedVariables(probNet)
						: getVariablesRevealedTransitivelyByVariable(decisionsList.get(i - 1), probNet);
			}
			for (Node parent : parentsCandidates) {
				if (parent.getNodeType() != NodeType.DECISION) {
					if (chanceVariables.contains(parent.getVariable())) {
						parentDecisionNodes.add(parent);
						chanceVariables.remove(parent.getVariable());
					}
				}
			}
			// Add parents and decision
			int numParents = parentDecisionNodes.size();
			if (numParents > 0) {
				List<Variable> decisionVariableParents = new ArrayList<>(numParents);
				for (Node parent : parentDecisionNodes) {
					decisionVariableParents.add(parent.getVariable());
				}
				partialOrder.add(decisionVariableParents);
			}
			// Add decision variable
			partialOrder.add(Collections.singletonList(decision));
		}
		List<Variable> remainingVariables = new ArrayList<>(chanceVariables.size());
		for (Variable remainingVariable : chanceVariables) {
			remainingVariables.add(remainingVariable);
		}
		if (remainingVariables.size() > 0) {
			partialOrder.add(remainingVariables);
		}
		return partialOrder;
	}

	/**
	 * @param variable Variable
	 * @param probNet Network
	 * @return The list of variables revealed by a variable in a DAN or by a chance
	 *         variable revealed by that variable, and so on...
	 */
	private static List<Node> getVariablesRevealedTransitivelyByVariable(Variable variable, ProbNet probNet) {
		Node variableNode = probNet.getNode(variable);
		List<Node> revealed = new ArrayList<>();
		List<Node> children = probNet.getChildren(variableNode);
		for (Node child : children) {
			Link<Node> link = probNet.getLink(variableNode, child, true);
			if (link.getRevealingStates().toArray().length == variable.getStates().length) {
				revealed.add(child);
				for (Node auxRevealed : getVariablesRevealedTransitivelyByVariable(child.getVariable(), probNet)) {
					if (!revealed.contains(auxRevealed)) {
						revealed.add(auxRevealed);
					}
				}
			}
		}
		return revealed;
	}

	public static Stack<Variable> getSequenceOfDecisions(ProbNet idCopy) {
		int numDecisions = idCopy.getNumNodes(NodeType.DECISION);
		Stack<Variable> decisions = new Stack<>();
		do {
			List<Node> nodes = idCopy.getNodes();
			for (Node node : nodes) {
				if (idCopy.getNumChildren(node) == 0) {
					if (node.getNodeType() == NodeType.DECISION) {
						decisions.push(node.getVariable());
						numDecisions--;
					}
					idCopy.removeNode(node);
				}
			}
		} while (numDecisions > 0);
		return decisions;
	}

	public static List<Variable> getAnAdmissibleOrderOfDecisions(ProbNet probNet) {
		List<Variable> decisions = new ArrayList<>();

		for (List<Variable> variablesSet : calculatePartialOrder(probNet)) {
			if (containsOneDecision(probNet, variablesSet)) {
				decisions.addAll(variablesSet);
			}
		}
		return decisions;
	}

	private static boolean containsOneDecision(ProbNet probNet, Collection<Variable> variables) {
		boolean containsOneDecision = false;
		if (variables.size() == 1) {
			for (Variable variable : variables) {
				containsOneDecision = probNet.getNode(variable).getNodeType() == NodeType.DECISION;
			}
		}

		return containsOneDecision;
	}

	/**
	 * @param probNet Network
	 * @param queryVariables        List of variables
	 * @param evidenceVariables     List of variables
	 * @param conditioningVariables List of variables
	 * @param variablesToEliminate  List of variables
	 * @return An order that has been pruned by eliminating the variables that are
	 *         in queryVariables or in evidenceVariables or in conditioningVariables
	 *         or not in variablesToEliminate
	 */
	public static List<List<Variable>> projectPartialOrder(ProbNet probNet, List<Variable> queryVariables,
			List<Variable> evidenceVariables, List<Variable> conditioningVariables,
			List<Variable> variablesToEliminate) {
		List<List<Variable>> newOrder;
		List<List<Variable>> newOrder2;
		// Remove variables
		newOrder = new ArrayList<>();
		for (List<Variable> auxArray : calculatePartialOrder(probNet)) {
			List<Variable> cloneAuxArray;
			cloneAuxArray = new ArrayList<>(auxArray);
			for (Variable auxVar : auxArray) {
				if (queryVariables.contains(auxVar) || evidenceVariables.contains(auxVar)
						|| conditioningVariables.contains(auxVar) || !variablesToEliminate.contains(auxVar)) {
					cloneAuxArray.remove(auxVar);
				}
			}
			newOrder.add(cloneAuxArray);

		}
		// Copy the non empty array lists
		newOrder2 = new ArrayList<>();

		for (List<Variable> auxArray : newOrder) {
			if (auxArray.size() > 0) {
				newOrder2.add(auxArray);
			}
		}
		return newOrder2;
	}

	/**
	 * @param probNet Network
	 * @return A {@code String} with an array of arrays.
	 */
	public static String toStringPartialOrder(ProbNet probNet) {

		List<List<Variable>> partialOrder = calculatePartialOrder(probNet);

		StringBuilder buffer = new StringBuilder();
		int numArrays = partialOrder.size();
		for (int i = 0; i < numArrays; i++) {
			List<Variable> array = partialOrder.get(i);
			int arraySize = array.size();
			if (arraySize > 1) {
				buffer.append("{");
			}
			int j = 0;
			for (Variable variable : array) {
				buffer.append(variable);
				if (j++ < arraySize - 1) {
					buffer.append(", ");
				}
			}
			if (arraySize > 1) {
				buffer.append("}");
			}
			if (i < numArrays - 1) {
				buffer.append(", ");
			}
		}
		return buffer.toString();
	}

	public static int getNumVariables(ProbNet probNet) {
		int num = 0;

		List<List<Variable>> partialOrder = calculatePartialOrder(probNet);

		if (partialOrder != null) {
			for (List<Variable> auxArray : partialOrder) {
				if (auxArray != null) {
					num = num + auxArray.size();
				}
			}
		} else {
			num = 0;
		}
		return num;
	}

	public static List<List<Variable>> resetPartialOrderToTrivial(ProbNet probNet) {
		List<List<Variable>> partialOrder;
		List<Variable> variables = probNet.getChanceAndDecisionVariables();
		List<List<Variable>> variablesOrder = new ArrayList<>();
		variablesOrder.add(variables);
		partialOrder = new ArrayList<>(variablesOrder);
		// partialOrder.setOrder(variablesOrder);
		return partialOrder;
	}

	/**
	 * @param probNet Netowk
	 * @param evidenceVariables     {@code List&#60;Variable&#62;}
	 * @param conditioningVariables {@code List&#60;Variable&#62;}
	 * @param variablesToEliminate  {@code List&#60;Variable&#62;}
	 * @param queryVariables  		{@code List&#60;Variable&#62;}
	 * @return An order that has been pruned by eliminating the variables that are
	 *         in queryVariables or in evidenceVariables or in conditioningVariables
	 *         or not in variablesToEliminate
	 */
	public static List<List<Variable>> projectPartialOrder2(ProbNet probNet, List<Variable> queryVariables,
			List<Variable> evidenceVariables, List<Variable> conditioningVariables,
			List<Variable> variablesToEliminate) {
		List<List<Variable>> newOrder;
		List<List<Variable>> newOrder2;
		// Remove variables
		newOrder = new ArrayList<>();
		for (List<Variable> auxArray : calculatePartialOrder2(probNet)) {
			List<Variable> cloneAuxArray;
			cloneAuxArray = new ArrayList<>(auxArray);
			for (Variable auxVar : auxArray) {
				if (evidenceVariables.contains(auxVar) || conditioningVariables.contains(auxVar)
						|| !variablesToEliminate.contains(auxVar)) {
					cloneAuxArray.remove(auxVar);
				}
			}
			newOrder.add(cloneAuxArray);

		}
		// Copy the non empty array lists
		newOrder2 = new ArrayList<>();

		for (List<Variable> auxArray : newOrder) {
			if (auxArray.size() > 0) {
				newOrder2.add(auxArray);
			}
		}
		return newOrder2;
	}

	/**
	 * @param probNet A probabilistic network of which the partial order will be
	 *                calculated
	 * @return {@code ArrayList} of {@code ArrayList} of
	 *         {@code Variables} with the partial order of the received probNet
	 */
	public static List<List<Variable>> calculatePartialOrder2(ProbNet probNet) {
		ProbNet idCopy = probNet.copy(); // Copy influence diagram

		/* A partial order is a list of lists of variables. */
		List<List<Variable>> partialOrder;

		// Get decisions (only) in elimination order
		int numDecisions = idCopy.getNumNodes(NodeType.DECISION);
		ArrayList<Variable> decisions = new ArrayList<>(numDecisions);

		for (Node utilityNode : idCopy.getNodes(NodeType.UTILITY)) {
			idCopy.removeNode(utilityNode);
		}
		List<Node> nodes = idCopy.getNodes();
		do {
			HashSet<Node> newGenNodes = new HashSet<>();
			for (Node node : nodes) {
				if (idCopy.getNumParents(node) == 0) {
					if (node.getNodeType() == NodeType.DECISION) {
						if (nodes.size() == 1) {
							decisions.add(node.getVariable());
							numDecisions--;
							newGenNodes.addAll(idCopy.getChildren(node));
							idCopy.removeNode(node);
						} else {
							// If there are chance nodes remove them first (add Decision to the next
							// generation of removed nodes)
							newGenNodes.add(node);
						}
					} else {
						newGenNodes.addAll(idCopy.getChildren(node));
						idCopy.removeNode(node);
					}
				}
			}

			// Check if there are more than one decision in the nextGenNodes
			int numberOfDecisions = 0;
			for (Node node : newGenNodes) {
				if (node.getNodeType().equals(NodeType.DECISION)) {
					numberOfDecisions++;
				}
			}

			if (numberOfDecisions > 1) {
				// throw new NotEvaluableNetworkException("There are more than one decision");
				System.out.println("BAD NET");
			}

			nodes.clear();
			nodes.addAll(newGenNodes);
		} while (numDecisions > 0);

		// Create elimination order adding chance nodes
		partialOrder = new ArrayList<>(numDecisions * 2 + 1);
		List<Node> chanceNodes = probNet.getNodes(NodeType.CHANCE);
		HashSet<Variable> chanceVariables = new HashSet<>();
		for (Node chanceNode : chanceNodes) {
			chanceVariables.add(chanceNode.getVariable());
		}
		while (!decisions.isEmpty()) {
			Variable decision = decisions.remove(decisions.size() - 1);
			Node decisionNode = probNet.getNode(decision);
			// Get nodes of the decision parents
			List<Node> parentDecisionNodes = new ArrayList<>();
			for (Node parent : probNet.getParents(decisionNode)) {
				if (parent.getNodeType() != NodeType.DECISION) {
					if (chanceVariables.contains(parent.getVariable())) {
						parentDecisionNodes.add(parent);
						chanceVariables.remove(parent.getVariable());
					}
				}
			}
			// Add parents and decision
			int numParents = parentDecisionNodes.size();
			if (numParents > 0) {
				List<Variable> decisionVariableParents = new ArrayList<>(numParents);
				for (Node parent : parentDecisionNodes) {
					decisionVariableParents.add(parent.getVariable());
				}
				partialOrder.add(decisionVariableParents);
			}
			// Add decision variable
			partialOrder.add(Collections.singletonList(decision));
		}
		List<Variable> remainingVariables = new ArrayList<>(chanceVariables.size());
		for (Variable remainingVariable : chanceVariables) {
			remainingVariables.add(remainingVariable);
		}
		if (remainingVariables.size() > 0) {
			partialOrder.add(remainingVariables);
		}
		return partialOrder;
	}
	
	public static ProbNet removeSuperValueNodes(ProbNet sourceProbNet, EvidenceCase evidence) {
		return removeSuperValueNodes(sourceProbNet, evidence, false, false, null);
	}
	
	/**
	 * @param sourceProbNet         Network from which we remove super value nodes
	 * @param evidence              Evidence of the network
	 * @param keepComponents        keep (or not) components
	 * @param leaveImplicitSum      leave (or not) the implicit sum
	 * @param utilityVariableToKeep utility variable to keep
	 * @return A copy of the probNet by removing super-value nodes. When
	 *         keepComponents is false the output network is equivalent to
	 *         'sourceProbNet'. However, when keepComponents is true the output
	 *         network has a utility node without children corresponding to each
	 *         utility node in 'sourceProbNet', and the utility function is given
	 *         explicitly in terms of the ancestors chance and decision nodes.
	 *         Parameter 'leaveImplicitSum' only applies when 'keepComponents' is
	 *         false. When 'leaveImplicitSum' is true then the output is in the form
	 *         of influence diagrams with an implicit sum like those processed by
	 *         Jensen's variable elimination algorithm; otherwise the structure of
	 *         super-value nodes is reduced into an only utility node. If
	 *         'utilityVariableToKeep' is different from null then it is the only
	 *         potential to keep. Otherwise all the variables are considered.
	 */
	//TODO This method should never be used as we are moving from the UTILITY / SUPER-VALUE nodes ideas to the DISCRETE / NUMERIC ones.
	
	public static ProbNet removeSuperValueNodes(ProbNet sourceProbNet, EvidenceCase evidence, boolean keepComponents,
			boolean leaveImplicitSum, Variable utilityVariableToKeep) {
		ProbNet network = sourceProbNet.copy();
		List<Node> utilityNodes = network.getNodes(NodeType.UTILITY);
		for (Node utilityNode : utilityNodes) {
			Variable utilityVariable = utilityNode.getVariable();
			if ((areAllItsParentsAbsorbable(utilityNode) && utilityVariableToKeep == null)
					|| utilityVariable == utilityVariableToKeep) {
				//absorbIntermediateParentNode(network, utilityNode, evidence);
			}
		}
		if (!keepComponents) {
			List<Variable> nodesToKeep;
			if (utilityVariableToKeep == null) {
				if (leaveImplicitSum) {
					// Get the nodes such as there is an implicit sum
					// between them
					nodesToKeep = getUtilityNodesToKeepImplicitSum(sourceProbNet);
				} else {
					nodesToKeep = getTerminalUtilityVariables(sourceProbNet);
				}
			} else {
				nodesToKeep = new ArrayList<>();
				nodesToKeep.add(utilityVariableToKeep);
			}
			for (Node utilityNode : utilityNodes) {
				if (!nodesToKeep.contains(utilityNode.getVariable())) {
					network.removeNode(utilityNode);
				}
			}
		}
		return network;
	}


}
