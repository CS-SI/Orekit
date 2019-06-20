/* Copyright 2002-2019 CS Systèmes d'Information
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

package org.orekit.orbits;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.ode.events.Action;
import org.hipparchus.ode.nonstiff.AdaptiveStepsizeIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.hipparchus.util.FastMath;
import org.orekit.bodies.CR3BPSystem;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.XZPlaneCrossingDetector;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.propagation.numerical.cr3bp.CR3BPForceModel;
import org.orekit.propagation.numerical.cr3bp.STMEquations;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.AbsolutePVCoordinates;
import org.orekit.utils.PVCoordinates;


/**
 * Class implementing the differential correction method for Halo or Lyapunov
 * Orbits. It is not a simple differential correction, it uses higher order
 * terms to be more accurate and meet orbits requirements.
 * @see "Three-dimensional, periodic, Halo Orbits by Kathleen Connor Howell, Stanford University"
 * @author Vincent Mouraux
 */
public class CR3BPDifferentialCorrection {

    /** Boolean return true if the propagated trajectory crosses the plane. */
    private boolean cross;

    /** first guess PVCoordinates of the point to start differential correction. */
    private final PVCoordinates firstGuess;

    /** CR3BP System considered. */
    private final CR3BPSystem syst;

    /** orbitalPeriod Orbital Period of the required orbit. */
    private final double orbitalPeriod;

    /** Propagator. */
    private final NumericalPropagator propagator;

    /** Simple Constructor.
     * <p> Standard constructor using DormandPrince853 integrator for the differential correction </p>
     * @param firstguess first guess PVCoordinates of the point to start differential correction
     * @param syst CR3BP System considered
     * @param orbitalPeriod Orbital Period of the required orbit
     */
    public CR3BPDifferentialCorrection(final PVCoordinates firstguess,
                                       final CR3BPSystem syst, final double orbitalPeriod) {
        this.firstGuess = firstguess;
        this.syst = syst;
        this.orbitalPeriod = orbitalPeriod;

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

        // Propagator definition
        this.propagator =
            new NumericalPropagator(integrator);

    }

    /** Simple Constructor.
     * <p> Constructor to use if you need to specify the Numerical Propagator settings to be used in the differential correction </p>
     * @param firstguess first guess PVCoordinates of the point to start differential correction
     * @param syst CR3BP System considered
     * @param orbitalPeriod Orbital Period of the required orbit
     * @param propagator Numerical Propagator with integrator, step and tolerances
     */
    public CR3BPDifferentialCorrection(final PVCoordinates firstguess,
                                       final CR3BPSystem syst, final double orbitalPeriod, final NumericalPropagator propagator) {
        this.firstGuess = firstguess;
        this.syst = syst;
        this.orbitalPeriod = orbitalPeriod;
        this.propagator = propagator;

    }

