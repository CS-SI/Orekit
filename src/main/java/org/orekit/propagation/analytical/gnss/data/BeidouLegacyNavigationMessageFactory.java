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
 * Factory for {@link BeidouLegacyNavigationMessage}.
 * @author Luc Maisonobe
 * @since 14.0
 */
public class BeidouLegacyNavigationMessageFactory
    extends AbstractNavigationMessageFactory<BeidouLegacyNavigationMessage> {

    /** Indicator for D2 messages. */
    private final boolean d2;

    /** Age of Data, Ephemeris. */
    private int aode;

    /** Age of Data, Clock. */
    private int aodc;

    /** Health identifier. */
    private int satH1;

    /** B1/B3 Group Delay Differential (s). */
    private double tgd1;

    /** B2/B3 Group Delay Differential (s). */
    private double tgd2;

    /** The user SV accuracy (m). */
    private double svAccuracy;

    /** Simple constructor.
     * @param timeScales known time scales
     * @param system     satellite system to use for interpreting week number
     * @param type       message type (null if not a navigation message)
     * @param inertial   reference inertial frame
     * @param bodyFixed  body fixed frame (will be frozen at {@code date} to build the orbital elements
     * @param d2         indicator for D2 messages
     */
    public BeidouLegacyNavigationMessageFactory(final TimeScales timeScales, final SatelliteSystem system,
                                                final String type, final Frame inertial, final Frame bodyFixed,
                                                final boolean d2) {
        super(GNSSConstants.BEIDOU_AV, GNSSConstants.BEIDOU_WEEK_NB, timeScales, system,
              type, inertial, bodyFixed, GNSSConstants.BEIDOU_MU);
        this.d2 = d2;
    }

    /** Check if message is a D2 message.
     * @return true if message is a D2 message
     */
    public boolean isD2() {
        return d2;
    }

    /** Get the Age Of Data Clock (AODC).
     * @return the Age Of Data Clock (AODC)
     */
    public int getAODC() {
        return aodc;
    }

    /** Set the Age Of Data Clock (AODC).
     * @param aodc the Age Of Data Clock (AODC)
     */
    public void setAODC(final int aodc) {
        this.aodc = aodc;
    }

    /** Get the Age Of Data Ephemeris (AODE).
     * @return the Age Of Data Ephemeris (AODE)
     */
    public int getAODE() {
        return aode;
    }

    /** Set the Age Of Data Ephemeris (AODE).
     * @param aode the Age Of Data Ephemeris (AODE)
     */
    public void setAODE(final int aode) {
        this.aode = aode;
    }

    /** Get the estimated group delay differential TGD1 for B1I signal.
     * @return the estimated group delay differential TGD1 for B1I signal (s)
     */
    public double getTGD1() {
        return tgd1;
    }

    /** Set the estimated group delay differential TGD1 for B1I signal.
     * @param tgd1 the estimated group delay differential TGD1 for B1I signal (s)
     */
    public void setTGD1(final double tgd1) {
        this.tgd1 = tgd1;
    }

    /** Get the estimated group delay differential TGD for B2I signal.
     * @return the estimated group delay differential TGD2 for B2I signal (s)
     */
    public double getTGD2() {
        return tgd2;
    }

    /** Set the estimated group delay differential TGD for B2I signal.
     * @param tgd2 the estimated group delay differential TGD2 for B2I signal (s)
     */
    public void setTGD2(final double tgd2) {
        this.tgd2 = tgd2;
    }

    /** Get the user SV accuray (meters).
     * @return the user SV accuracy
     */
    public double getSvAccuracy() {
        return svAccuracy;
    }

    /** Set the user SV accuray (meters).
     * @param svAccuracy the user SV accuracy
     */
    public void setSvAccuracy(final double svAccuracy) {
        this.svAccuracy = svAccuracy;
    }

    /** Get the health identifier.
     * @return health identifier
     */
    public int getSatH1() {
        return satH1;
    }

    /** Set the health identifier.
     * @param satH1 health identifier
     */
    public void setSatH1(final int satH1) {
        this.satH1 = satH1;
    }

    /** {@inheritDoc} */
    @Override
    public BeidouLegacyNavigationMessage createFromDrivers() {
        return new BeidouLegacyNavigationMessage(isD2(),
                                                 getTimeScales(), getType(), getPrn(),
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
                                                 getAODE(), getAODC(), getSatH1(),
                                                 getTGD1(), getTGD2(), getSvAccuracy());
    }

}
