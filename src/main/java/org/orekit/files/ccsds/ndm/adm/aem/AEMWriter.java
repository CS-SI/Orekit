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
package org.orekit.files.ccsds.ndm.adm.aem;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Locale;

import org.orekit.data.DataContext;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.ndm.adm.ADMMetadataKey;
import org.orekit.files.ccsds.ndm.adm.AttitudeEndPoints;
import org.orekit.files.ccsds.section.CommentsContainer;
import org.orekit.files.ccsds.section.Header;
import org.orekit.files.ccsds.section.HeaderKey;
import org.orekit.files.ccsds.section.MetadataKey;
import org.orekit.files.ccsds.utils.CcsdsTimeScale;
import org.orekit.files.general.AttitudeEphemerisFile;
import org.orekit.files.general.AttitudeEphemerisFile.AttitudeEphemerisSegment;
import org.orekit.files.general.AttitudeEphemerisFile.SatelliteAttitudeEphemeris;
import org.orekit.files.general.AttitudeEphemerisFileWriter;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.DateTimeComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScale;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.TimeStampedAngularCoordinates;

/**
 * A writer for Attitude Ephemeris Messsage (AEM) files.
 * @author Bryan Cazabonne
 * @since 10.2
 */
public class AEMWriter implements AttitudeEphemerisFileWriter {

    /** Version number implemented. **/
    public static final double CCSDS_AEM_VERS = 1.0;

    /** Default value for {@link HeaderKey#ORIGINATOR}. */
    public static final String DEFAULT_ORIGINATOR = "OREKIT";

    /** Default value for {@link #TIME_SYSTEM}. */
    public static final CcsdsTimeScale DEFAULT_TIME_SYSTEM = CcsdsTimeScale.UTC;

    /** Default file name for error messages. */
    public static final String DEFAULT_FILE_NAME = "<AEM output>";

    /**
     * Default format used for attitude ephemeris data output: 9 digits
     * after the decimal point and leading space for positive values.
     */
    public static final String DEFAULT_ATTITUDE_FORMAT = "% .9f";

    /** New line separator for output file. See 5.4.5. */
    private static final char NEW_LINE = '\n';

    /**
     * Standardized locale to use, to ensure files can be exchanged without
     * internationalization issues.
     */
    private static final Locale STANDARDIZED_LOCALE = Locale.US;

    /** String format used for all key/value pair lines. **/
    private static final String KV_FORMAT = "%s = %s%n";

    /** String format used for dates. **/
    private static final String DATE_FORMAT = "%04d-%02d-%02dT%02d:%02d:%012.9f";

    /** String format used for all comment lines. **/
    private static final String COMMENT_FORMAT = "COMMENT %s%n";

    /** Marker for start of metadata section. */
    private static final String META_START = "META_START";

    /** Marker for stop of metadata section. */
    private static final String META_STOP = "META_STOP";

    /** Marker for start of data section. */
    private static final String DATA_START = "DATA_START";

    /** Marker for stop of data section. */
    private static final String DATA_STOP = "DATA_STOP";

    /** Constant for external frame to local frame attitude. */
    private static final String EXTERNAL_TO_LOCAL = "A2B";

    /** Constant for local frame to external frame attitude. */
    private static final String LOCAL_TO_EXTERNAL = "B2A";

    /** Constant for quaternions with scalar component in first position. */
    private static final String FIRST = "FIRST";

    /** Constant for quaternions with scalar component in last position. */
    private static final String LAST = "LAST";

    /** Constant for angular rates in external frame. */
    private static final String EXTERNAL_RATES = "REF_FRAME_A";

    /** Constant for angular rates in local frame. */
    private static final String LOCAL_RATES = "REF_FRAME_B";

    /** Data context used for obtain frames and time scales. */
    private final DataContext dataContext;

    /** File name for error messages. */
    private final String fileName;

    /** Format for attitude ephemeris data output. */
    private final String attitudeFormat;

    /** File header. */
    private final Header header;

    /** Current metadata. */
    private final AEMMetadata metadata;

    /** Time scale for all segments. */
    private final TimeScale timeScale;

