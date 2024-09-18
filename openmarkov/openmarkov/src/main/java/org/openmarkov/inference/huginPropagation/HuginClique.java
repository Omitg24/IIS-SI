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
import org.openmarkov.core.model.network.potential.Potential;
import org.openmarkov.core.model.network.potential.TablePotential;
import org.openmarkov.core.model.network.potential.operation.DiscretePotentialOperations;
import org.openmarkov.inference.huginPropagation.ClusterPropagation.StorageLevel;

/**
 * Contains a set of variables. The main difference with
 * openmarkov.inference.ClusterOfVariables is in method
 * openmarkov.inference.ClusterOfVariables#distributeEvidence
 *
 * @author marias
 * @author fjdiez
 */
public class HuginClique extends ClusterOfVariables {
	// Attributes
	protected static String clusterNamePrefix = "Clique.";
	// Constructor

	/**
	 * @param huginForest        <code>HuginForest</code>
	 * @param cliqueVariables    <code>ArrayList</code> of <code>Variable</code>
	 * @param separatorVariables <code>ArrayList</code> of <code>Variable</code>
	 */
	public HuginClique(HuginForest huginForest, List<Variable> cliqueVariables, List<Variable> separatorVariables) {
		super(huginForest, cliqueVariables);
		name = clusterNamePrefix + (huginForest.getNumNodes() - 1);
		this.separatorVariables = separatorVariables;
	}

	// Methods

	/**
	 * @param storageLevel <code>int</code>
	 */
	public void distributeEvidence(StorageLevel storageLevel) {
		for (ClusterOfVariables childClique : getChildren()) {
			Potential upgoingChildMessage = childClique.getUpgoingMessage(storageLevel);
			Potential posteriorMarginalized = DiscretePotentialOperations
					.marginalize(posteriorPotential, childClique.getSeparatorVariables());
			TablePotential division = DiscretePotentialOperations.divide(posteriorMarginalized, upgoingChildMessage);
			List<TablePotential> potentials = new ArrayList<TablePotential>();
			potentials.add(division);
			potentials.add(childClique.getPosteriorPotential(storageLevel));
			childClique.setPosteriorPotential(DiscretePotentialOperations.multiply(potentials));
			childClique.distributeEvidence(storageLevel);
		}
	}

	/**
	 * @param variablesList <code>ArrayList</code> of <code>Variable</code>
	 * @return <code>true</code> if all the variables in
	 * <code>variablesList</code> are included in the clique variables
	 */
	public boolean containsAll(List<Variable> variablesList) {
		if (variables.containsAll(variablesList)) {
			return true;
		}
		return false;
	}
}
