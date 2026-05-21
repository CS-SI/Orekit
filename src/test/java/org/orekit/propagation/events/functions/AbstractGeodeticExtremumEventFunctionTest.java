/* Copyright 2022-2026 Romain Serra
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
package org.orekit.propagation.events.functions;

import org.hipparchus.analysis.differentiation.UnivariateDerivative1;
import org.hipparchus.analysis.differentiation.UnivariateDerivative2;
import org.hipparchus.analysis.differentiation.UnivariateDerivative2Field;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.bodies.FieldGeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.frames.FramesFactory;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.AbsolutePVCoordinates;
import org.orekit.utils.Constants;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.PVCoordinates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;

class AbstractGeodeticExtremumEventFunctionTest {

    @BeforeEach
    void setUp() {
        Utils.setDataRoot("regular-data");
    }

    @Test
    void testTransformToFieldGeodeticPoint() {
        // GIVEN
        final TestEventFunction eventFunction = new TestEventFunction();
        final PVCoordinates pvCoordinates = new PVCoordinates(new Vector3D(7e6, 1e2, 20), new Vector3D(10., 7.5e3, -1e2), new Vector3D(1, 2, 3));
        final AbsolutePVCoordinates coordinates = new AbsolutePVCoordinates(FramesFactory.getGCRF(), AbsoluteDate.ARBITRARY_EPOCH, pvCoordinates);
        // WHEN
        final FieldGeodeticPoint<UnivariateDerivative1> actualPoint = eventFunction.transformToFieldGeodeticPoint(new SpacecraftState(coordinates));
        // THEN
        final FieldPVCoordinates<UnivariateDerivative2> pv = pvCoordinates.toUnivariateDerivative2PV();
        final UnivariateDerivative2Field field = UnivariateDerivative2Field.getInstance();
        final UnivariateDerivative2 dt = new UnivariateDerivative2(0, 1, 0);
        final FieldAbsoluteDate<UnivariateDerivative2> fieldDate = new FieldAbsoluteDate<>(field, coordinates.getDate()).shiftedBy(dt);
        final FieldGeodeticPoint<UnivariateDerivative2> expected = eventFunction.getBodyShape().transform(pv.getPosition(), coordinates.getFrame(), fieldDate);
        assertEquals(expected.getAltitude().getReal(), actualPoint.getAltitude().getReal());
        assertEquals(expected.getAltitude().getFirstDerivative(), actualPoint.getAltitude().getFirstDerivative());
        assertEquals(expected.getLatitude().getReal(), actualPoint.getLatitude().getReal());
        assertEquals(expected.getLatitude().getFirstDerivative(), actualPoint.getLatitude().getFirstDerivative());
        assertEquals(expected.getLongitude().getReal(), actualPoint.getLongitude().getReal());
        assertEquals(expected.getLongitude().getFirstDerivative(), actualPoint.getLongitude().getFirstDerivative());
    }

    @Test
    void testDependsOnTimeOnly() {
        // GIVEN
        final AbstractGeodeticExtremumEventFunction eventFunction = mock();
        doCallRealMethod().when(eventFunction).dependsOnTimeOnly();
        // WHEN & THEN
        assertFalse(eventFunction.dependsOnTimeOnly());
    }

    @Test
    void testDpendsOnMainVariablesOnly() {
        // GIVEN
        final AbstractGeodeticExtremumEventFunction eventFunction = mock();
        doCallRealMethod().when(eventFunction).dependsOnMainVariablesOnly();
        // WHEN & THEN
        assertTrue(eventFunction.dependsOnMainVariablesOnly());
    }

    private static class TestEventFunction extends AbstractGeodeticExtremumEventFunction {

        protected TestEventFunction() {
            super(new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS, Constants.WGS84_EARTH_FLATTENING,
                    FramesFactory.getGTOD(true)));
        }

        @Override
        public double value(SpacecraftState state) {
            return 0;  // not used
        }
    }
}
