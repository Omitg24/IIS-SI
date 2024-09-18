/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.model.network.constraint;

import javax.swing.event.UndoableEditEvent;

import org.openmarkov.core.action.PNEdit;
import org.openmarkov.core.action.PNUndoableEditListener;
import org.openmarkov.core.exception.ConstraintViolationException;
import org.openmarkov.core.exception.NonProjectablePotentialException;
import org.openmarkov.core.exception.WrongCriterionException;
import org.openmarkov.core.model.network.ProbNet;

/**
 * A constraint is a condition that a model must fulfill.<p>
 * This class implements {@code PNUndoableEditListener} because like
 * that all the classes that implement this interface will be able to receive
 * the same messages than {@code UndoableEditListener} and they will be
 * able to be referenced with same identifier.
 */
public abstract class PNConstraint implements PNUndoableEditListener, Checkable {

	@Override public void undoableEditHappened(UndoableEditEvent e) {
		// Do nothing
	}

	/**
	 * Given a {@code probNet} that complies with this constraint, this
	 * method checks that after the application of the {@code edit}
	 * contained in the {@code event} received, the
	 * {@code probNet} continues complying with this constraint.
	 *
	 * @param event {@code UndoableEditEvent}
	 * @throws ConstraintViolationException ConstraintViolationException
	 * @throws WrongCriterionException WrongCriterionException
	 * @throws NonProjectablePotentialException NonProjectablePotentialException
	 */
	@Override public void undoableEditWillHappen(UndoableEditEvent event)
			throws ConstraintViolationException, NonProjectablePotentialException,
			WrongCriterionException {
		PNEdit edit = (PNEdit) event.getEdit();
		if (!checkEdit(edit.getProbNet(), edit)) {
			throw new ConstraintViolationException(getMessage());
		}

	}

	protected abstract String getMessage();

	@Override public void undoEditHappened(UndoableEditEvent event) {
		// Do nothing
	}

	/**
	 * @param probNet {@code ProbNet}
	 * @return {@code true} if the {@code probNet} fulfills the
	 * constraint.
	 */
	public abstract boolean checkProbNet(ProbNet probNet);

	/**
	 * Make sure all editions of the event do not violate restrictions.
	 *
	 * @param probNet {@code ProbNet}
	 * @param edit    {@code PNEdit}
	 * @return {@code true} if the {@code ProbNet} will fulfill the
	 * constraint after applying the {@code event} in a
	 * {@code ProbNet} that previously fulfilled the constraint.
	 * @throws WrongCriterionException WrongCriterionException
	 * @throws NonProjectablePotentialException NonProjectablePotentialException
	 */
	public abstract boolean checkEdit(ProbNet probNet, PNEdit edit)
			throws NonProjectablePotentialException, WrongCriterionException;

	@Override public String toString() {
		return this.getClass().getName();
	}

	@Override public boolean equals(Object paramObject) {
		return (paramObject.getClass() == this.getClass());
	}

	@Override public int hashCode() {
		int hashCode = 17 + this.getClass().hashCode();
		return hashCode;
	}

}
