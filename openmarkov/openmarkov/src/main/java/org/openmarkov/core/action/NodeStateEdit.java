/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.action;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openmarkov.core.exception.DoEditException;
import org.openmarkov.core.model.graph.Link;
import org.openmarkov.core.model.network.Node;
import org.openmarkov.core.model.network.NodeType;
import org.openmarkov.core.model.network.PartitionedInterval;
import org.openmarkov.core.model.network.State;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.VariableType;
import org.openmarkov.core.model.network.potential.Potential;
import org.openmarkov.core.model.network.potential.TablePotential;
import org.openmarkov.core.model.network.potential.operation.DiscretePotentialOperations;
import org.openmarkov.core.model.network.potential.operation.PotentialOperations;

/**
 * {@code NodeStateEdit} is a simple edit that allow modify the states of
 * one node.
 *
 * @author Miguel Palacios
 * @version 1.0 21/12/10
 */
public class NodeStateEdit extends SimplePNEdit {
	/**
	 *
	 */
	private static final long serialVersionUID = 4325259909756103849L;

	/**
	 * The new state
	 */
	private State newState;
	/**
	 * The last state before the edition
	 */
	private State oldState = new State("");
	/**
	 * index of the state selected in the view
	 */
	private int selectedStateIndex;
	/**
	 * The node that the stats belongs to
	 */
	private Node node = null;
	/**
	 * The action to carry out
	 */
	private StateAction stateAction;
	/**
	 * The last partitioned interval before the edition
	 */
	private PartitionedInterval currentPartitionedInterval;
	/**
	 * The last states before the edition
	 */
	private State[] oldStates;
	/***
	 * Map with the link restriction potential for each link.
	 */
	private Map<Link<Node>, double[]> linkRestrictionMap;
	/***
	 * Map with the revelation condition list for each link.
	 */
	private Map<Link<Node>, List> revelationConditionMap;

	/**
	 * Map with the list of potentials of each neighbour of the node
	 */
	private HashMap<Variable, List<Potential>> listOldPotentials;

	/**
	 * List of potentials of the node
	 */
	private List<Potential> oldPotentials;

	/**
	 * the index (in the table) associated to the state to edit
	 */
	private int indexState;

	private String newName;	

	private String oldName;

	/**
	 * Creates a new {@code NodeStateEdit} to carry out the specified
	 * action on the specified state.
	 *
	 * @param node        the node that will be edited.
	 * @param stateAction the action to carry out
	 * @param stateIndex  the index (in the table) associated to the state to edit
	 * @param newName     a new string for the state edited if the action is ADD.
	 */
	public NodeStateEdit(Node node, StateAction stateAction, int stateIndex, String newName) {
		super(node.getProbNet());
		this.node = node;
		this.newName = newName;
		this.indexState = stateIndex;
		Variable variable = node.getVariable();

        //When adding a new state selectedStateIndex is not used
        this.selectedStateIndex =0;
        if  (stateAction != StateAction.ADD) {
            this.selectedStateIndex = variable.getNumStates() - (stateIndex + 1);
        }

		State[] states = variable.getStates();
		// For RENAME, MOVE and and DELETE
		State selectedState = states[selectedStateIndex];
		this.newState = (stateAction != StateAction.RENAME) ? new State(newName)
				: selectedState;
		if (stateAction != StateAction.ADD) {
			this.oldState = selectedState;
			this.oldName = variable.getStateName(selectedStateIndex);
		}
		this.stateAction = stateAction;
		this.currentPartitionedInterval = variable.getPartitionedInterval();
		this.oldStates = states;
		this.linkRestrictionMap = new HashMap<>();
		this.revelationConditionMap = new HashMap<>();

		// Save the potentials of the node and its neighbours
		this.oldPotentials = probNet.getPotentials(variable);
		this.listOldPotentials = new HashMap<>();
		for (Node nodeNeighbour : probNet.getNeighbors(node)) {
			this.listOldPotentials.put(nodeNeighbour.getVariable(),
					probNet.getPotentials(nodeNeighbour.getVariable()));
		}
	}

