/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.model.network;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.openmarkov.core.action.PNESupport;
import org.openmarkov.core.action.PNEdit;
import org.openmarkov.core.exception.ConstraintViolationException;
import org.openmarkov.core.exception.DoEditException;
import org.openmarkov.core.exception.NodeNotFoundException;
import org.openmarkov.core.exception.NonProjectablePotentialException;
import org.openmarkov.core.exception.WrongCriterionException;
import org.openmarkov.core.inference.InferenceOptions;
import org.openmarkov.core.model.graph.Graph;
import org.openmarkov.core.model.graph.Link;
import org.openmarkov.core.model.network.Criterion.CECriterion;
import org.openmarkov.core.model.network.constraint.ConstraintManager;
import org.openmarkov.core.model.network.constraint.OnlyAtemporalVariables;
import org.openmarkov.core.model.network.constraint.OnlyChanceNodes;
import org.openmarkov.core.model.network.constraint.OnlyOneAgent;
import org.openmarkov.core.model.network.constraint.OnlyTemporalVariables;
import org.openmarkov.core.model.network.constraint.OnlyUndirectedLinks;
import org.openmarkov.core.model.network.constraint.PNConstraint;
import org.openmarkov.core.model.network.potential.Potential;
import org.openmarkov.core.model.network.potential.PotentialRole;
import org.openmarkov.core.model.network.potential.TablePotential;
import org.openmarkov.core.model.network.type.BayesianNetworkType;
import org.openmarkov.core.model.network.type.MarkovNetworkType;
import org.openmarkov.core.model.network.type.NetworkType;

/**
 * A {@code ProbNet} stores {@code Node}s in a efficient manner.
 * It has the operations to manage {@code Variables, nodes} and {@code
 * Potentials}.
 *
 * @author marias
 * @author fjdiez
 * @author mpalacios
 * @author mluque
 * @version 1.0
 * @see org.openmarkov.core.model.graph
 * @see org.openmarkov.core.model.network.Node
 * @since OpenMarkov 1.0
 */
public class ProbNet extends Graph<Node> implements Cloneable {
	/**
	 * This object contains all the information that the parser reads from disk
	 * that does not have a direct connection with the attributes stored in the
	 * {@code ProbNet} object.
	 */
	public HashMap<String, String> additionalProperties = new HashMap<>();
	/**
	 * Nodes are stored in several HashMaps to accelerate the access. The type
	 * of node determines the {@code HashMap} in which the node is stored.
	 */
	protected NodeTypeDepot nodeDepot;
	/**
	 * Network type of this {@code ProbNet}.
	 */
	private NetworkType networkType;
	/**
	 * {@code ArrayList} of {@code Constraints} that defines this
	 * {@code ProbNet}. This attribute is not frozen to allow conversions
	 */
	private List<PNConstraint> constraints;
	/**
	 * Set of agents, defined by a name. Each one may have several properties.
	 */
	private List<StringWithProperties> agents;
	/**
	 * Set of criterion for decision, defined by a name. Each one may have
	 * several properties.
	 */
	private List<Criterion> decisionCriteria;
	/**
	 * Cycle length value and unit of the probNet
	 */
	private CycleLength cycleLength;
	private PNESupport pNESupport;
	/**
	 * The file where the network has been saved
	 */
	private String name;
	/**
	 * ProbNet comment
	 */
	private String comment = "";
	/**
	 * Indicates whether the comment should be shown when opening the net
	 */
	private boolean showCommentWhenOpening = false;
	/**
	 * Default States of the probNet
	 */
	private State[] defaultStates = { new State("absent"), new State("present") };

	private InferenceOptions inferenceOptions;

	private Set<TablePotential> constantPotentials;

	// Constructors
	public ProbNet(NetworkType networkType) {
		this.pNESupport = new PNESupport(false);
		this.decisionCriteria = new ArrayList<>();
		decisionCriteria.add(new Criterion());
		this.constraints = new ArrayList<>();
		this.nodeDepot = new NodeTypeDepot();
		this.inferenceOptions = new InferenceOptions();
		this.constantPotentials = new HashSet<>();
		if (!hasConstraint(OnlyAtemporalVariables.class)) {
			this.cycleLength = new CycleLength();
		}

		try {
			this.setNetworkType(networkType);
		} catch (ConstraintViolationException e) {
			// Impossible to reach here as the net is empty
		}
	}

	/**
	 * Creates a probabilistic network. NetworkTypeConstraint defines the
	 * network type. If NetworkTypeConstraint is null the network type will be
	 * Bayesian Network
	 */
	public ProbNet() {
		this(BayesianNetworkType.getUniqueInstance());
	}

	/**
	 * @param nodes list of {@code Node}s
	 * @return variables corresponding to the received nodes.
	 * {@code List} of {@code Variable}
	 */
	public static List<Variable> getVariables(Collection<Node> nodes) {
		List<Variable> variables = null;
		if (nodes != null) {
			variables = new ArrayList<>(nodes.size());
			for (Node node : nodes) {
				variables.add(node.getVariable());
			}
		}
		return variables != null ? variables : new ArrayList<Variable>();
	}

	/**
	 * Adds the variables in the received {@code Potential} to this
	 * {@code MarkovNet}, creates links between those variables creating
	 * cliques and assigns the {@code potential} to the conditioned
	 * variable (the first one).
	 *
	 * @param projectedTablePotentials {@code ArrayList} of {@code Potential}s
	 * @return A Markov Network in witch potentials are used to create cliques.
	 * ({@code ProbNet}).
	 * Condition: At least one potential depends on at least one variable
	 * (otherwise the network would have no node, and it would be
	 * impossible to assign constant potentials)
	 */
	public ProbNet buildMarkovDecisionNetwork(Collection<? extends Potential> projectedTablePotentials) {
		ProbNet markovDecisionNetwork = new ProbNet(MarkovNetworkType.getUniqueInstance());

		try {
			markovDecisionNetwork.addConstraint(new OnlyUndirectedLinks(), true);
		} catch (ConstraintViolationException e) {
			e.printStackTrace(); // Unreachable code because probNet is empty.
		}
		for (Potential potential : projectedTablePotentials) {
			markovDecisionNetwork.addPotential(potential, this);
		}

		markovDecisionNetwork.setInferenceOptions(this.getInferenceOptions());
		return markovDecisionNetwork;
	}

