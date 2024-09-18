/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.model.network.type;

import org.openmarkov.core.model.network.type.plugin.ProbNetType;

@ProbNetType(name = "InfluenceDiagram") public class InfluenceDiagramType extends NetworkType {
	private static InfluenceDiagramType instance = null;

	// Constructor
	private InfluenceDiagramType() {
		super();
	}

	// Methods
	public static InfluenceDiagramType getUniqueInstance() {
		if (instance == null) {
			instance = new InfluenceDiagramType();
		}
		return instance;
	}

	/**
	 * @return String "InfluenceDiagram"
	 */
	public String toString() {
		return "INFLUENCE_DIAGRAM";
	}

}
