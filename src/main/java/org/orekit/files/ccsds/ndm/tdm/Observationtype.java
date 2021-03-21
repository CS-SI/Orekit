/* Copyright 2002-2021 CS GROUP
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
package org.orekit.files.ccsds.ndm.tdm;

import java.util.regex.Pattern;

import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.utils.ContextBinding;
import org.orekit.files.ccsds.utils.lexical.ParseToken;
import org.orekit.files.ccsds.utils.lexical.TokenType;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;


/** Keys for {@link Observation TDM observations} entries.
 * @author Maxime Journot
 * @since 11.0
 */
public enum Observationtype {

    // Signal related keywords.
    /** Data: Carrier power [dBW].<p>
     *  Strength of the radio signal transmitted by the spacecraft as received at the ground station or at another spacecraft.
     */
    CARRIER_POWER((ruConverter, medatada, date, value) -> value),
    /** Data: Doppler instantaneous [km/s].<p>
     *  Instantaneous range rate of the spacecraft.
     */
    DOPPLER_INSTANTANEOUS((ruConverter, medatada, date, value) -> 1000.0 * value),
    /** Data: Doppler integrated [km/s].<p>
     *  Mean range rate of the spacecraft over the INTEGRATION_INTERVAL specified in the meta-data section.
     */
    DOPPLER_INTEGRATED((ruConverter, medatada, date, value) -> 1000.0 * value),
    /** Data: Carrier power to noise spectral density ratio (Pc/No) [dBHz]. */
    PC_N0((ruConverter, medatada, date, value) -> value),
    /** Data: Ranging power to noise spectral density ratio (Pr/No) [dBHz]. */
    PR_N0((ruConverter, medatada, date, value) -> value),
    /** Data: Range value [km, s or RU].
     * @see #RANGE_UNITS
     */
    RANGE((ruConverter, medatada, date, value) -> {
        if (medatada.getRangeUnits() == RangeUnits.km) {
            return 1000.0 * value;
        } else if (medatada.getRangeUnits() == RangeUnits.s) {
            return Constants.SPEED_OF_LIGHT * value;
        } else {
            if (ruConverter == null) {
                throw new OrekitException(OrekitMessages.CCSDS_TDM_MISSING_RANGE_UNITS_CONVERTER);
            }
            return ruConverter.ruToMeters(medatada, date, value);
        }
    }),
    /** Data: Received frequencies [Hz].<p>
     * The RECEIVE_FREQ keyword shall be used to indicate that the values represent measurements of the received frequency.<p>
     * The keyword is indexed to accommodate a scenario in which multiple downlinks are used.<p>
     * RECEIVE_FREQ_n (n = 1, 2, 3, 4, 5)
     */
    RECEIVE_FREQ_1((ruConverter, medatada, date, value) -> value),
    /** Received frequency 2. */
    RECEIVE_FREQ_2((ruConverter, medatada, date, value) -> value),
    /** Received frequency 3. */
    RECEIVE_FREQ_3((ruConverter, medatada, date, value) -> value),
    /** Received frequency 4. */
    RECEIVE_FREQ_4((ruConverter, medatada, date, value) -> value),
    /** Received frequency 5. */
    RECEIVE_FREQ_5((ruConverter, medatada, date, value) -> value),
    /** Data: Received frequency [Hz].<p>
     *  Case without an index; where the frequency cannot be associated with a particular participant.
     */
    RECEIVE_FREQ((ruConverter, medatada, date, value) -> value),
    /** Data: Transmitted frequencies [Hz].<p>
     * The TRANSMIT_FREQ keyword shall be used to indicate that the values represent measurements of a transmitted frequency, e.g., from an uplink operation.<p>
     * The TRANSMIT_FREQ keyword is indexed to accommodate scenarios in which multiple transmitters are used.<p>
     * TRANSMIT_FREQ_n (n = 1, 2, 3, 4, 5)
     */
    TRANSMIT_FREQ_1((ruConverter, medatada, date, value) -> value),
    /** Transmitted frequency 2. */
    TRANSMIT_FREQ_2((ruConverter, medatada, date, value) -> value),
    /** Transmitted frequency 3. */
    TRANSMIT_FREQ_3((ruConverter, medatada, date, value) -> value),
    /** Transmitted frequency 4. */
    TRANSMIT_FREQ_4((ruConverter, medatada, date, value) -> value),
    /** Transmitted frequency 5. */
    TRANSMIT_FREQ_5((ruConverter, medatada, date, value) -> value),
    /** Data: Transmitted frequencies rates [Hz/s].<p>
     * The value associated with the TRANSMIT_FREQ_RATE_n keyword is the linear rate of
     * change of the frequency TRANSMIT_FREQ_n starting at the timetag and continuing
     *  until the next TRANSMIT_FREQ_RATE_n timetag (or until the end of the data).<p>
     * TRANSMIT_FREQ_RATE_n (n = 1, 2, 3, 4, 5)
     */
    TRANSMIT_FREQ_RATE_1((ruConverter, medatada, date, value) -> value),
    /** Transmitted frequency rate 2. */
    TRANSMIT_FREQ_RATE_2((ruConverter, medatada, date, value) -> value),
    /** Transmitted frequency rate 3. */
    TRANSMIT_FREQ_RATE_3((ruConverter, medatada, date, value) -> value),
    /** Transmitted frequency rate 4. */
    TRANSMIT_FREQ_RATE_4((ruConverter, medatada, date, value) -> value),
    /** Transmitted frequency rate 5. */
    TRANSMIT_FREQ_RATE_5((ruConverter, medatada, date, value) -> value),

