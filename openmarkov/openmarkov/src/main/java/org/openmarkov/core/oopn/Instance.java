/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.oopn;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.openmarkov.core.model.network.Node;
import org.openmarkov.core.model.network.ProbNet;

public class Instance {

	private String name;
	private ProbNet classNet;
	private boolean isInput;
	private ParameterArity arity;
	private List<Node> instanceNodes;
	private HashMap<String, Instance> subInstances;
	/**
	 * Constructor
	 *
	 * @param name Name
	 * @param classNet Network
	 * @param instanceNodes List of instance nodes
	 * @param isInput Is input?
	 */
	public Instance(String name, ProbNet classNet, List<Node> instanceNodes, boolean isInput) {
		super();
		this.name = name;
		this.classNet = classNet;
		this.instanceNodes = instanceNodes;
		this.subInstances = new HashMap<>();
		this.isInput = isInput;
		this.arity = ParameterArity.ONE;

		if (classNet instanceof OOPNet) {
			for (String subInstanceName : ((OOPNet) classNet).getInstances().keySet()) {
				Instance originalSubinstance = ((OOPNet) classNet).getInstances().get(subInstanceName);

				ArrayList<Node> subInstanceNodes = new ArrayList<>();
				for (Node originalSubinstanceNode : originalSubinstance.getNodes()) {
					String subinstanceNodeName = name + "." + originalSubinstanceNode.getName();
					int i = 0;
					boolean found = false;
					while (!found && i < instanceNodes.size()) {
						found = instanceNodes.get(i).getName().equals(subinstanceNodeName);
						if (!found) {
							++i;
						}
					}
					subInstanceNodes.add(instanceNodes.get(i));
				}
				this.subInstances.put(name + "." + subInstanceName,
						new Instance(name + "." + subInstanceName, originalSubinstance.getClassNet(), subInstanceNodes,
								originalSubinstance.isInput));
			}
		}
	}

	/**
	 * Constructor
	 *
	 * @param name Name
	 * @param classNet Network
	 * @param instanceNodes List of instance nodes
	 */
	public Instance(String name, ProbNet classNet, List<Node> instanceNodes) {
		this(name, classNet, instanceNodes, false);
	}

	/**
	 * @return the isInput
	 */
	public boolean isInput() {
		return isInput;
	}

	/**
	 * @param isInput the isInput to set
	 */
	public void setInput(boolean isInput) {
		this.isInput = isInput;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * sets the instance name
	 * @param name New name
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the classNet
	 */
	public ProbNet getClassNet() {
		return classNet;
	}

	/**
	 * @return the instanceNodes
	 */
	public List<Node> getNodes() {
		return instanceNodes;
	}

	/**
	 * @return sub instances
	 */
	public HashMap<String, Instance> getSubInstances() {
		return subInstances;
	}

	public ParameterArity getArity() {
		return arity;
	}

	public void setArity(ParameterArity arity) {
		this.arity = arity;
	}

	public enum ParameterArity {
		ONE, MANY;

		public static ParameterArity parseArity(String name) {
			ParameterArity arity = null;
			if (name.equals(ONE.toString())) {
				arity = ParameterArity.ONE;
			} else if (name.equals(MANY.toString())) {
				arity = ParameterArity.MANY;
			}
			return arity;
		}
	}

}
