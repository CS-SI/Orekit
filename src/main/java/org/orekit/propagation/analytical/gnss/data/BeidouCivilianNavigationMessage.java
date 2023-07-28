/* Copyright 2023 Luc Maisonobe
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

import org.orekit.gnss.Frequency;

/**
 * Container for data contained in a Beidou civilian navigation message.
 * @author Luc Maisonobe
 * @since 12.0
 */
public class BeidouCivilianNavigationMessage extends AbstractNavigationMessage {

    /** Identifier for Beidou-3 B1C message type. */
    public static final String CNV1 = "CNV1";

    /** Identifier for Beidou-3 B2A message type. */
    public static final String CNV2 = "CNV2";

    /** Identifier for Beidou-3 B2B message type. */
    public static final String CNV3 = "CNV3";

    /** Signal on which navigation signal is sent. */
    private final Frequency signal;

    /** Change rate in semi-major axis (m/s). */
    private double aDot;

    /** Change rate in Δn₀. */
    private double deltaN0Dot;

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

    /** Satellite type. */
    private BeidouSatelliteType satelliteType;

    /**
     * Constructor.
     * @param signal signal on which navigation signal is sent
     */
    public BeidouCivilianNavigationMessage(final Frequency signal) {
        super(GNSSConstants.BEIDOU_MU, GNSSConstants.BEIDOU_AV, GNSSConstants.BEIDOU_WEEK_NB);
        this.signal = signal;
    }

    /**
     * Getter for signal.
     * @return signal on which navigation signal is sent
     */
    public Frequency getSignal() {
        return signal;
    }

    /**
     * Getter for the change rate in semi-major axis.
     * @return the change rate in semi-major axis
     */
    public double getADot() {
        return aDot;
    }

    /**
     * Setter for the change rate in semi-major axis.
     * @param value the change rate in semi-major axis
     */
    public void setADot(final double value) {
        this.aDot = value;
    }

    /**
     * Getter for change rate in Δn₀.
     * @return change rate in Δn₀
     */
    public double getDeltaN0Dot() {
        return deltaN0Dot;
    }

    /**
     * Setter for change rate in Δn₀.
     * @param deltaN0Dot change rate in Δn₀
     */
    public void setDeltaN0Dot(final double deltaN0Dot) {
        this.deltaN0Dot = deltaN0Dot;
    }

    /**
     * Getter for the Issue Of Data Ephemeris (IODE).
     * @return the Issue Of Data Ephemeris (IODE)
     */
    public int getIODE() {
        return iode;
    }

    /**
     * Setter for the Issue of Data Ephemeris.
     * @param value the IODE to set
     */
    public void setIODE(final int value) {
        this.iode = value;
    }

    /**
     * Getter for the Issue Of Data Clock (IODC).
     * @return the Issue Of Data Clock (IODC)
     */
    public int getIODC() {
        return iodc;
    }

    /**
     * Setter for the Issue of Data Clock.
     * @param value the IODC to set
     */
    public void setIODC(final int value) {
        this.iodc = value;
    }

    /**
     * Getter for inter Signal Delay for B1 CD.
     * @return inter signal delay
     */
    public double getIscB1CD() {
        return iscB1CD;
    }

    /**
     * Setter for inter Signal Delay for B1 CD.
     * @param delay delay to set
     */
    public void setIscB1CD(final double delay) {
        this.iscB1CD = delay;
    }

    /**
     * Getter for inter Signal Delay for B2 AD.
     * @return inter signal delay
     */
    public double getIscB2AD() {
        return iscB2AD;
    }

    /**
     * Setter for inter Signal Delay for B2 AD.
     * @param delay delay to set
     */
    public void setIscB2AD(final double delay) {
        this.iscB2AD = delay;
    }

    /**
     * Getter for inter Signal Delay for B1 CP.
     * @return inter signal delay
     */
    public double getIscB1CP() {
        return iscB1CP;
    }

    /**
     * Setter for inter Signal Delay for B1 CP.
     * @param delay delay to set
     */
    public void setIscB1CP(final double delay) {
        this.iscB1CP = delay;
    }

    /**
     * Getter for Signal In Space Accuracy Index (along track and across track).
     * @return Signal In Space Accuracy Index (along track and across track)
     */
    public int getSisaiOe() {
        return sisaiOe;
    }

    /**
     * Setter for Signal In Space Accuracy Index (along track and across track).
     * @param sisaiOe Signal In Space Accuracy Index (along track and across track)
     */
    public void setSisaiOe(final int sisaiOe) {
        this.sisaiOe = sisaiOe;
    }

