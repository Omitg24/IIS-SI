/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.model.network.potential;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import org.openmarkov.core.exception.IncompatibleEvidenceException;
import org.openmarkov.core.exception.InvalidStateException;
import org.openmarkov.core.exception.NonProjectablePotentialException;
import org.openmarkov.core.exception.WrongCriterionException;
import org.openmarkov.core.inference.InferenceOptions;
import org.openmarkov.core.model.network.EvidenceCase;
import org.openmarkov.core.model.network.Finding;
import org.openmarkov.core.model.network.Node;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.VariableType;
import org.openmarkov.core.model.network.modelUncertainty.TablePotentialSampler;
import org.openmarkov.core.model.network.modelUncertainty.UncertainValue;
import org.openmarkov.core.model.network.potential.plugin.PotentialType;

/**
 * A {@code TablePotential} is a type of relation with a list of
 * probabilistic nodes. All variables will be discrete in this class.
 * <p>
 * Attributes {@code dimensions} and {@code offsets} only make sense
 * when the number of variables is greater than 0. Please be careful to check it
 * when necessary.
 *
 * @author marias
 * @author fjdiez
 * @version 1.0
 * @since OpenMarkov 1.0
 */
@PotentialType(name = "Table") public class TablePotential extends Potential implements Comparable<TablePotential> {
	// Attributes
	/**
	 * Table storing the numerical values of the potential. This attribute is
	 * public for efficiency and volatile for efficiency in concurrent
	 * operations.
	 */
	public volatile double[] values;
	public volatile StrategyTree[] strategyTrees;
	/**
	 * Table storing the values of the potential for the sensitivity analysis.
	 * This attribute is public for efficiency and volatile for efficiency in
	 * concurrent operations.
	 */
	public volatile UncertainValue[] uncertainValues;
	/**
	 * Dimensions (number of states) of the variables.
	 */
	protected int[] dimensions;
	/**
	 * Offsets of the variables in the table that represents this potential.
	 */
	protected int[] offsets;
	/**
	 * Indicates the first configuration. In a new potential it is 0. In a
	 * projected potential it may be different from 0.
	 */
	protected int initialPosition = 0;
	/**
	 * Indicates the number of configurations in this potentials. Note that this
	 * number can be less than {@code table.length} when the
	 * {@code TablePotential} is a projection.
	 */
	protected int tableSize;

	// Constructors

	/**
	 * @param variables . {@code List} of {@code Variable} used to build the
	 *                  {@code TablePotential}.
	 * @param role      . {@code PotentialRole}
	 */
	public TablePotential(List<Variable> variables, PotentialRole role) {
		super(variables, role);
		int numVariables = (variables != null) ? variables.size() : 0;
		if (numVariables != 0) {
			dimensions = TablePotential.calculateDimensions(variables);
			offsets = TablePotential.calculateOffsets(dimensions);
			tableSize = computeTableSize(variables);
			try {
				values = new double[tableSize];
			} catch (NegativeArraySizeException e) {
				throw new OutOfMemoryError();
			}
			setUniform(); // Initializes the table as an uniform potential
		} else {// In this case the potential is a constant
			tableSize = 1;
			values = new double[tableSize];
			offsets = new int[0];
		}
        /*if (role == PotentialRole.INTERVENTION) {
        	interventions = new Intervention[tableSize];
        }*/
	}

	/**
	 * @param variables . {@code ArrayList} of {@code Variable}
	 * @param role      . {@code PotentialRole}
	 * @param table     . {@code double[]}
	 * Condition: All variables must be discrete.
	 */
	public TablePotential(List<Variable> variables, PotentialRole role, double[] table) {
		this(variables, role);
		this.values = table;
	}

	/**
	 * @param role      . {@code PotentialRole}
	 * @param variables . {@code ArrayList} of {@code Variable}
	 * Condition: All variables must be discrete.
	 */
	public TablePotential(PotentialRole role, Variable... variables) {
		this(toList(variables), role);
	}

	/**
	 * Internal constructor used to create a projected potential.
	 *
	 * @param variables       . {@code ArrayList} of {@code Variable}
	 * @param role            . {@code PotentialRole}
	 * @param table           . {@code double[]}
	 * @param initialPosition First position in {@code table} (used in projected
	 *                        potentials).
	 * @param offsets         of variables. {@code int[]}
	 * @param dimensions      . Number of states of each variable. {@code int[]}
	 */
	private TablePotential(List<Variable> variables, PotentialRole role, double[] table, int initialPosition,
			int[] offsets, int[] dimensions) {
		super(variables, role);
		// this.originalVariables = this.variables;
		this.values = table;
		this.initialPosition = initialPosition;
		this.offsets = offsets;
		this.dimensions = dimensions;
		tableSize = computeTableSize(variables);
	}

	public TablePotential(TablePotential potential) {
		super(potential);
		this.initialPosition = potential.getInitialPosition();
		this.offsets = potential.getOffsets();
		this.dimensions = potential.getDimensions();
		tableSize = potential.tableSize;
		values = potential.values.clone();
		uncertainValues = potential.uncertainValues;
		strategyTrees = potential.strategyTrees;
	}

	// Methods

	/**
	 * Returns if an instance of a certain Potential type makes sense given the
	 * variables and the potential role.
	 *
	 * @param node      . {@code Node}
	 * @param variables . {@code List} of {@code Variable}.
	 * @param role      . {@code PotentialRole}.
	 * @return True   if an instance of a certain Potential type makes sense given the variables and the potential role.
	 */
	public static boolean validate(Node node, List<Variable> variables, PotentialRole role) {
		boolean suitable = true;
		int i = 0;
		while (suitable && i < variables.size()) {
			suitable &= variables.get(i).getVariableType() == VariableType.FINITE_STATES
					|| variables.get(i).getVariableType() == VariableType.DISCRETIZED;
			++i;
		}
		return suitable;
	}

