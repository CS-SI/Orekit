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
import java.util.Map;

import org.hipparchus.exception.LocalizedCoreFormats;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.utils.CCSDSFrame;
import org.orekit.frames.Frame;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.sampling.OrekitFixedStepHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;

/**
 * A writer for AEM files.
 *
 * <p> Each instance corresponds to a single AEM file.
 *
 * @author Bryan Cazabonne
 * @author Luc Maisonobe
 * @see <a href="https://public.ccsds.org/Pubs/504x0b1c1.pdf">CCSDS 504.0-B-1 Attitude Data Messages</a>
 * @see AEMWriter
 * @since 10.2
 */
public class StreamingAemWriter {

    /** Output stream. */
    private final Appendable appendable;

    /** Writer for the AEM message format. */
    private final AEMWriter aemWriter;

    /** Indicator for writing header. */
    private boolean headerWritePending;

    /**
     * Create an AEM writer than streams data to the given output stream as
     * {@link #StreamingAemWriter(Appendable, TimeScale, Map)} with
     * {@link java.util.Formatter format parameters} for attitude ephemeris data.
     *
     * @param appendable    The output stream for the AEM file. Most methods will append data
     *                  to this {@code appendable}.
     * @param aemWriter writer for the AEM message format
     * @since 10.3
     */
    public StreamingAemWriter(final Appendable appendable, final AEMWriter aemWriter) {
        this.appendable         = appendable;
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
         * <p> Sets the {@link AEMMetadataKey#START_TIME} and {@link AEMMetadataKey#STOP_TIME} in this
         * segment's metadata if not already set by the user. Then calls {@link
         * #writeMetadata()} to start the segment.
         */
        @Override
        public void init(final SpacecraftState s0, final AbsoluteDate t, final double step) {
            try {
                if (t.isBefore(s0)) {
                    throw new OrekitException(OrekitMessages.NON_CHRONOLOGICALLY_SORTED_ENTRIES, s0.getDate(), t);
                }

                if (headerWritePending) {
                    // we write the header only for the first segment
                    aemWriter.writeHeader(appendable);
                    headerWritePending = false;
                }

                final AEMMetadata metadata = aemWriter.getMetadata();
                metadata.setStartTime(s0.getDate());
                metadata.setUseableStartTime(null);
                metadata.setUseableStopTime(null);
                metadata.setStopTime(t);
                final Frame      stateFrame = s0.getAttitude().getReferenceFrame();
                final CCSDSFrame ccsdsFrame = CCSDSFrame.map(stateFrame);
                metadata.getEndPoints().setExternalFrame(ccsdsFrame);
                aemWriter.writeMetadata(appendable);
                aemWriter.startAttitudeBlock(appendable);
            } catch (IOException e) {
                throw new OrekitException(e, LocalizedCoreFormats.SIMPLE_MESSAGE, e.getLocalizedMessage());
            }
        }

        /** {@inheritDoc}. */
        @Override
        public void handleStep(final SpacecraftState currentState, final boolean isLast) {
            try {
                aemWriter.writeAttitudeEphemerisLine(appendable, currentState.getAttitude().getOrientation());
                if (isLast) {
                    aemWriter.endAttitudeBlock(appendable);
                }
            } catch (IOException e) {
                throw new OrekitException(e, LocalizedCoreFormats.SIMPLE_MESSAGE, e.getLocalizedMessage());
            }
        }

    }

}
