/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.model.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * This class implements the minimal set of methods for creating
 * a graph and inserting nodes and links.
 * <p>
 * Links are represented implicitly by the lists of parents, children and
 * siblings of each node. Links can be explicitly represented as objects of
 * class {@code LabelledLink}.
 * Links are implicit until the method {@code makeLinksExplicit} is
 * invoked.
 * Explicit links do not substitute implicit links. In fact, an explicit link
 * implies the existence of an implicit link.
 *
 * @author manuel
 * @author fjdiez
 * @author ibermejo
 * @version 1.1
 * invariant Two different nodes can not represent the same object
 * @see org.openmarkov.core.model.network.Node
 * @see Link
 * @since OpenMarkov 1.0
 */
public class Graph<T> {

	// Attributes
	private boolean explicitLinks = false;

	private List<T> nodes;

	private Map<T, List<Link<T>>> nodeLinks;

	private Map<T, List<T>> nodeChildren;
	private Map<T, List<T>> nodeParents;
	private Map<T, List<T>> nodeSiblings;

	// Constructor
	public Graph() {
		this.nodes = new ArrayList<>();
		this.nodeLinks = new HashMap<>();
		this.nodeChildren = new HashMap<>();
		this.nodeParents = new HashMap<>();
		this.nodeSiblings = new HashMap<>();
	}

	// Methods
	public List<T> getChildren(T node) {
		return (nodeChildren.containsKey(node)) ? new ArrayList<>(nodeChildren.get(node)) : new ArrayList<T>();
	}

	public List<T> getParents(T node) {
		return (nodeParents.containsKey(node)) ? new ArrayList<>(nodeParents.get(node)) : new ArrayList<T>();
	}

	public List<T> getSiblings(T node) {
		return (nodeSiblings.containsKey(node)) ? new ArrayList<>(nodeSiblings.get(node)) : new ArrayList<T>();
	}

	public int getNumChildren(T node) {
		return (nodeChildren.containsKey(node)) ? nodeChildren.get(node).size() : 0;
	}

	public int getNumParents(T node) {
		return (nodeParents.containsKey(node)) ? nodeParents.get(node).size() : 0;
	}

	public int getNumSiblings(T node) {
		return (nodeSiblings.containsKey(node)) ? nodeSiblings.get(node).size() : 0;
	}

	public int getNumNeighbors(T node) {
		return getNumParents(node) + getNumChildren(node) + getNumSiblings(node);
	}

	public List<T> getNeighbors(T node) {
		List<T> neighbors = new ArrayList<>();
		if (nodeChildren.containsKey(node))
			neighbors.addAll(nodeChildren.get(node));
		if (nodeParents.containsKey(node))
			neighbors.addAll(nodeParents.get(node));
		if (nodeSiblings.containsKey(node))
			neighbors.addAll(nodeSiblings.get(node));

		return neighbors;
	}

	/**
	 * Returns if node1 is child of node2
	 *
	 * @param node1 First node
	 * @param node2 Second node
	 * @return True if node1 is child of node2
	 */
	public boolean isChild(T node1, T node2) {
		return nodeChildren.containsKey(node2) && nodeChildren.get(node2).contains(node1);
	}

	/**
	 * Returns if node1 is parent of node2
	 *
	 * @param node1 First node
	 * @param node2 Second node
	 * @return True if node1 is parent of node2
	 */
	public boolean isParent(T node1, T node2) {
		return nodeParents.containsKey(node2) && nodeParents.get(node2).contains(node1);
	}

	/**
	 * Returns whether node1 and node2 are siblings
	 *
	 * @param node1 First node
	 * @param node2 Second node
	 * @return True if node1 and node2 are siblings
	 */
	public boolean isSibling(T node1, T node2) {
		return nodeSiblings.containsKey(node2) && nodeSiblings.get(node2).contains(node1);
	}

	/**
	 * Returns whether node1 and node2 are neighbors
	 *
	 * @param node1 First node
	 * @param node2 Second node
	 * @return True if node1 and node2 are neighbors
	 */
	public boolean isNeighbor(T node1, T node2) {
		return isParent(node1, node2) || isChild(node1, node2) || isSibling(node1, node2);
	}

