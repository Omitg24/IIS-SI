/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */
package org.openmarkov.core.inference;

import java.util.ArrayList;
import java.util.List;

import org.openmarkov.core.exception.NodeNotFoundException;
import org.openmarkov.core.model.graph.Link;
import org.openmarkov.core.model.network.Node;
import org.openmarkov.core.model.network.NodeType;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.Variable;

/**
 * @author mluque
 * The transitive reduction of the partial temporal order among decisions induced by the DAN.
 */
public class PartialOrderDAN {

	ProbNet order;

	public PartialOrderDAN(ProbNet probNet) throws NodeNotFoundException, NodeNotFoundException {

		order = new ProbNet();

		//Only keep decision nodes
		for (Node auxNode : probNet.getNodes()) {
			//order.getGraph().removeLinks(auxNode.getNode());
			NodeType auxType = auxNode.getNodeType();
			//if ((auxType!=NodeType.CHANCE)&&(auxType!=NodeType.DECISION)){
			if (auxType == NodeType.DECISION) {
				order.addNode(auxNode.getVariable(), auxType);
			}
		}

		//Transitive closure among decision nodes
		for (Node nodeI : order.getNodes()) {
			for (Node nodeJ : order.getNodes()) {
				if (nodeI != nodeJ) {
					Variable variableI = nodeI.getVariable();
					Variable variableJ = nodeJ.getVariable();
					Node probNetnodeI = probNet.getNode(variableI);
					Node probNetNodeJ = probNet.getNode(variableJ);
					if (probNet.existsPath(probNetnodeI, probNetNodeJ, true)) {
						order.addLink(order.getNode(variableI), order.getNode(variableJ), true);
					}
				}
			}
		}

		//Transitive reduction
		List<Link<Node>> linksToRemove = new ArrayList<>();
		for (Node dec : order.getNodes()) {
			List<Link<Node>> decLinks = order.getLinks(dec);
			for (int i = 0; i < decLinks.size(); i++) {
				Node nodeI = decLinks.get(i).getNode2();
				for (int j = 0; j < decLinks.size(); j++) {
					Link<Node> linkJ = decLinks.get(j);
					Node nodeJ = linkJ.getNode2();
					if ((nodeI != nodeJ) && order.existsPath(nodeI, nodeJ, true)) {
						linksToRemove.add(linkJ);

					}
				}
			}
		}
		for (Link<Node> auxLink : linksToRemove) {
			order.removeLink(auxLink);
		}
	}

	public ProbNet getOrder() {
		return order;
	}

	public String toStringForGraphviz() throws NodeNotFoundException {

		String content = null;

		ProbNet probNet = this.getOrder();
		List<Link<Node>> links = probNet.getLinks();
		content = "digraph G {\n";

		for (Node node : probNet.getNodes()) {
			String strType = null;
			switch (node.getNodeType()) {
			case CHANCE:
				strType = "ellipse";
				break;
			case DECISION:
				strType = "decision";
				break;
			default:
				strType = "";
			}
			content = content + getNameWithQuotes(node) + "[shape=" + strType + "]\n";
		}

		for (Link<Node> link : links) {
			Node node1 = link.getNode1();
			Node node2 = link.getNode2();

			content = content + getNameWithQuotes(node1) + "-> " + getNameWithQuotes(node2) + "\n";

		}
		content = content + "}\n";

		return content;

	}

	private String getNameWithQuotes(Node node) throws NodeNotFoundException {
		return "\"" + node.getVariable().getName() + "\"";

	}

}
