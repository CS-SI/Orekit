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
package org.orekit.propagation.numerical.cr3bp;

import java.util.ArrayList;
import java.util.List;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.ODEIntegrator;
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
import org.orekit.orbits.CR3BPDifferentialCorrection;
import org.orekit.orbits.HaloOrbit;
import org.orekit.orbits.LibrationOrbitFamily;
import org.orekit.orbits.LibrationOrbitType;
import org.orekit.orbits.RichardsonExpansion;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.AbsolutePVCoordinates;
import org.orekit.utils.LagrangianPoints;
import org.orekit.utils.PVCoordinates;

public class CR3BPMultipleShooterTest {

    @Test
    public void testCannotSetEpochFreedom() {
        Assertions.assertThrows(OrekitException.class, () -> {
            final CR3BPSystem syst = CR3BPFactory.getEarthMoonCR3BP();
            final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
            final HaloOrbit h1 = new HaloOrbit(new RichardsonExpansion(syst, LagrangianPoints.L1), 8E6, LibrationOrbitFamily.NORTHERN);
            final int narcs = 1;
            final List<STMEquations> cr3bpAdditionalEquations = new ArrayList<>(narcs);
            cr3bpAdditionalEquations.add(new STMEquations(syst));
            final PVCoordinates firstGuess1 = h1.getInitialPV();
            List<SpacecraftState> firstGuessList = new ArrayList<>(narcs + 1);
            firstGuessList.add(new SpacecraftState(new AbsolutePVCoordinates(syst.getRotatingFrame(),
                                                                             date,
                                                                             firstGuess1)));

            new CR3BPMultipleShooter(firstGuessList, new ArrayList<NumericalPropagator>(), cr3bpAdditionalEquations, 1E-8, 20).setEpochFreedom(1, false);
        });
    }

    @Test
    public void testCannotSetScaleLength() {
        Assertions.assertThrows(OrekitException.class, () -> {
            final CR3BPSystem syst = CR3BPFactory.getEarthMoonCR3BP();
            final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
            final HaloOrbit h1 = new HaloOrbit(new RichardsonExpansion(syst, LagrangianPoints.L1), 8E6, LibrationOrbitFamily.NORTHERN);
            final int narcs = 1;
            final List<STMEquations> cr3bpAdditionalEquations = new ArrayList<>(narcs);
            cr3bpAdditionalEquations.add(new STMEquations(syst));
            final PVCoordinates firstGuess1 = h1.getInitialPV();
            List<SpacecraftState> firstGuessList = new ArrayList<>(narcs + 1);
            firstGuessList.add(new SpacecraftState(new AbsolutePVCoordinates(syst.getRotatingFrame(),
                                                                             date,
                                                                             firstGuess1)));

            new CR3BPMultipleShooter(firstGuessList, new ArrayList<NumericalPropagator>(), cr3bpAdditionalEquations, 1E-8, 20).setScaleLength(1);
        });
    }

    @Test
    public void testCannotSetScaleTime() {
        Assertions.assertThrows(OrekitException.class, () -> {
            final CR3BPSystem syst = CR3BPFactory.getEarthMoonCR3BP();
            final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
            final HaloOrbit h1 = new HaloOrbit(new RichardsonExpansion(syst, LagrangianPoints.L1), 8E6, LibrationOrbitFamily.NORTHERN);
            final int narcs = 1;
            final List<STMEquations> cr3bpAdditionalEquations = new ArrayList<>(narcs);
            cr3bpAdditionalEquations.add(new STMEquations(syst));
            final PVCoordinates firstGuess1 = h1.getInitialPV();
            List<SpacecraftState> firstGuessList = new ArrayList<>(narcs + 1);
            firstGuessList.add(new SpacecraftState(new AbsolutePVCoordinates(syst.getRotatingFrame(),
                                                                             date,
                                                                             firstGuess1)));

            new CR3BPMultipleShooter(firstGuessList, new ArrayList<NumericalPropagator>(), cr3bpAdditionalEquations, 1E-8, 20).setScaleTime(1);
        });
    }

