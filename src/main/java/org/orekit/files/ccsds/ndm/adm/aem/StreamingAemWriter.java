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

import org.hipparchus.exception.LocalizedCoreFormats;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.definitions.FrameFacade;
import org.orekit.files.ccsds.section.Header;
import org.orekit.files.ccsds.utils.generation.Generator;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.sampling.OrekitFixedStepHandler;
import org.orekit.time.AbsoluteDate;

/**
 * A writer for AEM files.
 *
 * <p> Each instance corresponds to a single AEM file.
 *
 * <p> This class can be used as a step handler for a {@link Propagator}.
 *
 * <pre>{@code
 * Propagator propagator = ...; // pre-configured propagator
 * AEMWriter  aemWriter  = ...; // pre-configured writer
 *   try (Generator out = ...;) { // set-up output stream
 *     propagator.setMasterMode(step, new StreamingAemWriter(out, aemWriter).newSegment());
 *     propagator.propagate(startDate, stopDate);
 *   }
 * }</pre>
 * @author Bryan Cazabonne
 * @author Luc Maisonobe
 * @see <a href="https://public.ccsds.org/Pubs/504x0b1c1.pdf">CCSDS 504.0-B-1 Attitude Data Messages</a>
 * @see AemWriter
 * @since 10.2
 */
public class StreamingAemWriter {

    /** Generator for AEM output. */
    private final Generator generator;

    /** Writer for the AEM message format. */
    private final AemWriter writer;

    /** Header. */
    private final Header header;

    /** Current metadata. */
    private final AemMetadata metadata;

    /** Indicator for writing header. */
    private boolean headerWritePending;

    /** Simple constructor.
     * @param generator generator for AEM output
     * @param writer writer for the AEM message format
     * @param header file header (may be null)
     * @param template template for metadata
     * @since 11.0
     */
    public StreamingAemWriter(final Generator generator, final AemWriter writer,
                              final Header header, final AemMetadata template) {
        this.generator          = generator;
        this.writer             = writer;
        this.header             = header;
        this.metadata           = template.copy();
        this.headerWritePending = true;
    }

    /**
     * Create a writer for a new AEM attitude ephemeris segment.
     * <p> The returned writer can only write a single attitude ephemeris segment in an AEM.
     * This method must be called to create a writer for each attitude ephemeris segment.
     * @return a new AEM segment writer, ready for use.
     */
    public SegmentWriter newSegment() {
        return new SegmentWriter();
    }

    /** A writer for a segment of an AEM. */
    public class SegmentWriter implements OrekitFixedStepHandler {

        /**
         * {@inheritDoc}
         *
         * <p> Sets the {@link AemMetadataKey#START_TIME} and {@link AemMetadataKey#STOP_TIME} in this
         * segment's metadata if not already set by the user. Then calls {@link AemWriter#writeMessageHeader(Generator, Header)
         * writeHeader} if it is the first segment) and {@link AemWriter#writeMetadata(Generator, AemMetadata)} to start the segment.
         */
        @Override
        public void init(final SpacecraftState s0, final AbsoluteDate t, final double step) {
            try {
                if (t.isBefore(s0)) {
                    throw new OrekitException(OrekitMessages.NON_CHRONOLOGICALLY_SORTED_ENTRIES, s0.getDate(), t);
                }

                if (headerWritePending) {
                    // we write the header only for the first segment
                    writer.writeHeader(generator, header);
                    headerWritePending = false;
                }

                metadata.setStartTime(s0.getDate());
                metadata.setUseableStartTime(null);
                metadata.setUseableStopTime(null);
                metadata.setStopTime(t);
                if (metadata.getEndpoints().getFrameA() == null ||
                    metadata.getEndpoints().getFrameA().asSpacecraftBodyFrame() == null) {
                    // the external frame must be frame A
                    metadata.getEndpoints().setFrameA(FrameFacade.map(s0.getAttitude().getReferenceFrame()));
                } else {
                    // the external frame must be frame B
                    metadata.getEndpoints().setFrameB(FrameFacade.map(s0.getAttitude().getReferenceFrame()));
                }
                writer.writeMetadata(generator, metadata);
                writer.startAttitudeBlock(generator);
            } catch (IOException e) {
                throw new OrekitException(e, LocalizedCoreFormats.SIMPLE_MESSAGE, e.getLocalizedMessage());
            }
        }

        /** {@inheritDoc}. */
        @Override
        public void handleStep(final SpacecraftState currentState, final boolean isLast) {
            try {
                writer.writeAttitudeEphemerisLine(generator, metadata, currentState.getAttitude().getOrientation());
                if (isLast) {
                    writer.endAttitudeBlock(generator);
                }
            } catch (IOException e) {
                throw new OrekitException(e, LocalizedCoreFormats.SIMPLE_MESSAGE, e.getLocalizedMessage());
            }
        }

    }

}
