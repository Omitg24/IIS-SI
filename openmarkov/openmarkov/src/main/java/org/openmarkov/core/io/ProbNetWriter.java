/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.io;

import java.util.List;

import org.openmarkov.core.exception.WriterException;
import org.openmarkov.core.model.network.EvidenceCase;
import org.openmarkov.core.model.network.ProbNet;

/** Interface that must be implemented to write a ProbNet.<p>
 * Every writer that implements this interface must be annotated with descriptive information, that will be used by OpenMarkov
 * to manage the set of different writers.<p>
 * Annotation: <b>FormatType</b>
 * <ul>
 *   <li>name, for example: "PGMXWriter"</li>
 *   <li>version, for example: "0.2"</li>
 *   <li>extension, for example: "pgmx"</li>
 *   <li>description</li>
 *   <li>role, it can be either "Writer" or "Reader"</li>
 * </ul>
 */
public interface ProbNetWriter {

	/**
	 * @param netName = path + network name + extension.
	 * @param probNet {@code ProbNet} {@code String}
	 * @throws WriterException WriterException
	 */
	void writeProbNet(String netName, ProbNet probNet) throws WriterException;

	/**
	 * @param netName = path + network name + extension.
	 * @param probNet {@code ProbNet} {@code String}
	 * @param evidence Evidence
	 * @throws WriterException WriterException
	 */
	void writeProbNet(String netName, ProbNet probNet, List<EvidenceCase> evidence) throws WriterException;
}
