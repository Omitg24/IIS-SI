/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.inference.variableElimination.operation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;

import org.openmarkov.core.exception.CostEffectivenessException;
import org.openmarkov.core.exception.NodeNotFoundException;
import org.openmarkov.core.exception.NonProjectablePotentialException;
import org.openmarkov.core.exception.NotEvaluableNetworkException;
import org.openmarkov.core.exception.PotentialOperationException;
import org.openmarkov.core.exception.WrongCriterionException;
import org.openmarkov.core.model.network.CEP;
import org.openmarkov.core.model.network.Criterion;
import org.openmarkov.core.model.network.Criterion.CECriterion;
import org.openmarkov.core.model.network.Node;
import org.openmarkov.core.model.network.NodeType;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.State;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.VariableType;
import org.openmarkov.core.model.network.potential.GTablePotential;
import org.openmarkov.core.model.network.potential.Potential;
import org.openmarkov.core.model.network.potential.PotentialRole;
import org.openmarkov.core.model.network.potential.StrategyTree;
import org.openmarkov.core.model.network.potential.TablePotential;
import org.openmarkov.core.model.network.potential.operation.DiscretePotentialOperations;
import org.openmarkov.core.model.network.potential.treeadd.TreeADDBranch;

public class CEPotentialOperation {

	private final static int COST = 0;
	private final static int EFFECTIVENESS = 1;
	private final static int FIRST_VARIABLE_POSITION = 0;

	/**
	 * Receives an influence diagram with several utilities.
	 *
	 * @param markovDecisionNetwork <code>ProbNet</code>
	 */
	public static ProbNet getCostEffectivenessNetwork(ProbNet markovDecisionNetwork) {

		List<Node> utilityNodes = markovDecisionNetwork.getNodes(NodeType.UTILITY);
		List<Node> utilityNodesWithoutChildren = getUtilitiesWithoutChildren(utilityNodes);

		removeSuperValueNodes(
				CEBaseOperations.getNodesWithGivenCriterion(utilityNodesWithoutChildren, Criterion.CECriterion.Cost),
				markovDecisionNetwork);
		removeSuperValueNodes(CEBaseOperations
						.getNodesWithGivenCriterion(utilityNodesWithoutChildren, Criterion.CECriterion.Effectiveness),
				markovDecisionNetwork);

		return markovDecisionNetwork;
	}

	/**
	 * @param utilityNodes Utility nodes without parents
	 * @return A list of utility nodes without parents.
	 */
	public static List<Node> getUtilitiesWithoutChildren(List<Node> utilityNodes) {

		ArrayList<Node> utilityNodesWithoutChildren = new ArrayList<Node>();
		for (Node utilityNode : utilityNodes) {
			if (utilityNode.getNumChildren() == 0) {
				utilityNodesWithoutChildren.add(utilityNode);
			}
		}
		return utilityNodesWithoutChildren;
	}

	/**
	 * @param probNet
	 * @param isBiCriteria
	 */
	public static void removeSuperValueNodes(ProbNet probNet, boolean isBiCriteria) {
		List<Node> utilityNodes = probNet.getNodes(NodeType.UTILITY);
		List<Node> utilityNodesWithoutChildren = CEPotentialOperation.getUtilitiesWithoutChildren(utilityNodes);
		if (isBiCriteria) {
			removeSuperValueNodes(
					CEBaseOperations.getNodesWithGivenCriterion(utilityNodesWithoutChildren, CECriterion.Cost),
					probNet);
			removeSuperValueNodes(
					CEBaseOperations.getNodesWithGivenCriterion(utilityNodesWithoutChildren, CECriterion.Effectiveness),
					probNet);
		} else {
			removeSuperValueNodes(utilityNodesWithoutChildren, probNet);
		}
	}

	/**
	 * @param utilityNodes
	 * @param influenceDiagram
	 */
	public static void removeSuperValueNodes(List<Node> utilityNodes, ProbNet influenceDiagram) {

		for (Node utilityNode : utilityNodes) {
			if (utilityNode.isSuperValueNode()) {
				TablePotential potential;
				try {
					potential = utilityNode.getUtilityFunction();
					List<Potential> potentials = new ArrayList<Potential>();
					potentials.add(potential);
					utilityNode.setPotentials(potentials);
					removeUtilityParents(utilityNode, influenceDiagram);
					// add links between of new potential of supervalue nodes
					List<Variable> parentVariables = potential.getVariables();
					for (Variable variable : parentVariables) {
						Node node = influenceDiagram.getNode(variable);
						influenceDiagram.addLink(node, utilityNode, true);
					}
				} catch (NonProjectablePotentialException | WrongCriterionException e1) {
					e1.printStackTrace(); // Unreachable code
				}
			}
		}
	}

	/**
	 * Removes the utilities and links that are parents of the received Node.
	 *
	 * @param influenceDiagram
	 * @param node             <code>Node</code>
	 */
	private static void removeUtilityParents(Node node, ProbNet influenceDiagram) {
		List<Node> parentsNodes = node.getParents();
		for (Node parentNode : parentsNodes) {
			if (parentNode.getNodeType() == NodeType.UTILITY) {
				influenceDiagram.removeLink(parentNode, node, true);
				removeUtilityParents(parentNode, influenceDiagram);
				influenceDiagram.removeNode(parentNode);
			}
		}
	}

