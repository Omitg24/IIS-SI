/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.model.network.modelUncertainty;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.potential.TablePotential;

/**
 * TablePotentialSampler generates samples of table potentials
 *
 * @author manolo
 */
public class TablePotentialSampler extends Sampler {

	public TablePotentialSampler() {
	}

	public static boolean hasUncertainValuesUtility(UncertainValue[] uTable, int basePosition) {
		return uTable[basePosition] != null;
	}

	/**
	 * @param inputTablePotential Variable indexing the number of
	 *                            simulation. The number of simulations performed is the number
	 *                            of states of this variable
	 * @return A sampled potential table
	 */
	public TablePotential sample(TablePotential inputTablePotential) {
		TablePotential sampledTablePotential = null;
		int inputTableSize;
		List<Class<? extends ProbDensFunction>> functionTypes = initializeTypeFunctions();
		List<UncertainValue> uncertainValues = null;
		double[] sampledConfigurationValues;
		int numStates;
		UncertainValue[] uTable = inputTablePotential.getUncertainValues();
		double[] originalValues = inputTablePotential.getValues();
		if (!(inputTablePotential.getUncertainValues() == null)) {
			List<Variable> inputPotentialVariables = inputTablePotential.getVariables();
			List<Variable> sampledPotentialVariables = new ArrayList<>(inputPotentialVariables);
			sampledTablePotential = new TablePotential(sampledPotentialVariables,
					inputTablePotential.getPotentialRole());
			double[] sampledValues = sampledTablePotential.values;
			sampledTablePotential.setUncertainValues(inputTablePotential.getUncertainValues());
			numStates = numElementsInColumn(inputTablePotential);
			sampledConfigurationValues = new double[numStates];
			// Number of configurations of the conditioning variables
			inputTableSize = inputTablePotential.getTableSize();
			int numConfigurations = inputTableSize / numStates;
			boolean hasUncertainty;
			// iterates over the configurations
			for (int configurationIndex = 0; configurationIndex < numConfigurations; configurationIndex++) {
				int configurationBasePosition = numStates * configurationIndex;
				uncertainValues = getUncertainValuesChance(uTable, configurationBasePosition, numStates);
				hasUncertainty = uncertainValues.get(0) != null;
				if (hasUncertainty) {
					sampledConfigurationValues = generateSample(uncertainValues, numStates, functionTypes);
					// copies the auxiliary them in the auxiliary vector
					// 'sampledConfigurationValues'
					copyInArray(sampledValues, configurationBasePosition, sampledConfigurationValues);
				} else {
					// takes the values from the original potential and places
					// them in the auxiliary vector 'sampledConfigurationValues'
					for (int stateIndex = 0; stateIndex < numStates; stateIndex++) {
						sampledValues[configurationBasePosition + stateIndex] = originalValues[configurationBasePosition
								+ stateIndex];
					}
				}
			}
		} else {// There is no uncertainty for the input potential
			return inputTablePotential;
		}
		return sampledTablePotential;
	}

	@Override protected Random createRandomGenerator() {
		return new XORShiftRandom();
	}

	@Override protected double[] getSample(FamilyDistribution family, Random randomGenerator) {
		return family.getSample(randomGenerator);
	}

}
