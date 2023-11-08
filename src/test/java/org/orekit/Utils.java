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
package org.orekit;

import org.junit.jupiter.api.Assertions;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.FrameAlignedProvider;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.data.DataContext;
import org.orekit.data.DataProvidersManager;
import org.orekit.data.LazyLoadedDataContext;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.frames.EOPEntry;
import org.orekit.frames.EopHistoryLoader;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.ITRFVersion;
import org.orekit.models.earth.weather.GlobalPressureTemperature2Model;
import org.orekit.orbits.FieldCartesianOrbit;
import org.orekit.orbits.FieldCircularOrbit;
import org.orekit.orbits.FieldEquinoctialOrbit;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.propagation.semianalytical.dsst.utilities.JacobiPolynomials;
import org.orekit.propagation.semianalytical.dsst.utilities.NewcombOperators;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.GNSSDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.ParameterDriversList;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.concurrent.atomic.AtomicReference;

public class Utils {

    // epsilon for tests
    public static final double epsilonTest  = 1.e-12;

    // epsilon for eccentricity
    public static final double epsilonE     = 1.e+5 * epsilonTest;

    // epsilon for circular eccentricity
    public static final double epsilonEcir  = 1.e+8 * epsilonTest;

    // epsilon for angles
    public static final double epsilonAngle = 1.e+5 * epsilonTest;

    public static final double ae =  6378136.460;
    public static final double mu =  3.986004415e+14;

    public static void clearFactories() {
        DataContext.setDefault(new LazyLoadedDataContext());
        clearFactoryMaps(CelestialBodyFactory.class);
        CelestialBodyFactory.clearCelestialBodyLoaders();
        clearFactoryMaps(FramesFactory.class);
        clearFactoryMaps(TimeScalesFactory.class);
        clearFactory(TimeScalesFactory.class, TimeScale.class);
        clearFactoryMaps(FieldCartesianOrbit.class);
        clearFactoryMaps(FieldKeplerianOrbit.class);
        clearFactoryMaps(FieldCircularOrbit.class);
        clearFactoryMaps(FieldEquinoctialOrbit.class);
        clearFactoryMaps(JacobiPolynomials.class);
        clearFactoryMaps(NewcombOperators.class);
        for (final Class<?> c : NewcombOperators.class.getDeclaredClasses()) {
            if (c.getName().endsWith("PolynomialsGenerator")) {
                clearFactoryMaps(c);
            }
        }
        clearAtomicReference(GlobalPressureTemperature2Model.class);
        FramesFactory.clearEOPHistoryLoaders();
        FramesFactory.setEOPContinuityThreshold(5 * Constants.JULIAN_DAY);
        TimeScalesFactory.clearUTCTAIOffsetsLoaders();
        GNSSDate.setRolloverReference(null);
        GravityFieldFactory.clearPotentialCoefficientsReaders();
        GravityFieldFactory.clearOceanTidesReaders();
        DataContext.getDefault().getDataProvidersManager().clearProviders();
        DataContext.getDefault().getDataProvidersManager().resetFiltersToDefault();
        DataContext.getDefault().getDataProvidersManager().clearLoadedDataNames();

    }

    public static DataContext setDataRoot(String root) {
        try {
            clearFactories();
            StringBuilder buffer = new StringBuilder();
            for (String component : root.split(":")) {
                String componentPath;
                componentPath = Utils.class.getClassLoader().getResource(component).toURI().getPath();
                if (buffer.length() > 0) {
                    buffer.append(System.getProperty("path.separator"));
                }
                buffer.append(componentPath);
            }
            System.setProperty(DataProvidersManager.OREKIT_DATA_PATH, buffer.toString());
            return DataContext.getDefault();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private static void clearFactoryMaps(Class<?> factoryClass) {
        try {
            for (Field field : factoryClass.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) &&
                    Map.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    ((Map<?, ?>) field.get(null)).clear();
                }
            }
        } catch (IllegalAccessException iae) {
            Assertions.fail(iae.getMessage());
        }
    }

