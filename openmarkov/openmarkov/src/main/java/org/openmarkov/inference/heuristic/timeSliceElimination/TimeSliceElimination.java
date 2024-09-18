/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.inference.heuristic.timeSliceElimination;

import java.util.ArrayList;
import java.util.List;

import javax.swing.event.UndoableEditEvent;

import org.openmarkov.core.exception.ConstraintViolationException;
import org.openmarkov.core.exception.NonProjectablePotentialException;
import org.openmarkov.core.exception.WrongCriterionException;
import org.openmarkov.core.inference.heuristic.EliminationHeuristic;
import org.openmarkov.core.model.network.Node;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.potential.TablePotential;
import org.openmarkov.core.model.network.potential.operation.AuxiliaryOperations;

/**
 * This heuristic is meant for dynamic/temporal models and it eliminates variables starting
 * from the last slice and towards the first. It eliminates all variables of a slice before
 * proceeding to the next. Amongst the variables of each slice, it chooses the one that would
 * create the smallest potential.
 *
 * @author inigo
 */
public class TimeSliceElimination extends EliminationHeuristic {

	private int currentIndex;
	private List<List<List<Variable>>> variablesToEliminateInSlices;

	public TimeSliceElimination(ProbNet probNet, List<List<Variable>> variablesToEliminate) {
		super(probNet, variablesToEliminate);

		int numSlices = getNumSlices(variablesToEliminate);

		this.variablesToEliminateInSlices = new ArrayList<List<List<Variable>>>();

		for (List<Variable> variableList : variablesToEliminate) {
			List<List<Variable>> variablesInSlices = new ArrayList<>(numSlices + 2);
			for (int i = 0; i < numSlices + 2; ++i)
				variablesInSlices.add(new ArrayList<Variable>());

			for (Variable variable : variableList) {
				int index = variable.isTemporal() ? variable.getTimeSlice() + 1 : 0;
				variablesInSlices.get(index).add(variable);
			}
			this.variablesToEliminateInSlices.add(variablesInSlices);
		}

		currentIndex = numSlices + 1;
	}

	private int getNumSlices(List<List<Variable>> variables) {

		int numSlices = -1;
		for (List<Variable> variableList : variables) {
			for (Variable variable : variableList) {
				if (variable.isTemporal() && variable.getTimeSlice() > numSlices)
					numSlices = variable.getTimeSlice();
			}
		}
		return numSlices;
	}

	@Override public void undoableEditWillHappen(UndoableEditEvent event)
			throws ConstraintViolationException, NonProjectablePotentialException,
			WrongCriterionException {
		// TODO Auto-generated method stub
	}

	@Override public void undoEditHappened(UndoableEditEvent event) {
	}

	@Override public Variable getVariableToDelete() {
		Node bestNode = null;
		int bestScore = Integer.MAX_VALUE;
		if (currentIndex >= 0) {
			for (int i = variablesToEliminateInSlices.size() - 1; i >= 0 && bestNode == null; i--) {
				for (Variable variable : variablesToEliminateInSlices.get(i).get(currentIndex)) {
					Node node = probNet.getNode(variable);
					if (node != null) {
						List<Variable> variables = AuxiliaryOperations
								.getUnionVariables(probNet.getProbPotentials(variable));
						int tableSize = TablePotential.computeTableSize(variables);
						if (tableSize < bestScore) {
							bestScore = tableSize;
							bestNode = node;
						}
					}
				}
			}
		}
		return (bestNode != null) ? bestNode.getVariable() : null;
	}

	public void undoableEditHappened(UndoableEditEvent event) {
		super.undoableEditHappened(event);
		Variable deletedVariable = getEventVariable(event);
		if (deletedVariable != null) {
			List<List<Variable>> removableVariables = variablesToEliminateInSlices
					.get(variablesToEliminateInSlices.size() - 1);
			int index = deletedVariable.isTemporal() ? deletedVariable.getTimeSlice() + 1 : 0;
			removableVariables.get(index).remove(deletedVariable);
			while (currentIndex >= 0 && removableVariables.get(currentIndex).isEmpty()) {
				currentIndex--;
			}
			if (currentIndex < 0) {
				variablesToEliminateInSlices.remove(variablesToEliminateInSlices.size() - 1);
				if (!variablesToEliminateInSlices.isEmpty())
					currentIndex = variablesToEliminateInSlices.get(variablesToEliminateInSlices.size() - 1).size() - 1;
			}
		}

	}

}
