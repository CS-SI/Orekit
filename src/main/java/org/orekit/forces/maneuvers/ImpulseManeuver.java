/* Copyright 2002-2010 CS Communication & Systèmes
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
package org.orekit.forces.maneuvers;

import org.apache.commons.math.geometry.Rotation;
import org.apache.commons.math.geometry.Vector3D;
import org.orekit.attitudes.Attitude;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;

/** Impulse maneuver model.
 * <p>This class implements an impulse maneuver as a discrete event
 * that can be provided to any {@link org.orekit.propagation.Propagator
 * Propagator}.</p>
 * <p>The maneuver is triggered when an underlying event generates a
 * {@link EventDetector#STOP STOP} event, in which case this class will
 * generate a {@link EventDetector#RESET_STATE RESET_STATE} event (the
 * stop event from the underlying object is therefore filtered out).
 * In the simple cases, the underlying event detector may be a basic
 * {@link org.orekit.propagation.events.DateDetector date event}, but it
 * can also be a more elaborate {@link
 * org.orekit.propagation.events.ApsideDetector apside event} for apogee
 * maneuvers for example.</p>
 * <p>The maneuver is defined by a single velocity increment in satellite
 * frame. The current attitude of the spacecraft, defined by the current
 * spacecraft state, will be used to compute the velocity direction in
 * inertial frame. A typical case for tangential maneuvers is to use a
 * {@link org.orekit.attitudes.LofOffset LOF aligned} attitude law for state propagation and a
 * velocity increment along the +X satellite axis.</p>
 * <p>Beware that the triggering event detector must behave properly both
 * before and after maneuver. If for example a node detector is used to trigger
 * an inclination maneuver and the maneuver change the orbit to an equatorial one,
 * the node detector will fail just after the maneuver, being unable to find a
 * node on an equatorial orbit! This is a real case that has been encountered
 * during validation ...</p>
 * @see org.orekit.propagation.Propagator#addEventDetector(EventDetector)
 * @author Luc Maisonobe
 * @version $Revision$ $Date$
 */
public class ImpulseManeuver implements EventDetector {

    /** Serializable UID. */
    private static final long serialVersionUID = -7150871329986590368L;

    /** Triggering event. */
    private final EventDetector trigger;

    /** Velocity increment in satellite frame. */
    private final Vector3D deltaVSat;

    /** Engine exhaust velocity. */
    private final double vExhaust;

    /** Build a new instance.
     * @param trigger triggering event
     * @param deltaVSat velocity increment in satellite frame
     * @param isp engine specific impulse (s)
     */
    public ImpulseManeuver(final EventDetector trigger, final Vector3D deltaVSat,
                           final double isp) {
        this.trigger   = trigger;
        this.deltaVSat = deltaVSat;
        this.vExhaust  = ConstantThrustManeuver.G0 * isp;
    }

    /** {@inheritDoc} */
    public double getMaxCheckInterval() {
        return trigger.getMaxCheckInterval();
    }

    /** {@inheritDoc} */
    public int getMaxIterationCount() {
        return trigger.getMaxIterationCount();
    }

    /** {@inheritDoc} */
    public double getThreshold() {
        return trigger.getThreshold();
    }

    /** {@inheritDoc} */
    public int eventOccurred(final SpacecraftState s, final boolean increasing)
        throws OrekitException {
        // filter underlying event
        return (trigger.eventOccurred(s, increasing) == STOP) ? RESET_STATE : CONTINUE;
    }

    /** {@inheritDoc} */
    public double g(final SpacecraftState s) throws OrekitException {
        return trigger.g(s);
    }

    /** {@inheritDoc} */
    public SpacecraftState resetState(final SpacecraftState oldState)
        throws OrekitException {

        final Frame eme2000     = FramesFactory.getEME2000();
        final AbsoluteDate date = oldState.getDate();
        final Attitude attitude = oldState.getAttitude();

        // convert velocity increment in EME2000 frame
        final Rotation refToEME2000 =
            attitude.getReferenceFrame().getTransformTo(eme2000, date).getRotation();
        final Rotation satToEME2000 = refToEME2000.applyTo(attitude.getRotation().revert());
        final Vector3D deltaV = satToEME2000.applyTo(deltaVSat);

        // apply increment to position/velocity
        final PVCoordinates oldPV = oldState.getPVCoordinates(eme2000);
        final PVCoordinates newPV = new PVCoordinates(oldPV.getPosition(),
                                                      oldPV.getVelocity().add(deltaV));

        // compute new mass
        final double newMass = oldState.getMass() * Math.exp(-deltaV.getNorm() / vExhaust);

        // pack everything in a new state
        return new SpacecraftState(new EquinoctialOrbit(newPV, eme2000, date, oldState.getMu()),
                                   attitude, newMass);

    }

}
