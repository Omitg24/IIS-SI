/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.action;

import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.potential.Potential;

@SuppressWarnings("serial") public class AddPotentialEdit extends SimplePNEdit {

	protected Potential potential;

	// Constructor

	/**
	 * @param probNet   {@code ProbNet}
	 * @param potential {@code Potential}
	 */
	public AddPotentialEdit(ProbNet probNet, Potential potential) {
		super(probNet);
		this.potential = potential;
	}

	// Methods
	@Override public void doEdit() {
		probNet.addPotential(potential);
	}

	public void undo() {
		super.undo();
		probNet.removePotential(potential);
	}

	/**
	 * @return potential {@code Potential}
	 */
	public Potential getPotential() {
		return potential;
	}

	/**
	 * @return A {@code String} with the potential variables.
	 */
	public String toString() {
		StringBuilder buffer = new StringBuilder("AddPotentialEdit: ");
		if (potential != null) {
			buffer.append(potential.getVariables());
		} else {
			buffer.append("null !!!!");
		}
		return buffer.toString();
	}

}
