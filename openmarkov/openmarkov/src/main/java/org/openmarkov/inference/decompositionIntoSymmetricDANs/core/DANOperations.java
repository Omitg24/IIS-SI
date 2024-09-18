/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.inference.decompositionIntoSymmetricDANs.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import org.openmarkov.core.exception.IncompatibleEvidenceException;
import org.openmarkov.core.exception.InvalidStateException;
import org.openmarkov.core.exception.NodeNotFoundException;
import org.openmarkov.core.exception.NonProjectablePotentialException;
import org.openmarkov.core.exception.WrongCriterionException;
import org.openmarkov.core.inference.BasicOperations;
import org.openmarkov.core.model.graph.Link;
import org.openmarkov.core.model.network.CEP;
import org.openmarkov.core.model.network.EvidenceCase;
import org.openmarkov.core.model.network.Finding;
import org.openmarkov.core.model.network.Node;
import org.openmarkov.core.model.network.NodeType;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.ProbNetOperations;
import org.openmarkov.core.model.network.State;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.VariableType;
import org.openmarkov.core.model.network.potential.ExactDistrPotential;
import org.openmarkov.core.model.network.potential.GTablePotential;
import org.openmarkov.core.model.network.potential.Potential;
import org.openmarkov.core.model.network.potential.PotentialRole;
import org.openmarkov.core.model.network.potential.TablePotential;
import org.openmarkov.core.model.network.potential.operation.DiscretePotentialOperations;
import org.openmarkov.core.model.network.potential.treeadd.TreeADDBranch;
import org.openmarkov.core.model.network.potential.treeadd.TreeADDPotential;

public class DANOperations {

