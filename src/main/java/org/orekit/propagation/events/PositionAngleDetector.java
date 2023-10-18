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
package org.orekit.propagation.events;

import java.util.function.Function;

import org.hipparchus.analysis.UnivariateFunction;
import org.hipparchus.analysis.solvers.BracketingNthOrderBrentSolver;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;
import org.orekit.orbits.CircularOrbit;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.events.handlers.StopOnEvent;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.TimeSpanMap;

/** Detector for in-orbit position angle.
 * <p>
 * The detector is based on anomaly for {@link OrbitType#KEPLERIAN Keplerian}
 * orbits, latitude argument for {@link OrbitType#CIRCULAR circular} orbits,
 * or longitude argument for {@link OrbitType#EQUINOCTIAL equinoctial} orbits.
 * It does not support {@link OrbitType#CARTESIAN Cartesian} orbits. The
 * angles can be either {@link PositionAngleType#TRUE true}, {link {@link PositionAngleType#MEAN
 * mean} or {@link PositionAngleType#ECCENTRIC eccentric} angles.
 * </p>
 * @author Luc Maisonobe
 * @since 7.1
 */
public class PositionAngleDetector extends AbstractDetector<PositionAngleDetector> {

    /** Orbit type defining the angle type. */
    private final OrbitType orbitType;

    /** Type of position angle. */
    private final PositionAngleType positionAngleType;

    /** Fixed angle to be crossed. */
    private final double angle;

    /** Position angle extraction function. */
    private final Function<Orbit, Double> positionAngleExtractor;

    /** Estimators for the offset angle, taking care of 2π wrapping and g function continuity. */
    private TimeSpanMap<OffsetEstimator> offsetEstimators;

    /** Build a new detector.
     * <p>The new instance uses default values for maximal checking interval
     * ({@link #DEFAULT_MAXCHECK}) and convergence threshold ({@link
     * #DEFAULT_THRESHOLD}).</p>
     * @param orbitType orbit type defining the angle type
     * @param positionAngleType type of position angle
     * @param angle fixed angle to be crossed
     * @exception OrekitIllegalArgumentException if orbit type is {@link OrbitType#CARTESIAN}
     */
    public PositionAngleDetector(final OrbitType orbitType, final PositionAngleType positionAngleType,
                                 final double angle)
        throws OrekitIllegalArgumentException {
        this(DEFAULT_MAXCHECK, DEFAULT_THRESHOLD, orbitType, positionAngleType, angle);
    }

    /** Build a detector.
     * <p> This instance uses by default the {@link StopOnEvent} handler </p>
     * @param maxCheck maximal checking interval (s)
     * @param threshold convergence threshold (s)
     * @param orbitType orbit type defining the angle type
     * @param positionAngleType type of position angle
     * @param angle fixed angle to be crossed
     * @exception OrekitIllegalArgumentException if orbit type is {@link OrbitType#CARTESIAN}
     */
    public PositionAngleDetector(final double maxCheck, final double threshold,
                                 final OrbitType orbitType, final PositionAngleType positionAngleType,
                                 final double angle)
        throws OrekitIllegalArgumentException {
        this(s -> maxCheck, threshold, DEFAULT_MAX_ITER, new StopOnEvent(),
             orbitType, positionAngleType, angle);
    }

    /** Protected constructor with full parameters.
     * <p>
     * This constructor is not public as users are expected to use the builder
     * API with the various {@code withXxx()} methods to set up the instance
     * in a readable manner without using a huge amount of parameters.
     * </p>
     * @param maxCheck maximum checking interval
     * @param threshold convergence threshold (s)
     * @param maxIter maximum number of iterations in the event time search
     * @param handler event handler to call at event occurrences
     * @param orbitType orbit type defining the angle type
     * @param positionAngleType type of position angle
     * @param angle fixed angle to be crossed
     * @exception OrekitIllegalArgumentException if orbit type is {@link OrbitType#CARTESIAN}
     */
    protected PositionAngleDetector(final AdaptableInterval maxCheck, final double threshold,
                                    final int maxIter, final EventHandler handler,
                                    final OrbitType orbitType, final PositionAngleType positionAngleType,
                                    final double angle)
        throws OrekitIllegalArgumentException {

        super(maxCheck, threshold, maxIter, handler);

        this.orbitType        = orbitType;
        this.positionAngleType = positionAngleType;
        this.angle            = angle;
        this.offsetEstimators = null;

        switch (orbitType) {
            case KEPLERIAN:
                positionAngleExtractor = o -> ((KeplerianOrbit) orbitType.convertType(o)).getAnomaly(positionAngleType);
                break;
            case CIRCULAR:
                positionAngleExtractor = o -> ((CircularOrbit) orbitType.convertType(o)).getAlpha(positionAngleType);
                break;
            case EQUINOCTIAL:
                positionAngleExtractor = o -> ((EquinoctialOrbit) orbitType.convertType(o)).getL(positionAngleType);
                break;
            default:
                final String sep = ", ";
                throw new OrekitIllegalArgumentException(OrekitMessages.ORBIT_TYPE_NOT_ALLOWED,
                                                         orbitType,
                                                         OrbitType.KEPLERIAN   + sep +
                                                         OrbitType.CIRCULAR    + sep +
                                                         OrbitType.EQUINOCTIAL);
        }

    }

