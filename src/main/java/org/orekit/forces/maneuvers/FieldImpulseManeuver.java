/* Copyright 2002-2025 Exotrail
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
import org.orekit.orbits.FieldOrbit;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.events.FieldDetectorModifier;
import org.orekit.propagation.events.FieldEventDetectionSettings;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.propagation.events.handlers.FieldEventHandler;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.FieldAbsolutePVCoordinates;
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
 * <p>The maneuver velocity increment is defined via {@link FieldImpulseProvider}.
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
public class FieldImpulseManeuver<T extends CalculusFieldElement<T>> extends AbstractImpulseManeuver
        implements FieldDetectorModifier<T> {

    /** Triggering event. */
    private final FieldEventDetector<T> trigger;

    /** Specific impulse. */
    private final T isp;

    /** Engine exhaust velocity. */
    private final T vExhaust;

    /** Trigger's detection settings. */
    private final FieldEventDetectionSettings<T> detectionSettings;

    /** Specific event handler. */
    private final Handler<T> handler;

    /** Field impulse provider. */
    private final FieldImpulseProvider<T> fieldImpulseProvider;

    /** Indicator for forward propagation. */
    private boolean forward;

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
        this(trigger, attitudeOverride, FieldImpulseProvider.of(deltaVSat), isp, Control3DVectorCostType.TWO_NORM);
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
    public FieldImpulseManeuver(final FieldEventDetector<T> trigger, final AttitudeProvider attitudeOverride,
                                final FieldVector3D<T> deltaVSat, final T isp,
                                final Control3DVectorCostType control3DVectorCostType) {
        this(trigger, trigger.getDetectionSettings(), attitudeOverride,
                FieldImpulseProvider.of(deltaVSat), isp, control3DVectorCostType);
    }

    /** Build a new instance.
     * @param trigger triggering event
     * @param attitudeOverride the attitude provider to use for the maneuver
     * @param fieldImpulseProvider impulse provider
     * @param isp engine specific impulse (s)
     * @param control3DVectorCostType increment's norm for mass consumption
     */
    public FieldImpulseManeuver(final FieldEventDetector<T> trigger, final AttitudeProvider attitudeOverride,
                                final FieldImpulseProvider<T> fieldImpulseProvider, final T isp,
                                final Control3DVectorCostType control3DVectorCostType) {
        this(trigger, trigger.getDetectionSettings(), attitudeOverride, fieldImpulseProvider, isp, control3DVectorCostType);
    }

    /** Private constructor.
     * @param trigger triggering event
     * @param detectionSettings event detection settings
     * @param attitudeOverride the attitude provider to use for the maneuver
     * @param fieldImpulseProvider impulse provider
     * @param isp engine specific impulse (s)
     * @param control3DVectorCostType increment's norm for mass consumption
     */
    private FieldImpulseManeuver(final FieldEventDetector<T> trigger,
                                 final FieldEventDetectionSettings<T> detectionSettings,
                                 final AttitudeProvider attitudeOverride, final FieldImpulseProvider<T> fieldImpulseProvider,
                                 final T isp, final Control3DVectorCostType control3DVectorCostType) {
        super(attitudeOverride, control3DVectorCostType);
        this.trigger = trigger;
        this.detectionSettings = detectionSettings;
        this.fieldImpulseProvider = fieldImpulseProvider;
        this.isp = isp;
        this.vExhaust = this.isp.multiply(Constants.G0_STANDARD_GRAVITY);
        this.handler = new Handler<>();
    }

    /**
     * Creates a copy with different event detection settings.
     * @param eventDetectionSettings new detection settings
     * @return a new detector with same properties except for the detection settings
     */
    public FieldImpulseManeuver<T> withDetectionSettings(final FieldEventDetectionSettings<T> eventDetectionSettings) {
        return new FieldImpulseManeuver<>(trigger, eventDetectionSettings, getAttitudeOverride(), fieldImpulseProvider, isp,
                getControl3DVectorCostType());
    }

    /** {@inheritDoc} */
    @Override
    public void init(final FieldSpacecraftState<T> s0, final FieldAbsoluteDate<T> t) {
        FieldDetectorModifier.super.init(s0, t);
        forward = t.durationFrom(s0.getDate()).getReal() >= 0;
        fieldImpulseProvider.init(s0, t);
    }

    /** {@inheritDoc} */
    @Override
    public void finish(final FieldSpacecraftState<T> state) {
        FieldDetectorModifier.super.finish(state);
        fieldImpulseProvider.finish(state);
    }

    /** {@inheritDoc} */
    @Override
    public FieldEventDetectionSettings<T> getDetectionSettings() {
        return detectionSettings;
    }

    /** {@inheritDoc} */
    @Override
    public FieldEventDetector<T> getDetector() {
        return trigger;
    }

    /** Get the triggering event.
     * @return triggering event
     */
    public FieldEventDetector<T> getTrigger() {
        return getDetector();
    }

    /**
     * Getter for the impulse provider.
     * @return impulse provider
     * @since 13.0
     */
    public FieldImpulseProvider<T> getFieldImpulseProvider() {
        return fieldImpulseProvider;
    }

    /** Get the specific impulse.
     * @return specific impulse
     */
    public T getIsp() {
        return isp;
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
            im.trigger.getHandler().eventOccurred(s, im.trigger, increasing); // Action ignored but method still called
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
                rotation = im.getAttitudeOverride().getAttitudeRotation(isStateOrbitDefined ? oldState.getOrbit() : oldState.getAbsPVA(),
                        date, oldState.getFrame());
            }

            // convert velocity increment in inertial frame
            final FieldVector3D<T> deltaVSat = im.fieldImpulseProvider.getImpulse(oldState, im.forward);
            final FieldVector3D<T> deltaV = rotation.applyInverseTo(deltaVSat);
            final T one = oldState.getMass().getField().getOne();
            final T sign = (im.forward) ? one : one.negate();

            // apply increment to position/velocity
            final FieldPVCoordinates<T> oldPV = oldState.getPVCoordinates();
            final FieldVector3D<T> newVelocity = oldPV.getVelocity().add(deltaV);
            final FieldPVCoordinates<T> newPV = new FieldPVCoordinates<>(oldPV.getPosition(), newVelocity);

            // compute new mass
            final T normDeltaV = im.getControl3DVectorCostType().evaluate(deltaVSat);
            final T newMass = oldState.getMass().multiply(FastMath.exp(normDeltaV.multiply(sign.negate()).divide(im.vExhaust)));

            // pack everything in a new state
            if (oldState.isOrbitDefined()) {
                final FieldOrbit<T> newOrbit = new FieldCartesianOrbit<>(newPV, oldState.getFrame(), oldState.getDate(),
                        oldState.getOrbit().getMu());
                return new FieldSpacecraftState<>(oldState.getOrbit().getType().normalize(newOrbit, oldState.getOrbit()),
                        oldState.getAttitude(), newMass, oldState.getAdditionalStatesValues(),
                        oldState.getAdditionalStatesDerivatives());
            } else {
                final FieldAbsolutePVCoordinates<T> newAbsPVA = new FieldAbsolutePVCoordinates<>(oldState.getFrame(),
                        oldState.getDate(), newPV);
                return new FieldSpacecraftState<>(newAbsPVA, oldState.getAttitude(), newMass,
                        oldState.getAdditionalStatesValues(), oldState.getAdditionalStatesDerivatives());
            }
        }

    }
}
