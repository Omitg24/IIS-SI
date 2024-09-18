/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.model.network;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;

import org.openmarkov.core.model.network.potential.Potential;
import org.openmarkov.core.model.network.potential.PotentialRole;

/**
 * @author mluque It is the type of 'nodesHashMaps'. It contains a
 * {@code LinkedHashMap} from {@code NodeType} to
 * {@code NodesHashMapType}.
 */
public class NodeTypeDepot {
	private LinkedHashMap<NodeType, NodesHashMap> nodesHashMaps;

	public NodeTypeDepot() {
		nodesHashMaps = new LinkedHashMap<>();
		// create a linkedHashMap for each type of nodes
		for (NodeType type : NodeType.values()) {
			nodesHashMaps.put(type, new NodesHashMap());
		}
	}

	public int getNumNodes() {
		int numNodes = 0;
		for (NodesHashMap hashMap : nodesHashMaps.values()) {
			numNodes = numNodes + hashMap.size();
		}
		return numNodes;
	}

	public int getNumNodes(NodeType nodeType) {
		return nodesHashMaps.get(nodeType).size();
	}

	public List<Node> getNodes() {
		List<Node> nodes = new ArrayList<>(getNumNodes());
		for (NodesHashMap hashMap : nodesHashMaps.values()) {
			nodes.addAll(hashMap.values());
		}
		return nodes;
	}

	public List<Potential> getPotentialsByType(NodeType nodeType) {
		NodesHashMap nodesType = nodesHashMaps.get(nodeType);
		List<Potential> potentials = new ArrayList<>();
		for (Node node : nodesType.values()) {
			potentials.addAll(node.getPotentials());
		}
		return potentials;
	}

	/**
	 * @param nodeType Node type
	 * @return All the nodes of certain kind
	 */
	public List<Node> getNodes(NodeType nodeType) {
		return new ArrayList<>(nodesHashMaps.get(nodeType).values());
	}

	public List<Potential> getPotentialsByRole(PotentialRole role) {
		List<Potential> potentials = new ArrayList<>();
		for (NodesHashMap nodesHashMap : nodesHashMaps.values()) {
			for (Node auxNode : nodesHashMap.values()) {
				for (Potential auxPot : auxNode.getPotentials()) {
					if (auxPot.getPotentialRole() == role) {
						potentials.add(auxPot);
					}
				}
			}
		}
		return potentials;
	}

	public Node getNode(NodeType nodeType, Variable variable) {
		return nodesHashMaps.get(nodeType).get(variable);
	}

	public Node getNode(String nameOfVariable) {
		for (NodeType nodeType : NodeType.values()) {
			Collection<Node> nodes = nodesHashMaps.get(nodeType).values();
			for (Node node : nodes) {
				if (node.getVariable().getName().contentEquals(nameOfVariable)) {
					return node;
				}
			}
		}
		return null;
	}

	public Node getNode(Variable variable) {
		Node node = null;
		for (NodesHashMap nodes : nodesHashMaps.values()) {
			if ((node = nodes.get(variable)) != null) {
				break;
			}
		}
		return node;
	}

	/**
	 * @param nameOfVariable {@code String}
	 * @param nodeType       {@code NodeType}
	 * @return The node with {@code nameOfVariable} and
	 * {@code kindOfNode} if exists otherwhise null
	 */
	public Node getNode(String nameOfVariable, NodeType nodeType) {
		for (Node node : nodesHashMaps.get(nodeType).values()) {
			if (node.getVariable().getName().contentEquals(nameOfVariable)) {
				return node;
			}
		}
		return null;
	}

	public void addNode(Node node) {
		nodesHashMaps.get(node.getNodeType()).put(node.getVariable(), node);
	}

	public void removeNode(Node node) {
		NodeType nodeKindValue = node.getNodeType();
		Variable variable = node.getVariable();
		NodesHashMap nodesMap = nodesHashMaps.get(nodeKindValue);
		nodesMap.remove(variable);
	}

	public void removeNode(Variable variable) {
		for (NodesHashMap nodes : nodesHashMaps.values()) {
			if (nodes.get(variable) != null) {
				nodes.remove(variable);
				break;
			}
		}
	}

	public int getNumPotentials() {
		int numPotentials = 0;
		for (NodesHashMap linkedHasMap : nodesHashMaps.values()) {
			for (Node node : linkedHasMap.values()) {
				numPotentials += node.getNumPotentials();
			}
		}
		return numPotentials;
	}

	/**
	 * @author mluque Contains a {@code LinkedHashMap} from
	 * {@code Variable} to {@code Node}.
	 */
	private class NodesHashMap {
		LinkedHashMap<Variable, Node> nodesHashMap;

		NodesHashMap() {
			nodesHashMap = new LinkedHashMap<>();
		}

		public Node get(Variable variable) {
			return nodesHashMap.get(variable);
		}

		public void put(Variable variable, Node node) {
			nodesHashMap.put(variable, node);
		}

		public int size() {
			return nodesHashMap.size();
		}

		public Collection<Node> values() {
			return nodesHashMap.values();
		}

		public void remove(Variable variable) {
			nodesHashMap.remove(variable);
		}
	}

}
