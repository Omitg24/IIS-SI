/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.inference.tasks;

import java.util.ArrayList;
import java.util.List;

import org.openmarkov.core.exception.IncompatibleEvidenceException;
import org.openmarkov.core.exception.InvalidStateException;
import org.openmarkov.core.exception.NodeNotFoundException;
import org.openmarkov.core.exception.NonProjectablePotentialException;
import org.openmarkov.core.exception.WrongCriterionException;
import org.openmarkov.core.inference.BasicOperations;
import org.openmarkov.core.model.network.EvidenceCase;
import org.openmarkov.core.model.network.Node;
import org.openmarkov.core.model.network.NodeType;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.ProbNetOperations;
import org.openmarkov.core.model.network.TemporalNetOperations;
import org.openmarkov.core.model.network.UtilityOperations;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.constraint.OnlyChanceNodes;
import org.openmarkov.core.model.network.potential.Potential;
import org.openmarkov.core.model.network.potential.TablePotential;

/**
 * @author artasom
 */
public class TaskUtilities {

	public static ProbNet expandNetwork(ProbNet probNet, boolean isTemporal) {
		if (isTemporal) {
			return TemporalNetOperations.expandNetwork(probNet);
		} else {
			return probNet;
		}
	}

	public static ProbNet extendPreResolutionEvidence(ProbNet probNet, EvidenceCase preResolutionEvidence) {
		try {
			if (preResolutionEvidence != null) {
				preResolutionEvidence.extendEvidence(probNet);
			}
		} catch (IncompatibleEvidenceException | InvalidStateException | WrongCriterionException e) {
			e.printStackTrace();
		}
		return probNet;
	}

	public static ProbNet extendPostResolutionEvidence(ProbNet probNet, EvidenceCase postResolutionEvidence) {
		try {
			if (postResolutionEvidence != null) {
				postResolutionEvidence.extendEvidence(probNet);
			}
		} catch (IncompatibleEvidenceException | InvalidStateException | WrongCriterionException e) {
			e.printStackTrace();
		}
		return probNet;
	}

	// TODO: the imposed policies are already added, aren't they? The nodes must be
	// transformed to chance nodes. And the policies into conditional probabilities?
	public static ProbNet imposePolicies(ProbNet probNet) {
		if (!hasOnlyChanceNodes(probNet) && hasDecisions(probNet)) {
			replaceDecisionsWithPoliciesByChanceNodes(probNet, null);
		}
		return probNet;
	}

	public static ProbNet applyTransitionTime(ProbNet probNet, boolean isTemporal) {
		if (isTemporal) {
			TemporalNetOperations.applyTransitionTime(probNet);
		}
		return probNet;
	}

	public static ProbNet applyDiscounts(ProbNet probNet, boolean isTemporal) {
		if (isTemporal) {
			TemporalNetOperations.applyDiscountToUtilityNodes(probNet);
		}
		return probNet;
	}

	public static ProbNet scaleUtilitiesUnicriterion(ProbNet probNet) {
		// if
		// (probNet.getInferenceOptions().getMultiCriteriaOptions().getMulticriteriaType().
		// equals(MulticriteriaOptions.Type.UNICRITERION)) {
		UtilityOperations.transformToUnicriterion(probNet);
		// }
		return probNet;
	}

	public static ProbNet scaleUtilitiesCostEffectiveness(ProbNet probNet) {
		// if
		// (probNet.getInferenceOptions().getMultiCriteriaOptions().getMulticriteriaType().
		// equals(MulticriteriaOptions.Type.COST_EFFECTIVENESS)) {
		UtilityOperations.applyCEUtilityScaling(probNet);
		// }
		return probNet;
	}

	// public static ProbNet unscaleUtilitiesUnicriterion(ProbNet probNet) {
	// UtilityOperations.unicriterionUtilityUnscaling(probNet);
	// return probNet;
	// }
	//
	// public static ProbNet unscaleUtilitiesCostEffectiveness(ProbNet probNet) {
	// UtilityOperations.ceUtilityUnscaling(probNet);
	// return probNet;
	// }

	public static ProbNet discretizeNonObservedNumericVariables(ProbNet probNet, EvidenceCase preResolutionEvidence) {
		return ProbNetOperations.convertNumericalVariablesToFS(probNet, preResolutionEvidence);
	}

	public static ProbNet addPoliciesFromResolution(ProbNet probNet) {
		if (!hasOnlyChanceNodes(probNet) && hasDecisions(probNet)) {
			replaceDecisionsWithPoliciesByChanceNodes(probNet, null);
		}
		return probNet;
	}

	//TODO Do not delete next commented code as we are still debugging the transition from super-value nodes' concepts to numeric concepts
	/*
	 * @param probNet
	 * @param evidenceCase
	 * @return Remove super value nodes from probNet
	 *//*
		 * public static ProbNet removeSuperValueNodes(ProbNet probNet, EvidenceCase
		 * evidenceCase) { ProbNet probNetWithoutSV = probNet; if
		 * (!hasOnlyChanceNodes(probNetWithoutSV)) { probNetWithoutSV =
		 * BasicOperations.removeSuperValueNodes(probNet, evidenceCase, false, false,
		 * null); } return probNetWithoutSV; }
		 */

