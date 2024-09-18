/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.model.network.potential.treeadd;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.openmarkov.core.exception.IncompatibleEvidenceException;
import org.openmarkov.core.exception.NodeNotFoundException;
import org.openmarkov.core.exception.NonProjectablePotentialException;
import org.openmarkov.core.exception.WrongCriterionException;
import org.openmarkov.core.inference.InferenceOptions;
import org.openmarkov.core.model.network.Criterion;
import org.openmarkov.core.model.network.EvidenceCase;
import org.openmarkov.core.model.network.Finding;
import org.openmarkov.core.model.network.Node;
import org.openmarkov.core.model.network.PartitionedInterval;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.State;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.VariableType;
import org.openmarkov.core.model.network.modelUncertainty.UncertainValue;
import org.openmarkov.core.model.network.potential.Potential;
import org.openmarkov.core.model.network.potential.PotentialRole;
import org.openmarkov.core.model.network.potential.StrategyTree;
import org.openmarkov.core.model.network.potential.TablePotential;
import org.openmarkov.core.model.network.potential.UniformPotential;
import org.openmarkov.core.model.network.potential.operation.AuxiliaryOperations;
import org.openmarkov.core.model.network.potential.operation.DiscretePotentialOperations;
import org.openmarkov.core.model.network.potential.plugin.PotentialType;

/**
 * A TreeADDPotential is a potential defined by a top variable and its branches.
 * It implies several advantages instead of using tables when the potential has a substructure that repeats
 * information several times.
 *
 * @author myebra
 */
@PotentialType(name = "Tree/ADD", family = "Tree")
public class TreeADDPotential extends Potential {

	// Attributes
	/**
	 * topVariable is the variable at the root of the tree
	 */
	protected Variable topVariable;
	/**
	 * This List stores the branches created in the TreeADDPotential constructor
	 */
	protected List<TreeADDBranch> branches = new ArrayList<>();

	/**
	 * For role conditional. Call to the complex constructor
	 *
	 * @param variables List of variables
	 * @param topVariable Top variable
	 * @param role Potential role
	 */
	public TreeADDPotential(List<Variable> variables, Variable topVariable, PotentialRole role) {
		this(variables, topVariable, topVariable.getStates(), topVariable.getPartitionedInterval(), role);
	}

	/**
	 * For role conditional
	 *
	 * @param variables List of variables
	 * @param topVariable Top variable
	 * @param branchingStates Array of branching states
	 * @param interval Interval
	 * @param role            {@link org.openmarkov.core.model.network.potential.PotentialRole}
	 */
	public TreeADDPotential(List<Variable> variables, Variable topVariable, State[] branchingStates,
			PartitionedInterval interval, PotentialRole role) {
		super(variables, role);
		this.topVariable = topVariable;
		VariableType variableType = topVariable.getVariableType();
		List<Variable> potentialVariables;
		// if topVariable is finite states or discretized, it creates a branch
		// for each state
		if (variableType == VariableType.FINITE_STATES || variableType == VariableType.DISCRETIZED) {

			for (int i = branchingStates.length - 1; i >= 0; i--) {
				// if potential role of the treeADD is a conditional probability
				// it is assigned an uniform potential
				// to the conditioned variable which is always the first
				// variable of the arrayList of variables
				if (role == PotentialRole.CONDITIONAL_PROBABILITY) {
					Variable conditionedVariable = variables.get(0);
					potentialVariables = new ArrayList<>();
					potentialVariables.add(conditionedVariable);
					UniformPotential potential = new UniformPotential(potentialVariables, role);
					List<State> branchStates = new ArrayList<>();
					branchStates.add(branchingStates[i]);
					branches.add(new TreeADDBranch(branchStates, topVariable, potential, variables));
				}
			}
		}
		// if topVariable is numeric, it creates a branch whose thresholds are
		// the same as those defined for the variable
		if (variableType == VariableType.NUMERIC) {
			Threshold minimum = new Threshold(interval.getMin(), !interval.isLeftClosed());
			Threshold maximum = new Threshold(interval.getMax(), interval.isRightClosed());
			potentialVariables = new ArrayList<>();
			potentialVariables.add(variables.get(0));
			UniformPotential potential = new UniformPotential(potentialVariables, role);
			branches.add(new TreeADDBranch(minimum, maximum, topVariable, potential, variables));
		}
	}

