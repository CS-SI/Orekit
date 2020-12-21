/* Copyright 2002-2020 CS GROUP
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
package org.orekit.files.ccsds;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import org.hipparchus.exception.LocalizedCoreFormats;
import org.hipparchus.geometry.euclidean.threed.RotationOrder;
import org.orekit.errors.OrekitException;
import org.orekit.files.ccsds.AEMParser.AEMRotationOrder;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.sampling.OrekitFixedStepHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateTimeComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScale;
import org.orekit.utils.TimeStampedAngularCoordinates;

/**
 * A writer for AEM files.
 *
 * <p> Each instance corresponds to a single AEM file.
 *
 * <h2> Metadata </h2>
 *
 * <p> The AEM metadata used by this writer is described in the following table. Many
 * metadata items are optional or have default values so they do not need to be specified.
 * At a minimum the user must supply those values that are required and for which no
 * default exits: {@link Keyword#OBJECT_NAME}, and {@link Keyword#OBJECT_ID}. The usage
 * column in the table indicates where the metadata item is used, either in the AEM header
 * or in the metadata section at the start of an AEM attitude segment.
 *
 * <p> The AEM metadata for the whole AEM file is set in the {@link
 * #StreamingAemWriter(Appendable, TimeScale, Map) constructor}.
 *
 * <table>
 * <caption>AEM metadata</caption>
 *     <thead>
 *         <tr>
 *             <th>Keyword
 *             <th>Usage
 *             <th>Obligatory
 *             <th>Default
 *             <th>Reference
 *    </thead>
 *    <tbody>
 *        <tr>
 *            <td>{@link Keyword#CCSDS_AEM_VERS}
 *            <td>Header
 *            <td>Yes
 *            <td>{@link #CCSDS_AEM_VERS}
 *            <td>Table 4-2
 *        <tr>
 *            <td>{@link Keyword#COMMENT}
 *            <td>Header
 *            <td>No
 *            <td>
 *            <td>Table 4-2
 *        <tr>
 *            <td>{@link Keyword#CREATION_DATE}
 *            <td>Header
 *            <td>Yes
 *            <td>{@link Date#Date() Now}
 *            <td>Table 4-2
 *        <tr>
 *            <td>{@link Keyword#ORIGINATOR}
 *            <td>Header
 *            <td>Yes
 *            <td>{@link #DEFAULT_ORIGINATOR}
 *            <td>Table 4-2
 *        <tr>
 *            <td>{@link Keyword#OBJECT_NAME}
 *            <td>Segment
 *            <td>Yes
 *            <td>
 *            <td>Table 4-3
 *        <tr>
 *            <td>{@link Keyword#OBJECT_ID}
 *            <td>Segment
 *            <td>Yes
 *            <td>
 *            <td>Table 4-3
 *        <tr>
 *            <td>{@link Keyword#CENTER_NAME}
 *            <td>Segment
 *            <td>No
 *            <td>
 *            <td>Table 4-3
 *        <tr>
 *            <td>{@link Keyword#REF_FRAME_A}
 *            <td>Segment
 *            <td>Yes
 *            <td>
 *            <td>Table 4-3
 *        <tr>
 *            <td>{@link Keyword#REF_FRAME_B}
 *            <td>Segment
 *            <td>Yes
 *            <td>
 *            <td>Table 4-3
 *        <tr>
 *            <td>{@link Keyword#ATTITUDE_DIR}
 *            <td>Segment
 *            <td>Yes
 *            <td>
 *            <td>Table 4-3
 *        <tr>
 *            <td>{@link Keyword#TIME_SYSTEM}
 *            <td>Segment
 *            <td>Yes
 *            <td>
 *            <td>Table 4-3, Annex A
 *        <tr>
 *            <td>{@link Keyword#START_TIME}
 *            <td>Segment
 *            <td>Yes
 *            <td>
 *            <td>Table 4-3
 *        <tr>
 *            <td>{@link Keyword#USEABLE_START_TIME}
 *            <td>Segment
 *            <td>No
 *            <td>
 *            <td>Table 4-3
 *        <tr>
 *            <td>{@link Keyword#STOP_TIME}
 *            <td>Segment
 *            <td>Yes
 *            <td>
 *            <td>Table 4-3
 *        <tr>
 *            <td>{@link Keyword#USEABLE_STOP_TIME}
 *            <td>Segment
 *            <td>No
 *            <td>
 *            <td>Table 4-3
 *        <tr>
 *            <td>{@link Keyword#ATTITUDE_TYPE}
 *            <td>Segment
 *            <td>Yes
 *            <td>
 *            <td>Table 4-3, 4-4
 *        <tr>
 *            <td>{@link Keyword#QUATERNION_TYPE}
 *            <td>Segment
 *            <td>No
 *            <td>
 *            <td>Table 4-3, 4-4
 *        <tr>
 *            <td>{@link Keyword#EULER_ROT_SEQ}
 *            <td>Segment
 *            <td>No
 *            <td>
 *            <td>Table 4-3
 *        <tr>
 *            <td>{@link Keyword#RATE_FRAME}
 *            <td>Segment
 *            <td>No
 *            <td>
 *            <td>Table 4-3
 *        <tr>
 *            <td>{@link Keyword#INTERPOLATION_METHOD}
 *            <td>Segment
 *            <td>No
 *            <td>
 *            <td>Table 4-3
 *        <tr>
 *            <td>{@link Keyword#INTERPOLATION_DEGREE}
 *            <td>Segment
 *            <td>No
 *            <td>
 *            <td>Table 4-3
 *    </tbody>
 *</table>
 *
 * <p> The {@link Keyword#TIME_SYSTEM} must be constant for the whole file and is used
 * to interpret all dates except {@link Keyword#CREATION_DATE}. The guessing algorithm
 * is not guaranteed to work so it is recommended to provide values for {@link
 * Keyword#CENTER_NAME} and {@link Keyword#TIME_SYSTEM} to avoid any bugs associated with
 * incorrect guesses.
 *
 * <p> Standardized values for {@link Keyword#TIME_SYSTEM} are GMST, GPS, MET, MRT, SCLK,
 * TAI, TCB, TDB, TT, UT1, and UTC. Standardized values for reference frames
 * are EME2000, GTOD, ICRF, ITRF2000, ITRF-93, ITRF-97, LVLH, RTN, QSW, TOD, TNW, NTW and RSW.
 * Additionally ITRF followed by a four digit year may be used.
 *
 * @author Bryan Cazabonne
 * @see <a href="https://public.ccsds.org/Pubs/504x0b1c1.pdf">CCSDS 504.0-B-1 Attitude Data Messages</a>
 * @see AEMWriter
 * @since 10.2
 */