	/**
	 * The accumulated offset represents the increment (positive or negative) in
	 * the corresponding position of the table when a variable is incremented
	 * given an ordering of the variables of other potential.
	 *
	 * <big><b> Accumulated Offsets example
	 *
	 * </b></big> We have two potentials: Potential <b>Y</b> (b, d, a, c) and
	 * Potential <b>X</b> (a, b, c). All variables are binary for simplicity.
	 *
	 * <table border="2">
	 * <caption> Table</caption>
	 * <tr>
	 * <td><b>Y</b></td>
	 * <td><b>pos<sub>y</sub>(Y)</b></td>
	 * <td><b>Y<sup>X</sup></b></td>
	 * <td><b>pos<sub>X</sub>(Y<sup>X</sup>)</b></td>
	 * <td><b>varToIncr(Y)</b></td>
	 * <td><b>accOffset</b></td>
	 * </tr>
	 * <tr>
	 * </tr>
	 * <tr>
	 * <td>[b<sub>0</sub>,d<sub>0</sub>,a<sub>0</sub>,c<sub>0</sub>]</td>
	 * <td>0</td>
	 * <td>[a<sub>0</sub>,b<sub>0</sub>,c<sub>0</sub>]</td>
	 * <td>0</td>
	 * <td>0(B)</td>
	 * <td>+2</td>
	 * </tr>
	 * <tr>
	 * <td>[b<sub>1</sub>,d<sub>0</sub>,a<sub>0</sub>,c<sub>0</sub>]</td>
	 * <td>1</td>
	 * <td>[a<sub>0</sub>,b<sub>1</sub>,c<sub>0</sub>]</td>
	 * <td>2</td>
	 * <td>1(D)</td>
	 * <td>-2</td>
	 * </tr>
	 * <tr>
	 * <td>[b<sub>0</sub>,d<sub>1</sub>,a<sub>0</sub>,c<sub>0</sub>]</td>
	 * <td>2</td>
	 * <td>[a<sub>0</sub>,b<sub>0</sub>,c<sub>0</sub>]</td>
	 * <td>0</td>
	 * <td>0(B)</td>
	 * <td>+2</td>
	 * </tr>
	 * <tr>
	 * <td>[b<sub>1</sub>,d<sub>1</sub>,a<sub>0</sub>,c<sub>0</sub>]</td>
	 * <td>3</td>
	 * <td>[a<sub>0</sub>,b<sub>1</sub>,c<sub>0</sub>]</td>
	 * <td>2</td>
	 * <td>2(A)</td>
	 * <td>-1</td>
	 * </tr>
	 * <tr>
	 * <td>[b<sub>0</sub>,d<sub>0</sub>,a<sub>1</sub>,c<sub>0</sub>]</td>
	 * <td>4</td>
	 * <td>[a<sub>1</sub>,b<sub>0</sub>,c<sub>0</sub>]</td>
	 * <td>1</td>
	 * <td>0(B)</td>
	 * <td>+2</td>
	 * </tr>
	 * <tr>
	 * <td>[b<sub>1</sub>,d<sub>0</sub>,a<sub>1</sub>,c<sub>0</sub>]</td>
	 * <td>5</td>
	 * <td>[a<sub>1</sub>,b<sub>1</sub>,c<sub>0</sub>]</td>
	 * <td>3</td>
	 * <td>1(D)</td>
	 * <td>-2</td>
	 * </tr>
	 * <tr>
	 * <td>[b<sub>0</sub>,d<sub>1</sub>,a<sub>1</sub>,c<sub>0</sub>]</td>
	 * <td>6</td>
	 * <td>[a<sub>1</sub>,b<sub>0</sub>,c<sub>0</sub>]</td>
	 * <td>1</td>
	 * <td>0(B)</td>
	 * <td>+2</td>
	 * </tr>
	 * <tr>
	 * <td>[b<sub>1</sub>,d<sub>1</sub>,a<sub>1</sub>,c<sub>0</sub>]</td>
	 * <td>7</td>
	 * <td>[a<sub>1</sub>,b<sub>1</sub>,c<sub>0</sub>]</td>
	 * <td>3</td>
	 * <td>3(C)</td>
	 * <td>+1</td>
	 * </tr>
	 * <tr>
	 * <td>...</td>
	 * <td>...</td>
	 * <td>...</td>
	 * <td>...</td>
	 * <td>...</td>
	 * <td>...</td>
	 * </tr>
	 * </table>
	 *
	 * The order is imposed by the variables of <b>this</b> potential (<b>Y</b>)
	 *
	 * @param variables List of variables
	 * @param otherVariables {@code ArrayList} of {@code Variable}s of another
	 *                       potential (in this example: <b>Y<sup>X</sup></b> = [a, b, c])
	 * @return The accumulated offsets in an array of integers. In this example
	 * Accumulated offsets returns: [+2,-2,-1,+1]. Size = this
	 * {@code TablePotential} number of variables.
	 */
	public static int[] getAccumulatedOffsets(List<Variable> variables, List<Variable> otherVariables) {
		int otherSize = otherVariables.size();
		int thisSize = variables.size();
		int[] accOffsetXY = new int[thisSize];
		if (otherSize == 0) {
			return accOffsetXY; // Initialized to 0
		}
		int[] ordering = new int[thisSize];
		for (int i = 0; i < ordering.length; i++) {
			ordering[i] = otherVariables.indexOf(variables.get(i));
		}
		// offsets of otherVariables
		int[] offsetX = new int[otherSize];
		offsetX[0] = 1;
		for (int i = 1; i < offsetX.length; i++) {
			offsetX[i] = offsetX[i - 1] * otherVariables.get(i - 1).getNumStates();
		}
		int[] offsetXY = new int[thisSize];
		int ordering_0 = ordering[0];
		if (ordering_0 == -1) {
			offsetXY[0] = 0;
		} else {
			offsetXY[0] = offsetX[ordering_0];
		}
		accOffsetXY[0] = offsetXY[0];
		int ordering_j;
		for (int j = 1; j < accOffsetXY.length; j++) {
			ordering_j = ordering[j];
			if (ordering_j == -1) {
				offsetXY[j] = 0;
			} else {
				offsetXY[j] = offsetX[ordering_j];
			}
			int numStatesYj_1 = ((Variable) variables.get(j - 1)).getNumStates();
			accOffsetXY[j] = accOffsetXY[j - 1] + offsetXY[j] - (numStatesYj_1 * offsetXY[j - 1]);
		}
		return accOffsetXY;
	}

