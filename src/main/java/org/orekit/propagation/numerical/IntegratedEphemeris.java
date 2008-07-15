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
package org.orekit.propagation.numerical;

import org.apache.commons.math.ode.ContinuousOutputModel;
import org.apache.commons.math.ode.DerivativeException;
import org.apache.commons.math.ode.sampling.StepHandler;
import org.apache.commons.math.ode.sampling.StepInterpolator;
import org.orekit.attitudes.AttitudeLaw;
import org.orekit.errors.OrekitException;
import org.orekit.errors.PropagationException;
import org.orekit.frames.Frame;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.propagation.BoundedPropagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;

/** This class stores sequentially generated orbital parameters for
 * later retrieval.
 *
 * <p>
 * Instances of this class are built and then must be fed with the results
 * provided by {@link org.orekit.propagation.Propagator Propagator} objects
 * configured in {@link org.orekit.propagation.Propagator#setEphemerisMode()
 * ephemeris generation mode}. Once propagation is o, random access to any
 * intermediate state of the orbit throughout the propagation range is possible.
 * </p>
 * <p>
 * A typical use case is for numerically integrated orbits, which can be used by
 * algorithms that need to wander around according to their own algorithm without
 * cumbersome tight links with the integrator.
 * </p>
 * <p>
 * Another use case is for persistence, as this class is serializable.
 * </p>
 * <p>
 * As this class implements the {@link org.orekit.propagation.Propagator Propagator}
 * interface, it can itself be used in batch mode to build another instance of the
 * same type. This is however not recommended since it would be a waste of resources.
 * </p>
 * <p>
 * Note that this class stores all intermediate states along with interpolation
 * models, so it may be memory intensive.
 * </p>
 *
 * @see org.orekit.propagation.numerical.NumericalPropagator
 * @author Mathieu Rom&eacute;ro
 * @author Luc Maisonobe
 * @author V&eacute;ronique Pommier-Maurussane
 * @version $Revision:1698 $ $Date:2008-06-18 16:01:17 +0200 (mer., 18 juin 2008) $
 */
class IntegratedEphemeris implements BoundedPropagator, ModeHandler, StepHandler {

    /** Serializable UID. */
    private static final long serialVersionUID = 3312011510740592660L;

    /** Reference date. */
    private AbsoluteDate initializedReference;

    /** Frame. */
    private Frame initializedFrame;

    /** Central body gravitational constant. */
    private double initializedMu;

    /** Attitude law. */
    private AttitudeLaw initializedAttitudeLaw;

    /** Start date of the integration (can be min or max). */
    private AbsoluteDate startDate;

    /** First date of the range. */
    private AbsoluteDate minDate;

    /** Last date of the range. */
    private AbsoluteDate maxDate;

    /** Underlying raw mathematical model. */
    private ContinuousOutputModel model;

    /** Creates a new instance of IntegratedEphemeris which must be
     *  filled by the propagator.
     */
    public IntegratedEphemeris() {
        this.model = new ContinuousOutputModel();
    }

    /** {@inheritDoc} */
    public void initialize(final AbsoluteDate reference,
                           final Frame frame, final double mu,
                           final AttitudeLaw attitudeLaw) {
        this.initializedReference   = reference;
        this.initializedFrame       = frame;
        this.initializedMu          = mu;
        this.initializedAttitudeLaw = attitudeLaw;

        // dates will be set when last step is handled
        startDate        = null;
        minDate          = null;
        maxDate          = null;

    }

    /** {@inheritDoc} */
    public SpacecraftState propagate(final AbsoluteDate date)
        throws PropagationException {
        model.setInterpolatedTime(date.minus(startDate));
        final double[] state = model.getInterpolatedState();

        final EquinoctialOrbit eq =
            new EquinoctialOrbit(state[0], state[1], state[2],
                                 state[3], state[4], state[5], 2, initializedFrame, date, initializedMu);
        final double mass = state[6];

        try {
            return new SpacecraftState(eq,
                                       initializedAttitudeLaw.getState(date, eq.getPVCoordinates(), initializedFrame),
                                       mass);
        } catch (OrekitException oe) {
            throw new PropagationException(oe.getMessage(), oe);
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

    /** {@inheritDoc} */
    public void handleStep(final StepInterpolator interpolator,
                           final boolean isLast)
        throws DerivativeException {
        model.handleStep(interpolator, isLast);
        if (isLast) {
            startDate = new AbsoluteDate(initializedReference, model.getInitialTime());
            maxDate   = new AbsoluteDate(initializedReference, model.getFinalTime());
            if (maxDate.minus(startDate) < 0) {
                minDate = maxDate;
                maxDate = startDate;
            } else {
                minDate = startDate;
            }
        }
    }

    /** {@inheritDoc} */
    public boolean requiresDenseOutput() {
        return model.requiresDenseOutput();
    }

    /** {@inheritDoc} */
    public void reset() {
        model.reset();
    }

}
