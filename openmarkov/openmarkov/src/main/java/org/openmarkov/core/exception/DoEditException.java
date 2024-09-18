/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.exception;

@SuppressWarnings("serial") public class DoEditException extends OpenMarkovException {
	/**
	 * @param msg . {@code String}
	 */
	public DoEditException(String msg) {
		super(msg);
	}

	/**
	 * Writes the {@code exception} message and its stack trace.
	 *
	 * @param exception . {@code Exception}
	 */
	public DoEditException(Exception exception) {
		super(exception.getMessage());
	}

}
