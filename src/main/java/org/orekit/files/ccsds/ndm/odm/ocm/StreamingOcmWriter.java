/* Contributed in the public domain.
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
package org.orekit.files.ccsds.ndm.odm.ocm;

import java.io.IOException;
import java.util.Collections;

import org.hipparchus.exception.LocalizedCoreFormats;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.definitions.FrameFacade;
import org.orekit.files.ccsds.ndm.odm.OdmHeader;
import org.orekit.files.ccsds.section.XmlStructureKey;
import org.orekit.files.ccsds.utils.FileFormat;
import org.orekit.files.ccsds.utils.generation.Generator;
import org.orekit.frames.Frame;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.sampling.OrekitFixedStepHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.TimeStampedPVCoordinates;

/**
 * A writer for OCM files.
 *
 * <p> Each instance corresponds to a single Orbit Comprehensive Message.
 * A new OCM ephemeris trajectory state history block is started by calling
 * {@link #newBlock()}.
 * </p>
 *
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
 *
 * <p>
 * The blocks returned by this class can be used as step handlers for a {@link Propagator}.
 * </p>
 *
 * <pre>{@code
 * Propagator propagator = ...; // pre-configured propagator
 * OCMWriter  ocmWriter  = ...; // pre-configured writer
 *   try (Generator out = ...;  // set-up output stream
 *        StreamingOcmWriter sw = new StreamingOcmWriter(out, ocmWriter, header, metadata, template)) { // set-up streaming writer
 *
 *     // write block 1
 *     propagator.getMultiplexer().add(step, sw.newBlock());
 *     propagator.propagate(startDate1, stopDate1);
 *
 *     ...
 *
 *     // write block n
 *     propagator.getMultiplexer().clear();
 *     propagator.getMultiplexer().add(step, sw.newBlock());
 *     propagator.propagate(startDateN, stopDateN);
 *
 *   }
 * }</pre>
 *
 *
 * @author Luc Maisonobe
 * @see OcmWriter
 * @see EphemerisOcmWriter
 * @since 12.0
 */
public class StreamingOcmWriter implements AutoCloseable {

    /** Generator for OCM output. */
    private final Generator generator;

    /** Writer for the OCM message format. */
    private final OcmWriter writer;

    /** Writer for the trajectory data block. */
    private TrajectoryStateHistoryWriter trajectoryWriter;

    /** Header. */
    private final OdmHeader header;

    /** Current metadata. */
    private final OcmMetadata metadata;

    /** Current trajectory metadata. */
    private final TrajectoryStateHistoryMetadata trajectoryMetadata;

    /** If the propagator's frame should be used. */
    private final boolean useAttitudeFrame;

    /** Indicator for writing header. */
    private boolean headerWritePending;

    /** Last Z coordinate seen. */
    private double lastZ;

    /**
     * Construct a writer that for each segment uses the reference frame of the
     * first state's attitude.
     *
     * @param generator generator for OCM output
     * @param writer    writer for the OCM message format
     * @param header    file header (may be null)
     * @param metadata  file metadata
     * @param template  template for trajectory metadata
     * @see #StreamingOcmWriter(Generator, OcmWriter, OdmHeader, OcmMetadata, TrajectoryStateHistoryMetadata, boolean)
     */
    public StreamingOcmWriter(final Generator generator, final OcmWriter writer,
                              final OdmHeader header, final OcmMetadata metadata,
                              final TrajectoryStateHistoryMetadata template) {
        this(generator, writer, header, metadata, template, true);
    }

    /**
     * Simple constructor.
     *
     * @param generator           generator for OCM output
     * @param writer              writer for the OCM message format
     * @param header              file header (may be null)
     * @param metadata            file metadata
     * @param template            template for trajectory metadata
     * @param useAttitudeFrame    if {@code true} then the reference frame for
     *                            each segment is taken from the first state's
     *                            attitude. Otherwise the {@code template}'s
     *                            reference frame is used, {@link
     *                            TrajectoryStateHistoryMetadata#getTrajReferenceFrame()}.
     */
    public StreamingOcmWriter(final Generator generator, final OcmWriter writer,
                              final OdmHeader header, final OcmMetadata metadata,
                              final TrajectoryStateHistoryMetadata template,
                              final boolean useAttitudeFrame) {
        this.generator           = generator;
        this.writer              = writer;
        this.header              = header;
        this.metadata            = metadata;
        this.trajectoryMetadata  = template.copy(header == null ? writer.getDefaultVersion() : header.getFormatVersion());
        this.useAttitudeFrame    = useAttitudeFrame;
        this.headerWritePending  = true;
        this.lastZ               = Double.NaN;
    }

    /**
     * Create a writer for a new OCM trajectory state history block.
     * <p> The returned writer can only write a single trajectory state history block in an OCM.
     * This method must be called to create a writer for each trajectory state history block.
     * @return a new OCM trajectory state history block writer, ready for use.
     */
    public BlockWriter newBlock() {
        return new BlockWriter();
    }

