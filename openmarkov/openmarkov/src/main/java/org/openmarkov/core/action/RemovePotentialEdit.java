/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.action;

import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.potential.Potential;

@SuppressWarnings("serial") public class RemovePotentialEdit extends SimplePNEdit implements PNEdit {

	// Attributes
	protected Potential oldPotential;

	// Constructor

	/**
	 * @param probNet   {@code ProbNet}
	 * @param potential {@code Potential}
	 */
	public RemovePotentialEdit(ProbNet probNet, Potential potential) {
		super(probNet);
		oldPotential = potential;
	}

	// Methods
	@Override public void doEdit() {
		probNet.removePotential(oldPotential);
	}

	public void undo() {
		super.undo();
		probNet.addPotential(oldPotential);
	}

	public String toString() {
		return "RemovePotentialEdit: " + oldPotential.getVariables();
	}

}
