/* Copyright 2022-2025 Thales Alenia Space
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

import org.orekit.frames.Frame;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.time.GNSSDate;
import org.orekit.time.TimeScales;

/**
 * Factory for {@link BeidouCivilianNavigationMessage}.
 * @author Luc Maisonobe
 * @since 14.0
 */
public class BeidouCivilianNavigationMessageFactory
    extends AbstractNavigationMessageFactory<BeidouCivilianNavigationMessage> {

    /** Beidou civilian message type. */
    private final BeidouCivilianType beidouType;

    /** Satellite type. */
    private BeidouSatelliteType satelliteType;

    /** Issue of Data, Ephemeris. */
    private int iode;

    /** Issue of Data, Clock. */
    private int iodc;

    /** Inter Signal Delay for B1 CD. */
    private double iscB1CD;

    /** Inter Signal Delay for B1 CP. */
    private double iscB1CP;

    /** Inter Signal Delay for B2 AD. */
    private double iscB2AD;

    /** Signal In Space Accuracy Index (along track and across track). */
    private int sisaiOe;

    /** Signal In Space Accuracy Index (radial and clock). */
    private int sisaiOcb;

    /** Signal In Space Accuracy Index (clock drift accuracy). */
    private int sisaiOc1;

    /** Signal In Space Accuracy Index (clock drift rate accuracy). */
    private int sisaiOc2;

    /** Signal In Space Monitoring Accuracy Index. */
    private int sismai;

    /** Health. */
    private int health;

    /** Integrity flags. */
    private int integrityFlags;

    /** B1/B3 Group Delay Differential (s). */
    private double tgdB1Cp;

    /** B2 AP Group Delay Differential (s). */
    private double tgdB2ap;

    /** B2B_i / B3I Group Delay Differential (s). */
    private double tgdB2bI;

    /** Simple constructor.
     * @param timeScales    known time scales
     * @param system        satellite system to use for interpreting week number
     * @param type          message type (null if not a navigation message)
     * @param inertial      reference inertial frame
     * @param bodyFixed     body fixed frame (will be frozen at {@code date} to build the orbital elements
     * @param beidouType    Beidou civilian message type
     * @param satelliteType satellite type
     */
    public BeidouCivilianNavigationMessageFactory(final TimeScales timeScales, final SatelliteSystem system,
                                                  final String type, final Frame inertial, final Frame bodyFixed,
                                                  final BeidouCivilianType beidouType,
                                                  final BeidouSatelliteType satelliteType) {
        super(GNSSConstants.BEIDOU_AV, GNSSConstants.BEIDOU_WEEK_NB, timeScales, system,
              type, inertial, bodyFixed, GNSSConstants.BEIDOU_MU);
        this.beidouType    = beidouType;
        this.satelliteType = satelliteType;
    }

    /** Get the Beidou civilian message type.
     * @return Beidou civilian message type
     */
    public BeidouCivilianType getBeidouType() {
        return beidouType;
    }

    /** Get satellite type.
     * @return satellite type
     */
    public BeidouSatelliteType getSatelliteType() {
        return satelliteType;
    }

    /** Set satellite type.
     * @param satelliteType  satellite type
     */
    public void setSatelliteType(final BeidouSatelliteType satelliteType) {
        this.satelliteType = satelliteType;
    }

    /** Get the Issue Of Data Ephemeris (IODE).
     * @return the Issue Of Data Ephemeris (IODE)
     */
    public int getIODE() {
        return iode;
    }

    /** Set the Issue Of Data Ephemeris (IODE).
     * @param iode the Issue Of Data Ephemeris (IODE)
     */
    public void setIODE(final int iode) {
        this.iode = iode;
    }

    /** Get the Issue Of Data Clock (IODC).
     * @return the Issue Of Data Clock (IODC)
     */
    public int getIODC() {
        return iodc;
    }

    /** Set the Issue Of Data Clock (IODC).
     * @param iodc the Issue Of Data Clock (IODC)
     */
    public void setIODC(final int iodc) {
        this.iodc = iodc;
    }

    /** Get inter Signal Delay for B1 CD.
     * @return inter signal delay
     */
    public double getIscB1CD() {
        return iscB1CD;
    }

    /** Set inter Signal Delay for B1 CD.
     * @param iscB1CD inter signal delay
     */
    public void setIscB1CD(final double iscB1CD) {
        this.iscB1CD = iscB1CD;
    }

    /** Get inter Signal Delay for B2 AD.
     * @return inter signal delay
     */
    public double getIscB2AD() {
        return iscB2AD;
    }

    /** Set inter Signal Delay for B2 AD.
     * @param iscB2AD inter signal delay
     */
    public void setIscB2AD(final double iscB2AD) {
        this.iscB2AD = iscB2AD;
    }

    /** Get inter Signal Delay for B1 CP.
     * @return inter signal delay
     */
    public double getIscB1CP() {
        return iscB1CP;
    }

    /** Set inter Signal Delay for B1 CP.
     * @param iscB1CP inter signal delay
     */
    public void setIscB1CP(final double iscB1CP) {
        this.iscB1CP = iscB1CP;
    }

    /** Get Signal In Space Accuracy Index (along track and across track).
     * @return Signal In Space Accuracy Index (along track and across track)
     */
    public int getSisaiOe() {
        return sisaiOe;
    }

    /** Set Signal In Space Accuracy Index (along track and across track).
     * @param sisaiOe Signal In Space Accuracy Index (along track and across track)
     */
    public void setSisaiOe(final int sisaiOe) {
        this.sisaiOe = sisaiOe;
    }

    /** Get Signal In Space Accuracy Index (radial and clock).
     * @return Signal In Space Accuracy Index (radial and clock)
     */
    public int getSisaiOcb() {
        return sisaiOcb;
    }

    /** Set Signal In Space Accuracy Index (radial and clock).
     * @param sisaiOcb Signal In Space Accuracy Index (radial and clock)
     */
    public void setSisaiOcb(final int sisaiOcb) {
        this.sisaiOcb = sisaiOcb;
    }

    /** Get Signal In Space Accuracy Index (clock drift accuracy).
     * @return Signal In Space Accuracy Index (clock drift accuracy)
     */
    public int getSisaiOc1() {
        return sisaiOc1;
    }

    /** Set Signal In Space Accuracy Index (clock drift accuracy).
     * @param sisaiOc1 Signal In Space Accuracy Index (clock drift accuracy)
     */
    public void setSisaiOc1(final int sisaiOc1) {
        this.sisaiOc1 = sisaiOc1;
    }

    /** Get Signal In Space Accuracy Index (clock drift rate accuracy).
     * @return Signal In Space Accuracy Index (clock drift rate accuracy)
     */
    public int getSisaiOc2() {
        return sisaiOc2;
    }

    /** Set Signal In Space Accuracy Index (clock drift rate accuracy).
     * @param sisaiOc2 Signal In Space Accuracy Index (clock drift rate accuracy)
     */
    public void setSisaiOc2(final int sisaiOc2) {
        this.sisaiOc2 = sisaiOc2;
    }

    /** Get Signal In Space Monitoring Accuracy Index.
     * @return Signal In Space Monitoring Accuracy Index
     */
    public int getSismai() {
        return sismai;
    }

    /** Set Signal In Space Monitoring Accuracy Index.
     * @param sismai Signal In Space Monitoring Accuracy Index
     */
    public void setSismai(final int sismai) {
        this.sismai = sismai;
    }

    /** Get health.
     * @return health
     */
    public int getHealth() {
        return health;
    }

    /** Set health.
     * @param health health
     */
    public void setHealth(final int health) {
        this.health = health;
    }

    /** Get B1C integrity flags.
     * @return B1C integrity flags
     */
    public int getIntegrityFlags() {
        return integrityFlags;
    }

    /** Set B1C integrity flags.
     * @param integrityFlags B1C integrity flags
     */
    public void setIntegrityFlags(final int integrityFlags) {
        this.integrityFlags = integrityFlags;
    }

    /** Get B1/B3 Group Delay Differential (s).
     * @return B1/B3 Group Delay Differential (s)
     */
    public double getTgdB1Cp() {
        return tgdB1Cp;
    }

    /** Set B1/B3 Group Delay Differential (s).
     * @param tgdB1Cp B1/B3 Group Delay Differential (s)
     */
    public void setTgdB1Cp(final double tgdB1Cp) {
        this.tgdB1Cp = tgdB1Cp;
    }

    /** Get B2 AP Group Delay Differential (s).
     * @return B2 AP Group Delay Differential (s)
     */
    public double getTgdB2ap() {
        return tgdB2ap;
    }

    /** Set B2 AP Group Delay Differential (s).
     * @param tgdB2ap B2 AP Group Delay Differential (s)
     */
    public void setTgdB2ap(final double tgdB2ap) {
        this.tgdB2ap = tgdB2ap;
    }

    /** Get B2B_i / B3I Group Delay Differential (s).
     * @return B2B_i / B3I Group Delay Differential (s)
     */
    public double getTgdB2bI() {
        return tgdB2bI;
    }

    /** Set B2B_i / B3I Group Delay Differential (s).
     * @param tgdB2bI B2B_i / B3I Group Delay Differential (s)
     */
    public void setTgdB2bI(final double tgdB2bI) {
        this.tgdB2bI = tgdB2bI;
    }

    /** {@inheritDoc} */
    @Override
    public BeidouCivilianNavigationMessage createFromDrivers() {
        return new BeidouCivilianNavigationMessage(getBeidouType(),
                                                   getTimeScales(), getPrn(),
                                                   new GNSSDate(getWeek(), getTimeDriver().getValue(), getSystem()),
                                                   createOrbitFromDrivers(),
                                                   getADotDriver().getValue(),
                                                   getDeltaN0Driver().getValue(), getDeltaN0DotDriver().getValue(),
                                                   getIDotDriver().getValue(), getOmegaDotDriver().getValue(),
                                                   getCucDriver().getValue(), getCusDriver().getValue(),
                                                   getCrcDriver().getValue(), getCrsDriver().getValue(),
                                                   getCicDriver().getValue(), getCisDriver().getValue(),
                                                   getAf0Driver().getValue(), getAf1Driver().getValue(),
                                                   getAf2Driver().getValue(),
                                                   getTGD(), getToc(),
                                                   getEpochToc(), getTransmissionTime(),
                                                   getIODE(), getIODC(),
                                                   getIscB1CD(), getIscB1CP(), getIscB2AD(),
                                                   getSisaiOe(), getSisaiOcb(),
                                                   getSisaiOc1(), getSisaiOc2(),
                                                   getSismai(), getHealth(),
                                                   getIntegrityFlags(),
                                                   getTgdB1Cp(), getTgdB2ap(), getTgdB2bI(),
                                                   getSatelliteType());
    }

}
