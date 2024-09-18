/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.model.network.constraint;

import java.util.List;

import org.openmarkov.core.action.PNEdit;
import org.openmarkov.core.model.network.Node;
import org.openmarkov.core.model.network.NodeType;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.constraint.annotation.Constraint;
import org.openmarkov.core.model.network.potential.Potential;

@Constraint(name = "AllChanceVariablesHaveChancePotentials", defaultBehavior = ConstraintBehavior.OPTIONAL) public class AllChanceVariablesHaveChancePotentials
		extends PNConstraint {

	@Override public boolean checkEdit(ProbNet probNet, PNEdit edit) {
		return true;
	}

	@Override public boolean checkProbNet(ProbNet probNet) {
		List<Node> chanceNodes = probNet.getNodes(NodeType.CHANCE);
		for (Node chanceNode : chanceNodes) {
			Variable variable = chanceNode.getVariable();
			List<Potential> potentialsNode = chanceNode.getPotentials();
			boolean hasPotential = false;
			for (Potential potential : potentialsNode) {
				hasPotential = hasPotential || (potential.getVariables().get(0) == variable);
			}
			if (!hasPotential) {
				return false;
			}
		}
		return true;
	}

	@Override protected String getMessage() {
		return "chance variable without potential";
	}

}
