/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.model.network.potential.operation.concurrent;

public class DiscreteMultiplyAndMarginalize extends DiscreteMultiply implements Runnable {

	private SharedDataMultiplyAndMarginalize sdm;

	private int resultSize;

	private int[] currentPositions;

	private int eliminationSize;

	private int[] unionCoordinate;

	private int[] unionDimensions;

	public DiscreteMultiplyAndMarginalize(SharedDataMultiplyAndMarginalize sdm, int logicalProcessor) {
		super(sdm, logicalProcessor);
		resultSize = sdm.result.values.length;
		eliminationSize = sdm.eliminationSize;
		unionCoordinate = sdm.unionCoordinate;
		unionDimensions = sdm.unionDimensions;
	}

	public void run() {
		// Auxiliary variables for the nested loops
		double multiplicationResult; // product of the table values
		double accumulator; // in general, the sum or the maximum
		int increasedVariable = 0; // when computing the next configuration

		// outer iterations correspond to the variables to keep
		int initialPosition = sdm.resultPositionIntervals[logicalProcessor];
		int finalPosition = initialPosition + sdm.lengthResultIntervals[logicalProcessor];
		for (int outerIteration = initialPosition; outerIteration < finalPosition; outerIteration++) {
			// Inner iterations correspond to the variables to eliminate
			// accumulator summarizes the result of all inner iterations

			// first inner iteration
			multiplicationResult = constantFactor;
			for (int i = 0; i < numPotentials; i++) {
				// multiply the numbers
				multiplicationResult *= tables[i][currentPositions[i]];
			}
			accumulator = multiplicationResult;

			// next inner iterations
			for (int innerIteration = 1; innerIteration < eliminationSize; innerIteration++) {

				// find the next configuration and the index of	 the
				// increased
				// variable
				for (int j = 0; j < unionCoordinate.length; j++) {
					unionCoordinate[j]++;
					if (unionCoordinate[j] < unionDimensions[j]) {
						increasedVariable = j;
						break;
					}
					unionCoordinate[j] = 0;
				}

				// update the positions of the potentials we are multiplying
				for (int i = 0; i < numPotentials; i++) {
					currentPositions[i] += offAccPotentials[i][increasedVariable];
				}

				// multiply the table values of the potentials
				multiplicationResult = constantFactor;
				for (int i = 0; i < numPotentials; i++) {
					multiplicationResult = multiplicationResult * tables[i][currentPositions[i]];
				}

				// update the accumulator (for this inner iteration)
				accumulator += multiplicationResult;
				// accumulator =
				// operator.combine(accumlator,multiplicationResult);

			} // end of inner iteration

			// when eliminationSize == 0 there is a multiplication without
			// marginalization but we must find the next configuration
			if (outerIteration < resultSize - 1) {
				// find the next configuration and the index of the
				// increased
				// variable
				for (int j = 0; j < unionCoordinate.length; j++) {
					unionCoordinate[j]++;
					if (unionCoordinate[j] < unionDimensions[j]) {
						increasedVariable = j;
						break;
					}
					unionCoordinate[j] = 0;
				}

				// update the positions of the potentials we are multiplying
				for (int i = 0; i < numPotentials; i++) {
					currentPositions[i] += offAccPotentials[i][increasedVariable];
				}
			}

			resultTable[outerIteration] = accumulator / eliminationSize;

		} // end of outer iteration

	}
}
