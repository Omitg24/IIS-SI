/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.model.network;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.openmarkov.core.exception.IncompatibleEvidenceException;
import org.openmarkov.core.exception.InvalidStateException;
import org.openmarkov.core.exception.NoFindingException;
import org.openmarkov.core.exception.NodeNotFoundException;
import org.openmarkov.core.exception.NonProjectablePotentialException;
import org.openmarkov.core.exception.NotEvaluableNetworkException;
import org.openmarkov.core.exception.WrongCriterionException;
import org.openmarkov.core.inference.InferenceOptions;
import org.openmarkov.core.inference.PartialOrderDAN;
import org.openmarkov.core.model.graph.Link;
import org.openmarkov.core.model.network.modelUncertainty.UncertainValue;
import org.openmarkov.core.model.network.potential.Potential;
import org.openmarkov.core.model.network.potential.TablePotential;

/**
 * This class performs prune on {@code ProbNet}
 *
 * @author marias
 */
public class ProbNetOperations {

	// Methods

	/**
	 * Performs prune operation in these steps:
	 *
	 * <ol>
	 * <li>Copy the received {@code ProbNet}.
	 * <li>Remove barren nodes from the copied {@code ProbNet}.
	 * <li>Remove unreachable nodes from {@code variablesOfInterest} given
	 * the {@code variablesOfEvidence}.
	 * </ol>
	 *
	 * @param probNet Network
	 * @param evidence Evidence
	 * @param variablesOfInterest Collection of the variables of interest
	 * @return {@code ProbNet}. Evidence variables are removed in serial
	 * connections
	 */
	public static ProbNet getPruned(ProbNet probNet, Collection<Variable> variablesOfInterest, EvidenceCase evidence) {
		ProbNet prunedProbNet = probNet.copy();
		HashSet<Variable> variablesOfInterest2 = new HashSet<>(variablesOfInterest);
		HashSet<Variable> variablesOfEvidence2 = new HashSet<>(evidence.getVariables());
		prunedProbNet = removeBarrenNodes(prunedProbNet, variablesOfInterest2, variablesOfEvidence2);
		prunedProbNet = removeUnreachableNodes(prunedProbNet, variablesOfInterest2, variablesOfEvidence2);
		return prunedProbNet;
	}

	/**
	 * Projects the evidence in the {@code probNet} potentials and remove
	 * evidence variables
	 *
	 * @param probNet  . {@code ProbNet}
	 * @param evidence . {@code EvidenceCase}
	 */
	public static void projectEvidence(ProbNet probNet, EvidenceCase evidence) {
		List<Variable> variables = evidence.getVariables();
		for (Variable variable : variables) {
			List<Potential> potentials = probNet.getPotentials(variable);
			for (Potential potential : potentials) {
				probNet.removePotential(potential);
				try {
					for (Potential newPotential : potential.tableProject(evidence, null)) {
						if (newPotential.getNumVariables() > 0) {
							boolean containVariables = true;
							for (Variable potentialVariable : newPotential.getVariables()) {
								containVariables &= (probNet.getNode(potentialVariable) != null);
							}
							if (containVariables) {
								probNet.addPotential(newPotential);
							}
						}
					}
				} catch (NonProjectablePotentialException e) {
					e.printStackTrace(); // Unreachable code
				} catch (WrongCriterionException e) {
					e.printStackTrace(); // Unreachable code
				}
			}
			probNet.removeNode(probNet.getNode(variable));
		}
	}

	/**
	 * @param probNet Network
	 * @param node  {@code Node}
	 * @param nodes {@code Collection} of {@code Node}
	 * @return {@code true} if {@code node} has at least a neighbor
	 * other than those in {@code nodeList}
	 */
	public static boolean hasNeighborsOutside(ProbNet probNet, Node node, Collection<Node> nodes) {
		boolean hasNeighborsOutside = false;
		boolean neighborIsInList; // aux for the for-loop
		for (Node neighbor : probNet.getNeighbors(node)) {
			neighborIsInList = false;
			for (Node cliqueNode : nodes) {
				if (neighbor == cliqueNode) {
					neighborIsInList = true;
					break;
				}
			}
			if (!neighborIsInList) {
				hasNeighborsOutside = true;
				break;
			}
		}
		return hasNeighborsOutside;
	}

