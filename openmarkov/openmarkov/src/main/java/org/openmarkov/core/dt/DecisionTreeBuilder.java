/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.dt;

public class DecisionTreeBuilder {
//	public static DecisionTreeElement buildDecisionTree(ProbNet probNet) {
//		DecisionTreeElement root = null;
//		if (probNet.getNetworkType() instanceof InfluenceDiagramType) {
//			root = buildDecisionTreeFromID(probNet);
//		} else if (probNet.getNetworkType() instanceof DecisionAnalysisNetworkType) {
//			root = new DecisionTreeBranch(probNet);
//			((DecisionTreeBranch) root).setChild((DecisionTreeNode) buildDecisionTreeFromDAN(probNet, probNet));
//		}
//		return root;
//	}
//
//	private static DecisionTreeElement buildDecisionTreeFromDAN(ProbNet originalProbNet, ProbNet probNet) {
//		DecisionTreeElement root = null;
//		//TODO Uncomment next line when it is possible to invoke the constructor of DANDecisionTreeEvaluation from here.
//		//root = new DANDecisionTreeEvaluation(probNet).getDecisionTree();
//		return root;
//	}
//
//
//	public static List<Node> getAlwaysObservedVariablesWithoutObservableParents(ProbNet probNet) {
//		List<Node> alwaysObservedVariables = ProbNetOperations.getAlwaysObservedVariables(probNet);
//		List<Node> filteredVariables = new ArrayList<>();
//		for (Node alwaysObservedVariable : alwaysObservedVariables) {
//			boolean noObservedParent = true;
//			int i = 0;
//			while (i < alwaysObservedVariables.size() && noObservedParent) {
//				if (!alwaysObservedVariable.equals(alwaysObservedVariables.get(i)))
//					noObservedParent = !probNet.isParent(alwaysObservedVariables.get(i), alwaysObservedVariable);
//				++i;
//			}
//			if (noObservedParent)
//				filteredVariables.add(alwaysObservedVariable);
//		}
//		return filteredVariables;
//	}
//
//	public static List<Node> getNextDecisions(ProbNet probNet) {
//
//		List<Node> decisionNodes = ProbNetOperations.getParentlessDecisions(probNet);
//		// Check if the nodes revealed by a decision node are the subset of another
//		// In that case we don't need to consider them as valid orders
//		List<Set<Node>> revealedNodes = new ArrayList<>();
//		for (Node node : decisionNodes) {
//			Set<Node> revealedByDecision = new HashSet<>();
//			for (Link<Node> link : node.getLinks()) {
//				if (link.getNode1().equals(node) && link.hasRevealingConditions()) {
//					revealedByDecision.add((Node) link.getNode2());
//				}
//			}
//			revealedNodes.add(revealedByDecision);
//		}
//		List<Node> dominatedDecisions = new ArrayList<>();
//		for (int i = 0; i < decisionNodes.size(); ++i) {
//			Node nodeA = decisionNodes.get(i);
//			for (int j = 0; j < decisionNodes.size(); ++j) {
//				Node nodeB = decisionNodes.get(j);
//				if (nodeA != nodeB
//						// if both sets are equal, just mark one of the nodes as dominated
//						&& !(revealedNodes.get(i).equals(revealedNodes.get(j)) && i < j) && revealedNodes.get(i)
//						.containsAll(revealedNodes.get(j)))
//					dominatedDecisions.add(nodeB);
//			}
//		}
//		decisionNodes.removeAll(dominatedDecisions);
//
//		return decisionNodes;
//	}
//
//	/**
//	 * Builds a decision tree from an influence diagram
//	 *
//	 * @param probNet influence diagram
//	 * @return decision tree
//	 */
//	public static DecisionTreeElement buildDecisionTreeFromID(ProbNet probNet) {
//		ProbNet dtProbNet = probNet.copy();
//		Node svNode = getSuperValueNode(dtProbNet);
//		List<Variable> variables = getPartiallySortedVariables(dtProbNet);
//		DecisionTreeElement root = new DecisionTreeBranch(dtProbNet);
//		Stack<DecisionTreeElement> treeStack = new Stack<>();
//		treeStack.push(root);
//		List<DecisionTreeBranch> leaves = new ArrayList<>();
//		// Build tree with decision & utility nodes
//		while (!treeStack.isEmpty()) {
//			DecisionTreeElement treeElement = treeStack.pop();
//			// If a node
//			if (treeElement instanceof DecisionTreeNode) {
//				// Get next variable in the list
//				Variable variable = ((DecisionTreeNode) treeElement).getVariable();
//				for (State state : variable.getStates()) {
//					DecisionTreeBranch treeBranch = new DecisionTreeBranch(dtProbNet, variable, state);
//					((DecisionTreeNode) treeElement).addChild(treeBranch);
//					treeStack.push(treeBranch);
//				}
//			}
//			// If a branch
//			else if (treeElement instanceof DecisionTreeBranch) {
//				Variable branchVariable = ((DecisionTreeBranch) treeElement).getBranchVariable();
//				Variable childVariable = null;
//				// If this is the root
//				if (branchVariable == null) {
//					childVariable = variables.get(0);
//				}
//				// If this neither the root nor a leaf
//				else if (variables.indexOf(branchVariable) + 1 < variables.size()) {
//					childVariable = variables.get(variables.indexOf(branchVariable) + 1);
//				}
//				// If this is a leaf
//				else {
//					leaves.add((DecisionTreeBranch) treeElement);
//				}
//				if (childVariable != null) {
//					DecisionTreeNode child = new DecisionTreeNode(dtProbNet.getNode(childVariable));
//					((DecisionTreeBranch) treeElement).setChild(child);
//					treeStack.push(child);
//				}
//			}
//		}
//
//		// Add utility trees at the tip of each leaf
//		for (DecisionTreeBranch leaf : leaves) {
//			leaf.setChild(addUtilityNodes(svNode));
//		}
//		return root;
//	}
//
//	/**
//	 * Looks for the super value node. If there is none, it creates it.
//	 *
//	 * @param probNet network
//	 * @return supervalue node
//	 */
//	public static Node getSuperValueNode(ProbNet probNet) {
//		Node svNode = null;
//		// Look for leaves
//		List<Node> leaves = getUtilityLeaves(probNet);
//		// if there is more than one leave, create a new super value node
//		if (leaves.size() > 1) {
//			Variable svVariable = new Variable("Global Utility");
//			svNode = probNet.addNode(svVariable, NodeType.UTILITY);
//			List<Variable> leafVariables = new ArrayList<>(leaves.size());
//			for (Node leafNode : leaves) {
//				leafVariables.add(leafNode.getVariable());
//			}
//			svNode.addPotential(new SumPotential(leafVariables, PotentialRole.UNSPECIFIED));
//			for (Node leaf : leaves) {
//				probNet.addLink(leaf, svNode, true);
//			}
//		} else if (leaves.size() == 1) {
//			svNode = leaves.get(0);
//		}
//		return svNode;
//	}
//
//	private static List<Node> getUtilityLeaves(ProbNet probNet) {
//		List<Node> leaves = new ArrayList<>();
//		for (Node node : probNet.getNodes()) {
//			if (node.getNodeType() == NodeType.UTILITY && probNet.getChildren(node).isEmpty()) {
//				leaves.add(node);
//			}
//		}
//		return leaves;
//	}
//
//	/**
//	 * Using PartialOrder generates a sorted plain list of decision and chance variables
//	 *
//	 * @param probNet ProbNet
//	 * @return List<Variable>
//	 */
//	private static List<Variable> getPartiallySortedVariables(ProbNet probNet) {
//		List<Variable> variables;
//		//PartialOrder partialOrder = null;
//		List<List<Variable>> partialOrder = BasicOperations
//				.calculatePartialOrder(probNet); //new PartialOrder (probNet);
//		variables = new ArrayList<>(BasicOperations.getNumVariables(probNet)); //(partialOrder.getNumVariables ());
//		for (Collection<Variable> variableSubList : partialOrder) //partialOrder.getOrder ())
//		{
//			variables.addAll(variableSubList);
//		}
//		return variables;
//	}
//
//	/**
//	 * Adds a utility tree at the tip of each leaf
//	 *
//	 * @param svNode A super value node
//	 */
//	public static DecisionTreeNode addUtilityNodes(Node svNode) {
//		// Add utility nodes
//		DecisionTreeNode svTreeNode = new DecisionTreeNode(svNode);
//		ProbNet probNet = svNode.getProbNet();
//		Stack<DecisionTreeNode> utilityTreeStack = new Stack<>();
//		utilityTreeStack.push(svTreeNode);
//		while (!utilityTreeStack.isEmpty()) {
//			DecisionTreeNode utilityTreeNode = utilityTreeStack.pop();
//			Node utilityNode = probNet.getNode(utilityTreeNode.getVariable());
//			for (Node parentNode : utilityNode.getParents()) {
//				if (parentNode.getNodeType() == NodeType.UTILITY) {
//					DecisionTreeNode treeNode = new DecisionTreeNode(parentNode);
//					utilityTreeNode.addChild(treeNode);
//					utilityTreeStack.push(treeNode);
//				}
//			}
//		}
//		return svTreeNode;
//	}
//
//	private static ProbNet instantiate(ProbNet probNet, Node node, State state, ProbNet originalProbNet) {
//		ProbNet instantiatedNet = probNet.copy();
//
//		for (Link<Node> link : probNet.getLinks(node)) {
//			if (link.getNode1().equals(node)) // Our node is the source node
//			{
//				Node destinationNode = instantiatedNet.getNode(link.getNode2().getVariable());
//				// Remove link between restricting node and restricted node
//				instantiatedNet.removeLink(link.getNode1().getVariable(), link.getNode2().getVariable(), true);
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
//	public static List<Node> getNeverObservedVariables(ProbNet probNet) {
//		List<Node> neverObservedVariables = new ArrayList<>();
//		for (Node node : probNet.getNodes(NodeType.CHANCE)) {
//			if (probNet.getParents(node).isEmpty()) {
//				neverObservedVariables.add(node);
//			}
//		}
//		return neverObservedVariables;
//	}
//

}
