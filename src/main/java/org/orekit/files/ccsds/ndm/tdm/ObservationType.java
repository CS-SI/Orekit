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
package org.orekit.files.ccsds.ndm.tdm;

import java.util.regex.Pattern;

import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.definitions.Units;
import org.orekit.files.ccsds.utils.ContextBinding;
import org.orekit.files.ccsds.utils.lexical.ParseToken;
import org.orekit.files.ccsds.utils.lexical.TokenType;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.units.Unit;


/** Keys for {@link Observation TDM observations} entries.
 * @author Maxime Journot
 * @since 11.0
 */
public enum ObservationType {

    // Signal related keywords.
    /** Data: Carrier power [dBW].<p>
     *  Strength of the radio signal transmitted by the spacecraft as received at the ground station or at another spacecraft.
     */
    CARRIER_POWER(Unit.ONE),
    /** Data: Doppler counts [n/a].<p>
     *  Count of signal cycles.
     */
    DOPPLER_COUNT(Unit.ONE),
    /** Data: Doppler instantaneous [km/s].<p>
     *  Instantaneous range rate of the spacecraft.
     */
    DOPPLER_INSTANTANEOUS(Units.KM_PER_S),
    /** Data: Doppler integrated [km/s].<p>
     *  Mean range rate of the spacecraft over the INTEGRATION_INTERVAL specified in the meta-data section.
     */
    DOPPLER_INTEGRATED(Units.KM_PER_S),
    /** Data: Carrier power to noise spectral density ratio (Pc/No) [dBHz]. */
    PC_N0(Unit.ONE),
    /** Data: Ranging power to noise spectral density ratio (Pr/No) [dBHz]. */
    PR_N0(Unit.ONE),
    /** Data: phase cycle count at receiver. */
    RECEIVE_PHASE_CT_1(Unit.ONE),
    /** Data: phase cycle count at receiver. */
    RECEIVE_PHASE_CT_2(Unit.ONE),
    /** Data: phase cycle count at receiver. */
    RECEIVE_PHASE_CT_3(Unit.ONE),
    /** Data: phase cycle count at receiver. */
    RECEIVE_PHASE_CT_4(Unit.ONE),
    /** Data: phase cycle count at receiver. */
    RECEIVE_PHASE_CT_5(Unit.ONE),
    /** Data: phase cycle count at transmitter. */
    TRANSMIT_PHASE_CT_1(Unit.ONE),
    /** Data: phase cycle count at transmitter. */
    TRANSMIT_PHASE_CT_2(Unit.ONE),
    /** Data: phase cycle count at transmitter. */
    TRANSMIT_PHASE_CT_3(Unit.ONE),
    /** Data: phase cycle count at transmitter. */
    TRANSMIT_PHASE_CT_4(Unit.ONE),
    /** Data: phase cycle count at transmitter. */
    TRANSMIT_PHASE_CT_5(Unit.ONE),
    /** Data: Range value [km, s or RU].
     * @see RangeUnits
     */
    RANGE(Unit.KILOMETRE) {

        /** {@inheritDoc}*/
        @Override
        public double rawToSI(final RangeUnitsConverter ruConverter, final TdmMetadata metadata,
                              final AbsoluteDate date, final double rawValue) {
            if (metadata.getRangeUnits() == RangeUnits.km) {
                return Unit.KILOMETRE.toSI(rawValue);
            } else if (metadata.getRangeUnits() == RangeUnits.s) {
                return rawValue * Constants.SPEED_OF_LIGHT;
            } else {
                if (ruConverter == null) {
                    throw new OrekitException(OrekitMessages.CCSDS_TDM_MISSING_RANGE_UNITS_CONVERTER);
                }
                return ruConverter.ruToMeters(metadata, date, rawValue);
            }
        }

        /** {@inheritDoc}*/
        @Override
        public double siToRaw(final RangeUnitsConverter ruConverter, final TdmMetadata metadata,
                              final AbsoluteDate date, final double siValue) {
            if (metadata.getRangeUnits() == RangeUnits.km) {
                return Unit.KILOMETRE.fromSI(siValue);
            } else if (metadata.getRangeUnits() == RangeUnits.s) {
                return siValue / Constants.SPEED_OF_LIGHT;
            } else {
                if (ruConverter == null) {
                    throw new OrekitException(OrekitMessages.CCSDS_TDM_MISSING_RANGE_UNITS_CONVERTER);
                }
                return ruConverter.metersToRu(metadata, date, siValue);
            }
        }

    },

