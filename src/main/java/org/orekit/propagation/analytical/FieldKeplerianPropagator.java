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


import org.hipparchus.RealFieldElement;
import org.hipparchus.util.MathArrays;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.FieldAttitude;
import org.orekit.errors.OrekitException;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldTimeSpanMap;

/** Simple Keplerian orbit propagator.
 * @see FieldOrbit
 * @author Guylaine Prat
 */
public class FieldKeplerianPropagator<T extends RealFieldElement<T>> extends FieldAbstractAnalyticalPropagator<T> {


    /** Initial state. */
    private FieldSpacecraftState<T> initialState;

    /** All states. */
    private transient FieldTimeSpanMap<FieldSpacecraftState<T>, T> states;

    /** Build a propagator from orbit only.
     * <p>The central attraction coefficient μ is set to the same value used
     * for the initial orbit definition. Mass and attitude provider are set to
     * unspecified non-null arbitrary values.</p>
     * @param initialFieldOrbit initial orbit
     * @exception OrekitException if initial attitude cannot be computed
     */
    public FieldKeplerianPropagator(final FieldOrbit<T> initialFieldOrbit)
        throws OrekitException {
        this(initialFieldOrbit, DEFAULT_LAW, initialFieldOrbit.getMu(), initialFieldOrbit.getA().getField().getZero().add(DEFAULT_MASS));
    }

    /** Build a propagator from orbit and central attraction coefficient μ.
     * <p>Mass and attitude provider are set to unspecified non-null arbitrary values.</p>
     * @param initialFieldOrbit initial orbit
     * @param mu central attraction coefficient (m³/s²)
     * @exception OrekitException if initial attitude cannot be computed
     */
    public FieldKeplerianPropagator(final FieldOrbit<T> initialFieldOrbit, final double mu)
        throws OrekitException {
        this(initialFieldOrbit, DEFAULT_LAW, mu, initialFieldOrbit.getA().getField().getZero().add(DEFAULT_MASS));
    }

    /** Build a propagator from orbit and attitude provider.
     * <p>The central attraction coefficient μ is set to the same value
     * used for the initial orbit definition. Mass is set to an unspecified
     * non-null arbitrary value.</p>
     * @param initialFieldOrbit initial orbit
     * @param attitudeProv  attitude provider
     * @exception OrekitException if initial attitude cannot be computed
     */
    public FieldKeplerianPropagator(final FieldOrbit<T> initialFieldOrbit,
                                    final AttitudeProvider attitudeProv)
        throws OrekitException {
        this(initialFieldOrbit, attitudeProv, initialFieldOrbit.getMu(), initialFieldOrbit.getA().getField().getZero().add(DEFAULT_MASS));
    }

    /** Build a propagator from orbit, attitude provider and central attraction
     * coefficient μ.
     * <p>Mass is set to an unspecified non-null arbitrary value.</p>
     * @param initialFieldOrbit initial orbit
     * @param attitudeProv attitude provider
     * @param mu central attraction coefficient (m³/s²)
     * @exception OrekitException if initial attitude cannot be computed
     */
    public FieldKeplerianPropagator(final FieldOrbit<T> initialFieldOrbit,
                                    final AttitudeProvider attitudeProv,
                                    final double mu)
                                                    throws OrekitException {
        this(initialFieldOrbit, attitudeProv, mu, initialFieldOrbit.getA().getField().getZero().add(DEFAULT_MASS));
    }

    /** Build propagator from orbit, attitude provider, central attraction
     * coefficient μ and mass.
     * @param initialOrbit initial orbit
     * @param attitudeProv attitude provider
     * @param mu central attraction coefficient (m³/s²)
     * @param mass spacecraft mass (kg)
     * @exception OrekitException if initial attitude cannot be computed
     */
    public FieldKeplerianPropagator(final FieldOrbit<T> initialOrbit, final AttitudeProvider attitudeProv,
                                    final double mu, final T mass)
        throws OrekitException {

        super(initialOrbit.getA().getField(), attitudeProv);

        // ensure the orbit use the specified mu and has no non-Keplerian derivatives
        initialState = fixState(initialOrbit,
                                getAttitudeProvider().getAttitude(initialOrbit,
                                                                  initialOrbit.getDate(),
                                                                  initialOrbit.getFrame()),
                                mass, mu);
        states = new FieldTimeSpanMap<>(initialState, initialOrbit.getA().getField());
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
     * @return fixed orbit
     */
    private FieldSpacecraftState<T> fixState(final FieldOrbit<T> orbit, final FieldAttitude<T> attitude, final T mass,
                                     final double mu) {
        final OrbitType type = orbit.getType();
        final T[] stateVector = MathArrays.buildArray(mass.getField(), 6);
        type.mapOrbitToArray(orbit, PositionAngle.TRUE, stateVector, null);
        final FieldOrbit<T> fixedOrbit = type.mapArrayToOrbit(stateVector, null, PositionAngle.TRUE,
                                                              orbit.getDate(), mu, orbit.getFrame());
        return new FieldSpacecraftState<>(fixedOrbit, attitude, mass);
    }

    /** {@inheritDoc} */
    public void resetInitialState(final FieldSpacecraftState<T> state)
        throws OrekitException {

        // ensure the orbit use the specified mu and has no non-Keplerian derivatives
        final double mu = initialState == null ? state.getMu() : initialState.getMu();
        final FieldSpacecraftState<T> fixedState = fixState(state.getOrbit(),
                                                            state.getAttitude(),
                                                            state.getMass(),
                                                            mu);

        initialState = fixedState;
        states       = new FieldTimeSpanMap<>(initialState, state.getDate().getField());
        super.resetInitialState(fixedState);

    }

    /** {@inheritDoc} */
    protected void resetIntermediateState(final FieldSpacecraftState<T> state, final boolean forward)
        throws OrekitException {
        if (forward) {
            states.addValidAfter(state, state.getDate());
        } else {
            states.addValidBefore(state, state.getDate());
        }
    }

    /** {@inheritDoc} */
    protected FieldOrbit<T> propagateOrbit(final FieldAbsoluteDate<T> date)
        throws OrekitException {
        // propagate orbit
        FieldOrbit<T> orbit = states.get(date).getOrbit();
        do {
            // we use a loop here to compensate for very small date shifts error
            // that occur with long propagation time
            orbit = orbit.shiftedBy(date.durationFrom(orbit.getDate()));
        } while (!date.equals(orbit.getDate()));
        return orbit;
    }

    /** {@inheritDoc}*/
    protected T getMass(final FieldAbsoluteDate<T> date) {
        return states.get(date).getMass();
    }

}