    /** {@inheritDoc} */
    @Override
    protected PositionAngleDetector create(final AdaptableInterval newMaxCheck, final double newThreshold,
                                           final int newMaxIter,
                                           final EventHandler newHandler) {
        return new PositionAngleDetector(newMaxCheck, newThreshold, newMaxIter, newHandler,
                                         orbitType, positionAngleType, angle);
    }

    /** Get the orbit type defining the angle type.
     * @return orbit type defining the angle type
     */
    public OrbitType getOrbitType() {
        return orbitType;
    }

    /** Get the type of position angle.
     * @return type of position angle
     */
    public PositionAngleType getPositionAngleType() {
        return positionAngleType;
    }

    /** Get the fixed angle to be crossed (radians).
     * @return fixed angle to be crossed (radians)
     */
    public double getAngle() {
        return angle;
    }

    /** {@inheritDoc} */
    public void init(final SpacecraftState s0, final AbsoluteDate t) {
        super.init(s0, t);
        offsetEstimators = new TimeSpanMap<>(new OffsetEstimator(s0.getOrbit(), +1.0));
    }

    /** Compute the value of the detection function.
     * <p>
     * The value is the angle difference between the spacecraft and the fixed
     * angle to be crossed, with some sign tweaks to ensure continuity.
     * These tweaks imply the {@code increasing} flag in events detection becomes
     * irrelevant here! As an example, the angle always increase in a Keplerian
     * orbit, but this g function will increase and decrease so it
     * will cross the zero value once per orbit, in increasing and decreasing
     * directions on alternate orbits..
     * </p>
     * @param s the current state information: date, kinematics, attitude
     * @return angle difference between the spacecraft and the fixed
     * angle, with some sign tweaks to ensure continuity
     */
    public double g(final SpacecraftState s) {

        final Orbit orbit = s.getOrbit();

        // angle difference
        OffsetEstimator estimator = offsetEstimators.get(s.getDate());
        double          delta     = estimator.delta(orbit);

        // we use a value greater than π for handover in order to avoid
        // several switches to be estimated as the calling propagator
        // and Orbit.shiftedBy have different accuracy. It is sufficient
        // to have a handover roughly opposite to the detected position angle
        while (FastMath.abs(delta) >= 3.5) {
            // we are too far away from the current estimator, we need to set up a new one
            // ensuring that we do have a crossing event in the current orbit
            // and we ensure sign continuity with the current estimator

            // find when the previous estimator becomes invalid
            final AbsoluteDate handover = estimator.dateForOffset(FastMath.copySign(FastMath.PI, delta), orbit);

            // perform handover to a new estimator at this date
            estimator = new OffsetEstimator(orbit, delta);
            delta     = estimator.delta(orbit);
            if (isForward()) {
                offsetEstimators.addValidAfter(estimator, handover.getDate(), false);
            } else {
                offsetEstimators.addValidBefore(estimator, handover.getDate(), false);
            }

        }

        return delta;

    }

    /** Local class for estimating offset angle, handling 2π wrap-up and sign continuity. */
    private class OffsetEstimator {

        /** Target angle. */
        private final double target;

        /** Sign correction to offset. */
        private final double sign;

        /** Reference angle. */
        private final double r0;

        /** Slope of the linearized model. */
        private final double r1;

        /** Reference date. */
        private final AbsoluteDate t0;

        /** Simple constructor.
         * @param orbit current orbit
         * @param currentSign desired sign of the offset at current orbit time (magnitude is ignored)
         */
        OffsetEstimator(final Orbit orbit, final double currentSign) {
            r0     = positionAngleExtractor.apply(orbit);
            target = MathUtils.normalizeAngle(angle, r0);
            sign   = FastMath.copySign(1.0, (r0 - target) * currentSign);
            r1     = orbit.getKeplerianMeanMotion();
            t0     = orbit.getDate();
        }

        /** Compute offset from reference angle.
         * @param orbit current orbit
         * @return offset between current angle and reference angle
         */
        public double delta(final Orbit orbit) {
            final double rawAngle        = positionAngleExtractor.apply(orbit);
            final double linearReference = r0 + r1 * orbit.getDate().durationFrom(t0);
            final double linearizedAngle = MathUtils.normalizeAngle(rawAngle, linearReference);
            return sign * (linearizedAngle - target);
        }

        /** Find date at which offset reaches specified value.
         * <p>
         * This computation is an approximation because it relies on
         * {@link Orbit#shiftedBy(double)} only.
         * </p>
         * @param offset target value for offset angle
         * @param orbit current orbit
         * @return approximate date at which offset reached specified value
         */
        public AbsoluteDate dateForOffset(final double offset, final Orbit orbit) {

            // bracket the search
            final double period = orbit.getKeplerianPeriod();
            final double delta0 = delta(orbit);
            final double searchInf;
            final double searchSup;
            if ((delta0 - offset) * sign >= 0) {
                // the date is before current orbit
                searchInf = -period;
                searchSup = 0;
            } else {
                // the date is after current orbit
                searchInf = 0;
                searchSup = +period;
            }

            // find the date as an offset from current orbit
            final BracketingNthOrderBrentSolver solver = new BracketingNthOrderBrentSolver(getThreshold(), 5);
            final UnivariateFunction            f      = dt -> delta(orbit.shiftedBy(dt)) - offset;
            final double                        root   = solver.solve(getMaxIterationCount(), f, searchInf, searchSup);

            return orbit.getDate().shiftedBy(root);

        }

    }

}
