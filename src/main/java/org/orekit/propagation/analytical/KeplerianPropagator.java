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

import org.hipparchus.linear.RealMatrix;
import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.FrameAlignedProvider;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.AbstractMatricesHarvester;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.DoubleArrayDictionary;
import org.orekit.utils.TimeSpanMap;

/** Simple Keplerian orbit propagator.
 * @see Orbit
 * @author Guylaine Prat
 */
public class KeplerianPropagator extends AbstractAnalyticalPropagator {

    /** All states. */
    private TimeSpanMap<SpacecraftState> states;

    /** Build a propagator from orbit only.
     * <p>The central attraction coefficient μ is set to the same value used
     * for the initial orbit definition. Mass and attitude provider are set to
     * unspecified non-null arbitrary values.</p>
     *
     * @param initialOrbit initial orbit
     * @see #KeplerianPropagator(Orbit, AttitudeProvider)
     */
    public KeplerianPropagator(final Orbit initialOrbit) {
        this(initialOrbit, FrameAlignedProvider.of(initialOrbit.getFrame()),
             initialOrbit.getMu(), DEFAULT_MASS);
    }

    /** Build a propagator from orbit and central attraction coefficient μ.
     * <p>Mass and attitude provider are set to unspecified non-null arbitrary values.</p>
     *
     * @param initialOrbit initial orbit
     * @param mu central attraction coefficient (m³/s²)
     * @see #KeplerianPropagator(Orbit, AttitudeProvider, double)
     */
    public KeplerianPropagator(final Orbit initialOrbit, final double mu) {
        this(initialOrbit, FrameAlignedProvider.of(initialOrbit.getFrame()),
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
        final SpacecraftState initial = fixState(initialOrbit,
                                                 getAttitudeProvider().getAttitude(initialOrbit,
                                                                                   initialOrbit.getDate(),
                                                                                   initialOrbit.getFrame()),
                                                 mass, mu, null, null);
        states = new TimeSpanMap<SpacecraftState>(initial);
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
     * @param additionalStatesDerivatives additional states derivatives (may be null)
     * @return fixed orbit
     */
    private SpacecraftState fixState(final Orbit orbit, final Attitude attitude, final double mass, final double mu,
                                     final DoubleArrayDictionary additionalStates,
                                     final DoubleArrayDictionary additionalStatesDerivatives) {
        final OrbitType type = orbit.getType();
        final double[] stateVector = new double[6];
        type.mapOrbitToArray(orbit, PositionAngleType.TRUE, stateVector, null);
        final Orbit fixedOrbit = type.mapArrayToOrbit(stateVector, null, PositionAngleType.TRUE,
                                                      orbit.getDate(), mu, orbit.getFrame());
        SpacecraftState fixedState = new SpacecraftState(fixedOrbit, attitude, mass);
        if (additionalStates != null) {
            for (final DoubleArrayDictionary.Entry entry : additionalStates.getData()) {
                fixedState = fixedState.addAdditionalState(entry.getKey(), entry.getValue());
            }
        }
        if (additionalStatesDerivatives != null) {
            for (final DoubleArrayDictionary.Entry entry : additionalStatesDerivatives.getData()) {
                fixedState = fixedState.addAdditionalStateDerivative(entry.getKey(), entry.getValue());
            }
        }
        return fixedState;
    }

    /** {@inheritDoc} */
    public void resetInitialState(final SpacecraftState state) {

        // ensure the orbit use the specified mu and has no non-Keplerian derivatives
        final SpacecraftState formerInitial = getInitialState();
        final double mu = formerInitial == null ? state.getMu() : formerInitial.getMu();
        final SpacecraftState fixedState = fixState(state.getOrbit(),
                                                    state.getAttitude(),
                                                    state.getMass(),
                                                    mu,
                                                    state.getAdditionalStatesValues(),
                                                    state.getAdditionalStatesDerivatives());

        states = new TimeSpanMap<SpacecraftState>(fixedState);
        super.resetInitialState(fixedState);

    }

    /** {@inheritDoc} */
    protected void resetIntermediateState(final SpacecraftState state, final boolean forward) {
        if (forward) {
            states.addValidAfter(state, state.getDate(), false);
        } else {
            states.addValidBefore(state, state.getDate(), false);
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

    /** {@inheritDoc} */
    @Override
    protected AbstractMatricesHarvester createHarvester(final String stmName, final RealMatrix initialStm,
                                                        final DoubleArrayDictionary initialJacobianColumns) {
        // Create the harvester
        final KeplerianHarvester harvester = new KeplerianHarvester(this, stmName, initialStm, initialJacobianColumns);
        // Update the list of additional state provider
        addAdditionalStateProvider(harvester);
        // Return the configured harvester
        return harvester;
    }

}
