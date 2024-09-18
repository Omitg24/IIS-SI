/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.oopn.action;

import javax.swing.undo.CannotUndoException;

import org.openmarkov.core.action.SimplePNEdit;
import org.openmarkov.core.exception.DoEditException;
import org.openmarkov.core.model.network.Node;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.oopn.Instance;

@SuppressWarnings("serial") public class MarkAsInputEdit extends SimplePNEdit {

	private Node node = null;
	private Instance instance = null;
	private boolean isInput = false;
	private boolean wasInput = false;

	public MarkAsInputEdit(ProbNet probNet, boolean isInput, Node node) {
		super(probNet);
		this.isInput = isInput;
		this.node = node;
	}

	public MarkAsInputEdit(ProbNet probNet, boolean isInput, Instance instance) {
		super(probNet);
		this.isInput = isInput;
		this.instance = instance;
	}

	@Override public void doEdit() throws DoEditException {
		if (node != null) {
			node.setInput(isInput);
			wasInput = node.isInput();
		}
		if (instance != null) {
			instance.setInput(isInput);
			wasInput = instance.isInput();
		}
	}

	@Override public void undo() throws CannotUndoException {
		super.undo();
		if (node != null) {
			node.setInput(wasInput);
		}
		if (instance != null) {
			instance.setInput(wasInput);
		}
	}

}
