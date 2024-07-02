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
package org.orekit.files.ccsds.ndm.odm.oem;

import java.io.IOException;

import org.hipparchus.exception.LocalizedCoreFormats;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.definitions.FrameFacade;
import org.orekit.files.ccsds.ndm.odm.OdmHeader;
import org.orekit.files.ccsds.utils.generation.Generator;
import org.orekit.frames.Frame;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.sampling.OrekitFixedStepHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.TimeStampedPVCoordinates;

/**
 * A writer for OEM files.
 *
 * <p> Each instance corresponds to a single OEM file. A new OEM ephemeris segment is
 * started by calling {@link #newSegment()}.
 *
 * <p>
 * The segments returned by this class can be used as step handlers for a {@link Propagator}.
 * </p>
 *
 * <pre>{@code
 * Propagator propagator = ...; // pre-configured propagator
 * OEMWriter  aemWriter  = ...; // pre-configured writer
 *   try (Generator out = ...;  // set-up output stream
 *        StreamingOemWriter sw = new StreamingOemWriter(out, oemWriter, header, metadata)) { // set-up streaming writer
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
 * @author Evan Ward
 * @see <a href="https://public.ccsds.org/Pubs/502x0b2c1.pdf">CCSDS 502.0-B-2 Orbit Data
 *      Messages</a>
 * @see <a href="https://public.ccsds.org/Pubs/500x0g4.pdf">CCSDS 500.0-G-4 Navigation
 *      Data Definitions and Conventions</a>
 * @see OemWriter
 */
public class StreamingOemWriter implements AutoCloseable {

    /** Generator for OEM output. */
    private final Generator generator;

    /** Writer for the OEM message format. */
    private final OemWriter writer;

    /** Header. */
    private final OdmHeader header;

    /** Current metadata. */
    private final OemMetadata metadata;

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
     * @param generator generator for OEM output
     * @param writer    writer for the AEM message format
     * @param header    file header (may be null)
     * @param template  template for metadata
     * @since 11.0
     * @see #StreamingOemWriter(Generator, OemWriter, OdmHeader, OemMetadata, boolean)
     */
    public StreamingOemWriter(final Generator generator, final OemWriter writer,
                              final OdmHeader header, final OemMetadata template) {
        this(generator, writer, header, template, true);
    }

    /**
     * Construct a writer that writes position, velocity, and acceleration at
     * each time step.
     *
     * @param generator        generator for OEM output
     * @param writer           writer for the AEM message format
     * @param header           file header (may be null)
     * @param template         template for metadata
     * @param useAttitudeFrame if {@code true} then the reference frame for each
     *                         segment is taken from the first state's attitude.
     *                         Otherwise the {@code template}'s reference frame
     *                         is used, {@link OemMetadata#getReferenceFrame()}.
     * @see #StreamingOemWriter(Generator, OemWriter, OdmHeader, OemMetadata, boolean, boolean)
     * @since 11.1.2
     */
    public StreamingOemWriter(final Generator generator, final OemWriter writer,
                              final OdmHeader header, final OemMetadata template,
                              final boolean useAttitudeFrame) {
        this(generator, writer, header, template, useAttitudeFrame, true);
    }

    /**
     * Simple constructor.
     *
     * @param generator           generator for OEM output
     * @param writer              writer for the AEM message format
     * @param header              file header (may be null)
     * @param template            template for metadata
     * @param useAttitudeFrame    if {@code true} then the reference frame for
     *                            each segment is taken from the first state's
     *                            attitude. Otherwise the {@code template}'s
     *                            reference frame is used, {@link
     *                            OemMetadata#getReferenceFrame()}.
     * @param includeAcceleration if {@code true} then acceleration is included
     *                            in the OEM file produced. Otherwise only
     *                            position and velocity is included.
     * @since 11.1.2
     */
    public StreamingOemWriter(final Generator generator, final OemWriter writer,
                              final OdmHeader header, final OemMetadata template,
                              final boolean useAttitudeFrame,
                              final boolean includeAcceleration) {
        this.generator          = generator;
        this.writer             = writer;
        this.header             = header;
        this.metadata           = template.copy(header == null ? writer.getDefaultVersion() : header.getFormatVersion());
        this.useAttitudeFrame   = useAttitudeFrame;
        this.includeAcceleration = includeAcceleration;
        this.headerWritePending = true;
    }

    /**
     * Create a writer for a new OEM ephemeris segment.
     * <p> The returned writer can only write a single ephemeris segment in an OEM.
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

        /** Empty constructor.
         * <p>
         * This constructor is not strictly necessary, but it prevents spurious
         * javadoc warnings with JDK 18 and later.
         * </p>
         * @since 12.0
         */
        public SegmentWriter() {
            // nothing to do
        }

        /**
         * {@inheritDoc}
         *
         * <p>Writes the header automatically on first segment.
         * Sets the {@link OemMetadataKey#START_TIME} and {@link OemMetadataKey#STOP_TIME} in this
         * segment's metadata if not already set by the user.
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
                    // we write the header only for the first segment
                    writer.writeHeader(generator, header);
                    headerWritePending = false;
                }

                metadata.setStartTime(date);
                metadata.setUseableStartTime(null);
                metadata.setUseableStopTime(null);
                metadata.setStopTime(t);
                if (useAttitudeFrame) {
                    frame = s0.getAttitude().getReferenceFrame();
                    metadata.setReferenceFrame(FrameFacade.map(frame));
                } else {
                    frame = metadata.getFrame();
                }
                writer.writeMetadata(generator, metadata);
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
