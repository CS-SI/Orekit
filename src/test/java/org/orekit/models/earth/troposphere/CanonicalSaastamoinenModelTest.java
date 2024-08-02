/* Copyright 2002-2024 Thales Alenia Space
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
package org.orekit.models.earth.troposphere;

import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathArrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.bodies.FieldGeodeticPoint;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.models.earth.weather.FieldPressureTemperatureHumidity;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldTrackingCoordinates;
import org.orekit.utils.TrackingCoordinates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


class CanonicalSaastamoinenModelTest {

    @Test
    void testComparisonToModifiedModelLowElevation() {
        doTestComparisonToModifiedModel(FastMath.toRadians(5), -13.4, 0.14);
    }

    @Test
    void testComparisonToModifiedModelHighElevation() {
        doTestComparisonToModifiedModel(FastMath.toRadians(60), -1.36, 0.002);
    }

    private void doTestComparisonToModifiedModel(final double elevation,
                                                 final double minDifference, final double maxDifference) {
        final TrackingCoordinates trackingCoordinates = new TrackingCoordinates(0.0, elevation, 0.0);
        final CanonicalSaastamoinenModel canonical = new CanonicalSaastamoinenModel();
        final ModifiedSaastamoinenModel  modified  = ModifiedSaastamoinenModel.getStandardModel();
        assertTrue(canonical.getParametersDrivers().isEmpty());
        canonical.setLowElevationThreshold(0.125);
        assertEquals(0.125, canonical.getLowElevationThreshold(), 1.0e-12);
        canonical.setLowElevationThreshold(CanonicalSaastamoinenModel.DEFAULT_LOW_ELEVATION_THRESHOLD);
        for (double height = 0; height < 5000; height += 100) {
            final GeodeticPoint location = new GeodeticPoint(0.0, 0.0, height);
            final double canonicalDelay = canonical.pathDelay(trackingCoordinates, location,
                                                              TroposphericModelUtils.STANDARD_ATMOSPHERE,
                                                              null, AbsoluteDate.J2000_EPOCH).getDelay();
            final double modifiedDelay  = modified.pathDelay(trackingCoordinates, location,
                                                             TroposphericModelUtils.STANDARD_ATMOSPHERE,
                                                             null, AbsoluteDate.J2000_EPOCH).getDelay();
            assertTrue(modifiedDelay - canonicalDelay > minDifference);
            assertTrue(modifiedDelay - canonicalDelay < maxDifference);
            final Binary64Field field = Binary64Field.getInstance();
            assertEquals(canonicalDelay,
                                    canonical.pathDelay(new FieldTrackingCoordinates<>(field, trackingCoordinates),
                                                        new FieldGeodeticPoint<>(field, location),
                                                        new FieldPressureTemperatureHumidity<>(field,
                                                                                               TroposphericModelUtils.STANDARD_ATMOSPHERE),
                                                        MathArrays.buildArray(field, 0),
                                                        FieldAbsoluteDate.getJ2000Epoch(field)).getDelay().getReal(),
                                     1.0e-10);
        }
    }

    @BeforeEach
    void setUp() {
        Utils.setDataRoot("atmosphere");
    }

}
