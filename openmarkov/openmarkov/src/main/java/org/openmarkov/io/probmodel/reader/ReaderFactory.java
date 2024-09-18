/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.io.probmodel.reader;

import org.openmarkov.core.exception.ParserException;

/** Create readers given a version */
public class ReaderFactory {

	/** Enum to define the parser version labels */
	private enum Version {
		V02("0.2"),
		V05("0.5");  
		// Extension point in future parser versions.
		
		private String label;
		
		Version(String label) {
			this.label = label;
		}
		
		public String toString() {
			return label;
		}
	}
	
	/**
	 * @param strVersion, read from the PGXML file. <code>String</code>
	 * @return PGMXReader 0_2 or newer
	 * @throws ParserException 
	 */
	public static PGMXReader_0_2 getReader(String strVersion) throws ParserException {
		Version version = Version.V02; // Default value
		for (Version oneVersion : Version.values()) {
			if (strVersion.startsWith(oneVersion.toString())) { 
				// This means that, for example, 0.5.0 will be considered equal to 0.5.1,
				// because last digit is used to correct bugs. As consequence, both will use the same Reader.

				version = oneVersion;
				break;
			}
		}
    	PGMXReader_0_2 reader = null;
    	switch (version) {
    	case V02:
    		reader = new PGMXReader_0_2();
    		break;
    	case V05:
    		reader = new PGMXReader_0_5();
    		break;
   		// Extension point in future parser versions.
    	default:
    		throw new ParserException("The PGMX version " + strVersion + " is not readable in this OpenMarkov version.");
    	}
		return reader;
	}

}