	//	/**
	//	 * For role Utility
	//	 *
	//	 * @param variables
	//	 * @param topVariable
	//	 * @param utilityVariable
	//	 */
	//	public TreeADDPotential(Variable utilityVariable, List<Variable> variables, Variable topVariable) {
	//		this(utilityVariable, variables, topVariable, topVariable.getStates(), topVariable.getPartitionedInterval());
	//	}

	//	/**
	//	 * For role Utility
	//	 *
	//	 * @param variables
	//	 * @param topVariable
	//	 * @param branchingStates
	//	 * @param utilityVariable
	//	 */
	//	public TreeADDPotential(Variable utilityVariable, List<Variable> variables, Variable topVariable, State[] branchingStates, PartitionedInterval interval) {
	//		super(utilityVariable, variables);
	//		// setUtilityVariable(utilityVariable);
	//		this.topVariable = topVariable;
	//		VariableType variableType = topVariable.getVariableType();
	//		List<Variable> potentialVariables;
	//		// if topVariable is finite states or discretized, it creates a branch
	//		// for each state
	//		if (variableType == VariableType.FINITE_STATES || variableType == VariableType.DISCRETIZED) {
	//			for (int i = 0; i < branchingStates.length; i++) {
	//				// if the role of the treeADD is utility, it assigns a uniform
	//				// potential
	//				if (role == PotentialRole.UTILITY) {
	//					potentialVariables = new ArrayList<>();
	//					Potential potential = new UniformPotential(utilityVariable, potentialVariables);
	//					// potential.setUtilityVariable(utilityVariable);
	//					List<State> branchStates = new ArrayList<>();
	//					branchStates.add(branchingStates[i]);
	//					branches.add(new TreeADDBranch(branchStates, topVariable, potential, variables));
	//				}
	//			}
	//		}
	//		// if topVariable is numeric, it creates a branch whose thresholds are
	//		// the
	//		// same as those defined for the variable
	//		if (variableType == VariableType.NUMERIC) {
	//			Threshold minimum = new Threshold(interval.getMin(), !interval.isLeftClosed());
	//			Threshold maximum = new Threshold(interval.getMax(), interval.isRightClosed());
	//			potentialVariables = new ArrayList<>();
	//			// it is an utility potential for sure so it is not necessary to add
	//			// variable 0 to potential variables
	//			Potential potential = new UniformPotential(utilityVariable, potentialVariables);
	//			// potential.setUtilityVariable(utilityVariable);
	//			branches.add(new TreeADDBranch(minimum, maximum, topVariable, potential, variables));
	//		}
	//	}

	public TreeADDPotential(List<Variable> variables, PotentialRole role) {
		this(variables, variables.get(1), role);
	}

	//	public TreeADDPotential(Variable utilityVariable, List<Variable> variables) {
	//		this(utilityVariable, variables, variables.get(0));
	//	}

	/**
	 * Constructor for the parser
	 * @param variables List of variables
	 * @param topVariable Top variable
	 * @param role Potential role
	 * @param branches Branches
	 *
	 */
	public TreeADDPotential(List<Variable> variables, Variable topVariable, PotentialRole role,
			List<TreeADDBranch> branches) {
		super(variables, role);
		this.topVariable = topVariable;
		this.role = role;
		this.branches = branches;

		// Try to fill references in branches
		updateReferences(getLabeledBranches());
	}

