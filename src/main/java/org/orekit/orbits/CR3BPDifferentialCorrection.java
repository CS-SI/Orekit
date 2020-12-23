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
package org.orekit.orbits;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.ode.events.Action;
import org.hipparchus.ode.nonstiff.AdaptiveStepsizeIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.hipparchus.util.FastMath;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.bodies.CR3BPSystem;
import org.orekit.data.DataContext;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.HaloXZPlaneCrossingDetector;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.propagation.numerical.cr3bp.CR3BPForceModel;
import org.orekit.propagation.numerical.cr3bp.STMEquations;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.utils.AbsolutePVCoordinates;
import org.orekit.utils.PVCoordinates;


/**
 * Class implementing the differential correction method for Halo or Lyapunov
 * Orbits. It is not a simple differential correction, it uses higher order
 * terms to be more accurate and meet orbits requirements.
 * @see "Three-dimensional, periodic, Halo Orbits by Kathleen Connor Howell, Stanford University"
 * @author Vincent Mouraux
 * @since 10.2
 */
public class CR3BPDifferentialCorrection {

    /** Boolean return true if the propagated trajectory crosses the plane. */
    private boolean cross;

    /** first guess PVCoordinates of the point to start differential correction. */
    private final PVCoordinates firstGuess;

    /** CR3BP System considered. */
    private final CR3BPSystem syst;

    /** orbitalPeriodApprox Orbital Period of the firstGuess. */
    private final double orbitalPeriodApprox;

    /** orbitalPeriod Orbital Period of the required orbit. */
    private double orbitalPeriod;

    /** Propagator. */
    private final NumericalPropagator propagator;

    /** UTC time scale. */
    private final TimeScale utc;

    /** Simple Constructor.
     * <p> Standard constructor using DormandPrince853 integrator for the differential correction </p>
     * @param firstguess first guess PVCoordinates of the point to start differential correction
     * @param syst CR3BP System considered
     * @param orbitalPeriod Orbital Period of the required orbit
     */
    @DefaultDataContext
    public CR3BPDifferentialCorrection(final PVCoordinates firstguess,
                                       final CR3BPSystem syst, final double orbitalPeriod) {
        this(firstguess, syst, orbitalPeriod,
                Propagator.getDefaultLaw(DataContext.getDefault().getFrames()),
                DataContext.getDefault().getTimeScales().getUTC());
    }

    /** Simple Constructor.
     * <p> Standard constructor using DormandPrince853 integrator for the differential correction </p>
     * @param firstguess first guess PVCoordinates of the point to start differential correction
     * @param syst CR3BP System considered
     * @param orbitalPeriod Orbital Period of the required orbit
     * @param attitudeProvider the attitude law for the numerocal propagator
     * @param utc UTC time scale
     */
    public CR3BPDifferentialCorrection(final PVCoordinates firstguess,
                                       final CR3BPSystem syst,
                                       final double orbitalPeriod,
                                       final AttitudeProvider attitudeProvider,
                                       final TimeScale utc) {
        this.firstGuess = firstguess;
        this.syst = syst;
        this.orbitalPeriodApprox = orbitalPeriod;
        this.utc = utc;

        // Adaptive stepsize boundaries
        final double minStep = 1E-12;
        final double maxstep = 0.001;

        // Integrator tolerances
        final double positionTolerance = 1E-5;
        final double velocityTolerance = 1E-5;
        final double massTolerance = 1.0e-6;
        final double[] vecAbsoluteTolerances = {positionTolerance, positionTolerance, positionTolerance, velocityTolerance, velocityTolerance, velocityTolerance, massTolerance};
        final double[] vecRelativeTolerances = new double[vecAbsoluteTolerances.length];

        // Integrator definition
        final AdaptiveStepsizeIntegrator integrator = new DormandPrince853Integrator(minStep, maxstep,
                                                                                     vecAbsoluteTolerances,
                                                                                     vecRelativeTolerances);

        // Propagator definition
        this.propagator = new NumericalPropagator(integrator, attitudeProvider);

    }

