/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.inference.variableElimination.tasks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.openmarkov.core.exception.IncompatibleEvidenceException;
import org.openmarkov.core.exception.InvalidStateException;
import org.openmarkov.core.exception.NodeNotFoundException;
import org.openmarkov.core.exception.NonProjectablePotentialException;
import org.openmarkov.core.exception.NormalizeNullVectorException;
import org.openmarkov.core.exception.NotEvaluableNetworkException;
import org.openmarkov.core.exception.UnexpectedInferenceException;
import org.openmarkov.core.exception.WrongCriterionException;
import org.openmarkov.core.inference.MulticriteriaOptions;
import org.openmarkov.core.inference.heuristic.EliminationHeuristic;
import org.openmarkov.core.inference.tasks.Propagation;
import org.openmarkov.core.inference.tasks.TaskUtilities;
import org.openmarkov.core.model.network.EvidenceCase;
import org.openmarkov.core.model.network.Finding;
import org.openmarkov.core.model.network.Node;
import org.openmarkov.core.model.network.NodeType;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.ProbNetOperations;
import org.openmarkov.core.model.network.State;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.constraint.OnlyAtemporalVariables;
import org.openmarkov.core.model.network.potential.DeltaPotential;
import org.openmarkov.core.model.network.potential.Potential;
import org.openmarkov.core.model.network.potential.PotentialRole;
import org.openmarkov.core.model.network.potential.TablePotential;
import org.openmarkov.core.model.network.potential.operation.DiscretePotentialOperations;
import org.openmarkov.inference.variableElimination.VariableEliminationCore;

/**
 * Task: propagation
 * This task returns the probability of each chance variable and the utility of each utility node.
 * <p>
 * Input: a symmetric network and a list of variables of interest.
 * Optional input: post-resolution evidence.
 * <p>
 * Output: a table for each utility or chance node
 *
 * @author mluque
 * @author fjdiez
 * @author marias
 * @author jperez-martin
 * @author artasom
 */

public class VEPropagation extends VariableElimination implements Propagation {

	// Attributes
	private VariableEliminationCore variableEliminationCore = null;

	private HashMap<Variable, TablePotential> posteriorValues;

	/**
	 * Evidence when the network has been resolved.
	 * In influence diagrams this is Luque and Diez's evidence.
	 */
	private EvidenceCase postResolutionEvidence;

	private List<Variable> variablesOfInterest;

	private HashMap<Variable,Potential> optimalPolicies;

	/**
	 * @param network Probabilistic network to be resolved
	 * @throws NotEvaluableNetworkException  Constructor
	 */
	public VEPropagation(ProbNet network) throws NotEvaluableNetworkException {
		super(network);
		probNet.getInferenceOptions().getMultiCriteriaOptions()
				.setMulticriteriaType(MulticriteriaOptions.Type.UNICRITERION);
	}

	public VEPropagation(ProbNet network, HashMap<Variable, Potential> optimalPolicies) throws NotEvaluableNetworkException {
		super(network);
		probNet.getInferenceOptions().getMultiCriteriaOptions()
				.setMulticriteriaType(MulticriteriaOptions.Type.UNICRITERION);
		this.optimalPolicies = optimalPolicies;
	}

	private void calculateOptimalPolicies(ProbNet probNet, EvidenceCase preResolutionEvidence, List<Node> decisionNodes)
			throws NotEvaluableNetworkException, IncompatibleEvidenceException, UnexpectedInferenceException {
		// If there are any remaining decision nodes in the network, they do not have imposed policies
		if (TaskUtilities.hasDecisionsWithoutImposedPolicy(probNet)) {
			VEEvaluation veEvaluation = new VEEvaluation(probNet);
			veEvaluation.setPreResolutionEvidence(preResolutionEvidence);

			// TODO - Remove
			for (Variable conditioningVariable : conditioningVariables) {
				if (probNet.getNode(conditioningVariable).getNodeType() == NodeType.DECISION) {
					decisionNodes.remove(probNet.getNode(conditioningVariable));
				}
			}
			veEvaluation.setConditioningVariables(getConditioningVariables());

			optimalPolicies = veEvaluation.getOptimalPolicies();

		} else {
			optimalPolicies = new HashMap<>();
		}
	}

