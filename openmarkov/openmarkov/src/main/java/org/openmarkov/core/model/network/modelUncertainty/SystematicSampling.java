/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.model.network.modelUncertainty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.VariableType;
import org.openmarkov.core.model.network.potential.Potential;
import org.openmarkov.core.model.network.potential.TablePotential;
import org.openmarkov.core.model.network.potential.treeadd.TreeADDBranch;
import org.openmarkov.core.model.network.potential.treeadd.TreeADDPotential;

public class SystematicSampling extends Sampler {

	ProbNet network;

	/**
	 * @param net Network
	 * @return The UncertainParameters built from "net"
	 */
	public static List<UncertainParameter> getUncertainParameters(ProbNet net) {
		List<Potential> potentials = net.getPotentials();
		List<UncertainParameter> uncertainParams = new ArrayList<>();
		for (Potential potential : potentials) {
			Set<UncertainParameter> auxUncertainParameters = getUncertainParameters(potential);
			if (auxUncertainParameters != null) {
				uncertainParams.addAll(auxUncertainParameters);
			}
		}
		return uncertainParams;
	}

	/**
	 * @param potential Potential
	 * @return A set of UncertainParameters built from the uncertain values appearing in "potential"
	 */
	private static Set<UncertainParameter> getUncertainParameters(Potential potential) {
		Set<UncertainParameter> uncertainParams = new HashSet<>();

		Hashtable<UncertainValue, SubPotentialAndPosition> uncertainValues = getUncertainValues(potential);

		for (UncertainValue auxUncertainValue : uncertainValues.keySet()) {
			SubPotentialAndPosition subPotentialAndPosition = uncertainValues.get(auxUncertainValue);
			uncertainParams
					.add(new UncertainParameter(potential, auxUncertainValue, subPotentialAndPosition.getSubPotential(),
							subPotentialAndPosition.getPosition()));
		}
		return uncertainParams;
	}

	/**
	 * @param potential Potential
	 * @return A hash table with the uncertain values appearing in potential, and for each one the hash value is the subpotential where appearing
	 */
	private static Hashtable<UncertainValue, SubPotentialAndPosition> getUncertainValues(Potential potential) {
		Hashtable<UncertainValue, SubPotentialAndPosition> uncertainValuesHash = new Hashtable<>();

		if (potential instanceof TablePotential) {
			TablePotential tablePotential = (TablePotential) potential;
			UncertainValue[] uncertainValuesPotential = tablePotential.getUncertainValues();
			if (uncertainValuesPotential != null) {
				int i = 0;
				for (UncertainValue auxUncertain : uncertainValuesPotential) {
					if (auxUncertain != null) {
						addIfNonExisting(uncertainValuesHash, auxUncertain,
								new SystematicSampling.SubPotentialAndPosition(tablePotential, i));
					}
					i = i + 1;
				}
			}
		} else {
			if (potential instanceof TreeADDPotential) {
				for (TreeADDBranch branch : ((TreeADDPotential) potential).getBranches()) {
					if (branch != null) {
						Potential branchPotential = branch.getPotential();
						if (branchPotential != null) {
							Hashtable<UncertainValue, SubPotentialAndPosition> auxUncertainValues = getUncertainValues(
									branchPotential);
							for (UncertainValue auxUncertain : auxUncertainValues.keySet()) {
								addIfNonExisting(uncertainValuesHash, auxUncertain,
										auxUncertainValues.get(auxUncertain));
							}
						}

					}
				}
			}
		}
		return uncertainValuesHash;
	}

	/**
	 * @param uncertainValuesHash     Hastable of uncertainvalues and subpotential and position elements
	 * @param auxUncertain            Auxiliary uncertain value
	 * @param subPotentialAndPosition Subpotential and position of the auxiliary uncertain value
	 *                                Adds the key, value pair (auxUncertain, tablePotential) to "uncertainValuesHash" if "auxUncertain" does not belong to the key set
	 */
	private static void addIfNonExisting(Hashtable<UncertainValue, SubPotentialAndPosition> uncertainValuesHash,
			UncertainValue auxUncertain, SubPotentialAndPosition subPotentialAndPosition) {
		if (!uncertainValuesHash.containsKey(auxUncertain)) {
			uncertainValuesHash.put(auxUncertain, subPotentialAndPosition);
		}
	}

