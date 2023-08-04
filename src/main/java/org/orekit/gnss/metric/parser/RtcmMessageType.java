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
package org.orekit.gnss.metric.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.gnss.metric.messages.ParsedMessage;
import org.orekit.gnss.metric.messages.common.AccuracyProvider;
import org.orekit.gnss.metric.messages.common.ClockCorrection;
import org.orekit.gnss.metric.messages.common.GlonassUserRangeAccuracy;
import org.orekit.gnss.metric.messages.common.OrbitCorrection;
import org.orekit.gnss.metric.messages.common.SignalInSpaceAccuracy;
import org.orekit.gnss.metric.messages.common.SsrUpdateInterval;
import org.orekit.gnss.metric.messages.common.UserRangeAccuracy;
import org.orekit.gnss.metric.messages.rtcm.correction.Rtcm1057;
import org.orekit.gnss.metric.messages.rtcm.correction.Rtcm1058;
import org.orekit.gnss.metric.messages.rtcm.correction.Rtcm1060;
import org.orekit.gnss.metric.messages.rtcm.correction.Rtcm1063;
import org.orekit.gnss.metric.messages.rtcm.correction.Rtcm1064;
import org.orekit.gnss.metric.messages.rtcm.correction.Rtcm1066;
import org.orekit.gnss.metric.messages.rtcm.correction.Rtcm1240;
import org.orekit.gnss.metric.messages.rtcm.correction.Rtcm1241;
import org.orekit.gnss.metric.messages.rtcm.correction.Rtcm1243;
import org.orekit.gnss.metric.messages.rtcm.correction.RtcmClockCorrectionData;
import org.orekit.gnss.metric.messages.rtcm.correction.RtcmCombinedCorrectionData;
import org.orekit.gnss.metric.messages.rtcm.correction.RtcmCorrectionHeader;
import org.orekit.gnss.metric.messages.rtcm.correction.RtcmOrbitCorrectionData;
import org.orekit.gnss.metric.messages.rtcm.correction.RtcmOrbitCorrectionHeader;
import org.orekit.gnss.metric.messages.rtcm.ephemeris.Rtcm1019;
import org.orekit.gnss.metric.messages.rtcm.ephemeris.Rtcm1019Data;
import org.orekit.gnss.metric.messages.rtcm.ephemeris.Rtcm1020;
import org.orekit.gnss.metric.messages.rtcm.ephemeris.Rtcm1020Data;
import org.orekit.gnss.metric.messages.rtcm.ephemeris.Rtcm1042;
import org.orekit.gnss.metric.messages.rtcm.ephemeris.Rtcm1042Data;
import org.orekit.gnss.metric.messages.rtcm.ephemeris.Rtcm1044;
import org.orekit.gnss.metric.messages.rtcm.ephemeris.Rtcm1044Data;
import org.orekit.gnss.metric.messages.rtcm.ephemeris.Rtcm1045;
import org.orekit.gnss.metric.messages.rtcm.ephemeris.Rtcm1045Data;
import org.orekit.propagation.analytical.gnss.data.BeidouLegacyNavigationMessage;
import org.orekit.propagation.analytical.gnss.data.GLONASSNavigationMessage;
import org.orekit.propagation.analytical.gnss.data.GPSLegacyNavigationMessage;
import org.orekit.propagation.analytical.gnss.data.GalileoNavigationMessage;
import org.orekit.propagation.analytical.gnss.data.QZSSLegacyNavigationMessage;

/** Enum containing the supported RTCM messages types.
*
* @author Luc Maisonobe
* @author Bryan Cazabonne
*
* @see "RTCM STANDARD 10403.3, DIFFERENTIAL GNSS (GLOBAL NAVIGATION SATELLITE SYSTEMS) SERVICES – VERSION 3, October 2016."
*
* @since 11.0
*/
public enum RtcmMessageType implements MessageType {

    /** GPS Ephemeris message. */
    RTCM_1019("1019") {

        /** {@inheritDoc} */
        @Override
        public ParsedMessage parse(final EncodedMessage encodedMessage,
                                   final int messageNumber) {

            // Initialize data container and navigation message
            final Rtcm1019Data         rtcm1019Data  = new Rtcm1019Data();
            final GPSLegacyNavigationMessage gpsNavMessage = new GPSLegacyNavigationMessage();

            // Set the satellite ID
            final int gpsId = RtcmDataField.DF009.intValue(encodedMessage);
            rtcm1019Data.setSatelliteID(gpsId);

            // Week number
            final int gpsWeekNumber = RtcmDataField.DF076.intValue(encodedMessage);
            gpsNavMessage.setWeek(gpsWeekNumber);

            // Accuracy provider
            final AccuracyProvider gpsProvider = new UserRangeAccuracy(RtcmDataField.DF077.intValue(encodedMessage));
            rtcm1019Data.setAccuracyProvider(gpsProvider);
            gpsNavMessage.setSvAccuracy(gpsProvider.getAccuracy());

            // GPS Code on L2
            rtcm1019Data.setGpsCodeOnL2(RtcmDataField.DF078.intValue(encodedMessage));

            // Fill navigation message
            gpsNavMessage.setPRN(gpsId);
            gpsNavMessage.setIDot(RtcmDataField.DF079.doubleValue(encodedMessage));
            gpsNavMessage.setIODE(RtcmDataField.DF071.intValue(encodedMessage));
            rtcm1019Data.setGpsToc(RtcmDataField.DF081.doubleValue(encodedMessage));
            gpsNavMessage.setAf2(RtcmDataField.DF082.doubleValue(encodedMessage));
            gpsNavMessage.setAf1(RtcmDataField.DF083.doubleValue(encodedMessage));
            gpsNavMessage.setAf0(RtcmDataField.DF084.doubleValue(encodedMessage));
            gpsNavMessage.setIODC(RtcmDataField.DF085.intValue(encodedMessage));
            gpsNavMessage.setCrs(RtcmDataField.DF086.doubleValue(encodedMessage));
            gpsNavMessage.setDeltaN(RtcmDataField.DF087.doubleValue(encodedMessage));
            gpsNavMessage.setM0(RtcmDataField.DF088.doubleValue(encodedMessage));
            gpsNavMessage.setCuc(RtcmDataField.DF089.doubleValue(encodedMessage));
            gpsNavMessage.setE(RtcmDataField.DF090.doubleValue(encodedMessage));
            gpsNavMessage.setCus(RtcmDataField.DF091.doubleValue(encodedMessage));
            gpsNavMessage.setSqrtA(RtcmDataField.DF092.doubleValue(encodedMessage));
            gpsNavMessage.setTime(RtcmDataField.DF093.doubleValue(encodedMessage));
            gpsNavMessage.setCic(RtcmDataField.DF094.doubleValue(encodedMessage));
            gpsNavMessage.setOmega0(RtcmDataField.DF095.doubleValue(encodedMessage));
            gpsNavMessage.setCis(RtcmDataField.DF096.doubleValue(encodedMessage));
            gpsNavMessage.setI0(RtcmDataField.DF097.doubleValue(encodedMessage));
            gpsNavMessage.setCrc(RtcmDataField.DF098.doubleValue(encodedMessage));
            gpsNavMessage.setPa(RtcmDataField.DF099.doubleValue(encodedMessage));
            gpsNavMessage.setOmegaDot(RtcmDataField.DF100.doubleValue(encodedMessage));
            gpsNavMessage.setTGD(RtcmDataField.DF101.doubleValue(encodedMessage));
            gpsNavMessage.setSvHealth(RtcmDataField.DF102.intValue(encodedMessage));

            // Set the navigation message
            rtcm1019Data.setGpsNavigationMessage(gpsNavMessage);

            // L2 P data flag and fit interval
            rtcm1019Data.setGpsL2PDataFlag(RtcmDataField.DF103.booleanValue(encodedMessage));
            rtcm1019Data.setGpsFitInterval(RtcmDataField.DF137.intValue(encodedMessage));

            // Return the parsed message
            return new Rtcm1019(1019, rtcm1019Data);

        }

    },

