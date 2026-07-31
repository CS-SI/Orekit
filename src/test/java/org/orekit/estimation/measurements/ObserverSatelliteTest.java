/* Copyright 2025-2026 Hawkeye 360 (HE360)
 * Licensed to CS Group (CS) under one or more
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
package org.orekit.estimation.measurements;

import java.util.HashMap;
import java.util.Map;

import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.estimation.EstimationTestUtils;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.KeplerianExtendedPositionProvider;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.ExtendedPositionProvider;
import org.orekit.utils.FieldPVCoordinatesProvider;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.ParameterDriver;

class ObserverSatelliteTest {

    @Test
    void testExtendedPositionProvider() {
                
        EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final Frame frame = FramesFactory.getEME2000();
        final AbsoluteDate epoch = new AbsoluteDate(2004, 1, 1, 23, 30, 0.0, TimeScalesFactory.getUTC());
        final Vector3D position  = new Vector3D(-6142438.668, 3492467.560, -25767.25680);
        final Vector3D velocity  = new Vector3D(505.8479685, 942.7809215, 7435.922231);
        final Orbit initialOrbit = new KeplerianOrbit(new PVCoordinates(position, velocity),
                                                      frame, epoch, Constants.WGS84_EARTH_MU);

        final KeplerianExtendedPositionProvider kepExtendedPosProvider = new KeplerianExtendedPositionProvider(initialOrbit);

        ObserverSatellite satellite = new ObserverSatellite("extended-pos-satellite", kepExtendedPosProvider);

        // Setup measurement to generate estimates
        int nbParams = 6;
        final Map<String, Integer> indices = new HashMap<>();

        for (ParameterDriver driver : satellite.getParametersDrivers()) {
            driver.setReferenceDate(epoch);
            driver.setSelected(true);
            indices.put(driver.getNameSpan(epoch), nbParams++);
        }

        // Checks to make sure that fieldCoordsProvider is an instance of ExtendedPositionProvider
        final FieldPVCoordinatesProvider<Gradient> fieldCoordsProvider = satellite.getFieldPVCoordinatesProvider(nbParams, indices);
        Assertions.assertEquals(0, fieldCoordsProvider.getClass().getName()
            .substring(0, 41).compareTo(ExtendedPositionProvider.class.getName()));
    }

}
