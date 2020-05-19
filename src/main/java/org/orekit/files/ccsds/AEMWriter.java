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

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.AEMFile.AemSatelliteEphemeris;
import org.orekit.files.ccsds.AEMFile.AttitudeEphemeridesBlock;
import org.orekit.files.ccsds.StreamingAemWriter.AEMSegment;
import org.orekit.time.TimeScale;
import org.orekit.utils.TimeStampedAngularCoordinates;

/**
 * A writer for Attitude Ephemeris Messsage (AEM) files.
 * @author Bryan Cazabonne
 * @since 10.2
 */
public class AEMWriter {

    /** Originator name, usually the organization and/or country. **/
    private final String originator;

    /** Space object ID, usually an official international designator such as "1998-067A". */
    private final String spaceObjectId;

    /** Space object name, usually a common name for an object like "ISS". **/
    private final String spaceObjectName;

    /**
     * Standard default constructor that creates a writer with default
     * configurations.
     */
    public AEMWriter() {
        this(StreamingAemWriter.DEFAULT_ORIGINATOR, null, null);
    }

    /**
     * Constructor used to create a new AEM writer configured with the necessary
     * parameters to successfully fill in all required fields that aren't part
     * of a standard object.
     *
     * @param originator the originator field string
     * @param spaceObjectId the spacecraft ID
     * @param spaceObjectName the space object common name
     */
    public AEMWriter(final String originator, final String spaceObjectId,
                     final String spaceObjectName) {
        this.originator          = originator;
        this.spaceObjectId       = spaceObjectId;
        this.spaceObjectName     = spaceObjectName;
    }

    /**
     * Write the passed in {@link AEMFile} using the passed in {@link Appendable}.
     * @param writer a configured Appendable to feed with text
     * @param aemFile  a populated aem file to serialize into the buffer
     * @throws IOException if any buffer writing operations fail or if the underlying
     *         format doesn't support a configuration in the EphemerisFile
     *         for example having multiple satellites in one file, having
     *         the origin at an unspecified celestial body, etc.)
     */
    public void write(final Appendable writer, final AEMFile aemFile)
        throws IOException {

        if (writer == null) {
            throw new OrekitIllegalArgumentException(OrekitMessages.NULL_ARGUMENT, "writer");
        }

        if (aemFile == null) {
            return;
        }

        final String idToProcess;
        if (spaceObjectId != null) {
            if (aemFile.getSatellites().containsKey(spaceObjectId)) {
                idToProcess = spaceObjectId;
            } else {
                throw new OrekitIllegalArgumentException(OrekitMessages.VALUE_NOT_FOUND, spaceObjectId, "ephemerisFile");
            }
        } else if (aemFile.getSatellites().keySet().size() == 1) {
            idToProcess = aemFile.getSatellites().keySet().iterator().next();
        } else {
            throw new OrekitIllegalArgumentException(OrekitMessages.EPHEMERIS_FILE_NO_MULTI_SUPPORT);
        }

        // Get satellite and attitude ephemeris segments to output.
        final AemSatelliteEphemeris          satEphem = aemFile.getSatellites().get(idToProcess);
        final List<AttitudeEphemeridesBlock> segments = satEphem.getSegments();
        if (segments.isEmpty()) {
            // No data -> No output
            return;
        }
        // First segment
        final AttitudeEphemeridesBlock firstSegment = segments.get(0);

        final String objectName = this.spaceObjectName == null ? idToProcess : this.spaceObjectName;
        // Only one time scale per AEM file, see Section 4.2.5.4.2
        final TimeScale timeScale = firstSegment.getTimeScale();
        // Metadata that is constant for the whole AEM file
        final Map<Keyword, String> metadata = new LinkedHashMap<>();
        metadata.put(Keyword.TIME_SYSTEM, firstSegment.getTimeScaleString());
        metadata.put(Keyword.ORIGINATOR,  this.originator);
        // Only one object in an AEM file, see Section 2.3.1
        metadata.put(Keyword.OBJECT_NAME,   objectName);
        metadata.put(Keyword.OBJECT_ID,     idToProcess);
        // Writer for AEM files
        final StreamingAemWriter aemWriter =
                        new StreamingAemWriter(writer, timeScale, metadata);
        aemWriter.writeHeader();

        // Loop on segments
        for (final AttitudeEphemeridesBlock segment : segments) {
            // Segment specific metadata
            metadata.clear();
            metadata.put(Keyword.CENTER_NAME,          segment.getFrameCenterString());
            metadata.put(Keyword.REF_FRAME_A,          segment.getRefFrameAString());
            metadata.put(Keyword.REF_FRAME_B,          segment.getRefFrameBString());
            metadata.put(Keyword.ATTITUDE_DIR,         segment.getAttitudeDirection());
            metadata.put(Keyword.START_TIME,           segment.getStart().toString(timeScale));
            metadata.put(Keyword.STOP_TIME,            segment.getStop().toString(timeScale));
            metadata.put(Keyword.ATTITUDE_TYPE,        segment.getAttitudeType());
            metadata.put(Keyword.INTERPOLATION_METHOD, segment.getInterpolationMethod());
            metadata.put(Keyword.INTERPOLATION_DEGREE, String.valueOf(segment.getInterpolationDegree()));

            final AEMSegment segmentWriter = aemWriter.newSegment(metadata);
            segmentWriter.writeMetadata();
            segmentWriter.startAttitudeBlock();
            // Loop on attitude data
            for (final TimeStampedAngularCoordinates coordinates : segment.getAngularCoordinates()) {
                segmentWriter.writeAttitudeEphemerisLine(coordinates, segment.isFirst(),
                                                         segment.getAttitudeType(), segment.getRotationOrder());
            }
            segmentWriter.endAttitudeBlock();
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
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(outputFilePath), StandardCharsets.UTF_8)) {
            write(writer, aemFile);
        }
    }

}
