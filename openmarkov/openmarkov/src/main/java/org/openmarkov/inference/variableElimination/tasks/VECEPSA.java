/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.inference.variableElimination.tasks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.openmarkov.core.exception.IncompatibleEvidenceException;
import org.openmarkov.core.exception.NotEvaluableNetworkException;
import org.openmarkov.core.exception.UnexpectedInferenceException;
import org.openmarkov.core.inference.tasks.CEAnalysis;
import org.openmarkov.core.inference.tasks.CE_PSA;
import org.openmarkov.core.model.network.Node;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.potential.GTablePotential;
import org.openmarkov.core.model.network.potential.Potential;

/**
 * @author jperez-martin
 */
public class VECEPSA extends VariableElimination implements CE_PSA {

	private Collection<GTablePotential> ceaResults;

	private int progress;

	private boolean useMultithreading;

	private int numSimulations;

	private Variable decisionVariable;

	/**
	 * @param network a symmetric network having at least two criteria (and usually decisions and utility nodes)
	 */
	public VECEPSA(ProbNet network) throws NotEvaluableNetworkException {
		super(network);

	}

	private void resolve()
			throws NotEvaluableNetworkException, UnexpectedInferenceException, IncompatibleEvidenceException {
		List<GTablePotential> results = new ArrayList<>();
		progress = 0;
		if (useMultithreading) {
			int numThreads = Runtime.getRuntime().availableProcessors();
			boolean success = false;
			while (!success && numThreads > 0) {
				ExecutorService executor = Executors.newFixedThreadPool(numThreads);
				List<Future<GTablePotential>> list = new ArrayList<>();
				for (int i = 0; i < numSimulations; ++i) {
					Simulation simulation = new Simulation(probNet);
					list.add(executor.submit(simulation));
				}
				int simulationIndex = 0;
				try {
					for (Future<GTablePotential> result : list) {
						results.add(result.get());
						progress = simulationIndex * 100 / numSimulations;
						simulationIndex++;
					}
					success = true;
				} catch (InterruptedException | ExecutionException e) {
					System.out.println("WARNING: PSA failed with " + numThreads + " threads.");
					e.printStackTrace();
					System.out.println(e.getMessage());
					results.clear();
					numThreads /= 2;
				}
			}
			this.ceaResults = results;
			progress = 100;
		} else {
			for (int i = 0; i < numSimulations; ++i) {
				sampleNetworkPotentials(probNet);
				CEAnalysis veEvaluation;
				veEvaluation = new VECEAnalysis(probNet);
				veEvaluation.setPreResolutionEvidence(getPreResolutionEvidence());
				veEvaluation.setDecisionVariable(this.decisionVariable);
				results.add(veEvaluation.getUtility());
				progress = i * 100 / numSimulations;
			}
			this.ceaResults = results;
			progress = 100;
		}
	}

	public void setUseMultithreading(boolean useMultithreading) {
		this.useMultithreading = useMultithreading;
	}

	public void setNumSimulations(int numSimulations) {
		this.numSimulations = numSimulations;
	}

	private void sampleNetworkPotentials(ProbNet probNet) {
		for (Node node : probNet.getNodes()) {
			List<Potential> sampledPotentials = new ArrayList<>();
			for (Potential potential : node.getPotentials()) {
				sampledPotentials.add(potential.sample());
			}
			node.setPotentials(sampledPotentials);
		}

	}

	public Collection<GTablePotential> getCEPPotentials()
			throws NotEvaluableNetworkException, IncompatibleEvidenceException, UnexpectedInferenceException {
		if (ceaResults == null) {
			resolve();
		}
		return ceaResults;
	}

	@Override public void setDecisionVariable(Variable decisionSelected) {
		this.decisionVariable = decisionSelected;
	}

	private class Simulation implements Callable<GTablePotential> {

		ProbNet probNet;

		Simulation(ProbNet probNet) {
			super();
			this.probNet = probNet;
		}

		@Override public GTablePotential call() throws Exception {
			sampleNetworkPotentials(probNet);
			GTablePotential result = null;
			CEAnalysis veEvaluation;
			try {
				veEvaluation = new VECEAnalysis(probNet);
				veEvaluation.setPreResolutionEvidence(getPreResolutionEvidence());
				veEvaluation.setDecisionVariable(decisionVariable);
				result = veEvaluation.getUtility();
			} catch (NotEvaluableNetworkException | IncompatibleEvidenceException e) {
				e.printStackTrace();
			}
			return result;
		}
	}

}
