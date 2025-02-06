/* Copyright 2024-2025 The Johns Hopkins University Applied Physics Laboratory
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
package org.orekit.files.iirv;

import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitInternalError;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.general.EphemerisFile;
import org.orekit.files.general.EphemerisFileWriter;
import org.orekit.utils.TimeStampedPVCoordinates;

import java.io.IOException;
import java.util.List;

/**
 * An {@link EphemerisFileWriter} for generating {@link IIRVMessage IIRV} files.
 * <p>
 * This class uses an inputted {@link IIRVBuilder} object to define the message metadata
 * values that comprise an IIRV message.
 * <p>
 * This class can be used to write a list of {@link TimeStampedPVCoordinates} as an IIRV file as follows:
 * <pre>{@code
 *
 * // 1. Create an IIRVBuilder class to define the spacecraft/mission metadata values
 * IIRVBuilder iirvBuilder = new IIRVBuilder(TimeScalesFactory.getUTC());
 * iirvBuilder.setSupportIdCode(1221);
 * iirvBuilder.setDragCoefficient(2.2);
 * iirvBuilder.setOriginIdentification(OriginIdentificationTerm.GSFC);
 * iirvBuilder.setRoutingIndicator("MANY");
 * // ... (additional fields here)
 *
 * // 2. Create an IIRVFileWriter with the builder object
 * IIRVFileWriter writer = new IIRVFileWriter(iirvBuilder, IIRVMessage.IncludeMessageMetadata.ALL_VECTORS);
 *
 * // 3. Generate an IIRVEphemerisFile containing the ephemeris data
 * IIRVEphemerisFile iirvFile = iirvBuilder.buildEphemerisFile(coordinates);
 *
 * // 4. Write to disk. Recommendation: embed the start year in the filename (year does not appear in the IIRV itself)
 * String testFilename = "TestSatellite" + "_" +
 *      iirvFile.getStartYear() + "_" +
 *      iirvFile.getIIRV().get(0).getDayOfYear().toEncodedString() + "_" +
 *      iirvFile.getIIRV().get(0).getVectorEpoch().toEncodedString() + ".iirv";
 * writer.write(testFilename, iirvFile);
 *  }
 * </pre>
 *
 * @author Nick LaFarge
 * @see StreamingIIRVFileWriter
 * @see IIRVMessage
 * @since 13.0
 */
public class IIRVFileWriter implements EphemerisFileWriter {

    /** Builder class for IIRV. */
    private final IIRVBuilder builder;

    /** Setting for when message metadata terms appear in the created IIRV message. */
    private final IIRVMessage.IncludeMessageMetadata includeMessageMetadataSetting;

    /**
     * Constructor.
     *
     * @param builder                       Builder class for IIRV
     * @param includeMessageMetadataSetting Setting for when message metadata terms appear in the created IIRV message
     */
    public IIRVFileWriter(final IIRVBuilder builder, final IIRVMessage.IncludeMessageMetadata includeMessageMetadataSetting) {
        this.builder = builder;
        this.includeMessageMetadataSetting = includeMessageMetadataSetting;
    }

    /** {@inheritDoc} */
    @Override
    public <C extends TimeStampedPVCoordinates, S extends EphemerisFile.EphemerisSegment<C>> void write(final Appendable writer, final EphemerisFile<C, S> ephemerisFile) throws IOException {

        if (writer == null) {
            throw new OrekitIllegalArgumentException(OrekitMessages.NULL_ARGUMENT, "writer");
        }

        if (ephemerisFile == null) {
            return;
        }
        final EphemerisFile.SatelliteEphemeris<C, S> satEphem = ephemerisFile.getSatellites().get(builder.getSatelliteID());

        final List<S> segments = satEphem.getSegments();
        if (segments.size() > 1) {
            // This should never happen
            throw new OrekitInternalError(null);
        }

        final StreamingIIRVFileWriter streamingWriter = new StreamingIIRVFileWriter(writer, includeMessageMetadataSetting);
        final IIRVMessage iirvMessage = builder.buildIIRVMessage(segments.get(0).getCoordinates());
        streamingWriter.writeIIRVMessage(iirvMessage);
    }
}