	/**
	 * Copy constructor
	 *
	 * @param treeADD Tree ADD potential
	 */
	public TreeADDPotential(TreeADDPotential treeADD) {
		super(treeADD);
		this.topVariable = treeADD.getRootVariable();
		List<TreeADDBranch> treeBranches = new ArrayList<>();
		for (int i = 0; i < treeADD.getBranches().size(); i++) {
			treeBranches.add(treeADD.getBranches().get(i).copy());
		}
		this.branches = treeBranches;
		updateReferences(getLabeledBranches());
	}

	/**
	 * Returns if an instance of a certain Potential type makes sense given the
	 * variables and the potential role
	 *
	 * @param node Node
	 * @param variables List of variables
	 * @param role Potential role
	 * @return True if it is valid
	 */
	public static boolean validate(Node node, List<Variable> variables, PotentialRole role) {
		boolean validate = false;
		// node must have at least one parent node
		// @12/11/2014
		// Fixing issue #216
		// https://bitbucket.org/cisiad/org.openmarkov.issues/issue/216/treeadd-as-possible-type-of-potential-for
		// [...] and the node cannot be a super value node
		if (role == PotentialRole.UNSPECIFIED) {
			if (!node.isSuperValueNode()) {
				// in variables there is not utility variable
				if (variables.size() >= 1) {
					validate = true;
				}
			}
		} else if (role == PotentialRole.CONDITIONAL_PROBABILITY || node.hasPolicy()) {
			if (variables.size() >= 2) {
				validate = true;
			}
		}
		return validate;
	}

	/**
	 * If the intervention is a decision the number of branches is 1, otherwise,
	 * it is the number of states of the chance variable with probability greater than 0.
	 *
	 * @return {@code int}
	 */
	protected int getNumBranches() {
		return branches.size();
	}

	/**
	 * Recursively goes through the interventions tree adding the number of leaves.
	 *
	 * @return {@code int}
	 */
	protected int getNumLeaves() {
		int numLeaves = 0;
		for (TreeADDBranch branch : branches) {
			Potential potential = branch.getPotential();
			if (potential.getClass() == TreeADDPotential.class) {
				numLeaves += ((TreeADDPotential) potential).getNumLeaves();
			} else {
				numLeaves++;
			}
		}
		return (numLeaves == 0) ? 1 : numLeaves;
	}

	/**
	 * @return {@code False} when this intervention is a leaf.
	 */
	public boolean hasAnySubIntervention() {
		return branches.size() != 0;
	}

	/**
	 * Add a new branch
	 * @param branch Branch
	 */
	public void addBranch(TreeADDBranch branch) {
		branches.add(branch);
	}

	public List<TreeADDBranch> getBranches() {
		return branches;
	}

	public void setBranches(List<TreeADDBranch> branches) {
		this.branches = branches;
	}

	public void setBranchAtIndex(int index, TreeADDBranch treeBranch) {
		this.branches.set(index, treeBranch);
	}

	public Variable getRootVariable() {
		return topVariable;
	}

	public void setRootVariable(Variable variable) {
		this.topVariable = variable;
	}

	/**
	 * Adds variable to a treeADD potential
	 */
	public Potential addVariable(Variable variable) {
		variables.add(variable);
		for (TreeADDBranch branch : getBranches()) {
			branch.setParentVariables(variables);
			if (branch.getPotential() instanceof TreeADDPotential) {
				branch.getPotential().addVariable(variable);
			}
		}
		return this;
	}

	/**
	 * Removes variable from a treeADD potential
	 */
	public Potential removeVariable(Variable variable) {
		List<Variable> newVariables = getVariables();
		newVariables.remove(variable);
		return new UniformPotential(newVariables, getPotentialRole());
	}

	/*
	 * private TablePotential getPotentialMask () { }
	 */

