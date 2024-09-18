/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.inference.annotation;

import java.lang.annotation.AnnotationFormatError;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.openmarkov.core.exception.NotEvaluableNetworkException;
import org.openmarkov.core.inference.InferenceAlgorithm;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.type.BayesianNetworkType;
import org.openmarkov.core.model.network.type.DecisionAnalysisNetworkType;
import org.openmarkov.core.model.network.type.InfluenceDiagramType;
import org.openmarkov.core.model.network.type.TuningNetworkType;
import org.openmarkov.plugin.PluginLoader;
import org.openmarkov.plugin.service.FilterIF;
import org.openmarkov.plugin.service.PluginLoaderIF;

/**
 * This class is the manager of the inference annotations. Detects the plugins
 * with InferenceAnnotation annotations.
 *
 * @author mpalacios
 * @author myebra
 * @author ibermejo
 * @see InferenceAnnotation
 */
public class InferenceManager {
	/**
	 * The plugin loader
	 */
	private PluginLoaderIF pluginsLoader;
	/**
	 * The list of plugins detected in the project
	 */
	private HashMap<String, Class<? extends InferenceAlgorithm>> inferenceAlgorithms;

	/**
	 * Constructor for InferenceManager.
	 */
	@SuppressWarnings("unchecked") public InferenceManager() {
		super();
		this.pluginsLoader = new PluginLoader();
		this.inferenceAlgorithms = new HashMap<>();
		for (Class<?> InferenceAlgorithmClass : findAllInferencePlugins()) {
			InferenceAnnotation lAnnotation = InferenceAlgorithmClass.getAnnotation(InferenceAnnotation.class);
			if (InferenceAlgorithm.class.isAssignableFrom(InferenceAlgorithmClass)) {
				inferenceAlgorithms
						.put(lAnnotation.name(), (Class<? extends InferenceAlgorithm>) InferenceAlgorithmClass);
			} else {
				throw new AnnotationFormatError(
						"InferenceType annotation must be in a class that extends InferenceAlgorithm");
			}
		}
	}

