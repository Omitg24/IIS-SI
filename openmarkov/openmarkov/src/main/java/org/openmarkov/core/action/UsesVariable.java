/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.action;

import org.openmarkov.core.model.network.Variable;

/**
 * Declares a method {@code getVariable} used in edits that manages one
 * single variable.
 */
public interface UsesVariable {

	/**
	 * @return A {@code Variable}
	 */
	Variable getVariable();

}