	@Override public void doEdit() throws DoEditException {
		State[] newStates;
		Variable variable = node.getVariable();
		List<Node> children = node.getChildren();

		switch (stateAction) {
		case ADD:
			// assume that the new state is added in last position
			newStates = new State[variable.getNumStates() + 1];
			newStates[variable.getNumStates()] = newState;
			System.arraycopy(oldStates, 0, newStates, 0, oldStates.length);

			variable.setStates(newStates);

			setUniformPotential();

			// if the node is discretized add a new row in partitionedInterval
			// field of the node
			if (variable.getVariableType() == VariableType.DISCRETIZED) {
				PartitionedInterval newPartitionedInterval = getNewPartitionedInterval();
				variable.setPartitionedInterval(newPartitionedInterval);
			}
			selectedStateIndex++;
			resetLink(node);
			break;
		case REMOVE:
			newStates = new State[variable.getNumStates() - 1];
			int i1 = 0;
			boolean found = false;
			for (State states : variable.getStates()) {
				if (i1 != selectedStateIndex || found) {
					newStates[i1] = states;
					i1++;
				} else
					found = true;
			}
			variable.setStates(newStates);

			setUniformPotential();

			// change current partitioned interval
			if (variable.getVariableType() == VariableType.NUMERIC
					|| variable.getVariableType() == VariableType.DISCRETIZED) {

				double[] oldLimits = currentPartitionedInterval.getLimits();
				boolean[] oldBelongs = currentPartitionedInterval.getBelongsToLeftSide();

				int positionToRemove = selectedStateIndex;

				List<Double> newLimits = new ArrayList<>(oldLimits.length - 1);
				List<Boolean> newBelongs = new ArrayList<>(oldLimits.length - 1);

				for (int j = 0; j < oldLimits.length; j++) {
					if (j != positionToRemove) {
						newLimits.add(oldLimits[j]);
						newBelongs.add(oldBelongs[j]);
					}
				}
				double[] limits = new double[oldBelongs.length - 1];
				boolean[] belongs = new boolean[oldBelongs.length - 1];
				for (int j = 0; j < newLimits.size(); j++) {
					limits[j] = newLimits.get(j);
					belongs[j] = newBelongs.get(j);
				}

				variable.setPartitionedInterval(new PartitionedInterval(limits, belongs));

			}
			resetLink(node);
			break;
		case DOWN:
			if (selectedStateIndex > 0) {
				newStates = new State[variable.getStates().length];
				State state = variable.getStates()[selectedStateIndex - 1];
				State swapState = variable.getStates()[selectedStateIndex];
				for (int i = 0; i < oldStates.length; i++) {
					if (i == selectedStateIndex - 1) {
						newStates[i] = swapState;
					} else if (i == selectedStateIndex) {
						newStates[i] = state;
					} else {
						newStates[i] = oldStates[i];
					}
				}
				if (node.getNodeType() == NodeType.CHANCE || node.getNodeType() == NodeType.UTILITY) {
					Potential oldPotential = node.getPotentials().get(0);
					if (oldPotential instanceof TablePotential) {
						TablePotential newPotential = DiscretePotentialOperations
								.reorder((TablePotential) oldPotential, variable, newStates);
						node.setPotential(newPotential);
					}
				}
				for (Node child : children) {
					if (child.getNodeType() == NodeType.CHANCE || child.getNodeType() == NodeType.UTILITY) {
						Potential oldPotential = child.getPotentials().get(0);
						if (oldPotential instanceof TablePotential) {
							TablePotential newPotential = DiscretePotentialOperations
									.reorder((TablePotential) oldPotential, variable, newStates);
							child.setPotential(newPotential);
						}
					}
				}
				variable.setStates(newStates);
				resetLink(node);
			}
			break;
		case UP:
			if (selectedStateIndex < variable.getNumStates()) {
				newStates = new State[variable.getStates().length];
				State state = variable.getStates()[selectedStateIndex + 1];
				State swapState = variable.getStates()[selectedStateIndex];
				for (int i = 0; i < oldStates.length; i++) {
					if (i == selectedStateIndex) {
						newStates[i] = state;
					} else if (i == selectedStateIndex + 1) {
						newStates[i] = swapState;
					} else {
						newStates[i] = oldStates[i];
					}
				}
				if (node.getNodeType() == NodeType.CHANCE || node.getNodeType() == NodeType.UTILITY) {
					Potential oldPotential = node.getPotentials().get(0);
					if (oldPotential instanceof TablePotential) {
						TablePotential newPotential = DiscretePotentialOperations
								.reorder((TablePotential) oldPotential, variable, newStates);
						node.setPotential(newPotential);
					}
				}
				for (Node child : children) {
					if (child.getNodeType() == NodeType.CHANCE || child.getNodeType() == NodeType.UTILITY) {
						Potential oldPotential = child.getPotentials().get(0);
						if (oldPotential instanceof TablePotential) {
							TablePotential newPotential = DiscretePotentialOperations
									.reorder((TablePotential) oldPotential, variable, newStates);
							child.setPotential(newPotential);
						}
					}
				}
				variable.setStates(newStates);
				resetLink(node);
			}

			break;
		case RENAME:
			if (selectedStateIndex >= 0 && selectedStateIndex < variable.getNumStates()) {
				newState.setName(newName);
			}
			break;
		}

	}

