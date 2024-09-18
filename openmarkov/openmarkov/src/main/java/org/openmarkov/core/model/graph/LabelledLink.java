/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.model.graph;

/**
 * @author fjdiez
 * @author manuel
 * @version 1.0
 * @see Link
 * @since OpenMarkov 1.0
 */
public class LabelledLink<T> extends Link<T> {

	// Attributes
	/**
	 * In labelled graphs labels are used to distinguish among links.
	 * In unlabelled graphs, it can be used for other purposes.
	 */
	protected Object label;

	// Constructor

	/**
	 * Creates a labelled link and sets the cross references in the nodes.
	 *
	 * @param node1    {@code Node}
	 * @param node2    {@code Node}
	 * @param directed {@code boolean}
	 * @param label    {@code Object}
	 */
	public LabelledLink(T node1, T node2, boolean directed, Object label) {
		super(node1, node2, directed);
		this.label = label;
	}

	// Methods

	/**
	 * Gets the label value
	 *
	 * @return label {@code Object}
	 */
	public Object getLabel() {
		return label;
	}

	/**
	 * Sets the label value
	 *
	 * @param label {@code Object}
	 */
	public void setLabel(Object label) {
		this.label = label;
	}

}
