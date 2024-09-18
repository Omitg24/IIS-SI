/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.model.network.potential.canonical;

public enum ICIFamily {
	OR(0), AND(1), TUNING(2);

	private final int value;

	ICIFamily(int value) {
		this.value = value;
	}

	public int value() {
		return value;
	}

	public String toString() {
		String string = null;
		if (value == 0) {
			string = new String("OR");
		} else if (value == 1) {
			string = new String("AND");
		} else {
			string = new String("TUNING");
		}
		return string;
	}

}
