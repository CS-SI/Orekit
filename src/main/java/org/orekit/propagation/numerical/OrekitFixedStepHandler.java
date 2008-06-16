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

import java.io.Serializable;

import org.apache.commons.math.ode.FixedStepHandler;
import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.AttitudeLaw;
import org.orekit.errors.OrekitException;
import org.orekit.errors.PropagationException;
import org.orekit.frames.Frame;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;


/** This class is a space-dynamics aware fixed size step handler.
 *
 * <p>It mirrors the {@link org.apache.commons.math.ode.FixedStepHandler
 * FixedStepHandler} interface from <a href="http://commons.apache.org/math/">
 * commons-math</a> but provides a space-dynamics interface to the methods.</p>
 *
 * @version $Revision$ $Date$
 */
public abstract class OrekitFixedStepHandler
    implements FixedStepHandler, ModeHandler, Serializable {

    /** Reference date. */
    private AbsoluteDate initializedReference;

    /** Reference frame. */
    private Frame initializedFrame;

    /** Central body attraction coefficient. */
    private double initializedMu;

    /** Attitude law. */
    private AttitudeLaw initializedAttitudeLaw;

    /** {@inheritDoc} */
    public void initialize(final AbsoluteDate reference, final Frame frame,
                           final double mu, final AttitudeLaw attitudeLaw) {
        this.initializedReference   = reference;
        this.initializedFrame       = frame;
        this.initializedAttitudeLaw = attitudeLaw;
        this.initializedMu          = mu;
    }

    /** Handle the current step.
     * @param currentState current state at step time
     * @param isLast if true, this is the last integration step
     * @exception PropagationException if step cannot be handled
     */
    public abstract void handleStep(final SpacecraftState currentState, final boolean isLast)
        throws PropagationException;

    /** Check if the handler requires dense output.
     * @return true if the handler requires dense output
     * @see org.apache.commons.math.ode.StepHandler#requiresDenseOutput()
     */
    public abstract boolean requiresDenseOutput();

    /** Reset the step handler.
     * Initialize the internal data as required before the first step is
     * handled.
     * @see org.apache.commons.math.ode.StepHandler#reset()
     */
    public abstract void reset();

    /** {@inheritDoc}  */
    public void handleStep(final double t, final double[] y, final boolean isLast) {
        try {
            final AbsoluteDate current = new AbsoluteDate(initializedReference, t);
            final Orbit orbit =
                new EquinoctialOrbit(y[0], y[1], y[2], y[3], y[4], y[5],
                                          EquinoctialOrbit.TRUE_LATITUDE_ARGUMENT,
                                          initializedFrame, current, initializedMu);
            final Attitude attitude =
                initializedAttitudeLaw.getState(current, orbit.getPVCoordinates(), initializedFrame);
            final SpacecraftState state =
                new SpacecraftState(orbit, attitude, y[6]);
            handleStep(state, isLast);
        } catch (OrekitException e) {
            throw new RuntimeException(e.getLocalizedMessage());
        }

    }

}
