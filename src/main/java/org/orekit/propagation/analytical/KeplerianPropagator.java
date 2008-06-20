/* Copyright 2002-2008 CS Communication & Systèmes
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
import org.orekit.orbits.EquinoctialOrbit;
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
    private static final long serialVersionUID = 9192576472088570265L;

    /** Default mass. */
    private static final double DEFAULT_MASS = 1000.0;

    /** Default attitude law. */
    private static final AttitudeLaw DEFAULT_LAW = InertialLaw.J2000_ALIGNED;

    /** Initial orbit. */
    private EquinoctialOrbit initialOrbit;

    /** Attitude law. */
    private final AttitudeLaw attitudeLaw;

    /** Central attraction coefficient (m^3/s^2). */
    private final double mu;

    /** Initial mass. */
    private double mass;

    /** Mean motion. */
    private double meanMotion;

    /** Build a propagator from orbit only.
     * <p>The central attraction coefficient &mu; is set to the same value used
     * for the initial orbit definition. Mass and attitude law are set to
     * unspecified non-null arbitrary values.</p>
     * @param initialOrbit initial orbit
     */
    public KeplerianPropagator(final Orbit initialOrbit)  {
        this(initialOrbit, DEFAULT_LAW, initialOrbit.getMu(), DEFAULT_MASS);
    }

    /** Build a propagator from orbit and central attraction coefficient &mu;.
     * <p>Mass and attitude law are set to unspecified non-null arbitrary values.</p>
     * @param initialOrbit initial orbit
     * @param mu central attraction coefficient (m^3/s^2)
     */
    public KeplerianPropagator(final Orbit initialOrbit, final double mu)  {
        this(initialOrbit, DEFAULT_LAW, mu, DEFAULT_MASS);
    }

    /** Build a propagator from orbit and attitude law.
     * <p>The central attraction coefficient &mu; is set to the same value
     * used for the initial orbit definition. Mass is set to an unspecified
     * non-null arbitrary value.</p>
     * @param initialOrbit initial orbit
     * @param attitudeLaw attitude law
     */
    public KeplerianPropagator(final Orbit initialOrbit,
                               final AttitudeLaw attitudeLaw) {
        this(initialOrbit, attitudeLaw, initialOrbit.getMu(), DEFAULT_MASS);
    }

    /** Build a propagator from orbit, attitude law and central attraction
     * coefficient &mu;.
     * <p>Mass is set to an unspecified non-null arbitrary value.</p>
     * @param initialOrbit initial orbit
     * @param attitudeLaw attitude law
     * @param mu central attraction coefficient (m^3/s^2)
     */
    public KeplerianPropagator(final Orbit initialOrbit,
                               final AttitudeLaw attitudeLaw,
                               final double mu) {
        this(initialOrbit, attitudeLaw, mu, DEFAULT_MASS);
    }

    /** Build propagator from orbit, attitude law, central attraction
     * coefficient &mu; and mass.
     * @param initialOrbit initial orbit
     * @param attitudeLaw attitude law
     * @param mu central attraction coefficient (m^3/s^2)
     * @param mass spacecraft mass (kg)
     */
    public KeplerianPropagator(final Orbit initialOrbit, final AttitudeLaw attitudeLaw,
                               final double mu, final double mass) {
        this.initialOrbit = new EquinoctialOrbit(initialOrbit);
        this.attitudeLaw  = attitudeLaw;
        this.mu           = mu;
        this.mass         = mass;
        final double a    = initialOrbit.getA();
        this.meanMotion   = Math.sqrt(mu / a) / a;
    }

    /** {@inheritDoc} */
    protected AbsoluteDate getInitialDate() {
        return initialOrbit.getDate();
    }

    /** {@inheritDoc} */
    protected SpacecraftState basicPropagate(final AbsoluteDate date)
        throws PropagationException {
        try {

            // evaluation of LM = PA + RAAN + M at extrapolated time
            final EquinoctialOrbit orbit =
                new EquinoctialOrbit(initialOrbit.getA(), initialOrbit.getEquinoctialEx(),
                                     initialOrbit.getEquinoctialEy(), initialOrbit.getHx(),
                                     initialOrbit.getHy(),
                                     initialOrbit.getLM() +
                                     meanMotion * date.minus(initialOrbit.getDate()) ,
                                     EquinoctialOrbit.MEAN_LATITUDE_ARGUMENT,
                                     initialOrbit.getFrame(), date, mu);

            // evaluation of attitude
            final Attitude attitude = attitudeLaw.getState(date,
                                                           orbit.getPVCoordinates(),
                                                           orbit.getFrame());

            return new SpacecraftState(orbit, attitude, mass);

        } catch (OrekitException oe) {
            throw new PropagationException(oe.getMessage(), oe);
        }

    }

    /** {@inheritDoc} */
    protected void resetInitialState(final SpacecraftState state)
        throws PropagationException {
        initialOrbit   = new EquinoctialOrbit(state.getOrbit());
        final double a = initialOrbit.getA();
        meanMotion     = Math.sqrt(mu / a) / a;
        mass           = state.getMass();
    }

}