	/**
	 * Remove nodes that:
	 * <ol>
	 * <li>Are not included in {@code variablesOfInterest}
	 * <li>Are not included in {@code variablesOfEvidence}
	 * <li>Have no children or all its children are barren nodes.
	 * </ol>
	 *
	 * @param variablesOfEvidence Variables of evidence
	 * @param variablesOfInterest Variables of interest
	 * @param prunedProbNet       . {@code ProbNet}
	 * @return {@code ProbNet} without barren nodes.
	 */
	public static ProbNet removeBarrenNodes(ProbNet prunedProbNet, Collection<Variable> variablesOfInterest,
			HashSet<Variable> variablesOfEvidence) {
		HashSet<Node> barrenNodes = new HashSet<>();
		List<Node> nodes = prunedProbNet.getNodes();
		for (Node node : nodes) {
			if (node.getNumChildren() == 0) {
				Variable variable = node.getVariable();
				if (!variablesOfInterest.contains(variable) && !variablesOfEvidence.contains(variable)) {
					barrenNodes.add(node);
				}
			}
		}
		HashSet<Node> newBarrenNodes = new HashSet<>(barrenNodes);
		boolean foundBarrenNodes = newBarrenNodes.size() > 0;
		while (foundBarrenNodes) {
			foundBarrenNodes = false;
			List<Node> listNewBarrenNodes = new ArrayList<>(newBarrenNodes);
			for (Node node : listNewBarrenNodes) {
				newBarrenNodes.remove(node);
				List<Node> parents = node.getParents();
				for (Node parent : parents) {
					Variable parentVariable = parent.getVariable();
					if (!variablesOfInterest.contains(parentVariable) && !variablesOfEvidence.contains(parentVariable)
							&& !barrenNodes.contains(parent)) {
						List<Node> childrenOfParent = parent.getChildren();
						boolean allChildrenBarren = true;
						int numChildren = childrenOfParent.size();
						if (numChildren > 1) { // at least one children is
							// barren
							for (int i = 0; allChildrenBarren && i < numChildren; i++) {
								Node child = childrenOfParent.get(i);
								allChildrenBarren &= barrenNodes.contains(child);
							}
						}
						if (allChildrenBarren) {
							newBarrenNodes.add(parent);
							foundBarrenNodes = true;
						}
					}
				}
			}
			if (foundBarrenNodes) {
				barrenNodes.addAll(newBarrenNodes);
			}
		}
		// Remove barren nodes
		for (Node node : barrenNodes) {
			prunedProbNet.removeNode(node);
		}
		return prunedProbNet;
	}

	/**
	 * Removes the nodes that are not connected to the variables of interest by
	 * any path
	 *
	 * @param probNet             . {@code ProbNet}
	 * @param variablesOfInterest . {@code Collection} of {@code Variable}
	 * @param variablesOfEvidence . {@code HashSet} of {@code Variable}
	 * @return {@code ProbNet}
	 */
	public static ProbNet removeUnreachableNodes(ProbNet probNet, Collection<Variable> variablesOfInterest,
			HashSet<Variable> variablesOfEvidence) {
		// Gets nodes of interest and adds nodes connected to them
		UniqueStack<Node> nodesToExplore = new UniqueStack<>();
		Set<Node> nodesToKeep = new HashSet<>();

		// Store nodes of variablesOfInterest in nodesToKeep
		for (Variable variable : variablesOfInterest) {
			nodesToKeep.add(probNet.getNode(variable));
		}

		// Add neighbors of variablesOfInterest as nodesToKeep and store them in
		// nodesToExplore
		HashSet<Node> nodesToKeepClon = new HashSet<>(nodesToKeep);
		for (Node node : nodesToKeepClon) {
			List<Node> neighbors = node.getNeighbors();
			for (Node neighbor : neighbors) {
				if (!nodesToKeep.contains(neighbor)) {
					nodesToKeep.add(neighbor);
					nodesToExplore.push(neighbor);
				}
			}
		}

		// Store evidence nodes and nodes in collections
		Set<Node> hashEvidenceNodes = getEvidenceNodes(probNet, variablesOfEvidence);
		Set<Node> evidenceAndAncestors = getNodesAndAncestors(hashEvidenceNodes);

		// For each interest node, finds connected nodes via valid paths.
		while (!nodesToExplore.empty()) {
			Node node = nodesToExplore.pop();

			// Find head to head connected nodes: X->Y<-Z and
			// Y is evidence or Y has a descendent that is evidence
			if (evidenceAndAncestors.contains(node)) {
				List<Node> parents = node.getParents();
				int parentsSize = parents.size();
				for (int i = 0; i < parentsSize - 1; i++) {
					Node parentI = parents.get(i);
					boolean toKeepI = nodesToKeep.contains(parentI);
					for (int j = i + 1; j < parentsSize; j++) {
						Node parentJ = parents.get(j);
						boolean toKeepJ = nodesToKeep.contains(parentJ);
						if (toKeepI && !toKeepJ) {
							pushInExploreAndAddToKeep(parentJ, nodesToExplore, nodesToKeep);
						} else if (!toKeepI && toKeepJ) {
							pushInExploreAndAddToKeep(parentI, nodesToExplore, nodesToKeep);
							toKeepI = true;
						}
					}
				}
			}
			// X has a children Y that is part of the evidence
			List<Node> xChildren = node.getChildren();
			for (Node child : xChildren) {
				if (evidenceAndAncestors.contains(child)) {
					pushInExploreAndAddToKeep(child, nodesToExplore, nodesToKeep);
				}
			}

			// Find not head to head connected nodes:
			// X->Y->Z, X<-Y<-Z and X<-Y->Z
			if (!hashEvidenceNodes.contains(node)) {
				List<Node> children = node.getChildren();
				List<Node> parents = node.getParents();
				int numChildren = children.size();
				for (int i = 0; i < numChildren; i++) {
					Node child = children.get(i);
					boolean childInNodesToKeep = nodesToKeep.contains(child);
					// X->Y->Z and X<-Y<-Z
					for (Node parent : parents) {
						boolean parentInNodesToKeep = nodesToKeep.contains(parent);
						if (childInNodesToKeep && !parentInNodesToKeep) {
							pushInExploreAndAddToKeep(parent, nodesToExplore, nodesToKeep);
							parentInNodesToKeep = true;
						} else if (parentInNodesToKeep && !childInNodesToKeep) {
							pushInExploreAndAddToKeep(child, nodesToExplore, nodesToKeep);
							childInNodesToKeep = true;
						}
					}
					// X<-Y->Z
					for (int j = i + 1; j < numChildren; j++) {
						Node child2 = children.get(j);
						boolean child2InNodesToKeep = nodesToKeep.contains(child2);
						if (child2InNodesToKeep && !childInNodesToKeep) {
							pushInExploreAndAddToKeep(child, nodesToExplore, nodesToKeep);
							childInNodesToKeep = true;
						} else if (childInNodesToKeep && !child2InNodesToKeep) {
							pushInExploreAndAddToKeep(child2, nodesToExplore, nodesToKeep);
						}
					}
				}
			}
		}

		// remove nodes that are not in nodesToKeep in prunedProbNet
		List<Node> prunedNodes = probNet.getNodes();
		for (Node node : prunedNodes) {
			if (!nodesToKeep.contains(node)) {
				probNet.removeNode(node);
			}
		}

		return probNet;
	}

