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

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.orekit.data.DataProvidersManager;
import org.orekit.errors.OrekitException;

/** Factory used to read gravity potential files in several supported formats.
 * @author Fabien Maussion
 * @version $Revision:1665 $ $Date:2008-06-11 12:12:59 +0200 (mer., 11 juin 2008) $
 */
public class PotentialReaderFactory {

    /** Potential readers. */
    private final List<PotentialCoefficientsReader> readers;

    /** Simple constructor.
     * <p>
     * This constructor uses default values for gravity potential file names
     * regular expressions: "^g(\\d)+_eigen_(\\w)+_coef$" for ICGEM files,
     * "^eigen[-_](\\w)+_coef$" for SHM files and "^egm\\d\\d_to\\d.*$" for
     * EGM files.
     * </p>
     */
    public PotentialReaderFactory() {
        readers = new ArrayList<PotentialCoefficientsReader>();
        readers.add(new ICGEMFormatReader("^g(\\d)+_eigen_(\\w)+_coef$"));
        readers.add(new SHMFormatReader("^eigen[-_](\\w)+_coef$"));
        readers.add(new EGMFormatReader("^egm\\d\\d_to\\d.*$"));
    }

    /** Simple constructor.
     * @param icgemficNames regular expression for ICGEM (Eigen) gravity potential files,
     * if null, ICGEM files reader will not be set up
     * @param shmficNames regular expression for SHM (Eigen) gravity potential files,
     * if null, SHM files reader will not be set up
     * @param egmficNames regular expression for EGM gravity potential files,
     * if null, EGM files reader will not be set up
     */
    public PotentialReaderFactory(final String icgemficNames, final String shmficNames,
                                  final String egmficNames) {
        readers = new ArrayList<PotentialCoefficientsReader>();
        if (icgemficNames != null) {
            readers.add(new ICGEMFormatReader(icgemficNames));
        }
        if (shmficNames != null) {
            readers.add(new SHMFormatReader(shmficNames));
        }
        if (egmficNames != null) {
            readers.add(new EGMFormatReader(egmficNames));
        }
    }

    /** Adds a {@link PotentialCoefficientsReader} to the test list.
     * By construction, the default list already contains the {@link SHMFormatReader}
     * and the {@link EGMFormatReader}.
     * @param reader the reader to add
     */
    public void addPotentialReader(final PotentialCoefficientsReader reader) {
        readers.add(reader);
    }

    /** Get the gravity potential coefficients provider from the first supported file.
     * @return a gravity potential coefficients provider containing already loaded data
     * @exception IOException if data can't be read
     * @exception ParseException if data can't be parsed
     * @exception OrekitException if some data is missing
     * or if some loader specific error occurs
     */
    public PotentialCoefficientsProvider getPotentialProvider()
        throws IOException, ParseException, OrekitException {

        // test the available readers
        for (final PotentialCoefficientsReader reader : readers) {
            DataProvidersManager.getInstance().feed(reader.getSupportedNames(), reader);
            if (!reader.stillAcceptsData()) {
                return reader;
            }
        }

        throw new OrekitException("no gravity potential data loaded");

    }

}