	/**
	 * Multiplies a <code>GTablePotential</code> of <code>CEPartitionPotential</code>
	 * and a <code>TablePotential</code>
	 *
	 * @param potential  <code>TablePotential</code>.
	 * @param gPotential <code>GTablePotential</code>.
	 * @return A <code>GTablePotential</code> of <code>CEPartitionPotential</code> with
	 * the same variables of the received <code>GTablePotential</code>,
	 * in the same order, at the beginning plus the variables of the
	 * <code>TablePotential</code> not contained in the
	 * <code>GTablePotential</code> at the end.
	 */
	@SuppressWarnings("unchecked") public static GTablePotential multiply(TablePotential potential,
			GTablePotential gPotential) {

		// Create returning potential
		ArrayList<Variable> gVariables = new ArrayList<Variable>(gPotential.getVariables());
		ArrayList<Variable> pVariables = new ArrayList<Variable>(potential.getVariables());
		HashSet<Variable> variablesSet = new LinkedHashSet<Variable>(gVariables);
		variablesSet.addAll(pVariables);
		ArrayList<Variable> variablesResult = new ArrayList<Variable>(variablesSet);

		int numVariablesResult = variablesResult.size();
		//		GTablePotential result = new GTablePotential(variablesResult, PotentialRole.UTILITY);
		GTablePotential result = new GTablePotential(variablesResult, PotentialRole.UNSPECIFIED);

		int[] accOffGPot = result.getAccumulatedOffsets(gVariables);
		int[] accOffPPot = result.getAccumulatedOffsets(pVariables);
		int[] resultCoordinate = new int[numVariablesResult];
		int[] resultDimensions = result.getDimensions();
		int gPosition = 0;
		int pPosition = 0;
		int incrementedVariable = 0;

		while (incrementedVariable != numVariablesResult) {
			// fill arrays with data to multiply
			CEP partition = (CEP) gPotential.elementTable.get(gPosition);
			double probability = potential.values[pPosition];
			partition.multiply(probability);
			result.elementTable.add(partition);
			// next iteration
			for (incrementedVariable = 0; incrementedVariable < numVariablesResult; incrementedVariable++) {
				resultCoordinate[incrementedVariable] = resultCoordinate[incrementedVariable] + 1;
				if (resultCoordinate[incrementedVariable] == resultDimensions[incrementedVariable]) {
					resultCoordinate[incrementedVariable] = 0;
				} else {
					break;
				}
			}
			if (incrementedVariable != numVariablesResult) {
				gPosition += accOffGPot[incrementedVariable];
				pPosition += accOffPPot[incrementedVariable];
				incrementedVariable = 0;
			}
		}

		return result;
	}

	/**
	 * @param utilityPotential
	 * @param probabilityPotential
	 * @return
	 */
	@SuppressWarnings("unchecked") public static GTablePotential divide(GTablePotential utilityPotential,
			TablePotential probabilityPotential) {
		// variables to create the divided potential
		List<Variable> utilityVariables = utilityPotential.getVariables();
		List<Variable> probabilityVariables = probabilityPotential.getVariables();
		HashSet<Variable> setResultVariables = new LinkedHashSet<Variable>(utilityVariables);
		setResultVariables.addAll(probabilityVariables);
		List<Variable> resultVariables = new ArrayList<Variable>(setResultVariables);
		//		GTablePotential result = new GTablePotential(resultVariables, PotentialRole.UTILITY);
		GTablePotential result = new GTablePotential(resultVariables, PotentialRole.UNSPECIFIED);
		// variables for accOffsets algorithm
		int[] accOffsetsUtility = result.getAccumulatedOffsets(resultVariables);
		int[] accOffsetsProbability = result.getAccumulatedOffsets(probabilityVariables);
		int[] resultCoordinate = new int[resultVariables.size()];
		int increasedVariable = 0;
		int utilityPosition = 0;
		int probabilityPosition = 0;
		int numResultVariables = resultVariables.size();
		int[] resultDimensions = result.getDimensions();
		do {
			// gets partition and probability, then divide
			CEP partition = (CEP) utilityPotential.elementTable.get(utilityPosition);
			double probability = probabilityPotential.values[probabilityPosition];
			partition.divide(probability);
			result.elementTable.add(partition);
			// next coordinate
			boolean more;
			do {
				more = false;
				resultCoordinate[increasedVariable]++;
				if (resultCoordinate[increasedVariable] == resultDimensions[increasedVariable]) {
					resultCoordinate[increasedVariable++] = 0;
					more = true;
				}
			} while (increasedVariable < numResultVariables && more);
			if (increasedVariable < numResultVariables) {
				utilityPosition += accOffsetsUtility[increasedVariable];
				probabilityPosition += accOffsetsProbability[increasedVariable];
				increasedVariable = 0;
			}
		} while (increasedVariable < numResultVariables);
		return result;
	}

	/**
	 * Multiplies a <code>GTablePotential</code> of <code>CEPartitionPotential</code>
	 * and a <code>TablePotential</code>, marginalizing out <code>variableToRemove</code> using
	 * the weightedAverage algorithm, @seeorg.openmarkov.costeffectiveness.id.temporary.operation#weightedAverage
	 *
	 * @param utilityPotential     <code>GTablePotential</code>.
	 * @param probabilityPotential <code>TablePotential</code>.
	 * @return A <code>GTablePotential</code> of <code>CEPartitionPotential</code> with
	 * the same variables of the received <code>GTablePotential</code>,
	 * in the same order, at the beginning plus the variables of the
	 * <code>TablePotential</code> not contained in the
	 * <code>GTablePotential</code> at the end.
	 * @throws CostEffectivenessException
	 */
	@SuppressWarnings("unchecked") public static GTablePotential multiplyAndMarginalize(
			TablePotential probabilityPotential, GTablePotential utilityPotential, Variable variableToRemove)
			throws CostEffectivenessException {

		// Create returning potential
		List<Variable> utilityVariables = new ArrayList<Variable>(utilityPotential.getVariables());
		List<Variable> probabilityVariables = new ArrayList<Variable>(probabilityPotential.getVariables());
		HashSet<Variable> variablesResultSet = new LinkedHashSet<Variable>(utilityVariables);
		variablesResultSet
				.addAll(probabilityVariables); // All the set of variables contained in gPotential and variableToRemove
		variablesResultSet.remove(variableToRemove); // Except variableToRemove

		List<Variable> referenceVariables = new ArrayList<Variable>(variablesResultSet.size() + 1);
		referenceVariables.add(variableToRemove); // Put variableToRemove in the first place
		referenceVariables.addAll(variablesResultSet);  // Then the remaining.

		List<Variable> variablesResult = new ArrayList<Variable>(variablesResultSet);
		// Finally, this is the result
		GTablePotential result = null;
		//		result = new GTablePotential(variablesResult, PotentialRole.UTILITY);
		result = new GTablePotential(variablesResult, PotentialRole.UNSPECIFIED);

		// Using accumulated offsets
		int[] accOffProbabilityPot = TablePotential.getAccumulatedOffsets(referenceVariables, probabilityVariables);
		int[] accOffUtilityPot = TablePotential.getAccumulatedOffsets(referenceVariables, utilityVariables);

		int numReferenceVariables = referenceVariables.size();
		int[] referenceVariablesCoordinate = new int[numReferenceVariables];
		int[] referenceVariablesDimensions = new int[numReferenceVariables];
		for (int i = 0; i < numReferenceVariables; i++) {
			referenceVariablesDimensions[i] = referenceVariables.get(i).getNumStates();
		}
		int utilityPosition = 0;
		int probabilityPosition = 0;
		int incrementedVariable = 0;

		// Variables for weighted average
		int numStatesVariableToRemove = variableToRemove.getNumStates();
		List<CEP> partitions = new ArrayList<CEP>(numStatesVariableToRemove);
		double[] probabilities = new double[numStatesVariableToRemove];

		int i = 0;
		while (incrementedVariable != numReferenceVariables) {
			// fill arrays with data to apply weighted average algorithm
			CEP partition = (CEP) utilityPotential.elementTable.get(utilityPosition);
			partitions.add(partition);
			probabilities[i++] = probabilityPotential.values[probabilityPosition];
			// next iteration
			for (incrementedVariable = 0; incrementedVariable < numReferenceVariables; incrementedVariable++) {
				referenceVariablesCoordinate[incrementedVariable] = referenceVariablesCoordinate[incrementedVariable]
						+ 1;
				if (referenceVariablesCoordinate[incrementedVariable]
						== referenceVariablesDimensions[incrementedVariable]) {
					referenceVariablesCoordinate[incrementedVariable] = 0;
				} else {
					break;
				}
			}
			if (incrementedVariable != numReferenceVariables) {
				utilityPosition += accOffUtilityPot[incrementedVariable];
				probabilityPosition += accOffProbabilityPot[incrementedVariable];
			}
			if (incrementedVariable != FIRST_VARIABLE_POSITION) {
				if (!checkZero(probabilities) && !allZeroPartition(partitions)) {
					result.elementTable
							.add(CEBaseOperations.weightedAverage(partitions, variableToRemove, probabilities));
				} else {
					result.elementTable.add(CEP.getZeroPartition());
				}
				i = 0;
				partitions.removeAll(partitions);
			}
		}
		result.setCriterion(utilityPotential.getCriterion());
		return result;
	}

