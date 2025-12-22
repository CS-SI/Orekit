/* Copyright 2022-2025 Luc Maisonobe
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
import org.orekit.frames.Frame;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.GNSSDate;
import org.orekit.time.TimeScales;

import java.util.function.DoubleFunction;

/**
 * Container for data contained in a Beidou civilian navigation message.
 * @author Luc Maisonobe
 * @since 12.0
 */
public class BeidouCivilianNavigationMessage extends AbstractNavigationMessage<BeidouCivilianNavigationMessage> {

    /** Beidou civilian message type.
     * @since 14.0
     */
    private final BeidouCivilianType beidouType;

    /** Issue of Data, Ephemeris. */
    private final int iode;

    /** Issue of Data, Clock. */
    private final int iodc;

    /** Inter Signal Delay for B1 CD. */
    private final double iscB1CD;

    /** Inter Signal Delay for B1 CP. */
    private final double iscB1CP;

    /** Inter Signal Delay for B2 AD. */
    private final double iscB2AD;

    /** Signal In Space Accuracy Index (along track and across track). */
    private final int sisaiOe;

    /** Signal In Space Accuracy Index (radial and clock). */
    private final int sisaiOcb;

    /** Signal In Space Accuracy Index (clock drift accuracy). */
    private final int sisaiOc1;

    /** Signal In Space Accuracy Index (clock drift rate accuracy). */
    private final int sisaiOc2;

    /** Signal In Space Monitoring Accuracy Index. */
    private final int sismai;

    /** Health. */
    private final int health;

    /** Integrity flags. */
    private final int integrityFlags;

    /** B1/B3 Group Delay Differential (s). */
    private final double tgdB1Cp;

    /** B2 AP Group Delay Differential (s). */
    private final double tgdB2ap;

    /** B2B_i / B3I Group Delay Differential (s). */
    private final double tgdB2bI;

    /** Satellite type. */
    private final BeidouSatelliteType satelliteType;

    /**
     * Constructor.
     * @param beidouType       Beidou civilian message type
     * @param timeScales       known time scales
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
     * @param iode             issue of data, ephemeris
     * @param iodc             issue of data, clock
     * @param iscB1CD          inter signal delay for B1 CD
     * @param iscB1CP          inter signal delay for B1 CP
     * @param iscB2AD          inter signal delay for B2 AD
     * @param sisaiOe          signal in space accuracy index (along track and across track)
     * @param sisaiOcb         signal in space accuracy index (radial and clock)
     * @param sisaiOc1         signal in space accuracy index (clock drift accuracy)
     * @param sisaiOc2         signal in space accuracy index (clock drift rate accuracy)
     * @param sismai           signal in space monitoring accuracy index
     * @param health           health
     * @param integrityFlags   integrity flags
     * @param tgdB1Cp          B1/B3 Group Delay Differential (s)
     * @param tgdB2ap          B2 AP Group Delay Differential (s)
     * @param tgdB2bI          B2B_i / B3I Group Delay Differential (s)
     * @param satelliteType    satellite type
     */
    public BeidouCivilianNavigationMessage(final BeidouCivilianType beidouType,
                                           final TimeScales timeScales,  final int prn,
                                           final GNSSDate gnssDate, final KeplerianOrbit orbit,
                                           final double aDot, final double deltaN0, final double deltaN0Dot,
                                           final double iDot, final double omegaDot,
                                           final double cuc, final double cus,
                                           final double crc, final double crs,
                                           final double cic, final double cis,
                                           final double af0, final double af1, final double af2,
                                           final double tgd, final double toc,
                                           final AbsoluteDate epochToc, final double transmissionTime,
                                           final int iode, final int iodc,
                                           final double iscB1CD, final double iscB1CP, final double iscB2AD,
                                           final int sisaiOe, final int sisaiOcb,
                                           final int sisaiOc1, final int sisaiOc2,
                                           final int sismai, final int health, final int integrityFlags,
                                           final double tgdB1Cp, final double tgdB2ap, final double tgdB2bI,
                                           final BeidouSatelliteType satelliteType) {
        super(GNSSConstants.BEIDOU_AV, GNSSConstants.BEIDOU_WEEK_NB,
              timeScales, beidouType.name(), prn, gnssDate, orbit,
              aDot, deltaN0, deltaN0Dot, iDot, omegaDot, cuc, cus, crc, crs, cic, cis,
              af0, af1, af2, tgd, toc, epochToc, transmissionTime);
        this.beidouType = beidouType;
        this.iode           = iode;
        this.iodc           = iodc;
        this.iscB1CD        = iscB1CD;
        this.iscB1CP        = iscB1CP;
        this.iscB2AD        = iscB2AD;
        this.sisaiOe        = sisaiOe;
        this.sisaiOcb       = sisaiOcb;
        this.sisaiOc1       = sisaiOc1;
        this.sisaiOc2       = sisaiOc2;
        this.sismai         = sismai;
        this.health         = health;
        this.integrityFlags = integrityFlags;
        this.tgdB1Cp        = tgdB1Cp;
        this.tgdB2ap        = tgdB2ap;
        this.tgdB2bI        = tgdB2bI;
        this.satelliteType  = satelliteType;
    }

