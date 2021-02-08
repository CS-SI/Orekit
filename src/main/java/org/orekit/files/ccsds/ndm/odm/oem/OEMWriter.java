/* Copyright 2016 Applied Defense Solutions (ADS)
 * Licensed to CS GROUP (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * ADS licenses this file to You under the Apache License, Version 2.0
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
package org.orekit.files.ccsds.ndm.odm.oem;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.orekit.data.DataContext;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitInternalError;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.Keyword;
import org.orekit.files.ccsds.ndm.odm.oem.StreamingOemWriter.SegmentWriter;
import org.orekit.files.ccsds.section.Header;
import org.orekit.files.ccsds.section.HeaderKey;
import org.orekit.files.ccsds.utils.CcsdsTimeScale;
import org.orekit.files.general.EphemerisFile;
import org.orekit.files.general.EphemerisFile.EphemerisSegment;
import org.orekit.files.general.EphemerisFileWriter;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.TimeStampedPVCoordinates;

/**
 * An OEM Writer class that can take in a general {@link EphemerisFile} object
 * and export it as a valid OEM file.
 *
 * @author Hank Grabowski
 * @author Evan Ward
 * @since 9.0
 * @see <a href="https://public.ccsds.org/Pubs/502x0b2c1.pdf">CCSDS 502.0-B-2 Orbit Data
 *      Messages</a>
 * @see <a href="https://public.ccsds.org/Pubs/500x0g4.pdf">CCSDS 500.0-G-4 Navigation
 *      Data Definitions and Conventions</a>
 * @see StreamingOemWriter
 */
public class OEMWriter implements EphemerisFileWriter {

    /** Version number implemented. **/
    public static final double CCSDS_OEM_VERS = 3.0;

    /** Default value for {@link HeaderKey#ORIGINATOR}. */
    public static final String DEFAULT_ORIGINATOR = "OREKIT";

    /** Default value for {@link #TIME_SYSTEM}. */
    public static final CcsdsTimeScale DEFAULT_TIME_SYSTEM = CcsdsTimeScale.UTC;

    /** Default file name for error messages. */
    public static final String DEFAULT_FILE_NAME = "<OEM output>";

    /** Default interpolation method if the user specifies none. **/
    public static final InterpolationMethod DEFAULT_INTERPOLATION_METHOD = InterpolationMethod.LAGRANGE;

    /**
     * Default format used for position data output: 3 digits
     * after the decimal point and leading space for positive values.
     */
    public static final String DEFAULT_POSITION_FORMAT = "% .3f";

    /**
     * Default format used for velocity data output: 6 digits
     * after the decimal point and leading space for positive values.
     */
    public static final String DEFAULT_VELOCITY_FORMAT = "% .6f";

    /** New line separator for output file. */
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

    /** Marker for start of covariance section. */
    private static final String COVARIANCE_START = "COVARIANCE_START";

    /** Marker for stop of covariance section. */
    private static final String COVARIANCE_STOP = "COVARIANCE_STOP";

    /** Data context used for obtain frames and time scales. */
    private final DataContext dataContext;

    /** File name for error messages. */
    private final String fileName;

    /** Format for attitude ephemeris data output. */
    private final String attitudeFormat;

    /** File header. */
    private final Header header;

    /** Current metadata. */
    private final OEMMetadata metadata;

    /** Time scale for all segments. */
    private final TimeScale timeScale;

    /** Format for position ephemeris data output. */
    private final String positionFormat;

    /** Format for velocity ephemeris data output. */
    private final String velocityFormat;

    /**
     * Standard default constructor that creates a writer with default
     * configurations.
     * @param conventions IERS Conventions
     * @param dataContext used to retrieve frames, time scales, etc.
     * @param header file header (may be null)
     * @param template template for metadata
     * @since 11.0
     */
    public OEMWriter(final IERSConventions conventions, final DataContext dataContext,
                     final Header header, final OEMMetadata template) {
        this(conventions, dataContext, header, template,
             DEFAULT_FILE_NAME, DEFAULT_POSITION_FORMAT, DEFAULT_VELOCITY_FORMAT);
    }

