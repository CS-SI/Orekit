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
package org.orekit.files.ccsds.ndm.odm.ocm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.ndm.odm.OdmHeader;
import org.orekit.files.ccsds.section.XmlStructureKey;
import org.orekit.files.ccsds.utils.FileFormat;
import org.orekit.files.ccsds.utils.generation.Generator;
import org.orekit.files.ccsds.utils.generation.KvnGenerator;
import org.orekit.files.ccsds.utils.generation.XmlGenerator;
import org.orekit.files.general.EphemerisFile;
import org.orekit.files.general.EphemerisFile.SatelliteEphemeris;
import org.orekit.files.general.EphemerisFileWriter;
import org.orekit.frames.Frame;
import org.orekit.utils.TimeStampedPVCoordinates;

/** An {@link EphemerisFileWriter} generating {@link Ocm OCM} files.
 * <p>
 * This writer is intended to write only trajectory state history blocks.
 * It does not writes physical properties, covariance data, maneuver data,
 * perturbations parameters, orbit determination or user-defined parameters.
 * If these blocks are needed, then {@link OcmWriter OcmWriter} must be
 * used as it handles all OCM data blocks.
 * </p>
 * <p>
 * The trajectory blocks metadata identifiers ({@code TRAJ_ID},
 * {@code TRAJ_PREV_ID}, {@code TRAJ_NEXT_ID}) are updated automatically
 * using {@link TrajectoryStateHistoryMetadata#incrementTrajID(String)},
 * so users should generally only set {@link TrajectoryStateHistoryMetadata#setTrajID(String)}
 * in the template.
 * </p>
 * @author Luc Maisonobe
 * @since 12.0
 * @see OcmWriter
 * @see StreamingOcmWriter
 */
public class EphemerisOcmWriter implements EphemerisFileWriter {

    /** Underlying writer. */
    private final OcmWriter writer;

    /** Header. */
    private final OdmHeader header;

    /** File metadata. */
    private final OcmMetadata metadata;

    /** Current trajectory metadata. */
    private final TrajectoryStateHistoryMetadata trajectoryMetadata;

    /** File format to use. */
    private final FileFormat fileFormat;

    /** Output name for error messages. */
    private final String outputName;

    /** Column number for aligning units. */
    private final int unitsColumn;

    /** Maximum offset for relative dates. */
    private final double maxRelativeOffset;

    /** Central body.
     * @since 12.0
     */
    private final OneAxisEllipsoid body;

    /**
     * Constructor used to create a new OCM writer configured with the necessary parameters
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
     * @param metadata  file metadata
     * @param template  template for trajectory metadata
     * @param fileFormat file format to use
     * @param outputName output name for error messages
     * @param maxRelativeOffset maximum offset in seconds to use relative dates
     * (if a date is too far from reference, it will be displayed as calendar elements)
     * @param unitsColumn columns number for aligning units (if negative or zero, units are not output)
     */
    public EphemerisOcmWriter(final OcmWriter writer,
                              final OdmHeader header, final OcmMetadata metadata,
                              final TrajectoryStateHistoryMetadata template,
                              final FileFormat fileFormat, final String outputName,
                              final double maxRelativeOffset, final int unitsColumn) {
        this.writer             = writer;
        this.header             = header;
        this.metadata           = metadata.copy(header == null ? writer.getDefaultVersion() : header.getFormatVersion());
        this.trajectoryMetadata = template.copy(header == null ? writer.getDefaultVersion() : header.getFormatVersion());
        this.fileFormat         = fileFormat;
        this.outputName         = outputName;
        this.maxRelativeOffset  = maxRelativeOffset;
        this.unitsColumn        = unitsColumn;
        this.body               = Double.isNaN(writer.getEquatorialRadius()) ?
                                  null :
                                  new OneAxisEllipsoid(writer.getEquatorialRadius(),
                                                       writer.getFlattening(),
                                                       template.getTrajReferenceFrame().asFrame());
    }

