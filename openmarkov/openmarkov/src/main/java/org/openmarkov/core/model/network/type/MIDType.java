/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.model.network.type;

import org.openmarkov.core.model.network.constraint.ConstraintBehavior;
import org.openmarkov.core.model.network.constraint.OnlyAtemporalVariables;
import org.openmarkov.core.model.network.constraint.OnlyTemporalVariables;
import org.openmarkov.core.model.network.type.plugin.ProbNetType;

@ProbNetType(name = "MID", alternativeNames = { "MPAD"}) public class MIDType
		extends NetworkType {
	// Attributes
	private static MIDType instance = null;

	// Constructor
	private MIDType() {
		super();
		overrideConstraintBehavior(OnlyAtemporalVariables.class, ConstraintBehavior.NO);
		overrideConstraintBehavior(OnlyTemporalVariables.class, ConstraintBehavior.NO);
	}

	// Methods
	public static MIDType getUniqueInstance() {
		if (instance == null) {
			instance = new MIDType();
		}
		return instance;
	}

	/**
	 * @return String "MARKOV_INFLUENCE_DIAGRAM"
	 */
	public String toString() {
		return "MARKOV_INFLUENCE_DIAGRAM";
	}

}
