/* Contributed in the public domain.
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
package org.orekit.propagation.analytical;

import java.util.Collection;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.BoundedPropagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.TimeStampedPVCoordinates;

/**
 * A {@link BoundedPropagator} that covers a larger time span from several constituent
 * propagators that cover shorter time spans.
 *
 * @author Evan Ward
 * @see #AggregateBoundedPropagator(Collection)
 */
public class AggregateBoundedPropagator extends AbstractAnalyticalPropagator
        implements BoundedPropagator {

    /** Constituent propagators. */
    private final NavigableMap<AbsoluteDate, BoundedPropagator> propagators;

    /**
     * Create a propagator by concatenating several {@link BoundedPropagator}s.
     *
     * @param propagators that provide the backing data for this instance. If there are
     *                    gaps between the {@link BoundedPropagator#getMaxDate()} of one
     *                    propagator and the {@link BoundedPropagator#getMinDate()} of the
     *                    next propagator an exception may be thrown by any method of this
     *                    class at any time. If there are overlaps between the the {@link
     *                    BoundedPropagator#getMaxDate()} of one propagator and the {@link
     *                    BoundedPropagator#getMinDate()} of the next propagator then the
     *                    propagator with the latest {@link BoundedPropagator#getMinDate()}
     *                    is used.
     */
    public AggregateBoundedPropagator(
            final Collection<? extends BoundedPropagator> propagators) {
        super(DEFAULT_LAW);
        this.propagators = new TreeMap<>();
        for (final BoundedPropagator propagator : propagators) {
            this.propagators.put(propagator.getMinDate(), propagator);
        }
    }


    @Override
    public TimeStampedPVCoordinates getPVCoordinates(final AbsoluteDate date,
                                                     final Frame frame)
            throws OrekitException {
        return getPropagator(date).getPVCoordinates(date, frame);
    }

    @Override
    protected Orbit propagateOrbit(final AbsoluteDate date) throws OrekitException {
        return getPropagator(date).propagate(date).getOrbit();
    }

    @Override
    public AbsoluteDate getMinDate() {
        return propagators.firstEntry().getValue().getMinDate();
    }

    @Override
    public AbsoluteDate getMaxDate() {
        return propagators.lastEntry().getValue().getMaxDate();
    }

    @Override
    protected double getMass(final AbsoluteDate date) throws OrekitException {
        return getPropagator(date).propagate(date).getMass();
    }

    @Override
    public SpacecraftState getInitialState() throws OrekitException {
        return propagators.firstEntry().getValue().getInitialState();
    }

    @Override
    protected void resetIntermediateState(final SpacecraftState state,
                                          final boolean forward) throws OrekitException {
        throw new OrekitException(OrekitMessages.NON_RESETABLE_STATE);
    }

    /**
     * Get the propagator to use for the given date.
     *
     * @param date of query
     * @return propagator to use on date.
     */
    private BoundedPropagator getPropagator(final AbsoluteDate date) {
        final Entry<AbsoluteDate, BoundedPropagator> entry = propagators.floorEntry(date);
        if (entry != null) {
            return entry.getValue();
        } else {
            // let the first propagator throw the exception
            return propagators.firstEntry().getValue();
        }
    }

}
