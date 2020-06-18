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
package org.orekit.propagation.numerical;

import java.util.ArrayList;
import java.util.List;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.nonstiff.AdaptiveStepsizeIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
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
import org.orekit.propagation.integration.AdditionalEquations;
import org.orekit.propagation.numerical.cr3bp.CR3BPForceModel;
import org.orekit.propagation.numerical.cr3bp.CR3BPMultipleShooter;
import org.orekit.propagation.numerical.cr3bp.STMEquations;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.AbsolutePVCoordinates;
import org.orekit.utils.LagrangianPoints;
import org.orekit.utils.PVCoordinates;

public class CR3BPMultipleShooterTest {

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
        final List<AdditionalEquations> cr3bpAdditionalEquations = new ArrayList<AdditionalEquations>(narcs) ;
        cr3bpAdditionalEquations.add(new STMEquations(syst));

        // Propagator definition for CR3BP
        final List<NumericalPropagator> propagatorList = new ArrayList<NumericalPropagator>(narcs);
        final NumericalPropagator propagator = new NumericalPropagator(integrator);
        propagator.setOrbitType(null);
        propagator.setIgnoreCentralAttraction(true);
        propagator.addForceModel(new CR3BPForceModel(syst));

        // Add new set of additional equations to the propagator
        propagator.addAdditionalEquations(cr3bpAdditionalEquations.get(0));

        propagatorList.add(propagator);

        // First guess trajectory definition

        final PVCoordinates firstGuess1 = h1.getInitialPV();
        final PVCoordinates firstGuess2 = new PVCoordinates(firstGuess1.getPosition().add(new Vector3D(0.1,0,0)), firstGuess1.getVelocity().negate());
        final double arcDuration = h1.getOrbitalPeriod()/2;


        List<SpacecraftState> firstGuessList = new ArrayList<SpacecraftState>(narcs + 1) ;;
        firstGuessList.add(new SpacecraftState(new AbsolutePVCoordinates(syst.getRotatingFrame(),
                                                                         date,
                                                                         firstGuess1)));
        firstGuessList.add(new SpacecraftState(new AbsolutePVCoordinates(syst.getRotatingFrame(),
                                                                         date.shiftedBy(arcDuration),
                                                                         firstGuess2)));

        // Multiple Shooting definition
        final CR3BPMultipleShooter multipleShooting = new CR3BPMultipleShooter(firstGuessList, propagatorList, cr3bpAdditionalEquations, arcDuration, 1E-8);
        multipleShooting.setPatchPointComponentFreedom(1, 1, false);
        multipleShooting.setPatchPointComponentFreedom(1, 2, false);
        multipleShooting.setPatchPointComponentFreedom(1, 3, false);
        multipleShooting.setPatchPointComponentFreedom(1, 5, false);
        multipleShooting.setPatchPointComponentFreedom(2, 1, false);
        multipleShooting.setPatchPointComponentFreedom(2, 3, false);
        multipleShooting.setPatchPointComponentFreedom(2, 5, false);
        multipleShooting.setEpochFreedom(1, false);
        multipleShooting.setEpochFreedom(2, false);
        multipleShooting.addConstraint(1, 1, 1.0e-5);
        
        // Differential correction
        h1.applyDifferentialCorrection();
        final PVCoordinates initialPVDC = h1.getInitialPV();
        final double periodDC = h1.getOrbitalPeriod();

        // Multiple shooting computation
        List<SpacecraftState> result = multipleShooting.compute();
        final AbsolutePVCoordinates initialPVMS = result.get(0).getAbsPVA();
        final double periodMS = 2 * result.get(1).getDate().durationFrom(result.get(0).getDate()); 

        Assert.assertEquals(0.0, initialPVDC.getPosition().getY(), 1E-15);
        Assert.assertEquals(0.0, initialPVDC.getVelocity().getX(), 1E-15);
        Assert.assertEquals(0.0, initialPVDC.getVelocity().getZ(), 1E-15);

        Assert.assertEquals(0.0, initialPVMS.getPosition().getY(), 1E-15);
        Assert.assertEquals(0.0, initialPVMS.getVelocity().getX(), 1E-15);
        Assert.assertEquals(0.0, initialPVMS.getVelocity().getZ(), 1E-15);

        Assert.assertEquals(initialPVDC.getPosition().getX(), initialPVMS.getPosition().getX(), 6.6E-4);
        Assert.assertEquals(initialPVDC.getPosition().getZ(), initialPVMS.getPosition().getZ(), 1.0E-15);
        Assert.assertEquals(initialPVDC.getVelocity().getY(), initialPVMS.getVelocity().getY(), 7.2E-3);

        Assert.assertEquals(periodDC, periodMS, 3.0E-2);
    }

    @Test(expected=OrekitException.class)
    public void testLagrangianError() {
        CR3BPSystem syst = CR3BPFactory.getEarthMoonCR3BP();
        final HaloOrbit h = new HaloOrbit(new RichardsonExpansion(syst, LagrangianPoints.L3), 8E6, LibrationOrbitFamily.NORTHERN);
        h.getClass();
    }

    @Test(expected=OrekitException.class)
    public void testDifferentialCorrectionError() {

        CR3BPSystem syst = CR3BPFactory.getEarthMoonCR3BP();

        final double orbitalPeriod = 1;

        final PVCoordinates firstGuess = new PVCoordinates(new Vector3D(0.0, 1.0, 2.0), new Vector3D(3.0, 4.0, 5.0));

        final PVCoordinates initialConditions =
                        new CR3BPDifferentialCorrection(firstGuess, syst, orbitalPeriod).compute(LibrationOrbitType.HALO);
        initialConditions.toString();
    }

    @Test(expected=OrekitException.class)
    public void testSTMError() {
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
    }

    @Before
    public void setUp() {
        Utils.setDataRoot("cr3bp:regular-data");
    }
}
