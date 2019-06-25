
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
import org.hipparchus.ode.events.Action;
import org.hipparchus.ode.nonstiff.AdaptiveStepsizeIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.hipparchus.util.FastMath;
import org.orekit.bodies.CR3BPFactory;
import org.orekit.bodies.CR3BPSystem;
import org.orekit.data.DataProvidersManager;
import org.orekit.data.DirectoryCrawler;
import org.orekit.frames.Frame;
import org.orekit.orbits.HaloOrbit;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.YZPlaneCrossingDetector;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.propagation.numerical.cr3bp.CR3BPForceModel;
import org.orekit.propagation.numerical.cr3bp.STMEquations;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.AbsolutePVCoordinates;
import org.orekit.utils.PVCoordinates;

public class ManifoldTransfer {

    // Distance between the two primaries in the circular restricted system
    // (True semi-major axis of the secondary), meters
    private static double dDim;

    // Orbital Period in the circular restricted system, s
    public static double tDim;

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

        
        System.out.println("          Transfer from Earth-Moon L2 Halo Orbit to High Lunar Orbit using unstable manifolds");
        // Get the Earth-Moon Circular Restricted system
        final CR3BPSystem syst = CR3BPFactory.getEarthMoonCR3BP();

        // Get the system Mass Ratio
        final double mu = syst.getMassRatio();

        // Get the CR3BP Rotating Frame centered on Earth-Moon Barycenter
        final Frame Frame = syst.getRotatingFrame();

        // Time settings
        final AbsoluteDate initialDate =
            new AbsoluteDate(1996, 06, 25, 0, 0, 00.000,
                             TimeScalesFactory.getUTC());

        // Get the characteristic distance of the system, distance between m1
        // and m2
        dDim = syst.getDdim();

        // Get the characteristic velocity of the system
        final double vDim = syst.getVdim();

        // Orbital Period of the Halo Orbit
        final double orbitalPeriod = 3.4053705719360714;

        // Initial PVCoordinates on the Halo Orbit, it has been computed with
        // the CR3BPDifferential Corrector in the for a Southern Halo Orbit
        // around Earth-Moon L2 with Az = 8E6 meters
        final PVCoordinates initialConditions =
            new PVCoordinates(new Vector3D(1.1179828785636794, 0.0,
                                           -0.018142400819016753),
                              new Vector3D(0.0, 0.18299811433160457, 0.0));

        // Halo Orbit definition
        final HaloOrbit h =
            new HaloOrbit(syst, initialConditions, orbitalPeriod);

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
        final double positionTolerance = 0.001;
        final double velocityTolerance = 0.001;
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

        // Defining initial absolutePVCoordinates
        final AbsolutePVCoordinates initialAbsPV =
            new AbsolutePVCoordinates(Frame, initialDate, initialConditions);

        // Creating the initial spacecraftstate that will be given to the
        // propagator
        final SpacecraftState initialState = new SpacecraftState(initialAbsPV);

        // Creating equations model for the state transition matrix, it will be
        // propagated along the SpacecraftState and will be useful for further
        // manifold computation
        final STMEquations stm = new STMEquations(syst);

        // Initializing STM values
        final SpacecraftState augmentedInitialState =
            stm.setInitialPhi(initialState);

        // Creating propagator
        NumericalPropagator propagator = new NumericalPropagator(integrator);

        // Next three lines are standard in CR3BP
        propagator.setOrbitType(null);
        propagator.setIgnoreCentralAttraction(true);
        propagator.addForceModel(new CR3BPForceModel(syst));

        // Add our model for STM to be propagated
        propagator.addAdditionalEquations(stm);

        // Set initial state with SpacecraftState augmented by initial values
        // for STM
        propagator.setInitialState(augmentedInitialState);

        // Event detector settings
        final double maxcheck = 10;
        final double threshold = 1E-10;
        final double offS = 1.0 - mu;

        // Event detector definition, will be used for manifold convergence
        final EventDetector YZPlaneCrossing =
            new YZPlaneCrossingDetector(offS, maxcheck, threshold)
                .withHandler(new PlaneCrossingHandler());

        // Create another propagator for manifolds propagation
        NumericalPropagator mPropagator = new NumericalPropagator(integrator);
        mPropagator.setOrbitType(null);
        mPropagator.setIgnoreCentralAttraction(true);
        mPropagator.addForceModel(new CR3BPForceModel(syst));
        mPropagator.addEventDetector(YZPlaneCrossing);

        // Distance of the spacecraft from the Moon
        double distMoon;

        // Start Time of the transfer
        double transferTime;

        // SpacecraftState pre-Transfer
        SpacecraftState finalState;

