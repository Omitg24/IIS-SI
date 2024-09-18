/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.model.network.potential.operation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openmarkov.core.model.graph.Link;
import org.openmarkov.core.model.network.Node;
import org.openmarkov.core.model.network.NodeType;
import org.openmarkov.core.model.network.State;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.constraint.NoLinkRestriction;
import org.openmarkov.core.model.network.potential.ExactDistrPotential;
import org.openmarkov.core.model.network.potential.Potential;
import org.openmarkov.core.model.network.potential.TablePotential;

public class LinkRestrictionPotentialOperations {

	/*****
	 * This class contains methods to modify TablePotentials with the
	 * restrictions of the link restrictions.
	 */
	private LinkRestrictionPotentialOperations() {
	}

	/****
	 * Analyzes if the node has any parent link with a link restriction
	 *
	 * @param node Noded
	 * @return {@code true} if the node has a link restriction
	 */
	public static boolean hasLinkRestriction(Node node) {
		if (!node.getProbNet().hasConstraint(NoLinkRestriction.class)) {
			return (!getParentLinksWithRestriction(node).isEmpty());
		}
		return false;
	}

	/*****
	 * Retrieves the parent links of a node which have a link restriction.
	 *
	 * @param node Node
	 * @return a collection of links which have a link restriction.
	 */
	public static List<Link<Node>> getParentLinksWithRestriction(Node node) {
		List<Link<Node>> links = node.getLinks();
		List<Link<Node>> linksWithRestriction = new ArrayList<>();
		List<Node> parents = node.getParents();

		for (Link<Node> link : links) {
			if (parents.contains(link.getNode1()) && link.hasRestrictions()) {
				linksWithRestriction.add(link);
			}
		}
		return linksWithRestriction;
	}

	public static List<int[]> getStateCombinationsWithLinkRestriction(Node node) {
		//CMI Issue #162
		Potential p = node.getPotentials().get(0);
		TablePotential potential;
		if (p instanceof ExactDistrPotential) {
			potential = (TablePotential) ((ExactDistrPotential) p).getTablePotential();
		} else {
			potential = (TablePotential) node.getPotentials().get(0);
		}
		//TablePotential potential = (TablePotential) node.getPotentials ().get (0);
		//CMF
		List<Variable> nodeVariables = potential.getVariables();
		List<int[]> stateList = new ArrayList<>();
		List<Link<Node>> links = getParentLinksWithRestriction(node);
		for (Link<Node> link : links) {
			Variable var1 = link.getNode1().getVariable();
			State[] var1States = var1.getStates();
			Variable var2 = link.getNode2().getVariable();
			State[] var2States = var2.getStates();
			Map<Integer, Integer> independentVariables = new HashMap<>();
			int var1Index = 0, var2Index = 0;
			for (int i = 0; i < nodeVariables.size(); i++) {
				Variable var = nodeVariables.get(i);
				if (var.equals(var1)) {
					var1Index = i;
				} else {
					if (var.equals(var2)) {
						var2Index = i;
					} else {
						independentVariables.put(i, var.getNumStates());
					}
				}
			}
			for (int i = 0; i < var1States.length; i++) {
				for (int j = 0; j < var2States.length; j++) {
					if (link.areCompatible(var1States[i], var2States[j]) == 0) {
						stateList.addAll(LinkRestrictionPotentialOperations
								.getStateCombinations(independentVariables, nodeVariables, i, var1Index, j, var2Index));
					}
				}
			}
		}
		return stateList;
	}

	/****
	 * Checks whether a combination of states of the node is influenced by a
	 * link restriction
	 *
	 * @param combination
	 *            - combination of states of the variables of the potential
	 * @param links
	 *            - parent links with a link restriction
	 * @param node
	 *            - the node
	 * @param nodeVariables
	 *            - the variables of the potential of the node
	 * @param nodeStateIndex
	 *            - the index of the state of the node
	 * @return {@code true} if the combination of states is not influences
	 *         by a link restriction
	 */
	private static boolean hasRestriction(int[] combination, Collection<Link<Node>> links, Node node,
			List<Variable> nodeVariables, int nodeStateIndex) {
		State[] nodeStates = node.getVariable().getStates();
		for (Link<Node> link : links) {
			Variable var1 = link.getNode1().getVariable();
			State[] var1States = var1.getStates();
			int var1Index = nodeVariables.indexOf(var1);
			int var1StateIndex = combination[var1Index];
			if (link.areCompatible(var1States[var1StateIndex], nodeStates[nodeStateIndex]) == 0) {
				return true;
			}
		}
		return false;
	}

