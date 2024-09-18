/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */
package org.openmarkov.core.model.network.potential;

/**
 * @author marias
 * @version 1.0
 */
public enum PotentialRole {

	CONDITIONAL_PROBABILITY(0, "conditionalProbability"), //	DECISION(1, "decision"),
	JOINT_PROBABILITY(2, "joinProbability"), POLICY(3, "policy"), //	UTILITY(4, "utility"),
	LINK_RESTRICTION(5, "linkRestriction"), UNSPECIFIED(6, "unspecified"), // TODO Remove
	//	INTERVENTION(7,"intervention"),
	//UTIL_2(8,"utility_on_inference" )
	;

	private int type;

	private String label;

	PotentialRole(int type, String label) {
		this.type = type;
		this.label = label;
	}

	public static PotentialRole getEnumMember(String auxLabel) {
		for (PotentialRole role : values()) {
			String u = role.toString();
			if (u.equals(auxLabel)) {
				return role;
			}
		}
		return null;

	}

	public String toString() {
		return label;
	}

	public int getType() {
		return type;
	}

}
