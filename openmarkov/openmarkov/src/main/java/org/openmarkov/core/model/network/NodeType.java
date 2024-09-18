/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.model.network;

import java.io.Serializable;

/**
 * Codifies existing node types using this numbers:
 * <ol start="0">
 * <li>CHANCE
 * <li>DECISION
 * <li>UTILITY
 * <li>SV_SUM
 * <li>SV_PRODUCT
 * <li>COST
 * <li>EFFECTIVENESS
 * <li>CE (Cost-Effectiveness)
 * </ol>
 *
 * @author manuel
 * @author fjdiez
 */
public enum NodeType implements Serializable {
	CHANCE(0, "chance"), DECISION(1, "decision"), UTILITY(2, "utility"), SV_SUM(3, "svSum"), SV_PRODUCT(4, "svProduct");

	/**
	 * Existing types: CHANCE(0), DECISION(1), UTILITY(2), ...
	 */
	private int type;

	private String name;

	/**
	 * @param type {@code type} An integer: CHANCE(0), DECISION(1), ...
	 * Condition: value &gt;= than 0 and value &lt; NodeType.values().length
	 */
	NodeType(int type, String name) {
		this.type = type;
		this.name = name;
	}

	/**
	 * @param nodeType Node type
	 * @return nodeType.value. {@code int}
	 */
	public static int type(NodeType nodeType) {
		return nodeType.type();
	}

	/**
	 * @return {@code type}
	 */
	public int type() {
		return type;
	}

	public String toString() {
		return name;
	}

}
