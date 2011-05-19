/* Copyright 2002-2011 CS Communication & Systèmes
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
package org.orekit.propagation.events;

import org.apache.commons.math.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math.util.FastMath;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.errors.PropagationException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.TopocentricFrame;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.EcksteinHechlerPropagator;
import org.orekit.propagation.sampling.OrekitFixedStepHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;

public class ElevationDetectorTest {

    private double mu;
    private double ae;
    private double c20;
    private double c30;
    private double c40;
    private double c50;
    private double c60;

    @Test
    public void testAgata() throws OrekitException {

        final TimeScale utc = TimeScalesFactory.getUTC();
        final Vector3D position = new Vector3D(-6142438.668, 3492467.56, -25767.257);
        final Vector3D velocity = new Vector3D(505.848, 942.781, 7435.922);
        final AbsoluteDate date = new AbsoluteDate(2003, 9, 16, utc);
        final Orbit orbit = new EquinoctialOrbit(new PVCoordinates(position,  velocity),
                                                 FramesFactory.getEME2000(), date, mu);

        Propagator propagator =
            new EcksteinHechlerPropagator(orbit, ae, mu, c20, c30, c40, c50, c60);

        // Earth and frame  
        double ae =  6378137.0; // equatorial radius in meter
        double f  =  1.0 / 298.257223563; // flattening
        Frame ITRF2005 = FramesFactory.getITRF2005(); // terrestrial frame at an arbitrary date
        BodyShape earth = new OneAxisEllipsoid(ae, f, ITRF2005);
        GeodeticPoint point = new GeodeticPoint(FastMath.toRadians(48.833),
                                                FastMath.toRadians(2.333),
                                                0.0);
        CheckingDetector detector =
            new CheckingDetector(FastMath.toRadians(5.0), new TopocentricFrame(earth, point, "Gstation"));

        AbsoluteDate startDate = new AbsoluteDate(2003, 9, 15, 12, 0, 0, utc);
        propagator.resetInitialState(propagator.propagate(startDate));
        propagator.addEventDetector(detector);
        propagator.setMasterMode(10.0, detector);
        propagator.propagate(startDate.shiftedBy(Constants.JULIAN_DAY));

    }

    private static class CheckingDetector extends ElevationDetector implements OrekitFixedStepHandler {

        private static final long serialVersionUID = -599447199940831866L;
        private boolean visible;

        public CheckingDetector(final double elevation, final TopocentricFrame frame) {
            super(elevation, frame);
            visible = false;
        }

        public int eventOccurred(SpacecraftState s, boolean increasing) throws OrekitException {
            visible = increasing;
            return CONTINUE;
        }

        public void handleStep(SpacecraftState currentState, boolean isLast)
        throws PropagationException {
            try {
                TopocentricFrame topo = getTopocentricFrame();
                BodyShape shape = topo.getParentShape();
                GeodeticPoint p =
                    shape.transform(currentState.getPVCoordinates().getPosition(),
                                    currentState.getFrame(), currentState.getDate());
                Vector3D subSat = shape.transform(new GeodeticPoint(p.getLatitude(), p.getLongitude(), 0.0));
                double range = topo.getRange(subSat, shape.getBodyFrame(), currentState.getDate());

                if (visible) {
                    Assert.assertTrue(range < 2.45e6);
                } else {
                    Assert.assertTrue(range > 2.02e6);
                }

            } catch (OrekitException e) {
                throw new PropagationException(e);
            }

        }

    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
        mu  = 3.9860047e14;
        ae  = 6.378137e6;
        c20 = -1.08263e-3;
        c30 = 2.54e-6;
        c40 = 1.62e-6;
        c50 = 2.3e-7;
        c60 = -5.5e-7;
    }

    @After
    public void tearDown() {
        mu   = Double.NaN;
        ae   = Double.NaN;
        c20  = Double.NaN;
        c30  = Double.NaN;
        c40  = Double.NaN;
        c50  = Double.NaN;
        c60  = Double.NaN;
    }

}

