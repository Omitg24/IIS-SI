/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.model.network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.openmarkov.core.exception.InvalidStateException;
import org.openmarkov.core.model.network.potential.PotentialRole;
import org.openmarkov.core.model.network.potential.TablePotential;

// TODO  mantener la consistencia entre name y baseName cuando se cambian

/**
 * A variable (for instance, a random variable or a decision). Each
 * {@code Node} in a {@code ProbNet}work represents a
 * {@code Variable}
 *
 * @author marias
 * @author fjdiez
 * @version 1.0
 * @see org.openmarkov.core.model.network.Node
 * @see org.openmarkov.core.model.network.ProbNet
 */
public class Variable implements Cloneable, Comparable<Variable> {

	// Constant
	/**
	 * Time slice value when the variable is not temporal.
	 */
	public final static int noTemporalTimeSlice = Integer.MIN_VALUE;

	// Attributes
	private final String STATE_BASE_NAME = "state";
	/**
	 * A string (usually in English) that identifies this variable.
	 */
	protected String name;
	/**
	 * List of states that this variable can take on. Each state will be a
	 * {@code String}.
	 *
	 */
	protected State[] states;
	/**
	 * Variable role: discrete, continuous, discretized, ...
	 */
	protected VariableType variableType;
	/**
	 * Interval sets where this variable is defined in the case of continuous or
	 * discretized variable type.
	 */
	protected PartitionedInterval partitionedInterval;
	protected HashMap<String, String> additionalProperties;
	protected HashMap<String, HashMap<String, String>> statesAdditionalProperties;
	/**
	 * The time Slice of the node. The default value is no temporal.
	 */
	private int timeSlice = noTemporalTimeSlice;
	// Name without the time slice index. For example, if the name is "X [0]",
	// the baseName is "X"
	private String baseName;
	private StringWithProperties unit = new StringWithProperties("");
	/**
	 * Max error.
	 */
	private double precision = 0.01;
	/**
	 * Agent for decision nodes
	 */
	private StringWithProperties agent;
	/**
	 * Decision criterion for utility nodes
	 */
	private Criterion decisionCriterion;

	// Constructors

	/**
	 * Constructor for discrete variables.
	 *
	 * @param name   {@code String}
	 * @param states {@code String[]}
	 * Condition: All the states must be different
	 */
	public Variable(String name, State[] states) {

		this.name = name;
		this.states = states;
		this.variableType = VariableType.FINITE_STATES;
		this.partitionedInterval = null;
		setTimeSlice(getTimeSlice(name));
	}

	/**
	 * Constructor for discrete variables. It takes advantage of the feature of
	 * variable-length argument lists of Java 5 in order to accept the names of
	 * the states.
	 * <p>
	 * Creates a {@code FSVariable} whose states are given by the names
	 * {@code namesStates} states.
	 *
	 * @param nameVariable a {@code String}
	 * @param stateNames   a sequence of {@code String} by using the facilities of
	 *                     Java 5.
	 */
	public Variable(String nameVariable, String... stateNames) {

		int numStates = stateNames.length;
		this.name = nameVariable;
		states = new State[numStates];
		for (int i = 0; i < numStates; i++) {
			states[i] = new State(stateNames[i]);
		}
		this.variableType = VariableType.FINITE_STATES;
		this.partitionedInterval = null;
		setTimeSlice(getTimeSlice(name));

	}

	/**
	 * Constructor for discrete variables.
	 * <p>
	 * Creates a {@code FSVariable} with {@code numStates} states. The
	 * i-th state is named as "i".
	 *
	 * @param name      a {@code String}
	 * @param numStates {@code int}
	 */
	public Variable(String name, int numStates) {

		this.name = name;
		this.states = new State[numStates];
		for (int i = 0; i < numStates; i++) {
			states[i] = new State("" + i);
		}
		this.variableType = VariableType.FINITE_STATES;
		this.partitionedInterval = null;
		setTimeSlice(getTimeSlice(name));
	}

