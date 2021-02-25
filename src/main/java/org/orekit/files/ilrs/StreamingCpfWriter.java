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
package org.orekit.files.ilrs;

import java.io.IOException;
import java.util.Locale;

import org.hipparchus.exception.LocalizedCoreFormats;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.sampling.OrekitFixedStepHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateTimeComponents;
import org.orekit.time.TimeScale;
import org.orekit.utils.TimeStampedPVCoordinates;

/**
 * A writer for CPF files.
 *
 * <p> Each instance corresponds to a single CPF file.
 *
 * <p> This class can be used as a step handler for a {@link Propagator}.
 * The following example shows its use as a step handler.
 *
 * <p>
 * <b>Note:</b> By default, only required header keys are wrote (H1 and H2). Furthermore, only position data can be written.
 * Other keys (optionals) are simply ignored.
 * Contributions are welcome to support more fields in the format.
 *
 * @author Bryan Cazabonne
 * @since 10.3
 */
public class StreamingCpfWriter {

    /** New line separator for output file. */
    private static final String NEW_LINE = "\n";

    /** String A2 Format. */
    private static final String A2 = "%2s";

    /** String A3 Format. */
    private static final String A3 = "%3s";

    /** String A4 Format. */
    private static final String A4 = "%4s";

    /** String A8 Format. */
    private static final String A8 = "%8s";

    /** String A10 Format. */
    private static final String A10 = "%10s";

    /** Integer I1 Format. */
    private static final String I1 = "%1d";

    /** Integer I2 Format. */
    private static final String I2 = "%2d";

    /** Integer I3 Format. */
    private static final String I3 = "%3d";

    /** Integer I4 Format. */
    private static final String I4 = "%4d";

    /** Integer I5 Format. */
    private static final String I5 = "%5d";

    /** Real 13.6 Format. */
    private static final String F13_6 = "%13.6f";

    /** Real 17.3 Format. */
    private static final String F17_3 = "%17.3f";

    /** Space. */
    private static final String SPACE = " ";

    /** File format. */
    private static final String FORMAT = "CPF";

    /** Default locale. */
    private static final Locale STANDARDIZED_LOCALE = Locale.US;

    /** Default value for direction flag in position record. */
    private static final int DEFAULT_DIRECTION_FLAG = 0;

    /** Output stream. */
    private final Appendable writer;

    /** Time scale for all dates. */
    private final TimeScale timeScale;

    /** Container for header data. */
    private final CPFHeader header;

    /**
     * Create an OEM writer than streams data to the given output stream.
     *
     * @param writer     the output stream for the CPF file.
     * @param timeScale  for all times in the CPF
     * @param header     container for header data
     */
    public StreamingCpfWriter(final Appendable writer,
                              final TimeScale timeScale,
                              final CPFHeader header) {

        this.writer     = writer;
        this.timeScale  = timeScale;
        this.header     = header;
    }

    /**
     * Writes the CPF header for the file.
     * @throws IOException if the stream cannot write to stream
     */
    public void writeHeader() throws IOException {

        // Write H1
        HeaderLineWriter.H1.write(header, writer, timeScale);
        writer.append(NEW_LINE);

        // Write H2
        HeaderLineWriter.H2.write(header, writer, timeScale);
        writer.append(NEW_LINE);

        // End of header
        writer.append("H9");
        writer.append(NEW_LINE);

    }

    /**
     * Write end of file.
     * @throws IOException if the stream cannot write to stream
     */
    public void writeEndOfFile() throws IOException {
        writer.append("99");
    }

    /**
     * Create a writer for a new CPF ephemeris segment.
     * <p>
     * The returned writer can only write a single ephemeris segment in a CPF.
     * </p>
     * @param frame the reference frame to use for the segment. If this value is
     *              {@code null} then {@link Segment#handleStep(SpacecraftState,
     *              boolean)} will throw a {@link NullPointerException}.
     * @return a new CPF segment, ready for writing.
     */
    public Segment newSegment(final Frame frame) {
        return new Segment(frame);
    }

