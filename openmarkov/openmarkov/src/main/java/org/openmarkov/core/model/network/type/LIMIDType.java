/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.model.network.type;

import org.openmarkov.core.model.network.type.plugin.ProbNetType;

@ProbNetType(name = "LIMID") public class LIMIDType extends NetworkType {
	private static LIMIDType instance = null;

	// Constructor
	private LIMIDType() {
		super();
	}

	// Methods
	public static LIMIDType getUniqueInstance() {
		if (instance == null) {
			instance = new LIMIDType();
		}
		return instance;
	}

	/**
	 * @return String "LIMID"
	 */
	public String toString() {
		return "LIMID";
	}

}
