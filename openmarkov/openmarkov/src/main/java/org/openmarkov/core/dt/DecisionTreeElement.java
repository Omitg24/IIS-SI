/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.dt;

import java.util.List;

import org.openmarkov.core.model.network.EvidenceCase;

public interface DecisionTreeElement {
	List<DecisionTreeElement> getChildren();

	//double getUtility();

	EvidenceCase getBranchStates();

	double getScenarioProbability();

	void setParent(DecisionTreeElement parent);
		
	
}