	/**
	 * Copy constructor for Variable.
	 *
	 * @param variable Variable
	 */
	public Variable(Variable variable) {

		this.name = new String(variable.getName());
		this.states = variable.states.clone();
		this.variableType = variable.getVariableType();
		this.partitionedInterval = (variable.getPartitionedInterval() != null) ?
				(PartitionedInterval) variable.getPartitionedInterval().clone() :
				null;
		this.precision = variable.getPrecision();
		this.unit = variable.unit.copy();
		setTimeSlice(getTimeSlice(variable.getName()));
	}

	/**
	 * Default constructor for continuous variables.
	 * <p>
	 * A continuous variable is defined in an interval. In this case the
	 * interval is (-infinity, +infinity)
	 *
	 * @param name {@code String}
	 */
	public Variable(String name) {

		this(name, new State[] { new State("0") },
				new PartitionedInterval(false, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, false), 0.0);
		this.variableType = VariableType.NUMERIC;
	}

	/**
	 * Constructor for continuous variables.
	 * <p>
	 * A continuous variable is defined in an interval.
	 *
	 * @param name        . {@code String}
	 * @param leftClosed  . {@code boolean}
	 * @param min         . {@code double}
	 * @param max         . {@code double}
	 * @param rightClosed . {@code boolean}
	 * @param precision   . {@code double}
	 */
	public Variable(String name, boolean leftClosed, double min, double max, boolean rightClosed, double precision) {

		this(name, new State[] { new State("Only one state") },
				new PartitionedInterval(leftClosed, min, max, rightClosed), precision);
		this.variableType = VariableType.NUMERIC;
	}

	/**
	 * Constructor for hybrid variables (discrete and continuous).
	 * <p>
	 * The interval in with is defined the continuous variable is the addition
	 * of the set of intervals.
	 *
	 * @param name                . {@code String}
	 * @param states              . {@code String[]}
	 * @param partitionedInterval . {@code PartitionedInterval}
	 * @param precision           . {@code double}
	 * Condition: states.length = partitionedInterval.getNumSubintervals()
	 */
	public Variable(String name, State[] states, PartitionedInterval partitionedInterval, double precision) {

		this(name, states);
		this.partitionedInterval = partitionedInterval;
		this.precision = precision;
		this.variableType = VariableType.DISCRETIZED;
	}

	// Methods
	public Object clone() {
		Object object = null;
		try {
			object = super.clone();
		} catch (CloneNotSupportedException e) {
			// Unreachable code
			System.err.println("Can not clone object " + object);
		}
		return object;
	}

	// Methods

	/**
	 * @param additionalProperties . {@code HashMap} with key = {@code String} and
	 *                             value = {@code String}
	 */
	public void setAdditionalProperties(HashMap<String, String> additionalProperties) {
		this.additionalProperties = additionalProperties;
	}

	/**
	 * @param propertyName Property name
	 * @return property value if exists, otherwise {@code null}
	 * {@code String} and value = {@code String}
	 */
	public String getAdditionalProperty(String propertyName) {
		String property = null;
		if (additionalProperties != null) {
			property = additionalProperties.get(propertyName);
		}
		return property;
	}

	/**
	 * @param propertyValue Property value
	 * @param propertyName Property name
	 */
	public void setAdditionalProperty(String propertyName, String propertyValue) {
		if (additionalProperties == null) {
			additionalProperties = new HashMap<>();
		}
		additionalProperties.put(propertyName, propertyValue);
	}

	public void setStateAdditionalProperties(String stateName, HashMap<String, String> stateAdditionalProperties) {
		if (statesAdditionalProperties == null) {
			statesAdditionalProperties = new HashMap<>();
		}
		statesAdditionalProperties.put(stateName, stateAdditionalProperties);
	}

	public HashMap<String, String> getStateAdditionalProperties(String stateName) {
		HashMap<String, String> stateAdditionalProperties = null;
		if (statesAdditionalProperties != null) {
			stateAdditionalProperties = statesAdditionalProperties.get(stateName);
		}
		return stateAdditionalProperties;
	}

	public boolean isTemporal() {

		return timeSlice != Integer.MIN_VALUE;
	}

