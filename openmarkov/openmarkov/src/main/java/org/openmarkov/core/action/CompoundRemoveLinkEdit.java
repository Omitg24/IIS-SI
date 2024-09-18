/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.action;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openmarkov.core.model.network.Node;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.potential.Potential;
import org.openmarkov.core.model.network.potential.operation.PotentialOperations;

@SuppressWarnings("serial") public class CompoundRemoveLinkEdit extends CompoundPNEdit {

	// Attributes
	protected Variable variable1;

	protected Variable variable2;

	protected boolean isDirected;

	private Logger logger;

	// Constructor

	/**
	 * @param probNet    {@code ProbNet}
	 * @param variable1  {@code Variable}
	 * @param variable2  {@code Variable}
	 * @param isDirected {@code boolean}
	 */
	public CompoundRemoveLinkEdit(ProbNet probNet, Variable variable1, Variable variable2, boolean isDirected) {
		super(probNet);
		this.variable1 = variable1;
		this.variable2 = variable2;
		this.isDirected = isDirected;
		this.logger = LogManager.getLogger(CompoundPNEdit.class);
	}

	// Methods
	@Override public void generateEdits() {
		if (isDirected) {
			generateEditsDirectedLink();
		} else {
			generateEditsUndirectedLink();
		}
	}

	private void generateEditsUndirectedLink() {
		addEdit(new RemoveLinkEdit(probNet, variable1, variable2, isDirected));
	}

	private void generateEditsDirectedLink() {
		Node node2 = probNet.getNode(variable2);
		List<Potential> potentials = node2.getPotentials();
		for (Potential potential : potentials) {
			List<Variable> potentialVariables = potential.getVariables();
			if (potentialVariables.contains(variable1)) {
				potentialVariables = new ArrayList<>(potentialVariables);
				potentialVariables.remove(variable1);
				try {
					Potential marginalizedPotential = PotentialOperations.marginalize(potential, potentialVariables);
					addEdit(new PotentialChangeEdit(probNet, marginalizedPotential, potential));
				} catch (Exception e) {
					logger.fatal(e);
				}
			}
		}
		addEdit(new RemoveLinkEdit(probNet, variable1, variable2, isDirected));
	}

	public String toString() {
		StringBuilder buffer = new StringBuilder("CompoundRemoveLinkEdit: ");
		buffer.append(variable1.getName());
		if (isDirected) {
			buffer.append(" --> ");
		} else {
			buffer.append(" --- ");
		}
		buffer.append(variable2.getName()).append("\n");
		return buffer.toString();
	}

}
