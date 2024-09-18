/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.plugin.service;

/**
 * This interface represents a contract for plugin filters.
 * A plugin filter is used to characterize a plugin by its internal structure
 * in terms of a set of constrains related to extended classes, implemented
 * interfaces and present annotations. Once defined the plugin can be used
 * to validate whether a class fulfills that constrains.
 *
 * @author jvelez
 * @version 1.0
 * <p>
 * Development Environment        :  Eclipse
 * Name of the File               :  FilterIF.java
 * Creation/Modification History  :
 * <p>
 * jvelez     15/09/2011 19:08:10      Created.
 * Gigaesfera CO.
 * (#)FilterIF.java 1.0    15/09/2011 19:08:10
 */
public interface FilterIF {
	/**
	 * Checks whether a class is a valid plugin.
	 *
	 * @param aClass the class to validate.
	 * @return true if the class is a valid plugin.
	 */
    boolean checkPlugin(Class<?> aClass);
}