	/**
	 * @param node           . {@code Node}
	 * @param nodesToExplore . {@code UniqueStack} of {@code Node}
	 * @param nodesToKeep    . {@code HashSet} of {@code Node}
	 */
	private static void pushInExploreAndAddToKeep(Node node, UniqueStack<Node> nodesToExplore, Set<Node> nodesToKeep) {
		nodesToExplore.push(node);
		nodesToKeep.add(node);
	}

	/**
	 * @param probNet             . {@code ProbNet}
	 * @param variablesOfEvidence . {@code Collection} of {@code Variable}
	 * @return {@code HashSet} of {@code Node}
	 */
	private static Set<Node> getEvidenceNodes(ProbNet probNet, Collection<Variable> variablesOfEvidence) {
		Set<Node> hashEvidenceNodes = new HashSet<>();
		for (Variable variable : variablesOfEvidence) {
			Node evidenceNode = probNet.getNode(variable);
			if (evidenceNode != null) {
				hashEvidenceNodes.add(evidenceNode);
			}
		}
		return hashEvidenceNodes;
	}

	/**
	 * @param nodes . {@code ArrayList} of {@code Node}.
	 * @return {@code nodes} and its ancestors. {@code ArrayList} of
	 * {@code Node}.
	 */
	private static Set<Node> getNodesAndAncestors(Collection<Node> nodes) {
		Set<Node> ancestors = new HashSet<>(nodes);

		Stack<Node> noExploredNodes = new Stack<>();
		noExploredNodes.addAll(nodes);

		while (!noExploredNodes.empty()) {
			Node node = noExploredNodes.pop();
			List<Node> parents = node.getParents();
			for (Node parent : parents) {
				if (ancestors.add(parent)) {
					noExploredNodes.push(parent);
				}
			}
		}
		return ancestors;
	}

	/**
	 * Uses the algorithm by Kahn (1962)
	 *
	 * @param probNet Network
	 * @param variablesToSort Variables to sort	 *
	 * @return List of variables sorted topologically
	 */
	public static List<Variable> sortTopologically(ProbNet probNet, List<Variable> variablesToSort) {
		List<Node> sortedNodes = sortTopologically(probNet);
		List<Variable> sortedVariables = new ArrayList<>(sortedNodes.size());
		for (Node node : sortedNodes) {
			if (variablesToSort.contains(node.getVariable())) {
				sortedVariables.add(node.getVariable());
			}
		}
		return sortedVariables;
	}

	/**
	 * Uses the algorithm by Kahn (1962)
	 *
	 * @param probNet Network
	 * @return List of variables sorted topologically
	 */
	public static List<Node> sortTopologically(ProbNet probNet) {
		ProbNet graph = probNet.copy();

		// Empty list that will contain the sorted elements
		Stack<Node> stackOrderedNodes = new Stack<>();
		// Set of all nodes with no incoming edges
		List<Node> noEdgesListOfNodes = new ArrayList<>();
		// Look for variables/nodes with no parents
		for (Node node : graph.getNodes()) {
			if (node.getParents().size() == 0) {
				stackOrderedNodes.push(node);
			}
		}
		while (!stackOrderedNodes.isEmpty()) {
			// remove a node from stack
			Node nodeOrdered = stackOrderedNodes.pop();
			// insert int into no edges list of nodes
			noEdgesListOfNodes.add(nodeOrdered);
			// for each node  with an edge e from n to m do
			for (Node childOfOrdered : nodeOrdered.getChildren()) {
				// remove edges from childOfOrdered to its children
				graph.removeLink(nodeOrdered, childOfOrdered, true);
				// if the node has no other incoming edges then insert it into the stack of ordered nodes
				if (childOfOrdered.getParents().isEmpty()) {
					stackOrderedNodes.push(childOfOrdered);
				}
			}
		}

		List<Node> sortedNodes = new ArrayList<>();
		for (Node node : noEdgesListOfNodes)
			sortedNodes.add(probNet.getNode(node.getVariable()));
		return sortedNodes;
	}

