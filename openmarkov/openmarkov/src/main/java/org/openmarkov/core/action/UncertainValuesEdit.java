/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.action;

import java.util.ArrayList;
import java.util.List;

import org.openmarkov.core.model.network.Node;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.modelUncertainty.UncertainValue;
import org.openmarkov.core.model.network.potential.ExactDistrPotential;
import org.openmarkov.core.model.network.potential.TablePotential;

/**
 * {@code AddNodeEdit} is a edit that allow add a node to
 * {@code ProbNet} object.
 *
 * @author mluque
 * @version 1 23/06/11
 */
@SuppressWarnings("serial") public class UncertainValuesEdit extends SimplePNEdit {
	private List<Double> newValuesColumn;
	private List<UncertainValue> newUncertainColumn;
	private List<Double> oldValuesColumn;
	private List<UncertainValue> oldUncertainColumn;
	private int basePosition;
	private Node node;
	private boolean isChanceVariable;
	private boolean wasNullOldUncertainValues;
	/**
	 * Selected column in the values table
	 */
	private int selectedColumn;

	/**
	 * Creates a new {@code AddNodeEdit} with the network where the new
	 * new node will be added and basic information about it.
	 *
	 * @param node             the new node
	 * @param uncertainColumn Uncertain column
	 * @param valuesColumn Values column
	 * @param basePosition Base position
	 * @param selectedColumn Selected column
	 * @param isChanceVariable Is chance variable?
	 */
	public UncertainValuesEdit(Node node, List<UncertainValue> uncertainColumn, List<Double> valuesColumn,
			int basePosition, int selectedColumn, boolean isChanceVariable) {
		super(node.getProbNet());
		this.node = node;
		this.isChanceVariable = isChanceVariable;
		Variable variable = node.getVariable();
		newUncertainColumn = uncertainColumn;
		newValuesColumn = valuesColumn;
		this.basePosition = basePosition;
		UncertainValue[] oldUncertainValues = getPotential().getUncertainValues();
		wasNullOldUncertainValues = oldUncertainValues == null;
		oldUncertainColumn = wasNullOldUncertainValues ? null : getColumn(oldUncertainValues, variable, basePosition);
		oldValuesColumn = getColumn(getPotential().values, variable, basePosition);
		this.selectedColumn = selectedColumn;
	}

	/**
	 * It replaces a column in the uncertain values table. If parameter 'column'
	 * is null then all the replaced cells are set to null.
	 *
	 * @param potential Potential
	 * @param column Column
	 * @param var Variable
	 * @param basePosition Base position
	 */
	static void placeUncertainColumn(TablePotential potential, List<UncertainValue> column, Variable var,
			int basePosition) {
		UncertainValue[] table = (potential.getUncertainValues());
		for (int i = 0; i < var.getNumStates(); i++) {
			table[i + basePosition] = (column != null) ? column.get(i) : null;
		}
	}

	public int getBasePosition() {
		return basePosition;
	}

	public boolean isChanceVariable() {
		return isChanceVariable;
	}

	public Node getNode() {
		return node;
	}

	private List<Double> getColumn(double[] values, Variable variable, int basePosition) {
		List<Double> column = new ArrayList<>();
		int numElements = (isChanceVariable) ? variable.getNumStates() : 1;
		for (int i = 0; i < numElements; i++) {
			column.add(values[basePosition + i]);
		}
		return column;
	}

	private List<UncertainValue> getColumn(UncertainValue[] uncertainValues, Variable variable, int basePosition) {
		List<UncertainValue> column = new ArrayList<>();
		int numElements = (isChanceVariable) ? variable.getNumStates() : 1;
		for (int i = 0; i < numElements; i++) {
			column.add(uncertainValues[basePosition + i]);
		}
		return column;
	}

	public int getSelectedColumn() {
		return selectedColumn;
	}

	private TablePotential getPotential() {
		if (node.getPotentials().get(0) instanceof TablePotential) {
			return (TablePotential) (node.getPotentials().get(0));
		} else if (node.getPotentials().get(0) instanceof ExactDistrPotential) {
			return ((ExactDistrPotential) (node.getPotentials().get(0))).getTablePotential();
		} else {
			return null;
		}
	}

	public Variable getVariable() {
		return node.getVariable();
	}

	@Override public void doEdit() {
		TablePotential potential = getPotential();
		if (wasNullOldUncertainValues) {
			potential.setUncertainValues(new UncertainValue[potential.getTableSize()]);
		}
		placeNewUncertainColumn(potential);
		placeNewValuesColumn(potential);
	}

	private void placeNewValuesColumn(TablePotential potential) {
		placeValuesColumn(potential, newValuesColumn);
	}

	private void placeOldValuesColumn(TablePotential potential) {
		placeValuesColumn(potential, oldValuesColumn);
	}

	private void placeOldUncertainColumn(TablePotential potential) {
		placeUncertainColumn(potential, oldUncertainColumn, getVariable(), basePosition);
	}

	private void placeNewUncertainColumn(TablePotential potential) {
		placeUncertainColumn(potential, newUncertainColumn, getVariable(), basePosition);
	}

	private void placeValuesColumn(TablePotential potential, List<Double> column) {
		double[] table = potential.getValues();
		Variable var = getVariable();
		for (int i = 0; i < var.getNumStates(); i++) {
			table[i + basePosition] = column.get(i);
		}
	}

	public void undo() {
		super.undo();
		TablePotential potential = getPotential();
		if (wasNullOldUncertainValues) {
			potential.setUncertainValues(null);
		} else {
			placeOldUncertainColumn(potential);
		}
		placeOldValuesColumn(potential);
	}
}