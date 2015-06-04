/* Copyright 2002-2015 CS Systèmes d'Information
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
package org.orekit.attitudes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;

import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.AbstractDetector;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinatesProvider;

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
 * is handling of maneuver mode.<p>
 * @author Luc Maisonobe
 * @since 5.1
 */
public class AttitudesSequence implements AttitudeProvider {

    /** Serializable UID. */
    private static final long serialVersionUID = 20150603L;

    /** Active provider. */
    private AttitudeProvider active;

    /** Switching events map. */
    private final Map<AttitudeProvider, Collection<Switch<?>>> switchingMap;

    /** Constructor for an initially empty sequence.
     */
    public AttitudesSequence() {
        active       = null;
        switchingMap = new IdentityHashMap<AttitudeProvider, Collection<Switch<?>>>();
    }

    /** Reset the active provider.
     * @param provider provider to activate
     */
    public void resetActiveProvider(final AttitudeProvider provider) {

        // add the provider if not already known
        if (!switchingMap.containsKey(provider)) {
            switchingMap.put(provider, new ArrayList<Switch<?>>());
        }

        active = provider;

    }

    /** Register all wrapped switch events to the propagator.
     * <p>
     * This method must be called once before propagation, after the
     * switching conditions have been set up by calls to {@link
     * #addSwitchingCondition(AttitudeProvider, EventDetector, boolean,
     * boolean, AttitudeProvider, SwitchHandler) addSwitchingCondition}.
     * </p>
     * @param propagator propagator that will handle the events
     */
    public void registerSwitchEvents(final Propagator propagator) {
        for (final Collection<Switch<?>> collection : switchingMap.values()) {
            for (final Switch<?> s : collection) {
                propagator.addEventDetector(s);
            }
        }
    }

    /** Add a switching condition between two attitude providers.
     * <p>
     * This method simply calls {@link #addSwitchingCondition(AttitudeProvider, EventDetector,
     * boolean, boolean, AttitudeProvider, SwitchHandler) addSwitchingCondition} with the
     * switch handler set to null.
     * </p>
     * @param past attitude provider applicable for times in the switch event occurrence past
     * @param switchEvent event triggering the attitude providers switch
     * @param switchOnIncrease if true, switch is triggered on increasing event
     * @param switchOnDecrease if true, switch is triggered on decreasing event
     * @param future attitude provider applicable for times in the switch event occurrence future
     * @param <T> class type for the generic version
     */
    public <T extends EventDetector> void addSwitchingCondition(final AttitudeProvider past,
                                                                final T switchEvent,
                                                                final boolean switchOnIncrease,
                                                                final boolean switchOnDecrease,
                                                                final AttitudeProvider future) {
        addSwitchingCondition(past, switchEvent, switchOnIncrease, switchOnDecrease, future, null);
    }

    /** Add a switching condition between two attitude providers.
     * <p>
     * The {@code past} and {@code future} attitude providers are defined with regard
     * to the natural flow of time. This means that if the propagation is forward, the
     * propagator will switch from {@code past} provider to {@code future} provider at
     * event occurrence, but if the propagation is backward, the propagator will switch
     * from {@code future} provider to {@code past} provider at event occurrence. The
     * switch condition is therefore defined independently of the propagation direction
     * that will be used.
     * </p>
     * <p>
     * An attitude provider may have several different switch events associated to
     * it. Depending on which event is triggered, the appropriate provider is
     * switched to.
     * </p>
     * <p>
     * The switch events specified here must <em>not</em> be registered to the
     * propagator directly. The proper way to register these events is to
     * call {@link #registerSwitchEvents(Propagator)} once after all switching
     * conditions have been set up. The reason for this is that the events will
     * be wrapped before being registered.
     * </p>
     * <p>
     * If the underlying detector has an event handler associated to it, this handler
     * will be triggered (i.e. its {@link EventHandler#eventOccurred(SpacecraftState,
     * EventDetector, boolean) eventOccurred} method will be called), <em>regardless</em>
     * of the event really triggering an attitude switch or not. As an example, suppose an
     * eclipse detector is used to switch from day to night attitude mode when entering
     * eclipse, with {@code switchOnIncrease} set to {@code false} and {@code switchOnDecrease}
     * set to {@code true}. Then the handler would be triggered at both eclipse entry and eclipse
     * exit, but attitude switch would occur <em>only</em> at eclipse entry.
     * </p>
     * @param past attitude provider applicable for times in the switch event occurrence past
     * @param switchEvent event triggering the attitude providers switch
     * @param switchOnIncrease if true, switch is triggered on increasing event
     * @param switchOnDecrease if true, switch is triggered on decreasing event
     * @param future attitude provider applicable for times in the switch event occurrence future
     * @param handler handler to call for notifying when switch occurs (may be null)
     * @param <T> class type for the generic version
     * @since 7.1
     */
    public <T extends EventDetector> void addSwitchingCondition(final AttitudeProvider past,
                                                                final T switchEvent,
                                                                final boolean switchOnIncrease,
                                                                final boolean switchOnDecrease,
                                                                final AttitudeProvider future,
                                                                final SwitchHandler handler) {

        // add the providers if not already known
        if (!switchingMap.containsKey(past)) {
            switchingMap.put(past, new ArrayList<Switch<?>>());
        }
        if (!switchingMap.containsKey(future)) {
            switchingMap.put(future, new ArrayList<Switch<?>>());
        }

        // if it is the first switching condition, assume first active law is the past
        if (active == null) {
            active = past;
        }

        // add the switching condition
        switchingMap.get(past).add(new Switch<T>(switchEvent,
                                                 switchOnIncrease, switchOnDecrease,
                                                 past, future, handler));

    }

