/* Copyright 2002-2020 CS Group
 * Licensed to CS Group (CS) under one or more
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
import org.orekit.annotation.DefaultDataContext;
import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.data.DataContext;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.BoundedPropagator;
import org.orekit.propagation.Propagator;
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
public class Ephemeris extends AbstractAnalyticalPropagator implements BoundedPropagator {

    /** Default extrapolation time threshold: 1ms.
     * @since 9.0
     **/
    public static final double DEFAULT_EXTRAPOLATION_THRESHOLD_SEC = 1e-3;

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
     *
     * <p>This constructor uses the {@link DataContext#getDefault() default data context}.
     *
     * @param states tabulates states
     * @param interpolationPoints number of points to use in interpolation
          * @exception MathIllegalArgumentException if the number of states is smaller than
     * the number of points to use in interpolation
     * @see #Ephemeris(List, int, double)
     * @see #Ephemeris(List, int, double, AttitudeProvider)
     */
    @DefaultDataContext
    public Ephemeris(final List<SpacecraftState> states, final int interpolationPoints)
        throws MathIllegalArgumentException {
        this(states, interpolationPoints, DEFAULT_EXTRAPOLATION_THRESHOLD_SEC);
    }

    /** Constructor with tabulated states.
     *
     * <p>This constructor uses the {@link DataContext#getDefault() default data context}.
     *
     * @param states tabulates states
     * @param interpolationPoints number of points to use in interpolation
     * @param extrapolationThreshold the largest time difference in seconds between
     * the start or stop boundary of the ephemeris bounds to be doing extrapolation
     * @exception MathIllegalArgumentException if the number of states is smaller than
     * the number of points to use in interpolation
     * @since 9.0
     * @see #Ephemeris(List, int, double, AttitudeProvider)
     */
    @DefaultDataContext
    public Ephemeris(final List<SpacecraftState> states, final int interpolationPoints,
                     final double extrapolationThreshold)
        throws MathIllegalArgumentException {
        this(states, interpolationPoints, extrapolationThreshold,
                Propagator.getDefaultLaw(DataContext.getDefault().getFrames()));
    }

    /** Constructor with tabulated states.
     * @param states tabulates states
     * @param interpolationPoints number of points to use in interpolation
     * @param extrapolationThreshold the largest time difference in seconds between
     * the start or stop boundary of the ephemeris bounds to be doing extrapolation
     * @param attitudeProvider attitude law to use.
     * @exception MathIllegalArgumentException if the number of states is smaller than
     * the number of points to use in interpolation
     * @since 10.1
     */
    public Ephemeris(final List<SpacecraftState> states,
                     final int interpolationPoints,
                     final double extrapolationThreshold,
                     final AttitudeProvider attitudeProvider)
        throws MathIllegalArgumentException {

        super(attitudeProvider);

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

        pvProvider = new LocalPVProvider(states, interpolationPoints, extrapolationThreshold);

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
    public SpacecraftState basicPropagate(final AbsoluteDate date) {
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
    protected Orbit propagateOrbit(final AbsoluteDate date) {
        return basicPropagate(date).getOrbit();
    }

    /** {@inheritDoc} */
    protected double getMass(final AbsoluteDate date) {
        return basicPropagate(date).getMass();
    }

    /** {@inheritDoc} */
    public TimeStampedPVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame f) {
        return propagate(date).getPVCoordinates(f);
    }

    /** Try (and fail) to reset the initial state.
     * <p>
     * This method always throws an exception, as ephemerides cannot be reset.
     * </p>
     * @param state new initial state to consider
     */
    public void resetInitialState(final SpacecraftState state) {
        throw new OrekitException(OrekitMessages.NON_RESETABLE_STATE);
    }

    /** {@inheritDoc} */
    protected void resetIntermediateState(final SpacecraftState state, final boolean forward) {
        throw new OrekitException(OrekitMessages.NON_RESETABLE_STATE);
    }

    /** {@inheritDoc} */
    public SpacecraftState getInitialState() {
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

    /** Internal PVCoordinatesProvider for attitude computation. */
    private static class LocalPVProvider implements PVCoordinatesProvider, Serializable {

        /** Serializable UID. */
        private static final long serialVersionUID = 20160115L;

        /** Current state. */
        private SpacecraftState currentState;

        /** List of spacecraft states. */
        private List<SpacecraftState> states;

        /** Interpolation points number. */
        private int interpolationPoints;

        /** Extrapolation threshold. */
        private double extrapolationThreshold;

        /** Constructor.
         * @param states list of spacecraft states
         * @param interpolationPoints interpolation points number
         * @param extrapolationThreshold extrapolation threshold value
         */
        LocalPVProvider(final List<SpacecraftState> states, final int interpolationPoints,
                     final double extrapolationThreshold) {

            this.states = states;
            this.interpolationPoints = interpolationPoints;
            this.extrapolationThreshold = extrapolationThreshold;
        }

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
        public TimeStampedPVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame f) {
            final double dt = getCurrentState().getDate().durationFrom(date);
            final double closeEnoughTimeInSec = 1e-9;

            if (FastMath.abs(dt) > closeEnoughTimeInSec) {

                // used in case of attitude transition, the attitude computed is not at the current date.
                final Ephemeris ephemeris = new Ephemeris(states, interpolationPoints, extrapolationThreshold, null);
                return ephemeris.getPVCoordinates(date, f);
            }

            return currentState.getPVCoordinates(f);

        }

    }

}