public class StreamingAemWriter {

    /** Version number implemented. **/
    public static final String CCSDS_AEM_VERS = "1.0";

    /** Default value for {@link Keyword#ORIGINATOR}. */
    public static final String DEFAULT_ORIGINATOR = "OREKIT";

    /**
     * Default format used for attitude ephemeris data output: 5 digits
     * after the decimal point and leading space for positive values.
     */
    public static final String DEFAULT_ATTITUDE_FORMAT = "% .5f";

    /** New line separator for output file. See 5.4.5. */
    private static final String NEW_LINE = "\n";

    /**
     * Standardized locale to use, to ensure files can be exchanged without
     * internationalization issues.
     */
    private static final Locale STANDARDIZED_LOCALE = Locale.US;

    /** String format used for all key/value pair lines. **/
    private static final String KV_FORMAT = "%s = %s%n";

    /** Output stream. */
    private final Appendable writer;

    /** Metadata for this AEM file. */
    private final Map<Keyword, String> metadata;

    /** Time scale for all dates except {@link Keyword#CREATION_DATE}. */
    private final TimeScale timeScale;

    /** Format for attitude ephemeris data output. */
    private final String attitudeFormat;

    /**
     * Create an AEM writer that streams data to the given output stream.
     * {@link #DEFAULT_ATTITUDE_FORMAT Default formatting} will be used for attitude ephemeris data.
     *
     * @param writer    The output stream for the AEM file. Most methods will append data
     *                  to this {@code writer}.
     * @param timeScale for all times in the AEM except {@link Keyword#CREATION_DATE}. See
     *                  Section 4.2.5.4.2 and Annex A.
     * @param metadata  for the satellite.
     */
    public StreamingAemWriter(final Appendable writer,
                              final TimeScale timeScale,
                              final Map<Keyword, String> metadata) {
        this(writer, timeScale, metadata, DEFAULT_ATTITUDE_FORMAT);
    }

