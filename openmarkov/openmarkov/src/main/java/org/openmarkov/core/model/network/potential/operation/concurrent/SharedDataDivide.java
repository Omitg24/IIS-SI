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
import org.openmarkov.core.model.network.potential.Potential;
import org.openmarkov.core.model.network.potential.PotentialRole;
import org.openmarkov.core.model.network.potential.TablePotential;
import org.openmarkov.core.model.network.potential.operation.DiscretePotentialOperations;

/**
 * Contains shared data used concurrently by a set of threads computing an
 * operation. This class is not synchronized because the set of threads
 * accomplish the three conditions of Bernstein
 * All data have package access because they will be used outside this class,
 * only in this package and the access must be efficient (there is not getX or
 * setX)
 */
public class SharedDataDivide {

	TablePotential numerator;

	TablePotential denominator;

	int numVariables;

	double[][] tables;

	int[] quotientDimension;

	int[] quotientCoordinate;

	int[][] offsetAccumulate;

	int[][] potentialPositions;

	int tamTable;

	TablePotential result;

	public SharedDataDivide(Potential numerator, Potential denominator) {
		this.numerator = (TablePotential) numerator;
		this.denominator = (TablePotential) denominator;
	}

	public void initialize() {
		List<Variable> quotientVariables = numerator.getVariables();
		TablePotential quotient = null;
		try {
			quotient = new TablePotential(quotientVariables, PotentialRole.JOINT_PROBABILITY);
		} catch (Exception e) {
			e.printStackTrace();
		}

		int numVariables = quotient.getVariables().size();

		// Gets the tables of each TablePotential
		double[][] tables = new double[2][];
		tables[0] = numerator.values;
		tables[1] = denominator.values;

		// Gets dimension
		quotientDimension = quotient.getDimensions();

		// Gets offset accumulate
		ArrayList<TablePotential> potentials = new ArrayList<>();
		potentials.add(numerator);
		potentials.add(denominator);
		offsetAccumulate = DiscretePotentialOperations.getAccumulatedOffsets(potentials, quotient);

		// Gets coordinate
		if (numVariables != 0) {
			quotientCoordinate = new int[numVariables];
		} else {
			quotientCoordinate = new int[1];
			quotientCoordinate[0] = 0;
		}

		// Position in each table potential
		int[] potentialsPositions = new int[2];
		for (int i = 0; i < 2; i++) {
			potentialsPositions[i] = 0;
		}
	}

}
