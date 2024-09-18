/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */
package org.openmarkov.inference.likelihoodWeighting;

import java.util.HashMap;
import java.util.List;

import org.openmarkov.core.exception.NotEvaluableNetworkException;
import org.openmarkov.core.inference.annotation.InferenceAnnotation;
import org.openmarkov.core.inference.tasks.Propagation;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.potential.Potential;

/**
 * Logic Sampling algorithm for bayesian networks.
 *
 * @author ibermejo
 * @author fjdiez
 * @author iagoparis - spring 2018
 * @version 1.0
 */


@InferenceAnnotation(name = "LogicSampling")
public class LogicSampling extends StochasticPropagation implements Propagation {

	public LogicSampling(ProbNet probNet) throws NotEvaluableNetworkException {
		super(probNet);
	}

    /**
     * Creates a random state of the net sampling every node. Then weights this state against the evidence case
     * stored in {@code evidence} (In Logic Sampling, matching sampled evidence variable with the findings.)
     * Last, stores in an array the net state (ordered by variable ancestrally) and the weight and returns it.
     *
     *
     * @return a double array that includes first the samples of every sampled variable, ordered ancestrally
     * and last the weight of the state of the net resulting from that sample configuration.
     */
    @Override
    protected double[] getValuesSampledAndWeight() {

        HashMap<Variable, Integer> netState = new HashMap<>();

        double[] valuesAndWeight = new double[variablesToSample.size() + 1];

        for (int indexOfVariable = 0; indexOfVariable < variablesToSample.size(); indexOfVariable++) {

            // Extract potential
            Potential potential = probNet.getNode(variablesToSample.get(indexOfVariable)).getPotentials().get(0);

            // Sample
            int stateSampled = potential.sampleConditionedVariable(randomGenerator, netState);
            netState.put(potential.getVariable(0), stateSampled);
            valuesAndWeight[indexOfVariable] = stateSampled;
        }

        // List of findings

        // Weight matching sampled evidence variable with the findings
        List<Variable> variablesOfEvidence = fusedEvidence.getVariables();
        double weight = 1.0;
        for (Variable variable : variablesOfEvidence) {

            Integer finding = fusedEvidence.getFinding(variable).getStateIndex();
            if (!netState.get(variable).equals(finding)) {
                weight = 0;
                break;
            }

        }
        valuesAndWeight[variablesToSample.size()] = weight;

        return valuesAndWeight;
    }

    @Override
    public List<Variable> getVariablesToSample() {
		return sortedVariables;
	}

}