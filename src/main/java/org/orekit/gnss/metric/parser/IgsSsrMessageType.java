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
import org.orekit.gnss.SatelliteSystem;
import org.orekit.gnss.metric.messages.ParsedMessage;
import org.orekit.gnss.metric.messages.common.ClockCorrection;
import org.orekit.gnss.metric.messages.common.CodeBias;
import org.orekit.gnss.metric.messages.common.OrbitCorrection;
import org.orekit.gnss.metric.messages.common.PhaseBias;
import org.orekit.gnss.metric.messages.ssr.igm.SsrIgm01;
import org.orekit.gnss.metric.messages.ssr.igm.SsrIgm01Data;
import org.orekit.gnss.metric.messages.ssr.igm.SsrIgm01Header;
import org.orekit.gnss.metric.messages.ssr.igm.SsrIgm02;
import org.orekit.gnss.metric.messages.ssr.igm.SsrIgm02Data;
import org.orekit.gnss.metric.messages.ssr.igm.SsrIgm02Header;
import org.orekit.gnss.metric.messages.ssr.igm.SsrIgm03;
import org.orekit.gnss.metric.messages.ssr.igm.SsrIgm03Data;
import org.orekit.gnss.metric.messages.ssr.igm.SsrIgm03Header;
import org.orekit.gnss.metric.messages.ssr.igm.SsrIgm04;
import org.orekit.gnss.metric.messages.ssr.igm.SsrIgm04Data;
import org.orekit.gnss.metric.messages.ssr.igm.SsrIgm04Header;
import org.orekit.gnss.metric.messages.ssr.igm.SsrIgm05;
import org.orekit.gnss.metric.messages.ssr.igm.SsrIgm05Data;
import org.orekit.gnss.metric.messages.ssr.igm.SsrIgm05Header;
import org.orekit.gnss.metric.messages.ssr.igm.SsrIgm06;
import org.orekit.gnss.metric.messages.ssr.igm.SsrIgm06Data;
import org.orekit.gnss.metric.messages.ssr.igm.SsrIgm06Header;
import org.orekit.gnss.metric.messages.ssr.igm.SsrIgm07;
import org.orekit.gnss.metric.messages.ssr.igm.SsrIgm07Data;
import org.orekit.gnss.metric.messages.ssr.igm.SsrIgm07Header;
import org.orekit.gnss.metric.messages.ssr.subtype.SsrIm201;
import org.orekit.gnss.metric.messages.ssr.subtype.SsrIm201Data;
import org.orekit.gnss.metric.messages.ssr.subtype.SsrIm201Header;

/** Enum containing the supported IGS SSR messages types.
 *
 * @author Luc Maisonobe
 * @author Bryan Cazabonne
 *
 * @see "IGS State Space Representation (SSR) Format, Version 1.00, October 2020."
 *
 * @since 11.0
 */
public enum IgsSsrMessageType implements MessageType {