    /** Data: Received frequencies [Hz].<p>
     * The RECEIVE_FREQ keyword shall be used to indicate that the values represent measurements of the received frequency.<p>
     * The keyword is indexed to accommodate a scenario in which multiple downlinks are used.<p>
     * RECEIVE_FREQ_n (n = 1, 2, 3, 4, 5)
     */
    RECEIVE_FREQ_1(Unit.HERTZ),
    /** Received frequency 2. */
    RECEIVE_FREQ_2(Unit.HERTZ),
    /** Received frequency 3. */
    RECEIVE_FREQ_3(Unit.HERTZ),
    /** Received frequency 4. */
    RECEIVE_FREQ_4(Unit.HERTZ),
    /** Received frequency 5. */
    RECEIVE_FREQ_5(Unit.HERTZ),
    /** Data: Received frequency [Hz].<p>
     *  Case without an index; where the frequency cannot be associated with a particular participant.
     */
    RECEIVE_FREQ(Unit.HERTZ),
    /** Data: Transmitted frequencies [Hz].<p>
     * The TRANSMIT_FREQ keyword shall be used to indicate that the values represent measurements of a transmitted frequency, e.g., from an uplink operation.<p>
     * The TRANSMIT_FREQ keyword is indexed to accommodate scenarios in which multiple transmitters are used.<p>
     * TRANSMIT_FREQ_n (n = 1, 2, 3, 4, 5)
     */
    TRANSMIT_FREQ_1(Unit.HERTZ),
    /** Transmitted frequency 2. */
    TRANSMIT_FREQ_2(Unit.HERTZ),
    /** Transmitted frequency 3. */
    TRANSMIT_FREQ_3(Unit.HERTZ),
    /** Transmitted frequency 4. */
    TRANSMIT_FREQ_4(Unit.HERTZ),
    /** Transmitted frequency 5. */
    TRANSMIT_FREQ_5(Unit.HERTZ),
    /** Data: Transmitted frequencies rates [Hz/s].<p>
     * The value associated with the TRANSMIT_FREQ_RATE_n keyword is the linear rate of
     * change of the frequency TRANSMIT_FREQ_n starting at the timetag and continuing
     *  until the next TRANSMIT_FREQ_RATE_n timetag (or until the end of the data).<p>
     * TRANSMIT_FREQ_RATE_n (n = 1, 2, 3, 4, 5)
     */
    TRANSMIT_FREQ_RATE_1(Units.HZ_PER_S),
    /** Transmitted frequency rate 2. */
    TRANSMIT_FREQ_RATE_2(Units.HZ_PER_S),
    /** Transmitted frequency rate 3. */
    TRANSMIT_FREQ_RATE_3(Units.HZ_PER_S),
    /** Transmitted frequency rate 4. */
    TRANSMIT_FREQ_RATE_4(Units.HZ_PER_S),
    /** Transmitted frequency rate 5. */
    TRANSMIT_FREQ_RATE_5(Units.HZ_PER_S),

    // VLBI/Delta-DOR Related Keywords
    /** Data: DOR [s].<p>
     * the DOR keyword represents the range measured via PATH_2 minus the range measured via PATH_1.
     */
    DOR(Unit.SECOND),
    /** Data: VLBI delay [s].<p>
     * The observable associated with the VLBI_DELAY keyword represents the time of signal
     * arrival via PATH_2 minus the time of signal arrival via PATH_1.
     */
    VLBI_DELAY(Unit.SECOND),

    // Angle Related Keywords
    /** Data: ANGLE_1 in degrees and in [-180, +360[ [deg].<p>
     * The value assigned to the ANGLE_1 keyword represents the azimuth, right ascension, or ‘X’
     * angle of the measurement, depending on the value of the ANGLE_TYPE keyword.<p>
     * The angle measurement shall be a double precision value as follows: -180.0 &le; ANGLE_1 &lt; 360.0<p>
     * Units shall be degrees.<p>
     * See meta-data keyword ANGLE_TYPE for the definition of the angles.
     */
    ANGLE_1(Unit.DEGREE),
    /** Data: ANGLE_2 in degrees and in [-180, +360[ [deg].<p>
     * The value assigned to the ANGLE_2 keyword represents the elevation, declination, or ‘Y’
     * angle of the measurement, depending on the value of the ANGLE_TYPE keyword.<p>
     * The angle measurement shall be a double precision value as follows: -180.0 &le; ANGLE_2 &lt; 360.0.<p>
     * Units shall be degrees.<p>
     * See meta-data keyword ANGLE_TYPE for the definition of the angles.
     */
    ANGLE_2(Unit.DEGREE),