    /** {@inheritDoc}
     * <p>
     * As {@code EphemerisFile.SatelliteEphemeris} does not have all the entries
     * from {@link OcmMetadata}, the only values that will be extracted from the
     * {@code ephemerisFile} will be the start time, stop time, reference frame, interpolation
     * method and interpolation degree. The missing values (like object name, local spacecraft
     * body frame...) will be inherited from the template  metadata set at writer
     * {@link #EphemerisOcmWriter(OcmWriter, OdmHeader, OcmMetadata, TrajectoryStateHistoryMetadata,
     * FileFormat, String, double, int) construction}.
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

        final String name;
        if (metadata.getObjectName() != null) {
            name = metadata.getObjectName();
        } else if (metadata.getInternationalDesignator() != null) {
            name = metadata.getInternationalDesignator();
        } else if (metadata.getObjectDesignator() != null) {
            name = metadata.getObjectDesignator();
        } else {
            name = Ocm.UNKNOWN_OBJECT;
        }
        final SatelliteEphemeris<C, S> satEphem = ephemerisFile.getSatellites().get(name);
        if (satEphem == null) {
            throw new OrekitIllegalArgumentException(OrekitMessages.VALUE_NOT_FOUND,
                                                     name, "ephemerisFile");
        }

        // Get trajectory blocks to output.
        final List<S> blocks = satEphem.getSegments();
        if (blocks.isEmpty()) {
            // No data -> No output
            return;
        }

        try (Generator generator = fileFormat == FileFormat.KVN ?
                                   new KvnGenerator(appendable, OcmWriter.KVN_PADDING_WIDTH, outputName,
                                                    maxRelativeOffset, unitsColumn) :
                                   new XmlGenerator(appendable, XmlGenerator.DEFAULT_INDENT, outputName,
                                                    maxRelativeOffset, unitsColumn > 0, null)) {

            writer.writeHeader(generator, header);

            if (generator.getFormat() == FileFormat.XML) {
                generator.enterSection(XmlStructureKey.segment.name());
            }

            // write single segment metadata
            metadata.setStartTime(blocks.get(0).getStart());
            metadata.setStopTime(blocks.get(blocks.size() - 1).getStop());
            new OcmMetadataWriter(metadata, writer.getTimeConverter()).write(generator);

            if (generator.getFormat() == FileFormat.XML) {
                generator.enterSection(XmlStructureKey.data.name());
            }

            // Loop on trajectory blocks
            double lastZ = Double.NaN;
            for (final S block : blocks) {

                // prepare metadata
                trajectoryMetadata.setTrajNextID(TrajectoryStateHistoryMetadata.incrementTrajID(trajectoryMetadata.getTrajID()));
                trajectoryMetadata.setUseableStartTime(block.getStart());
                trajectoryMetadata.setUseableStopTime(block.getStop());
                trajectoryMetadata.setInterpolationDegree(block.getInterpolationSamples() - 1);

                // prepare data
                final OrbitElementsType type      = trajectoryMetadata.getTrajType();
                final Frame             frame     = trajectoryMetadata.getTrajReferenceFrame().asFrame();
                int                     crossings = 0;
                final List<TrajectoryState> states = new ArrayList<>(block.getCoordinates().size());
                for (final C pv : block.getCoordinates()) {
                    if (lastZ < 0.0 && pv.getPosition().getZ() >= 0.0) {
                        // we crossed ascending node
                        ++crossings;
                    }
                    lastZ = pv.getPosition().getZ();
                    states.add(new TrajectoryState(type, pv.getDate(), type.toRawElements(pv, frame, body, block.getMu())));
                }
                final TrajectoryStateHistory history = new TrajectoryStateHistory(trajectoryMetadata, states,
                                                                                  body, block.getMu());

                // write trajectory block
                final TrajectoryStateHistoryWriter trajectoryWriter =
                                new TrajectoryStateHistoryWriter(history, writer.getTimeConverter());
                trajectoryWriter.write(generator);

                // update the trajectory IDs
                trajectoryMetadata.setTrajPrevID(trajectoryMetadata.getTrajID());
                trajectoryMetadata.setTrajID(trajectoryMetadata.getTrajNextID());

                if (trajectoryMetadata.getOrbRevNum() >= 0) {
                    // update the orbits revolution number
                    trajectoryMetadata.setOrbRevNum(trajectoryMetadata.getOrbRevNum() + crossings);
                }

            }

            if (generator.getFormat() == FileFormat.XML) {
                generator.exitSection(); // exit data
                generator.exitSection(); // exit segment
            }

            writer.writeFooter(generator);

        }

    }

}