	/**
	 * Converts numerical variables with neither evidence nor induced findings
	 * (such as a delta potential) that are deterministically defined by their
	 * parents into finite state variables. It also adapts the potentials
	 * affected by these conversions.
	 *
	 * @param probNet Network
	 * @param evidence Evidence
	 * @return Network with numerical variables transformed into FS
	 */
	public static ProbNet convertNumericalVariablesToFS(ProbNet probNet, EvidenceCase evidence) {
		ProbNet convertedNet = probNet.copy();
		List<Node> sortedNodes = sortTopologically(convertedNet);
		List<Node> convertedNodes = new ArrayList<>();
		Map<Variable, Variable> originalVariables = new LinkedHashMap<>();
		Map<Variable, Variable> convertedVariables = new LinkedHashMap<>();

		for (Node node : sortedNodes) {
			Variable oldVariable = node.getVariable();
			// Should the node be converted
			if (oldVariable.getVariableType() == VariableType.NUMERIC && node.getNodeType() == NodeType.CHANCE) {
				EvidenceCase configuration = new EvidenceCase(evidence);
				Potential oldPotential = node.getPotentials().get(0);
				if (configuration.contains(oldVariable)) {
					// Convert numerical variables with evidence to one-state
					// variables
					Finding finding;
					try {
						finding = configuration.removeFinding(oldVariable);
						double value = finding.numericalValue;
						Variable newVariable = new Variable(oldVariable.getName(), String.valueOf(value));
						node.setVariable(newVariable);
						originalVariables.put(newVariable, oldVariable);
						convertedVariables.put(oldVariable, newVariable);
						convertedNodes.add(node);
						TablePotential potential = new TablePotential(Arrays.asList(newVariable),
								oldPotential.getPotentialRole());
						potential.values[0] = 1;
						node.setPotential(potential);
					} catch (NoFindingException e) {
						e.printStackTrace();
					}
				} else {
					List<Double> newStates = new ArrayList<>();
					// For each configuration x, add f(x) to the list (if it is
					// not already in)
					List<Node> parents = node.getParents();
					// Set initial configuration
					int numConfigurations = 1;
					int[] parentIndices = new int[parents.size()];
					for (int i = 0; i < parents.size(); ++i) {
						Variable parentVariable = parents.get(i).getVariable();
						numConfigurations *= parentVariable.getNumStates();
						parentIndices[i] = 0;
						try {
							if (originalVariables.containsKey(parentVariable)) {
								Variable originalVariable = originalVariables.get(parentVariable);
								double numericalValue = Double.valueOf(parentVariable.getStates()[0].getName());
								configuration.addFinding(new Finding(originalVariable, numericalValue));
							} else if (parentVariable.getVariableType() == VariableType.FINITE_STATES) {
								configuration.addFinding(new Finding(parentVariable, 0));
							} else {
								// TODO throw some exception
							}
						} catch (InvalidStateException | IncompatibleEvidenceException e) {
							e.printStackTrace();
						}
					}
					boolean nextConfiguration = true;
					double[] projectedValues = new double[numConfigurations];
					int index = 0;
					int parentIndex = 0;
					InferenceOptions inferenceOptions = new InferenceOptions(convertedNet, null);
					while (nextConfiguration) {

						// Calculate scalar value projecting configuration
						double scalarValue = Double.NEGATIVE_INFINITY;
						try {
							scalarValue = oldPotential.tableProject(configuration, inferenceOptions).get(0).values[0];
							scalarValue = oldVariable.round(scalarValue);
							projectedValues[index++] = scalarValue;
						} catch (NonProjectablePotentialException | WrongCriterionException e) {
							e.printStackTrace();
						}
						if (!newStates.contains(scalarValue)) {
							newStates.add(scalarValue);
						}

						// Get next configuration
						nextConfiguration = false;
						parentIndex = 0;
						while (!nextConfiguration && parentIndex < parents.size()) {
							Node parent = parents.get(parentIndex);
							Variable parentVariable = parent.getVariable();
							Variable findingVariable = (originalVariables.containsKey(parent.getVariable())) ?
									originalVariables.get(parent.getVariable()) :
									parent.getVariable();
							int nextStateIndex = ++parentIndices[parentIndex];
							if (nextStateIndex < parent.getVariable().getNumStates()) {
								nextConfiguration = true;
								try {
									if (originalVariables.containsKey(parentVariable)) {
										Variable originalVariable = originalVariables.get(parentVariable);
										double numericalValue = Double
												.valueOf(parentVariable.getStates()[nextStateIndex].getName());
										configuration.changeFinding(new Finding(originalVariable, numericalValue));
									} else if (parentVariable.getVariableType() == VariableType.FINITE_STATES) {
										configuration.changeFinding(new Finding(findingVariable, nextStateIndex));
									}
								} catch (InvalidStateException | IncompatibleEvidenceException e) {
									e.printStackTrace();
								}
							} else {
								parentIndices[parentIndex] = 0;
								parentIndex++;
							}
						}
					}

					Collections.sort(newStates);
					State[] states = new State[newStates.size()];
					Map<Double, Integer> stateIndices = new HashMap<>();
					DecimalFormat df = new DecimalFormat("#.#####", new DecimalFormatSymbols(Locale.US));
					for (int i = 0; i < newStates.size(); ++i) {
						states[i] = new State(df.format(newStates.get(i)));
						stateIndices.put(newStates.get(i), i);
					}

					Variable newVariable = new Variable(oldVariable.getName(), states);
					node.setVariable(newVariable);
					originalVariables.put(newVariable, oldVariable);
					convertedVariables.put(oldVariable, newVariable);
					convertedNodes.add(node);

					List<Variable> newPotentialVariables = new ArrayList<>();
					newPotentialVariables.add(newVariable);
					for (Node parent : parents) {
						newPotentialVariables.add(parent.getVariable());
					}

					TablePotential newPotential = new TablePotential(newPotentialVariables,
							oldPotential.getPotentialRole());
					double[] values = newPotential.values;
					int newVariableNumStates = newVariable.getNumStates();
					for (int i = 0; i < numConfigurations; i++) {
						int stateIndex = stateIndices.get(projectedValues[i]);
						for (int j = 0; j < newVariableNumStates; j++) {
							values[i * newVariableNumStates + j] = (stateIndex == j) ? 1 : 0;
						}
					}
					node.setPotential(newPotential);
				}
			} else if (!node.getPotentials().isEmpty() && doesPotentialContainAnyConvertedNode(node.getPotentials().get(0),
					convertedVariables.keySet())) {
				// Node is not numeric but contains numeric parents
				// Adapt potential to numeric finite states variables
				Potential newPotential = node.getPotentials().get(0).copy();
				List<Variable> convertedParentVariables = getConvertedParentVariables(newPotential, convertedVariables);
				for (Variable convertedParentVariable : convertedParentVariables) {
					newPotential.replaceNumericVariable(convertedParentVariable);
				}
				node.setPotential(newPotential);
			}
		}

		// Update evidence, replacing references to old variables with new ones
		List<Finding> findings = evidence.getFindings();
		for (Finding finding : findings) {
			Variable originalVariable = finding.getVariable();
			if (convertedVariables.containsKey(originalVariable)) {
				try {
					evidence.removeFinding(originalVariable);
					Variable convertedVariable = convertedVariables.get(originalVariable);
					double numericalValue = convertedVariable.round(finding.getNumericalValue());
					int stateIndex = convertedVariable.getStateIndex(String.valueOf(numericalValue));
					evidence.addFinding(new Finding(convertedVariable, stateIndex));
				} catch (NoFindingException | InvalidStateException | IncompatibleEvidenceException e) {
					e.printStackTrace();
				}
			}
		}

		return convertedNet;
	}