    /** GLONASS Ephemeris message. */
    RTCM_1020("1020") {

        /** {@inheritDoc} */
        @Override
        public ParsedMessage parse(final EncodedMessage encodedMessage,
                                   final int messageNumber) {

            // Initialize data container and navigation message
            final Rtcm1020Data             rtcm1020Data      = new Rtcm1020Data();
            final GLONASSNavigationMessage glonassNavMessage = new GLONASSNavigationMessage();

            // Set the satellite ID
            final int glonassId = RtcmDataField.DF038.intValue(encodedMessage);
            rtcm1020Data.setSatelliteID(glonassId);

            // Read data
            glonassNavMessage.setPRN(glonassId);
            glonassNavMessage.setFrequencyNumber(RtcmDataField.DF040.intValue(encodedMessage));
            glonassNavMessage.setHealth(RtcmDataField.DF104.intValue(encodedMessage));
            rtcm1020Data.setHealthAvailabilityIndicator(RtcmDataField.DF105.booleanValue(encodedMessage));
            rtcm1020Data.setP1(RtcmDataField.DF106.intValue(encodedMessage));
            rtcm1020Data.setTk(RtcmDataField.DF107.doubleValue(encodedMessage));
            rtcm1020Data.setBN(RtcmDataField.DF108.intValue(encodedMessage));
            rtcm1020Data.setP2(RtcmDataField.DF109.intValue(encodedMessage));
            glonassNavMessage.setTime(RtcmDataField.DF110.doubleValue(encodedMessage));
            glonassNavMessage.setXDot(RtcmDataField.DF111.doubleValue(encodedMessage));
            glonassNavMessage.setX(RtcmDataField.DF112.doubleValue(encodedMessage));
            glonassNavMessage.setXDotDot(RtcmDataField.DF113.doubleValue(encodedMessage));
            glonassNavMessage.setYDot(RtcmDataField.DF114.doubleValue(encodedMessage));
            glonassNavMessage.setY(RtcmDataField.DF115.doubleValue(encodedMessage));
            glonassNavMessage.setYDotDot(RtcmDataField.DF116.doubleValue(encodedMessage));
            glonassNavMessage.setZDot(RtcmDataField.DF117.doubleValue(encodedMessage));
            glonassNavMessage.setZ(RtcmDataField.DF118.doubleValue(encodedMessage));
            glonassNavMessage.setZDotDot(RtcmDataField.DF119.doubleValue(encodedMessage));
            rtcm1020Data.setP3(RtcmDataField.DF120.intValue(encodedMessage));
            glonassNavMessage.setGammaN(RtcmDataField.DF121.doubleValue(encodedMessage));
            rtcm1020Data.setP(RtcmDataField.DF122.intValue(encodedMessage));
            rtcm1020Data.setLNThirdString(RtcmDataField.DF123.intValue(encodedMessage));
            glonassNavMessage.setTauN(RtcmDataField.DF124.doubleValue(encodedMessage));
            rtcm1020Data.setDeltaTN(RtcmDataField.DF125.doubleValue(encodedMessage));
            rtcm1020Data.setEn(RtcmDataField.DF126.intValue(encodedMessage));
            rtcm1020Data.setP4(RtcmDataField.DF127.intValue(encodedMessage));

            // Glonass accuracy
            final int index = RtcmDataField.DF128.intValue(encodedMessage);
            rtcm1020Data.setAccuracyProvider(new GlonassUserRangeAccuracy(index));
            rtcm1020Data.setFT(index);

            // Read other data
            rtcm1020Data.setNt(RtcmDataField.DF129.intValue(encodedMessage));
            rtcm1020Data.setM(RtcmDataField.DF130.intValue(encodedMessage));
            rtcm1020Data.setAreAdditionalDataAvailable(RtcmDataField.DF131.booleanValue(encodedMessage));
            rtcm1020Data.setNA(RtcmDataField.DF132.intValue(encodedMessage));
            rtcm1020Data.setTauC(RtcmDataField.DF133.doubleValue(encodedMessage));
            rtcm1020Data.setN4(RtcmDataField.DF134.intValue(encodedMessage));
            rtcm1020Data.setTauGps(RtcmDataField.DF135.doubleValue(encodedMessage));
            rtcm1020Data.setLNFifthString(RtcmDataField.DF136.intValue(encodedMessage));

            // Set the navigation message
            rtcm1020Data.setGlonassNavigationMessage(glonassNavMessage);

            // Return the parsed message
            return new Rtcm1020(1020, rtcm1020Data);
        }


    },

