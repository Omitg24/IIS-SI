/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.action;

import org.openmarkov.core.exception.DoEditException;
import org.openmarkov.core.exception.NodeNotFoundException;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.Variable;

@SuppressWarnings("serial") public class OrientLinkEdit extends BaseLinkEdit {

	/**
	 * @param probNet    {@code ProbNet}
	 * @param variable1  {@code Variable}
	 * @param variable2  {@code Variable}
	 * @param isDirected {@code boolean}
	 */
	public OrientLinkEdit(ProbNet probNet, Variable variable1, Variable variable2, boolean isDirected) {
		super(probNet, variable1, variable2, isDirected);
	}

	// Methods
	/**
	 * Do the edition by removing the existing link and adding a new directed link between the same two variables.
	 * @throws DoEditException DoEditException
	 */
	@Override
	public void doEdit() throws DoEditException {
		try {
			probNet.removeLink(variable1, variable2, false);
			probNet.addLink(variable1, variable2, true);
		} catch (NodeNotFoundException e) {
			throw new DoEditException(e);
		}
	}

	/**
	 * Undo the edition by removing the existing link and adding
	 * a new undirected link between the same two variables.
	 */
	public void undo() {
		super.undo();
		try {
			probNet.removeLink(variable1, variable2, true);
			probNet.addLink(variable1, variable2, false);
		} catch (NodeNotFoundException e) {
			System.err.println(e.getMessage());
			e.printStackTrace(System.err);
		}
	}

	/**
	 * Method to compare two directLinkEdits comparing the names of
	 * the source and destination variables alphabetically.
	 * @param obj Edit
	 * @return result of the comparison
	 */
	public int compareTo(OrientLinkEdit obj) {
		int result;

		if ((
				result = variable1.getName().compareTo(obj.getVariable1().
						getName())
		) != 0)
			return result;
		if ((
				result = variable2.getName().compareTo(obj.getVariable2().
						getName())
		) != 0)
			return result;
		else
			return 0;
	}

	@Override public String getOperationName() {
		return "Orient link";
	}

	@Override public BaseLinkEdit getUndoEdit() {
		return this;
	}
}
