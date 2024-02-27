/* Copyright 2002-2024 CS GROUP
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
package org.orekit.estimation.measurements;

import java.util.Map;

import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.gnss.QuadraticClockModel;
import org.orekit.gnss.QuadraticFieldClockModel;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.ParameterDriver;

/** Class modeling a satellite that can be observed.
 *
 * @author Luc Maisonobe
 * @since 9.3
 */
public class ObservableSatellite {

    /** Prefix for clock offset parameter driver, the propagator index will be appended to it. */
    public static final String CLOCK_OFFSET_PREFIX = "clock-offset-satellite-";

    /** Prefix for clock drift parameter driver, the propagator index will be appended to it. */
    public static final String CLOCK_DRIFT_PREFIX = "clock-drift-satellite-";

    /** Prefix for clock acceleration parameter driver, the propagator index will be appended to it.
     * @since 12.1
     */
    public static final String CLOCK_ACCELERATION_PREFIX = "clock-acceleration-satellite-";

    /** Clock offset scaling factor.
     * <p>
     * We use a power of 2 to avoid numeric noise introduction
     * in the multiplications/divisions sequences.
     * </p>
     */
    private static final double CLOCK_OFFSET_SCALE = FastMath.scalb(1.0, -10);

    /** Index of the propagator related to this satellite. */
    private final int propagatorIndex;

    /** Parameter driver for satellite clock offset. */
    private final ParameterDriver clockOffsetDriver;

    /** Parameter driver for satellite clock drift. */
    private final ParameterDriver clockDriftDriver;

    /** Parameter driver for satellite clock acceleration.
     * @since 12.1
     */
    private final ParameterDriver clockAccelerationDriver;

    /** Simple constructor.
     * @param propagatorIndex index of the propagator related to this satellite
     */
    public ObservableSatellite(final int propagatorIndex) {
        this.propagatorIndex   = propagatorIndex;
        this.clockOffsetDriver = new ParameterDriver(CLOCK_OFFSET_PREFIX + propagatorIndex,
                                                     0.0, CLOCK_OFFSET_SCALE,
                                                     Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
        this.clockDriftDriver = new ParameterDriver(CLOCK_DRIFT_PREFIX + propagatorIndex,
                                                    0.0, CLOCK_OFFSET_SCALE,
                                                    Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
        this.clockAccelerationDriver = new ParameterDriver(CLOCK_ACCELERATION_PREFIX + propagatorIndex,
                                                           0.0, CLOCK_OFFSET_SCALE,
                                                           Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
    }

    /** Get the index of the propagator related to this satellite.
     * @return index of the propagator related to this satellite
     */
    public int getPropagatorIndex() {
        return propagatorIndex;
    }

    /** Get the clock offset parameter driver.
     * <p>
     * The offset value is defined as the value in seconds that must be <em>subtracted</em> from
     * the satellite clock reading of time to compute the real physical date. The offset
     * is therefore negative if the satellite clock is slow and positive if it is fast.
     * </p>
     * @return clock offset parameter driver
     */
    public ParameterDriver getClockOffsetDriver() {
        return clockOffsetDriver;
    }

    /** Get the clock drift parameter driver.
     * <p>
     * The drift is negative if the satellite clock is slowing down and positive if it is speeding up.
     * </p>
     * @return clock drift parameter driver
     * @since 10.3
     */
    public ParameterDriver getClockDriftDriver() {
        return clockDriftDriver;
    }

    /** Get the clock acceleration parameter driver.
     * @return clock acceleration parameter driver
     * @since 12.1
     */
    public ParameterDriver getClockAccelerationDriver() {
        return clockAccelerationDriver;
    }

    /** Get a quadratic clock model valid at some date.
     * @param date date at which the quadratic model should be valid
     * @return quadratic clock model
     * @since 12.1
     */
    public QuadraticClockModel getQuadraticClockModel(final AbsoluteDate date) {
        final double a0            = clockOffsetDriver.getValue(date);
        final double a1            = clockDriftDriver.getValue(date);
        final double a2            = clockAccelerationDriver.getValue(date);
        AbsoluteDate referenceDate = clockOffsetDriver.getReferenceDate();
        if (referenceDate == null) {
            if (a1 == 0 && a2 == 0) {
                // it is OK to not have a reference date is clock offset is constant
                referenceDate = date;
            } else {
                throw new OrekitException(OrekitMessages.NO_REFERENCE_DATE_FOR_PARAMETER,
                                          clockOffsetDriver.getName());
            }
        }
        return new QuadraticClockModel(referenceDate, a0, a1, a2);
    }

    /** Get a quadratic clock model valid at some date.
     * @param freeParameters total number of free parameters in the gradient
     * @param indices indices of the differentiation parameters in derivatives computations,
     * must be span name and not driver name
     * @param date date at which the quadratic model should be valid
     * @return quadratic clock model
     * @since 12.1
      */
    public QuadraticFieldClockModel<Gradient> getQuadraticClockModel(final int freeParameters, final Map<String, Integer> indices,
                                                                     final AbsoluteDate date) {
        final Gradient a0            = clockOffsetDriver.getValue(freeParameters, indices, date);
        final Gradient a1            = clockDriftDriver.getValue(freeParameters, indices, date);
        final Gradient a2            = clockAccelerationDriver.getValue(freeParameters, indices, date);
        AbsoluteDate   referenceDate = clockOffsetDriver.getReferenceDate();
        if (referenceDate == null) {
            if (a1.getReal() == 0 && a2.getReal() == 0) {
                // it is OK to not have a reference date is clock offset is constant
                referenceDate = date;
            } else {
                throw new OrekitException(OrekitMessages.NO_REFERENCE_DATE_FOR_PARAMETER,
                                          clockOffsetDriver.getName());
            }
        }
        return new QuadraticFieldClockModel<>(new FieldAbsoluteDate<>(a0.getField(), referenceDate), a0, a1, a2);
    }

    /** {@inheritDoc}
     * @since 12.0
     */
    @Override
    public boolean equals(final Object other) {
        if (other instanceof ObservableSatellite) {
            return propagatorIndex == ((ObservableSatellite) other).propagatorIndex;
        } else {
            return false;

        }
    }

    /** {@inheritDoc}
     * @since 12.0
     */
    @Override
    public int hashCode() {
        return propagatorIndex;
    }

}
