/* Copyright 2002-2010 CS Communication & Systèmes
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

/** Factory used to read gravity field files in several supported formats.
 * @author Fabien Maussion
 * @author Luc Maisonobe
 * @version $Revision$ $Date$
 * @deprecated as of 4.2, replaced by {@link GravityFieldFactory}
 */
@Deprecated
public class PotentialReaderFactory {

    /** Potential readers. */
    private final List<PotentialCoefficientsReader> readers;

    /** Simple constructor.
     * <p>
     * This constructor uses default values for gravity field file names
     * regular expressions: "^g(\\d)+_eigen_(\\w)+_coef$" for ICGEM files,
     * "^eigen[-_](\\w)+_coef$" for SHM files, "^egm\\d\\d_to\\d.*$" for
     * EGM files and "^grim\\d_.*$" for GRGS files.
     * </p>
     */
    public PotentialReaderFactory() {
        readers = new ArrayList<PotentialCoefficientsReader>();
        readers.add(new ICGEMFormatReader("^g(\\d)+_eigen_(\\w)+_coef$", true));
        readers.add(new SHMFormatReader("^eigen[-_](\\w)+_coef$", true));
        readers.add(new EGMFormatReader("^egm\\d\\d_to\\d.*$", true));
        readers.add(new GRGSFormatReader("^grim\\d_.*$", true));
    }

    /** Simple constructor.
     * @param icgemFicNames regular expression for ICGEM (Eigen) gravity field files,
     * if null, ICGEM files reader will not be set up
     * @param shmFicNames regular expression for SHM (Eigen) gravity field files,
     * if null, SHM files reader will not be set up
     * @param egmFicNames regular expression for EGM gravity field files,
     * if null, EGM files reader will not be set up
     * @param grgsFicNames regular expression for GRGS gravity field files,
     * if null, GRGS files reader will not be set up
     */
    public PotentialReaderFactory(final String icgemFicNames, final String shmFicNames,
                                  final String egmFicNames, final String grgsFicNames) {
        readers = new ArrayList<PotentialCoefficientsReader>();
        if (icgemFicNames != null) {
            readers.add(new ICGEMFormatReader(icgemFicNames, true));
        }
        if (shmFicNames != null) {
            readers.add(new SHMFormatReader(shmFicNames, true));
        }
        if (egmFicNames != null) {
            readers.add(new EGMFormatReader(egmFicNames, true));
        }
        if (grgsFicNames != null) {
            readers.add(new GRGSFormatReader(grgsFicNames, true));
        }
    }

    /** Adds a {@link PotentialCoefficientsReader} to the test list.
     * By construction, the default list already contains the {@link
     * ICGEMFormatReader}, {@link SHMFormatReader}, {@link EGMFormatReader}
     * and {@link GRGSFormatReader}
     * @param reader the reader to add
     */
    public void addPotentialReader(final PotentialCoefficientsReader reader) {
        readers.add(reader);
    }

    /** Get the gravity field coefficients provider from the first supported file.
     * @return a gravity field coefficients provider containing already loaded data
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

        throw new OrekitException("no gravity field data loaded");

    }

}
