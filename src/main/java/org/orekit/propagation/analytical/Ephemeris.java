/* Copyright 2002-2017 CS Systèmes d'Information
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
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.hipparchus.exception.LocalizedCoreFormats;
import org.hipparchus.exception.MathIllegalArgumentException;
import org.hipparchus.util.FastMath;
import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitInternalError;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.BoundedPropagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.ImmutableTimeStampedCache;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.TimeStampedPVCoordinates;

/** This class is designed to accept and handle tabulated orbital entries.
 * Tabulated entries are classified and then extrapolated in way to obtain
 * continuous output, with accuracy and computation methods configured by the user.
 *
 * @author Fabien Maussion
 * @author V&eacute;ronique Pommier-Maurussane
 * @author Luc Maisonobe
 */
public class Ephemeris extends AbstractAnalyticalPropagator implements BoundedPropagator, Serializable {

    /** Default extrapolation time threshold: 1ms.
     * @since 9.0
     **/
    public static final double DEFAULT_EXTRAPOLATION_THRESHOLD_SEC = 1e-3;

    /** Serializable UID. */
    private static final long serialVersionUID = 20170606L;

     /** First date in range. */
    private final AbsoluteDate minDate;

    /** Last date in range. */
    private final AbsoluteDate maxDate;

    /** The extrapolation threshold beyond which the propagation will fail. **/
    private final double extrapolationThreshold;

    /** Reference frame. */
    private final Frame frame;

    /** Names of the additional states. */
    private final String[] additional;

    /** Local PV Provider used for computing attitude. **/
    private LocalPVProvider pvProvider;

    /** Thread-safe cache. */
    private final transient ImmutableTimeStampedCache<SpacecraftState> cache;

    /** Constructor with tabulated states.
     * <p>
     * This constructor allows extrapolating outside of the states time span
     * by up to the 1ms {@link #DEFAULT_EXTRAPOLATION_THRESHOLD_SEC default
     * extrapolation threshold}.
     * </p>
     * @param states tabulates states
     * @param interpolationPoints number of points to use in interpolation
     * @exception OrekitException if some states have incompatible additional states
     * @exception MathIllegalArgumentException if the number of states is smaller than
     * the number of points to use in interpolation
     * @see #Ephemeris(List, int, double)
     */
    public Ephemeris(final List<SpacecraftState> states, final int interpolationPoints)
        throws OrekitException, MathIllegalArgumentException {
        this(states, interpolationPoints, DEFAULT_EXTRAPOLATION_THRESHOLD_SEC);
    }

    /** Constructor with tabulated states.
     * @param states tabulates states
     * @param interpolationPoints number of points to use in interpolation
     * @param extrapolationThreshold the largest time difference in seconds between
     * the start or stop boundary of the ephemeris bounds to be doing extrapolation
     * @exception OrekitException if some states have incompatible additional states
     * @exception MathIllegalArgumentException if the number of states is smaller than
     * the number of points to use in interpolation
     * @since 9.0
     */
    public Ephemeris(final List<SpacecraftState> states, final int interpolationPoints,
                     final double extrapolationThreshold)
        throws OrekitException, MathIllegalArgumentException {

        super(DEFAULT_LAW);

        if (states.size() < interpolationPoints) {
            throw new MathIllegalArgumentException(LocalizedCoreFormats.INSUFFICIENT_DIMENSION,
                                                   states.size(), interpolationPoints);
        }

        final SpacecraftState s0 = states.get(0);
        minDate = s0.getDate();
        maxDate = states.get(states.size() - 1).getDate();
        frame = s0.getFrame();

        final Set<String> names0 = s0.getAdditionalStates().keySet();
        additional = names0.toArray(new String[names0.size()]);

        // check all states handle the same additional states
        for (final SpacecraftState state : states) {
            s0.ensureCompatibleAdditionalStates(state);
        }

        pvProvider = new LocalPVProvider();

        // user needs to explicitly set attitude provider if they want to use one
        setAttitudeProvider(null);

        // set up cache
        cache = new ImmutableTimeStampedCache<SpacecraftState>(interpolationPoints, states);

        this.extrapolationThreshold = extrapolationThreshold;
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

    /** Get the maximum timespan outside of the stored ephemeris that is allowed
     * for extrapolation.
     * @return the extrapolation threshold in seconds
     */
    public double getExtrapolationThreshold() {
        return extrapolationThreshold;
    }

    @Override
    public Frame getFrame() {
        return frame;
    }

    @Override
    /** {@inheritDoc} */
    public SpacecraftState basicPropagate(final AbsoluteDate date) throws OrekitException {
        final SpacecraftState evaluatedState;

        final AbsoluteDate central;
        if (date.compareTo(minDate) < 0 && FastMath.abs(date.durationFrom(minDate)) <= extrapolationThreshold) {
            // avoid TimeStampedCacheException as we are still within the tolerance before minDate
            central = minDate;
        } else if (date.compareTo(maxDate) > 0 && FastMath.abs(date.durationFrom(maxDate)) <= extrapolationThreshold) {
            // avoid TimeStampedCacheException as we are still within the tolerance after maxDate
            central = maxDate;
        } else {
            central = date;
        }
        final List<SpacecraftState> neighbors = cache.getNeighbors(central).collect(Collectors.toList());
        evaluatedState = neighbors.get(0).interpolate(date, neighbors);

        final AttitudeProvider attitudeProvider = getAttitudeProvider();

        if (attitudeProvider == null) {
            return evaluatedState;
        } else {
            pvProvider.setCurrentState(evaluatedState);
            final Attitude calculatedAttitude = attitudeProvider.getAttitude(pvProvider, date,
                                                                             evaluatedState.getFrame());
            return new SpacecraftState(evaluatedState.getOrbit(), calculatedAttitude, evaluatedState.getMass());
        }
    }

    /** {@inheritDoc} */
    protected Orbit propagateOrbit(final AbsoluteDate date) throws OrekitException {
        return basicPropagate(date).getOrbit();
    }

    /** {@inheritDoc} */
    protected double getMass(final AbsoluteDate date) throws OrekitException {
        return basicPropagate(date).getMass();
    }

    /** {@inheritDoc} */
    public TimeStampedPVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame f)
        throws OrekitException {
        return propagate(date).getPVCoordinates(f);
    }

