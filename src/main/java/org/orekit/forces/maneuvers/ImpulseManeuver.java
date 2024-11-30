/* Copyright 2002-2024 CS GROUP
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
package org.orekit.forces.maneuvers;

import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.events.Action;
import org.hipparchus.util.FastMath;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetectionSettings;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.DoubleArrayDictionary;
import org.orekit.utils.PVCoordinates;

/** Impulse maneuver model.
 * <p>This class implements an impulse maneuver as a discrete event
 * that can be provided to any {@link org.orekit.propagation.Propagator
 * Propagator}.</p>
 * <p>The maneuver is executed when an underlying is triggered, in which case this class will generate a {@link
 * Action#RESET_STATE RESET_STATE} event. By default, the detection settings are those of the trigger.
 * In the simple cases, the underlying event detector may be a basic
 * {@link org.orekit.propagation.events.DateDetector date event}, but it
 * can also be a more elaborate {@link
 * org.orekit.propagation.events.ApsideDetector apside event} for apogee
 * maneuvers for example.</p>
 * <p>The maneuver is defined by a single velocity increment.
 * If no AttitudeProvider is given, the current attitude of the spacecraft,
 * defined by the current spacecraft state, will be used as the
 * {@link AttitudeProvider} so the velocity increment should be given in
 * the same pseudoinertial frame as the {@link SpacecraftState} used to
 * construct the propagator that will handle the maneuver.
 * If an AttitudeProvider is given, the velocity increment given should be
 * defined appropriately in consideration of that provider. So, a typical
 * case for tangential maneuvers is to provide a {@link org.orekit.attitudes.LofOffset LOF aligned}
 * attitude provider along with a velocity increment defined in accordance with
 * that LOF aligned attitude provider; e.g. if the LOF aligned attitude provider
 * was constructed using LOFType.VNC the velocity increment should be
 * provided in VNC coordinates.</p>
 * <p>The norm through which the delta-V maps to the mass consumption is chosen via the
 * enum {@link Control3DVectorCostType}. Default is Euclidean. </p>
 * <p>Beware that the triggering event detector must behave properly both
 * before and after maneuver. If for example a node detector is used to trigger
 * an inclination maneuver and the maneuver change the orbit to an equatorial one,
 * the node detector will fail just after the maneuver, being unable to find a
 * node on an equatorial orbit! This is a real case that has been encountered
 * during validation ...</p>
 * @see org.orekit.propagation.Propagator#addEventDetector(EventDetector)
 * @author Luc Maisonobe
 */
public class ImpulseManeuver implements EventDetector {

    /** The attitude to override during the maneuver, if set. */
    private final AttitudeProvider attitudeOverride;

    /** Triggering event. */
    private final EventDetector trigger;

    /** Velocity increment in satellite frame. */
    private final Vector3D deltaVSat;

    /** Specific impulse. */
    private final double isp;

    /** Engine exhaust velocity. */
    private final double vExhaust;

    /** Trigger's detection settings. */
    private final EventDetectionSettings detectionSettings;

    /** Specific event handler. */
    private final Handler handler;

    /** Indicator for forward propagation. */
    private boolean forward;

    /** Type of norm linking delta-V to mass consumption. */
    private final Control3DVectorCostType control3DVectorCostType;

    /** Build a new instance.
     * @param trigger triggering event
     * @param deltaVSat velocity increment in satellite frame
     * @param isp engine specific impulse (s)
     */
    public ImpulseManeuver(final EventDetector trigger, final Vector3D deltaVSat, final double isp) {
        this(trigger, null, deltaVSat, isp);
    }

    /** Build a new instance.
     * @param trigger triggering event
     * @param attitudeOverride the attitude provider to use for the maneuver
     * @param deltaVSat velocity increment in satellite frame
     * @param isp engine specific impulse (s)
     */
    public ImpulseManeuver(final EventDetector trigger, final AttitudeProvider attitudeOverride,
                           final Vector3D deltaVSat, final double isp) {
        this(trigger, attitudeOverride, deltaVSat, isp, Control3DVectorCostType.TWO_NORM);
    }

    /** Build a new instance.
     * @param trigger triggering event
     * @param attitudeOverride the attitude provider to use for the maneuver
     * @param deltaVSat velocity increment in satellite frame
     * @param isp engine specific impulse (s)
     * @param control3DVectorCostType increment's norm for mass consumption
     */
    public ImpulseManeuver(final EventDetector trigger, final AttitudeProvider attitudeOverride,
                           final Vector3D deltaVSat, final double isp, final Control3DVectorCostType control3DVectorCostType) {
        this(trigger, trigger.getDetectionSettings(), attitudeOverride, deltaVSat, isp, control3DVectorCostType);
    }

