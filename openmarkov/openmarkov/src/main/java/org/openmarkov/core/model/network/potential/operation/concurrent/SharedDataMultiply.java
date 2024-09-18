/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.model.network.potential.operation.concurrent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openmarkov.core.model.network.potential.PotentialRole;
import org.openmarkov.core.model.network.potential.TablePotential;
import org.openmarkov.core.model.network.potential.operation.AuxiliaryOperations;
import org.openmarkov.core.model.network.potential.operation.DiscretePotentialOperations;

/**
 * Contains shared data used concurrently by a set of threads computing an
 * operation. This class is not synchronized because the set of threads
 * accomplish the three conditions of Bernstein
 * All data have package access because they will be used outside this class,
 * only in this package and the access must be efficient (there is not getX or
 * setX)
 */
public class SharedDataMultiply {

	// Attributes related to the arguments
	volatile List<TablePotential> potentials;

	volatile int numPotentials;

	volatile int[][] offAccPotentials;

	/**
	 * Probability tables of potentials
	 */
	volatile double[][] tables;

	// Attributes related to result
	volatile TablePotential result;

	volatile int numResultVariables;

	volatile int[] resultDimensions;

	volatile int[] offsetsResult;

	// Product of constant potentials (1 if none)
	volatile double constantFactor;

	// Attributes related to splitted potentials. 
	// First coordinate = numLogicalProcessors
	volatile int[] resultPositionIntervals;

	volatile int[][] resultIntervals;

	volatile int[] lengthResultIntervals;

	volatile int[][] potentialsPositions;

	// Constructor
	public SharedDataMultiply(List<TablePotential> potentials) {
		this.potentials = new ArrayList<>(potentials);
	}

	// Methods

	/**
	 * Initialize the attributes using the potentials and the number of logical
	 * processors
	 *
	 * @throws Exception Exception
	 */
	public void initialize() throws Exception {
		// Sort the potentials according to the table size
		Collections.sort(potentials);

		// Gets constant factor: The product of constant potentials
		constantFactor = org.openmarkov.core.model.network.potential.operation.DiscretePotentialOperations
				.getConstantFactor(potentials);

		//nonConstantPotentials may be redundant, but since potentials may be modified by other threads,
		// separating declaration and assignment avoids that other threads mess with "potentials" while this
		// thread is updating it.
		List<TablePotential> nonConstantPotentials = AuxiliaryOperations.getNonConstantPotentials(potentials);
		potentials = nonConstantPotentials;

		numPotentials = potentials.size();

		// Gets the union
		result = new TablePotential(AuxiliaryOperations.getUnionVariables(potentials), PotentialRole.JOINT_PROBABILITY);

		numResultVariables = result.getVariables().size();

		// Gets the tables of each TablePotential
		tables = new double[numPotentials][];
		for (int i = 0; i < numPotentials; i++) {
			tables[i] = potentials.get(i).values;
		}

		// Gets offset accumulate
		offAccPotentials = DiscretePotentialOperations.getAccumulatedOffsets(potentials, result);

		// Position in each table potential
		potentialsPositions = new int[org.openmarkov.core.model.network.potential.operation.concurrent.DiscretePotentialOperations.numLogicalProcessors][numPotentials];

		// splits potentials
		offsetsResult = result.getOffsets();
		int numIntervals = org.openmarkov.core.model.network.potential.operation.concurrent.DiscretePotentialOperations.numLogicalProcessors;
		int numResultVariables = offsetsResult.length;
		resultIntervals = new int[numIntervals][numResultVariables];
		lengthResultIntervals = new int[numIntervals];
		resultPositionIntervals = new int[numIntervals];

		org.openmarkov.core.model.network.potential.operation.concurrent.DiscretePotentialOperations
				.splitResultPotential(result, resultIntervals, lengthResultIntervals, resultPositionIntervals);
		org.openmarkov.core.model.network.potential.operation.concurrent.DiscretePotentialOperations
				.splitOperatorsPotentials(potentials, result, resultIntervals, potentialsPositions);
	}

}