    @Test
    public void testHaloOrbit() {

        final CR3BPSystem syst = CR3BPFactory.getEarthMoonCR3BP();
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final HaloOrbit h1 = new HaloOrbit(new RichardsonExpansion(syst, LagrangianPoints.L1), 8E6, LibrationOrbitFamily.NORTHERN);

        // Adaptive stepsize boundaries
        final double minStep = 1E-12;
        final double maxstep = 0.001;

        // Integrator tolerances
        final double positionTolerance = 1E-5;
        final double velocityTolerance = 1E-5;
        final double massTolerance = 1.0e-6;
        final double[] vecAbsoluteTolerances = {positionTolerance, positionTolerance, positionTolerance, velocityTolerance, velocityTolerance, velocityTolerance, massTolerance };
        final double[] vecRelativeTolerances =
                        new double[vecAbsoluteTolerances.length];

        // Integrator definition
        final AdaptiveStepsizeIntegrator integrator =
                        new DormandPrince853Integrator(minStep, maxstep,
                                                       vecAbsoluteTolerances,
                                                       vecRelativeTolerances);
        final int narcs = 1;
        final List<STMEquations> cr3bpAdditionalEquations = new ArrayList<>(narcs);
        cr3bpAdditionalEquations.add(new STMEquations(syst));

        // Propagator definition for CR3BP
        final List<NumericalPropagator> propagatorList = new ArrayList<>(narcs);
        final NumericalPropagator propagator = new NumericalPropagator(integrator);
        propagator.setOrbitType(null);
        propagator.setIgnoreCentralAttraction(true);
        propagator.addForceModel(new CR3BPForceModel(syst));

        // Add new set of additional equations to the propagator
        propagator.addAdditionalDerivativesProvider(cr3bpAdditionalEquations.get(0));

        propagatorList.add(propagator);

        // First guess trajectory definition

        final PVCoordinates firstGuess1 = h1.getInitialPV();
        final PVCoordinates firstGuess2 = new PVCoordinates(firstGuess1.getPosition().add(new Vector3D(0.1,0,0)), firstGuess1.getVelocity().negate());
        final double arcDuration = h1.getOrbitalPeriod()/2;


        List<SpacecraftState> firstGuessList = new ArrayList<>(narcs + 1);
        firstGuessList.add(new SpacecraftState(new AbsolutePVCoordinates(syst.getRotatingFrame(),
                                                                         date,
                                                                         firstGuess1)));
        firstGuessList.add(new SpacecraftState(new AbsolutePVCoordinates(syst.getRotatingFrame(),
                                                                         date.shiftedBy(arcDuration),
                                                                         firstGuess2)));

        // Multiple Shooting definition
        final CR3BPMultipleShooter multipleShooting = new CR3BPMultipleShooter(firstGuessList, propagatorList, cr3bpAdditionalEquations, 1E-8, 20);
        multipleShooting.setPatchPointComponentFreedom(0, 1, false);
        multipleShooting.setPatchPointComponentFreedom(0, 2, false); // Halo corrector is Z-fix
        multipleShooting.setPatchPointComponentFreedom(0, 3, false);
        multipleShooting.setPatchPointComponentFreedom(0, 5, false);
        multipleShooting.setPatchPointComponentFreedom(1, 1, false);
        multipleShooting.setPatchPointComponentFreedom(1, 3, false);
        multipleShooting.setPatchPointComponentFreedom(1, 5, false);

        // Differential correction
        h1.applyDifferentialCorrection();
        final PVCoordinates initialPVDC = h1.getInitialPV();
        final double periodDC = h1.getOrbitalPeriod();

        // Multiple shooting computation
        List<SpacecraftState> result = multipleShooting.compute();
        final AbsolutePVCoordinates initialPVMS = result.get(0).getAbsPVA();
        final double periodMS = 2 * result.get(1).getDate().durationFrom(result.get(0).getDate());

        Assertions.assertEquals(0.0, initialPVDC.getPosition().getY(), 1E-15);
        Assertions.assertEquals(0.0, initialPVDC.getVelocity().getX(), 1E-15);
        Assertions.assertEquals(0.0, initialPVDC.getVelocity().getZ(), 1E-15);

        Assertions.assertEquals(0.0, initialPVMS.getPosition().getY(), 1E-15);
        Assertions.assertEquals(0.0, initialPVMS.getVelocity().getX(), 1E-15);
        Assertions.assertEquals(0.0, initialPVMS.getVelocity().getZ(), 1E-15);

        Assertions.assertEquals(initialPVDC.getPosition().getX(), initialPVMS.getPosition().getX(), 1E-9);
        Assertions.assertEquals(initialPVDC.getPosition().getZ(), initialPVMS.getPosition().getZ(), 1E-15);
        Assertions.assertEquals(initialPVDC.getVelocity().getY(), initialPVMS.getVelocity().getY(), 1E-7);

        Assertions.assertEquals(periodDC, periodMS, 7E-9);
    }