    /** SSR Orbit Correction. */
    IGM_01("21|41|61|81|101|121") {

        /** {@inheritDoc} */
        @Override
        public ParsedMessage parse(final EncodedMessage encodedMessage, final int messageNumber) {

            // Satellite system
            final SatelliteSystem system = messageNumberToSatelliteSystem(messageNumber);

            // Header data
            final SsrIgm01Header igm01Header = new SsrIgm01Header();
            igm01Header.setSsrEpoch1s(IgsSsrDataField.IDF003.intValue(encodedMessage));
            igm01Header.setSsrUpdateInterval(IgsSsrDataField.IDF004.intValue(encodedMessage));
            igm01Header.setSsrMultipleMessageIndicator(IgsSsrDataField.IDF005.intValue(encodedMessage));
            igm01Header.setIodSsr(IgsSsrDataField.IDF007.intValue(encodedMessage));
            igm01Header.setSsrProviderId(IgsSsrDataField.IDF008.intValue(encodedMessage));
            igm01Header.setSsrSolutionId(IgsSsrDataField.IDF009.intValue(encodedMessage));
            igm01Header.setCrsIndicator(IgsSsrDataField.IDF006.intValue(encodedMessage));

            // Number of satellites
            final int satNumber = IgsSsrDataField.IDF010.intValue(encodedMessage);
            igm01Header.setNumberOfSatellites(satNumber);

            // Initialize list of data
            final List<SsrIgm01Data> igm01Data = new ArrayList<>();

            // Loop on satellites and fill data
            for (int index = 0; index < satNumber; index++) {

                // Satellite ID
                final int igm01SatId = getSatelliteId(system, IgsSsrDataField.IDF011.intValue(encodedMessage));

                // GNSS IOD
                final int igm01Iod = IgsSsrDataField.IDF012.intValue(encodedMessage);

                // Orbit correction
                final OrbitCorrection igm01OrbitCorr =
                                new OrbitCorrection(IgsSsrDataField.IDF013.doubleValue(encodedMessage),   // IGM01 dRadial
                                                    IgsSsrDataField.IDF014.doubleValue(encodedMessage),   // IGM01 dAlongTrack
                                                    IgsSsrDataField.IDF015.doubleValue(encodedMessage),   // IGM01 dCrossTrack
                                                    IgsSsrDataField.IDF016.doubleValue(encodedMessage),   // IGM01 dRadialDot
                                                    IgsSsrDataField.IDF017.doubleValue(encodedMessage),   // IGM01 dAlongTrackDot
                                                    IgsSsrDataField.IDF018.doubleValue(encodedMessage));  // IGM01 dCrossTrackDot

                // Initialize a new container and fill data
                final SsrIgm01Data currentIgm01Data = new SsrIgm01Data();
                currentIgm01Data.setSatelliteID(igm01SatId);
                currentIgm01Data.setGnssIod(igm01Iod);
                currentIgm01Data.setOrbitCorrection(igm01OrbitCorr);

                // Update the list
                igm01Data.add(currentIgm01Data);

            }

            // Return the parsed message
            return new SsrIgm01(messageNumber, system, igm01Header, igm01Data);

        }

    },

    /** SSR Clock Correction. */
    IGM_02("22|42|62|82|102|122") {

        /** {@inheritDoc} */
        @Override
        public ParsedMessage parse(final EncodedMessage encodedMessage, final int messageNumber) {

            // Satellite system
            final SatelliteSystem system = messageNumberToSatelliteSystem(messageNumber);

            // Header data
            final SsrIgm02Header igm02Header = new SsrIgm02Header();
            igm02Header.setSsrEpoch1s(IgsSsrDataField.IDF003.intValue(encodedMessage));
            igm02Header.setSsrUpdateInterval(IgsSsrDataField.IDF004.intValue(encodedMessage));
            igm02Header.setSsrMultipleMessageIndicator(IgsSsrDataField.IDF005.intValue(encodedMessage));
            igm02Header.setIodSsr(IgsSsrDataField.IDF007.intValue(encodedMessage));
            igm02Header.setSsrProviderId(IgsSsrDataField.IDF008.intValue(encodedMessage));
            igm02Header.setSsrSolutionId(IgsSsrDataField.IDF009.intValue(encodedMessage));

            // Number of satellites
            final int satNumber = IgsSsrDataField.IDF010.intValue(encodedMessage);
            igm02Header.setNumberOfSatellites(satNumber);

            // Initialize list of data
            final List<SsrIgm02Data> igm02Data = new ArrayList<>();

            // Loop on satellites and fill data
            for (int index = 0; index < satNumber; index++) {

                // Satellite ID
                final int igm02SatId = getSatelliteId(system, IgsSsrDataField.IDF011.intValue(encodedMessage));

                // Clock correction
                final ClockCorrection igm02ClockCorr =
                                new ClockCorrection(IgsSsrDataField.IDF019.doubleValue(encodedMessage),  // IGM02 C0
                                                    IgsSsrDataField.IDF020.doubleValue(encodedMessage),  // IGM02 C1
                                                    IgsSsrDataField.IDF021.doubleValue(encodedMessage)); // IGM02 C2

                // Initialize a new container and fill data
                final SsrIgm02Data currentIgm02Data = new SsrIgm02Data();
                currentIgm02Data.setSatelliteID(igm02SatId);
                currentIgm02Data.setClockCorrection(igm02ClockCorr);

                // Update the list
                igm02Data.add(currentIgm02Data);

            }

            // Return the parsed message
            return new SsrIgm02(messageNumber, system, igm02Header, igm02Data);

        }

    },