    // VLBI/Delta-DOR Related Keywords
    /** Data: DOR [s].<p>
     * the DOR keyword represents the range measured via PATH_2 minus the range measured via PATH_1.
     */
    DOR((ruConverter, medatada, date, value) -> value),
    /** Data: VLBI delay [s].<p>
     * The observable associated with the VLBI_DELAY keyword represents the time of signal
     * arrival via PATH_2 minus the time of signal arrival via PATH_1.
     */
    VLBI_DELAY((ruConverter, medatada, date, value) -> value),

    // Angle Related Keywords
    /** Data: ANGLE_1 in degrees and in [-180, +360[ [deg].<p>
     * The value assigned to the ANGLE_1 keyword represents the azimuth, right ascension, or ‘X’
     * angle of the measurement, depending on the value of the ANGLE_TYPE keyword.<p>
     * The angle measurement shall be a double precision value as follows: -180.0 &le; ANGLE_1 &lt; 360.0<p>
     * Units shall be degrees.<p>
     * See meta-data keyword ANGLE_TYPE for the definition of the angles.
     */
    ANGLE_1((ruConverter, medatada, date, value) -> FastMath.toRadians(value)),
    /** Data: ANGLE_2 in degrees and in [-180, +360[ [deg].<p>
     * The value assigned to the ANGLE_2 keyword represents the elevation, declination, or ‘Y’
     * angle of the measurement, depending on the value of the ANGLE_TYPE keyword.<p>
     * The angle measurement shall be a double precision value as follows: -180.0 &le; ANGLE_2 &lt; 360.0.<p>
     * Units shall be degrees.<p>
     * See meta-data keyword ANGLE_TYPE for the definition of the angles.
     */
    ANGLE_2((ruConverter, medatada, date, value) -> FastMath.toRadians(value)),

    // Time Related Keywords
    /** Data: Clock bias [s].<p>
     * The CLOCK_BIAS keyword can be used by the message recipient to adjust timetag
     * measurements by a specified amount with respect to a common reference.
     */
    CLOCK_BIAS((ruConverter, medatada, date, value) -> value),
    /** Data: Clock drift [s/s].<p>
     * The CLOCK_DRIFT keyword should be used to adjust timetag measurements by an amount that is a function of time with
     * respect to a common reference, normally UTC (as opposed to the CLOCK_BIAS, which is meant to be a constant adjustment).
     */
    CLOCK_DRIFT((ruConverter, medatada, date, value) -> value),

