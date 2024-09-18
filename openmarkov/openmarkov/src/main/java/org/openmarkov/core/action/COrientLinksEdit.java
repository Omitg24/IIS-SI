/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.action;

import java.util.Vector;

import javax.swing.undo.UndoableEdit;

import org.openmarkov.core.model.network.ProbNet;

@SuppressWarnings("serial") public class COrientLinksEdit extends CompoundPNEdit {

	public COrientLinksEdit(ProbNet probNet, Vector<UndoableEdit> edits) {
		super(probNet);
		this.edits = edits;
	}

	// Methods
	@Override public void generateEdits() {
	}

	public String toString() {
		StringBuilder buffer = new StringBuilder("Orient links: ");
		for (UndoableEdit edit : edits) {
			OrientLinkEdit orientLinkEdit = (OrientLinkEdit) edit;
			buffer.append(orientLinkEdit.getVariable1().getName());
			if (orientLinkEdit.isDirected()) {
				buffer.append(" --> ");
			} else {
				buffer.append(" --- ");
			}
			buffer.append(orientLinkEdit.getVariable2().getName());
			buffer.append(", ");
		}
		buffer.delete(buffer.lastIndexOf(","), buffer.length());
		return buffer.toString();
	}

	@Override public boolean equals(Object arg0) {
		boolean sameInformation = true;

		if (arg0 instanceof COrientLinksEdit) {
			COrientLinksEdit editToCompare = (COrientLinksEdit) arg0;

			for (UndoableEdit edit : editToCompare.edits) {
				sameInformation &= edits.contains(edit);
			}

			for (UndoableEdit edit : edits) {
				sameInformation &= editToCompare.edits.contains(edit);
			}
		} else {
			sameInformation = false;
		}

		return sameInformation;
	}

}
