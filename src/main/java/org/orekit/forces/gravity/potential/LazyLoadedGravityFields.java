/* Contributed in the public domain.
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hipparchus.util.FastMath;
import org.orekit.data.DataProvidersManager;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;

/**
 * Loads gravity fields when first requested and can be configured until then. Designed to
 * match the behavior of {@link GravityFieldFactory} in Orekit 10.0.
 *
 * @author Evan Ward
 * @author Fabien Maussion
 * @author Pascal Parraud
 * @author Luc Maisonobe
 * @see GravityFieldFactory
 * @since 10.1
 */
public class LazyLoadedGravityFields implements GravityFields {

    /** Potential readers. */
    private final List<PotentialCoefficientsReader> readers = new ArrayList<>();

    /** Ocean tides readers. */
    private final List<OceanTidesReader> oceanTidesReaders =
            new ArrayList<>();

    /** Ocean load deformation coefficients. */
    private OceanLoadDeformationCoefficients oceanLoadDeformationCoefficients =
            OceanLoadDeformationCoefficients.IERS_2010;

    /** Provides access to auxiliary data files for loading gravity field files. */
    private final DataProvidersManager dataProvidersManager;

    /** Time scale for parsing dates. */
    private final TimeScale timeScale;

    /**
     * Create a factory for gravity fields that uses the given data manager to load the
     * gravity field files.
     *
     * @param dataProvidersManager provides access to auxiliary data files.
     * @param timeScale            use to parse dates for the {@link #addDefaultPotentialCoefficientsReaders()}.
     *                             In Orekit 10.0 it is TT.
     */
    public LazyLoadedGravityFields(final DataProvidersManager dataProvidersManager,
                                   final TimeScale timeScale) {
        this.dataProvidersManager = dataProvidersManager;
        this.timeScale = timeScale;
    }

    /** Add a reader for gravity fields.
     * @param reader custom reader to add for the gravity field
     * @see #addDefaultPotentialCoefficientsReaders()
     * @see #clearPotentialCoefficientsReaders()
     */
    public void addPotentialCoefficientsReader(final PotentialCoefficientsReader reader) {
        synchronized (readers) {
            readers.add(reader);
        }
    }

    /**
     * Add the default readers for gravity fields.
     *
     * <p> The default readers support ICGEM, SHM, EGM and GRGS formats with the
     * default names {@link GravityFieldFactory#ICGEM_FILENAME}, {@link
     * GravityFieldFactory#SHM_FILENAME}, {@link GravityFieldFactory#EGM_FILENAME}, {@link
     * GravityFieldFactory#GRGS_FILENAME} and don't allow missing coefficients.
     *
     * @see #addPotentialCoefficientsReader(PotentialCoefficientsReader)
     * @see #clearPotentialCoefficientsReaders()
     */
    public void addDefaultPotentialCoefficientsReaders() {
        synchronized (readers) {
            readers.add(new ICGEMFormatReader(GravityFieldFactory.ICGEM_FILENAME, false, timeScale));
            readers.add(new SHMFormatReader(GravityFieldFactory.SHM_FILENAME, false, timeScale));
            readers.add(new EGMFormatReader(GravityFieldFactory.EGM_FILENAME, false));
            readers.add(new GRGSFormatReader(GravityFieldFactory.GRGS_FILENAME, false, timeScale));
        }
    }

    /** Clear gravity field readers.
     * @see #addPotentialCoefficientsReader(PotentialCoefficientsReader)
     * @see #addDefaultPotentialCoefficientsReaders()
     */
    public void clearPotentialCoefficientsReaders() {
        synchronized (readers) {
            readers.clear();
        }
    }

    /** Add a reader for ocean tides.
     * @param reader custom reader to add for the gravity field
     * @see #addDefaultPotentialCoefficientsReaders()
     * @see #clearPotentialCoefficientsReaders()
     */
    public void addOceanTidesReader(final OceanTidesReader reader) {
        synchronized (oceanTidesReaders) {
            oceanTidesReaders.add(reader);
        }
    }

