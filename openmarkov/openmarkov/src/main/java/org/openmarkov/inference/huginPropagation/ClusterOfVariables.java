/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.inference.huginPropagation;

import java.util.ArrayList;
import java.util.List;

import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.VariableType;
import org.openmarkov.core.model.network.potential.Potential;
import org.openmarkov.core.model.network.potential.TablePotential;
import org.openmarkov.core.model.network.potential.operation.DiscretePotentialOperations;
import org.openmarkov.core.model.network.potential.operation.PotentialOperations;
import org.openmarkov.inference.huginPropagation.ClusterPropagation.StorageLevel;

/**
 * A <code>ClusterOfVariables</code> is a node in a <code>ClusterForest</code>.
 * <p>
 * This is in general a hypernode, in the sense that it contains a set of
 * variables, and each of these variables is represented by a <code>Node</code>.
 *
 * @author marias
 * @author fjdiez
 */
public abstract class ClusterOfVariables {
	// Attributes for performance test
	public static int collectEvidenceInvocations = 0;
	public static int distributeEvidenceInvocations = 0;

	/**
	 * Used to form the cluster's name.
	 */
	protected static String clusterNamePrefix = "Cluster.";
	/**
	 * Variables in common with another <code>ClusterOfVariables</code>.
	 */
	public List<Variable> separatorVariables;
	/**
	 * Cluster's name.
	 */
	protected String name;
	/**
	 * Potentials whose variables are all in the cluster.
	 */
	protected List<TablePotential> priorPotentials;
	/**
	 * An <code>evidencePotential</code> has only one variable with a
	 * probability of 1.0 one and only one state and 0.0 in the others.
	 */
	protected List<TablePotential> evidencePotentials;
	/**
	 * Resulting potential of multiplying prior and evidence potentials by the
	 * messages received from all its neighbors.
	 */
	protected TablePotential posteriorPotential = null;
	/**
	 * Variables in this cluster
	 */
	protected List<Variable> variables;
	/**
	 * Message created in the collect evidence phase, it goes from children to
	 * parents with the separator variables.
	 */
	protected TablePotential upgoingMessage = null;
	/**
	 * Message created in the distribute evidence phase, it goes from parents to
	 * children.
	 */
	protected TablePotential downgoingMessage = null;
	protected int clusterSize = 0;
	private ClusterForest clusterForest = null;

	// Constructor

	/**
	 * @param clusterForest <code>ClusterForest</code>.
	 * @param variables     <code>ArrayList</code> of <code>Variable</code>s.
	 */
	public ClusterOfVariables(ClusterForest clusterForest, List<Variable> variables) {
		this.clusterForest = clusterForest;
		this.variables = variables;
		// super(null, variables, NodeType.CLUSTER);
		clusterForest.increaseNumNodes();
		name = clusterNamePrefix + (clusterForest.getNumNodes() - 1);
		priorPotentials = new ArrayList<TablePotential>();
		evidencePotentials = new ArrayList<TablePotential>();
		separatorVariables = new ArrayList<Variable>();
	}

	// Methods

	/**
	 * @return <code>ArrayList</code> of <code>ClusterOfVariables</code>
	 */
	public List<ClusterOfVariables> getChildren() {
		return clusterForest.getChildren(this);
	}

	/**
	 * @return <code>ArrayList</code> of <code>ClusterOfVariables</code>
	 */
	public List<ClusterOfVariables> getParents() {
		return clusterForest.getParents(this);
	}

	/**
	 * @return The <code>Object</code> associated to this node that contains an
	 * <code>ArrayList</code> of <code>Variable</code>s.
	 */
	public List<Variable> getVariables() {
		return new ArrayList<Variable>(variables);
	}

	/**
	 * @param potential <code>Potential</code>
	 */
	public void addPriorPotential(TablePotential potential) {
		priorPotentials.add(potential);
	}

	/**
	 * @return priorPotentials <code>ArrayList</code> of <code>Potential</code>
	 * s.
	 */
	public List<TablePotential> getAssignedPotentials() {
		return priorPotentials;
	}

