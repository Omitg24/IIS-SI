/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.oopn;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.event.UndoableEditEvent;
import javax.swing.undo.UndoableEdit;

import org.openmarkov.core.action.AddLinkEdit;
import org.openmarkov.core.action.AddNodeEdit;
import org.openmarkov.core.action.CRemoveNodeEdit;
import org.openmarkov.core.action.CompoundPNEdit;
import org.openmarkov.core.action.ICIPotentialEdit;
import org.openmarkov.core.action.InvertLinkEdit;
import org.openmarkov.core.action.NodeStateEdit;
import org.openmarkov.core.action.PNEdit;
import org.openmarkov.core.action.PNUndoableEditListener;
import org.openmarkov.core.action.PotentialChangeEdit;
import org.openmarkov.core.action.RemoveLinkEdit;
import org.openmarkov.core.action.RemoveNodeEdit;
import org.openmarkov.core.action.SetPotentialEdit;
import org.openmarkov.core.exception.ConstraintViolationException;
import org.openmarkov.core.exception.DoEditException;
import org.openmarkov.core.exception.NodeNotFoundException;
import org.openmarkov.core.exception.NonProjectablePotentialException;
import org.openmarkov.core.exception.WrongCriterionException;
import org.openmarkov.core.model.network.Node;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.potential.Potential;
import org.openmarkov.core.model.network.potential.canonical.ICIPotential;
import org.openmarkov.core.model.network.type.NetworkType;
import org.openmarkov.core.oopn.exception.InstanceAlreadyExistsException;

/**
 * Object Oriented Probabilistic Network.
 */
public class OOPNet extends ProbNet implements PNUndoableEditListener {
	private LinkedHashMap<String, ProbNet> classes = new LinkedHashMap<>();
	private Map<String, Instance> instances = new HashMap<>();
	private List<ReferenceLink> referenceLinks = new ArrayList<>();

	/**
	 * Constructor for OOPNet.
	 */
	public OOPNet() {
		super();
	}

	/**
	 * Constructor for OOPNet.
	 *
	 * @param networkType Network type
	 */
	public OOPNet(NetworkType networkType) {
		super(networkType);
	}

	/**
	 * Constructor for OOPNet.
	 *
	 * @param probNet Network
	 */
	public OOPNet(ProbNet probNet) {
		super();
		try {
			setNetworkType(probNet.getNetworkType());
		} catch (ConstraintViolationException e1) {
			e1.printStackTrace();
		}
		// copy constraints
		int numConstraints = probNet.getConstraints().size();
		for (int i = 1; i < numConstraints; i++) {
			try {
				addConstraint(probNet.getConstraints().get(i), false);
			} catch (ConstraintViolationException e) {
				// Unreachable code because constraints are not tested in copy
			}
		}
		List<Node> nodes = probNet.getNodes();
		// Adds variables and create corresponding nodes. Also add potentials
		for (Node node : nodes) {
			// Add variables and create corresponding nodes
			Variable variable = node.getVariable();
			Node newNode = null;
			newNode = addNode(variable, node.getNodeType());
			newNode.setCoordinateX(node.getCoordinateX());
			newNode.setCoordinateY(node.getCoordinateY());
			newNode.setPotentials(node.getPotentials());

			// TODO Hacer clon para node y quitar estas lineas
			newNode.setPurpose(node.getPurpose());
			newNode.setRelevance(node.getRelevance());
			newNode.setComment(node.getComment());
			newNode.additionalProperties = additionalProperties;
		}

		// Add links
		for (Node node1 : nodes) {
			Variable variable1 = node1.getVariable();
			Node newNode1 = this.getNode(variable1);
			List<Node> neighbors = node1.getNeighbors();
			for (Node node2 : neighbors) {
				Variable variable2 = node2.getVariable();
				Node newNode2 = this.getNode(variable2);
				if (probNet.isSibling(node1, node2)) {
					if (!isSibling(newNode1, newNode2)) {
						addLink(newNode1, newNode2, false);
					}
				}
				if (probNet.isChild(node2, node1)) {
					addLink(newNode1, newNode2, true);
				}
			}
		}

		// copy listeners
		getPNESupport().setListeners(probNet.getPNESupport().getListeners());

		// Copy additionalProperties
		Set<String> keys = probNet.additionalProperties.keySet();
		HashMap<String, String> copyProperties = new HashMap<>();
		for (String key : keys) {
			copyProperties.put(key, probNet.additionalProperties.get(key));
		}
		additionalProperties = copyProperties;

	}

