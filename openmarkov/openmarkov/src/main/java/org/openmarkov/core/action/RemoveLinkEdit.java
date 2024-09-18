/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.action;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openmarkov.core.exception.NonProjectablePotentialException;
import org.openmarkov.core.exception.WrongCriterionException;
import org.openmarkov.core.model.graph.Link;
import org.openmarkov.core.model.network.Node;
import org.openmarkov.core.model.network.NodeType;
import org.openmarkov.core.model.network.PartitionedInterval;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.State;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.potential.Potential;
import org.openmarkov.core.model.network.potential.SumPotential;
import org.openmarkov.core.model.network.potential.UniformPotential;

@SuppressWarnings("serial") public class RemoveLinkEdit extends BaseLinkEdit {

	/**
	 * Resulting link of addition or removal.
	 */
	protected Link<Node> link;
	/**
	 * The last {@code Potential} of the second node before the edition
	 * /**
	 * parent node
	 */
	protected Node node1;
	/**
	 * child node
	 */
	protected Node node2;
	/**
	 * The new {@code Potential} of the second node
	 */
	protected List<Potential> newPotentials = new ArrayList<>();
	protected List<Potential> oldPotentials;
	private Logger logger;
	private boolean updatePotentials;
	// Constructor

	/**
	 * @param probNet    {@code ProbNet}
	 * @param variable1  {@code Variable}
	 * @param variable2  {@code Variable}
	 * @param isDirected {@code boolean}
	 * @param updatePotentials {@code boolean}
	 */
	public RemoveLinkEdit(ProbNet probNet, Variable variable1, Variable variable2, boolean isDirected,
			boolean updatePotentials) {
		super(probNet, variable1, variable2, isDirected);

		node1 = probNet.getNode(variable1);
		node2 = probNet.getNode(variable2);

		this.updatePotentials = updatePotentials;
		this.link = null;
		this.logger = LogManager.getLogger(RemoveLinkEdit.class);
	}

	public RemoveLinkEdit(ProbNet probNet, Variable variable1, Variable variable2, boolean isDirected) {
		this(probNet, variable1, variable2, isDirected, true);
	}

	@Override public void doEdit() {
		if (probNet.hasExplicitLinks()) {
			this.link = probNet.getLink(node1, node2, isDirected);
		}
		probNet.removeLink(node1, node2, isDirected);
		if (updatePotentials) {
			this.oldPotentials = node2.getPotentials();
			if (node2.getNodeType() == NodeType.UTILITY) {// supervalue nodes

				if (node2.onlyNumericalParents()) {// utility and numerical parents sum
					for (Potential oldPotential : oldPotentials) {
						// Update potential
						List<Variable> variables = oldPotential.getVariables();
						variables.remove(node1.getVariable());
						Potential newPotential = new SumPotential(variables, oldPotential.getPotentialRole());
						newPotentials.add(newPotential);
					}
				} else if (!node2.onlyNumericalParents()) {//mixture of finite states and numerical Uniform
					for (Potential oldPotential : oldPotentials) {
						// Update potential
						List<Variable> variables = new ArrayList<>();
						variables.addAll(oldPotential.getVariables());
						variables.remove(node1.getVariable());
						Potential newPotential = new UniformPotential(variables, oldPotential.getPotentialRole());
						newPotentials.add(newPotential);
					}
				}
				node2.setPotentials(newPotentials);
			} else {

				// Update potentials
				this.oldPotentials = node2.getPotentials();
				for (Potential oldPotential : oldPotentials) {
					Potential newPotential = oldPotential.removeVariable(node1.getVariable());
					// TODO - Implements validate for all potential types, at this moment it always return true.
					/*
					if (!newPotential.validate(node2, newPotential.getVariables(), newPotential.getPotentialRole())){
						newPotential = new UniformPotential(newPotential.getVariables(), newPotential.getPotentialRole());
					};
					 */

					// Temporal patch to be removed when the above TO-DO is implemented
					try {
						if (Arrays.stream(newPotential.getCPT().values).sum() == 0){
							newPotential = new UniformPotential(newPotential.getVariables(), newPotential.getPotentialRole());
						}
					} catch (NonProjectablePotentialException | WrongCriterionException e) {
						e.printStackTrace();
					}

					newPotentials.add(newPotential);
				}
				node2.setPotentials(newPotentials);
			}
		}
	}

	@Override public void undo() {
		super.undo();

		if (updatePotentials) {
			node2.setPotentials(oldPotentials);
		}
		try {
			probNet.addLink(variable1, variable2, isDirected);
			if (probNet.hasExplicitLinks()) {
				Link<Node> newLink = probNet.getLink(node1, node2, isDirected);
				if (link != null && newLink != null) {
					Potential restrictionsPotential = link.getRestrictionsPotential();
					newLink.setRestrictionsPotential(restrictionsPotential);
					List<State> revealingStates = link.getRevealingStates();
					newLink.setRevealingStates(revealingStates);
					List<PartitionedInterval> revealingIntervals = link.getRevealingIntervals();
					newLink.setRevealingIntervals(revealingIntervals);
				}
			}
		} catch (Exception e) {
			logger.fatal(e);
		}
	}

	/**
	 * Method to compare two RemoveLinkEdits comparing the names of
	 * the source and destination variable alphabetically.
	 *
	 * @param obj RemoveLinkEdit to be compared
	 * @return Result of comparison
	 */
	public int compareTo(RemoveLinkEdit obj) {
		int result;

		if ((
				result = variable1.getName().compareTo(obj.getVariable1().
						getName())
		) != 0)
			return result;
		if ((
				result = variable2.getName().compareTo(obj.getVariable2().
						getName())
		) != 0)
			return result;
		else
			return 0;
	}

	@Override public String getOperationName() {
		return "Remove link";
	}

	@Override public BaseLinkEdit getUndoEdit() {
		return new AddLinkEdit(getProbNet(), getVariable1(), getVariable2(), isDirected());
	}

}