    /** Return the real starting point PVCoordinates on the Halo orbit after differential correction from a first guess.
     * @return pv Position-Velocity of the starting point on the Halo Orbit
     */
    public PVCoordinates compute() {

        // number of iteration
        double iter = 0;

        // Final velocity difference in X direction
        double dvxf;

        // Final velocity difference in Z direction
        double dvzf;

        // Higher order STM Matrix creation
        final RealMatrix A = MatrixUtils.createRealMatrix(2, 2);

        // Time settings
        final AbsoluteDate initialDate =
            new AbsoluteDate(1996, 06, 25, 0, 0, 00.000,
                             TimeScalesFactory.getUTC());

        final Frame rotatingFrame = syst.getRotatingFrame();

        // Initializing PVCoordinates with first guess
        PVCoordinates pv = firstGuess;

        // Maximum integration Time to cross XZ plane equals to one full orbit.
        final double integrationTime = orbitalPeriod;

        // Event detector settings
        final double maxcheck = 10;
        final double threshold = 1E-10;

        // Event detector definition
        final EventDetector XZPlaneCrossing =
            new XZPlaneCrossingDetector(maxcheck, threshold)
                .withHandler(new PlaneCrossingHandler());

        // Additional equations set in order to compute the State Transition Matrix along the propagation
        final STMEquations stm = new STMEquations(syst);

        // CR3BP has no defined orbit type
        propagator.setOrbitType(null);

        // CR3BP has central Attraction
        propagator.setIgnoreCentralAttraction(true);

        // Add CR3BP Force Model to the propagator
        propagator.addForceModel(new CR3BPForceModel(syst));

        // Add previously set additional equations to the propagator
        propagator.addAdditionalEquations(stm);

        // Add previously set event detector to the propagator
        propagator.addEventDetector(XZPlaneCrossing);

        // Start a new differentially corrected propagation until it converges to a Halo Orbit
        do {

            // SpacecraftState initialization
            final AbsolutePVCoordinates initialAbsPV =
                new AbsolutePVCoordinates(rotatingFrame, initialDate, pv);

            final SpacecraftState initialState =
                new SpacecraftState(initialAbsPV);

            // Additional equations initialization
            final SpacecraftState augmentedInitialState =
                stm.setInitialPhi(initialState);

            // boolean changed to true by crossing XZ plane during propagation. Has to be true for the differential correction to converge
            cross = false;

            // Propagator initialization
            propagator.setInitialState(augmentedInitialState);

            // Propagate until trajectory crosses XZ Plane
            final SpacecraftState finalState =
                propagator.propagate(initialDate.shiftedBy(integrationTime));

            // Stops computation if trajectory did not cross XZ Plane after one full orbital period
            if (cross == false) {
                throw new OrekitException(OrekitMessages.TRAJECTORY_NOT_CROSSING_XZPLANE);
            }

            // Get State Transition Matrix phi
            final RealMatrix phi = stm.getStateTransitionMatrix(finalState);

            // Gap from desired X and Z axis velocity value ()
            dvxf = -finalState.getPVCoordinates().getVelocity().getX();
            dvzf = -finalState.getPVCoordinates().getVelocity().getZ();

            // Y axis velocity
            final double vy = finalState.getPVCoordinates().getVelocity().getY();

            // Spacecraft acceleration
            final Vector3D acc = finalState.getPVCoordinates().getAcceleration();
            final double accx = acc.getX();
            final double accz = acc.getZ();

            // Compute A coefficients
            final double a11 =
                phi.getEntry(3, 0) - accx * phi.getEntry(1, 0) / vy;
            final double a12 =
                phi.getEntry(3, 4) - accx * phi.getEntry(1, 4) / vy;
            final double a21 =
                phi.getEntry(5, 0) - accz * phi.getEntry(1, 0) / vy;
            final double a22 =
                phi.getEntry(5, 4) - accz * phi.getEntry(1, 4) / vy;

            A.setEntry(0, 0, a11);
            A.setEntry(0, 1, a12);
            A.setEntry(1, 0, a21);
            A.setEntry(1, 1, a22);

            // A determinant used for matrix inversion
            final double aDet =
                A.getEntry(0, 0) * A.getEntry(1, 1) -
                                A.getEntry(1, 0) * A.getEntry(0, 1);

            // Correction to apply to initial conditions
            final double deltax0 =
                (A.getEntry(1, 1) * dvxf - A.getEntry(0, 1) * dvzf) / aDet; // dx0
            final double deltavy0 =
                (-A.getEntry(1, 0) * dvxf + A.getEntry(0, 0) * dvzf) / aDet; // dvy0

            // Computation of the corrected initial PVCoordinates
            final double newx = pv.getPosition().getX() + deltax0;
            final double newvy = pv.getVelocity().getY() + deltavy0;

            pv =
                new PVCoordinates(new Vector3D(newx, pv.getPosition().getY(),
                                               pv.getPosition().getZ()),
                                  new Vector3D(pv.getVelocity().getX(), newvy,
                                               pv.getVelocity().getZ()));

            ++iter;
        } while ((FastMath.abs(dvxf) > 1E-8 || FastMath.abs(dvzf) > 1E-8) &
                 iter < 8); // Converge within 1E-8 tolerance and under 5 iterations

        return pv;
    }

    /** Static class for event detection.
     */
    private class PlaneCrossingHandler
        implements
        EventHandler<XZPlaneCrossingDetector> {

        /** {@inheritDoc} */
        public Action eventOccurred(final SpacecraftState s,
                                    final XZPlaneCrossingDetector detector,
                                    final boolean increasing) {
            cross = true;
            return Action.STOP;
        }
    }
}