	public static ProbNet convertNumericalVariablesToFS(ProbNet probNet) throws NotEvaluableNetworkException {
		return convertNumericalVariablesToFS(probNet, new EvidenceCase());
	}

	/**
	 * @param potential Potential
	 * @param projectedPotential Projected potential
	 * @param configuration      - configuration of projected variables
	 */
	public static void sumProjectedPotential(TablePotential potential, TablePotential projectedPotential,
			EvidenceCase configuration) {
		List<Variable> variables = potential.getVariables();
		List<Variable> unprojectedVariables = projectedPotential.getVariables();
		int[] unprojectedVariablesIndices = new int[unprojectedVariables.size()];
		int[] potentialVariableIndices = new int[variables.size()];
		// Set indices for initial configuration
		for (int i = 0; i < potentialVariableIndices.length; ++i) {
			Variable variable = variables.get(i);
			if (configuration.contains(variable)) {
				potentialVariableIndices[i] = configuration.getState(variable);
			} else {
				unprojectedVariablesIndices[unprojectedVariables.indexOf(variable)] = i;
				potentialVariableIndices[i] = 0;
			}
		}
		// Add uncertain values if projected potential has them
		if (projectedPotential.isUncertain() && !potential.isUncertain()) {
			potential.uncertainValues = new UncertainValue[potential.getTableSize()];
		}

		Variable conditionedVariable = potential.getConditionedVariable();
		// Index of the current configuration in the projected potential
		int projectedConfigIndex = 0;
		// Index of the current configuration in the original potential
		int configIndex = 0;
		boolean nextConfiguration = true;
		while (nextConfiguration) {
			configIndex = potential.getPosition(potentialVariableIndices);
			// TODO update potentialVariableIndices
			for (int i = 0; i < conditionedVariable.getNumStates(); ++i) {
				potential.values[configIndex + i] = projectedPotential.values[projectedConfigIndex + i];
				if (projectedPotential.isUncertain()) {
					potential.uncertainValues[configIndex + i] = projectedPotential.uncertainValues[projectedConfigIndex
							+ i];
				}
			}
			// TODO update projectedConfigIndex
			projectedConfigIndex += conditionedVariable.getNumStates();

			// Get next configuration
			nextConfiguration = false;
			int unprojectedParentIndex = (conditionedVariable == variables.get(unprojectedVariablesIndices[0])) ? 1 : 0;
			while (!nextConfiguration && unprojectedParentIndex < unprojectedVariablesIndices.length) {
				int parentIndex = unprojectedVariablesIndices[unprojectedParentIndex];
				if (potentialVariableIndices[parentIndex] + 1 < variables.get(parentIndex).getNumStates()) {
					potentialVariableIndices[parentIndex]++;
					nextConfiguration = true;
				} else {
					potentialVariableIndices[parentIndex] = 0;
					unprojectedParentIndex++;
				}
			}
		}
	}