	/**
	 * @param instance Instance
	 * @throws InstanceAlreadyExistsException InstanceAlreadyExistsException
	 */
	public void addInstance(Instance instance) throws InstanceAlreadyExistsException {
		if (instances.containsKey(instance.getName())) {
			throw new InstanceAlreadyExistsException();
		} else {
			instance.getClassNet().getPNESupport().addUndoableEditListener(this);
			instances.put(instance.getName(), instance);
		}
	}

	/**
	 * @return instance list
	 */
	public Map<String, Instance> getInstances() {
		return instances;
	}

	/**
	 * Add an instance link
	 *
	 * @param link Reference link
	 */
	public void addReferenceLink(ReferenceLink link) {
		referenceLinks.add(link);
	}

	/**
	 * @return the instanceLinks
	 */
	public List<ReferenceLink> getReferenceLinks() {
		return referenceLinks;
	}

	/**
	 * Removes an instance Link
	 *
	 * @param link Reference link
	 */
	public void removeReferenceLink(ReferenceLink link) {
		referenceLinks.remove(link);
	}

	/**
	 * Returns the equivalent node in {@code sourceInstance} to the
	 * {@code Node} in {@code destinationInstance}
	 *
	 * @param sourceInstance Source instance
	 * @param destInstance destination instance
	 * @param node Node
	 * @return The equivalent node in source instance to the node in the destination instance
	 */
	private Node getEquivalentNode(Instance sourceInstance, Instance destInstance, Node node) {
		Node equivalentNode = null;
		int i = 0;
		String nodeName = node.getName();
		nodeName = nodeName.replace(destInstance.getName() + ".", "");
		while (equivalentNode == null && i < sourceInstance.getNodes().size()) {
			String equivalentNodeName = sourceInstance.getNodes().get(i).getName();
			equivalentNodeName = equivalentNodeName.replace(sourceInstance.getName() + ".", "");
			if (equivalentNodeName.equals(nodeName)) {
				equivalentNode = sourceInstance.getNodes().get(i);
			}
			++i;
		}
		return equivalentNode;
	}

	/**
	 * Unrolls instances and returns a plain probabilistic network
	 *
	 * @return Plain probabilistic network
	 */
	public ProbNet getPlainProbNet() {
		ProbNet probNet = copy();
		probNet.makeLinksExplicit(false);
		for (Instance instance : getInstances().values()) {
			for (Instance subInstance : instance.getSubInstances().values()) {
				if (subInstance.isInput()) {
					List<ReferenceLink> linksToParameter = getLinksToParameter(subInstance);
					for (Node node : subInstance.getNodes()) {
						Node formalNode = probNet.getNode(node.getVariable());
						List<Node> paramNodes = new ArrayList<>();
						for (ReferenceLink link : linksToParameter) {
							InstanceReferenceLink instanceLink = (InstanceReferenceLink) link;
							Node equivalentNode = getEquivalentNode(instanceLink.getSourceInstance(),
									instanceLink.getDestSubInstance(), node);
							Node paramNode = probNet.getNode(equivalentNode.getVariable());
							if (paramNode != null) {
								paramNodes.add(paramNode);
							}
						}
						if (paramNodes.size() == 1) {
							replaceNode(probNet, formalNode, paramNodes.get(0));
						} else {//if (paramNodes.size () > 1){
							replaceNodes(probNet, formalNode, paramNodes);
						}
					}
					// Remove formal parameter nodes
					if (true)//linksToParameter.size () > 0)
					{
						for (Node node : subInstance.getNodes()) {
							probNet.removeNode(probNet.getNode(node.getVariable()));
						}
					}
				}
			}
		}

		for (ReferenceLink link : getReferenceLinks()) {
			if (link instanceof NodeReferenceLink) {
				NodeReferenceLink nodeLink = (NodeReferenceLink) link;
				Node sourceNode = probNet.getNode(nodeLink.getSourceNode().getVariable());
				Node destinationNode = probNet.getNode(nodeLink.getDestinationNode().getVariable());
				replaceNode(probNet, destinationNode, sourceNode);
				probNet.removeNode(destinationNode);
			}
		}

		return probNet;
	}