    /** {@inheritDoc} */
    @Override
    public void close() throws IOException {
        writer.writeFooter(generator);
    }

    /** A writer for a trajectory state history block of an OCM. */
    public class BlockWriter implements OrekitFixedStepHandler {

        /** Reference frame of this segment. */
        private Frame frame;

        /** Elements type. */
        private OrbitElementsType type;

        /** Number of ascending nodes crossings. */
        private int crossings;

        /** Empty constructor.
         * <p>
         * This constructor is not strictly necessary, but it prevents spurious
         * javadoc warnings with JDK 18 and later.
         * </p>
         * @since 12.0
         */
        public BlockWriter() {
            // nothing to do
        }

        /**
         * {@inheritDoc}
         *
         * <p>Writes the header automatically on first segment.
         * Sets the {@link OcmMetadataKey#START_TIME} and {@link OcmMetadataKey#STOP_TIME} in this
         * block metadata if not already set by the user.
         */
        @Override
        public void init(final SpacecraftState s0, final AbsoluteDate t, final double step) {
            try {
                final AbsoluteDate date = s0.getDate();
                if (t.isBefore(date)) {
                    throw new OrekitException(OrekitMessages.NON_CHRONOLOGICALLY_SORTED_ENTRIES,
                            date, t, date.durationFrom(t));
                }

                if (headerWritePending) {
                    // we write the header and metadata only for the first segment
                    writer.writeHeader(generator, header);
                    if (generator.getFormat() == FileFormat.XML) {
                        generator.enterSection(XmlStructureKey.segment.name());
                    }
                    new OcmMetadataWriter(metadata, writer.getTimeConverter()).write(generator);
                    if (generator.getFormat() == FileFormat.XML) {
                        generator.enterSection(XmlStructureKey.data.name());
                    }
                    headerWritePending = false;
                }

                trajectoryMetadata.setTrajNextID(TrajectoryStateHistoryMetadata.incrementTrajID(trajectoryMetadata.getTrajID()));
                trajectoryMetadata.setUseableStartTime(date);
                trajectoryMetadata.setUseableStopTime(t);
                if (useAttitudeFrame) {
                    frame = s0.getAttitude().getReferenceFrame();
                    trajectoryMetadata.setTrajReferenceFrame(FrameFacade.map(frame));
                } else {
                    frame = trajectoryMetadata.getTrajReferenceFrame().asFrame();
                }

                crossings = 0;
                type      = trajectoryMetadata.getTrajType();

                final OneAxisEllipsoid body = trajectoryMetadata.getTrajType() == OrbitElementsType.GEODETIC ?
                                              new OneAxisEllipsoid(writer.getEquatorialRadius(),
                                                                   writer.getFlattening(),
                                                                   trajectoryMetadata.getTrajReferenceFrame().asFrame()) :
                                              null;
                trajectoryWriter = new TrajectoryStateHistoryWriter(new TrajectoryStateHistory(trajectoryMetadata,
                                                                                               Collections.emptyList(),
                                                                                               body, s0.getMu()),
                                                                    writer.getTimeConverter());
                trajectoryWriter.enterSection(generator);
                trajectoryWriter.writeMetadata(generator);

            } catch (IOException e) {
                throw new OrekitException(e, LocalizedCoreFormats.SIMPLE_MESSAGE, e.getLocalizedMessage());
            }
        }

        /** {@inheritDoc}. */
        @Override
        public void handleStep(final SpacecraftState currentState) {
            try {
                final TimeStampedPVCoordinates pv =
                        currentState.getPVCoordinates(frame);
                if (lastZ < 0.0 && pv.getPosition().getZ() >= 0.0) {
                    // we crossed ascending node
                    ++crossings;
                }
                lastZ = pv.getPosition().getZ();
                final TrajectoryState state = new TrajectoryState(type, pv.getDate(),
                                                                  type.toRawElements(pv, frame,
                                                                                     trajectoryWriter.getHistory().getBody(),
                                                                                     currentState.getMu()));
                trajectoryWriter.writeState(generator, state, type.getUnits());
            } catch (IOException e) {
                throw new OrekitException(e, LocalizedCoreFormats.SIMPLE_MESSAGE, e.getLocalizedMessage());
            }
        }

        /** {@inheritDoc}. */
        @Override
        public void finish(final SpacecraftState finalState) {
            try {

                trajectoryWriter.exitSection(generator);

                // update the trajectory IDs
                trajectoryMetadata.setTrajPrevID(trajectoryMetadata.getTrajID());
                trajectoryMetadata.setTrajID(trajectoryMetadata.getTrajNextID());

                if (trajectoryMetadata.getOrbRevNum() >= 0) {
                    // update the orbits revolution number
                    trajectoryMetadata.setOrbRevNum(trajectoryMetadata.getOrbRevNum() + crossings);
                }

            } catch (IOException e) {
                throw new OrekitException(e, LocalizedCoreFormats.SIMPLE_MESSAGE, e.getLocalizedMessage());
            }
        }

    }

}
