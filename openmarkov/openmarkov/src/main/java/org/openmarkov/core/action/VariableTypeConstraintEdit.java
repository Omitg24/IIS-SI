/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.action;

import java.util.List;

import org.openmarkov.core.exception.ConstraintViolationException;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.constraint.OnlyContinuousVariables;
import org.openmarkov.core.model.network.constraint.OnlyDiscreteVariables;
import org.openmarkov.core.model.network.constraint.PNConstraint;

/**
 * This class Edit changes the variable type constraint of a ProbNet object.
 *
 * @author mpalacios based
 * @version 1.0
 */

@SuppressWarnings("serial") public class VariableTypeConstraintEdit extends SimplePNEdit {
	// Attributes
	/**
	 * The new constraint for variable type
	 */
	private PNConstraint newVariableTypeConstraint;
	/**
	 * The last constraint before the edit
	 */
	private PNConstraint lastConstraint;

	// Constructor

	/**
	 * This method creates a new VariableTypeConstraintEdit
	 *
	 * @param probNet                   the network that will be edited
	 *                                  {@code ProbNet}
	 * @param newVariableTypeConstraint the new constraint. If null, the network
	 *                                  will do not have constraint about variables, i.e, works with continuous
	 *                                  and discrete variables.
	 */
	public VariableTypeConstraintEdit(ProbNet probNet, PNConstraint newVariableTypeConstraint) {
		super(probNet);
		this.newVariableTypeConstraint = newVariableTypeConstraint;
		List<PNConstraint> constraints = probNet.getConstraints();
		for (PNConstraint constraint : constraints) {
			if (constraint instanceof OnlyDiscreteVariables || constraint instanceof OnlyContinuousVariables) {
				lastConstraint = constraint;
				break;
			}
		}

	}

	// Methods
	@Override public void doEdit() {

		if (lastConstraint != null) {
			probNet.removeConstraint(lastConstraint);
		}

		if (newVariableTypeConstraint != null) {
			try {
				probNet.addConstraint(newVariableTypeConstraint, false);
			} catch (ConstraintViolationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	public void undo() {
		super.undo();

		if (newVariableTypeConstraint != null) {
			probNet.removeConstraint(newVariableTypeConstraint);
		}

		if (lastConstraint != null) {
			try {
				probNet.addConstraint(lastConstraint, false);
			} catch (ConstraintViolationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

}
