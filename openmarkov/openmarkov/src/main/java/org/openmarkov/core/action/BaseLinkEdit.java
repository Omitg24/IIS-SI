/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmarkov.core.action;

import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.Variable;

/**
 * @author Inigo
 */
@SuppressWarnings("serial") public abstract class BaseLinkEdit extends SimplePNEdit {

	// Attributes
	protected Variable variable1;

	protected Variable variable2;

	protected boolean isDirected;

	// Constructor

	/**
	 * @param probNet    {@code ProbNet}
	 * @param variable1  {@code Variable}
	 * @param variable2  {@code Variable}
	 * @param isDirected {@code boolean}
	 */
	public BaseLinkEdit(ProbNet probNet, Variable variable1, Variable variable2, boolean isDirected) {

		super(probNet);
		this.variable1 = variable1;
		this.variable2 = variable2;
		this.isDirected = isDirected;
	}

	/**
	 * @return variable1 {@code Variable}
	 */
	public Variable getVariable1() {
		return variable1;
	}

	/**
	 * @return variable2 {@code Variable}
	 */
	public Variable getVariable2() {
		return variable2;
	}

	public boolean isDirected() {
		return isDirected;
	}

	@Override public int hashCode() {
		return variable1.hashCode() + 7 * variable2.hashCode() + 17 * ((isDirected) ? 1 : 0);
	}

	@Override public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if ((obj == null) || (obj.getClass() != this.getClass()))
			return false;
		return (
				(this.variable1.equals(((BaseLinkEdit) obj).variable1)) && (
						this.variable2.equals(((BaseLinkEdit) obj).variable2)
				) && (this.isDirected == (((BaseLinkEdit) obj).isDirected))
		);
	}

	/**
	 * @return A {@code String} with the type of link and the names of
	 * {@code variable1} and {@code variable2}.
	 */
	@Override public String toString() {
		StringBuilder buffer = new StringBuilder(getOperationName() + ": ");
		if (variable1 == null) {
			buffer.append("null");
		} else {
			buffer.append(variable1.getName());
		}
		if (isDirected) {
			buffer.append(" --> ");
		} else {
			buffer.append(" --- ");
		}
		if (variable2 == null) {
			buffer.append("null");
		} else {
			buffer.append(variable2.getName());
		}
		return buffer.toString();
	}

	public abstract String getOperationName();

	/**
	 * Returns the opposite edit. E.g. an AddLinkEdit would return a
	 * RemoveLinkEdit instance
	 *
	 * @return the opposite edit
	 */
	public abstract BaseLinkEdit getUndoEdit();
}
