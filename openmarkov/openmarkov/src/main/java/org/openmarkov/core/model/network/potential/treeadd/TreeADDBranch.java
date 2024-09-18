
/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.model.network.potential.treeadd;

import java.util.ArrayList;
import java.util.List;

import org.openmarkov.core.exception.NodeNotFoundException;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.State;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.VariableType;
import org.openmarkov.core.model.network.potential.Potential;
import org.openmarkov.core.model.network.potential.StrategyTree;

/**
 * {@code TreeADDBranch} represents branch of a treeADD. If the top variable of the
 * treeADD is numeric a branch is defined by two thresholds: a minimum and a
 * maximum. If top variable is finite states, then each branch is defined
 * by its states. In both cases each branch has a potential assigned. If the
 * branch is a leaf, its potential is any kind of potential except a
 * {@code TreeADDPotential}
 *
 * @author myebra
 */
public class TreeADDBranch {
	/**
	 * Each TreeADDBranch has an associated potential
	 */
	private Potential potential = null;

	private Variable rootVariable;

	/**
	 * If the topVariable of the tree is a finite states or a discretized
	 * variable each branch has an associated state.
	 */
	private List<State> states;

	/**
	 * If the topVariable of the tree is a continuous variable it is defined in
	 * a continuous interval which has two thresholds.
	 */
	private Threshold lowerBound;
	private Threshold upperBound;

	/**
	 * If the top variable is continuous, there is an interval and this variable is true.
	 */
	private boolean intervalBranch;

	/**
	 * If the top variable is discrete, the branch is associated to one or more states and this variable is true.
	 */
	private boolean statesBranch;

	/**
	 * If this treeADD is embedded in a larger treeADD and role is not INTERVENTION, then parentVariables
	 * is the set of variables of that treeADD.
	 * This attribute is used when building a TreeADD at the GUI.
	 * This attribute is not necessary for interventions.
	 */
	private List<Variable> parentVariables;

	/**
	 * A branch can be labeled, labels are used to reference potential from
	 * other branches when that potential has more than one parents
	 */
	private String label;
	/**
	 * A branch can reference a potential from other branch that has been
	 * labeled
	 */
	private String reference = null;
	private TreeADDBranch referencedBranch = null;

	private String indent = "    ";

	/**
	 * Constructor for discretized and finite states variables
	 *
	 * @param branchStates List of the branch states
	 * @param topVariable Top variable
	 * @param parentVariables List of the parent variables
	 */
	public TreeADDBranch(List<State> branchStates, Variable topVariable, List<Variable> parentVariables) {
		this.states = branchStates;
		this.rootVariable = topVariable;
		this.parentVariables = parentVariables;
		this.potential = null;
		this.statesBranch = true;
		this.intervalBranch = false;
	}

	/**
	 * Constructor for discretized and finite states variables
	 *
	 * @param branchStates List of the branch states
	 * @param topVariable Top variable
	 * @param potential Potential
	 * @param parentVariables List of the parent variables
	 */
	public TreeADDBranch(List<State> branchStates, Variable topVariable, Potential potential,
			List<Variable> parentVariables) {
		this(branchStates, topVariable, parentVariables);
		this.potential = potential;
		this.statesBranch = true;
		this.intervalBranch = false;
	}

	/**
	 * Constructor for numeric variables
	 *
	 * @param lowerBound Lower bound threshold
	 * @param upperBound Upper bound threshold
	 * @param topVariable Top variable
	 * @param potential Potential
	 * @param parentVariables List of parent variables
	 */
	public TreeADDBranch(Threshold lowerBound, Threshold upperBound, Variable topVariable, Potential potential,
			List<Variable> parentVariables) {
		this.lowerBound = lowerBound;
		this.upperBound = upperBound;
		this.potential = potential;
		this.rootVariable = topVariable;
		this.parentVariables = parentVariables;
		this.statesBranch = false;
		this.intervalBranch = true;
	}

	/**
	 * Constructor for discretized and finite states variables with reference
	 * @param branchStates List of the branch states
	 * @param topVariable Top variable
	 * @param reference Reference
	 * @param parentVariables List of the parent variables
	 */
	public TreeADDBranch(List<State> branchStates, Variable topVariable, String reference,
			List<Variable> parentVariables) {
		this.states = branchStates;
		this.reference = reference;
		this.rootVariable = topVariable;
		this.parentVariables = parentVariables;
		this.statesBranch = true;
		this.intervalBranch = false;
	}