	public void setStateAdditionalProperty(String stateName, String propertyName, String propertyValue) {
		if (statesAdditionalProperties == null) {
			statesAdditionalProperties = new HashMap<>();
		}
		HashMap<String, String> stateProperties = statesAdditionalProperties.get(stateName);
		if (stateProperties == null) {
			stateProperties = new HashMap<>();
			statesAdditionalProperties.put(stateName, stateProperties);
		}
		stateProperties.put(propertyName, propertyValue);
	}

	public String getStateAdditionalProperty(String stateName, String propertyName) {
		String propertyValue = null;
		if (statesAdditionalProperties != null) {
			HashMap<String, String> stateProperties = statesAdditionalProperties.get(stateName);
			if (stateProperties != null) {
				propertyValue = stateProperties.get(propertyName);
			}
		}
		return propertyValue;
	}

	/**
	 * Changes the name of one state.
	 *
	 * @param oldName . {@code String}.
	 * @param newName . {@code String}.
	 * @throws Exception Exception
	 */
	public void renameState(String oldName, String newName) throws Exception {

		for (int i = 0; i < states.length; i++) {
			if (states[i].getName().contentEquals(newName)) {
				throw new Exception("Change state name to a name that already exists");
			}
		}
		int index = getStateIndex(oldName);
		if (index == -1) {
			throw new Exception(
					"Try to change the state name " + oldName + " that does not exists in variable " + name);
		}
		states[getStateIndex(oldName)].setName(newName);

		// Change key of additional additionalProperties of this state if they
		// exists
		if (statesAdditionalProperties != null) {
			HashMap<String, String> additionalPropertiesOldName = statesAdditionalProperties.get(oldName);
			if (additionalPropertiesOldName != null) {
				statesAdditionalProperties.remove(oldName);
				statesAdditionalProperties.put(newName, additionalPropertiesOldName);
			}
		}
	}

	/**
	 * @param stateName . {@code String}
	 * @return The index of {@code state} or -1 if it does not exists.
	 * {@code int}
	 * @throws InvalidStateException InvalidStateException
	 */
	public int getStateIndex(String stateName) throws InvalidStateException {

		for (int i = 0; i < states.length; i++) {
			if (states[i].getName().contentEquals(stateName)) {
				return i;
			}
		}
		throw new InvalidStateException(InvalidStateException.generateMsg(this, stateName));
	}

	/**
	 * @param state . {@code State}
	 * @return stateIndex of state. {@code int}
	 * @throws Error if state does not exist
	 */
	public int getStateIndex(State state) {
		for (int i = 0; i < states.length; i++) {
			if (states[i].equals(state)) {
				return i;
			}
		}
		throw new Error("State " + state.getName() + " does" + " not exist in variable " + name);
	}

	/**
	 * @param value . {@code double}
	 * @return The state index corresponding to value. {@code int}
	 * @throws InvalidStateException exception when variable is discrete.
	 */
	public int getStateIndex(double value) throws InvalidStateException {

		int state = -1;
		if (variableType == VariableType.FINITE_STATES) {
			state = getStateIndex(String.valueOf(round(value)));
			if (state == -1) {
				throw new InvalidStateException(
						"Can not use " + "Variable.getStateIndex(double) in the discrete variable." + name);
			}
		} else {
			state = partitionedInterval.indexOfSubinterval(value);
			if (state == -1) {
				throw new InvalidStateException(
						value + " is not in any interval " + "of the discretized variable " + name + " (intervals are "
								+ partitionedInterval.toString() + ").");
			}
		}
		return state;
	}

	/**
	 * @return variableType. {@code VariableType}
	 */
	public VariableType getVariableType() {

		return variableType;
	}