	/**
	 * @param potential   Potential
	 * @param newVariable New variable
	 * @return Adds a variable to the end of the list variables of "potential", and replicates the original values and uncertainValues
	 */
	private static TablePotential addVariableReplicatingValuesAndUncertainValues(TablePotential potential,
			Variable newVariable) {
		// creates the new potential
		List<Variable> newVariables = new ArrayList<>(potential.getVariables());
		newVariables.add(newVariable);
		TablePotential newPotential = new TablePotential(newVariables, potential.getPotentialRole());
		// assigns the values of the new potential
		int newVariableNumStates = newVariable.getNumStates();
		double[] values = potential.getValues();
		UncertainValue[] uncertainValues = potential.getUncertainValues();
		boolean hasUncertainty = (uncertainValues != null) && (uncertainValues.length > 0);
		if (hasUncertainty) {
			newPotential.uncertainValues = new UncertainValue[newPotential.getTableSize()];
		}

		for (int i = 0; i < newVariableNumStates; i++) {

			int offset = i * values.length;
			for (int j = 0; j < values.length; j++) {
				int newPos = j + offset;
				newPotential.values[newPos] = values[j];
				if (hasUncertainty) {
					newPotential.uncertainValues[newPos] = uncertainValues[j];
				}
			}
		}
		return newPotential;
	}

	private static ProbNet sampleNetwork(ProbNet originalNet, List<ParameterAnalysisInformation> parameters,
			int numIntervals) {
		ProbNet net = originalNet.copy();

		UncertainParameter uncertainParameter;
		int numPoints = numIntervals + 1;
		List<Class<? extends ProbDensFunction>> functionTypes = initializeTypeFunctions();

		for (ParameterAnalysisInformation parameter : parameters) {
			uncertainParameter = parameter.uncertainParameter;
			if (uncertainParameter != null) {

				String iterationVariableName = parameter.iterationVariableName;
				Variable iterVariable = new Variable(iterationVariableName, numPoints);
				Potential originalPotential = uncertainParameter.potential;
				Potential newPotential = originalPotential.copy();
				TablePotential originalSubPotential = uncertainParameter.subPotential;
				TablePotential newSubPotential;
				if (originalPotential != originalSubPotential) {
					newPotential.addVariable(iterVariable);
					newSubPotential = (TablePotential) originalSubPotential.copy();
				} else {
					newSubPotential = (TablePotential) newPotential;
				}
				int position = getPosition(originalSubPotential, uncertainParameter.uncertainValue);
				int posUncertainInColumn = calculatePositionUncertainInColumn(originalSubPotential, position);
				int originalValuesLength = originalSubPotential.getTableSize();
				TablePotential newTablePotential = addVariableReplicatingValuesAndUncertainValues(originalSubPotential,
						iterVariable);
				newSubPotential.setVariables(newTablePotential.getVariables());
				newSubPotential.setValues(newTablePotential.getValues());
				newSubPotential.setUncertainValues(newTablePotential.getUncertainValues());
				double min = parameter.min;
				double pointsDistance = (parameter.max - min) / numIntervals;
				int numStates = numElementsInColumn(originalSubPotential);
				int configurationBasePositionInitColumn = position - posUncertainInColumn;
				List<UncertainValue> columnUncertainValues = getUncertainValuesChance(
						originalSubPotential.uncertainValues, configurationBasePositionInitColumn, numStates);
				Sampler sampler = new SystematicSampling();
				double[] sampledConfigurationValues = sampler
						.generateSample(columnUncertainValues, numStates, functionTypes);

				double[] auxSampledConfigurationValues = new double[numStates];
				double auxValueToAssign = min;
				for (int i = 0; i < numPoints; i++) {
					System.arraycopy(sampledConfigurationValues, 0, auxSampledConfigurationValues, 0, numStates);
					double[] newSubpotentialValues = newSubPotential.values;
					replaceValueAndRedistributeComplements(auxSampledConfigurationValues, sampler, posUncertainInColumn,
							auxValueToAssign);
					copyInArray(newSubpotentialValues, configurationBasePositionInitColumn + i * originalValuesLength,
							auxSampledConfigurationValues);
					//TODO Distribute the probability mass when changing one value
					/*newSubPotential.values[position + i * originalValuesLength] = min
							+ i * pointsDistance;*/
					auxValueToAssign += pointsDistance;
				}
				if (originalPotential != originalSubPotential) {
					replace((TreeADDPotential) newPotential, originalSubPotential, newSubPotential);
				}
				net.removePotential(originalPotential);
				net.addPotential(newPotential);
			}
		}
		return net;
	}