	public static void sumProjectedPotential(TablePotential potential, TablePotential projectedPotential,
			List<Variable> projectedVariables, int[] projectedIndices) {
		EvidenceCase configuration = new EvidenceCase();
		for (int i = 0; i < projectedVariables.size(); ++i) {
			try {
				configuration.addFinding(new Finding(projectedVariables.get(i), projectedIndices[i]));
			} catch (InvalidStateException | IncompatibleEvidenceException e) {
				e.printStackTrace();
			}
		}
		sumProjectedPotential(potential, projectedPotential, configuration);
	}

	@SuppressWarnings("unused") private static List<Variable> getConvertedPotentialVariables(Potential oldPotential,
			Map<Variable, Variable> convertedVariables) {
		List<Variable> originalVariables = oldPotential.getVariables();
		List<Variable> convertedPotentialVariables = new ArrayList<>(originalVariables.size());
		for (Variable originalVariable : originalVariables) {
			if (convertedVariables.containsKey(originalVariable)) {
				convertedPotentialVariables.add(convertedVariables.get(originalVariable));
			} else {
				convertedPotentialVariables.add(originalVariable);
			}
		}
		return convertedPotentialVariables;
	}

	private static List<Variable> getConvertedParentVariables(Potential potential,
			Map<Variable, Variable> convertedVariables) {
		List<Variable> convertedParentVariables = new ArrayList<>();
		for (Variable parentVariable : potential.getVariables()) {
			if (convertedVariables.containsKey(parentVariable)) {
				convertedParentVariables.add(convertedVariables.get(parentVariable));
			}
		}
		convertedParentVariables.remove(potential.getConditionedVariable());
		return convertedParentVariables;
	}

	private static boolean doesPotentialContainAnyConvertedNode(Potential potential, Set<Variable> convertedVariables) {
		return potential.getVariables().stream().anyMatch(x -> convertedVariables.contains(x));
	}

	public static List<State> getUnrestrictedStates(Link<Node> link, State[] restrictedVariableStates, State state) {
		List<State> nonRestrictedStates = new ArrayList<>();
		Potential linkRestrictions = link.getRestrictionsPotential();
		List<Variable> variables = linkRestrictions.getVariables();
		Variable sourceVariable = variables.get(0);
		Variable destinationVariable = variables.get(1);
		EvidenceCase configuration = new EvidenceCase();
		try {
			configuration.addFinding(new Finding(sourceVariable, state));
			for (State restrictedVariableState : restrictedVariableStates) {
				configuration.changeFinding(new Finding(destinationVariable, restrictedVariableState));
				if (linkRestrictions.getProbability(configuration) > 0) {
					nonRestrictedStates.add(restrictedVariableState);
				}
			}
		} catch (InvalidStateException | IncompatibleEvidenceException e) {
			// Not going to happen
		}
		return nonRestrictedStates;
	}

	public static boolean hasStructuralAsymmetry(ProbNet probNet) {
		boolean asymmetryFound = false;

		for (Link<Node> link : probNet.getLinks()) {
			// There is asymmetry if there are total restrictions or if only some states reveal a certain variable
			asymmetryFound |= link.hasTotalRestriction() || (
					link.hasRestrictions() && link.getNode2().getNodeType() == NodeType.DECISION
			) || (
					link.hasRevealingConditions() && link.getRevealingStates().size() < link.getNode1().getVariable()
							.getNumStates()
			);
		}

		return asymmetryFound;
	}

	/**
	 * Returns if a ProbNet has order asymmetry. A ProbNet has order asymmetry
	 * if and only if there is no directed path that goes through all the
	 * decision nodes
	 *
	 * @param probNet Network
	 * @return True if the network has order asymmetry
	 */
	public static boolean hasOrderAsymmetry(ProbNet probNet) {
		return hasOrderAsymmetry(probNet, null);
	}

	public static boolean hasOrderAsymmetry(ProbNet probNet, List<Variable> evidentialNodes) {
		List<Node> parentlessDecisions = getParentlessDecisions(probNet);
		if (parentlessDecisions.size() == 1) {
			List<Node> decisionNodes = probNet.getNodes(NodeType.DECISION);
			while (parentlessDecisions.size() == 1) {
				decisionNodes.remove(parentlessDecisions.get(0));
				parentlessDecisions.clear();
				for (Node decisionNode : decisionNodes) {
					boolean hasParentDecisions = false;
					Stack<Node> parentNodes = new Stack<>();
					parentNodes.push(decisionNode);
					while (!hasParentDecisions && !parentNodes.isEmpty()) {
						Node node = parentNodes.pop();
						List<Node> parents = node.getParents();
						int i = 0;
						while (i < parents.size() && !hasParentDecisions) {
							Node parentNode = parents.get(i++);
							boolean isDecision = parentNode.getNodeType() == NodeType.DECISION;
							if (!isDecision || decisionNodes.contains(parentNode)) {
								hasParentDecisions |= isDecision;
								parentNodes.push(parentNode);
							}
						}
					}
					if (!hasParentDecisions) {
						parentlessDecisions.add(decisionNode);
					}
				}
			}
		}
		if (evidentialNodes != null) {
			for (Variable variable : evidentialNodes) {
				parentlessDecisions.remove(probNet.getNode(variable));
			}
		}

		return parentlessDecisions.size() > 1;
	}