	/**
	 * @param variableType the variableType to set
	 */
	public void setVariableType(VariableType variableType) {

		this.variableType = variableType;
		// TODO this method assume that the states exists a priori
		// when Node is created, Variable is created, then when node
		// is changed from continuous to discrete, the edit has to
		// assign the default state indicated in probNet.
		switch (variableType) {
		case NUMERIC:
			this.setStates(new State[] { new State("") });
			setPartitionedInterval(new PartitionedInterval(getDefaultInterval(1), getDefaultBelongs(1)));
			break;
		case DISCRETIZED:
			setPartitionedInterval(
					new PartitionedInterval(getDefaultInterval(getNumStates()), getDefaultBelongs(getNumStates())));
			break;
		default:
			break;
		}

	}

	/**
	 * @return name. {@code String}
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param newName . {@code String}
	 */
	public void setName(String newName) {
		name = newName;// new String(newName);
		timeSlice = getTimeSlice(name);
	}

	/**
	 * @return name. {@code String}
	 */
	public String getBaseName() {
		if (baseName == null) {
			return name;
		}
		return baseName;
	}

	/**
	 * @param newBaseName . {@code String}
	 */
	public void setBaseName(String newBaseName) {
		this.baseName = newBaseName;
		this.name = this.baseName + ((timeSlice >= 0) ? " [" + timeSlice + "]" : "");
	}

	/**
	 * @return Number of states.
	 */
	public int getNumStates() {

		return states.length;
	}

	/**
	 * @return states. {@code String[]}
	 */
	public State[] getStates() {
		return states;
	}

	/**
	 * @param states the states to set
	 */
	public void setStates(State[] states) {

		this.states = states;
	}

	/**
	 * @param index . {@code int}
	 * @return Name of states[index]. {@code String}.
	 * Condition: index must be a number between 0 and (number-of-states -
	 * 1).
	 */
	public String getStateName(int index) {

		return states[index].getName();
	}

	/**
	 * @param name Name
	 * @return The state whose name is 'name'
	 * @throws InvalidStateException InvalidStateException
	 */
	public State getState(String name) throws InvalidStateException {
		return states[getStateIndex(name)];
	}

	/**
	 * @return partitionedInterval. {@code PartitionedInterval}
	 */
	public PartitionedInterval getPartitionedInterval() {

		return partitionedInterval;
	}

	/**
	 * @param partitionedInterval the partitionedInterval to set
	 */
	public void setPartitionedInterval(PartitionedInterval partitionedInterval) {

		this.partitionedInterval = partitionedInterval;
	}

	/**
	 * @return precision. {@code double}
	 */
	public double getPrecision() {

		return precision;
	}

	/**
	 * @param precision the precision to set
	 */
	public void setPrecision(double precision) {

		this.precision = precision;
	}

	public TablePotential deltaTablePotential(String stateName) throws InvalidStateException {
		List<Variable> potentialVariables = new ArrayList<>();
		potentialVariables.add(this);
		TablePotential potential = new TablePotential(potentialVariables, PotentialRole.CONDITIONAL_PROBABILITY);

		for (int i = 0; i < potential.values.length; i++) {
			potential.values[i] = 0.0;
		}
		potential.values[getStateIndex(stateName)] = 1.0;
		return potential;
	}

	public TablePotential deltaTablePotential(State state) {
		List<Variable> potentialVariables = new ArrayList<>();
		potentialVariables.add(this);
		TablePotential potential = new TablePotential(potentialVariables, PotentialRole.CONDITIONAL_PROBABILITY);

		for (int i = 0; i < potential.values.length; i++) {
			potential.values[i] = 0.0;
		}
		potential.values[getStateIndex(state)] = 1.0;
		return potential;
	}

	public TablePotential createDeltaTablePotential(int stateIndex) throws InvalidStateException {
		List<Variable> potentialVariables = new ArrayList<>();
		potentialVariables.add(this);
		TablePotential potential = new TablePotential(potentialVariables, PotentialRole.CONDITIONAL_PROBABILITY);
		potential.values[stateIndex] = 1.0;
		return potential;
	}

	public double[] getDefaultInterval(int numStates) {
		double[] interval = new double[numStates + 1];
		interval[0] = Double.NEGATIVE_INFINITY;
		interval[numStates] = Double.POSITIVE_INFINITY;
		double count = 0;
		for (int i = 1; i <= numStates - 1; i++) {
			interval[i] = count;
			double precision = Double.valueOf(getPrecision());
			count += precision;
		}
		return interval;
	}