    /**
     * Constructor used to create a new OEM writer configured with the necessary
     * parameters to successfully fill in all required fields that aren't part
     * of a standard {@link EphemerisFile} object and using default formatting for
     * {@link StreamingOemWriter#DEFAULT_POSITION_FORMAT position} and
     * {@link StreamingOemWriter#DEFAULT_VELOCITY_FORMAT velocity} ephemeris data output.
     *
     * @param interpolationMethod
     *            the interpolation method to specify in the OEM file
     * @param originator
     *            the originator field string
     * @param spaceObjectId
     *            the spacecraft ID
     * @param spaceObjectName
     *            the space object common name
     */
    public OEMWriter(final InterpolationMethod interpolationMethod, final String originator,
                     final String spaceObjectId, final String spaceObjectName) {
        this(interpolationMethod, originator, spaceObjectId, spaceObjectName,
             StreamingOemWriter.DEFAULT_POSITION_FORMAT,
             StreamingOemWriter.DEFAULT_VELOCITY_FORMAT);
    }

    /**
     * Constructor used to create a new OEM writer configured with the necessary parameters
     * to successfully fill in all required fields that aren't part of a standard object.
     * <p>
     * If the mandatory header entries are not present (or if header is null),
     * built-in defaults will be used
     * </p>
     * <p>
     * The writer is built from the complete header and partial metadata. The template
     * metadata is used to initialize and independent local copy, that will be updated
     * as new segments are written (with at least the segment start and stop will change,
     * but some other parts may change too). The {@code template} argument itself is not
     * changed.
     * </>
     * @param conventions IERS Conventions
     * @param dataContext used to retrieve frames, time scales, etc.
     * @param header file header (may be null)
     * @param template template for metadata
     * @param fileName file name for error messages
     * @param attitudeFormat {@link java.util.Formatter format parameters} for
     *                       attitude ephemeris data output
     * @since 11.0
     */
    public OEMWriter(final IERSConventions conventions, final DataContext dataContext,
                     final Header header, final OEMMetadata template,
                     final String fileName, final String attitudeFormat) {

        this.dataContext    = dataContext;
        this.header         = header;
        this.metadata       = copy(template);
        this.fileName       = fileName;
        this.attitudeFormat = attitudeFormat;
        this.timeScale      = metadata.getTimeSystem().getTimeScale(conventions, dataContext.getTimeScales());

    }

    /** Get the local copy of the template metadata.
     * <p>
     * The content of this copy should generally be updated before
     * {@link #writeMetadata(Appendable) writeMetadata} is called,
     * at least in order to update {@link OEMMetadata#setStartTime(AbsoluteDate)
     * start time} and {@link OEMMetadata#setStopTime(AbsoluteDate) stop time}
     * for the upcoming {@link #writeOrbitEphemerisLine(Appendable, TimeStampedPVCoordinates)
     * ephemeris data lines}.
     * </p>
     * @return local copy of the template metadata
     */
    public OEMMetadata getMetadata() {
        return metadata;
    }

