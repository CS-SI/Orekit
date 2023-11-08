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
import java.util.List;

import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.definitions.FrameFacade;
import org.orekit.files.ccsds.ndm.odm.OdmHeader;
import org.orekit.files.ccsds.utils.FileFormat;
import org.orekit.files.ccsds.utils.generation.Generator;
import org.orekit.files.ccsds.utils.generation.KvnGenerator;
import org.orekit.files.ccsds.utils.generation.XmlGenerator;
import org.orekit.files.general.EphemerisFile;
import org.orekit.files.general.EphemerisFile.SatelliteEphemeris;
import org.orekit.files.general.EphemerisFileWriter;
import org.orekit.utils.CartesianDerivativesFilter;
import org.orekit.utils.TimeStampedPVCoordinates;

/** An {@link EphemerisFileWriter} generating {@link Oem OEM} files.
 * @author Hank Grabowski
 * @author Evan Ward
 * @since 9.0
 * @see <a href="https://public.ccsds.org/Pubs/502x0b2c1.pdf">CCSDS 502.0-B-2 Orbit Data
 *      Messages</a>
 * @see <a href="https://public.ccsds.org/Pubs/500x0g4.pdf">CCSDS 500.0-G-4 Navigation
 *      Data Definitions and Conventions</a>
 * @see StreamingOemWriter
 */
public class EphemerisOemWriter implements EphemerisFileWriter {

    /** Underlying writer. */
    private final OemWriter writer;

    /** Header. */
    private final OdmHeader header;

    /** Current metadata. */
    private final OemMetadata metadata;

    /** File format to use. */
    private final FileFormat fileFormat;

    /** Output name for error messages. */
    private final String outputName;

    /** Maximum offset for relative dates.
     * @since 12.0
     */
    private final double maxRelativeOffset;

    /** Column number for aligning units. */
    private final int unitsColumn;

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
     * </p>
     * @param writer underlying writer
     * @param header file header (may be null)
     * @param template template for metadata
     * @param fileFormat file format to use
     * @param outputName output name for error messages
     * @param maxRelativeOffset maximum offset in seconds to use relative dates
     * (if a date is too far from reference, it will be displayed as calendar elements)
     * @param unitsColumn columns number for aligning units (if negative or zero, units are not output)
     * @since 12.0
     */
    public EphemerisOemWriter(final OemWriter writer,
                              final OdmHeader header, final OemMetadata template,
                              final FileFormat fileFormat, final String outputName,
                              final double maxRelativeOffset, final int unitsColumn) {
        this.writer            = writer;
        this.header            = header;
        this.metadata          = template.copy(header == null ? writer.getDefaultVersion() : header.getFormatVersion());
        this.fileFormat        = fileFormat;
        this.outputName        = outputName;
        this.maxRelativeOffset = maxRelativeOffset;
        this.unitsColumn       = unitsColumn;
    }

    /** {@inheritDoc}
     * <p>
     * As {@code EphemerisFile.SatelliteEphemeris} does not have all the entries
     * from {@link OemMetadata}, the only values that will be extracted from the
     * {@code ephemerisFile} will be the start time, stop time, reference frame, interpolation
     * method and interpolation degree. The missing values (like object name, local spacecraft
     * body frame...) will be inherited from the template  metadata set at writer
     * {@link #EphemerisOemWriter(OemWriter, OdmHeader, OemMetadata, FileFormat, String, double, int) construction}.
     * </p>
     */
    @Override
    public <C extends TimeStampedPVCoordinates, S extends EphemerisFile.EphemerisSegment<C>>
        void write(final Appendable appendable, final EphemerisFile<C, S> ephemerisFile)
        throws IOException {

        if (appendable == null) {
            throw new OrekitIllegalArgumentException(OrekitMessages.NULL_ARGUMENT, "writer");
        }

        if (ephemerisFile == null) {
            return;
        }

        final SatelliteEphemeris<C, S> satEphem = ephemerisFile.getSatellites().get(metadata.getObjectID());
        if (satEphem == null) {
            throw new OrekitIllegalArgumentException(OrekitMessages.VALUE_NOT_FOUND,
                                                     metadata.getObjectID(), "ephemerisFile");
        }

        // Get ephemeris segments to output.
        final List<S> segments = satEphem.getSegments();
        if (segments.isEmpty()) {
            // No data -> No output
            return;
        }

        try (Generator generator = fileFormat == FileFormat.KVN ?
                                   new KvnGenerator(appendable, OemWriter.KVN_PADDING_WIDTH, outputName,
                                                    maxRelativeOffset, unitsColumn) :
                                   new XmlGenerator(appendable, XmlGenerator.DEFAULT_INDENT, outputName,
                                                    maxRelativeOffset, unitsColumn > 0, null)) {

            writer.writeHeader(generator, header);

            // Loop on segments
            for (final S segment : segments) {
                writeSegment(generator, segment);
            }

            writer.writeFooter(generator);

        }

    }

    /** Write one segment.
     * @param generator generator to use for producing output
     * @param segment segment to write
     * @param <C> type of the Cartesian coordinates
     * @param <S> type of the segment
     * @throws IOException if any buffer writing operations fails
     */
    public <C extends TimeStampedPVCoordinates, S extends EphemerisFile.EphemerisSegment<C>>
        void writeSegment(final Generator generator, final S segment) throws IOException {

        // override template metadata with segment values
        if (segment instanceof OemSegment) {
            final OemSegment oemSegment = (OemSegment) segment;
            metadata.setReferenceFrame(oemSegment.getMetadata().getReferenceFrame());
        } else {
            metadata.setReferenceFrame(FrameFacade.map(segment.getFrame()));
        }
        metadata.setStartTime(segment.getStart());
        metadata.setStopTime(segment.getStop());
        metadata.setInterpolationDegree(segment.getInterpolationSamples() - 1);
        writer.writeMetadata(generator, metadata);

        // we enter data section
        writer.startData(generator);

        if (segment instanceof OemSegment) {
            // write data comments
            generator.writeComments(((OemSegment) segment).getData().getComments());
        }

        // Loop on orbit data
        final CartesianDerivativesFilter filter = segment.getAvailableDerivatives();
        if (filter == CartesianDerivativesFilter.USE_P) {
            throw new OrekitException(OrekitMessages.MISSING_VELOCITY);
        }
        final boolean useAcceleration = filter.equals(CartesianDerivativesFilter.USE_PVA);
        for (final TimeStampedPVCoordinates coordinates : segment.getCoordinates()) {
            writer.writeOrbitEphemerisLine(generator, metadata, coordinates, useAcceleration);
        }

        if (segment instanceof OemSegment) {
            // output covariance data
            writer.writeCovariances(generator, metadata, ((OemSegment) segment).getCovarianceMatrices());
        }

        // we exit data section
        writer.endData(generator);

    }

}
