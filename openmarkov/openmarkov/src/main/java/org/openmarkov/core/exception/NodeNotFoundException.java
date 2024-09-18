/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.exception;

import org.openmarkov.core.model.network.Node;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.Variable;

@SuppressWarnings("serial") public class NodeNotFoundException extends OpenMarkovException {

	// Constructor

	/**
	 * @param message {@code String}
	 */
	public NodeNotFoundException(String message) {
		super(message);
	}

	/**
	 * @param network      Network
	 * @param variableName Name of the variable
	 */
	public NodeNotFoundException(ProbNet network, String variableName) {
		super("Variable: " + variableName + " not found in network " + network.getName() + ".");
	}

	public NodeNotFoundException(Node node) {
		this(node.getProbNet(), node.getName());
	}

	public NodeNotFoundException(ProbNet network, Variable variable) {
		this(network, variable.getName());
	}

}
