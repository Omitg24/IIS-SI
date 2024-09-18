/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.model.network.potential.sdag;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.State;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.potential.Potential;
import org.openmarkov.core.model.network.potential.StrategyTree;
import org.openmarkov.core.model.network.potential.treeadd.TreeADDBranch;

// TODO Documentar
public class SDAGStrategyTree extends StrategyTree {

	protected Set<TreeADDBranch> parents;

	public SDAGStrategyTree(Variable topVariable) {
		super(topVariable);
		initializeParents();
	}

	public SDAGStrategyTree(Variable decisionVariable, List<State> optimalStates,
			List<StrategyTree> optimalStrategyTrees) {
		super(decisionVariable, optimalStates, optimalStrategyTrees);
		initializeParents();
		updateParentsBranches();
	}

	public SDAGStrategyTree(Variable decisionVariable, List<State> optimalStates, StrategyTree strategyTree) {
		super(decisionVariable, optimalStates, strategyTree);
		initializeParents();
		updateParentsBranches();
	}

	public SDAGStrategyTree(Variable decisionVariable, List<State> optimalStates) {
		super(decisionVariable, optimalStates);
		initializeParents();
		updateParentsBranches();
	}

	public static StrategyTree buildIntervention(List<StrategyTree> optimalStrategyTrees, Variable decisionVariable,
			List<State> optimalStates) {
		StrategyTree strategyTree = (optimalStrategyTrees.size() > 1) ?
				new SDAGStrategyTree(decisionVariable, optimalStates, optimalStrategyTrees) :
				new SDAGStrategyTree(decisionVariable, optimalStates, optimalStrategyTrees.get(0));

		return strategyTree;
	}

	private void updateParentsBranches() {
		if (branches != null) {
			for (TreeADDBranch branch : branches) {
				SDAGStrategyTree branchInterv = (SDAGStrategyTree) branch.getPotential();
				if (branchInterv != null) {
					branchInterv.addParent(branch);
				}
			}
		}
	}

	private void initializeParents() {
		parents = new HashSet<>();

	}

	/**
	 * Add {@code Intervention} to edges of this intervention
	 *
	 * @param strategyTree Strategy tree
	 */
	@Override public StrategyTree concatenate(StrategyTree strategyTree) {

		return concatenate(null, (SDAGStrategyTree) strategyTree);

	}

	/**
	 * @param branchParent Parent branch
	 * @param intervention Intervention
	 * @return It concatenates taking care of the coalescence
	 */
	public StrategyTree concatenate(TreeADDBranch branchParent, SDAGStrategyTree intervention) {

		SDAGStrategyTree result;
		Set<TreeADDBranch> auxParents = new HashSet<>();

		auxParents.addAll(parents);
		auxParents.remove(branchParent);

		//TODO Analize what happens if next line is uncommented, replacing its next line
		//The line "boolean hasOtherParents = true;" is correct, but it could be inefficient in huge networks
		//boolean hasOtherParents = auxParents.size() > 0;
		boolean hasOtherParents = true;

		if (hasOtherParents) {
			result = copy().carefreeConcatenate(intervention);
		} else {
			for (TreeADDBranch branch : branches) {
				SDAGStrategyTree branchIntervention = getCoalescedInterventionBranch(branch);
				if (branchIntervention == null) {
					branch.setPotential(intervention);
					intervention.parents.add(branch);
				} else {
					branchIntervention.concatenate(branch, intervention);
				}
			}
			result = this;
		}
		return result;
	}

	/**
	 * @param intervention Intervention
	 * @return It concatenates not taking care of the coalescence, because the receiving object is a copy
	 */
	private SDAGStrategyTree carefreeConcatenate(SDAGStrategyTree intervention) {

		for (TreeADDBranch branch : branches) {
			SDAGStrategyTree branchIntervention = getCoalescedInterventionBranch(branch);
			if (branchIntervention == null) {
				branch.setPotential(intervention);
				intervention.parents.add(branch);
			} else {
				branchIntervention.carefreeConcatenate(intervention);
			}
		}
		return this;
	}

	@Override public SDAGStrategyTree copy() {
		SDAGStrategyTree newInt = new SDAGStrategyTree(this.getRootVariable());

		if (this.getBranches() != null) {
			// Create branches
			for (TreeADDBranch branch : getBranches()) {
				List<State> newStates = new ArrayList<>();
				newStates.addAll(branch.getStates());
				SDAGStrategyTree interv = getCoalescedInterventionBranch(branch);
				SDAGStrategyTree intervCopy;
				if (interv != null) {
					intervCopy = interv.copy();
				} else {
					intervCopy = null;
				}
				TreeADDBranch newBranch = new TreeADDBranch(newStates, branch.getRootVariable(), intervCopy, null);
				newInt.addBranch(newBranch);
				if (intervCopy != null) {
					intervCopy.addParent(newBranch);
				}
			}
		}
		return newInt;

	}

	public SDAGStrategyTree getCoalescedInterventionBranch(TreeADDBranch branch) {
		return (SDAGStrategyTree) getInterventionBranch(branch);
	}

	private void addParent(TreeADDBranch newBranch) {
		parents.add(newBranch);

	}

	@Override public Potential deepCopy(ProbNet copyNet) {
		SDAGStrategyTree potential = (SDAGStrategyTree) super.deepCopy(copyNet);
		Set<TreeADDBranch> newParents = new HashSet<>();
		Iterator<TreeADDBranch> iterator = this.parents.iterator();

		while (iterator.hasNext()) {
			newParents.add(iterator.next().deepCopy(copyNet));
		}

		potential.parents = newParents;

		return potential;
	}
}
