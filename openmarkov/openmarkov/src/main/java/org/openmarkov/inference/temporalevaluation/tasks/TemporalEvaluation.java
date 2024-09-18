/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.inference.temporalevaluation.tasks;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.openmarkov.core.exception.NonProjectablePotentialException;
import org.openmarkov.core.exception.NotEvaluableNetworkException;
import org.openmarkov.core.exception.UnexpectedInferenceException;
import org.openmarkov.core.exception.WrongCriterionException;
import org.openmarkov.core.inference.InferenceOptions;
import org.openmarkov.core.inference.MulticriteriaOptions;
import org.openmarkov.core.inference.heuristic.EliminationHeuristic;
import org.openmarkov.core.inference.tasks.TaskUtilities;
import org.openmarkov.core.model.network.EvidenceCase;
import org.openmarkov.core.model.network.NodeType;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.constraint.OnlyAtemporalVariables;
import org.openmarkov.core.model.network.potential.Potential;
import org.openmarkov.core.model.network.potential.TablePotential;
import org.openmarkov.inference.variableElimination.VariableEliminationCore;
import org.openmarkov.inference.variableElimination.tasks.VariableElimination;

public class TemporalEvaluation extends VariableElimination {

	private VariableEliminationCore variableEliminationCore = null;

	// Classified potentials
	private List<List<TablePotential>> utilityPotentialBySlice;
	private List<List<TablePotential>> probabilityPotentialBySlice;
	private List<TablePotential> utilityPotentialAtemporal;
	private List<TablePotential> probabilityPotentialAtemporal;

	// Resulting utilities
	private List<TablePotential> utilityPotentialsPerSlice = null;
	private TablePotential atemporalUtility = null;

	private boolean isUnicriterion;

	private boolean isTemporal;

	/**
	 * @param network Probabilistic network to be resolved
	 * @throws NotEvaluableNetworkException  Constructor
	 */
	public TemporalEvaluation(ProbNet network) throws NotEvaluableNetworkException {
		super(network);
		int numberOfSlices = probNet.getInferenceOptions().getTemporalOptions().getHorizon();

		this.utilityPotentialBySlice = new ArrayList<>();
		this.probabilityPotentialBySlice = new ArrayList<>();
		this.utilityPotentialAtemporal = new ArrayList<>();
		this.probabilityPotentialAtemporal = new ArrayList<>();

		// Initialize the arrays for temporal utilities and probabilities
		for (int i = 0; i <= numberOfSlices; i++) {
			this.utilityPotentialBySlice.add(new ArrayList<>());
			this.probabilityPotentialBySlice.add(new ArrayList<>());
		}

		// This analysis only works conditioning on decisions (global analysis is not allowed)
		getConditioningVariables().addAll(probNet.getVariables(NodeType.DECISION));

		// pre-process steps
		this.isTemporal = !probNet.hasConstraint(OnlyAtemporalVariables.class);
		this.isUnicriterion = probNet.getInferenceOptions().getMultiCriteriaOptions().getMulticriteriaType()
				.equals(MulticriteriaOptions.Type.UNICRITERION);

	}

	public void resolve() throws UnexpectedInferenceException {
		LogManager.getLogger().debug("Expanding");
		// Expand network
		probNet = TaskUtilities.expandNetwork(probNet, isTemporal);
		LogManager.getLogger().debug("Extending pre-resolution evidence");
		// Extend pre-resolution evidence
		probNet = TaskUtilities.extendPreResolutionEvidence(probNet, getPreResolutionEvidence());
		LogManager.getLogger().debug("Applying discounts");
		// Apply discounts
		probNet = TaskUtilities.applyDiscounts(probNet, isTemporal);
		LogManager.getLogger().debug("Scaling");
		// Scale utilities for unicriterion or cost-effectiveness
		if (isUnicriterion) {
			probNet = TaskUtilities.scaleUtilitiesUnicriterion(probNet);
		} else {
			probNet = TaskUtilities.scaleUtilitiesCostEffectiveness(probNet);
		}
		LogManager.getLogger().debug("Discretizing non-observerd numeric variables");
		// Discretize non-observed numeric variables
		probNet = TaskUtilities.discretizeNonObservedNumericVariables(probNet, getPreResolutionEvidence());

		LogManager.getLogger().debug("Absorb intermediate numeric nodes");
		// Absorb intermediate numeric nodes
		probNet = TaskUtilities.absorbAllIntermediateNumericNodes(probNet, getPreResolutionEvidence());
		LogManager.getLogger().debug("Projecting and classifying");
		// Project and classify potentials
		try {
			tableProjectAndClassifyPotentials(probNet, getPreResolutionEvidence());
		} catch (NonProjectablePotentialException | WrongCriterionException e) {
			LogManager.getLogger().error(e);
		}
		LogManager.getLogger().debug("Evaluating");
		// Evaluate the network
		evaluateNetwork(probNet);
	}

