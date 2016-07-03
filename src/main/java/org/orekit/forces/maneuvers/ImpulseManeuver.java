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
package org.orekit.forces.maneuvers;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.errors.OrekitException;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.AbstractDetector;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;

/** Impulse maneuver model.
 * <p>This class implements an impulse maneuver as a discrete event
 * that can be provided to any {@link org.orekit.propagation.Propagator
 * Propagator}.</p>
 * <p>The maneuver is triggered when an underlying event generates a
 * {@link org.orekit.propagation.events.handlers.EventHandler.Action#STOP STOP} event,
 * in which case this class will generate a {@link
 * org.orekit.propagation.events.handlers.EventHandler.Action#RESET_STATE RESET_STATE}
 * event (the stop event from the underlying object is therefore filtered out).
 * In the simple cases, the underlying event detector may be a basic
 * {@link org.orekit.propagation.events.DateDetector date event}, but it
 * can also be a more elaborate {@link
 * org.orekit.propagation.events.ApsideDetector apside event} for apogee
 * maneuvers for example.</p>
 * <p>The maneuver is defined by a single velocity increment in satellite
 * frame. The current attitude of the spacecraft, defined by the current
 * spacecraft state, will be used to compute the velocity direction in
 * inertial frame. A typical case for tangential maneuvers is to use a
 * {@link org.orekit.attitudes.LofOffset LOF aligned} attitude provider for state propagation and a
 * velocity increment along the +X satellite axis.</p>
 * <p>Beware that the triggering event detector must behave properly both
 * before and after maneuver. If for example a node detector is used to trigger
 * an inclination maneuver and the maneuver change the orbit to an equatorial one,
 * the node detector will fail just after the maneuver, being unable to find a
 * node on an equatorial orbit! This is a real case that has been encountered
 * during validation ...</p>
 * @see org.orekit.propagation.Propagator#addEventDetector(EventDetector)
     * @param <T> class type for the generic version
 * @author Luc Maisonobe
 */
public class ImpulseManeuver<T extends EventDetector> extends AbstractDetector<ImpulseManeuver<T>> {

    /** Serializable UID. */
    private static final long serialVersionUID = 20131118L;

    /** The attitude to override during the maneuver, if set. **/
    private final AttitudeProvider attitudeOverride;

    /** Triggering event. */
    private final T trigger;

    /** Velocity increment in satellite frame. */
    private final Vector3D deltaVSat;

    /** Specific impulse. */
    private final double isp;

    /** Engine exhaust velocity. */
    private final double vExhaust;

    /** Indicator for forward propagation. */
    private boolean forward;

    /** Build a new instance.
     * @param trigger triggering event
     * @param deltaVSat velocity increment in satellite frame
     * @param isp engine specific impulse (s)
     */
    public ImpulseManeuver(final T trigger, final Vector3D deltaVSat, final double isp) {
        this(trigger.getMaxCheckInterval(), trigger.getThreshold(),
             trigger.getMaxIterationCount(), new Handler<T>(),
             trigger, null, deltaVSat, isp);
    }


