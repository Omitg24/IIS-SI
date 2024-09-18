/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.action;

import javax.swing.undo.AbstractUndoableEdit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openmarkov.core.exception.DoEditException;
import org.openmarkov.core.model.network.ProbNet;

/**
 * Abstract class that defines the basic attribute (a {@code ProbNet})
 * and operations of editions.
 */
@SuppressWarnings("serial") public abstract class SimplePNEdit extends AbstractUndoableEdit implements PNEdit {

	// Attributes
	/**
	 * {@code ProbNet} over witch the operations are defined.
	 */
	protected ProbNet probNet;

	private boolean typicalRedo = true;

	//All simple edits are significant  
	private boolean significant = true;

	private Logger logger;

	// Constructor

	/**
	 * @param probNet {@code ProbNet}
	 */
	public SimplePNEdit(ProbNet probNet) {
		this.probNet = probNet;
		this.logger = LogManager.getLogger(SimplePNEdit.class);
	}

	// Methods

	/**
	 * Abstract method to be defined in derived classes
	 *
	 * @throws DoEditException DoEditException
	 */
	public abstract void doEdit() throws DoEditException;

	/**
	 * @return probNet. {@code ProbNet}
	 */
	@Override public ProbNet getProbNet() {
		return probNet;
	}

	protected void setTypicalRedo(boolean redo) {
		typicalRedo = redo;
	}

	public void redo() {
		super.redo();
		if (typicalRedo) {
			try {
				doEdit();

			} catch (Exception e) {
				logger.fatal(e);
			}
		} else {
			typicalRedo = true;
		}

	}

	public boolean isSignificant() {
		return significant;
	}

	public void setSignificant(boolean significant) {
		this.significant = significant;
	}

}
