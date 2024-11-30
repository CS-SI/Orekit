/* Copyright 2002-2024 Exotrail
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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.ode.events.Action;
import org.hipparchus.util.FastMath;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.orbits.FieldCartesianOrbit;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.events.FieldEventDetectionSettings;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.propagation.events.handlers.FieldEventHandler;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.FieldArrayDictionary;
import org.orekit.utils.FieldPVCoordinates;

/** Impulse maneuver model for propagators working with Fields.
 * <p>This class implements an impulse maneuver as a discrete event
 * that can be provided to any {@link org.orekit.propagation.FieldPropagator
 * Propagator} and mirrors the standard version
 * {@link org.orekit.forces.maneuvers.ImpulseManeuver}.</p>
 * <p>The maneuver is executed when an underlying is triggered, in which case this class will generate a {@link
 * Action#RESET_STATE RESET_STATE} event. By default, the detection settings are those of the trigger.
 * In the simple cases, the underlying event detector may be a basic
 * {@link org.orekit.propagation.events.FieldDateDetector date event}, but it
 * can also be a more elaborate {@link
 * org.orekit.propagation.events.FieldApsideDetector apside event} for apogee
 * maneuvers for example.</p>
 * <p>The maneuver is defined by a single velocity increment.
 * If no AttitudeProvider is given, the current attitude of the spacecraft,
 * defined by the current spacecraft state, will be used as the
 * {@link AttitudeProvider} so the velocity increment should be given in
 * the same pseudoinertial frame as the {@link FieldSpacecraftState} used to
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
 * @see org.orekit.propagation.FieldPropagator#addEventDetector(FieldEventDetector)
 * @see org.orekit.forces.maneuvers.ImpulseManeuver
 * @author Romain Serra
 * @since 12.0
 * @param <T> type of the field elements
 */
public class FieldImpulseManeuver<T extends CalculusFieldElement<T>> implements FieldEventDetector<T> {

    /** The attitude to override during the maneuver, if set. */
    private final AttitudeProvider attitudeOverride;

    /** Triggering event. */
    private final FieldEventDetector<T> trigger;

    /** Velocity increment in satellite frame. */
    private final FieldVector3D<T> deltaVSat;

    /** Specific impulse. */
    private final T isp;

    /** Engine exhaust velocity. */
    private final T vExhaust;

    /** Trigger's detection settings. */
    private final FieldEventDetectionSettings<T> detectionSettings;

    /** Specific event handler. */
    private final Handler<T> handler;

    /** Indicator for forward propagation. */
    private boolean forward;

    /** Type of norm linking delta-V to mass consumption. */
    private final Control3DVectorCostType control3DVectorCostType;

    /** Build a new instance.
     * @param trigger triggering event
     * @param deltaVSat velocity increment in satellite frame
     * @param isp engine specific impulse (s)
     */
    public FieldImpulseManeuver(final FieldEventDetector<T> trigger, final FieldVector3D<T> deltaVSat, final T isp) {
        this(trigger, null, deltaVSat, isp);
    }

    /** Build a new instance.
     * @param trigger triggering event
     * @param attitudeOverride the attitude provider to use for the maneuver
     * @param deltaVSat velocity increment in satellite frame
     * @param isp engine specific impulse (s)
     */
    public FieldImpulseManeuver(final FieldEventDetector<T> trigger, final AttitudeProvider attitudeOverride,
                                final FieldVector3D<T> deltaVSat, final T isp) {
        this(trigger, attitudeOverride, deltaVSat, isp, Control3DVectorCostType.TWO_NORM);
    }

    /** Build a new instance.
     * @param trigger triggering event
     * @param attitudeOverride the attitude provider to use for the maneuver
     * @param deltaVSat velocity increment in satellite frame
     * @param isp engine specific impulse (s)
     * @param control3DVectorCostType increment's norm for mass consumption
     */
    public FieldImpulseManeuver(final FieldEventDetector<T> trigger, final AttitudeProvider attitudeOverride,
                                final FieldVector3D<T> deltaVSat, final T isp,
                                final Control3DVectorCostType control3DVectorCostType) {
        this(trigger, trigger.getDetectionSettings(), attitudeOverride, deltaVSat, isp, control3DVectorCostType);
    }

    /** Private constructor.
     * @param trigger triggering event
     * @param detectionSettings event detection settings
     * @param attitudeOverride the attitude provider to use for the maneuver
     * @param deltaVSat velocity increment in satellite frame
     * @param isp engine specific impulse (s)
     * @param control3DVectorCostType increment's norm for mass consumption
     */
    private FieldImpulseManeuver(final FieldEventDetector<T> trigger,
                                 final FieldEventDetectionSettings<T> detectionSettings,
                                 final AttitudeProvider attitudeOverride, final FieldVector3D<T> deltaVSat, final T isp,
                                 final Control3DVectorCostType control3DVectorCostType) {
        this.trigger = trigger;
        this.detectionSettings = detectionSettings;
        this.deltaVSat = deltaVSat;
        this.isp = isp;
        this.attitudeOverride = attitudeOverride;
        this.control3DVectorCostType = control3DVectorCostType;
        this.vExhaust = this.isp.multiply(Constants.G0_STANDARD_GRAVITY);
        this.handler = new Handler<>();
    }

