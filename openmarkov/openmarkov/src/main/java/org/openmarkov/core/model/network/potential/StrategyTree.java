/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.model.network.potential;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openmarkov.core.exception.IncompatibleEvidenceException;
import org.openmarkov.core.exception.InvalidStateException;
import org.openmarkov.core.exception.NodeNotFoundException;
import org.openmarkov.core.model.network.EvidenceCase;
import org.openmarkov.core.model.network.Finding;
import org.openmarkov.core.model.network.Node;
import org.openmarkov.core.model.network.PartitionedInterval;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.State;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.potential.sdag.SDAGStrategyTree;
import org.openmarkov.core.model.network.potential.treeadd.Threshold;
import org.openmarkov.core.model.network.potential.treeadd.TreeADDBranch;
import org.openmarkov.core.model.network.potential.treeadd.TreeADDPotential;

// TODO Documentar la clase
public class StrategyTree extends TreeADDPotential {

	// Constructors
	public StrategyTree(List<Variable> variables, Variable topVariable) {
		super(variables, topVariable, PotentialRole.UNSPECIFIED);
		ensureThatAllVariablesAreIncluded();
	}

	/**
	 * Creates an intervention without branches.
	 *
	 * @param topVariable Top variable
	 */
	public StrategyTree(Variable topVariable) {
		this(null, topVariable);
		ensureThatAllVariablesAreIncluded();
	}

	/**
	 * Creates an intervention having as much states in each branch as equal interventions.
	 * The number of states and interventions must be the same.
	 *
	 * @param topVariable   {@code Variable}
	 * @param states        One state for each intervention in the list. {@code List} of {@code State}
	 * @param strategyTrees It is possible that this list contains some equal interventions.
	 *                      {@code List} of {@code Intervention}
	 */
	public StrategyTree(Variable topVariable, List<State> states, List<StrategyTree> strategyTrees) {
		this(null, topVariable);
		List<StrategyTree> distinctStrategyTrees = new ArrayList<>();
		Map<StrategyTree, Set<State>> correspondingStates = new HashMap<>();
		int numInterventions = strategyTrees.size();
		for (int i = 0; i < numInterventions; i++) {
			StrategyTree strategyTree = strategyTrees.get(i);
			int numDistinctInterventions = distinctStrategyTrees.size();
			boolean noMatch = true;
			StrategyTree distinctStrategyTree = null;
			State correspondingState = null;

			// Check if there is any intervention equal to "intervention" in "distinctInterventions"
			for (int j = 0; j < numDistinctInterventions && noMatch; j++) {
				distinctStrategyTree = distinctStrategyTrees.get(j);
				noMatch &= !(distinctStrategyTree == strategyTree || distinctStrategyTree.equals(strategyTree));
				correspondingState = (!noMatch) ? states.get(i) : correspondingState;
			}
			if (noMatch) { // If no, add it to distinctInterventions and create a set of states in corresponding states
				distinctStrategyTrees.add(strategyTree);
				Set<State> statesIntervention = new HashSet<>();
				statesIntervention.add(states.get(i));
				correspondingStates.put(strategyTree, statesIntervention);
			} else { // Intervention is equal to a previous intervention. Add the state to the corresponding states set
				if (distinctStrategyTree != null) {
					correspondingStates.get(distinctStrategyTree).add(correspondingState);
				}
			}
		}

		// Create branches
		for (StrategyTree strategyTree : distinctStrategyTrees) {
			List<State> statesOfIntervention = new ArrayList<>(correspondingStates.get(strategyTree));
			if (strategyTree != null) {
				addBranch(new TreeADDBranch(statesOfIntervention, topVariable, strategyTree, null));
			}
		}
		ensureThatAllVariablesAreIncluded();
	}

	/**
	 * Creates an intervention with only one branch containing several states.
	 *
	 * @param topVariable Top variable
	 * @param states States
	 */
	public StrategyTree(Variable topVariable, State... states) {
		this(null, topVariable);
		List<State> branchStates = Arrays.asList(states);
		addBranch(new TreeADDBranch(branchStates, topVariable, null));
		ensureThatAllVariablesAreIncluded();
	}

