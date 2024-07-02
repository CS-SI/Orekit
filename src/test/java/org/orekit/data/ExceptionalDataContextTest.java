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
package org.orekit.data;

import org.hamcrest.MatcherAssert;
import org.hipparchus.ode.nonstiff.ClassicalRungeKuttaIntegrator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.OrekitMatchers;
import org.orekit.Utils;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.FrameAlignedProvider;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.frames.LazyLoadedFrames;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.LazyLoadedTimeScales;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;

/**
 * Test for {@link ExceptionalDataContext}.
 *
 * @author Evan Ward
 */
public class ExceptionalDataContextTest {

    /** Check the methods throw exceptions. */
    @Test
    public void testThrows() {
        // setup
        ExceptionalDataContext context = new ExceptionalDataContext();

        // verify
        try {
            context.getCelestialBodies();
            Assertions.fail("Expected Exception");
        } catch (OrekitException e) {
            Assertions.assertEquals(e.getSpecifier(), OrekitMessages.EXCEPTIONAL_DATA_CONTEXT);
        }
        try {
            context.getFrames();
            Assertions.fail("Expected Exception");
        } catch (OrekitException e) {
            Assertions.assertEquals(e.getSpecifier(), OrekitMessages.EXCEPTIONAL_DATA_CONTEXT);
        }
        try {
            context.getGeoMagneticFields();
            Assertions.fail("Expected Exception");
        } catch (OrekitException e) {
            Assertions.assertEquals(e.getSpecifier(), OrekitMessages.EXCEPTIONAL_DATA_CONTEXT);
        }
        try {
            context.getGravityFields();
            Assertions.fail("Expected Exception");
        } catch (OrekitException e) {
            Assertions.assertEquals(e.getSpecifier(), OrekitMessages.EXCEPTIONAL_DATA_CONTEXT);
        }
        try {
            context.getTimeScales();
            Assertions.fail("Expected Exception");
        } catch (OrekitException e) {
            Assertions.assertEquals(e.getSpecifier(), OrekitMessages.EXCEPTIONAL_DATA_CONTEXT);
        }
    }

    /** Check it can be set as the default data context with the right precautions. */
    @Test
    public void testDefault() {
        // setup
        Utils.setDataRoot("regular-data");
        // hack to initialize static fields that need the default data context
        hack();
        LazyLoadedDataContext context = DataContext.getDefault();
        DataContext.setDefault(new ExceptionalDataContext());

        // verify by running some code
        LazyLoadedTimeScales timeScales = context.getTimeScales();
        LazyLoadedFrames frames = context.getFrames();
        Frame eci = frames.getEME2000();
        Frame ecef = frames.getITRF(IERSConventions.IERS_2010, true);
        AbsoluteDate date = new AbsoluteDate(2019, 12, 20, timeScales.getUTC());
        double a = 6378e3 + 500e3;
        Orbit orbit = new KeplerianOrbit(
                a, 0, 0, 0, 0, 0,
                PositionAngleType.TRUE, eci, date, Constants.EIGEN5C_EARTH_MU);
        AttitudeProvider attitude = new FrameAlignedProvider(eci);
        Propagator propagator = new KeplerianPropagator(orbit, attitude);
        SpacecraftState state = propagator.propagate(date.shiftedBy(86400));
        MatcherAssert.assertThat(
                state.getPosition(ecef).getNorm(),
                OrekitMatchers.relativelyCloseTo(a, 10));

        // verify using default data context throws an exception
        try {
            new NumericalPropagator(new ClassicalRungeKuttaIntegrator(60.0));
            Assertions.fail("Expected Exception");
        } catch (OrekitException e) {
            Assertions.assertEquals(e.getSpecifier(), OrekitMessages.EXCEPTIONAL_DATA_CONTEXT);
        }

    }

    /**
     * Force initialization of classes that have static fields that use the default data
     * context. See JLS 12.4.1 "When Initialization Occurs".
     */
    private void hack() {
        Object o = AbsoluteDate.ARBITRARY_EPOCH;
        Assertions.assertNotNull(o);
    }

}
