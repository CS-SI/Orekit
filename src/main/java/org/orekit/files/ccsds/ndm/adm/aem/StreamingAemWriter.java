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
 * AEMWriter  aemWriter  = ...; // pre-configured writer with header and metadata
 *   try (Generator out = ...;) { // set-up output stream
 *     propagator.setMasterMode(step, new StreamingAemWriter(out, oemWriter).newSegment());
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
    private final AemWriter aemWriter;

    /** Indicator for writing header. */
    private boolean headerWritePending;

    /** Simple constructor.
     * @param generator generator for AEM output
     * @param aemWriter writer for the AEM message format
     * @since 10.3
     */
    public StreamingAemWriter(final Generator generator, final AemWriter aemWriter) {
        this.generator          = generator;
        this.aemWriter          = aemWriter;
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
         * segment's metadata if not already set by the user. Then calls {@link AemWriter#writeHeader(Generator)
         * writeHeader} if it is the first segment) and {@link AemWriter#writeMetadata()} to start the segment.
         */
        @Override
        public void init(final SpacecraftState s0, final AbsoluteDate t, final double step) {
            try {
                if (t.isBefore(s0)) {
                    throw new OrekitException(OrekitMessages.NON_CHRONOLOGICALLY_SORTED_ENTRIES, s0.getDate(), t);
                }

                if (headerWritePending) {
                    // we write the header only for the first segment
                    aemWriter.writeHeader(generator, aemWriter.getHeader());
                    headerWritePending = false;
                }

                final AemMetadata metadata = aemWriter.getMetadata();
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
                aemWriter.writeMetadata(generator);
                aemWriter.startAttitudeBlock(generator);
            } catch (IOException e) {
                throw new OrekitException(e, LocalizedCoreFormats.SIMPLE_MESSAGE, e.getLocalizedMessage());
            }
        }

        /** {@inheritDoc}. */
        @Override
        public void handleStep(final SpacecraftState currentState, final boolean isLast) {
            try {
                aemWriter.writeAttitudeEphemerisLine(generator, currentState.getAttitude().getOrientation());
                if (isLast) {
                    aemWriter.endAttitudeBlock(generator);
                }
            } catch (IOException e) {
                throw new OrekitException(e, LocalizedCoreFormats.SIMPLE_MESSAGE, e.getLocalizedMessage());
            }
        }

    }

}
