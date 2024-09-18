/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.plugin.service;

/**
 * This class represents a throwable exception related to plugins.
 *
 * @author jvelez
 * @version 1.0
 * <p>
 * Development Environment        :  Eclipse
 * Name of the File               :  PluginExeption.java
 * Creation/Modification History  :
 * <p>
 * jvelez      15/09/2011 19:08:10      Created.
 * Gigaesfera  CO.
 * (#)PluginExeption.java 1.0    15/09/2011		19:08:10
 */

public class PluginException extends Exception {
	private static final long serialVersionUID = -9088696846391248107L;

	/**
	 * Constructor for PluginException.
	 */
	public PluginException() {
		super();
	}

	/**
	 * Constructor for PluginException.
	 *
	 * @param message The message.
	 * @param cause   The cause.
	 */
	public PluginException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Constructor for PluginException.
	 *
	 * @param message The message.
	 */
	public PluginException(String message) {
		super(message);
	}

	/**
	 * Constructor for PluginException.
	 *
	 * @param cause The cause.
	 */
	public PluginException(Throwable cause) {
		super(cause);
	}
}