    /**
     * Return the real starting PVCoordinates on the Libration orbit type
     * after differential correction from a first guess.
     * @param type libration orbit type
     * @return pv Position-Velocity of the starting point on the Halo Orbit
     */
    public PVCoordinates compute(final LibrationOrbitType type) {
        // Event detector settings
        final double maxcheck = 10;
        final double threshold = 1E-10;

        // Event detector definition
        final EventDetector XZPlaneCrossing =
            new HaloXZPlaneCrossingDetector(maxcheck, threshold).withHandler(new PlaneCrossingHandler());

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

        return type == LibrationOrbitType.HALO ? computeHalo(stm) : computeLyapunov(stm);
    }

    /** Return the real starting PVCoordinates on the Halo orbit after
     * differential correction from a first guess.
     * @param stm additional equations
     * @return pv Position-Velocity of the starting point on the Halo Orbit
     */
    private PVCoordinates computeHalo(final STMEquations stm) {

        // number of iteration
        double iHalo = 0;

        // Time settings (this date has no effect on the result, this is only for code structure purpose)
        final AbsoluteDate startDateHalo = new AbsoluteDate(1996, 06, 25, 0, 0, 00.000, utc);

        // Initializing PVCoordinates with first guess
        PVCoordinates pvHalo = firstGuess;

        // Start a new differentially corrected propagation until it converges to a Halo Orbit
        do {

            // SpacecraftState initialization
            final AbsolutePVCoordinates initialAbsPVHalo = new AbsolutePVCoordinates(syst.getRotatingFrame(), startDateHalo, pvHalo);
            final SpacecraftState       initialStateHalo = new SpacecraftState(initialAbsPVHalo);

            // Additional equations initialization
            final SpacecraftState augmentedInitialStateHalo = stm.setInitialPhi(initialStateHalo);

            // boolean changed to true by crossing XZ plane during propagation. Has to be true for the differential correction to converge
            cross = false;

            // Propagator initialization
            propagator.setInitialState(augmentedInitialStateHalo);

            // Propagate until trajectory crosses XZ Plane
            final SpacecraftState finalStateHalo =
                propagator.propagate(startDateHalo.shiftedBy(orbitalPeriodApprox));

            // Stops computation if trajectory did not cross XZ Plane after one full orbital period
            if (cross == false) {
                throw new OrekitException(OrekitMessages.TRAJECTORY_NOT_CROSSING_XZPLANE);
            }

            // Get State Transition Matrix phi
            final RealMatrix phiHalo = stm.getStateTransitionMatrix(finalStateHalo);

            // Gap from desired X and Z axis velocity value ()
            final double dvxf = -finalStateHalo.getPVCoordinates().getVelocity().getX();
            final double dvzf = -finalStateHalo.getPVCoordinates().getVelocity().getZ();

            orbitalPeriod = 2 * finalStateHalo.getDate().durationFrom(startDateHalo);

            if (FastMath.abs(dvxf) > 1E-8 || FastMath.abs(dvzf) > 1E-8) {
                // Y axis velocity
                final double vy =
                    finalStateHalo.getPVCoordinates().getVelocity().getY();

                // Spacecraft acceleration
                final Vector3D acc  = finalStateHalo.getPVCoordinates().getAcceleration();
                final double   accx = acc.getX();
                final double   accz = acc.getZ();

                // Compute A coefficients
                final double a11 = phiHalo.getEntry(3, 0) - accx * phiHalo.getEntry(1, 0) / vy;
                final double a12 = phiHalo.getEntry(3, 4) - accx * phiHalo.getEntry(1, 4) / vy;
                final double a21 = phiHalo.getEntry(5, 0) - accz * phiHalo.getEntry(1, 0) / vy;
                final double a22 = phiHalo.getEntry(5, 4) - accz * phiHalo.getEntry(1, 4) / vy;

                // A determinant used for matrix inversion
                final double aDet = a11 * a22 - a21 * a12;

                // Correction to apply to initial conditions
                final double deltax0  = (a22 * dvxf - a12 * dvzf) / aDet;
                final double deltavy0 = (-a21 * dvxf + a11 * dvzf) / aDet;

                // Computation of the corrected initial PVCoordinates
                final double newx  = pvHalo.getPosition().getX() + deltax0;
                final double newvy = pvHalo.getVelocity().getY() + deltavy0;

                pvHalo = new PVCoordinates(new Vector3D(newx,
                                                        pvHalo.getPosition().getY(),
                                                        pvHalo.getPosition().getZ()),
                                           new Vector3D(pvHalo.getVelocity().getX(),
                                                        newvy,
                                                        pvHalo.getVelocity().getZ()));
                ++iHalo;
            } else {
                break;
            }

        } while (iHalo < 30);  // Converge within 1E-8 tolerance and under 5 iterations

        // Return
        return pvHalo;
    }

