/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.model.network;

/**
 * This class contains a string and its properties, those properties are stored in a HashMap(key=string, value=string)
 * @author Manuel Arias
 */
public class StringWithProperties {

	// Attributes
	public String string;

	private Properties properties;

	// Constructor

	/**
	 * @param string {@code String}
	 */
	public StringWithProperties(String string) {
		this.string = string;
		properties = new Properties();
	}

	// Methods

	/**
	 * @param key Key
	 * @return The object stored with {@code key} or {@code null} if
	 * it does not exists. {@code Object}
	 */
	public Object get(String key) {
		return properties.get(string);
	}

	/**
	 * @param key   {@code String}
	 * @param value {@code Object}
	 */
	public void put(String key, String value) {
		properties.put(key, value);
	}

	/**
	 * @param properties {@code Properties}
	 */
	public void put(Properties properties) {
		this.properties = properties;
	}

	/**
	 * @param key {@code String}
	 * @return The object stored with {@code key} or {@code null} if
	 * it does not exists. {@code Object}
	 */
	public Object remove(String key) {
		return properties.remove(key);
	}

	public String getString() {
		return string;
	}

	public Properties getAdditionalProperties() {
		return properties;
	}

	public StringWithProperties copy() {
		StringWithProperties copiedStringWithProperties = new StringWithProperties(string);
		copiedStringWithProperties.put(properties);
		return copiedStringWithProperties;
	}
}
