/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.action;

import org.openmarkov.core.exception.DoEditException;
import org.openmarkov.core.model.network.Node;

@SuppressWarnings("serial")

/*
  {@code PurposeEdit} is a simple edit that allows modify
  the node purpose property.

  @version 1.0 21/12/10
 * @author Miguel Palacios
 */
public class PurposeEdit extends SimplePNEdit {
	/**
	 * The last purpose before the edition
	 */
	private String lastPurpose;
	/**
	 * The new purpose after the edition
	 */
	private String newPurpose;
	/**
	 * The edited node
	 */
	private Node node = null;

	/**
	 * Creates a new {@code PurposeEdit} with the node and its new purpose.
	 *
	 * @param node       the edited node
	 * @param newPurpose the new purpose
	 */
	public PurposeEdit(Node node, String newPurpose) {
		super(node.getProbNet());
		this.lastPurpose = node.getPurpose();
		this.newPurpose = newPurpose;
		this.node = node;
	}

	@Override public void doEdit() throws DoEditException {
		node.setPurpose(newPurpose);
	}

	@Override public void undo() {
		super.undo();
		node.setPurpose(lastPurpose);
	}

	/**
	 * Gets the new purpose after the edition
	 *
	 * @return the new purpose
	 */
	public String getNewPurpose() {
		return newPurpose;
	}

	/**
	 * Gets the last purpose before the edition
	 *
	 * @return the last purpose
	 */
	public String getLastPurpose() {
		return lastPurpose;
	}
}

