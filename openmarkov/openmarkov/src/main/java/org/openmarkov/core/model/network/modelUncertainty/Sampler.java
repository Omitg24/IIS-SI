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

import org.openmarkov.core.model.network.VariableType;
import org.openmarkov.core.model.network.potential.Potential;

public abstract class Sampler {

	protected SamplerUncertainValues samplerUncertainValues;

	protected static void placeInArray(double[] refValue, int[] indexes, double[] x) {
		for (int i = 0; i < indexes.length; i++) {
			refValue[indexes[i]] = x[i];
		}
	}

	public static int[] getIndexesUncertainValuesOfClass(List<UncertainValue> uncertainValues,
			Class<? extends ProbDensFunction> functionClass) {
		List<Class<? extends ProbDensFunction>> classes = new ArrayList<>();
		classes.add(functionClass);
		return getIndexesUncertainValuesOfClasses(uncertainValues, classes);
	}

	protected static List<Class<? extends ProbDensFunction>> initializeTypeFunctions() {
		List<Class<? extends ProbDensFunction>> functionTypes;
		functionTypes = new ArrayList<>();
		functionTypes.add(ComplementFunction.class);
		functionTypes.add(DirichletFunction.class);
		return functionTypes;
	}

	/**
	 * @param uncertainValues List of uncertaing values
	 * @param types           Probability density function types
	 * @return indexes of uncertain values NOT of classes
	 */
	protected static int[] getIndexesUncertainValuesNotOfClasses(List<UncertainValue> uncertainValues,
			List<Class<? extends ProbDensFunction>> types) {
		List<Integer> indexes = new ArrayList<>();
		for (int i = 0; i < uncertainValues.size(); i++) {
			UncertainValue uncertainValue = uncertainValues.get(i);
			ProbDensFunction probDensFunction = uncertainValue.getProbDensFunction();
			boolean isInTypes = false;
			for (int j = 0; (j < types.size()) && !isInTypes; j++) {
				isInTypes = types.get(j).isAssignableFrom(probDensFunction.getClass());
			}
			if (!isInTypes) {
				indexes.add(i);
			}
		}
		int numIndexesOfTypes = indexes.size();
		int[] intIndexes = new int[numIndexesOfTypes];
		for (int i = 0; i < numIndexesOfTypes; i++) {
			intIndexes[i] = indexes.get(i);
		}
		return intIndexes;
	}

	protected static void copyInArray(double[] sampledValues, int configurationBasePosition,
			double[] sampledConfigurationValues) {
		for (int stateIndex = 0; stateIndex < sampledConfigurationValues.length; stateIndex++) {
			sampledValues[configurationBasePosition + stateIndex] = sampledConfigurationValues[stateIndex];
		}

	}

	public static int numElementsInColumn(Potential potential) {
		int numStates;
		// Probability potential
		if (potential.getVariables().get(0).getVariableType().equals(VariableType.NUMERIC)) {
			numStates = 1;
		} else {
			numStates = potential.getVariables().get(0).getNumStates();
		}
		return numStates;
	}

	protected static List<UncertainValue> getUncertainValuesChance(UncertainValue[] uTable, int basePos,
			int numStates) {
		List<UncertainValue> uv;
		uv = new ArrayList<>();
		for (int i = 0; i < numStates; i++) {
			uv.add(uTable[basePos + i]);
		}
		return uv;
	}

	/**
	 * @param uncertainValues Uncertain values
	 * @param types           Probability density function types
	 * @return indexes of uncertain values of classes
	 */
	private static int[] getIndexesUncertainValuesOfClasses(List<UncertainValue> uncertainValues,
			List<Class<? extends ProbDensFunction>> types) {
		List<Integer> indexes = new ArrayList<>();
		for (int i = 0; i < uncertainValues.size(); i++) {
			UncertainValue uncertainValue = uncertainValues.get(i);
			ProbDensFunction probDensFunction = uncertainValue.getProbDensFunction();
			boolean isInTypes = false;
			for (int j = 0; (j < types.size()) && !isInTypes; j++) {
				isInTypes = types.get(j).isAssignableFrom(probDensFunction.getClass());
			}
			if (isInTypes) {
				indexes.add(i);
			}
		}
		int numIndexesOfTypes = indexes.size();
		int[] intIndexes = new int[numIndexesOfTypes];
		for (int i = 0; i < numIndexesOfTypes; i++) {
			intIndexes[i] = indexes.get(i);
		}
		return intIndexes;
	}

	protected double[] generateSample(List<UncertainValue> uncertainValues, int numStates,
			List<Class<? extends ProbDensFunction>> functionTypes) {
		createSamplerUncertainValues(uncertainValues, functionTypes);
		return generateSample(samplerUncertainValues.otherFamily, samplerUncertainValues.dirFamily,
				samplerUncertainValues.complementFamily, samplerUncertainValues.indexesOther,
				samplerUncertainValues.indexesDirichlet, samplerUncertainValues.indexesComplement, numStates);

	}

	protected double[] generateSample(FamilyDistribution otherFamily, DirichletFamily dirFamily,
			ComplementFamily complementFamily, int[] indexesOther, int[] indexesDirichlet, int[] indexesComplement,
			int numStates) {
		Random randomGenerator = createRandomGenerator();
		double[] sampleOther;
		double[] sampleDir;
		double massForComp;
		double[] sampledConfigurationValues = new double[numStates];
		// processes the uncertain values that can be sampled individually
		sampleOther = getSample(otherFamily, randomGenerator);
		placeInArray(sampledConfigurationValues, indexesOther, sampleOther);
		// processes Dirichlet
		sampleDir = getSample(dirFamily, randomGenerator);
		placeInArray(sampledConfigurationValues, indexesDirichlet, sampleDir);
		// Process complements
		massForComp = 1.0 - (Tools.sum(sampleOther));
		complementFamily.setProbMass(massForComp);
		double[] sampleComp = complementFamily.getSample();
		placeInArray(sampledConfigurationValues, indexesComplement, sampleComp);
		return sampledConfigurationValues;
	}

	protected abstract double[] getSample(FamilyDistribution family, Random randomGenerator);

	protected abstract Random createRandomGenerator();

	public void createSamplerUncertainValues(List<UncertainValue> columnUncertainValues,
			List<Class<? extends ProbDensFunction>> functionTypes) {
		samplerUncertainValues = new SamplerUncertainValues(columnUncertainValues, functionTypes);

	}

}