    @Test
    public void testClosedOrbit() {

        // Earth-Moon system and L2 southern Halo
        final CR3BPSystem earthMoon = CR3BPFactory.getEarthMoonCR3BP();
        final HaloOrbit halo        = new HaloOrbit(new RichardsonExpansion(earthMoon, LagrangianPoints.L2), 30e6, LibrationOrbitFamily.SOUTHERN);
        halo.applyDifferentialCorrection();
        final double periodGuess    = halo.getOrbitalPeriod();

        // reference frame and date
        final Frame frame           = earthMoon.getRotatingFrame();
        final AbsoluteDate date     = AbsoluteDate.J2000_EPOCH;

        // propagators and additional equations
        final int nArcs = 2;
        final List<NumericalPropagator> propagators = new ArrayList<>(nArcs);
        final List<STMEquations> stmEquations       = new ArrayList<>(nArcs);
        for (int i = 0; i < nArcs; i++) {
            final ODEIntegrator integ      = new DormandPrince853Integrator(1e-16, 1e16, 1e-14, 3e-14);
            final NumericalPropagator prop = new NumericalPropagator(integ);
            prop.setOrbitType(null);
            prop.setIgnoreCentralAttraction(true);
            prop.addForceModel(new CR3BPForceModel(earthMoon));
            propagators.add(prop);
            stmEquations.add(new STMEquations(earthMoon));
            propagators.get(i).addAdditionalDerivativesProvider(stmEquations.get(i));
        }

        // initial guess
        final List<SpacecraftState> initialGuess = new ArrayList<>(nArcs + 1);
        initialGuess.add(new SpacecraftState(new AbsolutePVCoordinates(frame, date, halo.getInitialPV())));

        final ODEIntegrator integ            = new DormandPrince853Integrator(1e-16, 1e16, 1e-6, 3e-6);
        final NumericalPropagator propagator = new NumericalPropagator(integ);
        propagator.setOrbitType(null);
        propagator.setIgnoreCentralAttraction(true);
        propagator.addForceModel(new CR3BPForceModel(earthMoon));
        propagator.setInitialState(initialGuess.get(0));
        initialGuess.add(propagator.propagate(date.shiftedBy(periodGuess / 2.0)));

        propagator.setInitialState(initialGuess.get(0));
        initialGuess.add(propagator.propagate(date.shiftedBy(periodGuess)));

        // shooting
        final CR3BPMultipleShooter shooter = new CR3BPMultipleShooter(initialGuess, propagators, stmEquations, 1e-13, 20);
        shooter.setClosedOrbitConstraint(true);
        shooter.setPatchPointComponentFreedom(0, 1, false);
        shooter.setPatchPointComponentFreedom(0, 3, false);
        shooter.setPatchPointComponentFreedom(0, 5, false);

        final List<SpacecraftState> corrStates = shooter.compute();

        final PVCoordinates pv0 = corrStates.get(0).getPVCoordinates();
        final PVCoordinates pv2 = corrStates.get(nArcs).getPVCoordinates();
        Assertions.assertEquals(pv0.getPosition().getX(), pv2.getPosition().getX(), 1e-13);
        Assertions.assertEquals(pv0.getPosition().getY(), pv2.getPosition().getY(), 1e-13);
        Assertions.assertEquals(pv0.getPosition().getZ(), pv2.getPosition().getZ(), 1e-13);
        Assertions.assertEquals(pv0.getVelocity().getX(), pv2.getVelocity().getX(), 1e-13);
        Assertions.assertEquals(pv0.getVelocity().getY(), pv2.getVelocity().getY(), 1e-13);
        Assertions.assertEquals(pv0.getVelocity().getZ(), pv2.getVelocity().getZ(), 1e-13);
        Assertions.assertEquals(0.0, pv0.getPosition().getY(), 1e-16);
        Assertions.assertEquals(0.0, pv2.getVelocity().getX(), 1e-16);
        Assertions.assertEquals(0.0, pv2.getVelocity().getZ(), 1e-16);

    }