	/**
	 * Creates an intervention with only one branch containing several states.
	 *
	 * @param topVariable Top variable
	 * @param states List of states
	 */
	public StrategyTree(Variable topVariable, List<State> states) {
		this(null, topVariable);
		List<State> branchStates = new ArrayList<>(states.size());
		branchStates.addAll(states);
		addBranch(new TreeADDBranch(branchStates, topVariable, null));
		ensureThatAllVariablesAreIncluded();
	}

	/**
	 * Creates an intervention with only one branch.
	 *
	 * @param topVariable Top variable
	 * @param states List of states
	 * @param strategyTree Strategy tree
	 */
	public StrategyTree(Variable topVariable, List<State> states, StrategyTree strategyTree) {
		this(null, topVariable);
		List<State> branchStates = new ArrayList<>(states.size());
		branchStates.addAll(states);
		addBranch(new TreeADDBranch(branchStates, topVariable, strategyTree, null));
		ensureThatAllVariablesAreIncluded();
	}

	/**
	 * Creates an intervention with a continuous variable with a partitioned interval.
	 * The partitioned interval must have the same number of sub-intervals than interventions.
	 *
	 * @param partitionedInterval Partitioned interval
	 * @param topVariable Top variable
	 * @param strategyTrees List of strategy trees
	 */
	public StrategyTree(Variable topVariable, PartitionedInterval partitionedInterval,
			List<StrategyTree> strategyTrees) {
		this(null, topVariable);
		double[] limits = partitionedInterval.getLimits();
		for (int i = 0; i < limits.length - 1; i++) {
			addBranch(
					new TreeADDBranch(new Threshold(limits[i], false), new Threshold(limits[i + 1], true), topVariable,
							strategyTrees.get(i), null));
		}
		ensureThatAllVariablesAreIncluded();
	}

	/** Ensures that the variables that exists in rootVariable, branches and sub-potentials
	 * are also included in the list of variables. */
	private void ensureThatAllVariablesAreIncluded() {
		LinkedHashSet<Variable> variables = new LinkedHashSet<>();
		variables.add(getRootVariable());
		for (TreeADDBranch branch : branches) {
			Potential potential = branch.getPotential();
			if (potential != null) {
				variables.addAll(potential.getVariables());
			}
		}
		if (!this.variables.containsAll(variables)) {
			this.variables = new ArrayList<>(variables);
		}
	}

	/**
	 * Creates an intervention from a set of interventions and probabilities.
	 *
	 * @param chanceVariable {@code Variable}
	 * @param probabilities  {@code double[]}
	 * @param strategyTrees  {@code Intervention[]}
	 * @return A Intervention. {@code Intervention}
	 */
	public static StrategyTree averageOfInterventions(Variable chanceVariable, double[] probabilities,
			StrategyTree[] strategyTrees) {
		State[] states = chanceVariable.getStates();

		// Select interventions and states whose probability is greater than
		// 0.0.
		List<StrategyTree> selectedStrategyTrees = new ArrayList<>();
		List<State> selectedStates = new ArrayList<>();
		for (int i = 0; i < probabilities.length; i++) {
			if (probabilities[i] > 0.0) {
				selectedStrategyTrees.add(strategyTrees[i]);
				selectedStates.add(states[i]);
			}
		}
		int numSelectedInterventions = selectedStrategyTrees.size();
		StrategyTree strategyTree;
		if (numSelectedInterventions == 0) { // All probabilities == 0.0
			strategyTree = null;
		} else {
			if (numSelectedInterventions == 1) { // Only one intervention with
				// probability != 0.0
				strategyTree = selectedStrategyTrees.get(0);
			} else { // More than one intervention with probability != 0.0
				if (equalInterventions(selectedStrategyTrees.toArray(new StrategyTree[numSelectedInterventions]))) {
					// All interventions are equals
					strategyTree = selectedStrategyTrees.get(0);
				} else {
					strategyTree = new StrategyTree(chanceVariable, selectedStates, selectedStrategyTrees);
				}
			}
		}
		return strategyTree;
	}