	private static void replaceValueAndRedistributeComplements(double[] samples, Sampler sampler, int posToReplace,
			double newValue) {
		double oldValue = samples[posToReplace];
		samples[posToReplace] = newValue;
		ComplementFamily complemFamily = sampler.samplerUncertainValues.complementFamily;
		ComplementFamily auxComplementFamily = new ComplementFamily(complemFamily.family);
		auxComplementFamily.setProbMass(complemFamily.getProbMass() + oldValue - newValue);
		double[] newComplementSamples = auxComplementFamily.getSample();
		placeInArray(samples, sampler.samplerUncertainValues.indexesComplement, newComplementSamples);
	}

	private static int calculatePositionUncertainInColumn(TablePotential potential, int position) {
		int posInCol;
		if (potential.getVariable(0).getVariableType().equals(VariableType.NUMERIC)) {
			posInCol = 0;
		} else {//Probability potential
			Variable var = potential.getVariable(0);
			posInCol = position % var.getNumStates();
		}
		return posInCol;
	}

	/**
	 * @param originalNet           Original network
	 * @param uncertainParameter    Uncertain parameter
	 * @param min                   Min
	 * @param max                   Max
	 * @param numIntervals          Number of intervals
	 * @param iterationVariableName Conditioned variable
	 * @return The original network "net" where the sub potential table where "parameterName" appears has been conditioned in the variable
	 * "interationVariable" name, and for state of this variable the cell of the parameter has been assigned one of the equally distant
	 * points between min and max.
	 */
	public static ProbNet sampleNetwork(ProbNet originalNet, UncertainParameter uncertainParameter, double min,
			double max, int numIntervals, String iterationVariableName) {
		List<ParameterAnalysisInformation> parameters = Collections
				.singletonList(new ParameterAnalysisInformation(uncertainParameter, min, max, iterationVariableName));
		return SystematicSampling.sampleNetwork(originalNet, parameters, numIntervals);
	}

	public static ProbNet sampleNetwork(ProbNet originalNet, UncertainParameter uncertainParameter1, double min1,
			double max1, UncertainParameter uncertainParameter2, double min2, double max2, int numIntervals,
			String iterationVariableName1, String iterationVariableName2) {
		List<ParameterAnalysisInformation> parameters = Arrays
				.asList(new ParameterAnalysisInformation(uncertainParameter1, min1, max1, iterationVariableName1),
						new ParameterAnalysisInformation(uncertainParameter2, min2, max2, iterationVariableName2));
		return SystematicSampling.sampleNetwork(originalNet, parameters, numIntervals);
	}

	private static void replace(TreeADDPotential potential, TablePotential subPotToReplace, TablePotential newSubPot) {

		List<TreeADDBranch> branches = potential.getBranches();
		if (branches != null)
			for (TreeADDBranch treeADDBranch : branches) {
				if (treeADDBranch != null) {
					replace(treeADDBranch, subPotToReplace, newSubPot);
				}
			}
	}