	/**
	 * Constructor for numeric variables with reference
	 * @param lowerBound Lower bound threshold
	 * @param upperBound Upper bound threshold
	 * @param topVariable Top variable
	 * @param reference Reference
	 * @param parentVariables List of parent variables
	 */
	public TreeADDBranch(Threshold lowerBound, Threshold upperBound, Variable topVariable, String reference,
			List<Variable> parentVariables) {
		this.lowerBound = lowerBound;
		this.upperBound = upperBound;
		this.reference = reference;
		this.rootVariable = topVariable;
		this.parentVariables = parentVariables;
		this.statesBranch = false;
		this.intervalBranch = true;
	}

	public void setIndent(String indent) {
		this.indent = indent;
	}

	public TreeADDBranch copy() {
		TreeADDBranch branch = null;
		if (potential != null) {
			if (this.rootVariable.getVariableType() == VariableType.FINITE_STATES
					|| this.rootVariable.getVariableType() == VariableType.DISCRETIZED) {
				branch = new TreeADDBranch(new ArrayList<>(getBranchStates()), this.getRootVariable(),
						this.getPotential().copy(), this.getParentVariables());

			} else if (this.rootVariable.getVariableType() == VariableType.NUMERIC) {
				branch = new TreeADDBranch(new Threshold(this.getLowerBound()), new Threshold(this.getUpperBound()),
						this.getRootVariable(), this.getPotential().copy(), this.getParentVariables());
			}
			if (label != null) {
				branch.setLabel(label);
			}
		} else if (reference != null) {
			if (this.rootVariable.getVariableType() == VariableType.FINITE_STATES
					|| this.rootVariable.getVariableType() == VariableType.DISCRETIZED) {
				branch = new TreeADDBranch(new ArrayList<>(getBranchStates()), this.getRootVariable(), this.reference,
						this.getParentVariables());
			} else if (this.rootVariable.getVariableType() == VariableType.NUMERIC) {
				branch = new TreeADDBranch(new Threshold(this.getLowerBound()), new Threshold(this.getUpperBound()),
						this.getRootVariable(), this.reference, this.getParentVariables());
			}
			if (referencedBranch != null) {
				branch.setReferencedBranch(referencedBranch);
			}
		} else // HACK for interventions
		{
			if (this.rootVariable.getVariableType() == VariableType.FINITE_STATES
					|| this.rootVariable.getVariableType() == VariableType.DISCRETIZED) {
				branch = new TreeADDBranch(new ArrayList<>(getBranchStates()), this.getRootVariable(), (Potential) null,
						this.getParentVariables());

			} else if (this.rootVariable.getVariableType() == VariableType.NUMERIC) {
				branch = new TreeADDBranch(new Threshold(this.getLowerBound()), new Threshold(this.getUpperBound()),
						this.getRootVariable(), (Potential) null, this.getParentVariables());
			}
		}
		return branch;
	}

	public List<State> getStates() {
		return states;
	}

	public void setStates(List<State> states) {
		this.states = states;
	}

	public List<Variable> getAddableVariables() {
		List<Variable> addableVariables = new ArrayList<>(parentVariables);
		addableVariables.removeAll(potential.getVariables());
		return addableVariables;
	}

	public List<State> getBranchStates() {
		return this.states;
	}

	public List<Variable> getParentVariables() {
		return this.parentVariables;
	}

	public void setParentVariables(List<Variable> parentVariables) {
		this.parentVariables = parentVariables;
	}

	public Variable getRootVariable() {
		return this.rootVariable;
	}

	public void setRootVariable(Variable topVariable) {
		this.rootVariable = topVariable;
	}

	public Potential getPotential() {
		return (potential != null || referencedBranch == null) ? potential : referencedBranch.getPotential();
	}

	public void setPotential(Potential potential) {
		this.potential = potential;
	}

	public Threshold getLowerBound() {
		return lowerBound;
	}

	public void setLowerBound(Threshold lowerBound) {
		this.lowerBound = lowerBound;
	}

