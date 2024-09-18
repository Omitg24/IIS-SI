/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.model.network.potential.operation.concurrent;

import org.openmarkov.core.model.network.potential.TablePotential;

public class DiscreteDivide implements Runnable {

	int[] dimension;
	int[] potentialsPositions;
	private TablePotential quotient;
	private int numVariables;
	private int[] quotientCoordinate;
	private int[] quotientDimension;
	private TablePotential numerator;
	private TablePotential denominator;
	private int[] offset;
	private int[][] offsetsAccumulate;

	public DiscreteDivide(SharedDataDivide sdd, int logicalProcessor) {
		this.numerator = sdd.numerator;
		this.denominator = sdd.denominator;
		this.quotientCoordinate = sdd.quotientCoordinate;
		this.quotientDimension = sdd.quotientDimension;
		this.offsetsAccumulate = sdd.offsetAccumulate;
		this.quotient = sdd.result;
	}

	public void run() {
		int tamTable = 1; // If numVariables == 0 the potential is a constant
		if (numVariables > 0) {
			tamTable = dimension[numVariables - 1] * offset[numVariables - 1];
		}

		int incrementedVariable = 0;
		for (int quotientPosition = 0; quotientPosition < tamTable; quotientPosition++) {
			/* increment the result coordinate and
			   find out which variable is to be incremented */
			for (int iVariable = 0; iVariable < quotientCoordinate.length; iVariable++) {
				// try by incrementing the current variable (given by iVariable)
				quotientCoordinate[iVariable]++;
				if (quotientCoordinate[iVariable] != quotientDimension[iVariable]) {
					// we have incremented the right variable
					incrementedVariable = iVariable;
					// do not increment other variables;
					break;
				}
				/* this variable could not be incremented;
				   we set it to 0 in resultCoordinate
				   (the next iteration of the for-loop will increment
				   the next variable) */
				quotientCoordinate[iVariable] = 0;
			}

			// divide
			if (denominator.values[potentialsPositions[1]] == 0.0) {
				quotient.values[quotientPosition] = 0.0;
			} else {
				quotient.values[quotientPosition] = numerator.values[potentialsPositions[0]]
						/ denominator.values[potentialsPositions[1]];
			}
			for (int iPotential = 0; iPotential < 2; iPotential++) {
				// update the current position in each potential table
				potentialsPositions[iPotential] += offsetsAccumulate[iPotential][incrementedVariable];
			}
		}
	}
}
