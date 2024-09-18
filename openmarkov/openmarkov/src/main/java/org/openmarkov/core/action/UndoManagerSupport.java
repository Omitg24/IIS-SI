/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.action;

import java.util.Enumeration;
import java.util.Vector;

import javax.swing.UIManager;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.CompoundEdit;
import javax.swing.undo.UndoableEdit;

/**
 * This class is based on undoManager copy from Sun by Ray Ryan.
 *
 * @author Miguel Palacios
 */

public class UndoManagerSupport extends CompoundEdit implements UndoableEditListener {

	private static final long serialVersionUID = 4034432307306728799L;

	//Start code of UndoManager from Sun
	int indexOfNextAdd;
	int limit;

	/**
	 * Creates a new {@code UndoManager}.
	 */
	public UndoManagerSupport() {
		super();
		indexOfNextAdd = 0;
		limit = 100;
		edits.ensureCapacity(limit);
	}

	/**
	 * Returns the maximum number of edits this {@code UndoManager}
	 * holds. A value less than 0 indicates the number of edits is not
	 * limited.
	 *
	 * @return the maximum number of edits this {@code UndoManager} holds
	 * @see #addEdit
	 * @see #setLimit
	 */
	public synchronized int getLimit() {
		return limit;
	}

	/**
	 * Sets the maximum number of edits this {@code UndoManager}
	 * holds. A value less than 0 indicates the number of edits is not
	 * limited. If edits need to be discarded to shrink the limit,
	 * {@code die} will be invoked on them in the reverse
	 * order they were added.  The default is 100.
	 *
	 * @param l the new limit
	 * @throws RuntimeException if this {@code UndoManager} is not in progress
	 *                          ({@code end} has been invoked)
	 * @see #isInProgress
	 * @see #end
	 * @see #addEdit
	 * @see #getLimit
	 */
	public synchronized void setLimit(int l) {
		if (!isInProgress())
			throw new RuntimeException(
					"Attempt to call UndoManager.setLimit() after UndoManager.end() has been called");
		limit = l;
		trimForLimit();
	}

	/**
	 * Empties the undo manager sending each edit a {@code die} message
	 * in the process.
	 *
	 * @see AbstractUndoableEdit#die
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" }) public synchronized void discardAllEdits() {
		Enumeration cursor = edits.elements();
		while (cursor.hasMoreElements()) {
			UndoableEdit e = (UndoableEdit) cursor.nextElement();
			e.die();
		}
		edits = new Vector();
		indexOfNextAdd = 0;
		// TODO: PENDING(rjrjr) when vector grows a removeRange() method
		// (expected in JDK 1.2), trimEdits() will be nice and
		// efficient, and this method can call that instead.
	}

	/**
	 * Reduces the number of queued edits to a range of size limit,
	 * centered on the index of the next edit.
	 */
	protected void trimForLimit() {
		if (limit >= 0) {
			int size = edits.size();
			//          System.out.print("limit: " + limit +
			//                           " size: " + size +
			//                           " indexOfNextAdd: " + indexOfNextAdd +
			//                           "\n");

			if (size > limit) {
				int halfLimit = limit / 2;
				int keepFrom = indexOfNextAdd - 1 - halfLimit;
				int keepTo = indexOfNextAdd - 1 + halfLimit;

				// These are ints we're playing with, so dividing by two
				// rounds down for odd numbers, so make sure the limit was
				// honored properly. Note that the keep range is
				// inclusive.

				if (keepTo - keepFrom + 1 > limit) {
					keepFrom++;
				}

				// The keep range is centered on indexOfNextAdd,
				// but odds are good that the actual edits Vector
				// isn't. Move the keep range to keep it legal.

				if (keepFrom < 0) {
					keepTo -= keepFrom;
					keepFrom = 0;
				}
				if (keepTo >= size) {
					int delta = size - keepTo - 1;
					keepTo += delta;
					keepFrom += delta;
				}

				//              System.out.println("Keeping " + keepFrom + " " + keepTo);
				trimEdits(keepTo + 1, size - 1);
				trimEdits(0, keepFrom - 1);
			}
		}
	}