    /**
     * Standard default constructor that creates a writer with default configurations
     * including {@link #DEFAULT_ATTITUDE_FORMAT Default formatting}
     * and {@link #DEFAULT_FILE_NAME default file name} for error messages.
     * <p>
     * If the mandatory header entries are not present (or if header is null),
     * built-in defaults will be used
     * </p>
     * <p>
     * The writer is built from the complete header and partial metadata. The template
     * metadata is used to initialize and independent internal copy, that will be updated
     * as new segments are written (with at least the segment start and stop will change,
     * but some other parts may change too). The {@code template} object itself is not changed.
     * </>
     * @param conventions IERS Conventions
     * @param dataContext used to retrieve frames, time scales, etc.
     * @param header file header
     * @param template template for metadata
     * @since 11.0
     */
    public AEMWriter(final IERSConventions conventions, final DataContext dataContext,
                     final Header header, final AEMMetadata template) {
        this(conventions, dataContext, header, template, DEFAULT_FILE_NAME, DEFAULT_ATTITUDE_FORMAT);
    }

    /**
     * Constructor used to create a new AEM writer configured with the necessary parameters
     * to successfully fill in all required fields that aren't part of a standard object
     * and using {@link StreamingAemWriter#DEFAULT_ATTITUDE_FORMAT default formatting}
     * for attitude ephemeris data output.
     * <p>
     * If the mandatory header entries are not present (or if header is null),
     * built-in defaults will be used
     * </p>
     * <p>
     * The writer is built from the complete header and partial metadata. The template
     * metadata is used to initialize and independent internal copy, that will be updated
     * as new segments are written (with at least the segment start and stop will change,
     * but some other parts may change too). The {@code template} object itself is not changed.
     * </>
     * @param conventions IERS Conventions
     * @param dataContext used to retrieve frames, time scales, etc.
     * @param header file header
     * @param template template for metadata
     * @param fileName file name for error messages
     * @param attitudeFormat {@link java.util.Formatter format parameters} for
     *                       attitude ephemeris data output
     * @since 11.0
     */
    public AEMWriter(final IERSConventions conventions, final DataContext dataContext,
                     final Header header, final AEMMetadata template,
                     final String fileName, final String attitudeFormat) {

        this.dataContext    = dataContext;
        this.header         = header;
        this.metadata       = copy(template);
        this.fileName       = fileName;
        this.attitudeFormat = attitudeFormat;
        this.timeScale      = metadata.getTimeSystem().getTimeScale(conventions, dataContext.getTimeScales());

    }

    /** {@inheritDoc} */
    @Override
    public void write(final Appendable appendable, final AttitudeEphemerisFile ephemerisFile)
        throws IOException {

        if (appendable == null) {
            throw new OrekitIllegalArgumentException(OrekitMessages.NULL_ARGUMENT, "writer");
        }

        if (ephemerisFile == null) {
            return;
        }

        final SatelliteAttitudeEphemeris satEphem = ephemerisFile.getSatellites().get(metadata.getObjectID());
        if (satEphem == null) {
            throw new OrekitIllegalArgumentException(OrekitMessages.VALUE_NOT_FOUND,
                                                     metadata.getObjectID(), "ephemerisFile");
        }

        // Get attitude ephemeris segments to output.
        final List<? extends AttitudeEphemerisSegment> segments = satEphem.getSegments();
        if (segments.isEmpty()) {
            // No data -> No output
            return;
        }

        writeHeader(appendable);

        // Loop on segments
        for (final AttitudeEphemerisSegment segment : segments) {
            metadata.setInterpolationMethod(segment.getInterpolationMethod());
            metadata.setInterpolationDegree(segment.getInterpolationSamples() - 1);
            writeMetadata(appendable, segment.getStart(), segment.getStop());
            startAttitudeBlock(appendable);
            // Loop on attitude data
            for (final TimeStampedAngularCoordinates coordinates : segment.getAngularCoordinates()) {
                writeAttitudeEphemerisLine(appendable, coordinates);
            }
            endAttitudeBlock(appendable);
        }

    }

    /**
     * Write the passed in {@link AEMFile} to a file at the output path specified.
     * @param outputFilePath a file path that the corresponding file will be written to
     * @param aemFile a populated aem file to serialize into the buffer
     * @throws IOException if any file writing operations fail or if the underlying
     *         format doesn't support a configuration in the EphemerisFile
     *         (for example having multiple satellites in one file, having
     *         the origin at an unspecified celestial body, etc.)
     */
    public void write(final String outputFilePath, final AEMFile aemFile)
        throws IOException {
        try (BufferedWriter appendable = Files.newBufferedWriter(Paths.get(outputFilePath), StandardCharsets.UTF_8)) {
            write(appendable, aemFile);
        }
    }

