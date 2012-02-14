/* Copyright 2002-2011 CS Communication & Systèmes
 * Licensed to CS Communication & Systèmes (CS) under one or more
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

import java.io.Serializable;

import org.apache.commons.math3.ode.events.EventHandler;
import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitExceptionWrapper;
import org.orekit.frames.Frame;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;

/** Adapt an {@link org.orekit.propagation.events.EventDetector}
 * to commons-math {@link org.apache.commons.math3.ode.events.EventHandler} interface.
 * @author Fabien Maussion
 */
public class AdaptedEventDetector implements EventHandler, Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = -5983739314228874403L;

    /** Propagation orbit type. */
    private final OrbitType orbitType;

    /** Position angle type. */
    private final PositionAngle angleType;

    /** Attitude provider. */
    private final AttitudeProvider attitudeProvider;

    /** Underlying event detector. */
    private final EventDetector detector;

    /** Reference date from which t is counted. */
    private final AbsoluteDate referenceDate;

    /** Central body attraction coefficient (m<sup>3</sup>/s<sup>2</sup>). */
    private final double mu;

    /** integrationFrame frame in which integration is performed. */
    private final Frame integrationFrame;

    /** Build a wrapped event detector.
     * @param detector event detector to wrap
     * @param orbitType orbit type
     * @param angleType position angle type
     * @param attitudeProvider attitude provider
     * @param referenceDate reference date from which t is counted
     * @param mu central body attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
     * @param integrationFrame frame in which integration is performed
     */
    public AdaptedEventDetector(final EventDetector detector,
                                final OrbitType orbitType, final PositionAngle angleType,
                                final AttitudeProvider attitudeProvider,
                                final AbsoluteDate referenceDate,
                                final double mu, final Frame integrationFrame) {
        this.detector         = detector;
        this.orbitType        = orbitType;
        this.angleType        = angleType;
        this.attitudeProvider = attitudeProvider;
        this.referenceDate    = referenceDate;
        this.mu               = mu;
        this.integrationFrame = integrationFrame;
    }

    /** Map array to spacecraft state.
     * @param t relative date
     * @param y current state
     * @return spacecraft state as a flight dynamics object
     * @exception OrekitException if mapping cannot be done
     */
    private SpacecraftState mapArrayToState(final double t, final double[] y)
        throws OrekitException {
        final AbsoluteDate currentDate = referenceDate.shiftedBy(t);
        final Orbit currentOrbit =
            orbitType.mapArrayToOrbit(y, angleType, currentDate, mu, integrationFrame);
        final Attitude currentAttitude =
            attitudeProvider.getAttitude(currentOrbit, currentDate, integrationFrame);
        return new SpacecraftState(currentOrbit, currentAttitude, y[6]);
    }

    /** {@inheritDoc} */
    public void init(final double t0, final double[] y0, final double t) {
        try {
            detector.init(mapArrayToState(t0, y0), referenceDate.shiftedBy(t));
        } catch (OrekitException oe) {
            throw new OrekitExceptionWrapper(oe);
        }
    }

    /** {@inheritDoc} */
    public double g(final double t, final double[] y) {
        try {
            return detector.g(mapArrayToState(t, y));
        } catch (OrekitException oe) {
            throw new OrekitExceptionWrapper(oe);
        }
    }

    /** {@inheritDoc} */
    public Action eventOccurred(final double t, final double[] y, final boolean increasing) {
        try {

            final SpacecraftState state = mapArrayToState(t, y);
            final EventDetector.Action whatNext = detector.eventOccurred(state, increasing);

            switch (whatNext) {
            case STOP :
                return Action.STOP;
            case RESET_STATE :
                return Action.RESET_STATE;
            case RESET_DERIVATIVES :
                return Action.RESET_DERIVATIVES;
            default :
                return Action.CONTINUE;
            }
        } catch (OrekitException oe) {
            throw new OrekitExceptionWrapper(oe);
        }
    }

    /** {@inheritDoc} */
    public void resetState(final double t, final double[] y) {
        try {
            final SpacecraftState oldState = mapArrayToState(t, y);
            final SpacecraftState newState = detector.resetState(oldState);
            orbitType.mapOrbitToArray(newState.getOrbit(), angleType, y);
            y[6] = newState.getMass();
        } catch (OrekitException oe) {
            throw new OrekitExceptionWrapper(oe);
        }
    }

}
