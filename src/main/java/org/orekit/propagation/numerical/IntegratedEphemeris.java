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
package org.orekit.propagation.numerical;

import org.apache.commons.math.ode.ContinuousOutputModel;
import org.orekit.attitudes.AttitudeLaw;
import org.orekit.errors.OrekitException;
import org.orekit.errors.PropagationException;
import org.orekit.frames.Frame;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.propagation.BoundedPropagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;


/** This class stores numerically integrated orbital parameters for
 * later retrieval.
 *
 * <p>Instances of this class are built and then must be filled with the results
 * provided by {@link NumericalModel} objects in order to allow random
 * access to any intermediate state of the orbit throughout the integration range.
 * Numerically integrated orbits can therefore be used by algorithms that
 * need to wander around according to their own algorithm without cumbersome
 * tight link with the integrator.</p>
 *
 * <p> This class handles a {@link ContinuousOutputModel} and can be very
 *  voluminous. Refer to {@link ContinuousOutputModel} for more information.</p>
 *
 * @see NumericalModel
 * @author Mathieu Roméro
 * @author Luc Maisonobe
 * @author Véronique Pommier-Maurussane
 * @version $Revision$ $Date$
 */
public class IntegratedEphemeris implements BoundedPropagator {

    /** Serializable UID. */
    private static final long serialVersionUID = -5016030285313444680L;

    /** Central body gravitational constant. */
    private double mu;

    /** Attitude law. */
    private AttitudeLaw attitudeLaw;

    /** Start date of the integration (can be min or max). */
    private AbsoluteDate startDate;

    /** First date of the range. */
    private AbsoluteDate minDate;

    /** Last date of the range. */
    private AbsoluteDate maxDate;

    /** Underlying raw mathematical model. */
    private ContinuousOutputModel model;

    /** Frame. */
    private Frame frame;

    /** Indicator for initialized instances. */
    private boolean isInitialized;

    /** Creates a new instance of IntegratedEphemeris which must be
     *  filled by the propagator.
     */
    public IntegratedEphemeris() {
        isInitialized = false;
    }

    /** Initialize the ephemeris propagator.
     * @param model interpolation model containing the state evolution
     * @param ref reference date
     * @param frame frame in which state has been integrated
     * @param attitudeLaw attitude law
     * @param mu central body attraction coefficient
     */
    protected void initialize(// CHECKSTYLE: stop HiddenField check
                              final ContinuousOutputModel model, final AbsoluteDate ref,
                              final Frame frame,
                              final AttitudeLaw attitudeLaw, final double mu
                              // CHECKSTYLE: resume HiddenField check
                              ) {
        this.model     = model;
        this.frame = frame;
        this.attitudeLaw = attitudeLaw;
        this.mu = mu;
        startDate = new AbsoluteDate(ref, model.getInitialTime());
        maxDate = new AbsoluteDate(ref, model.getFinalTime());
        if (maxDate.minus(startDate) < 0) {
            minDate = maxDate;
            maxDate = startDate;
        } else {
            minDate = startDate;
        }
        this.isInitialized = true;
    }

    /** Get the orbit at a specific date.
     * @param date desired date for the orbit
     * @return the {@link SpacecraftState} at the specified date and null if not initialized.
     * @exception PropagationException if the date is outside of the range
     */
    public SpacecraftState propagate(final AbsoluteDate date)
        throws PropagationException {
        if (isInitialized) {
            model.setInterpolatedTime(date.minus(startDate));
            final double[] state = model.getInterpolatedState();

            final EquinoctialOrbit eq =
                new EquinoctialOrbit(state[0], state[1], state[2],
                                          state[3], state[4], state[5], 2, frame, date, mu);
            final double mass = state[6];

            try {
                return new SpacecraftState(eq, mass,
                                           attitudeLaw.getState(date, eq.getPVCoordinates(), frame));
            } catch (OrekitException oe) {
                throw new PropagationException(oe.getMessage(), oe);
            }
        } else {
            return null;
        }
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

}
