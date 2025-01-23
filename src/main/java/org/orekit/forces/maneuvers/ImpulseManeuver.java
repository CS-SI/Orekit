/* Copyright 2002-2025 CS GROUP
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
import org.orekit.forces.maneuvers.propulsion.ThrustPropulsionModel;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.DetectorModifier;
import org.orekit.propagation.events.EventDetectionSettings;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.AbsolutePVCoordinates;
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
 * <p>The maneuver velocity increment is defined via {@link ImpulseProvider}.
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
public class ImpulseManeuver extends AbstractImpulseManeuver implements DetectorModifier {

    /** Triggering event. */
    private final EventDetector trigger;

    /** Specific impulse. */
    private final double isp;

    /** Engine exhaust velocity. */
    private final double vExhaust;

    /** Trigger's detection settings. */
    private final EventDetectionSettings detectionSettings;

    /** Specific event handler. */
    private final Handler handler;

    /** Impulse provider. */
    private final ImpulseProvider impulseProvider;

    /** Indicator for forward propagation. */
    private boolean forward;

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
        this(trigger, attitudeOverride, ImpulseProvider.of(deltaVSat), isp, Control3DVectorCostType.TWO_NORM);
    }

    /** Build a new instance.
     * @param trigger triggering event
     * @param attitudeOverride the attitude provider to use for the maneuver
     * @param deltaVSat velocity increment in satellite frame
     * @param isp engine specific impulse (s)
     * @param control3DVectorCostType increment's norm for mass consumption
     * @deprecated since 13.0
     */
    @Deprecated
    public ImpulseManeuver(final EventDetector trigger, final AttitudeProvider attitudeOverride,
                           final Vector3D deltaVSat, final double isp, final Control3DVectorCostType control3DVectorCostType) {
        this(trigger, trigger.getDetectionSettings(), attitudeOverride, ImpulseProvider.of(deltaVSat), isp, control3DVectorCostType);
    }

    /** Build a new instance.
     * @param trigger triggering event
     * @param attitudeOverride the attitude provider to use for the maneuver
     * @param impulseProvider impulse provider
     * @param isp engine specific impulse (s)
     * @param control3DVectorCostType increment's norm for mass consumption
     * @since 13.0
     */
    public ImpulseManeuver(final EventDetector trigger, final AttitudeProvider attitudeOverride,
                           final ImpulseProvider impulseProvider, final double isp, final Control3DVectorCostType control3DVectorCostType) {
        this(trigger, trigger.getDetectionSettings(), attitudeOverride, impulseProvider, isp, control3DVectorCostType);
    }

    /** Private constructor.
     * @param trigger triggering event
     * @param detectionSettings event detection settings
     * @param attitudeOverride the attitude provider to use for the maneuver
     * @param impulseProvider impulse provider
     * @param isp engine specific impulse (s)
     * @param control3DVectorCostType increment's norm for mass consumption
     * @since 13.0
     */
    private ImpulseManeuver(final EventDetector trigger, final EventDetectionSettings detectionSettings,
                            final AttitudeProvider attitudeOverride, final ImpulseProvider impulseProvider,
                            final double isp, final Control3DVectorCostType control3DVectorCostType) {
        super(attitudeOverride, control3DVectorCostType);
        this.trigger   = trigger;
        this.detectionSettings = detectionSettings;
        this.impulseProvider = impulseProvider;
        this.isp       = isp;
        this.vExhaust  = ThrustPropulsionModel.getExhaustVelocity(isp);
        this.handler = new Handler();
    }

    /**
     * Creates a copy with different event detection settings.
     * @param eventDetectionSettings new detection settings
     * @return a new detector with same properties except for the detection settings
     */
    public ImpulseManeuver withDetectionSettings(final EventDetectionSettings eventDetectionSettings) {
        return new ImpulseManeuver(trigger, eventDetectionSettings, getAttitudeOverride(), impulseProvider, isp,
                getControl3DVectorCostType());
    }

    /** {@inheritDoc} */
    @Override
    public void init(final SpacecraftState s0, final AbsoluteDate t) {
        DetectorModifier.super.init(s0, t);
        forward = t.durationFrom(s0.getDate()) >= 0;
        impulseProvider.init(s0, t);
    }

    /** {@inheritDoc} */
    @Override
    public void finish(final SpacecraftState state) {
        DetectorModifier.super.finish(state);
        impulseProvider.finish(state);
    }

    /** {@inheritDoc} */
    @Override
    public EventDetector getDetector() {
        return getTrigger();
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

    /** Get the triggering event.
     * @return triggering event
     */
    public EventDetector getTrigger() {
        return trigger;
    }

    /**
     * Getter for the impulse provider.
     * @return impulse provider
     * @since 13.0
     */
    public ImpulseProvider getImpulseProvider() {
        return impulseProvider;
    }

    /** Get the specific impulse.
    * @return specific impulse
    */
    public double getIsp() {
        return isp;
    }

    /** Local handler. */
    private static class Handler implements EventHandler {

        /** {@inheritDoc} */
        public Action eventOccurred(final SpacecraftState s, final EventDetector detector,
                                    final boolean increasing) {
            final ImpulseManeuver im = (ImpulseManeuver) detector;
            im.trigger.getHandler().eventOccurred(s, im.trigger, increasing); // Action is ignored but method still called
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
            final Vector3D deltaVSat = im.impulseProvider.getImpulse(oldState, im.forward);
            final Vector3D deltaV = rotation.applyInverseTo(deltaVSat);

            // apply increment to position/velocity
            final PVCoordinates oldPV = oldState.getPVCoordinates();
            final Vector3D newVelocity = oldPV.getVelocity().add(deltaV);
            final PVCoordinates newPV = new PVCoordinates(oldPV.getPosition(), newVelocity);

            // compute new mass
            final double normDeltaV = im.getControl3DVectorCostType().evaluate(deltaVSat);
            final double sign     = im.forward ? +1 : -1;
            final double newMass = oldState.getMass() * FastMath.exp(-sign * normDeltaV / im.vExhaust);

            // pack everything in a new state
            if (oldState.isOrbitDefined()) {
                final CartesianOrbit newOrbit = new CartesianOrbit(newPV, oldState.getFrame(), oldState.getDate(),
                        oldState.getOrbit().getMu());
                return new SpacecraftState(oldState.getOrbit().getType().normalize(newOrbit, oldState.getOrbit()),
                        oldState.getAttitude(), newMass, oldState.getAdditionalStatesValues(), oldState.getAdditionalStatesDerivatives());
            } else {
                final AbsolutePVCoordinates newAPV = new AbsolutePVCoordinates(oldState.getFrame(), oldState.getDate(),
                        newPV);
                return new SpacecraftState(newAPV, oldState.getAttitude(), newMass,
                        oldState.getAdditionalStatesValues(), oldState.getAdditionalStatesDerivatives());
            }
        }

    }

}
