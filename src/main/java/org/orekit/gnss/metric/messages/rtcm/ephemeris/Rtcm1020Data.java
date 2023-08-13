/* Copyright 2002-2023 CS GROUP
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
package org.orekit.gnss.metric.messages.rtcm.ephemeris;

import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.DataContext;
import org.orekit.propagation.analytical.gnss.data.GLONASSNavigationMessage;
import org.orekit.propagation.numerical.GLONASSNumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.GLONASSDate;
import org.orekit.time.TimeScales;

/**
 * Container for RTCM 1020 data.
 * <p>
 * Spacecraft coordinates read from this RTCM message are given in PZ-90.02 frame.
 * </p>
 * @author Bryan Cazabonne
 * @since 11.0
 */
public class Rtcm1020Data extends RtcmEphemerisData {

    /** Glonass navigation message. */
    private GLONASSNavigationMessage glonassNavigationMessage;

    /** Number of the current four year interval. */
    private int n4;

    /** Number of the current day in a four year interval. */
    private int nt;

    /** Almanac health availability indicator. */
    private boolean healthAvailabilityIndicator;

    /** Glonass P1 Word. */
    private int p1;

    /** Time referenced to the beginning of the frame within the current day [s]. */
    private double tk;

    /** Glonass B<sub>n</sub> Word. */
    private int bN;

    /** Glonass P2 Word. */
    private int p2;

    /** Glonass P3 Word. */
    private int p3;

    /** Glonass P Word. */
    private int p;

    /** Glonass l<sub>n</sub> (third string). */
    private int lNThirdString;

    /**
     * Glonass time difference between navigation RF signal transmitted
     * in L2 sub-band and navigation RF signal transmitted in L1 sub-band.
     */
    private double deltaTauN;

    /** Glonass E<sub>n</sub> Word. */
    private int eN;

    /** Glonass P4 Word. */
    private int p4;

    /** Glonass F<sub>T</sub> Word. */
    private int fT;

    /** Glonass M word. */
    private int m;

    /** Flag indicating if additional parameters are in the message. */
    private boolean areAdditionalDataAvailable;

    /** Glonass N<sup>A</sup> Word. */
    private int nA;

    /** Glonass time scale correction to UTC time. */
    private double tauC;

    /** Correction to GPS time relative to GLONASS time. */
    private double tauGps;

    /** Glonass l<sub>n</sub> (fifth string). */
    private int lNFifthString;

    /** Constructor. */
    public Rtcm1020Data() {
        // Nothing to do ...
    }

    /**
     * Get the Glonass navigation message corresponding to the current RTCM data.
     * <p>
     * This object can be used to initialize a {@link GLONASSNumericalPropagator}
     * <p>
     * This method uses the {@link DataContext#getDefault()} to initialize
     * the time scales used to configure the reference epochs of the navigation
     * message.
     *
     * @return the Glonass navigation message
     */
    @DefaultDataContext
    public GLONASSNavigationMessage getGlonassNavigationMessage() {
        return getGlonassNavigationMessage(DataContext.getDefault().getTimeScales());
    }

    /**
     * Get the Glonass navigation message corresponding to the current RTCM data.
     * <p>
     * This object can be used to initialize a {@link GLONASSNumericalPropagator}
     * <p>
     * When calling this method, the reference epochs of the navigation message
     * (i.e. ephemeris and clock epochs) are initialized using the provided time scales.
     *
     * @param timeScales time scales to use for initializing epochs
     * @return the Glonass navigation message
     */
    public GLONASSNavigationMessage getGlonassNavigationMessage(final TimeScales timeScales) {

        final double tb = glonassNavigationMessage.getTime();

        // Set the ephemeris reference data
        final AbsoluteDate refDate = new GLONASSDate(nt, n4, tb, timeScales.getGLONASS()).getDate();
        glonassNavigationMessage.setDate(refDate);
        glonassNavigationMessage.setEpochToc(refDate);

        // Return the navigation message
        return glonassNavigationMessage;

    }

    /**
     * Set the Glonass navigation message.
     * @param glonassNavigationMessage the Glonass navigation message to set
     */
    public void setGlonassNavigationMessage(final GLONASSNavigationMessage glonassNavigationMessage) {
        this.glonassNavigationMessage = glonassNavigationMessage;
    }

    /**
     * Get the four-year interval number starting from 1996.
     * @return the four-year interval number starting from 1996
     */
    public int getN4() {
        return n4;
    }

