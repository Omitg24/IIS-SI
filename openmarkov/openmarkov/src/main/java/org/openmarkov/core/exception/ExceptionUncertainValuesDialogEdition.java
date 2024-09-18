/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.exception;

import javax.swing.JOptionPane;
public class ExceptionUncertainValuesDialogEdition extends OpenMarkovException {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	public ExceptionUncertainValuesDialogEdition(String message) {
		JOptionPane.showMessageDialog(null, message, "Error", JOptionPane.ERROR_MESSAGE);

	}

}