    /** SSR Combined Orbit and Clock Correction. */
    IGM_03("23|43|63|83|103|123") {

        /** {@inheritDoc} */
        @Override
        public ParsedMessage parse(final EncodedMessage encodedMessage, final int messageNumber) {

            // Satellite system
            final SatelliteSystem system = messageNumberToSatelliteSystem(messageNumber);

            // Header data
            final SsrIgm03Header igm03Header = new SsrIgm03Header();
            igm03Header.setSsrEpoch1s(IgsSsrDataField.IDF003.intValue(encodedMessage));
            igm03Header.setSsrUpdateInterval(IgsSsrDataField.IDF004.intValue(encodedMessage));
            igm03Header.setSsrMultipleMessageIndicator(IgsSsrDataField.IDF005.intValue(encodedMessage));
            igm03Header.setIodSsr(IgsSsrDataField.IDF007.intValue(encodedMessage));
            igm03Header.setSsrProviderId(IgsSsrDataField.IDF008.intValue(encodedMessage));
            igm03Header.setSsrSolutionId(IgsSsrDataField.IDF009.intValue(encodedMessage));
            igm03Header.setCrsIndicator(IgsSsrDataField.IDF006.intValue(encodedMessage));

            // Number of satellites
            final int satNumber = IgsSsrDataField.IDF010.intValue(encodedMessage);
            igm03Header.setNumberOfSatellites(satNumber);

            // Initialize list of data
            final List<SsrIgm03Data> igm03Data = new ArrayList<>();

            // Loop on satellites and fill data
            for (int index = 0; index < satNumber; index++) {

                // Satellite ID
                final int igm03SatId = getSatelliteId(system, IgsSsrDataField.IDF011.intValue(encodedMessage));

                // GNSS IOD
                final int igm03Iod = IgsSsrDataField.IDF012.intValue(encodedMessage);

                // Orbit correction
                final OrbitCorrection igm03OrbitCorr =
                                new OrbitCorrection(IgsSsrDataField.IDF013.doubleValue(encodedMessage),   // IGM03 dRadial
                                                    IgsSsrDataField.IDF014.doubleValue(encodedMessage),   // IGM03 dAlongTrack
                                                    IgsSsrDataField.IDF015.doubleValue(encodedMessage),   // IGM03 dCrossTrack
                                                    IgsSsrDataField.IDF016.doubleValue(encodedMessage),   // IGM03 dRadialDot
                                                    IgsSsrDataField.IDF017.doubleValue(encodedMessage),   // IGM03 dAlongTrackDot
                                                    IgsSsrDataField.IDF018.doubleValue(encodedMessage));  // IGM03 dCrossTrackDot

                // Clock correction
                final ClockCorrection igm03ClockCorr =
                                new ClockCorrection(IgsSsrDataField.IDF019.doubleValue(encodedMessage),  // IGM03 C0
                                                    IgsSsrDataField.IDF020.doubleValue(encodedMessage),  // IGM03 C1
                                                    IgsSsrDataField.IDF021.doubleValue(encodedMessage)); // IGM03 C2

                // Initialize a new container and fill data
                final SsrIgm03Data currentIgm03Data = new SsrIgm03Data();
                currentIgm03Data.setSatelliteID(igm03SatId);
                currentIgm03Data.setGnssIod(igm03Iod);
                currentIgm03Data.setOrbitCorrection(igm03OrbitCorr);
                currentIgm03Data.setClockCorrection(igm03ClockCorr);

                // Update the list
                igm03Data.add(currentIgm03Data);

            }

            // Return the parsed message
            return new SsrIgm03(messageNumber, system, igm03Header, igm03Data);

        }

    },