    /**
     * Set the four-year interval number starting from 1996.
     * @param n4 the number to set
     */
    public void setN4(final int n4) {
        this.n4 = n4;
    }

    /**
     * Get the current date.
     * <p>
     * Current date is a calendar number of day within four-year interval
     * starting from the 1-st of January in a leap year
     * </p>
     * @return the current date
     */
    public int getNt() {
        return nt;
    }

    /**
     * Set the current date.
     * @param nt the current date to set
     */
    public void setNt(final int nt) {
        this.nt = nt;
    }

    /**
     * Get the flag indicating if GLONASS almanac health is available.
     * @return true if GLONASS almanac health is available
     */
    public boolean isHealthAvailable() {
        return healthAvailabilityIndicator;
    }

    /**
     * Set the flag indicating if GLONASS almanac health is available.
     * @param healthAvailabilityIndicator true if GLONASS almanac health is available
     */
    public void setHealthAvailabilityIndicator(final boolean healthAvailabilityIndicator) {
        this.healthAvailabilityIndicator = healthAvailabilityIndicator;
    }

    /**
     * Get the GLONASS P1 Word.
     * <p>
     * Word P1 is a flag of the immediate data updating. It indicates a time interval
     * between two adjacent values of {@link GLONASSNavigationMessage#getTime() tb}
     * parameter (in seconds).
     * </p>
     * @return the GLONASS P1 Word
     */
    public int getP1() {
        return p1;
    }

    /**
     * Set the GLONASS P1 Word.
     * @param p1 the GLONASS P1 Word to set
     */
    public void setP1(final int p1) {
        this.p1 = p1;
    }

    /**
     * Get the time referenced to the beginning of the frame within the current day.
     * @return the time in seconds
     */
    public double getTk() {
        return tk;
    }

    /**
     * Set the time referenced to the beginning of the frame within the current day.
     * @param tk the time to set in seconds
     */
    public void setTk(final double tk) {
        this.tk = tk;
    }

    /**
     * Get the GLONASS B<sub>n</sub> Word.
     * <p>
     * Word B<sub>n</sub> is the health flag
     * </p>
     * @return the GLONASS B<sub>n</sub> Word
     */
    public int getBN() {
        return bN;
    }

    /**
     * Set the GLONASS B<sub>n</sub> Word.
     * @param word the word to set
     */
    public void setBN(final int word) {
        this.bN = word;
    }

    /**
     * Get the GLONASS P2 Word.
     * <p>
     * Word P2 is flag of oddness ("1") or evenness ("0") of the value of
     * {@link GLONASSNavigationMessage#getTime() tb}.
     * </p>
     * @return the GLONASS P2 Word
     */
    public int getP2() {
        return p2;
    }

    /**
     * Set the GLONASS P2 Word.
     * @param p2 the GLONASS P2 Word to set
     */
    public void setP2(final int p2) {
        this.p2 = p2;
    }

    /**
     * Get the GLONASS P3 Word.
     * <p>
     * Word P3 is flag indicating a number of satellites for which almanac is
     * transmitted within given frame
     * </p>
     * @return the GLONASS P3 Word
     */
    public int getP3() {
        return p3;
    }

    /**
     * Set the the GLONASS P3 Word.
     * @param p3 the GLONASS P3 Word to set
     */
    public void setP3(final int p3) {
        this.p3 = p3;
    }

    /**
     * Get the GLONASS P Word.
     * <p>
     * Word P is a technological parameter of control segment,
     * indication the satellite operation mode in respect of
     * time parameters.
     * </p>
     * @return the GLONASS P Word
     */
    public int getP() {
        return p;
    }

    /**
     * Set the GLONASS P Word.
     * @param p the GLONASS P Word to set
     */
    public void setP(final int p) {
        this.p = p;
    }

    /**
     * Get the GLONASS l<sub>n</sub> Word extracted from third string of the subframe.
     * @return the GLONASS l<sub>n</sub> (third string)
     */
    public int getLNThirdString() {
        return lNThirdString;
    }

    /**
     * Set the GLONASS l<sub>n</sub> Word extracted from third string of the subframe.
     * @param word the word to set
     */
    public void setLNThirdString(final int word) {
        this.lNThirdString = word;
    }

    /**
     * Get the deltaTauN value.
     * <p>
     * It represents the GLONASS time difference between navigation RF signal
     * transmitted in L2 sub-band and navigation RF signal transmitted in L1 sub-band.
     * </p>
     * @return deltaTauN
     */
    public double getDeltaTN() {
        return deltaTauN;
    }