        // SpacecraftState post-Transfer
        SpacecraftState transferedState;

        // Simple incrementation
        double k = 0;

        // This loop will lead to the closest manifold transfer to the Moon
        do {
            // the unstable manifold is different for each point on a
            // Halo Orbit, so we start from different points to find the best
            // manifold to use for our purpose
            transferTime = orbitalPeriod - 0.388 + 0.001 * k;
            
            // First propagator on the Halo Orbit until it finds the point from which it will start the transfer
            finalState =
                propagator.propagate(initialDate.shiftedBy(transferTime));
            
            // Get the unstable manifold direction as PVCoordinates from the finalState point
            final PVCoordinates initialUnstableManifoldsDirection =
                h.getManifolds(finalState, false);
            
            // Defining the new SpacecraftState
            final AbsolutePVCoordinates absInitialManifold =
                new AbsolutePVCoordinates(Frame, finalState.getDate(),
                                          initialUnstableManifoldsDirection);
            final SpacecraftState manifoldState =
                new SpacecraftState(absInitialManifold);
            
            // Initializing the manifold propagator
            mPropagator.setInitialState(manifoldState);
            
            // Get the spacecraftState post-Transfer
            transferedState =
                mPropagator.propagate(manifoldState.getDate().shiftedBy(5));
            
            // Spacecraft State position
            final double xf =
                transferedState.getPVCoordinates().getPosition().getX();
            final double yf =
                transferedState.getPVCoordinates().getPosition().getY();
            final double zf =
                transferedState.getPVCoordinates().getPosition().getZ();

            distMoon =
                FastMath.sqrt((xf - (1 - mu)) * (xf - (1 - mu)) +
                              yf * yf + zf * zf) *
                       dDim;

            k++;
        } while (distMoon > 9.5E6 && k<=20); // it will stop if k>20 or if the spacecraft is less than 9 500 km away from the Moon

        System.out
            .println("\nDistance between the spacecraft and the Moon after Transfer: " +
                     distMoon + " meters");

        final Vector3D velocity =
            transferedState.getPVCoordinates().getVelocity();
        System.out.println("\nSpacecraft velocity after transfer: " +
                           velocity.scalarMultiply(vDim) + " m/s");

        // Creating a new propagator for the Moon captured orbit
        NumericalPropagator oPropagator = new NumericalPropagator(integrator);
        oPropagator.setOrbitType(null);
        oPropagator.setIgnoreCentralAttraction(true);
        oPropagator.addForceModel(new CR3BPForceModel(syst));

        // Pre-defined insertion Manoeuver, this can be computed and optimized with other tools in orekit
        final PVCoordinates orbitPV =
            new PVCoordinates(transferedState.getPVCoordinates().getPosition(),
                              new Vector3D(-0.75, 0.0, 0.0));
        System.out
            .println("Spacecraft velocity after Lunar orbit insertion manoeuver: " +
                     orbitPV.getVelocity().scalarMultiply(vDim) + " m/s");

        final AbsolutePVCoordinates absOrbit =
            new AbsolutePVCoordinates(Frame, transferedState.getDate(),
                                      orbitPV);
        final SpacecraftState orbitState = new SpacecraftState(absOrbit);
        oPropagator.addEventDetector(YZPlaneCrossing);
        oPropagator.setInitialState(orbitState);
        final SpacecraftState halfOrbit =
            oPropagator.propagate(transferedState.getDate().shiftedBy(1));
        final SpacecraftState fullOrbit =
            oPropagator.propagate(transferedState.getDate().shiftedBy(1));

        System.out.println("\nInitial Position: " +
                           initialState.getPVCoordinates().getPosition()
                               .scalarMultiply(dDim));
        System.out.println("Position before Transfer: " +
                           finalState.getPVCoordinates().getPosition()
                               .scalarMultiply(dDim));
        System.out.println("Position after Transfer: " +
                           transferedState.getPVCoordinates().getPosition()
                               .scalarMultiply(dDim));
        System.out.println("Position after first Moon captured half orbit: " +
                           halfOrbit.getPVCoordinates().getPosition()
                               .scalarMultiply(dDim));
        System.out.println("Position after first Moon captured full orbit: " +
                           fullOrbit.getPVCoordinates().getPosition()
                               .scalarMultiply(dDim));

    }

    /**
     * Static class for event detection.
     */
    private static class PlaneCrossingHandler
        implements
        EventHandler<YZPlaneCrossingDetector> {

        public Action eventOccurred(final SpacecraftState s,
                                    final YZPlaneCrossingDetector detector,
                                    final boolean increasing) {
            return Action.STOP;
        }
    }

}
