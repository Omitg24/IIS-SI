/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.model.network.constraint;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.swing.undo.UndoableEdit;

import org.openmarkov.core.action.CompoundPNEdit;
import org.openmarkov.core.action.PNEdit;
import org.openmarkov.core.exception.NonProjectablePotentialException;
import org.openmarkov.core.exception.WrongCriterionException;

/**
 * Utility methods for constraint package.
 */
public class UtilConstraints {

	/**
	 * @param edit     {@code UndoableEditEvent}
	 * @param typeEdit {@code Class}
	 * @return An {@code ArrayList} of {@code PNEdit}s of type
	 * {@code typeEdit} that are contained in the
	 * {@code event} received (if there is any)
	 * @throws WrongCriterionException WrongCriterionException
	 * @throws NonProjectablePotentialException NonProjectablePotentialException
	 */
	public static List<PNEdit> getSimpleEditsByType(PNEdit edit, Class<?> typeEdit)
			throws NonProjectablePotentialException, WrongCriterionException {
		List<PNEdit> edits = new ArrayList<>();
		if (edit.getClass() == typeEdit) {
			edits.add((PNEdit) edit);
		} else { // Check compound edits
			if (CompoundPNEdit.class.isInstance(edit)) {
				Vector<UndoableEdit> simpleEdits = ((CompoundPNEdit) edit).getEdits();
				for (UndoableEdit simpleEdit : simpleEdits) {
					if (typeEdit.isInstance(simpleEdit)) {
						edits.add((PNEdit) simpleEdit);
					}
				}
			}
		}
		return edits;
	}

}