	/**
	 * Creates an intervention
	 *
	 * @param decisionVariable Decision variable
	 * @param utilities Array of utilities
	 * @param strategyTrees Array of strategy trees
	 * @param coalescedInterventions Coalesced interventions
	 * @return Optimal intervention
	 */
	public static StrategyTree optimalIntervention(Variable decisionVariable, double[] utilities,
			StrategyTree[] strategyTrees, boolean coalescedInterventions) {
		State[] states = decisionVariable.getStates();
		List<State> optimalStates = new ArrayList<>();
		List<StrategyTree> optimalStrategyTrees = new ArrayList<>();
		double max = Double.NEGATIVE_INFINITY;
		for (int i = 0; i < states.length; i++) {
			StrategyTree strategyTreeI = strategyTrees[i];
			double utilityI = utilities[i];
			if (utilityI > max) {
				max = utilityI;
				optimalStates.clear();
				optimalStates.add(states[i]);
				optimalStrategyTrees.clear();
				optimalStrategyTrees.add(strategyTreeI);
			} else if (utilityI == max) {  // there is a tie
				optimalStates.add(states[i]);
				if (strategyTreeI != null) {
					boolean isInOptimalInterventions = false;
					for (int j = 0; j < optimalStrategyTrees.size() && !isInOptimalInterventions; j++) {
						isInOptimalInterventions = optimalStrategyTrees.get(j).equals(strategyTreeI);
					}
					if (!isInOptimalInterventions) {
						optimalStrategyTrees.add(strategyTreeI);
					}
				}
			}
		}

		StrategyTree strategyTree = null;
		boolean severalOptimalInterventions = optimalStrategyTrees.size() > 1;
		if (!coalescedInterventions) {
			strategyTree = severalOptimalInterventions ?
					new StrategyTree(decisionVariable, optimalStates, optimalStrategyTrees) :
					new StrategyTree(decisionVariable, optimalStates, optimalStrategyTrees.get(0));
		} else {
			strategyTree = severalOptimalInterventions ?
					new SDAGStrategyTree(decisionVariable, optimalStates, optimalStrategyTrees) :
					new SDAGStrategyTree(decisionVariable, optimalStates, optimalStrategyTrees.get(0));

		}

		return strategyTree;
	}

	/**
	 * Creates an intervention
	 *
	 * @param decisionVariable Decision Variable
	 * @param utilities Array of utilities
	 * @param strategyTrees Array of strategy trees
	 * @param coalescedInterventions Coalesced Interventions
	 * @return Optimal intervention
	 */
	public static StrategyTree optimalInterventionTakingOneOptimal(Variable decisionVariable, double[] utilities,
			StrategyTree[] strategyTrees, boolean coalescedInterventions) {
		State[] states = decisionVariable.getStates();
		List<State> optimalStates = new ArrayList<>();
		State optimalState = null;
		StrategyTree optimalStrategyTree = null;
		double max = Double.NEGATIVE_INFINITY;
		for (int i = 0; i < states.length; i++) {
			StrategyTree strategyTreeI = strategyTrees[i];
			double utilityI = utilities[i];
			if (utilityI > max) {
				max = utilityI;
				optimalState = states[i];
				optimalStrategyTree = strategyTreeI;
			}
		}
		optimalStates.add(optimalState);
		StrategyTree strategyTree = null;
		if (optimalStrategyTree != null) {
			strategyTree = (!coalescedInterventions) ?
					new StrategyTree(decisionVariable, optimalStates, optimalStrategyTree) :
					new SDAGStrategyTree(decisionVariable, optimalStates, optimalStrategyTree);
		} else {
			strategyTree = (!coalescedInterventions) ?
					new StrategyTree(decisionVariable, optimalStates) :
					new SDAGStrategyTree(decisionVariable, optimalStates);

		}
		return strategyTree;

	}

