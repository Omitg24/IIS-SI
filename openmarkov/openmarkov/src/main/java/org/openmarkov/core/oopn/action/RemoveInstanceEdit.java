/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.oopn.action;

import java.util.HashSet;

import javax.swing.undo.CannotUndoException;

import org.openmarkov.core.action.CRemoveNodeEdit;
import org.openmarkov.core.action.CompoundPNEdit;
import org.openmarkov.core.action.RemoveLinkEdit;
import org.openmarkov.core.exception.DoEditException;
import org.openmarkov.core.exception.NonProjectablePotentialException;
import org.openmarkov.core.exception.WrongCriterionException;
import org.openmarkov.core.model.graph.Link;
import org.openmarkov.core.model.network.Node;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.oopn.Instance;
import org.openmarkov.core.oopn.InstanceReferenceLink;
import org.openmarkov.core.oopn.OOPNet;
import org.openmarkov.core.oopn.ReferenceLink;
import org.openmarkov.core.oopn.exception.InstanceAlreadyExistsException;

/**
 * @author ibermejo
 */
@SuppressWarnings("serial") public class RemoveInstanceEdit extends CompoundPNEdit {

	private Instance instance;
	private HashSet<Node> nodesToRemove;
	private HashSet<Link<Node>> linksToRemove;
	private HashSet<ReferenceLink> instanceLinksToRemove;

	/**
	 *
	 * @param probNet Network
	 * @param instanceName Instance name
	 */
	public RemoveInstanceEdit(ProbNet probNet, String instanceName) {
		super(probNet);
		this.instance = ((OOPNet) probNet).getInstances().get(instanceName);

		nodesToRemove = new HashSet<>();
		linksToRemove = new HashSet<>();
		instanceLinksToRemove = new HashSet<>();
		for (Node node : instance.getNodes()) {
			nodesToRemove.add(node);
			linksToRemove.addAll(node.getLinks());
		}
		for (ReferenceLink link : ((OOPNet) probNet).getReferenceLinks()) {
			if (link instanceof InstanceReferenceLink) {
				InstanceReferenceLink instanceLink = (InstanceReferenceLink) link;

				if (instanceLink.getSourceInstance().equals(this.instance) || instanceLink.getDestInstance()
						.equals(this.instance)) {
					instanceLinksToRemove.add(link);
				}
			}
		}
	}

	@Override public void generateEdits() throws NonProjectablePotentialException, WrongCriterionException {

		for (Link<Node> link : linksToRemove) {
			edits.add(new RemoveLinkEdit(probNet, link.getNode1().getVariable(), link.getNode2().getVariable(),
					link.isDirected()));
		}

		for (Node node : nodesToRemove) {
			edits.add(new CRemoveNodeEdit(probNet, node));
		}
	}

	@Override public void doEdit() throws DoEditException, NonProjectablePotentialException, WrongCriterionException {
		super.doEdit();
		((OOPNet) probNet).getInstances().remove(instance.getName());
		for (ReferenceLink instanceLink : instanceLinksToRemove) {
			((OOPNet) probNet).getReferenceLinks().remove(instanceLink);
		}
	}

	@Override public void undo() throws CannotUndoException {
		// TODO Auto-generated method stub
		super.undo();
		try {
			((OOPNet) probNet).addInstance(instance);
		} catch (InstanceAlreadyExistsException e) {
			//Impossible to get here
		}
		for (ReferenceLink instanceLink : instanceLinksToRemove) {
			((OOPNet) probNet).getReferenceLinks().add(instanceLink);
		}
	}

}
