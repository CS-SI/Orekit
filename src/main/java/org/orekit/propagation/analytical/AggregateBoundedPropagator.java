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
import java.util.NavigableMap;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.FieldAttitude;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.BoundedPropagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldPVCoordinatesProvider;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.TimeSpanMap;
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
    private final TimeSpanMap<BoundedPropagator> map;

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
    public AggregateBoundedPropagator(final Collection<? extends BoundedPropagator> propagators) {
        super(null);
        map = new TimeSpanMap<>(null);
        propagators.forEach(p -> map.addValidAfter(p, p.getMinDate(), false));
        setAttitudeProvider(new AggregateAttitudeProvider());
        this.min = map.getFirstNonNullSpan().getData().getMinDate();
        this.max = map.getLastNonNullSpan().getData().getMaxDate();
        super.resetInitialState(getInitialState());
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
    public AggregateBoundedPropagator(final NavigableMap<AbsoluteDate, ? extends BoundedPropagator> propagators,
                                      final AbsoluteDate min, final AbsoluteDate max) {
        super(null);
        map = new TimeSpanMap<>(null);
        propagators.forEach((d, p) -> map.addValidAfter(p, p.getMinDate(), false));
        setAttitudeProvider(new AggregateAttitudeProvider());
        this.min = min;
        this.max = max;
        super.resetInitialState(getInitialState());
    }

    /** Get the propagators map.
     * @return propagators map
     * @since 12.1
     */
    public TimeSpanMap<BoundedPropagator> getPropagatorsMap() {
        return map;
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
    public Vector3D getPosition(final AbsoluteDate date, final Frame frame) {
        return getPropagator(date).propagate(date).getPosition(frame);
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
        return map.getFirstNonNullSpan().getData().getInitialState();
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
        final BoundedPropagator propagator = map.get(date);
        if (propagator != null) {
            return propagator;
        } else {
            // let the first propagator throw the exception
            return map.getFirstNonNullSpan().getData();
        }
    }

    /** Local attitude provider. */
    private class AggregateAttitudeProvider implements AttitudeProvider {

        /** {@inheritDoc} */
        @Override
        public Attitude getAttitude(final PVCoordinatesProvider pvProv,
                                    final AbsoluteDate date,
                                    final Frame frame) {
            return getPropagator(date).getAttitudeProvider().getAttitude(pvProv, date, frame);
        }

        /** {@inheritDoc} */
        @Override
        public <T extends CalculusFieldElement<T>> FieldAttitude<T> getAttitude(final FieldPVCoordinatesProvider<T> pvProv,
                                                                                final FieldAbsoluteDate<T> date,
                                                                                final Frame frame) {
            return getPropagator(date.toAbsoluteDate()).getAttitudeProvider().getAttitude(pvProv, date, frame);
        }

        /** {@inheritDoc} */
        @Override
        public Rotation getAttitudeRotation(final PVCoordinatesProvider pvProv, final AbsoluteDate date, final Frame frame) {
            return getPropagator(date).getAttitudeProvider().getAttitudeRotation(pvProv, date, frame);
        }

        /** {@inheritDoc} */
        @Override
        public <T extends CalculusFieldElement<T>> FieldRotation<T> getAttitudeRotation(final FieldPVCoordinatesProvider<T> pvProv,
                                                                                        final FieldAbsoluteDate<T> date,
                                                                                        final Frame frame) {
            return getPropagator(date.toAbsoluteDate()).getAttitudeProvider().getAttitudeRotation(pvProv, date, frame);
        }
    }

}