	/**
	 * Returns an instance of the network in which the variable has taken the value given by the state. The links between
	 * the instantiated variable (the node of this variable) and their children are removed and the potentials are projected
	 * for the given state of the variable. The instantiated variable and the links with their parents are maintained in
	 * the instantiated network.
	 *
	 * @param originalDAN original network
	 * @param variable    variable
	 * @param state       state of the variable
	 * @return instantiated network
	 */
	public static ProbNet instantiate(ProbNet originalDAN, Variable variable, State state) {
		ProbNet instantiatedNet = originalDAN.copy();

		Node originalNode = null;
		try {
			originalNode = originalDAN.getNode(variable.getName());
		} catch (NodeNotFoundException e) {
			e.printStackTrace();
		}

		// If we are instantiating a decision variable we must project potentials for the selected state of the decision
		if (originalNode != null /*&& originalNode.getNodeType().equals(NodeType.DECISION)*/) {
			try {
				projectPotentials(instantiatedNet, variable, state);
			} catch (InvalidStateException | IncompatibleEvidenceException | NonProjectablePotentialException | WrongCriterionException e) {
				e.printStackTrace();
			}
		}

		// Get the links of the node in the original network
		for (Link<Node> link : originalDAN.getLinks(originalNode)) {
			if (link.getNode1().equals(originalNode)) // Our originalNode is the source originalNode (act as parent)
			{
				Node destinationNode = instantiatedNet.getNode(link.getNode2().getVariable());
				// Remove link between restricting originalNode and restricted originalNode
				instantiatedNet.removeLink(link.getNode1().getVariable(), link.getNode2().getVariable(), true);

				if (destinationNode.getNodeType() == NodeType.CHANCE) {
					if (link.hasRevealingConditions()) {
						if (link.getRevealingStates().contains(state)) {
							List<Node> predecessorDecisions = ProbNetOperations
									.getPredecessorDecisions(destinationNode, instantiatedNet);
							// If it has predecessor decisions, do not reveal it yet, but add revealing links
							// from every predecessor decision to the originalNode
							if (predecessorDecisions.isEmpty()) {
								destinationNode.setAlwaysObserved(true);
							} else {
								for (Node predecessorDecision : predecessorDecisions) {
									Link<Node> revealingArc = instantiatedNet
											.addLink(predecessorDecision, destinationNode, true);
									State[] predecessorDecisionStates = predecessorDecision.getVariable().getStates();
									for (State predecessorDecisionState : predecessorDecisionStates)
										revealingArc.addRevealingState(predecessorDecisionState);
								}
							}
						}
					}
				}
				if (link.hasRestrictions()) {
					Variable destinationVariable = destinationNode.getVariable();
					State[] restrictedVariableStates = destinationVariable.getStates();
					List<State> nonRestrictedStates = ProbNetOperations
							.getUnrestrictedStates(link, restrictedVariableStates, state);

					if (nonRestrictedStates.isEmpty()) {
						// Remove destination originalNode and its descendants!
						Stack<Node> disposableNodes = new Stack<>();
						disposableNodes.push(destinationNode);
						while (!disposableNodes.isEmpty()) {
							Node disposableNode = disposableNodes.pop();
							// If it's a decision originalNode, check if there is another
							// path to it from another decision
							if (disposableNode.getNodeType() != NodeType.DECISION || !ProbNetOperations
									.hasPredecessorDecision(disposableNode, instantiatedNet)) {
								for (Node descendant : instantiatedNet.getChildren(disposableNode)) {
									disposableNodes.push(descendant);
								}
								// Guarantee that the DAN has at least a utility node
								if (disposableNode.getNodeType() != NodeType.UTILITY
										|| instantiatedNet.getNodes(NodeType.UTILITY).size() > 1) {
									instantiatedNet.removeNode(disposableNode);
								}
								else {//If the node of utility should be removed, then we leave it in the network with a zero value
									replaceUtilityPotentialWithZero(instantiatedNet, disposableNode);
								}
							}
						}

						//                    }else if(nonRestrictedStates.size () == 1) // Remove variables with a single variable
						//                    {
						//                        ProbNet probNetWithoutSingleStateVariable = probNetCopy.copy ();
						//                        probNetWithoutSingleStateVariable.removeNode (probNetWithoutSingleStateVariable.getNode (destinationNode.getVariable ()));
						//                        probNetCopy = applyRestrictionsAndReveal(probNetWithoutSingleStateVariable, destinationNode, nonRestrictedStates.get (0), originalProbNet);
					} else if (nonRestrictedStates.size() < restrictedVariableStates.length) {
						// At least one of the states of the destination originalNode is restricted.
						// Make a copy of the variable and remove the restricted states
						//if (destinationNode.getNodeType()!=NodeType.CHANCE){
						State[] unrestrictedStates = nonRestrictedStates.toArray(new State[0]);
						Variable restrictedVariable = new Variable(destinationVariable.getName(), unrestrictedStates);
						restrictedVariable.setVariableType(destinationVariable.getVariableType());
						updatePotentialsWithNewVariable(instantiatedNet, destinationVariable, restrictedVariable);
						destinationNode.setVariable(restrictedVariable);
						//}
					}
				}
			}
		}
		if (originalNode.getNodeType() == NodeType.DECISION) {
			instantiatedNet.removeNode(instantiatedNet.getNode(originalNode.getVariable()));
		}
		return instantiatedNet;
	}
	
	
	public static void replaceUtilityPotentialWithZero(ProbNet dan, Node node) {
		dan.removePotentials(node);
		TablePotential newUtilityPotential;
		newUtilityPotential = new TablePotential(Arrays.asList(node.getVariable()),PotentialRole.UNSPECIFIED);
		double values[] = new double[1];
		values[0] = 0.0;
		newUtilityPotential.setValues(values);
		dan.addPotential(newUtilityPotential);
	}

	
	private static void updatePotentialsWithNewVariable(ProbNet instantiatedNet, Variable oldVariable,
			Variable newVariable) {
		Hashtable<State, EvidenceCase> evidence = new Hashtable<>();
		for (State auxState : newVariable.getStates()) {
			EvidenceCase auxEvi = new EvidenceCase();
			try {
				auxEvi.addFinding(new Finding(oldVariable, auxState));
			} catch (InvalidStateException | IncompatibleEvidenceException e) {
				e.printStackTrace();
			}
			evidence.put(auxState, auxEvi);
		}
		List<Potential> oldPotentials = instantiatedNet.getPotentials(oldVariable);
		if (oldPotentials != null && oldPotentials.size() > 0) {
			for (Potential auxPotential : instantiatedNet.getPotentials(oldVariable)) {
				List<Variable> newVars = new ArrayList<>();
				for (Variable auxVar : auxPotential.getVariables()) {
					newVars.add(auxVar != oldVariable ? auxVar : newVariable);
				}
				Node node = instantiatedNet.getNode(auxPotential.getConditionedVariable());
				TreeADDPotential newPot = new TreeADDPotential(newVars, newVariable, auxPotential.getPotentialRole());
				newPot.setRootVariable(newVariable);
				List<TreeADDBranch> branches = new ArrayList<>();
				for (State auxState : newVariable.getStates()) {
					Potential auxPotBranch = null;
					EvidenceCase auxEvidence = evidence.get(auxState);
					try {
						auxPotBranch = (
								auxPotential instanceof TablePotential || auxPotential instanceof ExactDistrPotential
						) ? auxPotential.project(auxEvidence) : auxPotential.tableProject(auxEvidence, null).get(0);
					} catch (WrongCriterionException | NonProjectablePotentialException e) {
						e.printStackTrace();
					}
					branches.add(new TreeADDBranch(Arrays.asList(auxState), newVariable, auxPotBranch, newVars));
				}
				newPot.setBranches(branches);
				node.removePotential(auxPotential);
				node.setPotential(newPot);
			}
		}
	}