    /**
     * Write a String value in the file.
     * @param cpfWriter writer
     * @param format format
     * @param value value
     * @throws IOException if value cannot be written
     */
    private static void writeValue(final Appendable cpfWriter, final String format, final String value)
        throws IOException {
        cpfWriter.append(String.format(STANDARDIZED_LOCALE, format, value)).append(SPACE);
    }

    /**
     * Write a integer value in the file.
     * @param cpfWriter writer
     * @param format format
     * @param value value
     * @throws IOException if value cannot be written
     */
    private static void writeValue(final Appendable cpfWriter, final String format, final int value)
        throws IOException {
        cpfWriter.append(String.format(STANDARDIZED_LOCALE, format, value)).append(SPACE);
    }

    /**
     * Write a real value in the file.
     * @param cpfWriter writer
     * @param format format
     * @param value value
     * @throws IOException if value cannot be written
     */
    private static void writeValue(final Appendable cpfWriter, final String format, final double value)
        throws IOException {
        cpfWriter.append(String.format(STANDARDIZED_LOCALE, format, value)).append(SPACE);
    }

    /**
     * Write a String value in the file.
     * @param cpfWriter writer
     * @param format format
     * @param value value
     * @throws IOException if value cannot be writtent
     */
    private static void writeValue(final Appendable cpfWriter, final String format, final boolean value)
        throws IOException {
        // Change to an integer value
        final int intValue = value ? 1 : 0;
        writeValue(cpfWriter, format, intValue);
    }

    /** A writer for a segment of a CPF. */
    public class Segment implements OrekitFixedStepHandler {

        /** Reference frame of the output states. */
        private final Frame frame;

        /**
         * Create a new segment writer.
         *
         * @param frame    for the output states. Used by {@link #handleStep(SpacecraftState,
         *                 boolean)}.
         */
        private Segment(final Frame frame) {
            this.frame = frame;
        }

        /** {@inheritDoc}. */
        @Override
        public void handleStep(final SpacecraftState currentState, final boolean isLast) {
            try {

                // Write ephemeris line
                writeEphemerisLine(currentState.getPVCoordinates(frame));

                // Write end of file
                if (isLast) {
                    writeEndOfFile();
                }

            } catch (IOException e) {
                throw new OrekitException(e, LocalizedCoreFormats.SIMPLE_MESSAGE,
                                          e.getLocalizedMessage());
            }

        }

        /**
         * Write a single ephemeris line This method does not
         * write the velocity terms.
         *
         * @param pv the time, position, and velocity to write.
         * @throws IOException if the output stream throws one while writing.
         */
        public void writeEphemerisLine(final TimeStampedPVCoordinates pv)
            throws IOException {

            // Record type and direction flag
            writeValue(writer, A2, "10");
            writeValue(writer, I1, DEFAULT_DIRECTION_FLAG);

            // Epoch
            final AbsoluteDate epoch = pv.getDate();
            final DateTimeComponents dtc = epoch.getComponents(timeScale);
            writeValue(writer, I5, dtc.getDate().getMJD());
            writeValue(writer, F13_6, dtc.getTime().getSecondsInLocalDay());

            // Leap second flag (default 0)
            writeValue(writer, I2, 0);

            // Position
            final Vector3D position = pv.getPosition();
            writeValue(writer, F17_3, position.getX());
            writeValue(writer, F17_3, position.getY());
            writeValue(writer, F17_3, position.getZ());

            // New line
            writer.append(NEW_LINE);

        }

    }

    /** Writer for specific header lines. */
    public enum HeaderLineWriter {

