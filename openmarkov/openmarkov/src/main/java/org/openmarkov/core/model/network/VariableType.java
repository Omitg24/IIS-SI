/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.model.network;

import java.io.Serializable;

/**
 * @author manuel
 */
public enum VariableType implements Serializable {
	FINITE_STATES(0, "finiteStates"), NUMERIC(1, "numeric"), DISCRETIZED(2, "discretized");

	private int value;

	private String name;

	VariableType(int value, String name) {
		this.value = value;
		this.name = name;
	}

	public int value() {
		return value;
	}

	public String toString() {
		return name;
	}

}