    /**
     * Creates a copy with different event detection settings.
     * @param eventDetectionSettings new detection settings
     * @return a new detector with same properties except for the detection settings
     */
    public FieldImpulseManeuver<T> withDetectionSettings(final FieldEventDetectionSettings<T> eventDetectionSettings) {
        return new FieldImpulseManeuver<>(trigger, eventDetectionSettings, attitudeOverride, deltaVSat, isp,
                control3DVectorCostType);
    }

    /** {@inheritDoc} */
    @Override
    public void init(final FieldSpacecraftState<T> s0, final FieldAbsoluteDate<T> t) {
        forward = t.durationFrom(s0.getDate()).getReal() >= 0;
        // Initialize the triggering event
        trigger.init(s0, t);
    }

    /** {@inheritDoc} */
    @Override
    public T g(final FieldSpacecraftState<T> fieldSpacecraftState) {
        return trigger.g(fieldSpacecraftState);
    }

    /** {@inheritDoc} */
    @Override
    public void finish(final FieldSpacecraftState<T> state) {
        FieldEventDetector.super.finish(state);
        trigger.finish(state);
    }

    /** {@inheritDoc} */
    @Override
    public FieldEventDetectionSettings<T> getDetectionSettings() {
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
    public FieldEventDetector<T> getTrigger() {
        return trigger;
    }

    /** Get the velocity increment in satellite frame.
     * @return velocity increment in satellite frame
     */
    public FieldVector3D<T> getDeltaVSat() {
        return deltaVSat;
    }

    /** Get the specific impulse.
     * @return specific impulse
     */
    public T getIsp() {
        return isp;
    }

    /** Get the control vector's cost type.
     * @return control cost type
     * @since 12.0
     */
    public Control3DVectorCostType getControl3DVectorCostType() {
        return control3DVectorCostType;
    }

    @Override
    public FieldEventHandler<T> getHandler() {
        return handler;
    }

    /** Local handler. */
    private static class Handler<T extends CalculusFieldElement<T>> implements FieldEventHandler<T> {

        /** {@inheritDoc} */
        @Override
        public Action eventOccurred(final FieldSpacecraftState<T> s,
                                    final FieldEventDetector<T> detector,
                                    final boolean increasing) {
            final FieldImpulseManeuver<T> im = (FieldImpulseManeuver<T>) detector;
            im.trigger.getHandler().eventOccurred(s, detector, increasing); // Action ignored but method still called
            return Action.RESET_STATE;
        }

        /** {@inheritDoc} */
        @Override
        public FieldSpacecraftState<T> resetState(final FieldEventDetector<T> detector,
                                                  final FieldSpacecraftState<T> oldState) {

            final FieldImpulseManeuver<T> im = (FieldImpulseManeuver<T>) detector;
            final FieldAbsoluteDate<T> date = oldState.getDate();
            final boolean isStateOrbitDefined = oldState.isOrbitDefined();
            final FieldRotation<T> rotation;

            if (im.getAttitudeOverride() == null) {
                rotation = oldState.getAttitude().getRotation();
            } else {
                rotation = im.attitudeOverride.getAttitudeRotation(isStateOrbitDefined ? oldState.getOrbit() : oldState.getAbsPVA(),
                        date, oldState.getFrame());
            }

            // convert velocity increment in inertial frame
            final FieldVector3D<T> deltaV = rotation.applyInverseTo(im.deltaVSat);
            final T one = oldState.getMu().getField().getOne();
            final T sign = (im.forward) ? one : one.negate();

            // apply increment to position/velocity
            final FieldPVCoordinates<T> oldPV = oldState.getPVCoordinates();
            final FieldPVCoordinates<T> newPV =
                    new FieldPVCoordinates<>(oldPV.getPosition(),
                            new FieldVector3D<>(one, oldPV.getVelocity(), sign, deltaV));
            final FieldCartesianOrbit<T> newOrbit =
                    new FieldCartesianOrbit<>(newPV, oldState.getFrame(), date, oldState.getMu());

            // compute new mass
            final T normDeltaV = im.control3DVectorCostType.evaluate(im.deltaVSat);
            final T newMass = oldState.getMass().multiply(FastMath.exp(normDeltaV.multiply(sign.negate()).divide(im.vExhaust)));

            // pack everything in a new state
            FieldSpacecraftState<T> newState;
            if (isStateOrbitDefined) {
                newState = new FieldSpacecraftState<>(oldState.getOrbit().getType().normalize(newOrbit,
                        oldState.getOrbit()), oldState.getAttitude(), newMass);
            } else {
                newState = new FieldSpacecraftState<>(oldState.getAbsPVA(), oldState.getAttitude(), newMass);
            }

            for (final FieldArrayDictionary<T>.Entry entry : oldState.getAdditionalStatesValues().getData()) {
                newState = newState.addAdditionalState(entry.getKey(), entry.getValue());
            }
            for (final FieldArrayDictionary<T>.Entry entry : oldState.getAdditionalStatesDerivatives().getData()) {
                newState = newState.addAdditionalStateDerivative(entry.getKey(), entry.getValue());
            }

            return newState;
        }

    }
}
