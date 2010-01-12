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
 * @author Pascal Parraud
 * @author Luc Maisonobe
 * @version $Revision$ $Date$
 */
public class GravityFieldFactory {

    /** Default regular expression for ICGEM files. */
    public static final String ICGEM_FILENAME = "^.*\\.gfc$";

    /** Default regular expression for SHM files. */
    public static final String SHM_FILENAME = "^eigen[-_](\\w)+_coef$";

    /** Default regular expression for EGM files. */
    public static final String EGM_FILENAME = "^egm\\d\\d_to\\d.*$";

    /** Default regular expression for GRGS files. */
    public static final String GRGS_FILENAME = "^grim\\d_.*$";

    /** Potential readers. */
    private static final List<PotentialCoefficientsReader> readers =
        new ArrayList<PotentialCoefficientsReader>();

    /** Private constructor.
     * <p>This class is a utility class, it should neither have a public
     * nor a default constructor. This private constructor prevents
     * the compiler from generating one automatically.</p>
     */
    private GravityFieldFactory() {
    }

    /** Add a reader for gravity fields.
     * @param reader custom reader to add for the gravity field
     * @see #addDefaultPotentialCoefficientsReaders()
     * @see #clearPotentialCoefficientsReaders()
     */
    public static void addPotentialCoefficientsReader(final PotentialCoefficientsReader reader) {
        synchronized (readers) {
            readers.add(reader);
        }
    }

    /** Add the default readers for gravity fields.
     * <p>
     * The default readers supports ICGEM, SHM, EGM and GRGS formats with the
     * default names {@link #ICGEM_FILENAME}, {@link #SHM_FILENAME}, {@link
     * #EGM_FILENAME}, {@link #GRGS_FILENAME} and don't allow missing coefficients.
     * </p>
     * @see #addPotentialCoefficientsReader(PotentialCoefficientsReader)
     * @see #clearPotentialCoefficientsReaders()
     */
    public static void addDefaultPotentialCoefficientsReaders() {
        synchronized (readers) {
            readers.add(new ICGEMFormatReader(ICGEM_FILENAME, false));
            readers.add(new SHMFormatReader(SHM_FILENAME, false));
            readers.add(new EGMFormatReader(EGM_FILENAME, false));
            readers.add(new GRGSFormatReader(GRGS_FILENAME, false));
        }
    }

    /** Clear gravity field readers.
     * @see #addPotentialCoefficientsReader(PotentialCoefficientsReader)
     * @see #addDefaultPotentialCoefficientsReaders()
     */
    public static void clearPotentialCoefficientsReaders() {
        synchronized (readers) {
            readers.clear();
        }
    }

    /** Get the gravity field coefficients provider from the first supported file.
     * <p>
     * If no {@link PotentialCoefficientsReader} has been added by calling {@link
     * #addPotentialCoefficientsReader(PotentialCoefficientsReader)
     * addPotentialCoefficientsReader} or if {@link #clearPotentialCoefficientsReaders()
     * clearPotentialCoefficientsReaders} has been called afterwards,the {@link
     * #addDefaultPotentialCoefficientsReaders() addDefaultPotentialCoefficientsReaders}
     * method will be called automatically.
     * </p>
     * @return a gravity field coefficients provider containing already loaded data
     * @exception IOException if data can't be read
     * @exception ParseException if data can't be parsed
     * @exception OrekitException if some data is missing
     * or if some loader specific error occurs
     */
    public static PotentialCoefficientsProvider getPotentialProvider()
        throws IOException, ParseException, OrekitException {

        synchronized (readers) {

            if (readers.isEmpty()) {
                addDefaultPotentialCoefficientsReaders();
            }

            // test the available readers
            for (final PotentialCoefficientsReader reader : readers) {
                DataProvidersManager.getInstance().feed(reader.getSupportedNames(), reader);
                if (!reader.stillAcceptsData()) {
                    return reader;
                }
            }
        }

        throw new OrekitException("no gravity field data loaded");

    }

}