	/**
	 * Returns the list of the names of the algorithms that can evaluate the
	 * given instance of ProbNet
	 *
	 * @param probNet Network
	 * @return the list of the names of the algorithms that can evaluate the network
	 */
	public List<String> getInferenceAlgorithmNames(ProbNet probNet) {
		List<String> inferenceAlgorithmNames = new ArrayList<>();
		for (String algorithmName : inferenceAlgorithms.keySet()) {
			Constructor<? extends InferenceAlgorithm> constructor = null;
			try {
				constructor = inferenceAlgorithms.get(algorithmName).getConstructor(ProbNet.class);
			} catch (SecurityException e1) {
				e1.printStackTrace();
			} catch (NoSuchMethodException e1) {
				e1.printStackTrace();
			}
			if (constructor != null) {
				try {
					constructor.newInstance(probNet);
					inferenceAlgorithmNames.add(algorithmName);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		return inferenceAlgorithmNames;
	}

	/**
	 * Returns the list of the names of the algorithms that can evaluate the
	 * given instance of ProbNet
	 *
	 * @param probNet Network
	 * @return the list of the names of the algorithms that can evaluate the network
	 */
	public List<InferenceAlgorithm> getInferenceAlgorithms(ProbNet probNet) {
		List<InferenceAlgorithm> inferenceAlgorithms = new ArrayList<>();
		for (String algorithmName : this.inferenceAlgorithms.keySet()) {
			Constructor<? extends InferenceAlgorithm> constructor = null;
			Method checkEval = null;
			boolean isEvaluable = true;
			try {
				Class<? extends InferenceAlgorithm> inferenceAlgorithmClass = this.inferenceAlgorithms
						.get(algorithmName);
				constructor = this.inferenceAlgorithms.get(algorithmName).getConstructor(ProbNet.class);
				checkEval = inferenceAlgorithmClass.getMethod("checkEvaluability", ProbNet.class);
				try {
					checkEval.invoke(inferenceAlgorithmClass, probNet);
				} catch (InvocationTargetException e) {
					isEvaluable = e.getTargetException().getClass() != NotEvaluableNetworkException.class;
				}
			} catch (SecurityException | NoSuchMethodException | IllegalAccessException | IllegalArgumentException e1) {
				e1.printStackTrace();
			}
			if (constructor != null && isEvaluable) {
				try {
					InferenceAlgorithm inferenceAlgorithm = constructor.newInstance(probNet);
					inferenceAlgorithms.add(inferenceAlgorithm);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		return inferenceAlgorithms;
	}

	/**
	 * Returns an instance of the algorithm whose names we receive as a
	 * parameter, given the ProbNet
	 *
	 * @param algorithmName Algorithm name
	 * @param probNet Network
	 * @return an instance of the algorithm whose names we receive as a parameter
	 * @throws NotEvaluableNetworkException NotEvaluableNetworkException
	 * @throws NoSuchMethodException NoSuchMethodException
	 */
	public InferenceAlgorithm getInferenceAlgorithmByName(String algorithmName, ProbNet probNet)
			throws NotEvaluableNetworkException, NoSuchMethodException {
		InferenceAlgorithm instance = null;
		Constructor<? extends InferenceAlgorithm> constructor = null;
		Class<? extends InferenceAlgorithm> inferenceAlgorithmClass = inferenceAlgorithms.get(algorithmName);
		Method checkEval = null;
		try {
			constructor = inferenceAlgorithmClass.getConstructor(ProbNet.class);
			checkEval = inferenceAlgorithmClass.getMethod("checkEvaluability", ProbNet.class);
		} catch (SecurityException e1) {
			e1.printStackTrace();
		}
		if (constructor != null) {
			try {
				checkEval.invoke(inferenceAlgorithms.get(algorithmName), probNet);
			} catch (InvocationTargetException e) {
				Throwable targetExcep = e.getTargetException();
				if (targetExcep.getClass() == NotEvaluableNetworkException.class) {
					throw (NotEvaluableNetworkException) targetExcep;
				}
			} catch (IllegalAccessException | IllegalArgumentException e) {
				e.printStackTrace();
			}
			try {
				instance = constructor.newInstance(probNet);
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				e.printStackTrace();
			}
		}
		return instance;
	}

	/**
	 * Returns an instance of the default algorithm given the ProbNet
	 *
	 * @param probNet Network
	 * @return an instance of the default algorithm
	 * @throws NotEvaluableNetworkException NotEvaluableNetworkException
	 */
	public InferenceAlgorithm getDefaultInferenceAlgorithm(ProbNet probNet) throws NotEvaluableNetworkException {
		InferenceAlgorithm defaultAlgorithm = null;
		try {
			if (probNet.getNetworkType().equals(BayesianNetworkType.getUniqueInstance())) {
				defaultAlgorithm = getInferenceAlgorithmByName("VariableElimination", probNet);
			} else if (probNet.getNetworkType().equals(InfluenceDiagramType.getUniqueInstance())) {
				defaultAlgorithm = getInferenceAlgorithmByName("VariableElimination", probNet);
			} else if (probNet.getNetworkType().equals(TuningNetworkType.getUniqueInstance())) {
				defaultAlgorithm = getInferenceAlgorithmByName("VariableElimination", probNet);
			} else if (probNet.getNetworkType().equals(DecisionAnalysisNetworkType.getUniqueInstance())) {
				defaultAlgorithm = getInferenceAlgorithmByName("DSD", probNet);
			} else {
				List<InferenceAlgorithm> possibleAlgorithms = getInferenceAlgorithms(probNet);
				if (!possibleAlgorithms.isEmpty()) {
					defaultAlgorithm = possibleAlgorithms.get(0); // Get the first
				}
			}
		} catch (SecurityException | NoSuchMethodException e) {
			// This should not be the case as we are hard coding to an algorithm
			// that should have a public constructor
			e.printStackTrace();
		} catch (NotEvaluableNetworkException e) {
			throw e;
		}
		return defaultAlgorithm;
	}

	/**
	 * Returns an instance of the default approximate algorithm given the
	 * ProbNet
	 *
	 * @param probNet Network
	 * @return An instance of the default approximate algorithm
	 * @throws NotEvaluableNetworkException NotEvaluableNetworkException
	 */
	public InferenceAlgorithm getDefaultApproximateAlgorithm(ProbNet probNet) throws NotEvaluableNetworkException {
		InferenceAlgorithm defaultAlgorithm = null;
		try {
			defaultAlgorithm = getInferenceAlgorithmByName("LikelihoodWeighting", probNet);
		} catch (SecurityException | NoSuchMethodException e) {
			// This should not be the case as we are hard coding to an algorithm
			// that should have a public constructor
			e.printStackTrace();
		}
		return defaultAlgorithm;
	}

	/**
	 * This method gets all the plugins with InferenceType annotations
	 *
	 * @return a list with the plugins detected with InferenceType annotations.
	 */
	private List<Class<?>> findAllInferencePlugins() {
		try {
			FilterIF filter = org.openmarkov.plugin.Filter.filter().toBeAnnotatedBy(InferenceAnnotation.class);
			return pluginsLoader.loadAllPlugins(filter);
		} catch (Exception e) {
		}
		return null;
	}
}
