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
import org.openmarkov.core.model.network.NodeType;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.constraint.annotation.Constraint;

/**
 * This class implements the NoMixedParents constraint, which establishes that all the parents
 *  of a utility node belong to only one of these two sets of parents:
 *  - chance and decision nodes
 *  - utility nodes
 *  @author ckonig
 */
@Constraint(name = "NoMixedParents", defaultBehavior = ConstraintBehavior.OPTIONAL)
public class NoMixedParents extends PNConstraint {

	@Override public boolean checkProbNet(ProbNet probNet) {
		List<Node> utilityNodes = probNet.getNodes(NodeType.UTILITY);
		boolean metCondition = true;
		int i = 0;
		int numUtilityNodes = utilityNodes.size();
		while (i < numUtilityNodes && metCondition) {
			List<Node> parents = probNet.getParents(utilityNodes.get(i++));
			int numParents = parents.size();
			boolean utilityParent = false;
			boolean chanceOrDecisionParent = false;
			for (int j = 0; j < numParents && metCondition; j++) {
				NodeType parentNodeType = parents.get(j).getNodeType();
				utilityParent |= parentNodeType == NodeType.UTILITY;
				chanceOrDecisionParent |= parentNodeType == NodeType.CHANCE || parentNodeType == NodeType.DECISION;
				metCondition = !(utilityParent && chanceOrDecisionParent);
			}
		}
		return metCondition;
	}

	@Override public boolean checkEdit(ProbNet probNet, PNEdit edit)
			throws NonProjectablePotentialException, WrongCriterionException {
		List<PNEdit> edits = UtilConstraints.getSimpleEditsByType(edit, AddLinkEdit.class);
		for (PNEdit simpleEdit : edits) {
			AddLinkEdit addLinkEdit = (AddLinkEdit) simpleEdit;
			if (addLinkEdit.isDirected()) {
				Variable variable2 = addLinkEdit.getVariable2();
				Node node2 = probNet.getNode(variable2);
				if (node2.getNodeType() == NodeType.UTILITY) {
					Variable variable1 = addLinkEdit.getVariable1();
					Node node1 = probNet.getNode(variable1);
					return !hasMixedParents(probNet, node1, node2);
				}
			}
		}

		List<PNEdit> edits3 = UtilConstraints.getSimpleEditsByType(edit, InvertLinkEdit.class);
		for (PNEdit simpleEdit : edits3) {
			InvertLinkEdit invertLinkEdit = (InvertLinkEdit) simpleEdit;
			Variable variable2 = invertLinkEdit.getVariable2();
			Node node2 = probNet.getNode(variable2);
			if (node2.getNodeType() == NodeType.UTILITY) {
				Variable variable1 = invertLinkEdit.getVariable1();
				Node node1 = probNet.getNode(variable1);
				return !hasMixedParents(probNet, node2, node1);
			}
		}
		return true;
	}

	/******
	 * Checks if a node has mixed parents.
	 * @param parentNode the parent node
	 * @param childNode the child node
	 * @return {@code true} if the {@code childNode} has mixedParents
	 */
	private boolean hasMixedParents(ProbNet probNet, Node parentNode, Node childNode) {
		boolean utilityParent = parentNode.getNodeType() == NodeType.UTILITY;
		boolean chanceOrDecisionParent = parentNode.getNodeType() == NodeType.DECISION
				|| parentNode.getNodeType() == NodeType.CHANCE;

		for (Node parent : probNet.getParents(childNode)) {
			NodeType parentNodeType = parent.getNodeType();
			utilityParent |= parentNodeType == NodeType.UTILITY;
			chanceOrDecisionParent |= parentNodeType == NodeType.CHANCE || parentNodeType == NodeType.DECISION;

			if (utilityParent && chanceOrDecisionParent) {
				return true;
			}
		}
		return false;
	}

	@Override protected String getMessage() {
		return "utility nodes can not have mixed parents.";
	}
}