	public static List<Node> getNeverObservedVariables(ProbNet probNet) {
		List<Node> neverObservedVariables = new ArrayList<>();
		Set<Node> observableVariables = null;

		try {
			observableVariables = getObservableVariables(probNet);
		} catch (NodeNotFoundException e) {
			e.printStackTrace();
		}

		for (Node node : probNet.getNodes(NodeType.CHANCE)) {
			if (!observableVariables.contains(node)) {
				neverObservedVariables.add(node);
			}
		}
		return neverObservedVariables;
	}

	/**
	 * @param probNet A DAN
	 * @return A list of chance variables that are observable; this list includes always observed variables and
	 * those variables that can be reached from an always observed variable or from a decision, always following
	 * a path formed exclusively by revelation links.
	 * @throws NodeNotFoundException NodeNotFoundException
	 */
	public static Set<Node> getObservableVariables(ProbNet probNet)
			throws NodeNotFoundException {
		Set<Node> observable;
		Set<Variable> visitedDecisions;
		ConcurrentLinkedQueue<Variable> variablesToProcess = new ConcurrentLinkedQueue<>();

		observable = new HashSet<>();
		observable.addAll(getAlwaysObservedVariables(probNet));
		for (Node auxNode : observable) {
			variablesToProcess.add(auxNode.getVariable());
		}
		PartialOrderDAN order = new PartialOrderDAN(probNet);
		for (Node auxNode : getParentlessDecisions(probNet)) {
			variablesToProcess.add(auxNode.getVariable());
		}
		visitedDecisions = new HashSet<>();
		while (!variablesToProcess.isEmpty()) {
			Variable variableToProcess = variablesToProcess.poll();
			Node nodeToProcess = probNet.getNode(variableToProcess);

			//Process children in the graph
			for (Node child : nodeToProcess.getChildren()) {
				if (!observable.contains(child)) {
					boolean isFound = false;
					List<Link<Node>> links = probNet.getLinks();
					Link<Node> link = null;
					for (int i = 0; (i < links.size()) && !isFound; i++) {
						link = links.get(i);
						isFound = link.getNode1().equals(nodeToProcess) && link.getNode2() == child;
					}
					if (link.hasRevealingConditions()) {
						observable.add(child);
						variablesToProcess.add(child.getVariable());
					}
				}
			}
			if (nodeToProcess.getNodeType() == NodeType.DECISION) {
				visitedDecisions.add(variableToProcess);
				//Process the children of the decision in the partial order that we have not still visited
				for (Node childNodeInOrder : order.getOrder().getNode(variableToProcess).getChildren()) {
					Variable varChild = childNodeInOrder.getVariable();
					if (!visitedDecisions.contains(varChild)) {
						variablesToProcess.add(varChild);
					}
				}
			}

		}
		return observable;

	}

	/**
	 * Generates a list of decision nodes that don't have parent decisions
	 *
	 * @param probNet Network
	 * @return Parentless decisions
	 */
	public static List<Node> getParentlessDecisions(ProbNet probNet) {
		List<Node> parentlessDecisions = new ArrayList<>();
		for (Node parent : probNet.getNodes(NodeType.DECISION)) {
			boolean hasParentDecisions = false;
			Stack<Node> parentNodes = new Stack<>();
			parentNodes.push(parent);
			while (!hasParentDecisions && !parentNodes.isEmpty()) {
				Node node = parentNodes.pop();
				List<Node> parents = node.getParents();
				int i = 0;
				while (i < parents.size() && !hasParentDecisions) {
					Node parentNode = (Node) parents.get(i++);
					hasParentDecisions |= parentNode.getNodeType() == NodeType.DECISION;
					parentNodes.push(parentNode);
				}
			}
			if (!hasParentDecisions) {
				parentlessDecisions.add(parent);
			}
		}
		return parentlessDecisions;
	}

	/**
	 * Gets the list of always-observed-variables in the DAN
	 *
	 * @param probNet Network
	 * @return list of always-observed-variables
	 */
	public static List<Node> getAlwaysObservedVariables(ProbNet probNet) {
		List<Node> alwaysObservedVariables = new ArrayList<>();
		for (Node node : probNet.getNodes()) {
			if (node.isAlwaysObserved()) {
				alwaysObservedVariables.add(node);
			}
		}
		return alwaysObservedVariables;
	}

	/**
	 * Returns whether the node has a predecessor decision
	 *
	 * @param node Node
	 * @param probNet Network
	 * @return True if the node has a predecessor decision
	 */
	public static boolean hasPredecessorDecision(Node node, ProbNet probNet) {
		Stack<Node> predecessors = new Stack<>();
		predecessors.add(node);
		boolean found = false;
		while (!found && !predecessors.isEmpty()) {
			Node predecessor = predecessors.pop();
			found = !predecessor.equals(node) && predecessor.getNodeType() == NodeType.DECISION;
			for (Node parent : predecessor.getParents()) {
				predecessors.push(parent);
			}
		}
		return found;
	}

