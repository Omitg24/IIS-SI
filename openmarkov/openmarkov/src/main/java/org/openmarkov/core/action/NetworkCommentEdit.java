/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.action;

import org.openmarkov.core.model.network.ProbNet;

/**
 * {@code NetworkCommentEdit} is a simple edit that allow modify a network
 * comment.
 *
 * @author Miguel Palacios
 * @version 1.0 21/12/10
 */
@SuppressWarnings("serial") public class NetworkCommentEdit extends SimplePNEdit {
	/**
	 * The current network comment
	 */
	private String currentComment;
	/**
	 * The new network comment
	 */
	private String newComment;

	/**
	 * Indicates whether the comment should be shown when opening the net
	 */
	private boolean showCommentWhenOpening;

	/**
	 * Creates a new {@code NetworkCommentEdit} with the network and new
	 * comment specified.
	 *
	 * @param probNet    the network that will be edited.
	 * @param newComment the new comment
	 * @param showCommentWhenOpening {@code boolean}
	 */
	public NetworkCommentEdit(ProbNet probNet, String newComment, boolean showCommentWhenOpening) {
		super(probNet);
		this.newComment = newComment;
		this.currentComment = probNet.getComment();
		this.showCommentWhenOpening = showCommentWhenOpening;
	}

	@Override public void doEdit() {
		probNet.setComment(newComment);
		probNet.setShowCommentWhenOpening(showCommentWhenOpening);
	}

	public void undo() {
		super.undo();
		probNet.setComment(currentComment);

	}
}


