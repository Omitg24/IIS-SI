/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.io.probmodel.strings;

/** Constraints applicable to a ProbNet */
public enum XMLBasicConstraints {
	DISTINCT_VARIABLE_NAMES(0, "DistinctVariableNames"),
	MAX_NUM_PARENTS(1, "MaxNumParents"),
	NO_BACKWARD_LINKS(2, "NoBackwardLinks"),
	NO_CLOSED_PATH(3, "NoClosedPath"),
	NO_CYCLES(4, "NoCycles"),
	NO_EMPTY_NAME(5, "NoEmptyName"),
	NO_MIXED_PARENTS(6, "NoMixedParents"),
	NO_SELF_LOOPS(7, "NoSelfLoops"),
	NO_SUPER_VALUE_NODES(8, "NoSuperValueNodes"),
	NO_UTILITY_PARENTS(9, "NoUtilityParents"),
	NOT_EQUAL_LINKS(10, "NotEqualLinks"),
	NOT_MULTIPLE_LINKS(11, "NotMultipleLinks"),
	ONLY_CHANCE_NODES(12, "OnlyChanceNodes"),
	ONLY_DIRECTED_LINKS(13, "OnlyDirectedLinks"),
	ONLY_FINITE_STATE_VARIABLES(14, "OnlyFiniteStateVariables"),
	ONLY_NUMERIC_VARIABLES(15, "OnlyNumericVariables"),
	ONLY_ONE_UTILITY_NODE(16, "OnlyOneUtilityNode"),
	ONLY_TEMPORAL_NODES(17, "OnlyTemporalNodes"),
	ONLY_UNDIRECTED_LINKS(18, "OnlyUndirectedLinks"),
	UNLABELED_LINKS(19, "UnlabeledLinks");
	
	private int type;
	
	private String name;
	
	XMLBasicConstraints(int type, String name) {
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
