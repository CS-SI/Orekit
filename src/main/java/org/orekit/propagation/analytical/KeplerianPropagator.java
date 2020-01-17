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

import java.util.Collections;
import java.util.Map;

import org.orekit.annotation.DefaultDataContext;
import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.data.DataContext;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.TimeSpanMap;

/** Simple Keplerian orbit propagator.
 * @see Orbit
 * @author Guylaine Prat
 */
public class KeplerianPropagator extends AbstractAnalyticalPropagator {

    /** Initial state. */
    private SpacecraftState initialState;

    /** All states. */
    private TimeSpanMap<SpacecraftState> states;

    /** Build a propagator from orbit only.
     * <p>The central attraction coefficient μ is set to the same value used
     * for the initial orbit definition. Mass and attitude provider are set to
     * unspecified non-null arbitrary values.</p>
     *
     * <p>This constructor uses the {@link DataContext#getDefault() default data context}.
     *
     * @param initialOrbit initial orbit
     * @see #KeplerianPropagator(Orbit, AttitudeProvider)
     */
    @DefaultDataContext
    public KeplerianPropagator(final Orbit initialOrbit) {
        this(initialOrbit, Propagator.getDefaultLaw(DataContext.getDefault().getFrames()),
                initialOrbit.getMu(), DEFAULT_MASS);
    }

    /** Build a propagator from orbit and central attraction coefficient μ.
     * <p>Mass and attitude provider are set to unspecified non-null arbitrary values.</p>
     *
     * <p>This constructor uses the {@link DataContext#getDefault() default data context}.
     *
     * @param initialOrbit initial orbit
     * @param mu central attraction coefficient (m³/s²)
     * @see #KeplerianPropagator(Orbit, AttitudeProvider, double)
     */
    @DefaultDataContext
    public KeplerianPropagator(final Orbit initialOrbit, final double mu) {
        this(initialOrbit, Propagator.getDefaultLaw(DataContext.getDefault().getFrames()),
                mu, DEFAULT_MASS);
    }

    /** Build a propagator from orbit and attitude provider.
     * <p>The central attraction coefficient μ is set to the same value
     * used for the initial orbit definition. Mass is set to an unspecified
     * non-null arbitrary value.</p>
     * @param initialOrbit initial orbit
     * @param attitudeProv  attitude provider
     */
    public KeplerianPropagator(final Orbit initialOrbit,
                               final AttitudeProvider attitudeProv) {
        this(initialOrbit, attitudeProv, initialOrbit.getMu(), DEFAULT_MASS);
    }

    /** Build a propagator from orbit, attitude provider and central attraction
     * coefficient μ.
     * <p>Mass is set to an unspecified non-null arbitrary value.</p>
     * @param initialOrbit initial orbit
     * @param attitudeProv attitude provider
     * @param mu central attraction coefficient (m³/s²)
     */
    public KeplerianPropagator(final Orbit initialOrbit,
                               final AttitudeProvider attitudeProv,
                               final double mu) {
        this(initialOrbit, attitudeProv, mu, DEFAULT_MASS);
    }

    /** Build propagator from orbit, attitude provider, central attraction
     * coefficient μ and mass.
     * @param initialOrbit initial orbit
     * @param attitudeProv attitude provider
     * @param mu central attraction coefficient (m³/s²)
     * @param mass spacecraft mass (kg)
     */
    public KeplerianPropagator(final Orbit initialOrbit, final AttitudeProvider attitudeProv,
                               final double mu, final double mass) {

        super(attitudeProv);

        // ensure the orbit use the specified mu and has no non-Keplerian derivatives
        initialState = fixState(initialOrbit,
                                getAttitudeProvider().getAttitude(initialOrbit,
                                                                  initialOrbit.getDate(),
                                                                  initialOrbit.getFrame()),
                                mass, mu, Collections.emptyMap());
        states = new TimeSpanMap<SpacecraftState>(initialState);
        super.resetInitialState(initialState);

    }

    /** Fix state to use a specified mu and remove derivatives.
     * <p>
     * This ensures the propagation model (which is based on calling
     * {@link Orbit#shiftedBy(double)}) is Keplerian only and uses a specified mu.
     * </p>
     * @param orbit orbit to fix
     * @param attitude current attitude
     * @param mass current mass
     * @param mu gravity coefficient to use
     * @param additionalStates additional states
     * @return fixed orbit
     */
    private SpacecraftState fixState(final Orbit orbit, final Attitude attitude, final double mass,
                                     final double mu, final Map<String, double[]> additionalStates) {
        final OrbitType type = orbit.getType();
        final double[] stateVector = new double[6];
        type.mapOrbitToArray(orbit, PositionAngle.TRUE, stateVector, null);
        final Orbit fixedOrbit = type.mapArrayToOrbit(stateVector, null, PositionAngle.TRUE,
                                                      orbit.getDate(), mu, orbit.getFrame());
        SpacecraftState fixedState = new SpacecraftState(fixedOrbit, attitude, mass);
        for (final Map.Entry<String, double[]> entry : additionalStates.entrySet()) {
            fixedState = fixedState.addAdditionalState(entry.getKey(), entry.getValue());
        }
        return fixedState;
    }

    /** {@inheritDoc} */
    public void resetInitialState(final SpacecraftState state) {

        // ensure the orbit use the specified mu and has no non-Keplerian derivatives
        final double mu = initialState == null ? state.getMu() : initialState.getMu();
        final SpacecraftState fixedState = fixState(state.getOrbit(),
                                                    state.getAttitude(),
                                                    state.getMass(),
                                                    mu,
                                                    state.getAdditionalStates());

        initialState = fixedState;
        states       = new TimeSpanMap<SpacecraftState>(initialState);
        super.resetInitialState(fixedState);

    }

    /** {@inheritDoc} */
    protected void resetIntermediateState(final SpacecraftState state, final boolean forward) {
        if (forward) {
            states.addValidAfter(state, state.getDate());
        } else {
            states.addValidBefore(state, state.getDate());
        }
        stateChanged(state);
    }

    /** {@inheritDoc} */
    protected Orbit propagateOrbit(final AbsoluteDate date) {

        // propagate orbit
        Orbit orbit = states.get(date).getOrbit();
        do {
            // we use a loop here to compensate for very small date shifts error
            // that occur with long propagation time
            orbit = orbit.shiftedBy(date.durationFrom(orbit.getDate()));
        } while (!date.equals(orbit.getDate()));

        return orbit;

    }

    /** {@inheritDoc}*/
    protected double getMass(final AbsoluteDate date) {
        return states.get(date).getMass();
    }

}