    // Non regression test
    @Test
    public void testWithConstraint() {

        // Earth-Moon system and L2 southern Halo
        final CR3BPSystem earthMoon = CR3BPFactory.getEarthMoonCR3BP();
        final HaloOrbit halo        = new HaloOrbit(new RichardsonExpansion(earthMoon, LagrangianPoints.L2), 30e6, LibrationOrbitFamily.SOUTHERN);
        halo.applyDifferentialCorrection();
        final double periodGuess    = halo.getOrbitalPeriod();

        // reference frame and date
        final Frame frame           = earthMoon.getRotatingFrame();
        final AbsoluteDate date     = AbsoluteDate.J2000_EPOCH;

        // propagators and additional equations
        final int nArcs = 2;
        final List<NumericalPropagator> propagators = new ArrayList<>(nArcs);
        final List<STMEquations> stmEquations       = new ArrayList<>(nArcs);
        for (int i = 0; i < nArcs; i++) {
            final ODEIntegrator integ      = new DormandPrince853Integrator(1e-16, 1e16, 1e-14, 3e-14);
            final NumericalPropagator prop = new NumericalPropagator(integ);
            prop.setOrbitType(null);
            prop.setIgnoreCentralAttraction(true);
            prop.addForceModel(new CR3BPForceModel(earthMoon));
            propagators.add(prop);
            stmEquations.add(new STMEquations(earthMoon));
            propagators.get(i).addAdditionalDerivativesProvider(stmEquations.get(i));
        }

        // initial guess
        final List<SpacecraftState> initialGuess = new ArrayList<>(nArcs + 1);
        initialGuess.add(new SpacecraftState(new AbsolutePVCoordinates(frame, date, halo.getInitialPV())));

        final ODEIntegrator integ            = new DormandPrince853Integrator(1e-16, 1e16, 1e-6, 3e-6);
        final NumericalPropagator propagator = new NumericalPropagator(integ);
        propagator.setOrbitType(null);
        propagator.setIgnoreCentralAttraction(true);
        propagator.addForceModel(new CR3BPForceModel(earthMoon));
        propagator.setInitialState(initialGuess.get(0));
        initialGuess.add(propagator.propagate(date.shiftedBy(periodGuess / 2.0)));

        propagator.setInitialState(initialGuess.get(0));
        initialGuess.add(propagator.propagate(date.shiftedBy(periodGuess)));

        // shooting
        final CR3BPMultipleShooter shooter = new CR3BPMultipleShooter(initialGuess, propagators, stmEquations, 1e-13, 20);
        shooter.setClosedOrbitConstraint(true);
        shooter.setPatchPointComponentFreedom(0, 1, false);
        shooter.setPatchPointComponentFreedom(0, 3, false);
        shooter.setPatchPointComponentFreedom(0, 5, false);
        shooter.addConstraint(1, 1, 1.0);

        final List<SpacecraftState> corrStates = shooter.compute();

        final PVCoordinates pv0 = corrStates.get(0).getPVCoordinates();
        final PVCoordinates pv2 = corrStates.get(nArcs).getPVCoordinates();
        Assertions.assertEquals(pv0.getPosition().getX(), pv2.getPosition().getX(), 1e-6);
        Assertions.assertEquals(pv0.getPosition().getY(), pv2.getPosition().getY(), 1e-6);
        Assertions.assertEquals(pv0.getPosition().getZ(), pv2.getPosition().getZ(), 1e-6);
        Assertions.assertEquals(pv0.getVelocity().getX(), pv2.getVelocity().getX(), 1e-6);
        Assertions.assertEquals(pv0.getVelocity().getY(), pv2.getVelocity().getY(), 1e-6);
        Assertions.assertEquals(pv0.getVelocity().getZ(), pv2.getVelocity().getZ(), 1e-6);

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
    public void testDifferentialCorrectionError() {
        Assertions.assertThrows(OrekitException.class, () -> {
            CR3BPSystem syst = CR3BPFactory.getEarthMoonCR3BP();

            final double orbitalPeriod = 1;

            final PVCoordinates firstGuess = new PVCoordinates(new Vector3D(0.0, 1.0, 2.0), new Vector3D(3.0, 4.0, 5.0));

            final PVCoordinates initialConditions =
                    new CR3BPDifferentialCorrection(firstGuess, syst, orbitalPeriod).compute(LibrationOrbitType.HALO);
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
            HaloOrbit h = new HaloOrbit(new RichardsonExpansion(syst, LagrangianPoints.L1), 8E6, LibrationOrbitFamily.SOUTHERN);

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