	/**
	 * Returns one instance of the network for each state of the variable.
	 * In each instance the variable has taken one of its possible states.
	 *
	 * @param originalDAN original network
	 * @param variable    variable
	 * @return list of instantiated networks
	 */
	public static List<ProbNet> instantiate(ProbNet originalDAN, Variable variable) {
		List<ProbNet> instantiatedNetworks = new ArrayList<>();
		for (State state : variable.getStates()) {
			instantiatedNetworks.add(instantiate(originalDAN, variable, state));
		}
		return instantiatedNetworks;
	}

	/**
	 * Draws links between decision variable and the other possible decisions (see getNextDecisions)
	 *
	 * @param probNet          original network
	 * @param decisionVariable priority decision
	 * @return Network with the decision prioritized
	 */
	public static ProbNet prioritize(ProbNet probNet, Variable decisionVariable) {
		ProbNet prioritizedNetwork = probNet.copy();
		try {
			List<Node> nextDecisions = getNextDecisions(prioritizedNetwork);
			Variable priorVariable = prioritizedNetwork.getVariable(decisionVariable.getName());

			for (Node decisionNode : nextDecisions) {
				Variable nextDecisionVariable = decisionNode.getVariable();
				if (nextDecisionVariable != priorVariable) {
					prioritizedNetwork.addLink(priorVariable, nextDecisionVariable, true);
				}
			}
		} catch (NodeNotFoundException e) {
			e.printStackTrace();
		}
		return prioritizedNetwork;
	}

	public static ProbNet prioritize(ProbNet probNet, Node node) {
		return prioritize(probNet, node.getVariable());
	}

	/**
	 * Get the decisions that could be made first
	 *
	 * @param probNet network
	 * @return list of possible next decisions
	 */
	public static List<Node> getNextDecisions(ProbNet probNet) {
		List<Node> decisionNodes = ProbNetOperations.getParentlessDecisions(probNet);
		// Check if the nodes revealed by a decision node are the subset of another
		// In that case we don't need to consider them as valid orders
		List<List<Node>> revealedNodes = new ArrayList<>();
		for (Node node : decisionNodes) {
			List<Node> revealedByDecision = new ArrayList<>();
			for (Link<Node> link : node.getLinks()) {
				if (link.getNode1().equals(node) && link.hasRevealingConditions()) {
					revealedByDecision.add(link.getNode2());
				}
			}
			revealedNodes.add(revealedByDecision);
		}
		List<Node> dominatedDecisions = new ArrayList<>();
		for (int i = 0; i < decisionNodes.size(); ++i) {
			Node nodeA = decisionNodes.get(i);
			if (!revealedNodes.get(i).isEmpty()) {
				for (int j = 0; j < decisionNodes.size(); ++j) {
					Node nodeB = decisionNodes.get(j);
					if (nodeA != nodeB && revealedNodes.get(i).containsAll(revealedNodes.get(j)))
						dominatedDecisions.add(nodeB);
				}
			}
		}
		decisionNodes.removeAll(dominatedDecisions);

		return decisionNodes;
	}