    /** SSR High Rate Clock Correction. */
    IGM_04("24|44|64|84|104|124") {

        /** {@inheritDoc} */
        @Override
        public ParsedMessage parse(final EncodedMessage encodedMessage, final int messageNumber) {

            // Satellite system
            final SatelliteSystem system = messageNumberToSatelliteSystem(messageNumber);

            // Header data
            final SsrIgm04Header igm04Header = new SsrIgm04Header();
            igm04Header.setSsrEpoch1s(IgsSsrDataField.IDF003.intValue(encodedMessage));
            igm04Header.setSsrUpdateInterval(IgsSsrDataField.IDF004.intValue(encodedMessage));
            igm04Header.setSsrMultipleMessageIndicator(IgsSsrDataField.IDF005.intValue(encodedMessage));
            igm04Header.setIodSsr(IgsSsrDataField.IDF007.intValue(encodedMessage));
            igm04Header.setSsrProviderId(IgsSsrDataField.IDF008.intValue(encodedMessage));
            igm04Header.setSsrSolutionId(IgsSsrDataField.IDF009.intValue(encodedMessage));

            // Number of satellites
            final int satNumber = IgsSsrDataField.IDF010.intValue(encodedMessage);
            igm04Header.setNumberOfSatellites(satNumber);

            // Initialize list of data
            final List<SsrIgm04Data> igm04Data = new ArrayList<>();

            // Loop on satellites and fill data
            for (int index = 0; index < satNumber; index++) {

                // Initialize a new container
                final SsrIgm04Data currentIgm04Data = new SsrIgm04Data();
                currentIgm04Data.setSatelliteID(getSatelliteId(system, IgsSsrDataField.IDF011.intValue(encodedMessage)));
                currentIgm04Data.setHighRateClockCorrection(IgsSsrDataField.IDF022.doubleValue(encodedMessage));

                // Update the list
                igm04Data.add(currentIgm04Data);

            }

            // Return the parsed message
            return new SsrIgm04(messageNumber, system, igm04Header, igm04Data);

        }

    },

    /** SSR Code Bias. */
    IGM_05("25|45|65|85|105|125") {

        /** {@inheritDoc} */
        @Override
        public ParsedMessage parse(final EncodedMessage encodedMessage, final int messageNumber) {

            // Satellite system
            final SatelliteSystem system = messageNumberToSatelliteSystem(messageNumber);

            // Header data
            final SsrIgm05Header igm05Header = new SsrIgm05Header();
            igm05Header.setSsrEpoch1s(IgsSsrDataField.IDF003.intValue(encodedMessage));
            igm05Header.setSsrUpdateInterval(IgsSsrDataField.IDF004.intValue(encodedMessage));
            igm05Header.setSsrMultipleMessageIndicator(IgsSsrDataField.IDF005.intValue(encodedMessage));
            igm05Header.setIodSsr(IgsSsrDataField.IDF007.intValue(encodedMessage));
            igm05Header.setSsrProviderId(IgsSsrDataField.IDF008.intValue(encodedMessage));
            igm05Header.setSsrSolutionId(IgsSsrDataField.IDF009.intValue(encodedMessage));

            // Number of satellites
            final int satNumber = IgsSsrDataField.IDF010.intValue(encodedMessage);
            igm05Header.setNumberOfSatellites(satNumber);

            // Initialize list of data
            final List<SsrIgm05Data> igm05Data = new ArrayList<>();

            // Loop on satellites
            for (int index = 0; index < satNumber; index++) {

                // Initialize a new container
                final SsrIgm05Data currentIgm05Data = new SsrIgm05Data();
                currentIgm05Data.setSatelliteID(getSatelliteId(system, IgsSsrDataField.IDF011.intValue(encodedMessage)));

                // Number of biases
                final int biasesNumber = IgsSsrDataField.IDF023.intValue(encodedMessage);
                currentIgm05Data.setNumberOfBiasesProcessed(biasesNumber);

                // Loop on biases
                for (int biasIndex = 0; biasIndex < biasesNumber; biasIndex++) {
                    // Initialize a new code bias
                    final CodeBias codeBias = new CodeBias(IgsSsrDataField.IDF024.intValue(encodedMessage),
                                                           IgsSsrDataField.IDF025.doubleValue(encodedMessage));
                    // Add the codeBias to the container
                    currentIgm05Data.addCodeBias(codeBias);
                }

                // Update the list of data
                igm05Data.add(currentIgm05Data);

            }

            // Return the parsed message
            return new SsrIgm05(messageNumber, system, igm05Header, igm05Data);

        }

    },

