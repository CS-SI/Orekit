/* Copyright 2002-2023 CS GROUP
 * Licensed to CS GROUP (CS) under one or more
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

import java.util.List;

import org.hipparchus.util.FastMath;
import org.hipparchus.util.Precision;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.DataContext;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.AbsoluteDate;

/** Factory used to read gravity field files in several supported formats.
 * @author Fabien Maussion
 * @author Pascal Parraud
 * @author Luc Maisonobe
 */
public class GravityFieldFactory {

    /* These constants were left here instead of being moved to LazyLoadedGravityFields
     * because they are public.
     */

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

    /** Private constructor.
     * <p>This class is a utility class, it should neither have a public
     * nor a default constructor. This private constructor prevents
     * the compiler from generating one automatically.</p>
     */
    private GravityFieldFactory() {
    }

    /* Data loading methods. */

    /**
     * Get the instance of {@link GravityFields} that is called by the static methods of
     * this class.
     *
     * @return the gravity fields used by this factory.
     * @since 10.1
     */
    @DefaultDataContext
    public static LazyLoadedGravityFields getGravityFields() {
        return DataContext.getDefault().getGravityFields();
    }

    /** Add a reader for gravity fields.
     * @param reader custom reader to add for the gravity field
     * @see #addDefaultPotentialCoefficientsReaders()
     * @see #clearPotentialCoefficientsReaders()
     */
    @DefaultDataContext
    public static void addPotentialCoefficientsReader(final PotentialCoefficientsReader reader) {
        getGravityFields().addPotentialCoefficientsReader(reader);
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
    @DefaultDataContext
    public static void addDefaultPotentialCoefficientsReaders() {
        getGravityFields().addDefaultPotentialCoefficientsReaders();
    }

    /** Clear gravity field readers.
     * @see #addPotentialCoefficientsReader(PotentialCoefficientsReader)
     * @see #addDefaultPotentialCoefficientsReaders()
     */
    @DefaultDataContext
    public static void clearPotentialCoefficientsReaders() {
        getGravityFields().clearPotentialCoefficientsReaders();
    }

    /** Add a reader for ocean tides.
     * @param reader custom reader to add for the gravity field
     * @see #addDefaultPotentialCoefficientsReaders()
     * @see #clearPotentialCoefficientsReaders()
     */
    @DefaultDataContext
    public static void addOceanTidesReader(final OceanTidesReader reader) {
        getGravityFields().addOceanTidesReader(reader);
    }

    /** Configure ocean load deformation coefficients.
     * @param oldc ocean load deformation coefficients
     * @see #getOceanLoadDeformationCoefficients()
     */
    @DefaultDataContext
    public static void configureOceanLoadDeformationCoefficients(final OceanLoadDeformationCoefficients oldc) {
        getGravityFields().configureOceanLoadDeformationCoefficients(oldc);
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
    @DefaultDataContext
    public static OceanLoadDeformationCoefficients getOceanLoadDeformationCoefficients() {
        return getGravityFields().getOceanLoadDeformationCoefficients();
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
          * @see #addPotentialCoefficientsReader(PotentialCoefficientsReader)
     * @see #clearPotentialCoefficientsReaders()
     * @see #configureOceanLoadDeformationCoefficients(OceanLoadDeformationCoefficients)
     * @see #getOceanLoadDeformationCoefficients()
     */
    @DefaultDataContext
    public static void addDefaultOceanTidesReaders() {
        getGravityFields().addDefaultOceanTidesReaders();
    }

    /** Clear ocean tides readers.
     * @see #addPotentialCoefficientsReader(PotentialCoefficientsReader)
     * @see #addDefaultPotentialCoefficientsReaders()
     */
    @DefaultDataContext
    public static void clearOceanTidesReaders() {
        getGravityFields().clearOceanTidesReaders();
    }

    /** Get the constant gravity field coefficients provider from the first supported file.
     * <p>
     * If no {@link PotentialCoefficientsReader} has been added by calling {@link
     * #addPotentialCoefficientsReader(PotentialCoefficientsReader)
     * addPotentialCoefficientsReader} or if {@link #clearPotentialCoefficientsReaders()
     * clearPotentialCoefficientsReaders} has been called afterwards, the {@link
     * #addDefaultPotentialCoefficientsReaders() addDefaultPotentialCoefficientsReaders}
     * method will be called automatically.
     * </p>
     * @param degree maximal degree
     * @param order maximal order
     * @param freezingDate freezing epoch
     * @return a gravity field coefficients provider containing already loaded data
     * @since 12.0
     * @see #getNormalizedProvider(int, int)
     */
    @DefaultDataContext
    public static NormalizedSphericalHarmonicsProvider getConstantNormalizedProvider(final int degree, final int order,
                                                                                     final AbsoluteDate freezingDate) {
        return getGravityFields().getConstantNormalizedProvider(degree, order, freezingDate);
    }

    /** Get the gravity field coefficients provider from the first supported file.
     * <p>
     * If no {@link PotentialCoefficientsReader} has been added by calling {@link
     * #addPotentialCoefficientsReader(PotentialCoefficientsReader)
     * addPotentialCoefficientsReader} or if {@link #clearPotentialCoefficientsReaders()
     * clearPotentialCoefficientsReaders} has been called afterwards, the {@link
     * #addDefaultPotentialCoefficientsReaders() addDefaultPotentialCoefficientsReaders}
     * method will be called automatically.
     * </p>
     * @param degree maximal degree
     * @param order maximal order
     * @return a gravity field coefficients provider containing already loaded data
     * @since 6.0
     * @see #getConstantNormalizedProvider(int, int, AbsoluteDate)
     */
    @DefaultDataContext
    public static NormalizedSphericalHarmonicsProvider getNormalizedProvider(final int degree,
                                                                             final int order) {
        return getGravityFields().getNormalizedProvider(degree, order);
    }

    /** Get the constant gravity field coefficients provider from the first supported file.
     * <p>
     * If no {@link PotentialCoefficientsReader} has been added by calling {@link
     * #addPotentialCoefficientsReader(PotentialCoefficientsReader)
     * addPotentialCoefficientsReader} or if {@link #clearPotentialCoefficientsReaders()
     * clearPotentialCoefficientsReaders} has been called afterwards, the {@link
     * #addDefaultPotentialCoefficientsReaders() addDefaultPotentialCoefficientsReaders}
     * method will be called automatically.
     * </p>
     * @param degree maximal degree
     * @param order maximal order
     * @param freezingDate freezing epoch
     * @return a gravity field coefficients provider containing already loaded data
     * @since 6.0
     * @see #getUnnormalizedProvider(int, int)
     */
    @DefaultDataContext
    public static UnnormalizedSphericalHarmonicsProvider getConstantUnnormalizedProvider(final int degree, final int order,
                                                                                         final AbsoluteDate freezingDate) {
        return getGravityFields().getConstantUnnormalizedProvider(degree, order, freezingDate);
    }

    /** Get the gravity field coefficients provider from the first supported file.
     * <p>
     * If no {@link PotentialCoefficientsReader} has been added by calling {@link
     * #addPotentialCoefficientsReader(PotentialCoefficientsReader)
     * addPotentialCoefficientsReader} or if {@link #clearPotentialCoefficientsReaders()
     * clearPotentialCoefficientsReaders} has been called afterwards, the {@link
     * #addDefaultPotentialCoefficientsReaders() addDefaultPotentialCoefficientsReaders}
     * method will be called automatically.
     * </p>
     * @param degree maximal degree
     * @param order maximal order
     * @return a gravity field coefficients provider containing already loaded data
     * @since 6.0
     * @see #getConstantUnnormalizedProvider(int, int, AbsoluteDate)
     */
    @DefaultDataContext
    public static UnnormalizedSphericalHarmonicsProvider getUnnormalizedProvider(final int degree,
                                                                                 final int order) {
        return getGravityFields().getUnnormalizedProvider(degree, order);
    }

    /** Read a gravity field coefficients provider from the first supported file.
     * <p>
     * If no {@link PotentialCoefficientsReader} has been added by calling {@link
     * #addPotentialCoefficientsReader(PotentialCoefficientsReader)
     * addPotentialCoefficientsReader} or if {@link #clearPotentialCoefficientsReaders()
     * clearPotentialCoefficientsReaders} has been called afterwards, the {@link
     * #addDefaultPotentialCoefficientsReaders() addDefaultPotentialCoefficientsReaders}
     * method will be called automatically.
     * </p>
     * @param maxParseDegree maximal degree to parse
     * @param maxParseOrder maximal order to parse
     * @return a reader containing already loaded data
     * @since 6.0
     */
    @DefaultDataContext
    public static PotentialCoefficientsReader readGravityField(final int maxParseDegree,
                                                               final int maxParseOrder) {
        return getGravityFields().readGravityField(maxParseDegree, maxParseOrder);
    }

    /** Get the ocean tides waves from the first supported file.
     * <p>
     * If no {@link OceanTidesReader} has been added by calling {@link
     * #addOceanTidesReader(OceanTidesReader)
     * addOceanTidesReader} or if {@link #clearOceanTidesReaders()
     * clearOceanTidesReaders} has been called afterwards, the {@link
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
     * @since 6.1
     */
    @DefaultDataContext
    public static List<OceanTidesWave> getOceanTidesWaves(final int degree, final int order) {
        return getGravityFields().getOceanTidesWaves(degree, order);
    }

    /* static helper methods that don't load data. */

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
        final Flattener flattener = new Flattener(normalizedC.length - 1, normalizedC[normalizedC.length - 1].length - 1);
        final RawSphericalHarmonicsProvider constant =
                        new ConstantSphericalHarmonics(ae, mu, tideSystem, flattener,
                                                       flattener.flatten(normalizedC), flattener.flatten(normalizedS));
        return new WrappingNormalizedProvider(constant);
    }

    /** Create a {@link NormalizedSphericalHarmonicsProvider} from an {@link UnnormalizedSphericalHarmonicsProvider}.
     * <p>
     * Note that contrary to the other factory method, this one does not read any data, it simply uses
     * the provided data.
     * </p>
     * @param unnormalized provider to normalize
     * @return provider for normalized coefficients
     * @since 6.0
     */
    public static NormalizedSphericalHarmonicsProvider getNormalizedProvider(final UnnormalizedSphericalHarmonicsProvider unnormalized) {
        return new Normalizer(unnormalized);
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
        final Flattener flattener = new Flattener(unnormalizedC.length - 1, unnormalizedC[unnormalizedC.length - 1].length - 1);
        final RawSphericalHarmonicsProvider constant =
                        new ConstantSphericalHarmonics(ae, mu, tideSystem, flattener,
                                                       flattener.flatten(unnormalizedC), flattener.flatten(unnormalizedS));
        return new WrappingUnnormalizedProvider(constant);
    }

    /** Create an {@link UnnormalizedSphericalHarmonicsProvider} from a {@link NormalizedSphericalHarmonicsProvider}.
     * <p>
     * Note that contrary to the other factory method, this one does not read any data, it simply uses
     * the provided data.
     * </p>
     * @param normalized provider to un-normalize
     * @return provider for un-normalized coefficients
     * @since 6.0
     */
    public static UnnormalizedSphericalHarmonicsProvider getUnnormalizedProvider(final NormalizedSphericalHarmonicsProvider normalized) {
        return new Unnormalizer(normalized);
    }

    /** Get a un-normalization factors array.
     * <p>
     * Un-normalized coefficients are obtained by multiplying normalized
     * coefficients by the factors array elements.
     * </p>
     * @param degree maximal degree
     * @param order maximal order
     * @return triangular un-normalization factors array
     * @since 6.0
     */
    public static double[][] getUnnormalizationFactors(final int degree, final int order) {

        // allocate a triangular array
        final int rows = degree + 1;
        final double[][] factor = new double[rows][];
        factor[0] = new double[] {1.0};

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
