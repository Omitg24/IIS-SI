/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.io;

import java.io.InputStream;

import org.openmarkov.core.exception.ParserException;
import org.openmarkov.core.model.network.ProbNet;

public interface ProbNetReader {

	/**
	 * @param file File
	 * @param netName = path + network name + extension. {@code String}
	 * @throws ParserException ParserException
	 * @return A {@code ProbNetInfo} or {@code null}
	 */
	ProbNetInfo loadProbNetInfo(String netName, InputStream... file) throws ParserException;

	/**
	 * @param file File
	 * @param netName = path + network name + extension. {@code String}
	 * @throws ParserException ParserException
	 * @return A {@code ProbNet} or {@code null}
	 */
	ProbNet loadProbNet(String netName, InputStream... file) throws ParserException;

}