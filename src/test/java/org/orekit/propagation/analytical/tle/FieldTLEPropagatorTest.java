/* Copyright 2002-2020 CS GROUP
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
package org.orekit.propagation.analytical.tle;


import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldLine;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Decimal64Field;
import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.attitudes.BodyCenterPointing;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.frames.FieldTransform;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.propagation.FieldBoundedPropagator;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.sampling.FieldOrekitFixedStepHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;


public class FieldTLEPropagatorTest {

    private double period;

    
    @Test
    public void testSlaveMode() {
        doTestSlaveMode(Decimal64Field.getInstance());
    }
    
    @Test
    public void testEphemerisMode() {
        doTestEphemerisMode(Decimal64Field.getInstance());
    }
    
    @Test
    public void testBodyCenterInPointingDirection() {
        doTestBodyCenterInPointingDirection(Decimal64Field.getInstance());
    }
    
    @Test
    public void testNonRegression() {
        doTestNonRegression(Decimal64Field.getInstance());
    }

    public <T extends RealFieldElement<T>> void doTestSlaveMode(Field<T> field) {
        
        // setup a TLE for a GPS satellite
        String line1 = "1 37753U 11036A   12090.13205652 -.00000006  00000-0  00000+0 0  2272";
        String line2 = "2 37753  55.0032 176.5796 0004733  13.2285 346.8266  2.00565440  5153";
        FieldTLE<T> tle = new FieldTLE<>(field, line1, line2);
        
        FieldTLEPropagator<T> propagator = FieldTLEPropagator.selectExtrapolator(tle);
        FieldAbsoluteDate<T> initDate = tle.getDate();
        FieldSpacecraftState<T> initialState = propagator.getInitialState();

        // Simulate a full period of a GPS satellite
        // -----------------------------------------
        FieldSpacecraftState<T> finalState = propagator.propagate(initDate.shiftedBy(period));

        // Check results
        Assert.assertEquals(initialState.getA().getReal(), finalState.getA().getReal(), 1e-1);
        Assert.assertEquals(initialState.getEquinoctialEx().getReal(), finalState.getEquinoctialEx().getReal(), 1e-1);
        Assert.assertEquals(initialState.getEquinoctialEy().getReal(), finalState.getEquinoctialEy().getReal(), 1e-1);
        Assert.assertEquals(initialState.getHx().getReal(), finalState.getHx().getReal(), 1e-3);
        Assert.assertEquals(initialState.getHy().getReal(), finalState.getHy().getReal(), 1e-3);
        Assert.assertEquals(initialState.getLM().getReal(), finalState.getLM().getReal(), 1e-3);

    }

    public <T extends RealFieldElement<T>> void doTestEphemerisMode(Field<T> field) {
        
        // setup a TLE for a GPS satellite
        String line1 = "1 37753U 11036A   12090.13205652 -.00000006  00000-0  00000+0 0  2272";
        String line2 = "2 37753  55.0032 176.5796 0004733  13.2285 346.8266  2.00565440  5153";
        FieldTLE<T> tle = new FieldTLE<>(field, line1, line2);
        
        FieldTLEPropagator<T> propagator = FieldTLEPropagator.selectExtrapolator(tle);
        propagator.setEphemerisMode();

        FieldAbsoluteDate<T> initDate = tle.getDate();
        FieldSpacecraftState<T> initialState = propagator.getInitialState();

        // Simulate a full period of a GPS satellite
        // -----------------------------------------
        FieldAbsoluteDate<T> endDate = initDate.shiftedBy(period);
        propagator.propagate(endDate);

        // get the ephemeris
        FieldBoundedPropagator<T> boundedProp = propagator.getGeneratedEphemeris();

        // get the initial state from the ephemeris and check if it is the same as
        // the initial state from the TLE
        FieldSpacecraftState<T> boundedState = boundedProp.propagate(initDate);

        // Check results
        Assert.assertEquals(initialState.getA().getReal(), boundedState.getA().getReal(), 0.);
        Assert.assertEquals(initialState.getEquinoctialEx().getReal(), boundedState.getEquinoctialEx().getReal(), 0.);
        Assert.assertEquals(initialState.getEquinoctialEy().getReal(), boundedState.getEquinoctialEy().getReal(), 0.);
        Assert.assertEquals(initialState.getHx().getReal(), boundedState.getHx().getReal(), 0.);
        Assert.assertEquals(initialState.getHy().getReal(), boundedState.getHy().getReal(), 0.);
        Assert.assertEquals(initialState.getLM().getReal(), boundedState.getLM().getReal(), 1e-14);

        FieldSpacecraftState<T> finalState = boundedProp.propagate(endDate);

        // Check results
        Assert.assertEquals(initialState.getA().getReal(), finalState.getA().getReal(), 1e-1);
        Assert.assertEquals(initialState.getEquinoctialEx().getReal(), finalState.getEquinoctialEx().getReal(), 1e-1);
        Assert.assertEquals(initialState.getEquinoctialEy().getReal(), finalState.getEquinoctialEy().getReal(), 1e-1);
        Assert.assertEquals(initialState.getHx().getReal(), finalState.getHx().getReal(), 1e-3);
        Assert.assertEquals(initialState.getHy().getReal(), finalState.getHy().getReal(), 1e-3);
        Assert.assertEquals(initialState.getLM().getReal(), finalState.getLM().getReal(), 1e-3);

    }

    /** Test if body center belongs to the direction pointed by the satellite
     */
    public <T extends RealFieldElement<T>> void doTestBodyCenterInPointingDirection(Field<T> field) {

        // setup a TLE for a GPS satellite
        String line1 = "1 37753U 11036A   12090.13205652 -.00000006  00000-0  00000+0 0  2272";
        String line2 = "2 37753  55.0032 176.5796 0004733  13.2285 346.8266  2.00565440  5153";       
        FieldTLE<T> tle = new FieldTLE<>(field, line1, line2);
        
        // setup a 0 T element.
        T T_zero = field.getZero();
        
        final Frame itrf = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                            Constants.WGS84_EARTH_FLATTENING,
                                                            itrf);
        FieldDistanceChecker<T> checker = new FieldDistanceChecker<T>(itrf);

        // with Earth pointing attitude, distance should be small
        FieldTLEPropagator<T> propagator =
                FieldTLEPropagator.selectExtrapolator(tle,
                                                 new BodyCenterPointing(FramesFactory.getTEME(), earth),
                                                 T_zero.add(Propagator.DEFAULT_MASS));
        propagator.setMasterMode(T_zero.add(900.0), checker);
        propagator.propagate(tle.getDate().shiftedBy(period));
        Assert.assertEquals(0.0, checker.getMaxDistance(), 2.0e-7);

        // with default attitude mode, distance should be large
        propagator = FieldTLEPropagator.selectExtrapolator(tle);
        propagator.setMasterMode(T_zero.add(900.0), checker);
        propagator.propagate(tle.getDate().shiftedBy(period));
        Assert.assertEquals(1.5219e7, checker.getMinDistance(), 1000.0);
        Assert.assertEquals(2.6572e7, checker.getMaxDistance(), 1000.0);

    }

    private static class FieldDistanceChecker<T extends RealFieldElement<T>> implements FieldOrekitFixedStepHandler<T> {

        private final Frame itrf;
        private double minDistance;
        private double maxDistance;

        public FieldDistanceChecker(Frame itrf) {
            this.itrf = itrf;
        }

        public double getMinDistance() {
            return minDistance;
        }

        public double getMaxDistance() {
            return maxDistance;
        }

        public void init(FieldSpacecraftState<T> s0, FieldAbsoluteDate<T> t, T step) {
            minDistance = Double.POSITIVE_INFINITY;
            maxDistance = Double.NEGATIVE_INFINITY;
        }

        public void handleStep(FieldSpacecraftState<T> currentState, boolean isLast)
            {
            // Get satellite attitude rotation, i.e rotation from inertial frame to satellite frame
            FieldRotation<T> rotSat = currentState.getAttitude().getRotation();

            // Transform Z axis from satellite frame to inertial frame
            FieldVector3D<T> zSat = rotSat.applyInverseTo(Vector3D.PLUS_K);

            // Transform Z axis from inertial frame to ITRF
            FieldTransform<T> transform = currentState.getFrame().getTransformTo(itrf, currentState.getDate());
            FieldVector3D<T> zSatITRF = transform.transformVector(zSat);

            // Transform satellite position/velocity from inertial frame to ITRF
            FieldPVCoordinates<T> pvSatITRF = transform.transformPVCoordinates(currentState.getPVCoordinates());

            // Line containing satellite point and following pointing direction
            FieldLine<T> pointingLine = new FieldLine<T>(pvSatITRF.getPosition(),
                                         pvSatITRF.getPosition().add(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                                     zSatITRF),
                                         1.0e-10);

            double distance = pointingLine.distance(Vector3D.ZERO).getReal();
            minDistance = FastMath.min(minDistance, distance);
            maxDistance = FastMath.max(maxDistance, distance);
        }

    }

    public <T extends RealFieldElement<T>> void doTestNonRegression(Field<T> field) {
        
        // propagation time.
        final double propagtime = 10 * 60;
        
        // setup a TLE for a GPS satellite
        String line1 = "1 37753U 11036A   12090.13205652 -.00000006  00000-0  00000+0 0  2272";
        String line2 = "2 37753  55.0032 176.5796 0004733  13.2285 346.8266  2.00565440  5153";
        
        // build FieldTLE and TLE instances for GPS
        FieldTLE<T> fieldtleGPS = new FieldTLE<>(field, line1, line2);
        TLE tleGPS = new TLE(line1, line2);
        
        // setup a TLE for ISS
        String line3 = "1 25544U 98067A   20162.14487814  .00001100  00000-0  27734-4 0  9997";
        String line4 = "2 25544  51.6445  23.3222 0002345  38.1770 106.6280 15.49436440230920";
        
        // build FieldTLE and TLE instances for ISS
        FieldTLE<T> fieldtleISS = new FieldTLE<>(field, line3, line4);
        TLE tleISS = new TLE(line3, line4);
        
        // propagate Field GPS orbit
        FieldTLEPropagator<T> fieldpropagator = FieldTLEPropagator.selectExtrapolator(fieldtleGPS);
        FieldAbsoluteDate<T> fieldinitDate = fieldtleGPS.getDate();
        FieldAbsoluteDate<T> fieldendDate = fieldinitDate.shiftedBy(propagtime);        
        FieldPVCoordinates<T> fieldfinalGPS = fieldpropagator.getPVCoordinates(fieldendDate);
        
        // propagate GPS orbit
        TLEPropagator propagator = TLEPropagator.selectExtrapolator(tleGPS);
        AbsoluteDate initDate = tleGPS.getDate();
        AbsoluteDate endDate = initDate.shiftedBy(propagtime);        
        PVCoordinates finalGPS = propagator.getPVCoordinates(endDate);
        
        // propagate Field GPS orbit
        fieldpropagator = FieldTLEPropagator.selectExtrapolator(fieldtleISS);
        fieldinitDate = fieldtleISS.getDate();
        fieldendDate = fieldinitDate.shiftedBy(propagtime);        
        FieldPVCoordinates<T> fieldfinalISS = fieldpropagator.getPVCoordinates(fieldendDate);
        
        // propagate GPS orbit
        propagator = TLEPropagator.selectExtrapolator(tleISS);
        initDate = tleISS.getDate();
        endDate = initDate.shiftedBy(propagtime);        
        PVCoordinates finalISS = propagator.getPVCoordinates(endDate);
        
        // calculate GPS position and velocity differences between Field and Double
        double normDifPosGPS = fieldfinalGPS.getPosition().subtract(finalGPS.getPosition()).getNorm().getReal();
        double normDifVelGPS = fieldfinalGPS.getVelocity().subtract(finalGPS.getVelocity()).getNorm().getReal();
        
        // calculate ISS position and velocity differences between Field and Double
        double normDifPosISS = fieldfinalISS.getPosition().subtract(finalISS.getPosition()).getNorm().getReal();
        double normDifVelISS = fieldfinalISS.getVelocity().subtract(finalISS.getVelocity()).getNorm().getReal();
        
        // check
        Assert.assertEquals(0, normDifPosGPS, 2e-3);
        Assert.assertEquals(0, normDifVelGPS, 1e-5);
        
        Assert.assertEquals(0, normDifPosISS, 2e-3);
        Assert.assertEquals(0, normDifVelISS, 1e-5);
        
    }
    
    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
        
     // the period of the GPS satellite
        period = 717.97 * 60.0;
    }
    
}
