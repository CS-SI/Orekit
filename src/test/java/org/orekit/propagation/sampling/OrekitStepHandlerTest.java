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
package org.orekit.propagation.sampling;

import static org.junit.Assert.*;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.math3.util.FastMath;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.errors.OrekitException;
import org.orekit.errors.PropagationException;
import org.orekit.frames.FactoryManagedFrame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;

public class OrekitStepHandlerTest {

    @Test
    public void testForwardBackwardStep()
        throws OrekitException, InterruptedException, ExecutionException {
        final AbsoluteDate initialDate = new AbsoluteDate(2014, 01, 01, 00, 00,
                                                          00.000,
                                                          TimeScalesFactory
                                                              .getUTC());
        final double mu = CelestialBodyFactory.getEarth().getGM();
        FactoryManagedFrame inertialFrame = FramesFactory.getEME2000();

        final double propagationTime = 7200.0;// seconds
        final double fixedStepSize = 3600; // seconds

        final double semimajorAxis = 8000e3; // meters
        final double eccentricity = 0.001; // unitless
        final double inclination = FastMath.toRadians(15.0);
        final double argPerigee = FastMath.toRadians(10.0);
        final double raan = FastMath.toRadians(45.0);
        final double trueAnomaly = FastMath.toRadians(10.0);

        KeplerianOrbit initialOrbit = new KeplerianOrbit(semimajorAxis,
                                                         eccentricity,
                                                         inclination,
                                                         argPerigee, raan,
                                                         trueAnomaly,
                                                         PositionAngle.TRUE,
                                                         inertialFrame,
                                                         initialDate, mu);

        final Propagator kepler = new KeplerianPropagator(initialOrbit);

        kepler.setMasterMode(fixedStepSize, new OrekitFixedStepHandler() {

            @Override
            public void init(SpacecraftState s0, AbsoluteDate t)
                throws PropagationException {
            }

            @Override
            public void handleStep(SpacecraftState currentState, boolean isLast)
                throws PropagationException {
                // System.out.println("Step stopped at: " +
                // currentState.getDate());;
            }
        });

        kepler.propagate(initialDate.shiftedBy(propagationTime));

        final double stepSizeInSeconds = 120;
        final long longestWaitTimeMS = 20;
        ExecutorService service = Executors.newSingleThreadExecutor();
        for (double elapsedTime = 0; elapsedTime <= propagationTime; elapsedTime += stepSizeInSeconds) {
            final double dt = elapsedTime;
            Future<SpacecraftState> stateFuture = service
                .submit(new Callable<SpacecraftState>() {

                    public SpacecraftState call()
                        throws PropagationException {
                        return kepler.propagate(initialDate.shiftedBy(dt));
                    }
                });

            Thread.sleep(longestWaitTimeMS);
            assertTrue(stateFuture.isDone());
            SpacecraftState finalState = stateFuture.get();
            assertNotNull(finalState);
        }
    }

    @Before
    public void setUp()
        throws OrekitException {
        Utils.setDataRoot("regular-data");
    }
}
