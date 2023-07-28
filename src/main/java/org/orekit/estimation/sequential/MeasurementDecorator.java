/* Copyright 2002-2023 CS GROUP
 * Licensed to CS GROUP (CS) under one or more
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
package org.orekit.estimation.sequential;

import org.hipparchus.filtering.kalman.Measurement;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.linear.RealVector;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.time.AbsoluteDate;


/** Decorator adding {@link Measurement} API to an {@link ObservedMeasurement}.
 * @author Luc Maisonobe
 * @since 9.2
 */
public class MeasurementDecorator implements Measurement {

    /** Wrapped observed measurement. */
    private final ObservedMeasurement<?> observedMeasurement;

    /** Covariance. */
    private final RealMatrix covariance;

    /** Reference date. */
    private final AbsoluteDate reference;

    /** Simple constructor.
     * @param observedMeasurement observed measurement
     * @param covariance measurement covariance
     * @param reference reference date
     */
    public MeasurementDecorator(final ObservedMeasurement<?> observedMeasurement,
                                final RealMatrix covariance, final AbsoluteDate reference) {
        this.observedMeasurement = observedMeasurement;
        this.covariance          = covariance;
        this.reference           = reference;
    }

    /** Get the observed measurement.
     * @return observed measurement
     */
    public ObservedMeasurement<?> getObservedMeasurement() {
        return observedMeasurement;
    }

    /** {@inheritDoc} */
    @Override
    public double getTime() {
        return observedMeasurement.getDate().durationFrom(reference);
    }

    /** {@inheritDoc} */
    @Override
    public RealVector getValue() {
        return MatrixUtils.createRealVector(observedMeasurement.getObservedValue());
    }

    /** {@inheritDoc} */
    @Override
    public RealMatrix getCovariance() {
        return covariance;
    }

}