    /** Beidou Ephemeris message. */
    RTCM_1042("1042") {

        /** {@inheritDoc} */
        @Override
        public ParsedMessage parse(final EncodedMessage encodedMessage,
                                   final int messageNumber) {

            // Initialize data container and navigation message
            final Rtcm1042Data            rtcm1042Data  = new Rtcm1042Data();
            final BeidouLegacyNavigationMessage beidouNavMessage = new BeidouLegacyNavigationMessage();

            // Set the satellite ID
            final int beidouId = RtcmDataField.DF488.intValue(encodedMessage);
            rtcm1042Data.setSatelliteID(beidouId);

            // Week number
            final int beidouWeekNumber = RtcmDataField.DF489.intValue(encodedMessage);
            beidouNavMessage.setWeek(beidouWeekNumber);

            // Accuracy provider
            final AccuracyProvider beidouProvider = new UserRangeAccuracy(RtcmDataField.DF490.intValue(encodedMessage));
            rtcm1042Data.setAccuracyProvider(beidouProvider);
            beidouNavMessage.setSvAccuracy(beidouProvider.getAccuracy());

            // Fill navigation message
            beidouNavMessage.setPRN(beidouId);
            beidouNavMessage.setIDot(RtcmDataField.DF491.doubleValue(encodedMessage));
            beidouNavMessage.setAODE(RtcmDataField.DF492.intValue(encodedMessage));
            rtcm1042Data.setBeidouToc(RtcmDataField.DF493.doubleValue(encodedMessage));
            beidouNavMessage.setAf2(RtcmDataField.DF494.doubleValue(encodedMessage));
            beidouNavMessage.setAf1(RtcmDataField.DF495.doubleValue(encodedMessage));
            beidouNavMessage.setAf0(RtcmDataField.DF496.doubleValue(encodedMessage));
            beidouNavMessage.setAODC(RtcmDataField.DF497.intValue(encodedMessage));
            beidouNavMessage.setCrs(RtcmDataField.DF498.doubleValue(encodedMessage));
            beidouNavMessage.setDeltaN(RtcmDataField.DF499.doubleValue(encodedMessage));
            beidouNavMessage.setM0(RtcmDataField.DF500.doubleValue(encodedMessage));
            beidouNavMessage.setCuc(RtcmDataField.DF501.doubleValue(encodedMessage));
            beidouNavMessage.setE(RtcmDataField.DF502.doubleValue(encodedMessage));
            beidouNavMessage.setCus(RtcmDataField.DF503.doubleValue(encodedMessage));
            beidouNavMessage.setSqrtA(RtcmDataField.DF504.doubleValue(encodedMessage));
            beidouNavMessage.setTime(RtcmDataField.DF505.doubleValue(encodedMessage));
            beidouNavMessage.setCic(RtcmDataField.DF506.doubleValue(encodedMessage));
            beidouNavMessage.setOmega0(RtcmDataField.DF507.doubleValue(encodedMessage));
            beidouNavMessage.setCis(RtcmDataField.DF508.doubleValue(encodedMessage));
            beidouNavMessage.setI0(RtcmDataField.DF509.doubleValue(encodedMessage));
            beidouNavMessage.setCrc(RtcmDataField.DF510.doubleValue(encodedMessage));
            beidouNavMessage.setPa(RtcmDataField.DF511.doubleValue(encodedMessage));
            beidouNavMessage.setOmegaDot(RtcmDataField.DF512.doubleValue(encodedMessage));
            beidouNavMessage.setTGD1(RtcmDataField.DF513.doubleValue(encodedMessage));
            beidouNavMessage.setTGD2(RtcmDataField.DF514.doubleValue(encodedMessage));
            rtcm1042Data.setSvHealth(RtcmDataField.DF515.intValue(encodedMessage));

            // Set the navigation message
            rtcm1042Data.setBeidouNavigationMessage(beidouNavMessage);

            // Return the parsed message
            return new Rtcm1042(1042, rtcm1042Data);

        }

    },

    /** QZSS Ephemeris message. */
    RTCM_1044("1044") {

        /** {@inheritDoc} */
        @Override
        public ParsedMessage parse(final EncodedMessage encodedMessage,
                                   final int messageNumber) {

            // Initialize data container and navigation message
            final Rtcm1044Data          rtcm1044Data   = new Rtcm1044Data();
            final QZSSLegacyNavigationMessage qzssNavMessage = new QZSSLegacyNavigationMessage();

            // Set the satellite ID
            final int qzssId = RtcmDataField.DF429.intValue(encodedMessage);
            rtcm1044Data.setSatelliteID(qzssId);

            // Fill navigation message
            qzssNavMessage.setPRN(qzssId);
            rtcm1044Data.setQzssToc(RtcmDataField.DF430.doubleValue(encodedMessage));
            qzssNavMessage.setAf2(RtcmDataField.DF431.doubleValue(encodedMessage));
            qzssNavMessage.setAf1(RtcmDataField.DF432.doubleValue(encodedMessage));
            qzssNavMessage.setAf0(RtcmDataField.DF433.doubleValue(encodedMessage));
            qzssNavMessage.setIODE(RtcmDataField.DF434.intValue(encodedMessage));
            qzssNavMessage.setCrs(RtcmDataField.DF435.doubleValue(encodedMessage));
            qzssNavMessage.setDeltaN(RtcmDataField.DF436.doubleValue(encodedMessage));
            qzssNavMessage.setM0(RtcmDataField.DF437.doubleValue(encodedMessage));
            qzssNavMessage.setCuc(RtcmDataField.DF438.doubleValue(encodedMessage));
            qzssNavMessage.setE(RtcmDataField.DF439.doubleValue(encodedMessage));
            qzssNavMessage.setCus(RtcmDataField.DF440.doubleValue(encodedMessage));
            qzssNavMessage.setSqrtA(RtcmDataField.DF441.doubleValue(encodedMessage));
            qzssNavMessage.setTime(RtcmDataField.DF442.doubleValue(encodedMessage));
            qzssNavMessage.setCic(RtcmDataField.DF443.doubleValue(encodedMessage));
            qzssNavMessage.setOmega0(RtcmDataField.DF444.doubleValue(encodedMessage));
            qzssNavMessage.setCis(RtcmDataField.DF445.doubleValue(encodedMessage));
            qzssNavMessage.setI0(RtcmDataField.DF446.doubleValue(encodedMessage));
            qzssNavMessage.setCrc(RtcmDataField.DF447.doubleValue(encodedMessage));
            qzssNavMessage.setPa(RtcmDataField.DF448.doubleValue(encodedMessage));
            qzssNavMessage.setOmegaDot(RtcmDataField.DF449.doubleValue(encodedMessage));
            qzssNavMessage.setIDot(RtcmDataField.DF450.doubleValue(encodedMessage));

            // QZSS Code on L2
            rtcm1044Data.setQzssCodeOnL2(RtcmDataField.DF451.intValue(encodedMessage));

            // Week number
            qzssNavMessage.setWeek(RtcmDataField.DF452.intValue(encodedMessage));

            // Accuracy provider
            final AccuracyProvider qzssProvider = new UserRangeAccuracy(RtcmDataField.DF453.intValue(encodedMessage));
            rtcm1044Data.setAccuracyProvider(qzssProvider);
            qzssNavMessage.setSvAccuracy(qzssProvider.getAccuracy());

            // Health
            qzssNavMessage.setSvHealth(RtcmDataField.DF454.intValue(encodedMessage));

            // Tgd, IODC, and fit interval
            qzssNavMessage.setTGD(RtcmDataField.DF455.doubleValue(encodedMessage));
            qzssNavMessage.setIODC(RtcmDataField.DF456.intValue(encodedMessage));
            rtcm1044Data.setQzssFitInterval(RtcmDataField.DF457.intValue(encodedMessage));

            // Set the navigation message
            rtcm1044Data.setQzssNavigationMessage(qzssNavMessage);

            // Return the parsed message
            return new Rtcm1044(1044, rtcm1044Data);

        }

    },