	/**
	 * Set uniform potential for the edited node and its children (except for Decision nodes)
	 */
	private void setUniformPotential() {
		Potential uniformPotential;
		List<Potential> potentials;

		if (node.getNodeType() != NodeType.DECISION) {
			uniformPotential = PotentialOperations.getUniformPotential(probNet, node.getVariable(), node.getNodeType());
			potentials = new ArrayList<>();
			potentials.add(uniformPotential);
			node.setPotentials(potentials);

		}
		for (Node child : node.getChildren()) {
			if (child.getNodeType() != NodeType.DECISION) {
				potentials = new ArrayList<>();
				uniformPotential = PotentialOperations
						.getUniformPotential(probNet, child.getVariable(), child.getNodeType());
				potentials.add(uniformPotential);
				child.setPotentials(potentials);
			}
		}

	}

	@SuppressWarnings("unchecked") @Override public void undo() {
		super.undo();
		switch (stateAction) {
		case RENAME:
			oldState.setName(oldName);
			break;
		case ADD:
		case REMOVE:
		case UP:
		case DOWN:
			//We restore the states
			node.getVariable().setStates(oldStates);

			//We restore the intervals if the variable type was discretized
			if (node.getVariable().getVariableType() == VariableType.DISCRETIZED) {
				node.getVariable().setPartitionedInterval(currentPartitionedInterval);
			}

			//We restore the link restriction's
			for (Link<Node> link : linkRestrictionMap.keySet()) {
				link.initializesRestrictionsPotential();
				TablePotential restrictionPotential = (TablePotential) link.getRestrictionsPotential();
				restrictionPotential.setValues(linkRestrictionMap.get(link));
			}
			for (Link<Node> link : revelationConditionMap.keySet()) {
				VariableType varType = link.getNode1().getVariable().getVariableType();
				if ((varType == VariableType.NUMERIC)) {
					link.setRevealingIntervals(revelationConditionMap.get(link));
				} else {
					link.setRevealingStates(revelationConditionMap.get(link));
				}

			}

			//We restore the potentials
			node.setPotentials(oldPotentials);
			for (Variable var : listOldPotentials.keySet()) {
				probNet.getNode(var).setPotentials(listOldPotentials.get(var));
			}

			break;
		}

	}

	// TODO redo() implementation

	/**
	 * Gets the new state created if the action was ADD
	 *
	 * @return the new state
	 */
	public State getNewState() {
		return newState;
	}

	/**
	 * Gets the new state created if the action was ADD
	 *
	 * @return the new state
	 */
	public State getLastState() {
		return oldState;
	}

	public Node getNode() {
		return node;
	}

	public StateAction getStateAction() {
		return stateAction;
	}

	/**
	 * This method add a new default subInterval, in the current
	 * PartitionedInterval object
	 *
	 * @return The PartitionedInterval object with a new default subInterval
	 */