	/**
	 * Creates an intervention
	 *
	 * @param decisionVariable Decision variable
	 * @param utilities Array of utilities
	 * @param strategyTrees Array of strategy trees
	 * @return Optimal intervention
	 */
	public static StrategyTree optimalInterventionTakingAllOptimal(Variable decisionVariable, double[] utilities,
			StrategyTree[] strategyTrees) {
		State[] states = decisionVariable.getStates();
		List<State> optimalStates = new ArrayList<>();
		List<StrategyTree> optimalStrategyTrees = new ArrayList<>();
		StrategyTree strategyTree;
		double max = Double.NEGATIVE_INFINITY;
		for (int i = 0; i < states.length; i++) {
			double utilityI = utilities[i];
			if (utilityI >= max) {
				if (utilityI > max) {
					max = utilityI;
					optimalStates = new ArrayList<>();
					optimalStrategyTrees = new ArrayList<>();
				}
				optimalStates.add(states[i]);
				optimalStrategyTrees.add(strategyTrees[i]);
			}
		}

		if (!areNullOptimalInterventions(optimalStrategyTrees)) {
			strategyTree = new StrategyTree(decisionVariable, optimalStates, optimalStrategyTrees);
		} else {
			strategyTree = new StrategyTree(decisionVariable, optimalStates);
		}
		return strategyTree;
	}

	private static boolean areNullOptimalInterventions(List<StrategyTree> strategyTrees) {
		return (strategyTrees.isEmpty() || strategyTrees.get(0) == null);
	}

	/**
	 * Creates an intervention
	 *
	 * @param decisionVariable Decision variable
	 * @param utilities Array of utilities
	 * @param strategyTrees Array of strategy trees
	 * @param coalescedInterventions Coalesced interventions
	 * @return Optimal intervention
	 */
	public static StrategyTree optimalInterventionTakingOptimalMinimalDepth(Variable decisionVariable,
			double[] utilities, StrategyTree[] strategyTrees, boolean coalescedInterventions) {
		State[] states = decisionVariable.getStates();
		List<State> optimalStates = new ArrayList<>();
		State optimalState = null;
		StrategyTree optimalStrategyTree = null;
		int depthOfOptimalInterv = Integer.MAX_VALUE;
		double max = Double.NEGATIVE_INFINITY;
		for (int i = 0; i < states.length; i++) {
			StrategyTree strategyTreeI = strategyTrees[i];
			double utilityI = utilities[i];
			if (utilityI > max) {
				max = utilityI;
				optimalState = states[i];
				optimalStrategyTree = strategyTreeI;
			}
			if (utilityI == max) {
				if (strategyTreeI != null) {
					int auxDepth = strategyTreeI.getDepth();
					if (auxDepth < depthOfOptimalInterv) {
						optimalState = states[i];
						optimalStrategyTree = strategyTreeI;
						depthOfOptimalInterv = auxDepth;
					}
				}

			}
		}
		optimalStates.add(optimalState);
		StrategyTree strategyTree = null;
		if (optimalStrategyTree != null) {
			strategyTree = (!coalescedInterventions) ?
					new StrategyTree(decisionVariable, optimalStates, optimalStrategyTree) :
					new SDAGStrategyTree(decisionVariable, optimalStates, optimalStrategyTree);
		} else {
			strategyTree = (!coalescedInterventions) ?
					new StrategyTree(decisionVariable, optimalStates) :
					new SDAGStrategyTree(decisionVariable, optimalStates);

		}
		return strategyTree;

	}

	/**
	 * @param strategyTrees Array of strategy trees
	 * @return {@code true} when all the interventions are equal.
	 */
	protected static boolean equalInterventions(StrategyTree[] strategyTrees) {
		boolean equalInterventions = true;
		int numInterventions = strategyTrees.length;
		if (strategyTrees != null && numInterventions > 1) {
			StrategyTree firstStrategyTree = strategyTrees[0];
			for (int i = 1; i < numInterventions && equalInterventions; i++) {
				equalInterventions &= (firstStrategyTree == null) ?
						strategyTrees[i] == null :
						firstStrategyTree.equals(strategyTrees[i]);
			}
		}
		return equalInterventions;
	}

	/**
	 * @param branch TreeADDBranch
	 * @return The intervention corresponding to 'branch'
	 */
	public static StrategyTree getInterventionBranch(TreeADDBranch branch) {
		return (StrategyTree) (branch.getPotential());
	}

	private int getDepth() {
		int depth = 0;

		if (branches != null) {
			if (branches.size() > 0) {
				int auxDepth = 0;
				for (int i = 0; i < branches.size(); i++) {
					TreeADDBranch auxBranch = branches.get(i);
					StrategyTree auxStrategyTreeBranch = getInterventionBranch(auxBranch);
					if (auxStrategyTreeBranch != null) {
						auxDepth = Math.max(auxDepth, auxStrategyTreeBranch.getDepth());
					}
				}
				depth = 1 + auxDepth;
			}
		}

		return depth;
	}

