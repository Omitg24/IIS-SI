/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.exception;
@SuppressWarnings("serial") public class CanNotAccessFileException extends OpenMarkovException {

	// Constructor

	/**
	 * @param fileName string with the filename
	 */
	public CanNotAccessFileException(String fileName) {
		super("It is not possible to access file: " + fileName);
	}

}
