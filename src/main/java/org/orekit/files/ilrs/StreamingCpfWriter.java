/* Copyright 2002-2023 CS GROUP
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
    private static final String A1 = "%1s";

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

    /** Real 19.6 Format. */
    private static final String F19_6 = "%19.6f";

    /** Space. */
    private static final String SPACE = " ";

    /** Empty string. */
    private static final String EMPTY_STRING = "";

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

    /** Flag for optional velocity record. */
    private final boolean velocityFlag;

    /**
     * Create a CPF writer than streams data to the given output stream.
     * <p>
     * Using this constructor, velocity data are not written.
     * </p>
     * @param writer     the output stream for the CPF file.
     * @param timeScale  for all times in the CPF
     * @param header     container for header data
     * @see #StreamingCpfWriter(Appendable, TimeScale, CPFHeader, boolean)
     */
    public StreamingCpfWriter(final Appendable writer,
                              final TimeScale timeScale,
                              final CPFHeader header) {
        this(writer, timeScale, header, false);
    }

    /**
     * Create a CPF writer than streams data to the given output stream.
     *
     * @param writer       the output stream for the CPF file.
     * @param timeScale    for all times in the CPF
     * @param header       container for header data
     * @param velocityFlag true if velocity must be written
     * @since 11.2
     */
    public StreamingCpfWriter(final Appendable writer,
                              final TimeScale timeScale,
                              final CPFHeader header,
                              final boolean velocityFlag) {
        this.writer       = writer;
        this.timeScale    = timeScale;
        this.header       = header;
        this.velocityFlag = velocityFlag;
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
     * @param frame the reference frame to use for the segment.
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
     * @param withSpace true if a space must be added
     * @throws IOException if value cannot be written
     */
    private static void writeValue(final Appendable cpfWriter, final String format,
                                   final String value, final boolean withSpace)
        throws IOException {
        cpfWriter.append(String.format(STANDARDIZED_LOCALE, format, value)).append(withSpace ? SPACE : EMPTY_STRING);
    }

    /**
     * Write a integer value in the file.
     * @param cpfWriter writer
     * @param format format
     * @param value value
     * @param withSpace true if a space must be added
     * @throws IOException if value cannot be written
     */
    private static void writeValue(final Appendable cpfWriter, final String format,
                                   final int value, final boolean withSpace)
        throws IOException {
        cpfWriter.append(String.format(STANDARDIZED_LOCALE, format, value)).append(withSpace ? SPACE : EMPTY_STRING);
    }

    /**
     * Write a real value in the file.
     * @param cpfWriter writer
     * @param format format
     * @param value value
     * @param withSpace true if a space must be added
     * @throws IOException if value cannot be written
     */
    private static void writeValue(final Appendable cpfWriter, final String format,
                                   final double value, final boolean withSpace)
        throws IOException {
        cpfWriter.append(String.format(STANDARDIZED_LOCALE, format, value)).append(withSpace ? SPACE : EMPTY_STRING);
    }

    /**
     * Write a String value in the file.
     * @param cpfWriter writer
     * @param format format
     * @param value value
     * @param withSpace true if a space must be added
     * @throws IOException if value cannot be written
     */
    private static void writeValue(final Appendable cpfWriter, final String format,
                                   final boolean value, final boolean withSpace)
        throws IOException {
        // Change to an integer value
        final int intValue = value ? 1 : 0;
        writeValue(cpfWriter, format, intValue, withSpace);
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
        public void handleStep(final SpacecraftState currentState) {
            try {

                // Write ephemeris line
                writeEphemerisLine(currentState.getPVCoordinates(frame));

            } catch (IOException e) {
                throw new OrekitException(e, LocalizedCoreFormats.SIMPLE_MESSAGE,
                                          e.getLocalizedMessage());
            }

        }

        /** {@inheritDoc}. */
        @Override
        public void finish(final SpacecraftState finalState) {
            try {
                // Write ephemeris line
                writeEphemerisLine(finalState.getPVCoordinates(frame));

                // Write end of file
                writeEndOfFile();

            } catch (IOException e) {
                throw new OrekitException(e, LocalizedCoreFormats.SIMPLE_MESSAGE,
                                          e.getLocalizedMessage());
            }

        }

        /**
         * Write ephemeris lines.
         * <p>
         * If <code>velocityFlag</code> is equals to true, both
         * position and velocity records are written. Otherwise,
         * only the position data are used.
         * </p>
         * @param pv the time, position, and velocity to write.
         * @throws IOException if the output stream throws one while writing.
         */
        public void writeEphemerisLine(final TimeStampedPVCoordinates pv)
            throws IOException {

            // Record type and direction flag
            writeValue(writer, A2, "10",                                    true);
            writeValue(writer, I1, DEFAULT_DIRECTION_FLAG,                  true);

            // Epoch
            final AbsoluteDate epoch = pv.getDate();
            final DateTimeComponents dtc = epoch.getComponents(timeScale);
            writeValue(writer, I5, dtc.getDate().getMJD(),                  true);
            writeValue(writer, F13_6, dtc.getTime().getSecondsInLocalDay(), true);

            // Leap second flag (default 0)
            writeValue(writer, I2, 0, true);

            // Position
            final Vector3D position = pv.getPosition();
            writeValue(writer, F17_3, position.getX(), true);
            writeValue(writer, F17_3, position.getY(), true);
            writeValue(writer, F17_3, position.getZ(), false);

            // New line
            writer.append(NEW_LINE);

            // Write the velocity record
            if (velocityFlag) {

                // Record type and direction flag
                writeValue(writer, A2, "20",                                    true);
                writeValue(writer, I1, DEFAULT_DIRECTION_FLAG,                  true);

                // Velocity
                final Vector3D velocity = pv.getVelocity();
                writeValue(writer, F19_6, velocity.getX(), true);
                writeValue(writer, F19_6, velocity.getY(), true);
                writeValue(writer, F19_6, velocity.getZ(), false);

                // New line
                writer.append(NEW_LINE);

            }

        }

    }

    /** Writer for specific header lines. */
    public enum HeaderLineWriter {

        /** Header first line. */
        H1("H1") {

            /** {@inheritDoc} */
            @Override
            public void write(final CPFHeader cpfHeader, final Appendable cpfWriter, final TimeScale timescale)
                throws IOException {

                // write first keys
                writeValue(cpfWriter, A2, getIdentifier(),                           true);
                writeValue(cpfWriter, A3, FORMAT,                                    true);
                writeValue(cpfWriter, I2, cpfHeader.getVersion(),                    true);
                writeValue(cpfWriter, A1, SPACE, false); // One additional column, see CPF v1 format
                writeValue(cpfWriter, A3, cpfHeader.getSource(),                     true);
                writeValue(cpfWriter, I4, cpfHeader.getProductionEpoch().getYear(),  true);
                writeValue(cpfWriter, I2, cpfHeader.getProductionEpoch().getMonth(), true);
                writeValue(cpfWriter, I2, cpfHeader.getProductionEpoch().getDay(),   true);
                writeValue(cpfWriter, I2, cpfHeader.getProductionHour(),             true);
                writeValue(cpfWriter, A1, SPACE, false); // One additional column, see CPF v1 format
                writeValue(cpfWriter, I3, cpfHeader.getSequenceNumber(),             true);

                // check file version
                if (cpfHeader.getVersion() == 2) {
                    writeValue(cpfWriter, I2, cpfHeader.getSubDailySequenceNumber(), true);
                }

                // write target name from official list
                writeValue(cpfWriter, A10, cpfHeader.getName(),                      true);

                // write notes (not supported yet)
                writeValue(cpfWriter, A10, SPACE,                                    false);
            }

        },

        /** Header second line. */
        H2("H2") {

            /** {@inheritDoc} */
            @Override
            public void write(final CPFHeader cpfHeader, final Appendable cpfWriter, final TimeScale timescale)
                throws IOException {

                // write identifiers
                writeValue(cpfWriter, A2, getIdentifier(),                                 true);
                writeValue(cpfWriter, A8, cpfHeader.getIlrsSatelliteId(),                  true);
                writeValue(cpfWriter, A4, cpfHeader.getSic(),                              true);
                writeValue(cpfWriter, A8, cpfHeader.getNoradId(),                          true);

                // write starting epoch
                final AbsoluteDate starting = cpfHeader.getStartEpoch();
                final DateTimeComponents dtcStart = starting.getComponents(timescale);
                writeValue(cpfWriter, I4, dtcStart.getDate().getYear(),                    true);
                writeValue(cpfWriter, I2, dtcStart.getDate().getMonth(),                   true);
                writeValue(cpfWriter, I2, dtcStart.getDate().getDay(),                     true);
                writeValue(cpfWriter, I2, dtcStart.getTime().getHour(),                    true);
                writeValue(cpfWriter, I2, dtcStart.getTime().getMinute(),                  true);
                writeValue(cpfWriter, I2, (int) dtcStart.getTime().getSecond(),            true);

                // write ending epoch
                final AbsoluteDate ending = cpfHeader.getEndEpoch();
                final DateTimeComponents dtcEnd = ending.getComponents(timescale);
                writeValue(cpfWriter, I4, dtcEnd.getDate().getYear(),                      true);
                writeValue(cpfWriter, I2, dtcEnd.getDate().getMonth(),                     true);
                writeValue(cpfWriter, I2, dtcEnd.getDate().getDay(),                       true);
                writeValue(cpfWriter, I2, dtcEnd.getTime().getHour(),                      true);
                writeValue(cpfWriter, I2, dtcEnd.getTime().getMinute(),                    true);
                writeValue(cpfWriter, I2, (int)  dtcEnd.getTime().getSecond(),             true);

                // write last keys
                writeValue(cpfWriter, I5, cpfHeader.getStep(),                             true);
                writeValue(cpfWriter, I1, cpfHeader.isCompatibleWithTIVs(),                true);
                writeValue(cpfWriter, I1, cpfHeader.getTargetClass(),                      true);
                writeValue(cpfWriter, I2, cpfHeader.getRefFrameId(),                       true);
                writeValue(cpfWriter, I1, cpfHeader.getRotationalAngleType(),              true);
                if (cpfHeader.getVersion() == 1) {
                    writeValue(cpfWriter, I1, cpfHeader.isCenterOfMassCorrectionApplied(), false);
                } else {
                    writeValue(cpfWriter, I1, cpfHeader.isCenterOfMassCorrectionApplied(), true);
                    writeValue(cpfWriter, I2, cpfHeader.getTargetLocation(),               false);
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
         * @param timescale time scale for dates
         * @throws IOException
         *             if any buffer writing operations fail or if the underlying
         *             format doesn't support a configuration in the file
         */
        public abstract void write(CPFHeader cpfHeader, Appendable cpfWriter, TimeScale timescale)  throws IOException;

        /**
         * Get the regular expression for identifying line.
         * @return the regular expression for identifying line
         */
        public String getIdentifier() {
            return identifier;
        }

    }

}