	/**
	 * Calculates the marginalized multiplication of: <code>priorPotentials,
	 * evidencePotentials</code> and the recursively collected evidence from
	 * the children of this <code>ClusterOfVariables</code>.
	 *
	 * @param storageLevel If its value is 2 the collected evidence is stored in
	 *                     the <code>posteriorPotential</code> without being marginalized
	 *                     <code>int</code>
	 * @return The marginalized multiplication (<code>Potential</code>).
	 */
	public TablePotential collectEvidence(StorageLevel storageLevel) {
		if (upgoingMessage != null) { // It has been calculated before
			return upgoingMessage;
		}
		collectEvidenceInvocations++;
		// adds the prior potentials and evidence potentials
		List<TablePotential> potentials = new ArrayList<>(priorPotentials);
		for (Potential pot : evidencePotentials) {
			potentials.add((TablePotential) pot);
		}
		// recursively invokes collectEvidence on its children
		// and add the collected potentials
		for (ClusterOfVariables child : getChildren()) {
			potentials.add(child.collectEvidence(storageLevel));
		}
		boolean isRootClique = separatorVariables.size() == 0;
		TablePotential collectedEvidence = null;
		posteriorPotential = DiscretePotentialOperations.multiply(potentials);
		upgoingMessage = (isRootClique) ?
				posteriorPotential :
				DiscretePotentialOperations.marginalize(posteriorPotential, separatorVariables);
		collectedEvidence = upgoingMessage;
		switch (storageLevel) {
		case NO_STORAGE: {
			// Delete upgoing message
			upgoingMessage = null;
			break;
		}
		case MEDIUM: {
			// Delete posterior potential
			posteriorPotential = null;
			break;
		}
		case FULL: {
			// Do nothing
			break;
		}
		}
		return collectedEvidence;
	}

	/**
	 * Sends a message to each child. The message is the multiplication of:
	 * <code>priorPotentials, evidencePotentials</code> and the upgoing messages
	 * from its other children
	 *
	 * @param storageLevel the amount of intermediate operation that are stored.
	 */
	public void distributeEvidence(StorageLevel storageLevel) {
		// stores the product of priorPotentials, evidencePotentials, and
		// the downgoingMessage
		TablePotential intermediateProduct = getIntermediateProduct();
		// sends a downgoingMessage to each child;
		// this message is the product of the intermediateProduct multiplied
		// by the upgoing messages from its other children
		List<ClusterOfVariables> children = getChildren();
		List<ClusterOfVariables> otherChildren = null;
		for (ClusterOfVariables child : children) {
			List<TablePotential> potentials = new ArrayList<TablePotential>();
			potentials.add(intermediateProduct);
			otherChildren = new ArrayList<ClusterOfVariables>();
			otherChildren.addAll(children);
			otherChildren.remove(child);
			for (ClusterOfVariables otherChild : otherChildren) {
				potentials.add(otherChild.getUpgoingMessage(storageLevel));
			}
			child.setDowngoingPotential(
					DiscretePotentialOperations.multiplyAndMarginalize(potentials, child.getSeparatorVariables()));
			child.setPosteriorPotential(child.getPosteriorPotential(storageLevel));
		}
	}

	/**
	 * @return The product of prior potentials, evidence potentials and the
	 * downgoing message if it exists (does not exist in root clusters).
	 */
	private TablePotential getIntermediateProduct() {
		// adds the prior potentials and evidence potentials
		List<TablePotential> potentials = new ArrayList<TablePotential>();
		potentials.addAll(priorPotentials);
		potentials.addAll(evidencePotentials);
		// downgoingMessage is null for root clusters
		if (downgoingMessage != null) {
			potentials.add(downgoingMessage);
		}
		// stores the product of priorPotentials, evidencePotentials, and
		// the downgoingMessage
		if (potentials.size() == 0) {
			return null;
		}
		return DiscretePotentialOperations.multiply(potentials);
	}

	/**
	 * @param child        <code>ClusterOfVariables</code>.
	 * @param storageLevel <code>int</code>.
	 * @return The <code>Potential</code> sended to <code>child</code>.
	 */
	protected Potential getDowngoingPotential(ClusterOfVariables child, StorageLevel storageLevel) throws Exception {
		TablePotential intermediateProduct = getIntermediateProduct();
		// sends a downgoingMessage to each child;
		// this message is the product of the intermediateProduct multiplied
		// by the upgoing messages from its other children
		List<ClusterOfVariables> children = getChildren();
		List<ClusterOfVariables> otherChildren = null;
		List<TablePotential> potentials = new ArrayList<TablePotential>();
		if (intermediateProduct != null) {
			potentials.add(intermediateProduct);
		}
		otherChildren = new ArrayList<ClusterOfVariables>();
		otherChildren.addAll(children);
		otherChildren.remove(child);
		for (ClusterOfVariables otherChild : otherChildren) {
			potentials.add(otherChild.getUpgoingMessage(storageLevel));
		}
		return PotentialOperations.multiplyAndMarginalize(potentials, child.getSeparatorVariables());
	}

	/**
	 * @param storageLevel <code>int</code>.
	 * @return posteriorPotential <code>Potential</code>.
	 */
	public TablePotential getPosteriorPotential(StorageLevel storageLevel) {
		if (posteriorPotential != null) {
			return posteriorPotential;
		}
		// adds the prior potentials and evidence potentials
		List<TablePotential> potentials = new ArrayList<TablePotential>(priorPotentials);
		potentials.addAll(evidencePotentials);
		// recursively invokes collectEvidence on its children
		// and add the collected potentials
		List<ClusterOfVariables> children = getChildren();
		for (ClusterOfVariables child : children) {
			potentials.add(child.collectEvidence(storageLevel));
		}
		return DiscretePotentialOperations.multiply(potentials);
	}

