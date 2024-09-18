/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.model.network.constraint;

import java.util.List;

import org.openmarkov.core.action.AddLinkEdit;
import org.openmarkov.core.action.PNEdit;
import org.openmarkov.core.exception.NonProjectablePotentialException;
import org.openmarkov.core.exception.WrongCriterionException;
import org.openmarkov.core.model.network.Node;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.constraint.annotation.Constraint;

@Constraint(name = "MaxNumParents", defaultBehavior = ConstraintBehavior.OPTIONAL) public class MaxNumParents
		extends PNConstraint {

	private int maxNumParents;

	public void setMaxNumParents(int maxNumParents) {
		this.maxNumParents = maxNumParents;
	}

	@Override public boolean checkProbNet(ProbNet probNet) {
		for (Node child : probNet.getNodes()) {
			int numParents = probNet.getNumParents(child);
			if (numParents > maxNumParents) {
				return false;
			}
		}
		return true;
	}

	@Override public boolean checkEdit(ProbNet probNet, PNEdit edit)
			throws NonProjectablePotentialException, WrongCriterionException {

		List<PNEdit> edits = UtilConstraints.getSimpleEditsByType(edit, AddLinkEdit.class);

		for (PNEdit simpleEdit : edits) {
			if (((AddLinkEdit) simpleEdit).isDirected()) {
				Variable variable2 = ((AddLinkEdit) simpleEdit).getVariable2();
				Node node2 = probNet.getNode(variable2);
				int numParents = probNet.getNumParents(node2);
				if (numParents >= maxNumParents) {
					return false;
				}
			}
		}

		/*edits = UtilConstraints.getEditsType(edit, RemoveLinkEdit.class);
		 for (PNEdit simpleEdit : edits) {
		 if (((RemoveLinkEdit)simpleEdit).isDirected()) {
		 Node node2 = ((RemoveLinkEdit)simpleEdit).getNode2().getNode();
		 int numParents=node2.getParents().size();
		 if (numParents >=maxNumParents) {
		 return false;
		 }
		 }
		 }*/
		return true;

	}

	@Override protected String getMessage() {
		return "a node may not have more than " + maxNumParents + "parents.";
	}

}