	@Override public List<TablePotential> tableProject(EvidenceCase evidenceCase, InferenceOptions inferenceOptions,
			List<TablePotential> projectedPotentials) throws NonProjectablePotentialException, WrongCriterionException {
		TablePotential projected = null;
		if (topVariable.getVariableType() != VariableType.NUMERIC) {
			Map<TreeADDBranch, TablePotential> potentialsToBlend = new HashMap<>();
			List<TreeADDBranch> branches = this.getBranches();
			for (TreeADDBranch branch : branches) {
				Potential branchPotential = branch.getPotential();
				List<TablePotential> tablePotentials = branchPotential
						.tableProject(evidenceCase, inferenceOptions, projectedPotentials);
				potentialsToBlend.put(branch, tablePotentials.get(0));
			}
			projected = blendPotentials(topVariable, potentialsToBlend, evidenceCase);
		} else {
			// if there is no evidence for the numerical topVariable it is not
			// possible to project the tree
			if (evidenceCase == null || evidenceCase.getFinding(topVariable) == null) {
				throw new NonProjectablePotentialException(
						"It is not possible to project this tree " + this.toShortString() + " because top variable "
								+ topVariable.getName() + " is numeric and has no evidence");
			}
			double topVariableValue = evidenceCase.getFinding(topVariable).getNumericalValue();
			List<TreeADDBranch> numericalBranches = getBranches();
			Potential potential = null;
			for (TreeADDBranch numericalBranch : numericalBranches) {
				double minLimit = numericalBranch.getLowerBound().getLimit();
				double maxlimit = numericalBranch.getUpperBound().getLimit();
				if (minLimit <= topVariableValue && topVariableValue <= maxlimit) {
					if (minLimit == topVariableValue) {
						if (numericalBranch.getLowerBound().belongsToLeft()) {
							continue;
						} else {
							potential = numericalBranch.getPotential();
							break;
						}
					} else if (maxlimit == topVariableValue) {
						if (numericalBranch.getUpperBound().belongsToLeft()) {
							potential = numericalBranch.getPotential();
							break;
						} else {
							continue;
						}
					} else { // minLimit < topVariableValue < maxLimit
						potential = numericalBranch.getPotential();
						break;
					}
				}
			}
			// if potential still null that means finding was not within the
			// numerical variable domain so
			if (potential == null) {
				throw new NonProjectablePotentialException("It is not possible to project this tree, "
						+ "top variable value was not within the topVariable domain");
			}
			projected = potential.tableProject(evidenceCase, inferenceOptions).get(0);
		}
		// Make sure variables are in the correct order after applying the mask
		// there will be variables that disappear from the potential because of
		// evidence propagation
		/*
		 * if (role == PotentialRole.CONDITIONAL_PROBABILITY || role ==
		 * PotentialRole.UTILITY) { for (int i = 0; i < correctOrder.size ();
		 * i++) { if (!projected.contains (correctOrder.get (i))) {
		 * correctOrder.remove (i); } } if (role == PotentialRole.UTILITY) {
		 * projected = DiscretePotentialOperations.reorder (projected,
		 * correctOrder); } else { projected.setVariables (correctOrder); } }
		 */

		return Arrays.asList(projected);
	}

	/**
	 * Eliminates the nodes whose variable name is equal to the parameter 'variableName'
	 * and grafts the daughter branches of that node in the parent node.
	 * The variable must have only one child.
	 * @param variableName Name of the variable
	 */
	public void pruneAndGraftNode(String variableName) {
		if (getRootVariable().getName().toUpperCase().matches(variableName.toUpperCase())) {
			int numBranches = branches.size();
			if (numBranches == 1 && TreeADDPotential.class.isAssignableFrom(branches.get(0).getPotential().getClass())) {
				TreeADDPotential treeADDPotential = (TreeADDPotential)branches.get(0).getPotential();
				setRootVariable(treeADDPotential.getRootVariable());
				branches = treeADDPotential.branches;
				indentLevel = treeADDPotential.indentLevel;
			} else {
				if (numBranches > 1) { // Tie. Choose randomly one branch (the first branch) whose child is a TreeADDPotential. Otherwise, do nothing.
					boolean assignableBranchFound = false;
					for (int i = 0; i < numBranches && !assignableBranchFound; i++) {
						TreeADDBranch branch = branches.get(i);
						assignableBranchFound = TreeADDPotential.class.isAssignableFrom(branch.getPotential().getClass());
						if (assignableBranchFound) {
							TreeADDPotential treeADDPotential = (TreeADDPotential)branch.getPotential();
							setRootVariable(treeADDPotential.getRootVariable());
							branches = treeADDPotential.branches;
							indentLevel = treeADDPotential.indentLevel;
						}
					}
				}
			}
		}

		for (TreeADDBranch branch : branches) {
			Potential potential = branch.getPotential();
			if (potential != null && TreeADDPotential.class.isAssignableFrom(potential.getClass())) {
				TreeADDPotential childTreeADDPotential = (TreeADDPotential)potential;
				String childVariableName = childTreeADDPotential.getRootVariable().getName().toUpperCase();
				if (childVariableName.matches(variableName.toUpperCase())) {
					List<TreeADDBranch> potentialBranches = childTreeADDPotential.getBranches();
					if (potentialBranches.size() > 0) {
						branch.setPotential(potentialBranches.get(0).getPotential());
					}
				}
				childTreeADDPotential.pruneAndGraftNode(variableName);
			}
		}
	}

