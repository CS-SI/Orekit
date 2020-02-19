/* Contributed in the public domain.
 * Licensed to CS Group (CS) under one or more
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
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.hipparchus.exception.LocalizedCoreFormats;
import org.hipparchus.linear.RealMatrix;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.OEMFile.CovarianceMatrix;
import org.orekit.frames.FactoryManagedFrame;
import org.orekit.frames.Frame;
import org.orekit.frames.LOFType;
import org.orekit.frames.Predefined;
import org.orekit.frames.VersionedITRF;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.sampling.OrekitFixedStepHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateTimeComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScale;
import org.orekit.utils.TimeStampedPVCoordinates;

/**
 * A writer for OEM files.
 *
 * <p> Each instance corresponds to a single OEM file. A new OEM ephemeris segment is
 * started by calling {@link #newSegment(Frame, Map)}.
 *
 * <h3> Metadata </h3>
 *
 * <p> The OEM metadata used by this writer is described in the following table. Many
 * metadata items are optional or have default values so they do not need to be specified.
 * At a minimum the user must supply those values that are required and for which no
 * default exits: {@link Keyword#OBJECT_NAME}, and {@link Keyword#OBJECT_ID}. The usage
 * column in the table indicates where the metadata item is used, either in the OEM header
 * or in the metadata section at the start of an OEM ephemeris segment.
 *
 * <p> The OEM metadata for the whole OEM file is set in the {@link
 * #StreamingOemWriter(Appendable, TimeScale, Map) constructor}. Any of the metadata may
 * be overridden for a particular segment using the {@code metadata} argument to {@link
 * #newSegment(Frame, Map)}.
 *
 * <table summary="OEM metada">
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
 *            <td>{@link Keyword#CCSDS_OEM_VERS}
 *            <td>Header
 *            <td>Yes
 *            <td>{@link #CCSDS_OEM_VERS}
 *            <td>Table 5-2
 *        <tr>
 *            <td>{@link Keyword#COMMENT}
 *            <td>Header
 *            <td>No
 *            <td>
 *            <td>Table 5-2
 *        <tr>
 *            <td>{@link Keyword#CREATION_DATE}
 *            <td>Header
 *            <td>Yes
 *            <td>{@link Date#Date() Now}
 *            <td>Table 5.2, 6.5.9
 *        <tr>
 *            <td>{@link Keyword#ORIGINATOR}
 *            <td>Header
 *            <td>Yes
 *            <td>{@link #DEFAULT_ORIGINATOR}
 *            <td>Table 5-2
 *        <tr>
 *            <td>{@link Keyword#OBJECT_NAME}
 *            <td>Segment
 *            <td>Yes
 *            <td>
 *            <td>Table 5-3
 *        <tr>
 *            <td>{@link Keyword#OBJECT_ID}
 *            <td>Segment
 *            <td>Yes
 *            <td>
 *            <td>Table 5-3
 *        <tr>
 *            <td>{@link Keyword#CENTER_NAME}
 *            <td>Segment
 *            <td>Yes
 *            <td>Guessed from the {@link #newSegment(Frame, Map) segment}'s {@code frame}
 *            <td>Table 5-3
 *        <tr>
 *            <td>{@link Keyword#REF_FRAME}
 *            <td>Segment
 *            <td>Yes
 *            <td>Guessed from the {@link #newSegment(Frame, Map) segment}'s {@code frame}
 *            <td>Table 5-3, Annex A
 *        <tr>
 *            <td>{@link Keyword#REF_FRAME_EPOCH}
 *            <td>Segment
 *            <td>No
 *            <td>
 *            <td>Table 5-3, 6.5.9
 *        <tr>
 *            <td>{@link Keyword#TIME_SYSTEM}
 *            <td>Segment
 *            <td>Yes
 *            <td>Guessed from {@code timeScale} set in the
 *                {@link #StreamingOemWriter(Appendable, TimeScale, Map) constructor}.
 *            <td>Table 5-3, Annex A
 *        <tr>
 *            <td>{@link Keyword#START_TIME}
 *            <td>Segment
 *            <td>Yes
 *            <td>Date of initial state in {@link Segment#init(SpacecraftState,
 *                AbsoluteDate, double) Segment.init(...)}
 *            <td>Table 5-3, 6.5.9
 *        <tr>
 *            <td>{@link Keyword#USEABLE_START_TIME}
 *            <td>Segment
 *            <td>No
 *            <td>
 *            <td>Table 5-3, 6.5.9
 *        <tr>
 *            <td>{@link Keyword#STOP_TIME}
 *            <td>Segment
 *            <td>Yes
 *            <td>Target date in {@link Segment#init(SpacecraftState,
 *                AbsoluteDate, double) Segment.init(...)}
 *            <td>Table 5-3, 6.5.9
 *        <tr>
 *            <td>{@link Keyword#USEABLE_STOP_TIME}
 *            <td>Segment
 *            <td>No
 *            <td>
 *            <td>Table 5-3, 6.5.9
 *        <tr>
 *            <td>{@link Keyword#INTERPOLATION}
 *            <td>Segment
 *            <td>No
 *            <td>
 *            <td>Table 5-3
 *        <tr>
 *            <td>{@link Keyword#INTERPOLATION_DEGREE}
 *            <td>Segment
 *            <td>No
 *            <td>
 *            <td>Table 5-3
 *    </tbody>
 *</table>
 *
 * <p> The {@link Keyword#TIME_SYSTEM} must be constant for the whole file and is used
 * to interpret all dates except {@link Keyword#CREATION_DATE}. The guessing algorithm
 * is not guaranteed to work so it is recommended to provide values for {@link
 * Keyword#CENTER_NAME}, {@link Keyword#REF_FRAME}, and {@link Keyword#TIME_SYSTEM} to
 * avoid any bugs associated with incorrect guesses.
 *
 * <p> Standardized values for {@link Keyword#TIME_SYSTEM} are GMST, GPS, ME, MRT, SCLK,
 * TAI, TCB, TDB, TCG, TT, UT1, and UTC. Standardized values for {@link Keyword#REF_FRAME}
 * are EME2000, GCRF, GRC, ICRF, ITRF2000, ITRF-93, ITRF-97, MCI, TDR, TEME, and TOD.
 * Additionally ITRF followed by a four digit year may be used.
 *
 * <h3> Examples </h3>
 *
 * <p> This class can be used as a step handler for a {@link Propagator}, or on its own.
 * Either way the object name and ID must be specified. The following example shows its
 * use as a step handler.
 *
 * <pre>{@code
 * Propagator propagator = ...; // pre-configured propagator
 * Appendable out = ...; // set-up output stream
 * Map<Keyword, String> metadata = new LinkedHashMap<>();
 * metadata.put(Keyword.OBJECT_NAME, "Vanguard");
 * metadata.put(Keyword.OBJECT_ID, "1958-002B");
 * StreamingOemWriter writer = new StreamingOemWriter(out, utc, metadata);
 * writer.writeHeader();
 * Segment segment = writer.newSegment(frame, Collections.emptyMap());
 * propagator.setMasterMode(step, segment);
 * propagator.propagate(startDate, stopDate);
 * }</pre>
 *
 * Alternatively a collection of state vectors can be written without the use of a
 * Propagator. In this case the {@link Keyword#START_TIME} and {@link Keyword#STOP_TIME}
 * need to be specified as part of the metadata.
 *
 * <pre>{@code
 * List<TimeStampedPVCoordinates> states = ...; // pre-generated states
 * Appendable out = ...; // set-up output stream
 * Map<Keyword, String> metadata = new LinkedHashMap<>();
 * metadata.put(Keyword.OBJECT_NAME, "Vanguard");
 * metadata.put(Keyword.OBJECT_ID, "1958-002B");
 * StreamingOemWriter writer = new StreamingOemWriter(out, utc, metadata);
 * writer.writeHeader();
 * // manually set start and stop times for this segment
 * Map<Keyword, String> segmentData = new LinkedHashMap<>();
 * segmentData.put(Keyword.START_TIME, start.toString());
 * segmentData.put(Keyword.STOP_TIME, stop.toString());
 * Segment segment = writer.newSegment(frame, segmentData);
 * segment.writeMetadata(); // output metadata block
 * for (TimeStampedPVCoordinates state : states) {
 *     segment.writeEphemerisLine(state);
 * }
 * }</pre>
 *
 * @author Evan Ward
 * @see <a href="https://public.ccsds.org/Pubs/502x0b2c1.pdf">CCSDS 502.0-B-2 Orbit Data
 *      Messages</a>
 * @see <a href="https://public.ccsds.org/Pubs/500x0g3.pdf">CCSDS 500.0-G-3 Navigation
 *      Data Definitions and Conventions</a>
 * @see OEMWriter
 */