    /** SSR Phase Bias. */
    IGM_06("26|46|66|86|106|126") {

        /** {@inheritDoc} */
        @Override
        public ParsedMessage parse(final EncodedMessage encodedMessage, final int messageNumber) {

            // Satellite system
            final SatelliteSystem system = messageNumberToSatelliteSystem(messageNumber);

            // Header data
            final SsrIgm06Header igm06Header = new SsrIgm06Header();
            igm06Header.setSsrEpoch1s(IgsSsrDataField.IDF003.intValue(encodedMessage));
            igm06Header.setSsrUpdateInterval(IgsSsrDataField.IDF004.intValue(encodedMessage));
            igm06Header.setSsrMultipleMessageIndicator(IgsSsrDataField.IDF005.intValue(encodedMessage));
            igm06Header.setIodSsr(IgsSsrDataField.IDF007.intValue(encodedMessage));
            igm06Header.setSsrProviderId(IgsSsrDataField.IDF008.intValue(encodedMessage));
            igm06Header.setSsrSolutionId(IgsSsrDataField.IDF009.intValue(encodedMessage));
            igm06Header.setIsConsistencyMaintained(IgsSsrDataField.IDF032.booleanValue(encodedMessage));
            igm06Header.setIsMelbourneWubbenaConsistencyMaintained(IgsSsrDataField.IDF033.booleanValue(encodedMessage));

            // Number of satellites
            final int satNumber = IgsSsrDataField.IDF010.intValue(encodedMessage);
            igm06Header.setNumberOfSatellites(satNumber);

            // Initialize list of data
            final List<SsrIgm06Data> igm06Data = new ArrayList<>();

            // Loop on satellites
            for (int index = 0; index < satNumber; index++) {

                // Initialize a new container
                final SsrIgm06Data currentIgm06Data = new SsrIgm06Data();
                currentIgm06Data.setSatelliteID(getSatelliteId(system, IgsSsrDataField.IDF011.intValue(encodedMessage)));

                // Number of biases
                final int biasesNumber = IgsSsrDataField.IDF023.intValue(encodedMessage);
                currentIgm06Data.setNumberOfBiasesProcessed(biasesNumber);

                // Yaw angle and rate
                currentIgm06Data.setYawAngle(IgsSsrDataField.IDF026.doubleValue(encodedMessage));
                currentIgm06Data.setYawRate(IgsSsrDataField.IDF027.doubleValue(encodedMessage));

                // Loop on biases
                for (int biasIndex = 0; biasIndex < biasesNumber; biasIndex++) {
                    // Initialize a new phase bias
                    final PhaseBias phaseBias = new PhaseBias(IgsSsrDataField.IDF024.intValue(encodedMessage),
                                                              IgsSsrDataField.IDF029.booleanValue(encodedMessage),
                                                              IgsSsrDataField.IDF030.intValue(encodedMessage),
                                                              IgsSsrDataField.IDF031.intValue(encodedMessage),
                                                              IgsSsrDataField.IDF028.doubleValue(encodedMessage));
                    // Add the codeBias to the container
                    currentIgm06Data.addPhaseBias(phaseBias);
                }

                // Update the list of data
                igm06Data.add(currentIgm06Data);

            }

            // Return the parsed message
            return new SsrIgm06(messageNumber, system, igm06Header, igm06Data);

        }

    },