        /** Header first line. */
        H1("H1") {

            /** {@inheritDoc} */
            @Override
            public void write(final CPFHeader cpfHeader, final Appendable cpfWriter,
                              final TimeScale utc) throws IOException {

                // write first keys
                writeValue(cpfWriter, A2, getIdentifier());
                writeValue(cpfWriter, A3, FORMAT);
                writeValue(cpfWriter, I2, cpfHeader.getVersion());
                writeValue(cpfWriter, A3, cpfHeader.getSource());
                writeValue(cpfWriter, I4, cpfHeader.getProductionEpoch().getYear());
                writeValue(cpfWriter, I2, cpfHeader.getProductionEpoch().getMonth());
                writeValue(cpfWriter, I2, cpfHeader.getProductionEpoch().getDay());
                writeValue(cpfWriter, I2, cpfHeader.getProductionHour());
                writeValue(cpfWriter, I3, cpfHeader.getSequenceNumber());

                // check file version
                if (cpfHeader.getVersion() == 2) {
                    writeValue(cpfWriter, I2, cpfHeader.getSubDailySequenceNumber());
                }

                // write last key
                writeValue(cpfWriter, A10, cpfHeader.getName());

            }

        },

        /** Header second line. */
        H2("H2") {

            /** {@inheritDoc} */
            @Override
            public void write(final CPFHeader cpfHeader, final Appendable cpfWriter,
                              final TimeScale utc) throws IOException {

                // write identifiers
                writeValue(cpfWriter, A2, getIdentifier());
                writeValue(cpfWriter, A8, cpfHeader.getIlrsSatelliteId());
                writeValue(cpfWriter, A4, cpfHeader.getSic());
                writeValue(cpfWriter, A8, cpfHeader.getNoradId());

                // write starting epoch
                final AbsoluteDate starting = cpfHeader.getStartEpoch();
                final DateTimeComponents dtcStart = starting.getComponents(utc);
                writeValue(cpfWriter, I4, dtcStart.getDate().getYear());
                writeValue(cpfWriter, I2, dtcStart.getDate().getMonth());
                writeValue(cpfWriter, I2, dtcStart.getDate().getDay());
                writeValue(cpfWriter, I2, dtcStart.getTime().getHour());
                writeValue(cpfWriter, I2, dtcStart.getTime().getMinute());
                writeValue(cpfWriter, I2, (int) dtcStart.getTime().getSecond());

                // write ending epoch
                final AbsoluteDate ending = cpfHeader.getStartEpoch();
                final DateTimeComponents dtcEnd = ending.getComponents(utc);
                writeValue(cpfWriter, I4, dtcEnd.getDate().getYear());
                writeValue(cpfWriter, I2, dtcEnd.getDate().getMonth());
                writeValue(cpfWriter, I2, dtcEnd.getDate().getDay());
                writeValue(cpfWriter, I2, dtcEnd.getTime().getHour());
                writeValue(cpfWriter, I2, dtcEnd.getTime().getMinute());
                writeValue(cpfWriter, I2, (int)  dtcEnd.getTime().getSecond());

                // write last keys
                writeValue(cpfWriter, I5, cpfHeader.getStep());
                writeValue(cpfWriter, I1, cpfHeader.isCompatibleWithTIVs());
                writeValue(cpfWriter, I1, cpfHeader.getTargetClass());
                writeValue(cpfWriter, I2, cpfHeader.getRefFrameId());
                writeValue(cpfWriter, I1, cpfHeader.getRotationalAngleType());
                writeValue(cpfWriter, I1, cpfHeader.isCenterOfMassCorrectionApplied());
                if (cpfHeader.getVersion() == 2) {
                    writeValue(cpfWriter, I1, cpfHeader.getTargetLocation());
                }

            }

        };

        /** Identifier. */
        private final String identifier;

        /** Simple constructor.
         * @param identifier regular expression for identifying line (i.e. first element)
         */
        HeaderLineWriter(final String identifier) {
            this.identifier = identifier;
        }

        /** Write a line.
         * @param cpfHeader container for header data
         * @param cpfWriter writer
         * @param utc utc time scale for dates
         * @throws IOException
         *             if any buffer writing operations fail or if the underlying
         *             format doesn't support a configuration in the file
         */
        public abstract void write(CPFHeader cpfHeader, Appendable cpfWriter, TimeScale utc)  throws IOException;

        /**
         * Get the regular expression for identifying line.
         * @return the regular expression for identifying line
         */
        public String getIdentifier() {
            return identifier;
        }

    }

}
