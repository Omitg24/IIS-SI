/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.action;

import org.openmarkov.core.exception.DoEditException;

@SuppressWarnings("serial") public class CloseParenthesisEdit extends SimplePNEdit {

	// Constant
	public static final String description = ")";

	// The open parenthesis of the close parenthesis
	private OpenParenthesisEdit openParenthesisEdit;

	// Constructor
	public CloseParenthesisEdit(OpenParenthesisEdit openParenthesisEdit) {
		super(null);
		this.openParenthesisEdit = openParenthesisEdit;
	}

	// Methods
	@Override public void doEdit() throws DoEditException {
		//super.addEdit(this);
	}

	public void undo() {
		super.undo();
	}

	public String getUndoPresentationName() {
		return description + " " + getPresentationName();
	}

	public String getRedoPresentationName() {
		return description + " " + getPresentationName();
	}

	public String toString() {
		return description;
	}

	public OpenParenthesisEdit getOpenParenthesisEdit() {
		return openParenthesisEdit;
	}

}