	/**
	 * @param partitions <code>List</code> of <code>CEPartition</code>s.
	 * @return <code>true</code> when all the partitions have a zero probability.
	 */
	private static boolean allZeroPartition(List<CEP> partitions) {

		boolean allZero = true;
		int index = 0;
		int numPartitions = partitions.size();
		while (allZero && index < numPartitions) {
			allZero &= partitions.get(index++).isZero();
		}
		return allZero;
	}

	/**
	 * Remove cost and effectiveness nodes and potentials replacing them for
	 * a new cost-effectiveness node and potential. The new
	 * <code>Node</code> has as parents the parents of all the cost and
	 * effectiveness parents. This method assumes that there are no super value
	 * nodes
	 *
	 * @param influenceDiagram <code>ProbNet</code>
	 * @param lambdaMin        <code>Double</code>
	 * @param lambdaMax        <code>Double</code>
	 * @throws CostEffectivenessException
	 * @throws NotEvaluableNetworkException
	 */
	public static ProbNet getInitializedID(ProbNet influenceDiagram, double lambdaMin, double lambdaMax)
			throws CostEffectivenessException, NotEvaluableNetworkException {
		influenceDiagram = getCostEffectivenessNetwork(influenceDiagram);
		// Gets cost and effectiveness potentials of id
		TablePotential costPotential = getCostPotential(influenceDiagram);
		TablePotential effectivenessPotential = getEffectivenessPotential(influenceDiagram);
		GTablePotential utilityPotential = getCEPotential(costPotential, effectivenessPotential, lambdaMin, lambdaMax);
		//		utilityPotential.setPotentialRole(PotentialRole.UTILITY);
		utilityPotential.setPotentialRole(PotentialRole.UNSPECIFIED);

		// Remove cost and effectiveness potentials and adds the GTablePotential
		List<Node> costAndEffectivenessNodes = influenceDiagram.getNodes(NodeType.UTILITY);

		// Get parents of cost and effectiveness potentials
		HashSet<Node> parents = new HashSet<Node>();
		for (Node costAndEffecivenessNode : costAndEffectivenessNodes) {
			parents.addAll(costAndEffecivenessNode.getParents());
		}

		// Remove cost and effectiveness potentials
		for (Node Node : costAndEffectivenessNodes) {
			influenceDiagram.removePotentials(Node.getPotentials());
		}

		// Remove cost and effectiveness Nodes
		for (Node Node : costAndEffectivenessNodes) {
			influenceDiagram.removeNode(Node);
		}

		// Create ceNode
		Variable ceVariable = new Variable("CostEffectiveness");
		ceVariable.setVariableType(VariableType.NUMERIC);
		Node ceNode = influenceDiagram.addNode(ceVariable, NodeType.UTILITY);
		utilityPotential.setCriterion(ceVariable.getDecisionCriterion());
		//		utilityPotential.setUtilityVariable(ceVariable);
		ceNode.addPotential(utilityPotential);

		// Add links from parents to the new node
		for (Node parent : parents) {
			try {
				influenceDiagram.addLink(parent.getVariable(), ceVariable, true);
			} catch (NodeNotFoundException e) {
				throw new NotEvaluableNetworkException(e.getMessage());
			}
		}
		return influenceDiagram;
	}

