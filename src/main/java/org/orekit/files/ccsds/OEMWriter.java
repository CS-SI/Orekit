/* Copyright 2016 Applied Defense Solutions (ADS)
 * Licensed to CS Group (CS) under one or more
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
 * @see <a href="https://public.ccsds.org/Pubs/500x0g3.pdf">CCSDS 500.0-G-3 Navigation
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

    /**
     * The space object ID we want to export, or null if we will process
     * whichever space object is in an {@link EphemerisFile} with only one space
     * object in it.
     */
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

    /**
     * Standard default constructor that creates a writer with default
     * configurations.
     */
    public OEMWriter() {
        this(DEFAULT_INTERPOLATION_METHOD, DEFAULT_ORIGINATOR, null, null);
    }

    /**
     * Constructor used to create a new OEM writer configured with the necessary
     * parameters to successfully fill in all required fields that aren't part
     * of a standard @{link EphemerisFile} object.
     *
     * @param interpolationMethod
     *            The interpolation method to specify in the OEM file
     * @param originator
     *            The originator field string
     * @param spaceObjectId
     *            The spacecraft ID
     * @param spaceObjectName
     *            The space object common name
     */
    public OEMWriter(final InterpolationMethod interpolationMethod, final String originator, final String spaceObjectId,
            final String spaceObjectName) {
        this.interpolationMethod = interpolationMethod;
        this.originator = originator;
        this.spaceObjectId = spaceObjectId;
        this.spaceObjectName = spaceObjectName;
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
        final StreamingOemWriter oemWriter =
                new StreamingOemWriter(writer, timeScale, metadata);
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
