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
package org.orekit.forces.maneuvers;


import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.RotationConvention;
import org.apache.commons.math3.geometry.euclidean.threed.RotationOrder;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.util.FastMath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.attitudes.InertialProvider;
import org.orekit.attitudes.LofOffset;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.errors.OrekitException;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.LOFType;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.propagation.events.DateDetector;
import org.orekit.propagation.events.NodeDetector;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.TimeStampedPVCoordinates;

public class ImpulseManeuverTest {
    @Test
    public void testInclinationManeuver() throws OrekitException {
        final Orbit initialOrbit =
            new KeplerianOrbit(24532000.0, 0.72, 0.3, FastMath.PI, 0.4, 2.0,
                               PositionAngle.MEAN, FramesFactory.getEME2000(),
                               new AbsoluteDate(new DateComponents(2008, 06, 23),
                                                new TimeComponents(14, 18, 37),
                                                TimeScalesFactory.getUTC()),
                               3.986004415e14);
        final double a  = initialOrbit.getA();
        final double e  = initialOrbit.getE();
        final double i  = initialOrbit.getI();
        final double mu = initialOrbit.getMu();
        final double vApo = FastMath.sqrt(mu * (1 - e) / (a * (1 + e)));
        double dv = 0.99 * FastMath.tan(i) * vApo;
        KeplerianPropagator propagator = new KeplerianPropagator(initialOrbit,
                                                                 new LofOffset(initialOrbit.getFrame(), LOFType.VVLH));
        propagator.addEventDetector(new ImpulseManeuver<NodeDetector>(new NodeDetector(initialOrbit, FramesFactory.getEME2000()),
                                                                      new Vector3D(dv, Vector3D.PLUS_J), 400.0));
        SpacecraftState propagated = propagator.propagate(initialOrbit.getDate().shiftedBy(8000));
        Assert.assertEquals(0.0028257, propagated.getI(), 1.0e-6);
    }

    @Test
    public void testInertialManeuver() throws OrekitException {
        final double mu = CelestialBodyFactory.getEarth().getGM();

        final double initialX = 7100e3;
        final double initialY = 0.0;
        final double initialZ = 1300e3;
        final double initialVx = 0;
        final double initialVy = 8000;
        final double initialVz = 1000;

        final Vector3D position = new Vector3D(initialX, initialY, initialZ);
        final Vector3D velocity = new Vector3D(initialVx, initialVy, initialVz);
        final AbsoluteDate epoch = new AbsoluteDate(2010, 1, 1, 0, 0, 0, TimeScalesFactory.getUTC());
        final TimeStampedPVCoordinates state = new TimeStampedPVCoordinates(epoch, position, velocity, Vector3D.ZERO);
        final Orbit initialOrbit = new CartesianOrbit(state, FramesFactory.getEME2000(), mu);

        final double totalPropagationTime = 0.00001;
        final double driftTimeInSec = totalPropagationTime / 2.0;
        final double deltaX = 0.01;
        final double deltaY = 0.02;
        final double deltaZ = 0.03;
        final double isp = 300;

        final Vector3D deltaV = new Vector3D(deltaX, deltaY, deltaZ);

        KeplerianPropagator propagator = new KeplerianPropagator(initialOrbit, new LofOffset(initialOrbit.getFrame(),LOFType.VNC));
        DateDetector dateDetector = new DateDetector(epoch.shiftedBy(driftTimeInSec));
        InertialProvider attitudeOverride = new InertialProvider(new Rotation(RotationOrder.XYX,
                                                                              RotationConvention.VECTOR_OPERATOR,
                                                                              0, 0, 0));
        ImpulseManeuver<DateDetector> burnAtEpoch = new ImpulseManeuver<DateDetector>(dateDetector, attitudeOverride, deltaV, isp).withThreshold(driftTimeInSec/4);
        propagator.addEventDetector(burnAtEpoch);

        SpacecraftState finalState = propagator.propagate(epoch.shiftedBy(totalPropagationTime));

        final double finalVxExpected = initialVx + deltaX;
        final double finalVyExpected = initialVy + deltaY;
        final double finalVzExpected = initialVz + deltaZ;
        final double maneuverTolerance = 1e-4;

        final Vector3D finalVelocity = finalState.getPVCoordinates().getVelocity();
        Assert.assertEquals(finalVxExpected, finalVelocity.getX(), maneuverTolerance);
        Assert.assertEquals(finalVyExpected, finalVelocity.getY(), maneuverTolerance);
        Assert.assertEquals(finalVzExpected, finalVelocity.getZ(), maneuverTolerance);                                               

    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

}
