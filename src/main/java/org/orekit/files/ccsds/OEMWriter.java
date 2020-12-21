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
package org.orekit.files.ccsds;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.OEMFile.CovarianceMatrix;
import org.orekit.files.ccsds.OEMFile.EphemeridesBlock;
import org.orekit.files.ccsds.StreamingOemWriter.Segment;
import org.orekit.files.general.EphemerisFile;
import org.orekit.files.general.EphemerisFile.EphemerisSegment;
import org.orekit.files.general.EphemerisFileWriter;
import org.orekit.time.TimeScale;
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
    public static final String CCSDS_OEM_VERS = "2.0";

    /** Default interpolation method if the user specifies none. **/
    public static final InterpolationMethod DEFAULT_INTERPOLATION_METHOD = InterpolationMethod.LAGRANGE;

    /** Default originator field value if user specifies none. **/
    public static final String DEFAULT_ORIGINATOR = "OREKIT";

    /** The interpolation method for ephemeris data. */
    private final InterpolationMethod interpolationMethod;

    /** Originator name, usually the organization and/or country. **/
    private final String originator;

    /**
     * Space object ID, usually an official international designator such as
     * "1998-067A".
     **/
    private final String spaceObjectId;

    /** Space object name, usually a common name for an object like "ISS". **/
    private final String spaceObjectName;

    /** Format for position ephemeris data output. */
    private final String positionFormat;

    /** Format for velocity ephemeris data output. */
    private final String velocityFormat;

    /**
     * Standard default constructor that creates a writer with default
     * configurations.
     */
    public OEMWriter() {
        this(DEFAULT_INTERPOLATION_METHOD, DEFAULT_ORIGINATOR, null, null,
             StreamingOemWriter.DEFAULT_POSITION_FORMAT,
             StreamingOemWriter.DEFAULT_VELOCITY_FORMAT);
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
     * Constructor used to create a new OEM writer configured with the necessary
     * parameters to successfully fill in all required fields that aren't part
     * of a standard {@link EphemerisFile} object and user-defined position and
     * velocity ephemeris data output {@link java.util.Formatter format}.
     *
     * @param interpolationMethod
     *            the interpolation method to specify in the OEM file
     * @param originator
     *            the originator field string
     * @param spaceObjectId
     *            the spacecraft ID
     * @param spaceObjectName
     *            the space object common name
     * @param positionFormat
     *            format parameters for position ephemeris data output
     * @param velocityFormat
     *            format parameters for velocity ephemeris data output
     */
    public OEMWriter(final InterpolationMethod interpolationMethod, final String originator,
            final String spaceObjectId, final String spaceObjectName,
            final String positionFormat, final String velocityFormat) {
        this.interpolationMethod = interpolationMethod;
        this.originator = originator;
        this.spaceObjectId = spaceObjectId;
        this.spaceObjectName = spaceObjectName;
        this.positionFormat = positionFormat;
        this.velocityFormat = velocityFormat;
    }

    /** {@inheritDoc} */
    @Override
    public void write(final Appendable writer, final EphemerisFile ephemerisFile)
            throws IOException {

        if (writer == null) {
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
        } else if (ephemerisFile.getSatellites().keySet().size() == 1) {
            idToProcess = ephemerisFile.getSatellites().keySet().iterator().next();
        } else {
            throw new OrekitIllegalArgumentException(OrekitMessages.EPHEMERIS_FILE_NO_MULTI_SUPPORT);
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
        metadata.put(Keyword.ORIGINATOR, this.originator);
        // Only one object in an OEM file, see Section 2.1
        metadata.put(Keyword.OBJECT_ID, idToProcess);
        metadata.put(Keyword.OBJECT_NAME, objectName);
        metadata.put(Keyword.INTERPOLATION, this.interpolationMethod.toString());

        // Header comments. If header comments are presents, they are assembled together in a single line
        if (ephemerisFile instanceof OEMFile) {
            // Cast to OEMFile
            final OEMFile oemFile = (OEMFile) ephemerisFile;
            if (!oemFile.getHeaderComment().isEmpty()) {
                // Loop on comments
                final StringBuffer buffer = new StringBuffer();
                for (String comment : oemFile.getHeaderComment()) {
                    buffer.append(comment);
                }
                // Update metadata
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

            final Segment segmentWriter = oemWriter.newSegment(null, metadata);
            segmentWriter.writeMetadata();
            for (final TimeStampedPVCoordinates coordinates : segment.getCoordinates()) {
                segmentWriter.writeEphemerisLine(coordinates);
            }

            if (segment instanceof EphemeridesBlock) {
                final EphemeridesBlock curr_ephem_block = (EphemeridesBlock) segment;
                final List<CovarianceMatrix> covarianceMatrices = curr_ephem_block.getCovarianceMatrices();
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
