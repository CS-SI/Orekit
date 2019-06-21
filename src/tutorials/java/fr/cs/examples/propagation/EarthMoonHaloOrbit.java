
/*
 * Copyright 2002-2019 CS Systèmes d'Information Licensed to CS Systèmes
 * d'Information (CS) under one or more contributor license agreements. See the
 * NOTICE file distributed with this work for additional information regarding
 * copyright ownership. CS licenses this file to You under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

import java.io.File;
import java.util.Locale;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.nonstiff.AdaptiveStepsizeIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.orekit.bodies.CR3BPFactory;
import org.orekit.bodies.CR3BPSystem;
import org.orekit.data.DataProvidersManager;
import org.orekit.data.DirectoryCrawler;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.orbits.CR3BPDifferentialCorrection;
import org.orekit.orbits.HaloOrbit;
import org.orekit.orbits.HaloOrbitType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.propagation.numerical.cr3bp.CR3BPForceModel;
import org.orekit.propagation.sampling.OrekitFixedStepHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.AbsolutePVCoordinates;
import org.orekit.utils.LagrangianPoints;
import org.orekit.utils.PVCoordinates;

public class EarthMoonHaloOrbit {

    // Distance between the two primaries in the circular restricted system
    // (True semi-major axis of the secondary), meters
    public static double dDim;

    // Orbital Period in the circular restricted system, s
    public static double tDim;

    // Checkpoints
    public static double outputStep;

    // Simple counter
    public static double compteur = 0;

    public static void main(String[] args) {

        // configure Orekit data provider
        File home = new File(System.getProperty("user.home"));
        File orekitData = new File(home, "orekit-data");
        if (!orekitData.exists()) {
            System.err.format(Locale.US, "Failed to find %s folder%n",
                              orekitData.getAbsolutePath());
            System.err
                .format(Locale.US,
                        "You need to download %s from the %s page and unzip it in %s for this tutorial to work%n",
                        "orekit-data.zip",
                        "https://www.orekit.org/forge/projects/orekit/files",
                        home.getAbsolutePath());
            System.exit(1);
        }
        DataProvidersManager manager = DataProvidersManager.getInstance();
        manager.addProvider(new DirectoryCrawler(orekitData));

        // Get the Earth-Moon Circular Restricted system
        final CR3BPSystem syst = CR3BPFactory.getEarthMoonCR3BP();

        // Define a Northern Halo orbit around Earth-Moon L1 with a Z-amplitude
        // of 8 000 km
        HaloOrbit h =
            new HaloOrbit(syst, LagrangianPoints.L1, 8E6,
                          HaloOrbitType.NORTHERN);

        // Return a first guess PVCoordinates on this Halo Orbit using
        // Third-Order Richardson Expansion
        PVCoordinates firstguess = h.getFirstGuess();

        // Get the CR3BP Rotating Frame centered on Earth-Moon Barycenter
        final Frame Frame = syst.getRotatingFrame();

        // Time settings
        final AbsoluteDate initialDate =
            new AbsoluteDate(1996, 06, 25, 0, 0, 00.000,
                             TimeScalesFactory.getUTC());

        // Get the characteristic distance of the system, distance between m1
        // and m2
        dDim = syst.getDdim();

        // Get the characteristic time of the system, orbital period of m2
        tDim = syst.getTdim();

        final double orbitalPeriod = h.getOrbitalPeriod();
        System.out.println("Orbital Period of the expected Halo orbit " + orbitalPeriod * tDim / 2 / 3.14 / 86400 + " days");

        double integrationTime = orbitalPeriod;
        outputStep = 0.001;

        // Integration parameters
        // These parameters are used for the Dormand-Prince integrator, a
        // variable step integrator,
        // these limits prevent the integrator to spend too much time when the
        // equations are too stiff,
        // as well as the reverse situation.
        final double minStep = 1E-10;
        final double maxstep = 1E-3;

        // tolerances for integrators
        // Used by the integrator to estimate its variable integration step
        final double positionTolerance = 1.0E-6;
        final double velocityTolerance = 1.0E-6;
        final double massTolerance = 1.0e-6;
        final double[] vecAbsoluteTolerances =
            {
                positionTolerance, positionTolerance, positionTolerance,
                velocityTolerance, velocityTolerance, velocityTolerance,
                massTolerance
            };
        final double[] vecRelativeTolerances =
            new double[vecAbsoluteTolerances.length];

        // Defining the numerical integrator that will be used by the propagator
        AdaptiveStepsizeIntegrator integrator =
            new DormandPrince853Integrator(minStep, maxstep,
                                           vecAbsoluteTolerances,
                                           vecRelativeTolerances);

        // This propagator will be used only for the differential correction
        // You can also choose to ignore this part and not give any propagator
        // to the differential correction. In that case, the differential
        // correction will use a predefined numerical propagator.
        NumericalPropagator propagatorDif = new NumericalPropagator(integrator);

        // Differential correction on the first guess necessary to find a point that will lead to an Halo Orbit.
        final PVCoordinates initialConditions =
            new CR3BPDifferentialCorrection(firstguess, syst, orbitalPeriod,
                                            propagatorDif).compute();

        // Define a clean propagator for the final propagation
        NumericalPropagator propagator = new NumericalPropagator(integrator);
        
        // The following steps are the same as in a standard propagation in CR3BP
        final AbsolutePVCoordinates initialAbsPV =
            new AbsolutePVCoordinates(Frame, initialDate, initialConditions);

        final SpacecraftState initialState = new SpacecraftState(initialAbsPV);

        propagator.setOrbitType(null);
        propagator.setIgnoreCentralAttraction(true);
        propagator.addForceModel(new CR3BPForceModel(syst));
        propagator.setInitialState(initialState);
        propagator.setMasterMode(outputStep, new TutorialStepHandler());

        final SpacecraftState finalState =
            propagator.propagate(initialDate.shiftedBy(integrationTime));
        
        System.out.println("\nInitial position: " + initialState.getPVCoordinates().getPosition().scalarMultiply(dDim));
        System.out.println("Final position: " + finalState.getPVCoordinates().getPosition().scalarMultiply(dDim));
    }

    private static class TutorialStepHandler
        implements
        OrekitFixedStepHandler {

        @Override
        public void init(final SpacecraftState s0, final AbsoluteDate t,
                         final double step) {
            System.out.format(Locale.US, "%s %s %s %s %n", "date",
                              "                              X",
                              "                     Y",
                              "                       Z");
        }

        public void handleStep(SpacecraftState currentState, boolean isLast) {
            try {
                final double duration = compteur * outputStep * tDim / 2 / 3.14;
                compteur++;
                
                final AbsoluteDate d =
                    currentState.getDate().shiftedBy(duration);
                final double px =
                    currentState.getPVCoordinates().getPosition()
                        .getX() * dDim;
                final double py =
                    currentState.getPVCoordinates().getPosition()
                        .getY() * dDim;
                final double pz =
                    currentState.getPVCoordinates().getPosition()
                        .getZ() * dDim;
                
                System.out.format(Locale.US, "%s  %18.12f  %18.12f  %18.12f%n",
                                  d, px, py, pz);

            } catch (OrekitException oe) {
                System.err.println(oe.getMessage());
            }
        }
    }
}
