/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */
package org.openmarkov.core.model.network.modelUncertainty;

import java.lang.annotation.AnnotationFormatError;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openmarkov.plugin.PluginLoader;
import org.openmarkov.plugin.service.FilterIF;
import org.openmarkov.plugin.service.PluginLoaderIF;

public class ProbDensFunctionManager {

	private static ProbDensFunctionManager instance;
	private PluginLoaderIF pluginLoader;
	private Map<String, Class<?>> probDensFunctions;

	//CMI
	//For Univariate
	private Map<String, List<String[]>> probDensParametrizations;
	//CMF

	/**
	 * Constructor for ProbDensFunctionManager.
	 */
	private ProbDensFunctionManager() {
		super();
		this.pluginLoader = new PluginLoader();
		this.probDensFunctions = new HashMap<>();
		//CMI
		//For Univariate
		this.probDensParametrizations = new HashMap<>();
		//CMF

		List<Class<?>> plugins = findAllProbDensFunctions();
		for (Class<?> plugin : plugins) {
			ProbDensFunctionType annotation = plugin.getAnnotation(ProbDensFunctionType.class);
			if (ProbDensFunction.class.isAssignableFrom(plugin)) {
				probDensFunctions.put(annotation.name(), plugin);
				//CMI
				//For Univariate
				String univariateName = annotation.univariateName();
				String name = annotation.name();
				if (univariateName.equals("default"))
					univariateName = name;
				String[] parametersNames = annotation.parameters();
				String parametersNamesConcat = parametersNames[0];
				for (int i = 1; i < parametersNames.length; i++) {
					parametersNamesConcat += ", " + parametersNames[i];
				}
				String[] parametrizationData = new String[] { parametersNamesConcat, name };
				if (probDensParametrizations.containsKey(univariateName)) {
					List<String[]> parametersList = probDensParametrizations.get(univariateName);
					parametersList.add(parametrizationData);
				} else {
					List<String[]> parametersList = new ArrayList<String[]>();
					parametersList.add(parametrizationData);
					probDensParametrizations.put(univariateName, parametersList);
				}
				//CMF
			} else {
				throw new AnnotationFormatError(
						"ProbDensFunctionType annotation must be in a class that extends ProbDensFunction");
			}
		}
	}

	// Methods

	/**
	 * Singleton pattern.
	 *
	 * @return The unique instance.
	 */
	public static ProbDensFunctionManager getUniqueInstance() {
		if (instance == null) {
			instance = new ProbDensFunctionManager();
		}
		return instance;
	}

	//CMI
	//For Univariate
	public List<String> getValidProbDensFunctions() {
		List<String> validFunctions = new ArrayList<>();
		for (String functionName : probDensFunctions.keySet()) {
			validFunctions.add(functionName);
		}
		return validFunctions;
	}

	public List<String> getDistributions() {
		List<String> distributions = new ArrayList<>();
		for (String distributionUnivariateName : probDensParametrizations.keySet()) {
			distributions.add(distributionUnivariateName);
		}
		return distributions;

	}

	public List<String[]> getParametrizations(String univariateName) {
		return probDensParametrizations.get(univariateName);
	}

	public String getDistributionName(String univariateName, String parametrization) {
		String name = "";
		List<String[]> parametrizationList = probDensParametrizations.get(univariateName);
		for (String[] p : parametrizationList) {
			if (p[0].equals(parametrization)) {
				name = p[1];
				break;
			}
		}
		return name;
	}

	/**
	 * @param functionName Name of the function
	 * @return The class for that function name
	 */
	@SuppressWarnings("unchecked") public Class<? extends ProbDensFunction> getProbDensFunctionClass(
			String functionName) {
		Class<? extends ProbDensFunction> probDensFunctionClass = null;
		try {
			probDensFunctionClass = (Class<? extends ProbDensFunction>) probDensFunctions.get(functionName);
		} catch (ClassCastException e) {
			//TODO
		}
		return probDensFunctionClass;
	}

	public Class<? extends ProbDensFunction> getProbDensFunctionClass(String univariateName, String parametrization) {

		return getProbDensFunctionClass(getDistributionName(univariateName, parametrization));
	}

	//CMF

	public List<String> getValidProbDensFunctions(boolean isChance) {
		List<String> validFunctions = new ArrayList<>();
		for (String functionName : probDensFunctions.keySet()) {
			Class<?> functionClass = probDensFunctions.get(functionName);
			ProbDensFunctionType annotation = functionClass.getAnnotation(ProbDensFunctionType.class);
			if ((annotation.isValidForNumeric() && !isChance) || (annotation.isValidForProbabilities() && isChance)) {
				validFunctions.add(functionName);
			}
		}
		return validFunctions;
	}

	public String[] getParameters(String functionName) {
		Class<?> functionClass = probDensFunctions.get(functionName);
		ProbDensFunctionType annotation = functionClass.getAnnotation(ProbDensFunctionType.class);
		return annotation.parameters();
	}

	public ProbDensFunction newInstance(String functionName, double[] parameters) {
		Class<?> probDensFunctionClass = probDensFunctions.get(functionName);
		ProbDensFunction newInstance = null;
		try {
			newInstance = (ProbDensFunction) probDensFunctionClass.newInstance();
			newInstance.setParameters(parameters);
		} catch (InstantiationException | IllegalAccessException e) {
			e.printStackTrace();
		}
		return newInstance;
	}

	private List<Class<?>> findAllProbDensFunctions() {
		try {
			FilterIF filter = org.openmarkov.plugin.Filter.filter().toBeAnnotatedBy(ProbDensFunctionType.class);
			return pluginLoader.loadAllPlugins(filter);
		} catch (Exception e) {
		}
		return null;
	}

}
