/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.model.network.constraint;

import java.util.ArrayList;
import java.util.List;

import org.openmarkov.core.action.AddLinkEdit;
import org.openmarkov.core.action.NodeAlwaysObservedEdit;
import org.openmarkov.core.action.PNEdit;
import org.openmarkov.core.exception.NonProjectablePotentialException;
import org.openmarkov.core.exception.WrongCriterionException;
import org.openmarkov.core.model.network.Node;
import org.openmarkov.core.model.network.NodeType;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.constraint.annotation.Constraint;

@Constraint(name = "NoAlwaysObservedDescendantOfDecision", defaultBehavior = ConstraintBehavior.YES) public class NoAlwaysObservedDescendantOfDecision
		extends PNConstraint {

	@Override protected String getMessage() {
		return "an always-observed variable cannot be a descendant of a decision node";
	}

	@Override public boolean checkProbNet(ProbNet probNet) {
		boolean checkNetwork = true;
		List<Node> decisionNodes = getDecisionNodes(probNet);
		List<Node> alwaysObservedNodes = getAlwaysObservedNodes(probNet);
		for (int i = 0; i < alwaysObservedNodes.size() && checkNetwork; i++) {
			checkNetwork = !itHasSomeAncestorInList(probNet, alwaysObservedNodes.get(i), decisionNodes);
		}
		return checkNetwork;
	}

	/**
	 * @param node Node
	 * @param nodes Nodes
	 * @return true if 'node' has some ancestor in 'nodes' (considering
	 * direction of the links)
	 */
	private boolean itHasSomeAncestorInList(ProbNet network, Node node, List<Node> nodes) {
		boolean itHasAncestor = false;
		for (int i = 0; i < nodes.size() && !itHasAncestor; i++) {
			itHasAncestor = isReachable(network, nodes.get(i), node);
		}
		return itHasAncestor;
	}

	private List<Node> getAlwaysObservedNodes(ProbNet network) {
		List<Node> alwaysObservedNodes = new ArrayList<Node>();
		for (Node node : network.getNodes()) {
			if (node.isAlwaysObserved()) {
				alwaysObservedNodes.add(node);
			}
		}
		return alwaysObservedNodes;
	}

	@Override public boolean checkEdit(ProbNet probNet, PNEdit edit)
			throws NonProjectablePotentialException, WrongCriterionException {
		boolean checkEdit = true;

		List<Node> decisionNodes = getDecisionNodes(probNet);
		List<Node> alwaysObservedNodes = getAlwaysObservedNodes(probNet);

		List<PNEdit> edits = UtilConstraints.getSimpleEditsByType(edit, AddLinkEdit.class);
		for (int i = 0; i < edits.size() && checkEdit; i++) {
			PNEdit simpleEdit = edits.get(i);
			AddLinkEdit addLinkEdit = (AddLinkEdit) simpleEdit;
			if (addLinkEdit.isDirected()) { // checks constraint
				Variable variable1 = addLinkEdit.getVariable1();
				Node node1 = probNet.getNode(variable1);
				Variable variable2 = addLinkEdit.getVariable2();
				Node node2 = probNet.getNode(variable2);
				checkEdit = !(
						itHasSomeAncestorInList(probNet, node1, decisionNodes) && itHasSomeDescendantInList(probNet,
								node2, alwaysObservedNodes)
				);
			}
		}

		if (checkEdit) {
			List<PNEdit> edits2 = UtilConstraints.getSimpleEditsByType(edit, NodeAlwaysObservedEdit.class);
			for (int i = 0; i < edits2.size() && checkEdit; i++) {
				PNEdit simpleEdit = edits2.get(i);
				NodeAlwaysObservedEdit nodeAlwaysObservedEdit = (NodeAlwaysObservedEdit) simpleEdit;
				checkEdit = !(
						nodeAlwaysObservedEdit.getNewAlwaysObserved() && itHasSomeAncestorInList(probNet,
								nodeAlwaysObservedEdit.getNode(), decisionNodes)
				);
			}
		}
		return checkEdit;
	}

	private boolean itHasSomeDescendantInList(ProbNet network, Node node, List<Node> nodes) {
		boolean itHasDescendant = false;
		for (int i = 0; i < nodes.size() && !itHasDescendant; i++) {
			itHasDescendant = isReachable(network, node, nodes.get(i));
		}
		return itHasDescendant;
	}

	private boolean isReachable(ProbNet network, Node node1, Node node2) {
		return network.existsPath(node1, node2, true);
	}

	private List<Node> getDecisionNodes(ProbNet network) {
		return network.getNodes(NodeType.DECISION);
	}

}
