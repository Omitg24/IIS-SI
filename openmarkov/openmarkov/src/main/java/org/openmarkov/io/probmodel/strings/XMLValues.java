/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.io.probmodel.strings;

import java.io.Serializable;

public enum XMLValues implements Serializable {
	FALSE(0, "false"),
	TRUE(1, "true"),
	TABLE(2, "table"),  // potential role
	DECISION(3, "decision"),  // potential role
	UTILITY(4, "utility"),  // potential role
	LEFT(5, "left"), // for intervals (belongsTo = left)
	RIGHT(6, "right"), // for intervals (belongsTo = right)
	POLICY(7, "Policy");
	
	private int type;
	
	private String name;
	
	XMLValues(int type, String name) {
		this.type = type;
		this.name = name;
	}
	
	public String toString() {
		return name;
	}
	
	public int getType() {
		return type;
	}
	
}