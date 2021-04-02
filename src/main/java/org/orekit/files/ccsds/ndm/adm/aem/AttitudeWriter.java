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

import java.io.IOException;
import java.util.List;

import org.orekit.data.DataContext;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.definitions.FrameFacade;
import org.orekit.files.ccsds.section.Header;
import org.orekit.files.ccsds.utils.FileFormat;
import org.orekit.files.ccsds.utils.generation.Generator;
import org.orekit.files.ccsds.utils.generation.KvnGenerator;
import org.orekit.files.ccsds.utils.generation.XmlGenerator;
import org.orekit.files.general.AttitudeEphemerisFile;
import org.orekit.files.general.AttitudeEphemerisFile.SatelliteAttitudeEphemeris;
import org.orekit.files.general.AttitudeEphemerisFileWriter;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.TimeStampedAngularCoordinates;

/** An {@link AttitudeEphemerisFileWriter} generating {@link AemFile AEM} files.
 * @author Bryan Cazabonne
 * @since 11.0
 */
public class AttitudeWriter implements AttitudeEphemerisFileWriter {

    /** Underlying writer. */
    private final AemWriter writer;

    /** Header. */
    private final Header header;

    /** Current metadata. */
    private final AemMetadata metadata;

    /** File format to use. */
    private final FileFormat fileFormat;

    /** Output name for error messages. */
    private final String outputName;

    /**
     * Constructor used to create a new AEM writer configured with the necessary parameters
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
     * <p>
     * Calling this constructor directly is not recommended. Users should rather use
     * {@link org.orekit.files.ccsds.ndm.WriterBuilder#buildA
     * writerBuilder.buildAemWriter(template, fileName)}.
     * </p>
     * @param writer underlying writer
     * @param header file header (may be null)
     * @param template template for metadata
     * @param fileFormat file format to use
     * @param outputName output name for error messages
     * @since 11.0
     */
    public AttitudeWriter(final AemWriter writer,
                          final Header header, final AemMetadata template,
                          final FileFormat fileFormat, final String outputName) {
        this.writer     = writer;
        this.header     = header;
        this.metadata   = template.copy();
        this.fileFormat = fileFormat;
        this.outputName = outputName;
    }

    /** Get current metadata.
     * @return current metadata
     */
    AemMetadata getMetadata() {
        return metadata;
    }

    /** {@inheritDoc}
     * <p>
     * As {@link AttitudeEphemerisFile.SatelliteAttitudeEphemeris} does not have all the entries
     * from {@link AemMetadata}, the only values that will be extracted from the
     * {@code ephemerisFile} will be the start time, stop time, reference frame, interpolation
     * method and interpolation degree. The missing values (like object name, local spacecraft
     * body frame, attitude type...) will be inherited from the template  metadata set at writer
     * {@link #AEMWriter(IERSConventions, DataContext, Header, AemMetadata, String, String) construction}.
     * </p>
     */
    @Override
    public <C extends TimeStampedAngularCoordinates, S extends AttitudeEphemerisFile.AttitudeEphemerisSegment<C>>
        void write(final Appendable appendable, final AttitudeEphemerisFile<C, S> ephemerisFile)
        throws IOException {

        if (appendable == null) {
            throw new OrekitIllegalArgumentException(OrekitMessages.NULL_ARGUMENT, "writer");
        }

        if (ephemerisFile == null) {
            return;
        }

        final SatelliteAttitudeEphemeris<C, S> satEphem =
                        ephemerisFile.getSatellites().get(metadata.getObjectID());
        if (satEphem == null) {
            throw new OrekitIllegalArgumentException(OrekitMessages.VALUE_NOT_FOUND,
                                                     metadata.getObjectID(), "ephemerisFile");
        }

        // Get attitude ephemeris segments to output.
        final List<S> segments = satEphem.getSegments();
        if (segments.isEmpty()) {
            // No data -> No output
            return;
        }

        try (Generator generator = fileFormat == FileFormat.KVN ?
                                   new KvnGenerator(appendable, AemWriter.KVN_PADDING_WIDTH, outputName) :
                                   new XmlGenerator(appendable, XmlGenerator.DEFAULT_INDENT, outputName)) {

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
     * @param <C> type of the angular coordinates
     * @param <S> type of the segment
     * @throws IOException if any buffer writing operations fails
     */
    private <C extends TimeStampedAngularCoordinates, S extends AttitudeEphemerisFile.AttitudeEphemerisSegment<C>>
        void writeSegment(final Generator generator, final S segment) throws IOException {

        // override template metadata with segment values
        metadata.setStartTime(segment.getStart());
        metadata.setStopTime(segment.getStop());
        if (metadata.getEndpoints().getFrameA() == null ||
            metadata.getEndpoints().getFrameA().asSpacecraftBodyFrame() == null) {
            // the external frame must be frame A
            metadata.getEndpoints().setFrameA(FrameFacade.map(segment.getReferenceFrame()));
        } else {
            // the external frame must be frame B
            metadata.getEndpoints().setFrameB(FrameFacade.map(segment.getReferenceFrame()));
        }
        metadata.setInterpolationMethod(segment.getInterpolationMethod());
        metadata.setInterpolationDegree(segment.getInterpolationSamples() - 1);
        writer.writeMetadata(generator, metadata);

        // Loop on attitude data
        writer.startAttitudeBlock(generator);
        if (segment instanceof AemSegment) {
            generator.writeComments(((AemSegment) segment).getData().getComments());
        }
        for (final TimeStampedAngularCoordinates coordinates : segment.getAngularCoordinates()) {
            writer.writeAttitudeEphemerisLine(generator, metadata, coordinates);
        }
        writer.endAttitudeBlock(generator);

    }

}