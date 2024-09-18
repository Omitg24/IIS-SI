/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.model.network;

public class CycleLength {

	/**
	 * Cycles of each unit in a year. The order must be the same as in the units (Year, Month, Week, ...)
	 */
	static double[] cyclesInAYear = { 1, 12, 52, 365, 8760, 525600, 31536000, 31536000E3 };
	private final double DEFAULT_CYCLE_LENGTH = 1;
	private final Unit DEFAULT_UNIT = Unit.YEAR;
	/**
	 * Selected unit
	 */
	private Unit unit;
	/**
	 * Scale of the unit
	 */
	private double value;

	public CycleLength() {
		this.unit = DEFAULT_UNIT;
		this.value = DEFAULT_CYCLE_LENGTH;
	}

	public CycleLength(Unit unit) {
		this.unit = unit;
		this.value = DEFAULT_CYCLE_LENGTH;
	}

	public CycleLength(Unit unit, double value) {
		this.unit = unit;
		this.value = value;
	}

	public CycleLength(CycleLength cycleLength) {
		this.unit = cycleLength.unit;
		this.value = cycleLength.value;
	}

	/**
	 * Get the adjusted discount in cycles
	 *
	 * @param cycleUnit         ProbNet cycle selected unit
	 * @param cycleLength       ProbNet cycle length
	 * @param unitToBeConverted Actual discount unit
	 * @param discount          Value of the discount
	 * @return Discount per cycle length
	 */
	public static double getTemporalAdjustedDiscount(Unit cycleUnit, double cycleLength, DiscountUnit unitToBeConverted,
			double discount) {
		if (unitToBeConverted.equals(DiscountUnit.YEAR)) {
			double rate = cyclesInAYear[cycleUnit.ordinal()] / cycleLength;
			return Math.pow(1 + discount, 1 / rate) - 1.0;
		} else { // if(unitToBeConverted.equals(DiscountUnit.CYCLE)){
			return discount;
		}
	}

	public Unit getUnit() {
		return unit;
	}

	public void setUnit(Unit unit) {
		this.unit = unit;
	}

	public double getValue() {
		return value;
	}

	public void setValue(double value) {
		this.value = value;
	}

	public CycleLength clone() {
		return new CycleLength(this);
	}

	/**
	 * Possible units
	 */
	public enum Unit {
		YEAR, MONTH, WEEK, DAY, HOUR, MINUTE, SECOND, MILISECOND
	}

	/**
	 * Temporal units of discounts
	 */
	public enum DiscountUnit {
		YEAR, CYCLE
	}

}
