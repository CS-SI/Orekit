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
package org.orekit;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.bodies.JPLEphemeridesLoader;
import org.orekit.data.DataProvidersManager;
import org.orekit.errors.OrekitException;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.frames.EOPEntryEquinox;
import org.orekit.frames.EOPEntryNonRotatingOrigin;
import org.orekit.frames.EOPHistoryEquinoxLoader;
import org.orekit.frames.EOPHistoryNonRotatingOriginLoader;
import org.orekit.frames.FramesFactory;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;

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

    public static void setDataRoot(String root) {
        try {
            Utils.clearFactoryMaps(CelestialBodyFactory.class);
            CelestialBodyFactory.clearCelestialBodyLoaders();
            Utils.clearFactoryMaps(FramesFactory.class);
            Utils.clearFactoryMaps(TimeScalesFactory.class);
            Utils.clearFactory(TimeScalesFactory.class, TimeScale.class);
            TimeScalesFactory.clearUTCTAILoaders();
            Utils.clearJPLEphemeridesConstants();
            GravityFieldFactory.clearPotentialCoefficientsReaders();
            DataProvidersManager.getInstance().clearProviders();
            DataProvidersManager.getInstance().clearLoadedDataNames();
            StringBuffer buffer = new StringBuffer();
            for (String component : root.split(":")) {
                String componentPath;
                componentPath = Utils.class.getClassLoader().getResource(component).toURI().getPath();
                if (buffer.length() > 0) {
                    buffer.append(System.getProperty("path.separator"));
                }
                buffer.append(componentPath);
            }
            System.setProperty(DataProvidersManager.OREKIT_DATA_PATH, buffer.toString());
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
            Assert.fail(iae.getMessage());
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
            Assert.fail(iae.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private static void clearJPLEphemeridesConstants() {
        try {
            for (Field field : JPLEphemeridesLoader.class.getDeclaredFields()) {
                if (field.getName().equals("CONSTANTS")) {
                    field.setAccessible(true);
                    ((Map<String, Double>) field.get(null)).clear();
                }
            }
        } catch (IllegalAccessException iae) {
            Assert.fail(iae.getMessage());
        }
    }

    public static List<EOPEntryEquinox> buildEquinox(double[][] data) throws OrekitException {
        final List<EOPEntryEquinox> equinox = new ArrayList<EOPEntryEquinox>();
        for (double[] row : data) {
            equinox.add(new EOPEntryEquinox((int) row[0], row[1], row[2],
                                            Constants.ARC_SECONDS_TO_RADIANS * row[3],
                                            Constants.ARC_SECONDS_TO_RADIANS * row[4],
                                            Constants.ARC_SECONDS_TO_RADIANS * row[5],
                                            Constants.ARC_SECONDS_TO_RADIANS * row[6]));
        }
        return equinox;
    }

    public static List<EOPEntryNonRotatingOrigin> buildNRO(double[][] data) throws OrekitException {
        final List<EOPEntryNonRotatingOrigin> nro = new ArrayList<EOPEntryNonRotatingOrigin>();
        for (double[] row : data) {
            nro.add(new EOPEntryNonRotatingOrigin((int) row[0], row[1], row[2],
                                                  Constants.ARC_SECONDS_TO_RADIANS * row[3],
                                                  Constants.ARC_SECONDS_TO_RADIANS * row[4],
                                                  Constants.ARC_SECONDS_TO_RADIANS * row[5],
                                                  Constants.ARC_SECONDS_TO_RADIANS * row[6]));
        }
        return nro;
    }

    public static void setLoaders(final IERSConventions conventions,
                                  final List<EOPEntryEquinox> equinox,
                                  final List<EOPEntryNonRotatingOrigin> nro) {

        Utils.clearFactoryMaps(FramesFactory.class);
        Utils.clearFactoryMaps(TimeScalesFactory.class);

        if (equinox != null) {
            FramesFactory.addEOPHistoryEquinoxLoader(conventions,
                                                     new EOPHistoryEquinoxLoader() {
                public boolean stillAcceptsData() {
                    return true;
                }
                public void loadData(InputStream input, String name) {
                }

                public void fillHistoryEquinox(Collection<? super EOPEntryEquinox> history) {
                    history.addAll(equinox);
                }
            });
        }

        if (nro != null) {
            FramesFactory.addEOPHistoryNonRotatingOriginLoader(conventions,
                                                               new EOPHistoryNonRotatingOriginLoader() {
                public boolean stillAcceptsData() {
                    return true;
                }
                public void loadData(InputStream input, String name) {
                }

                public void fillHistoryNonRotatingOrigin(Collection<? super EOPEntryNonRotatingOrigin> history) {
                    history.addAll(nro);
                }
            });
        }

    }

}
