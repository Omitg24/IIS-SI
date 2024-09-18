/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.exception;

/**
 * Thrown when the {@code Potential} cannot be projected into a set of
 * {@code TablePotential}s given the evidence supplied.
 */
@SuppressWarnings("serial") public class NonProjectablePotentialException extends OpenMarkovException {

	public NonProjectablePotentialException(String token, String... attributes) {
		super(token, attributes);
	}

	public NonProjectablePotentialException(String string) {
		super(string);
	}

	public NonProjectablePotentialException(String string, Throwable cause) {
		super(string, cause);
	}

}