	@Override public void shift(ProbNet probNet, int timeDifference) throws NodeNotFoundException {
		super.shift(probNet, timeDifference);
		List<Variable> copiedTreeVariables = new ArrayList<>();

		if (getRootVariable().isTemporal()) {
			setRootVariable(probNet.getShiftedVariable(getRootVariable(), timeDifference));
		}
		for (TreeADDBranch branch : getBranches()) {
			branch.setParentVariables(copiedTreeVariables);
			branch.setRootVariable(getRootVariable());
			if (!branch.isReference()) {
				Potential originalPotential = branch.getPotential();
				originalPotential.shift(probNet, timeDifference);
				branch.setPotential(originalPotential);
			}
		}
	}

	@Override public Potential copy() {
		return new TreeADDPotential(this);
	}

	@Override public boolean isUncertain() {
		// If at least one of the leaf potentials has uncertainty then returns
		// true
		boolean hasUncertainty = false;
		for (TreeADDBranch branch : getBranches()) {
			Potential branchPotential = branch.getPotential();
			hasUncertainty = branchPotential.isUncertain();
			if (hasUncertainty == true)
				break;
		}
		return hasUncertainty;
	}

	/**
	 * Generates a sampled potential
	 */
	@Override public Potential sample() {
		TreeADDPotential sampledTree = (TreeADDPotential) this.copy();
		for (TreeADDBranch branch : sampledTree.getBranches()) {
			branch.setPotential(branch.getPotential().sample());
		}
		return sampledTree;
	}

	public Map<String, TreeADDBranch> getLabeledBranches() {
		Map<String, TreeADDBranch> labeledBranches = new HashMap<>();
		Stack<TreeADDPotential> subtrees = new Stack<>();
		subtrees.push(this);
		while (!subtrees.isEmpty()) {
			TreeADDPotential treeADD = subtrees.pop();
			for (TreeADDBranch branch : treeADD.getBranches()) {
				if (branch.getLabel() != null) {
					labeledBranches.put(branch.getLabel(), branch);
				}
				if (branch.getPotential() != null && branch.getPotential() instanceof TreeADDPotential) {
					subtrees.push((TreeADDPotential) branch.getPotential());
				}
			}
		}
		return labeledBranches;
	}

	/**
	 * Update references
	 * @param labeledBranches Labeled branches
	 */
	public void updateReferences(Map<String, TreeADDBranch> labeledBranches) {
		Stack<TreeADDPotential> subtrees = new Stack<>();
		if (!labeledBranches.isEmpty()) {
			subtrees.push(this);
			while (!subtrees.isEmpty()) {
				TreeADDPotential treeADD = subtrees.pop();
				for (TreeADDBranch branch : treeADD.getBranches()) {
					if (branch.getReference() != null && labeledBranches.containsKey(branch.getReference())) {
						branch.setReferencedBranch(labeledBranches.get(branch.getReference()));
					}
					if (branch.getPotential() instanceof TreeADDPotential) {
						subtrees.push((TreeADDPotential) branch.getPotential());
					}
				}
			}
		}
	}

