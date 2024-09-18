/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */
package org.openmarkov.core.io.database.plugin;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import org.openmarkov.core.io.database.CaseDatabaseReader;
import org.openmarkov.core.io.database.CaseDatabaseWriter;
import org.openmarkov.plugin.PluginLoader;
import org.openmarkov.plugin.service.FilterIF;
import org.openmarkov.plugin.service.PluginLoaderIF;

/**
 * This class is the manager of the case database formats. Detects the class anotated as CaseDatabaseFormat
 * annotations.
 *
 * @author ibermejo
 * @see org.openmarkov.core.io.format.annotation.FormatType
 */
public class CaseDatabaseManager {
	/**
	 * The plugin loader
	 */
	private PluginLoaderIF pluginsLoader;
	/**
	 * The list of case database reader plugins detected in the project
	 */
	private HashMap<String, Class<?>> readerPlugins;
	/**
	 * The list of case database writer plugins detected in the project
	 */
	private HashMap<String, Class<?>> writerPlugins;

	/**
	 * Gets a FormatManager instance
	 */
	public CaseDatabaseManager() {
		super();
		this.pluginsLoader = new PluginLoader();
		this.readerPlugins = new LinkedHashMap<>();
		this.writerPlugins = new LinkedHashMap<>();

		for (Class<?> plugin : findAllFormatPlugins()) {
			CaseDatabaseFormat lAnnotation = plugin.getAnnotation(CaseDatabaseFormat.class);
			if (CaseDatabaseReader.class.isAssignableFrom(plugin)) {
				readerPlugins.put(lAnnotation.extension(), plugin);
			}
			if (CaseDatabaseWriter.class.isAssignableFrom(plugin)) {
				writerPlugins.put(lAnnotation.extension(), plugin);
			}
		}
	}

	/**
	 * This method gets all the plugins with CaseDatabaseFormat annotations
	 *
	 * @return a list with the plugins detected with CaseDatabaseFormat annotations.
	 */
	private List<Class<?>> findAllFormatPlugins() {
		try {
			FilterIF filter = org.openmarkov.plugin.Filter.filter().toBeAnnotatedBy(CaseDatabaseFormat.class);
			return pluginsLoader.loadAllPlugins(filter);
		} catch (Exception e) {
		}
		return null;
	}

	/**
	 * Gets the writer with the extension
	 *
	 * @param extension the extension required
	 * @return a CaseDatabaseWriter object
	 */
	public CaseDatabaseWriter getWriter(String extension) {

		CaseDatabaseWriter instance = null;
		try {
			instance = (CaseDatabaseWriter) writerPlugins.get(extension).newInstance();
		} catch (Exception e) {
		}

		return instance;
	}

	/**
	 * @param extension the extension required
	 * @return a CaseDatabaseReader object
	 */
	public CaseDatabaseReader getReader(String extension) {

		CaseDatabaseReader instance = null;
		try {
			instance = (CaseDatabaseReader) readerPlugins.get(extension).newInstance();
		} catch (Exception e) {
		}

		return instance;
	}

	/**
	 * Returns a HashMap whose keys are extensions accepted by the readers and
	 * whose values are descriptions of the file format read by the reader
	 *
	 * @return a HashMap whose keys are extensions accepted by the readers and
	 * whose values are descriptions of the file format read by the reader
	 */
	public HashMap<String, String> getAllReaders() {
		HashMap<String, String> readersInfo = new HashMap<>();
		for (String extension : readerPlugins.keySet()) {
			String description = readerPlugins.get(extension).getAnnotation(CaseDatabaseFormat.class).name();
			readersInfo.put(extension, description);
		}

		return readersInfo;
	}

	/**
	 * Returns a HashMap whose keys are extensions accepted by the writers and
	 * whose values are descriptions of the file format written by the writer
	 *
	 * @return a HashMap whose keys are extensions accepted by the writers and
	 * whose values are descriptions of the file format written by the writer
	 */
	public HashMap<String, String> getAllWriters() {
		HashMap<String, String> writersInfo = new HashMap<>();
		for (String extension : writerPlugins.keySet()) {
			String description = writerPlugins.get(extension).getAnnotation(CaseDatabaseFormat.class).name();
			writersInfo.put(extension, description);
		}

		return writersInfo;

	}

}
