/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.model.network.constraint;

import java.util.List;

import org.openmarkov.core.action.AddLinkEdit;
import org.openmarkov.core.action.InvertLinkEdit;
import org.openmarkov.core.action.PNEdit;
import org.openmarkov.core.exception.NonProjectablePotentialException;
import org.openmarkov.core.exception.WrongCriterionException;
import org.openmarkov.core.model.network.Node;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.constraint.annotation.Constraint;

@Constraint(name = "NoCycle", defaultBehavior = ConstraintBehavior.YES) public class NoCycle extends PNConstraint {

	@Override public boolean checkProbNet(ProbNet probNet) {
		for (Node parent : probNet.getNodes()) {
			List<Node> children = probNet.getChildren(parent);
			for (Node child : children) {
				if (probNet.existsPath(child, parent, true)) {
					return false;
				}
			}
		}
		return true;
	}


	/**
	 * Check edit
	 * @param edit {@code PNEdit}
	 * @param probNet Network
	 * @return {@code true} if {@code event} comply with this constraint
	 */
	@Override
	public boolean checkEdit(ProbNet probNet, PNEdit edit)
			throws NonProjectablePotentialException, WrongCriterionException {
		List<PNEdit> edits = UtilConstraints.getSimpleEditsByType(edit, AddLinkEdit.class);
		//int u=0;
		for (PNEdit simpleEdit : edits) {
			if (((AddLinkEdit) simpleEdit).isDirected()) { // checks constraint
				Variable variable1 = ((AddLinkEdit) simpleEdit).getVariable1();
				Node node1 = probNet.getNode(variable1);
				Variable variable2 = ((AddLinkEdit) simpleEdit).getVariable2();
				Node node2 = probNet.getNode(variable2);
				if (probNet.existsPath(node2, node1, true)) {
					return false;
				}
			}
		}
		List<PNEdit> edits2 = UtilConstraints.getSimpleEditsByType(edit, InvertLinkEdit.class);
		for (PNEdit simpleEdit : edits2) {
			if (((InvertLinkEdit) simpleEdit).isDirected()) { // checks constraint
				Variable variable1 = ((InvertLinkEdit) simpleEdit).getVariable1();
				Node node1 = probNet.getNode(variable1);
				Variable variable2 = ((InvertLinkEdit) simpleEdit).getVariable2();
				Node node2 = probNet.getNode(variable2);
				probNet.removeLink(node1, node2, true);
				boolean existsPath = probNet.existsPath(node1, node2, true);
				probNet.addLink(node1, node2, true);
				if (existsPath) {
					return false;
				}
			}
		}
		return true;
	}

	@Override protected String getMessage() {
		return "no cycles allowed";
	}

}