	/**
	 * @return explicitLinks {@code boolean}.
	 */
	public boolean hasExplicitLinks() {
		return explicitLinks;
	}

	/**
	 * @return Number of nodes in the graph
	 */
	public int getNumNodes() {
		return getNodes().size();
	}

	private void addLink(Link<T> link) {
		nodeLinks.get(link.getNode1()).add(link);
		nodeLinks.get(link.getNode2()).add(link);
	}

	/**
	 * Inserts a link between {@code node1} and {@code node2}. {@code node1} and <code>node2</code> belongs to this
	 *
	 * @param node1    {@code Node}
	 * @param node2    {@code Node}
	 * @param directed {@code boolean}
	 * {@code graph}
	 * @return Link
	 */
	public Link<T> addLink(T node1, T node2, boolean directed) {
		Link<T> newLink = null;
		if (explicitLinks) {
			newLink = new Link<>(node1, node2, directed);
			addLink(newLink);
		}
		addImplicitLink(node1, node2, directed);

		return newLink;
	}

	/**
	 * Removes a link between two nodes.
	 *
	 * @param node1    {@code Node}
	 * @param node2    {@code Node}
	 * @param directed {@code boolean}
	 */
	public void removeLink(T node1, T node2, boolean directed) {
		if (explicitLinks) { // get the explicit link and remove it
			Link<T> link = getLink(node1, node2, directed);
			if (link != null) {
				removeLink(link); // remove explicit and implicit link
			}
		} else { // remove the implicit link
			removeImplicitLink(node1, node2, directed);
		}
	}

	/**
	 * Removes an explicit link. Links must be explicit.
	 *
	 * @param link Link&#60;T&#62;
	 */
	public void removeLink(Link<T> link) {
		T node1 = link.getNode1();
		T node2 = link.getNode2();

		nodeLinks.get(node1).remove(link);
		nodeLinks.get(node2).remove(link);

		removeImplicitLink(node1, node2, link.isDirected());
	}

	/**
	 * @param node1    {@code Node}
	 * @param node2    {@code Node}
	 * @param directed {@code boolean}
	 * @return The link between node1 and node2, if it exists, otherwise
	 * returns {@code null}
	 */
	public Link<T> getLink(T node1, T node2, boolean directed) {
		makeLinksExplicit(false);
		List<Link<T>> linksNode1 = nodeLinks.get(node1);
		if (linksNode1 != null) {
			for (Link<T> link : linksNode1) {
				if (directed && link.isDirected() && link.getNode2().equals(node2) || !directed && !link.isDirected()
						&& link.contains(node2)) {
					return link;
				}
			}
		}
		return null;
	}

	/**
	 * Creates the explicit links (based on the implicit links).<p> When
	 * {@code createLabelledLinks = true} create explicit links with label
	 * = {@code null}. Otherwise, create unlabeled explicit links.
	 *
	 * @param createLabelledLinks {@code boolean}
	 */
	public void makeLinksExplicit(boolean createLabelledLinks) {
		if (!explicitLinks) {
			for (T node : nodes) {
				nodeLinks.put(node, new LinkedList<Link<T>>());
			}
			for (T node : nodes) {
				List<T> children = nodeChildren.get(node);
				if (children != null) {
					for (T child : children) {
						Link<T> newLink = null;
						if (createLabelledLinks) {
							newLink = new LabelledLink<>(node, child, true, null);
						} else {
							newLink = new Link<>(node, child, true);
						}
						addLink(newLink);
					}
				}
				List<T> siblings = nodeSiblings.get(node);
				if (siblings != null) {
					int auxNode1Index = nodes.indexOf(node);
					for (T sibling : siblings) {
						if (auxNode1Index > nodes.indexOf(sibling)) {
							Link<T> newLink = null;
							if (createLabelledLinks) {
								newLink = new LabelledLink<>(node, sibling, false, null);
							} else {
								newLink = new Link<>(node, sibling, false);
							}
							addLink(newLink);
						}
					}
				}
			}
			this.explicitLinks = true;
		}
	}

