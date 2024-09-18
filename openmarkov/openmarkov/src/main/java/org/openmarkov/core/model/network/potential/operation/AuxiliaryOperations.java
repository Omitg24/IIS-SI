/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.model.network.potential.operation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.VariableType;
import org.openmarkov.core.model.network.potential.Potential;
import org.openmarkov.core.model.network.potential.TablePotential;

/**
 * Auxiliary methods for {@code DiscretePotentialOperations} and
 * {@code PotentialOperations} classes
 *
 * @author Manuel Arias
 */
public class AuxiliaryOperations {

	// Constructor

	/**
	 * Don't let anyone instantiate this class.
	 */
	private AuxiliaryOperations() {
	}

	// Methods

	/**
	 * A proper potential is a potential that is not a constant; as consequence
	 * it has a not empty set of variables and a table with more than one value.
	 *
	 * @param potentials An {@code ArrayList} of
	 *                   {@code TablePotential}s
	 * @return An {@code ArrayList} of {@code TablePotential}s without
	 * the constant potentials.
	 */
	public static List<TablePotential> getNonConstantPotentials(Collection<TablePotential> potentials) {
		List<TablePotential> properPotentials = new ArrayList<>();
		for (TablePotential potential : potentials) {
			if (potential.values.length > 1) {
				properPotentials.add(potential);
			}
		}
		return properPotentials;
	}

	/**
	 * @param collection  A generic {@code Collection}
	 * @param classObject Class object (i.e. {@code TablePotential.class})
	 * @return {@code true} if all the objects in the parameter
	 * {@code collection} belongs to the parameter type
	 * {@code classObject}.
	 */
	public static boolean checkObjectsCollectionType(Collection<?> collection, Class<?> classObject) {
		for (Object object : collection) {
			if (object.getClass() != classObject) {
				return false;
			}
		}
		return true;
	}

	/**
	 * @param collection   A {@code Collection} of {@code Variable}
	 * @param variableType {@code VariableType})
	 * @return {@code true} if all the variables in the parameter
	 * {@code collection} belongs to {@code variableType}.
	 */
	public static boolean checkVariablesCollectionType(Collection<Variable> collection, VariableType variableType) {
		for (Variable variable : collection) {
			if (variable.getVariableType() != variableType) {
				return false;
			}
		}
		return true;
	}

	/**
	 * @param potentials {@code Collection} of {@code Potential}s
	 * @return {@code ArrayList} of {@code Variable}s.
	 */
	public static List<Variable> getUnionVariables(Collection<? extends Potential> potentials) {

		Set<Variable> variables = new LinkedHashSet<>();

		for (Potential potential : potentials) {
			variables.addAll(potential.getVariables());
		}
		return new ArrayList<>(variables);
	}

	/**
	 * @param allVariables   {@code ArrayList} of {@code Variable}s
	 * @param variablesNames {@code String[]}s
	 * @return an {@code ArrayList} of {@code Variable}s with the
	 * variables in {@code allVariables} whose names are contained in
	 * {@code selectedVariables}, in the same order they are stored in
	 * {@code variablesNames}
	 */
	public static List<Variable> getVariables(List<Variable> allVariables, String[] variablesNames) {
		List<Variable> variables = new ArrayList<>();
		for (String name : variablesNames) {
			for (Variable variable : allVariables) {
				if (name.contentEquals(variable.getName())) {
					variables.add(variable);
				}
			}
		}
		return variables;
	}
}