	//	@Override
	//	public void setUtilityVariable(Variable utilityVariable) {
	//		super.setUtilityVariable(utilityVariable);
	//		for (TreeADDBranch branch : branches) {
	//			if (branch.getPotential() != null && branch.getPotential().getUtilityVariable() == null) {
	//				branch.getPotential().setUtilityVariable(utilityVariable);
	//			}
	//		}
	//	}

	@Override public Collection<Finding> getInducedFindings(EvidenceCase evidenceCase)
			throws IncompatibleEvidenceException, WrongCriterionException {
		List<Finding> newFindings = new ArrayList<>();
		for (TreeADDBranch branch : branches) {
			if (evidenceCase.contains(topVariable)) {
				boolean isInduced = false;
				Finding finding = evidenceCase.getFinding(topVariable);
				if (topVariable.getVariableType() == VariableType.NUMERIC) {
					isInduced = branch.isInsideInterval(finding.getNumericalValue());
				} else {
					int i = 0;
					List<State> branchStates = branch.getBranchStates();
					while (i < branchStates.size() && !isInduced) {
						isInduced = finding.getState().equals(branchStates.get(i++).getName());
					}
				}
				if (isInduced) {
					newFindings.addAll(branch.getPotential().getInducedFindings(evidenceCase));
				}
			}
		}
		return newFindings;
	}

