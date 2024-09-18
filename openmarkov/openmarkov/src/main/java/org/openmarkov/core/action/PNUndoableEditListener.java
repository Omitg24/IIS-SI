/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.action;

import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;

import org.openmarkov.core.exception.ConstraintViolationException;
import org.openmarkov.core.exception.NonProjectablePotentialException;
import org.openmarkov.core.exception.WrongCriterionException;

public interface PNUndoableEditListener extends UndoableEditListener {

	/**
	 * An undoable edit will happen
	 * @param event Event
	 * @throws WrongCriterionException WrongCriterionException
	 * @throws ConstraintViolationException ConstraintViolationException
	 * @throws NonProjectablePotentialException NonProjectablePotentialException
	 */
	void undoableEditWillHappen(UndoableEditEvent event)
			throws ConstraintViolationException, NonProjectablePotentialException,
			WrongCriterionException;

	void undoEditHappened(UndoableEditEvent event);

}
