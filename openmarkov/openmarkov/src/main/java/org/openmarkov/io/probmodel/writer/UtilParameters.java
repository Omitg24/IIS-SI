/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.io.probmodel.writer;

import org.openmarkov.core.exception.WriterException;
import org.openmarkov.core.model.network.ProbNet;

/** Manage the error conditions in PGMXWriter0_2 and newer classes */
public class UtilParameters {

    public static void manageParametersWriter(String netName, ProbNet probNet) throws WriterException {
        if (probNet == null || netName == null) {
            String msg;
            if (probNet == null && netName == null) {
                msg = "Trying to write a null probNet with null name.";
            } else if (probNet == null) {
                msg = "Trying to write a null probNet.";
            } else {
                msg = "Trying to write a probNet with null name.";
            }
            throw new WriterException(msg);
        }

    }
}
