/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.io.format.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This class sets the labels for the annotations format
 * @author mpalacios
 * @author carmenyago -- added the annotation type element {@code version}
 */
@Retention(RetentionPolicy.RUNTIME) @Target(ElementType.TYPE) public @interface FormatType {
	/**
	 * Gets the name of the class
	 * @return The name of the format
	 */
	String name();

	//CMI

	/**
	 * Gets the version of the class. Sometimes as in the case of the PGMXReader/Writer
	 * there are several classes representing several versions of something as, for example,
	 * the ProbModelXML
	 * @return The version of the format
	 */
	String version();
	//CMF

	/**
	 * Gets the file extension that write/read the class
	 * @return The format extension
	 */
	String extension();

	/**
	 * Gets the file description that writes/reads the class.
	 * It will be used as a string id for the file description to be shown in the GUI.
	 * The string id will be built using the following pattern: {@code "FileExtension." + description + ".Description"}
	 * @return The format description
	 */
	String description();

	/**
	 * Gets the role of the class.
	 * @return the role of the class. "Writer" if implements the ProbNetWriter interface, "Reader" if implements the ProbNetReader interface.
	 */
	String role();
}
