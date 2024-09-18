/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.action;

import javax.swing.undo.CannotUndoException;

import org.openmarkov.core.exception.DoEditException;
import org.openmarkov.core.model.network.CycleLength;
import org.openmarkov.core.model.network.ProbNet;

public class CycleLengthEdit extends SimplePNEdit {

	/**
	 * Default serial UID
	 */
	private static final long serialVersionUID = 1L;

	private CycleLength oldTemporalUnit;
	private CycleLength newTemporalUnit;

	public CycleLengthEdit(ProbNet probNet, CycleLength newTemporalUnit) {
		super(probNet);
		this.oldTemporalUnit = probNet.getCycleLength().clone();
		this.newTemporalUnit = newTemporalUnit;
	}

	@Override public void doEdit() throws DoEditException {
		probNet.setCycleLength(this.newTemporalUnit);
	}

	@Override public void undo() throws CannotUndoException {
		super.undo();
		probNet.setCycleLength(this.oldTemporalUnit);
	}

	@Override public void redo() {
		super.redo();
		try {
			doEdit();
		} catch (DoEditException e) {
			e.printStackTrace();
		}
	}

}
