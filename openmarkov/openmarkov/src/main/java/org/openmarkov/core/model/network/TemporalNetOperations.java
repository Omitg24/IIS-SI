/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.model.network;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openmarkov.core.exception.ConstraintViolationException;
import org.openmarkov.core.exception.NodeNotFoundException;
import org.openmarkov.core.inference.TransitionTime;
import org.openmarkov.core.model.network.potential.CycleLengthShift;
import org.openmarkov.core.model.network.potential.ExactDistrPotential;
import org.openmarkov.core.model.network.potential.Potential;
import org.openmarkov.core.model.network.potential.PotentialRole;
import org.openmarkov.core.model.network.potential.SameAsPrevious;
import org.openmarkov.core.model.network.potential.TablePotential;
import org.openmarkov.core.model.network.potential.operation.DiscretePotentialOperations;
import org.openmarkov.core.model.network.type.InfluenceDiagramType;

public class TemporalNetOperations {

	// Attributes
	/**
	 * Vertical separation in pixels between slices.
	 */
	private static final double VERTICAL_OFFSET = 0;

	/**
	 * Horizontal separation between slices
	 */
	private static final double MARGIN_BETWEEN_SLICES = 150;

	/**
	 * When invoking this method, probNet is a copy of the concise net. We add
	 * new nodes, links, and potentials to make it a compact net.
	 * If some of the slices of the concise net misses a node present in previous slices,
	 * adds the node to that slice
	 * @param probNet Network
	 * @return Compact network
	 */
	public static List<List<Node>> compactNetwork(ProbNet probNet) {
		List<List<Node>> classifiedNodes = classifyNodesbySlices(probNet, probNet.getVariables());
		// generate the new nodes of the compact net
		List<Node> generatingNodes = new ArrayList<>();
		List<Node> generatedNodes = new ArrayList<>();
		for (int slice = 0; slice < classifiedNodes.size() - 1; slice++) {
			double sliceWidth = getSliceWidth(classifiedNodes.get(slice));
			List<Node> generatedNodesInThisSlice = new ArrayList<>(classifiedNodes.get(slice).size());
			for (Node generatingNode : classifiedNodes.get(slice)) {
				if (!probNet.containsShiftedVariable(generatingNode.getVariable(), 1)) {
					Node newNode = probNet
							.addShiftedNode(generatingNode, 1, sliceWidth + MARGIN_BETWEEN_SLICES, VERTICAL_OFFSET);
					generatingNodes.add(generatingNode);
					generatedNodes.add(newNode);
					generatedNodesInThisSlice.add(newNode);
				} else {
					// Replace all SameAsPrevious potentials
					try {
						Variable variable = probNet.getShiftedVariable(generatingNode.getVariable(), 1);
						Node node = probNet.getNode(variable);
						if (!node.getPotentials().isEmpty() && node.getPotentials().get(0) instanceof SameAsPrevious) {
							Potential newPotential = ((SameAsPrevious) node.getPotentials().get(0))
									.getOriginalPotential(probNet).copy();
							newPotential.shift(probNet,
									variable.getTimeSlice() - newPotential.getConditionedVariable().getTimeSlice());
							node.setPotential(newPotential);
						}

					} catch (NodeNotFoundException e) {
						e.printStackTrace();
					}
				}
			}
			for (Node node : generatedNodesInThisSlice) {
				classifiedNodes.get(node.getVariable().getTimeSlice()).add(node);
			}
		}
		// assign potentials to the new nodes of the compact net
		Node generatingNode, generatedNode;
		for (int i = 0; i < generatedNodes.size(); i++) {
			generatingNode = generatingNodes.get(i);
			generatedNode = generatedNodes.get(i);
			try {
				expandPotentialAndLinks(probNet, generatingNode, generatedNode, 1);
			} catch (NodeNotFoundException e) {
				// If we get here is because we have not generated the nodes as
				// we should
				e.printStackTrace();
			}
		}
		return classifiedNodes;
	}

	/**
	 * @param probNet Original network
	 * @return expanded network
	 */
	public static ProbNet expandNetwork(ProbNet probNet) {
		ProbNet expandedNet = probNet.copy();
		List<List<Node>> classifiedNodes = compactNetwork(expandedNet);
		while (classifiedNodes.size() <= probNet.getInferenceOptions().getTemporalOptions().getHorizon()) {
			generateNextSlice(expandedNet, classifiedNodes);
		}
		return expandedNet;
	}