	/**
	 * Creates a <code>GeneralizedTablePotential</code> of
	 * <code>CEPartitionPotential</code> using the cost and effectiveness potentials.
	 *
	 * @param costPotential          <code>TablePotential</code>.
	 * @param effectivenessPotential <code>TablePotential</code>.
	 * @param lambdaMin              used in <code>CEPartitionPotential</code>.
	 * @param lambdaMax              used in <code>CEPartitionPotential</code>.
	 * @throws CostEffectivenessException CostEffectivenessException
	 * <code>fsVariables</code> must be equal to <code>effectiveness.getVariables()</code> union
	 * <code>cost.getVariables()</code>.
	 */
	@SuppressWarnings("unchecked") public static GTablePotential getCEPotential(TablePotential costPotential,
			TablePotential effectivenessPotential, double lambdaMin, double lambdaMax)
			throws CostEffectivenessException {

		// Gets the union of the variables of cost and effectiveness potential
		ArrayList<Variable> costVariables = new ArrayList<Variable>();
		costVariables.addAll(costPotential.getVariables());
		ArrayList<Variable> costAndEffectivenessVariables = new ArrayList<Variable>();
		costAndEffectivenessVariables.addAll(costVariables);
		ArrayList<Variable> effectivenessVariables = new ArrayList<Variable>();
		effectivenessVariables.addAll(effectivenessPotential.getVariables());
		for (Variable effectivenessVariable : effectivenessVariables) {
			if (!costAndEffectivenessVariables.contains(effectivenessVariable)) {
				costAndEffectivenessVariables.add(effectivenessVariable);
			}
		}

		// Get cost and effectiveness potential
		GTablePotential gPotential = new GTablePotential(new ArrayList<Variable>(costAndEffectivenessVariables),
				PotentialRole.UNSPECIFIED);
		//    			new ArrayList<Variable>(costAndEffectivenessVariables), PotentialRole.UTILITY);
		double cost, effectiveness;

		int[] dimensionsResult = gPotential.getDimensions();

		// offsets accumulate algorithm
		// Set up variables
		int[] coordinate = new int[dimensionsResult.length];
		int[][] accumulatedOffsets = new int[2][];

		accumulatedOffsets[COST] = gPotential.getAccumulatedOffsets(costPotential.getVariables());
		accumulatedOffsets[EFFECTIVENESS] = gPotential.getAccumulatedOffsets(effectivenessPotential.getVariables());
		int[] positions = { costPotential.getInitialPosition(), effectivenessPotential.getInitialPosition() };
		int increasedVariable;
		int tableSize = gPotential.getTableSize();

		// Loop
		for (int i = 0; i < tableSize; i++) {
			cost = costPotential.values[positions[COST]];
			effectiveness = effectivenessPotential.values[positions[EFFECTIVENESS]];
			CEP partition = new CEP(null, cost, effectiveness, lambdaMin, lambdaMax);
			gPotential.elementTable.add(partition);

			// calculate next position using accumulated offsets
			increasedVariable = 0;
			for (int j = 0; j < coordinate.length; j++) {
				coordinate[j]++;
				if (coordinate[j] < dimensionsResult[j]) {
					increasedVariable = j;
					break;
				}
				coordinate[j] = 0;
			}

			// update the positions of the potentials we are multiplying
			for (int j = 0; j < positions.length; j++) {
				positions[j] += accumulatedOffsets[j][increasedVariable];
			}
		}

		return gPotential;
	}

	/**
	 * Get potentials attached to a variable whose criterion is cost and sums them.
	 *
	 * @param influenceDiagram <code>ProbNet</code>
	 * @return <code>TablePotential</code>
	 */
	private static TablePotential getCostPotential(ProbNet influenceDiagram) {
		List<Variable> variables = influenceDiagram.getVariables();
		ArrayList<TablePotential> costPotentials = new ArrayList<TablePotential>();
		for (Variable variable : variables) {
			if (CEBaseOperations.isCostVariable(variable)) {
				Collection<Potential> potentialsVariable = influenceDiagram.getPotentials(variable);
				for (Potential potential : potentialsVariable) {
					//					if (potential.getPotentialRole() == PotentialRole.UTILITY) {
					if (potential.getCriterion().getCECriterion().equals(Criterion.CECriterion.Cost)) {

						costPotentials.add((TablePotential) potential);
					}
				}
			}
		}
		TablePotential costPotential = DiscretePotentialOperations.sum(costPotentials);
		return costPotential;
	}

	/**
	 * Get potentials attached to a variable whose criterion is effectiveness and adds them.
	 *
	 * @param influenceDiagram <code>ProbNet</code>
	 * @return <code>TablePotential</code>
	 */
	private static TablePotential getEffectivenessPotential(ProbNet influenceDiagram) {
		List<Variable> variables = influenceDiagram.getVariables();
		ArrayList<TablePotential> effectivenessPotentials = new ArrayList<TablePotential>();
		for (Variable variable : variables) {
			if (CEBaseOperations.isEffectivenessVariable(variable)) {
				Collection<Potential> potentialsVariable = influenceDiagram.getPotentials(variable);
				for (Potential potential : potentialsVariable) {
					//					if (potential.getPotentialRole() == PotentialRole.UTILITY) {
					if (potential.getCriterion().getCECriterion().equals(Criterion.CECriterion.Effectiveness)) {
						effectivenessPotentials.add((TablePotential) potential);
					}
				}
			}
		}
		TablePotential effectivenessPotential = DiscretePotentialOperations.sum(effectivenessPotentials);
		return effectivenessPotential;
	}