    /** Return the real starting PVCoordinates on the Lyapunov orbit after
     * differential correction from a first guess.
     * @param stm additional equations
     * @return pv Position-Velocity of the starting point on the Lyapunov Orbit
     */
    public PVCoordinates computeLyapunov(final STMEquations stm) {

        // number of iteration
        double iLyapunov = 0;

        // Time settings (this date has no effect on the result, this is only for code structure purpose)
        final AbsoluteDate startDateLyapunov = new AbsoluteDate(1996, 06, 25, 0, 0, 00.000, utc);

        // Initializing PVCoordinates with first guess
        PVCoordinates pvLyapunov = firstGuess;

        // Start a new differentially corrected propagation until it converges to a Halo Orbit
        do {

            // SpacecraftState initialization
            final AbsolutePVCoordinates initialAbsPVLyapunov = new AbsolutePVCoordinates(syst.getRotatingFrame(), startDateLyapunov, pvLyapunov);
            final SpacecraftState       initialStateLyapunov = new SpacecraftState(initialAbsPVLyapunov);

            // Additional equations initialization
            final SpacecraftState augmentedInitialStateLyapunov = stm.setInitialPhi(initialStateLyapunov);

            // boolean changed to true by crossing XZ plane during propagation. Has to be true for the differential correction to converge
            cross = false;

            // Propagator initialization
            propagator.setInitialState(augmentedInitialStateLyapunov);

            // Propagate until trajectory crosses XZ Plane
            final SpacecraftState finalStateLyapunov =
                propagator.propagate(startDateLyapunov.shiftedBy(orbitalPeriodApprox));

            // Stops computation if trajectory did not cross XZ Plane after one full orbital period
            if (cross == false) {
                throw new OrekitException(OrekitMessages.TRAJECTORY_NOT_CROSSING_XZPLANE);
            }

            // Get State Transition Matrix phi
            final RealMatrix phi = stm.getStateTransitionMatrix(finalStateLyapunov);

            // Gap from desired y position and x velocity value ()
            final double dvxf = -finalStateLyapunov.getPVCoordinates().getVelocity().getX();

            orbitalPeriod = 2 * finalStateLyapunov.getDate().durationFrom(startDateLyapunov);

            if (FastMath.abs(dvxf) > 1E-14) {
                // Y axis velocity
                final double vy = finalStateLyapunov.getPVCoordinates().getVelocity().getY();

                // Spacecraft acceleration
                final double accy = finalStateLyapunov.getPVCoordinates().getAcceleration().getY();

                // Compute A coefficients
                final double deltavy0 = dvxf / (phi.getEntry(3, 4) - accy * phi.getEntry(1, 4) / vy);

                // Computation of the corrected initial PVCoordinates
                final double newvy = pvLyapunov.getVelocity().getY() + deltavy0;

                pvLyapunov = new PVCoordinates(new Vector3D(pvLyapunov.getPosition().getX(),
                                                            pvLyapunov.getPosition().getY(),
                                                            0),
                                               new Vector3D(pvLyapunov.getVelocity().getX(),
                                                            newvy,
                                                            0));

                ++iLyapunov;
            } else {
                break;
            }

        } while (iLyapunov < 30); // Converge within 1E-8 tolerance and under 5 iterations

        // Return
        return pvLyapunov;
    }

    /** Get the orbital period of the required orbit.
     * @return the orbitalPeriod
     */
    public double getOrbitalPeriod() {
        return orbitalPeriod;
    }

    /**
     * Static class for event detection.
     */
    private class PlaneCrossingHandler implements EventHandler<HaloXZPlaneCrossingDetector> {

        /** {@inheritDoc} */
        public Action eventOccurred(final SpacecraftState s,
                                    final HaloXZPlaneCrossingDetector detector,
                                    final boolean increasing) {
            cross = true;
            return Action.STOP;
        }
    }
}
