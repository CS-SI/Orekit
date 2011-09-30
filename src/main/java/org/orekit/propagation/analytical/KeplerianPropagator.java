/* Copyright 2002-2011 CS Communication & Systèmes
 * Licensed to CS Communication & Systèmes (CS) under one or more
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

import org.orekit.attitudes.AttitudeProvider;
import org.orekit.errors.OrekitException;
import org.orekit.errors.PropagationException;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.AbstractPropagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;

/** Simple keplerian orbit propagator.
 * @see Orbit
 * @author Guylaine Prat
 */
public class KeplerianPropagator extends AbstractPropagator {

    /** Serializable UID. */
    private static final long serialVersionUID = 2094439036855266946L;

    /** Initial state. */
    private SpacecraftState initialState;

    /** Build a propagator from orbit only.
     * <p>The central attraction coefficient &mu; is set to the same value used
     * for the initial orbit definition. Mass and attitude provider are set to
     * unspecified non-null arbitrary values.</p>
     * @param initialOrbit initial orbit
     * @exception PropagationException if initial attitude cannot be computed
     */
    public KeplerianPropagator(final Orbit initialOrbit)
        throws PropagationException {
        this(initialOrbit, DEFAULT_LAW, initialOrbit.getMu(), DEFAULT_MASS);
    }

    /** Build a propagator from orbit and central attraction coefficient &mu;.
     * <p>Mass and attitude provider are set to unspecified non-null arbitrary values.</p>
     * @param initialOrbit initial orbit
     * @param mu central attraction coefficient (m^3/s^2)
     * @exception PropagationException if initial attitude cannot be computed
     */
    public KeplerianPropagator(final Orbit initialOrbit, final double mu)
        throws PropagationException {
        this(initialOrbit, DEFAULT_LAW, mu, DEFAULT_MASS);
    }

    /** Build a propagator from orbit and attitude provider.
     * <p>The central attraction coefficient &mu; is set to the same value
     * used for the initial orbit definition. Mass is set to an unspecified
     * non-null arbitrary value.</p>
     * @param initialOrbit initial orbit
     * @param attitudeProv  attitude provider
     * @exception PropagationException if initial attitude cannot be computed
     */
    public KeplerianPropagator(final Orbit initialOrbit,
                               final AttitudeProvider attitudeProv)
        throws PropagationException {
        this(initialOrbit, attitudeProv, initialOrbit.getMu(), DEFAULT_MASS);
    }

    /** Build a propagator from orbit, attitude provider and central attraction
     * coefficient &mu;.
     * <p>Mass is set to an unspecified non-null arbitrary value.</p>
     * @param initialOrbit initial orbit
     * @param attitudeProv attitude provider
     * @param mu central attraction coefficient (m^3/s^2)
     * @exception PropagationException if initial attitude cannot be computed
     */
    public KeplerianPropagator(final Orbit initialOrbit,
                               final AttitudeProvider attitudeProv,
                               final double mu)
        throws PropagationException {
        this(initialOrbit, attitudeProv, mu, DEFAULT_MASS);
    }

    /** Build propagator from orbit, attitude provider, central attraction
     * coefficient &mu; and mass.
     * @param initialOrbit initial orbit
     * @param attitudeProv attitude provider
     * @param mu central attraction coefficient (m^3/s^2)
     * @param mass spacecraft mass (kg)
     * @exception PropagationException if initial attitude cannot be computed
     */
    public KeplerianPropagator(final Orbit initialOrbit, final AttitudeProvider attitudeProv,
                               final double mu, final double mass)
        throws PropagationException {

        super(attitudeProv);

        try {
            resetInitialState(new SpacecraftState(initialOrbit,
                                                   getAttitudeProvider().getAttitude(initialOrbit,
                                                                                     initialOrbit.getDate(),
                                                                                     initialOrbit.getFrame()),
                                                   mass));
        } catch (OrekitException oe) {
            throw new PropagationException(oe);
        }
    }

    /** {@inheritDoc} */
    public void resetInitialState(final SpacecraftState state)
        throws PropagationException {
        super.resetInitialState(state);
        initialState   = state;
    }

    /** {@inheritDoc} */
    protected Orbit propagateOrbit(final AbsoluteDate date)
        throws PropagationException {

        // propagate orbit
        Orbit orbit = initialState.getOrbit();
        do {
            // we use a loop here to compensate for very small date shifts error
            // that occur with long propagation time
            orbit = orbit.shiftedBy(date.durationFrom(orbit.getDate()));
        } while(!date.equals(orbit.getDate()));

        return orbit;

    }

    /** {@inheritDoc}*/
    protected double getMass(final AbsoluteDate date) {
        return initialState.getMass();
    }

}