    /** Build a new instance.
     * @param trigger triggering event
     * @param attitudeOverride the attitude provider to use for the maneuver
     * @param deltaVSat velocity increment in satellite frame
     * @param isp engine specific impulse (s)
     */
    public ImpulseManeuver(final T trigger, final AttitudeProvider attitudeOverride, final Vector3D deltaVSat, final double isp) {
        this(trigger.getMaxCheckInterval(), trigger.getThreshold(),
             trigger.getMaxIterationCount(), new Handler<T>(),
             trigger, attitudeOverride, deltaVSat, isp);
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
     * @param trigger triggering event
     * @param attitudeOverride the attitude provider to use for the maneuver
     * @param deltaVSat velocity increment in satellite frame
     * @param isp engine specific impulse (s)
     * @since 6.1
     */
    private ImpulseManeuver(final double maxCheck, final double threshold,
                            final int maxIter, final EventHandler<? super ImpulseManeuver<T>> handler,
                            final T trigger, final AttitudeProvider attitudeOverride, final Vector3D deltaVSat,
                            final double isp) {
        super(maxCheck, threshold, maxIter, handler);
        this.attitudeOverride = attitudeOverride;
        this.trigger   = trigger;
        this.deltaVSat = deltaVSat;
        this.isp       = isp;
        this.vExhaust  = Constants.G0_STANDARD_GRAVITY * isp;
    }

    /** {@inheritDoc} */
    @Override
    protected ImpulseManeuver<T> create(final double newMaxCheck, final double newThreshold,
                                        final int newMaxIter, final EventHandler<? super ImpulseManeuver<T>> newHandler) {
        return new ImpulseManeuver<T>(newMaxCheck, newThreshold, newMaxIter, newHandler,
                                      trigger, attitudeOverride, deltaVSat, isp);
    }

    /** {@inheritDoc} */
    public void init(final SpacecraftState s0, final AbsoluteDate t) {
        forward = t.durationFrom(s0.getDate()) >= 0;
    }

    /** {@inheritDoc} */
    public double g(final SpacecraftState s) throws OrekitException {
        return trigger.g(s);
    }

    /**
     * Get the Attitude Provider to use during maneuver.
     * @return the attitude provider
     */
    public AttitudeProvider getAttitudeOverride() {
        return attitudeOverride;
    }

    /** Get the triggering event.
     * @return triggering event
     */
    public T getTrigger() {
        return trigger;
    }

    /** Get the velocity increment in satellite frame.
    * @return velocity increment in satellite frame
    */
    public Vector3D getDeltaVSat() {
        return deltaVSat;
    }

    /** Get the specific impulse.
    * @return specific impulse
    */
    public double getIsp() {
        return isp;
    }

    /** Local handler.
     * @param <T> class type for the generic version
     */
    private static class Handler<T extends EventDetector> implements EventHandler<ImpulseManeuver<T>> {

        /** {@inheritDoc} */
        public EventHandler.Action eventOccurred(final SpacecraftState s, final ImpulseManeuver<T> im,
                                                 final boolean increasing)
            throws OrekitException {

            // filter underlying event
            final EventHandler.Action underlyingAction = im.trigger.eventOccurred(s, increasing);

            return (underlyingAction == Action.STOP) ? Action.RESET_STATE : Action.CONTINUE;

        }

        /** {@inheritDoc} */
        @Override
        public SpacecraftState resetState(final ImpulseManeuver<T> im, final SpacecraftState oldState)
            throws OrekitException {

            final AbsoluteDate date = oldState.getDate();
            final AttitudeProvider override = im.getAttitudeOverride();
            final Attitude attitude;

            if (override == null) {
                attitude = oldState.getAttitude();
            } else {
                attitude = override.getAttitude(oldState.getOrbit(), date, oldState.getFrame());
            }

            // convert velocity increment in inertial frame
            final Vector3D deltaV = attitude.getRotation().applyInverseTo(im.deltaVSat);
            final double sign     = im.forward ? +1 : -1;

            // apply increment to position/velocity
            final PVCoordinates oldPV = oldState.getPVCoordinates();
            final PVCoordinates newPV =
                            new PVCoordinates(oldPV.getPosition(),
                                              new Vector3D(1, oldPV.getVelocity(), sign, deltaV));
            final CartesianOrbit newOrbit =
                    new CartesianOrbit(newPV, oldState.getFrame(), date, oldState.getMu());

            // compute new mass
            final double newMass = oldState.getMass() * FastMath.exp(-sign * deltaV.getNorm() / im.vExhaust);

            // pack everything in a new state
            return new SpacecraftState(oldState.getOrbit().getType().convertType(newOrbit),
                                       attitude, newMass);

        }

    }

}