    /**
     * Write a single key and value to the stream using Key Value Notation (KVN).
     * @param appendable    The output stream for the AEM file. Most methods will append data
     *                  to this {@code appendable}.
     * @param key   the keyword to write
     * @param value the value to write
     * @param mandatory if true, null values triggers exception, otherwise they are silently ignored
     * @throws IOException if an I/O error occurs.
     */
    private void writeKeyValue(final Appendable appendable, final String key,
                               final String value, final boolean mandatory)
        throws IOException {
        if (value == null && mandatory) {
            throw new OrekitException(OrekitMessages.CCSDS_MISSING_KEYWORD, key, fileName);
        }
        appendable.append(String.format(STANDARDIZED_LOCALE, KV_FORMAT, key, value));
    }

    /**
     * convert a date to string value with high precision.
     * @param date date to write
     * @return date as a string
     */
    private String dateToString(final AbsoluteDate date) {
        final DateTimeComponents dt = date.getComponents(timeScale);
        return String.format(STANDARDIZED_LOCALE, DATE_FORMAT,
                             dt.getDate().getYear(),
                             dt.getDate().getMonth(),
                             dt.getDate().getDay(),
                             dt.getTime().getHour(),
                             dt.getTime().getMinute(),
                             dt.getTime().getSecond());
    }

    /**
     * Write a comment line using Key Value Notation (KVN).
     * @param appendable    The output stream for the AEM file. Most methods will append data
     *                  to this {@code appendable}.
     * @param comments comments to write
     * @throws IOException if an I/O error occurs.
     */
    private void writeComments(final Appendable appendable, final CommentsContainer comments) throws IOException {
        for (final String comment : comments.getComments()) {
            appendable.append(String.format(STANDARDIZED_LOCALE, COMMENT_FORMAT, comment));
        }
    }

    /**
     * Convert a double value to string, without internationalizetion issues.
     * @param value the value to convert
     * @return converted string
     */
    private String toString(final double value) throws IOException {
        return String.format(STANDARDIZED_LOCALE, "%f", value);
    }

    /** Writes the standard AEM header for the file.
     * @param appendable    The output stream for the AEM file. Most methods will append data
     *                  to this {@code appendable}.
     * @throws IOException if the stream cannot write to stream
     */
    public void writeHeader(final Appendable appendable) throws IOException {

        // Use built-in default if mandatory version not present
        writeKeyValue(appendable, AEMFile.FORMAT_VERSION_KEY, toString(header == null || Double.isNaN(header.getFormatVersion()) ?
                   CCSDS_AEM_VERS : header.getFormatVersion()),
                      true);

        // comments are optional
        if (header != null) {
            writeComments(appendable, header);
        }

        // creation date is informational only, but mandatory and always in UTC
        if (header == null || header.getCreationDate() == null) {
            final ZonedDateTime zdt = ZonedDateTime.now(ZoneOffset.UTC);
            writeKeyValue(appendable, HeaderKey.CREATION_DATE.name(),
                          String.format(STANDARDIZED_LOCALE, DATE_FORMAT,
                                        zdt.getYear(), zdt.getMonthValue(), zdt.getDayOfMonth(),
                                        zdt.getHour(), zdt.getMinute(), zdt.getSecond()),
                          true);
        } else {
            final DateTimeComponents creationDate =
                            header.getCreationDate().getComponents(dataContext.getTimeScales().getUTC());
            final DateComponents dc = creationDate.getDate();
            final TimeComponents tc = creationDate.getTime();
            writeKeyValue(appendable, HeaderKey.CREATION_DATE.name(),
                          String.format(STANDARDIZED_LOCALE, DATE_FORMAT,
                                        dc.getYear(), dc.getMonth(), dc.getDay(),
                                        tc.getHour(), tc.getMinute(), tc.getSecond()),
                          true);
        }


        // Use built-in default if mandatory originator not present
        writeKeyValue(appendable, HeaderKey.ORIGINATOR.name(),
                      (header == null || header.getOriginator() == null) ? DEFAULT_ORIGINATOR : header.getOriginator(),
                      true);

        // add an empty line for presentation
        appendable.append(NEW_LINE);

    }

