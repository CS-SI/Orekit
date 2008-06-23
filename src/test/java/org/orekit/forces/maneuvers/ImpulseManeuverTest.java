/* Copyright 2002-2008 CS Communication & Systèmes
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
package org.orekit.forces.maneuvers;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.commons.math.geometry.Vector3D;
import org.orekit.attitudes.LofOffset;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.iers.IERSDirectoryCrawler;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.propagation.events.NodeDetector;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.ChunkedDate;
import org.orekit.time.ChunkedTime;
import org.orekit.time.UTCScale;

public class ImpulseManeuverTest extends TestCase {

    public void testInclinationManeuver() throws OrekitException {
        final Orbit initialOrbit =
            new KeplerianOrbit(24532000.0, 0.72, 0.3, Math.PI, 0.4, 2.0,
                               KeplerianOrbit.MEAN_ANOMALY,
                               Frame.getJ2000(),
                               new AbsoluteDate(new ChunkedDate(2008, 06, 23),
                                                new ChunkedTime(14, 18, 37),
                                                UTCScale.getInstance()),
                               3.986004415e14);
        final double a  = initialOrbit.getA();
        final double e  = initialOrbit.getE();
        final double i  = initialOrbit.getI();
        final double mu = initialOrbit.getMu();
        final double vApo = Math.sqrt(mu * (1 - e) / (a * (1 + e)));
        double dv = 0.99 * Math.tan(i) * vApo;
        KeplerianPropagator propagator = new KeplerianPropagator(initialOrbit, LofOffset.LOF_ALIGNED);
        propagator.addEventDetector(new ImpulseManeuver(new NodeDetector(initialOrbit, Frame.getJ2000()),
                                                        new Vector3D(dv, Vector3D.PLUS_J), 400.0));
        SpacecraftState propagated = propagator.propagate(new AbsoluteDate(initialOrbit.getDate(), 8000));
        assertEquals(0.0028257, propagated.getI(), 1.0e-6);
    }

    public void setUp() {
        System.setProperty(IERSDirectoryCrawler.IERS_ROOT_DIRECTORY, "regular-data");
    }

    public static Test suite() {
        return new TestSuite(ImpulseManeuverTest.class);
    }

}
