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

import org.apache.commons.math.ode.DerivativeException;
import org.apache.commons.math.ode.StepHandler;
import org.apache.commons.math.ode.StepInterpolator;
import org.orekit.attitudes.AttitudeLaw;
import org.orekit.errors.PropagationException;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;


/** This class is a space-dynamics aware step handler.
 *
 * <p>It mirrors the {@link org.apache.commons.math.ode.StepHandler
 * StepHandler} interface from <a href="http://commons.apache.org/math/">
 * commons-math</a> but provides a space-dynamics interface to the methods.</p>
 *
 * @version $Revision$ $Date$
 */
public abstract class OrekitStepHandler
    implements StepHandler, ModeHandler, Serializable {

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
     * @param interpolator interpolator set up for the current step
     * @param isLast if true, this is the last integration step
     * @exception PropagationException if step cannot be handled
     */
    public abstract void handleStep(final OrekitStepInterpolator interpolator,
                                    final boolean isLast)
        throws PropagationException;

    /** {@inheritDoc} */
    public abstract boolean requiresDenseOutput();

    /** {@inheritDoc} */
    public abstract void reset();

    /** {@inheritDoc} */
    public void handleStep(final StepInterpolator interpolator, final boolean isLast)
        throws DerivativeException {
        try {
            final OrekitStepInterpolator orekitInterpolator =
                new OrekitStepInterpolator(initializedReference, initializedFrame, initializedMu, initializedAttitudeLaw, interpolator);
            handleStep(orekitInterpolator, isLast);
        } catch (PropagationException pe) {
            throw new DerivativeException(pe);
        }
    }

}
