/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.oopn;

import org.openmarkov.core.model.network.Node;

public class NodeReferenceLink extends ReferenceLink {

	private Node sourceNode;
	private Node destinationNode;

	public NodeReferenceLink(Node sourceNode, Node destinationNode) {
		this.sourceNode = sourceNode;
		this.destinationNode = destinationNode;
	}

	public Node getSourceNode() {
		return sourceNode;
	}

	public Node getDestinationNode() {
		return destinationNode;
	}

}
