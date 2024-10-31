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
package org.orekit.propagation.analytical.gnss.data;

import org.orekit.gnss.SatelliteSystem;
import org.orekit.time.TimeScales;
import org.orekit.utils.ParameterDriver;

/** Container for common GNSS data contained in almanac and navigation messages.
 * @author Bryan Cazabonne
 * @since 11.0
 */
public class CommonGnssData extends GNSSOrbitalElements implements GNSSClockElements {

    /** Name for zero-th order clock correction parameter.
     * @since 13.0
     */
    public static final String AF0 = "GnssClock0";

    /** Name for first order clock correction parameter.
     * @since 13.0
     */
    public static final String AF1 = "GnssClock1";

    /** Name for second order clock correction parameter.
     * @since 13.0
     */
    public static final String AF2 = "GnssClock2";

    /** SV zero-th order clock correction (s). */
    private final ParameterDriver af0Driver;

    /** SV first order clock correction (s/s). */
    private final ParameterDriver af1Driver;

    /** SV second order clock correction (s/s²). */
    private final ParameterDriver af2Driver;

    /** Group delay differential TGD for L1-L2 correction. */
    private double tgd;

    /** Time Of Clock. */
    private double toc;

    /**
     * Constructor.
     * @param mu Earth's universal gravitational parameter
     * @param angularVelocity mean angular velocity of the Earth for the GNSS model
     * @param weeksInCycle number of weeks in the GNSS cycle
     * @param timeScales      known time scales
     * @param system          satellite system to consider for interpreting week number
     *                        (may be different from real system, for exmple in Rinex nav weeks
     *                        are always according to GPS)
     */
    public CommonGnssData(final double mu, final double angularVelocity, final int weeksInCycle,
                          final TimeScales timeScales, final SatelliteSystem system) {
        super(mu, angularVelocity, weeksInCycle, timeScales, system);
        this.af0Driver = createDriver(AF0);
        this.af1Driver = createDriver(AF1);
        this.af2Driver = createDriver(AF2);
    }

    /** {@inheritDoc} */
    @Override
    public double getAf0() {
        return af0Driver.getValue();
    }

    /**
     * Setter for the SV Clock Bias Correction Coefficient (s).
     * @param af0 the SV Clock Bias Correction Coefficient to set
     */
    public void setAf0(final double af0) {
        af0Driver.setValue(af0);
    }

    /** {@inheritDoc} */
    @Override
    public double getAf1() {
        return af1Driver.getValue();
    }

    /**
     * Setter for the SV Clock Drift Correction Coefficient (s/s).
     * @param af1 the SV Clock Drift Correction Coefficient to set
     */
    public void setAf1(final double af1) {
        af1Driver.setValue(af1);
    }

    /** {@inheritDoc} */
    @Override
    public double getAf2() {
        return af2Driver.getValue();
    }

    /**
     * Setter for the Drift Rate Correction Coefficient (s/s²).
     * @param af2 the Drift Rate Correction Coefficient to set
     */
    public void setAf2(final double af2) {
        af2Driver.setValue(af2);
    }

    /**
     * Set the estimated group delay differential TGD for L1-L2 correction.
     * @param tgd the estimated group delay differential TGD for L1-L2 correction (s)
     */
    public void setTGD(final double tgd) {
        this.tgd = tgd;
    }

    /** {@inheritDoc} */
    @Override
    public double getTGD() {
        return tgd;
    }

    /**
     * Set the time of clock.
     * @param toc the time of clock (s)
     * @see #getAf0()
     * @see #getAf1()
     * @see #getAf2()
     */
    public void setToc(final double toc) {
        this.toc = toc;
    }

    /** {@inheritDoc} */
    @Override
    public double getToc() {
        return toc;
    }

}