	/******
	 * Recalculates the probability of the potential for the variable so that
	 * all combinations sum up to 1. The potential is calculates for all
	 * combinations of the variable given the state of the parent variables.
	 * This methods only assigns probabilities to state combinations, which are
	 * not influenced by a link restriction.
	 *
	 * @param node
	 *            - the probability node
	 * @param potential
	 *            - the probability potential
	 * @param stateCombination
	 *            - the combination of states of the potential.
	 *
	 * @return the potential with the updated probabilities.
	 */
	public static Potential redistributeProbabilities(Node node, TablePotential potential, int[] stateCombination) {
		List<Variable> nodeVariables = potential.getVariables();
		int varIndex = nodeVariables.indexOf(node.getVariable());
		Variable var = nodeVariables.get(varIndex);
		List<Link<Node>> parentLinks = getParentLinksWithRestriction(node);
		List<Integer> modifiableStateList = new ArrayList<>();
		int states = var.getNumStates();
		if (states > 1) {
			double sum = 0;
			int[] singleState = stateCombination.clone();
			for (int i = 0; i < states; i++) {

				singleState[varIndex] = i;
				if (!hasRestriction(singleState, parentLinks, node, nodeVariables, i)) {
					modifiableStateList.add(i);
					sum += potential.getValue(nodeVariables, singleState);
				}

			}
			if (sum > 0) {
				for (int i = 0; i < states; i++) {
					singleState[varIndex] = i;
					if (modifiableStateList.contains(i)) {
						double oldValue = potential.getValue(nodeVariables, singleState);
						potential.setValue(nodeVariables, singleState, oldValue / sum);
					}
				}
			} else {
				if (states == 2) {
					for (int i = 0; i < states; i++) {
						singleState[varIndex] = i;
						if (modifiableStateList.contains(i)) {
							potential.setValue(nodeVariables, singleState, 1);
						}
					}

				}

			}
		}

		return potential;
	}

	/*******
	 * Updates the probability potential according to the new link restriction
	 * added. The link restriction belongs to the combination of the variables
	 * of the link as specified by var1StateIndex and var2StateIndex.
	 *
	 * @param node
	 *            - The child node of the link.
	 * @param linkRestriction
	 *            - the linkRestriction
	 * @param var1StateIndex
	 *            - the index of the state of the parent variable
	 * @param var2StateIndex
	 *            - the index of the state of the child variable.
	 * @return the probability potential of the node updated properly.

	 */
	public static Potential updatePotentialByAddLinkRestriction(Node node, TablePotential linkRestriction,
			int var1StateIndex, int var2StateIndex) {

		List<Variable> linkVariables = linkRestriction.getVariables();
		Variable var1 = linkVariables.get(0);
		Variable var2 = linkVariables.get(1);
		Potential potential = node.getPotentials().get(0);

		List<Variable> nodeVariables = potential.getVariables();
		Map<Integer, Integer> independentVarMap = new HashMap<>();
		int var1Index = 0, var2Index = 0;
		for (int i = 0; i < nodeVariables.size(); i++) {
			Variable var = nodeVariables.get(i);
			if (var.equals(var1)) {
				var1Index = i;
			} else {
				if (var.equals(var2)) {
					var2Index = i;
				} else {
					independentVarMap.put(i, var.getNumStates());
				}
			}
		}

		List<int[]> stateCombinations = getStateCombinations(independentVarMap, nodeVariables, var1StateIndex,
				var1Index, var2StateIndex, var2Index);
		for (int[] configuration : stateCombinations) {
			((TablePotential) potential).setValue(nodeVariables, configuration, 0);
			if (node.getNodeType() == NodeType.CHANCE) {
				redistributeProbabilities(node, (TablePotential) potential, configuration);
			}

		}
		return potential;

	}