	/**
	 * Get the decisions that could be made first
	 *
	 * @param probNet network
	 * @return list of possible next decisions
	 */
	public static List<Node> getNextDecisions(ProbNet probNet, EvidenceCase evidence) {
		List<Node> decisionNodes = ProbNetOperations.getParentlessDecisions(probNet);
		if (evidence != null) {
			List<Variable> eviVariables = evidence.getVariables();
			if (eviVariables != null) {
				for (Variable var : evidence.getVariables()) {
					try {
						decisionNodes.remove(probNet.getNode(var.getName()));
					} catch (NodeNotFoundException e) {
						e.printStackTrace();
					}
				}
			}
		}

		// Check if the nodes revealed by a decision node are the subset of another
		// In that case we don't need to consider them as valid orders
		List<List<Node>> revealedNodes = new ArrayList<>();
		for (Node node : decisionNodes) {
			List<Node> revealedByDecision = new ArrayList<>();
			for (Link<Node> link : node.getLinks()) {
				if (link.getNode1().equals(node) && link.hasRevealingConditions()) {
					revealedByDecision.add(link.getNode2());
				}
			}
			revealedNodes.add(revealedByDecision);
		}
		List<Node> dominatedDecisions = new ArrayList<>();
		for (int i = 0; i < decisionNodes.size(); ++i) {
			Node nodeA = decisionNodes.get(i);
			if (!revealedNodes.get(i).isEmpty()) {
				for (int j = 0; j < decisionNodes.size(); ++j) {
					Node nodeB = decisionNodes.get(j);
					if (nodeA != nodeB && revealedNodes.get(i).containsAll(revealedNodes.get(j)))
						dominatedDecisions.add(nodeB);
				}
			}
		}
		decisionNodes.removeAll(dominatedDecisions);

		return decisionNodes;
	}

	/**
	 * Project potentials where this variable appears.
	 *
	 * @param probNet  network
	 * @param variable decision variable
	 * @param state    state of the decision
	 * @throws InvalidStateException
	 * @throws IncompatibleEvidenceException
	 * @throws NonProjectablePotentialException
	 * @throws WrongCriterionException
	 */
	private static void projectPotentials(ProbNet probNet, Variable variable, State state)
			throws InvalidStateException, IncompatibleEvidenceException, NonProjectablePotentialException,
			WrongCriterionException {

		EvidenceCase decisionEvidence = new EvidenceCase();
		decisionEvidence.addFinding(new Finding(variable, state));
		List<Potential> nodePotentials = probNet.getNode(variable).getPotentials();
		Potential nodePotential;

		if (nodePotentials != null && nodePotentials.size() > 0) {
			nodePotential = nodePotentials.get(0);
		} else {
			nodePotential = null;
		}

		for (Potential potential : probNet.getPotentials(variable)) {
			// Project all potentials except the potential of the node
			if (nodePotential == null || (!nodePotential.equals(potential))) {
				Variable conditionedVariable = potential.getConditionedVariable();
				Node node = probNet.getNode(conditionedVariable);
				if (potential instanceof TablePotential || potential instanceof ExactDistrPotential) {
					// Maintain the type of potential.
					node.setPotential(potential.project(decisionEvidence));
				} else {
					//We ensure here that the first variable of a utility potential is the variable of the utility node.
					//This condition is necessary to correctly evaluate the network with VariableEliminationCore
					List<TablePotential> projectedPotentials = potential.tableProject(decisionEvidence, null);
					TablePotential potentialProjectedFromTreeADD = projectedPotentials.get(0);
					Potential newPotential;
					Node newConditionedVariable = probNet
							.getNode(potentialProjectedFromTreeADD.getConditionedVariable());
					NodeType utilityType = NodeType.UTILITY;
					if (((newConditionedVariable == null) || (newConditionedVariable.getNodeType() != utilityType)) && (
							node.getNodeType() == utilityType
					)) {
						List<Variable> newVarsPotential = new ArrayList<>();
						newVarsPotential.add(conditionedVariable);
						newVarsPotential.addAll(potentialProjectedFromTreeADD.getVariables());
						newPotential = new ExactDistrPotential(newVarsPotential, PotentialRole.UNSPECIFIED,
								potentialProjectedFromTreeADD.values);
					} else {
						newPotential = potentialProjectedFromTreeADD;
					}
					node.setPotential(newPotential);
				}
			}
		}
	}

	/**
	 * Returns true if the probNet is symmetric.
	 *
	 * @param probNet network
	 */
	public static boolean isSymmetric(ProbNet probNet, EvidenceCase evidence) {
		boolean hasStrucAsymm = ProbNetOperations.hasStructuralAsymmetry(probNet);
		boolean hasOrderAsymm = ProbNetOperations
				.hasOrderAsymmetry(probNet, (evidence != null) ? evidence.getVariables() : null);
		return !(hasStrucAsymm || hasOrderAsymm);
	}

