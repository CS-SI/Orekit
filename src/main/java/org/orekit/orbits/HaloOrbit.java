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
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.ode.events.Action;
import org.hipparchus.ode.nonstiff.AdaptiveStepsizeIntegrator;
import org.hipparchus.ode.nonstiff.GraggBulirschStoerIntegrator;
import org.hipparchus.util.FastMath;
import org.orekit.bodies.CR3BPSystem;
import org.orekit.frames.Frame;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.XZPlaneCrossingDetector;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.propagation.numerical.cr3bp.STMEquations;
import org.orekit.propagation.numerical.cr3bp.forces.CR3BPForceModel;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.AbsolutePVCoordinates;
import org.orekit.utils.LagrangianPoints;
import org.orekit.utils.PVCoordinates;

/** Class calculating different parameters of a Halo Orbit.
 * @author Vincent Mouraux
 */
public class HaloOrbit {

    /** Orbital Period of the Halo Orbit. */
    private double orbitalPeriod;

    /** Orbital Period of the secondary body around barycenter. */
    private double tDim;

    /** CR3BP System of the Halo Orbit. */
    private final CR3BPSystem cr3bpSystem;

    /** Position-Velocity first guess for a point on a Halo Orbit. */
    private final PVCoordinates firstGuess;

    /** Simple Constructor.
     * @param syst CR3BP System considered
     * @param point Lagrangian Point considered
     * @param firstGuess PVCoordinates of the first guess
    */
    public HaloOrbit(final CR3BPSystem syst, final LagrangianPoints point,
                     final PVCoordinates firstGuess) {
        this.tDim = syst.getTdim();
        this.cr3bpSystem = syst;
        this.firstGuess = firstGuess;
        orbitalPeriod = 0;
    }

    /** Simple Constructor.
     * @param syst CR3BP System considered
     * @param point Lagrangian Point considered
     * @param az z-axis Amplitude of the required Halo Orbit, meters
     * @param type type of the Halo Orbit ("Northern" or "Southern")
    */
    public HaloOrbit(final CR3BPSystem syst, final LagrangianPoints point, final double az, final String type) {
        this.tDim = syst.getTdim();
        this.cr3bpSystem = syst;

        this.firstGuess = new RichardsonExpansionContext(cr3bpSystem, point).computeFirstGuess(az, type);
        this.orbitalPeriod = new RichardsonExpansionContext(cr3bpSystem, point).getOrbitalPeriod(az, type);
    }

    /** Simple Constructor.
     * @param syst CR3BP System considered
     * @param point Lagrangian Point considered
     * @param az z-axis Amplitude of the required Halo Orbit, meters
     * @param type type of the Halo Orbit ("Northern" or "Southern")
     * @param t Orbit time, seconds (>0)
     * @param phi Orbit phase, rad
    */
    public HaloOrbit(final CR3BPSystem syst, final LagrangianPoints point, final double az,
                     final String type, final double t, final double phi) {
        this.tDim = syst.getTdim();
        this.cr3bpSystem = syst;

        firstGuess =
            new RichardsonExpansionContext(cr3bpSystem, point)
                .computeFirstGuess(az, type, t, phi);
        orbitalPeriod =
            new RichardsonExpansionContext(cr3bpSystem, point)
                .getOrbitalPeriod(az, type);
    }

    /** Return the orbital period of the Halo Orbit in second.
     * @return orbitalPeriod  orbital period of the Halo Orbit, second
     */
    public double getOrbitalPeriod() {
        return orbitalPeriod * tDim / (2 * FastMath.PI);
    }

    /** Return the first guess Position-Velocity of a point on the Halo Orbit.
     * @return firstGuess first guess Position-Velocity of a point on the Halo Orbit
     */
    public PVCoordinates getFirstGuess() {
        return firstGuess;
    }