	/*********
	 * Updates the probability potential of the node considering the link
	 * restrictions of any parent link.
	 *
	 * @param node Node
	 * @return the probability potential of the node updated according to the
	 *         link restrictions.
	 */
	public static Potential updatePotentialByLinkRestrictions(Node node) {
		TablePotential potential = (TablePotential) (node.getPotentials().get(0));
		List<Link<Node>> parentLinks = getParentLinksWithRestriction(node);

		for (Link<Node> link : parentLinks) {
			potential = (TablePotential) updatePotentialByLinkRestriction(link.getNode2(),
					(TablePotential) link.getRestrictionsPotential(), potential);
		}
		return potential;
	}

	/*****
	 * Updates the probability potential of the node according to the link
	 * restriction.
	 *
	 * @param node
	 *            - the node having the potential
	 * @param linkRestriction
	 *            - the link restriction potential
	 * @param potential
	 *            - the probability potential to update.
	 * @return the probability potential updated according to the link
	 *         restriction.
	 */
	public static Potential updatePotentialByLinkRestriction(Node node, TablePotential linkRestriction,
			Potential potential) {

		List<Variable> linkVariables = linkRestriction.getVariables();
		Variable var1 = linkVariables.get(0);
		Variable var2 = linkVariables.get(1);
		State[] state1 = linkVariables.get(0).getStates();
		State[] state2 = linkVariables.get(1).getStates();

		List<Variable> nodeVariables = potential.getVariables();
		Map<Integer, Integer> independentVarMap = new HashMap<>();
		int var1Index = 0, var2Index = 0;
		for (int i = 0; i < nodeVariables.size(); i++) {
			Variable var = nodeVariables.get(i);
			if (var.equals(var1)) {
				var1Index = i;
			} else {
				if (var.equals(var2)) {
					var2Index = i;
				} else {
					independentVarMap.put(i, var.getNumStates());
				}
			}
		}

		for (int var1State = 0; var1State < state1.length; var1State++) {
			for (int var2State = 0; var2State < state2.length; var2State++) {
				int[] statesIndices = new int[] { var1State, var2State };
				if (linkRestriction.getValue(linkVariables, statesIndices) == 0) {
					List<int[]> stateCombinations = getStateCombinations(independentVarMap, nodeVariables, var1State,
							var1Index, var2State, var2Index);
					for (int[] configuration : stateCombinations) {
						((TablePotential) potential).setValue(nodeVariables, configuration, 0);
						if (node.getNodeType() == NodeType.CHANCE) {
							redistributeProbabilities(node, (TablePotential) potential, configuration);
						}
					}
				}
			}

		}

		return potential;

	}

	/*
	/*****
	 * Generates the state combinations for a set of variables having variable 1
	 * and variable 2 a given value.
	 * 
	 * @param independentVariables
	 *            - Set of independent variables
	 * @param variables
	 *            - List containing the variables
	 * @param var1Value
	 *            - the value of the state of variable1
	 * @param var1Index
	 *            - the index to which corresponds variable1 in the variables
	 *            list.
	 * @param var2Value
	 *            - the value of the state of variable2.
	 * @param var2Index
	 *            - the index to which corresponds variable2 in the variables
	 *            list.
	 * @return an List containing the generated state combinations.
	 *//*
	private static List<int[]> getStateCombinations(
			Map<Integer, Integer> independentVariables,
			List<Variable> variables, int var1Value, int var1Index,
			int var2Value, int var2Index) {
	    List<int[]> combinationList = new ArrayList<int[]>();
		LinkedList<Integer> leftLifo = new LinkedList<Integer>();
		LinkedList<Integer> rightLifo = new LinkedList<Integer>();

		int[] combination = new int[independentVariables.size() + 2];
		combination[var1Index] = var1Value;
		combination[var2Index] = var2Value;
		combinationList.add(combination.clone());

		Iterator<Integer> it = independentVariables.keySet().iterator();
		while (it.hasNext()) {
			Integer varIndex = (Integer) it.next();
			leftLifo.addLast(varIndex);
		}

		while (!leftLifo.isEmpty()) {
			Integer variableIndex = (Integer) leftLifo.removeLast();
			//generateCombination(rightLifo, variableIndex, independentVariables,
			//		combination, combinationList);
			generateCombinationNew(independentVariables,
							combination, combinationList);
		}
		return combinationList;
	}*/

