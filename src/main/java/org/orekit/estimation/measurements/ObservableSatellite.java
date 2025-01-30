/* Copyright 2002-2025 CS GROUP
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

import org.hipparchus.util.FastMath;
import org.orekit.utils.ParameterDriver;

/** Class modeling a satellite that can be observed.
 *
 * @author Luc Maisonobe
 * @since 9.3
 */
public class ObservableSatellite {

    /** Prefix for clock offset parameter driver, the propagator index will be appended to it. */
    public static final String CLOCK_OFFSET_PREFIX = "clock-offset-";

    /** Prefix for clock drift parameter driver, the propagator index will be appended to it. */
    public static final String CLOCK_DRIFT_PREFIX = "clock-drift-";

    /** Prefix for clock acceleration parameter driver, the propagator index will be appended to it.
     * @since 12.1
     */
    public static final String CLOCK_ACCELERATION_PREFIX = "clock-acceleration-";

    /** Clock offset scaling factor.
     * <p>
     * We use a power of 2 to avoid numeric noise introduction
     * in the multiplications/divisions sequences.
     * </p>
     */
    private static final double CLOCK_OFFSET_SCALE = FastMath.scalb(1.0, -10);

    /** Prefix for satellite names. */
    private static final String SAT_PREFIX = "sat-";

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

    /** Name of the satellite.
     * @since 13.0
     */
    private final String name;

    /** Simple constructor.
     * <p>
     * This constructor builds a default name based on the propagator index.
     * </p>
     * @param propagatorIndex index of the propagator related to this satellite
     */
    public ObservableSatellite(final int propagatorIndex) {
        this(propagatorIndex, null);
    }

    /** Simple constructor.
     * @param propagatorIndex index of the propagator related to this satellite
     * @param name satellite name (if null, a default name built from index will be used)
     * @since 13.0
     */
    public ObservableSatellite(final int propagatorIndex, final String name) {
        this.propagatorIndex   = propagatorIndex;
        this.name              = name == null ? SAT_PREFIX + propagatorIndex : name;
        this.clockOffsetDriver = new ParameterDriver(CLOCK_OFFSET_PREFIX + this.name,
                                                     0.0, CLOCK_OFFSET_SCALE,
                                                     Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
        this.clockDriftDriver = new ParameterDriver(CLOCK_DRIFT_PREFIX + this.name,
                                                    0.0, CLOCK_OFFSET_SCALE,
                                                    Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
        this.clockAccelerationDriver = new ParameterDriver(CLOCK_ACCELERATION_PREFIX + this.name,
                                                           0.0, CLOCK_OFFSET_SCALE,
                                                           Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
    }

    /** Build a name for the satellite.
     * <p>
     * This is mainly useful to build the arguments for {@link
     * org.orekit.estimation.measurements.gnss.AmbiguityCache#getAmbiguity(String,
     * String, double)}
     * </p>
     * @return name for the satellite
     * @since 12.1
     */
    public String getName() {
        return name;
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
     * @return quadratic clock model
     * @since 12.1
     */
    public QuadraticClockModel getQuadraticClockModel() {
        return new QuadraticClockModel(clockOffsetDriver,
                                       clockDriftDriver,
                                       clockAccelerationDriver);
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
