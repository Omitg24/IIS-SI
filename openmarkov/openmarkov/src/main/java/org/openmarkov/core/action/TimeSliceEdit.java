/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.action;

import org.openmarkov.core.exception.DoEditException;
import org.openmarkov.core.model.network.Node;
import org.openmarkov.core.model.network.Variable;

/**
 * @author myebra
 */
@SuppressWarnings("serial") public class TimeSliceEdit extends SimplePNEdit {
	/**
	 * The last time slice before the edition
	 */
	private int lastTimeSlice;
	/**
	 * The new time slice after the edition
	 */
	private int newTimeSlice;
	/**
	 * The edited node
	 */
	private Node node = null;
	/**
	 * the last base name of the temporal variable
	 */
	private String lastBaseName;
	/**
	 * The last variable name
	 */
	private String lastName;

	private Variable variable;

	/**
	 * @param node Node
	 * @param timeSlice Time slice
	 */
	public TimeSliceEdit(Node node, int timeSlice) {
		super(node.getProbNet());
		variable = node.getVariable();
		this.lastTimeSlice = variable.getTimeSlice();
		this.newTimeSlice = timeSlice;
		this.lastBaseName = variable.getBaseName();
		this.lastName = variable.getName();
		this.node = node;
	}

	@Override public void doEdit() throws DoEditException {
		//onlyTemporal && not only atemporal
		variable.setTimeSlice(newTimeSlice);
		if (newTimeSlice == Integer.MIN_VALUE && lastTimeSlice != Integer.MIN_VALUE && lastBaseName != null) {
			variable.setBaseName(null);
			int beginSlicePart = lastName.lastIndexOf('[') - 1;
			String newName = null;
			if (beginSlicePart > 0) {
				newName = lastName.substring(0, beginSlicePart);
			}
			variable.setName(newName);
		}
		//not only temporaL && not only atemporal but also set name and base name
		if (lastTimeSlice == Integer.MIN_VALUE) {
			variable.setBaseName(lastBaseName);
			variable.setName(lastName + " " + "[" + String.valueOf(newTimeSlice) + "]");
		}
	}

	@Override public void undo() {
		super.undo();
		//onlyTemporal
		variable.setTimeSlice(lastTimeSlice);
		//not only temporaL && not only atemporal but also set name and base name
		if (lastTimeSlice == Integer.MIN_VALUE) {
			variable.setBaseName(lastBaseName);
			variable.setName(lastName);
		}
	}

	/**
	 * Gets the new name of the node
	 *
	 * @return the new name of the node
	 */
	public String getNewName() {
		return lastBaseName + " " + "[" + String.valueOf(newTimeSlice) + "]";
	}

	/**
	 * Gets the previous name of the node
	 *
	 * @return the previous name of the node
	 */
	public String getPreviousName() {
		return lastBaseName + " " + "[" + String.valueOf(lastTimeSlice) + "]";
	}
}
