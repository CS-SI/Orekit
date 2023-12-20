/* Copyright 2023 Thales Alenia Space
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

import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.TrackingCoordinates;


public class CanonicalSaastamoinenModelTest {

    @Test
    public void testComparisonToModifiedModelLowElevation() {
        doTestComparisonToModifiedModel(FastMath.toRadians(5), -13.24, -1.03);
    }

    @Test
    public void testComparisonToModifiedModelHighElevation() {
        doTestComparisonToModifiedModel(FastMath.toRadians(60), -1.35, -0.11);
    }

    private void doTestComparisonToModifiedModel(final double elevation,
                                                 final double minDifference, final double maxDifference) {
        final TrackingCoordinates trackingCoordinates = new TrackingCoordinates(0.0, elevation, 0.0);
        final CanonicalSaastamoinenModel canonical = CanonicalSaastamoinenModel.getStandardModel();
        final ModifiedSaastamoinenModel  modified  = ModifiedSaastamoinenModel.getStandardModel();
        for (double height = 0; height < 5000; height += 100) {
            final GeodeticPoint location = new GeodeticPoint(0.0, 0.0, height);
            final double canonicalDelay = canonical.pathDelay(trackingCoordinates, location,
                                                              TroposphericModelUtils.STANDARD_ATMOSPHERE,
                                                              null, AbsoluteDate.J2000_EPOCH).getDelay();
            final double modifiedDelay  = modified.pathDelay(trackingCoordinates, location,
                                                             TroposphericModelUtils.STANDARD_ATMOSPHERE,
                                                             null, AbsoluteDate.J2000_EPOCH).getDelay();
            Assertions.assertTrue(modifiedDelay - canonicalDelay > minDifference);
            Assertions.assertTrue(modifiedDelay - canonicalDelay < maxDifference);
        }
    }

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("atmosphere");
    }

}
