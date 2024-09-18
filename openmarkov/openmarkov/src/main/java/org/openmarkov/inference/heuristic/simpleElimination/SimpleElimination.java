/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.inference.heuristic.simpleElimination;

import java.util.List;

import javax.swing.event.UndoableEditEvent;

import org.openmarkov.core.exception.ConstraintViolationException;
import org.openmarkov.core.exception.NonProjectablePotentialException;
import org.openmarkov.core.exception.WrongCriterionException;
import org.openmarkov.core.inference.heuristic.EliminationHeuristic;
import org.openmarkov.core.model.network.Node;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.Variable;

/**
 * This heuristic chooses a <code>Node</code> to eliminate. The rule is:
 * Choose the node with fewer neighbors.
 *
 * @author manuel
 * @author fjdiez
 */
public class SimpleElimination extends EliminationHeuristic {

	/**
	 * @param probNet probNet
	 * @param queryVariables queryVariables
	 */
	public SimpleElimination(ProbNet probNet, List<List<Variable>> queryVariables) {
		super(probNet, queryVariables);
	}

	@Override
	/** This method returns the <code>Node</code> that fulfils the rule
	 * defined in the heuristic: It chooses the node with less neighbours
	 * @see openmarkov.inference.CanoMoralElimination#getNextNodeToDelete()
	 * @see openmarkov.inference.EliminationHeuristic#getNodeToDelete() */ public Variable getVariableToDelete() {
		Node bestNode = null;
		int numNeighborsBestNode = Integer.MAX_VALUE;
		for (int i = nodesToEliminate.size() - 1; i >= 0 && bestNode == null; i--) {
			for (Node node : nodesToEliminate.get(i)) {
				if (node != null) {
					int numSiblings = node.getNumSiblings();
					if (numSiblings < numNeighborsBestNode) {
						bestNode = node;
						numNeighborsBestNode = numSiblings;
					}
				}
			}
		}
		return (bestNode != null) ? bestNode.getVariable() : null;
	}

	@Override public void undoableEditWillHappen(UndoableEditEvent event)
			throws ConstraintViolationException, NonProjectablePotentialException,
			WrongCriterionException {
		// TODO Auto-generated method stub
}

	@Override public void undoEditHappened(UndoableEditEvent event) {
		// TODO Auto-generated method stub

	}

}