    /**
     * Getter for Signal In Space Accuracy Index (radial and clock).
     * @return Signal In Space Accuracy Index (radial and clock)
     */
    public int getSisaiOcb() {
        return sisaiOcb;
    }

    /**
     * Setter for Signal In Space Accuracy Index (radial and clock).
     * @param sisaiOcb Signal In Space Accuracy Index (radial and clock)
     */
    public void setSisaiOcb(final int sisaiOcb) {
        this.sisaiOcb = sisaiOcb;
    }

    /**
     * Getter for Signal In Space Accuracy Index (clock drift accuracy).
     * @return Signal In Space Accuracy Index (clock drift accuracy)
     */
    public int getSisaiOc1() {
        return sisaiOc1;
    }

    /**
     * Setter for Signal In Space Accuracy Index (clock drift accuracy).
     * @param sisaiOc1 Signal In Space Accuracy Index (clock drift accuracy)
     */
    public void setSisaiOc1(final int sisaiOc1) {
        this.sisaiOc1 = sisaiOc1;
    }

    /**
     * Getter for Signal In Space Accuracy Index (clock drift rate accuracy).
     * @return Signal In Space Accuracy Index (clock drift rate accuracy)
     */
    public int getSisaiOc2() {
        return sisaiOc2;
    }

    /**
     * Setter for Signal In Space Accuracy Index (clock drift rate accuracy).
     * @param sisaiOc2 Signal In Space Accuracy Index (clock drift rate accuracy)
     */
    public void setSisaiOc2(final int sisaiOc2) {
        this.sisaiOc2 = sisaiOc2;
    }

    /**
     * Getter for Signal In Space Monitoring Accuracy Index.
     * @return Signal In Space Monitoring Accuracy Index
     */
    public int getSismai() {
        return sismai;
    }

    /**
     * Setter for Signal In Space Monitoring Accuracy Index.
     * @param sismai Signal In Space Monitoring Accuracy Index
     */
    public void setSismai(final int sismai) {
        this.sismai = sismai;
    }

    /**
     * Getter for health.
     * @return health
     */
    public int getHealth() {
        return health;
    }

    /**
     * Setter for health.
     * @param health health
     */
    public void setHealth(final int health) {
        this.health = health;
    }

    /**
     * Getter for B1C integrity flags.
     * @return B1C integrity flags
     */
    public int getIntegrityFlags() {
        return integrityFlags;
    }

    /**
     * Setter for B1C integrity flags.
     * @param integrityFlags integrity flags
     */
    public void setIntegrityFlags(final int integrityFlags) {
        this.integrityFlags = integrityFlags;
    }

    /**
     * Getter for B1/B3 Group Delay Differential (s).
     * @return B1/B3 Group Delay Differential (s)
     */
    public double getTgdB1Cp() {
        return tgdB1Cp;
    }

    /**
     * Setter for B1/B3 Group Delay Differential (s).
     * @param tgdB1Cp B1/B3 Group Delay Differential (s)
     */
    public void setTgdB1Cp(final double tgdB1Cp) {
        this.tgdB1Cp = tgdB1Cp;
    }

    /**
     * Getter for B2 AP Group Delay Differential (s).
     * @return B2 AP Group Delay Differential (s)
     */
    public double getTgdB2ap() {
        return tgdB2ap;
    }

    /**
     * Setter for B2 AP Group Delay Differential (s).
     * @param tgdB2ap B2 AP Group Delay Differential (s)
     */
    public void setTgdB2ap(final double tgdB2ap) {
        this.tgdB2ap = tgdB2ap;
    }

    /**
     * Getter for B2B_i / B3I Group Delay Differential (s).
     * @return B2B_i / B3I Group Delay Differential (s)
     */
    public double getTgdB2bI() {
        return tgdB2bI;
    }

    /**
     * Setter for B2B_i / B3I Group Delay Differential (s).
     * @param tgdB2bI B2B_i / B3I Group Delay Differential (s)
     */
    public void setTgdB2bI(final double tgdB2bI) {
        this.tgdB2bI = tgdB2bI;
    }

    /**
     * Getter for satellite type.
     * @return satellite type
     */
    public BeidouSatelliteType getSatelliteType() {
        return satelliteType;
    }

    /**
     * Setter for satellite type.
     * @param satelliteType satellite type
     */
    public void setSatelliteType(final BeidouSatelliteType satelliteType) {
        this.satelliteType = satelliteType;
    }

}
