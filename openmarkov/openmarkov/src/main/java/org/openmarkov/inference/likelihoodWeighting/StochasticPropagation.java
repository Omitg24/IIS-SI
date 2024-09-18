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
import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openmarkov.core.exception.IncompatibleEvidenceException;
import org.openmarkov.core.exception.NotEvaluableNetworkException;
import org.openmarkov.core.inference.InferenceAlgorithm;
import org.openmarkov.core.inference.tasks.Propagation;
import org.openmarkov.core.model.network.EvidenceCase;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.ProbNetOperations;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.constraint.PNConstraint;
import org.openmarkov.core.model.network.modelUncertainty.XORShiftRandom;
import org.openmarkov.core.model.network.potential.PotentialRole;
import org.openmarkov.core.model.network.potential.TablePotential;
import org.openmarkov.core.model.network.type.BayesianNetworkType;
import org.openmarkov.core.model.network.type.NetworkType;

/**
 * Father of likelihood wieghting and logic sampling for bayesian networks. It reunites its many similarities.
 *
 * @author ibermejo
 * @author fjdiez
 * @author iagoparis - spring 2018
 * @version 1.0
 */

public abstract class StochasticPropagation extends InferenceAlgorithm implements Propagation {

    protected Logger logger;

    private static final int DEFAULT_SAMPLE_SIZE = 10000; // Default sample size
    private int sampleSize;

    Random randomGenerator;
    Long seed;

    private EvidenceCase postResolutionEvidence;
    // Post- and pre-resolution evidence combined
    EvidenceCase fusedEvidence;
    List<Variable> sortedVariables;
    private List<Variable> variablesOfInterest;
    List<Variable> variablesToSample;

    // whether the algorithm stores the samples
    private boolean storingSamples;
    // samples (if they are stored)
    private double[][] samples;

    // sum of the weights of the samples
    private double accumulatedWeight;
    // number of samples with a non-null weight
    private int numPositiveSamples;

    private double algorithmExecutionTime;
    // Last output of getPosteriorValues(). Saves running the propagation every time needed. Created to avoid running
    // the propagation inside writeToXlsx method in the StochasticPropagationOutput plugin.
    private HashMap<Variable,TablePotential> lastPosteriorValues;


    StochasticPropagation(ProbNet probNet) throws NotEvaluableNetworkException {
        super(probNet);
        this.logger = LogManager.getLogger(StochasticPropagation.class.getName());
        // Order the variables ancestrally
        this.sortedVariables = ProbNetOperations.sortTopologically(probNet, probNet.getVariables());
        this.variablesOfInterest = new ArrayList<>(sortedVariables);
        this.sampleSize = DEFAULT_SAMPLE_SIZE;
        this.postResolutionEvidence = new EvidenceCase();
        this.storingSamples = false;
        this.seed = null;
    }

    @Override
    protected List<NetworkType> getPossibleNetworkTypes() {
        List<NetworkType> possibleNetworkTypes = new ArrayList<>();
        possibleNetworkTypes.add(BayesianNetworkType.getUniqueInstance());
        return possibleNetworkTypes;
    }

    @Override
    protected List<PNConstraint> getAdditionalConstraints() {
        return new ArrayList<>();
    }

    /**
     * The variables to sample depend on the algorithm and on the evidence
     * @return a list of the variables to sample
     */
    public abstract List<Variable> getVariablesToSample();

    /**
     * Computes a sample, i.e., a configuration with a value for each variable to sample,
     * and the weight of the sample.
     * @return an array containing an integer (the index of the state) for each variable to sample, plus the weight.
     */
    protected abstract double[] getValuesSampledAndWeight();

