/*
	 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
	 * Unless required by applicable law or agreed to in writing,
	 * this code is distributed on an "AS IS" basis,
	 * WITHOUT WARRANTIES OF ANY KIND.
	 */

	package org.openmarkov.inference.decompositionIntoSymmetricDANs.core;

	import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.openmarkov.core.dt.DecisionTreeBranch;
import org.openmarkov.core.dt.DecisionTreeNode;
import org.openmarkov.core.exception.IncompatibleEvidenceException;
import org.openmarkov.core.exception.InvalidStateException;
import org.openmarkov.core.exception.NotEvaluableNetworkException;
import org.openmarkov.core.exception.UnexpectedInferenceException;
import org.openmarkov.core.model.network.EvidenceCase;
import org.openmarkov.core.model.network.Finding;
import org.openmarkov.core.model.network.Node;
import org.openmarkov.core.model.network.NodeType;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.State;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.potential.PotentialRole;
import org.openmarkov.core.model.network.potential.StrategyTree;
import org.openmarkov.core.model.network.potential.SumPotential;
import org.openmarkov.inference.decompositionIntoSymmetricDANs.DecisionTreeComputation;

	public class DANDecisionTreeInference extends DANInference implements DecisionTreeComputation {
		private DecisionTreeNode decisionTree;
		//private ProbNet probNet;
		private boolean computeDecisionTreeForGUI;
		
	public DANDecisionTreeInference(ProbNet probNet, boolean isCEA) throws NotEvaluableNetworkException {
		this(probNet, false, isCEA);
	}

	public DANDecisionTreeInference(ProbNet probNet, boolean computeDecisionTreeForGUI, boolean isCEA)
			throws NotEvaluableNetworkException {
		this(probNet, null, computeDecisionTreeForGUI, isCEA);
	}

	public DANDecisionTreeInference(ProbNet probNet, EvidenceCase evidenceCase, boolean isCEA)
			throws NotEvaluableNetworkException {
		this(probNet, evidenceCase, false, isCEA);
	}
	
	public DANDecisionTreeInference(ProbNet probNet, int treeDepth, boolean computeDecisionTreeForGUI2) throws NotEvaluableNetworkException {
		this(probNet, treeDepth, computeDecisionTreeForGUI2, false);
	}

	public DANDecisionTreeInference(ProbNet probNet, EvidenceCase evidenceCase, boolean computeDecisionTreeForGUI,
			boolean isCEA) throws NotEvaluableNetworkException {
		this(probNet, evidenceCase, Integer.MAX_VALUE, computeDecisionTreeForGUI, isCEA);
	}

	public DANDecisionTreeInference(ProbNet probNet, int treeDepth, boolean computeDecisionTreeForGUI, boolean isCEA)
			throws NotEvaluableNetworkException {
		this(probNet, null, treeDepth, computeDecisionTreeForGUI, isCEA);
	}
	
	public DANDecisionTreeInference(ProbNet probNet, int treeDepth, boolean computeDecisionTreeForGUI2,
			EvidenceCase evidenceCase) throws NotEvaluableNetworkException {
		this(probNet, evidenceCase, treeDepth, computeDecisionTreeForGUI2, false);
	}

	public DANDecisionTreeInference(ProbNet dan, EvidenceCase evidenceCase, int treeDepth,
			boolean computeDecisionTreeForGUI, boolean isCEA) throws NotEvaluableNetworkException {
		this(dan, evidenceCase, treeDepth, 0, computeDecisionTreeForGUI, isCEA);
	}
		
	
		
		
		protected DANDecisionTreeInference(ProbNet dan, EvidenceCase evidenceCase, int maximumTreeDepth,
				int currentTreeDepth, boolean computeDecisionTreeForGUI, boolean isCEA) throws NotEvaluableNetworkException {
			super(dan, isCEA);
			List<Variable> alwaysObservedVariables;
			boolean thereAreDecisions = !dan.getNodes(NodeType.DECISION).isEmpty();

			this.setComputeDecisionTreeForGUI(computeDecisionTreeForGUI);
			if (thereAreDecisions) {
				alwaysObservedVariables = getAlwaysObservedVariables(dan, null, evidenceCase);
			} else {
				alwaysObservedVariables = DANOperations.getChanceVariablesNotInEvidence(dan, evidenceCase);
			}
			boolean maximumTreeDepthReached = (currentTreeDepth == maximumTreeDepth);
			if (maximumTreeDepthReached || (alwaysObservedVariables.isEmpty() && !thereAreDecisions)) {
				DANInference evaluation;
				if (maximumTreeDepthReached) {
					evaluation = new DANDecompositionIntoSymmetricDANsInference(dan, null, evidenceCase, isCEAnalysis);
				} else {// alwaysObservedVariables.isEmpty() && !thereAreDecisions
					evaluation = new DANConditionalSymmetricInference(dan, null, evidenceCase, isCEAnalysis);
				}
				setProbabilityAndUtilityFromEvaluation(evaluation);
				// Create the structure of leaves in the DT with utility nodes
				Node svNode = getSuperValueNode(dan,isCEA);
				if (computeDecisionTreeForGUI) {
					decisionTree = createDecisionTreeNode(dan, svNode,evidenceCase);
					//TODO Consider in some moment to have the tree structure of SV nodes in the DT in the GUI
					//addNonSVUtilityNodesToDecisionTree();
				}
			} else {
				int childTreeDepth = currentTreeDepth + 1;
				if (!alwaysObservedVariables.isEmpty()) { // If O is not empty
					Variable x = DANOperations.selectVariableWithoutAncestorsInVariables(alwaysObservedVariables, dan);
					if (computeDecisionTreeForGUI) {
						decisionTree = createDecisionTreeNode(dan, x,evidenceCase);
					}
					State[] states = x.getStates();
					for (State state : states) {
						ProbNet dan_x = DANOperations.instantiate(dan, x, state);
						try {
							childEvaluationDecisionTree(dan_x, DANOperations.extendEvidenceCase(evidenceCase, x, state),
									maximumTreeDepth, childTreeDepth, x, state);
						} catch (InvalidStateException | IncompatibleEvidenceException e) {
							e.printStackTrace();
						}
					}
					conditionEliminateChanceAndSetProbabilityAndUtility(dan, x);
				} else {
					Variable rootDecision;
					// Get the decisions that can be made first
					List<Node> nextDecisions = DANOperations.getNextDecisions(dan);
					if (nextDecisions.size() == 1) { // If exactly one decision D can be made first
						rootDecision = nextDecisions.get(0).getVariable();
						if (computeDecisionTreeForGUI) {
							decisionTree = createDecisionTreeNode(dan, rootDecision,evidenceCase);
						}
						for (State state : rootDecision.getStates()) {
							ProbNet dan_x = DANOperations.instantiate(dan, rootDecision, state);
							childEvaluationDecisionTree(dan_x, evidenceCase, maximumTreeDepth, childTreeDepth, rootDecision,
									state);
						}
					} else {// Several decisions can be made first
						rootDecision = DANOperations.createDummyVariableOfOrder(nextDecisions);
						Node orderDecisionNode = new Node(probNet, rootDecision, NodeType.DECISION);
						if (computeDecisionTreeForGUI) {
							decisionTree = createDecisionTreeNode(dan, orderDecisionNode,evidenceCase);
						}
						// Prioritize each decision
						for (Node decision : nextDecisions) {
							ProbNet prioritizedDAN = DANOperations.prioritize(dan, decision);
							try {
								childEvaluationDecisionTree(prioritizedDAN, evidenceCase, maximumTreeDepth, childTreeDepth,
										rootDecision, rootDecision.getState(decision.getName()));
							} catch (InvalidStateException e) {
								e.printStackTrace();
							}
						}
					}
					conditionMaximizeAndSetProbabilityAndUtility(dan, rootDecision);
				}
			}
			if (computeDecisionTreeForGUI) {
				decisionTree.setScenarioProbability(DANOperations.getOnlyValuePotential(this.getProbability()));
				decisionTree.setOnlyValueForUtility(this.getUtility());
			}

		}
		
		
		

		

		

		/**
		 * Looks for the super value node. If there is none, it creates it.
		 *
		 * @param probNet network
		 * @param isCEA 
		 * @return supervalue node
		 * TODO: Please, do not remove this method. It was implemented by Iñigo, when the structure of super-value nodes was managed differently in the decision tree GUI.
		 * It contains a valuable knowledge that could be reused in the future.
		 */		
		public static Node oldGetSuperValueNode(ProbNet probNet, boolean addSVNodeToTheDAN, boolean isCEA) {
			Node svNode = null;
			// Look for leaves
			List<Node> leaves = getUtilityLeaves(probNet);
			// if there is more than one leave, create a new super value node
			int numLeaves = leaves.size();
			if (numLeaves > 1) {
				Variable svVariable = new Variable(!isCEA?"Global Utility":"Value");
				NodeType svType = NodeType.UTILITY;
				if (addSVNodeToTheDAN) {
					svNode = probNet.addNode(svVariable, svType);
					List<Variable> leafVariables = new ArrayList<>(numLeaves);
					for (Node leafNode : leaves) {
						leafVariables.add(leafNode.getVariable());
					}
					svNode.addPotential(new SumPotential(leafVariables, PotentialRole.UNSPECIFIED));
					for (Node leaf : leaves) {
						probNet.addLink(leaf, svNode, true);
					}
				}
				else {
					svNode = new Node(probNet,svVariable,svType);
				}
			} else if (numLeaves == 1) {
				svNode = leaves.get(0);
			}
			return svNode;
		}
		
		
		
		public static Node getSuperValueNode(ProbNet probNet, boolean isCEA) {
			Node svNode = null;
			// Look for leaves
			List<Node> leaves = getUtilityLeaves(probNet);
			// if there is more than one leave, create a new super value node
			int numLeaves = leaves.size();
			if (numLeaves != 1) {
				Variable svVariable = new Variable(!isCEA?"Global Utility":"Value");
				NodeType svType = NodeType.UTILITY;
				svNode = new Node(probNet,svVariable,svType);
			} else if (numLeaves == 1) {
				svNode = leaves.get(0);
			}
			return svNode;
		}

		private static List<Node> getUtilityLeaves(ProbNet probNet) {
			List<Node> leaves = new ArrayList<>();
			for (Node node : probNet.getNodes()) {
				if (node.getNodeType() == NodeType.UTILITY && probNet.getChildren(node).isEmpty()) {
					leaves.add(node);
				}
			}
			return leaves;
		}
		
		
		/**
		 * Adds a utility tree at the tip of each leaf
		 */
		public void addNonSVUtilityNodesToDecisionTree() {
			// Add utility nodes
			//DecisionTreeNode svTreeNode = new DecisionTreeNode(svNode);
			ProbNet probNet = decisionTree.getNetwork();		
			
			Stack<DecisionTreeNode> utilityTreeStack = new Stack<>();
			utilityTreeStack.push(decisionTree);
			while (!utilityTreeStack.isEmpty()) {
				DecisionTreeNode utilityTreeNode = utilityTreeStack.pop();
				Node utilityNode = probNet.getNode(utilityTreeNode.getVariable());
				for (Node parentNode : utilityNode.getParents()) {
					if (parentNode.getNodeType() == NodeType.UTILITY) {
						//DecisionTreeNode treeNode = new DecisionTreeNode(parentNode);
						DecisionTreeNode treeNode = FactoryDecisionTree.createInstanceDecisionTreeNode(isCEAnalysis, parentNode);
						utilityTreeNode.addChild(treeNode);
						utilityTreeStack.push(treeNode);
					}
				}
			}
		}

		private DecisionTreeNode createDecisionTreeNode(ProbNet dan, Variable rootDecision, EvidenceCase evidence) {
			setDANNameFromEvidence(dan,evidence);
			return FactoryDecisionTree.createInstanceDecisionTreeNode(isCEAnalysis, rootDecision, dan);
		}

		private void setDANNameFromEvidence(ProbNet dan, EvidenceCase evidence) {
			if (evidence != null) {
				List<Finding> findings = evidence.getFindings();
				int size = findings.size();
				if (size > 0) {
					Finding lastFinding = findings.get(size - 1);
					Variable variable = lastFinding.getVariable();
					String oldName = dan.getName();
					oldName = oldName.substring(0,oldName.length()-5);
					dan.setName(oldName + "-"+ replaceWhiteSpaces(variable.getName()) + "=" + replaceWhiteSpaces(variable.getStateName(lastFinding.getStateIndex()))+".pgmx");
				}
			}
		}

		private String replaceWhiteSpaces(String name) {
			return name.replaceAll("\\s","_");
		}

		private DecisionTreeNode createDecisionTreeNode(ProbNet dan, Node node, EvidenceCase evidence) {
			setDANNameFromEvidence(dan,evidence);
			return FactoryDecisionTree.createInstanceDecisionTreeNode(isCEAnalysis, node, dan);
		}
		
	
		

		/**
		 * @param dan
		 * @param evidenceCase
		 * @param maximumTreeDepth
		 * @param childTreeDepth
		 * @param x
		 * @param state
		 * @throws NotEvaluableNetworkException This method (1) executes a recursive (child) evaluation with
		 *                                      parameters 'dan', 'evidenceCase, 'maximumTreeDepth' and
		 *                                      'childTreeDepth', (2) adds the results of the child
		 *                                      evaluation to the attributes of this object
		 *                                      'childrenProbability' and 'childrenUtility', and (3) adds a
		 *                                      branch to the decision tree
		 */
		private void childEvaluationDecisionTree(ProbNet dan, EvidenceCase evidenceCase, int maximumTreeDepth,
				int childTreeDepth, Variable x, State state) throws NotEvaluableNetworkException {
			DANDecisionTreeInference auxEval = null;
			auxEval = constructDecisionTreeInference(dan, evidenceCase, maximumTreeDepth, childTreeDepth);
			addChildEvaluationResultsAndUpdateDecisionTree(auxEval, dan, x, state);
		}

		protected DANDecisionTreeInference constructDecisionTreeInference(ProbNet dan, EvidenceCase evidenceCase,
				int maximumTreeDepth, int childTreeDepth) throws NotEvaluableNetworkException {
			return new DANDecisionTreeInference(dan, evidenceCase, maximumTreeDepth, childTreeDepth, computeDecisionTreeForGUI(), isCEAnalysis);
		}

		/**
		 * @param childEvaluation This method: (1) adds to 'childrenProbability' and
		 *                        'childrenUtility' the results of the child evaluation
		 *                        'childEvaluation'; and (2) updates the decision tree by adding a
		 *                        branch labeled with ('Variable' = 'state') and by setting the
		 *                        corresponding values of 'utility' and 'scenarioProbability".
		 * @param dan             ProbNet representing a Decision Analysis Network
		 * @param x               Variable
		 * @param state           State
		 */
		private void addChildEvaluationResultsAndUpdateDecisionTree(DANDecisionTreeInference childEvaluation, ProbNet dan,
				Variable x, State state) {
			super.addResultsOfChildEvaluation(childEvaluation);
			if (computeDecisionTreeForGUI()) {
				addBranchAndChildToDecisionTree(dan, x, state, childEvaluation);
			}
		}

		private void addBranchAndChildToDecisionTree(ProbNet dan, Variable x, State state,
				DANDecisionTreeInference auxEval) {
			DecisionTreeBranch treeBranch = new DecisionTreeBranch(dan, x, state);
			decisionTree.addChild(treeBranch);
			treeBranch.setChild(auxEval.getDecisionTree());
			treeBranch.setScenarioProbability(DANOperations.getOnlyValuePotential(auxEval.getProbability()));
			// TODO See if this is really necessary
			//treeBranch.setOnlyValueUtilityOrCEP(auxEval.getUtility());
		}

		
	
		public StrategyTree getOptimalStrategyTree()
				throws UnexpectedInferenceException, IncompatibleEvidenceException, NotEvaluableNetworkException {
			return null;
		}

		@Override
		public DecisionTreeNode getDecisionTree() {
			return decisionTree;
		}

		public boolean computeDecisionTreeForGUI() {
			return computeDecisionTreeForGUI;
		}

		public void setComputeDecisionTreeForGUI(boolean computeDecisionTreeForGUI) {
			this.computeDecisionTreeForGUI = computeDecisionTreeForGUI;
		}

		
	}
