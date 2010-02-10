/* Copyright 2002-2010 CS Communication & Systèmes
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

import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.AttitudeLaw;
import org.orekit.attitudes.InertialLaw;
import org.orekit.errors.OrekitException;
import org.orekit.errors.PropagationException;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.AbstractPropagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;

/** Simple keplerian orbit propagator.
 * @see Orbit
 * @author Guylaine Prat
 * @version $Revision:1665 $ $Date:2008-06-11 12:12:59 +0200 (mer., 11 juin 2008) $
 */
public class KeplerianPropagator extends AbstractPropagator {

    /** Serializable UID. */
    private static final long serialVersionUID = 2094439036855266946L;

    /** Default mass. */
    private static final double DEFAULT_MASS = 1000.0;

    /** Default attitude law. */
    private static final AttitudeLaw DEFAULT_LAW = InertialLaw.EME2000_ALIGNED;

    /** Initial state. */
    private SpacecraftState initialState;

    /** Attitude law. */
    private final AttitudeLaw attitudeLaw;

    /** Initial mass. */
    private double mass;

    /** Build a propagator from orbit only.
     * <p>The central attraction coefficient &mu; is set to the same value used
     * for the initial orbit definition. Mass and attitude law are set to
     * unspecified non-null arbitrary values.</p>
     * @param initialOrbit initial orbit
     * @exception PropagationException if initial attitude cannot be computed
     */
    public KeplerianPropagator(final Orbit initialOrbit)
        throws PropagationException {
        this(initialOrbit, DEFAULT_LAW, initialOrbit.getMu(), DEFAULT_MASS);
    }

    /** Build a propagator from orbit and central attraction coefficient &mu;.
     * <p>Mass and attitude law are set to unspecified non-null arbitrary values.</p>
     * @param initialOrbit initial orbit
     * @param mu central attraction coefficient (m^3/s^2)
     * @exception PropagationException if initial attitude cannot be computed
     */
    public KeplerianPropagator(final Orbit initialOrbit, final double mu)
        throws PropagationException {
        this(initialOrbit, DEFAULT_LAW, mu, DEFAULT_MASS);
    }

    /** Build a propagator from orbit and attitude law.
     * <p>The central attraction coefficient &mu; is set to the same value
     * used for the initial orbit definition. Mass is set to an unspecified
     * non-null arbitrary value.</p>
     * @param initialOrbit initial orbit
     * @param attitudeLaw attitude law
     * @exception PropagationException if initial attitude cannot be computed
     */
    public KeplerianPropagator(final Orbit initialOrbit,
                               final AttitudeLaw attitudeLaw)
        throws PropagationException {
        this(initialOrbit, attitudeLaw, initialOrbit.getMu(), DEFAULT_MASS);
    }

    /** Build a propagator from orbit, attitude law and central attraction
     * coefficient &mu;.
     * <p>Mass is set to an unspecified non-null arbitrary value.</p>
     * @param initialOrbit initial orbit
     * @param attitudeLaw attitude law
     * @param mu central attraction coefficient (m^3/s^2)
     * @exception PropagationException if initial attitude cannot be computed
     */
    public KeplerianPropagator(final Orbit initialOrbit,
                               final AttitudeLaw attitudeLaw,
                               final double mu)
        throws PropagationException {
        this(initialOrbit, attitudeLaw, mu, DEFAULT_MASS);
    }

    /** Build propagator from orbit, attitude law, central attraction
     * coefficient &mu; and mass.
     * @param initialOrbit initial orbit
     * @param attitudeLaw attitude law
     * @param mu central attraction coefficient (m^3/s^2)
     * @param mass spacecraft mass (kg)
     * @exception PropagationException if initial attitude cannot be computed
     */
    public KeplerianPropagator(final Orbit initialOrbit, final AttitudeLaw attitudeLaw,
                               final double mu, final double mass)
        throws PropagationException {
        try {
            this.initialState = new SpacecraftState(initialOrbit, attitudeLaw.getAttitude(initialOrbit), mass);
            this.attitudeLaw  = attitudeLaw;
            this.mass         = mass;
        } catch (OrekitException oe) {
            throw new PropagationException(oe.getLocalizedMessage(), oe);
        }
    }

    /** {@inheritDoc} */
    public SpacecraftState getInitialState() {
        return initialState;
    }

    /** {@inheritDoc} */
    protected SpacecraftState basicPropagate(final AbsoluteDate date)
        throws PropagationException {
        try {

            // evaluation of orbit
            final Orbit initialOrbit = initialState.getOrbit();
            final Orbit orbit = initialOrbit.shiftedBy(date.durationFrom(initialOrbit.getDate()));

            // evaluation of attitude
            final Attitude attitude = attitudeLaw.getAttitude(orbit);

            return new SpacecraftState(orbit, attitude, mass);

        } catch (OrekitException oe) {
            throw new PropagationException(oe.getLocalizedMessage(), oe);
        }

    }

    /** {@inheritDoc} */
    public void resetInitialState(final SpacecraftState state)
        throws PropagationException {
        initialState   = state;
        mass           = state.getMass();
    }

}