	/**
	 * Returns the list of predecessor decisions of node decisionNode
	 *
	 * @param node Node
	 * @param probNet Network
	 * @return Predecessors of the decision node
	 */
	public static List<Node> getPredecessorDecisions(Node node, ProbNet probNet) {
		List<Node> predecessorDecisions = new ArrayList<>();
		Stack<Node> predecessors = new Stack<>();
		// push first the parents of node
		for (Node parent : node.getParents()) {
			predecessors.push(parent);
		}
		// loop until we have processed all predecessors
		while (!predecessors.isEmpty()) {
			Node predecessor = predecessors.pop();
			if (predecessor.getNodeType() == NodeType.DECISION) {
				predecessorDecisions.add(predecessor);
			} else {
				for (Node parent : predecessor.getParents()) {
					predecessors.push(parent);
				}
			}
		}
		return predecessorDecisions;
	}

	public static List<Node> getDecisionSequence(ProbNet probNet) {
		List<Node> decisionSequence = new ArrayList<>();
		ProbNet probNetCopy = probNet.copy();
		List<Node> orphanDecisionNodes = getParentlessDecisions(probNetCopy);
		while (orphanDecisionNodes.size() == 1) {
			Node decision = orphanDecisionNodes.iterator().next();
			decisionSequence.add(decision);
			probNetCopy.removeNode(decision);
			orphanDecisionNodes = getParentlessDecisions(probNetCopy);
		}
		return decisionSequence;
	}

	/**
	 * Method to add non-forgetting arcs.
	 * The assumption of "no forgetting" is made explicit by arcs
	 * from predecessors of decision nodes
	 * @param probNet Network
	 */
	public static void addNoForgettingArcs(ProbNet probNet) {
		// First, we retrieve the decision variables from the network
		List<Variable> decisionVariables = probNet.getVariables(NodeType.DECISION);
		// We need some auxiliary variables
		List<Variable> orderedVariables;
		Variable upperVariable;
		Node upperNode;
		List<Variable> lowerVariables;
		Node lowerNode;
		List<Node> nodesParentsUpperVariable;
		// It's only necessary to add "no forgetting arcs" when there is more than one decision node
		if (decisionVariables.size() > 1) {
			// If so, we order the decision variables
			orderedVariables = ProbNetOperations.sortTopologically(probNet, decisionVariables);
			// There is no need to treat the last decision, as it does not have decisions after it
			for (int i = 0; i < orderedVariables.size() - 1; i++) {
				// The "upper" variable is the one that is before in the order
				upperVariable = orderedVariables.get(i);
				// We retrieve also its node
				upperNode = probNet.getNode(upperVariable);
				// and its parents
				nodesParentsUpperVariable = probNet.getNode(upperVariable).getParents();
				// And then, the "lower" variables, those that came after the "upperNode", one are calculated
				lowerVariables = new ArrayList<>();
				for (int j = i + 1; j < orderedVariables.size(); j++) {
					lowerVariables.add(orderedVariables.get(j));
				}
				// All the parents of the upper decision have to be parents of the lower decisions
				// Therefore, we iterate "lower" variables
				for (Variable lowerVariable : lowerVariables) {
					// retrieve the corresponding node of the variable that is being treated
					lowerNode = probNet.getNode(lowerVariable);
					// and for each one of the fathers of the upper variable
					for (Node nodeParentsHigherVariable : nodesParentsUpperVariable) {
						// if the parent node is not already parent of the lower variable
						if (!lowerNode.isParent(nodeParentsHigherVariable)) {
							// a new link is created
							probNet.addLink(nodeParentsHigherVariable, lowerNode, true);
						}
					}
					// Should the upper node not be a father of the lower decision.
					if (!lowerNode.isParent(upperNode)) {
						// a new link is created for the former has to be a father of the latter
						probNet.addLink(upperNode, lowerNode, true);
					}
				}
			}
		}
	}

	public static List<Variable> getInformationalPredecessors(ProbNet network, Variable variable) {
		List<Variable> informationalPredecessors = new ArrayList<>();
		Node decisionNode = network.getNode(variable);

		List<Node> predecessorDecisions = new ArrayList<>();
		for (Node candidateDecisionNode : network.getNodes(NodeType.DECISION)) {
			if (network.existsPath(candidateDecisionNode, decisionNode, true)) {
				predecessorDecisions.add(candidateDecisionNode);
			}
		}
		informationalPredecessors.addAll(ProbNet.getVariables(predecessorDecisions));

		for (Node candidateNode : network.getNodes(NodeType.CHANCE)) {
			boolean isInformationalPredecessor = decisionNode.isParent(candidateNode);
			int i = 0;
			while (i < predecessorDecisions.size() && !isInformationalPredecessor) {
				isInformationalPredecessor = predecessorDecisions.get(i).isParent(candidateNode);
				++i;
			}
			if (isInformationalPredecessor) {
				informationalPredecessors.add(candidateNode.getVariable());
			}
		}
		return informationalPredecessors;
	}

	/**
	 * @param node {@code Node}.
	 * @return {@code node} and its ancestors. {@code Set} of
	 * {@code Node}.
	 */
	public static Set<Node> getNodeAncestors(Node node) {
		Set<Node> ancestors = new HashSet<>();

		Stack<Node> noExploredNodes = new Stack<>();
		noExploredNodes.add(node);

		while (!noExploredNodes.empty()) {
			Node noExploredNode = noExploredNodes.pop();
			List<Node> parents = noExploredNode.getParents();
			for (Node parent : parents) {
				if (ancestors.add(parent)) {
					noExploredNodes.push(parent);
				}
			}
		}
		return ancestors;
	}

}
