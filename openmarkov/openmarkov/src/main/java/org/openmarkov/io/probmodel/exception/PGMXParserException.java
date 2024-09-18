/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.io.probmodel.exception;

import org.jdom2.Element;
import org.jdom2.located.LocatedElement;
import org.openmarkov.core.exception.ParserException;

@SuppressWarnings("serial")
public class PGMXParserException extends ParserException {

	private Element element;
	
	public PGMXParserException(String message, Element element) {
		super(message);
		this.element = element;
	}

	@Override
	public String getMessage() {
		StringBuilder sb = new StringBuilder(super.getMessage());
		if (element instanceof LocatedElement)
		{
			LocatedElement locatedElement = (LocatedElement) element;
			sb.append(" (at line ");
			sb.append(locatedElement.getLine());
			sb.append(", column ");
			sb.append(locatedElement.getColumn());
			sb.append(")");
		}
		return  sb.toString();
	}

}