	/**
	 * Add {@code Intervention} to edges of this intervention
	 *
	 * @param strategyTree Strategy tree
	 * @return A concatenated Strategy tree
	 */
	public StrategyTree concatenate(StrategyTree strategyTree) {
		//
		StrategyTree oldStrategyTree;
		for (TreeADDBranch branch : branches) {
			oldStrategyTree = (StrategyTree) branch.getPotential();
			if (oldStrategyTree == null) {
				branch.setPotential(strategyTree);
			} else {
				StrategyTree branchStrategyTree = (StrategyTree) branch.getPotential();
				branchStrategyTree = branchStrategyTree.concatenate(strategyTree);
			}
		}
		return this;
	}

	public boolean cehasCycle() {
		boolean hasCycle;
		hasCycle = false;
		if (branches != null) {
			for (int i = 0; (i < branches.size() && !hasCycle); i++) {
				TreeADDBranch branch = branches.get(i);
				if (branch != null) {
					StrategyTree strategyTreeBranch = (StrategyTree) branch.getPotential();
					if (strategyTreeBranch != null) {
						hasCycle = strategyTreeBranch.isReachable(this);
					}
				}

			}
		}
		return hasCycle;

	}

	/**
	 * @param strategyTree StrategyTree
	 * @return True iff 'intervention' can be reached from 'this'
	 */
	private boolean isReachable(StrategyTree strategyTree) {
		boolean isReachable = false;
		if (branches != null)
			for (int i = 0; (i < branches.size() && !isReachable); i++) {
				TreeADDBranch branch = branches.get(i);
				if (branch != null) {
					StrategyTree auxBranchStrategyTree = (StrategyTree) branch.getPotential();
					if (auxBranchStrategyTree != null) {
						isReachable = auxBranchStrategyTree == strategyTree;
						isReachable = isReachable || auxBranchStrategyTree.isReachable(strategyTree);
					}
				}
			}
		return isReachable;
	}

	/**
	 * @param strategyTree {@code Intervention}
	 * @return True when {@code this} and {@code intervention} are equals.
	 */
	public boolean equals(StrategyTree strategyTree) {
		int numBranches = branches.size();
		boolean stillEqual = strategyTree != null && strategyTree.topVariable == topVariable
				&& strategyTree.branches.size() == numBranches;
		if (stillEqual) {
			// Compare each branch
			for (int i = 0; i < numBranches && stillEqual; i++) {
				TreeADDBranch branch = branches.get(i);
				// Get the corresponding branch to "this.branches.get(i)" in the other "intervention"
				List<State> states = branch.getStates();
				// A branch always has at least one state
				TreeADDBranch interventionBranch = strategyTree.getBranch(states.get(0));
				stillEqual &= interventionBranch != null;
				// Compare states
				if (stillEqual) {
					List<State> interventionBranchStates = interventionBranch.getStates();
					stillEqual &= interventionBranchStates.size() == states.size() && interventionBranchStates
							.containsAll(states);
				}
				// Compare potentials
				if (stillEqual) {
					StrategyTree strategyTreeBranchPotential = (StrategyTree) interventionBranch.getPotential();
					StrategyTree branchPotential = (StrategyTree) branch.getPotential();
					stillEqual &= !(
							(strategyTreeBranchPotential == null && branchPotential != null) || (
									strategyTreeBranchPotential != null && branchPotential == null
							)
					);
					// Recursive part (it won't be evaluated when already false)
					stillEqual &= strategyTreeBranchPotential != null ?
							strategyTreeBranchPotential.equals(branchPotential) :
							true;
				}
			}
		}
		return stillEqual;
	}

	/**
	 * @return List of interventions contained in branches if they are not null.
	 */
	public List<StrategyTree> getNextInterventions() {
		List<StrategyTree> nextStrategyTrees = new ArrayList<>();
		for (TreeADDBranch branch : branches) { // branches is never null according to TreeADDPotential code
			Potential branchPotential = branch.getPotential();
			if (branchPotential != null) {
				nextStrategyTrees.add((StrategyTree) branchPotential);
			}
		}
		return nextStrategyTrees;
	}

