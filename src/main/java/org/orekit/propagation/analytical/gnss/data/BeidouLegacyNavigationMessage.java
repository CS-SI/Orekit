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
import org.hipparchus.analysis.differentiation.Gradient;
import org.orekit.frames.Frame;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.GNSSDate;
import org.orekit.time.TimeScales;

/**
 * Container for data contained in a BeiDou navigation message.
 * @author Bryan Cazabonne
 * @since 11.0
 */
public class BeidouLegacyNavigationMessage extends AbstractNavigationMessage<BeidouLegacyNavigationMessage> {

    /** Identifier for message type. */
    public static final String D1 = "D1";

    /** Identifier for message type. */
    public static final String D2 = "D2";

    /** Indicator for D2 messages.
     * @since 14.0
     */
    private final boolean d2;

    /** Age of Data, Ephemeris. */
    private final int aode;

    /** Age of Data, Clock. */
    private final int aodc;

    /** Health identifier.
     * @since 14.0
     */
    private final int satH1;

    /** B1/B3 Group Delay Differential (s). */
    private final double tgd1;

    /** B2/B3 Group Delay Differential (s). */
    private final double tgd2;

    /** The user SV accuracy (m). */
    private final double svAccuracy;

    /** Constructor.
     * @param d2               indicator for D2 messages
     * @param timeScales       known time scales
     * @param type             message type
     * @param prn              PRN number of the satellite
     * @param gnssDate         GNSS date (<em>must</em> be consistent with {@code orbit})
     * @param orbit            Keplerian orbit in Earth-frozen frame
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
     * @param aode             age of data, ephemeris
     * @param aodc             age of data, clock
     * @param satH1            health identifier
     * @param tgd1             B1/B3 Group Delay Differential (s)
     * @param tgd2             B2/B3 Group Delay Differential (s)
     * @param svAccuracy       user SV accuracy (m)
     */
    public BeidouLegacyNavigationMessage(final boolean d2,
                                         final TimeScales timeScales, final String type,
                                         final int prn, final GNSSDate gnssDate, final KeplerianOrbit orbit,
                                         final double aDot, final double deltaN0, final double deltaN0Dot,
                                         final double iDot, final double omegaDot,
                                         final double cuc, final double cus,
                                         final double crc, final double crs,
                                         final double cic, final double cis,
                                         final double af0, final double af1, final double af2,
                                         final double tgd, final double toc,
                                         final AbsoluteDate epochToc, final double transmissionTime,
                                         final int aode, final int aodc, final int satH1,
                                         final double tgd1, final double tgd2, final double svAccuracy) {
        super(GNSSConstants.BEIDOU_AV, GNSSConstants.BEIDOU_WEEK_NB,
              timeScales, type, prn, gnssDate, orbit,
              aDot, deltaN0, deltaN0Dot, iDot, omegaDot, cuc, cus, crc, crs, cic, cis,
              af0, af1, af2, tgd, toc, epochToc, transmissionTime);
        this.d2         = d2;
        this.aode       = aode;
        this.aodc       = aodc;
        this.satH1      = satH1;
        this.tgd1       = tgd1;
        this.tgd2       = tgd2;
        this.svAccuracy = svAccuracy;
    }

    /** Constructor from field instance.
     * @param <T> type of the field elements
     * @param original regular field instance
     */
    public <T extends CalculusFieldElement<T>> BeidouLegacyNavigationMessage(final FieldBeidouLegacyNavigationMessage<T> original) {
        super(original);
        d2         = original.isD2();
        aode       = original.getAODE();
        aodc       = original.getAODC();
        satH1      = original.getSatH1();
        tgd1       = original.getTGD1().getReal();
        tgd2       = original.getTGD2().getReal();
        svAccuracy = original.getSvAccuracy().getReal();
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public <T extends CalculusFieldElement<T>, F extends FieldGnssOrbitalElements<T, BeidouLegacyNavigationMessage, F>>
        F toField(final Field<T> field) {
        return (F) new FieldBeidouLegacyNavigationMessage<>(new FieldKeplerianOrbit<>(field, getOrbit()), this);
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public <P extends FieldGnssOrbitalElements<Gradient, BeidouLegacyNavigationMessage, P>>
        P toGradient(final FieldKeplerianOrbit<Gradient> orbit, final NonKeplerianDriversFactory nonKeplerian) {
        final int freeParameters = orbit.getMu().getFreeParameters();
        return (P) new FieldBeidouLegacyNavigationMessage<>(isD2(),
                                                            getAngularVelocity(), getWeeksInCycle(), getTimeScales(),
                                                            getType(), getPrn(), getGnssDate(), orbit,
                                                            nonKeplerian.toGradients(freeParameters),
                                                            Gradient.constant(freeParameters, getTGD()),
                                                            Gradient.constant(freeParameters, getToc()),
                                                            new FieldAbsoluteDate<>(orbit.getMu().getField(),
                                                                                    getEpochToc()),
                                                            Gradient.constant(freeParameters, getTransmissionTime()),
                                                            getAODE(), getAODC(), getSatH1(),
                                                            Gradient.constant(freeParameters, getTGD1()),
                                                            Gradient.constant(freeParameters, getTGD2()),
                                                            Gradient.constant(freeParameters, getSvAccuracy()));
    }

    /**
     * Check if message is a D2 message.
     * @return true if message is a D2 message
     * @since 14.0
     */
    public boolean isD2() {
        return d2;
    }

    /**
     * Getter for the Age Of Data Clock (AODC).
     * @return the Age Of Data Clock (AODC)
     */
    public int getAODC() {
        return aodc;
    }

    /**
     * Getter for the Age Of Data Ephemeris (AODE).
     * @return the Age Of Data Ephemeris (AODE)
     */
    public int getAODE() {
        return aode;
    }

    /**
     * Getter for the estimated group delay differential TGD1 for B1I signal.
     * @return the estimated group delay differential TGD1 for B1I signal (s)
     */
    public double getTGD1() {
        return tgd1;
    }

    /**
     * Getter for the estimated group delay differential TGD for B2I signal.
     * @return the estimated group delay differential TGD2 for B2I signal (s)
     */
    public double getTGD2() {
        return tgd2;
    }

    /**
     * Getter for the user SV accuray (meters).
     * @return the user SV accuracy
     */
    public double getSvAccuracy() {
        return svAccuracy;
    }

    /** Get the health identifier.
     * @return health identifier
     * @since 14.0
     */
    public int getSatH1() {
        return satH1;
    }

    /** {@inheritDoc} */
    @Override
    public BeidouLegacyNavigationMessageFactory baseFactory(final Frame inertial, final Frame bodyFixed) {
        return new BeidouLegacyNavigationMessageFactory(getTimeScales(), getGnssDate().getSystem(), getType(),
                                                        inertial, bodyFixed, isD2());
    }

}