	/**
	 * @param probNet
	 * @return List of always observed variables
	 */
	public static List<Variable> getAlwaysObservedVariables(ProbNet probNet) {
		List<Variable> alwaysObservedVariables = new ArrayList<>();
		for (Node node : probNet.getNodes()) {
			if (node.isAlwaysObserved()) {
				alwaysObservedVariables.add(node.getVariable());
			}
		}
		return alwaysObservedVariables;
	}

	/**
	 * @param probNet
	 * @return List of asymmetric observed variables (The A's variables given
	 * that the link A-&gt;B has restrictions)
	 */
	public static List<Variable> getAsymmetricObservableVariables(ProbNet probNet) {
		List<Variable> asymetricObservableVariables = new ArrayList<>();
		for (Link<Node> link : probNet.getLinks()) {
			Node parentNode = link.getNode1();
			if (parentNode.isAlwaysObserved() && link.hasRestrictions()) {
				asymetricObservableVariables.add(parentNode.getVariable());
			}
		}
		return asymetricObservableVariables;
	}

	/**
	 * @param list1 list of variables
	 * @param list2 list of variables
	 * @return Join two list of variables, without duplicates
	 */
	public static List<Variable> join(List<Variable> list1, List<Variable> list2) {
		List<Variable> result;
		result = new ArrayList<>();
		if (!isEmpty(list1)) {
			result.addAll(list1);
		}
		if (!isEmpty(list2)) {
			for (Variable var : list2) {
				if (!result.contains(var)) {
					result.add(var);
				}
			}
		}
		return result;
	}
	
	private static boolean isEmpty(List<Variable> list) {
		return ((list == null) || (list.size() == 0));
	}

	/**
	 * @param variables
	 * @param dan
	 * @return A variable of 'variables' of 'dan' that has no ancestors belonging to 'variables'
	 */
	static Variable selectVariableWithoutAncestorsInVariables(List<Variable> variables, ProbNet dan) {
		boolean withoutAncestors = true;
		boolean selected = false;
		Variable variableSelected = null;
		for (int i = 0; i < variables.size() && !selected; i++) {
			Variable candidate = variables.get(i);
			Set<Node> ancestors = ProbNetOperations.getNodeAncestors(dan.getNode(candidate));
			Set<Variable> ancestorsVariables = new HashSet<>();
			ancestors.forEach(node -> ancestorsVariables.add(node.getVariable()));
			for (int j = 0; i < variables.size(); i++) {
				Variable auxVar = variables.get(j);
				if (auxVar != candidate) {
					withoutAncestors = !ancestorsVariables.contains(auxVar);
				}
			}
			if (withoutAncestors) {
				selected = true;
				variableSelected = candidate;
			}
		}
		return variableSelected;
	}

	static EvidenceCase extendEvidenceCase(EvidenceCase evidenceCase, Variable x, State state)
			throws InvalidStateException, IncompatibleEvidenceException {
		EvidenceCase newEvi;
		newEvi = new EvidenceCase(evidenceCase);
		newEvi.addFinding(new Finding(x, state));
		return newEvi;
	}

	public static TablePotential sumUtilityPotentials(ProbNet dan, List<TablePotential> potentials) {
		List<TablePotential> utilPotentials = new ArrayList<>();

		for (int i = 0; i < potentials.size(); i++) {
			TablePotential aux = potentials.get(i);
			if (aux.isAdditive()) {
				utilPotentials.add(aux);
			}
		}
		TablePotential result = DiscretePotentialOperations.sum(utilPotentials);
		if (result == null) {
			result = DiscretePotentialOperations.createZeroUtilityPotential(dan);
		}
		return result;
	}

	
/*	public static TablePotential firstUtilityPotential(ProbNet dan,List<TablePotential> potentials) {
		TablePotential result = getUtilityPotential(potentials);
		if (result == null) {
			if (potentials.size()>0){
				result = potentials.get(0);
			}
			else{
				result = createZeroUtilityPotential(dan);
			}
		}
		return result;
	}*/

	public static Variable createDummyVariableOfOrder(List<Node> nodes) {
		Variable dummyVariable = new Variable("OD");
		dummyVariable.setVariableType(VariableType.FINITE_STATES);
		State statesDummyVariable[] = new State[nodes.size()];
		for (int i = 0; i < nodes.size(); i++) {
			statesDummyVariable[i] = new State(nodes.get(i).getName());
		}
		dummyVariable.setStates(statesDummyVariable);
		return dummyVariable;

	}

		
	
	
	