	/**
	 * @return {@code List} of {@code State}
	 */
	public List<State> getNonZeroProbabilityStates() {
		List<State> states = new ArrayList<>();
		for (TreeADDBranch branch : branches) {
			states.addAll(branch.getStates());
		}
		return states;
	}

	/**
	 * @param state State
	 * @return branch that contains state or null
	 */
	public TreeADDBranch getBranch(State state) {
		for (TreeADDBranch branch : branches) {
			if (branch.getBranchStates().contains(state)) {
				return branch;
			}
		}
		return null;
	}

	public String toString() {
		StringBuilder strBuffer = new StringBuilder();
		//		strBuffer.append(indent);
		//		strBuffer.append(topVariable.getName());
		// Print variables
		if (branches != null && branches.size() > 0) {
			//strBuffer.append("\n");
			//strBuffer.append(" = ");
			for (TreeADDBranch branch : branches) {
				strBuffer.append(branch);
			}
		}
		return strBuffer.toString();
	}

	public String toStringForGraphviz(ProbNet net) {

		String content = null;

		content = "digraph G {\n";

		Map<StrategyTree, Integer> idNode = new Hashtable<>();

		Set<StrategyTree> nodes = this.getInterventions();
		Set<StrategyTree> leaves = this.getInterventionsLeaves();

		int i = 0;
		for (StrategyTree skNode : nodes) {
			idNode.put(skNode, i);
			String strNodes;

			if (skNode.topVariable != null) {
				strNodes = skNode.topVariable.getName();
				if (leaves.contains(skNode)) {
					List<TreeADDBranch> skNodeBranches = skNode.getBranches();
					if ((skNodeBranches != null) && (skNodeBranches.size() > 0)) {
						strNodes = strNodes + "=" + skNodeBranches.get(0).getStates().toString();
					}
				}

				content = content + i + " [label=\"" + strNodes + "\",shape=" + toStringShapeForGraphviz(net,
						skNode.topVariable) + ",fillcolor=" + toStringColorShapeForGraphviz(net, skNode.topVariable)
						+ ",style=filled];\n";
			}
			i = i + 1;
		}

		for (StrategyTree node : nodes) {
			int nodeIdNode = idNode.get(node);
			if (node.branches != null) {
				List<StrategyTree> nodeInterv = node.getInterventionsChildren();

				for (int j = 0; j < node.branches.size(); j++) {

					StrategyTree child = nodeInterv.get(j);
					if (child != null) {

						List<State> states = node.branches.get(j).getBranchStates();
						String strStates = getStringStates(states);
						content = content + nodeIdNode + "->" + idNode.get(child) + "[label=\"" + strStates + "\"];\n";
					}
				}
			}
		}

		content = content + "}\n";
		return content;
	}

	/**
	 * @return The Interventions that are the leaves of the tree rooted at 'this'
	 */
	private Set<StrategyTree> getInterventionsLeaves() {

		Set<StrategyTree> auxSet = new HashSet<>();

		if (branches.size() == 0) {
			auxSet.add(this);
		} else {
			for (TreeADDBranch auxBranch : branches) {
				StrategyTree auxStrategyTreeBranch = getInterventionBranch(auxBranch);
				if (auxStrategyTreeBranch != null) {
					auxSet.addAll(auxStrategyTreeBranch.getInterventionsLeaves());
				} else {
					auxSet.add(this);
				}
			}
		}

		return auxSet;
	}

	private String getStringStates(List<State> states) {
		String str = "";

		if (states != null) {
			int size = states.size();
			if (size > 0) {
				str = states.get(0).toString();
				for (int i = 1; i < size; i++) {
					str = str + states.get(i).toString();
					if (i < size - 1) {
						str = str + ", ";
					}
				}
			}
		}

		return str;
	}

	public List<StrategyTree> getInterventionsChildren() {

		List<StrategyTree> list = new ArrayList<>();
		if (branches != null) {
			for (TreeADDBranch branch : branches) {
				list.add(getInterventionBranch(branch));
			}
		}
		return list;
	}