	/**
	 * Removes implicit and explicit links to {@code node} from the
	 * neighbors of {@code node}.
	 *
	 * @param node {@code Node}
	 */
	public void removeLinks(T node) {

		if (explicitLinks) {
			List<Link<T>> linksNode = new ArrayList<>();
			linksNode.addAll(nodeLinks.get(node));

			for (Link<T> link : linksNode) {
				removeLink(link);
			}
		}

		List<T> children = nodeChildren.get(node);
		if (children != null) {
			for (T child : children)
				nodeParents.get(child).remove(node);
			children.clear();
		}

		List<T> parents = nodeParents.get(node);
		if (parents != null) {
			for (T parent : getParents(node))
				nodeChildren.get(parent).remove(node);
			parents.clear();
		}

		List<T> siblings = nodeSiblings.get(node);
		if (siblings != null) {
			for (T sibling : siblings)
				nodeSiblings.get(sibling).remove(node);
			siblings.clear();
		}

	}

	/**
	 * @return A clone of the list of nodes ({@code List} of
	 * {@code Node}).
	 */
	public List<T> getNodes() {
		return new ArrayList<>(nodes);
	}

	public List<Link<T>> getLinks(T node) {
		makeLinksExplicit(false);
		return nodeLinks.containsKey(node) ? new ArrayList<>(nodeLinks.get(node)) : new ArrayList<Link<T>>();
	}

	public int getNumLinks(T node) {
		int numLinks = 0;
		if (explicitLinks)
			numLinks = nodeLinks.containsKey(node) ? nodeLinks.get(node).size() : 0;
		else
			numLinks = getNumParents(node) + getNumChildren(node) + getNumSiblings(node);
		return numLinks;
	}

	/**
	 * @return The {@code Graph} explicit links.
	 */
	public List<Link<T>> getLinks() {
		makeLinksExplicit(false);

		List<Link<T>> links = new ArrayList<>();
		for (T node : nodes) {
			for (Link<T> link : nodeLinks.get(node)) {
				if (link.getNode1().equals(node))
					links.add(link);
			}
		}
		return links;
	}

	/**
	 * {@code node1} and {@code node2} belongs to this graph. Otherwise this method always returns {@code false}.
	 * @param node1    {@code Node}.
	 * @param node2    {@code Node}.
	 * @param directed {@code boolean}. If this parameter is true, this
	 *                 method returns {@code true} only if there is a directed path;
	 *                 otherwise, this method returns {@code true} if there is any path.
	 * @return {@code true} if it exists a path between node1 and node2
	 * with a criterion to go from a node to another.
	 */
	public boolean existsPath(T node1, T node2, boolean directed) {
		if ((node1 == null) || (node2 == null)) {
			return false;
		}
		if (node1 == node2) {
			return true;
		}
		int numNodes = nodes.size();
		boolean[] markedNodes = new boolean[numNodes];
		Stack<T> nodesToExpand = new Stack<>();

		for (int i = 0; i < numNodes; i++) {
			markedNodes[i] = false;
		}

		// Mark node1 and put it in the list of nodes to be expanded
		nodesToExpand.push(node1);
		markedNodes[nodes.indexOf(node1)] = true;

		List<T> neighbors = new ArrayList<>();
		while (!nodesToExpand.empty()) {
			T expandableNode = nodesToExpand.pop(); // the top of the stack
			neighbors = (directed) ? getChildren(expandableNode) : getNeighbors(expandableNode);
			if (neighbors.indexOf(node2) != -1) {
				return true; // node2 is in a path from node1
			}
			for (T neighborNode : neighbors) {
				if (!markedNodes[nodes.indexOf(neighborNode)]) {
					nodesToExpand.push(neighborNode);
					markedNodes[nodes.indexOf(neighborNode)] = true;
				}
			}
		}
		return false;
	}