    /** Galileo F/NAV Ephemeris message. */
    RTCM_1045("1045") {

        /** {@inheritDoc} */
        @Override
        public ParsedMessage parse(final EncodedMessage encodedMessage,
                                   final int messageNumber) {

            // Initialize data container and navigation message
            final Rtcm1045Data             rtcm1045Data      = new Rtcm1045Data();
            final GalileoNavigationMessage galileoNavMessage = new GalileoNavigationMessage();

            // Set the satellite ID
            final int galileoId = RtcmDataField.DF252.intValue(encodedMessage);
            rtcm1045Data.setSatelliteID(galileoId);

            // Week number
            final int galileoWeekNumber = RtcmDataField.DF289.intValue(encodedMessage);
            galileoNavMessage.setWeek(galileoWeekNumber);

            // IODNav
            galileoNavMessage.setIODNav(RtcmDataField.DF290.intValue(encodedMessage));

            // Accuracy provider
            final AccuracyProvider galileoProvider = new SignalInSpaceAccuracy(RtcmDataField.DF291.intValue(encodedMessage));
            rtcm1045Data.setAccuracyProvider(galileoProvider);
            galileoNavMessage.setSisa(galileoProvider.getAccuracy());

            // Fill navigation message
            galileoNavMessage.setPRN(galileoId);
            galileoNavMessage.setIDot(RtcmDataField.DF292.doubleValue(encodedMessage));
            rtcm1045Data.setGalileoToc(RtcmDataField.DF293.doubleValue(encodedMessage));
            galileoNavMessage.setAf2(RtcmDataField.DF294.doubleValue(encodedMessage));
            galileoNavMessage.setAf1(RtcmDataField.DF295.doubleValue(encodedMessage));
            galileoNavMessage.setAf0(RtcmDataField.DF296.doubleValue(encodedMessage));
            galileoNavMessage.setCrs(RtcmDataField.DF297.doubleValue(encodedMessage));
            galileoNavMessage.setDeltaN(RtcmDataField.DF298.doubleValue(encodedMessage));
            galileoNavMessage.setM0(RtcmDataField.DF299.doubleValue(encodedMessage));
            galileoNavMessage.setCuc(RtcmDataField.DF300.doubleValue(encodedMessage));
            galileoNavMessage.setE(RtcmDataField.DF301.doubleValue(encodedMessage));
            galileoNavMessage.setCus(RtcmDataField.DF302.doubleValue(encodedMessage));
            galileoNavMessage.setSqrtA(RtcmDataField.DF303.doubleValue(encodedMessage));
            galileoNavMessage.setTime(RtcmDataField.DF304.doubleValue(encodedMessage));
            galileoNavMessage.setCic(RtcmDataField.DF305.doubleValue(encodedMessage));
            galileoNavMessage.setOmega0(RtcmDataField.DF306.doubleValue(encodedMessage));
            galileoNavMessage.setCis(RtcmDataField.DF307.doubleValue(encodedMessage));
            galileoNavMessage.setI0(RtcmDataField.DF308.doubleValue(encodedMessage));
            galileoNavMessage.setCrc(RtcmDataField.DF309.doubleValue(encodedMessage));
            galileoNavMessage.setPa(RtcmDataField.DF310.doubleValue(encodedMessage));
            galileoNavMessage.setOmegaDot(RtcmDataField.DF311.doubleValue(encodedMessage));
            galileoNavMessage.setBGDE1E5a(RtcmDataField.DF312.doubleValue(encodedMessage));
            galileoNavMessage.setSvHealth(RtcmDataField.DF314.intValue(encodedMessage));

            // Set the navigation message
            rtcm1045Data.setGalileoNavigationMessage(galileoNavMessage);

            // NAV data validity status
            rtcm1045Data.setGalileoDataValidityStatus(RtcmDataField.DF315.intValue(encodedMessage));

            // Return the parsed message
            return new Rtcm1045(1045, rtcm1045Data);

        }

    },

