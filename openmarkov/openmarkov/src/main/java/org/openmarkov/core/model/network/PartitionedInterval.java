/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.model.network;

/**
 * Defines a set of intervals
 *
 * @author fjdiez
 * @author manuel
 * @version 1.2 jlgozalo add method equals()
 * invariant belongsToLeftSide.length = limits.length
 * @since OpenMarkov 1.0
 */
public class PartitionedInterval implements Cloneable {

	// Attributes
	/**
	 * This numbers delimits the set of subintervals
	 */
	protected double[] limits;

	protected boolean[] belongsToLeftSide;

	/**
	 * Number of sub-intervals
	 */
	protected int numSubintervals;

	// Constructors

	/**
	 * Condition: limits.size() == belongsToLeftSide.size()
	 * Condition: limits[i] &#60;= limits[i+1]
	 * Condition: if limits[i] == limits[i+1] then belongsToLeftSide[i] =
	 * false and belongsToLeftSide[i+1] = true
	 * @param belongsToLeftSide Array pointing If belongs to left side
	 * @param limits Array of limits
	 */
	public PartitionedInterval(double[] limits, boolean[] belongsToLeftSide) {
		this.limits = limits.clone();
		this.belongsToLeftSide = belongsToLeftSide.clone();
		numSubintervals = limits.length - 1;
	}

	/**
	 * Creates a {@code PartitionedInterval} with only one Subinterval
	 * @param leftClosed If left closed
	 * @param min Minimum
	 * @param max Maximum
	 * @param rightClosed If right closed
	 */
	public PartitionedInterval(boolean leftClosed, double min, double max, boolean rightClosed) {
		limits = new double[] { min, max };
		belongsToLeftSide = new boolean[] { leftClosed, rightClosed };
		numSubintervals = 1;
	}

	/**
	 * Creates a {@code PartitionedInterval} from an Object[][] table
	 * @param values Values
	 */
	public PartitionedInterval(Object[][] values) {
		int numSubIntervals = 0;
		double limits[] = null;
		boolean belongsToLeftSide[] = null;
		int i = 0;
		numSubIntervals = values.length;
		limits = new double[numSubIntervals + 1];
		belongsToLeftSide = new boolean[numSubIntervals + 1];
		if (numSubIntervals > 1) {
			try {// id-name-symbol-value-separator-value-symbol
				for (i = 0; i < numSubIntervals; i++) {
					belongsToLeftSide[i] = (values[i][2] == "[" ? true : false);
					limits[i] = ((Double) values[i][3]).doubleValue();
				}
				limits[i] = ((Double) values[i - 1][5]).doubleValue();
				belongsToLeftSide[i] = (values[i - 1][6] == "]" ? true : false);

			} catch (NumberFormatException ex) {
				// TODO set the actions to capture this exception if happens
			}
			this.limits = limits.clone();
			this.belongsToLeftSide = belongsToLeftSide.clone();
			numSubintervals = limits.length - 1;
		} else {
			boolean leftClosed = (values[0][2] == "[" ? false : true);
			boolean rightClosed = (values[0][6] == "]" ? true : false);
			double min = ((Double) values[0][3]).doubleValue();
			double max = ((Double) values[0][5]).doubleValue();
			this.limits = new double[] { min, max };
			this.belongsToLeftSide = new boolean[] { leftClosed, rightClosed };
			this.numSubintervals = 1;

		}
	}

	// Methods

	/**
	 * @param number Number
	 * @return true if the value is included between the outside limits
	 */
	public boolean contains(double number) {
		return (
				((limits[0] < number) && (number < limits[limits.length - 1])) || (
						(number == limits[0]) && !belongsToLeftSide[0]
				) || (
						(number == limits[limits.length - 1]) && belongsToLeftSide[limits.length - 1]
				)
		);
	}

