/* Copyright 2002-2015 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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
package org.orekit.estimation.leastsquares;

import java.util.List;
import java.util.SortedSet;

import org.apache.commons.math3.fitting.leastsquares.MultivariateJacobianFunction;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.util.Pair;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitExceptionWrapper;
import org.orekit.estimation.Parameter;
import org.orekit.estimation.measurements.Evaluation;
import org.orekit.estimation.measurements.Measurement;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.conversion.PropagatorBuilder;
import org.orekit.propagation.events.DateDetector;
import org.orekit.time.AbsoluteDate;

/** Bridge between {@link Measurement measurements} and {@link
 * org.apache.commons.math3.fitting.leastsquares.LeastSquaresProblem
 * least squares problems}.
 * @author Luc Maisonobe
 * @since 7.1
 */
class Model implements MultivariateJacobianFunction {

    /** Builder for propagator. */
    private final PropagatorBuilder propagatorBuilder;

    /** Orbit date. */
    private final AbsoluteDate orbitDate;

    /** Measurements. */
    private final List<Measurement> measurements;

    /** Parameters. */
    private final SortedSet<Parameter> parameters;

    /** Date of the first enabled measurement. */
    private AbsoluteDate firstDate;

    /** Date of the last enabled measurement. */
    private AbsoluteDate lastDate;

    /** Simple constructor.
     * @param propagatorBuilder builder to user for propagation
     * @param orbitDate orbit date
     * @param parameters parameters
     * @param measurements measurements
     */
    public Model(final PropagatorBuilder propagatorBuilder, final AbsoluteDate orbitDate,
                 final SortedSet<Parameter> parameters, final List<Measurement> measurements) {
        this.propagatorBuilder = propagatorBuilder;
        this.orbitDate         = orbitDate;
        this.parameters        = parameters;
        this.measurements      = measurements;
    }

    /** {@inheritDoc} */
    @Override
    public Pair<RealVector, RealMatrix> value(final RealVector point)
        throws OrekitExceptionWrapper {
        try {

            final Propagator propagator = createPropagator(point);
            configureMeasurementsEvents(propagator);
            propagator.propagate(firstDate.shiftedBy(-1.0), lastDate.shiftedBy(+1.0));

            // TODO
            return null;

        } catch (OrekitException oe) {
            throw new OrekitExceptionWrapper(oe);
        }
    }

    /** Crate the propagator and parameters corresponding to an evaluation point.
     * @param point evaluation point
     * @return a new propagator
     * @exception OrekitException if orbit cannot be created with the current point
     */
    Propagator createPropagator(final RealVector point)
        throws OrekitException {

        // set up the propagator
        final Propagator propagator = propagatorBuilder.buildPropagator(orbitDate, point.toArray());

        // set up the measurement modifiers parameters
        int n = 6;
        for (final Parameter parameter : parameters) {
            if (parameter.isEstimated()) {
                final double[] parameterValue = new double[parameter.getDimension()];
                for (int i = 0; i < parameterValue.length; ++i) {
                    parameterValue[i] = point.getEntry(n + i);
                }
                parameter.setValue(parameterValue);
                n += parameter.getDimension();
            }
        }

        return propagator;

    }

    /** Configure the propagator to handle measurements.
     * @param propagator {@link Propagator} to configure
     * @exception OrekitException if orbit cannot be created with the current point
     */
    private void configureMeasurementsEvents(final Propagator propagator)
        throws OrekitException {

        firstDate = AbsoluteDate.FUTURE_INFINITY;
        lastDate  = AbsoluteDate.PAST_INFINITY;

        // set up events to handle measurements
        int p = 0;
        for (final Measurement measurement : measurements) {
            if (measurement.isEnabled()) {
                final AbsoluteDate       md = measurement.getDate();
                if (md.compareTo(firstDate) < 0) {
                    firstDate = md;
                }
                if (md.compareTo(lastDate) > 0) {
                    lastDate = md;
                }
                final MeasurementHandler mh = new MeasurementHandler(this, measurement, p);
                propagator.addEventDetector(new DateDetector(md).withHandler(mh));
                p += measurement.getDimension();
            }
        }

    }

    /** Fetch a measurement that was evaluated during propagation.
     * @param index index of the measurement first component
     * @param evaluation measurement evaluation
     */
    void fetchEvaluatedMeasurement(final int index, final Evaluation evaluation) {
        // TODO
    }

}