	private void replaceNodes(ProbNet probNet, Node formalNode, List<Node> paramNodes) {
		// Update potentials
		HashMap<Potential, Potential> potentialsToReplace = new HashMap<>();
		for (Potential potential : probNet.getPotentials(formalNode.getVariable())) {
			if (potential instanceof ICIPotential) {
				ICIPotential potentialCopy;

				potentialCopy = (ICIPotential) potential.copy();
				double[] noisyParameters = potentialCopy.getNoisyParameters(formalNode.getVariable());
				if (noisyParameters != null) {
					potentialCopy = (ICIPotential) potentialCopy.removeVariable(formalNode.getVariable());
					for (Node paramNode : paramNodes) {
						potentialCopy = (ICIPotential) potentialCopy.addVariable(paramNode.getVariable());
						potentialCopy.setNoisyParameters(paramNode.getVariable(), noisyParameters);
					}
					potentialsToReplace.put(potential, potentialCopy);
				}
			}
		}
		for (Node node : probNet.getNodes()) {
			for (Potential potentialToReplace : potentialsToReplace.keySet()) {
				if (node.getPotentials().contains(potentialToReplace)) {
					List<Potential> potentials = node.getPotentials();
					potentials.remove(potentialToReplace);
					potentials.add(potentialsToReplace.get(potentialToReplace));
					node.setPotentials(potentials);
				}
			}
		}
		// Update Links
		// Replace links to children
		for (Node child : formalNode.getChildren()) {
			probNet.removeLink(formalNode, child, true);
			for (Node paramNode : paramNodes) {
				if (!child.isParent(paramNode)) {
					probNet.addLink(paramNode, child, true);
				}
			}
		}
	}

	private List<ReferenceLink> getLinksToParameter(Instance instance) {
		List<ReferenceLink> links = new ArrayList<>();
		for (ReferenceLink link : getReferenceLinks()) {
			if (link instanceof InstanceReferenceLink && ((InstanceReferenceLink) link).getDestSubInstance()
					.equals(instance)) {
				links.add(link);
			}
		}
		return links;
	}

	/**
	 * @param probNet Network
	 * @param formalNode Formal node
	 * @param paramNode Param node
	 */
	private void replaceNode(ProbNet probNet, Node formalNode, Node paramNode) {
		// Update potentials
		HashMap<Potential, Potential> potentialsToReplace = new HashMap<>();
		for (Potential potential : probNet.getPotentials(formalNode.getVariable())) {
			Potential potentialCopy = potential.copy();
			potentialCopy.replaceVariable(formalNode.getVariable(), paramNode.getVariable());
			potentialsToReplace.put(potential, potentialCopy);
		}
		for (Node node : probNet.getNodes()) {
			for (Potential potentialToReplace : potentialsToReplace.keySet()) {
				if (node.getPotentials().contains(potentialToReplace)) {
					List<Potential> potentials = node.getPotentials();
					potentials.remove(potentialToReplace);
					potentials.add(potentialsToReplace.get(potentialToReplace));
					node.setPotentials(potentials);
				}
			}
		}
		// Update Links
		// Replace links to children
		for (Node child : formalNode.getChildren()) {
			probNet.removeLink(formalNode, child, true);
			if (!child.isParent(paramNode)) {
				probNet.addLink(paramNode, child, true);
			}
		}
		// Replace links from parents
		for (Node parent : formalNode.getParents()) {
			probNet.removeLink(parent, formalNode, true);
			if (!paramNode.isParent(parent)) {
				probNet.addLink(parent, paramNode, true);
			}
		}
		// Replace links between siblings
		for (Node sibling : formalNode.getSiblings()) {
			probNet.removeLink(sibling, formalNode, false);
			probNet.addLink(sibling, paramNode, false);
		}
	}

