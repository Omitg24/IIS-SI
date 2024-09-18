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
 * {@code RelevanceEdit} is a simple edit that allows modify
 * the node relevance property.
 *
 * @author Miguel Palacios
 * @version 1.0 21/12/10
 */
@SuppressWarnings("serial") public class RelevanceEdit extends SimplePNEdit {
	/**
	 * The last relevance before the edition
	 */
	private double lastRelevance;
	/**
	 * The new relevance after the edition
	 */
	private double newRelevance;
	/**
	 * The edited node
	 */
	private Node node = null;

	/**
	 * Creates a new {@code RelevanceEdit} with the node and new relevance
	 * specified.
	 *
	 * @param node         the node that will be edited
	 * @param newRelevance the new relevance
	 */
	public RelevanceEdit(Node node, double newRelevance) {
		super(node.getProbNet());
		this.lastRelevance = node.getRelevance();
		this.newRelevance = newRelevance;
		this.node = node;
	}

	@Override public void doEdit() throws DoEditException {
		node.setRelevance(newRelevance);
	}

	@Override public void undo() {
		super.undo();
		node.setRelevance(lastRelevance);
	}

	/**
	 * Gets the new relevance after the edition
	 *
	 * @return the new relevance
	 */
	public double getNewRelevance() {
		return newRelevance;
	}

	/**
	 * Gets the new relevance before the edition
	 *
	 * @return the last relevance
	 */
	public double getLastRelevance() {
		return lastRelevance;
	}
}