    /** SSR URA. */
    IGM_07("27|47|67|87|107|127") {

        /** {@inheritDoc} */
        @Override
        public ParsedMessage parse(final EncodedMessage encodedMessage, final int messageNumber) {

            // Satellite system
            final SatelliteSystem system = messageNumberToSatelliteSystem(messageNumber);

            // Header data
            final SsrIgm07Header igm07Header = new SsrIgm07Header();
            igm07Header.setSsrEpoch1s(IgsSsrDataField.IDF003.intValue(encodedMessage));
            igm07Header.setSsrUpdateInterval(IgsSsrDataField.IDF004.intValue(encodedMessage));
            igm07Header.setSsrMultipleMessageIndicator(IgsSsrDataField.IDF005.intValue(encodedMessage));
            igm07Header.setIodSsr(IgsSsrDataField.IDF007.intValue(encodedMessage));
            igm07Header.setSsrProviderId(IgsSsrDataField.IDF008.intValue(encodedMessage));
            igm07Header.setSsrSolutionId(IgsSsrDataField.IDF009.intValue(encodedMessage));

            // Number of satellites
            final int satNumber = IgsSsrDataField.IDF010.intValue(encodedMessage);
            igm07Header.setNumberOfSatellites(satNumber);

            // Initialize list of data
            final List<SsrIgm07Data> igm07Data = new ArrayList<>();

            // Loop on satellites and fill data
            for (int index = 0; index < satNumber; index++) {

                // Initialize a new container
                final SsrIgm07Data currentIgm07Data = new SsrIgm07Data();
                currentIgm07Data.setSatelliteID(getSatelliteId(system, IgsSsrDataField.IDF011.intValue(encodedMessage)));
                currentIgm07Data.setSsrUra(IgsSsrDataField.IDF034.intValue(encodedMessage));

                // Update the list
                igm07Data.add(currentIgm07Data);

            }

            // Return the parsed message
            return new SsrIgm07(messageNumber, system, igm07Header, igm07Data);

        }

    },

    /** SSR Ionosphere VTEC Spherical Harmonics Message. */
    IM_201("201") {

        /** {@inheritDoc} */
        @Override
        public ParsedMessage parse(final EncodedMessage encodedMessage, final int messageNumber) {

            // Header data
            final SsrIm201Header im201Header = new SsrIm201Header();
            im201Header.setSsrEpoch1s(IgsSsrDataField.IDF003.intValue(encodedMessage));
            im201Header.setSsrUpdateInterval(IgsSsrDataField.IDF004.intValue(encodedMessage));
            im201Header.setSsrMultipleMessageIndicator(IgsSsrDataField.IDF005.intValue(encodedMessage));
            im201Header.setIodSsr(IgsSsrDataField.IDF007.intValue(encodedMessage));
            im201Header.setSsrProviderId(IgsSsrDataField.IDF008.intValue(encodedMessage));
            im201Header.setSsrSolutionId(IgsSsrDataField.IDF009.intValue(encodedMessage));
            im201Header.setVtecQualityIndicator(IgsSsrDataField.IDF041.doubleValue(encodedMessage));

            // Number of ionospheric layers
            final int numberOfIonosphericLayers = IgsSsrDataField.IDF035.intValue(encodedMessage);
            im201Header.setNumberOfIonosphericLayers(numberOfIonosphericLayers);

            // Initialize list of data
            final List<SsrIm201Data> im201Data = new ArrayList<>();

            // Loop on ionospheric layers
            for (int layerIndex = 0; layerIndex < numberOfIonosphericLayers; layerIndex++) {

                // Initialize a new container
                final SsrIm201Data currentIm201Data = new SsrIm201Data();

                // Height of the ionospheric layer
                currentIm201Data.setHeightIonosphericLayer(IgsSsrDataField.IDF036.doubleValue(encodedMessage));

                // Degree and order of spherical harmonics
                final int n = IgsSsrDataField.IDF037.intValue(encodedMessage);
                final int m = IgsSsrDataField.IDF038.intValue(encodedMessage);

                // Initialize arrays
                final double[][] cnm = new double[n + 1][m + 1];
                final double[][] snm = new double[n + 1][m + 1];

                ////
                // Cosine coefficients
                ////

                // Loop on degree
                for (int order = 0; order <= m; order++) {
                    // Loop on order
                    for (int degree = order; degree <= n; degree++) {
                        cnm[degree][order] = IgsSsrDataField.IDF039.doubleValue(encodedMessage);
                    }
                }

                ////
                // Sine coefficients
                ////

                // Loop on degree
                for (int order = 1; order <= m; order++) {
                    // Loop on order
                    for (int degree = order; degree <= n; degree++) {
                        snm[degree][order] = IgsSsrDataField.IDF040.doubleValue(encodedMessage);
                    }
                }

                currentIm201Data.setSphericalHarmonicsDegree(n);
                currentIm201Data.setSphericalHarmonicsOrder(m);
                currentIm201Data.setCnm(cnm);
                currentIm201Data.setSnm(snm);

                // Update the list
                im201Data.add(currentIm201Data);

            }

            // Return the parsed message
            return new SsrIm201(messageNumber, im201Header, im201Data);

        }

    };