	@Override public void undoableEditHappened(UndoableEditEvent e) {
		if (e.getEdit() instanceof PNEdit) {
			PNEdit edit = (PNEdit) e.getEdit();
			List<PNEdit> simpleEdits = new ArrayList<>();
			if (edit instanceof CompoundPNEdit) {
				try {
					for (UndoableEdit undoableEdit : ((CompoundPNEdit) edit).getEdits()) {
						simpleEdits.add((PNEdit) undoableEdit);
					}
				} catch (NonProjectablePotentialException | WrongCriterionException e1) {
					e1.printStackTrace();
				}
			} else {
				simpleEdits.add((PNEdit) edit);
			}

			ProbNet classNet = edit.getProbNet();
			PNEdit newEdit = null;

			for (Instance instance : instances.values()) {
				if (instance.getClassNet().equals(classNet)) {
					String instanceName = instance.getName();
					for (PNEdit simpleEdit : simpleEdits) {
						if (simpleEdit instanceof AddLinkEdit) {
							AddLinkEdit addLinkEdit = (AddLinkEdit) simpleEdit;
							Variable variable1;
							try {
								variable1 = getVariable(instanceName + "." + addLinkEdit.getVariable1().getName());
								Variable variable2 = getVariable(
										instanceName + "." + addLinkEdit.getVariable2().getName());
								newEdit = new AddLinkEdit(this, variable1, variable2, addLinkEdit.isDirected());
							} catch (NodeNotFoundException e1) {
								e1.printStackTrace();
							}
						} else if (simpleEdit instanceof RemoveLinkEdit) {
							RemoveLinkEdit removeLinkEdit = (RemoveLinkEdit) simpleEdit;
							Variable variable1;
							try {
								variable1 = getVariable(instanceName + "." + removeLinkEdit.getVariable1().getName());
								Variable variable2 = getVariable(
										instanceName + "." + removeLinkEdit.getVariable2().getName());
								newEdit = new RemoveLinkEdit(this, variable1, variable2, removeLinkEdit.isDirected());
							} catch (NodeNotFoundException e1) {
								e1.printStackTrace();
							}

						} else if (simpleEdit instanceof InvertLinkEdit) {
							InvertLinkEdit invertLinkEdit = (InvertLinkEdit) simpleEdit;
							Variable variable1;
							try {
								variable1 = getVariable(instanceName + "." + invertLinkEdit.getVariable1().getName());
								Variable variable2 = getVariable(
										instanceName + "." + invertLinkEdit.getVariable2().getName());
								newEdit = new InvertLinkEdit(this, variable1, variable2, invertLinkEdit.isDirected());
							} catch (NodeNotFoundException e1) {
								e1.printStackTrace();
							}
						} else if (simpleEdit instanceof AddNodeEdit) {
							AddNodeEdit addNodeEdit = (AddNodeEdit) simpleEdit;

							Variable newVariable = new Variable(addNodeEdit.getVariable());
							newVariable.setName(instanceName + "." + newVariable.getName());

							//determine position of new node inside instance, using a node as reference
							Point2D.Double position = new Point2D.Double();
							if (!classNet.getNodes().isEmpty()) {

								Node referenceNode = classNet.getNodes().get(0);
								Node referenceInstanceNode = null;
								try {
									referenceInstanceNode = getNode(
											instanceName + "." + referenceNode.getVariable().getName());
								} catch (NodeNotFoundException e1) {
									e1.printStackTrace();
								}
								double x = addNodeEdit.getCursorPosition().getX() - referenceNode.getCoordinateX()
										+ referenceInstanceNode.getCoordinateX();
								double y = addNodeEdit.getCursorPosition().getY() - referenceNode.getCoordinateY()
										+ referenceInstanceNode.getCoordinateY();
								position = new Point2D.Double(x, y);
							}
							newEdit = new AddNodeEdit(this, newVariable, addNodeEdit.getNodeType(), position);
						} else if (simpleEdit instanceof RemoveNodeEdit) {
							RemoveNodeEdit removeNodeEdit = (RemoveNodeEdit) simpleEdit;
							newEdit = new RemoveNodeEdit(this, removeNodeEdit.getVariable());
						} else if (simpleEdit instanceof CRemoveNodeEdit) {
							CRemoveNodeEdit removeNodeEdit = (CRemoveNodeEdit) simpleEdit;
							Node nodeToRemove = null;
							try {
								nodeToRemove = getNode(instanceName + "." + removeNodeEdit.getVariable().getName());
							} catch (NodeNotFoundException e1) {
								e1.printStackTrace();
							}
							newEdit = new CRemoveNodeEdit(this, nodeToRemove);
						} else if (simpleEdit instanceof NodeStateEdit) {
							NodeStateEdit nodeStateEdit = (NodeStateEdit) simpleEdit;
							Node nodeInInstance = null;
							try {
								nodeInInstance = getNode(instanceName + "." + nodeStateEdit.getNode().getName());
							} catch (NodeNotFoundException e1) {
								e1.printStackTrace();
							}
							newEdit = new NodeStateEdit(nodeInInstance, nodeStateEdit.getStateAction(),
									nodeStateEdit.getIndexState(), nodeStateEdit.getNewState().getName());
						} else if (simpleEdit instanceof SetPotentialEdit) {
							try {
								SetPotentialEdit setPotentialEdit = (SetPotentialEdit) simpleEdit;
								Node node = getNode(instanceName + "." + setPotentialEdit.getNode().getName());
								if (setPotentialEdit.getNewPotential() != null) {
									Potential newPotential = setPotentialEdit.getNewPotential().copy();
									for (Variable variable : setPotentialEdit.getNewPotential().getVariables()) {
										newPotential.replaceVariable(variable,
												getVariable(instanceName + "." + variable.getName()));
									}

									newEdit = new SetPotentialEdit(node, newPotential);
								} else {
									newEdit = new SetPotentialEdit(node, setPotentialEdit.getNewPotentialType());
								}
							} catch (NodeNotFoundException e1) {
								e1.printStackTrace();
							}

						} else if (simpleEdit instanceof PotentialChangeEdit) {
							try {
								PotentialChangeEdit changePotentialEdit = (PotentialChangeEdit) simpleEdit;

								// Find oldPotential in instance
								Potential oldPotential = findEquivalentPotentialInInstance(instanceName,
										changePotentialEdit.getOldPotential());

								// Copy newPotential with the variables in the instance
								Potential newPotential = changePotentialEdit.getNewPotential().copy();
								for (Variable variable : changePotentialEdit.getNewPotential().getVariables()) {
									newPotential.replaceVariable(variable,
											getVariable(instanceName + "." + variable.getName()));
								}

								newEdit = new PotentialChangeEdit(this, oldPotential, newPotential);
							} catch (NodeNotFoundException e1) {
								e1.printStackTrace();
							}
						} else if (simpleEdit instanceof ICIPotentialEdit) {
							ICIPotentialEdit iciPotentialEdit = (ICIPotentialEdit) simpleEdit;
							// Find oldPotential in instance
							ICIPotential potential = (ICIPotential) findEquivalentPotentialInInstance(instanceName,
									iciPotentialEdit.getPotential());
							if (iciPotentialEdit.isNoisyParameter()) {
								Variable variable = null;
								try {
									variable = getVariable(
											instanceName + "." + iciPotentialEdit.getVariable().getName());
									newEdit = new ICIPotentialEdit(this, potential, variable,
											iciPotentialEdit.getNoisyParameters());
								} catch (NodeNotFoundException e1) {
									e1.printStackTrace();
								}
							} else {
								newEdit = new ICIPotentialEdit(this, potential, iciPotentialEdit.getLeakyParameters());
							}
						}

						if (newEdit != null) {
							try {
								doEdit(newEdit);
							} catch (ConstraintViolationException | NonProjectablePotentialException | WrongCriterionException | DoEditException e1) {
								e1.printStackTrace();
							}
						}
					}
				}
			}
		}

	}

