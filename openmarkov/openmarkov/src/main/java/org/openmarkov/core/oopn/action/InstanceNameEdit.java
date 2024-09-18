/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.oopn.action;

import javax.swing.undo.CannotUndoException;

import org.openmarkov.core.action.CompoundPNEdit;
import org.openmarkov.core.action.NodeNameEdit;
import org.openmarkov.core.exception.NonProjectablePotentialException;
import org.openmarkov.core.exception.WrongCriterionException;
import org.openmarkov.core.model.network.Node;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.oopn.Instance;

@SuppressWarnings("serial") public class InstanceNameEdit extends CompoundPNEdit {

	private String newName;
	private String oldName;
	private Instance instance;

	public InstanceNameEdit(ProbNet probNet, Instance instance, String newName) {
		super(probNet);
		this.newName = newName;
		this.instance = instance;
		this.oldName = instance.getName();
	}

	@Override public void generateEdits() throws NonProjectablePotentialException, WrongCriterionException {
		this.instance.setName(newName);
		for (Node instanceNode : instance.getNodes()) {
			String newNodeName = instanceNode.getName().replace(oldName, newName);
			addEdit(new NodeNameEdit(instanceNode, newNodeName));
		}
	}

	@Override public void undo() throws CannotUndoException {
		super.undo();
		this.instance.setName(oldName);
	}

}