	/**
	 * @param dan
	 * @param conditioningVariablesList
	 * @param evidenceCase
	 * @param isDAN if true is DAN, otherwise is ID
	 * @return the list variables observed from the beginning
	 */
	public static List<Variable> getVariablesObservedFromTheBegginning(ProbNet dan, List<Variable> conditioningVariablesList,
			EvidenceCase evidenceCase,boolean isDAN){
		List<Variable> variablesObservedFromBeginning = isDAN? DANOperations.getAlwaysObservedVariables(dan):
			DANOperations.getVariablesObservedByFirstDecision(dan);
		if (conditioningVariablesList != null) {
			variablesObservedFromBeginning.removeAll(conditioningVariablesList);
		}
		if (evidenceCase != null) {
			for (Variable variable : evidenceCase.getVariables()) {
				Variable variableInDAN = getVariableIfAppearsWithSameNameInNetwork(dan, variable);
				if (variableInDAN != null) {
					variablesObservedFromBeginning.remove(variableInDAN);
				}
			}
		}
		return variablesObservedFromBeginning;
	}
	

	public static List<Variable> getVariablesObservedByFirstDecision(ProbNet dan) {
		ProbNet idCopy = dan.copy(); // Copy influence diagram
		Stack<Variable> decisions = BasicOperations.getSequenceOfDecisions(idCopy);		
		List<Variable> decisionsList = new ArrayList<>(decisions);
		Collections.reverse(decisionsList);
		Variable decision = decisionsList.get(0);
		Node decisionNode = dan.getNode(decision);
		// Get nodes of the decision parents
		List<Node> parentDecisionNodes = dan.getParents(decisionNode);
		List<Variable> observed = new ArrayList<>();
		parentDecisionNodes.forEach(parent -> observed.add(parent.getVariable()));
		return observed;		
	}
	
	
	private static Variable getVariableIfAppearsWithSameNameInNetwork(ProbNet network, Variable variable) {
		Variable variableInNetwork = null;
		String variableName = variable.getName();
		if (network.containsVariable(variableName)) {
			try {
				variableInNetwork = network.getVariable(variableName);
			} catch (NodeNotFoundException e) {
				e.printStackTrace();
			}
		}
		return variableInNetwork;
		
	}
	
	
	

	public static List<Variable> getChanceVariablesNotInEvidence(ProbNet dan, EvidenceCase evidenceCase) {
		List<Variable> variables = DANOperations.getChanceVariables(dan);
		if (evidenceCase != null) {
			for (Variable variable : evidenceCase.getVariables()) {
				Variable variableInDAN = getVariableIfAppearsWithSameNameInNetwork(dan, variable);
				if (variableInDAN != null) {
					variables.remove(variableInDAN);
				}
			}
		}
		return variables;
	}

	public static List<Variable> getChanceVariables(ProbNet dan) {
		List<Variable> variables = new ArrayList<>();
		List<Node> nodes = dan.getNodes();
		for (Node node : nodes) {
			if (node.getNodeType() == NodeType.CHANCE) {
				variables.add(node.getVariable());
			}
		}
		return variables;
	}

	/**
	 * @param network
	 * @return Transforms an evidence case to the set of variables in network. This method is necessary in case the evidence
	 * of 'evidenceCase' has variables whose names appear in 'network', but in different Variable objects.
	 * @throws IncompatibleEvidenceException
	 */
	static EvidenceCase translateEvidenceTo(ProbNet network, EvidenceCase evidenceCase) throws Exception {
		EvidenceCase newEvidenceCase = new EvidenceCase();
		if (evidenceCase != null) {
			for (Finding finding : evidenceCase.getFindings()) {
				Variable newVariable;
				try {
					newVariable = getVariableIfAppearsWithSameNameInNetwork(network,finding.getVariable());
					//newVariable = network.getVariable(finding.getVariable().getName());
					if (newVariable!=null) {
						State state = newVariable.getState(finding.getState());
						Finding newFinding = new Finding(newVariable, state);
						newEvidenceCase.addFinding(newFinding);
					}
				} catch (InvalidStateException | IncompatibleEvidenceException e) {
					throw new IncompatibleEvidenceException(null);
				}

			}
		} else {
			newEvidenceCase = null;
		}
		return newEvidenceCase;

	}
	
	public static double getOnlyValuePotential(TablePotential potential) {
		return potential.getFirstValue();
	}
	
	
	public static CEP getOnlyValuePotentialCEP(GTablePotential potential) {
		return (CEP) potential.elementTable.get(0);
	}


}