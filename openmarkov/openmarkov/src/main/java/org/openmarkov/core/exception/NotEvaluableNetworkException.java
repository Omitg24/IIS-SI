/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.exception;

public class NotEvaluableNetworkException extends OpenMarkovException {

	private static final long serialVersionUID = -6555375975623328551L;

	// Constructor

	/**
	 * @param e {@code Exception}
	 */
	public NotEvaluableNetworkException(Exception e) {
		super(e.getMessage());
	}

	public NotEvaluableNetworkException(String message) {
		super(message);
	}

}
