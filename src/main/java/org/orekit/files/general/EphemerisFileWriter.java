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
package org.orekit.files.general;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.orekit.utils.TimeStampedPVCoordinates;

/**
 * An interface for writing out ephemeris files to disk.
 *
 * <p>
 * An {@link EphemerisFile} consists of one or more satellites each an ID unique
 * within the file. The ephemeris for each satellite consists of one or more
 * segments.
 *
 * <p>
 * Ephemeris file formats may have additional settings that need to be
 * configured to be compliant with their formats.
 *
 * @author Hank Grabowski
 * @since 9.0
 *
 */
public interface EphemerisFileWriter {

    /**
     * Write the passed in {@link EphemerisFile} using the passed in
     * {@link Appendable}.
     *
     * @param writer
     *            a configured Appendable to feed with text
     * @param ephemerisFile
     *            a populated ephemeris file to serialize into the buffer
     * @param <C> type of the Cartesian coordinates
     * @param <S> type of the segment
     * @throws IOException
     *             if any buffer writing operations fail or if the underlying
     *             format doesn't support a configuration in the EphemerisFile
     *             (for example having multiple satellites in one file, having
     *             the origin at an unspecified celestial body, etc.)
     */
    <C extends TimeStampedPVCoordinates, S extends EphemerisFile.EphemerisSegment<C>>
        void write(Appendable writer, EphemerisFile<C, S> ephemerisFile) throws IOException;

    /**
     * Write the passed in {@link EphemerisFile} to a file at the output path
     * specified.
     *
     * @param outputFilePath
     *            a file path that the corresponding file will be written to
     * @param ephemerisFile
     *            a populated ephemeris file to serialize into the buffer
     * @param <C> type of the Cartesian coordinates
     * @param <S> type of the segment
     * @throws IOException
     *             if any file writing operations fail or if the underlying
     *             format doesn't support a configuration in the EphemerisFile
     *             (for example having multiple satellites in one file, having
     *             the origin at an unspecified celestial body, etc.)
     */
    default <C extends TimeStampedPVCoordinates, S extends EphemerisFile.EphemerisSegment<C>>
        void write(final String outputFilePath, EphemerisFile<C, S> ephemerisFile)
            throws IOException {
        try (BufferedWriter writer =
                        Files.newBufferedWriter(Paths.get(outputFilePath), StandardCharsets.UTF_8)) {
            write(writer, ephemerisFile);
        }
    }

}