	/**
	 * Assigns nodes to slices in a collection of slices. Each slice is a
	 * collection of nodes.
	 *
	 * @return {@code List} of {@code List} of {@code Node}
	 */
	private static List<List<Node>> classifyNodesbySlices(ProbNet probNet, List<Variable> variables) {
		List<List<Node>> classifiedNodes;
		int firstSliceIndex = Integer.MAX_VALUE;
		int lastSliceIndex = Integer.MIN_VALUE;
		// find the indexes of the first and last slice
		int timeSlice;
		for (Variable variable : variables) {
			if (variable.isTemporal()) {
				timeSlice = variable.getTimeSlice();
				if (timeSlice < firstSliceIndex) {
					firstSliceIndex = timeSlice;
				}
				if (timeSlice > lastSliceIndex) {
					lastSliceIndex = timeSlice;
				}
			}
		}
		int numSlices = lastSliceIndex - firstSliceIndex + 1;
		// initializes the variable classifiedNodes
		classifiedNodes = new ArrayList<>(numSlices);
		for (int slice = 0; slice < numSlices; slice++) {
			classifiedNodes.add(new ArrayList<Node>());
		}
		// assigns each node to its slice
		Variable variable;
		for (Node node : probNet.getNodes()) {
			variable = node.getVariable();
			if (variable.isTemporal()) {
				classifiedNodes.get(variable.getTimeSlice()).add(node);
			}
		}
		return classifiedNodes;
	}

	/**
	 * Condition: extendedNet in this class must be a compact net
	 */
	private static void generateNextSlice(ProbNet probNet, List<List<Node>> classifiedNodes) {
		List<Node> lastSliceNodes = classifiedNodes.get(classifiedNodes.size() - 1);
		List<Node> newSliceNodes = new ArrayList<>();
		// generates the new nodes
		double sliceWidth = getSliceWidth(lastSliceNodes);
		for (Node generatingNode : lastSliceNodes) {
			Node newNode = probNet
					.addShiftedNode(generatingNode, 1, sliceWidth + MARGIN_BETWEEN_SLICES, VERTICAL_OFFSET);
			newSliceNodes.add(newNode);
		}
		// generates new slices
		// assign potentials to the new nodes
		Node generatingNode, generatedNode;
		for (int i = 0; i < lastSliceNodes.size(); i++) {
			generatingNode = lastSliceNodes.get(i);
			generatedNode = newSliceNodes.get(i);
			try {
				expandPotentialAndLinks(probNet, generatingNode, generatedNode, 1);
			} catch (NodeNotFoundException e) {
				// If we get here is because we have not generated the nodes as
				// we should
				e.printStackTrace();
			}
		}
		classifiedNodes.add(newSliceNodes);
	}

	/**
	 * TODO document: oldNode is a node in the last slice of the compact net
	 * TODO We are assuming that there is only one potential per node. Revise
	 *
	 * @throws NodeNotFoundException NodeNotFoundException
	 */
	private static void expandPotentialAndLinks(ProbNet probNet, Node oldNode, Node newNode, int timeDifference)
			throws NodeNotFoundException {

		Potential oldPotential = null;
		// If there is a node that not have any potential, skip
		// TODO This code is skipping decision nodes as they have no potentials
		if (oldNode.getPotentials() != null && !oldNode.getPotentials().isEmpty()) {
			oldPotential = oldNode.getPotentials().get(0);

			Potential newPotential;
			if (oldPotential instanceof CycleLengthShift) {
				newPotential = new CycleLengthShift(oldPotential.getShiftedVariables(probNet, timeDifference),
						probNet.getCycleLength());
			} else {
				newPotential = oldPotential.copy();
				newPotential.shift(probNet, timeDifference);
			}
			newNode.addPotential(newPotential);
			newPotential.createDirectedLinks(probNet);
		} else if (oldNode.getPotentials().isEmpty() && oldNode.getNodeType() == NodeType.DECISION) {
		    // Create a blueprint potential
            List<Variable> variables = new ArrayList<>();
            variables.add(oldNode.getVariable());
            for (Node parent : oldNode.getParents()) {
                variables.add(parent.getVariable());
            }
            TablePotential blueprint = new TablePotential(variables, PotentialRole.POLICY);
            blueprint.shift(probNet, timeDifference);

            // Use the blueprint to create the new links
            blueprint.createDirectedLinks(probNet);
            System.out.println("End of decision links copied");
        }
	}

