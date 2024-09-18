/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.io.probmodel.strings;

/** Names of attributes */
public enum XMLAttributes {
	BELONGS_TO("belongsTo"),
	DIRECTED("directed"),
	DISTRIBUTION("distribution"),
	FORMAT_VERSION("formatVersion"),
	FUNCTION("function"),
	LABEL("label"),
	NAME("name"),
	NUMERIC_VALUE("numericValue"),
	ORDER("order"),
	//CMI
	//For Univariate
	PARAMETRIZATION("parametrization"),
	//CMF
	REF("ref"),
	ROLE("role"),
	SHOW_COMMENT("showWhenOpeningNetwork"),
	TIMESLICE("timeSlice"),
	TYPE("type"),
	VALUE("value"),
	VAR1("var1"),
	VAR2("var2"),
	X("x"),
	Y("y"),
	//TODO OOBN start
	IS_INPUT("isInput"),
	//TODO OOBN end
	
	UNIT("unit");
	
	private int type;
	
	private String name;
	
	XMLAttributes(String name) {
		this.type = this.ordinal();
		this.name = name;
	}
	
	public String toString() {
		return name;
	}
	
	public int getType() {
		return type;
	}

	
}