    /** GPS Orbit Correction. */
    RTCM_1057("1057") {

        /** {@inheritDoc} */
        @Override
        public ParsedMessage parse(final EncodedMessage encodedMessage, final int messageNumber) {

            // Header data
            final RtcmOrbitCorrectionHeader rtcm1057Header = new RtcmOrbitCorrectionHeader();
            rtcm1057Header.setEpochTime1s(RtcmDataField.DF385.intValue(encodedMessage));
            rtcm1057Header.setSsrUpdateInterval(new SsrUpdateInterval(RtcmDataField.DF391.intValue(encodedMessage)));
            rtcm1057Header.setMultipleMessageIndicator(RtcmDataField.DF388.intValue(encodedMessage));
            rtcm1057Header.setSatelliteReferenceDatum(RtcmDataField.DF375.intValue(encodedMessage));
            rtcm1057Header.setIodSsr(RtcmDataField.DF413.intValue(encodedMessage));
            rtcm1057Header.setSsrProviderId(RtcmDataField.DF414.intValue(encodedMessage));
            rtcm1057Header.setSsrSolutionId(RtcmDataField.DF415.intValue(encodedMessage));

            // Number of satellites
            final int satNumber = RtcmDataField.DF387.intValue(encodedMessage);
            rtcm1057Header.setNumberOfSatellites(satNumber);

            // Initialize list of data
            final List<RtcmOrbitCorrectionData> rtcm1057Data = new ArrayList<>();

            // Loop on satellites and fill data
            for (int index = 0; index < satNumber; index++) {

                // Satellite ID
                final int rtcm1057SatId = RtcmDataField.DF068.intValue(encodedMessage);

                // GNSS IOD
                final int rtcm1057Iod = RtcmDataField.DF071.intValue(encodedMessage);

                // Orbit correction
                final OrbitCorrection rtcm1057OrbitCorr =
                                new OrbitCorrection(RtcmDataField.DF365.doubleValue(encodedMessage),
                                                    RtcmDataField.DF366.doubleValue(encodedMessage),
                                                    RtcmDataField.DF367.doubleValue(encodedMessage),
                                                    RtcmDataField.DF368.doubleValue(encodedMessage),
                                                    RtcmDataField.DF369.doubleValue(encodedMessage),
                                                    RtcmDataField.DF370.doubleValue(encodedMessage));

                // Initialize a new container and fill data
                final RtcmOrbitCorrectionData currentRtcm1057Data = new RtcmOrbitCorrectionData();
                currentRtcm1057Data.setSatelliteID(rtcm1057SatId);
                currentRtcm1057Data.setGnssIod(rtcm1057Iod);
                currentRtcm1057Data.setOrbitCorrection(rtcm1057OrbitCorr);

                // Update the list
                rtcm1057Data.add(currentRtcm1057Data);

            }

            // Return the parsed message
            return new Rtcm1057(messageNumber, rtcm1057Header, rtcm1057Data);

        }

    },

    /** GPS Clock Correction. */
    RTCM_1058("1058") {

        /** {@inheritDoc} */
        @Override
        public ParsedMessage parse(final EncodedMessage encodedMessage, final int messageNumber) {

            // Header data
            final RtcmCorrectionHeader rtcm1058Header = new RtcmCorrectionHeader();
            rtcm1058Header.setEpochTime1s(RtcmDataField.DF385.intValue(encodedMessage));
            rtcm1058Header.setSsrUpdateInterval(new SsrUpdateInterval(RtcmDataField.DF391.intValue(encodedMessage)));
            rtcm1058Header.setMultipleMessageIndicator(RtcmDataField.DF388.intValue(encodedMessage));
            rtcm1058Header.setIodSsr(RtcmDataField.DF413.intValue(encodedMessage));
            rtcm1058Header.setSsrProviderId(RtcmDataField.DF414.intValue(encodedMessage));
            rtcm1058Header.setSsrSolutionId(RtcmDataField.DF415.intValue(encodedMessage));

            // Number of satellites
            final int satNumber = RtcmDataField.DF387.intValue(encodedMessage);
            rtcm1058Header.setNumberOfSatellites(satNumber);

            // Initialize list of data
            final List<RtcmClockCorrectionData> rtcm1058Data = new ArrayList<>();

            // Loop on satellites and fill data
            for (int index = 0; index < satNumber; index++) {

                // Satellite ID
                final int rtcm1058SatId = RtcmDataField.DF068.intValue(encodedMessage);

                // Clock correction
                final ClockCorrection rtcm1058ClockCorr =
                                new ClockCorrection(RtcmDataField.DF376.doubleValue(encodedMessage),
                                                    RtcmDataField.DF377.doubleValue(encodedMessage),
                                                    RtcmDataField.DF378.doubleValue(encodedMessage));

                // Initialize a new container and fill data
                final RtcmClockCorrectionData currentRtcm1058Data = new RtcmClockCorrectionData();
                currentRtcm1058Data.setSatelliteID(rtcm1058SatId);
                currentRtcm1058Data.setClockCorrection(rtcm1058ClockCorr);

                // Update the list
                rtcm1058Data.add(currentRtcm1058Data);

            }

            // Return the parsed message
            return new Rtcm1058(messageNumber, rtcm1058Header, rtcm1058Data);

        }

    },

    /** GPS Combined Orbit and Clock Correction. */
    RTCM_1060("1060") {

        /** {@inheritDoc} */
        @Override
        public ParsedMessage parse(final EncodedMessage encodedMessage, final int messageNumber) {

            // Header data
            final RtcmOrbitCorrectionHeader rtcm1060Header = new RtcmOrbitCorrectionHeader();
            rtcm1060Header.setEpochTime1s(RtcmDataField.DF385.intValue(encodedMessage));
            rtcm1060Header.setSsrUpdateInterval(new SsrUpdateInterval(RtcmDataField.DF391.intValue(encodedMessage)));
            rtcm1060Header.setMultipleMessageIndicator(RtcmDataField.DF388.intValue(encodedMessage));
            rtcm1060Header.setSatelliteReferenceDatum(RtcmDataField.DF375.intValue(encodedMessage));
            rtcm1060Header.setIodSsr(RtcmDataField.DF413.intValue(encodedMessage));
            rtcm1060Header.setSsrProviderId(RtcmDataField.DF414.intValue(encodedMessage));
            rtcm1060Header.setSsrSolutionId(RtcmDataField.DF415.intValue(encodedMessage));

            // Number of satellites
            final int satNumber = RtcmDataField.DF387.intValue(encodedMessage);
            rtcm1060Header.setNumberOfSatellites(satNumber);

            // Initialize list of data
            final List<RtcmCombinedCorrectionData> rtcm1060Data = new ArrayList<>();

            // Loop on satellites and fill data
            for (int index = 0; index < satNumber; index++) {

                // Satellite ID
                final int rtcm1060SatId = RtcmDataField.DF068.intValue(encodedMessage);

                // GNSS IOD
                final int rtcm1060Iod = RtcmDataField.DF071.intValue(encodedMessage);

                // Orbit correction
                final OrbitCorrection rtcm1060OrbitCorr =
                                new OrbitCorrection(RtcmDataField.DF365.doubleValue(encodedMessage),
                                                    RtcmDataField.DF366.doubleValue(encodedMessage),
                                                    RtcmDataField.DF367.doubleValue(encodedMessage),
                                                    RtcmDataField.DF368.doubleValue(encodedMessage),
                                                    RtcmDataField.DF369.doubleValue(encodedMessage),
                                                    RtcmDataField.DF370.doubleValue(encodedMessage));

                // Clock correction
                final ClockCorrection rtcm1060ClockCorr =
                                new ClockCorrection(RtcmDataField.DF376.doubleValue(encodedMessage),
                                                    RtcmDataField.DF377.doubleValue(encodedMessage),
                                                    RtcmDataField.DF378.doubleValue(encodedMessage));

                // Initialize a new container and fill data
                final RtcmCombinedCorrectionData currentRtcm1060Data = new RtcmCombinedCorrectionData();
                currentRtcm1060Data.setSatelliteID(rtcm1060SatId);
                currentRtcm1060Data.setGnssIod(rtcm1060Iod);
                currentRtcm1060Data.setOrbitCorrection(rtcm1060OrbitCorr);
                currentRtcm1060Data.setClockCorrection(rtcm1060ClockCorr);

                // Update the list
                rtcm1060Data.add(currentRtcm1060Data);

            }

            // Return the parsed message
            return new Rtcm1060(messageNumber, rtcm1060Header, rtcm1060Data);

        }

    },

