/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.model.network.constraint;

import java.lang.annotation.AnnotationFormatError;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.openmarkov.core.model.network.constraint.annotation.Constraint;
import org.openmarkov.core.model.network.type.NetworkType;
import org.openmarkov.plugin.PluginLoader;
import org.openmarkov.plugin.service.FilterIF;
import org.openmarkov.plugin.service.PluginLoaderIF;

public class ConstraintManager {
	private static ConstraintManager instance;
	private PluginLoaderIF pluginLoader;
	private HashMap<Class<? extends PNConstraint>, ConstraintBehavior> defaultConstraintBehaviors;

	/**
	 * Constructor for ConstraintManager.
	 */
	@SuppressWarnings("unchecked") private ConstraintManager() {
		super();
		this.pluginLoader = new PluginLoader();
		this.defaultConstraintBehaviors = new HashMap<>();

		List<Class<?>> plugins = findAllConstraints();
		for (Class<?> plugin : plugins) {
			Constraint lAnnotation = plugin.getAnnotation(Constraint.class);
			if (PNConstraint.class.isAssignableFrom(plugin)) {
				defaultConstraintBehaviors.put((Class<? extends PNConstraint>) plugin, lAnnotation.defaultBehavior());
			} else {
				throw new AnnotationFormatError("Constraint annotation must be in a class that extends PNConstraint");
			}
		}
	}

	// Methods

	/**
	 * Singleton pattern.
	 *
	 * @return The unique instance.
	 */
	public static ConstraintManager getUniqueInstance() {
		if (instance == null) {
			instance = new ConstraintManager();
		}
		return instance;
	}

	/**
	 * Generates the minimal (i.e. not including optional constraints)
	 * constraint list given the network type and the Constraints annotated as
	 * such.
	 *
	 * @param includeOptionals If include optional constraints
	 * @param type of the network the list is being generated for.
	 * @return a minimal list of constraint.
	 */
	public ArrayList<PNConstraint> buildConstraintList(NetworkType type, boolean includeOptionals) {
		// Init the list with those constraints that have the default value set to YES
		ArrayList<PNConstraint> constraints = new ArrayList<>();
		for (Class<? extends PNConstraint> constraintClass : defaultConstraintBehaviors.keySet()) {
			if (getDefaultBehavior(constraintClass).equals(ConstraintBehavior.YES) || (
					includeOptionals && getDefaultBehavior(constraintClass).equals(ConstraintBehavior.OPTIONAL)
			)) {
				try {
					constraints.add(constraintClass.newInstance());
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		// Overwrite the list with the constraints specified in the corresponding network type
		HashMap<Class<? extends PNConstraint>, ConstraintBehavior> overwrittenConstraints = type
				.getOverwrittenConstraints();
		for (Class<? extends PNConstraint> constraintClass : overwrittenConstraints.keySet()) {
			if (overwrittenConstraints.get(constraintClass) == ConstraintBehavior.YES) {
				try {
					constraints.add(constraintClass.newInstance());
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else if (overwrittenConstraints.get(constraintClass) == ConstraintBehavior.NO) {
				for (int i = 0; i < constraints.size(); ++i) {
					if (constraints.get(i).getClass().equals(constraintClass)) {
						constraints.remove(i);
					}
				}
			}
		}
		return constraints;

	}

	public final ArrayList<PNConstraint> buildConstraintList(NetworkType type) {
		return buildConstraintList(type, false);
	}

	public ConstraintBehavior getDefaultBehavior(Class<?> constraintClass) {
		return defaultConstraintBehaviors.get(constraintClass);
	}

	public final List<Class<?>> findAllConstraints() {
		try {
			FilterIF filter = org.openmarkov.plugin.Filter.filter().toBeAnnotatedBy(Constraint.class);
			return pluginLoader.loadAllPlugins(filter);
		} catch (Exception e) {
		}
		return null;
	}
}