	/**
	 * @param probNet Network
	 * @param evidenceCase Evidence case
	 * @return Remove intermediate numeric nodes from probNet
	 */
	public static ProbNet absorbAllIntermediateNumericNodes(ProbNet probNet, EvidenceCase evidenceCase) {
		ProbNet resultNetwork = null;
		if (!hasOnlyChanceNodes(probNet)) {
			resultNetwork = BasicOperations.absorbAllIntermediateNumericNodes(probNet, evidenceCase);
		} else {
			return probNet;
		}
		return resultNetwork;
	}

	/**
	 * @param network Network
	 * @return boolean
	 */
	public static boolean hasDecisions(ProbNet network) {
		List<Node> decisionNodes = network.getNodes(NodeType.DECISION);
		return (decisionNodes != null && !decisionNodes.isEmpty());
	}
	
	
	public static boolean hasDecisionsWithoutImposedPolicy(ProbNet network) {
		boolean hasDecWithoutImposedPolicy;
		List<Node> decisionNodes = network.getNodes(NodeType.DECISION);
		hasDecWithoutImposedPolicy = (decisionNodes != null && !decisionNodes.isEmpty());
		hasDecWithoutImposedPolicy &= decisionNodes.stream().anyMatch(x -> !hasImposedPolicy(network, x.getVariable()));
		return hasDecWithoutImposedPolicy;
	}

	/**
	 * @param network A probabilistic network
	 * @return True if the network has only chance nodes.
	 */
	public static boolean hasOnlyChanceNodes(ProbNet network) {
		return network.hasConstraint(OnlyChanceNodes.class);
	}

	/**
	 * @param probNet                   Replaces decision nodes in 'probNet' by
	 *                                  chance nodes by using the corresponding
	 *                                  policies. In PRERESOLUTION phase only
	 *                                  imposed policies are used. In POSTRESOLUTION
	 *                                  phase both imposed and calculated policies
	 *                                  are used. Decision nodes in
	 *                                  'informationalPredecessors' are not changed.
	 * @param informationalPredecessors Informational predecessors
	 */
	private static void replaceDecisionsWithPoliciesByChanceNodes(ProbNet probNet,
			List<Variable> informationalPredecessors) {
		// Change decision nodes by chance nodes whose probability potential
		// is given by the corresponding policy
		List<Node> decisions = probNet.getNodes(NodeType.DECISION);
		for (Node decision : decisions) {
			Variable varDecision = decision.getVariable();

			if ((informationalPredecessors == null) || (!informationalPredecessors.contains(varDecision))) {

				Potential policy = getPolicy(probNet, varDecision);

				if (policy != null) {
					List<Node> childrenOfDecision = probNet.getNode(varDecision).getChildren();
					// Remove decision
					probNet.removeNode(decision);
					// Create a chance node for the same variable
					Node decisionNode = probNet.addNode(varDecision, NodeType.CHANCE);

					// Add the links to the children (chance) of decision node
					for (Node child : childrenOfDecision) {
						NodeType type = child.getNodeType();
						if (type == NodeType.CHANCE || type == NodeType.UTILITY) {
							try {
								probNet.addLink(varDecision, child.getVariable(), true);
							} catch (NodeNotFoundException e) {
								e.printStackTrace();
							}
						}
					}

					// Incoming Links for the variable
					List<Variable> domainPolicy = policy.getVariables();
					domainPolicy.remove(varDecision);
					for (Variable varInDomain : domainPolicy) {
						try {
							probNet.addLink(varInDomain, varDecision, true);
						} catch (NodeNotFoundException e) {
							e.printStackTrace();
						}
					}

					List<Potential> potentials = decisionNode.getPotentials();
					if (potentials != null) {
						for (Potential potential : potentials) {
							decisionNode.removePotential(potential);
						}
					}

					// Potential probability for the variable
					probNet.addPotential(policy);
				}
			}
		}
	}

	/**
	 * @param probNet Network
	 * @param decision Decision variable
	 * @return True if the decision has an imposed policy.
	 */
	public static boolean hasImposedPolicy(ProbNet probNet, Variable decision) {
		return (getPolicy(probNet, decision) != null);
	}

	/**
	 * @param probNet  a probNet
	 * @param decision a decision variable
	 * @return The imposed policy of the decision
	 */
	private static Potential getPolicy(ProbNet probNet, Variable decision) {
		Potential policy;

		Node decisionNode = probNet.getNode(decision);
		if (decisionNode == null) {
			policy = null;
		} else {
			List<Potential> potentials = decisionNode.getPotentials();
			if ((potentials == null) || (potentials.size() == 0)) {
				policy = null;
			} else {
				policy = potentials.get(0);
			}
		}
		return policy;
	}

	/**
	 * @param network Network
	 * @param evidence Evidence
	 * @return Projected network
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

}