	/**
	 * This method project the potentials of the network and classifies the projected potentials by two criteria:
	 * Temporal or atemporal potentials and if they are utility or probability potentials.
	 *
	 * @param evidenceCase Evidence in that the potentials will be projected
	 * @throws NonProjectablePotentialException NonProjectablePotentialException
	 * @throws WrongCriterionException WrongCriterionException
	 */
	public void tableProjectAndClassifyPotentials(ProbNet probNet, EvidenceCase evidenceCase)
			throws NonProjectablePotentialException, WrongCriterionException {
		List<Potential> originalPotentials = probNet.getSortedPotentials();
		List<TablePotential> projectedPotentials = new ArrayList<>();
		// each original potential may yield several projected potentials;
		List<TablePotential> potentials;
		for (Potential potential : originalPotentials) {
			InferenceOptions inferenceOptions = new InferenceOptions(probNet, null);
			potentials = potential.tableProject(evidenceCase, inferenceOptions, projectedPotentials);

			// Get the main variable of the potential to know if it is of utility or probability
			Variable potentialVariable = potential.getVariable(0);

			if (probNet.getNode(potentialVariable).getNodeType().equals(NodeType.UTILITY)) {
				for (TablePotential projectedPotential : potentials) {
					// If the utility potential is a constant potential and it's value it is equals to zero we don't needs it
					if (projectedPotential.getVariables().size() != 0 || projectedPotential.values[0] != 0) {
						// We need to distinguish between atemporal and temporal potentials
						if (potentialVariable.isTemporal()) {
							// Add the potential to the potentials by slice array
							utilityPotentialBySlice.get(potentialVariable.getTimeSlice()).add(projectedPotential);
						} else {
							// Add the potential to the atemporal array
							utilityPotentialAtemporal.add(projectedPotential);
						}
					}
				}
			} else {
				for (TablePotential projectedPotential : potentials) {
					// If the probability potential is a constant potential and it's value it is equals to one we don't needs it
					if (projectedPotential.getVariables().size() != 0 || projectedPotential.values[0] != 1) {
						// Same logic as for utility potentials
						if (potentialVariable.isTemporal()) {
							probabilityPotentialBySlice.get(potentialVariable.getTimeSlice()).add(projectedPotential);
						} else {
							probabilityPotentialAtemporal.add(projectedPotential);

						}
					}
				}
			}
		}
	}