    /** {@inheritDoc} */
    public Attitude getAttitude(final PVCoordinatesProvider pvProv,
                                final AbsoluteDate date, final Frame frame)
        throws OrekitException {
        // delegate attitude computation to the active provider
        return active.getAttitude(pvProv, date, frame);
    }

    /** Switch specification.
     * @param <T> class type for the generic version
     */
    private class Switch<T extends EventDetector> extends AbstractDetector<Switch<T>> {

        /** Serializable UID. */
        private static final long serialVersionUID = 20141228L;

        /** Event. */
        private final T event;

        /** Event direction triggering the switch. */
        private final boolean switchOnIncrease;

        /** Event direction triggering the switch. */
        private final boolean switchOnDecrease;

        /** Attitude provider applicable for times in the switch event occurrence past. */
        private final AttitudeProvider past;

        /** Attitude provider applicable for times in the switch event occurrence future. */
        private final AttitudeProvider future;

        /** Attitude provider applicable preceding the switch. */
        private AttitudeProvider preceding;

        /** Attitude provider applicable following the switch. */
        private AttitudeProvider following;

        /** Handler to call for notifying when switch occurs (may be null). */
        private final SwitchHandler switchHandler;

        /** Simple constructor.
         * @param event event
         * @param switchOnIncrease if true, switch is triggered on increasing event
         * @param switchOnDecrease if true, switch is triggered on decreasing event
         * otherwise switch is triggered on decreasing event
         * @param past attitude provider applicable for times in the switch event occurrence past
         * @param future attitude provider applicable for times in the switch event occurrence future
         * @param switchHandler handler to call for notifying when switch occurs (may be null)
         */
        public Switch(final T event,
                      final boolean switchOnIncrease, final boolean switchOnDecrease,
                      final AttitudeProvider past, final AttitudeProvider future,
                      final SwitchHandler switchHandler) {
            this(event.getMaxCheckInterval(), event.getThreshold(), event.getMaxIterationCount(),
                 new LocalHandler<T>(), event, switchOnIncrease, switchOnDecrease, past, future,
                 switchHandler);
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
         * @param event event
         * @param switchOnIncrease if true, switch is triggered on increasing event
         * @param switchOnDecrease if true, switch is triggered on decreasing event
         * otherwise switch is triggered on decreasing event
         * @param past attitude provider applicable for times in the switch event occurrence past
         * @param future attitude provider applicable for times in the switch event occurrence future
         * @param switchHandler handler to call for notifying when switch occurs (may be null)
         * @since 6.1
         */
        private Switch(final double maxCheck, final double threshold,
                       final int maxIter, final EventHandler<Switch<T>> handler, final T event,
                       final boolean switchOnIncrease, final boolean switchOnDecrease,
                       final AttitudeProvider past, final AttitudeProvider future,
                       final SwitchHandler switchHandler) {
            super(maxCheck, threshold, maxIter, handler);
            this.event            = event;
            this.switchOnIncrease = switchOnIncrease;
            this.switchOnDecrease = switchOnDecrease;
            this.past             = past;
            this.future           = future;
            this.switchHandler    = switchHandler;
            this.preceding        = null;
            this.following        = null;
        }

        /** {@inheritDoc} */
        @Override
        protected Switch<T> create(final double newMaxCheck, final double newThreshold,
                                   final int newMaxIter, final EventHandler<Switch<T>> newHandler) {
            return new Switch<T>(newMaxCheck, newThreshold, newMaxIter, newHandler,
                                 event, switchOnIncrease, switchOnDecrease, past, future, switchHandler);
        }

        /** {@inheritDoc} */
        public void init(final SpacecraftState s0, final AbsoluteDate t) {
            if (t.durationFrom(s0.getDate()) >= 0.0) {
                preceding = past;
                following = future;
            } else {
                preceding = future;
                following = past;
            }
            event.init(s0, t);
        }

        /** {@inheritDoc} */
        public double g(final SpacecraftState s)
            throws OrekitException {
            return event.g(s);
        }

    }

    /** Local handler.
     * @param <T> class type for the generic version
     */
    private class LocalHandler<T extends EventDetector> implements EventHandler<Switch<T>> {

        /** {@inheritDoc} */
        public EventHandler.Action eventOccurred(final SpacecraftState s, final Switch<T> sw, final boolean increasing)
            throws OrekitException {

            if (active == sw.preceding &&
                ((increasing && sw.switchOnIncrease) || (!increasing && sw.switchOnDecrease))) {

                // switch to next attitude provider
                if (sw.switchHandler != null) {
                    sw.switchHandler.switchOccurred(sw.preceding, sw.following, s);
                }
                active = sw.following;

            }

            // trigger the underlying event regardless of it really triggering a switch or not
            return sw.event.eventOccurred(s, increasing);

        }

        /** {@inheritDoc} */
        @Override
        public SpacecraftState resetState(final Switch<T> sw, final SpacecraftState oldState)
            throws OrekitException {
            return sw.event.resetState(oldState);
        }

    }

    /** Interface for attitude switch notifications.
     * <p>
     * This interface is intended to be implemented by users who want to be
     * notified when an attitude switch occurs.
     * </p>
     * @since 7.1
     */
    public interface SwitchHandler {

        /** Method called when attitude is switched from one law to another law.
         * @param preceding attitude law used preceding the switch (i.e. in the past
         * of the switch event for a forward propagation, or in the future
         * of the switch event for a backward propagation)
         * @param following attitude law used following the switch (i.e. in the future
         * of the switch event for a forward propagation, or in the past
         * of the switch event for a backward propagation)
         * @param state state at switch time (with attitude computed using the {@code preceding} law)
         * @exception OrekitException if some unexpected condition occurs
         */
        void switchOccurred(AttitudeProvider preceding, AttitudeProvider following, SpacecraftState state)
            throws OrekitException;

    }

}
