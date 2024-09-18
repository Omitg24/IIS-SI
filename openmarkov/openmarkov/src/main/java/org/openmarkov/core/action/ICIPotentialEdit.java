/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.action;

import javax.swing.undo.CannotUndoException;

import org.openmarkov.core.exception.DoEditException;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.potential.canonical.ICIPotential;

@SuppressWarnings("serial") public class ICIPotentialEdit extends SimplePNEdit {

	private ICIPotential potential = null;
	private Variable variable = null;
	private double[] noisyParameters = null;
	private double[] oldNoisyParameters = null;
	private double[] leakyParameters = null;
	private double[] oldLeakyParameters = null;
	private boolean isNoisyParameter;

	public ICIPotentialEdit(ProbNet probNet, ICIPotential potential, Variable variable, double[] noisyParameters) {
		super(probNet);
		this.potential = potential;
		this.variable = variable;
		this.oldNoisyParameters = potential.getNoisyParameters(variable);
		this.noisyParameters = noisyParameters;
		this.isNoisyParameter = true;
	}

	public ICIPotentialEdit(ProbNet probNet, ICIPotential potential, double[] leakyParameters) {
		super(probNet);
		this.potential = potential;
		this.oldLeakyParameters = potential.getLeakyParameters();
		this.leakyParameters = leakyParameters;
		this.isNoisyParameter = false;
	}

	@Override public void doEdit() throws DoEditException {
		if (isNoisyParameter) {
			potential.setNoisyParameters(variable, noisyParameters);
		} else {
			potential.setLeakyParameters(leakyParameters);
		}
	}

	@Override public void undo() throws CannotUndoException {
		super.undo();
		if (isNoisyParameter) {
			potential.setNoisyParameters(variable, oldNoisyParameters);
		} else {
			potential.setLeakyParameters(oldLeakyParameters);
		}
	}

	public ICIPotential getPotential() {
		return potential;
	}

	public Variable getVariable() {
		return variable;
	}

	public double[] getNoisyParameters() {
		return noisyParameters;
	}

	public double[] getLeakyParameters() {
		return leakyParameters;
	}

	public boolean isNoisyParameter() {
		return isNoisyParameter;
	}

}