	/**
	 * Use accumulated offsets to calculate the next position in a potential.
	 * The content of actualPosition will be modified
	 *
	 * @param actualPosition Actual position
	 * @param actualCoordinate Array of actual coordinates
	 * @param dimensions Array of dimensions
	 * @param accOffsets Array of accumulated offsets
	 * @return Next position or -1 if it reach the end of the potential
	 */
	public static int getNextPosition(int actualPosition, int[] actualCoordinate, int[] dimensions, int[] accOffsets) {
		for (int j = 0; j < actualCoordinate.length; j++) {
			actualCoordinate[j]++;
			if (actualCoordinate[j] < dimensions[j]) {
				// this is what never should be done like this, but it is fast and here that is more important
				return actualPosition + accOffsets[j];
			}
			actualCoordinate[j] = 0;
		}
		return -1;
	}

	/**
	 * This method is {@code static} because sometimes it can be used
	 * without creating the {@code TablePotential}; for instance, to
	 * estimate the amount of memory that would be necessary to actually create
	 * the PotentialTable.
	 *
	 * @param fsVariables {@code ArrayList} of {@code Variable}s.
	 * @return array of {@code int[]} with the dimension of each variable.
	 */
	public static int[] calculateDimensions(List<Variable> fsVariables) {
		int numVariables = fsVariables == null ? 0 : fsVariables.size();
		int[] dimensions = new int[numVariables];
		for (int i = 0; i < numVariables; i++) {
			dimensions[i] = fsVariables.get(i).getNumStates();
		}
		return dimensions;
	}

	/**
	 * This method is {@code static} because sometimes can be used outside
	 * of a {@code TablePotential}.
	 *
	 * @param dimensions of variables. Array of {@code int[]}.
	 * @return array of {@code int[]} with the offset of each variable.
	 */
	public static int[] calculateOffsets(int[] dimensions) {
		int[] offsets = new int[dimensions.length];
		offsets[0] = 1;
		for (int i = 1; i < dimensions.length; i++) {
			offsets[i] = dimensions[i - 1] * offsets[i - 1];
		}
		return offsets;
	}

	/**
	 * Calculates {@code tableSize} = product of dimensions of variables.
	 * In projected potentials {@code tableSize} can be distinct that
	 * {@code table.length}.
	 * @param variables List of variables
	 * @return table size
	 */
	public static int computeTableSize(List<Variable> variables) {
		int tableSize = 1;
		for (Variable variable : variables) {
			tableSize *= variable.getNumStates();
		}
		return tableSize;
	}

	/**
	 * @param uncertainValues List of uncertain values
	 * @return true if the uncertain values are correct
	 */
	public static boolean checkUncertainTable(List<UncertainValue> uncertainValues) {
		return true;
	}

	/**
	 * Remove a variable of the potential
	 * @param variable Variable to be removed
	 * @return Potential without the removed variable
	 */
	public Potential removeVariable(Variable variable) {
		Potential newPotential = this;
		if (variables.contains(variable)) {
			Finding finding = new Finding(variable, 0);
			EvidenceCase evidenceCase = new EvidenceCase();
			try {
				evidenceCase.addFinding(finding);
				newPotential = tableProject(evidenceCase, null).get(0);
			} catch (InvalidStateException | WrongCriterionException | IncompatibleEvidenceException | NonProjectablePotentialException e) {
				// Unreachable code
				e.printStackTrace();
			}
		} else {
			newPotential = this;
		}
		return newPotential;
	}

	private void copyValuesInterventionsAndUncertainValues(int position, TablePotential fromPotential,
			int fromPotentialPosition, boolean hasUncertainTable) {
		values[position] = fromPotential.values[fromPotentialPosition];
		if ((strategyTrees != null) && (fromPotential.strategyTrees != null)) {
			strategyTrees[position] = fromPotential.strategyTrees[fromPotentialPosition];
		}
		if (hasUncertainTable) {
			uncertainValues[position] = fromPotential.uncertainValues[fromPotentialPosition];
		}
	}

