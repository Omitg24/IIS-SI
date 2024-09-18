/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.inference.heuristic.fileElimination;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.event.UndoableEditEvent;

import org.apache.logging.log4j.LogManager;
import org.openmarkov.core.inference.heuristic.EliminationHeuristic;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.Variable;

/**
 * This heuristic reads a list of variable names from a file.
 *
 * @author manuel
 * @author fjdiez
 * @version 1.0
 * @since OpenMarkov 1.0
 */
public class FileElimination extends EliminationHeuristic {

	// Attributes
	protected List<List<Variable>> fileVariables;

	// Constructor

	/**
	 * @param probNet                    <code>ProbNet</code>
	 * @param setsOfVariablesToEliminate <code>ArrayList</code> of
	 *                                   <code>Variable</code>
	 * @param fileName                   = path + file name. <code>String</code>
	 */
	public FileElimination(ProbNet probNet, List<List<Variable>> setsOfVariablesToEliminate, String fileName) {
		super(probNet, setsOfVariablesToEliminate);
		fileVariables = setsOfVariablesToEliminate = readEliminationOrder(fileName, setsOfVariablesToEliminate);
	}

	/**
	 * Reads from a file a set of variables names
	 *
	 * @param fileName            <code>String</code>
	 * @param setsOfSetsVariables <code>ArrayList</code> of
	 *                            <code>? extends Variable</code>
	 * @return An ordered <code>ArrayList</code> of variables taken from
	 * <code>variables</code> corresponding to the given names
	 */
	public static List<List<Variable>> readEliminationOrder(String fileName, List<List<Variable>> setsOfSetsVariables) {

		String fullFileName = fileName.replace("elv", "hugin");

		ArrayList<String> names = new ArrayList<String>();
		String line;
		// Reads the file
		try {
			BufferedReader in = new BufferedReader(new FileReader(new File(fullFileName)));

			while ((line = in.readLine()) != null) {
				names.add(line);
			}
			in.close();
		} catch (IOException ioException) {
			LogManager.getLogger(FileElimination.class).fatal(ioException);
		}

		List<List<Variable>> orderedVariables = new ArrayList<>(1);
		List<Variable> allVariables = new ArrayList<Variable>();
		orderedVariables.add(allVariables);

		for (String name : names) {
			for (List<Variable> variables : setsOfSetsVariables) {
				for (Variable variable : variables) {
					if (name.contains(variable.getName())) {
						allVariables.add(variable);
						break;
					}
				}
			}
		}

		// Reverse variables because they will be taken from the last to the first in the array.
		int first = 0;
		int last = allVariables.size() - 1;
		Variable auxSwap;
		while (first < last) {
			auxSwap = allVariables.get(first);
			allVariables.set(first, allVariables.get(last));
			allVariables.set(last, auxSwap);
			first++;
			last--;
		}

		return orderedVariables;
	}

	// Methods
	@Override
	/** @return The <code>Variable</code> that the heuristic suggest to
	 *   eliminate */ public Variable getVariableToDelete() {
		Variable variable = null;
		List<Variable> variables = null;
		int i = fileVariables.size();
		while (--i >= 0 && (variables = fileVariables.get(i)).size() == 0)
			;
		if (i >= 0) {
			variable = variables.get(variables.size() - 1);
		}
		return variable;
	}

	public void undoableEditWillHappen(UndoableEditEvent e) {
	}

	public void undoableEditHappened(UndoableEditEvent e) {
		Variable variableToEliminate = getVariableToDelete();
		fileVariables.get(0).remove(variableToEliminate);
	}

	public void undoEditHappened(UndoableEditEvent event) {
	}

}