	/**
	 * Applies edit to the probNet
	 *
	 * @param edit edit to be applied
	 * @throws ConstraintViolationException ConstraintViolationException
	 * @throws NonProjectablePotentialException NonProjectablePotentialException
	 * @throws WrongCriterionException WrongCriterionException
	 * @throws DoEditException DoEditException
	 */
	public void doEdit(PNEdit edit)
			throws ConstraintViolationException, NonProjectablePotentialException,
			WrongCriterionException, DoEditException {
		pNESupport.announceEdit(edit);
		pNESupport.doEdit(edit);
	}

	/**
	 * @param constraint {@code PNConstraint}
	 * @param check      . when {@code false}, constraint is added to the
	 *                   constraints list without testing. Otherwise,
	 *                   {@code constraint} is added only when it is full-filled.
	 *                   {@code boolean}
	 * @throws ConstraintViolationException ConstraintViolationException
	 */
	public void addConstraint(PNConstraint constraint, boolean check) throws ConstraintViolationException {
		if (!this.networkType.isApplicableConstraint(constraint)) {
			throw new ConstraintViolationException(
					"Can not apply " + constraint.toString() + " to a probNet of type " + this.networkType.getClass());
		} else if (!constraints.contains(constraint)) {
			if (check && !constraint.checkProbNet(this)) {
				throw new ConstraintViolationException("Can not apply " + constraint.toString() + " to this probNet.");
			}
			constraints.add(constraint);
			pNESupport.addUndoableEditListener(constraint);
		}
	}

	public void addConstraint(PNConstraint constraint) throws ConstraintViolationException {
		addConstraint(constraint, true);
	}

	/**
	 * @param constraints ArrayList of PNConstraint
	 * @param check       . when {@code false}, constraint is added to the
	 *                    constraints list without testing. Otherwise,
	 *                    {@code constraint} is added only when it is full-filled.
	 *                    {@code boolean}
	 * @throws ConstraintViolationException ConstraintViolationException
	 */
	public void addConstraints(List<PNConstraint> constraints, boolean check) throws ConstraintViolationException {
		for (PNConstraint constraint : constraints) {
			addConstraint(constraint, check);
		}
	}

	/**
	 * @param constraint {@code PNConstraint}
	 */
	public void removeConstraint(PNConstraint constraint) {
		if (constraints.contains(constraint)) {
			constraints.remove(constraint);
			pNESupport.removeUndoableEditListener(constraint);
		}
	}

	/**
	 * @param constraints ArrayList of PNConstraint
	 */
	public void removeConstraints(List<PNConstraint> constraints) {
		for (PNConstraint constraint : constraints) {
			removeConstraint(constraint);
		}
	}

	/**
	 * Remove all the constraints in the network
	 *
	 * @param constraintClass {@code Class}
	 */
	public void removeAllConstraints(Class<PNConstraint> constraintClass) {
		List<PNConstraint> constraintsToRemove = new ArrayList<>();
		for (PNConstraint constraint : constraints) {
			if (constraint.getClass().equals(constraintClass)) {
				constraintsToRemove.add(constraint);
			}
		}
		constraints.removeAll(constraintsToRemove);
	}

	/**
	 * @return {@code ArrayList} of {@code PNConstraint}s
	 */
	public List<PNConstraint> getConstraints() {
		return new ArrayList<>(constraints);
	}

	/**
	 * @return {@code ArrayList} of {@code PNConstraint}s
	 */
	public List<PNConstraint> getAdditionalConstraints() {
		List<PNConstraint> additionalConstraints = new ArrayList<>(constraints);
		List<PNConstraint> networkTypeConstraints = ConstraintManager.getUniqueInstance()
				.buildConstraintList(networkType);
		additionalConstraints.removeAll(networkTypeConstraints);
		return additionalConstraints;
	}

	@SuppressWarnings("rawtypes") public boolean containsConstraint(Class receivedConstraintClass) {
		boolean containsConstraint = false;
		int numConstraints = constraints.size();
		for (int i = 0; i < numConstraints && !containsConstraint; i++) {
			containsConstraint = constraints.get(i).getClass() == receivedConstraintClass;
		}
		return containsConstraint;
	}

	/**
	 * Gets Network type constraint. There is only one and it is stored in first
	 * position.
	 *
	 * @return constraint. NetworkType
	 */
	public NetworkType getNetworkType() {
		return networkType;
	}

	/**
	 * Sets Network type
	 *
	 * @param networkType {@code NetworkType}
	 * @throws ConstraintViolationException ConstraintViolationException
	 */
	public void setNetworkType(NetworkType networkType) throws ConstraintViolationException {
		NetworkType oldNetworkType = this.networkType;
		this.networkType = networkType;
		List<PNConstraint> constraints;
		try {
			constraints = ConstraintManager.getUniqueInstance().buildConstraintList(networkType);
			// Add new constraints implied by the network type
			addConstraints(constraints, true);
			// Remove those constraints that are no longer applicable to the new
			// network type
			List<PNConstraint> constraintsToRemove = new ArrayList<>();
			for (PNConstraint constraint : this.constraints) {
				if (!networkType.isApplicableConstraint(constraint)) {
					constraintsToRemove.add(constraint);
				}
			}
			removeConstraints(constraintsToRemove);
		} catch (ConstraintViolationException e) {
			// Revert
			this.networkType = oldNetworkType;
			throw e;
		}
	}

