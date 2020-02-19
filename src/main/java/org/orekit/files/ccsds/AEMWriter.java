/* Copyright 2002-2020 CS Group
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

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;

/**
 * A writer for Attitude Ephemeris Messsage (AEM) files.
 * @author Bryan Cazabonne
 * @since 10.2
 */
public class AEMWriter {

    /** Default interpolation method if the user specifies none. **/
    public static final AEMInterpolationMethod DEFAULT_INTERPOLATION_METHOD = AEMInterpolationMethod.LAGRANGE;

    /** The interpolation method for ephemeris data. */
    private final AEMInterpolationMethod interpolationMethod;

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
    public AEMWriter() {
        this(DEFAULT_INTERPOLATION_METHOD, StreamingAemWriter.DEFAULT_ORIGINATOR, null, null);
    }

    /**
     * Constructor used to create a new AEM writer configured with the necessary
     * parameters to successfully fill in all required fields that aren't part
     * of a standard object.
     *
     * @param interpolationMethod the interpolation method to specify in the AEM file
     * @param originator the originator field string
     * @param spaceObjectId the spacecraft ID
     * @param spaceObjectName the space object common name
     */
    public AEMWriter(final AEMInterpolationMethod interpolationMethod,
                     final String originator, final String spaceObjectId, final String spaceObjectName) {
        this.interpolationMethod = interpolationMethod;
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

    /** AEM interpolation method. See Table 4-3. */
    public enum AEMInterpolationMethod {

        /** Hermite interpolation. */
        HERMITE,

        /** Lagrange interpolation. */
        LAGRANGE,

        /** Linear interpolation. */
        LINEAR

    }

    /** AEM interpolation method. See Table 4-3. */
    public enum AEMAttitudeType {

        /** Quaternion. */
        QUATERNION,

        /** Quaternion and derivatives. */
        QUATERNION_DERIVATIVE,

        /** Quaternion and rotation rate. */
        QUATERNION_RATE,

        /** Euler angles. */
        EULER_ANGLE,

        /** Euler angles and rotation rate. */
        EULER_ANGLE_RATE,

        /** Spin. */
        SPIN,

        /** Spin and nutation. */
        SPIN_NUTATION
    }

}