	/**
	 * This method evaluates the network
	 *
	 * @param probNet Network
	 * @throws UnexpectedInferenceException UnexpectedInferenceException
	 */
	public void evaluateNetwork(ProbNet probNet) throws UnexpectedInferenceException {
		utilityPotentialsPerSlice = new ArrayList<>();
		int numberOfSlices = probNet.getInferenceOptions().getTemporalOptions().getHorizon();

		// First we evaluate the atemporal part of the network, for that we build a Markov network with atemporal
		// probability potentials.
		ProbNet markovNetworkInference = probNet.buildMarkovDecisionNetwork(probabilityPotentialAtemporal);
		// Add utility potentials to the network
		ProbNet markovWithUtility = markovNetworkInference.copy();
		for (TablePotential utilityPotential : utilityPotentialAtemporal) {
			markovWithUtility.addPotential(utilityPotential);
		}
		// Create heuristic instance
		EliminationHeuristic heuristic = heuristicFactory(markovWithUtility, new ArrayList<Variable>(),
				getPreResolutionEvidence().getVariables(), getConditioningVariables(),
				markovWithUtility.getChanceAndDecisionVariables());
		variableEliminationCore = new VariableEliminationCore(markovWithUtility, heuristic, isUnicriterion);

		atemporalUtility = variableEliminationCore.getUtility();

		// With atemporal part analysed, we build then a markov network to analyse the first slice (slice zero)
		List<TablePotential> atemporalAndFirstSlicePotentials = new ArrayList<>();
		atemporalAndFirstSlicePotentials.addAll(probabilityPotentialBySlice.get(0));
		// We add and keep all atemporal probability potentials because they could affect all the network
		atemporalAndFirstSlicePotentials.addAll(probabilityPotentialAtemporal);
		markovNetworkInference = probNet.buildMarkovDecisionNetwork(atemporalAndFirstSlicePotentials);
		// Add utility potentials of this slice
		markovWithUtility = markovNetworkInference.copy();
		for (TablePotential utilityPotential : utilityPotentialBySlice.get(0)) {
			markovWithUtility.addPotential(utilityPotential);
		}

		// Create heuristic instance
		heuristic = heuristicFactory(markovWithUtility, new ArrayList<Variable>(),
				getPreResolutionEvidence().getVariables(), getConditioningVariables(),
				markovWithUtility.getChanceAndDecisionVariables());
		variableEliminationCore = new VariableEliminationCore(markovWithUtility, heuristic, isUnicriterion);

		utilityPotentialsPerSlice.add(variableEliminationCore.getUtility());
		variableEliminationCore = null;
		markovWithUtility = null;

		for (int i = 1; i <= numberOfSlices; i++) {
			Runtime r = Runtime.getRuntime();
			r.gc();
			System.out.println("Cycle" + i);
			// Add probabilities of the new slice
			for (TablePotential probabilityPontial : probabilityPotentialBySlice.get(i)) {
				markovNetworkInference.addPotential(probabilityPontial);
			}

			// Make a copy of the network
			markovWithUtility = markovNetworkInference.copy();

			// Add utilities to the copied network
			for (TablePotential utilityPotential : utilityPotentialBySlice.get(i)) {
				markovWithUtility.addPotential(utilityPotential);
			}

			// Create heuristic instance
			heuristic = heuristicFactory(markovWithUtility, new ArrayList<Variable>(),
					getPreResolutionEvidence().getVariables(), getConditioningVariables(),
					markovWithUtility.getChanceAndDecisionVariables());
			variableEliminationCore = new VariableEliminationCore(markovWithUtility, heuristic, isUnicriterion);
			utilityPotentialsPerSlice.add(variableEliminationCore.getUtility());
			// Delete the network
			variableEliminationCore = null;
			markovWithUtility = null;

			// Remove the probability variables of the previous slice (keeping always atemporal probabilities)
			List<Variable> variablesToEliminate = new ArrayList<>();
			for (TablePotential potentialToDelete : probabilityPotentialBySlice.get(i - 1)) {
				variablesToEliminate.add(potentialToDelete.getVariable(0));
			}

			heuristic = heuristicFactory(markovNetworkInference, new ArrayList<Variable>(),
					getPreResolutionEvidence().getVariables(), conditioningVariables, variablesToEliminate);
			variableEliminationCore = new VariableEliminationCore(markovNetworkInference, heuristic, isUnicriterion);

			markovNetworkInference = variableEliminationCore.getMarkovDecisionNetwork();

		}

	}

	// ------- GETTERS (TO OBTAIN THE RESULTS OF THE ANALYSIS) -----
	public List<TablePotential> getUtilityPotentialsPerSlice()
			throws UnexpectedInferenceException, NotEvaluableNetworkException {
		if (utilityPotentialsPerSlice == null || utilityPotentialsPerSlice.isEmpty()) {
			resolve();
		}
		return utilityPotentialsPerSlice;
	}

	public TablePotential getAtemporalUtility() throws UnexpectedInferenceException, NotEvaluableNetworkException {
		if (atemporalUtility == null) {
			resolve();
		}
		return atemporalUtility;
	}
	//
	//	public ProbNet getProbNet() {
	//		return probNet;
	//	}

}
