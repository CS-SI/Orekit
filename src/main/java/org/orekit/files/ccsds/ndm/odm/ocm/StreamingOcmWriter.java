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

import org.hipparchus.exception.LocalizedCoreFormats;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.definitions.FrameFacade;
import org.orekit.files.ccsds.section.Header;
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
 * A new OCM ephemeris segment is started by calling {@link #newSegment()}.
 * </p>
 *
 * <p>
 * This writer is intended to write only trajectory state history blocks.
 * It does not writes physical properties, covariance data, maneuver data,
 * perturbations parameters, orbit determination or user-defined parameters.
 * If these blocks are needed, then {@link OcmWriter OcmWriter} must be
 * used as it handles all OCM data blocks.
 * </p>
 *
 * <p> This class can be used as a step handler for a {@link Propagator}.
 * </>
 *
 * <pre>{@code
 * Propagator propagator = ...; // pre-configured propagator
 * OCMWriter  ocmWriter  = ...; // pre-configured writer
 *   try (Generator out = ...;  // set-up output stream
 *        StreamingOcmWriter sw = new StreamingOcmWriter(out, ocmWriter)) { // set-up streaming writer
 *
 *     // write segment 1
 *     propagator.getMultiplexer().add(step, sw.newSegment());
 *     propagator.propagate(startDate1, stopDate1);
 *
 *     ...
 *
 *     // write segment n
 *     propagator.getMultiplexer().clear();
 *     propagator.getMultiplexer().add(step, sw.newSegment());
 *     propagator.propagate(startDateN, stopDateN);
 *
 *   }
 * }</pre>
 *
 *
 * @author Luc Maisonobe
 * @see ocmWriter
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
    private final Header header;

    /** Current metadata. */
    private final OcmMetadata metadata;

    /** Current trajectory metadata. */
    private final TrajectoryStateHistoryMetadata trajectoryMetadata;

    /** If the propagator's frame should be used. */
    private final boolean useAttitudeFrame;

    /** If acceleration should be included in the output. */
    private final boolean includeAcceleration;

    /** Indicator for writing header. */
    private boolean headerWritePending;

    /**
     * Construct a writer that for each segment uses the reference frame of the
     * first state's attitude.
     *
     * @param generator generator for OCM output
     * @param writer    writer for the OCM message format
     * @param header    file header (may be null)
     * @param metadata  file metadata
     * @param template  template for trajectory metadata
     * @see #StreamingOcmWriter(Generator, OcmWriter, Header, OcmMetadata, boolean)
     */
    public StreamingOcmWriter(final Generator generator, final OcmWriter writer,
                              final Header header, final OcmMetadata metadata,
                              final TrajectoryStateHistoryMetadata template) {
        this(generator, writer, header, metadata, template, true);
    }

    /**
     * Construct a writer that writes position, velocity, and acceleration at
     * each time step.
     *
     * @param generator        generator for OCM output
     * @param writer           writer for the OCM message format
     * @param header           file header (may be null)
     * @param metadata         file metadata
     * @param template         template for trajectory metadata
     * @param useAttitudeFrame if {@code true} then the reference frame for each
     *                         segment is taken from the first state's attitude.
     *                         Otherwise the {@code template}'s reference frame
     *                         is used, {@link TrajectoryStateHistoryMetadata#getTrajReferenceFrame()}.
     * @see #StreamingOcmWriter(Generator, OcmWriter, Header, OcmMetadata,
     * boolean, boolean)
     */
    public StreamingOcmWriter(final Generator generator, final OcmWriter writer,
                              final Header header, final OcmMetadata metadata,
                              final TrajectoryStateHistoryMetadata template,
                              final boolean useAttitudeFrame) {
        this(generator, writer, header, metadata, template, useAttitudeFrame, true);
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
     *                            TrajectoryStateHistoryMetadata#getTrajReferenceFrame()()}.
     * @param includeAcceleration if {@code true} then acceleration is included
     *                            in the OCM file produced. Otherwise only
     *                            position and velocity is included.
     */
    public StreamingOcmWriter(final Generator generator, final OcmWriter writer,
                              final Header header, final OcmMetadata metadata,
                              final TrajectoryStateHistoryMetadata template,
                              final boolean useAttitudeFrame,
                              final boolean includeAcceleration) {
        this.generator           = generator;
        this.writer              = writer;
        this.header              = header;
        this.metadata            = metadata;
        this.trajectoryMetadata  = template.copy(header == null ? writer.getDefaultVersion() : header.getFormatVersion());
        this.useAttitudeFrame    = useAttitudeFrame;
        this.includeAcceleration = includeAcceleration;
        this.headerWritePending  = true;
    }

    /**
     * Create a writer for a new OCM ephemeris segment.
     * <p> The returned writer can only write a single ephemeris segment in an OCM.
     * This method must be called to create a writer for each ephemeris segment.
     * @return a new OEM segment writer, ready for use.
     */
    public SegmentWriter newSegment() {
        return new SegmentWriter();
    }

    /** {@inheritDoc} */
    @Override
    public void close() throws IOException {
        writer.writeFooter(generator);
    }

    /** A writer for a segment of an OEM. */
    public class SegmentWriter implements OrekitFixedStepHandler {

        /** Reference frame of this segment. */
        private Frame frame;

        /**
         * {@inheritDoc}
         *
         * <p> Sets the {@link OcmMetadataKey#START_TIME} and {@link OcmMetadataKey#STOP_TIME} in this
         * segment's metadata if not already set by the user. Then calls {@link OcmWriter#writeHeader(Generator, Header)
         * writeHeader} if it is the first segment) and {@link OcmWriter#writeMetadata(Generator, OcmMetadata)}
         * to start the segment.
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
                    writer.writeMetadata(generator, metadata);
                    headerWritePending = false;
                }

                trajectoryWriter = TrajectoryStateHistoryWriter(history, getTimeConverter()).write(generator);

                // TODO: set up TRAJ_ID/TRAJ_PREV_ID/TRAJ_NEXT_ID

                trajectoryMetadata.setUseableStartTime(date);
                trajectoryMetadata.setUseableStopTime(t);
                if (useAttitudeFrame) {
                    frame = s0.getAttitude().getReferenceFrame();
                    trajectoryMetadata.setTrajReferenceFrame(FrameFacade.map(frame));
                } else {
                    frame = trajectoryMetadata.getTrajReferenceFrame().asFrame();
                }

                // TODO: set up ORB_REVNUM

                trajectoryWriter.writeMetadata(generator, trajectoryMetadata);
                writer.startData(generator);
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
                writer.writeOrbitEphemerisLine(generator, metadata, pv, includeAcceleration);
            } catch (IOException e) {
                throw new OrekitException(e, LocalizedCoreFormats.SIMPLE_MESSAGE, e.getLocalizedMessage());
            }
        }

        /** {@inheritDoc}. */
        @Override
        public void finish(final SpacecraftState finalState) {
            try {
                writer.endData(generator);
            } catch (IOException e) {
                throw new OrekitException(e, LocalizedCoreFormats.SIMPLE_MESSAGE, e.getLocalizedMessage());
            }
        }

    }

}
