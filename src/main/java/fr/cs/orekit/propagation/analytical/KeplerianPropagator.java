/* Copyright 2002-2008 CS Communication & Systèmes
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fr.cs.orekit.propagation.analytical;

import fr.cs.orekit.attitudes.Attitude;
import fr.cs.orekit.attitudes.AttitudeLaw;
import fr.cs.orekit.attitudes.InertialLaw;
import fr.cs.orekit.errors.OrekitException;
import fr.cs.orekit.errors.PropagationException;
import fr.cs.orekit.orbits.EquinoctialOrbit;
import fr.cs.orekit.orbits.Orbit;
import fr.cs.orekit.propagation.Propagator;
import fr.cs.orekit.propagation.SpacecraftState;
import fr.cs.orekit.time.AbsoluteDate;

/** Simple keplerian orbit propagator.
 * @see Orbit
 * @author Guylaine Prat
 * @version $Revision$ $Date$
 */
public class KeplerianPropagator implements Propagator {

    /** Serializable UID. */
    private static final long serialVersionUID = -5001964704759409909L;

    /** Attitude law. */
    private final AttitudeLaw attitudeLaw;

    /** Initial orbit date. */
    private final AbsoluteDate initialDate;

    /** Initial orbit parameters. */
    private final EquinoctialOrbit initialParameters;

    /** Initial mass. */
    private final double mass;

    /** Mean motion. */
    private final double n;

    /** Build a new instance.
     * <p>This constructor allows to create a propagator from spacecraft state
     * and a specified attitude law.</p>
     * @param initialState initial state
     * @param attitudeLaw attitude law
     */
    public KeplerianPropagator(final SpacecraftState initialState,
                               final AttitudeLaw attitudeLaw) {
        this.initialDate       = initialState.getDate();
        this.initialParameters = new EquinoctialOrbit(initialState.getOrbit());
        this.mass              = initialState.getMass();
        final double a         = initialParameters.getA();
        this.n                 = Math.sqrt(initialState.getOrbit().getMu() / a) / a;
        this.attitudeLaw       = attitudeLaw;
    }

    /** Build a new instance.
     * <p>This constructor allows to create a propagator from spacecraft state.
     * Attitude law is set to a default inertial {@link fr.cs.orekit.frames.Frame
     * J<sub>2000</sub>} aligned law.</p>
     * @param initialState initial state
     */
    public KeplerianPropagator(final SpacecraftState initialState) {
        this(initialState, InertialLaw.J2000_ALIGNED);
    }

    /** Build a new instance.
     * <p>This constructor allows to create a propagator from orbit only,
     *  without spacecraft state. Mass is given an arbitrary value (1000 kg).
     *  Attitude law is the one specified.</p>
     * @param initialOrbit initial orbit
     * @param attitudeLaw attitude law
     */
    public KeplerianPropagator(final Orbit initialOrbit, final AttitudeLaw attitudeLaw) {
        this.initialDate       = initialOrbit.getDate();
        this.initialParameters = new EquinoctialOrbit(initialOrbit);
        this.mass              = 1000.0;
        final double a         = initialParameters.getA();
        this.n                 = Math.sqrt(initialOrbit.getMu() / a) / a;
        this.attitudeLaw       = attitudeLaw;
    }

    /** Build a new instance.
     * <p>This constructor allows to create a propagator from orbit only.
     * Mass is given an arbitrary value (1000 kg) and attitude law is set to
     * a default inertial {@link fr.cs.orekit.frames.Frame J<sub>2000</sub>}
     * aligned law.</p>
     * @param initialOrbit initial orbit
     */
    public KeplerianPropagator(final Orbit initialOrbit)  {
        this(initialOrbit, InertialLaw.J2000_ALIGNED);
    }

    /** {@inheritDoc} */
    public SpacecraftState propagate(final AbsoluteDate date)
        throws PropagationException {
        try {

            // evaluation of LM = PA + RAAN + M at extrapolated time
            final EquinoctialOrbit orbit =
                new EquinoctialOrbit(initialParameters.getA(), initialParameters.getEquinoctialEx(),
                                     initialParameters.getEquinoctialEy(), initialParameters.getHx(),
                                     initialParameters.getHy(),
                                     initialParameters.getLM() + n * date.minus(initialDate) ,
                                     EquinoctialOrbit.MEAN_LATITUDE_ARGUMENT,
                                     initialParameters.getFrame(),
                                     date, initialParameters.getMu());

            // evaluation of attitude
            final Attitude attitude = attitudeLaw.getState(date,
                                                           orbit.getPVCoordinates(),
                                                           orbit.getFrame());

            return new SpacecraftState(orbit, mass, attitude);

        } catch (OrekitException oe) {
            throw new PropagationException(oe.getMessage(), oe);
        }

    }

}
