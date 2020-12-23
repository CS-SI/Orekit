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
package org.orekit.files.ilrs;

import java.io.IOException;
import java.util.List;

import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.general.EphemerisFile;
import org.orekit.files.general.EphemerisFile.EphemerisSegment;
import org.orekit.files.general.EphemerisFileWriter;
import org.orekit.files.ilrs.StreamingCpfWriter.Segment;
import org.orekit.time.TimeScale;
import org.orekit.utils.TimeStampedPVCoordinates;

/**
 * An CPF Writer class that can take in a general {@link EphemerisFile} object
 * and export it as a valid CPF file.
 * <p>
 * It supports both 1.0 and 2.0 versions
 * <p>
 * <b>Note:</b> By default, only required header keys are wrote (H1 and H2).
 * Furthermore, only position data can be written.
 * Other keys (i.e. in header and other types of ephemeris entries) are simply ignored.
 * Contributions are welcome to support more fields in the format.
 * @author Bryan Cazabonne
 * @since 10.3
 * @see <a href="https://ilrs.gsfc.nasa.gov/docs/2006/cpf_1.01.pdf">1.0 file format</a>
 * @see <a href="https://ilrs.gsfc.nasa.gov/docs/2018/cpf_2.00h-1.pdf">2.0 file format</a>
 */
public class CPFWriter implements EphemerisFileWriter {

    /** Container for header data. */
    private final CPFHeader header;

    /**
     * Constructor.
     * @param header container for header data
     */
    public CPFWriter(final CPFHeader header) {
        this.header = header;
    }


    /** {@inheritDoc} */
    @Override
    public void write(final Appendable writer, final EphemerisFile ephemerisFile)
        throws IOException {

        // Verify if writer is not a null object
        if (writer == null) {
            throw new OrekitIllegalArgumentException(OrekitMessages.NULL_ARGUMENT, "writer");
        }

        // Verify if the populated ephemeris file to serialize into the buffer is not null
        if (ephemerisFile == null) {
            return;
        }

        // Get satellite and ephemeris segments to output.
        final EphemerisFile.SatelliteEphemeris satEphem = ephemerisFile.getSatellites().get(header.getIlrsSatelliteId());
        final List<? extends EphemerisSegment> segments = satEphem.getSegments();
        final EphemerisSegment firstSegment = segments.get(0);

        // Time scale
        final TimeScale timeScale = firstSegment.getTimeScale();

        // Writer
        final StreamingCpfWriter cpfWriter =
                        new StreamingCpfWriter(writer, timeScale, header);
        // Write header
        cpfWriter.writeHeader();

        // Loop on ephemeris segments
        for (final EphemerisSegment segment : segments) {
            final Segment segmentWriter = cpfWriter.newSegment(header.getRefFrame());
            // Loop on coordiates
            for (final TimeStampedPVCoordinates coordinates : segment.getCoordinates()) {
                segmentWriter.writeEphemerisLine(coordinates);
            }
        }

        // Write end of file
        cpfWriter.writeEndOfFile();

    }

}
