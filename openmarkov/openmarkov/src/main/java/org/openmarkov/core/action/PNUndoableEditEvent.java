/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.action;

import javax.swing.event.UndoableEditEvent;
import javax.swing.undo.UndoableEdit;

import org.openmarkov.core.model.network.ProbNet;

@SuppressWarnings("serial")

/*
  The different between {@code PNUndoableEditEvent} and
  {@code UndoableEditEvent} is that a <code>PNUndoableEditEvent</code>
  use a {@code ProbNet}.
 */
public class PNUndoableEditEvent extends UndoableEditEvent {

	// Attributes
	private ProbNet probNet;

	// Constructor

	/**
	 * @param source  The {@code Object} that originated the event.
	 * @param edit    An {@code UndoableEdit} object.
	 * @param probNet The {@code ProbNet} on witch the event will operate
	 */
	public PNUndoableEditEvent(Object source, UndoableEdit edit, ProbNet probNet) {
		super(source, edit);
		this.probNet = probNet;
	}

	// Methods

	/**
	 * @return probNet. {@code ProbNet}
	 */
	public ProbNet getProbNet() {
		return probNet;
	}

}
