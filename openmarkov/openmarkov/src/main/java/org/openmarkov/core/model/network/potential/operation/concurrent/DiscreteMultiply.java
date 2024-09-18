/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.model.network.potential.operation.concurrent;

public class DiscreteMultiply implements Runnable {

	volatile protected SharedDataMultiply sdm;

	protected int logicalProcessor;

	protected int[] resultDimension;

	protected int[] resultCoordinate;

	protected double[] resultTable;

	protected int[] potentialsPositions;

	protected double[][] tables;

	protected int[][] offAccPotentials;

	protected int numPotentials;

	protected double constantFactor;

	public DiscreteMultiply(SharedDataMultiply sdm, int logicalProcessor) {
		this.sdm = sdm;
		this.logicalProcessor = logicalProcessor;
		potentialsPositions = sdm.potentialsPositions[logicalProcessor];
		resultDimension = sdm.result.getDimensions();
		resultCoordinate = sdm.resultIntervals[logicalProcessor];
		tables = sdm.tables;
		offAccPotentials = sdm.offAccPotentials;
		resultTable = sdm.result.values;
		numPotentials = sdm.numPotentials;
		constantFactor = sdm.constantFactor;
	}

	public void run() {
		// Multiply
		int incrementedVariable = 0;
		double mulResult;

		int initialPosition = sdm.resultPositionIntervals[logicalProcessor];
		int finalPosition = initialPosition + sdm.lengthResultIntervals[logicalProcessor];
		
		/*
		System.out.println("Logical processor: " + logicalProcessor + 
			", Initial position: " + initialPosition + ", Final position: " +
			finalPosition + ". Total iterations: " 
			+ (finalPosition - initialPosition) + "\n" 
			+ "First resultCoordinate: " 
			+ openmarkov.UtilTestMethods.printArrayOfIntegers(resultCoordinate));
		*/

		for (int resultPosition = initialPosition; resultPosition < finalPosition; resultPosition++) {
			mulResult = constantFactor;

          /* increment the result coordinate and
			   find out which variable is to be incremented */
			for (int iVariable = 0; iVariable < resultCoordinate.length; iVariable++) {
				// try by incrementing the current variable (given by iVariable)
				resultCoordinate[iVariable]++;
				if (resultCoordinate[iVariable] != resultDimension[iVariable]) {
					// we have incremented the right variable
					incrementedVariable = iVariable;
					// do not increment other variables;
					break;
				}
				/* this variable could not be incremented;
				   we set it to 0 in resultCoordinate
				   (the next iteration of the for-loop will increment
				   the next variable) */
				resultCoordinate[iVariable] = 0;
			}

			// multiply
			for (int iPotential = 0; iPotential < numPotentials; iPotential++) {
				// multiply the numbers
				try {
					mulResult = mulResult * tables[iPotential][potentialsPositions[iPotential]];
				} catch (Exception e) {
					System.err.println(getErrorMsg(e, iPotential));
				}
				// update the current position in each potential table
				potentialsPositions[iPotential] += offAccPotentials[iPotential][incrementedVariable];

			}
			resultTable[resultPosition] = mulResult;
		}
	}

	private String getErrorMsg(Exception e, int iPotential) {
		String error = new String();
		error = error + e.getMessage();
		error = error + "Logical processor: " + logicalProcessor + "\n";
		error = error + "iPotential: " + iPotential + "\n";
		error = error + "potentialsPositions.length: " + potentialsPositions.length + "\n";
		error = error + "tables[iPotential].length: " + sdm.tables[iPotential].length + "\n";
		error = error + "potentialsPositions[iPotential]: " + potentialsPositions[iPotential] + "\n";

		return error;
	}
}