	private TablePotential blendPotentials(Variable topVariable, Map<TreeADDBranch, TablePotential> branchPotentials,
			EvidenceCase evidence) {
		List<TablePotential> potentials = new ArrayList<>();
		// branchStateIndex contains in it's i-th position the index of the
		// potential in potentials that is relevant for topVariable's i-th state
		int[] branchStateIndex = new int[topVariable.getNumStates()];
		Criterion criterion = null;
		for (TreeADDBranch branch : branchPotentials.keySet()) {

			// Get the criterion
			if (branchPotentials.get(branch).isAdditive()) {
				criterion = branchPotentials.get(branch).getCriterion();
			}
			potentials.add(branchPotentials.get(branch));
			for (State branchState : branch.getBranchStates()) {
				branchStateIndex[topVariable.getStateIndex(branchState)] = potentials.size() - 1;
			}
		}

		int numPotentials = potentials.size();

		// Gets the union
		List<Variable> resultVariables = AuxiliaryOperations.getUnionVariables(potentials);

		// Make sure conditioned variable is in first position
		int conditionedVarIndex = resultVariables.indexOf(getConditionedVariable());
		if (conditionedVarIndex > 0) {
			Variable otherVariable = resultVariables.get(0);
			resultVariables.set(0, getConditionedVariable());
			resultVariables.set(conditionedVarIndex, otherVariable);
		}

		// Add top variable to resulting potential's variable list
		int topVariableIndex = resultVariables.indexOf(topVariable);
		int topVariableEvidenceStateIndex = -1;
		if (evidence == null || !evidence.contains(topVariable)) {
			if (topVariableIndex == -1) {
				topVariableIndex = resultVariables.indexOf(getConditionedVariable()) + 1;
				resultVariables.add(topVariableIndex, topVariable);
			}
		} else {
			topVariableEvidenceStateIndex = evidence.getFinding(topVariable).getStateIndex();
		}

		TablePotential resultPotential = new TablePotential(resultVariables, potentials.get(0).getPotentialRole());
		resultPotential.setCriterion(criterion);
		// Number of variables
		int numVariables = resultVariables.size();

		// Gets the tables of each TablePotential
		double[][] tables = new double[numPotentials][];
		StrategyTree[][] interventionsTables = new StrategyTree[numPotentials][];
		for (int i = 0; i < numPotentials; i++) {
			tables[i] = potentials.get(i).values;
			interventionsTables[i] = potentials.get(i).strategyTrees;
		}

		// Gets the uncertain tables of each TablePotential
		boolean containsUncertainty = false;
		boolean containsInterventions = false;
		UncertainValue[][] uncertaintyTables = new UncertainValue[numPotentials][];
		for (int i = 0; i < numPotentials; i++) {
			uncertaintyTables[i] = potentials.get(i).uncertainValues;
			containsUncertainty |= uncertaintyTables[i] != null;
			containsInterventions |= potentials.get(i).strategyTrees != null;
		}
		if (containsUncertainty) {
			resultPotential.uncertainValues = new UncertainValue[resultPotential.getTableSize()];
		}
		if (containsInterventions) {
			resultPotential.strategyTrees = new StrategyTree[resultPotential.getTableSize()];
		}

		// Gets dimensions
		int[] resultDimensions = resultPotential.getDimensions();

		// Gets accumulated offsets
		int[][] accumulatedOffsets = DiscretePotentialOperations.getAccumulatedOffsets(potentials, resultVariables);

		// Gets coordinate
		int[] resultCoordinates;
		if (numVariables != 0) {
			resultCoordinates = new int[numVariables];
		} else {
			resultCoordinates = new int[1];
			resultCoordinates[0] = 0;
		}

		// Position in each table potential
		int[] potentialPositions = new int[numPotentials];
		for (int i = 0; i < numPotentials; i++) {
			potentialPositions[i] = 0;
		}

		int incrementedVariable = 0;
		int tableSize = resultPotential.getTableSize();
		double[] resultValues = resultPotential.values;
		StrategyTree[] resultStrategyTrees = resultPotential.strategyTrees;
		UncertainValue[] uncertainValues = resultPotential.uncertainValues;
		int topVariableStateIndex = (topVariableEvidenceStateIndex != -1) ?
				topVariableEvidenceStateIndex :
				resultCoordinates[topVariableIndex];
		int potentialIndex = branchStateIndex[topVariableStateIndex];

		if (potentials.size() > 0) {
			for (int resultPosition = 0; resultPosition < tableSize; resultPosition++) {
				/*
				 * increment the result coordinate and find out which variable
				 * is to be incremented
				 */
				for (int iVariable = 0; iVariable < resultCoordinates.length; iVariable++) {
					// try by incrementing the current variable (given by
					// iVariable)
					resultCoordinates[iVariable]++;
					if (resultDimensions == null || resultCoordinates[iVariable] != resultDimensions[iVariable]) {
						// we have incremented the right variable
						incrementedVariable = iVariable;
						// do not increment other variables;
						break;
					}
					/*
					 * this variable could not be incremented; we set it to 0 in
					 * resultCoordinate (the next iteration of the for-loop will
					 * increment the next variable)
					 */
					resultCoordinates[iVariable] = 0;
				}

				// Find out which is the relevant potential for this state of the root variable

				// Copy the value of the relevant potential onto the result potential
				int i = potentialPositions[potentialIndex];
				resultValues[resultPosition] = tables[potentialIndex][i];
				StrategyTree[] interventionsTablesPotentialIndices = interventionsTables[potentialIndex];
				if (interventionsTablesPotentialIndices != null) {
					resultStrategyTrees[resultPosition] = interventionsTablesPotentialIndices[i];
				}
				UncertainValue[] uncertainValuesPotentialIndex = uncertaintyTables[potentialIndex];
				if (uncertainValuesPotentialIndex != null) {
					uncertainValues[resultPosition] = uncertainValuesPotentialIndex[i];
				}

				for (int iPotential = 0; iPotential < numPotentials; iPotential++) {
					// update the current position in each potential table
					if (accumulatedOffsets[iPotential].length > 0) {
						potentialPositions[iPotential] += accumulatedOffsets[iPotential][incrementedVariable];
					}
				}
				topVariableStateIndex = (topVariableEvidenceStateIndex != -1) ?
						topVariableEvidenceStateIndex :
						resultCoordinates[topVariableIndex];
				potentialIndex = branchStateIndex[topVariableStateIndex];

			}
		}
		return resultPotential;
	}

