/* Copyright 2002-2021 CS GROUP
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
package org.orekit.forces.maneuvers.trigger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.TimeSpanMap;

/**
 * Base class for triggers.
 * @author Luc Maisonobe
 * @since 11.1
 */
public abstract class AbstractManeuverTriggers implements ManeuverTriggers {

    /** Firing time spans. */
    private TimeSpanMap<Boolean> firings;

    /** Propagation direction. */
    private boolean forward;

    /** Observers for the maneuver triggers. */
    private final List<ManeuverTriggersObserver> observers;

    /** Cached field-based observers. */
    private final transient Map<Field<? extends CalculusFieldElement<?>>, List<FieldManeuverTriggersObserver<?>>> cached;

    /** Simple constructor.
     */
    protected AbstractManeuverTriggers() {
        this.firings   = new TimeSpanMap<>(Boolean.FALSE);
        this.observers = new ArrayList<>();
        this.cached    = new HashMap<>();
    }

    /** {@inheritDoc} */
    @Override
    public void init(final SpacecraftState initialState, final AbsoluteDate target) {

        forward = target.isAfterOrEqualTo(initialState);
        firings = new TimeSpanMap<>(Boolean.FALSE);
        for (final ManeuverTriggersObserver o : observers) {
            o.init(initialState, target);
        }

        if (isFiringOnInitialState(initialState, forward)) {
            if (forward) {
                firings.addValidAfter(Boolean.TRUE, initialState.getDate());
            } else {
                firings.addValidBefore(Boolean.TRUE, initialState.getDate());
            }
        }

    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public <T extends CalculusFieldElement<T>> void init(final FieldSpacecraftState<T> initialState, final FieldAbsoluteDate<T> target) {

        forward = target.isAfterOrEqualTo(initialState);
        firings = new TimeSpanMap<>(Boolean.FALSE);
        // check if we already have observers for this field
        final List<FieldManeuverTriggersObserver<?>> list = cached.get(initialState.getDate().getField());
        if (list != null) {
            for (FieldManeuverTriggersObserver<?> o : list) {
                ((FieldManeuverTriggersObserver<T>) o).init(initialState, target);
            }
        }

        if (isFiringOnInitialState(initialState.toSpacecraftState(), forward)) {
            if (forward) {
                firings.addValidAfter(Boolean.TRUE, initialState.getDate().toAbsoluteDate());
            } else {
                firings.addValidBefore(Boolean.TRUE, initialState.getDate().toAbsoluteDate());
            }
        }

    }

    /**
     * Method to check if the thruster is firing on initialization. can be called by
     * sub classes
     *
     * @param initialState initial spacecraft state
     * @param isForward if true, propagation will be in the forward direction
     * @return true if firing in propagation direction
     */
    protected abstract boolean isFiringOnInitialState(SpacecraftState initialState, boolean isForward);

    /** {@inheritDoc} */
    @Override
    public boolean isFiring(final AbsoluteDate date, final double[] parameters) {
        return firings.get(date);
    }

    /** {@inheritDoc} */
    @Override
    public <S extends CalculusFieldElement<S>> boolean isFiring(final FieldAbsoluteDate<S> date, final S[] parameters) {
        return firings.get(date.toAbsoluteDate());
    }

    /** Get the firings detected during last propagation.
     * @return firings detected during last propagation
     */
    public TimeSpanMap<Boolean> getFirings() {
        return firings;
    }

    /** Add an observer.
     * @param observer observer to add
     */
    public void addObserver(final ManeuverTriggersObserver observer) {
        observers.add(observer);
    }

    /** Add an observer.
     * @param field field to which the state belongs
     * @param observer observer to add
     * @param <T> type of the field elements
     */
    public <T extends CalculusFieldElement<T>> void addObserver(final Field<T> field, final FieldManeuverTriggersObserver<T> observer) {

        // check if we already have observers for this field
        List<FieldManeuverTriggersObserver<?>> list = cached.get(field);
        if (list == null) {
            list = new ArrayList<>();
            cached.put(field, list);
        }

        // add the observer to the list
        list.add(observer);

    }

    /** Notify observers.
     * @param state spacecraft state at trigger date
     * @param start if true, the trigger is the start of the maneuver
     */
    protected void notifyObservers(final SpacecraftState state, final boolean start) {
        observers.forEach(o -> o.maneuverTriggered(state, start));
    }

    /** Notify observers.
     * @param state spacecraft state at trigger date
     * @param start if true, the trigger is the start of the maneuver
     * @param <T> type of the field elements
     */
    @SuppressWarnings("unchecked")
    protected <T extends CalculusFieldElement<T>> void notifyObservers(final FieldSpacecraftState<T> state, final boolean start) {
        final List<FieldManeuverTriggersObserver<?>> list = cached.get(state.getDate().getField());
        if (list != null) {
            list.forEach(o -> ((FieldManeuverTriggersObserver<T>) o).maneuverTriggered(state, start));
        }
    }

}
