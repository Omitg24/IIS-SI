/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.dt;

public class DecisionTreeEvaluator {

//	private static ProbNet instantiate(ProbNet probNet, Node node, State state, ProbNet originalProbNet) {
//		ProbNet instantiatedNet = probNet.copy();
//
//		for (Link<Node> link : probNet.getLinks(node)) {
//			if (link.getNode1().equals(node)) // Our node is the source node
//			{
//				Node destinationNode = instantiatedNet.getNode(link.getNode2().getVariable());
//
//				// Remove link between restricting node and restricted node
//				instantiatedNet.removeLink(link.getNode1().getVariable(), link.getNode2().getVariable(), true);
//
//				if (destinationNode.getNodeType() == NodeType.CHANCE) {
//					if (link.hasRevealingConditions()) {
//						if (link.getRevealingStates().contains(state)) {
//							List<Node> predecessorDecisions = ProbNetOperations
//									.getPredecessorDecisions(destinationNode, instantiatedNet);
//							// If it has predecessor decisions, do not reveal it yet, but add revealing links
//							// from every predecessor decision to the node
//							if (predecessorDecisions.isEmpty()) {
//								destinationNode.setAlwaysObserved(true);
//							} else {
//								for (Node predecessorDecision : predecessorDecisions) {
//									Link<Node> revealingArc = instantiatedNet
//											.addLink(predecessorDecision, destinationNode, true);
//									State[] predecessorDecisionStates = predecessorDecision.getVariable().getStates();
//									for (int i = 0; i < predecessorDecisionStates.length; ++i)
//										revealingArc.addRevealingState(predecessorDecisionStates[i]);
//								}
//							}
//						}
//					}
//				}
//				if (link.hasRestrictions()) {
//					State[] restrictedVariableStates = destinationNode.getVariable().getStates();
//					List<State> nonRestrictedStates = ProbNetOperations
//							.getUnrestrictedStates(link, restrictedVariableStates, state);
//
//					if (nonRestrictedStates.isEmpty()) {
//						// Remove destination node and its descendants!
//						Stack<Node> disposableNodes = new Stack<>();
//						disposableNodes.push(destinationNode);
//						while (!disposableNodes.isEmpty()) {
//							Node disposableNode = disposableNodes.pop();
//							// If it's a decision node, check if there is another
//							// path to it from another decision
//							if (disposableNode.getNodeType() != NodeType.DECISION || !ProbNetOperations
//									.hasPredecessorDecision(disposableNode, instantiatedNet)) {
//								for (Node descendant : instantiatedNet.getChildren(disposableNode)) {
//									disposableNodes.push(descendant);
//								}
//								instantiatedNet.removeNode(disposableNode);
//							}
//						}
//
//						//                    }else if(nonRestrictedStates.size () == 1) // Remove variables with a single variable
//						//                    {
//						//                        ProbNet probNetWithoutSingleStateVariable = probNetCopy.copy ();
//						//                        probNetWithoutSingleStateVariable.removeNode (probNetWithoutSingleStateVariable.getNode (destinationNode.getVariable ()));
//						//                        probNetCopy = applyRestrictionsAndReveal(probNetWithoutSingleStateVariable, destinationNode, nonRestrictedStates.get (0), originalProbNet);
//					} else if (nonRestrictedStates.size() < restrictedVariableStates.length) {
//						// At least one of the states of the destination node is restricted.
//						// Make a copy of the variable and remove the restricted states
//						State[] unrestrictedStates = nonRestrictedStates.toArray(new State[0]);
//						Variable restrictedVariable = new Variable(destinationNode.getVariable().getName(),
//								unrestrictedStates);
//						restrictedVariable.setVariableType(destinationNode.getVariable().getVariableType());
//						//                        List<Node> parents = destinationNode.getParents();
//						//                        List<Node> children = destinationNode.getChildren();
//						//                        probNetCopy.removeNode(destinationNode);
//						//                        Node newNode = probNetCopy.addNode(restrictedVariable, destinationNode.getNodeType());
//						destinationNode.setVariable(restrictedVariable);
//					} else {
//						// No state restricted, leave destinationNode as it is
//					}
//				}
//			}
//		}
//
//		instantiatedNet.removeNode(instantiatedNet.getNode(node.getVariable()));
//		return instantiatedNet;
//	}
//
//	public double getMEU(ProbNet danNet) {
//		DecisionTreeNode root = resolve(danNet, danNet, null);
//		return root.getUtility();
//	}
//
//	private DecisionTreeNode resolve(ProbNet probNet, ProbNet originalProbNet, DecisionTreeNode parentNode) {
//		DecisionTreeNode root = null;
//		try {
//			List<Node> alwaysObservedVariables = DecisionTreeBuilder
//					.getAlwaysObservedVariablesWithoutObservableParents(probNet);
//			if (!alwaysObservedVariables.isEmpty()) // Always observed variables
//			{
//				// Get first node in the list
//				Node alwaysObservedNode = alwaysObservedVariables.get(0);
//				Variable alwaysObservedVariable = alwaysObservedNode.getVariable();
//				Variable originalVariable = getOriginalVariable(alwaysObservedVariable, originalProbNet);
//				DecisionTreeNode treeNode = null;
//				double utility = 0;
//				double scenarioProbability = 0;
//				for (int i = 0; i < alwaysObservedVariable.getNumStates(); ++i) {
//					State state = alwaysObservedVariable.getStates()[i];
//					int originalState = originalVariable.getStateIndex(alwaysObservedVariable.getStates()[i]);
//					treeNode = new DecisionTreeNode(originalVariable, originalState, parentNode);
//					ProbNet restrictedProbNet = instantiate(probNet, alwaysObservedNode, state, originalProbNet);
//					DecisionTreeNode node = resolve(restrictedProbNet, originalProbNet, treeNode);
//					// Join
//					utility += node.getUtility() * node.getProbability();
//					scenarioProbability += node.getProbability();
//				}
//				treeNode.setProbability(scenarioProbability);
//				treeNode.setUtility((scenarioProbability != 0) ? utility / scenarioProbability : 0);
//				root = treeNode;
//			} else {
//				List<Node> parentlessDecisions = DecisionTreeBuilder.getNextDecisions(probNet);
//				if (!parentlessDecisions.isEmpty()) // Parentless decision nodes
//				{
//					double utility = Double.NEGATIVE_INFINITY;
//					DecisionTreeNode treeNode = null;
//					for (Node decisionNode : parentlessDecisions) {
//						Variable decisionVariable = decisionNode.getVariable();
//						Variable originalVariable = getOriginalVariable(decisionVariable, originalProbNet);
//						for (int i = 0; i < decisionVariable.getNumStates(); ++i) {
//							State state = decisionVariable.getStates()[i];
//							int originalState = originalVariable.getStateIndex(decisionVariable.getStates()[i]);
//							treeNode = new DecisionTreeNode(originalVariable, originalState, parentNode);
//							ProbNet restrictedProbNet = instantiate(probNet, decisionNode, state, originalProbNet);
//							DecisionTreeNode node = resolve(restrictedProbNet, originalProbNet, treeNode);
//							// All probabilities should be equal
//							treeNode.setProbability(node.getProbability());
//							// join by maximization
//							utility = (node.getUtility() > utility) ? node.getUtility() : utility;
//						}
//					}
//					treeNode.setUtility(utility);
//					root = treeNode;
//				} else {
//					List<Node> neverObservedNodes = probNet.getNodes(NodeType.CHANCE);
//					if (!neverObservedNodes.isEmpty()) // Never observed variables
//					{
//						ProbNet dtProbNet = probNet.copy();
//						Node neverObservedNode = neverObservedNodes.get(0);
//						Variable neverObservedVariable = neverObservedNode.getVariable();
//						Variable originalVariable = getOriginalVariable(neverObservedVariable, originalProbNet);
//						dtProbNet.removeNode(dtProbNet.getNode(neverObservedVariable));
//						DecisionTreeNode treeNode = null;
//						double scenarioProbability = 0;
//						double utility = 0;
//						for (int i = 0; i < neverObservedVariable.getNumStates(); ++i) {
//							int originalState = originalVariable.getStateIndex(neverObservedVariable.getStates()[i]);
//							treeNode = new DecisionTreeNode(originalVariable, originalState, parentNode);
//							DecisionTreeNode node = resolve(dtProbNet, originalProbNet, treeNode);
//							utility += node.getUtility() * node.getProbability();
//							scenarioProbability += node.getProbability();
//						}
//						treeNode.setProbability(scenarioProbability);
//						treeNode.setUtility((scenarioProbability != 0) ? utility / scenarioProbability : 0);
//						root = treeNode;
//					} else // Utility nodes
//					{
//						// Calculate utility and scenario probability
//						ProbNet dtProbNet = probNet.copy();
//						root = new DecisionTreeNode();
//						HashMap<Variable, Integer> scenarioMap = getScenarioMap(originalProbNet, parentNode);
//						root.setProbability(getScenarioProbability(originalProbNet, parentNode, scenarioMap));
//						root.setUtility(getUtility(dtProbNet, scenarioMap));
//					}
//				}
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		return root;
//	}
//
//	private Variable getOriginalVariable(Variable variable, ProbNet originalProbNet) {
//		Variable originalVariable = variable;
//		if (!originalProbNet.containsVariable(variable)) {
//			try {
//				originalVariable = originalProbNet.getVariable(variable.getName());
//			} catch (NodeNotFoundException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
//		return originalVariable;
//	}
//
//	private double getUtility(ProbNet probNet, HashMap<Variable, Integer> scenarioMap) {
//		Map<Variable, Double> utilities = new HashMap<>();
//		double utility = Double.NaN;
//		while (probNet.getNumNodes() > 0) {
//			// Find utilities without parents
//			List<Node> orphanNodes = new ArrayList<>();
//			for (Node node : probNet.getNodes()) {
//				if (probNet.getNumParents(node) == 0)
//					orphanNodes.add(node);
//			}
//			for (Node orphanNode : orphanNodes) {
//				Potential potential = orphanNode.getPotentials().get(0);
//				// If it is a Sum Potential, sum their parents
//				double nodeUtility = Double.NaN;
//				if (potential instanceof SumPotential) {
//					nodeUtility = 0;
//					for (Variable parentVariable : potential.getVariables()) {
//						nodeUtility += utilities.get(parentVariable);
//					}
//				} else if (potential instanceof ProductPotential) {
//					// If they are products, multiply their parents
//					nodeUtility = 1;
//					for (Variable parentVariable : potential.getVariables()) {
//						nodeUtility *= utilities.get(parentVariable);
//					}
//				} else {
//					// Otherwise, calculate their utility
//					nodeUtility = potential.getProbability(scenarioMap);
//				}
//				utilities.put(orphanNode.getVariable(), nodeUtility);
//			}
//			// Remove nodes from probNet
//			for (Node orphanNode : orphanNodes) {
//				probNet.removeNode(orphanNode);
//			}
//
//			if (probNet.getNumNodes() == 0) {
//				// Sum utilities of all orphanNodes
//				utility = 0;
//				for (Node orphanNode : orphanNodes) {
//					utility += utilities.get(orphanNode.getVariable());
//				}
//			}
//		}
//		return utility;
//	}
//
//	private double getScenarioProbability(ProbNet originalProbNet, DecisionTreeNode parentNode,
//			HashMap<Variable, Integer> scenarioMap) throws InvalidStateException, IncompatibleEvidenceException {
//		List<Potential> parentPotentials = getScenarioPotentials(originalProbNet, parentNode);
//		double probability = 1;
//		for (Potential potential : parentPotentials) {
//			probability *= potential.getProbability(scenarioMap);
//		}
//		return probability;
//	}
//
//	private List<Potential> getScenarioPotentials(ProbNet probNet, DecisionTreeNode treeNode) {
//		List<Potential> parentPotentials = new ArrayList<>();
//		DecisionTreeNode currentTreeNode = treeNode;
//		while (currentTreeNode != null) {
//			Node node = probNet.getNode(currentTreeNode.getVariable());
//			if (node == null) {
//				try {
//					node = probNet.getNode(currentTreeNode.getVariable().getName());
//				} catch (NodeNotFoundException e) {
//				}
//			}
//			List<Potential> potentials = node.getPotentials();
//			if (potentials != null && !potentials.isEmpty())
//				parentPotentials.add(potentials.get(0));
//			currentTreeNode = currentTreeNode.getParent();
//		}
//		return parentPotentials;
//	}
//
//	private HashMap<Variable, Integer> getScenarioMap(ProbNet probNet, DecisionTreeNode parentNode)
//			throws InvalidStateException, IncompatibleEvidenceException {
//		HashMap<Variable, Integer> scenarioMap = new LinkedHashMap<>();
//		DecisionTreeNode node = parentNode;
//		while (node != null) {
//			scenarioMap.put(node.getVariable(), node.getState());
//			node = node.getParent();
//		}
//		return scenarioMap;
//	}
//
//	private class DecisionTreeNode {
//		private DecisionTreeNode parent;
//		private Variable variable;
//		private int state;
//		private double utility = Double.NEGATIVE_INFINITY;
//		private double probability = Double.NEGATIVE_INFINITY;
//
//		public DecisionTreeNode() {
//		}
//
//		public DecisionTreeNode(Variable variable, int state, DecisionTreeNode parentNode) {
//			this.variable = variable;
//			this.state = state;
//			this.parent = parentNode;
//		}
//
//		public DecisionTreeNode getParent() {
//			return parent;
//		}
//
//		public Variable getVariable() {
//			return variable;
//		}
//
//		public int getState() {
//			return state;
//		}
//
//		public double getUtility() {
//			return utility;
//		}
//
//		public void setUtility(double utility) {
//			this.utility = utility;
//		}
//
//		public double getProbability() {
//			return probability;
//		}
//
//		public void setProbability(double probability) {
//			this.probability = probability;
//		}
//
//		public String toString() {
//			StringBuilder sb = new StringBuilder();
//			sb.append((variable != null) ? variable.getName() + "=" + state : "");
//			sb.append(utility != Double.NEGATIVE_INFINITY ? " U=" + utility : "");
//			sb.append(probability != Double.NEGATIVE_INFINITY ? " P=" + probability : "");
//			return sb.toString();
//		}
//
//	}

}