	/**
	 * @param evidenceCase {@code EvidenceCase}
	 * @return A {@code List} of {@code TablePotential}s containing
	 * only one element, which is a {@code ProjectedPotential}
	 * @throws WrongCriterionException WrongCriterionException
	 */
	public List<TablePotential> tableProject(EvidenceCase evidenceCase, InferenceOptions inferenceOptions,
			List<TablePotential> projectedPotentials) throws WrongCriterionException {
		// returned value
		boolean hasUncertainTable = (uncertainValues != null);
		List<TablePotential> newProjectedPotentials = new ArrayList<>(1);
		List<Variable> unobservedVariables = new ArrayList<>(variables);
		if (evidenceCase != null) {
			unobservedVariables.removeAll(evidenceCase.getVariables());
		}
		int numUnobservedVariables = unobservedVariables.size();
		TablePotential projectedPotential;
		int numVariables = (variables != null) ? variables.size() : 0;
		if (numVariables == numUnobservedVariables) { // No projection.
			projectedPotential = this;
		} else {// Common part in constant potential and not constant potentials
			projectedPotential = new TablePotential(unobservedVariables, role);
			int length = projectedPotential.values.length;
			if (hasUncertainTable) {
				projectedPotential.setUncertainValues(new UncertainValue[length]);
			}
			// position (in this potential) of the first value
			// of the projected potential
			int firstPosition = 0;
			// auxiliary for the for loop
			int state;
			// iterate over the variables of this potential
			for (int i = 0; i < variables.size(); i++) {
				Variable variable = variables.get(i);
				if ((evidenceCase != null) && evidenceCase.contains(variable)) {
					state = evidenceCase.getState(variable);
					firstPosition += state * offsets[i];
				}
			}
			if (numUnobservedVariables == 0) {// Projection = constant potential
				projectedPotential.copyValuesInterventionsAndUncertainValues(0, this, firstPosition, hasUncertainTable);
			} else { // Create projected potential
				// Go trough this potential using accumulatedOffests
				int[] accumulatedOffsets = projectedPotential.getAccumulatedOffsets(variables);
				int numVariablesProjected = projectedPotential.getNumVariables();
				int[] projectedCoordinate = new int[numVariablesProjected];
				int[] projectedDimensions = new int[numVariablesProjected];
				for (int i = 0; i < numVariablesProjected; i++) {
					projectedDimensions[i] = unobservedVariables.get(i).getNumStates();
				}
				// Copy configurations using the accumulated offsets algorithm
				for (int projectedPosition = 0; projectedPosition < length - 1; projectedPosition++) {
					projectedPotential.copyValuesInterventionsAndUncertainValues(projectedPosition, this, firstPosition,
							hasUncertainTable);
					// find the next configuration and the index of the
					// increased variable
					int increasedVariable = 0;
					for (int j = 0; j < projectedCoordinate.length; j++) {
						projectedCoordinate[j]++;
						if (projectedCoordinate[j] < projectedDimensions[j]) {
							increasedVariable = j;
							break;
						}
						projectedCoordinate[j] = 0;
					}
					// update the positions of the potentials we are multiplying
					firstPosition += accumulatedOffsets[increasedVariable];
				}
				int lastPositionProjected = length - 1;
				projectedPotential.copyValuesInterventionsAndUncertainValues(lastPositionProjected, this, firstPosition,
						hasUncertainTable);
			}
			// Common final part for constant and not constant potentials
			projectedPotential.setUncertainTableToNullIfNullValues();
		}
		newProjectedPotentials.add(projectedPotential);
		return newProjectedPotentials;
	}

	@Override public TablePotential project(EvidenceCase evidenceCase)
			throws NonProjectablePotentialException, WrongCriterionException {
		return tableProject(evidenceCase, null).get(0);
	}

	private void setUncertainTableToNullIfNullValues() {
		boolean allNull;
		if (uncertainValues != null) {
			allNull = true;
			for (int i = 0; i < uncertainValues.length && allNull; i++) {
				allNull = (uncertainValues[i] == null);
			}
			if (allNull) {
				uncertainValues = null;
			}
		}
	}