	private static double getSliceWidth(List<Node> nodes) {
		double minX = Double.POSITIVE_INFINITY;
		double maxX = 0.0;
		for (Node node : nodes) {
			if (node.getCoordinateX() > maxX) {
				maxX = node.getCoordinateX();
			}
			if (node.getCoordinateX() < minX) {
				minX = node.getCoordinateX();
			}
		}
		return maxX - minX;
	}

	/**
	 * Method that receives a node and retrieves all the nodes related to it that belong to other time slices
	 *
	 * @param node Node of reference
	 * @return a list with the nodes that belong to other time slices. Null if there no nodes related to other
	 * time slices or if the received node is not 'temporal'
	 */
	public static List<Node> getRelatedNodesOtherTimeSlices(Node node) {
		// We define the list that will be returned
		List<Variable> listOfRelatedVariables = null;
		try {
			// The node can have related variables only if its variable is temporal
			if (node.getProbNet().getVariable(node.getName()).isTemporal()) {
				// If so, we retrieve all the variables of the network as potentially
				// all of the can be related to the node
				listOfRelatedVariables = new ArrayList<>(node.getProbNet().getVariables());
				// and we create a list to store all those variables that are not related to the node
				List<Variable> listOfNotRelatedVariables = new ArrayList<>();
				// we add to this list the variable of the node itself
				listOfNotRelatedVariables.add(node.getVariable());
				// we store the name of the node
				String nodeName = node.getVariable().getBaseName();
				// and then we go through all the potential variables
				for (Variable variable : listOfRelatedVariables) {
					// if the variable being studied is not temporal and does not share its base name with the node
					if (!(variable.isTemporal() && variable.getBaseName().compareTo(nodeName) == 0)) {
						// it is removed from the list of related variables
						listOfNotRelatedVariables.add(variable);
					}
				}
				// From the potential list we remove all the variables that are not related to the variable of the node
				listOfRelatedVariables.removeAll(listOfNotRelatedVariables);
				// if the list is empty, the node has no related variables and we reset the list as null
				if (listOfRelatedVariables.size() == 0) {
					listOfRelatedVariables = null;
				}
			}
		} catch (NodeNotFoundException e) {
			e.printStackTrace();
		}
		// The nodes of the variables remaining in the list are returned, if any
		if (listOfRelatedVariables != null) {
			return node.getProbNet().getNodes(listOfRelatedVariables);
		} else {
			return null;
		}
	}