    /** Private constructor.
     * @param trigger triggering event
     * @param detectionSettings event detection settings
     * @param attitudeOverride the attitude provider to use for the maneuver
     * @param deltaVSat velocity increment in satellite frame
     * @param isp engine specific impulse (s)
     * @param control3DVectorCostType increment's norm for mass consumption
     * @since 13.0
     */
    private ImpulseManeuver(final EventDetector trigger, final EventDetectionSettings detectionSettings,
                            final AttitudeProvider attitudeOverride, final Vector3D deltaVSat,
                            final double isp, final Control3DVectorCostType control3DVectorCostType) {
        this.attitudeOverride = attitudeOverride;
        this.trigger   = trigger;
        this.detectionSettings = detectionSettings;
        this.deltaVSat = deltaVSat;
        this.isp       = isp;
        this.vExhaust  = Constants.G0_STANDARD_GRAVITY * isp;
        this.control3DVectorCostType = control3DVectorCostType;
        this.handler = new Handler();
    }

    /**
     * Creates a copy with different event detection settings.
     * @param eventDetectionSettings new detection settings
     * @return a new detector with same properties except for the detection settings
     */
    public ImpulseManeuver withDetectionSettings(final EventDetectionSettings eventDetectionSettings) {
        return new ImpulseManeuver(trigger, eventDetectionSettings, attitudeOverride, deltaVSat, isp,
                control3DVectorCostType);
    }

    /** {@inheritDoc} */
    @Override
    public void init(final SpacecraftState s0, final AbsoluteDate t) {
        forward = t.durationFrom(s0.getDate()) >= 0;
        // Initialize the triggering event
        trigger.init(s0, t);
    }

    /** {@inheritDoc} */
    @Override
    public double g(final SpacecraftState s) {
        return trigger.g(s);
    }

    /** {@inheritDoc} */
    @Override
    public void finish(final SpacecraftState state) {
        EventDetector.super.finish(state);
        trigger.finish(state);
    }

    /** {@inheritDoc} */
    @Override
    public EventHandler getHandler() {
        return handler;
    }

    /** {@inheritDoc} */
    @Override
    public EventDetectionSettings getDetectionSettings() {
        return detectionSettings;
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
    public EventDetector getTrigger() {
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

    /** Get the control vector's cost type.
     * @return control cost type
     * @since 12.0
     */
    public Control3DVectorCostType getControl3DVectorCostType() {
        return control3DVectorCostType;
    }

    /** Local handler. */
    private static class Handler implements EventHandler {

        /** {@inheritDoc} */
        public Action eventOccurred(final SpacecraftState s, final EventDetector detector,
                                    final boolean increasing) {
            final ImpulseManeuver im = (ImpulseManeuver) detector;
            im.trigger.getHandler().eventOccurred(s, detector, increasing); // Action is ignored but method still called
            return Action.RESET_STATE;
        }

        /** {@inheritDoc} */
        @Override
        public SpacecraftState resetState(final EventDetector detector, final SpacecraftState oldState) {

            final ImpulseManeuver im = (ImpulseManeuver) detector;
            final AbsoluteDate date = oldState.getDate();
            final AttitudeProvider override = im.getAttitudeOverride();
            final boolean isStateOrbitDefined = oldState.isOrbitDefined();
            final Rotation rotation;

            if (override == null) {
                rotation = oldState.getAttitude().getRotation();
            } else {
                rotation = override.getAttitudeRotation(isStateOrbitDefined ? oldState.getOrbit() : oldState.getAbsPVA(),
                        date, oldState.getFrame());
            }

            // convert velocity increment in inertial frame
            final Vector3D deltaV = rotation.applyInverseTo(im.deltaVSat);
            final double sign     = im.forward ? +1 : -1;

            // apply increment to position/velocity
            final PVCoordinates oldPV = oldState.getPVCoordinates();
            final PVCoordinates newPV =
                            new PVCoordinates(oldPV.getPosition(),
                                              new Vector3D(1, oldPV.getVelocity(), sign, deltaV));
            final CartesianOrbit newOrbit =
                    new CartesianOrbit(newPV, oldState.getFrame(), date, oldState.getMu());

            // compute new mass
            final double normDeltaV = im.control3DVectorCostType.evaluate(im.deltaVSat);
            final double newMass = oldState.getMass() * FastMath.exp(-sign * normDeltaV / im.vExhaust);

            // pack everything in a new state
            SpacecraftState newState;
            if (isStateOrbitDefined) {
                newState = new SpacecraftState(oldState.getOrbit().getType().normalize(newOrbit, oldState.getOrbit()),
                        oldState.getAttitude(), newMass);
            } else {
                newState = new SpacecraftState(oldState.getAbsPVA(), oldState.getAttitude(), newMass);
            }
            for (final DoubleArrayDictionary.Entry entry : oldState.getAdditionalStatesValues().getData()) {
                newState = newState.addAdditionalState(entry.getKey(), entry.getValue());
            }
            for (final DoubleArrayDictionary.Entry entry : oldState.getAdditionalStatesDerivatives().getData()) {
                newState = newState.addAdditionalStateDerivative(entry.getKey(), entry.getValue());
            }
            return newState;

        }

    }

}