	/**
	 * The accumulated offset represents the increment (positive or negative) in
	 * the corresponding position of the table when a variable is incremented
	 * given an ordering of the variables of other potential.
	 * 
	 * <big><b> Accumulated Offsets example
	 * 
	 * </b></big> We have two potentials: Potential <b>Y</b> (b, d, a, c) and
	 * Potential <b>X</b> (a, b, c). All variables are binary for simplicity.
	 * 
	 * <table border="2">
	 * <caption>Table</caption>
	 * <tr>
	 * <td><b>Y</b></td>
	 * <td><b>pos<sub>y</sub>(Y)</b></td>
	 * <td><b>Y<sup>X</sup></b></td>
	 * <td><b>pos<sub>X</sub>(Y<sup>X</sup>)</b></td>
	 * <td><b>varToIncr(Y)</b></td>
	 * <td><b>accOffset</b></td>
	 * </tr>
	 * 
	 * <tr>
	 * <td>[b<sub>0</sub>,d<sub>0</sub>,a<sub>0</sub>,c<sub>0</sub>]</td>
	 * <td>0</td>
	 * <td>[a<sub>0</sub>,b<sub>0</sub>,c<sub>0</sub>]</td>
	 * <td>0</td>
	 * <td>0(B)</td>
	 * <td>+2</td>
	 * </tr>
	 *
	 * <tr>
	 * <td>[b<sub>1</sub>,d<sub>0</sub>,a<sub>0</sub>,c<sub>0</sub>]</td>
	 * <td>1</td>
	 * <td>[a<sub>0</sub>,b<sub>1</sub>,c<sub>0</sub>]</td>
	 * <td>2</td>
	 * <td>1(D)</td>
	 * <td>-2</td>
	 * </tr>
	 * 
	 * <tr>
	 * <td>[b<sub>0</sub>,d<sub>1</sub>,a<sub>0</sub>,c<sub>0</sub>]</td>
	 * <td>2</td>
	 * <td>[a<sub>0</sub>,b<sub>0</sub>,c<sub>0</sub>]</td>
	 * <td>0</td>
	 * <td>0(B)</td>
	 * <td>+2</td>
	 * </tr>
	 * <tr>
	 * <td>[b<sub>1</sub>,d<sub>1</sub>,a<sub>0</sub>,c<sub>0</sub>]</td>
	 * <td>3</td>
	 * <td>[a<sub>0</sub>,b<sub>1</sub>,c<sub>0</sub>]</td>
	 * <td>2</td>
	 * <td>2(A)</td>
	 * <td>-1</td>
	 * </tr>
	 * <tr>
	 * <td>[b<sub>0</sub>,d<sub>0</sub>,a<sub>1</sub>,c<sub>0</sub>]</td>
	 * <td>4</td>
	 * <td>[a<sub>1</sub>,b<sub>0</sub>,c<sub>0</sub>]</td>
	 * <td>1</td>
	 * <td>0(B)</td>
	 * <td>+2</td>
	 * </tr>
	 * <tr>
	 * <td>[b<sub>1</sub>,d<sub>0</sub>,a<sub>1</sub>,c<sub>0</sub>]</td>
	 * <td>5</td>
	 * <td>[a<sub>1</sub>,b<sub>1</sub>,c<sub>0</sub>]</td>
	 * <td>3</td>
	 * <td>1(D)</td>
	 * <td>-2</td>
	 * </tr>
	 * <tr>
	 * <td>[b<sub>0</sub>,d<sub>1</sub>,a<sub>1</sub>,c<sub>0</sub>]</td>
	 * <td>6</td>
	 * <td>[a<sub>1</sub>,b<sub>0</sub>,c<sub>0</sub>]</td>
	 * <td>1</td>
	 * <td>0(B)</td>
	 * <td>+2</td>
	 * </tr>
	 * <tr>
	 * <td>[b<sub>1</sub>,d<sub>1</sub>,a<sub>1</sub>,c<sub>0</sub>]</td>
	 * <td>7</td>
	 * <td>[a<sub>1</sub>,b<sub>1</sub>,c<sub>0</sub>]</td>
	 * <td>3</td>
	 * <td>3(C)</td>
	 * <td>+1</td>
	 * </tr>
	 * <tr>
	 * <td>...</td>
	 * <td>...</td>
	 * <td>...</td>
	 * <td>...</td>
	 * <td>...</td>
	 * <td>...</td>
	 * </tr>
	 * </table>
	 * <p>
	 * The order is imposed by the variables of <b>this</b> potential (<b>Y</b>)
	 * <p>
	 *
	 * @param otherVariables {@code ArrayList} of {@code Variable}s of another
	 *                       potential (in this example: <b>Y<sup>X</sup></b> = [a, b, c])
	 * @return The accumulated offsets in an array of integers. In this example
	 * Accumulated offsets returns: [+2,-2,-1,+1]. Size = this
	 * {@code TablePotential} number of variables.
	 */
	public int[] getAccumulatedOffsets(List<Variable> otherVariables) {
		int otherSize = otherVariables.size();
		int thisSize = variables.size();
		int[] accOffsetXY = new int[thisSize];
		if (otherSize == 0) {
			return accOffsetXY; // Initialized to 0
		}
		int[] ordering = new int[thisSize];
		for (int i = 0; i < ordering.length; i++) {
			ordering[i] = otherVariables.indexOf(variables.get(i));
		}
		// offsets of otherVariables
		int[] offsetX = new int[otherSize];
		offsetX[0] = 1;
		for (int i = 1; i < offsetX.length; i++) {
			offsetX[i] = offsetX[i - 1] * otherVariables.get(i - 1).getNumStates();
		}
		int[] offsetXY = new int[thisSize];
		int ordering_0 = ordering[0];
		if (ordering_0 == -1) {
			offsetXY[0] = 0;
		} else {
			offsetXY[0] = offsetX[ordering_0];
		}
		accOffsetXY[0] = offsetXY[0];
		int ordering_j;
		for (int j = 1; j < accOffsetXY.length; j++) {
			ordering_j = ordering[j];
			if (ordering_j == -1) {
				offsetXY[j] = 0;
			} else {
				offsetXY[j] = offsetX[ordering_j];
			}
			int numStatesYj_1 = ((Variable) variables.get(j - 1)).getNumStates();
			accOffsetXY[j] = accOffsetXY[j - 1] + offsetXY[j] - (numStatesYj_1 * offsetXY[j - 1]);
		}
		return accOffsetXY;
	}

	/**
	 * Get accumulated offsets of a projected potential.
	 *
	 * @param otherVariables    . Actual set of variables in a projected potential.
	 *                          {@code ArrayList} of {@code Variable}
	 * @param originalVariables . Complete set of variables in a projected potential.
	 *                          {@code ArrayList} of {@code Variable}
	 * @return The accumulated offsets in an array of integers.
	 * Condition: otherVariables is contained in originalVariables.
	 * Condition: otherVariables and originalVariables have the same order.
	 */
	public int[] getProjectedAccumulatedOffsets(List<Variable> otherVariables, List<Variable> originalVariables) {
		if (otherVariables == originalVariables) { // Not projected potential
			return getAccumulatedOffsets(otherVariables);
		}
		int[] originalAccOffsets = getAccumulatedOffsets(originalVariables);
		int[] accOffsets = new int[otherVariables.size()];
		int j = 0;
		for (int i = 0; i < originalVariables.size(); i++) {
			Variable variable = originalVariables.get(i);
			if (otherVariables.contains(variable)) {
				accOffsets[j++] = originalAccOffsets[i];
			}
		}
		return accOffsets;
	}

	/**
	 * A configuration is a set of integers that represents a position in the
	 * table.
	 * <p>
	 * <strong>Example</strong>: A potential <strong>T</strong> with two binary
	 * variables: <strong>a</strong> and <strong>b</strong> has this possible
	 * configurations and position in the table:
	 * <table style="text-align:center;boder:1px solid black:">
	 *     <caption>Table</caption>
	 * <tr>
	 * <td><strong>a b position</strong></td>
	 * </tr>
	 * <tr>
	 * <td>0 0 0</td>
	 * </tr>
	 * <tr>
	 * <td>1 0 1</td>
	 * </tr>
	 * <tr>
	 * <td>0 1 2</td>
	 * </tr>
	 * <tr>
	 * <td>1 1 3</td>
	 * </tr>
	 * </table>
	 * @param coordinates Coordinates
	 * @return The position in the table of the value corresponding to the given
	 * coordinates of the variables.
	 * In the above example {@code T.getPosition([0,1])} will
	 * return: <strong>2</strong>
	 * Condition: coordinates.length = numVariables
	 * Condition: coordinates[i] &#62;= 0 and coordinates[i] &#60; dimensions[i].
	 */
	public int getPosition(int[] coordinates) {
		int position = 0;
		int numVariables = (variables != null) ? variables.size() : 0;
		for (int i = 0; i < numVariables; i++) {
			position += offsets[i] * coordinates[i];
		}
		return position;
	}

