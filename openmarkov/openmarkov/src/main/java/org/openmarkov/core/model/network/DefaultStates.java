/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.model.network;

import java.util.ArrayList;

/**
 * This class is used to encapsulate the default states of the nodes and their
 * dependent-language strings.
 *
 * @author jmendoza
 * @version 1.1 jlgozalo - fix javadoc and initial values for fields
 */
public class DefaultStates {

	/**
	 * Internal names of the different states. Each element of the outer
	 * ArrayList contains another ArrayList that has of the posibles states that
	 * a node may take.
	 */
	protected static ArrayList<ArrayList<String>> list = null;

	/**
	 * This method adds all the default states.
	 */
	protected static void fillList() {

		ArrayList<String> defaultStates = null;

		if (list == null) {
			list = new ArrayList<>();
			defaultStates = new ArrayList<>();
			defaultStates.add("absent");
			defaultStates.add("present");
			list.add(defaultStates);
			defaultStates = new ArrayList<>();
			defaultStates.add("no");
			defaultStates.add("yes");
			list.add(defaultStates);
			defaultStates = new ArrayList<>();
			defaultStates.add("negative");
			defaultStates.add("positive");
			list.add(defaultStates);
			defaultStates = new ArrayList<>();
			defaultStates.add("absent");
			defaultStates.add("mild");
			defaultStates.add("moderate");
			defaultStates.add("severe");
			list.add(defaultStates);
			defaultStates = new ArrayList<>();
			defaultStates.add("low");
			defaultStates.add("medium");
			defaultStates.add("high");
			list.add(defaultStates);
			//defaultStates = new ArrayList<String>();
			//defaultStates.add("nonamed");
			//list.add(defaultStates);
		}
	}

	/**
	 * This method returns an array containing the default states of an element
	 * of the list. If the index is out of range (index &#60; 0 || index &#62; list
	 * size) the null is returned.
	 *
	 * @param index element of the list of default states.
	 * @return an array that contains the default states of an element of the
	 * list of default states.
	 */
	public static String[] getByIndex(int index) {

		ArrayList<String> defaultStates = null;

		if (list == null) {
			fillList();
		}
		try {
			defaultStates = list.get(index);
		} catch (IndexOutOfBoundsException e) {
			return null;
		}
		return defaultStates.toArray(new String[defaultStates.size()]);
	}

	/**
	 * This method returns the index in the list of the states passed as
	 * parameter. A states set matches an element of the list if has the same
	 * size and the same elements in the same order. The elements of the list
	 * are the names of the states, not the language-dependent strings. If the
	 * parameter doesn't match any element of the list, then the last index is
	 * returned.
	 *
	 * @param states array that contains the names of the states.
	 * @return the index in the list of the states set
	 */
	public static int getIndex(State[] states) {

		ArrayList<String> statesAsList = new ArrayList<>();
		int i = 0, l = 0;
		boolean found = false;

		if (list == null) {
			fillList();
		}
		for (State state : states) {
			statesAsList.add(state.getName());
		}
		l = list.size();
		while (!found && (i < l)) {
			if (list.get(i).equals(statesAsList)) {
				found = true;
			} else {
				i++;
			}
		}
		return (found) ? i : l - 1;
	}

	/**
	 * Returns the default states that correspond to a type of node. A default
	 * set of states is given for the chance nodes. A prefixed set of states
	 * (yes, no) corresponds to the decision nodes. Utility nodes hasn't states.
	 *
	 * @param type                 type of the node.
	 * @param networkDefaultStates default set of states.
	 * @return a set of states corresponding to the type of the node.
	 */
	public static State[] getStatesNodeType(NodeType type, State[] networkDefaultStates) {

		ArrayList<String> elements = null;

		if (list == null) {
			fillList();
		}
		switch (type) {
		case CHANCE: {
			return networkDefaultStates;
		}
		case DECISION: {
			elements = list.get(1);
			String[] statesAux = elements.toArray(new String[elements.size()]);
			State[] states = new State[elements.size()];
			int i = 0;
			for (String stateSingle : statesAux) {
				states[i] = new State(stateSingle);
				i++;

			}
			return states;
		}
		case UTILITY: {
			return new State[] { new State("Default") };
		}
		default: {
			return null;
		}
		}
	}
}
