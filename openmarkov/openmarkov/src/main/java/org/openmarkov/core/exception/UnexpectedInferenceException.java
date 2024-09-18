/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */
package org.openmarkov.core.exception;

@SuppressWarnings("serial") public class UnexpectedInferenceException extends OpenMarkovException {

	// Constructor

	/**
	 * @param message Message
	 */
	public UnexpectedInferenceException(String message) {
		super(message);
	}

	/**
	 * @param message Message
	 * @param cause Cause
	 */
	public UnexpectedInferenceException(String message, Throwable cause) {
		super(message, cause);
	}

}
