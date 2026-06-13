/* Copyright 2002-2026 CS GROUP
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
import org.orekit.frames.Frame;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.time.TimeScales;


/**
 * Class for Galileo almanac.
 *
 * @see "European GNSS (Galileo) Open Service, Signal In Space,
 *      Interface Control Document, Table 75"
 *
 * @author Bryan Cazabonne
 * @since 10.0
 *
 */
public class GalileoAlmanac extends GNSSOrbitalElements<GalileoAlmanac> {

    /** Nominal inclination (Ref: Galileo ICD - Table 75). */
    private static final double I0 = FastMath.toRadians(56.0);

    /** Nominal semi-major axis in meters (Ref: Galileo ICD - Table 75). */
    private static final double A0 = 29600000;

    /** Satellite E5a signal health status. */
    private final int healthE5a;

    /** Satellite E5b signal health status. */
    private final int healthE5b;

    /** Satellite E1-B/C signal health status. */
    private final int healthE1;

    /** Almanac Issue Of Data. */
    private final int iod;

    /**
     * Build a new almanac.
     * @param timeScales known time scales
     * @param system     satellite system to consider for interpreting week number
     *                   (may be different from real system, for example in Rinex nav, weeks
     *                   are always according to GPS)
     * @param prn        PRN number of the satellite
     * @param orbit      Keplerian orbit in Earth-frozen frame
     * @param aDot       change rate in semi-major axis (m/s)
     * @param deltaN0    delta of satellite mean motion
     * @param deltaN0Dot change rate in Δn₀
     * @param iDot       inclination rate (rad/s)
     * @param omegaDot   rate of right ascension (rad/s)
     * @param cuc        amplitude of the cosine harmonic correction term to the argument of latitude
     * @param cus        amplitude of the sine harmonic correction term to the argument of latitude
     * @param crc        amplitude of the cosine harmonic correction term to the orbit radius
     * @param crs        amplitude of the sine harmonic correction term to the orbit radius
     * @param cic        amplitude of the cosine harmonic correction term to the inclination
     * @param cis        amplitude of the sine harmonic correction term to the inclination
     * @param af0        zero-th order clock correction (s)
     * @param af1        first order clock correction (s/s)
     * @param af2        second order clock correction (s/s²)
     * @param tgd        group delay differential TGD for L1-L2 correction
     * @param toc        time of clock
     * @param healthE5a  satellite E5a signal health status
     * @param healthE5b  satellite E5b signal health status
     * @param healthE1   satellite E1-B/C signal health status
     * @param iod        issue of data
     */
    public GalileoAlmanac(final TimeScales timeScales, final SatelliteSystem system,
                          final int prn, final KeplerianOrbit orbit, final double aDot,
                          final double deltaN0, final double deltaN0Dot,
                          final double iDot, final double omegaDot,
                          final double cuc, final double cus,
                          final double crc, final double crs,
                          final double cic, final double cis,
                          final double af0, final double af1, final double af2,
                          final double tgd, final double toc,
                          final int healthE5a, final int healthE5b, final int healthE1, final int iod) {
        super(GNSSConstants.GALILEO_AV, GNSSConstants.GALILEO_WEEK_NB,
              timeScales, system, null,
              prn, orbit, aDot, deltaN0, deltaN0Dot, iDot, omegaDot, cuc, cus, crc, crs, cic, cis,
              af0, af1, af2, tgd, toc);
        this.healthE5a = healthE5a;
        this.healthE5b = healthE5b;
        this.healthE1 = healthE1;
        this.iod      = iod;
    }

    /** Constructor from field instance.
     * @param <T> type of the field elements
     * @param original regular field instance
     */
    public <T extends CalculusFieldElement<T>> GalileoAlmanac(final FieldGalileoAlmanac<T> original) {
        super(original);
        healthE5a = original.getHealthE5a();
        healthE5b = original.getHealthE5b();
        healthE1  = original.getHealthE1();
        iod       = original.getIOD();
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public <T extends CalculusFieldElement<T>, F extends FieldGnssOrbitalElements<T, GalileoAlmanac, F>>
        F toField(final FieldKeplerianOrbit<T> orbit) {
        return (F) new FieldGalileoAlmanac<>(orbit, this);
    }

    /** Get the E1-B/C signal health status.
     * @return the E1-B/C signal health status
     */
    public int getHealthE1() {
        return healthE1;
    }

    /** Get the E5a signal health status.
     * @return the E5a signal health status
     */
    public int getHealthE5a() {
        return healthE5a;
    }

    /** Get the E5b signal health status.
     * @return the E5b signal health status
     */
    public int getHealthE5b() {
        return healthE5b;
    }

    /** Get the Issue of Data (IOD).
     * @return the Issue Of Data
     */
    public int getIOD() {
        return iod;
    }

    /** {@inheritDoc} */
    @Override
    public GalileoAlmanacFactory baseFactory(final Frame inertial, final Frame bodyFixed) {
        return new GalileoAlmanacFactory(getTimeScales(), getSystem(), inertial, bodyFixed);
    }

}
