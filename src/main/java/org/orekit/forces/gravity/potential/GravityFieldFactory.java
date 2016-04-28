/* Copyright 2002-2016 CS Systèmes d'Information
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
import java.util.Map;

import org.hipparchus.util.FastMath;
import org.hipparchus.util.Precision;
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

    /** Default regular expression for FES Cnm, Snm tides files. */
    public static final String FES_CNM_SNM_FILENAME = "^fes(\\d)+_Cnm-Snm.dat$";

    /** Default regular expression for FES C hat and epsilon tides files. */
    public static final String FES_CHAT_EPSILON_FILENAME = "^fes(\\d)+.dat$";

    /** Default regular expression for FES Hf tides files. */
    public static final String FES_HF_FILENAME = "^hf-fes(\\d)+.dat$";

    /** Potential readers. */
    private static final List<PotentialCoefficientsReader> READERS =
        new ArrayList<PotentialCoefficientsReader>();

    /** Ocean tides readers. */
    private static final List<OceanTidesReader> OCEAN_TIDES_READERS =
        new ArrayList<OceanTidesReader>();

    /** Ocean load deformation coefficients. */
    private static OceanLoadDeformationCoefficients OCEAN_LOAD_DEFORMATION_COEFFICIENTS =
        OceanLoadDeformationCoefficients.IERS_2010;

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

    /** Add the default readers for gravity fields.
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

    /** Clear gravity field readers.
     * @see #addPotentialCoefficientsReader(PotentialCoefficientsReader)
     * @see #addDefaultPotentialCoefficientsReaders()
     */
    public static void clearPotentialCoefficientsReaders() {
        synchronized (READERS) {
            READERS.clear();
        }
    }

    /** Add a reader for ocean tides.
     * @param reader custom reader to add for the gravity field
     * @see #addDefaultPotentialCoefficientsReaders()
     * @see #clearPotentialCoefficientsReaders()
     */
    public static void addOceanTidesReader(final OceanTidesReader reader) {
        synchronized (OCEAN_TIDES_READERS) {
            OCEAN_TIDES_READERS.add(reader);
        }
    }

    /** Configure ocean load deformation coefficients.
     * @param oldc ocean load deformation coefficients
     * @see #getOceanLoadDeformationCoefficients()
     */
    public static void configureOceanLoadDeformationCoefficients(final OceanLoadDeformationCoefficients oldc) {
        OCEAN_LOAD_DEFORMATION_COEFFICIENTS = oldc;
    }

    /** Get the configured ocean load deformation coefficients.
     * <p>
     * If {@link #configureOceanLoadDeformationCoefficients(OceanLoadDeformationCoefficients)
     * configureOceanLoadDeformationCoefficients} has never been called, the default
     * value will be the {@link OceanLoadDeformationCoefficients#IERS_2010 IERS 2010}
     * coefficients.
     * </p>
     * @return ocean load deformation coefficients
     * @see #configureOceanLoadDeformationCoefficients(OceanLoadDeformationCoefficients)
     */
    public static OceanLoadDeformationCoefficients getOceanLoadDeformationCoefficients() {
        return OCEAN_LOAD_DEFORMATION_COEFFICIENTS;
    }

    /** Add the default READERS for ocean tides.
     * <p>
     * The default READERS supports files similar to the fes2004_Cnm-Snm.dat and
     * fes2004.dat as published by IERS, using the {@link
     * #configureOceanLoadDeformationCoefficients(OceanLoadDeformationCoefficients)
     * configured} ocean load deformation coefficients, which by default are the
     * IERS 2010 coefficients, which are limited to degree 6. If higher degree
     * coefficients are needed, the {@link
     * #configureOceanLoadDeformationCoefficients(OceanLoadDeformationCoefficients)
     * configureOceanLoadDeformationCoefficients} method can be called prior to
     * loading the ocean tides model with the {@link
     * OceanLoadDeformationCoefficients#GEGOUT high degree coefficients} computed
     * by Pascal Gégout.
     * </p>
     * <p>
     * WARNING: the files referenced in the published conventions have some errors.
     * These errors have been corrected and the updated files can be found here:
     * <a href="http://tai.bipm.org/iers/convupdt/convupdt_c6.html">
     * http://tai.bipm.org/iers/convupdt/convupdt_c6.html</a>.
     * </p>
     * @exception OrekitException if astronomical amplitudes cannot be read
     * @see #addPotentialCoefficientsReader(PotentialCoefficientsReader)
     * @see #clearPotentialCoefficientsReaders()
     * @see #configureOceanLoadDeformationCoefficients(OceanLoadDeformationCoefficients)
     * @see #getOceanLoadDeformationCoefficients()
     */
    public static void addDefaultOceanTidesReaders()
        throws OrekitException {
        synchronized (OCEAN_TIDES_READERS) {

            OCEAN_TIDES_READERS.add(new FESCnmSnmReader(FES_CNM_SNM_FILENAME, 1.0e-11));

            final AstronomicalAmplitudeReader aaReader =
                    new AstronomicalAmplitudeReader(FES_HF_FILENAME, 5, 2, 3, 1.0);
            DataProvidersManager.getInstance().feed(aaReader.getSupportedNames(), aaReader);
            final Map<Integer, Double> map = aaReader.getAstronomicalAmplitudesMap();
            OCEAN_TIDES_READERS.add(new FESCHatEpsilonReader(FES_CHAT_EPSILON_FILENAME,
                                                             0.01, FastMath.toRadians(1.0),
                                                             getOceanLoadDeformationCoefficients(),
                                                             map));


        }
    }

    /** Clear ocean tides readers.
     * @see #addPotentialCoefficientsReader(PotentialCoefficientsReader)
     * @see #addDefaultPotentialCoefficientsReaders()
     */
    public static void clearOceanTidesReaders() {
        synchronized (OCEAN_TIDES_READERS) {
            OCEAN_TIDES_READERS.clear();
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
     * @param tideSystem tide system
     * @param normalizedC normalized tesseral-sectorial coefficients (cosine part)
     * @param normalizedS normalized tesseral-sectorial coefficients (sine part)
     * @return provider for normalized coefficients
     * @since 6.0
     */
    public static NormalizedSphericalHarmonicsProvider getNormalizedProvider(final double ae, final double mu,
                                                                             final TideSystem tideSystem,
                                                                             final double[][] normalizedC,
                                                                             final double[][] normalizedS) {
        return new WrappingNormalizedProvider(new ConstantSphericalHarmonics(ae, mu, tideSystem,
                                                                             normalizedC, normalizedS));
    }

    /** Create a {@link NormalizedSphericalHarmonicsProvider} from an {@link UnnormalizedSphericalHarmonicsProvider}.
     * <p>
     * Note that contrary to the other factory method, this one does not read any data, it simply uses
     * the provided data.
     * </p>
     * @param unnormalized provider to normalize
     * @return provider for normalized coefficients
     * @exception OrekitException if degree and order are too large
     * and the normalization coefficients underflow
     * @since 6.0
     */
    public static NormalizedSphericalHarmonicsProvider getNormalizedProvider(final UnnormalizedSphericalHarmonicsProvider unnormalized)
        throws OrekitException {
        return new Normalizer(unnormalized);
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
     * @param tideSystem tide system
     * @param unnormalizedC un-normalized tesseral-sectorial coefficients (cosine part)
     * @param unnormalizedS un-normalized tesseral-sectorial coefficients (sine part)
     * @return provider for un-normalized coefficients
     * @since 6.0
     */
    public static UnnormalizedSphericalHarmonicsProvider getUnnormalizedProvider(final double ae, final double mu,
                                                                                 final TideSystem tideSystem,
                                                                                 final double[][] unnormalizedC,
                                                                                 final double[][] unnormalizedS) {
        return new WrappingUnnormalizedProvider(new ConstantSphericalHarmonics(ae, mu, tideSystem,
                                                                               unnormalizedC, unnormalizedS));
    }

    /** Create an {@link UnnormalizedSphericalHarmonicsProvider} from a {@link NormalizedSphericalHarmonicsProvider}.
     * <p>
     * Note that contrary to the other factory method, this one does not read any data, it simply uses
     * the provided data.
     * </p>
     * @param normalized provider to un-normalize
     * @return provider for un-normalized coefficients
     * @exception OrekitException if degree and order are too large
     * and the un-normalization coefficients underflow
     * @since 6.0
     */
    public static UnnormalizedSphericalHarmonicsProvider getUnnormalizedProvider(final NormalizedSphericalHarmonicsProvider normalized)
        throws OrekitException {
        return new Unnormalizer(normalized);
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
        final int rows = degree + 1;
        final double[][] factor = new double[rows][];
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

    /** Get the ocean tides waves from the first supported file.
     * <p>
     * If no {@link OceanTidesReader} has been added by calling {@link
     * #addOceanTidesReader(OceanTidesReader)
     * addOceanTidesReader} or if {@link #clearOceanTidesReaders()
     * clearOceanTidesReaders} has been called afterwards,the {@link
     * #addDefaultOceanTidesReaders() addDefaultOceanTidesReaders}
     * method will be called automatically.
     * </p>
     * <p><span style="color:red">
     * WARNING: as of 2013-11-17, there seem to be an inconsistency when loading
     * one or the other file, for wave Sa (Doodson number 56.554) and P1 (Doodson
     * number 163.555). The sign of the coefficients are different. We think the
     * problem lies in the input files from IERS and not in the conversion (which
     * works for all other waves), but cannot be sure. For this reason, ocean
     * tides are still considered experimental at this date.
     * </span></p>
     * @param degree maximal degree
     * @param order maximal order
     * @return list of tides waves containing already loaded data
     * @exception OrekitException if some data can't be read (missing or read error)
     * or if some loader specific error occurs
     * @since 6.1
     */
    public static List<OceanTidesWave> getOceanTidesWaves(final int degree, final int order)
        throws OrekitException {

        synchronized (OCEAN_TIDES_READERS) {

            if (OCEAN_TIDES_READERS.isEmpty()) {
                addDefaultOceanTidesReaders();
            }

            // test the available readers
            for (final OceanTidesReader reader : OCEAN_TIDES_READERS) {
                reader.setMaxParseDegree(degree);
                reader.setMaxParseOrder(order);
                DataProvidersManager.getInstance().feed(reader.getSupportedNames(), reader);
                if (!reader.stillAcceptsData()) {
                    return reader.getWaves();
                }
            }
        }

        throw new OrekitException(OrekitMessages.NO_OCEAN_TIDE_DATA_LOADED);

    }

}
