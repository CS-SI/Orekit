/* Contributed in the public domain.
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
package org.orekit.propagation.analytical;

import java.util.Collection;
import java.util.Collections;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.FrameAlignedProvider;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.BoundedPropagator;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.TimeStampedPVCoordinates;

/**
 * A {@link BoundedPropagator} that covers a larger time span from several constituent
 * propagators that cover shorter time spans.
 *
 * @author Evan Ward
 * @see #AggregateBoundedPropagator(Collection)
 * @since 9.0
 */
public class AggregateBoundedPropagator extends AbstractAnalyticalPropagator
        implements BoundedPropagator {

    /** Constituent propagators. */
    private final NavigableMap<AbsoluteDate, ? extends BoundedPropagator> propagators;
    /** Minimum date for {@link #getMinDate()}. */
    private final AbsoluteDate min;
    /** Maximum date for {@link #getMaxDate()}. */
    private final AbsoluteDate max;

    /**
     * Create a propagator by concatenating several {@link BoundedPropagator}s.
     *
     * @param propagators that provide the backing data for this instance. There must be
     *                    at least one propagator in the collection. If there are gaps
     *                    between the {@link BoundedPropagator#getMaxDate()} of one
     *                    propagator and the {@link BoundedPropagator#getMinDate()} of the
     *                    next propagator an exception may be thrown by any method of this
     *                    class at any time. If there are overlaps between the {@link
     *                    BoundedPropagator#getMaxDate()} of one propagator and the {@link
     *                    BoundedPropagator#getMinDate()} of the next propagator then the
     *                    propagator with the latest {@link BoundedPropagator#getMinDate()}
     *                    is used.
     */
    public AggregateBoundedPropagator(
            final Collection<? extends BoundedPropagator> propagators) {
        super(defaultAttitude(propagators));
        final NavigableMap<AbsoluteDate, BoundedPropagator> map =
                new TreeMap<>();
        for (final BoundedPropagator propagator : propagators) {
            map.put(propagator.getMinDate(), propagator);
        }
        this.propagators = map;
        this.min = map.firstEntry().getValue().getMinDate();
        this.max = map.lastEntry().getValue().getMaxDate();
        super.resetInitialState(
                this.propagators.firstEntry().getValue().getInitialState());
    }

    /**
     * Create a propagator from several constituent propagators.
     *
     * @param propagators that provide the backing data for this instance. Each
     *                    propagator is used from the date of it's key in the
     *                    map until the date of the next key. The first
     *                    propagator is also used before the first key and the
     *                    last propagator after the last key.
     * @param min         the value for {@link #getMinDate()}.
     * @param max         the value for {@link #getMaxDate()}.
     */
    public AggregateBoundedPropagator(
            final NavigableMap<AbsoluteDate, ? extends BoundedPropagator> propagators,
            final AbsoluteDate min,
            final AbsoluteDate max) {
        super(defaultAttitude(propagators.values()));
        this.propagators = propagators;
        this.min = min;
        this.max = max;
        super.resetInitialState(
                this.propagators.firstEntry().getValue().getInitialState());
    }

    /**
     * Helper function for the constructor.
     * @param propagators to consider.
     * @return attitude provider.
     */
    private static AttitudeProvider defaultAttitude(
            final Collection<? extends Propagator> propagators) {
        // this check is needed here because it can't be before the super() call in the
        // constructor.
        if (propagators.isEmpty()) {
            throw new OrekitException(OrekitMessages.NOT_ENOUGH_PROPAGATORS);
        }
        return new FrameAlignedProvider(propagators.iterator().next().getFrame());
    }

    /** Get an unmodifiable view of the propagators map.
     * <p>
     * The key of the map entries are the {@link BoundedPropagator#getMinDate() min dates}
     * of each propagator.
     * </p>
     * @return unmodifiable view of the propagators map
     * @since 12.0
     */
    public NavigableMap<AbsoluteDate, ? extends BoundedPropagator> getPropagators() {
        return Collections.unmodifiableNavigableMap(propagators);
    }

    @Override
    protected SpacecraftState basicPropagate(final AbsoluteDate date) {
        // #589 override this method for a performance benefit,
        // getPropagator(date).propagate(date) is only called once

        // do propagation
        final SpacecraftState state = getPropagator(date).propagate(date);

        // evaluate attitude
        final Attitude attitude =
                getAttitudeProvider().getAttitude(this, date, state.getFrame());

        // build raw state
        if (state.isOrbitDefined()) {
            return new SpacecraftState(
                    state.getOrbit(), attitude, state.getMass(),
                    state.getAdditionalStatesValues(), state.getAdditionalStatesDerivatives());
        } else {
            return new SpacecraftState(
                    state.getAbsPVA(), attitude, state.getMass(),
                    state.getAdditionalStatesValues(), state.getAdditionalStatesDerivatives());
        }
    }

    @Override
    public TimeStampedPVCoordinates getPVCoordinates(final AbsoluteDate date,
                                                     final Frame frame) {
        return getPropagator(date).getPVCoordinates(date, frame);
    }

    @Override
    protected Orbit propagateOrbit(final AbsoluteDate date) {
        return getPropagator(date).propagate(date).getOrbit();
    }

    @Override
    public AbsoluteDate getMinDate() {
        return min;
    }

    @Override
    public AbsoluteDate getMaxDate() {
        return max;
    }

    @Override
    protected double getMass(final AbsoluteDate date) {
        return getPropagator(date).propagate(date).getMass();
    }

    @Override
    public SpacecraftState getInitialState() {
        return propagators.firstEntry().getValue().getInitialState();
    }

    @Override
    protected void resetIntermediateState(final SpacecraftState state,
                                          final boolean forward) {
        throw new OrekitException(OrekitMessages.NON_RESETABLE_STATE);
    }

    @Override
    public void resetInitialState(final SpacecraftState state) {
        throw new OrekitException(OrekitMessages.NON_RESETABLE_STATE);
    }

    /**
     * Get the propagator to use for the given date.
     *
     * @param date of query
     * @return propagator to use on date.
     */
    private BoundedPropagator getPropagator(final AbsoluteDate date) {
        final Entry<AbsoluteDate, ? extends BoundedPropagator> entry =
                propagators.floorEntry(date);
        if (entry != null) {
            return entry.getValue();
        } else {
            // let the first propagator throw the exception
            return propagators.firstEntry().getValue();
        }
    }

}
