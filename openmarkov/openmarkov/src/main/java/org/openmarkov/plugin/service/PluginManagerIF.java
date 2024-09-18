/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.plugin.service;

/**
 * This interface is a contract for plugin managers.
 * A plugin manager manages a set of plugins.
 *
 * @author jvelez
 * @version 1.0
 * <p>
 * Development Environment        :  Eclipse
 * Name of the File               :  PluginsManagerIF.java
 * Creation/Modification History  :
 * <p>
 * jvelez     15/09/2011 19:08:10      Created.
 * Gigaesfera CO.
 * (#)PluginsManagerIF.java 1.0    15/09/2011 19:08:10
 */
public interface PluginManagerIF {
	/**
	 * Adds a new plugin to the manager.
	 *
	 * @param plugin the plugin class to add.
	 */
    void addPlugin(Class<?> plugin);

	/**
	 * Removes a plugin from the manager.
	 *
	 * @param plugin the plugin class to remove.
	 */
    void removePlugin(Class<?> plugin);

	/**
	 * Removes all plugins from the manager.
	 */
    void clearPlugins();

	/**
	 * Indicates whether a plugin is contained.
	 *
	 * @param plugin The plugin class to find.
	 * @return true if the plugin is contained.
	 */
    boolean containsPlugin(Class<?> plugin);
}