	@Override public void replaceVariable(int position, Variable variable) {
		Variable oldVariable = variables.get(position);
		super.replaceVariable(position, variable);
		if (topVariable.equals(oldVariable)) {
			topVariable = variable;
		}
		for (TreeADDBranch branch : branches) {
			branch.getPotential().replaceVariable(oldVariable, variable);
		}
	}

	@Override public void replaceNumericVariable(Variable convertedParentVariable) {
		super.replaceNumericVariable(convertedParentVariable);

		if (topVariable.getName().equals(convertedParentVariable.getName())) {
			State[] states = convertedParentVariable.getStates();
			double[] stateValues = new double[states.length];
			for (int i = 0; i < states.length; ++i) {
				stateValues[i] = Double.parseDouble(states[i].getName());
			}
			for (TreeADDBranch branch : branches) {
				List<State> branchStates = new ArrayList<>();
				for (int i = 0; i < stateValues.length; ++i) {
					if (branch.isInsideInterval(stateValues[i])) {
						branchStates.add(states[i]);
					}
				}
				branch.setStates(branchStates);
				branch.setRootVariable(convertedParentVariable);
			}
			topVariable = convertedParentVariable;
		}
		for (TreeADDBranch branch : branches) {
			branch.getPotential().replaceNumericVariable(convertedParentVariable);
		}
	}

	@Override public void scalePotential(double scale) {
		// Scale all the potentials of the branches
		for (TreeADDBranch branch : branches) {
			branch.getPotential().scalePotential(scale);
		}

	}

	@Override public Potential deepCopy(ProbNet copyNet) {
		TreeADDPotential treeADDPotential = (TreeADDPotential) super.deepCopy(copyNet);
		List<TreeADDBranch> treeADDBranches = new ArrayList<>();
		for (TreeADDBranch branch : this.branches) {
			treeADDBranches.add(branch.deepCopy(copyNet));
		}

		treeADDPotential.setBranches(treeADDBranches);

		treeADDPotential.indent = new String(this.indent);
		treeADDPotential.indentLevel = this.indentLevel;

		if (this.topVariable != null) {
			try {
				treeADDPotential.topVariable = copyNet.getVariable(this.topVariable.getName());
			} catch (NodeNotFoundException e) {
				e.printStackTrace();
			}
		}

		return treeADDPotential;
	}

	// Attributes used in toString()
	private static String DEFAULT_INDENT_STRING = "";
	protected static int indentIncrement = 4;
	private String indent = DEFAULT_INDENT_STRING;
	private int indentLevel;

	// Methods used in toString()
	public void setIndentLevel(int indentLevel) {
		this.indentLevel = indentLevel;
		indent = "";
		for (int i = 0; i < indentLevel; i++) {
			indent = indent + " ";
		}
	}

	// Methods for toString()
	public String toString() {
		StringBuilder strBuffer = new StringBuilder();
		strBuffer.append(indent);
		strBuffer.append(this.getClass().getSimpleName()); // Print name
		strBuffer.append(" - topVariable: ");
		strBuffer.append(topVariable.getName());
		// Print variables
		if (variables != null) {
			int numVariables = variables.size();
			int i = 0;
			if (numVariables > 0) {
				strBuffer.append(" - Variables (");
			} else {
				strBuffer.append(" - No variables");
			}
			for (Variable variable : variables) {
				strBuffer.append(variable.getName());
				if (i++ == numVariables - 1) {
					strBuffer.append(")");
				} else {
					strBuffer.append(", ");
				}
			}
		}
		if (branches != null && branches.size() > 0) {
			strBuffer.append("\n");
			for (TreeADDBranch branch : branches) {
				strBuffer.append(branch);
			}
		} else {
			strBuffer.append(" - No branches.");
			strBuffer.append(")\n");
		}
		return strBuffer.toString();
	}

}
