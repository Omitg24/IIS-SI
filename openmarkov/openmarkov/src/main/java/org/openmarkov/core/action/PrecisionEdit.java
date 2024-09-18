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
  This is a simple edit that allows modify the node precision property.
  @version 1.0 21/12/10
 * @author Miguel Palacios
 */
public class PrecisionEdit extends SimplePNEdit {
	/**
	 * The last purpose before the edition
	 */
	private Double lastPrecision;
	/**
	 * The new purpose after the edition
	 */
	private double newPrecision;
	/**
	 * The edited node
	 */
	private Node node = null;

	/**
	 * Creates a new {@code PurposeEdit} with the node and its new purpose.
	 *
	 * @param node         the edited node
	 * @param newPrecision the new precision
	 */
	public PrecisionEdit(Node node, double newPrecision) {
		super(node.getProbNet());
		this.lastPrecision = node.getVariable().getPrecision();
		this.newPrecision = newPrecision;
		this.node = node;
	}

	@Override public void doEdit() throws DoEditException {
		node.getVariable().setPrecision(newPrecision);
	}

	@Override public void undo() {
		super.undo();
		node.getVariable().setPrecision(lastPrecision);
	}

}