    /** GLONASS Orbit Correction. */
    RTCM_1063("1063") {

        /** {@inheritDoc} */
        @Override
        public ParsedMessage parse(final EncodedMessage encodedMessage, final int messageNumber) {

            // Header data
            final RtcmOrbitCorrectionHeader rtcm1063Header = new RtcmOrbitCorrectionHeader();
            rtcm1063Header.setEpochTime1s(RtcmDataField.DF386.intValue(encodedMessage));
            rtcm1063Header.setSsrUpdateInterval(new SsrUpdateInterval(RtcmDataField.DF391.intValue(encodedMessage)));
            rtcm1063Header.setMultipleMessageIndicator(RtcmDataField.DF388.intValue(encodedMessage));
            rtcm1063Header.setSatelliteReferenceDatum(RtcmDataField.DF375.intValue(encodedMessage));
            rtcm1063Header.setIodSsr(RtcmDataField.DF413.intValue(encodedMessage));
            rtcm1063Header.setSsrProviderId(RtcmDataField.DF414.intValue(encodedMessage));
            rtcm1063Header.setSsrSolutionId(RtcmDataField.DF415.intValue(encodedMessage));

            // Number of satellites
            final int satNumber = RtcmDataField.DF387.intValue(encodedMessage);
            rtcm1063Header.setNumberOfSatellites(satNumber);

            // Initialize list of data
            final List<RtcmOrbitCorrectionData> rtcm1063Data = new ArrayList<>();

            // Loop on satellites and fill data
            for (int index = 0; index < satNumber; index++) {

                // Satellite ID
                final int rtcm1063SatId = RtcmDataField.DF384.intValue(encodedMessage);

                // GNSS IOD
                final int rtcm1063Iod = RtcmDataField.DF392.intValue(encodedMessage);

                // Orbit correction
                final OrbitCorrection rtcm1063OrbitCorr =
                                new OrbitCorrection(RtcmDataField.DF365.doubleValue(encodedMessage),
                                                    RtcmDataField.DF366.doubleValue(encodedMessage),
                                                    RtcmDataField.DF367.doubleValue(encodedMessage),
                                                    RtcmDataField.DF368.doubleValue(encodedMessage),
                                                    RtcmDataField.DF369.doubleValue(encodedMessage),
                                                    RtcmDataField.DF370.doubleValue(encodedMessage));

                // Initialize a new container and fill data
                final RtcmOrbitCorrectionData currentRtcm1063Data = new RtcmOrbitCorrectionData();
                currentRtcm1063Data.setSatelliteID(rtcm1063SatId);
                currentRtcm1063Data.setGnssIod(rtcm1063Iod);
                currentRtcm1063Data.setOrbitCorrection(rtcm1063OrbitCorr);

                // Update the list
                rtcm1063Data.add(currentRtcm1063Data);

            }

            // Return the parsed message
            return new Rtcm1063(messageNumber, rtcm1063Header, rtcm1063Data);

        }

    },

    /** GLONASS Clock Correction. */
    RTCM_1064("1064") {

        /** {@inheritDoc} */
        @Override
        public ParsedMessage parse(final EncodedMessage encodedMessage, final int messageNumber) {

            // Header data
            final RtcmCorrectionHeader rtcm1064Header = new RtcmCorrectionHeader();
            rtcm1064Header.setEpochTime1s(RtcmDataField.DF386.intValue(encodedMessage));
            rtcm1064Header.setSsrUpdateInterval(new SsrUpdateInterval(RtcmDataField.DF391.intValue(encodedMessage)));
            rtcm1064Header.setMultipleMessageIndicator(RtcmDataField.DF388.intValue(encodedMessage));
            rtcm1064Header.setIodSsr(RtcmDataField.DF413.intValue(encodedMessage));
            rtcm1064Header.setSsrProviderId(RtcmDataField.DF414.intValue(encodedMessage));
            rtcm1064Header.setSsrSolutionId(RtcmDataField.DF415.intValue(encodedMessage));

            // Number of satellites
            final int satNumber = RtcmDataField.DF387.intValue(encodedMessage);
            rtcm1064Header.setNumberOfSatellites(satNumber);

            // Initialize list of data
            final List<RtcmClockCorrectionData> rtcm1064Data = new ArrayList<>();

            // Loop on satellites and fill data
            for (int index = 0; index < satNumber; index++) {

                // Satellite ID
                final int rtcm1064SatId = RtcmDataField.DF384.intValue(encodedMessage);

                // Clock correction
                final ClockCorrection rtcm1064ClockCorr =
                                new ClockCorrection(RtcmDataField.DF376.doubleValue(encodedMessage),
                                                    RtcmDataField.DF377.doubleValue(encodedMessage),
                                                    RtcmDataField.DF378.doubleValue(encodedMessage));

                // Initialize a new container and fill data
                final RtcmClockCorrectionData currentRtcm1058Data = new RtcmClockCorrectionData();
                currentRtcm1058Data.setSatelliteID(rtcm1064SatId);
                currentRtcm1058Data.setClockCorrection(rtcm1064ClockCorr);

                // Update the list
                rtcm1064Data.add(currentRtcm1058Data);

            }

            // Return the parsed message
            return new Rtcm1064(messageNumber, rtcm1064Header, rtcm1064Data);

        }

    },