	/**
	 * This method is similar to getPosition(int []), but the input argument is
	 * a configuration of variables which are not necessarily in the same order
	 * that the variables in the potential
	 *
	 * @param configuration Evidence case
	 * @return position
	 */
	public int getPosition(EvidenceCase configuration) {
		int[] coordinates;
		int sizeCoordinates;
		int pos;
		int sizeEvi = configuration.getFindings().size();
		List<Variable> varsTable = this.getVariables();
		int startLoop = 0;

		// If we have a variable without evidence, we are in the case of P(c|a,b). In this case
		// coordinate for "c" would be '0'. Else we are in the case P(a,b) (or U(a,b))
		if (varsTable.size() != sizeEvi) {
			sizeCoordinates = sizeEvi + 1;
			startLoop = 1;
			coordinates = new int[sizeCoordinates];
			coordinates[0] = 0;
		} else {
			sizeCoordinates = sizeEvi;
			coordinates = new int[sizeCoordinates];
		}

		for (int i = startLoop; i < sizeCoordinates; i++) {
			coordinates[i] = configuration.getFinding(varsTable.get(i)).getStateIndex();
		}
		pos = getPosition(coordinates);
		return pos;
	}

	/**
	 * It returns the first position in the table of the consecutive cells where
	 * all the values corresponding to a certain configuration are stored. It
	 * assumes that configuration is a complete instantiation of the parents of
	 * the variable associated to the table.
	 *
	 * @param configuration Evidence case
	 * @return first position in the table of the consecutive cells where all the values corresponding to a certain configuration are stored
	 */
	public int getBasePosition(EvidenceCase configuration) {
		int[] coordinates;
		int sizeCoordinates;
		int pos;
		int sizeEvi = configuration.getFindings().size();
		sizeCoordinates = sizeEvi + 1;
		coordinates = new int[sizeCoordinates];
		List<Variable> varsTable = this.getVariables();
		int startLoop;
		coordinates[0] = 0;
		startLoop = 1;
		for (int i = startLoop; i < sizeCoordinates; i++) {
			coordinates[i] = configuration.getFinding(varsTable.get(i)).getStateIndex();
		}
		pos = this.getPosition(coordinates);
		return pos;
	}

	/**
	 * @param position in the table. {@code int}
	 * @return The configuration corresponding to {@code position}
	 * {@code double}
	 */
	public int[] getConfiguration(int position) {
		int[] coordinate = new int[offsets.length];
		for (int i = offsets.length - 1; i >= 0; i--) {
			coordinate[i] = position / offsets[i];
			position -= coordinate[i] * offsets[i];
		}
		return coordinate;
	}

	/**
	 * Given a set of variables and a set of corresponding states indices, gets
	 * the corresponding value in the table.
	 *
	 * @param variables     . {@code ArrayList} of {@code Variable}
	 * @param statesIndices . {@code int[]}
	 * @return {@code double}
	 * Condition: All the variables in this potentials are included into the
	 * received variables.
	 */
	public double getValue(List<Variable> variables, int[] statesIndices) {
		int position = 0;
		for (int i = 0; i < variables.size(); i++) {
			Variable variable = variables.get(i);
			int indexVariable = this.variables.indexOf(variable);
			if (indexVariable != -1) {
				position += offsets[indexVariable] * statesIndices[i];
			}
		}
		return values[position];
	}

	/**
	 * Given a set an EvidenceCase, gets the corresponding value in the table.
	 *
	 * @param configuration . {@code EvidenceCase}
	 * @return {@code double}
	 * Condition: All the variables in this potentials are included into the
	 * variables field of the evidence case (configuration).
	 */
	public double getValue(EvidenceCase configuration) {
		int[] states;
		List<Variable> variables;
		int size;
		variables = configuration.getVariables();
		size = variables.size();
		states = new int[size];
		List<Finding> findings = configuration.getFindings();
		for (int i = 0; i < size; i++) {
			states[i] = findings.get(i).getStateIndex();
		}
		return getValue(variables, states);
	}

	/*******
	 * Assigns a value at the table for the combination of a set of variables
	 * and the corresponding state indices.
	 *
	 * @param variables
	 *            . {@code ArrayList} of {@code Variable}
	 * @param statesIndexes
	 *            . {@code int[]}
	 * @param value Value
	 */
	public void setValue(List<Variable> variables, int[] statesIndexes, double value) {
		int position = 0;
		for (int i = 0; i < variables.size(); i++) {
			Variable variable = variables.get(i);
			int indexVariable = this.variables.indexOf(variable);
			if (indexVariable != -1) {
				position += offsets[indexVariable] * statesIndexes[i];
			}
		}
		values[position] = value;
	}

	/**
	 * @return {@code int[]}: The offsets of the variables in the table of
	 * values.
	 */
	public int[] getOffsets() {
		return offsets;
	}

	/**
	 * @return {@code double[]}: Table containing the values of the
	 * potential.
	 */
	public double[] getValues() {
		return values;
	}

	/**
	 * Set the values of the table. The dimensions of the new table have to be same that the current table
	 * @param table Table
	 */
	public void setValues(double[] table) {
		this.values = table;
	}

	/**
	 * Uncertain Table
	 *
	 * @return Array of uncertain values
	 */
	public UncertainValue[] getUncertainValues() {
		return uncertainValues;
	}