    /**
     * Propagates the evidence through the net using a stochastic propagation algorithm.
     * It returns the posterior values and, if <code>storingSamples</code> is true,
     * stores the samples in the variable <code>samples</code> .
     *
     * @return the approximate posterior value(s) for each variable of interest.
     * @throws IncompatibleEvidenceException When postResolutionEvidence contradicts preResolutionEvidence, or
     * if all the samples are weighted as 0. It could mean that the evidence case is very specific.
     */
    @Override
    public HashMap<Variable, TablePotential> getPosteriorValues() throws IncompatibleEvidenceException {

        long startTime = System.nanoTime();

        // Fuse post and preResolutionEvidence
        fusedEvidence = new EvidenceCase(postResolutionEvidence);
        fusedEvidence.fuse(getPreResolutionEvidence(), true);

        variablesOfInterest.removeAll(fusedEvidence.getVariables());

        int numOfVariablesOfInterest = variablesOfInterest.size();

        /// Create a table of accumulated probabilities. Initialize it.
        double[][] accumulatedProbabilities = new double[numOfVariablesOfInterest][];
        for (int indexOfVariable = 0; indexOfVariable < numOfVariablesOfInterest; indexOfVariable++) {
            double[] accumulatedProbability = new double[variablesOfInterest.get(indexOfVariable).getNumStates()];
            accumulatedProbabilities[indexOfVariable] = accumulatedProbability;
        }


        variablesToSample = getVariablesToSample();
        int numOfVariablesToSample = variablesToSample.size();

        // each sample contains one integer for the state of each variable plus one double for the weight
        samples = new double[getSampleSize()][numOfVariablesToSample + 1];

        accumulatedWeight = 0;
        numPositiveSamples = 0;

        // auxiliary variables for the "for" loop:
        // values sampled for the variables of interest, plus the weight of the sample
        double[] valuesSampledAndWeight;
        // weight of the sample
        double weight;

        randomGenerator = new XORShiftRandom();
        // If a seed was set, use it
        if (seed != null) {
            randomGenerator.setSeed(seed);
        }

        // sample and store the results
        // for each sample...
        for (int sampleIndex = 0; sampleIndex < sampleSize; sampleIndex++) {
            valuesSampledAndWeight = getValuesSampledAndWeight();

            weight = valuesSampledAndWeight[valuesSampledAndWeight.length - 1];
            accumulatedWeight += weight;
            if (weight > 0) {
                numPositiveSamples++;
            }
            if (storingSamples) {
                samples[sampleIndex][variablesToSample.size()] = weight;
            }

            int stateSampled;
            int indexOfVariableOfInterest = 0;
            for (int indexOfVariable = 0; indexOfVariable < numOfVariablesToSample; indexOfVariable++) {
                // for each variable...
                stateSampled = (int) valuesSampledAndWeight[indexOfVariable];
                indexOfVariableOfInterest = variablesOfInterest.indexOf(variablesToSample.get(indexOfVariable));
//                if (variablesOfInterest.contains(variablesToSample.get(indexOfVariable))) {
                	//wrong index
                if(indexOfVariableOfInterest!=-1) {
                    accumulatedProbabilities[indexOfVariableOfInterest][stateSampled] += weight;
                    indexOfVariableOfInterest++;
                }
                if (storingSamples) {
                    samples[sampleIndex][indexOfVariable] = stateSampled;
                }
            }
        }

        if (accumulatedWeight == 0) {
            logger.warn("All stochastic propagation samples have been weighed as 0");
            throw new IncompatibleEvidenceException("All samples have been weighted as 0.");
        }

        HashMap<Variable, TablePotential> posteriorValues = new HashMap<>();

        // normalize the posterior probabilities
        for (int i = 0; i < numOfVariablesOfInterest; i++) {
            double[] accumulatedValues = accumulatedProbabilities[i];
                // normalize
                double sum = 0;
                for (int j = 0; j < accumulatedValues.length; j++) {
                    sum += accumulatedValues[j];
                }

                // The for loop is divided because the sum must be completed prior to normalization to be possible
                for (int j = 0; j < accumulatedValues.length; j++) {
                    if (sum > 0) {
                        accumulatedValues[j] = accumulatedValues[j] / sum;
                    } // if sum = 0, every accumulated value is 0 and no normalization is needed.
                }

            TablePotential posteriorProbability = new TablePotential(PotentialRole.JOINT_PROBABILITY,
                    variablesOfInterest.get(i));
            posteriorProbability.values = accumulatedValues;
            posteriorValues.put(variablesOfInterest.get(i), posteriorProbability);
        }

        long endTime = System.nanoTime();
        algorithmExecutionTime = ((double) (endTime - startTime))/1000000; // Turn to milliseconds

        setLastPosteriorValues(posteriorValues);
        return posteriorValues;

    } // End of getPosteriorValues


    public void setSampleSize(int sampleSize) {
        this.sampleSize = sampleSize;
    }

    public int getSampleSize() {
        return sampleSize;
    }

    public void setSeed(Long seed) {
        this.seed = seed;
    }

    public double getAlgorithmExecutionTime() {
        return algorithmExecutionTime;
    }

    @Override
    public void setVariablesOfInterest(List<Variable> variablesOfInterest) {
        this.variablesOfInterest = variablesOfInterest;
    }

    // Storage getters and setters
    public void setStoringSamples(boolean storingSamples) {
        this.storingSamples = storingSamples;
    }

    public double[][] getSamples() {
        return samples;
    }


    public int getNumPositiveSamples() {
        return numPositiveSamples;
    }

    public double getAccumulatedWeight() {
        return accumulatedWeight;
    }

    /**
     * Returns the ratio of non-null samples versus total samples.
     */
    public double getPositiveSampleRatio() {
        return numPositiveSamples / (double) sampleSize;
    }

    @Override
    public void setPostResolutionEvidence(EvidenceCase postResolutionEvidence) {
        this.postResolutionEvidence = postResolutionEvidence;
    }

    public EvidenceCase getFusedEvidence() {
        return fusedEvidence;
    }

    public HashMap<Variable, TablePotential> getLastPosteriorValues() {
        return lastPosteriorValues;
    }

    private void setLastPosteriorValues(HashMap<Variable, TablePotential> lastPosteriorValues) {
        this.lastPosteriorValues = lastPosteriorValues;
    }
}