	/*****
	 * Generates the state combinations for a set of variables having variable 1
	 * and variable 2 a given value.
	 *
	 * @param independentVariables
	 *            - Set of independent variables
	 * @param variables
	 *            - List containing the variables
	 * @param var1Value
	 *            - the value of the state of variable1
	 * @param var1Index
	 *            - the index to which corresponds variable1 in the variables
	 *            list.
	 * @param var2Value
	 *            - the value of the state of variable2.
	 * @param var2Index
	 *            - the index to which corresponds variable2 in the variables
	 *            list.
	 * @return an List containing the generated state combinations.
	 */
	private static List<int[]> getStateCombinations(Map<Integer, Integer> independentVariables,
			List<Variable> variables, int var1Value, int var1Index, int var2Value, int var2Index) {
		List<int[]> combinationList = new ArrayList<>();

		int[] combination = new int[independentVariables.size() + 2];
		combination[var1Index] = var1Value;
		combination[var2Index] = var2Value;
		// We add the combination of zeros to this pair of values
		combinationList.add(combination.clone());

		// We generate the combinations for the two main variables (var1 and var2)
		generateCombinationNew(independentVariables, combination, combinationList);
		return combinationList;
	}
	
    /*
	/*******
	 * Generates a new state combination for the variable corresponding to the
	 * variableIndex inside the independent variable list.
	 * 
	 * @param rightLifo
	 *            - the queue containing the variables to update at the right
	 *            (higher position at the independent variable list).
	 * @param variableIndex
	 *            - the index of the current variable of the independent
	 *            variable list.
	 * @param independentVariables
	 *            - set of independent variables
	 * @param currentCombination
	 *            - the current combination of variables
	 * @param combinationsList
	 *            - the list of combinations
	 *//*
	private static void generateCombination(LinkedList<Integer> rightLifo,
			int variableIndex, Map<Integer, Integer> independentVariables,
			int[] currentCombination, List<int[]> combinationsList) {
		int numStates = (Integer) independentVariables.get(variableIndex);
		for (int i = 1; i < numStates; i++) {
			int[] newCombination = currentCombination.clone();
			newCombination[variableIndex] = i;
			combinationsList.add(newCombination.clone());
			if (!rightLifo.isEmpty()) {
				int nextIndex = (Integer) rightLifo.removeLast();
				generateCombination(rightLifo, nextIndex, independentVariables,
						newCombination, combinationsList);
			}
		}
		rightLifo.addLast(variableIndex);
	}*/

	/**
	 * Generates all the possible states of the independents variables
	 * (the current combination sets the immutable states defined by the nodes with the link restriction.
	 *
	 * @param independentVariables Variables for that we need to generate combinations
	 * @param currentCombination   Predefined combination with the values of the nodes with the link restriction
	 * @param combinationsList     List of all posibles combinations
	 */
	private static void generateCombinationNew(Map<Integer, Integer> independentVariables, int[] currentCombination,
			List<int[]> combinationsList) {

		// Calculate the number of possible combinations of the current combination
		int numCombinations = 1;
		int[] independentVariablesKeys = new int[independentVariables.size()];
		int temporalIndex = 0;
		for (Integer key : independentVariables.keySet()) {
			numCombinations = numCombinations * independentVariables.get(key);
			independentVariablesKeys[temporalIndex] = key;
			temporalIndex++;
		}

		// Constructs the combinations repetitions array. 
		//This array sets the number of occurrences of the same value for each state (to do the combinatory task)
		int temporal = numCombinations;
		int[] combinationsRepetitions = new int[independentVariables.size()];
		for (int i = 0; i < independentVariables.size(); i++) {
			combinationsRepetitions[i] = temporal / independentVariables.get(independentVariablesKeys[i]);
			temporal = combinationsRepetitions[i];
		}

		// We start at the second combination because the first combination is formed with zeros 
		// (and it was inserted before the call to this method)
		int[] newCombination;
		for (int i = 1; i < numCombinations; i++) {
			newCombination = currentCombination.clone();
			// For each combination we set the states of each independent variable
			int coordinate = 0;
			int temporalRest = i;
			for (Integer key : independentVariables.keySet()) {
				newCombination[key] = temporalRest / combinationsRepetitions[coordinate];
				temporalRest = temporalRest % combinationsRepetitions[coordinate];
				coordinate++;
			}
			combinationsList.add(newCombination);
		}

	}
}