	/**
	 * @param cepsPotential    <code>GTablePotential</code> of <code>CEPartitionPotential</code>.
	 * @param variableToDelete <code>Variable</code>.
	 * @return Marginalization of <code>potential</code>. Each configuration is
	 * the addition of several <code>CEPartitionPotential</code>
	 * @throws CostEffectivenessException
	 * @throws PotentialOperationException
	 */
	@SuppressWarnings("unchecked") public static GTablePotential marginalize(GTablePotential cepsPotential,
			Variable variableToDelete, double[] chanceVariableDistribution)
			throws PotentialOperationException, CostEffectivenessException {

		// create returning potential
		ArrayList<Variable> potentialVariables = new ArrayList<Variable>(cepsPotential.getVariables());
		int numVariables = potentialVariables.size();
		potentialVariables.remove(variableToDelete);
		GTablePotential marginalized = new GTablePotential(potentialVariables, cepsPotential.getPotentialRole());

		// create a fictitious potential with variables: decision + remainder
		ArrayList<Variable> fictitiousVariables = new ArrayList<Variable>();
		fictitiousVariables.add((Variable) variableToDelete);
		fictitiousVariables.addAll(potentialVariables);
		// restore variablesPotential
		potentialVariables = new ArrayList<Variable>(cepsPotential.getVariables());

		// gets information about variable received: position and num options
		int variablePosition = potentialVariables.indexOf(variableToDelete);
		int[] potentialDimensions = cepsPotential.getDimensions();
		int numVariableOptions = potentialDimensions[variablePosition];

		// to cross the coordinates of this potential
		TablePotential fictitious = new TablePotential(fictitiousVariables, null);
		int[] offsetsAccPotential = fictitious.getAccumulatedOffsets(potentialVariables);
		int potentialPosition = 0;
		int[] fictCoordinate = new int[numVariables];
		int[] fictDimensions = fictitious.getDimensions();

		int incremented = 0;
		do {
			ArrayList<CEP> partitions = new ArrayList<CEP>();
			for (int i = 0; i < numVariableOptions; i++) {// cross same decision
				// get partitions of a configuration
				partitions.add((CEP) cepsPotential.elementTable.get(potentialPosition));
				if (i < numVariableOptions - 1) {
					potentialPosition += offsetsAccPotential[0];// decision in 0
				}
			}
			// maximize
			CEP marginalization = addMarginalize(partitions, variableToDelete, chanceVariableDistribution);
			marginalized.elementTable.add(marginalization);
			// next coordinate
			for (incremented = 1; incremented < numVariables; incremented++) {
				fictCoordinate[incremented] = fictCoordinate[incremented] + 1;
				if (fictCoordinate[incremented] == fictDimensions[incremented]) {
					fictCoordinate[incremented] = 0;
				} else {
					break;
				}
			}
			if (incremented != numVariables) {
				potentialPosition += offsetsAccPotential[incremented];
			}
		} while (incremented != numVariables);

		return marginalized;
	}

	/**
	 * @param partitions
	 * @param variableToDelete
	 * @param probabilities its length should be equal to numPartitions.size()
	 * @throws CostEffectivenessException
	 * @return CEPartitionPotential
	 */
	private static CEP addMarginalize(ArrayList<CEP> partitions, Variable variableToDelete, double[] probabilities)
			throws CostEffectivenessException {
		int numPartitions = partitions.size();
		if (numPartitions != probabilities.length) {
			throw new CostEffectivenessException(
					"Number of partitions = " + partitions.size() + " and number of probabilities = "
							+ probabilities.length);
		}
		if (checkZero(probabilities)) {
			return CEP.getZeroPartition();
		}
		boolean[] takeIntoAccount = new boolean[probabilities.length];
		for (int i = 0; i < probabilities.length; i++) {
			if (probabilities[i] != 0.0) {
				takeIntoAccount[i] = true;
			}
		}
		double[] thresholds = CEBaseOperations.getUnionThresholds(partitions, null);
		int numIntervals = thresholds.length + 1;
		double[] costs = new double[numIntervals];
		double[] effectivities = new double[numIntervals];
		StrategyTree[] strategyTrees = new StrategyTree[numIntervals];

		for (int interval = 0; interval < numIntervals; interval++) {
			Potential[] interventionsInterval = new Potential[numPartitions];
			for (int partitionIndex = 0; partitionIndex < numPartitions; partitionIndex++) {
				CEP partition = partitions.get(partitionIndex);
				double medium;
				if (interval == 0) {
					medium = (partition.getMinThreshold() + thresholds[0]) / 2;
				} else if (interval == thresholds.length) {
					medium = (thresholds[thresholds.length - 1] + partition.getMaxThreshold()) / 2;
				} else {
					medium = (thresholds[interval - 1] + thresholds[interval]) / 2;
				}
				costs[interval] += partition.getCost(medium);
				effectivities[interval] += partition.getEffectiveness(medium);
				interventionsInterval[partitionIndex] = partition.getIntervention(medium);
			}
			List<Variable> parentVariables = null;
			StrategyTree intervalTree = null;
			for (int partitionIndex = 0; partitionIndex < numPartitions; partitionIndex++) {
				if (takeIntoAccount[partitionIndex]) {
					List<State> states = new ArrayList<State>();
					states.add(variableToDelete.getStates()[partitionIndex]);
					Potential intervention = strategyTrees[partitionIndex];
					for (int j = partitionIndex + 1; j < numPartitions; j++) {
						if ((intervention != null && interventionsInterval[j] != null) ?
								intervention.equals(interventionsInterval[j]) :
								intervention == null && interventionsInterval[j] == null) {
							takeIntoAccount[j] = false;
							states.add(variableToDelete.getStates()[j]);
						}
					}
					if (intervalTree == null) {
						intervalTree = new StrategyTree(variableToDelete);
					}
					intervalTree.addBranch(new TreeADDBranch(states, variableToDelete, intervention, parentVariables));
				}
			}
			strategyTrees[interval] = intervalTree;
		}
		return new CEP(strategyTrees, costs, effectivities, thresholds);
	}

	/**
	 * @param chanceVariableDistribution
	 * @return true if all the probabilities of chanceVariableDistribution are 0.
	 */
	private static boolean checkZero(double[] chanceVariableDistribution) {
		int i;
		for (i = 0; i < chanceVariableDistribution.length && chanceVariableDistribution[i] == 0.0; i++)
			;
		return i == chanceVariableDistribution.length;
	}