    /**
     * Create an AEM writer than streams data to the given output stream as
     * {@link #StreamingAemWriter(Appendable, TimeScale, Map)} with
     * {@link java.util.Formatter format parameters} for attitude ephemeris data.
     *
     * @param writer    The output stream for the AEM file. Most methods will append data
     *                  to this {@code writer}.
     * @param timeScale for all times in the AEM except {@link Keyword#CREATION_DATE}. See
     *                  Section 4.2.5.4.2 and Annex A.
     * @param metadata  for the satellite.
     * @param attitudeFormat format parameters for attitude ephemeris data output.
     * @since 10.3
     */
    public StreamingAemWriter(final Appendable writer,
                              final TimeScale timeScale,
                              final Map<Keyword, String> metadata,
                              final String attitudeFormat) {
        this.writer    = writer;
        this.timeScale = timeScale;
        this.metadata  = new LinkedHashMap<>(metadata);

        // Set default metadata
        this.metadata.putIfAbsent(Keyword.CCSDS_AEM_VERS, CCSDS_AEM_VERS);

        // creation date is informational only
        this.metadata.putIfAbsent(Keyword.CREATION_DATE,
                ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT));
        this.metadata.putIfAbsent(Keyword.ORIGINATOR, DEFAULT_ORIGINATOR);
        this.metadata.putIfAbsent(Keyword.TIME_SYSTEM, timeScale.getName());
        this.attitudeFormat = attitudeFormat;
    }

    /**
     * Write a single key and value to the stream using Key Value Notation (KVN).
     * @param key   the keyword to write
     * @param value the value to write
     * @throws IOException if an I/O error occurs.
     */
    private void writeKeyValue(final Keyword key, final String value) throws IOException {
        writer.append(String.format(STANDARDIZED_LOCALE, KV_FORMAT, key.toString(), value));
    }

    /**
     * Writes the standard AEM header for the file.
     * @throws IOException if the stream cannot write to stream
     */
    public void writeHeader() throws IOException {
        writeKeyValue(Keyword.CCSDS_AEM_VERS, this.metadata.get(Keyword.CCSDS_AEM_VERS));
        final String comment = this.metadata.get(Keyword.COMMENT);
        if (comment != null) {
            writeKeyValue(Keyword.COMMENT, comment);
        }
        writeKeyValue(Keyword.CREATION_DATE, this.metadata.get(Keyword.CREATION_DATE));
        writeKeyValue(Keyword.ORIGINATOR, this.metadata.get(Keyword.ORIGINATOR));
        writer.append(NEW_LINE);
    }

    /**
     * Create a writer for a new AEM attitude ephemeris segment.
     * <p> The returned writer can only write a single attitude ephemeris segment in an AEM.
     * This method must be called to create a writer for each attitude ephemeris segment.
     * @param segmentMetadata the metadata to use for the segment. Overrides for this
     *                        segment any other source of meta data values. See {@link
     *                        #StreamingAemWriter} for a description of which metadata are
     *                        required and how they are determined.
     * @return a new AEM segment, ready for writing.
     */
    public AEMSegment newSegment(final Map<Keyword, String> segmentMetadata) {
        final Map<Keyword, String> meta = new LinkedHashMap<>(this.metadata);
        meta.putAll(segmentMetadata);
        return new AEMSegment(meta);
    }

    /**
     * Convert a date to a string with more precision.
     *
     * @param components to convert to a String.
     * @return the String form of {@code date} with at least 9 digits of precision.
     */
    static String dateToString(final DateTimeComponents components) {
        final TimeComponents time = components.getTime();
        final int hour = time.getHour();
        final int minute = time.getMinute();
        final double second = time.getSecond();
        // Decimal formatting classes could be static final if they were thread safe.
        final DecimalFormatSymbols locale = new DecimalFormatSymbols(STANDARDIZED_LOCALE);
        final DecimalFormat twoDigits = new DecimalFormat("00", locale);
        final DecimalFormat precise = new DecimalFormat("00.0########", locale);
        return components.getDate().toString() + "T" + twoDigits.format(hour) + ":" +
                twoDigits.format(minute) + ":" + precise.format(second);
    }

    /** A writer for a segment of an AEM. */
    public class AEMSegment implements OrekitFixedStepHandler {

        /** Metadata for this AEM Segment. */
        private final Map<Keyword, String> metadata;

        /**
         * Create a new segment writer.
         * @param metadata to use when writing this segment.
         */
        private AEMSegment(final Map<Keyword, String> metadata) {
            this.metadata = metadata;
        }

        /**
         * Write the ephemeris segment metadata.
         *
         * <p> See {@link StreamingAemWriter} for a description of how the metadata is
         * set.
         *
         * @throws IOException if the output stream throws one while writing.
         */
        public void writeMetadata() throws IOException {
            // Start metadata
            writer.append("META_START").append(NEW_LINE);

            // Table 4.3
            writeKeyValue(Keyword.OBJECT_NAME,  this.metadata.get(Keyword.OBJECT_NAME));
            writeKeyValue(Keyword.OBJECT_ID,    this.metadata.get(Keyword.OBJECT_ID));
            writeKeyValue(Keyword.CENTER_NAME,  this.metadata.get(Keyword.CENTER_NAME));
            writeKeyValue(Keyword.REF_FRAME_A,  this.metadata.get(Keyword.REF_FRAME_A));
            writeKeyValue(Keyword.REF_FRAME_B,  this.metadata.get(Keyword.REF_FRAME_B));
            writeKeyValue(Keyword.ATTITUDE_DIR, this.metadata.get(Keyword.ATTITUDE_DIR));
            writeKeyValue(Keyword.TIME_SYSTEM,  this.metadata.get(Keyword.TIME_SYSTEM));
            writeKeyValue(Keyword.START_TIME,   this.metadata.get(Keyword.START_TIME));

            // Optional values: USEABLE_START_TIME & USEABLE_STOP_TIME
            final String usableStartTime = this.metadata.get(Keyword.USEABLE_START_TIME);
            if (usableStartTime != null) {
                writeKeyValue(Keyword.USEABLE_START_TIME, usableStartTime);
            }
            final String usableStopTime = this.metadata.get(Keyword.USEABLE_STOP_TIME);
            if (usableStopTime != null) {
                writeKeyValue(Keyword.USEABLE_STOP_TIME, usableStopTime);
            }

            // Table 4.3
            writeKeyValue(Keyword.STOP_TIME,     this.metadata.get(Keyword.STOP_TIME));
            writeKeyValue(Keyword.ATTITUDE_TYPE, this.metadata.get(Keyword.ATTITUDE_TYPE));

            // Optional values: QUATERNION_ TYPE; EULER_ROT_SEQ; RATE_FRAME; INTERPOLATION_METHOD and INTERPOLATION_DEGREE
            final String quaternionType = this.metadata.get(Keyword.QUATERNION_TYPE);
            if (quaternionType != null) {
                writeKeyValue(Keyword.QUATERNION_TYPE, quaternionType);
            }
            final String eulerRotSeq = this.metadata.get(Keyword.EULER_ROT_SEQ);
            if (eulerRotSeq != null) {
                writeKeyValue(Keyword.EULER_ROT_SEQ, eulerRotSeq);
            }
            final String rateFrame = this.metadata.get(Keyword.RATE_FRAME);
            if (rateFrame != null) {
                writeKeyValue(Keyword.RATE_FRAME, rateFrame);
            }
            final String interpolationMethod = this.metadata.get(Keyword.INTERPOLATION_METHOD);
            if (interpolationMethod != null) {
                writeKeyValue(Keyword.INTERPOLATION_METHOD, interpolationMethod);
            }
            final String interpolationDegree = this.metadata.get(Keyword.INTERPOLATION_DEGREE);
            if (interpolationDegree != null) {
                writeKeyValue(Keyword.INTERPOLATION_DEGREE, interpolationDegree);
            }

            // Stop metadata
            writer.append("META_STOP").append(NEW_LINE).append(NEW_LINE);
        }

        /**
         * Write a single attitude ephemeris line according to section 4.2.4 and Table 4-4.
         * @param attitude the attitude information for a given date.
         * @param isFirst true if QC is the first element in the attitude data
         * @param attitudeName name of the attitude type
         * @param rotationOrder rotation order
         * @throws IOException if the output stream throws one while writing.
         */
        public void writeAttitudeEphemerisLine(final TimeStampedAngularCoordinates attitude,
                                               final boolean isFirst,
                                               final String attitudeName,
                                               final RotationOrder rotationOrder)
            throws IOException {
            // Epoch
            final String epoch = dateToString(attitude.getDate().getComponents(timeScale));
            writer.append(epoch).append(" ");
            // Attitude data in degrees
            final AEMAttitudeType type = AEMAttitudeType.getAttitudeType(attitudeName);
            final double[]        data = type.getAttitudeData(attitude, isFirst, rotationOrder);
            final int             size = data.length;
            for (int index = 0; index < size; index++) {
                writer.append(String.format(STANDARDIZED_LOCALE, attitudeFormat, data[index]));
                final String space = (index == size - 1) ? "" : " ";
                writer.append(space);
            }
            // end the line
            writer.append(NEW_LINE);
        }

        /**
         * {@inheritDoc}
         *
         * <p> Sets the {@link Keyword#START_TIME} and {@link Keyword#STOP_TIME} in this
         * segment's metadata if not already set by the user. Then calls {@link
         * #writeMetadata()} to start the segment.
         */
        @Override
        public void init(final SpacecraftState s0,
                         final AbsoluteDate t,
                         final double step) {
            try {
                final String start = dateToString(s0.getDate().getComponents(timeScale));
                final String stop = dateToString(t.getComponents(timeScale));
                this.metadata.putIfAbsent(Keyword.START_TIME, start);
                this.metadata.putIfAbsent(Keyword.STOP_TIME, stop);
                this.writeMetadata();
            } catch (IOException e) {
                throw new OrekitException(e, LocalizedCoreFormats.SIMPLE_MESSAGE,
                        e.getLocalizedMessage());
            }
        }

        /** {@inheritDoc}. */
        @Override
        public void handleStep(final SpacecraftState currentState, final boolean isLast) {
            try {

                // Quaternion type
                final String quaternionType = this.metadata.get(Keyword.QUATERNION_TYPE);
                // If the QUATERNION_TYPE keyword is not present in the file, this means that
                // the attitude data are not given using quaternion. Therefore, the computation
                // of the attitude data will not be sensitive to this parameter. A default value
                // can be set
                boolean isFirst = false;
                if (quaternionType != null) {
                    isFirst = (quaternionType.equals("FIRST")) ? true : false;
                }

                // Attitude type
                final String attitudeType = this.metadata.get(Keyword.ATTITUDE_TYPE);

                // Rotation order
                final String eulerRotSeq = this.metadata.get(Keyword.EULER_ROT_SEQ);
                final RotationOrder order = (eulerRotSeq == null) ? null : AEMRotationOrder.getRotationOrder(eulerRotSeq);

                // Write attitude ephemeris data
                writeAttitudeEphemerisLine(currentState.getAttitude().getOrientation(), isFirst,
                                           attitudeType, order);

            } catch (IOException e) {
                throw new OrekitException(e, LocalizedCoreFormats.SIMPLE_MESSAGE,
                        e.getLocalizedMessage());
            }

        }

        /**
         * Start of an attitude block.
         * @throws IOException if the output stream throws one while writing.
         */
        void startAttitudeBlock() throws IOException {
            writer.append("DATA_START").append(NEW_LINE);
        }

        /**
         * End of an attitude block.
         * @throws IOException if the output stream throws one while writing.
         */
        void endAttitudeBlock() throws IOException {
            writer.append("DATA_STOP").append(NEW_LINE);
        }

    }

}
