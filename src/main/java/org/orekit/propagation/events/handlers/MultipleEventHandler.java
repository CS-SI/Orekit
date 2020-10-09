/* Copyright 2013 Applied Defense Solutions, Inc.
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
package org.orekit.propagation.events.handlers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.hipparchus.ode.events.Action;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.AbstractDetector;
import org.orekit.propagation.events.EventDetector;
import org.orekit.time.AbsoluteDate;

/**
 * Facade handlers that allows to use several handlers for one detector.
 * Otherwise, the use of several detectors, each associated with one handler, that detect 
 * the same event can lead to non-deterministic behaviour.
 * This handler manages several handlers. The action returned is based on a priority rule 
 * (see {@link #eventOccurred}) :
 * {@link Action#STOP stop} > {@link Action#RESET_STATE resetState} > {@link Action#RESET_DERIVATIVES resetDerivatives} > {@link Action#RESET_EVENTS resetRevents} > {@link Action#CONTINUE continue}
 *
 * @author Lara Hué
 *
 * @param <D> object type of the detector which is linked to the handler
 * @since 10.3
 */
public class MultipleEventHandler<D extends EventDetector> implements EventHandler<D> {

    /** Default list of handlers for event overrides. */
    private List<EventHandler<D>> handlers = new ArrayList<>();

    /** List of handlers whose Action returned is RESET_STATE. */
    private List<EventHandler<D>> resetStateHandlers = new ArrayList<>();

    /** Initialize event handler at the start of a propagation.
     * <p>
     * This method is called once at the start of the propagation. It
     * may be used by the event handler to initialize some internal data
     * if needed.
     * </p>
     * <p>
     * The default implementation does nothing
     * </p>
     * <p>
     * All handlers' init methods are successively called, the order method is 
     * the order in which handlers are added
     * </p>     
     * @param initialState initial state
     * @param target target date for the propagation
     *
     */
    @Override
    public void init(SpacecraftState initialState, AbsoluteDate target) {
        handlers.forEach(handler -> handler.init(initialState, target));
    }

    /**
     * eventOccurred method mirrors the same interface method as in {@link EventDetector}
     * and its subclasses, but with an additional parameter that allows the calling
     * method to pass in an object from the detector which would have potential
     * additional data to allow the implementing class to determine the correct
     * return state.
     * 
     * The MultipleEventHandler class implies a different behaviour on event detections 
     * than with other handlers :
     * Without the MultipleEventHandler, there is a total order on event occurrences. 
     * Handlers H1, H2, ... that are associated with different instances of {@link AbstractDetector} 
     * are successively called and Action from H1 can prevent H2 from happening if H1 returned {@link Action#RESET_STATE resetState}. 
     * With the MultipleEventHandler class, when event E occurs, all methods eventOccurred
     * of Handlers H1, H2... from MultiEventHandler attributes are called, then Action is decided.
     *
     * @param s SpaceCraft state to be used in the evaluation
     * @param detector object with appropriate type that can be used in determining correct return state
     * @param increasing with the event occurred in an "increasing" or "decreasing" slope direction
     * @return the Action that the calling detector should pass back to the evaluation system
     *
     */
    @Override
    public Action eventOccurred(SpacecraftState s, D detector, boolean increasing) {
        Map<EventHandler<D>, Action> actions = handlers.stream().collect(Collectors.toMap(Function.identity(), handler -> handler.eventOccurred(s, detector, increasing)));

        if (actions.containsValue(Action.STOP)) {
            return Action.STOP;
        }

        if (actions.containsValue(Action.RESET_STATE)) {
            resetStateHandlers = actions.entrySet().stream().filter(entry -> Action.RESET_STATE.equals(entry.getValue())).map(Map.Entry::getKey).collect(Collectors.toList());
            return Action.RESET_STATE;
        }

        if (actions.containsValue(Action.RESET_DERIVATIVES)) {
            return Action.RESET_DERIVATIVES;
        }

        if (actions.containsValue(Action.RESET_EVENTS)) {
            return Action.RESET_EVENTS;
        }

        return Action.CONTINUE;
    }

    /** Reset the state prior to continue propagation.
     * <p>This method is called after the step handler has returned and
     * before the next step is started, but only when {@link
     * #eventOccurred} has itself returned the {@link Action#RESET_STATE resetState} 
     * for at least one handler of the list, except if one handler has return {@link Action#STOP stop}, 
     * see priorities in {@link #eventOccurred}
     * It allows the user to reset the state for the next step,
     * without perturbing the step handler of the finishing step. If the
     * {@link #eventOccurred} never returns the {@link Action#RESET_STATE resetState}
     * indicator, this function will never be called, and it is safe to simply return null.</p>
     * <p>
     * The default implementation simply return its argument.
     * </p>
     * <p> 
     * All handlers that return {@link Action#RESET_STATE resetState} when calling {@link #eventOccurred} are
     * saved in resetStateHandlers. Their methods resetState are successively called.
     * The order for calling resetState methods is the order in which handlers are added.
     * </p>
     * @param detector object with appropriate type that can be used in determining correct return state
     * @param oldState old state
     * @return new state
     */
    @Override
    public SpacecraftState resetState(D detector, SpacecraftState oldState) {
        SpacecraftState newState = oldState;
        for (EventHandler<D> handler : resetStateHandlers) {
            newState = handler.resetState(detector, newState);
        }
        return newState;
    }

    /** Add one handler to the managed handlers list
     * @param handler handler associated with D detector
     *
     */
    public MultipleEventHandler<D> addHandler(EventHandler<D> handler) {
        handlers.add(handler);
        return this;
    }

    /** Add several handlers to the managed handlers list
     * @param newHandlers handlers associated with D detector
     *
     */
    @SafeVarargs // this method is safe
    public final MultipleEventHandler<D> addHandlers(EventHandler<D>... newHandlers) {
    Arrays.stream(newHandlers).forEach(this::addHandler);
        return this;
    }

    /** Change handlers list with user input
     * @param newHandlers new handlers list associated with D detector
     *
     */
    public void setHandlers(List<EventHandler<D>> newHandlers) {
        handlers = newHandlers;
    }

    /** Retrieve managed handlers list
     *
     */
    public List<EventHandler<D>> getHandlers() {
        return this.handlers;
    }
}