	/**
	 * Applies the changes necessary to indicate the time at which the transition of states
	 * in temporary networks is performed.
	 *
	 * @param network Network to be transformed
	 */
	public static void applyTransitionTime(ProbNet network) {
		int numSlices = network.getInferenceOptions().getTemporalOptions().getHorizon();
		List<Node> utilityNodes = network.getNodes(NodeType.UTILITY);
		TransitionTime transitionTime = network.getInferenceOptions().getTemporalOptions().getTransition();
		List<Node> nodesToRemove = new ArrayList<>();
		if (transitionTime == TransitionTime.HALF) {
			// Half cycle correction
			Map<String, Node[]> temporalNodes = new HashMap<>();
			for (Node utilityNode : utilityNodes) {
				Variable utilityVariable = utilityNode.getVariable();
				if (utilityVariable.isTemporal() && utilityVariable.getTimeSlice() > 0 && utilityVariable
						.getDecisionCriterion().getCriterionName().equalsIgnoreCase("effectiveness")) {
					if (!temporalNodes.containsKey(utilityVariable.getBaseName()))
						temporalNodes.put(utilityVariable.getBaseName(), new Node[numSlices + 1]);
					temporalNodes.get(utilityVariable.getBaseName())[utilityVariable.getTimeSlice()] = utilityNode;
				}
			}
			for (Node[] tempNodes : temporalNodes.values()) {
				for (int k = tempNodes.length - 1; k > 0; --k) {
					if (tempNodes[k] != null && tempNodes[k - 1] != null) {
						Node utilityNode = tempNodes[k];
						Node previousCycleNode = tempNodes[k - 1];
						List<Potential> currentCyclePotentials = utilityNode.getPotentials();
						List<Potential> previousCyclePotentials = previousCycleNode.getPotentials();
						List<Potential> newPotentials = new ArrayList<>();
						for (int i = 0; i < utilityNode.getNumPotentials(); ++i) {
// CMI
// Supposing potentials of utility nodes are "tables";
// currently (01/01/2019) "tables" in these nodes are coded using ExactDistrPotential but this will be changed soon
// TODO update this method (applyTransitionTime) when the new TablePotential is finished

//							TablePotential currentCyclePotential = (TablePotential) currentCyclePotentials.get(i);
//							TablePotential previousCyclePotential = (TablePotential) previousCyclePotentials.get(i);
							TablePotential currentCyclePotential = (TablePotential) ((ExactDistrPotential)currentCyclePotentials.get(i)).getTablePotential();
							TablePotential previousCyclePotential = (TablePotential) ((ExactDistrPotential)previousCyclePotentials.get(i)).getTablePotential();
// CMF
							TablePotential sumPotential = DiscretePotentialOperations
									.sum(Arrays.asList(currentCyclePotential, previousCyclePotential));
							for (int j = 0; j < sumPotential.values.length; ++j)
								sumPotential.values[j] /= 2;
							newPotentials.add(sumPotential);
						}

						utilityNode.setPotentials(newPotentials);
						for (Node parent : previousCycleNode.getParents()) {
							network.addLink(parent, utilityNode, true);
						}

					}
				}
			}

		}
		if (transitionTime == TransitionTime.BEGINNING || transitionTime == TransitionTime.HALF) {
			// prune zero cycle utilities
			for (Node utilityNode : utilityNodes) {
				if (utilityNode.getVariable().getTimeSlice() == 0) {
					nodesToRemove.add(utilityNode);
				}
			}
		} else if (transitionTime == TransitionTime.END) {
			// Prune last cycle utilities
			for (Node utilityNode : utilityNodes) {
				if (utilityNode.getVariable().getTimeSlice() == numSlices) {
					nodesToRemove.add(utilityNode);
				}
			}
		}
		for (Node nodeToRemove : nodesToRemove) {
			Potential potential = nodeToRemove.getPotentials().get(0).deepCopy(network);
			potential.scalePotential(0);
			nodeToRemove.setPotential(potential);
			//			network.removeNode(nodeToRemove);
		}
	}

	/**
	 * Apply the discounts for all temporal utility nodes in the expanded network
	 *
	 * @param probNet Expanded network
	 */
	public static void applyDiscountToUtilityNodes(ProbNet probNet) {

		// Get the utility nodes of the expanded network
		List<Node> utilityExpandedNodes = probNet.getNodes(NodeType.UTILITY);
		//For each utility Node, applies the discount of its criterion
		for (Node utilityNode : utilityExpandedNodes) {
			Variable utilityVariable = utilityNode.getVariable();

			if (utilityVariable.isTemporal()) {
				// Get a deep copy of the potential. Deep copy re-links the potential to their variaible in the
				// probNet
				Potential potential = utilityNode.getPotentials().get(0).deepCopy(probNet);
				int timeSlice = utilityVariable.getTimeSlice();
				double discount = CycleLength.getTemporalAdjustedDiscount(probNet.getCycleLength().getUnit(),
						probNet.getCycleLength().getValue(), utilityVariable.getDecisionCriterion().getDiscountUnit(),
						utilityVariable.getDecisionCriterion().getDiscount());

				// Get the discount rate
				double discountRate = 1.0 / (Math.pow((1.0 + discount), timeSlice));

				// Scale the potential by the discount rate
				potential.scalePotential(discountRate);
				utilityNode.setPotential(potential);
			}
		}
	}