	/**
	 * Removes edits in the specified range.
	 * All edits in the given range (inclusive, and in reverse order)
	 * will have {@code die} invoked on them and are removed from
	 * the list of edits. This has no effect if
	 * {@code from} &gt; {@code to}.
	 *
	 * @param from the minimum index to remove
	 * @param to   the maximum index to remove
	 */
	protected void trimEdits(int from, int to) {
		if (from <= to) {
			//          System.out.println("Trimming " + from + " " + to + " with index " +
			//                           indexOfNextAdd);
			for (int i = to; from <= i; i--) {
				UndoableEdit e = (UndoableEdit) edits.elementAt(i);
				//              System.out.println("JUM: Discarding " +
				//                                 e.getUndoPresentationName());
				e.die();
				// PENDING(rjrjr) when Vector supports range deletion (JDK
				// 1.2) , we can optimize the next line considerably.
				edits.removeElementAt(i);
			}

			if (indexOfNextAdd > to) {
				//              System.out.print("...right...");
				indexOfNextAdd -= to - from + 1;
			} else if (indexOfNextAdd >= from) {
				//              System.out.println("...mid...");
				indexOfNextAdd = from;
			}

			//          System.out.println("new index " + indexOfNextAdd);
		}
	}

	/**
	 * Returns the the next significant edit to be undone if {@code undo}
	 * is invoked. This returns {@code null} if there are no edits
	 * to be undone.
	 *
	 * @return the next significant edit to be undone
	 */
	protected UndoableEdit editToBeUndone() {
		int i = indexOfNextAdd;
		while (i > 0) {
			UndoableEdit edit = (UndoableEdit) edits.elementAt(--i);
			if (edit.isSignificant()) {
				return edit;
			}
		}

		return null;
	}

	/**
	 * Returns the the next significant edit to be redone if {@code redo}
	 * is invoked. This returns {@code null} if there are no edits
	 * to be redone.
	 *
	 * @return the next significant edit to be redone
	 */
	protected UndoableEdit editToBeRedone() {
		int count = edits.size();
		//by default the first next edit will be "redo", if is significant or
		// not
		int i = indexOfNextAdd + 1;
		if (indexOfNextAdd == count - 1) {//when the edit is the last
			return lastEdit();
		} else {
			while (i < count) {
				UndoableEdit edit = (UndoableEdit) edits.elementAt(i++);
				if (edit.isSignificant()) {
					return edit;
				}
			}
			if (i == count) {//when do not found a significant edit but can do redo
				return lastEdit();
			}
		}

		return null;
	}

	/**
	 * Undoes all changes from the index of the next edit to
	 * {@code edit}, updating the index of the next edit appropriately.
	 * @param edit Edit
	 * @throws CannotUndoException if one of the edits throws
	 *                             {@code CannotUndoException}
	 */
	protected void undoTo(UndoableEdit edit) throws CannotUndoException {
		boolean done = false;
		while (!done) {
			UndoableEdit next = (UndoableEdit) edits.elementAt(--indexOfNextAdd);
			next.undo();
			done = next == edit;
		}
	}

	/**
	 * Redoes all changes from the index of the next edit to
	 * {@code edit}, updating the index of the next edit appropriately.
	 * @param edit Edit
	 * @throws CannotRedoException if one of the edits throws
	 *                             {@code CannotRedoException}
	 */
	protected void redoTo(UndoableEdit edit) throws CannotRedoException {
		boolean done = false;
		while (!done) {
			UndoableEdit current = (UndoableEdit) edits.elementAt(indexOfNextAdd++);
			current.redo();
			if (indexOfNextAdd < edits.size()) {
				UndoableEdit next = (UndoableEdit) edits.elementAt(indexOfNextAdd);
				done = next == edit && edit.isSignificant();//asegura detenerse si encuentra un significant
			} else
				done = true;

		}

	}