    /** Configure ocean load deformation coefficients.
     * @param oldc ocean load deformation coefficients
     * @see #getOceanLoadDeformationCoefficients()
     */
    public void configureOceanLoadDeformationCoefficients(final OceanLoadDeformationCoefficients oldc) {
        oceanLoadDeformationCoefficients = oldc;
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
    public OceanLoadDeformationCoefficients getOceanLoadDeformationCoefficients() {
        return oceanLoadDeformationCoefficients;
    }

    /** Add the default readers for ocean tides.
     * <p>
     * The default readers support files similar to the fes2004_Cnm-Snm.dat and
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
    public void addDefaultOceanTidesReaders() {
        synchronized (oceanTidesReaders) {

            oceanTidesReaders.add(new FESCnmSnmReader(GravityFieldFactory.FES_CNM_SNM_FILENAME, 1.0e-11));

            final AstronomicalAmplitudeReader aaReader =
                    new AstronomicalAmplitudeReader(GravityFieldFactory.FES_HF_FILENAME, 5, 2, 3, 1.0);
            dataProvidersManager.feed(aaReader.getSupportedNames(), aaReader);
            final Map<Integer, Double> map = aaReader.getAstronomicalAmplitudesMap();
            oceanTidesReaders.add(new FESCHatEpsilonReader(GravityFieldFactory.FES_CHAT_EPSILON_FILENAME,
                    0.01, FastMath.toRadians(1.0),
                    getOceanLoadDeformationCoefficients(),
                    map));


        }
    }

    /** Clear ocean tides readers.
     * @see #addPotentialCoefficientsReader(PotentialCoefficientsReader)
     * @see #addDefaultPotentialCoefficientsReaders()
     */
    public void clearOceanTidesReaders() {
        synchronized (oceanTidesReaders) {
            oceanTidesReaders.clear();
        }
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
    public PotentialCoefficientsReader readGravityField(final int maxParseDegree,
                                                        final int maxParseOrder) {

        synchronized (readers) {

            if (readers.isEmpty()) {
                addDefaultPotentialCoefficientsReaders();
            }

            // test the available readers
            for (final PotentialCoefficientsReader reader : readers) {
                reader.setMaxParseDegree(maxParseDegree);
                reader.setMaxParseOrder(maxParseOrder);
                dataProvidersManager.feed(reader.getSupportedNames(), reader);
                if (!reader.stillAcceptsData()) {
                    return reader;
                }
            }
        }

        throw new OrekitException(OrekitMessages.NO_GRAVITY_FIELD_DATA_LOADED);

    }

    /**
     * {@inheritDoc}
     *
     * <p> If no {@link PotentialCoefficientsReader} has been added by calling {@link
     * #addPotentialCoefficientsReader(PotentialCoefficientsReader)
     * addPotentialCoefficientsReader} or if {@link #clearPotentialCoefficientsReaders()
     * clearPotentialCoefficientsReaders} has been called afterwards, the {@link
     * #addDefaultPotentialCoefficientsReaders() addDefaultPotentialCoefficientsReaders}
     * method will be called automatically.
     */
    @Override
    public NormalizedSphericalHarmonicsProvider getConstantNormalizedProvider(final int degree, final int order,
                                                                              final AbsoluteDate freezingDate) {
        final RawSphericalHarmonicsProvider provider;
        synchronized (readers) {
            final PotentialCoefficientsReader reader = readGravityField(degree, order);
            provider = reader.getProvider(true, degree, order);
        }
        final ConstantSphericalHarmonics frozen = new ConstantSphericalHarmonics(freezingDate, provider);
        return new WrappingNormalizedProvider(frozen);
    }

    /**
     * {@inheritDoc}
     *
     * <p>If no {@link PotentialCoefficientsReader} has been added by calling {@link
     * #addPotentialCoefficientsReader(PotentialCoefficientsReader)
     * addPotentialCoefficientsReader} or if {@link #clearPotentialCoefficientsReaders()
     * clearPotentialCoefficientsReaders} has been called afterwards, the {@link
     * #addDefaultPotentialCoefficientsReaders() addDefaultPotentialCoefficientsReaders}
     * method will be called automatically.
     */
    @Override
    public NormalizedSphericalHarmonicsProvider getNormalizedProvider(final int degree,
                                                                      final int order) {
        final RawSphericalHarmonicsProvider provider;
        synchronized (readers) {
            final PotentialCoefficientsReader reader = readGravityField(degree, order);
            provider = reader.getProvider(true, degree, order);
        }
        return new WrappingNormalizedProvider(provider);
    }

    /**
     * {@inheritDoc}
     *
     * <p>If no {@link PotentialCoefficientsReader} has been added by calling {@link
     * #addPotentialCoefficientsReader(PotentialCoefficientsReader)
     * addPotentialCoefficientsReader} or if {@link #clearPotentialCoefficientsReaders()
     * clearPotentialCoefficientsReaders} has been called afterwards, the {@link
     * #addDefaultPotentialCoefficientsReaders() addDefaultPotentialCoefficientsReaders}
     * method will be called automatically.
     */
    @Override
    public UnnormalizedSphericalHarmonicsProvider getConstantUnnormalizedProvider(final int degree, final int order,
                                                                                  final AbsoluteDate freezingDate) {
        final RawSphericalHarmonicsProvider provider;
        synchronized (readers) {
            final PotentialCoefficientsReader reader = readGravityField(degree, order);
            provider = reader.getProvider(false, degree, order);
        }
        final ConstantSphericalHarmonics frozen = new ConstantSphericalHarmonics(freezingDate, provider);
        return new WrappingUnnormalizedProvider(frozen);
    }

    /**
     * {@inheritDoc}
     *
     * <p>If no {@link PotentialCoefficientsReader} has been added by calling {@link
     * #addPotentialCoefficientsReader(PotentialCoefficientsReader)
     * addPotentialCoefficientsReader} or if {@link #clearPotentialCoefficientsReaders()
     * clearPotentialCoefficientsReaders} has been called afterwards, the {@link
     * #addDefaultPotentialCoefficientsReaders() addDefaultPotentialCoefficientsReaders}
     * method will be called automatically.
     */
    @Override
    public UnnormalizedSphericalHarmonicsProvider getUnnormalizedProvider(final int degree,
                                                                          final int order) {
        final RawSphericalHarmonicsProvider provider;
        synchronized (readers) {
            final PotentialCoefficientsReader reader = readGravityField(degree, order);
            provider = reader.getProvider(false, degree, order);
        }
        return new WrappingUnnormalizedProvider(provider);
    }

    /**
     * {@inheritDoc}
     *
     * <p>If no {@link OceanTidesReader} has been added by calling {@link
     * #addOceanTidesReader(OceanTidesReader)
     * addOceanTidesReader} or if {@link #clearOceanTidesReaders()
     * clearOceanTidesReaders} has been called afterwards, the {@link
     * #addDefaultOceanTidesReaders() addDefaultOceanTidesReaders}
     * method will be called automatically.
     */
    @Override
    public List<OceanTidesWave> getOceanTidesWaves(final int degree, final int order) {

        synchronized (oceanTidesReaders) {

            if (oceanTidesReaders.isEmpty()) {
                addDefaultOceanTidesReaders();
            }

            // test the available readers
            for (final OceanTidesReader reader : oceanTidesReaders) {
                reader.setMaxParseDegree(degree);
                reader.setMaxParseOrder(order);
                dataProvidersManager.feed(reader.getSupportedNames(), reader);
                if (!reader.stillAcceptsData()) {
                    return reader.getWaves();
                }
            }
        }

        throw new OrekitException(OrekitMessages.NO_OCEAN_TIDE_DATA_LOADED);

    }

}
