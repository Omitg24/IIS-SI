/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.exception;

import java.util.List;

import org.openmarkov.core.model.network.constraint.PNConstraint;

/**
 * Thrown when propagation cannot be done.
 */
@SuppressWarnings("serial") public class NoPropagationCanBeDoneException extends OpenMarkovException {

	private List<PNConstraint> constraints;

	public NoPropagationCanBeDoneException(List<PNConstraint> constraints) {
		this.constraints = constraints;
	}

	/**
	 * Returns the constraints.
	 *
	 * @return the constraints.
	 */
	public List<PNConstraint> getConstraints() {
		return constraints;
	}

}
