/* Copyright 2002-2012 CS Systèmes d'Information
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

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.math3.exception.MathIllegalArgumentException;
import org.apache.commons.math3.exception.util.LocalizedFormats;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.errors.PropagationException;
import org.orekit.errors.TimeStampedCacheException;
import org.orekit.frames.Frame;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.BoundedPropagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.OrekitConfiguration;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TimeStampedCache;
import org.orekit.utils.TimeStampedGenerator;

/** This class is designed to accept and handle tabulated orbital entries.
 * Tabulated entries are classified and then extrapolated in way to obtain
 * continuous output, with accuracy and computation methods configured by the user.
 *
 * @author Fabien Maussion
 * @author V&eacute;ronique Pommier-Maurussane
 * @author Luc Maisonobe
 */
public class Ephemeris extends AbstractAnalyticalPropagator implements BoundedPropagator {

    /** Serializable UID. */
    private static final long serialVersionUID = -7270780789524246722L;

    /** First date in range. */
    private final AbsoluteDate minDate;

    /** Last date in range. */
    private final AbsoluteDate maxDate;

    /** Thread-safe cache. */
    private final transient TimeStampedCache<SpacecraftState> cache;

    /** Constructor with tabulated states.
     * @param states tabulates states
     * @param interpolationPoints number of points to use in interpolation
     * @exception MathIllegalArgumentException if the number of states is smaller than
     * the number of points to use in interpolation
     */
    public Ephemeris(final List<SpacecraftState> states, final int interpolationPoints)
        throws MathIllegalArgumentException {

        super(DEFAULT_LAW);

        if (states.size() < interpolationPoints) {
            throw new MathIllegalArgumentException(LocalizedFormats.INSUFFICIENT_DIMENSION,
                                                   states.size(), interpolationPoints);
        }

        minDate = states.get(0).getDate();
        maxDate = states.get(states.size() - 1).getDate();

        // set up cache
        final TimeStampedGenerator<SpacecraftState> generator =
                new TimeStampedGenerator<SpacecraftState>() {
                    /** {@inheritDoc} */
                    public List<SpacecraftState> generate(final SpacecraftState existing, final AbsoluteDate date) {
                        return states;
                    }
                };
        cache = new TimeStampedCache<SpacecraftState>(interpolationPoints,
                                                      OrekitConfiguration.getCacheSlotsNumber(),
                                                      Double.POSITIVE_INFINITY, Constants.JULIAN_DAY,
                                                      generator, SpacecraftState.class);

    }

    /** Get the first date of the range.
     * @return the first date of the range
     */
    public AbsoluteDate getMinDate() {
        return minDate;
    }

    /** Get the last date of the range.
     * @return the last date of the range
     */
    public AbsoluteDate getMaxDate() {
        return maxDate;
    }

    @Override
    /** {@inheritDoc} */
    public SpacecraftState basicPropagate(final AbsoluteDate date) throws PropagationException {
        try {
            final SpacecraftState[] neighbors = cache.getNeighbors(date);
            return neighbors[0].interpolate(date, Arrays.asList(neighbors));
        } catch (TimeStampedCacheException tce) {
            throw new PropagationException(tce);
        }
    }

    /** {@inheritDoc} */
    protected Orbit propagateOrbit(final AbsoluteDate date) throws PropagationException {
        return basicPropagate(date).getOrbit();
    }

    /** {@inheritDoc} */
    protected double getMass(final AbsoluteDate date) throws PropagationException {
        return basicPropagate(date).getMass();
    }

    /** {@inheritDoc} */
    public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame)
        throws OrekitException {
        return propagate(date).getPVCoordinates(frame);
    }

    /** Try (and fail) to reset the initial state.
     * <p>
     * This method always throws an exception, as ephemerides cannot be reset.
     * </p>
     * @param state new initial state to consider
     * @exception PropagationException always thrown as ephemerides cannot be reset
     */
    public void resetInitialState(final SpacecraftState state)
        throws PropagationException {
        throw new PropagationException(OrekitMessages.NON_RESETABLE_STATE);
    }

    /** {@inheritDoc} */
    public SpacecraftState getInitialState() throws PropagationException {
        return basicPropagate(getMinDate());
    }

    /** Replace the instance with a data transfer object for serialization.
     * <p>
     * This intermediate class serializes only the data needed for generation,
     * but does <em>not</em> serializes the cache itself (in fact the cache is
     * not serializable).
     * </p>
     * @return data transfer object that will be serialized
     */
    private Object writeReplace() {
        try {
            return new DataTransferObject(cache.getGenerator().generate(null, null),
                                          cache.getNeighborsSize());
        } catch (TimeStampedCacheException tce) {
            // this should never happen
            throw OrekitException.createInternalError(tce);
        }
    }

    /** Internal class used only for serialization. */
    private static class DataTransferObject implements Serializable {

        /** Serializable UID. */
        private static final long serialVersionUID = -8479036196711159270L;

        /** Tabulates states. */
        private final List<SpacecraftState> states;

        /** Number of points to use in interpolation. */
        private final int interpolationPoints;

        /** Simple constructor.
         * @param states tabulates states
         * @param interpolationPoints number of points to use in interpolation
         */
        private DataTransferObject(final List<SpacecraftState> states, final int interpolationPoints) {
            this.states              = states;
            this.interpolationPoints = interpolationPoints;
        }

        /** Replace the deserialized data transfer object with a {@link Ephemeris}.
         * @return replacement {@link Ephemeris}
         */
        private Object readResolve() {
            // build a new provider, with an empty cache
            return new Ephemeris(states, interpolationPoints);
        }

    }

}
