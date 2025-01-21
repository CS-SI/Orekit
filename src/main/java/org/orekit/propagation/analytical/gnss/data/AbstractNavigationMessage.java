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
package org.orekit.propagation.analytical.gnss.data;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.util.FastMath;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScales;

/**
 * Base class for GNSS navigation messages.
 * @param <O> type of the orbital elements
 * @author Bryan Cazabonne
 * @since 11.0
 *
 * @see GPSLegacyNavigationMessage
 * @see GalileoNavigationMessage
 * @see BeidouLegacyNavigationMessage
 * @see QZSSLegacyNavigationMessage
 * @see IRNSSNavigationMessage
 */
public abstract class AbstractNavigationMessage<O extends AbstractNavigationMessage<O>> extends AbstractAlmanac<O> {

    /** Mean Motion Difference from Computed Value. */
    private double deltaN;

    /** Time of clock epoch. */
    private AbsoluteDate epochToc;

    /** Transmission time.
     * @since 12.0
     */
    private double transmissionTime;

    /**
     * Constructor.
     * @param mu Earth's universal gravitational parameter
     * @param angularVelocity mean angular velocity of the Earth for the GNSS model
     * @param weeksInCycle number of weeks in the GNSS cycle
     * @param timeScales      known time scales
     * @param system          satellite system to consider for interpreting week number
     *                        (may be different from real system, for example in Rinex nav, weeks
     *                        are always according to GPS)
     */
    protected AbstractNavigationMessage(final double mu, final double angularVelocity, final int weeksInCycle,
                                        final TimeScales timeScales, final SatelliteSystem system) {
        super(mu, angularVelocity, weeksInCycle, timeScales, system);
    }

    /** Constructor from field instance.
     * @param <T> type of the field elements
     * @param <A> type of the orbital elements (non-field version)
     * @param original regular field instance
     */
    protected <T extends CalculusFieldElement<T>,
               A extends AbstractNavigationMessage<A>> AbstractNavigationMessage(final FieldAbstractNavigationMessage<T, A> original) {
        super(original);
        setDeltaN(original.getDeltaN().getReal());
        setEpochToc(original.getEpochToc().toAbsoluteDate());
        setTransmissionTime(original.getTransmissionTime().getReal());
    }

    /**
     * Getter for Square Root of Semi-Major Axis (√m).
     * @return Square Root of Semi-Major Axis (√m)
     */
    public double getSqrtA() {
        return FastMath.sqrt(getSma());
    }

    /**
     * Setter for the Square Root of Semi-Major Axis (√m).
     * <p>
     * In addition, this method set the value of the Semi-Major Axis.
     * </p>
     * @param sqrtA the Square Root of Semi-Major Axis (√m)
     */
    public void setSqrtA(final double sqrtA) {
        getSmaDriver().setValue(sqrtA * sqrtA);
    }

    /** {@inheritDoc} */
    @Override
    public double getMeanMotion() {
        return super.getMeanMotion() + deltaN;
    }

    /**
     * Getter for the delta of satellite mean motion.
     * @return delta of satellite mean motion
     */
    public double getDeltaN() {
        return deltaN;
    }

    /**
     * Setter for the delta of satellite mean motion.
     * @param deltaN the value to set
     */
    public void setDeltaN(final double deltaN) {
        this.deltaN = deltaN;
    }

    /**
     * Getter for the time of clock epoch.
     * @return the time of clock epoch
     */
    public AbsoluteDate getEpochToc() {
        return epochToc;
    }

    /**
     * Setter for the time of clock epoch.
     * @param epochToc the epoch to set
     */
    public void setEpochToc(final AbsoluteDate epochToc) {
        this.epochToc = epochToc;
    }

    /**
     * Getter for transmission time.
     * @return transmission time
     * @since 12.0
     */
    public double getTransmissionTime() {
        return transmissionTime;
    }

    /**
     * Setter for transmission time.
     * @param transmissionTime transmission time
     * @since 12.0
     */
    public void setTransmissionTime(final double transmissionTime) {
        this.transmissionTime = transmissionTime;
    }

}
