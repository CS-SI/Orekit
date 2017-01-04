/* Copyright 2002-2016 CS Systèmes d'Information
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
import org.orekit.attitudes.FieldAttitudeProvider;
import org.orekit.attitudes.FieldInertialProvider;
import org.orekit.errors.OrekitException;
import org.orekit.orbits.FieldOrbit;
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
        this(initialFieldOrbit, new FieldInertialProvider<T>(initialFieldOrbit.getA().getField()), initialFieldOrbit.getMu(), initialFieldOrbit.getA().getField().getZero().add(DEFAULT_MASS));
    }

    /** Build a propagator from orbit and central attraction coefficient μ.
     * <p>Mass and attitude provider are set to unspecified non-null arbitrary values.</p>
     * @param initialFieldOrbit initial orbit
     * @param mu central attraction coefficient (m³/s²)
     * @exception OrekitException if initial attitude cannot be computed
     */
    public FieldKeplerianPropagator(final FieldOrbit<T> initialFieldOrbit, final double mu)
        throws OrekitException {
        this(initialFieldOrbit, new FieldInertialProvider<T>(initialFieldOrbit.getA().getField()), mu, initialFieldOrbit.getA().getField().getZero().add(DEFAULT_MASS));
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
                               final FieldAttitudeProvider<T> attitudeProv)
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
                               final FieldAttitudeProvider<T> attitudeProv,
                               final double mu)
        throws OrekitException {
        this(initialFieldOrbit, attitudeProv, mu, initialFieldOrbit.getA().getField().getZero().add(DEFAULT_MASS));
    }

    /** Build propagator from orbit, attitude provider, central attraction
     * coefficient μ and mass.
     * @param initialFieldOrbit initial orbit
     * @param attitudeProv attitude provider
     * @param mu central attraction coefficient (m³/s²)
     * @param mass spacecraft mass (kg)
     * @exception OrekitException if initial attitude cannot be computed
     */
    public FieldKeplerianPropagator(final FieldOrbit<T> initialFieldOrbit, final FieldAttitudeProvider<T> attitudeProv,
                               final double mu, final T mass)
        throws OrekitException {

        super(initialFieldOrbit.getA().getField(), attitudeProv);

        try {

            // ensure the orbit use the specified mu
            final OrbitType type = initialFieldOrbit.getType();
            final T[] stateVector = MathArrays.buildArray(initialFieldOrbit.getA().getField(), 6);
            type.mapOrbitToArray(initialFieldOrbit, PositionAngle.TRUE, stateVector);
            final FieldOrbit<T> orbit = type.mapArrayToOrbit(stateVector, PositionAngle.TRUE,
                                                     initialFieldOrbit.getDate(), mu, initialFieldOrbit.getFrame());

            resetInitialState(new FieldSpacecraftState<T>(orbit,
                                                   super.getAttitudeProvider().getAttitude(orbit,
                                                                                     orbit.getDate(),
                                                                                     orbit.getFrame()),
                                                   mass));

        } catch (OrekitException oe) {
            throw new OrekitException(oe);
        }
    }

    /** {@inheritDoc} */
    public void resetInitialState(final FieldSpacecraftState<T> state)
        throws OrekitException {
        super.resetInitialState(state);
        initialState   = state;
        states         = new FieldTimeSpanMap<FieldSpacecraftState<T>, T>(initialState, state.getA().getField());
    }

    /** {@inheritDoc} */
    protected void resetIntermediateState(final FieldSpacecraftState<T> state, final boolean forward)
        throws OrekitException {
     //   super.resetIntermediateState(state, forward);
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