	/**
	 * Convenience method that invokes one of {@code undo} or
	 * {@code redo}. If any edits have been undone (the index of
	 * the next edit is less than the length of the edits list) this
	 * invokes {@code redo}, otherwise it invokes {@code undo}.
	 *
	 * @throws CannotUndoException if one of the edits throws
	 *                             {@code CannotUndoException}
	 * @throws CannotRedoException if one of the edits throws
	 *                             {@code CannotRedoException}
	 * @see #canUndoOrRedo
	 * @see #getUndoOrRedoPresentationName
	 */
	public synchronized void undoOrRedo() throws CannotRedoException, CannotUndoException {
		if (indexOfNextAdd == edits.size()) {
			undo();
		} else {
			redo();
		}
	}

	/**
	 * Returns true if it is possible to invoke {@code undo} or
	 * {@code redo}.
	 *
	 * @return true if invoking {@code canUndoOrRedo} is valid
	 * @see #undoOrRedo
	 */
	public synchronized boolean canUndoOrRedo() {
		if (indexOfNextAdd == edits.size()) {
			return canUndo();
		} else {
			return canRedo();
		}
	}

	/**
	 * Undoes the appropriate edits.  If {@code end} has been
	 * invoked this calls through to the superclass, otherwise
	 * this invokes {@code undo} on all edits between the
	 * index of the next edit and the last significant edit, updating
	 * the index of the next edit appropriately.
	 *
	 * @throws CannotUndoException if one of the edits throws
	 *                             {@code CannotUndoException} or there are no edits
	 *                             to be undone
	 * @see CompoundEdit#end
	 * @see #canUndo
	 * @see #editToBeUndone
	 */
	public synchronized void undo() throws CannotUndoException {
		if (isInProgress()) {
			UndoableEdit edit = editToBeUndone();
			if (edit == null) {
				throw new CannotUndoException();
			}
			undoTo(edit);
		} else {
			super.undo();
		}
	}

	/**
	 * Returns true if edits may be undone.  If {@code end} has
	 * been invoked, this returns the value from super.  Otherwise
	 * this returns true if there are any edits to be undone
	 * ({@code editToBeUndone} returns non-{@code null}).
	 *
	 * @return true if there are edits to be undone
	 * @see CompoundEdit#canUndo
	 * @see #editToBeUndone
	 */
	public synchronized boolean canUndo() {
		if (isInProgress()) {
			UndoableEdit edit = editToBeUndone();
			return edit != null && edit.canUndo();
		} else {
			return super.canUndo();
		}
	}

	/**
	 * Redoes the appropriate edits.  If {@code end} has been
	 * invoked this calls through to the superclass.  Otherwise
	 * this invokes {@code redo} on all edits between the
	 * index of the next edit and the next significant edit, updating
	 * the index of the next edit appropriately.
	 *
	 * @throws CannotRedoException if one of the edits throws
	 *                             {@code CannotRedoException} or there are no edits
	 *                             to be redone
	 * @see CompoundEdit#end
	 * @see #canRedo
	 * @see #editToBeRedone
	 */
	public synchronized void redo() throws CannotRedoException {
		if (isInProgress()) {
			UndoableEdit edit = editToBeRedone();
			if (edit == null) {
				throw new CannotRedoException();
			}
			redoTo(edit);
		} else {
			super.redo();
		}
	}

	/**
	 * Returns true if edits may be redone.  If {@code end} has
	 * been invoked, this returns the value from super.  Otherwise,
	 * this returns true if there are any edits to be redone
	 * ({@code editToBeRedone} returns non-{@code null}).
	 *
	 * @return true if there are edits to be redone
	 * @see CompoundEdit#canRedo
	 * @see #editToBeRedone
	 */
	public synchronized boolean canRedo() {
		if (isInProgress()) {
			UndoableEdit edit = editToBeRedone();
			return edit != null && edit.canRedo();
		} else {
			return super.canRedo();
		}
	}