public class StreamingOemWriter {

    /** Version number implemented. **/
    public static final String CCSDS_OEM_VERS = "2.0";
    /** Default value for {@link Keyword#ORIGINATOR}. */
    public static final String DEFAULT_ORIGINATOR = "OREKIT";

    /** New line separator for output file. See 6.3.6. */
    private static final String NEW_LINE = "\n";
    /**
     * Standardized locale to use, to ensure files can be exchanged without
     * internationalization issues.
     */
    private static final Locale STANDARDIZED_LOCALE = Locale.US;
    /** String format used for all key/value pair lines. **/
    private static final String KV_FORMAT = "%s = %s%n";
    /** Factor for converting meters to km. */
    private static final double M_TO_KM = 1e-3;
    /** Suffix of the name of the inertial frame attached to a planet. */
    private static final String INERTIAL_FRAME_SUFFIX = "/inertial";

    /** Output stream. */
    private final Appendable writer;
    /** Metadata for this OEM file. */
    private final Map<Keyword, String> metadata;
    /** Time scale for all dates except {@link Keyword#CREATION_DATE}. */
    private final TimeScale timeScale;

    /**
     * Create an OEM writer than streams data to the given output stream.
     *
     * @param writer    The output stream for the OEM file. Most methods will append data
     *                  to this {@code writer}.
     * @param timeScale for all times in the OEM except {@link Keyword#CREATION_DATE}. See
     *                  Section 5.2.4.5 and Annex A.
     * @param metadata  for the satellite. Can be overridden in {@link #newSegment(Frame,
     *                  Map)} for a specific segment. See {@link StreamingOemWriter}.
     */
    public StreamingOemWriter(final Appendable writer,
                              final TimeScale timeScale,
                              final Map<Keyword, String> metadata) {

        this.writer = writer;
        this.timeScale = timeScale;
        this.metadata = new LinkedHashMap<>(metadata);
        // set default metadata
        this.metadata.putIfAbsent(Keyword.CCSDS_OEM_VERS, CCSDS_OEM_VERS);
        // creation date is informational only
        this.metadata.putIfAbsent(Keyword.CREATION_DATE,
                ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT));
        this.metadata.putIfAbsent(Keyword.ORIGINATOR, DEFAULT_ORIGINATOR);
        this.metadata.putIfAbsent(Keyword.TIME_SYSTEM, timeScale.getName());
    }

    /**
     * Guesses names from Table 5-3 and Annex A.
     *
     * <p> The goal of this method is to perform the opposite mapping of {@link
     * CCSDSFrame}.
     *
     * @param frame a reference frame for ephemeris output.
     * @return the string to use in the OEM file to identify {@code frame}.
     */
    static String guessFrame(final Frame frame) {
        // define some constant strings to make checkstyle happy
        final String tod = "TOD";
        final String itrf = "ITRF";
        // Try to determine the CCSDS name from Annex A by examining the Orekit name.
        final String name = frame.getName();
        if (Arrays.stream(CCSDSFrame.values())
                .map(CCSDSFrame::name)
                .anyMatch(name::equals)) {
            // should handle J2000, GCRF, TEME, and some frames created by OEMParser.
            return name;
        } else if (frame instanceof CcsdsModifiedFrame) {
            return ((CcsdsModifiedFrame) frame).getRefFrame();
        } else if ((CelestialBodyFactory.MARS + INERTIAL_FRAME_SUFFIX).equals(name)) {
            return "MCI";
        } else if ((CelestialBodyFactory.SOLAR_SYSTEM_BARYCENTER + INERTIAL_FRAME_SUFFIX)
                .equals(name)) {
            return "ICRF";
        } else if (name.contains("GTOD")) {
            return "TDR";
        } else if (name.contains(tod)) { // check after GTOD
            return tod;
        } else if (name.contains("Equinox") && name.contains(itrf)) {
            return "GRC";
        } else if (frame instanceof VersionedITRF) {
            return ((VersionedITRF) frame).getITRFVersion().getName().replace("-", "");
        } else if (name.contains("CIO") && name.contains(itrf)) {
            return "ITRF2014";
        } else {
            // don't know how to map it to a CCSDS reference frame
            return name;
        }
    }

    /**
     * Guess the name of the center of the reference frame.
     *
     * @param frame a reference frame for ephemeris output.
     * @return the string to use in the OEM file to describe the origin of {@code frame}.
     */
    static String guessCenter(final Frame frame) {
        final String name = frame.getName();
        if (name.endsWith(INERTIAL_FRAME_SUFFIX) || name.endsWith("/rotating")) {
            return name.substring(0, name.length() - 9).toUpperCase(STANDARDIZED_LOCALE);
        } else if (frame instanceof CcsdsModifiedFrame) {
            return ((CcsdsModifiedFrame) frame).getCenterName();
        } else if (frame.getName().equals(Predefined.ICRF.getName())) {
            return CelestialBodyFactory.SOLAR_SYSTEM_BARYCENTER.toUpperCase(STANDARDIZED_LOCALE);
        } else if (frame.getDepth() == 0 || frame instanceof FactoryManagedFrame) {
            return "EARTH";
        } else {
            return "UNKNOWN";
        }
    }

    /**
     * Write a single key and value to the stream using Key Value Notation (KVN).
     *
     * @param key   the keyword to write
     * @param value the value to write
     * @throws IOException if an I/O error occurs.
     */
    private void writeKeyValue(final Keyword key, final String value) throws IOException {
        writer.append(String.format(STANDARDIZED_LOCALE, KV_FORMAT, key.toString(), value));
    }

    /**
     * Writes the standard OEM header for the file.
     *
     * @throws IOException if the stream cannot write to stream
     */
    public void writeHeader() throws IOException {
        writeKeyValue(Keyword.CCSDS_OEM_VERS, this.metadata.get(Keyword.CCSDS_OEM_VERS));
        final String comment = this.metadata.get(Keyword.COMMENT);
        if (comment != null) {
            writeKeyValue(Keyword.COMMENT, comment);
        }
        writeKeyValue(Keyword.CREATION_DATE, this.metadata.get(Keyword.CREATION_DATE));
        writeKeyValue(Keyword.ORIGINATOR, this.metadata.get(Keyword.ORIGINATOR));
        writer.append(NEW_LINE);
    }

    /**
     * Create a writer for a new OEM ephemeris segment.
     *
     * <p> The returned writer can only write a single ephemeris segment in an OEM. This
     * method must be called to create a writer for each ephemeris segment.
     *
     * @param frame           the reference frame to use for the segment. If this value is
     *                        {@code null} then {@link Segment#handleStep(SpacecraftState,
     *                        boolean)} will throw a {@link NullPointerException} and the
     *                        metadata item {@link Keyword#REF_FRAME} must be specified in
     *                        the metadata.
     * @param segmentMetadata the metadata to use for the segment. Overrides for this
     *                        segment any other source of meta data values. See {@link
     *                        #StreamingOemWriter} for a description of which metadata are
     *                        required and how they are determined.
     * @return a new OEM segment, ready for writing.
     */
    public Segment newSegment(final Frame frame,
                              final Map<Keyword, String> segmentMetadata) {
        final Map<Keyword, String> meta = new LinkedHashMap<>(this.metadata);
        meta.putAll(segmentMetadata);
        if (!meta.containsKey(Keyword.REF_FRAME)) {
            meta.put(Keyword.REF_FRAME, guessFrame(frame));
        }
        if (!meta.containsKey(Keyword.CENTER_NAME)) {
            meta.put(Keyword.CENTER_NAME, guessCenter(frame));
        }
        return new Segment(frame, meta);
    }

    /** A writer for a segment of an OEM. */
    public class Segment implements OrekitFixedStepHandler {

        /** Reference frame of the output states. */
        private final Frame frame;
        /** Metadata for this OEM Segment. */
        private final Map<Keyword, String> metadata;

        /**
         * Create a new segment writer.
         *
         * @param frame    for the output states. Used by {@link #handleStep(SpacecraftState,
         *                 boolean)}.
         * @param metadata to use when writing this segment.
         */
        private Segment(final Frame frame, final Map<Keyword, String> metadata) {
            this.frame = frame;
            this.metadata = metadata;
        }

        /**
         * Write the ephemeris segment metadata.
         *
         * <p> See {@link StreamingOemWriter} for a description of how the metadata is
         * set.
         *
         * @throws IOException if the output stream throws one while writing.
         */
        public void writeMetadata() throws IOException {
            writer.append("META_START").append(NEW_LINE);
            if (this.frame != null) {
                writer.append("COMMENT ").append("Orekit frame: ")
                        .append(this.frame.toString()).append(NEW_LINE);
            }
            // Table 5.3
            writeKeyValue(Keyword.OBJECT_NAME, this.metadata.get(Keyword.OBJECT_NAME));
            writeKeyValue(Keyword.OBJECT_ID, this.metadata.get(Keyword.OBJECT_ID));
            writeKeyValue(Keyword.CENTER_NAME, this.metadata.get(Keyword.CENTER_NAME));
            writeKeyValue(Keyword.REF_FRAME, this.metadata.get(Keyword.REF_FRAME));
            final String refFrameEpoch = this.metadata.get(Keyword.REF_FRAME_EPOCH);
            if (refFrameEpoch != null) {
                writeKeyValue(Keyword.REF_FRAME_EPOCH, refFrameEpoch);
            }
            writeKeyValue(Keyword.TIME_SYSTEM, this.metadata.get(Keyword.TIME_SYSTEM));
            writeKeyValue(Keyword.START_TIME, this.metadata.get(Keyword.START_TIME));
            final String usableStartTime = this.metadata.get(Keyword.USEABLE_START_TIME);
            if (usableStartTime != null) {
                writeKeyValue(Keyword.USEABLE_START_TIME, usableStartTime);
            }
            writeKeyValue(Keyword.STOP_TIME, this.metadata.get(Keyword.STOP_TIME));
            final String usableStopTime = this.metadata.get(Keyword.USEABLE_STOP_TIME);
            if (usableStopTime != null) {
                writeKeyValue(Keyword.USEABLE_STOP_TIME, usableStopTime);
            }
            final String interpolation = this.metadata.get(Keyword.INTERPOLATION);
            if (interpolation != null) {
                writeKeyValue(Keyword.INTERPOLATION, interpolation);
            }
            final String interpolationDegree =
                    this.metadata.get(Keyword.INTERPOLATION_DEGREE);
            if (interpolationDegree != null) {
                writeKeyValue(Keyword.INTERPOLATION_DEGREE, interpolationDegree);
            }
            writer.append("META_STOP").append(NEW_LINE).append(NEW_LINE);
        }

        /**
         * Write a single ephemeris line according to section 5.2.4. This method does not
         * write the optional acceleration terms.
         *
         * @param pv the time, position, and velocity to write.
         * @throws IOException if the output stream throws one while writing.
         */
        public void writeEphemerisLine(final TimeStampedPVCoordinates pv)
                throws IOException {
            final String epoch = dateToString(pv.getDate().getComponents(timeScale));
            writer.append(epoch).append(" ");
            // output in km, see Section 6.6.2.1
            writer.append(Double.toString(pv.getPosition().getX() * M_TO_KM)).append(" ");
            writer.append(Double.toString(pv.getPosition().getY() * M_TO_KM)).append(" ");
            writer.append(Double.toString(pv.getPosition().getZ() * M_TO_KM)).append(" ");
            writer.append(Double.toString(pv.getVelocity().getX() * M_TO_KM)).append(" ");
            writer.append(Double.toString(pv.getVelocity().getY() * M_TO_KM)).append(" ");
            writer.append(Double.toString(pv.getVelocity().getZ() * M_TO_KM));
            writer.append(NEW_LINE);
        }

        /**
         * Write covariance matrices of the segment according to section 5.2.5.
         *
         * @param covarianceMatrices the list of covariance matrices related to the segment.
         * @throws IOException if the output stream throws one while writing.
         */
        public void writeCovarianceMatrices(final List<CovarianceMatrix> covarianceMatrices)
                throws IOException {
            writer.append("COVARIANCE_START").append(NEW_LINE);
            // Sort to ensure having the matrices in chronological order when
            // they are in the same data section (see section 5.2.5.7)
            Collections.sort(covarianceMatrices, (mat1, mat2)->mat1.getEpoch().compareTo(mat2.getEpoch()));
            for (final CovarianceMatrix covarianceMatrix : covarianceMatrices) {
                final String epoch = dateToString(covarianceMatrix.getEpoch().getComponents(timeScale));
                writeKeyValue(Keyword.EPOCH, epoch);

                if (covarianceMatrix.getFrame() != null ) {
                    writeKeyValue(Keyword.COV_REF_FRAME, guessFrame(covarianceMatrix.getFrame()));
                } else if (covarianceMatrix.getLofType() != null) {
                    if (covarianceMatrix.getLofType() == LOFType.QSW) {
                        writeKeyValue(Keyword.COV_REF_FRAME, "RTN");
                    } else if (covarianceMatrix.getLofType() == LOFType.TNW) {
                        writeKeyValue(Keyword.COV_REF_FRAME, covarianceMatrix.getLofType().name());
                    } else {
                        throw new OrekitException(OrekitMessages.CCSDS_INVALID_FRAME, toString());
                    }
                }

                final RealMatrix covRealMatrix = covarianceMatrix.getMatrix();
                for (int i = 0; i < covRealMatrix.getRowDimension(); i++) {
                    writer.append(Double.toString(covRealMatrix.getEntry(i, 0)));
                    for (int j = 1; j < i + 1; j++) {
                        writer.append(" ").append(Double.toString(covRealMatrix.getEntry(i, j)));
                    }
                    writer.append(NEW_LINE);
                }
            }
            writer.append("COVARIANCE_STOP").append(NEW_LINE).append(NEW_LINE);
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

        @Override
        public void handleStep(final SpacecraftState s,
                               final boolean isLast) {
            try {
                writeEphemerisLine(s.getPVCoordinates(this.frame));
            } catch (IOException e) {
                throw new OrekitException(e, LocalizedCoreFormats.SIMPLE_MESSAGE,
                        e.getLocalizedMessage());
            }

        }

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

}