    /**
     * Set the deltaTauN value.
     * @param deltaTN the value to set
     */
    public void setDeltaTN(final double deltaTN) {
        this.deltaTauN = deltaTN;
    }

    /**
     * Get the GLONASS E<sub>n</sub> Word.
     * <p>
     * It characterises the "age" of a current information.
     * </p>
     * @return the GLONASS E<sub>n</sub> Word in days
     */
    public int getEn() {
        return eN;
    }

    /**
     * Get the GLONASS E<sub>n</sub> Word.
     * @param word the word to set
     */
    public void setEn(final int word) {
        this.eN = word;
    }

    /**
     * Get the GLONASS P4 Word.
     * <p>
     * GLONASS P4 Word is a flag to show that ephemeris parameters are present.
     * "1" indicates that updated ephemeris or frequency/time parameters have been
     * uploaded by the control segment
     * </p>
     * @return the GLONASS P4 Word
     */
    public int getP4() {
        return p4;
    }

    /**
     * Set the GLONASS P4 Word.
     * @param p4 the GLONASS P4 Word to set
     */
    public void setP4(final int p4) {
        this.p4 = p4;
    }

    /**
     * Get the GLONASS F<sub>T</sub> Word.
     * <p>
     * It is a parameter that provides the predicted satellite user range accuracy
     * at time {@link GLONASSNavigationMessage#getTime() tb}.
     * </p>
     * @return the GLONASS F<sub>T</sub> Word
     */
    public int getFT() {
        return fT;
    }

    /**
     * Set the GLONASS F<sub>T</sub> Word.
     * @param word the word to set
     */
    public void setFT(final int word) {
        this.fT = word;
    }

    /**
     * Get the GLONASS M Word.
     * <p>
     * Word M represents the type of satellite transmitting navigation signal.
     * "0" refers to GLONASS satellite, "1" refers to a GLONASS-M satellite.
     * </p>
     * @return the GLONASS M Word
     */
    public int getM() {
        return m;
    }

    /**
     * Set the GLONASS M Word.
     * @param m the GLONASS M Word to set
     */
    public void setM(final int m) {
        this.m = m;
    }

    /**
     * Get the flag indicating if additional parameters are in the message.
     * @return true if additional parameters are in the message
     */
    public boolean areAdditionalDataAvailable() {
        return areAdditionalDataAvailable;
    }

    /**
     * Set the flag indicating if additional parameters are in the message.
     * @param areAdditionalDataAvailable true if additional parameters are in the message
     */
    public void setAreAdditionalDataAvailable(final boolean areAdditionalDataAvailable) {
        this.areAdditionalDataAvailable = areAdditionalDataAvailable;
    }

    /**
     * Get the GLONASS N<sup>A</sup> Word.
     * <p>
     * It is the calendar day number within the four-year period beginning since
     * the leap year. It is used for almanac data.
     * </p>
     * @return the GLONASS N<sup>A</sup> Word
     */
    public int getNA() {
        return nA;
    }

    /**
     * Set the GLONASS N<sup>A</sup> Word.
     * @param word the word to set
     */
    public void setNA(final int word) {
        this.nA = word;
    }

    /**
     * Get the GLONASS time scale correction to UTC time.
     * @return the GLONASS time scale correction to UTC time in seconds
     */
    public double getTauC() {
        return tauC;
    }

    /**
     * Set the GLONASS time scale correction to UTC time.
     * @param tauC the value to set in seconds.
     */
    public void setTauC(final double tauC) {
        this.tauC = tauC;
    }

    /**
     * Get the correction to GPS time relative to GLONASS time.
     * @return the correction to GPS time relative to GLONASS time in seconds
     */
    public double getTauGps() {
        return tauGps;
    }

    /**
     * Set the correction to GPS time relative to GLONASS time.
     * @param tauGps the value to set in seconds
     */
    public void setTauGps(final double tauGps) {
        this.tauGps = tauGps;
    }

    /**
     * Get the GLONASS l<sub>n</sub> Word extracted from fifth string of the subframe.
     * @return the GLONASS l<sub>n</sub> (fifth string)
     */
    public int getLNFifthString() {
        return lNFifthString;
    }

    /**
     * Set the GLONASS l<sub>n</sub> Word extracted from fifth string of the subframe.
     * @param word the word to set
     */
    public void setLNFifthString(final int word) {
        this.lNFifthString = word;
    }

}
