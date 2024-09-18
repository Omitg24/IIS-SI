/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.model.network.potential.canonical;

import java.io.Serializable;

public enum ICIModelType implements Serializable {
	OR(0), CAUSAL_MAX(1), GENERAL_MAX(2),

	AND(3), CAUSAL_MIN(4), GENERAL_MIN(5),

	TUNING(6);

	private final int value;

	ICIModelType(int value) {
		this.value = value;
	}

	public int value() {
		return value;
	}

	// TODO Find a way of returning the family that does not depend on
	// the number of types
	public ICIFamily getFamily() {
		if (value < 3) {
			return ICIFamily.OR;
		} else if (value < 6) {
			return ICIFamily.AND;
		} else {
			return ICIFamily.TUNING;
		}

	}

	public String toString() {
		String string = null;
		switch (value) {
		case 0:
			string = new String("OR");
			break;
		case 1:
			string = new String("CAUSAL_MAX");
			break;
		case 2:
			string = new String("GENERAL_MAX");
			break;
		case 3:
			string = new String("AND");
			break;
		case 4:
			string = new String("CAUSAL_MIN");
			break;
		case 5:
			string = new String("GENERAL_MIN");
			break;
		case 6:
			string = new String("TUNING");
			break;
		default:
			string = new String("Wrong ICIModel");
			break;
		}
		return string;
	}

}
