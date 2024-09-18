/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.exception;

@SuppressWarnings("serial") public class ConflictNameException extends OpenMarkovException {

	public ConflictNameException(String msg) {
		super(msg);
	}
}
