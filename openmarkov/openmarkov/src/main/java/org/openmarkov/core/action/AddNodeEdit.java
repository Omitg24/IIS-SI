/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.action;

import java.awt.geom.Point2D;

import org.openmarkov.core.model.network.Node;
import org.openmarkov.core.model.network.NodeType;
import org.openmarkov.core.model.network.PolicyType;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.potential.operation.PotentialOperations;

/**
 * {@code AddNodeEdit} is a edit that allow add a node to
 * {@code ProbNet} object.
 *
 * @author mpalacios
 * @version 1 21/12/10
 */
@SuppressWarnings("serial") public class AddNodeEdit extends SimplePNEdit {

	// Atribbutes
	/**
	 * The new Variable object that match the new node.
	 */
	protected Variable variable;

	/**
	 * The node type of the new node.
	 */
	protected NodeType nodeType;

	/**
	 * Graphic position of the new node
	 */
	protected Point2D.Double cursorPosition;

	/**
	 * The new node
	 */
	protected Node newNode;

	/**
	 * Creates a new {@code AddNodeEdit} with the network where the new
	 * new node will be added and basic information about it.
	 *
	 * @param probNet        the {@code ProbNet} where the new node will be added.
	 * @param variable       the variable contained in the new node
	 * @param nodeType       The new node type.
	 * @param cursorPosition the position (coordinates X,Y) of the node.
	 */
	public AddNodeEdit(ProbNet probNet, Variable variable, NodeType nodeType, Point2D.Double cursorPosition) {
		super(probNet);
		this.cursorPosition = (Point2D.Double) cursorPosition.clone();
		this.probNet = probNet;
		this.nodeType = nodeType;
		this.variable = variable;
	}

	/**
	 * Creates a new {@code AddNodeEdit} with the network where the new
	 * new node will be added and basic information about it.
	 *
	 * @param probNet  the {@code ProbNet} where the new node will be added.
	 * @param variable the variable contained in the new node
	 * @param nodeType The new node type.
	 */
	public AddNodeEdit(ProbNet probNet, Variable variable, NodeType nodeType) {
		this(probNet, variable, nodeType, new Point2D.Double());
	}

	@Override public void doEdit() {
		// Adds the new variable to network ( creates a node instance )
		newNode = probNet.addNode(variable, nodeType);
		// TODO revisar si es conveniente utilizar una constraint
		// Sets a uniformPotential for the new node
		// Decision node has no potential when is created
		if (nodeType != NodeType.DECISION) {
			probNet.addPotential(PotentialOperations.getUniformPotential(probNet, variable, nodeType));
			newNode = probNet.getNode(variable);
		} else {
			newNode.setPolicyType(PolicyType.OPTIMAL);
		}
		// Sets the visual node position
		newNode.setCoordinateX((int) cursorPosition.getX());
		newNode.setCoordinateY((int) cursorPosition.getY());
	}

	public void undo() {
		super.undo();
		probNet.removeNode(newNode);
	}

	/**
	 * @return newNode the new {@code Node} added
	 */
	public Node getNode() {
		return newNode;
	}

	public Variable getVariable() {
		return variable;
	}

	public NodeType getNodeType() {
		return nodeType;
	}

	public Point2D.Double getCursorPosition() {
		return cursorPosition;
	}

	public String getPresentationName() {
		return "Edit.AddNodeEdit";
	}

	public String getUndoPresentationName() {
		return "Edit.AddNodeEdit.Undo";
	}

	public String getRedoPresentationName() {
		return "Edit.AddNodeEdit.Redo";
	}

	public void redo() {
		setTypicalRedo(false);
		super.redo();
		probNet.addNode(newNode);
	}
}