    /** Write an ephemeris segment metadata.
     * @param appendable    The output stream for the AEM file. Most methods will append data
     *                  to this {@code appendable}.
     * @param start start date to use, if not already present in {@code metadata}
     * @param stop stop date to use, if not already present in {@code metadata}
     * @throws IOException if the output stream throws one while writing.
     */
    public void writeMetadata(final Appendable appendable, final AbsoluteDate start, final AbsoluteDate stop)
        throws IOException {

        // Start metadata
        appendable.append(META_START).append(NEW_LINE);

        writeComments(appendable, metadata);

        // objects
        writeKeyValue(appendable, ADMMetadataKey.OBJECT_NAME.name(), metadata.getObjectName(), true);
        writeKeyValue(appendable, ADMMetadataKey.OBJECT_ID.name(), metadata.getObjectID(),   true);
        writeKeyValue(appendable, ADMMetadataKey.CENTER_NAME.name(), metadata.getCenterName(), false);

        // frames
        final AttitudeEndPoints endPoints = metadata.getEndPoints();
        writeKeyValue(appendable, AEMMetadataKey.REF_FRAME_A.name(),
                      endPoints.getExternalFrame() == null ? null : endPoints.getExternalFrame().name(),
                      true);
        writeKeyValue(appendable, AEMMetadataKey.REF_FRAME_B.name(),
                      endPoints.getLocalFrame() == null ? null : endPoints.getLocalFrame().toString(),
                      true);
        writeKeyValue(appendable, AEMMetadataKey.ATTITUDE_DIR.name(),
                      EXTERNAL_TO_LOCAL, true);

        // time
        final CcsdsTimeScale cts = metadata.getTimeSystem();
        if (cts == null) {
            throw new OrekitException(OrekitMessages.CCSDS_MISSING_KEYWORD,
                                      MetadataKey.TIME_SYSTEM.name(), fileName);
        }
        writeKeyValue(appendable, MetadataKey.TIME_SYSTEM.name(), cts.name(), true);
        writeKeyValue(appendable, AEMMetadataKey.START_TIME.name(), dateToString(metadata.getStartTime() == null ? start : metadata.getStartTime()),
                      true);
        if (metadata.getUseableStartTime() != null) {
            writeKeyValue(appendable, AEMMetadataKey.USEABLE_START_TIME.name(), dateToString(metadata.getUseableStartTime()),
                          false);
        }
        if (metadata.getUseableStopTime() != null) {
            writeKeyValue(appendable, AEMMetadataKey.USEABLE_STOP_TIME.name(), dateToString(metadata.getUseableStopTime()),
                          false);
        }
        writeKeyValue(appendable, AEMMetadataKey.STOP_TIME.name(), dateToString(metadata.getStopTime() == null ? stop : metadata.getStopTime()),
                      true);

        // types
        final AEMAttitudeType attitudeType = metadata.getAttitudeType();
        writeKeyValue(appendable, AEMMetadataKey.ATTITUDE_TYPE.name(), attitudeType.toString(), true);
        if (attitudeType == AEMAttitudeType.QUATERNION ||
            attitudeType == AEMAttitudeType.QUATERNION_DERIVATIVE ||
            attitudeType == AEMAttitudeType.QUATERNION_DERIVATIVE) {
            writeKeyValue(appendable, AEMMetadataKey.QUATERNION_TYPE.name(), metadata.isFirst() ? FIRST : LAST, false);
        }

        if (attitudeType == AEMAttitudeType.EULER_ANGLE ||
            attitudeType == AEMAttitudeType.EULER_ANGLE_RATE) {
            if (metadata.getEulerRotSeq() == null) {
                // the keyword *will* be missing because we cannot set it
                throw new OrekitException(OrekitMessages.CCSDS_MISSING_KEYWORD,
                                          AEMMetadataKey.EULER_ROT_SEQ.name(), fileName);
            }
            writeKeyValue(appendable, AEMMetadataKey.EULER_ROT_SEQ.name(), metadata.getEulerRotSeq().name().replace('X', '1').replace('Y', '2').replace('Z', '3'),
                          false);
        }

        if (attitudeType == AEMAttitudeType.QUATERNION_RATE ||
            attitudeType == AEMAttitudeType.EULER_ANGLE_RATE) {
            if (metadata.localRates() == null) {
                // the keyword *will* be missing because we cannot set it
                throw new OrekitException(OrekitMessages.CCSDS_MISSING_KEYWORD,
                                          AEMMetadataKey.RATE_FRAME.name(), fileName);
            }
            writeKeyValue(appendable, AEMMetadataKey.RATE_FRAME.name(), metadata.localRates() ? LOCAL_RATES : EXTERNAL_RATES,
                          false);
        }

        // interpolation
        writeKeyValue(appendable, AEMMetadataKey.INTERPOLATION_METHOD.name(), metadata.getInterpolationMethod(),
                      false);
        writeKeyValue(appendable, AEMMetadataKey.INTERPOLATION_DEGREE.name(), Integer.toString(metadata.getInterpolationDegree()),
                      false);

        // Stop metadata
        appendable.append(META_STOP).append(NEW_LINE);

    }

