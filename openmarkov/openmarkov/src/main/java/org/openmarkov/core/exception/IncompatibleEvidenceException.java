/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.exception;

/**
 * @author marias
 * @version 1.0
 */
@SuppressWarnings("serial") public class IncompatibleEvidenceException extends OpenMarkovException {

	// Constructor

	/**
	 * @param message message
	 */
	public IncompatibleEvidenceException(String message) {
		super(message);
	}

}
