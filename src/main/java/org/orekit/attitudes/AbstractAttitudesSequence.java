/* Copyright 2022-2024 Romain Serra
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
package org.orekit.attitudes;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.ode.events.Action;
import org.orekit.frames.Frame;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.AdapterDetector;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.propagation.events.FieldEventDetectionSettings;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.events.handlers.FieldEventHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldPVCoordinatesProvider;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.TimeSpanMap;

/** This classes manages a sequence of different attitude providers that are activated
 * in turn according to switching events.
 * <p>Only one attitude provider in the sequence is in an active state. When one of
 * the switch event associated with the active provider occurs, the active provider becomes
 * the one specified with the event. A simple example is a provider for the sun lighted part
 * of the orbit and another provider for the eclipse time. When the sun lighted provider is active,
 * the eclipse entry event is checked and when it occurs the eclipse provider is activated.
 * When the eclipse provider is active, the eclipse exit event is checked and when it occurs
 * the sun lighted provider is activated again. This sequence is a simple loop.</p>
 * <p>An active attitude provider may have several switch events and next provider settings, leading
 * to different activation patterns depending on which events are triggered first. An example
 * of this feature is handling switches to safe mode if some contingency condition is met, in
 * addition to the nominal switches that correspond to proper operations. Another example
 * is handling of maneuver mode.
 * <p>
 * Note that this attitude provider is stateful, it keeps in memory the sequence of active
 * underlying providers with their switch dates and the transitions from one provider to
 * the other. This implies that this provider should <em>not</em> be shared among different
 * propagators at the same time, each propagator should use its own instance of this provider.
 * <p>
 * The sequence kept in memory is reset when {@link #resetActiveProvider(AttitudeProvider)}
 * is called, and only the specify provider is kept. The sequence is also partially
 * reset each time a propagation starts. If a new propagation is started after a first
 * propagation has been run, all the already computed switches that occur after propagation
 * start for forward propagation or before propagation start for backward propagation will
 * be erased. New switches will be computed and applied properly according to the new
 * propagation settings. The already computed switches that are not in covered are kept
 * in memory. This implies that if a propagation is interrupted and restarted in the
 * same direction, then attitude switches will remain in place, ensuring that even if the
 * interruption occurred in the middle of an attitude transition the second propagation will
 * properly complete the transition that was started by the first propagator.
 * </p>
 * @author Luc Maisonobe
 * @author Romain Serra
 * @since 13.0
 */
abstract class AbstractAttitudesSequence implements AttitudeProvider {

    /** Providers that have been activated. */
    private TimeSpanMap<AttitudeProvider> activated;

    /** Constructor for an initially empty sequence.
     */
    protected AbstractAttitudesSequence() {
        activated = null;
    }

    /** Reset the active provider.
     * <p>
     * Calling this method clears all already seen switch history,
     * so it should <em>not</em> be used during the propagation itself,
     * it is intended to be used only at start
     * </p>
     * @param provider provider to activate
     */
    public void resetActiveProvider(final AttitudeProvider provider) {
        activated = new TimeSpanMap<>(provider);
    }

    /**
     * Setter for map of activate attitude providers.
     * @param activated new map
     */
    protected void setActivated(final TimeSpanMap<AttitudeProvider> activated) {
        this.activated = activated;
    }

    /**
     * Getter for map of activated attitude providers.
     * @return map of providers
     */
    protected TimeSpanMap<AttitudeProvider> getActivated() {
        return activated;
    }

