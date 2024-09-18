/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.model.network.type.plugin;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openmarkov.core.model.network.type.NetworkType;
import org.openmarkov.plugin.PluginLoader;
import org.openmarkov.plugin.service.FilterIF;
import org.openmarkov.plugin.service.PluginLoaderIF;

/**
 * @author Iñigo
 */
public class NetworkTypeManager {
	private Map<String, Class<? extends NetworkType>> networkTypeClasses = null;

	public NetworkType getNetworkType(String name) {
		if (networkTypeClasses == null) {
			networkTypeClasses = getNetworkTypesMap();
		}
		NetworkType instance = null;
		if (networkTypeClasses.containsKey(name)) {
			Class<? extends NetworkType> networkTypeClass = networkTypeClasses.get(name);
			try {
				instance = (NetworkType) networkTypeClass.getMethod("getUniqueInstance").invoke(this);
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
				e.printStackTrace();
			}
		}
		return instance;
	}

	/**
	 * Gets the set of all defined network types
	 *
	 * @return The set of all defined network types
	 */
	public Set<String> getNetworkTypeNames() {
		if (networkTypeClasses == null) {
			networkTypeClasses = getNetworkTypesMap();
		}
		return networkTypeClasses.keySet();
	}

	/**
	 * Returns the name of the given network type
	 *
	 * @param networkType Network type
	 * @return The name of the given network type
	 */
	public String getName(NetworkType networkType) {
		return networkType.getClass().getAnnotation(ProbNetType.class).name();
	}

	/**
	 * Builds the network type map looking for classes annotated as ProbNetType
	 *
	 * @return The network type map
	 */
	@SuppressWarnings("unchecked") private Map<String, Class<? extends NetworkType>> getNetworkTypesMap() {
		List<Class<?>> networkTypes = findAllNetworkTypes();
		Map<String, Class<? extends NetworkType>> networkTypeClasses = new HashMap<>();

		for (Class<?> networkTypeClass : networkTypes) {
			if (NetworkType.class.isAssignableFrom(networkTypeClass)) {
				ProbNetType lAnnotation = networkTypeClass.getAnnotation(ProbNetType.class);
				networkTypeClasses.put(lAnnotation.name(), (Class<? extends NetworkType>) networkTypeClass);
				String[] alternativeNames = lAnnotation.alternativeNames();
				for (int i = 0; i < alternativeNames.length; ++i) {
					if (!alternativeNames[i].isEmpty()) {
						networkTypeClasses.put(alternativeNames[i], (Class<? extends NetworkType>) networkTypeClass);
					}
				}
			}
		}
		return networkTypeClasses;
	}

	/**
	 * This method gets all the plugins with ProbNetType annotations
	 *
	 * @return a list with the plugins detected with ProbNetType annotations.
	 */
	private List<Class<?>> findAllNetworkTypes() {
		PluginLoaderIF pluginsLoader = new PluginLoader();
		try {
			FilterIF filter = org.openmarkov.plugin.Filter.filter().toBeAnnotatedBy(ProbNetType.class);
			return pluginsLoader.loadAllPlugins(filter);
		} catch (Exception e) {
			//ignore
		}
		return null;
	}
}