/* Copyright 2002-2013 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.util.Precision;
import org.orekit.data.DataProvidersManager;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;

/** Factory used to read gravity field files in several supported formats.
 * @author Fabien Maussion
 * @author Pascal Parraud
 * @author Luc Maisonobe
 */
public class GravityFieldFactory {

    /** Default regular expression for ICGEM files. */
    public static final String ICGEM_FILENAME = "^(.*\\.gfc)|(g(\\d)+_eigen[-_](\\w)+_coef)$";

    /** Default regular expression for SHM files. */
    public static final String SHM_FILENAME = "^eigen[-_](\\w)+_coef$";

    /** Default regular expression for EGM files. */
    public static final String EGM_FILENAME = "^egm\\d\\d_to\\d.*$";

    /** Default regular expression for GRGS files. */
    public static final String GRGS_FILENAME = "^grim\\d_.*$";

    /** Potential READERS. */
    private static final List<PotentialCoefficientsReader> READERS =
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
        synchronized (READERS) {
            READERS.add(reader);
        }
    }

    /** Add the default READERS for gravity fields.
     * <p>
     * The default READERS supports ICGEM, SHM, EGM and GRGS formats with the
     * default names {@link #ICGEM_FILENAME}, {@link #SHM_FILENAME}, {@link
     * #EGM_FILENAME}, {@link #GRGS_FILENAME} and don't allow missing coefficients.
     * </p>
     * @see #addPotentialCoefficientsReader(PotentialCoefficientsReader)
     * @see #clearPotentialCoefficientsReaders()
     */
    public static void addDefaultPotentialCoefficientsReaders() {
        synchronized (READERS) {
            READERS.add(new ICGEMFormatReader(ICGEM_FILENAME, false));
            READERS.add(new SHMFormatReader(SHM_FILENAME, false));
            READERS.add(new EGMFormatReader(EGM_FILENAME, false));
            READERS.add(new GRGSFormatReader(GRGS_FILENAME, false));
        }
    }

    /** Clear gravity field READERS.
     * @see #addPotentialCoefficientsReader(PotentialCoefficientsReader)
     * @see #addDefaultPotentialCoefficientsReaders()
     */
    public static void clearPotentialCoefficientsReaders() {
        synchronized (READERS) {
            READERS.clear();
        }
    }

    /** Get the constant gravity field coefficients provider from the first supported file.
     * <p>
     * If no {@link PotentialCoefficientsReader} has been added by calling {@link
     * #addPotentialCoefficientsReader(PotentialCoefficientsReader)
     * addPotentialCoefficientsReader} or if {@link #clearPotentialCoefficientsReaders()
     * clearPotentialCoefficientsReaders} has been called afterwards,the {@link
     * #addDefaultPotentialCoefficientsReaders() addDefaultPotentialCoefficientsReaders}
     * method will be called automatically.
     * </p>
     * @param degree maximal degree
     * @param order maximal order
     * @return a gravity field coefficients provider containing already loaded data
     * @exception OrekitException if some data can't be read (missing or read error)
     * or if some loader specific error occurs
     * @since 6.0
     * @see #getNormalizedProvider(int, int)
     */
    public static NormalizedSphericalHarmonicsProvider getConstantNormalizedProvider(final int degree,
                                                                                     final int order)
        throws OrekitException {
        final PotentialCoefficientsReader reader = readGravityField(degree, order);
        final RawSphericalHarmonicsProvider provider = reader.getConstantProvider(true, degree, order);
        return new WrappingNormalizedProvider(provider);
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
     * @param degree maximal degree
     * @param order maximal order
     * @return a gravity field coefficients provider containing already loaded data
     * @exception OrekitException if some data can't be read (missing or read error)
     * or if some loader specific error occurs
     * @since 6.0
     * @see #getConstantNormalizedProvider(int, int)
     */
    public static NormalizedSphericalHarmonicsProvider getNormalizedProvider(final int degree,
                                                                             final int order)
        throws OrekitException {
        final PotentialCoefficientsReader reader = readGravityField(degree, order);
        final RawSphericalHarmonicsProvider provider = reader.getProvider(true, degree, order);
        return new WrappingNormalizedProvider(provider);
    }

    /** Create a time-independent {@link NormalizedSphericalHarmonicsProvider} from canonical coefficients.
     * <p>
     * Note that contrary to the other factory method, this one does not read any data, it simply uses
     * the provided data
     * </p>
     * @param ae central body reference radius
     * @param mu central body attraction coefficient
     * @param normalizedC normalized tesseral-sectorial coefficients (cosine part)
     * @param normalizedS normalized tesseral-sectorial coefficients (sine part)
     * @since 6.0
     */
    public static NormalizedSphericalHarmonicsProvider getNormalizedProvider(final double ae, final double mu,
                                                                             final double[][] normalizedC,
                                                                             final double[][] normalizedS) {
        return new WrappingNormalizedProvider(new ConstantSphericalHarmonics(ae, mu, normalizedC, normalizedS));
    }

    /** Get the constant gravity field coefficients provider from the first supported file.
     * <p>
     * If no {@link PotentialCoefficientsReader} has been added by calling {@link
     * #addPotentialCoefficientsReader(PotentialCoefficientsReader)
     * addPotentialCoefficientsReader} or if {@link #clearPotentialCoefficientsReaders()
     * clearPotentialCoefficientsReaders} has been called afterwards,the {@link
     * #addDefaultPotentialCoefficientsReaders() addDefaultPotentialCoefficientsReaders}
     * method will be called automatically.
     * </p>
     * @param degree maximal degree
     * @param order maximal order
     * @return a gravity field coefficients provider containing already loaded data
     * @exception OrekitException if some data can't be read (missing or read error)
     * or if some loader specific error occurs
     * @since 6.0
     * @see #getUnnormalizedProvider(int, int)
     */
    public static UnnormalizedSphericalHarmonicsProvider getConstantUnnormalizedProvider(final int degree,
                                                                                         final int order)
        throws OrekitException {
        final PotentialCoefficientsReader reader = readGravityField(degree, order);
        final RawSphericalHarmonicsProvider provider = reader.getConstantProvider(false, degree, order);
        return new WrappingUnnormalizedProvider(provider);
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
     * @param degree maximal degree
     * @param order maximal order
     * @return a gravity field coefficients provider containing already loaded data
     * @exception OrekitException if some data can't be read (missing or read error)
     * or if some loader specific error occurs
     * @since 6.0
     * @see #getConstantUnnormalizedProvider(int, int)
     */
    public static UnnormalizedSphericalHarmonicsProvider getUnnormalizedProvider(final int degree,
                                                                                 final int order)
        throws OrekitException {
        final PotentialCoefficientsReader reader = readGravityField(degree, order);
        final RawSphericalHarmonicsProvider provider = reader.getProvider(false, degree, order);
        return new WrappingUnnormalizedProvider(provider);
    }

    /** Create a time-independent {@link UnnormalizedSphericalHarmonicsProvider} from canonical coefficients.
     * <p>
     * Note that contrary to the other factory method, this one does not read any data, it simply uses
     * the provided data
     * </p>
     * @param ae central body reference radius
     * @param mu central body attraction coefficient
     * @param unnormalizedC un-normalized tesseral-sectorial coefficients (cosine part)
     * @param unnormalizedS un-normalized tesseral-sectorial coefficients (sine part)
     * @since 6.0
     */
    public static UnnormalizedSphericalHarmonicsProvider getUnnormalizedProvider(final double ae, final double mu,
                                                                                 final double[][] unnormalizedC,
                                                                                 final double[][] unnormalizedS) {
        return new WrappingUnnormalizedProvider(new ConstantSphericalHarmonics(ae, mu, unnormalizedC, unnormalizedS));
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
     * @exception OrekitException if some data is missing
     * or if some loader specific error occurs
     * @deprecated as of 6.0, replaced by {@link #getUnnormalizedProvider(int, int)}
     */
    public static PotentialCoefficientsProvider getPotentialProvider()
        throws OrekitException {
        final PotentialCoefficientsReader reader = readGravityField(Integer.MAX_VALUE, Integer.MAX_VALUE);
        final RawSphericalHarmonicsProvider provider = reader.getConstantProvider(false,
                                                                                  reader.getMaxAvailableDegree(),
                                                                                  reader.getMaxAvailableOrder());
        return new ProviderConverter(new WrappingUnnormalizedProvider(provider));
    }

    /** Read a gravity field coefficients provider from the first supported file.
     * <p>
     * If no {@link PotentialCoefficientsReader} has been added by calling {@link
     * #addPotentialCoefficientsReader(PotentialCoefficientsReader)
     * addPotentialCoefficientsReader} or if {@link #clearPotentialCoefficientsReaders()
     * clearPotentialCoefficientsReaders} has been called afterwards,the {@link
     * #addDefaultPotentialCoefficientsReaders() addDefaultPotentialCoefficientsReaders}
     * method will be called automatically.
     * </p>
     * @param maxParseDegree maximal degree to parse
     * @param maxParseOrder maximal order to parse
     * @return a reader containing already loaded data
     * @exception OrekitException if some data is missing
     * or if some loader specific error occurs
     * @since 6.0
     */
    public static PotentialCoefficientsReader readGravityField(final int maxParseDegree,
                                                               final int maxParseOrder)
        throws OrekitException {

        synchronized (READERS) {

            if (READERS.isEmpty()) {
                addDefaultPotentialCoefficientsReaders();
            }

            // test the available readers
            for (final PotentialCoefficientsReader reader : READERS) {
                reader.setMaxParseDegree(maxParseDegree);
                reader.setMaxParseOrder(maxParseOrder);
                DataProvidersManager.getInstance().feed(reader.getSupportedNames(), reader);
                if (!reader.stillAcceptsData()) {
                    return reader;
                }
            }
        }

        throw new OrekitException(OrekitMessages.NO_GRAVITY_FIELD_DATA_LOADED);

    }

    /** Get a un-normalization factors array.
     * <p>
     * Un-normalized coefficients are obtained by multiplying normalized
     * coefficients by the factors array elements.
     * </p>
     * @param degree maximal degree
     * @param order maximal order
     * @return triangular un-normalization factors array
     * @exception OrekitException if degree and order are too large
     * and the latest coefficients underflow
     * @since 6.0
     */
    public static double[][] getUnnormalizationFactors(final int degree, final int order)
        throws OrekitException {

        // allocate a triangular array
        final double[][] factor = new double[degree + 1][];
        factor[0] = new double[] {
            1.0
        };

        // compute the factors
        for (int n = 1; n <= degree; n++) {
            final double[] row = new double[FastMath.min(n, order) + 1];
            row[0] = FastMath.sqrt(2 * n + 1);
            double coeff = 2.0 * (2 * n + 1);
            for (int m = 1; m < row.length; m++) {
                coeff /= (n - m + 1) * (n + m);
                row[m] = FastMath.sqrt(coeff);
                if (row[m] < Precision.SAFE_MIN) {
                    throw new OrekitException(OrekitMessages.GRAVITY_FIELD_NORMALIZATION_UNDERFLOW,
                                              n, m);
                }
            }
            factor[n] = row;
        }

        return factor;

    }

}