    /** GLONASS Combined Orbit and Clock Correction. */
    RTCM_1066("1066") {

        /** {@inheritDoc} */
        @Override
        public ParsedMessage parse(final EncodedMessage encodedMessage, final int messageNumber) {

            // Header data
            final RtcmOrbitCorrectionHeader rtcm1066Header = new RtcmOrbitCorrectionHeader();
            rtcm1066Header.setEpochTime1s(RtcmDataField.DF386.intValue(encodedMessage));
            rtcm1066Header.setSsrUpdateInterval(new SsrUpdateInterval(RtcmDataField.DF391.intValue(encodedMessage)));
            rtcm1066Header.setMultipleMessageIndicator(RtcmDataField.DF388.intValue(encodedMessage));
            rtcm1066Header.setSatelliteReferenceDatum(RtcmDataField.DF375.intValue(encodedMessage));
            rtcm1066Header.setIodSsr(RtcmDataField.DF413.intValue(encodedMessage));
            rtcm1066Header.setSsrProviderId(RtcmDataField.DF414.intValue(encodedMessage));
            rtcm1066Header.setSsrSolutionId(RtcmDataField.DF415.intValue(encodedMessage));

            // Number of satellites
            final int satNumber = RtcmDataField.DF387.intValue(encodedMessage);
            rtcm1066Header.setNumberOfSatellites(satNumber);

            // Initialize list of data
            final List<RtcmCombinedCorrectionData> rtcm1066Data = new ArrayList<>();

            // Loop on satellites and fill data
            for (int index = 0; index < satNumber; index++) {

                // Satellite ID
                final int rtcm1066SatId = RtcmDataField.DF384.intValue(encodedMessage);

                // GNSS IOD
                final int rtcm1066Iod = RtcmDataField.DF392.intValue(encodedMessage);

                // Orbit correction
                final OrbitCorrection rtcm1066OrbitCorr =
                                new OrbitCorrection(RtcmDataField.DF365.doubleValue(encodedMessage),
                                                    RtcmDataField.DF366.doubleValue(encodedMessage),
                                                    RtcmDataField.DF367.doubleValue(encodedMessage),
                                                    RtcmDataField.DF368.doubleValue(encodedMessage),
                                                    RtcmDataField.DF369.doubleValue(encodedMessage),
                                                    RtcmDataField.DF370.doubleValue(encodedMessage));

                // Clock correction
                final ClockCorrection rtcm1066ClockCorr =
                                new ClockCorrection(RtcmDataField.DF376.doubleValue(encodedMessage),
                                                    RtcmDataField.DF377.doubleValue(encodedMessage),
                                                    RtcmDataField.DF378.doubleValue(encodedMessage));

                // Initialize a new container and fill data
                final RtcmCombinedCorrectionData currentRtcm1066Data = new RtcmCombinedCorrectionData();
                currentRtcm1066Data.setSatelliteID(rtcm1066SatId);
                currentRtcm1066Data.setGnssIod(rtcm1066Iod);
                currentRtcm1066Data.setOrbitCorrection(rtcm1066OrbitCorr);
                currentRtcm1066Data.setClockCorrection(rtcm1066ClockCorr);

                // Update the list
                rtcm1066Data.add(currentRtcm1066Data);

            }

            // Return the parsed message
            return new Rtcm1066(messageNumber, rtcm1066Header, rtcm1066Data);

        }

    },

    /** Galileo Orbit Correction Message. */
    RTCM_1240("1240") {

        /** {@inheritDoc} */
        @Override
        public ParsedMessage parse(final EncodedMessage encodedMessage, final int messageNumber) {

            // Header data
            final RtcmOrbitCorrectionHeader rtcm1240Header = new RtcmOrbitCorrectionHeader();
            rtcm1240Header.setEpochTime1s(RtcmDataField.DF458.intValue(encodedMessage));
            rtcm1240Header.setSsrUpdateInterval(new SsrUpdateInterval(RtcmDataField.DF391.intValue(encodedMessage)));
            rtcm1240Header.setMultipleMessageIndicator(RtcmDataField.DF388.intValue(encodedMessage));
            rtcm1240Header.setSatelliteReferenceDatum(RtcmDataField.DF375.intValue(encodedMessage));
            rtcm1240Header.setIodSsr(RtcmDataField.DF413.intValue(encodedMessage));
            rtcm1240Header.setSsrProviderId(RtcmDataField.DF414.intValue(encodedMessage));
            rtcm1240Header.setSsrSolutionId(RtcmDataField.DF415.intValue(encodedMessage));

            // Number of satellites
            final int satNumber = RtcmDataField.DF387.intValue(encodedMessage);
            rtcm1240Header.setNumberOfSatellites(satNumber);

            // Initialize list of data
            final List<RtcmOrbitCorrectionData> rtcm1240Data = new ArrayList<>();

            // Loop on satellites and fill data
            for (int index = 0; index < satNumber; index++) {

                // Satellite ID
                final int rtcm1240SatId = RtcmDataField.DF252.intValue(encodedMessage);

                // GNSS IOD
                final int rtcm1240Iod = RtcmDataField.DF290.intValue(encodedMessage);

                // Orbit correction
                final OrbitCorrection rtcm1240OrbitCorr =
                                new OrbitCorrection(RtcmDataField.DF365.doubleValue(encodedMessage),
                                                    RtcmDataField.DF366.doubleValue(encodedMessage),
                                                    RtcmDataField.DF367.doubleValue(encodedMessage),
                                                    RtcmDataField.DF368.doubleValue(encodedMessage),
                                                    RtcmDataField.DF369.doubleValue(encodedMessage),
                                                    RtcmDataField.DF370.doubleValue(encodedMessage));

                // Initialize a new container and fill data
                final RtcmOrbitCorrectionData currentRtcm1240Data = new RtcmOrbitCorrectionData();
                currentRtcm1240Data.setSatelliteID(rtcm1240SatId);
                currentRtcm1240Data.setGnssIod(rtcm1240Iod);
                currentRtcm1240Data.setOrbitCorrection(rtcm1240OrbitCorr);

                // Update the list
                rtcm1240Data.add(currentRtcm1240Data);

            }

            // Return the parsed message
            return new Rtcm1240(messageNumber, rtcm1240Header, rtcm1240Data);

        }

    },

