/* Copyright 2002-2008 CS Communication & Systèmes
 * Licensed to CS Communication & Systèmes (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * CS licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.orekit.forces.gravity.potential;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.orekit.errors.OrekitException;

/** This pattern determines which reader to use with the selected file.
 * @author Fabien Maussion
 * @version $Revision:1665 $ $Date:2008-06-11 12:12:59 +0200 (mer., 11 juin 2008) $
 */
public class PotentialReaderFactory {

    /** Potential readers. */
    private List<PotentialCoefficientsReader> readers;

    /** Simple constructor.
     */
    public PotentialReaderFactory() {
        readers = new ArrayList<PotentialCoefficientsReader>();
        readers.add(new SHMFormatReader());
        readers.add(new EGMFormatReader());
    }

    /** Adds a {@link PotentialCoefficientsReader} to the test list.
     * By construction, the list contains allready the {@link SHMFormatReader}
     * and the {@link EGMFormatReader}.
     * @param reader the reader to add
     */
    public void addPotentialReader(final PotentialCoefficientsReader reader) {
        readers.add(reader);
    }

    /** Determines the proper reader to use wich the selected file.
     * It tests all the readers it contains to see if they match the input format.
     * @param in the file to check (it can be compressed)
     * @return the proper reader
     * @exception OrekitException when no known reader can read the file
     * @exception IOException when the {@link InputStream} is not valid.
     */
    public PotentialCoefficientsReader getPotentialReader(final InputStream in)
        throws OrekitException, IOException {

        BufferedInputStream filter = new BufferedInputStream(in);
        filter.mark(1024 * 1024);

        boolean isCompressed = false;
        try {
            isCompressed = new GZIPInputStream(filter).read() != -1;
        } catch (IOException e) {
            isCompressed = false;
        }
        filter.reset();

        if (isCompressed) {
            filter = new BufferedInputStream(new GZIPInputStream(filter));
        }
        filter.mark(1024 * 1024);
        PotentialCoefficientsReader result = null;

        // test the available readers
        for (final PotentialCoefficientsReader test : readers) {
            if (test.isFileOK(filter)) {
                result = test;
            }
            filter.reset();
        }

        if (result == null) {
            throw new OrekitException("Unknown file format ", new Object[0]);
        }

        return result;

    }

}