    /**
     * Write a single attitude ephemeris line according to section 4.2.4 and Table 4-4.
     * @param appendable    The output stream for the AEM file. Most methods will append data
     *                  to this {@code appendable}.
     * @param attitude the attitude information for a given date.
     * @throws IOException if the output stream throws one while writing.
     */
    public void writeAttitudeEphemerisLine(final Appendable appendable, final TimeStampedAngularCoordinates attitude)
        throws IOException {

        // Epoch
        appendable.append(dateToString(attitude.getDate()));

        // Attitude data in degrees
        final double[] data = metadata.getAttitudeType().getAttitudeData(attitude,
                                                                         metadata.isFirst(),
                                                                         metadata.localRates(),
                                                                         metadata.getEulerRotSeq());
        final int      size = data.length;
        for (int index = 0; index < size; index++) {
            appendable.append(' ').append(String.format(STANDARDIZED_LOCALE, attitudeFormat, data[index]));
        }

        // end the line
        appendable.append(NEW_LINE);

    }

    /**
     * Start of an attitude block.
     * @param appendable    The output stream for the AEM file. Most methods will append data
     *                  to this {@code appendable}.
     * @throws IOException if the output stream throws one while writing.
     */
    void startAttitudeBlock(final Appendable appendable) throws IOException {
        appendable.append(DATA_START).append(NEW_LINE);
    }

    /**
     * End of an attitude block.
     * @param appendable    The output stream for the AEM file. Most methods will append data
     *                  to this {@code appendable}.
     * @throws IOException if the output stream throws one while writing.
     */
    void endAttitudeBlock(final Appendable appendable) throws IOException {
        appendable.append(DATA_STOP).append(NEW_LINE);
    }

    /** Copy a metadata object (excluding times), making sure mandatory fields have been initialized.
     * @param original original object
     * @return a new copy
     */
    private AEMMetadata copy(final AEMMetadata original) {

        original.checkMandatoryEntries();

        // allocate new instance
        final AEMMetadata copy = new AEMMetadata(original.getInterpolationDegree());

        // copy comments
        for (String comment : original.getComments()) {
            copy.addComment(comment);
        }

        // copy object
        copy.setObjectName(original.getObjectName());
        copy.setObjectID(original.getObjectID());
        if (original.getCenterName() != null) {
            copy.setCenterName(original.getObjectName(), dataContext.getCelestialBodies());
        }

        // copy frames
        copy.getEndPoints().setFrameA(original.getEndPoints().getExternalFrame().name());
        copy.getEndPoints().setFrameB(original.getEndPoints().getLocalFrame().toString());
        copy.getEndPoints().setDirection(original.getEndPoints().isExternal2Local() ? EXTERNAL_TO_LOCAL : LOCAL_TO_EXTERNAL);
        copy.getEndPoints().setFrameA(original.getEndPoints().getExternalFrame().name());

        // copy time system only (ignore times themselves)
        copy.setTimeSystem(original.getTimeSystem());

        // copy attitude definitions
        copy.setAttitudeType(original.getAttitudeType());
        if (original.isFirst() != null) {
            copy.setIsFirst(original.isFirst());
        }
        if (original.getEulerRotSeq() != null) {
            copy.setEulerRotSeq(original.getEulerRotSeq());
        }
        if (original.localRates() != null) {
            copy.setLocalRates(original.localRates());
        }

        // copy interpolation (degree has already been set up at construction)
        if (original.getInterpolationMethod() != null) {
            copy.setInterpolationMethod(original.getInterpolationMethod());
        }

        return copy;

    }
}
