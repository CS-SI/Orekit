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
package org.orekit.propagation.events;

import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitInternalError;
import org.orekit.errors.OrekitMessages;
import org.orekit.orbits.CircularOrbit;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.events.handlers.StopOnIncreasing;

/** Detector for in-orbit position angle.
 * <p>
 * The detector is based on anomaly for {@link OrbitType#KEPLERIAN Keplerian}
 * orbits, latitude argument for {@link OrbitType#CIRCULAR circular} orbits,
 * or longitude argument for {@link OrbitType#EQUINOCTIAL equinoctial} orbits.
 * It does not support {@link OrbitType#CARTESIAN Cartesian} orbits. The
 * angles can be either {@link PositionAngle#TRUE true}, {link {@link PositionAngle#MEAN
 * mean} or {@link PositionAngle#ECCENTRIC eccentric} angles.
 * </p>
 * @author Luc Maisonobe
 * @since 7.1
 */
public class PositionAngleDetector extends AbstractDetector<PositionAngleDetector> {

    /** Serializable UID. */
    private static final long serialVersionUID = 20150825L;

    /** Orbit type defining the angle type. */
    private final OrbitType orbitType;

    /** Type of position angle. */
    private final PositionAngle positionAngle;

    /** Fixed angle to be crossed. */
    private final double angle;

    /** Sign to apply for angle difference. */
    private double sign;

    /** Previous angle difference. */
    private double previousDelta;

    /** Build a new detector.
     * <p>The new instance uses default values for maximal checking interval
     * ({@link #DEFAULT_MAXCHECK}) and convergence threshold ({@link
     * #DEFAULT_THRESHOLD}).</p>
     * @param orbitType orbit type defining the angle type
     * @param positionAngle type of position angle
     * @param angle fixed angle to be crossed
     * @exception OrekitIllegalArgumentException if orbit type is {@link OrbitType#CARTESIAN}
     */
    public PositionAngleDetector(final OrbitType orbitType, final PositionAngle positionAngle,
                                 final double angle)
        throws OrekitIllegalArgumentException {
        this(DEFAULT_MAXCHECK, DEFAULT_THRESHOLD, orbitType, positionAngle, angle);
    }

    /** Build a detector.
     * @param maxCheck maximal checking interval (s)
     * @param threshold convergence threshold (s)
     * @param orbitType orbit type defining the angle type
     * @param positionAngle type of position angle
     * @param angle fixed angle to be crossed
     * @exception OrekitIllegalArgumentException if orbit type is {@link OrbitType#CARTESIAN}
     */
    public PositionAngleDetector(final double maxCheck, final double threshold,
                                 final OrbitType orbitType, final PositionAngle positionAngle,
                                 final double angle)
        throws OrekitIllegalArgumentException {
        this(maxCheck, threshold, DEFAULT_MAX_ITER, new StopOnIncreasing<PositionAngleDetector>(),
             orbitType, positionAngle, angle);
    }

    /** Private constructor with full parameters.
     * <p>
     * This constructor is private as users are expected to use the builder
     * API with the various {@code withXxx()} methods to set up the instance
     * in a readable manner without using a huge amount of parameters.
     * </p>
     * @param maxCheck maximum checking interval (s)
     * @param threshold convergence threshold (s)
     * @param maxIter maximum number of iterations in the event time search
     * @param handler event handler to call at event occurrences
     * @param orbitType orbit type defining the angle type
     * @param positionAngle type of position angle
     * @param angle fixed angle to be crossed
     * @exception OrekitIllegalArgumentException if orbit type is {@link OrbitType#CARTESIAN}
     */
    private PositionAngleDetector(final double maxCheck, final double threshold,
                                     final int maxIter, final EventHandler<? super PositionAngleDetector> handler,
                                     final OrbitType orbitType, final PositionAngle positionAngle,
                                     final double angle)
        throws OrekitIllegalArgumentException {
        super(maxCheck, threshold, maxIter, handler);
        if (orbitType == OrbitType.CARTESIAN) {
            final String sep = ", ";
            throw new OrekitIllegalArgumentException(OrekitMessages.ORBIT_TYPE_NOT_ALLOWED,
                                                     orbitType,
                                                     OrbitType.KEPLERIAN   + sep +
                                                     OrbitType.CIRCULAR    + sep +
                                                     OrbitType.EQUINOCTIAL);
        }
        this.orbitType     = orbitType;
        this.positionAngle = positionAngle;
        this.angle         = angle;
        this.sign          = +1.0;
        this.previousDelta = Double.NaN;
    }

    /** {@inheritDoc} */
    @Override
    protected PositionAngleDetector create(final double newMaxCheck, final double newThreshold,
                                              final int newMaxIter,
                                              final EventHandler<? super PositionAngleDetector> newHandler) {
        return new PositionAngleDetector(newMaxCheck, newThreshold, newMaxIter, newHandler,
                                         orbitType, positionAngle, angle);
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
    public PositionAngle getPositionAngle() {
        return positionAngle;
    }

    /** Get the fixed angle to be crossed (radians).
     * @return fixed angle to be crossed (radians)
     */
    public double getAngle() {
        return angle;
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
     * @exception OrekitException if some specific error occurs
     */
    public double g(final SpacecraftState s) throws OrekitException {

        // get angle
        final double currentAngle;
        switch (orbitType) {
            case KEPLERIAN:
                currentAngle = ((KeplerianOrbit) orbitType.convertType(s.getOrbit())).getAnomaly(positionAngle);
                break;
            case CIRCULAR:
                currentAngle = ((CircularOrbit) orbitType.convertType(s.getOrbit())).getAlpha(positionAngle);
                break;
            case EQUINOCTIAL:
                currentAngle = ((EquinoctialOrbit) orbitType.convertType(s.getOrbit())).getL(positionAngle);
                break;
            default:
                // this should never happen as type was checked at construction
                throw new OrekitInternalError(null);
        }

        // angle difference
        double delta = MathUtils.normalizeAngle(sign * (currentAngle - angle), 0.0);

        // ensure continuity
        if (FastMath.abs(delta - previousDelta) > FastMath.PI) {
            sign  = -sign;
            delta = MathUtils.normalizeAngle(sign * (currentAngle - angle), 0.0);
        }
        previousDelta = delta;

        return delta;

    }

}
