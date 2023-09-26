/* Copyright 2002-2023 CS GROUP
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


import java.util.Collections;
import java.util.List;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.util.MathArrays;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.FieldAttitude;
import org.orekit.attitudes.FrameAlignedProvider;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldArrayDictionary;
import org.orekit.utils.FieldTimeSpanMap;
import org.orekit.utils.ParameterDriver;

/** Simple Keplerian orbit propagator.
 * @see FieldOrbit
 * @author Guylaine Prat
 * @param <T> type of the field elements
 */
public class FieldKeplerianPropagator<T extends CalculusFieldElement<T>> extends FieldAbstractAnalyticalPropagator<T> {


    /** All states. */
    private transient FieldTimeSpanMap<FieldSpacecraftState<T>, T> states;

    /** Build a propagator from orbit only.
     * <p>The central attraction coefficient μ is set to the same value used
     * for the initial orbit definition. Mass and attitude provider are set to
     * unspecified non-null arbitrary values.</p>
     *
     * @param initialFieldOrbit initial orbit
     * @see #FieldKeplerianPropagator(FieldOrbit, AttitudeProvider)
     */
    public FieldKeplerianPropagator(final FieldOrbit<T> initialFieldOrbit) {
        this(initialFieldOrbit, FrameAlignedProvider.of(initialFieldOrbit.getFrame()),
             initialFieldOrbit.getMu(), initialFieldOrbit.getA().getField().getZero().add(DEFAULT_MASS));
    }

    /** Build a propagator from orbit and central attraction coefficient μ.
     * <p>Mass and attitude provider are set to unspecified non-null arbitrary values.</p>
     *
     * @param initialFieldOrbit initial orbit
     * @param mu central attraction coefficient (m³/s²)
     * @see #FieldKeplerianPropagator(FieldOrbit, AttitudeProvider, CalculusFieldElement)
     */
    public FieldKeplerianPropagator(final FieldOrbit<T> initialFieldOrbit, final T mu) {
        this(initialFieldOrbit, FrameAlignedProvider.of(initialFieldOrbit.getFrame()),
             mu, initialFieldOrbit.getA().getField().getZero().add(DEFAULT_MASS));
    }

    /** Build a propagator from orbit and attitude provider.
     * <p>The central attraction coefficient μ is set to the same value
     * used for the initial orbit definition. Mass is set to an unspecified
     * non-null arbitrary value.</p>
     * @param initialFieldOrbit initial orbit
     * @param attitudeProv  attitude provider
     */
    public FieldKeplerianPropagator(final FieldOrbit<T> initialFieldOrbit,
                                    final AttitudeProvider attitudeProv) {
        this(initialFieldOrbit, attitudeProv, initialFieldOrbit.getMu(), initialFieldOrbit.getA().getField().getZero().add(DEFAULT_MASS));
    }

    /** Build a propagator from orbit, attitude provider and central attraction
     * coefficient μ.
     * <p>Mass is set to an unspecified non-null arbitrary value.</p>
     * @param initialFieldOrbit initial orbit
     * @param attitudeProv attitude provider
     * @param mu central attraction coefficient (m³/s²)
     */
    public FieldKeplerianPropagator(final FieldOrbit<T> initialFieldOrbit,
                                    final AttitudeProvider attitudeProv,
                                    final T mu) {
        this(initialFieldOrbit, attitudeProv, mu, initialFieldOrbit.getA().getField().getZero().add(DEFAULT_MASS));
    }

    /** Build propagator from orbit, attitude provider, central attraction
     * coefficient μ and mass.
     * @param initialOrbit initial orbit
     * @param attitudeProv attitude provider
     * @param mu central attraction coefficient (m³/s²)
     * @param mass spacecraft mass (kg)
     */
    public FieldKeplerianPropagator(final FieldOrbit<T> initialOrbit, final AttitudeProvider attitudeProv,
                                    final T mu, final T mass) {

        super(initialOrbit.getA().getField(), attitudeProv);

        // ensure the orbit use the specified mu and has no non-Keplerian derivatives
        final FieldSpacecraftState<T> initial = fixState(initialOrbit,
                                                         getAttitudeProvider().getAttitude(initialOrbit,
                                                                                           initialOrbit.getDate(),
                                                                                           initialOrbit.getFrame()),
                                                         mass, mu, null, null);
        states = new FieldTimeSpanMap<>(initial, initialOrbit.getA().getField());
        super.resetInitialState(initial);
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
     * @param additionalStates additional states (may be null)
     * @param additionalStatesderivatives additional states derivatives (may be null)
     * @return fixed orbit
     */
    private FieldSpacecraftState<T> fixState(final FieldOrbit<T> orbit, final FieldAttitude<T> attitude, final T mass, final T mu,
                                             final FieldArrayDictionary<T> additionalStates,
                                             final FieldArrayDictionary<T> additionalStatesderivatives) {
        final OrbitType type = orbit.getType();
        final T[] stateVector = MathArrays.buildArray(mass.getField(), 6);
        type.mapOrbitToArray(orbit, PositionAngleType.TRUE, stateVector, null);
        final FieldOrbit<T> fixedOrbit = type.mapArrayToOrbit(stateVector, null, PositionAngleType.TRUE,
                                                              orbit.getDate(), mu, orbit.getFrame());
        FieldSpacecraftState<T> fixedState = new FieldSpacecraftState<>(fixedOrbit, attitude, mass);
        if (additionalStates != null) {
            for (final FieldArrayDictionary<T>.Entry entry : additionalStates.getData()) {
                fixedState = fixedState.addAdditionalState(entry.getKey(), entry.getValue());
            }
        }
        if (additionalStatesderivatives != null) {
            for (final FieldArrayDictionary<T>.Entry entry : additionalStatesderivatives.getData()) {
                fixedState = fixedState.addAdditionalStateDerivative(entry.getKey(), entry.getValue());
            }
        }
        return fixedState;
    }

    /** {@inheritDoc} */
    public void resetInitialState(final FieldSpacecraftState<T> state) {

        // ensure the orbit use the specified mu and has no non-Keplerian derivatives
        final FieldSpacecraftState<T> formerInitial = getInitialState();
        final T mu = formerInitial == null ? state.getMu() : formerInitial.getMu();
        final FieldSpacecraftState<T> fixedState = fixState(state.getOrbit(),
                                                            state.getAttitude(),
                                                            state.getMass(),
                                                            mu,
                                                            state.getAdditionalStatesValues(),
                                                            state.getAdditionalStatesDerivatives());

        states = new FieldTimeSpanMap<>(fixedState, state.getDate().getField());
        super.resetInitialState(fixedState);

    }

    /** {@inheritDoc} */
    protected void resetIntermediateState(final FieldSpacecraftState<T> state, final boolean forward) {
        if (forward) {
            states.addValidAfter(state, state.getDate());
        } else {
            states.addValidBefore(state, state.getDate());
        }
        stateChanged(state);
    }

    /** {@inheritDoc} */
    protected FieldOrbit<T> propagateOrbit(final FieldAbsoluteDate<T> date, final T[] parameters) {
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

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        // Keplerian propagation model does not have parameter drivers.
        return Collections.emptyList();
    }

}
