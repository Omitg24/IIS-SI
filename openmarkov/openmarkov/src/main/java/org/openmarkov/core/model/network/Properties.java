/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.model.network;

import java.util.HashMap;
import java.util.Set;

/**
 * This class is a wrapper of a HashMap(key=String,value=String).<p>
 * It gives the possibility to store String attributes accessed by a name (the key).
 */
public class Properties {

	// Attributes
	protected HashMap<String, String> information;

	// Constructor
	public Properties() {
		information = new HashMap<>();
	}

	// Methods

	/**
	 * @param key Key
	 * @return The object stored with {@code key} or {@code null} if
	 * it does not exists.
	 */
	public Object get(String key) {
		return information.get(key);
	}

	/**
	 * @param key   {@code String}
	 * @param value {@code Object}
	 */
	public void put(String key, String value) {
		information.put(key, value);
	}

	/**
	 * @param key Key to be removed
	 * @return The object stored with {@code key} or {@code null} if
	 * it does not exists.
	 */
	public Object remove(String key) {
		return information.remove(key);
	}

	public int size() {
		return information.size();
	}

	/**
	 * @return The set of keys. {@code Set} of {@code String}
	 */
	public Set<String> getKeySet() {
		return information.keySet();
	}

	public HashMap<String, String> getInformation() {
		return information;
	}
}