	/**
	 * @param number {@code double}
	 * @return The number of subinterval where is located the number (0, 1, ...)
	 * or -1 if it is outside
	 */
	public int indexOfSubinterval(double number) {
		for (int i = 0; i < limits.length - 1; i++) {
			if (((limits[i] < number) && (number < limits[i + 1])) || ((number == limits[i]) && !belongsToLeftSide[i])
					|| ((number == limits[i + 1]) && belongsToLeftSide[i + 1])) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * This method remove the index-th subinterval.
	 *
	 * @param index {@code int}
	 */
	public void removeSubinterval(int index) {
		double[] newLimits = new double[limits.length - 1];
		boolean[] newBelongsToLeftSide = new boolean[limits.length - 1];
		for (int i = 0; i < newLimits.length; i++) {
			if (i <= index) {
				newLimits[i] = limits[i];
				newBelongsToLeftSide[i] = belongsToLeftSide[i];
			} else {
				newLimits[i] = limits[i + 1];
				newBelongsToLeftSide[i] = belongsToLeftSide[i + 1];
			}
		}

		limits = newLimits.clone();
		belongsToLeftSide = newBelongsToLeftSide.clone();

	}

	/**
	 * @return numSubintervals. {@code int}
	 */
	public int getNumSubintervals() {
		return numSubintervals = limits.length - 1;
	}

	/**
	 * @return limits. {@code double[]}
	 */
	public double[] getLimits() {
		return limits;
	}

	/**
	 * @param index Index
	 * @return limit. {@code double}
	 */
	public double getLimit(int index) {
		return limits[index];
	}

	/**
	 * @return belongsToLeftSide. {@code boolean[]}
	 */
	public boolean[] getBelongsToLeftSide() {
		return belongsToLeftSide;
	}

	/**
	 * @param index {@code int}
	 * @return belongsToLeftSide. {@code boolean}
	 */
	public boolean getBelongsToLeftSide(int index) {
		return belongsToLeftSide[index];
	}

	/**
	 * @param index {@code int}
	 * @return belongsTo. {@code String}
	 */
	public String getBelongsTo(int index) {
		if (belongsToLeftSide[index] == true)
			return "left";
		else
			return "right";
	}

	/**
	 * @return min
	 */
	public double getMin() {
		return limits[0];
	}

	/**
	 * @return max
	 */
	public double getMax() {
		return limits[getNumSubintervals()];
	}

	/**
	 * @return leftClosed
	 */
	public boolean isLeftClosed() {
		return !belongsToLeftSide[0];
	}

	/**
	 * @return rightClosed
	 */
	public boolean isRightClosed() {
		return belongsToLeftSide[getNumSubintervals()];
	}

	/**
	 * @param indexOfLimit Index of limit
	 * @param newLimit New limit
	 * @param newBelongsToLeftSide If new limit belongs to left side
	 * Condition: newLimit &#60; limit[indexOfLimit+1] &#x26;&#x26; newLimit &#62;
	 * limit[indexOfLimit-1]
	 * Condition: if limit[indexOfLimit-1] = newLimit then
	 * belongsToLeftSide[indexOfLimit] = true &#x26;&#x26;
	 * belongsToLeftSide[indexOfLimit-1] = false
	 * Condition: if limit[indexOfLimit+1] = newLimit then
	 * belongsToLeftSide[indexOfLimit] = false &#x26;&#x26;
	 * belongsToLeftSide[indexOfLimit+1] = true
	 */
	public void changeLimit(int indexOfLimit, double newLimit, boolean newBelongsToLeftSide) {
		limits[indexOfLimit] = newLimit;
		belongsToLeftSide[indexOfLimit] = newBelongsToLeftSide;
	}

	/**
	 * Convert a PartitionedInterval in an array of arrays of objects with the
	 * same elements.
	 *
	 * @return an array of arrays of objects that has the same elements.
	 */
	public Object[][] convertToTableFormat() {

		Object[][] data;
		int i = 0;
		int numIntervals = 0;
		int numColumns = 6; // name-symbol-value-separator-value-symbol
		double[] limits;
		boolean[] belongsToLeftSide;

		numIntervals = getNumSubintervals();
		limits = getLimits();
		belongsToLeftSide = getBelongsToLeftSide();
		data = new Object[numIntervals][numColumns];
		for (i = 0; i < numIntervals; i++) {
			//for (i = numIntervals-1; i <=0; i--) {
			data[i][0] = ""; // name
			data[i][1] = (belongsToLeftSide[i] ? "(" : "["); // low interval
			// symbol
			data[i][2] = limits[i]; // low interval value
			data[i][3] = ","; // separator ","
			data[i][4] = limits[i + 1]; // high interval value
			data[i][5] = (belongsToLeftSide[i + 1] ? "]" : ")"); // high
			// interval
			// symbol
		}
		return data;

	}

	/**
	 * print a readable format of the Partitioned Interval
	 */
	public String toString() {
		StringBuilder buffer = new StringBuilder();
		buffer.append("Partitioned Interval ");
		buffer.append("\n");
		buffer.append("  > numSubIntervals = " + getNumSubintervals());
		buffer.append("\n");
		for (int i = 0; i < getNumSubintervals(); i++) {
			buffer.append("   > interval[" + i + "]=");
			buffer.append(!belongsToLeftSide[i] ? "[" : "(");
			buffer.append(limits[i]);
			buffer.append(",");
			buffer.append(limits[i + 1]);
			buffer.append(!belongsToLeftSide[i + 1] ? ")" : "]");
			buffer.append("\n");
		}
		return buffer.toString();
	}

	/**
	 * Indicates whether some other object is "equal to" this one. The graph
	 * which the node belongs to is not compared.
	 *
	 * @param obj object to compare with this one. It must be a NodeProperties
	 *            instance.
	 */
	@Override public boolean equals(Object obj) {

		PartitionedInterval otherInterval;

		boolean result = true;
		if (obj != null && obj instanceof PartitionedInterval) {
			otherInterval = (PartitionedInterval) obj;
			if ((numSubintervals == otherInterval.numSubintervals) && (
					this.belongsToLeftSide.length == otherInterval.belongsToLeftSide.length
			) && (this.limits.length == otherInterval.limits.length)) {
				for (int i = 0; result && (i < this.belongsToLeftSide.length); i++) {
					result = this.belongsToLeftSide[i] == otherInterval.belongsToLeftSide[i];
				}
				for (int i = 0; result && (i < this.limits.length); i++) {
					result = this.limits[i] == otherInterval.limits[i];
				}
			} else {
				result = false;
			}
		} else {
			result = false;
		}
		return result;
	}

	public Object clone() {
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
}
