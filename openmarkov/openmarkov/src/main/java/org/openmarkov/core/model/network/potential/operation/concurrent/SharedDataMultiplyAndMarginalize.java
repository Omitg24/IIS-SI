/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.model.network.potential.operation.concurrent;

import java.util.ArrayList;
import java.util.List;

import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.potential.TablePotential;

public class SharedDataMultiplyAndMarginalize extends SharedDataMultiply {

	List<Variable> variablesToKeep;

	List<Variable> variablesToEliminate;

	// Attributes related to the product potential that will be marginalized
	List<Variable> unionVariables;

	int[] unionCoordinate;

	int[] unionDimensions;

	int eliminationSize;

	@SuppressWarnings("unchecked") public SharedDataMultiplyAndMarginalize(List<TablePotential> potentials,
			List<Variable> fSVariablesToKeep, List<Variable> fSVariablesToEliminate) {

		super(potentials);

		this.variablesToKeep = fSVariablesToKeep;

		this.variablesToEliminate = (ArrayList<Variable>) ((Object) fSVariablesToEliminate);
	}

	/**
	 * Does some previous not parallel operations
	 *
	 * @throws Exception Exception
	 */
	public void initializeMultiplyAndMarginalize() throws Exception {
		super.initialize();
		// variables in the resulting potential
		List<Variable> unionVariables = new ArrayList<>(variablesToEliminate);
		unionVariables.addAll(variablesToKeep);
		int numUnionVariables = unionVariables.size();

		// current coordinate in the resulting potential
		unionCoordinate = new int[numUnionVariables];
		unionDimensions = TablePotential.calculateDimensions(unionVariables);

		// Defines some arrays for the proper potentials...
		double[][] tables = new double[numPotentials][];
		int[] initialPositions = new int[numPotentials];
		int[] currentPositions = new int[numPotentials];
		int[][] accumulatedOffsets = new int[numPotentials][];
		// ... and initializes them
		TablePotential unionPotential = new TablePotential(unionVariables, null);
		for (int i = 0; i < numPotentials; i++) {
			TablePotential potential = (TablePotential) potentials.get(i);
			tables[i] = potential.values;
			initialPositions[i] = potential.getInitialPosition();
			currentPositions[i] = initialPositions[i];
			accumulatedOffsets[i] = unionPotential
					//.getAccumulatedOffsets(potential.getOriginalVariables());
					.getAccumulatedOffsets(potential.getVariables());
		}

		// The elimination size is the product of the dimensions of the
		// variables to eliminate
		eliminationSize = 1;
		for (Variable variable : variablesToEliminate) {
			eliminationSize *= variable.getNumStates();
		}

	}

}
