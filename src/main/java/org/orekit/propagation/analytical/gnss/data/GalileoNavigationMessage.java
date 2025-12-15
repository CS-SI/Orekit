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
import org.hipparchus.Field;
import org.orekit.frames.Frame;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScales;

/**
 * Container for data contained in a Galileo navigation message.
 * @author Bryan Cazabonne
 * @since 11.0
 */
public class GalileoNavigationMessage extends AbstractNavigationMessage<GalileoNavigationMessage> {

    /** Message type.
     * @since 14.0
     */
    public static final String INAV = "INAV";

    /** Message type.
     * @since 14.0
     */
    public static final String FNAV = "FNAV";

    /** Issue of Data of the navigation batch. */
    private final int iodNav;

    /** Data source.
     * @since 12.0
     */
    private final int dataSource;

    /** E1/E5a broadcast group delay (s). */
    private final double bgbE1E5a;

    /** E5b/E1 broadcast group delay (s). */
    private final double bgdE5bE1;

    /** Signal in space accuracy. */
    private final double sisa;

    /** Satellite health status. */
    private final double svHealth;

    /** Constructor.
     * @param timeScales       known time scales
     * @param system           satellite system to consider for interpreting week number
     *                         (may be different from real system, for example in Rinex nav, weeks
     *                         are always according to GPS)
     * @param type             message type
     * @param prn              PRN number of the satellite
     * @param week             reference Week of the orbit
     * @param orbit            Keplerian orbit in Earth-frozen frame
     * @param time             reference time
     * @param aDot             change rate in semi-major axis (m/s)
     * @param deltaN0          delta of satellite mean motion
     * @param deltaN0Dot       change rate in Δn₀
     * @param iDot             inclination rate (rad/s)
     * @param omegaDot         rate of right ascension (rad/s)
     * @param cuc              amplitude of the cosine harmonic correction term to the argument of latitude
     * @param cus              amplitude of the sine harmonic correction term to the argument of latitude
     * @param crc              amplitude of the cosine harmonic correction term to the orbit radius
     * @param crs              amplitude of the sine harmonic correction term to the orbit radius
     * @param cic              amplitude of the cosine harmonic correction term to the inclination
     * @param cis              amplitude of the sine harmonic correction term to the inclination
     * @param af0              zero-th order clock correction (s)
     * @param af1              first order clock correction (s/s)
     * @param af2              second order clock correction (s/s²)
     * @param tgd              group delay differential TGD for L1-L2 correction
     * @param toc              time of clock
     * @param epochToc         time of clock epoch
     * @param transmissionTime transmission time
     * @param iodNav           issue of Data of the navigation batch
     * @param dataSource       data source
     * @param bgbE1E5a         E1/E5a broadcast group delay (s)
     * @param bgdE5bE1         E5b/E1 broadcast group delay (s)
     * @param sisa             signal in space accuracy
     * @param svHealth         satellite health status
     */
    public GalileoNavigationMessage(final TimeScales timeScales, final SatelliteSystem system, final String type,
                                    final int prn, final int week, final KeplerianOrbit orbit,
                                    final double time, final double aDot,
                                    final double deltaN0, final double deltaN0Dot,
                                    final double iDot, final double omegaDot,
                                    final double cuc, final double cus,
                                    final double crc, final double crs,
                                    final double cic, final double cis,
                                    final double af0, final double af1, final double af2,
                                    final double tgd, final double toc,
                                    final AbsoluteDate epochToc, final double transmissionTime,
                                    final int iodNav, final int dataSource,
                                    final double bgbE1E5a, final double bgdE5bE1,
                                    final double sisa, final double svHealth) {
        super(GNSSConstants.GALILEO_AV, GNSSConstants.GALILEO_WEEK_NB,
              timeScales, system, type, prn, week, orbit,
              time, aDot, deltaN0, deltaN0Dot, iDot, omegaDot, cuc, cus, crc, crs, cic, cis,
              af0, af1, af2, tgd, toc, epochToc, transmissionTime);
        this.iodNav     = iodNav;
        this.dataSource = dataSource;
        this.bgbE1E5a   = bgbE1E5a;
        this.bgdE5bE1   = bgdE5bE1;
        this.sisa       = sisa;
        this.svHealth   = svHealth;
    }

    /** Constructor from field instance.
     * @param <T> type of the field elements
     * @param original regular field instance
     */
    public <T extends CalculusFieldElement<T>> GalileoNavigationMessage(final FieldGalileoNavigationMessage<T> original) {
        super(original);
        iodNav     = original.getIODNav();
        dataSource = original.getDataSource();
        bgbE1E5a   = original.getBGDE1E5a().getReal();
        bgdE5bE1   = original.getBGDE5bE1().getReal();
        sisa       = original.getSisa().getReal();
        svHealth   = original.getSvHealth().getReal();
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public <T extends CalculusFieldElement<T>, F extends FieldGnssOrbitalElements<T, GalileoNavigationMessage>>
        F toField(final Field<T> field) {
        return (F) new FieldGalileoNavigationMessage<>(field, this);
    }

    /** Get the Issue Of Data (IOD).
     * @return Issue Of Data (IOD)
     */
    public int getIODNav() {
        return iodNav;
    }

    /** Get the data source.
     * @return the data source
     * @since 12.0
     */
    public int getDataSource() {
        return dataSource;
    }

    /** Get the E1/E5a broadcast group delay.
     * @return the E1/E5a broadcast group delay (s)
     */
    public double getBGDE1E5a() {
        return bgbE1E5a;
    }

    /** Get the Broadcast Group Delay E5b/E1.
     * @return the Broadcast Group Delay E5b/E1 (s)
     */
    public double getBGDE5bE1() {
        return bgdE5bE1;
    }

    /** Get the signal in space accuracy (m).
     * @return the signal in space accuracy
     */
    public double getSisa() {
        return sisa;
    }

    /** Get the SV health status.
     * @return the SV health status
     */
    public double getSvHealth() {
        return svHealth;
    }

    /** {@inheritDoc} */
    @Override
    public GalileoNavigationMessageFactory baseFactory(final Frame inertial, final Frame bodyFixed) {
        return new GalileoNavigationMessageFactory(getTimeScales(), getSystem(), getType(),
                                                   inertial, bodyFixed, getDate());
    }

}