    /** Codes map. */
    private static final Map<Pattern, IgsSsrMessageType> CODES_MAP = new HashMap<>();
    static {
        for (final IgsSsrMessageType type : values()) {
            CODES_MAP.put(type.getPattern(), type);
        }
    }

    /** Message pattern (i.e. allowed message numbers). */
    private final Pattern pattern;

    /** Simple constructor.
     * @param regex message regular expression
     */
    IgsSsrMessageType(final String regex) {
        this.pattern = Pattern.compile(regex);
    }

    /** Get the message number.
     * @return message number
     */
    public Pattern getPattern() {
        return pattern;
    }

    /** Get the message type corresponding to a message number.
     * @param number message number
     * @return the message type corresponding to the message number
     */
    public static IgsSsrMessageType getMessageType(final String number) {
        // Try to find a match with an existing message type
        for (Map.Entry<Pattern, IgsSsrMessageType> entry : CODES_MAP.entrySet()) {
            // Matcher
            final Matcher matcher = entry.getKey().matcher(number);
            // Check the match !
            if (matcher.matches()) {
                // return the message type
                return entry.getValue();
            }
        }
        // No match found
        throw new OrekitException(OrekitMessages.UNKNOWN_ENCODED_MESSAGE_NUMBER, number);
    }

    /**
     * Find the satellite system corresponding to the sub-type message number.
     * <p>
     * See Table 5 of reference
     * </p>
     * @param subTypeMessage message umber
     * @return the corresponding satellite system
     */
    public static SatelliteSystem messageNumberToSatelliteSystem(final int subTypeMessage) {

        if (subTypeMessage > 20 && subTypeMessage <= 40) {
            // GPS messages
            return SatelliteSystem.GPS;
        } else if (subTypeMessage <= 60) {
            // GLONASS messages
            return SatelliteSystem.GLONASS;
        } else if (subTypeMessage <= 80) {
            // Galileo messages
            return SatelliteSystem.GALILEO;
        } else if (subTypeMessage <= 100) {
            // QZSS messages
            return SatelliteSystem.QZSS;
        } else if (subTypeMessage <= 120) {
            // Beidou messages
            return SatelliteSystem.BEIDOU;
        } else if (subTypeMessage <= 140) {
            // SBAS messages
            return SatelliteSystem.SBAS;
        } else {
            // IRNSS messages
            return SatelliteSystem.IRNSS;
        }

    }

    /**
     * Transform the satellite ID parsed from the IGS SSR message to the real ID.
     * @param system the satellite system of the parsed message
     * @param id the parsed satellite ID
     * @return the real satellite ID
     */
    public static int getSatelliteId(final SatelliteSystem system, final int id) {

        // Switch on satellite systems
        switch (system) {
            case QZSS:
                // ID = ID(parsed) + 192
                return id + 192;
            case SBAS:
                // ID = ID(parsed) + 119
                return id + 119;
            default:
                // For GPS, GLONASS, Beidou, and Galileo the id is unchanged
                return id;
        }

    }

}
