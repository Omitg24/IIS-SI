/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */
package org.openmarkov.inference.likelihoodWeighting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.openmarkov.core.exception.NotEvaluableNetworkException;
import org.openmarkov.core.inference.annotation.InferenceAnnotation;
import org.openmarkov.core.inference.tasks.Propagation;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.potential.Potential;

/**
 * Likelihood Weighting algorithm for bayesian networks.
 *
 * @author ibermejo
 * @author fjdiez
 * @author iagoparis - spring 2018
 * @version 1.1
 */


@InferenceAnnotation(name = "LikelihoodWeighting")
public class LikelihoodWeighting extends StochasticPropagation implements Propagation {

    public LikelihoodWeighting(ProbNet probNet) throws NotEvaluableNetworkException {
        super(probNet);
    }

	@Override
	protected double[] getValuesSampledAndWeight() {

        // The configuration of the net, the states of all the variables
        HashMap<Variable, Integer> configuration = new HashMap<>();
        // The states of only the sampled variables
        double[] valuesSampledAndWeight = new double[variablesToSample.size() + 1];

        int indexOfVariableToSample = 0;
        for (Variable variable : sortedVariables) {

            // Set in the configuration the findings
            if (fusedEvidence.contains(variable)) {
                int finding = fusedEvidence.getFinding(variable).getStateIndex();
                configuration.put(variable, finding);

            // Set in the configuration the sampled states
            } else {
                // Extract potential
                Potential potential = probNet.getNode(variable).getPotentials().get(0);

                // Sample
                int stateSampled = potential.sampleConditionedVariable(randomGenerator, configuration);
                configuration.put(variable, stateSampled);
                valuesSampledAndWeight[indexOfVariableToSample] = stateSampled;
                indexOfVariableToSample++;
            }
        }

		/// Extract potentials to compute weight
		List<Potential> potentialsOfEvidence = new ArrayList<>();
        List<Variable> variablesOfEvidence = fusedEvidence.getVariables();

		for (Variable variable : variablesOfEvidence) {
			try {
				potentialsOfEvidence.add(probNet.getNode(variable).getPotentials().get(0));
			} catch (NullPointerException e) {
			    logger.error("Variable " + variable.getName() + " has no Potential");
			    // Send the name of the variable without potential to GUI
				throw new NullPointerException(variable.getName());
			}
		}

		// Weight by conditional probabilities of evidence findings
		double weight = 1.0;
		for (Potential potential : potentialsOfEvidence) {
			// [Iñigo] Ignore potentials that belong to parentless nodes as they are constant across samples

			// [Iago] It means: don't enter if parentless evidence node with P(sample) != 0. It is rebasing
            // the maximum weight to 1 in the case of a common factor reducing every sample
            // (parentless evidence node case).
			if (potential.getVariables().size() > 1 || potential.getProbability(configuration) == 0) {
				weight *= potential.getProbability(configuration);
			}
		}
        valuesSampledAndWeight[variablesToSample.size()] = weight;

		return valuesSampledAndWeight;
	}

	@Override
    public List<Variable> getVariablesToSample() {
		List<Variable> variablesToSample = new ArrayList<>(sortedVariables);
		variablesToSample.removeAll(fusedEvidence.getVariables());
		return variablesToSample;
	}

}