	public Threshold getUpperBound() {
		return upperBound;
	}

	public void setUpperBound(Threshold upperBound) {
		this.upperBound = upperBound;
	}

	public boolean isInsideInterval(double numericValue) {
		return (lowerBound == null || lowerBound.isBelow(numericValue)) && (
				upperBound == null || upperBound.isAbove(numericValue)
		);
	}

	public String getLabel() {
		return this.label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public boolean isLabeled() {
		return this.label != null;
	}

	public String getReference() {
		return this.reference;
	}

	public boolean isReference() {
		return this.reference != null;
	}

	public void setReference(String reference) {
		this.reference = reference;
	}

	@Override public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(rootVariable);
		builder.append(" = ");

		if (states != null) {
			states.forEach(x -> builder.append(x));
		}

		if (potential != null) {
			builder.append(" -> ");
		}
		boolean isNullPotential = potential == null;
		boolean isStrategyTree = !isNullPotential && potential.getClass() == StrategyTree.class;
		if (!isNullPotential && !isStrategyTree) {
			List<Variable> potentialVariables = potential.getVariables();
			if (potentialVariables != null && potentialVariables.size() > 0) {
				builder.append(" ");
				builder.append(potentialVariables.size());
				builder.append(" variables(");
				for (int i = 0; i < potentialVariables.size(); i++) {
					builder.append(potentialVariables.get(i));
					builder.append((i < potentialVariables.size() - 1) ? ", " : "); ");
				}
			}
		}
		if (parentVariables != null && parentVariables.size() > 0 && !isStrategyTree) {
			//			builder.append("\n");
			builder.append(indent);
			builder.append("ParentVariables = ");
			builder.append(parentVariables);
		}
		if (lowerBound != null && upperBound != null) {
			//			builder.append("\n");
			builder.append(indent);
			builder.append("Interval: (");
			builder.append(lowerBound);
			builder.append(", ");
			builder.append(upperBound);
			builder.append(")");
		}
		//builder.append("\n");		
		if (isStrategyTree) {
			List<TreeADDBranch> branches = ((StrategyTree) potential).getBranches();
			for (TreeADDBranch branch : branches) {
				branch.setIndent(indent + "    ");
				if (branches.size() > 1) {
					builder.append(" IF ");
				}
				builder.append(branch.toString());
			}
		}
		return builder.toString();
	}

	public void setReferencedBranch(TreeADDBranch treeADDBranch) {
		this.reference = treeADDBranch.getLabel();
		this.referencedBranch = treeADDBranch;
		this.potential = null;
	}

	public boolean isIntervalBranch() {
		return intervalBranch;
	}

	public boolean isStatesBranch() {
		return statesBranch;
	}

	public TreeADDBranch deepCopy(ProbNet copyNet) {
		List<State> newStates = null;
		if (states != null) {
			newStates = new ArrayList<>(states);
		}
		Variable newRootVariable = null;
		try {
			newRootVariable = copyNet.getVariable(this.rootVariable.getName());
		} catch (NodeNotFoundException e) {
			e.printStackTrace();
		}

		List<Variable> newParentVariables = new ArrayList<>();
		for (Variable variable : this.parentVariables) {
			try {
				newParentVariables.add(copyNet.getVariable(variable.getName()));
			} catch (NodeNotFoundException e) {
				e.printStackTrace();
			}
		}

		TreeADDBranch branch = new TreeADDBranch(newStates, newRootVariable, newParentVariables);

		branch.indent = new String(this.indent);
		branch.intervalBranch = this.intervalBranch;

		if (this.label != null) {
			branch.label = new String(this.label);
		}

		if (this.lowerBound != null) {
			branch.lowerBound = new Threshold(this.lowerBound);
		}

		if (this.upperBound != null) {
			branch.upperBound = new Threshold(this.upperBound);
		}

		if (this.potential != null) {
			branch.potential = this.potential.deepCopy(copyNet);
		}

		if (this.reference != null) {
			branch.reference = new String(this.reference);
		}

		if (this.referencedBranch != null) {
			branch.referencedBranch = this.referencedBranch.deepCopy(copyNet);
		}

		branch.statesBranch = this.statesBranch;

		return branch;
	}

}