    /** {@inheritDoc}
     * <p>
     * As {@link EphemerisFile.SatelliteEphemeris} does not have all the entries
     * from {@link OEMMetadata}, the only values that will be extracted from the
     * {@code ephemerisFile} will be the start time, stop time, reference frame, interpolation
     * method and interpolation degree. The missing values (like object name, local spacecraft
     * body frame...) will be inherited from the template  metadata set at writer
     * {@link #OEMWriter(IERSConventions, DataContext, Header, OEMMetadata, String, String) construction}.
     * </p>
     */
    @Override
    public void write(final Appendable appendable, final EphemerisFile ephemerisFile)
            throws IOException {

        if (appendable == null) {
            throw new OrekitIllegalArgumentException(OrekitMessages.NULL_ARGUMENT, "writer");
        }

        if (ephemerisFile == null) {
            return;
        }

        final String idToProcess;
        if (spaceObjectId != null) {
            if (ephemerisFile.getSatellites().containsKey(spaceObjectId)) {
                idToProcess = spaceObjectId;
            } else {
                throw new OrekitIllegalArgumentException(OrekitMessages.VALUE_NOT_FOUND, spaceObjectId, "ephemerisFile");
            }
        } else if (ephemerisFile.getSatellites().size() == 1) {
            idToProcess = ephemerisFile.getSatellites().entrySet().iterator().next().getKey();
        } else {
            // TODO: remove
            throw new OrekitInternalError(null);
        }

        // Get satellite and ephemeris segments to output.
        final EphemerisFile.SatelliteEphemeris satEphem = ephemerisFile.getSatellites().get(idToProcess);
        final List<? extends EphemerisSegment> segments = satEphem.getSegments();
        if (segments.isEmpty()) {
            // no data -> no output
            return;
        }
        final EphemerisSegment firstSegment = segments.get(0);

        final String objectName = this.spaceObjectName == null ?
                idToProcess : this.spaceObjectName;
        // Only one time scale per OEM file, see Section 5.2.4.5
        final TimeScale timeScale = firstSegment.getTimeScale();
        // metadata that is constant for the whole OEM file
        final Map<Keyword, String> metadata = new LinkedHashMap<>();
        metadata.put(Keyword.TIME_SYSTEM, firstSegment.getTimeScaleString());

        // TODO: ORIGINATOR belongs to header, not metadata
        metadata.put(Keyword.ORIGINATOR, this.originator);
        // Only one object in an OEM file, see Section 2.1
        metadata.put(Keyword.OBJECT_ID, idToProcess);
        metadata.put(Keyword.OBJECT_NAME, objectName);
        metadata.put(Keyword.INTERPOLATION, this.interpolationMethod.toString());

        // Header comments. If header comments are presents, they are assembled together in a single line
        if (ephemerisFile instanceof OEMFile) {
            // Cast to OEMFile
            final OEMFile oemFile = (OEMFile) ephemerisFile;
            if (!oemFile.getHeader().getComments().isEmpty()) {
                // Loop on comments
                final StringBuffer buffer = new StringBuffer();
                for (String comment : oemFile.getHeader().getComments()) {
                    buffer.append(comment);
                }
                // Update metadata
                // TODO: comments should not be assembled in a single line
                metadata.put(Keyword.COMMENT, buffer.toString());
            }
        }

        final StreamingOemWriter oemWriter =
                new StreamingOemWriter(writer, timeScale, metadata, positionFormat, velocityFormat);
        oemWriter.writeHeader();

        for (final EphemerisSegment segment : segments) {
            // segment specific metadata
            metadata.clear();
            metadata.put(Keyword.CENTER_NAME, segment.getFrameCenterString());
            metadata.put(Keyword.REF_FRAME, segment.getFrameString());
            metadata.put(Keyword.START_TIME, segment.getStart().toString(timeScale));
            metadata.put(Keyword.STOP_TIME, segment.getStop().toString(timeScale));
            metadata.put(Keyword.INTERPOLATION_DEGREE,
                    String.valueOf(segment.getInterpolationSamples() - 1));

            final SegmentWriter segmentWriter = oemWriter.newSegment(null, metadata);
            segmentWriter.writeMetadata();
            for (final TimeStampedPVCoordinates coordinates : segment.getCoordinates()) {
                segmentWriter.writeEphemerisLine(coordinates);
            }

            if (segment instanceof OEMSegment) {
                final List<CovarianceMatrix> covarianceMatrices = ((OEMSegment) segment).getCovarianceMatrices();
                if (!covarianceMatrices.isEmpty()) {
                    segmentWriter.writeCovarianceMatrices(covarianceMatrices);
                }
            }
        }
    }

    /** OEM interpolation method. See Table 5-3. */
    public enum InterpolationMethod {
        /** Hermite interpolation. */
        HERMITE,
        /** Lagrange interpolation. */
        LAGRANGE,
        /** Linear interpolation. */
        LINEAR
    }

}
