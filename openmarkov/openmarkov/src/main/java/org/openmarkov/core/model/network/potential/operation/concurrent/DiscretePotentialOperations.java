/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.model.network.potential.operation.concurrent;

import java.util.List;

import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.potential.Potential;
import org.openmarkov.core.model.network.potential.TablePotential;

public class DiscretePotentialOperations {

	// Attributes
	/**
	 * Number of logical processors, bear in mind that logical processors are
	 * not always the physical ones. If the processors have a n
	 * hyper threading then logical processors = n * physical processors.
	 */
	public static int numLogicalProcessors = java.lang.Runtime.getRuntime().availableProcessors();

	// Constructor. Don't let anyone instantiate this class because in contains 
	// only static methods.
	private DiscretePotentialOperations() {
	}

	// Methods
	/**
	 *
	 * @param potentials ArrayList of Potentials
	 * @return The multiplied potentials
	 * @throws Exception Exception
	 */
	public static TablePotential multiply(List<TablePotential> potentials) throws Exception {
		// Sequential part
		SharedDataMultiply sdm = new SharedDataMultiply(potentials);
		sdm.initialize();

		// Concurrent part
		// Starts concurrency
		Thread[] multipliers = new Thread[numLogicalProcessors];
		DiscreteMultiply[] cdm = new DiscreteMultiply[numLogicalProcessors];
		for (int i = 0; i < numLogicalProcessors; i++) {
			cdm[i] = new DiscreteMultiply(sdm, i);
			multipliers[i] = new Thread(cdm[i]);
			multipliers[i].start();
			//cdm[i].run();
		}

		// Synchronization. The current thread waits until all 
		// concurrent operations finish
		for (int i = 0; i < numLogicalProcessors; i++) {
			try {
				multipliers[i].join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		return sdm.result;
	}

	public static TablePotential multiplyAndMarginalize(List<TablePotential> tablePotentials,
			List<Variable> fSVariablesToKeep, List<Variable> fSVariablesToEliminate) throws Exception {

		// Sequential part
		SharedDataMultiplyAndMarginalize sdm = new SharedDataMultiplyAndMarginalize(tablePotentials, fSVariablesToKeep,
				fSVariablesToEliminate);
		sdm.initializeMultiplyAndMarginalize();

		// Concurrent part
		// Starts concurrency
		Thread[] multipliers = new Thread[numLogicalProcessors];
		DiscreteMultiplyAndMarginalize[] cdm = new DiscreteMultiplyAndMarginalize[numLogicalProcessors];
		for (int i = 0; i < numLogicalProcessors; i++) {
			cdm[i] = new DiscreteMultiplyAndMarginalize(sdm, i);
			multipliers[i] = new Thread(cdm[i]);
			multipliers[i].start();
		}

		// Synchronization. The current thread waits until all 
		// concurrent operations finish
		for (int i = 0; i < numLogicalProcessors; i++) {
			try {
				multipliers[i].join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		return sdm.result;
	}

	/**
	 * @param numeratorPotential   {@code Potential}
	 * @param denominatorPotential {@code Potential}
	 * @return numeratorPotential / denominatorPotential {@code TablePotential}
	 */
	public static TablePotential divide(Potential numeratorPotential, Potential denominatorPotential) {
		// Sequential part
		SharedDataDivide sdd = new SharedDataDivide(numeratorPotential, denominatorPotential);
		sdd.initialize();

		// Concurrent part
		// Starts concurrency
		Thread[] multipliers = new Thread[numLogicalProcessors];
		DiscreteDivide[] cdd = new DiscreteDivide[numLogicalProcessors];
		for (int i = 0; i < numLogicalProcessors; i++) {
			cdd[i] = new DiscreteDivide(sdd, i);
			multipliers[i] = new Thread(cdd[i]);
			multipliers[i].start();
		}

		// Synchronization. The current thread waits until all 
		// concurrent operations finish
		for (int i = 0; i < numLogicalProcessors; i++) {
			try {
				multipliers[i].join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		return sdd.result;
	}

	/**
	 * Calculates potentialPositions for each interval
	 */
	static void splitOperatorsPotentials(List<TablePotential> potentials, TablePotential result,
			int[][] resultIntervals, int[][] potentialsPositions) {

		List<Variable> resultVariables = result.getVariables();

		int aux;
		int numIntervals = org.openmarkov.core.model.network.potential.operation.concurrent.DiscretePotentialOperations.numLogicalProcessors;

		for (int i = 0; i < numIntervals; i++) {
			for (int j = 0; j < potentials.size(); j++) {
				TablePotential potential = potentials.get(j);
				int[] offsetsPotential = potential.getOffsets();
				List<Variable> potentialVariables = potential.getVariables();
				aux = 0;

				// This is different for each operation (marginalization, etc)
				for (int k = 0; k < potentialVariables.size(); k++) {
					Variable variable = potentialVariables.get(k);
					// gets the variable coordinate value in the 
					// potentialPosition
					int indexResult = resultVariables.indexOf(variable);
					int valueResult = resultIntervals[i][indexResult];
					aux += offsetsPotential[k] * valueResult;
				}
				potentialsPositions[i][j] = aux;
			}
		}
	}

	/**
	 * Divides an operation in {@code numLogicalProcessors} parts by
	 * computing the following arrays {@code resultIntervals},
	 * {@code lengthResultIntervals} and
	 * {@code variableToIncrement}.
	 */
	static void splitResultPotential(TablePotential result, int[][] resultIntervals, int[] lengthResultIntervals,
			int[] resultPositionIntervals) {
		int[] offsetsResult = result.getOffsets();
		int[] dimensions = result.getDimensions();
		int numIntervals = org.openmarkov.core.model.network.potential.operation.concurrent.DiscretePotentialOperations.numLogicalProcessors;
		int numResultVariables = offsetsResult.length;
		int lastVariable = numResultVariables - 1;
		int resultSize = offsetsResult[lastVariable] * dimensions[lastVariable];

		int intervalLength = resultSize / numIntervals;

		int remainderConfigurations = resultSize % numIntervals;

		// Calculate length of each interval
		resultPositionIntervals[0] = 0;
		for (int i = 0; i < numIntervals; i++) {
			lengthResultIntervals[i] = intervalLength;
			if (i < remainderConfigurations) {
				lengthResultIntervals[i]++;
			}
			if (i > 0) {
				resultPositionIntervals[i] = resultPositionIntervals[i - 1] + lengthResultIntervals[i];
			}
		}

		int actualPosition = 0;
		// Calculate intervals
		// First configuration starts with (0,0,...,0).  
		int i;
		for (i = 1; i < numIntervals; i++) {
			actualPosition += lengthResultIntervals[i - 1];
			int remainder = actualPosition;
			for (int j = lastVariable; j >= 0; j--) {
				resultIntervals[i][j] = remainder / offsetsResult[j];
				remainder = remainder % offsetsResult[j];
			}
		}
	}

}