	@Override public void undoableEditWillHappen(UndoableEditEvent event)
			throws ConstraintViolationException, NonProjectablePotentialException,
			WrongCriterionException {
		// Do nothing

	}

	@Override public void undoEditHappened(UndoableEditEvent event) {
		// TODO Auto-generated method stub

	}

	private Potential findEquivalentPotentialInInstance(String instanceName, Potential potential) {
		Potential oldPotential = null;
		List<Variable> instanceVariables = new ArrayList<>();
		try {
			for (Variable variable : potential.getVariables()) {
				instanceVariables.add(getVariable(instanceName + "." + variable.getName()));
			}
			oldPotential = findPotentialByVariables(instanceVariables);
		} catch (NodeNotFoundException e) {
			e.printStackTrace();
		}
		return oldPotential;
	}

	private Potential findPotentialByVariables(List<Variable> variables) {
		int i = 0;
		List<Potential> potentials = getPotentials();
		Potential potential = null;

		while (i < potentials.size() && potential == null) {
			boolean match = potentials.get(i).getVariables().size() == variables.size();
			int j = 0;
			while (match && j < variables.size()) {
				match &= potentials.get(i).getVariables().contains(variables.get(j));
				++j;
			}
			if (match) {
				potential = potentials.get(i);
			}
			++i;
		}
		return potential;
	}

	/**
	 * Returns the classes.
	 *
	 * @return the classes.
	 */
	public LinkedHashMap<String, ProbNet> getClasses() {
		return classes;
	}

	/**
	 * Sets the classes.
	 *
	 * @param classes the classes to set.
	 */
	public void setClasses(LinkedHashMap<String, ProbNet> classes) {
		this.classes = classes;
	}

	public void fillClassList() {
		this.classes = getClassList();
	}

	protected LinkedHashMap<String, ProbNet> getClassList() {
		LinkedHashMap<String, ProbNet> classes = new LinkedHashMap<>();
		for (Instance instance : getInstances().values()) {
			if (instance.getClassNet() instanceof OOPNet) {
				classes.putAll(((OOPNet) instance.getClassNet()).getClassList());
			}
			if (!classes.containsKey(instance.getClassNet().getName())) {
				classes.put(instance.getClassNet().getName(), instance.getClassNet());
			}
		}

		return classes;
	}

}
