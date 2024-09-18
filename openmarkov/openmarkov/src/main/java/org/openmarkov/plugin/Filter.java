/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.plugin;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import org.openmarkov.plugin.service.FilterIF;

/**
 * This class is an implementation of PluginsFilterIF
 *
 * @author jvelez
 * @version 1.0
 * <p>
 * Development Environment        :  Eclipse
 * Name of the File               :  Filter.java
 * Creation/Modification History  :
 * <p>
 * jvelez     15/09/2011 19:08:10      Created.
 * Gigaesfera CO.
 * (#)Filter.java 1.0    15/09/2011 19:08:10
 */
public class Filter implements FilterIF {
	private static final String CONSTRAINT_CLASS = "CONSTRAINT_CLASS";
	private static final String CONSTRAINT_INTERFACE = "CONSTRAINT_INTERFACE";
	private static final String CONSTRAINT_ANNOTATION = "CONSTRAINT_ANNOTATION";
	private static final String COMBINATION_AND = "AND";
	private static final String COMBINATION_OR = "OR";

	private Class<?> cls;
	private String type;

	private Filter parent;
	private List<FilterIF> children;
	private String combination;

	/**
	 * Constructor for Filter.
	 */
	private Filter() {
		this(COMBINATION_AND, null);
	}

	/**
	 * Constructor for Filter.
	 *
	 * @param combination the combinational operation.
	 * @param parent      the parent Filter.
	 */
	private Filter(String combination, Filter parent) {
		super();
		this.parent = parent;
		this.children = new ArrayList<FilterIF>();
		this.combination = combination;
	}

	/**
	 * Constructor for Filter.
	 *
	 * @param aClass
	 * @param type
	 */
	private Filter(Class<?> aClass, String type) {
		super();
		this.cls = aClass;
		this.type = type;

	}

	/**
	 * Static constructor for Filter.
	 */
	public static Filter filter() {
		return new Filter();
	}

	/**
	 * Sets a class extension constraint.
	 *
	 * @param aClass the class to extend.
	 * @return the configured plugin filter.
	 */
	public Filter toExtend(Class<?> aClass) {
		FilterIF filter = new Filter(aClass, Filter.CONSTRAINT_CLASS);
		this.children.add(filter);
		return this;
	}

	/**
	 * Sets an interface implementation constraint.
	 *
	 * @param aClass the interface to implement.
	 * @return the configured plugin filter.
	 */
	public Filter toImplement(Class<?> aClass) {
		FilterIF filter = new Filter(aClass, Filter.CONSTRAINT_INTERFACE);
		this.children.add(filter);
		return this;
	}

	/**
	 * Sets an annotation constraint.
	 *
	 * @param aClass the annotation to be present.
	 * @return the configured plugin filter.
	 */
	public Filter toBeAnnotatedBy(Class<?> aClass) {
		FilterIF filter = new Filter(aClass, Filter.CONSTRAINT_ANNOTATION);
		this.children.add(filter);
		return this;
	}

	/**
	 * Combines a set of constrains as and logic.
	 *
	 * @return the configured plugin filter.
	 */
	public Filter and() {
		Filter filter = new Filter(COMBINATION_AND, this);
		this.children.add(filter);
		return filter;
	}

	/**
	 * Combines a set of constrains as or logic.
	 *
	 * @return the configured plugin filter.
	 */
	public Filter or() {
		Filter filter = new Filter(COMBINATION_OR, this);
		this.children.add(filter);
		return filter;
	}

	/**
	 * Closes a set of constrains.
	 *
	 * @return the configured plugin filter.
	 */
	public Filter end() {
		if (parent == null)
			return this;
		return parent;
	}

	/**
	 * Checks whether a class is a valid plugin.
	 *
	 * @param aClass the class to validate.
	 * @return true if the class is a valid plugin.
	 */
	public boolean checkPlugin(Class<?> aClass) {
		if (isSimpleFilter()) {
			if (CONSTRAINT_CLASS.equals(type))
				return cls.isAssignableFrom(aClass);
			if (CONSTRAINT_INTERFACE.equals(type))
				return cls.isAssignableFrom(aClass);
			if (CONSTRAINT_ANNOTATION.equals(type)) {
				Annotation[] annotations = aClass.getAnnotations();
				for (Annotation anAnnotation : annotations)
					if (anAnnotation.annotationType().equals(cls))
						return true;
				return false;
			}
		} else {
			if (COMBINATION_AND.equals(combination)) {
				boolean result = true;
				for (FilterIF aFilter : children)
					result &= aFilter.checkPlugin(aClass);
				return result;
			}
			if (COMBINATION_OR.equals(combination)) {
				boolean result = false;
				for (FilterIF aFilter : children)
					result |= aFilter.checkPlugin(aClass);
				return result;
			}
		}
		return false;
	}

	/**
	 * Returns the hashCode.
	 *
	 * @return the hashCode.
	 */
	@Override public int hashCode() {
		return 31 * 31 * 31 * 31 * ((cls == null) ? 0 : cls.hashCode()) + 31 * 31 * 31 * (
				(type == null) ? 0 : type.hashCode()
		) + 31 * 31 * ((parent == null) ? 0 : parent.hashCode()) + 31 * ((children == null) ? 0 : children.hashCode())
				+ ((combination == null) ? 0 : combination.hashCode());

	}

	/**
	 * Indicates whether the other object is equals to this one.
	 *
	 * @return true if the other object is equals to this one.
	 */
	@Override public boolean equals(Object other) {
		if (this == other)
			return true;
		if (other == null)
			return false;
		if (other instanceof Filter) {
			Filter aPlugin = (Filter) other;
			return (cls == null) ?
					true :
					cls.equals(aPlugin.cls) && (type == null) ?
							true :
							type.equals(aPlugin.type) && (parent == null) ?
									true :
									parent.equals(aPlugin.parent) && (children == null) ?
											true :
											children.equals(aPlugin.children) && (combination == null) ?
													true :
													combination.equals(aPlugin.combination);
		}
		return false;
	}

	/**
	 * Returns the String representing this object.
	 *
	 * @return the String representing this object.
	 */
	@Override public String toString() {
		StringBuffer strBuffer = new StringBuffer();
		if (cls != null) {
			strBuffer.append("[Filter] - (Simple) {");
			strBuffer.append("class = ");
			strBuffer.append(cls);
			strBuffer.append(", constraint Type = ");
			strBuffer.append(type);
			strBuffer.append("}");
		} else {
			strBuffer.append("[Filter] - (Complex) {");
			strBuffer.append("parent = ");
			strBuffer.append(parent);
			strBuffer.append(", combination = ");
			strBuffer.append(combination);
			strBuffer.append(", children = ");
			strBuffer.append(children);
			strBuffer.append("}");
		}
		return strBuffer.toString();
	}

	/**
	 * Indicates whether the filter is not complex.
	 *
	 * @return true is the filter is not complex
	 */
	private boolean isSimpleFilter() {
		return (cls != null) && (type != null);
	}
}