	/**
	 * @param uncertainValues Array of uncertain values
	 */
	public void setUncertainValues(UncertainValue[] uncertainValues) {
		this.uncertainValues = uncertainValues;
	}

	/**
	 * @return dimensions of the variables in an array of {@code int[]}.
	 */
	public int[] getDimensions() {
		return dimensions;
	}

	/**
	 * @return {@code initialPosition int}.
	 */
	public int getInitialPosition() {
		return initialPosition;
	}

	/**
	 * Compares two {@code TablePotential}s using {@code tableSize} as
	 * a criterion.
	 *
	 * @param other {@code Object}.
	 * @return {@code int}:

	 * &#60;0 if {@code this} table size is minor than the received
	 * potential
	 * =
	 * 0 if tables size is equal
	 * &#62;
	 * 0 if {@code this} table size is greater than the table size
	 * of received potential.
	 */
	public int compareTo(TablePotential other) {
		return this.tableSize - other.tableSize;
	}

	/**
	 * @param configuration Evidence case
	 * @return true if and only if the potential contains uncertainty values for
	 * a certain configuration
	 */
	public boolean hasUncertainty(EvidenceCase configuration) {
		boolean hasUncertainty;
		if (uncertainValues == null) {
			hasUncertainty = false;
		} else {
			int positionConfiguration;
			positionConfiguration = getPosition(configuration);
			hasUncertainty = uncertainValues[positionConfiguration] != null;
		}
		return hasUncertainty;
	}

	/**
	 * @return tableSize {@code int}
	 */
	public int getTableSize() {
		return tableSize;
	}

	// TODO revisar para que no use tableProject(...)
	public Collection<Finding> getInducedFindings(EvidenceCase evidenceCase)
			throws IncompatibleEvidenceException, WrongCriterionException {
		Collection<Finding> inducedFindings = new ArrayList<>();
		if (role == PotentialRole.CONDITIONAL_PROBABILITY || role == PotentialRole.POLICY) {
			// Iterates over the list of parents. If some parent is not in the
			// evidence case, it is not possible to induce a new Finding
			for (int i = 1; i < variables.size(); i++) {
				if (!evidenceCase.contains(variables.get(i))) {
					// returnS the empty list
					return inducedFindings;
				}
			}
			// Checks if the projected potentials are deterministic
			try {
				TablePotential projectedPotential = tableProject(evidenceCase, null).get(0);
				if ((projectedPotential.getNumVariables() == 1) && (projectedPotential instanceof TablePotential)) {
					double[] table = ((TablePotential) projectedPotential).values;
					int zeros = 0;
					int position = 0;
					for (int i = 0; i < table.length; i++) {
						if (table[i] == 0.0) {
							zeros++;
						} else {
							position = i;
						}
					}
					if (zeros == (table.length - 1)) {// new finding
						inducedFindings.add(new Finding(projectedPotential.getVariable(0), position));
					}

				}
			} catch (NonProjectablePotentialException e) {
				e.printStackTrace();
			}
		}
		return inducedFindings;
	}

	/**
	 * Initialize the table as a uniform potential.
	 */
	public void setUniform() {
		int numVariables;
		boolean setValue = false;
		Double value = 0.0;
		if (variables != null) {
			numVariables = variables.size();
			if ((numVariables > 0) && noNumericVariables() && (
					(role == PotentialRole.CONDITIONAL_PROBABILITY) || (role == PotentialRole.POLICY) || (
							role == PotentialRole.JOINT_PROBABILITY
					) || (role == PotentialRole.LINK_RESTRICTION)
			)) {
				setValue = true;
				value = 0.0;
				switch (role) {
                /*
                20/10/2014 - Solving issue 211
                https://bitbucket.org/cisiad/org.openmarkov.issues/issue/211/policy-tables-imposed-should-be-a
                Moved the case POLICY statement to share the code with CONDITIONAL_PROBABILITY.
                Before, it was acting as JOINT_PROBABILITY
                 */
				case CONDITIONAL_PROBABILITY:
				case POLICY:
					value = 1.0 / new Double(variables.get(0).getNumStates());
					break;
				case JOINT_PROBABILITY:
					value = 1.0;
					for (Variable variable : variables) {
						value *= variable.getNumStates();
					}
					value = 1 / value;
					break;
				case LINK_RESTRICTION:
					value = 1.0;
					break;
				default:
					// Do nothing
					break;
				} // When role = UTILITY -> value = 0.0 (default)
				for (int i = 0; i < values.length; i++) {
					values[i] = value;
				}
			} else if (numVariables == 0) {
				setValue = true;
				if (role == PotentialRole.JOINT_PROBABILITY) {
					value = 1.0;
				} else {
					value = 0.0;
				}
			}
			if (setValue) {
				for (int i = 0; i < values.length; i++) {
					values[i] = value;
				}
			}
		}
	}

	/**
	 * Overrides {@code toString} method. Mainly for test purposes
	 */
	public String toString() {
		DecimalFormat formatter = new DecimalFormat("0.###", new DecimalFormatSymbols(Locale.US));
		// writes variables names
		StringBuilder buffer = new StringBuilder(super.toString());
		// Print configurations
		int valuesPosition = 0;
		boolean openBrace = false;
		if (buffer.length() < STRING_MAX_LENGTH) {
			if (variables.size() > 0) {
				buffer.append(" = {");
				openBrace = true;
			} else {
				buffer.append(" ");
			}
		}
		while ((buffer.length() < STRING_MAX_LENGTH) && (valuesPosition < values.length)) {
			buffer.append(formatter.format(values[valuesPosition++]));
			if ((valuesPosition < values.length) && (buffer.length() < (STRING_MAX_LENGTH - 2))) {
				buffer.append(", ");
			}
		}
		if (values.length != 1) {
			if (valuesPosition != values.length || variables.size() == 0) {
				buffer.append("...");
			}
		}
		if (openBrace) {
			buffer.append("}");
		}
		return buffer.toString();
	}

