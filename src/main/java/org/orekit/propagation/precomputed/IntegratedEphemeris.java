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
package org.orekit.propagation.precomputed;

import org.apache.commons.math.ode.ContinuousOutputModel;
import org.apache.commons.math.ode.DerivativeException;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.errors.PropagationException;
import org.orekit.frames.Frame;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.AbstractPropagator;
import org.orekit.propagation.BoundedPropagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.numerical.StateMapper;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;

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
public class IntegratedEphemeris
    extends AbstractPropagator implements BoundedPropagator {

    /** Serializable UID. */
    private static final long serialVersionUID = -5009587563342612582L;

    /** Mapper between spacecraft state and simple array. */
    private final StateMapper mapper;

    /** Reference frame. */
    private final Frame referenceFrame;

    /** Central body gravitational constant. */
    private final double mu;

    /** Start date of the integration (can be min or max). */
    private final AbsoluteDate startDate;

    /** First date of the range. */
    private final AbsoluteDate minDate;

    /** Last date of the range. */
    private final AbsoluteDate maxDate;

    /** Underlying raw mathematical model. */
    private ContinuousOutputModel model;

    /** Creates a new instance of IntegratedEphemeris.
     * @param startDate Start date of the integration (can be minDate or maxDate)
     * @param minDate first date of the range
     * @param maxDate last date of the range
     * @param mapper mapper between spacecraft state and simple array
     * @param model underlying raw mathematical model
     * @param referenceFrame reference referenceFrame
     * @param mu central body attraction coefficient
     */
    public IntegratedEphemeris(final AbsoluteDate startDate,
                               final AbsoluteDate minDate, final AbsoluteDate maxDate,
                               final StateMapper mapper, final ContinuousOutputModel model,
                               final Frame referenceFrame, final double mu) {
        super(DEFAULT_LAW);
        this.startDate      = startDate;
        this.minDate        = minDate;
        this.maxDate        = maxDate;
        this.mapper         = mapper;
        this.model          = model;
        this.referenceFrame = referenceFrame;
        this.mu             = mu;
    }

    /** {@inheritDoc} */
    protected SpacecraftState basicPropagate(final AbsoluteDate date)
        throws PropagationException {
        try {
            if ((date.compareTo(minDate) < 0) || (date.compareTo(maxDate) > 0)) {
                throw new PropagationException(OrekitMessages.OUT_OF_RANGE_EPHEMERIDES_DATE,
                                               date, minDate, maxDate);
            }
            model.setInterpolatedTime(date.durationFrom(startDate));
            return mapper.mapArrayToState(model.getInterpolatedState(), date, mu, referenceFrame);
        } catch (DerivativeException de) {
            throw new PropagationException(de, de.getGeneralPattern(), de.getArguments());
        } catch (OrekitException oe) {
            throw new PropagationException(oe);
        }
    }

    /** {@inheritDoc} */
    protected Orbit propagateOrbit(final AbsoluteDate date)
        throws PropagationException {
        return basicPropagate(date).getOrbit();
    }

    /** {@inheritDoc} */
    protected double getMass(final AbsoluteDate date) throws PropagationException {
        return basicPropagate(date).getMass();
    }

    /** {@inheritDoc} */
    public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame)
        throws OrekitException {
        return propagate(date).getPVCoordinates(frame);
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
    public void resetInitialState(final SpacecraftState state)
        throws PropagationException {
        throw new PropagationException(OrekitMessages.NON_RESETABLE_STATE);
    }

    /** {@inheritDoc} */
    public SpacecraftState getInitialState() throws OrekitException {
        return basicPropagate(getMinDate());
    }

}