    /** Return the real starting point PVCoordinates on the Halo orbit after differential correction from a first guess.
     * @param firstguess first guess PVCoordinates of the point to start differential correction
     * @param syst CR3BP System considered
     * @return pv Position-Velocity of the starting point on the Halo Orbit
     */
    public PVCoordinates differentialCorrection(final PVCoordinates firstguess,
                                                final CR3BPSystem syst) {
        //double iter = 0;
        double dvxf;
        double dvzf;

        // Time settings
        final AbsoluteDate initialDate =
            new AbsoluteDate(1996, 06, 25, 0, 0, 00.000,
                             TimeScalesFactory.getUTC());

        // Get the Rotating Frame in which both primaries are orbiting around
        // their common barycenter.
        final Frame rotatingFrame = syst.getRotatingFrame();

        PVCoordinates pv = firstguess;

        // !!!!!! NOT IN SECONDS !!!!! normalized time used in CR3BP calculation, treal = Tdim * t / (2 * pi)
        final double integrationTime = 5;

        // Integration parameters
        // These parameters are used for the Dormand-Prince integrator, a
        // variable step integrator,
        // these limits prevent the integrator to spend too much time when the
        // equations are too stiff,
        // as well as the reverse situation.
        final double minStep = 0.0001;
        final double maxstep = 1;

        // tolerances for integrators
        // Used by the integrator to estimate its variable integration step
        final double positionTolerance = 0.001;
        final double velocityTolerance = 0.001;
        final double massTolerance = 1.0e-6;
        final double[] vecAbsoluteTolerances = {positionTolerance, positionTolerance, positionTolerance, velocityTolerance, velocityTolerance, velocityTolerance, massTolerance };
        final double[] vecRelativeTolerances =
            new double[vecAbsoluteTolerances.length];

        // Defining the numerical integrator that will be used by the propagator
        final AdaptiveStepsizeIntegrator integrator =
            new GraggBulirschStoerIntegrator(minStep, maxstep,
                                             vecAbsoluteTolerances,
                                             vecRelativeTolerances);

        final double maxcheck = 1;
        final double threshold = 0.001;

        do {
            // PVCoordinates linked to a Frame and a Date
            final AbsolutePVCoordinates initialAbsPV =
                new AbsolutePVCoordinates(rotatingFrame, initialDate, pv);

            // Creating the initial spacecraftstate that will be given to the
            // propagator
            final SpacecraftState initialState = new SpacecraftState(initialAbsPV);

            final STMEquations stm = new STMEquations(syst);
            final SpacecraftState augmentedInitialState =
                stm.setInitialPhi(initialState);

            final EventDetector yPlaneCrossing =
                new XZPlaneCrossingDetector(maxcheck, threshold)
                    .withHandler(new planeCrossingHandler());

            final NumericalPropagator propagator =
                new NumericalPropagator(integrator);
            propagator.setOrbitType(null); // No known orbit type can be linked
                                           // to this propagation
            propagator.setIgnoreCentralAttraction(true); // The attraction in
                                                         // this problem is not
                                                         // central, mu is used
                                                         // differently
            propagator.addForceModel(new CR3BPForceModel(syst)); // Add our
                                                                 // specific
                                                                 // force model
                                                                 // to the
                                                                 // propagation,
                                                                 // it has to be
                                                                 // propagated
                                                                 // in the
                                                                 // rotating
                                                                 // frame*
            propagator.addAdditionalEquations(stm);
            propagator.setInitialState(augmentedInitialState);
            propagator.addEventDetector(yPlaneCrossing);

            final SpacecraftState finalState =
                propagator.propagate(initialDate.shiftedBy(integrationTime));
            final RealMatrix phi = stm.getStateTransitionMatrix(finalState);

            dvxf = -finalState.getPVCoordinates().getVelocity().getX();
            dvzf = -finalState.getPVCoordinates().getVelocity().getZ();
            // System.out.println(dvxf);
            // System.out.println(dvzf);
            // System.out.println(finalState.getPVCoordinates().getPosition().getY());
            final double Mdet =
                phi.getEntry(3, 0) * phi.getEntry(5, 4) -
                          phi.getEntry(5, 0) * phi.getEntry(3, 4);

            final double deltax0 =
                (phi.getEntry(5, 4) * dvxf - phi.getEntry(3, 4) * dvzf) / Mdet; // dx0
            final double deltavy0 =
                (-phi.getEntry(5, 0) * dvxf + phi.getEntry(3, 0) * dvzf) / Mdet; // dvy0

            final double newx = pv.getPosition().getX() + deltax0;
            final double newvy = pv.getVelocity().getY() + deltavy0;

            pv =
                new PVCoordinates(new Vector3D(newx, pv.getPosition().getY(),
                                               pv.getPosition().getZ()),
                                  new Vector3D(pv.getVelocity().getX(), newvy,
                                               pv.getVelocity().getZ()));
            //++iter;
        } while (FastMath.abs(dvxf) > 1E-5 || FastMath.abs(dvzf) > 1E-5);

        //System.out.println(iter);
        return pv;
    }

    /** Return the manifold vector.
     * @param pva point coordinates on the Halo orbit
     * @param phi Monodromy Matrix
     * @param p
     * @return manifold first guess Position-Velocity of a point on the Halo Orbit
     */
    /*
     * public PVCoordinates getManifolds(PVCoordinates pva, RealMatrix phi,
     * CR3BPSystem syst) { final RealVector unstableManifoldEigen = new
     * EigenDecomposition(phi).getEigenvector(0); final RealVector
     * stableManifoldEigen = new EigenDecomposition(phi).getEigenvector(1); }
     */

    /** Static class for event detection.
     */
    private static class planeCrossingHandler
        implements
        EventHandler<XZPlaneCrossingDetector> {

        /** {@inheritDoc} */
        public Action eventOccurred(final SpacecraftState s,
                                    final XZPlaneCrossingDetector detector,
                                    final boolean increasing) {
            return Action.STOP;
        }
    }
}