	/**
	 * Adds an undirected link between each pair of nodes in
	 * {@code nodeList} if it did not exist. All nodes in {@code nodeList} belongs to {@code this}.
	 *
	 * @param nodeList {@code ArrayList} of {@code ? extends Node}.
	 */
	public void marry(Collection<T> nodeList) {
		int size = nodeList.size();
		List<T> nodes = new ArrayList<>(nodeList);
		for (int i = 0; i < size - 1; i++) {
			T node_i = nodes.get(i);
			for (int j = i + 1; j < size; j++) {
				T node_j = nodes.get(j);
				if (!isSibling(node_i, node_j)) {
					addLink(node_i, node_j, false);
				}
			}
		}
	}

	/**
	 * @param node {@code Node}
	 */
	public void removeNode(T node) {
		removeLinks(node);
		nodes.remove(node);
	}

	/**
	 * Adds an implicit link by setting cross references between the two nodes. Both nodes must belong to the same graph.
	 *
	 * @param node1    {@code Node}
	 * @param node2    {@code Node}
	 * @param directed {@code boolean}
	 *
	 */
	private void addImplicitLink(T node1, T node2, boolean directed) {
		if (directed) {
			if (!isChild(node2, node1)) {
				if (!nodeChildren.containsKey(node1))
					nodeChildren.put(node1, new LinkedList<T>());
				nodeChildren.get(node1).add(node2);
			}
			if (!isParent(node1, node2)) {
				if (!nodeParents.containsKey(node2))
					nodeParents.put(node2, new LinkedList<T>());
				nodeParents.get(node2).add(node1);
			}
		} else {
			if (!isSibling(node1, node2)) {
				if (!nodeSiblings.containsKey(node1))
					nodeSiblings.put(node1, new LinkedList<T>());
				if (!nodeSiblings.containsKey(node2))
					nodeSiblings.put(node2, new LinkedList<T>());
				nodeSiblings.get(node1).add(node2);
				nodeSiblings.get(node2).add(node1);
			}
		}
	}

	/**
	 * Removes an implicit link by deleting cross references between the two
	 * nodes. The two nodes must belong to the same graph
	 *
	 * @param node1    {@code Node}
	 * @param node2    {@code Node}
	 * @param directed {@code boolean}
	 */
	private void removeImplicitLink(T node1, T node2, boolean directed) {
		if (directed) {
			nodeChildren.get(node1).remove(node2);
			nodeParents.get(node2).remove(node1);
		} else {
			List<T> siblings = nodeSiblings.get(node1);
			if (siblings != null)
				siblings.remove(node2);
			siblings = nodeSiblings.get(node2);
			if (siblings != null)
				siblings.remove(node1);
		}
	}

	/**
	 * @param node {@code T}
	 */
	public void addNode(T node) {
		nodes.add(node);
		if (explicitLinks) {
			nodeLinks.put(node, new LinkedList<Link<T>>());
		}
	}

	//private addLink(Map<T,List<T>> link)

	/**
	 * @return A {@code String} with:
	 * <ol>
	 * <li>Number of nodes.
	 * <li>List of nodes. For each node calls {@code node.toString()}.
	 * </ol>
	 */
	public String toString() {
		StringBuilder buffer = new StringBuilder("Nodes (" + nodes.size() + "): \n");
		for (T node : nodes) {
			buffer.append(node.toString() + "\n");
		}
		buffer.append("Links: \n");
		if (explicitLinks) {
			for (T node : nodeLinks.keySet()) {
				List<Link<T>> links = nodeLinks.get(node);
				for (Link<T> link : links) {
					if (node.equals(link.getNode1()))
						buffer.append(link.toString() + "\n");
				}
			}
		} else {
			for (T node : nodeChildren.keySet()) {
				for (T child : nodeChildren.get(node)) {
					buffer.append(node.toString() + " --> " + child.toString() + "\n");
				}
			}
			for (T node : nodeSiblings.keySet()) {
				int indexNode = nodes.indexOf(node);
				for (T sibling : nodeSiblings.get(node)) {
					if (indexNode < nodes.indexOf(sibling))
						buffer.append(node.toString() + " --- " + sibling.toString() + "\n");
				}
			}
		}
		return buffer.toString();
	}

}