    /** Constructor from field instance.
     * @param <T> type of the field elements
     * @param original regular field instance
     */
    public <T extends CalculusFieldElement<T>> BeidouCivilianNavigationMessage(final FieldBeidouCivilianNavigationMessage<T> original) {
        super(original);
        this.beidouType     = original.getBeidouType();
        this.iode           = original.getIODE();
        this.iodc           = original.getIODC();
        this.iscB1CD        = original.getIscB1CD().getReal();
        this.iscB1CP        = original.getIscB1CP().getReal();
        this.iscB2AD        = original.getIscB2AD().getReal();
        this.sisaiOe        = original.getSisaiOe();
        this.sisaiOcb       = original.getSisaiOcb();
        this.sisaiOc1       = original.getSisaiOc1();
        this.sisaiOc2       = original.getSisaiOc2();
        this.sismai         = original.getSismai();
        this.health         = original.getHealth();
        this.integrityFlags = original.getIntegrityFlags();
        this.tgdB1Cp        = original.getTgdB1Cp().getReal();
        this.tgdB2ap        = original.getTgdB2ap().getReal();
        this.tgdB2bI        = original.getTgdB2bI().getReal();
        this.satelliteType  = original.getSatelliteType();

    }

    /** Get the Beidou civilian message type.
     * @return Beidou civilian message type
     * @since 14.0
     */
    public BeidouCivilianType getBeidouType() {
        return beidouType;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isCivilianMessage() {
        return true;
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public <T extends CalculusFieldElement<T>, P extends FieldGnssOrbitalElements<T, BeidouCivilianNavigationMessage, P>>
    P toField(final FieldKeplerianOrbit<T> orbit, final T[] nonKeplerian, final DoubleFunction<T> converter) {
        return (P) new FieldBeidouCivilianNavigationMessage<>(getBeidouType(),
                                                              getAngularVelocity(), getWeeksInCycle(), getTimeScales(),
                                                              getType(), getPrn(), getGnssDate(), orbit, nonKeplerian,
                                                              converter.apply(getTGD()),
                                                              converter.apply(getToc()),
                                                              new FieldAbsoluteDate<>(orbit.getMu().getField(),
                                                                                      getEpochToc()),
                                                              converter.apply(getTransmissionTime()),
                                                              getIODE(), getIODC(),
                                                              converter.apply(getIscB1CD()),
                                                              converter.apply(getIscB1CP()),
                                                              converter.apply(getIscB2AD()),
                                                              getSisaiOe(), getSisaiOcb(),
                                                              getSisaiOc1(), getSisaiOc2(),
                                                              getSismai(), getHealth(), getIntegrityFlags(),
                                                              converter.apply(getTgdB1Cp()),
                                                              converter.apply(getTgdB2ap()),
                                                              converter.apply(getTgdB2bI()),
                                                              getSatelliteType());
    }

    /** Get the Issue Of Data Ephemeris (IODE).
     * @return the Issue Of Data Ephemeris (IODE)
     */
    public int getIODE() {
        return iode;
    }

    /** Get the Issue Of Data Clock (IODC).
     * @return the Issue Of Data Clock (IODC)
     */
    public int getIODC() {
        return iodc;
    }

    /** Get inter Signal Delay for B1 CD.
     * @return inter signal delay
     */
    public double getIscB1CD() {
        return iscB1CD;
    }

    /** Get inter Signal Delay for B2 AD.
     * @return inter signal delay
     */
    public double getIscB2AD() {
        return iscB2AD;
    }

    /** Get inter Signal Delay for B1 CP.
     * @return inter signal delay
     */
    public double getIscB1CP() {
        return iscB1CP;
    }

    /** Get Signal In Space Accuracy Index (along track and across track).
     * @return Signal In Space Accuracy Index (along track and across track)
     */
    public int getSisaiOe() {
        return sisaiOe;
    }

    /** Get Signal In Space Accuracy Index (radial and clock).
     * @return Signal In Space Accuracy Index (radial and clock)
     */
    public int getSisaiOcb() {
        return sisaiOcb;
    }

    /** Get Signal In Space Accuracy Index (clock drift accuracy).
     * @return Signal In Space Accuracy Index (clock drift accuracy)
     */
    public int getSisaiOc1() {
        return sisaiOc1;
    }

    /** Get Signal In Space Accuracy Index (clock drift rate accuracy).
     * @return Signal In Space Accuracy Index (clock drift rate accuracy)
     */
    public int getSisaiOc2() {
        return sisaiOc2;
    }

    /** Get Signal In Space Monitoring Accuracy Index.
     * @return Signal In Space Monitoring Accuracy Index
     */
    public int getSismai() {
        return sismai;
    }

    /** Get health.
     * @return health
     */
    public int getHealth() {
        return health;
    }

    /** Get B1C integrity flags.
     * @return B1C integrity flags
     */
    public int getIntegrityFlags() {
        return integrityFlags;
    }

    /** Get B1/B3 Group Delay Differential (s).
     * @return B1/B3 Group Delay Differential (s)
     */
    public double getTgdB1Cp() {
        return tgdB1Cp;
    }

    /** Get B2 AP Group Delay Differential (s).
     * @return B2 AP Group Delay Differential (s)
     */
    public double getTgdB2ap() {
        return tgdB2ap;
    }

    /** Get B2B_i / B3I Group Delay Differential (s).
     * @return B2B_i / B3I Group Delay Differential (s)
     */
    public double getTgdB2bI() {
        return tgdB2bI;
    }

    /** Get satellite type.
     * @return satellite type
     */
    public BeidouSatelliteType getSatelliteType() {
        return satelliteType;
    }

    /** {@inheritDoc} */
    @Override
    public BeidouCivilianNavigationMessageFactory baseFactory(final Frame inertial, final Frame bodyFixed) {
        return new BeidouCivilianNavigationMessageFactory(getTimeScales(), getGnssDate().getSystem(), getType(),
                                                          inertial, bodyFixed, getBeidouType(), getSatelliteType());
    }

}
