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
import org.openmarkov.core.model.graph.Link;
import org.openmarkov.core.model.network.Node;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.constraint.annotation.Constraint;

/**
 * This class implements the NoMultipleLinks constraint, which establishes the following rules:
 *  - The undirected link between A and B is both incompatible with any directed link between A and B.
 *  - The directed link between A and B is compatible with the directed link between B and A
 *  @author ckonig
 */
@Constraint(name = "NoMultipleLinks", defaultBehavior = ConstraintBehavior.YES)
public class NoMultipleLinks extends PNConstraint {

	@Override public boolean checkProbNet(ProbNet probNet) {
		List<Node> nodesGraph = probNet.getNodes();
		for (Node node : nodesGraph) {
			for (Link<Node> link : probNet.getLinks(node)) {
				Node node1 = link.getNode1();
				Node node2 = link.getNode2();
				boolean directed = link.isDirected();
				if (!checkLink(probNet, node1, node2, directed)) {
					return false;
				}
			}
		}
		return true;
	}

	@Override public boolean checkEdit(ProbNet probNet, PNEdit edit)
			throws NonProjectablePotentialException, WrongCriterionException {
		List<PNEdit> edits = UtilConstraints.getSimpleEditsByType(edit, AddLinkEdit.class);
		for (PNEdit simpleEdit : edits) {
			Variable variable1 = ((AddLinkEdit) simpleEdit).getVariable1();
			Node node1 = probNet.getNode(variable1);
			Variable variable2 = ((AddLinkEdit) simpleEdit).getVariable2();
			Node node2 = probNet.getNode(variable2);
			boolean directed = ((AddLinkEdit) simpleEdit).isDirected();
			if (!checkLink(probNet, node1, node2, directed)) {
				return false;
			}
		}

		List<PNEdit> edits3 = UtilConstraints.getSimpleEditsByType(edit, InvertLinkEdit.class);
		for (PNEdit simpleEdit : edits3) {
			Variable variable1 = ((InvertLinkEdit) simpleEdit).getVariable1();
			Node node1 = probNet.getNode(variable1);
			Variable variable2 = ((InvertLinkEdit) simpleEdit).getVariable2();
			Node node2 = probNet.getNode(variable2);
			boolean directed = ((InvertLinkEdit) simpleEdit).isDirected();
			if (!checkLink(probNet, node2, node1, directed)) {
				return false;
			}
		}
		return true;
	}

	/*****
	 * Checks if a link between node1 and node2
	 * satisfies the restriction of noMultipleLinks
	 * @param probNet Network
	 * @param node1 First node
	 * @param node2 Second node
	 * @param directed - true if the link is directed
	 * @return True if the link between node1 and
	 *         node1 has no multipleLinks
	 */
	private boolean checkLink(ProbNet probNet, Node node1, Node node2, boolean directed) {
		if (directed) {
			return checkDirectedLink(probNet, node1, node2);
		} else {
			return checkUndirectedLink(probNet, node1, node2);
		}
	}

	/*********
	 * Checks if a directed link between {@code node1} and
	 * {@code node2} satisfies the restriction of noMultipleLinks
	 * @param probNet Network
	 * @param node1 First node
	 * @param node2 Second node
	 * @return {@code true} if the link between {@code node1} and
	 *         {@code node2}has no multipleLinks
	 */
	private boolean checkDirectedLink(ProbNet probNet, Node node1, Node node2) {
		if (probNet.getLink(node1, node2, false) != null) {
			return false;
		}
		return true;
	}

	/*****
	 * Checks if a undirected link between {@code node1} and
	 * {@code node2} satisfies the restriction of noMultipleLinks
	 * @param probNet Network
	 * @param node1 First node
	 * @param node2 Second node
	 * @return {@code true} if the link between {@code node1} and
	 *         {@code node2}has no multipleLinks
	 */
	private boolean checkUndirectedLink(ProbNet probNet, Node node1, Node node2) {
		// neither a directed link from node1 -> node2 nor node2 ->
		// node1 may exist
		if ((probNet.getLink(node1, node2, true) != null) || (probNet.getLink(node2, node1, true) != null)) {
			return false;
		}
		return true;
	}

	@Override protected String getMessage() {
		return " no multiple links allowed.";
	}
}