    /** Try (and fail) to reset the initial state.
     * <p>
     * This method always throws an exception, as ephemerides cannot be reset.
     * </p>
     * @param state new initial state to consider
     * @exception OrekitException always thrown as ephemerides cannot be reset
     */
    public void resetInitialState(final SpacecraftState state)
        throws OrekitException {
        throw new OrekitException(OrekitMessages.NON_RESETABLE_STATE);
    }

    /** {@inheritDoc} */
    protected void resetIntermediateState(final SpacecraftState state, final boolean forward)
        throws OrekitException {
        throw new OrekitException(OrekitMessages.NON_RESETABLE_STATE);
    }

    /** {@inheritDoc} */
    public SpacecraftState getInitialState() throws OrekitException {
        return basicPropagate(getMinDate());
    }

    /** {@inheritDoc} */
    @Override
    public boolean isAdditionalStateManaged(final String name) {

        // the additional state may be managed by a specific provider in the base class
        if (super.isAdditionalStateManaged(name)) {
            return true;
        }

        // the additional state may be managed in the states sample
        for (final String a : additional) {
            if (a.equals(name)) {
                return true;
            }
        }

        return false;

    }

    /** {@inheritDoc} */
    @Override
    public String[] getManagedAdditionalStates() {
        final String[] upperManaged = super.getManagedAdditionalStates();
        final String[] managed = new String[upperManaged.length + additional.length];
        System.arraycopy(upperManaged, 0, managed, 0, upperManaged.length);
        System.arraycopy(additional, 0, managed, upperManaged.length, additional.length);
        return managed;
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
        return new DataTransferObject(cache.getAll(), cache.getNeighborsSize(), extrapolationThreshold);
    }

    /** Internal class used only for serialization. */
    private static class DataTransferObject implements Serializable {

        /** Serializable UID. */
        private static final long serialVersionUID = 20170606L;

        /** Tabulates states. */
        private final List<SpacecraftState> states;

        /** Number of points to use in interpolation. */
        private final int interpolationPoints;

        /** The extrapolation threshold beyond which the propagation will fail. **/
        private final double extrapolationThreshold;

        /** Simple constructor.
         * @param states tabulates states
         * @param interpolationPoints number of points to use in interpolation
         * @param extrapolationThreshold extrapolation threshold beyond which the propagation will fail
         */
        private DataTransferObject(final List<SpacecraftState> states, final int interpolationPoints,
                                   final double extrapolationThreshold) {
            this.states                 = states;
            this.interpolationPoints    = interpolationPoints;
            this.extrapolationThreshold = extrapolationThreshold;
        }

        /** Replace the deserialized data transfer object with a
         * {@link Ephemeris}.
         * @return replacement {@link Ephemeris}
         */
        private Object readResolve() {
            try {
                // build a new provider, with an empty cache
                return new Ephemeris(states, interpolationPoints, extrapolationThreshold);
            } catch (OrekitException oe) {
                // this should never happen
                throw new OrekitInternalError(oe);
            }
        }

    }

    /** Internal PVCoordinatesProvider for attitude computation. */
    private static class LocalPVProvider implements PVCoordinatesProvider, Serializable {

        /** Serializable UID. */
        private static final long serialVersionUID = 20160115L;

        /** Current state. */
        private SpacecraftState currentState;

        /** Get the current state.
         * @return current state
         */
        public SpacecraftState getCurrentState() {
            return currentState;
        }

        /** Set the current state.
         * @param state state to set
         */
        public void setCurrentState(final SpacecraftState state) {
            this.currentState = state;
        }

        /** {@inheritDoc} */
        public TimeStampedPVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame f)
            throws OrekitException {
            final double dt = getCurrentState().getDate().durationFrom(date);
            final double closeEnoughTimeInSec = 1e-9;

            if (FastMath.abs(dt) > closeEnoughTimeInSec) {
                throw new OrekitException(LocalizedCoreFormats.OUT_OF_RANGE_SIMPLE, FastMath.abs(dt), 0.0,
                                          closeEnoughTimeInSec);
            }

            return getCurrentState().getPVCoordinates(f);

        }

    }

}
