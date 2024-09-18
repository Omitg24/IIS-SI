/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.io.probmodel.strings;

/** A compound constraint defines a type of ProbNet and it contains a bundle of basic constraints. */
public enum XMLCompoundConstraints {
	BAYESIAN_NETWORK(0, "BayesianNetwork"),
	DINAMIC_BAYESIAN_NETWORK(1, "DinamicBayesianNetwork"),
	INFLUENCE_DIAGRAM(2, "InfluenceDiagram"),
	MDP(3, "MDP"),
	POMDP(4, "POMDP");
	
	private int type;
	
	private String name;
	
	XMLCompoundConstraints(int type, String name) {
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
