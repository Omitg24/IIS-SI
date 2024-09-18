/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.action;

import org.openmarkov.core.exception.DoEditException;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.potential.Potential;

/**
 * Changes an old potential for a new potential
 */
@SuppressWarnings("serial") public class PotentialChangeEdit extends SimplePNEdit {

	// Attribute
	private Potential newPotential;

	private Potential oldPotential;

	// Constructor

	/**
	 * @param probNet      {@code ProbNet}
	 * @param oldPotential {@code Potential}
	 * @param newPotential {@code Potential}
	 */
	public PotentialChangeEdit(ProbNet probNet, Potential oldPotential, Potential newPotential) {
		super(probNet);
		this.newPotential = newPotential;
		this.oldPotential = oldPotential;
	}

	@Override public void doEdit() throws DoEditException {
		if (probNet.removePotential(oldPotential) == null) {
			throw new DoEditException("Can not remove potential: " + oldPotential.toString());
		}
		probNet.addPotential(newPotential);
	}

	public void undo() {
		super.undo();
		probNet.removePotential(newPotential);
		probNet.addPotential(oldPotential);
	}

	/**
	 * @return A {@code String} with the variables of both potentials.
	 */
	public String toString() {
		return "ChangePotentialEdit: " + oldPotential.getVariables() + " --> " + newPotential.getVariables();
	}

	public Potential getNewPotential() {
		return newPotential;
	}

	public Potential getOldPotential() {
		return oldPotential;
	}

}
