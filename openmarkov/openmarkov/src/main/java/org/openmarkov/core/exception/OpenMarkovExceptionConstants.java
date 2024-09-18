/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.exception;

public class OpenMarkovExceptionConstants {

	public static final String GenericException = "GenericException";

	public static final String CanNotAccessFileException = "CanNotAccessFileException";

	/**
	 * Unused constant (derived from an unused exception)
	 */
	public static final String CanNotDoEditException = "CanNotDoEditException";

	public static final String CanNotWriteNetworkToFileException = "CanNotWriteNetworkToFileException";

	/**
	 * Invalid State name
	 */
	public static final String InvalidStateNameDuplicatedException = "InvalidStateNameDuplicatedException";
	public static final String InvalidStateNameEmptyException = "InvalidStateNameEmptyException";
	/**
	 * Invalid Variable name
	 */
	public static final String InvalidVariableNameExistingException = "InvalidVariableNameExistingException";
	public static final String InvalidVariableNameExistingTimeSliceException = "InvalidVariableNameExistingTimeSliceException";
	/**
	 * NonProjectableUniformPotential
	 */
	public static final String NonProjectablePotentialException_UniformNumeric = "NonProjectablePotentialException.UniformNumeric";

}
