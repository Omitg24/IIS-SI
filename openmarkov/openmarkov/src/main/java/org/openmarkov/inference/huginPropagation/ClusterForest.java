/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.inference.huginPropagation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openmarkov.core.model.graph.Graph;
import org.openmarkov.core.model.graph.Link;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.potential.Potential;

/**
 * Represent a tree of <code>ClustersOfVariable</code>.
 *
 * @author Manuel Arias
 * @author Francisco Javier Diez
 * @version 1.0
 * @since OpenMarkov 1.0
 */
public abstract class ClusterForest extends Graph<ClusterOfVariables> {
	// Attributes
	protected int numClusters;
	protected List<ClusterOfVariables> rootClusters;
	/**
	 * this <code>Map</code> stores the smallest cluster containing each
	 * variable.
	 */
	protected Map<Variable, ClusterOfVariables> variables2Clusters;

	// Constructors

	/**
	 * Empty constructor
	 */
	public ClusterForest() {
		rootClusters = new ArrayList<ClusterOfVariables>();
		variables2Clusters = new HashMap<Variable, ClusterOfVariables>();
	}

	/**
	 * @param rootCluster <code>ClusterOfVariables</code>
	 */
	public ClusterForest(ClusterOfVariables rootCluster) {
		this();
		rootClusters.add(rootCluster);
	}

	// Methods

	/**
	 * @return <code>ArrayList</code> of <code>ClusterOfVariables</code>
	 */
	public List<ClusterOfVariables> getNodes() {
		List<ClusterOfVariables> rootCliques = new ArrayList<>(getRootClusters());
		List<ClusterOfVariables> cliques = new ArrayList<ClusterOfVariables>();
		while (rootCliques.size() != 0) {
			int lastCliqueIndex = rootCliques.size() - 1;
			ClusterOfVariables cliqueToExpand = rootCliques.get(lastCliqueIndex);
			rootCliques.remove(lastCliqueIndex);
			List<ClusterOfVariables> children = cliqueToExpand.getChildren();
			if (children.size() >= 0) {
				rootCliques.addAll(children);
			}
			cliques.add(cliqueToExpand);
		}
		return cliques;
	}

	/**
	 * @param cluster <code>ClusterOfVariables</code>.
	 */
	public void addCluster(ClusterOfVariables cluster) {
		rootClusters.add(cluster);
	}

	/**
	 * @param variable <code>Variable</code>.
	 * @return One of the clusters containing the variable; more precisely, the
	 * cluster associated to that variable in the <code>hashMap</code>
	 * variables2Clusters.
	 */
	public ClusterOfVariables getCluster(Variable variable) {
		return variables2Clusters.get(variable);
	}

	/**
	 * @return The collection stored in this class: <code>ArrayList</code> of
	 * <code>ClusterOfVariables</code>
	 */
	public List<ClusterOfVariables> getRootClusters() {
		return rootClusters;
	}

	/**
	 * @param variable  <code>Variable</code>.
	 * @param potential <code>Potential</code>.
	 */
	public void introduceFindingPotential(Variable variable, Potential potential) {
	}

	public void increaseNumNodes() {
		numClusters++;
	}

	/**
	 * @param cluster <code>ClusterOfVariables</code>.
	 */
	public void setClusterAsRoot(ClusterOfVariables cluster) {
		rootClusters.add(cluster);
	}

	/**
	 * @return numClusters <code>int</code>
	 */
	public int getNumNodes() {
		return numClusters;
	}

	/**
	 * Generates a string with root clusters (more than one in construction
	 * time) and non root clusters.
	 *
	 * @return <code>String</code>
	 */
	public String toString() {
		StringBuffer out = new StringBuffer();
		out.append("Root cluster");
		if (rootClusters.size() > 1) {
			out.append("s");
		}
		out.append(":");

		for (ClusterOfVariables cluster : rootClusters) {
			out.append("\n  " + cluster.toString());
		}
		out.append("\nNodes (" + getNodes().size() + "):");
		for (ClusterOfVariables clique : getNodes()) {
			out.append("\n  " + clique.toString());
		}
		out.append("\nLinks (" + getLinks().size() + "):");
		for (Link<ClusterOfVariables> link : getLinks()) {
			out.append("\n  " + link.getNode1().getName() + " -> " + link.getNode2().getName());
		}
		return out.toString();
	}
}