	/**
	 * Adds an {@code UndoableEdit} to this
	 * {@code UndoManager}, if it's possible.  This removes all
	 * edits from the index of the next edit to the end of the edits
	 * list.  If {@code end} has been invoked the edit is not added
	 * and {@code false} is returned.  If {@code end} hasn't
	 * been invoked this returns {@code true}.
	 *
	 * @param anEdit the edit to be added
	 * @return true if {@code anEdit} can be incorporated into this
	 * edit
	 * @see CompoundEdit#end
	 * @see CompoundEdit#addEdit
	 */
	public synchronized boolean addEdit(UndoableEdit anEdit) {
		boolean retVal;

		// Trim from the indexOfNextAdd to the end, as we'll
		// never reach these edits once the new one is added.
		trimEdits(indexOfNextAdd, edits.size() - 1);

		retVal = super.addEdit(anEdit);
		if (isInProgress()) {
			retVal = true;
		}

		// Maybe super added this edit, maybe it didn't (perhaps
		// an in progress compound edit took it instead. Or perhaps
		// this UndoManager is no longer in progress). So make sure
		// the indexOfNextAdd is pointed at the right place.
		indexOfNextAdd = edits.size();

		// Enforce the limit
		trimForLimit();

		return retVal;
	}

	/**
	 * Turns this {@code UndoManager} into a normal
	 * {@code CompoundEdit}.  This removes all edits that have
	 * been undone.
	 *
	 * @see CompoundEdit#end
	 */
	public synchronized void end() {
		super.end();
		this.trimEdits(indexOfNextAdd, edits.size() - 1);
	}

	/**
	 * Convenience method that returns either
	 * {@code getUndoPresentationName} or
	 * {@code getRedoPresentationName}.  If the index of the next
	 * edit equals the size of the edits list,
	 * {@code getUndoPresentationName} is returned, otherwise
	 * {@code getRedoPresentationName} is returned.
	 *
	 * @return undo or redo name
	 */
	public synchronized String getUndoOrRedoPresentationName() {
		if (indexOfNextAdd == edits.size()) {
			return getUndoPresentationName();
		} else {
			return getRedoPresentationName();
		}
	}

	/**
	 * Returns a description of the undoable form of this edit.
	 * If {@code end} has been invoked this calls into super.
	 * Otherwise if there are edits to be undone, this returns
	 * the value from the next significant edit that will be undone.
	 * If there are no edits to be undone and {@code end} has not
	 * been invoked this returns the value from the {@code UIManager}
	 * property "AbstractUndoableEdit.undoText".
	 *
	 * @return a description of the undoable form of this edit
	 * @see #undo
	 * @see CompoundEdit#getUndoPresentationName
	 */
	public synchronized String getUndoPresentationName() {
		if (isInProgress()) {
			if (canUndo()) {
				return editToBeUndone().getUndoPresentationName();
			} else {
				return UIManager.getString("AbstractUndoableEdit.undoText");
			}
		} else {
			return super.getUndoPresentationName();
		}
	}

	/**
	 * Returns a description of the redoable form of this edit.
	 * If {@code end} has been invoked this calls into super.
	 * Otherwise if there are edits to be redone, this returns
	 * the value from the next significant edit that will be redone.
	 * If there are no edits to be redone and {@code end} has not
	 * been invoked this returns the value from the {@code UIManager}
	 * property "AbstractUndoableEdit.redoText".
	 *
	 * @return a description of the redoable form of this edit
	 * @see #redo
	 * @see CompoundEdit#getRedoPresentationName
	 */
	public synchronized String getRedoPresentationName() {
		if (isInProgress()) {
			if (canRedo()) {
				return editToBeRedone().getRedoPresentationName();
			} else {
				return UIManager.getString("AbstractUndoableEdit.redoText");
			}
		} else {
			return super.getRedoPresentationName();
		}
	}

	/**
	 * An {@code UndoableEditListener} method. This invokes
	 * {@code addEdit} with {@code e.getEdit()}.
	 *
	 * @param e the {@code UndoableEditEvent} the
	 *          {@code UndoableEditEvent} will be added from
	 * @see #addEdit
	 */
	public void undoableEditHappened(UndoableEditEvent e) {
		addEdit(e.getEdit());
	}

	/**
	 * Returns a string that displays and identifies this
	 * object's properties.
	 *
	 * @return a String representation of this object
	 */
	public String toString() {
		return super.toString() + " limit: " + limit + " indexOfNextAdd: " + indexOfNextAdd;
	}

	//Finish code of UndoManager from Sun

	public synchronized void deleteEdits(int numOfEdits) {
		if (numOfEdits <= edits.size()) {
			this.trimEdits(edits.size() - numOfEdits, edits.size() - 1);
		}

	}

}