	/**
	 * @param posteriorPotential <code>Potential</code>.
	 */
	public void setPosteriorPotential(TablePotential posteriorPotential) {
		this.posteriorPotential = posteriorPotential;
	}

	/**
	 * @param potential <code>Potential</code>.
	 */
	public void addEvidencePotential(TablePotential potential) {
		evidencePotentials.add(potential);
	}

	/**
	 * @return separatorVariables <code>ArrayList</code> of <code>Variable</code>
	 * s.
	 */
	public List<Variable> getSeparatorVariables() {
		return separatorVariables;
	}

	/**
	 * @param separatorVariables <code>ArrayList</code> of <code>Variable</code>
	 *                           s. Must be included in <code>cliqueVariables</code>.
	 */
	public void setSeparatorVariables(List<Variable> separatorVariables) {
		this.separatorVariables = separatorVariables;
	}

	/**
	 * @param storageLevel <code>int</code>.
	 * @return upgoingMessage <code>Potential</code>.
	 */
	public TablePotential getUpgoingMessage(StorageLevel storageLevel) {
		if (upgoingMessage != null) {
			return upgoingMessage;
		}
		return collectEvidence(storageLevel);
	}

	/**
	 * @return name <code>String</code>.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Multiplies the priorPotentials and replaces them with the product.
	 * It does the same recursively in its children
	 *
	 */
	public void compilePriorPotentials() {
		if (separatorVariables.size() == 0) { // root clique, without separator
			if (priorPotentials.size() > 1) {
				TablePotential priorPotential = DiscretePotentialOperations.multiply(priorPotentials);
				priorPotentials.clear();
				priorPotentials.add(priorPotential);
			}
		} else { // no root clique, with separator
			TablePotential priorPotential = DiscretePotentialOperations
					.multiplyAndMarginalize(priorPotentials, getVariables());
			priorPotentials.clear();
			priorPotentials.add(priorPotential);
		}
		// recursive call
		List<ClusterOfVariables> children = getChildren();
		for (ClusterOfVariables child : children) {
			child.compilePriorPotentials();
		}
	}

	/**
	 * Overrides <code>toString</code> method. Mainly for test purposes.
	 */
	public String toString() {
		StringBuffer out = new StringBuffer(name);
		out.append(": {");
		for (int i = 0; i < variables.size(); i++) {
			out.append(variables.get(i).getName());
			if (i < variables.size() - 1) {
				out.append(", ");
			}
		}
		out.append("} - ");
		if ((separatorVariables != null) && (separatorVariables.size() > 0)) {
			out.append(((separatorVariables.size() == 1) ? "Separator" : "Separators") + ": {");
			for (int i = 0; i < separatorVariables.size(); i++) {
				out.append(separatorVariables.get(i).getName());
				if (i < separatorVariables.size() - 1) {
					out.append(", ");
				}
			}
			out.append("}");
		} else {
			out.append("No separator");
		}
		out.append(" - ");
		if (posteriorPotential != null) {
			out.append("Posterior potential: ");
			out.append(posteriorPotential.toShortString());
			out.append(" - ");
		}
		if (priorPotentials != null && priorPotentials.size() > 0) {
			out.append("Prior potentials (" + priorPotentials.size() + "): ");
			for (int i = 0; i < priorPotentials.size(); i++) {
				out.append(priorPotentials.get(i).toShortString());
				if (i < priorPotentials.size() - 1) {
					out.append(", ");
				}
			}
			out.append(" - ");
		}
		if (evidencePotentials != null && evidencePotentials.size() > 0) {
			out.append("Evidence potentials (" + evidencePotentials.size() + "): ");
			for (int i = 0; i < evidencePotentials.size(); i++) {
				out.append(evidencePotentials.get(i).toShortString());
				if (i < evidencePotentials.size() - 1) {
					out.append(", ");
				}
			}
			out.append(" - ");
		}
		if (upgoingMessage != null) {
			out.append("Upgoing message: ");
			out.append(upgoingMessage.toShortString());
			out.append(" - ");
		}
		if (downgoingMessage != null) {
			out.append("Downgoing message: ");
			out.append(downgoingMessage.toShortString());
			out.append(" - ");
		}
		return out.toString();
	}

	/**
	 * @param potential <code>Potential</code>.
	 */
	protected void setDowngoingPotential(TablePotential potential) {
		downgoingMessage = potential;
	}

	/**
	 * @return Clique size = product of number of states of variable (all of
	 * them discrete) <code>int</code>
	 */
	public int size() {
		if (clusterSize == 0) {
			clusterSize = 1;
			for (Variable variable : variables) {
				if (variable.getVariableType() == VariableType.FINITE_STATES) {
					clusterSize *= variable.getNumStates();
				}
			}
		}
		return clusterSize;
	}
}