    private static void clearFactory(Class<?> factoryClass, Class<?> cachedFieldsClass) {
        try {
            for (Field field : factoryClass.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) &&
                    cachedFieldsClass.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    field.set(null, null);
                }
            }
        } catch (IllegalAccessException iae) {
            Assertions.fail(iae.getMessage());
        }
    }

    private static void clearAtomicReference(Class<?> factoryClass) {
        try {
            for (Field field : factoryClass.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) &&
                    AtomicReference.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    ((AtomicReference<?>) field.get(null)).set(null);
                }
            }
        } catch (IllegalAccessException iae) {
            Assertions.fail(iae.getMessage());
        }
    }

    public static List<EOPEntry> buildEOPList(IERSConventions conventions, ITRFVersion version,
                                              double[][] data) {
        IERSConventions.NutationCorrectionConverter converter =
                conventions.getNutationCorrectionConverter();
        final TimeScale utc = DataContext.getDefault().getTimeScales().getUTC();
        final List<EOPEntry> list = new ArrayList<EOPEntry>();
        for (double[] row : data) {
            final AbsoluteDate date =
                    new AbsoluteDate(new DateComponents(DateComponents.MODIFIED_JULIAN_EPOCH, (int) row[0]),
                                     TimeScalesFactory.getUTC());
            final double[] nro;
            final double[] equinox;
            if (Double.isNaN(row[7])) {
                equinox = new double[] {
                    Constants.ARC_SECONDS_TO_RADIANS * row[5],
                    Constants.ARC_SECONDS_TO_RADIANS * row[6]
                };
                nro     = converter.toNonRotating(date, equinox[0], equinox[1]);
            } else if (Double.isNaN(row[5])) {
                nro     = new double[] {
                    Constants.ARC_SECONDS_TO_RADIANS * row[7],
                    Constants.ARC_SECONDS_TO_RADIANS * row[8]
                };
                equinox = converter.toEquinox(date, nro[0], nro[1]);
            } else {
                equinox = new double[] {
                    Constants.ARC_SECONDS_TO_RADIANS * row[5],
                    Constants.ARC_SECONDS_TO_RADIANS * row[6]
                };
                nro     = new double[] {
                    Constants.ARC_SECONDS_TO_RADIANS * row[7],
                    Constants.ARC_SECONDS_TO_RADIANS * row[8]
                };
            }
            list.add(new EOPEntry((int) row[0], row[1], row[2],
                                  Constants.ARC_SECONDS_TO_RADIANS * row[3],
                                  Constants.ARC_SECONDS_TO_RADIANS * row[4],
                                  Double.NaN, Double.NaN,
                                  equinox[0], equinox[1],
                                  nro[0], nro[1], version,
                                  AbsoluteDate.createMJDDate((int) row[0], 0.0, utc)));
        }
        return list;
    }

    public static void setLoaders(final IERSConventions conventions, final List<EOPEntry> eop) {

        clearFactories();

        FramesFactory.addEOPHistoryLoader(conventions, new EopHistoryLoader() {
            public void fillHistory(IERSConventions.NutationCorrectionConverter converter,
                                    SortedSet<EOPEntry> history) {
                history.addAll(eop);
            }
        });

    }

    /**
     * Assert that the normalized values of given expected and actual {@link ParameterDriversList} are identical.
     *
     * @param expected expected {@link ParameterDriversList}
     * @param actual actual {@link ParameterDriversList}
     */
    public static void assertParametersDriversValues(final ParameterDriversList expected,
                                                     final ParameterDriversList actual) {

        final List<ParameterDriversList.DelegatingDriver> expectedDriversList = expected.getDrivers();
        final List<ParameterDriversList.DelegatingDriver> actualDriversList   = actual.getDrivers();
        for (int i = 0; i < expectedDriversList.size(); i++) {
            final ParameterDriversList.DelegatingDriver currentExpectedDriver = expectedDriversList.get(i);
            final ParameterDriversList.DelegatingDriver currentActualDriver = actualDriversList.get(i);

            Assertions.assertArrayEquals(currentExpectedDriver.getValues(), currentActualDriver.getValues());
            Assertions.assertEquals(currentExpectedDriver.getValue(), currentActualDriver.getValue());
            Assertions.assertEquals(currentExpectedDriver.getNormalizedValue(), currentActualDriver.getNormalizedValue());
            Assertions.assertEquals(currentExpectedDriver.getMaxValue(), currentActualDriver.getMaxValue());
            Assertions.assertEquals(currentExpectedDriver.getMinValue(), currentActualDriver.getMinValue());
            Assertions.assertEquals(currentExpectedDriver.getName(), currentActualDriver.getName());
            Assertions.assertEquals(currentExpectedDriver.getNbOfValues(), currentActualDriver.getNbOfValues());
            Assertions.assertEquals(currentExpectedDriver.getReferenceValue(), currentActualDriver.getReferenceValue());

        }
    }

    /**
     * An attitude law compatible with the old Propagator.DEFAULT_LAW. This is used so as
     * not to change the results of tests written against the old implementation.
     *
     * @return an attitude law.
     */
    public static AttitudeProvider defaultLaw() {
        return FrameAlignedProvider.of(FramesFactory.getEME2000());
    }

}
