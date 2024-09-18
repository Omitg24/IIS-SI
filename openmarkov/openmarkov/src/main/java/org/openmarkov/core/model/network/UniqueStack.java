/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.model.network;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;

/**
 * This class stack ensures that each element is stored only once.
 *
 * @author marias
 */
public class UniqueStack<T> {
	private final Stack<T> stack = new Stack<>();
	private final HashSet<T> set = new HashSet<>();

	/**
	 * @param t template parameter. {@code T}
	 * @return true (as specified by Collection.add) {@code boolean}
	 */
	public boolean push(T t) {
		// Only add element to stack if the set does not contain the specified element.
		if (set.add(t)) {
			stack.add(t);
		}
		return true;
	}

	/**
	 * @return t template parameter. {@code T}
	 */
	public T pop() {
		T ret = stack.pop();
		set.remove(ret);
		return ret;
	}

	/**
	 * @return {@code boolean}
	 */
	public boolean empty() {
		return set.isEmpty();
	}

	/**
	 * @return {@code List} of the template class T
	 */
	public List<T> list() {
		return new ArrayList<>(stack);
	}
}

