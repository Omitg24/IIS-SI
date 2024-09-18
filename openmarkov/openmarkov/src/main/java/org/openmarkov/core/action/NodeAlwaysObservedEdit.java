/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */
package org.openmarkov.core.action;

import org.openmarkov.core.exception.DoEditException;
import org.openmarkov.core.model.network.Node;

/**
 * {@code NodeAlwaysObservedEdit} is a simple edit that allow modify the always observed property of a variable
 * name.
 */

@SuppressWarnings("serial") public class NodeAlwaysObservedEdit extends SimplePNEdit {

	/**
	 * The last 'alwaysObserved' before the edition
	 */
	private boolean lastAlwaysObserved;
	/**
	 * The new 'alwaysObserved' after the edition
	 */
	private boolean newAlwaysObserved;
	/**
	 * The edited node
	 */
	private Node node = null;

	/**
	 * Creates a new {@code AlwaysObservedEdit} with the node and new 'alwaysObserved'
	 * specified.
	 *
	 * @param node              the node that will be edited
	 * @param newAlwaysObserved the new alwaysObserved
	 */
	public NodeAlwaysObservedEdit(Node node, boolean newAlwaysObserved) {
		super(node.getProbNet());
		this.lastAlwaysObserved = node.isAlwaysObserved();
		this.newAlwaysObserved = newAlwaysObserved;
		this.node = node;
	}

	@Override public void doEdit() throws DoEditException {
		node.setAlwaysObserved(newAlwaysObserved);
	}

	@Override public void undo() {
		super.undo();
		node.setAlwaysObserved(lastAlwaysObserved);
	}

	/**
	 * Gets the new alwaysObserved after the edition
	 *
	 * @return the new alwaysObserved
	 */
	public boolean getNewAlwaysObserved() {
		return newAlwaysObserved;
	}

	/**
	 * Gets the new alwaysObserved before the edition
	 *
	 * @return the last alwaysObserved
	 */
	public boolean getLastAlwaysObserved() {
		return lastAlwaysObserved;
	}

	public Node getNode() {
		return node;
	}

}
