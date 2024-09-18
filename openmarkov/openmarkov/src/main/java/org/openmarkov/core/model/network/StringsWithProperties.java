/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.model.network;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Set;

/**
 * @author marias
 */
public class StringsWithProperties {

	// Attributes
	private LinkedHashMap<String, Properties> stringsWithProperties;

	// Constructors
	public StringsWithProperties() {
		stringsWithProperties = new LinkedHashMap<>();
	}

	/**
	 * Constructor that receives a collection of strings (without properties)
	 *
	 * @param strings {@code Collection} of {@code String}s
	 */
	public StringsWithProperties(Collection<String> strings) {
		stringsWithProperties = new LinkedHashMap<>();
		for (String string : strings) {
			stringsWithProperties.put(string, null);
		}
	}

	/**
	 * Constructor based in a previous LinkedHashMap
	 *
	 * @param stringsWithProperties LinkedHashMap with key type = String and
	 *                              value type = Properties
	 */
	public StringsWithProperties(LinkedHashMap<String, Properties> stringsWithProperties) {
		LinkedHashMap<String, Properties> stringsWithPropertiesCopied = new LinkedHashMap<>();
		Set<String> keys = stringsWithProperties.keySet();
		for (String key : keys) {
			stringsWithPropertiesCopied.put(key, stringsWithProperties.get(key));
		}
		this.stringsWithProperties = stringsWithPropertiesCopied;
	}

	// Methods

	/**
	 * @param propertyName Property name
	 * @param string String property
	 * @return A string property if it exists or {@code null} otherwise.
	 */
	public Object get(String string, String propertyName) {
		Object propertyValue = null;
		Properties properties = stringsWithProperties.get(string);
		if (properties != null) {
			propertyValue = properties.get(propertyName);
		}
		return propertyValue;
	}

	/**
	 * @return All the strings in a {@code Set} of {@code String}.
	 */
	public Set<String> getNames() {
		return stringsWithProperties.keySet();
	}

	/**
	 * @param string {@code String}
	 * @return All the properties of a given {@code String}, or {@code null}
	 * if the string does not exists. {@code Properties}.
	 */
	public Properties getProperties(String string) {
		return stringsWithProperties.get(string);
	}

	/**
	 * @param key {@code String}
	 */
	public void put(String key) {
		Properties properties = stringsWithProperties.get(key);
		if (properties == null) {
			properties = new Properties();
			stringsWithProperties.put(key, null);// ¿? (key, properties)
			//added ¿?
		} else {
			stringsWithProperties.put(key, properties);
		}

	}

	/**
	 * @param key           {@code String}
	 * @param propertyName  {@code String}
	 * @param propertyValue {@code String}
	 */
	public void put(String key, String propertyName, String propertyValue) {
		Properties properties = stringsWithProperties.get(key);
		if (properties == null) {
			properties = new Properties();
			stringsWithProperties.put(key, properties);
		}
		properties.put(propertyName, propertyValue);
	}

	/**
	 * @param key        {@code String}
	 * @param properties {@code Properties}
	 */
	public void put(String key, Properties properties) {
		if (properties == null) {
			properties = new Properties();
			stringsWithProperties.put(key, properties);
		} else {
			stringsWithProperties.put(key, properties);
		}
	}

	/**
	 * Removes they key and all its properties.
	 *
	 * @param key {@code String}
	 */
	public void remove(String key) {
		stringsWithProperties.remove(key);
		/*Properties properties = stringsWithProperties.get(key);
		if (properties != null) {
			stringsWithProperties.remove(key);
		}*/
	}

	/**
	 * @param key          {@code String}
	 * @param propertyName {@code String}
	 * @return The object stored with {@code key} or {@code null} if
	 * it does not exists.
	 */
	public Object remove(String key, String propertyName) {
		Object removedObject = null;
		Properties properties = stringsWithProperties.get(key);
		if (properties != null) {
			removedObject = properties.remove(propertyName);
		}
		return removedObject;
	}

	/**
	 * Renames the key entry
	 *
	 * @param key    {@code String}
	 * @param newKey {@code String}
	 */
	public void rename(String key, String newKey) {
		Properties properties = stringsWithProperties.get(key);
		stringsWithProperties.remove(key);
		if (properties == null) {
			stringsWithProperties.put(newKey, null);
		} else {
			stringsWithProperties.put(newKey, properties);

		}
	}

	/**
	 * @return {@code boolean}
	 */
	public boolean isEmpty() {
		return stringsWithProperties.isEmpty();
	}

	/**
	 * @return {@code StringsWithProperties}
	 */
	public StringsWithProperties copy() {
		return new StringsWithProperties(stringsWithProperties);
	}

	/**
	 * @return {@code String}
	 */
	public String toString() {
		StringBuilder outString = new StringBuilder();
		Set<String> strings = stringsWithProperties.keySet();
		for (String stringWithProperties : strings) {
			outString.append(stringWithProperties);
			Properties properties = stringsWithProperties.get(stringWithProperties);
			if (properties != null && properties.size() > 0) {
				outString.append(":\n");
				Set<String> keysProperties = properties.getKeySet();
				for (String keyProperty : keysProperties) {
					outString.append("    " + properties.get(keyProperty) + "\n");
				}
			}
			outString.append("\n");
		}
		return outString.toString();
	}

}