	/**
	 * Computes the optimal decision associated to a decision elimination.
	 *
	 * @param potential <code>GTablePotential</code> of <code>PartitionLCE</code>.
	 * @param decision  <code>Variable</code>. It should belong to <code>potential</code>
	 * variables.
	 * @return A <code>GTablePotential</code> of <code>PartitionLCE</code> with
	 * the same variables as the <code>GTablePotential</code> received
	 * but without the variable <code>decision</code>. Each
	 * <code>PartitionLCE</code> can be divided in several intervals.
	 */
	@SuppressWarnings("unchecked") public static GTablePotential ceMaximize(GTablePotential potential,
			Variable decision) throws PotentialOperationException {
		// Generate result potential
		// Get variables
		ArrayList<Variable> resultPotentialVariables = new ArrayList<Variable>(potential.getVariables());
		resultPotentialVariables.remove(decision);
		GTablePotential resultPotential = new GTablePotential(resultPotentialVariables, PotentialRole.UNSPECIFIED);

		// Offsets accumulated algorithm.
		// Create the order potential that imposes the order to be followed
		// creating the result potential.
		// Create variables. First: decision; Second: result potential variables
		ArrayList<Variable> potentialVariables = new ArrayList<Variable>(potential.getVariables());
		int numVariables = potentialVariables.size();
		ArrayList<Variable> orderVariables = new ArrayList<Variable>(numVariables);
		orderVariables.add(decision);
		orderVariables.addAll(resultPotentialVariables);
		TablePotential orderPotential = new TablePotential(orderVariables, null);

		// Gets accumulated offsets and coordinates.
		int[] accOffsetsPotential = orderPotential.getAccumulatedOffsets(potentialVariables);
		int potentialPosition = 0;
		int[] orderCoordinate = new int[orderVariables.size()];
		int[] orderDimensions = orderPotential.getDimensions();
		int numDecisionOptions = decision.getNumStates();

		int incrementedVariable;
		do {
			ArrayList<CEP> partitions = new ArrayList<CEP>();
			for (int i = 0; i < numDecisionOptions; i++) {// cross same decision
				// get partitions of a configuration
				partitions.add((CEP) potential.elementTable.get(potentialPosition));
				if (i < numDecisionOptions - 1) {
					potentialPosition += accOffsetsPotential[0];// decision in 0
				}
			}
			// maximize
			CEP maximizedPartition;
			try {
				maximizedPartition = CEBaseOperations.optimalCEP(decision, partitions);
			} catch (CostEffectivenessException e) {
				throw new PotentialOperationException(e.getMessage());
			}
			resultPotential.elementTable.add(maximizedPartition);
			// next coordinate
			incrementedVariable = 1;
			if (incrementedVariable < numVariables) {
				boolean more;
				do {
					orderCoordinate[incrementedVariable]++;
					if (orderCoordinate[incrementedVariable] == orderDimensions[incrementedVariable]) {
						orderCoordinate[incrementedVariable++] = 0;
						more = true;
					} else {
						more = false;
					}
				} while (incrementedVariable < numVariables && more);
			}
			if (incrementedVariable < numVariables) {
				potentialPosition += accOffsetsPotential[incrementedVariable];
			}
		} while (incrementedVariable < numVariables);
		resultPotential.setCriterion(potential.getCriterion());
		return resultPotential;
	}

