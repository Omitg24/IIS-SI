/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.action;

import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.State;

/**
 * {@code NetworkDefaultStatesEdit} is a simple edit that allows modify
 * the default states of the network
 *
 * @author Miguel Palacios
 * @version 1.0 21/12/2010
 */
@SuppressWarnings("serial") public class NetworkDefaultStatesEdit extends SimplePNEdit {

	/**
	 * The current default states of the network
	 */
	private State[] currentDefaultStates;

	/**
	 * The new default states of the network
	 */
	private State[] newDefaultStates;

	/**
	 * Creates a {@code NetworkDefaultStatesEdit} with the network and new
	 * default states specified.
	 *
	 * @param probNet       the network that will be modified.
	 * @param defaultStates the new default states.
	 */
	public NetworkDefaultStatesEdit(ProbNet probNet, State[] defaultStates) {
		super(probNet);
		this.currentDefaultStates = probNet.getDefaultStates();
		this.newDefaultStates = defaultStates;
	}

	// Methods
	@Override public void doEdit() {
		if (newDefaultStates != null) {
			probNet.setDefaultStates(newDefaultStates);
		}
	}

	public void undo() {
		super.undo();
		if (currentDefaultStates != null) {
			probNet.setDefaultStates(currentDefaultStates);
		}
	}
}
