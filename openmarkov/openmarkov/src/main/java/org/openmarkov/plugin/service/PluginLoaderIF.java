/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.plugin.service;

import java.util.List;

/**
 * This interface is a contract for plugin loaders.
 * A plugin loader is responsible for find and load plugins.
 *
 * @author jvelez
 * @version 1.0
 * <p>
 * Development Environment        :  Eclipse
 * Name of the File               :  PluginsLoaderIF.java
 * Creation/Modification History  :
 * <p>
 * jvelez     15/09/2011 19:08:10      Created.
 * Gigaesfera CO.
 * (#)PluginsLoaderIF.java 1.0    15/09/2011 19:08:10
 */
public interface PluginLoaderIF {
	/**
	 * Returns a plugin from the system environment.
	 *
	 * @param name The qualified name of the plugin class
	 * @return a plugin from the system environment.
	 */
    Class<?> loadPlugin(String name) throws PluginException;

	/**
	 * Returns all plugins from the system environment.
	 *
	 * @param filter the plugins filter to select plugins.
	 * @return all plugins from the system environment.
	 */
    List<Class<?>> loadAllPlugins(FilterIF filter) throws PluginException;

	/**
	 * Returns all plugins from the system environment.
	 *
	 * @return all plugins from the system environment.
	 */
    List<Class<?>> loadAllPlugins() throws PluginException;
}
