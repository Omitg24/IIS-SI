/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.model.network.constraint;

import org.openmarkov.core.action.PNEdit;
import org.openmarkov.core.exception.NonProjectablePotentialException;
import org.openmarkov.core.exception.WrongCriterionException;
import org.openmarkov.core.model.network.ProbNet;

public interface Checkable {

	/**
	 * @param probNet {@code ProbNet}
	 * @return {@code true} if the {@code probNet} fulfills the
	 * condition.
	 */
	boolean checkProbNet(ProbNet probNet);

	/**
	 * Make sure all editions of the event fulfill the condition.
	 *
	 * @param probNet {@code ProbNet}
	 * @param edit    {@code PNEdit}
	 * @return {@code true} if the {@code ProbNet} will fulfill certain
	 * condition after applying the {@code edit} in a
	 * {@code ProbNet} that previously fulfilled the constraint.
	 * @throws WrongCriterionException WrongCriterionException
	 * @throws NonProjectablePotentialException NonProjectablePotentialException
	 */
	boolean checkEdit(ProbNet probNet, PNEdit edit) throws NonProjectablePotentialException, WrongCriterionException;

}
