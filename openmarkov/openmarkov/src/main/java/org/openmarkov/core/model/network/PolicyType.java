/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.model.network;

public enum PolicyType {
	OPTIMAL("Optimal"), DETERMINISTIC("Deterministic"), PROBABILISTIC("Probabilistic");

	String name;
	int type;

	PolicyType(String name) {
		this.type = this.ordinal();
		this.name = name;
	}

	public String toString() {
		return name;
	}

	public int getType() {
		return type;
	}

}
