/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.action;

import javax.swing.undo.UndoableEdit;

import org.openmarkov.core.exception.DoEditException;
import org.openmarkov.core.exception.NonProjectablePotentialException;
import org.openmarkov.core.exception.WrongCriterionException;
import org.openmarkov.core.model.network.ProbNet;

/**
 * An edition is one action defined over a Probabilistic Network.
 */
public interface PNEdit extends UndoableEdit {

	/**
	 * Puts into effect the edition.
	 *
	 * @throws WrongCriterionException WrongCriterionException
	 * @throws NonProjectablePotentialException NonProjectablePotentialException
	 * @throws DoEditException DoEditException
	 */
	void doEdit() throws DoEditException, NonProjectablePotentialException, WrongCriterionException;

	void setSignificant(boolean significant);

	ProbNet getProbNet();

}