	//	/**
	//	 * TODO - This method is not working, the TreeADDPotential cannot be casted to a TablePotential
	//	 * @param expandedNetwork
	//	 * @param evidence
	//	 * @return
	//	 * @throws NotEvaluableNetworkException
	//	 */
	//	public static ProbNet adaptNetworkforCE(ProbNet expandedNetwork,
	//			EvidenceCase evidence) throws NotEvaluableNetworkException {
	//
	//		// Convert numeric variables
	//		expandedNetwork = ProbNetOperations.convertNumericalVariablesToFS(expandedNetwork, evidence);
	//
	//		// make all utility nodes of the expanded probNet children of the decision criteria node
	//		Variable decisionCriteriaVariable =
	//				new Variable("***CECriteria***", CECriterion.Cost.toString(), CECriterion.Effectiveness.toString());
	//		Node decisionCECriteriaNode = expandedNetwork.addNode(decisionCriteriaVariable, NodeType.DECISION);
	//
	//		for (Node utilityNode : BasicOperations.getTerminalUtilityNodes(expandedNetwork)) {
	//			expandedNetwork.addLink(decisionCECriteriaNode, utilityNode, true);
	//			List<Variable> newPotentialVariables = utilityNode.getPotentials().get(0).getVariables();
	//			newPotentialVariables.add(decisionCriteriaVariable);
	//			TablePotential oldTablePotential = null;
	//			try {
	//				oldTablePotential = utilityNode.getPotentials().get(0).getCPT();
	//			} catch (NonProjectablePotentialException | WrongCriterionException e) {
	//				// TODO Auto-generated catch block
	//				e.printStackTrace();
	//			}
	//			TablePotential decisionCEPotential = new TablePotential(newPotentialVariables, PotentialRole.UTILITY);
	//			double newValues [] = new double[decisionCEPotential.values.length];
	//
	//			int startPosition = 0;
	//			if(utilityNode.getVariable().getDecisionCriterion().getCECriterion() == CECriterion.Cost){
	//				startPosition = 0;
	//			}else{
	//				startPosition = oldTablePotential.getValues().length;
	//			}
	//
	//			for(int i = 0; i < oldTablePotential.getValues().length; i++){
	//				newValues[i + startPosition] = oldTablePotential.getValues()[i];
	//			}
	//			utilityNode.setPotential(decisionCEPotential);
	//
	//		}
	//
	//		return expandedNetwork;
	//	}

	public static void transformToID(ProbNet expandedNetwork) {
		for (Node node : expandedNetwork.getNodes()) {
			Variable variable = node.getVariable();
			if (variable.isTemporal()) {
				variable.setName(variable.getBaseName() + " |" + variable.getTimeSlice() + "|");
				variable.setTimeSlice(Variable.noTemporalTimeSlice);
			}
		}
		try {
			expandedNetwork.setNetworkType(InfluenceDiagramType.getUniqueInstance());
		} catch (ConstraintViolationException e) {
			e.printStackTrace();
		}

	}

	//	 TODO - ¿Unused method?
	//	/**
	//	 * Applies a discount to a utility potential with uncertainty
	//	 * @param potential Potential to be discounted
	//	 * @param timeSlice Time slice in which the potential is
	//	 * @param discount Discount of the criterion
	//	 */
	//    public static void applyDiscountToUncertainPotential(Potential potential, int timeSlice, double discount) {
	//        double discountRate = 1.0 / (Math.pow((1.0 + (discount / 100.0)), timeSlice));
	//        if(potential instanceof TablePotential)
	//        {
	//            TablePotential tablePotential = ((TablePotential)potential);
	//            double[] potentialValues = tablePotential.getValues();
	//            if(tablePotential.getUncertainValues() != null)
	//            {
	//                UncertainValue[] uncertaintyTable = tablePotential.getUncertainValues();
	//                for(int j=0; j < uncertaintyTable.length; ++j)
	//                {
	//                    if(uncertaintyTable[j] != null)
	//                    {
	//                        potentialValues[j] = potentialValues[j] * discountRate;
	//                    }
	//                }
	//            }
	//        }else if (potential instanceof TreeADDPotential)
	//        {
	//            TreeADDPotential treeADD = (TreeADDPotential)potential;
	//            for(TreeADDBranch branch : treeADD.getBranches())
	//            {
	//                applyDiscountToUncertainPotential(branch.getPotential(), timeSlice, discount);
	//            }
	//        }
	//    }

}