	private String toStringShapeForGraphviz(ProbNet net, Variable topVariable) {
		String string = null;

		if (net != null) {
			Node node;
			try {
				node = net.getNode(topVariable.getName());
			} catch (NodeNotFoundException e) {
				node = null;
			}
			if (node != null) {
				switch (node.getNodeType()) {
				case DECISION:
					string = "box";
					break;
				case CHANCE:
					string = "ellipse";
					break;
				default:
					break;
				}
			} else {
				string = "box";
			}
		} else {
			string = "box";
		}

		return string;
	}

	private String toStringColorShapeForGraphviz(ProbNet net, Variable topVariable) {
		String string = null;
		String colorDecision = "lightblue";
		String colorChance = "yellow";

		if (net != null) {
			Node node;
			try {
				node = net.getNode(topVariable.getName());
			} catch (NodeNotFoundException e) {
				node = null;
			}
			if (node != null) {
				switch (node.getNodeType()) {
				case DECISION:
					string = colorDecision;
					break;
				case CHANCE:
					string = colorChance;
					break;
				default:
					break;
				}
			} else {
				string = colorDecision;
			}
		} else {
			string = colorDecision;
		}

		return string;
	}

	private Set<StrategyTree> getInterventions() {
		return this.auxGetInterventions();
	}

	private Set<StrategyTree> auxGetInterventions() {
		Set<StrategyTree> auxSet;

		auxSet = new HashSet<>();
		auxSet.add(this);

		if (branches != null) {

			for (int i = 0; i < branches.size(); i++) {
				TreeADDBranch auxBranch = branches.get(i);
				StrategyTree auxStrategyTreeBranch = getInterventionBranch(auxBranch);
				if (auxStrategyTreeBranch != null) {
					auxSet.addAll(auxStrategyTreeBranch.auxGetInterventions());
				}
			}
		}

		return auxSet;
	}

	public boolean hasInterventionForDecision(Variable decision) {
		boolean hasInterv = false;

		if (topVariable == decision) {
			hasInterv = true;
		} else {
			if (branches != null) {
				for (int i = 0; i < branches.size() && !hasInterv; i++) {
					TreeADDBranch branch = branches.get(i);
					StrategyTree branchInterv = getInterventionBranch(branch);
					if (branchInterv != null) {
						hasInterv = branchInterv.hasInterventionForDecision(decision);
					}
				}
			}
		}
		return hasInterv;
	}

	@Override public Potential deepCopy(ProbNet copyNet) {
		StrategyTree strategyTree = (StrategyTree) super.deepCopy(copyNet);
		return strategyTree;
	}

    public TablePotential tableProject() {
		TablePotential projection = new TablePotential(variables, role);
		fillPotential(projection, new EvidenceCase(), this);
		return projection;
	}

	private void fillPotential(TablePotential tablePotential, EvidenceCase evidenceCase, Potential potential) {
		if (potential == null || !potential.getClass().isAssignableFrom(TreeADDPotential.class)) { // leave
			fillCompatibleConfigurations(tablePotential, evidenceCase);
		} else {
			for (TreeADDBranch branch : branches) {
				for (State state : branch.getStates()) {
					try {
						evidenceCase.changeFinding(new Finding(topVariable, state));
						fillPotential(tablePotential, evidenceCase, branch.getPotential());
					} catch (InvalidStateException | IncompatibleEvidenceException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	private void fillCompatibleConfigurations(TablePotential tablePotential, EvidenceCase evidenceCase) {
		if (tablePotential.getNumVariables() == evidenceCase.getNumberOfFindings()) {
			tablePotential.values[tablePotential.getPosition(evidenceCase)] = 1.0;
		} else {
			List<Variable> variables = tablePotential.getVariables();
			variables.removeAll(evidenceCase.getVariables());
			Variable variable = variables.get(0);
			int numStates = variable.getNumStates();
			for (int i = 0; i < numStates; i++) {
				try {
					evidenceCase.changeFinding(new Finding(variable, i));
					fillCompatibleConfigurations(tablePotential, evidenceCase);
				} catch (InvalidStateException | IncompatibleEvidenceException e) {
					e.printStackTrace();
				}
			}
		}
	}

}
