/* Copyright 2016 Applied Defense Solutions (ADS)
 * Licensed to CS Systèmes d'Information (CS) under one or more
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

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Date;
import java.util.Locale;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.general.EphemerisFile;
import org.orekit.files.general.EphemerisFile.EphemerisSegment;
import org.orekit.files.general.EphemerisFileWriter;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.TimeStampedPVCoordinates;

/**
 * An OEM Writer class that can take in a general {@link EphemerisFile} object
 * and export it as a valid OEM file.
 *
 * @author Hank Grabowski
 * @since 9.0
 */
public class OEMWriter implements EphemerisFileWriter {
    /** Version number implemented. **/
    public static final String CCSDS_OEM_VERS = "2.0";

    /** Default interpolation method if the user specifies none. **/
    public static final InterpolationMethod DEFAULT_INTERPOLATION_METHOD = InterpolationMethod.LAGRANGE;

    /** Default originator field value if user specifies none. **/
    public static final String DEFAULT_ORIGINATOR = "OREKIT";

    /** Standardized locale to use, to ensure files can be exchanged without internationalization issues. */
    private static final Locale STANDARDIZED_LOCALE = Locale.US;

    /** String format used for all key/value pair lines. **/
    private static final String KV_FORMAT = "%s = %s";

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
    public void write(final BufferedWriter writer, final EphemerisFile ephemerisFile)
            throws OrekitException, IOException {

        if (writer == null) {
            throw new OrekitIllegalArgumentException(OrekitMessages.NULL_ARGUMENT, "writer");
        }

        if (ephemerisFile == null) {
            return;
        }

        String idToProcess = null;
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

        final EphemerisFile.SatelliteEphemeris satEphem = ephemerisFile.getSatellites().get(idToProcess);
        writeHeader(writer);
        writer.newLine();

        for (EphemerisSegment segment : satEphem.getSegments()) {
            String objectName = this.spaceObjectName;
            if (objectName == null) {
                objectName = idToProcess;
            }
            writeMetadata(writer, segment, objectName, idToProcess);
            writer.newLine();
            writeEphemeris(writer, segment);
            writer.newLine();
            writer.newLine();
        }
    }

    /**
     * Given an {@link EphemerisSegment} writes out all ephemeris lines in an
     * OEM standard format.
     *
     * @param writer
     *            the writer object to feed the serialized text to
     * @param segment
     *            the segment to process
     * @throws OrekitException
     *             if the time scale calculations go awry
     * @throws IOException
     *             if the stream cannot write to stream
     */
    private void writeEphemeris(final BufferedWriter writer, final EphemerisSegment segment)
            throws OrekitException, IOException {
        final double meterToKm = 1e-3;
        final String ephemerisLineFormat = "%s %16.16e %16.16e %16.16e %16.16e %16.16e %16.16e";

        for (TimeStampedPVCoordinates coordinates : segment.getCoordinates()) {
            final String timeString = coordinates.getDate().toString(segment.getTimeScale());
            final Vector3D position = coordinates.getPosition();
            final Vector3D velocity = coordinates.getVelocity();
            final double x = position.getX() * meterToKm;
            final double y = position.getY() * meterToKm;
            final double z = position.getZ() * meterToKm;
            final double vx = velocity.getX() * meterToKm;
            final double vy = velocity.getY() * meterToKm;
            final double vz = velocity.getZ() * meterToKm;
            final String outputString = String.format(STANDARDIZED_LOCALE, ephemerisLineFormat,
                                                      timeString, x, y, z, vx, vy, vz);
            writer.write(outputString);
            writer.newLine();
        }
    }

    /**
     * Writes the standard OEM header for the file.
     *
     * @param writer
     *            the writer object to feed the serialized text to
     * @throws OrekitException
     *             if the time scale calculations go awry
     * @throws IOException
     *             if the stream cannot write to stream
     */
    private void writeHeader(final BufferedWriter writer) throws IOException, OrekitException {
        writer.write(String.format(STANDARDIZED_LOCALE, KV_FORMAT, "CCSDS_OEM_VERS", CCSDS_OEM_VERS));
        writer.newLine();
        writer.write(
                String.format(STANDARDIZED_LOCALE, KV_FORMAT, "CREATION_DATE", new AbsoluteDate(new Date(), TimeScalesFactory.getUTC())));
        writer.newLine();
        writer.write(String.format(STANDARDIZED_LOCALE, KV_FORMAT, "ORIGINATOR", originator));
        writer.newLine();
    }

    /**
     * Writes out the metadata block associated with this {@link EphemerisSegment}.
     *
     * @param writer
     *            the writer object to feed the serialized text to
     * @param segment
     *            the segment to process
     * @param objectName common object name to use for this space object
     * @param objectId standard object ID to use for this space object
     * @throws OrekitException
     *             if the time scale calculations go awry
     * @throws IOException
     *             if the stream cannot write to stream
     */
    private void writeMetadata(final BufferedWriter writer, final EphemerisSegment segment, final String objectName,
            final String objectId) throws IOException, OrekitException {
        writer.write("META_START");
        writer.newLine();
        writer.write(String.format(STANDARDIZED_LOCALE, KV_FORMAT, "OBJECT_NAME", objectName));
        writer.newLine();
        writer.write(String.format(STANDARDIZED_LOCALE, KV_FORMAT, "OBJECT_ID", objectId));
        writer.newLine();
        writer.write(String.format(STANDARDIZED_LOCALE, KV_FORMAT, "CENTER_NAME", segment.getFrameCenterString()));
        writer.newLine();
        writer.write(String.format(STANDARDIZED_LOCALE, KV_FORMAT, "REF_FRAME", segment.getFrameString()));
        writer.newLine();
        writer.write(String.format(STANDARDIZED_LOCALE, KV_FORMAT, "TIME_SYSTEM", segment.getTimeScaleString()));
        writer.newLine();
        writer.write(String.format(STANDARDIZED_LOCALE, KV_FORMAT, "START_TIME", segment.getStart().toString(segment.getTimeScale())));
        writer.newLine();
        writer.write(String.format(STANDARDIZED_LOCALE, KV_FORMAT, "USEABLE_START_TIME", segment.getStart().toString(segment.getTimeScale())));
        writer.newLine();
        writer.write(String.format(STANDARDIZED_LOCALE, KV_FORMAT, "USEABLE_STOP_TIME", segment.getStop().toString(segment.getTimeScale())));
        writer.newLine();
        writer.write(String.format(STANDARDIZED_LOCALE, KV_FORMAT, "STOP_TIME", segment.getStop().toString(segment.getTimeScale())));
        writer.newLine();
        writer.write(String.format(STANDARDIZED_LOCALE, KV_FORMAT, "INTERPOLATION", this.interpolationMethod));
        writer.newLine();
        if (this.interpolationMethod != InterpolationMethod.LINEAR) {
            writer.write(String.format(STANDARDIZED_LOCALE, KV_FORMAT, "INTERPOLATION_DEGREE", segment.getInterpolationSamples() - 1));
            writer.newLine();
        }
        writer.write("META_STOP");
        writer.newLine();
    }

    public enum InterpolationMethod {
        /**
         * Hermite interpolation.
         */
        HERMITE,
        /**
         * Lagrange interpolation.
         */
        LAGRANGE,
        /**
         * Linear interpolation.
         */
        LINEAR
    }

}
