/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.action;

import javax.swing.undo.CannotUndoException;

import org.openmarkov.core.exception.DoEditException;
import org.openmarkov.core.inference.TemporalOptions;
import org.openmarkov.core.model.network.ProbNet;

public class TemporalOptionsEdit extends SimplePNEdit {

	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 1L;

	private TemporalOptions oldTemporalOptions;
	private TemporalOptions newTemporalOptions;

	public TemporalOptionsEdit(ProbNet probNet, TemporalOptions options) {
		super(probNet);
		this.oldTemporalOptions = probNet.getInferenceOptions().getTemporalOptions().clone();
		this.newTemporalOptions = options;
	}

	@Override public void doEdit() throws DoEditException {
		probNet.getInferenceOptions().setTemporalOptions(this.newTemporalOptions);

	}

	@Override public void undo() throws CannotUndoException {
		super.undo();
		probNet.getInferenceOptions().setTemporalOptions(this.oldTemporalOptions);
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