    // Media Related Keywords
    /** Data: STEC - Slant Total Electron Count [TECU].
     * The STEC keyword shall be used to convey the line of sight,
     * one way charged particle delay or total electron count (TEC) at the timetag associated with a
     * tracking measurement, which is calculated by integrating the electron density along the
     * propagation path (electrons/m2).
     */
    STEC((ruConverter, medatada, date, value) -> value),
    /** Data: TROPO DRY [m].<p>
     * Dry zenith delay through the troposphere measured at the timetag.
     */
    TROPO_DRY((ruConverter, medatada, date, value) -> value),
    /** Data: TROPO WET [m].<p>
     * Wet zenith delay through the troposphere measured at the timetag.
     */
    TROPO_WET((ruConverter, medatada, date, value) -> value),

    // Meteorological Related Keywords
    /** Data: Pressure [hPa].<p>
     * Atmospheric pressure observable as measured at the tracking participant.
     */
    PRESSURE((ruConverter, medatada, date, value) -> value),
    /** Data: Relative humidity [%].<p>
     * Relative humidity observable as measured at the tracking participant.
     */
    RHUMIDITY((ruConverter, medatada, date, value) -> value),
    /** Data: Temperature [K].<p>
     * Temperature observable as measured at the tracking participant.
     */
    TEMPERATURE((ruConverter, medatada, date, value) -> value);

    /** Pattern for delimiting regular expressions. */
    private static final Pattern SEPARATOR = Pattern.compile("\\s+");

    /** Units converter. */
    private final UnitsConverter unitsConverter;

    /** Simple constructor.
     * @param unitsConverter converter for observation units
     */
    Observationtype(final UnitsConverter unitsConverter) {
        this.unitsConverter = unitsConverter;
    }

    /** Process an observation line.
     * @param token parse token
     * @param context context binding
     * @param ruConverter converter for {@link RangeUnits#RU Range Units} (may be null)
     * @param metadata metadata for current block
     * @param observationsBlock observation block to fill
     * @return true if token was accepted
     */
    public boolean process(final ParseToken token, final ContextBinding context,
                           final RangeUnitsConverter ruConverter, final TdmMetadata metadata,
                           final ObservationsBlock observationsBlock) {

        if (token.getType() == TokenType.ENTRY) {
            // in an XML file, an observation element contains only the value, the epoch has been parsed before
            // in a KVN file, an observation line should contains both epoch and value

            if (observationsBlock.getCurrentObservationEpoch() != null) {
                // we are parsing an XML file with epoch already parsed
                // parse the measurement
                observationsBlock.addObservationValue(this,
                                                      unitsConverter.rawToSI(ruConverter, metadata,
                                                                             observationsBlock.getCurrentObservationEpoch(),
                                                                             token.getContentAsDouble()));
            } else {

                // we are parsing a KVN file and need to parse both epoch and measurement
                final String[] fields = SEPARATOR.split(token.getContentAsNormalizedString());
                if (fields.length != 2) {
                    throw token.generateException(null);
                }
                // parse the epoch
                final AbsoluteDate epoch = context.getTimeSystem().getConverter(context).parse(fields[0]);
                observationsBlock.addObservationEpoch(epoch);

                // parse the measurement
                try {
                    observationsBlock.addObservationValue(this,
                                                          unitsConverter.rawToSI(ruConverter, metadata,
                                                                                 observationsBlock.getCurrentObservationEpoch(),
                                                                                 Double.parseDouble(fields[1])));
                } catch (NumberFormatException nfe) {
                    throw token.generateException(nfe);
                }
            }
        }

        return true;

    }

    /** Interface for converting observation units. */
    interface UnitsConverter {
        /** Convert a measurement to SI units.
         * @param ruConverter converter for {@link RangeUnits#RU Range Units} (may be null)
         * @param metadata metadata corresponding to the observation
         * @param date observation date
         * @param value measurement raw value
         * @return measurement in SI units
         */
        double rawToSI(RangeUnitsConverter ruConverter, TdmMetadata metadata, AbsoluteDate date, double value);
    }

}