	/*
	 * private double[] getDefaultInterval(int numStates) { double [] interval =
	 * new double[numStates+1]; interval[0] = 0; int count = 2; for (int i=1;i
	 * <= numStates; i++){ interval[i] = count; count += 2; } return interval; }
	 */
	public boolean[] getDefaultBelongs(int numStates) {
		boolean[] limits = new boolean[numStates + 1];
		limits[0] = true;
		for (int i = 1; i < numStates; i++) {
			limits[i] = false;
		}
		return limits;
	}

	/**
	 * Overrides {@code toString} method. Mainly for test purposes
	 */
	public String toString() {

		return name;
	}

	private int getTimeSlice(String variableName) {
		int timeSlice = noTemporalTimeSlice;
		if (variableName.contains(" [")) {
			// Set base name
			int lastOpenBracket = variableName.lastIndexOf(" [");
			baseName = variableName.substring(0, lastOpenBracket);
			int lastClosedBracket = variableName.lastIndexOf("]");
			if (lastClosedBracket > lastOpenBracket) {
				int firstNumber = lastOpenBracket + 2;
				try {
					timeSlice = Integer.valueOf((String) variableName.subSequence(firstNumber, lastClosedBracket));
				} catch (NumberFormatException e) {
					// There is not a number between brackets
				}
			}
		} else {
			baseName = variableName;
		}
		return timeSlice;
	}

	public int getTimeSlice() {
		return timeSlice;
	}

	/**
	 * Changes or sets for the first time the time slice. Modifies the variable
	 * name adding or changing [timeSlice]. If the variable is not temporal
	 * change to temporal.
	 *
	 * @param timeSlice . {@code int}
	 */
	public void setTimeSlice(int timeSlice) {
		if (timeSlice != Integer.MIN_VALUE) {
			int beginSlicePart = name.lastIndexOf('[');
			if (beginSlicePart != -1) {
				baseName = name.substring(0, beginSlicePart - 1);
			}
			name = baseName + " [" + timeSlice + "]";
		}
		this.timeSlice = timeSlice;
	}

	/**
	 * @return the unit
	 */
	public StringWithProperties getUnit() {
		return unit;
	}

	/**
	 * @param unit the unit to set
	 */
	public void setUnit(StringWithProperties unit) {
		this.unit = unit;
	}

	public StringWithProperties getAgent() {
		return agent;
	}

	public void setAgent(StringWithProperties agent) {
		this.agent = agent;
	}

	public Criterion getDecisionCriterion() {
		return decisionCriterion;
	}

	public void setDecisionCriterion(Criterion decisionCriterion) {
		this.decisionCriterion = decisionCriterion;
	}

	public double round(double value) {
		return Math.round(value / precision) * precision;
	}

	@Override public int compareTo(Variable o) {
		int othersHashCode = o.hashCode();
		int thisHashCode = hashCode();
		int result = 0;
		if (othersHashCode > thisHashCode)
			result = -1;
		else if (othersHashCode < thisHashCode)
			result = 0;
		return result;
	}

	/**
	 * This methods checks if a string may be a valid state name
	 *
	 * @param newState New state
	 * @return True if a string may be a valid state name
	 */
	public boolean chekNewStateName(String newState) {
		for (State state : states) {
			if (state.getName().equals(newState)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * This method returns a valid status name based on the current
	 * states and a based prefix defined as a constant in this class
	 *
	 * @return a new valid name for a state
	 */
	public String getNewValidName() {
		String newValidName = null;
		int actualState = 0;
		boolean validName;
		do {
			validName = true;
			newValidName = STATE_BASE_NAME + actualState;
			for (State state : states) {
				if (state.getName().equals(newValidName)) {
					validName = false;
					break;
				}
			}
			actualState++;
		} while (!validName);

		return newValidName;
	}

	// TODO Ver si este código resuelve el problema que se puede dar en
	// Elvira con los dos nombres de las variables que tiene.

}
