/* Copyright 2002-2023 CS GROUP
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
package org.orekit.orbits;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.nonstiff.AdaptiveStepsizeIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.bodies.CR3BPFactory;
import org.orekit.bodies.CR3BPSystem;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.propagation.numerical.cr3bp.CR3BPForceModel;
import org.orekit.propagation.numerical.cr3bp.STMEquations;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.AbsolutePVCoordinates;
import org.orekit.utils.LagrangianPoints;
import org.orekit.utils.PVCoordinates;

public class LyapunovOrbitTest {


    @Test
    public void testLyapunovOrbit() {
        CR3BPSystem syst = CR3BPFactory.getEarthMoonCR3BP();

        final PVCoordinates firstGuess = new PVCoordinates(new Vector3D(0.0, 1.0, 2.0), new Vector3D(3.0, 4.0, 5.0));
        final LyapunovOrbit h1 = new LyapunovOrbit(new RichardsonExpansion(syst, LagrangianPoints.L1), 8E6);
        final LyapunovOrbit h2 = new LyapunovOrbit(syst, firstGuess, 2.0);
        final LyapunovOrbit h3 = new LyapunovOrbit(new RichardsonExpansion(syst, LagrangianPoints.L2), 8E6);

        final double orbitalPeriod1 = h1.getOrbitalPeriod();
        final double orbitalPeriod2 = h2.getOrbitalPeriod();
        final double orbitalPeriod3 = h3.getOrbitalPeriod();

        final PVCoordinates firstGuess1 = h1.getInitialPV();
        final PVCoordinates firstGuess2 = h2.getInitialPV();
        final PVCoordinates firstGuess3 = h3.getInitialPV();

        Assertions.assertNotEquals(0.0, orbitalPeriod1, 0.5);
        Assertions.assertNotEquals(0.0, orbitalPeriod3, 0.5);
        Assertions.assertEquals(2.0, orbitalPeriod2, 1E-15);

        Assertions.assertNotEquals(0.0, firstGuess1.getPosition().getX(), 0.6);
        Assertions.assertEquals(0.0, firstGuess1.getPosition().getY(), 1E-15);
        Assertions.assertEquals(0.0, firstGuess1.getVelocity().getX(), 1E-15);
        Assertions.assertNotEquals(0.0, firstGuess1.getVelocity().getY(), 0.01);
        Assertions.assertEquals(0.0, firstGuess1.getVelocity().getZ(), 1E-15);

        Assertions.assertNotEquals(0.0, firstGuess3.getPosition().getX(), 1);
        Assertions.assertEquals(0.0, firstGuess3.getPosition().getY(), 1E-15);
        Assertions.assertEquals(0.0, firstGuess3.getVelocity().getX(), 1E-15);
        Assertions.assertNotEquals(0.0, firstGuess3.getVelocity().getY(), 0.01);
        Assertions.assertEquals(0.0, firstGuess3.getVelocity().getZ(), 1E-15);

        Assertions.assertEquals(firstGuess.getPosition().getX(), firstGuess2.getPosition().getX(), 1E-15);
        Assertions.assertEquals(firstGuess.getPosition().getY(), firstGuess2.getPosition().getY(), 1E-15);
        Assertions.assertEquals(firstGuess.getPosition().getZ(), firstGuess2.getPosition().getZ(), 1E-15);
        Assertions.assertEquals(firstGuess.getVelocity().getX(), firstGuess2.getVelocity().getX(), 1E-15);
        Assertions.assertEquals(firstGuess.getVelocity().getY(), firstGuess2.getVelocity().getY(), 1E-15);
        Assertions.assertEquals(firstGuess.getVelocity().getZ(), firstGuess2.getVelocity().getZ(), 1E-15);
    }

    @Test
        public void testLagrangianError() {
        Assertions.assertThrows(OrekitException.class, () -> {
            CR3BPSystem syst = CR3BPFactory.getEarthMoonCR3BP();
            final HaloOrbit h = new HaloOrbit(new RichardsonExpansion(syst, LagrangianPoints.L3), 8E6, LibrationOrbitFamily.NORTHERN);
            h.getClass();
        });
    }

    @Test
    public void testManifolds() {

        // Time settings
        final AbsoluteDate initialDate =
            new AbsoluteDate(1996, 06, 25, 0, 0, 00.000,
                             TimeScalesFactory.getUTC());
        CR3BPSystem syst = CR3BPFactory.getEarthMoonCR3BP();

        final Frame Frame = syst.getRotatingFrame();

        LyapunovOrbit h = new LyapunovOrbit(new RichardsonExpansion(syst, LagrangianPoints.L1), 8E6);
        h.applyDifferentialCorrection();
        final double orbitalPeriod = h.getOrbitalPeriod();
        double integrationTime = orbitalPeriod * 0.1;
        final PVCoordinates initialConditions = h.getInitialPV();

        final AbsolutePVCoordinates initialAbsPV =
            new AbsolutePVCoordinates(Frame, initialDate, initialConditions);

        // Creating the initial spacecraftstate that will be given to the
        // propagator
        final SpacecraftState initialState = new SpacecraftState(initialAbsPV);

        // Integration parameters
        // These parameters are used for the Dormand-Prince integrator, a
        // variable step integrator,
        // these limits prevent the integrator to spend too much time when the
        // equations are too stiff,
        // as well as the reverse situation.
        final double minStep = 1E-10;
        final double maxstep = 1E-2;

        // tolerances for integrators
        // Used by the integrator to estimate its variable integration step
        final double positionTolerance = 0.0001;
        final double velocityTolerance = 0.0001;
        final double massTolerance = 1.0e-6;
        final double[] vecAbsoluteTolerances = { positionTolerance, positionTolerance, positionTolerance,
                velocityTolerance, velocityTolerance, velocityTolerance, massTolerance };
        final double[] vecRelativeTolerances = new double[vecAbsoluteTolerances.length];

        // Defining the numerical integrator that will be used by the propagator
        AdaptiveStepsizeIntegrator integrator = new DormandPrince853Integrator(minStep, maxstep, vecAbsoluteTolerances,
                vecRelativeTolerances);

        final STMEquations stm = new STMEquations(syst);
        final SpacecraftState augmentedInitialState =
                        stm.setInitialPhi(initialState);
        NumericalPropagator propagator = new NumericalPropagator(integrator);
        propagator.setOrbitType(null);
        propagator.setIgnoreCentralAttraction(true);
        propagator.addForceModel(new CR3BPForceModel(syst));
        propagator.addAdditionalDerivativesProvider(stm);
        propagator.setInitialState(augmentedInitialState);
        final SpacecraftState finalState = propagator.propagate(initialDate.shiftedBy(integrationTime));

        final PVCoordinates initialUnstableManifold = h.getManifolds(finalState, false);
        final PVCoordinates initialStableManifold = h.getManifolds(finalState, true);

        Assertions.assertNotEquals(finalState.getPosition().getX(), initialUnstableManifold.getPosition().getX(), 1E-7);
        Assertions.assertNotEquals(finalState.getPosition().getY(), initialUnstableManifold.getPosition().getY(), 1E-7);

        Assertions.assertNotEquals(finalState.getPosition().getX(), initialStableManifold.getPosition().getX(), 1E-7);
        Assertions.assertNotEquals(finalState.getPosition().getY(), initialStableManifold.getPosition().getY(), 1E-7);
    }

    @Test
    public void testDifferentialCorrectionError() {
        Assertions.assertThrows(OrekitException.class, () -> {
            CR3BPSystem syst = CR3BPFactory.getEarthMoonCR3BP();

            final double orbitalPeriod = 1;

            final PVCoordinates firstGuess = new PVCoordinates(new Vector3D(0.0, 1.0, 2.0), new Vector3D(3.0, 4.0, 5.0));

            final PVCoordinates initialConditions =
                    new CR3BPDifferentialCorrection(firstGuess, syst, orbitalPeriod).compute(LibrationOrbitType.LYAPUNOV);
            initialConditions.toString();
        });
    }

    @Test
    public void testSTMError() {
        Assertions.assertThrows(OrekitException.class, () -> {
            // Time settings
            final AbsoluteDate initialDate =
                    new AbsoluteDate(1996, 06, 25, 0, 0, 00.000,
                            TimeScalesFactory.getUTC());
            CR3BPSystem syst = CR3BPFactory.getEarthMoonCR3BP();

            final Frame Frame = syst.getRotatingFrame();

            // Define a Northern Halo orbit around Earth-Moon L1 with a Z-amplitude
            // of 8 000 km
            LyapunovOrbit h = new LyapunovOrbit(new RichardsonExpansion(syst, LagrangianPoints.L1), 8E6);

            final PVCoordinates pv = new PVCoordinates(new Vector3D(0.0, 1.0, 2.0), new Vector3D(3.0, 4.0, 5.0));

            final AbsolutePVCoordinates initialAbsPV =
                    new AbsolutePVCoordinates(Frame, initialDate, pv);

            // Creating the initial spacecraftstate that will be given to the
            // propagator
            final SpacecraftState s = new SpacecraftState(initialAbsPV);

            final PVCoordinates manifold = h.getManifolds(s, true);
            manifold.getMomentum();
        });
    }

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("cr3bp:regular-data");
    }
}