    /** Galileo Clock Correction Message. */
    RTCM_1241("1241") {

        /** {@inheritDoc} */
        @Override
        public ParsedMessage parse(final EncodedMessage encodedMessage, final int messageNumber) {

            // Header data
            final RtcmCorrectionHeader rtcm1241Header = new RtcmCorrectionHeader();
            rtcm1241Header.setEpochTime1s(RtcmDataField.DF458.intValue(encodedMessage));
            rtcm1241Header.setSsrUpdateInterval(new SsrUpdateInterval(RtcmDataField.DF391.intValue(encodedMessage)));
            rtcm1241Header.setMultipleMessageIndicator(RtcmDataField.DF388.intValue(encodedMessage));
            rtcm1241Header.setIodSsr(RtcmDataField.DF413.intValue(encodedMessage));
            rtcm1241Header.setSsrProviderId(RtcmDataField.DF414.intValue(encodedMessage));
            rtcm1241Header.setSsrSolutionId(RtcmDataField.DF415.intValue(encodedMessage));

            // Number of satellites
            final int satNumber = RtcmDataField.DF387.intValue(encodedMessage);
            rtcm1241Header.setNumberOfSatellites(satNumber);

            // Initialize list of data
            final List<RtcmClockCorrectionData> rtcm1241Data = new ArrayList<>();

            // Loop on satellites and fill data
            for (int index = 0; index < satNumber; index++) {

                // Satellite ID
                final int rtcm1241SatId = RtcmDataField.DF252.intValue(encodedMessage);

                // Clock correction
                final ClockCorrection rtcm1241ClockCorr =
                                new ClockCorrection(RtcmDataField.DF376.doubleValue(encodedMessage),
                                                    RtcmDataField.DF377.doubleValue(encodedMessage),
                                                    RtcmDataField.DF378.doubleValue(encodedMessage));

                // Initialize a new container and fill data
                final RtcmClockCorrectionData currentRtcm1241Data = new RtcmClockCorrectionData();
                currentRtcm1241Data.setSatelliteID(rtcm1241SatId);
                currentRtcm1241Data.setClockCorrection(rtcm1241ClockCorr);

                // Update the list
                rtcm1241Data.add(currentRtcm1241Data);

            }

            // Return the parsed message
            return new Rtcm1241(messageNumber, rtcm1241Header, rtcm1241Data);

        }

    },

    /** Galileo Combined Orbit and Clock Correction Message. */
    RTCM_1243("1243") {

        /** {@inheritDoc} */
        @Override
        public ParsedMessage parse(final EncodedMessage encodedMessage, final int messageNumber) {

            // Header data
            final RtcmOrbitCorrectionHeader rtcm1243Header = new RtcmOrbitCorrectionHeader();
            rtcm1243Header.setEpochTime1s(RtcmDataField.DF458.intValue(encodedMessage));
            rtcm1243Header.setSsrUpdateInterval(new SsrUpdateInterval(RtcmDataField.DF391.intValue(encodedMessage)));
            rtcm1243Header.setMultipleMessageIndicator(RtcmDataField.DF388.intValue(encodedMessage));
            rtcm1243Header.setSatelliteReferenceDatum(RtcmDataField.DF375.intValue(encodedMessage));
            rtcm1243Header.setIodSsr(RtcmDataField.DF413.intValue(encodedMessage));
            rtcm1243Header.setSsrProviderId(RtcmDataField.DF414.intValue(encodedMessage));
            rtcm1243Header.setSsrSolutionId(RtcmDataField.DF415.intValue(encodedMessage));

            // Number of satellites
            final int satNumber = RtcmDataField.DF387.intValue(encodedMessage);
            rtcm1243Header.setNumberOfSatellites(satNumber);

            // Initialize list of data
            final List<RtcmCombinedCorrectionData> rtcm1243Data = new ArrayList<>();

            // Loop on satellites and fill data
            for (int index = 0; index < satNumber; index++) {

                // Satellite ID
                final int rtcm1243SatId = RtcmDataField.DF252.intValue(encodedMessage);

                // GNSS IOD
                final int rtcm1243Iod = RtcmDataField.DF290.intValue(encodedMessage);

                // Orbit correction
                final OrbitCorrection rtcm1243OrbitCorr =
                                new OrbitCorrection(RtcmDataField.DF365.doubleValue(encodedMessage),
                                                    RtcmDataField.DF366.doubleValue(encodedMessage),
                                                    RtcmDataField.DF367.doubleValue(encodedMessage),
                                                    RtcmDataField.DF368.doubleValue(encodedMessage),
                                                    RtcmDataField.DF369.doubleValue(encodedMessage),
                                                    RtcmDataField.DF370.doubleValue(encodedMessage));

                // Clock correction
                final ClockCorrection rtcm1243ClockCorr =
                                new ClockCorrection(RtcmDataField.DF376.doubleValue(encodedMessage),
                                                    RtcmDataField.DF377.doubleValue(encodedMessage),
                                                    RtcmDataField.DF378.doubleValue(encodedMessage));

                // Initialize a new container and fill data
                final RtcmCombinedCorrectionData currentRtcm1243Data = new RtcmCombinedCorrectionData();
                currentRtcm1243Data.setSatelliteID(rtcm1243SatId);
                currentRtcm1243Data.setGnssIod(rtcm1243Iod);
                currentRtcm1243Data.setOrbitCorrection(rtcm1243OrbitCorr);
                currentRtcm1243Data.setClockCorrection(rtcm1243ClockCorr);

                // Update the list
                rtcm1243Data.add(currentRtcm1243Data);

            }

            // Return the parsed message
            return new Rtcm1243(messageNumber, rtcm1243Header, rtcm1243Data);

        }

    };

    /** Codes map. */
    private static final Map<Pattern, RtcmMessageType> CODES_MAP = new HashMap<>();
    static {
        for (final RtcmMessageType type : values()) {
            CODES_MAP.put(type.getPattern(), type);
        }
    }

    /** Message pattern (i.e. allowed message numbers). */
    private final Pattern pattern;

    /** Simple constructor.
     * @param messageRegex message regular expression
     */
    RtcmMessageType(final String messageRegex) {
        this.pattern = Pattern.compile(messageRegex);
    }

    /** Get the message number.
     * @return message number
     */
    public Pattern getPattern() {
        return pattern;
    }

    /** Get the message type corresponding to a message number.
     * @param rtcmNumber message number
     * @return the message type corresponding to the message number
     */
    public static RtcmMessageType getMessageType(final String rtcmNumber) {
        // Try to find a match with an existing message type
        for (Map.Entry<Pattern, RtcmMessageType> rtcmEntry : CODES_MAP.entrySet()) {
            // Matcher
            final Matcher matcher = rtcmEntry.getKey().matcher(rtcmNumber);
            // Check the match !
            if (matcher.matches()) {
                // return the message type
                return rtcmEntry.getValue();
            }
        }
        // No match found
        throw new OrekitException(OrekitMessages.UNKNOWN_ENCODED_MESSAGE_NUMBER, rtcmNumber);
    }

}
