/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.action;

import java.util.Vector;

import javax.swing.undo.CompoundEdit;
import javax.swing.undo.UndoableEdit;

import org.openmarkov.core.exception.DoEditException;
import org.openmarkov.core.exception.NonProjectablePotentialException;
import org.openmarkov.core.exception.WrongCriterionException;
import org.openmarkov.core.model.network.ProbNet;

/**
 * A compound edit is a complex edition composed of several editions. This is an
 * abstract class.
 */
@SuppressWarnings("serial") public abstract class CompoundPNEdit extends CompoundEdit implements PNEdit {
	// Attribute
	protected ProbNet probNet;
	private boolean generatedEdits;
	// All simple edits are significant
	private boolean significant = true;

	// Constructor

	/**
	 * @param probNet <tt>ProbNet</tt>
	 */
	public CompoundPNEdit(ProbNet probNet) {
		this.probNet = probNet;
		generatedEdits = false;
	}

	// Methods

	/**
	 * Generate edits and does them
	 *
	 * @throws DoEditException DoEditException
	 * @throws WrongCriterionException WrongCriterionException
	 * @throws NonProjectablePotentialException NonProjectablePotentialException
	 */
	public void doEdit() throws DoEditException, NonProjectablePotentialException, WrongCriterionException {
		if (!generatedEdits) {
			generateEdits();
			generatedEdits = true;
		}
		for (UndoableEdit edit : edits) {
			((PNEdit) edit).doEdit();
		}
		super.end();
	}

	public abstract void generateEdits() throws NonProjectablePotentialException, WrongCriterionException;

	/**
	 * @return {@code Vector} of {@code UndoableEdit}s
	 * @throws WrongCriterionException WrongCriterionException
	 * @throws NonProjectablePotentialException NonProjectablePotentialException
	 */
	public Vector<UndoableEdit> getEdits() throws NonProjectablePotentialException, WrongCriterionException {
		if (!generatedEdits) {
			generateEdits();
			generatedEdits = true;
		}
		return edits;
	}

	public boolean isSignificant() {
		return significant;
	}

	public void setSignificant(boolean significant) {
		this.significant = significant;
	}

	@Override public ProbNet getProbNet() {
		return probNet;
	}

}