	private void resolve() throws IncompatibleEvidenceException, NotEvaluableNetworkException {
//		LogManager.getLogger(getClass()).trace("Resolving VEPropagation");
		posteriorValues = new HashMap<>();
		boolean isTemporal = !probNet.hasConstraint(OnlyAtemporalVariables.class);
		List<Node> decisionNodes = probNet.getNodes(NodeType.DECISION);

		try {
			calculateOptimalPolicies(probNet, getPreResolutionEvidence(), decisionNodes);
		} catch (UnexpectedInferenceException e) {
			e.printStackTrace();
		}

		for (Node decisionNode : decisionNodes) {
			Potential policy = optimalPolicies.get(decisionNode.getVariable());
			if (policy != null) { // If the optimal policy is null here it is just because the decision node has a policy imposed by the user
				decisionNode.setPotential(policy);
			}
		}

		generalPreprocessing();
//		unicriterionPreprocess();
		// TODO - Implement: For each super-value node, create a new node whose parents are all chance or decision nodes
		exactAlgorithmsPreprocessing();
		probNet = TaskUtilities.extendPostResolutionEvidence(probNet, getPostResolutionEvidence());

		List<Variable> variablesOfInterestBelongingToEvidence = new ArrayList<>();
		EvidenceCase evidence = getAllEvidence();
		List<Variable> evidenceVariables = evidence.getVariables();

		if (variablesOfInterest != null) {
			for (Variable variableOfInterest : variablesOfInterest) {
				try {
					Variable variableOfInterestInProbnet = probNet.getVariable(variableOfInterest.getName());
					if (evidenceVariables.contains(variableOfInterestInProbnet)) {
						variablesOfInterestBelongingToEvidence.add(variableOfInterestInProbnet);
					} else {
						ProbNet preprocessedNetwork = pruneNetwork(probNet.copy(), variableOfInterest);
						ProbNet markovNetwork = TaskUtilities
								.projectTablesAndBuildMarkovDecisionNetwork(preprocessedNetwork, evidence);
						InvokeVariableEliminationCore(markovNetwork, evidence, variableOfInterest);
					}
				} catch (NodeNotFoundException e) {
					e.printStackTrace();
				}
			}
		}

		// We have to create a potential for each variable of interest that belongs to the evidence
		TablePotential probPotential = null;
		DeltaPotential deltaPotential;
		for (Variable variable : variablesOfInterestBelongingToEvidence) {
			deltaPotential = new DeltaPotential(Collections.singletonList(variable),
					PotentialRole.CONDITIONAL_PROBABILITY, new State(evidence.getFinding(variable).getState()));
			try {
				probPotential = deltaPotential.tableProject(new EvidenceCase(), null).get(0);
				probPotential.setPotentialRole(PotentialRole.CONDITIONAL_PROBABILITY);

			} catch (NonProjectablePotentialException | WrongCriterionException e) {
				e.printStackTrace();
			}
			try {
				posteriorValues.put(probNet.getVariable(variable.getName()), probPotential);
			} catch (NodeNotFoundException e) {
				e.printStackTrace();
			}
		}

	}

	// Methods