	private static void replace(TreeADDBranch treeADDBranch, TablePotential subPotToReplace, TablePotential newSubPot) {
		Potential potential = treeADDBranch.getPotential();
		if (potential == subPotToReplace) {
			treeADDBranch.setPotential(newSubPot);
		} else {
			if (potential != null && potential instanceof TreeADDPotential) {
				replace((TreeADDPotential) potential, subPotToReplace, newSubPot);
			}
		}
	}

	public static UncertainParameter getUncertainParameter(ProbNet net, String parameterName) {
		List<UncertainParameter> uncertainParameters = SystematicSampling.getUncertainParameters(net);
		return getUncertainParameter(uncertainParameters, parameterName);
	}

	/**
	 * @param potential      Potential
	 * @param uncertainValue Uncertain value
	 * @return The position in the uncertain values table of "potential" where "uncertainValue" is placed
	 */
	private static int getPosition(TablePotential potential, UncertainValue uncertainValue) {
		int pos = -1;
		boolean found = false;
		UncertainValue[] uncertainValues = potential.getUncertainValues();
		for (int i = 0; i < uncertainValues.length && !found; i++) {
			found = uncertainValues[i] == uncertainValue;
			if (found) {
				pos = i;
			}
		}
		return pos;
	}

	/*public static TablePotential getExpectedUtilitySystematicSampling(ProbNet net, String parameterName, double min, double max,
			 int numIntervals, String iterationVariableName){
		ProbNet newNet = sampleNetwork(net,parameterName,min,max,numIntervals,iterationVariableName);
		Variable iterVariable = null;
		try {
			iterVariable = newNet.getVariable(iterationVariableName);
		} catch (NodeNotFoundException e) {
			e.printStackTrace();
		}
		VariableElimination algorithm = null;
		try {
			algorithm = new VariableElimination(newNet);
		} catch (NotEvaluableNetworkException e) {
			e.printStackTrace();
		}
		algorithm.setConditioningVariables(Arrays.asList(iterVariable));
		TablePotential globalUtility = null;
		try {
			globalUtility = algorithm.getGlobalUtility();
		} catch (IncompatibleEvidenceException e) {
			e.printStackTrace();
		} catch (UnexpectedInferenceException e) {
			e.printStackTrace();
		}
		return globalUtility;
		
	}*/

	/**
	 * @param uncertainParameters List of uncertain parameters
	 * @param parameterName       Parameter name
	 * @return The UncertainParameter that corresponds to "parameterName"
	 */
	private static UncertainParameter getUncertainParameter(List<UncertainParameter> uncertainParameters,
			String parameterName) {
		UncertainParameter paramFound = null;
		boolean found = false;
		for (int i = 0; i < uncertainParameters.size() && !found; i++) {
			UncertainParameter auxUncertain = uncertainParameters.get(i);
			String auxName = auxUncertain.uncertainValue.getName();
			if (auxName != null && auxName.equals(parameterName)) {
				found = true;
				paramFound = auxUncertain;
			}
		}
		return paramFound;
	}

	@Override protected double[] getSample(FamilyDistribution family, Random randomGenerator) {
		return family.getMean();
	}

	@Override protected Random createRandomGenerator() {
		// TODO Auto-generated method stub
		return null;
	}

	private static class SubPotentialAndPosition {

		private TablePotential subPotential;
		private int position;

		public SubPotentialAndPosition(TablePotential subPotential, int position) {
			this.subPotential = subPotential;
			this.position = position;
		}

		public TablePotential getSubPotential() {
			return subPotential;
		}

		public int getPosition() {
			return position;
		}

	}

	private static class ParameterAnalysisInformation {
		UncertainParameter uncertainParameter;
		double min;
		double max;
		String iterationVariableName;

		public ParameterAnalysisInformation(UncertainParameter uncertainParameter, double min, double max,
				String iterationVariableName) {
			this.uncertainParameter = uncertainParameter;
			this.min = min;
			this.max = max;
			this.iterationVariableName = iterationVariableName;
		}

	}
}
