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
package org.orekit.propagation.events;

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.util.Decimal64Field;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.FieldCartesianOrbit;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.OrbitType;
import org.orekit.propagation.analytical.FieldEcksteinHechlerPropagator;
import org.orekit.propagation.events.FieldEventsLogger.FieldLoggedEvent;
import org.orekit.propagation.events.handlers.FieldContinueOnEvent;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.FieldPVCoordinates;

public class FieldApsideDetectorTest {
    
    @Test
    public void testSimple() throws OrekitException{
        doTestSimple(Decimal64Field.getInstance());
    }
    
    private <T extends RealFieldElement<T>> void doTestSimple(Field<T> field) throws OrekitException {
        final T zero = field.getZero();

        final TimeScale utc = TimeScalesFactory.getUTC();
        final FieldVector3D<T> position = new FieldVector3D<T>(zero.add(-6142438.668), zero.add(3492467.56), zero.add(-25767.257));
        final FieldVector3D<T> velocity = new FieldVector3D<T>(zero.add(506.0), zero.add(943.0), zero.add(7450));
        final FieldAbsoluteDate<T> date = new FieldAbsoluteDate<T>(field, 2003, 9, 16, utc);
        final FieldOrbit<T> orbit = new FieldCartesianOrbit<T>(new FieldPVCoordinates<T>(position,  velocity),
                                               FramesFactory.getEME2000(), date,
                                               Constants.EIGEN5C_EARTH_MU);
        FieldEcksteinHechlerPropagator<T> propagator =
                        new FieldEcksteinHechlerPropagator<T>(orbit,
                                                      Constants.EIGEN5C_EARTH_EQUATORIAL_RADIUS,
                                                      Constants.EIGEN5C_EARTH_MU,
                                                      zero.add(Constants.EIGEN5C_EARTH_C20),
                                                      zero.add(Constants.EIGEN5C_EARTH_C30),
                                                      zero.add(Constants.EIGEN5C_EARTH_C40),
                                                      zero.add(Constants.EIGEN5C_EARTH_C50),
                                                      zero.add(Constants.EIGEN5C_EARTH_C60));

        FieldEventDetector<T> detector = new FieldApsideDetector<T>(propagator.getInitialState().getOrbit()).
                                 withMaxCheck(zero.add(600.0)).
                                 withThreshold(zero.add(1.0e-12)).
                                 withHandler(new FieldContinueOnEvent<FieldApsideDetector<T>,T>());

        Assert.assertEquals(600.0, detector.getMaxCheckInterval().getReal(), 1.0e-15);
        Assert.assertEquals(1.0e-12, detector.getThreshold().getReal(), 1.0e-15);
        Assert.assertEquals(AbstractDetector.DEFAULT_MAX_ITER, detector.getMaxIterationCount());
        
        FieldEventsLogger<T> logger = new FieldEventsLogger<T>();
        propagator.addEventDetector(logger.monitorDetector(detector));

        propagator.propagate(propagator.getInitialState().getOrbit().getDate().shiftedBy(Constants.JULIAN_DAY));

        Assert.assertEquals(30, logger.getLoggedEvents().size());
        for (FieldLoggedEvent<T> e : logger.getLoggedEvents()) {
            FieldKeplerianOrbit<T> o = (FieldKeplerianOrbit<T>) OrbitType.KEPLERIAN.convertType(e.getState().getOrbit());
            double expected = e.isIncreasing() ? 0.0 : FastMath.PI;
            Assert.assertEquals(expected, MathUtils.normalizeAngle(o.getMeanAnomaly().getReal(), expected), 4.0e-14);
        }

    }

    @Before
    public void setUp() throws OrekitException {
        Utils.setDataRoot("regular-data");
    }

}