	/**
	 * Checks all the constraints applied to this {@code probNet}.
	 *
	 * @return {@code true} when all the constraints are full filled,
	 * otherwise {@code false}.
	 */
	public boolean checkProbNet() {
		for (PNConstraint constraint : constraints) {
			if ((constraint != null) && (!constraint.checkProbNet(this))) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Checks whether this {@code probNet} is temporal or not.
	 *
	 * @return {@code true} when this network has not associated
	 * OnlyAtemporalVariables constraint, otherwise {@code false}.
	 */
	public boolean variablesCouldBeTemporal() {
		for (PNConstraint constraint : constraints) {
			if (constraint instanceof OnlyAtemporalVariables) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Checks whether this {@code probNet} is multiagent or not.
	 *
	 * @return {@code true} when this network has not associated
	 * OnlyOneAgent constraint, otherwise {@code false}.
	 */
	public boolean isMultiagent() {
		for (PNConstraint constraint : constraints) {
			if (constraint instanceof OnlyOneAgent) {
				return false;
			}
		}
		return true;
	}

	/**
	 * @return {@code int}
	 */
	public int getNumCriteria() {
		List<String> criterionNames = new ArrayList<String>(2);
		int numDistinctCriteria = 0;
		for (Potential potential : getPotentials()) {
			Criterion criterion = potential.getCriterion();
			if (criterion != null) {
				String potentialCriterionName = criterion.getCriterionName();
				int criteriaCount;
				// Looks for a criterion in the list of criteria
				for (criteriaCount = 0;
					 criteriaCount < numDistinctCriteria && potentialCriterionName != null && !potentialCriterionName
							 .equalsIgnoreCase(criterionNames.get(criteriaCount)); criteriaCount++)
					;
				if (criteriaCount == numDistinctCriteria) {
					criterionNames.add(potentialCriterionName);
					numDistinctCriteria++;
				}
			}
		}
		return numDistinctCriteria;
	}

	public boolean thereAreTemporalNodes() {
		boolean thereAreTemporalNodes = false;
		for (int i = 0; i < getNodes().size(); i++) {
			if (getNodes().get(i).getVariable().isTemporal()) {
				thereAreTemporalNodes = true;
				break;
			}
		}
		return thereAreTemporalNodes;
	}

	/**
	 * Checks whether this {@code probNet} is temporal or not.
	 *
	 * @return {@code true} when this network has not associated
	 * OnlyAtemporalVariables constraint, otherwise {@code false}.
	 */
	public boolean onlyTemporal() {
		for (PNConstraint constraint : constraints) {
			if (constraint instanceof OnlyTemporalVariables) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Checks whether this {@code probNet} has only chance node or not.
	 *
	 * @return {@code true} when this network has not associated
	 * OnlyChanceNodes constraint, otherwise {@code false}.
	 */
	public boolean onlyChanceNodes() {
		for (PNConstraint constraint : constraints) {
			if (constraint instanceof OnlyChanceNodes) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Creates a low deep copy of {@code this ProbNet}: copy the
	 * {@code graph} and the {@code nodes} but do not copy nor
	 * variables nor potentials.
	 *
	 * @return {@code this probNet} copied.
	 */
	public ProbNet copy() {
		ProbNet copyNet = new ProbNet(this.networkType);
		return auxCopy(copyNet);
	}

	/**
	 * Auxiliary method for copy, which creates a low deep copy of {@code this ProbNet}: copy the
	 * {@code graph} and the {@code nodes} but do not copy nor
	 * variables nor potentials.
	 * @param copyNet Network
	 * @return {@code this probNet} copied.
	 */
	protected ProbNet auxCopy(ProbNet copyNet) {
		//ProbNet copyNet = new ProbNet(this.networkType);
		copyNet.setName(name);
		// copy constraints
		int numConstraints = constraints.size();
		for (int i = 1; i < numConstraints; i++) {
			try {
				copyNet.addConstraint(constraints.get(i), false);
			} catch (ConstraintViolationException e) {
				// Unreachable code because constraints are not tested in copy
			}
		}
		List<Node> nodes = getNodes();
		// Adds variables and create corresponding nodes. Also add potentials
		for (Node node : nodes) {
			// Add variables and create corresponding nodes
			Variable variable = node.getVariable();
			Node newNode = copyNet.addNode(variable, node.getNodeType());
			newNode.setCoordinateX(node.getCoordinateX());
			newNode.setCoordinateY(node.getCoordinateY());
			newNode.setPotentials(node.getPotentials());
			// TODO Hacer clon para node y quitar estas lineas
			newNode.setPurpose(node.getPurpose());
			newNode.setRelevance(node.getRelevance());
			newNode.setComment(node.getComment());
			newNode.additionalProperties = additionalProperties;
			newNode.setAlwaysObserved(node.isAlwaysObserved());
		}
		// Adds links
		// Copy explicit links' properties
		if (hasExplicitLinks()) {
			copyNet.makeLinksExplicit(false);
			for (Link<Node> originalLink : getLinks()) {
				Node copyNode1 = copyNet.getNode(originalLink.getNode1().getVariable());
				Node copyNode2 = copyNet.getNode(originalLink.getNode2().getVariable());
				Link<Node> copyLink = copyNet.addLink(copyNode1, copyNode2, originalLink.isDirected());
				copyLink.setRestrictionsPotential(originalLink.getRestrictionsPotential());
				copyLink.setRevealingIntervals(originalLink.getRevealingIntervals());
				copyLink.setRevealingStates(originalLink.getRevealingStates());
			}
		} else {
			for (Node node : nodes) {
				Node copyNode = copyNet.getNode(node.getVariable());
				List<Node> siblings = getSiblings(node);
				for (Node sibling : siblings) {
					Node copySibling = copyNet.getNode(sibling.getVariable());
					if (!copyNet.isSibling(copyNode, copySibling)) {
						copyNet.addLink(copyNode, copySibling, false);
					}
				}
				List<Node> children = getChildren(node);
				for (Node child : children) {
					Node copyChild = copyNet.getNode(child.getVariable());
					copyNet.addLink(copyNode, copyChild, true);
				}
			}
		}
		// copy listeners
		copyNet.getPNESupport().setListeners(pNESupport.getListeners());
		// Copy additionalProperties
		Set<String> keys = additionalProperties.keySet();
		HashMap<String, String> copyProperties = new HashMap<>();
		for (String key : keys) {
			copyProperties.put(key, additionalProperties.get(key));
		}
		copyNet.additionalProperties = copyProperties;
		// Copy decisionCriterion variable
		// copy decision criteria
		if (this.getDecisionCriteria() != null) {
			//            copyNet.setDecisionCriteria(new ArrayList<>(this.getDecisionCriteria()));
			copyNet.setDecisionCriteria(this.getDecisionCriteria());
		}

		// Copy temporal units
		if (this.getCycleLength() != null) {
			copyNet.setCycleLength(this.getCycleLength());
		}

		//Copy Inference Options
		copyNet.getInferenceOptions().setMultiCriteriaOptions(this.getInferenceOptions().getMultiCriteriaOptions());
		copyNet.getInferenceOptions().setTemporalOptions(this.getInferenceOptions().getTemporalOptions());
		return copyNet;
	}

	/**
	 * Inserts a link ({@code directed = true} or {@code false})
	 * between the nodes associated to {@code variable1} and
	 * {@code variable2} in {@code this} graph.
	 *
	 * @param variable1 {@code Variable}
	 * @param variable2 {@code Variable}
	 * @param directed  {@code boolean}
	 * @throws NodeNotFoundException exception when the addition of this link is not consistent
	 *                               with the restrictions applied to the graph or when one or
	 *                               both variables does not belong to {@code this} graph.
	 */
	public void addLink(Variable variable1, Variable variable2, boolean directed) throws NodeNotFoundException {
		// Get nodes
		Node node1 = getNode(variable1);
		Node node2 = getNode(variable2);
		if (node1 == null) {
			throw new NodeNotFoundException(this, variable1);
		}
		if (node2 == null) {
			throw new NodeNotFoundException(this, variable2);
		}
		addLink(node1, node2, directed);
	}

	/**
	 * Inverts the link ({@code directed = true} or {@code false})
	 * that goes from the nodes associated to {@code variable1} and
	 * {@code variable2} in {@code this} graph.
	 *
	 * @param variable1 {@code Variable}
	 * @param variable2 {@code Variable}
	 * @param directed  {@code boolean}
	 *                  exception when the inversion of this link is not consistent
	 *                  with the restrictions applied to the graph or when one or
	 *                  both variables does not belong to {@code this} graph.
	 * @throws Exception Exception
	 */
	public void invertLink(Variable variable1, Variable variable2, boolean directed) throws Exception {
		removeLink(variable1, variable2, true);
		addLink(variable2, variable1, true);
	}

	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return Number of nodes in {@code probNet}. {@code int}
	 */
	public int getNumNodes() {
		return nodeDepot.getNumNodes();
	}

	/**
	 * @param nodeType - {@code NodeType}
	 * @return Number of nodes with {@code NodeType = nodeType}.
	 * {@code int}
	 */
	public int getNumNodes(NodeType nodeType) {
		return nodeDepot.getNumNodes(nodeType);
	}

	/**
	 * @param evidenceCase Evidence in that the potentials will be projected
	 * @return The potentials of the network projected on the evidence
	 * @throws NonProjectablePotentialException NonProjectablePotentialException
	 * @throws WrongCriterionException WrongCriterionException
	 */
	public List<TablePotential> tableProjectPotentials(EvidenceCase evidenceCase)
			throws NonProjectablePotentialException, WrongCriterionException {
		List<Potential> originalPotentials = getSortedPotentials();
		List<TablePotential> projectedPotentials = new ArrayList<>();
		// each original potential may yield several projected potentials;
		List<TablePotential> potentials;
		for (Potential potential : originalPotentials) {
			InferenceOptions inferenceOptions = new InferenceOptions(this, null);
			potentials = potential.tableProject(evidenceCase, inferenceOptions, projectedPotentials);
			projectedPotentials.addAll(potentials);
		}
		return projectedPotentials;
	}

	/**
	 * Get all the potentials in the network (constant or nodes potentials)
	 *
	 * @return All the potentials of this network. {@code List} of
	 * {@code Potential}s.
	 */
	public List<Potential> getPotentials() {
		List<Node> nodes = getNodes();
		List<Potential> potentials = new ArrayList<>();
		for (Node node : nodes) {
			List<Potential> potentialsNode = node.getPotentials();
			for (Potential potential : potentialsNode) {
				if (null != potential) {
					potentials.add(potential);
				}
			}
		}
		if (constantPotentials != null) {
			for (Potential potential : constantPotentials) {
				if (null != potential) {
					potentials.add(potential);
				}
			}
		}
		return potentials;
	}

	/**
	 * @return All the potentials of this network sorted topologically.
	 * {@code List} of {@code Potential}s.
	 */
	public List<Potential> getSortedPotentials() {
		List<Node> nodes = ProbNetOperations.sortTopologically(this);
		List<Potential> potentials = new ArrayList<>();
		for (Node node : nodes) {
			potentials.addAll(node.getPotentials());
		}
		return potentials;
	}

	/**
	 * @param variables {@code ArrayList} of {@code Variable}
	 * @return All the nodes corresponding to variables in same order.
	 * {@code ArrayList} of {@code Node}
	 */
	public List<Node> getNodes(List<Variable> variables) {
		List<Node> nodes = new ArrayList<>(variables.size());
		for (Variable variable : variables) {
			nodes.add(getNode(variable));
		}
		return nodes;
	}

	/**
	 * @param nodeType nodetype filter
	 * @return All the nodes of certain kind
	 */
	public List<Node> getNodes(NodeType nodeType) {
		return nodeDepot.getNodes(nodeType);
	}

	/**
	 * Gets all utility nodes with the cECriterion
	 *
	 * @param cECriterion cost-effectiveness criterion filter
	 * @return All utility nodes with the cost effectiveness criterion
	 */
	public List<Node> getNodes(CECriterion cECriterion) {
		List<Node> utilityNodes = getNodes(NodeType.UTILITY);
		List<Node> nodes = new ArrayList<>();

		for (Node utilityNode : utilityNodes) {
			if (utilityNode.getVariable().getDecisionCriterion().getCECriterion() == cECriterion) {
				nodes.add(utilityNode);
			}
		}
		return nodes;
	}

	/**
	 * The potentials that contain {@code variable} are stored in the node
	 * associated to the {@code variable} or in the neighbors of that node.
	 * This method returns as well the constant potentials (i.e., the potentials
	 * that do not depend on any variable) stored in the node associated to
	 * {@code variable}.
	 *
	 * @param variable {@code Variable}.
	 * @return {@code ArrayList} of all the {@code Potential}s in this
	 * network that contains {@code variable}
	 */
	public List<Potential> getPotentials(Variable variable) {
		List<Potential> potentials = new ArrayList<>();
		Node node = getNode(variable);
		// potentials associated to this node
		if (node != null) { // Variable exists in this ProbNet
			// potentials in neighbors that contains variable
			Set<Node> semiNeighbors = new LinkedHashSet<Node>(getNeighbors(node));
			semiNeighbors.add(node);
			List<Node> children = getChildren(node);
			for (Node child : children) {
				semiNeighbors.addAll(child.getParents());
			}
			for (Node neighbor : semiNeighbors) {
				List<Potential> nodePotentials = neighbor.getPotentials();
				for (Potential potential : nodePotentials) {
					if (potential.contains(variable)) {
						potentials.add(potential);
					}
				}
			}
		}
		return potentials;
	}

	/**
	 * Get all the potentials of a specific node type
	 *
	 * @param nodeType node type filter
	 * @return All the utility potentials of a type.
	 */
	public List<Potential> getPotentialsByType(NodeType nodeType) {
		return nodeDepot.getPotentialsByType(nodeType);
	}

	/**
	 * Get all the potentials of a specific role
	 *
	 * @param role potential role filter
	 * @return All the potentials of a role.
	 */
	public List<Potential> getPotentialsByRole(PotentialRole role) {

		List<Potential> potentials = nodeDepot.getPotentialsByRole(role);

		if (constantPotentials != null) {
			for (Potential potential : constantPotentials) {
				if (potential.getPotentialRole() == role) {
					potentials.add(potential);
				}
			}
		}

		return potentials;
	}

	/**
	 * Get all the additive potentials
	 *
	 * @return All additive potentials. {@code List} of {@code Potential}
	 */
	public List<Potential> getAdditivePotentials() {
		List<Node> nodes = getNodes();
		List<Potential> potentials = new ArrayList<>();
		for (Node node : nodes) {
			List<Potential> potentialsNode = node.getPotentials();
			for (Potential potential : potentialsNode) {
				if (null != potential && potential.isAdditive()) {
					potentials.add(potential);
				}
			}
		}
		if (constantPotentials != null) {
			for (Potential potential : constantPotentials) {
				if (null != potential && potential.isAdditive()) {
					potentials.add(potential);
				}
			}
		}
		return potentials;
	}

	/**
	 * Gets all the probability potentials that contain the
	 * {@code Variable} received. The potentials that can contain that
	 * variable are in the node associated to the variable and its neighbors.
	 *
	 * @param variable variable that belongs to this {@code ProbNet}
	 * @return {@code ArrayList} of potentials containing
	 * {@code variable}.
	 */
	public List<Potential> getProbPotentials(Variable variable) {
		Node nodeVariable = getNode(variable);
		List<Node> allNodes = getNeighbors(nodeVariable);
		allNodes.add(nodeVariable);
		List<Potential> potentialsVariable = new ArrayList<>();
		for (Node node : allNodes) {
			List<Potential> potentialsNode = node.getPotentials();
			for (Potential potential : potentialsNode) {
				if ((potential.getVariables().contains(variable))
						&& potential.getVariable(0).getDecisionCriterion() == null
						&& potential.getCriterion() == null) {
					potentialsVariable.add(potential);
				}
			}
		}
		return potentialsVariable;
	}

	/**
	 * Gets all the utility potentials that contain the {@code variable}
	 * received. Constant utility potentials are also returned by this method.
	 * <p>
	 * The potentials that can contain that variable are in the node associated
	 * to the variable and its neighbors.
	 *
	 * @param variable that belongs to this {@code ProbNet}
	 *                 {@code Variable}.
	 * @return {@code ArrayList} of potentials containing
	 * {@code variable}.
	 */
	public List<Potential> getUtilityPotentials(Variable variable) {
		Node nodeVariable = getNode(variable);
		List<Node> allNodes = getNeighbors(nodeVariable);
		allNodes.add(nodeVariable);
		List<Potential> potentialsVariable = new ArrayList<>();
		for (Node node : allNodes) {
			List<Potential> potentialsNode = node.getPotentials();
			for (Potential potential : potentialsNode) {
				List<Variable> variables = potential.getVariables();
				if (variables.contains(variable)
						&& (potential.getCriterion() != null || (node.nodeType.equals(NodeType.UTILITY)
								&& node.getVariable().getDecisionCriterion() != null))) {
					potentialsVariable.add(potential);
				}
			}
		}
		return potentialsVariable;
	}

	/**
	 * Removes {@code potential} from this {@code ProbNet}
	 *
	 * @param potential Potential
	 * @return The node where the potential was located or {@code null} if
	 * it did not exists
	 *
	 */
	public Node removePotential(Potential potential) {
		List<Variable> variables = potential.getVariables();
		List<Node> candidateNodes = new ArrayList<>();
		// gets nodes that could contain the potential
		if (variables.size() == 0) {// Constant potentials can be in any
			// node
			candidateNodes = this.getNodes();
		} else {
			for (Variable variable : variables) {
				Node node = getNode(variable);
				if (node != null) {
					candidateNodes.add(getNode(variable));
				}
			}
		}

		// find in such nodes the potential to remove
		boolean wasFound = false;
		for (Node node : candidateNodes) {
			if (node != null) {
				List<Potential> potentialsNode = node.getPotentials();
				for (Potential potentialNode : potentialsNode) {
					if (potentialNode == potential) {
						if (node.removePotential(potentialNode)) {
							wasFound = true;
							return node;
						}
					}
				}
			}
		}

		if (!wasFound) {
			constantPotentials.remove(potential);
		}
		return null;
	}

	/**
	 * Removes all the potentials that contains the {@code variable}
	 * associated to {@code node}
	 *
	 * @param node {@code Node}
	 */
	public void removePotentials(Node node) {
		// get the nodes that contains potentials associated to the variable
		List<Node> nodes = new ArrayList<>();
		Variable variable = node.getVariable();
		nodes.add(node);
		// and its siblings
		nodes.addAll(getSiblings(node));
		// for each node extract its potentials ...
		for (Node otherNode : nodes) {
			List<Potential> potentialsNode = new ArrayList<>(otherNode.getPotentials());
			for (Potential potential : potentialsNode) {
				// ... and removes the potentials that contains the variable
				if (potential.getVariables().contains(variable)) {
					otherNode.removePotential(potential);
				}
			}
		}
	}

	/**
	 * Removes all the potentials in the array of potentials received.
	 *
	 * @param toRemovePotentials {@code ArrayList} of {@code Potential}
	 */
	public void removePotentials(List<Potential> toRemovePotentials) {
		if (toRemovePotentials != null) {
			for (Potential potential : toRemovePotentials) {
				removePotential(potential);
			}
		}
	}

	/**
	 * @param variable that not belongs
	 *                 . {@code Variable}
	 * @param nodeType . {@code NodeType}
	 * @return The {@code node} that points to {@code variable} in
	 * {@code this} network.
	 * Condition: the variable must not be in the ProbNet.
	 */
	public Node addNode(Variable variable, NodeType nodeType) {
		Node node = nodeDepot.getNode(nodeType, variable);
		if (node == null) {
			node = new Node(this, variable, nodeType);
			addNode(node);
		}
		return node;
	}

	/**
	 * @param node . {@code Node}
	 * Condition: the variable must not be in the ProbNet. This method is
	 * used to redo the {@code AddVariableEdit}, i.e., to
	 * reinsert a Node that has been removed.
	 */
	@Override public void addNode(Node node) {
		super.addNode(node);
		nodeDepot.addNode(node);
	}

	/**
	 *
	 * @param nameOfVariable {@code String}
	 * @throws NodeNotFoundException NodeNotFoundException
	 * @return The {@code Node} that matches the
	 * {@code nameOfVariable}
	 */
	public Node getNode(String nameOfVariable) throws NodeNotFoundException {
		Node node = nodeDepot.getNode(nameOfVariable);
		if (node == null) {
			throw new NodeNotFoundException(this, nameOfVariable);
		}
		return node;
	}

	/**
	 * @param nameOfVariable {@code String}
	 * @param nodeType       {@code NodeType}
	 * @return The node with {@code nameOfVariable} and
	 * {@code kindOfNode} if exists otherwise null
	 * @throws NodeNotFoundException NodeNotFoundException
	 */
	public Node getNode(String nameOfVariable, NodeType nodeType) throws NodeNotFoundException {
		Node node = nodeDepot.getNode(nameOfVariable, nodeType);
		if (node == null) {
			throw new NodeNotFoundException(this, nameOfVariable);
		}
		return node;
	}

	/**
	 * @param variable {@code Variable}
	 * @return The {@code Node} that matches the {@code Variable}
	 */
	public Node getNode(Variable variable) {
		return nodeDepot.getNode(variable);
	}

	/**
	 * @param variableName name of the variable
	 *                     . {@code String}
	 * @throws NodeNotFoundException NodeNotFoundException
	 * @return variable that matches {@code variableName} if exists,
	 * otherwise {@code null}. {@code Variable}
	 */
	public Variable getVariable(String variableName) throws NodeNotFoundException {
		Node node = getNode(variableName);
		return node.getVariable();
	}

	/**
	 * Returns variable on a certain timeSlice
	 *
	 * @param baseName  base name of the variable
	 * @param timeSlice time slice of the variable
	 * @return return variable with that basename and time slice
	 * @throws NodeNotFoundException NodeNotFoundException
	 */
	public Variable getVariable(String baseName, int timeSlice) throws NodeNotFoundException {
		return getVariable(baseName + " [" + timeSlice + "]");
	}

	/**
	 * @param variable       . a {@code Variable}
	 * @param timeDifference time slice diference
	 * @return a new variable having the same base name as the first argument
	 * but in the time slice indicated by the second argument
	 * @throws NodeNotFoundException NodeNotFoundException
	 * Condition: variable must be in the network and must be temporal
	 */
	public Variable getShiftedVariable(Variable variable, int timeDifference) throws NodeNotFoundException {
		return getVariable(variable.getBaseName(), variable.getTimeSlice() + timeDifference);
	}

	// TODO Con este nuevo metodo podemos evitar la chapuza hecha en
	// varios lugares de invocar getVariable para ver si lanzaba una excepcion.
	// Revisar el uso de esa excepcion y evitarla en lo posible.
	public boolean containsVariable(String variableName) {
		Node node;
		try {
			node = getNode(variableName);
		} catch (NodeNotFoundException e) {
			return false;
		}
		return (node != null);
	}

	public boolean containsVariable(Variable variable) {
		return getNode(variable) != null;
	}

	/**
	 * Returns true if this probNet contains the shifted variable
	 *
	 * @param variable       variable
	 * @param timeDifference time difference
	 * @return if the probNet contains that shifted variable returns true, else in other case
	 */
	public boolean containsShiftedVariable(Variable variable, int timeDifference) {
		int timeSlice = variable.getTimeSlice() + timeDifference;
		String baseName = variable.getBaseName();
		return containsVariable(baseName + " [" + timeSlice + "]");
	}

	/**
	 * Adds the received potential to the list of potentials of the conditioned
	 * variable (the first one).
	 *
	 * @param potential . {@code Potential}
	 * @return The {@code Node} in which the {@code potential}
	 * received has been added.
	 * Condition: network contains at least one chance variable
	 * Condition: potential type must correspond with the roles (discrete or
	 * continuous) of the variables in the network
	 * Condition: If A is the first variable in the potential and
	 * B<sub>0</sub> ... B<sub>n</sub> the remainders, there must
	 * be a directed link B<sub>i</sub> -&#82; A for every variable
	 * B<sub>i</sub> in the potential (other than A)
	 */
	public Node addPotential(Potential potential) {
		return addPotential(potential, null);
	}

	/**
	 * @param potential       {@code Potential}
	 * @param originalProbNet To get information from nodes when using this method to build a {@code ProbNet}
	 *                        from another one.
	 * @return The {@code Node} in which the {@code potential}
	 * received has been added.
	 * @see org.openmarkov.core.model.network.ProbNet#addPotential(Potential)
	 */
	public Node addPotential(Potential potential, ProbNet originalProbNet) {

		List<Variable> variables = potential.getVariables();

		for (Variable variable : variables) {
			// add the variables that are not yet in the network
			if (getNode(variable) == null) {
				if (originalProbNet == null || originalProbNet.getNode(variable) == null) {
					addNode(variable, NodeType.CHANCE);
				} else {
					addNode(variable, originalProbNet.getNode(variable).getNodeType());
				}
			}
		}

		// add the potential
		if (potential.getVariables().size() == 0) {
			// TODO - Change constant potentials (Potential vs TablePotential)
			this.constantPotentials.add((TablePotential) potential);
		} else {
			this.getNode(potential.getVariable(0)).addPotential(potential);
		}

		// draw links between the variables
		boolean isDirected = !containsConstraint(OnlyUndirectedLinks.class);
		if (isDirected) {
			// TODO - CHECK
			if (variables.size() == 0) {
				return null;
			}

			Node conditionedNode;
			conditionedNode = getNode(variables.get(0));
			for (int i = 1; i < variables.size(); i++) {
				Node conditioningNode = getNode(variables.get(i));
				if (!isParent(conditioningNode, conditionedNode)) {
					addLink(conditioningNode, conditionedNode, true);
				}
			}

		} else {
			int potentialSize = variables.size();
			for (int i = 0; i < potentialSize - 1; i++) {
				Node node1 = getNode(variables.get(i));
				for (int j = i + 1; j < potentialSize; j++) {
					Node node2 = getNode(variables.get(j));
					if (!isSibling(node1, node2)) {
						addLink(node1, node2, false);
					}
				}
			}
		}

		// TODO - Remove return in this method
		try {
			return this.getNode(variables.get(0));
		} catch (Exception e) {
			e.getStackTrace();
			return null;
		}
	}

	/**
	 * @param constraint {@code Class}
	 * @return {@code true} if this probabilistic network contains the
	 * received constraint type.
	 */
	public boolean hasConstraint(Class<?> constraint) {
		for (PNConstraint constraintProbNet : constraints) {
			if (constraintProbNet.getClass() == constraint) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @return All {@code Variable}s except utility nodes variables.
	 * {@code ArrayList} of {@code Variable}.
	 */
	public List<Variable> getChanceAndDecisionVariables() {
		List<Variable> variables = new ArrayList<>();
		List<Node> nodes = getNodes();
		for (Node node : nodes) {
			if (node.getNodeType() != NodeType.UTILITY) {
				variables.add(node.getVariable());
			}
		}
		return variables;
	}

	/**
	 * @param nodeType {@code NodeType}
	 * @return Variables corresponding to the node type received.
	 * {@code ArrayList} of {@code Variable}
	 */
	public List<Variable> getVariables(NodeType nodeType) {
		List<Variable> variablesType = new ArrayList<>();
		List<Node> nodes = getNodes(nodeType);
		for (Node node : nodes) {
			variablesType.add(node.getVariable());
		}
		return variablesType;
	}

	/**
	 * @return All the variables. {@code ArrayList} of
	 * {@code Variable}
	 */
	public List<Variable> getVariables() {
		List<Variable> variables = new ArrayList<>();
		for (Node node : getNodes()) {
			variables.add(node.getVariable());
		}
		return variables;
	}

	/**
	 * Removes {@code node} from {@code this ProbNet} and removes
	 * also the associated {@code node} from the associated
	 * {@code Graph}.
	 *
	 * @param node {@code Node}
	 */
	public void removeNode(Node node) {
		super.removeNode(node);
		nodeDepot.removeNode(node);
	}

	/**
	 * @param variable1 {@code Variable}
	 * @param variable2 {@code Variable}
	 * @param directed  {@code boolean}
	 */
	public void removeLink(Variable variable1, Variable variable2, boolean directed) {
		Node node1 = getNode(variable1);
		Node node2 = getNode(variable2);
		removeLink(node1, node2, directed);
	}

	/**
	 * @return Number of potentials. {@code int}
	 */
	public int getNumPotentials() {
		return nodeDepot.getNumPotentials();
	}

	public PNESupport getPNESupport() {
		return pNESupport;
	}

	/**
	 * @return String
	 */
	public String toString() {
		StringBuilder out = new StringBuilder();
		out.append("Type: ").append(networkType.toString()).append("\n");
		List<Node> nodes = getNodes();
		int numPotentials = getNumPotentials();
		int numNodes = nodes.size();
		if (numNodes == 0) {
			out.append("No nodes.\n");
		} else {
			out.append("Nodes (").append(numNodes).append("): ");
			for (Node node : nodes) {
				out.append("\n  ").append(node.toString());
			}
			out.append("\n");
		}
		if (numPotentials == 0) {
			out.append("No potentials.\n");
		} else {
			out.append("Number of potentials: ").append(numPotentials).append("\n");
		}
		if (constraints.size() == 0) {
			out.append("No constraints\n");
		} else {
			out.append("Constraints: ");
			for (int i = 0; i < constraints.size(); i++) {
				String strConstraint = constraints.get(i).toString();
				strConstraint = strConstraint.substring(strConstraint.lastIndexOf('.') + 1, strConstraint.length());
				out.append(strConstraint);
				if (i < constraints.size() - 1) {
					out.append(", ");
				}
			}
			out.append("\n");
		}
		if (agents != null) {
			out.append("\n");
			out.append("Agents:\n").append(agents.toString());
		}
		return out.toString();
	}

	/**
	 * @return the comment
	 */
	public String getComment() {
		return comment;
	}

	/**
	 * @param comment the comment to set
	 */
	public void setComment(String comment) {
		this.comment = comment;
	}

	/**
	 * @return the defaultStates
	 */
	public State[] getDefaultStates() {
		State[] states = new State[defaultStates.length];

		for (int stateIndex = 0; stateIndex < defaultStates.length; stateIndex++) {
			states[stateIndex] = new State(defaultStates[stateIndex]);
		}

		return states;
	}

	/**
	 * @param defaultStates the defaultStates to set
	 */
	public void setDefaultStates(State[] defaultStates) {
		this.defaultStates = defaultStates;
	}

	/**
	 * Condition: oldNode belongs to this probNet
	 * @param coordinateXOffset Coordinate X offset
	 * @param coordinateYOffset Coordinate Y offset
	 * @param oldNode Old node
	 * @param timeDifference Time difference
	 * @return Node
	 */
	public Node addShiftedNode(Node oldNode, int timeDifference, double coordinateXOffset, double coordinateYOffset) {
		Variable oldVariable = oldNode.getVariable();
		Variable newVariable = (Variable) oldVariable.clone();
		newVariable.setTimeSlice(oldVariable.getTimeSlice() + timeDifference);
		Node newNode = addNode(newVariable, oldNode.getNodeType());
		newNode.setCoordinateX(oldNode.getCoordinateX() + coordinateXOffset);
		newNode.setCoordinateY(oldNode.getCoordinateY() + coordinateYOffset);
		// TODO Hacer clon para node y quitar estas lineas
		newNode.setPurpose(oldNode.getPurpose());
		newNode.setRelevance(oldNode.getRelevance());
		newNode.setComment(oldNode.getComment());
		newNode.additionalProperties = additionalProperties;
		return newNode;
	}

	/**
	 * @return {@code ArrayList} of {@code StringsWithProperties}
	 */
	public List<StringWithProperties> getAgents() {
		return agents;
	}

	/**
	 * @param agents . {@code StringsWithProperties}
	 */
	public void setAgents(List<StringWithProperties> agents) {
		this.agents = agents;
	}

	/**
	 * @return {@code StringsWithProperties}
	 */
	public List<Criterion> getDecisionCriteria() {
		return decisionCriteria;
	}

	/**
	 * @param decisionCriteria . {@code StringsWithProperties}
	 */
	public void setDecisionCriteria(List<Criterion> decisionCriteria) {
		this.decisionCriteria = decisionCriteria;
	}

	public boolean getShowCommentWhenOpening() {
		return showCommentWhenOpening;
	}

	public void setShowCommentWhenOpening(boolean showCommentWhenOpening) {
		this.showCommentWhenOpening = showCommentWhenOpening;
	}

	/**
	 * Node calls this method when its variable instance has been changed, so
	 * that probNet updates its reference too.
	 *
	 * @param oldVariable old variable to be updated
	 */
	public void updateVariable(Variable oldVariable) {
		Node node = nodeDepot.getNode(oldVariable);
		nodeDepot.removeNode(oldVariable);
		nodeDepot.addNode(node);
	}

	public InferenceOptions getInferenceOptions() {
		return inferenceOptions;
	}

	public void setInferenceOptions(InferenceOptions inferenceOptions) {
		this.inferenceOptions = inferenceOptions;
	}

	public CycleLength getCycleLength() {
		return cycleLength;
	}

	public void setCycleLength(CycleLength temporalUnit) {
		this.cycleLength = temporalUnit;
	}

	public ProbNet deepCopy() {
		ProbNet copyNet = new ProbNet(this.networkType);
		copyNet.constraints = new ArrayList<>();

		// copy decision criteria
		if (this.getDecisionCriteria() != null) {
			List<Criterion> newDecisionCriteria = new ArrayList<>();
			for (Criterion criterion : this.getDecisionCriteria()) {
				Criterion newCriterion = new Criterion(criterion);
				newDecisionCriteria.add(newCriterion);
			}
			copyNet.setDecisionCriteria(newDecisionCriteria);
		}

		// copy net name
		copyNet.setName(name);

		// Copy temporal units
		if (this.getCycleLength() != null) {
			copyNet.setCycleLength(new CycleLength(this.getCycleLength()));
		}

		//Copy Inference Options
		copyNet.setInferenceOptions(new InferenceOptions(this.getInferenceOptions()));

		// copy constraints
		int numConstraints = constraints.size();
		for (int i = 1; i < numConstraints; i++) {
			try {
				copyNet.addConstraint(constraints.get(i), false);
			} catch (ConstraintViolationException e) {
				// Unreachable code because constraints are not tested in copy
			}
		}

		List<Node> nodes = getNodes();
		// Adds variables and create corresponding nodes. Also add potentials
		for (Node node : nodes) {
			Node newNode = node.clone(copyNet);
			copyNet.addNode(newNode);
		}

		// Add new potentials and update list of neighbours
		for (Node node : nodes) {
			List<Node> neighbours = this.getNeighbors(node);
			for (Node neighbour : neighbours) {
				try {
					// TODO - Problem?
					neighbour = copyNet.getNode(neighbour.getName());
				} catch (NodeNotFoundException e) {
					e.printStackTrace();
				}
			}

			ArrayList<Potential> newPotentials = new ArrayList<>();
			for (Potential potential : node.getPotentials()) {
				newPotentials.add(potential.deepCopy(copyNet));
			}

			try {
				copyNet.getNode(node.getName()).setPotentials(newPotentials);
			} catch (NodeNotFoundException e) {
				e.printStackTrace();
			}
		}

		// Adds links
		// Copy explicit links' properties
		// TODO - Check this code
		if (hasExplicitLinks()) {
			copyNet.makeLinksExplicit(false);
			for (Link<Node> originalLink : getLinks()) {
				try {

					Node copyNode1 = copyNet.getNode(originalLink.getNode1().getVariable().getName());
					Node copyNode2 = copyNet.getNode(originalLink.getNode2().getVariable().getName());

					Link<Node> copyLink = copyNet.addLink(copyNode1, copyNode2, originalLink.isDirected());
					if (originalLink.getRestrictionsPotential() != null) {
						copyLink.setRestrictionsPotential(originalLink.getRestrictionsPotential().deepCopy(copyNet));
					}

					List<PartitionedInterval> newRevealingIntervals = new ArrayList<>();
					for (PartitionedInterval interval : originalLink.getRevealingIntervals()) {
						PartitionedInterval newInterval = new PartitionedInterval(interval.limits.clone(),
								interval.belongsToLeftSide.clone());
						newRevealingIntervals.add(newInterval);
					}

					copyLink.setRevealingIntervals(newRevealingIntervals);
					copyLink.setRevealingStates(new ArrayList<>(originalLink.getRevealingStates()));
				} catch (NodeNotFoundException e) {
					e.printStackTrace();
				}
			}
		} else {
			for (Node node : nodes) {
				try {
					Node copyNode = copyNet.getNode(node.getVariable().getName());
					List<Node> siblings = getSiblings(node);
					for (Node sibling : siblings) {
						Node copySibling = copyNet.getNode(sibling.getVariable().getName());
						if (!copyNet.isSibling(copyNode, copySibling)) {
							copyNet.addLink(copyNode, copySibling, false);
						}
					}
					List<Node> children = getChildren(node);
					for (Node child : children) {
						Node copyChild = copyNet.getNode(child.getVariable().getName());
						copyNet.addLink(copyNode, copyChild, true);
					}
				} catch (NodeNotFoundException e) {
					e.printStackTrace();
				}
			}

		}
		// copy listeners
		copyNet.getPNESupport().setListeners(pNESupport.getListeners());
		// Copy additionalProperties
		Set<String> keys = additionalProperties.keySet();
		HashMap<String, String> copyProperties = new HashMap<>();
		for (String key : keys) {
			copyProperties.put(key, additionalProperties.get(key));
		}
		copyNet.additionalProperties = copyProperties;

		return copyNet;
	}

	public Set<TablePotential> getConstantPotentials() {
		return constantPotentials;
	}

	public void setConstantPotentials(Set<TablePotential> constantPotentials) {
		this.constantPotentials = constantPotentials;
	}

}