    /** {@inheritDoc} */
    @Override
    public Attitude getAttitude(final PVCoordinatesProvider pvProv,
                                final AbsoluteDate date, final Frame frame) {
        return activated.get(date).getAttitude(pvProv, date, frame);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldAttitude<T> getAttitude(final FieldPVCoordinatesProvider<T> pvProv,
                                                                            final FieldAbsoluteDate<T> date,
                                                                            final Frame frame) {
        return activated.get(date.toAbsoluteDate()).getAttitude(pvProv, date, frame);
    }

    /** {@inheritDoc} */
    @Override
    public Rotation getAttitudeRotation(final PVCoordinatesProvider pvProv, final AbsoluteDate date, final Frame frame) {
        return activated.get(date).getAttitudeRotation(pvProv, date, frame);
    }

    @Override
    public <T extends CalculusFieldElement<T>> FieldRotation<T> getAttitudeRotation(final FieldPVCoordinatesProvider<T> pvProv,
                                                                                    final FieldAbsoluteDate<T> date,
                                                                                    final Frame frame) {
        return activated.get(date.toAbsoluteDate()).getAttitudeRotation(pvProv, date, frame);
    }

    /**
     * Method creating a Field attitude switch from a non-Field one.
     * @param field field
     * @param attitudeSwitch attitude switch
     * @return Field detector
     * @param <T> field type
     */
    protected <T extends CalculusFieldElement<T>> FieldEventDetector<T> getFieldEventDetector(final Field<T> field,
                                                                                              final AbstractAttitudeSwitch attitudeSwitch) {
        return new FieldEventDetector<T>() {

            /** {@inheritDoc} */
            @Override
            public void init(final FieldSpacecraftState<T> s0, final FieldAbsoluteDate<T> t) {
                attitudeSwitch.init(s0.toSpacecraftState(), t.toAbsoluteDate());
            }

            /** {@inheritDoc} */
            @Override
            public T g(final FieldSpacecraftState<T> s) {
                return field.getZero().newInstance(attitudeSwitch.g(s.toSpacecraftState()));
            }

            @Override
            public FieldEventDetectionSettings<T> getDetectionSettings() {
                return new FieldEventDetectionSettings<>(field, attitudeSwitch.getDetectionSettings());
            }

            /** {@inheritDoc} */
            @Override
            public FieldEventHandler<T> getHandler() {
                return new FieldEventHandler<T>() {
                    /** {@inheritDoc} */
                    @Override
                    public Action eventOccurred(final FieldSpacecraftState<T> s,
                                                final FieldEventDetector<T> detector,
                                                final boolean increasing) {
                        return attitudeSwitch.eventOccurred(s.toSpacecraftState(), attitudeSwitch, increasing);
                    }

                    /** {@inheritDoc} */
                    @Override
                    public FieldSpacecraftState<T> resetState(final FieldEventDetector<T> detector,
                                                              final FieldSpacecraftState<T> oldState) {
                        return new FieldSpacecraftState<>(field, attitudeSwitch.resetState(attitudeSwitch, oldState.toSpacecraftState()));
                    }
                };
            }

        };
    }

    /** Abstract class to manage attitude switches. */
    abstract static class AbstractAttitudeSwitch extends AdapterDetector implements EventHandler {

        /**
         * Event direction triggering the switch.
         */
        private final boolean switchOnIncrease;

        /**
         * Event direction triggering the switch.
         */
        private final boolean switchOnDecrease;

        /**
         * Attitude provider applicable for times in the switch event occurrence past.
         */
        private final AttitudeProvider past;

        /**
         * Attitude provider applicable for times in the switch event occurrence future.
         */
        private final AttitudeProvider future;

        /**
         * Handler to call for notifying when switch occurs (may be null).
         */
        private final AttitudeSwitchHandler switchHandler;

        /**
         * Simple constructor.
         *
         * @param event            event
         * @param switchOnIncrease if true, switch is triggered on increasing event
         * @param switchOnDecrease if true, switch is triggered on decreasing event otherwise switch is triggered on
         *                         decreasing event
         * @param past             attitude provider applicable for times in the switch event occurrence past
         * @param future           attitude provider applicable for times in the switch event occurrence future
         * @param switchHandler    handler to call for notifying when switch occurs (may be null)
         */
        protected AbstractAttitudeSwitch(final EventDetector event, final boolean switchOnIncrease,
                                         final boolean switchOnDecrease, final AttitudeProvider past,
                                         final AttitudeProvider future, final AttitudeSwitchHandler switchHandler) {
            super(event);
            this.switchOnIncrease = switchOnIncrease;
            this.switchOnDecrease = switchOnDecrease;
            this.past = past;
            this.future = future;
            this.switchHandler = switchHandler;
        }

        /**
         * Protected getter for switch handle.
         * @return switch handler
         */
        protected AttitudeSwitchHandler getSwitchHandler() {
            return switchHandler;
        }

        /**
         * Protected getter for future attitude provider.
         * @return future provider
         */
        protected AttitudeProvider getFuture() {
            return future;
        }

        /**
         * Protected getter for past attitude provider.
         * @return pas provider
         */
        protected AttitudeProvider getPast() {
            return past;
        }

        /**
         * Protected getter for switch-on-decrease flag.
         * @return flag
         */
        protected boolean isSwitchOnDecrease() {
            return switchOnDecrease;
        }

        /**
         * Protected getter for switch-on-increase flag.
         * @return flag
         */
        protected boolean isSwitchOnIncrease() {
            return switchOnIncrease;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public EventHandler getHandler() {
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public SpacecraftState resetState(final EventDetector detector, final SpacecraftState oldState) {
            // delegate to underlying event
            return getDetector().getHandler().resetState(getDetector(), oldState);
        }

    }

}