	public String treeADDString() {
		if (role == PotentialRole.CONDITIONAL_PROBABILITY && variables != null && variables.size() == 1) {
			Variable firstVariable = variables.get(0);
			for (int i = 0; i < firstVariable.getNumStates(); i++) {
				if (values[i] == 1) {
					return firstVariable.getName() + " = " + firstVariable.getStateName(i);
				}
			}
		}
		return toString();
	}

	/**
	 * Generates a sampled potential
	 */
	public Potential sample() {
		Potential sampledPotential = this;
		if (uncertainValues != null) {
			TablePotentialSampler samplePotentialTable = new TablePotentialSampler();
			sampledPotential = samplePotentialTable.sample(this);
		}
		return sampledPotential;
	}

	@Override public boolean equals(Object arg0) {
		boolean isEqual = super.equals(arg0) && arg0 instanceof TablePotential;
		if (isEqual) {
			double[] otherValues = ((TablePotential) arg0).getValues();
			if (values.length == otherValues.length) {
				for (int i = 0; i < values.length; i++) {
					isEqual &= values[i] == otherValues[i];
				}
			} else {
				isEqual = false;
			}
		}
		return isEqual;
	}

	@Override public Potential copy() {
		return new TablePotential(this);
	}

	@Override public int sampleConditionedVariable(Random randomGenerator, Map<Variable, Integer> sampledParents) {
		int index = 0;
		int sampleIndex = 0;
		// find index of first position for the given configuration
		for (int i = 1; i < variables.size(); ++i) {
			index += sampledParents.get(variables.get(i)) * offsets[i];
		}
		double random = randomGenerator.nextDouble();
		double accumulatedProbability = values[index + sampleIndex];
		while (random > accumulatedProbability
				// Make sure we don't go out of bounds even if the sum of probabilities
				// is smaller than one.
				&& sampleIndex < variables.get(0).getNumStates() - 1) {
			++sampleIndex;
			accumulatedProbability += values[index + sampleIndex];
		}
		return sampleIndex;
	}

	@Override public double getProbability(HashMap<Variable, Integer> sampledStateIndexes) {
		int index = 0;
		// find index of first position for the given configuration
		for (int i = 0; i < variables.size(); ++i) {
			index += sampledStateIndexes.get(variables.get(i)) * offsets[i];
		}
		return values[index];
	}

	public double getUtility(HashMap<Variable, Integer> sampledStateIndexes, HashMap<Variable, Double> utilities) {
		return getProbability(sampledStateIndexes);
	}

	@Override public Potential addVariable(Variable newVariable) {
		// creates the new potential
		List<Variable> newVariables = new ArrayList<>(variables);
		newVariables.add(newVariable);
		TablePotential newPotential = new TablePotential(newVariables, role);
		// assigns the values of the new potential
		int newVariableNumStates = newVariable.getNumStates();
		for (int i = 0; i < newVariableNumStates; i++) {
			for (int j = 0; j < values.length; j++) {
				newPotential.values[j + i * values.length] = values[j];
				// newPotential.uncertainValues[j + i * values.length] =
				// uncertainValues[j];
			}
		}
		return newPotential;
	}

	@Override public boolean isUncertain() {
		return (this.uncertainValues != null) ? true : false;
	}

	@Override public void scalePotential(double scale) {
		for (int j = 0; j < this.values.length; j++) {
			this.values[j] = this.values[j] * scale;
		}

	}

	@Override public Potential deepCopy(ProbNet copyNet) {
		TablePotential potential = (TablePotential) super.deepCopy(copyNet);

		if (this.dimensions != null) {
			potential.dimensions = this.dimensions.clone();
		}

		potential.initialPosition = this.initialPosition;

		if (this.strategyTrees != null) {
			potential.strategyTrees = new StrategyTree[this.strategyTrees.length];

			for (int interventionIndex = 0; interventionIndex < this.strategyTrees.length; interventionIndex++) {
				potential.strategyTrees[interventionIndex] = (StrategyTree) this.strategyTrees[interventionIndex]
						.deepCopy(copyNet);
			}
		}

		potential.offsets = this.offsets.clone();
		potential.tableSize = this.tableSize;

		if (this.uncertainValues != null) {
			potential.uncertainValues = new UncertainValue[this.uncertainValues.length];
			for (int uncertainValueIndex = 0;
				 uncertainValueIndex < this.uncertainValues.length; uncertainValueIndex++) {
				if (this.uncertainValues[uncertainValueIndex] != null) {
					potential.uncertainValues[uncertainValueIndex] = this.uncertainValues[uncertainValueIndex].copy();
				}
			}
		}

		potential.setValues(this.values.clone());

		return potential;
	}

	/**
	 * @return The maximum time slice of the variables referenced by this potential
	 */
	public int getTimeSlice() {
		int maxTimeSlice = Integer.MIN_VALUE;
		int variableTimeSlice;
		for (Variable variable : variables) {
			maxTimeSlice = ((variableTimeSlice = variable.getTimeSlice()) > maxTimeSlice) ?
					variableTimeSlice :
					maxTimeSlice;
		}
		return maxTimeSlice;

	}

	/**
	 * @return The first value of attribute 'values' (that at 0-th position)
	 */
	public double getFirstValue() {
		return values[0];
	}

	/**
	 * @return true iff the table potential has interventions
	 */
	public boolean hasInterventions() {
		return strategyTrees != null && strategyTrees.length > 0 && strategyTrees[0] != null;
	}

	/**
	 * @param decision Decision variable
	 * @return true iff it has interventions that contains 'decision'
	 */
	public boolean hasInterventionForDecision(Variable decision) {
		return hasInterventions() && strategyTrees[0].hasInterventionForDecision(decision);
	}

}