    // Optical/Radar Related keywords
    /** Data: visual magnitude. */
    MAG(Unit.ONE),
    /** Data: Radar Cross section [m²]. */
    RCS(Units.M2),

    // Time Related Keywords
    /** Data: Clock bias [s].<p>
     * The CLOCK_BIAS keyword can be used by the message recipient to adjust timetag
     * measurements by a specified amount with respect to a common reference.
     */
    CLOCK_BIAS(Unit.SECOND),
    /** Data: Clock drift [s/s].<p>
     * The CLOCK_DRIFT keyword should be used to adjust timetag measurements by an amount that is a function of time with
     * respect to a common reference, normally UTC (as opposed to the CLOCK_BIAS, which is meant to be a constant adjustment).
     */
    CLOCK_DRIFT(Unit.ONE),

    // Media Related Keywords
    /** Data: STEC - Slant Total Electron Count [TECU].
     * The STEC keyword shall be used to convey the line of sight,
     * one way charged particle delay or total electron count (TEC) at the timetag associated with a
     * tracking measurement, which is calculated by integrating the electron density along the
     * propagation path (electrons/m2).
     */
    STEC(Unit.TOTAL_ELECTRON_CONTENT_UNIT),
    /** Data: TROPO DRY [m].<p>
     * Dry zenith delay through the troposphere measured at the timetag.
     */
    TROPO_DRY(Unit.METRE),
    /** Data: TROPO WET [m].<p>
     * Wet zenith delay through the troposphere measured at the timetag.
     */
    TROPO_WET(Unit.METRE),

    // Meteorological Related Keywords
    /** Data: Pressure [hPa].<p>
     * Atmospheric pressure observable as measured at the tracking participant.
     */
    PRESSURE(Units.HECTO_PASCAL),
    /** Data: Relative humidity [%].<p>
     * Relative humidity observable as measured at the tracking participant.
     */
    RHUMIDITY(Unit.PERCENT),
    /** Data: Temperature [K].<p>
     * Temperature observable as measured at the tracking participant.
     */
    TEMPERATURE(Unit.ONE);

    /** Pattern for delimiting regular expressions. */
    private static final Pattern SEPARATOR = Pattern.compile("\\s+");

    /** Unit. */
    private final Unit unit;

    /** Simple constructor.
     * @param unit observation unit
     */
    ObservationType(final Unit unit) {
        this.unit = unit;
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
                final AbsoluteDate epoch    = observationsBlock.getCurrentObservationEpoch();
                final double       rawValue = token.getContentAsDouble();
                observationsBlock.addObservationValue(this, rawToSI(ruConverter, metadata, epoch, rawValue));
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
                    final double rawValue = Double.parseDouble(fields[1]);
                    observationsBlock.addObservationValue(this, rawToSI(ruConverter, metadata, epoch, rawValue));
                } catch (NumberFormatException nfe) {
                    throw token.generateException(nfe);
                }
            }
        }

        return true;

    }

    /** Convert a measurement to SI units.
     * @param ruConverter converter for {@link RangeUnits#RU Range Units} (may be null)
     * @param metadata metadata corresponding to the observation
     * @param date observation date
     * @param rawValue measurement raw value
     * @return measurement in SI units
     */
    public double rawToSI(final RangeUnitsConverter ruConverter, final TdmMetadata metadata,
                          final AbsoluteDate date, final double rawValue) {
        return unit.toSI(rawValue);
    }

    /** Convert a measurement from SI units.
     * @param ruConverter converter for {@link RangeUnits#RU Range Units} (may be null)
     * @param metadata metadata corresponding to the observation
     * @param date observation date
     * @param siValue measurement value in SI units
     * @return measurement raw value
     */
    public double siToRaw(final RangeUnitsConverter ruConverter, final TdmMetadata metadata,
                          final AbsoluteDate date, final double siValue) {
        return unit.fromSI(siValue);
    }

}