	private PartitionedInterval getNewPartitionedInterval() {
		double limits[] = currentPartitionedInterval.getLimits();
		double newLimits[] = new double[limits.length + 1];
		boolean belongsToLeftSide[] = currentPartitionedInterval.getBelongsToLeftSide();
		boolean newBelongsToLeftSide[] = new boolean[limits.length + 1];
		for (int i = 0; i < limits.length; i++) {
			newLimits[i] = limits[i];
			newBelongsToLeftSide[i] = belongsToLeftSide[i];
		}

		if (currentPartitionedInterval.getMax() == Double.POSITIVE_INFINITY) {
			newLimits[limits.length - 1] = newLimits[limits.length - 2] + node.getVariable().getPrecision();
			newLimits[limits.length] = Double.POSITIVE_INFINITY;
		} else {
			newLimits[limits.length] = currentPartitionedInterval.getMax() + node.getVariable().getPrecision();
		}
		newBelongsToLeftSide[limits.length] = false;
		return new PartitionedInterval(newLimits, newBelongsToLeftSide);
	}
/*
    /**
     * This method gets the new row data when new state is inserted in a
     * discretized variable.
     * 
     * @return The row data of the new state
     */
    /*
    public Object[] getNewRowOfData() {
        String firstSymbol = null;
        String secondSymbol = null;
        double limits[] = null;
        boolean belongsToLeftSide[];
        if (stateAction == StateAction.ADD) {
            limits = node.getVariable().getPartitionedInterval().getLimits();
            belongsToLeftSide = node.getVariable().getPartitionedInterval().getBelongsToLeftSide();
            firstSymbol = (belongsToLeftSide[limits.length - 2] ? "(" : "[");
            secondSymbol = (belongsToLeftSide[limits.length - 1] ? "]" : ")");
        } else if (stateAction == StateAction.REMOVE) {
            limits = node.getVariable().getPartitionedInterval().getLimits();
            belongsToLeftSide = node.getVariable().getPartitionedInterval().getBelongsToLeftSide();

            firstSymbol = (belongsToLeftSide[selectedStateIndex] ? "(" : "[");
            secondSymbol = (belongsToLeftSide[selectedStateIndex + 1] ? "]" : ")");

        }
        return new Object[] {
                "",
                GUIDefaultStates.getString(node.getVariable().getStates()[selectedStateIndex].getName()),
                firstSymbol, limits[selectedStateIndex], ",", limits[selectedStateIndex + 1],
                secondSymbol };

    }*/
    
/*	Used only in the rename case in doEdit. Now obsolete code?
	public void renameBranchesStates(TreeADDPotential tree, String oldName, String newName) {
		if (tree.getRootVariable().equals(node.getVariable())) {
			for (int i = 0; i < tree.getBranches().size(); i++) {
				for (int j = 0; j < tree.getBranches().get(i).getBranchStates().size(); j++) {
					if (tree.getBranches().get(i).getBranchStates().get(j).getName()
							.equals(oldName)) {
						tree.getBranches().get(i).getBranchStates().get(j).setName(newName);
					}
				}
				if (tree.getBranches().get(i).getPotential() instanceof TreeADDPotential) {
					renameBranchesStates((TreeADDPotential) tree.getBranches().get(i)
							.getPotential(), oldName, newName);
				}

			}

		} else {// look if there are more subtrees within the tree
			for (int i = 0; i < tree.getBranches().size(); i++) {

				if (tree.getBranches().get(i).getPotential() instanceof TreeADDPotential) {
					renameBranchesStates((TreeADDPotential) tree.getBranches().get(i)
							.getPotential(), oldName, newName);
				}

			}
		}
	}
*/

	/****
	 * This method resets the link restriction and revelation conditions of the
	 * links of the node
	 *
	 * @param node Node
	 */
	private void resetLink(Node node) {

		for (Link<Node> link : node.getLinks()) {
			if (link.hasRestrictions()) {
				double[] lastPotential = ((TablePotential) link.getRestrictionsPotential()).values.clone();
				linkRestrictionMap.put(link, lastPotential);
				link.setRestrictionsPotential(null);

			}
		}

		for (Node child : node.getChildren()) {
			Link<Node> link = probNet.getLink(node, child, true);
			if (link.hasRevealingConditions()) {
				VariableType varType = link.getNode1().getVariable().getVariableType();
				if (varType == VariableType.NUMERIC) {
					this.revelationConditionMap.put(link, link.getRevealingIntervals());
					link.setRevealingIntervals(new ArrayList<PartitionedInterval>());
				} else {
					this.revelationConditionMap.put(link, link.getRevealingStates());
					link.setRevealingStates(new ArrayList<State>());
				}
			}
		}

	}

	public int getIndexState() {
		return indexState;
	}
	
	public String getNewName() {
		return newName;
	}

}