	private void InvokeVariableEliminationCore(ProbNet network, EvidenceCase evidence, Variable variableOfInterest)
			throws IncompatibleEvidenceException {
		// Build list of variables to eliminate
		List<Variable> variablesToEliminate = probNet.getChanceAndDecisionVariables();
		variablesToEliminate.remove(variableOfInterest);
		//TODO: eliminate the observable variables (DANs)

		// Create heuristic instance
		EliminationHeuristic heuristic = heuristicFactory(network, new ArrayList<Variable>(), evidence.getVariables(),
				getConditioningVariables(), variablesToEliminate);

		try {
			variableEliminationCore = new VariableEliminationCore(network, heuristic, true);
		} catch (UnexpectedInferenceException e) {
			e.printStackTrace();
		}

		TablePotential posteriorValue = null;
		if (probNet.getNode(variableOfInterest).getNodeType() == NodeType.UTILITY) {
			try {
				posteriorValue = variableEliminationCore.getUtility();
				if (posteriorValue == null) {
					posteriorValue = new TablePotential(Arrays.asList(variableOfInterest), PotentialRole.UNSPECIFIED);
				}
			} catch (UnexpectedInferenceException e) {
				e.printStackTrace();
			}
		} else {
			posteriorValue = variableEliminationCore.getProbability();
			try {
				if (posteriorValue != null) {
					if (posteriorValue.getVariables().get(0) != variableOfInterest) {
						// TODO - Comprobar este código
						List<Variable> oldOrderVariables = new ArrayList<>();
						oldOrderVariables.addAll(posteriorValue.getVariables());
						oldOrderVariables.remove(variableOfInterest);

						List<Variable> orderedVariables = new ArrayList<>();
						orderedVariables.add(variableOfInterest);
						orderedVariables.addAll(oldOrderVariables);

						posteriorValue = DiscretePotentialOperations.reorder(posteriorValue, orderedVariables);
					}
					// TODO - Realizar la normalización condicionada
					if (getConditioningVariables() == null || getConditioningVariables().isEmpty()) {
						DiscretePotentialOperations.normalize(posteriorValue);
					}
				}
			} catch (NormalizeNullVectorException e) {
				throw new IncompatibleEvidenceException("Incompatible Evidence");
			}
		}

		posteriorValues.put(variableOfInterest, posteriorValue);
	}

	/**
	 * @param preprocessedNetwork
	 * @param variableOfInterest
	 * @return
	 * @throws IncompatibleEvidenceException
	 */
	private ProbNet pruneNetwork(ProbNet preprocessedNetwork, Variable variableOfInterest)
			throws IncompatibleEvidenceException {
		//Prune all the nodes except the variable of interest and its ancestors (and the corresponding findings).
		List<Variable> variablesNotToBePruned = new ArrayList<>();
		variablesNotToBePruned.add(variableOfInterest);
		for (Node node : ProbNetOperations.getNodeAncestors(preprocessedNetwork.getNode(variableOfInterest))) {
			variablesNotToBePruned.add(node.getVariable());
		}
		for (Finding finding : getAllEvidence().getFindings()) {
			if (!variablesNotToBePruned.contains(finding.getVariable())) {
				variablesNotToBePruned.add(finding.getVariable());
			}
		}
		return ProbNetOperations.getPruned(preprocessedNetwork, variablesNotToBePruned, getAllEvidence());
	}

	@Override public HashMap<Variable, TablePotential> getPosteriorValues()
			throws IncompatibleEvidenceException, NotEvaluableNetworkException {
		if (posteriorValues == null) {
			resolve();
		}
		return posteriorValues;
	}

	public EvidenceCase getPostResolutionEvidence() {
		return postResolutionEvidence;
	}

	public void setPostResolutionEvidence(EvidenceCase postResolutionEvidence) {
		this.postResolutionEvidence = postResolutionEvidence;
	}

	public EvidenceCase getAllEvidence() throws IncompatibleEvidenceException {
		EvidenceCase evidence = new EvidenceCase(getPreResolutionEvidence());
		try {
			if (postResolutionEvidence != null) {
				evidence.addFindings(postResolutionEvidence.getFindings());
			}
		} catch (InvalidStateException e) {
			e.printStackTrace();
		}
		return evidence;
	}

	public List<Variable> getVariablesOfInterest() {
		return variablesOfInterest;
	}

	public void setVariablesOfInterest(List<Variable> variablesOfInterest) {
		this.variablesOfInterest = variablesOfInterest;
	}
}