	//	/**
	//	 * @param tablePotentials
	//	 *            array to multiply
	//	 * @param variablesToKeep
	//	 *            The set of variables that will appear in the resulting
	//	 *            potential
	//	 * @param variablesToEliminate
	//	 *            The set of variables eliminated by marginalization (in
	//	 *            general, by summing out or maximizing)
	//	 * @argCondition variablesToKeep and variablesToEliminate are a partition of
	//	 *               the union of the variables of the potential
	//	 * @return A <code>TablePotential</code> result of multiply and marginalize.
	//	 */
	//	public static TablePotential multiplyAndMarginalize(List<TablePotential> tablePotentials,
	//			List<Variable> variablesToKeep,
	//			List<Variable> variablesToEliminate) {
	//
	//		// Constant potentials are those that do not depend on any variables.
	//		// The product of all the constant potentials is the constant factor.
	//		double constantFactor = 1.0;
	//		// Non constant potentials are proper potentials.
	//		List<TablePotential> nonConstantPotentials = new ArrayList<TablePotential>();
	//		for (TablePotential potential : tablePotentials) {
	//			if (potential.getNumVariables() != 0) {
	//				nonConstantPotentials.add(potential);
	//			} else {
	//				constantFactor *= potential.values[potential.getInitialPosition()];
	//			}
	//		}
	//
	//		int numNonConstantPotentials = nonConstantPotentials.size();
	//
	//		if (numNonConstantPotentials == 0) {
	//			TablePotential resultingPotential = new TablePotential(variablesToKeep,
	//					DiscretePotentialOperations.getRole(tablePotentials));
	//			resultingPotential.values[0] = constantFactor;
	//			return resultingPotential;
	//		}
	//
	//		// variables in the resulting potential
	//		List<Variable> unionVariables = new ArrayList<Variable>(variablesToEliminate);
	//		unionVariables.addAll(variablesToKeep);
	//		int numUnionVariables = unionVariables.size();
	//
	//		// current coordinate in the resulting potential
	//		int[] unionCoordinate = new int[numUnionVariables];
	//		int[] unionDimensions = TablePotential.calculateDimensions(unionVariables);
	//
	//		// Defines some arrays for the proper potentials...
	//		double[][] tables = new double[numNonConstantPotentials][];
	//		int[] initialPositions = new int[numNonConstantPotentials];
	//		int[] currentPositions = new int[numNonConstantPotentials];
	//		int[][] accumulatedOffsets = new int[numNonConstantPotentials][];
	//		// ... and initializes them
	//		// TablePotential unionPotential = new
	//		// TablePotential(unionVariables,null);
	//		for (int i = 0; i < numNonConstantPotentials; i++) {
	//			TablePotential potential = nonConstantPotentials.get(i);
	//			tables[i] = potential.values;
	//			initialPositions[i] = potential.getInitialPosition();
	//			currentPositions[i] = initialPositions[i];
	//			accumulatedOffsets[i] = TablePotential.getAccumulatedOffsets(unionVariables, potential.getVariables());
	//		}
	//
	//		// The result size is the product of the dimensions of the
	//		// variables to keep
	//		int resultSize = TablePotential.computeTableSize(variablesToKeep);
	//		double[] resultValues = new double[resultSize];
	//		// The elimination size is the product of the dimensions of the
	//		// variables to eliminate
	//		int eliminationSize = 1;
	//		for (Variable variable : variablesToEliminate) {
	//			eliminationSize *= variable.getNumStates();
	//		}
	//
	//		// Auxiliary variables for the nested loops
	//		double multiplicationResult; // product of the table values
	//		double accumulator; // in general, the sum or the maximum
	//		int increasedVariable = 0; // when computing the next configuration
	//
	//		// outer iterations correspond to the variables to keep
	//		boolean thereAreInterventions = false;
	//		for (TablePotential potential : tablePotentials) {
	//			thereAreInterventions |= potential.interventions != null;
	//		}
	//		Potential[] resultInterventions = null;
	//		if (thereAreInterventions) {
	//			resultInterventions = new Potential[resultSize];
	//		}
	//		for (int outerIteration = 0; outerIteration < resultSize; outerIteration++) {
	//			// Inner iterations correspond to the variables to eliminate
	//			// accumulator summarizes the result of all inner iterations
	//
	//			// first inner iteration
	//            Potential[] interventions = new Potential[eliminationSize];
	//			multiplicationResult = constantFactor;
	//			for (int i = 0; i < numNonConstantPotentials; i++) {
	//				// multiply the numbers
	//				multiplicationResult *= tables[i][currentPositions[i]];
	//				if (nonConstantPotentials.get(0).interventions != null) {
	//					interventions[i] = nonConstantPotentials.get(0).interventions[0];
	//				}
	//			}
	//			accumulator = multiplicationResult;
	//
	//			// next inner iterations
	//			double[] probability = new double[eliminationSize];
	//			for (int innerIteration = 1; innerIteration < eliminationSize; innerIteration++) {
	//
	//				// find the next configuration and the index of the
	//				// increased variable
	//				for (int j = 0; j < unionCoordinate.length; j++) {
	//					unionCoordinate[j]++;
	//					if (unionCoordinate[j] < unionDimensions[j]) {
	//						increasedVariable = j;
	//						break;
	//					}
	//					unionCoordinate[j] = 0;
	//				}
	//				// update the positions of the potentials we are multiplying
	//				for (int i = 0; i < numNonConstantPotentials; i++) {
	//					currentPositions[i] += accumulatedOffsets[i][increasedVariable];
	//				}
	//
	//				// multiply the table values of the potentials
	//				multiplicationResult = constantFactor;
	//				for (int i = 0; i < numNonConstantPotentials; i++) {
	//					multiplicationResult *= tables[i][currentPositions[i]];
	//					if (nonConstantPotentials.get(i).interventions != null) {
	//						interventions[innerIteration] = nonConstantPotentials.get(i).interventions[currentPositions[i]];
	//					}
	//				}
	//
	//				// update the accumulator (for this inner iteration)
	//				accumulator += multiplicationResult;
	//				// accumulator =
	//				// operator.combine(accumulator,multiplicationResult);
	//
	//			} // end of inner iteration
	//			probability[outerIteration] = accumulator;
	//
	//			// when eliminationSize == 0 there is a multiplication without
	//			// marginalization but we must find the next configuration
	//			if (outerIteration < resultSize - 1) {
	//				// find the next configuration and the index of the
	//				// increased variable
	//				for (int j = 0; j < unionCoordinate.length; j++) {
	//					unionCoordinate[j]++;
	//					if (unionCoordinate[j] < unionDimensions[j]) {
	//						increasedVariable = j;
	//						break;
	//					}
	//					unionCoordinate[j] = 0;
	//				}
	//
	//				// update the positions of the potentials we are multiplying
	//				for (int i = 0; i < numNonConstantPotentials; i++) {
	//					currentPositions[i] += accumulatedOffsets[i][increasedVariable];
	//				}
	//			}
	//
	//			resultValues[outerIteration] = accumulator;
	//			if (thereAreInterventions) {
	//				// variablesToEliminate must contain a single value.
	//				try {
	//					resultInterventions[outerIteration] =
	//							new ChanceIntervention(variablesToEliminate.get(0), probability, interventions);
	//				} catch (CostEffectivenessException e) {
	//					e.printStackTrace(); // Unreachable code
	//				}
	//			}
	//
	//		} // end of outer iteration
	//
	//		return new TablePotential(variablesToKeep, DiscretePotentialOperations.getRole(tablePotentials), resultValues);
	//	}
	//
	//    /**
	//     * @param tablePotentials
	//     *            <code>ArrayList</code> of <code>TablePotential</code>s.
	//     * @param fsVariablesToKeep
	//     *            <code>ArrayList</code> of <code>Variable</code>s.
	//     * @param fsVariableToMaximize
	//     *            <code>Variable</code>.
	//     * @return Two potentials: 1) a <code>Potential</code> resulting of
	//     *         multiplication and maximization of
	//     *         <code>variableToMaximize</code> and 2) a
	//     *         <code>GTablePotential</code> of <code>Choice</code> (same
	//     *         variables as preceding) with the value choosed for
	//     *         <code>variableToMaximize</code> in each configuration.
	//     */
	//    @SuppressWarnings("unchecked")
	//    public static Object[] multiplyAndMaximize(List<Potential> tablePotentials,
	//            List<Variable> fSVariablesToKeep,
	//            Variable fSVariableToMaximize) {
	//        List<TablePotential> potentials = (ArrayList<TablePotential>) ((Object) tablePotentials);
	//        List<Variable> variablesToKeep = (ArrayList<Variable>) ((Object) fSVariablesToKeep);
	//
	//        PotentialRole role = DiscretePotentialOperations.getRole(tablePotentials);
	//
	//        TablePotential resultingPotential = new TablePotential(variablesToKeep, role);
	//
	//        GTablePotential<Choice> gResult = new GTablePotential<Choice>(variablesToKeep, role);
	//        int numStates = ((Variable) fSVariableToMaximize).getNumStates();
	//        int[] statesChoosed;
	//        Choice choice;
	//
	//        // Constant potentials are those that do not depend on any variables.
	//        // The product of all the constant potentials is the constant factor.
	//        double constantFactor = 1.0;
	//        // Non constant potentials are proper potentials.
	//        List<TablePotential> properPotentials = new ArrayList<TablePotential>();
	//        for (Potential potential : potentials) {
	//            if (potential.getNumVariables() != 0) {
	//                properPotentials.add((TablePotential) potential);
	//            } else {
	//                constantFactor *= ((TablePotential) potential).values[((TablePotential) potential).getInitialPosition()];
	//            }
	//        }
	//
	//        int numProperPotentials = properPotentials.size();
	//
	//        if (numProperPotentials == 0) {
	//            resultingPotential.values[0] = constantFactor;
	//            return new Object[] { resultingPotential, gResult };
	//        }
	//
	//        // variables in the resulting potential
	//        List<Variable> unionVariables = new ArrayList<Variable>();
	//        unionVariables.add((Variable) fSVariableToMaximize);
	//        unionVariables.addAll(variablesToKeep);
	//        int numUnionVariables = unionVariables.size();
	//
	//        // current coordinate in the resulting potential
	//        int[] unionCoordinate = new int[numUnionVariables];
	//        int[] unionDimensions = TablePotential.calculateDimensions(unionVariables);
	//
	//        // Defines some arrays for the proper potentials...
	//        double[][] tables = new double[numProperPotentials][];
	//        int[] initialPositions = new int[numProperPotentials];
	//        int[] currentPositions = new int[numProperPotentials];
	//        int[][] accumulatedOffsets = new int[numProperPotentials][];
	//        // ... and initializes them
	//        TablePotential unionPotential = new TablePotential(unionVariables, null);
	//        for (int i = 0; i < numProperPotentials; i++) {
	//            TablePotential potential = (TablePotential) properPotentials.get(i);
	//            tables[i] = potential.values;
	//            initialPositions[i] = potential.getInitialPosition();
	//            currentPositions[i] = initialPositions[i];
	//            accumulatedOffsets[i] = unionPotential
	//            // .getAccumulatedOffsets(potential.getOriginalVariables());
	//            .getAccumulatedOffsets(potential.getVariables());
	//        }
	//
	//        // The result size is the product of the dimensions of the
	//        // variables to keeep
	//        int resultSize = resultingPotential.values.length;
	//        // The elimination size is the product of the dimensions of the
	//        // variables to eliminate
	//        int eliminationSize = 1;
	//        eliminationSize *= ((Variable) fSVariableToMaximize).getNumStates();
	//
	//        // Auxiliary variables for the nested loops
	//        double multiplicationResult; // product of the table values
	//        double accumulator; // in general, the sum or the maximum
	//        int increasedVariable = 0; // when computing the next configuration
	//
	//        // outer iterations correspond to the variables to keep
	//        Potential[] properInterventions = new Potential[numProperPotentials];
	//        for (int outerIteration = 0; outerIteration < resultSize; outerIteration++) {
	//            // Inner iterations correspond to the variables to eliminate
	//            // accumulator summarizes the result of all inner iterations
	//
	//            // first inner iteration
	//            multiplicationResult = constantFactor;
	//            int numInterventions = 0;
	//            for (int i = 0; i < numProperPotentials; i++) {
	//                // multiply the numbers
	//                multiplicationResult *= tables[i][currentPositions[i]];
	//                if (properPotentials.get(i).interventions != null) {
	//                	properInterventions[i] = properPotentials.get(i).interventions[currentPositions[i]];
	//                	numInterventions++;
	//                }
	//            }
	//            statesChoosed = new int[numStates];
	//            statesChoosed[0] = 0;
	//            choice = new Choice(fSVariableToMaximize, statesChoosed);
	//            accumulator = multiplicationResult;
	//            choice.setValue(0); // because in first iteration we have a maximum
	//
	//            // next inner iterations
	//            Potential actualIntervention = null;
	//            int maxValueIndex = 0;
	//            for (int innerIteration = 1; innerIteration < eliminationSize; innerIteration++) {
	//
	//                // find the next configuration and the index of the
	//                // increased variable
	//                for (int j = 0; j < unionCoordinate.length; j++) {
	//                    unionCoordinate[j]++;
	//                    if (unionCoordinate[j] < unionDimensions[j]) {
	//                        increasedVariable = j;
	//                        break;
	//                    }
	//                    unionCoordinate[j] = 0;
	//                }
	//                // update the positions of the potentials we are multiplying
	//                for (int i = 0; i < numProperPotentials; i++) {
	//                    currentPositions[i] += accumulatedOffsets[i][increasedVariable];
	//                }
	//
	//                // multiply the table values of the potentials
	//                multiplicationResult = constantFactor;
	//                for (int i = 0; i < numProperPotentials; i++) {
	//                    multiplicationResult = multiplicationResult * tables[i][currentPositions[i]];
	//                    if (properPotentials.get(i).interventions != null) {
	//                    	actualIntervention = properPotentials.get(i).interventions[currentPositions[i]];
	//                    }
	//                }
	//
	//                // update the accumulator (for this inner iteration)
	//                if (multiplicationResult > (accumulator + DiscretePotentialOperations.maxRoundErrorAllowed)) {
	//                    choice.setValue(innerIteration);
	//                    accumulator = multiplicationResult;
	//                    maxValueIndex = innerIteration;
	//                } else {
	//                    if ((multiplicationResult < (accumulator + DiscretePotentialOperations.maxRoundErrorAllowed))
	//                            && (multiplicationResult >= (accumulator - DiscretePotentialOperations.maxRoundErrorAllowed))) {
	//                        choice.addValue(innerIteration);
	//                    }
	//                }
	//                // accumulator =
	//                // operator.combine(accumlator,multiplicationResult);
	//
	//            } // end of inner iteration
	//
	//            // when eliminationSize == 0 there is a multiplication without
	//            // maximization but we must find the next configuration
	//            if (outerIteration < resultSize - 1) {
	//                // find the next configuration and the index of the
	//                // increased variable
	//                for (int j = 0; j < unionCoordinate.length; j++) {
	//                    unionCoordinate[j]++;
	//                    if (unionCoordinate[j] < unionDimensions[j]) {
	//                        increasedVariable = j;
	//                        break;
	//                    }
	//                    unionCoordinate[j] = 0;
	//                }
	//
	//                // update the positions of the potentials we are multiplying
	//                for (int i = 0; i < numProperPotentials; i++) {
	//                    currentPositions[i] += accumulatedOffsets[i][increasedVariable];
	//                }
	//            }
	//            if (numInterventions > 0) {
	//            	if (actualIntervention != null) { // actual intervention is the next intervention
	//            		resultingPotential.interventions[outerIteration] =
	//            				new DecisionIntervention(fSVariableToMaximize, maxValueIndex, actualIntervention);
	//            	} else {
	//            		resultingPotential.interventions[outerIteration] =
	//            				new DecisionIntervention(fSVariableToMaximize, maxValueIndex);
	//            	}
	//            }
	//            resultingPotential.values[outerIteration] = accumulator;
	//            gResult.elementTable.add(choice);
	//
	//        } // end of outer iteration
	//
	//        Object[] resultPotentials = { resultingPotential, gResult };
	//        return resultPotentials;
	//    }
	//
	//

}
