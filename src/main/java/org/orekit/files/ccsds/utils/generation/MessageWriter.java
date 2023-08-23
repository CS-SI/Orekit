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
package org.orekit.files.ccsds.utils.generation;

import java.io.IOException;

import org.orekit.files.ccsds.ndm.NdmConstituent;
import org.orekit.files.ccsds.section.Header;
import org.orekit.files.ccsds.section.Segment;

/**
 * Interface for writing Navigation Data Message (NDM) files.
 * @param <H> type of the header
 * @param <S> type of the segments
 * @param <F> type of the file
 * @author Luc Maisonobe
 * @since 11.0
 */
public interface MessageWriter<H extends Header, S extends Segment<?, ?>, F extends NdmConstituent<H, S>> {

    /** Write one complete message.
     * @param generator generator to use for producing output
     * @param message message to write
     * @throws IOException if the stream cannot write to stream
     */
    default void writeMessage(final Generator generator, final F message)
        throws IOException {
        writeHeader(generator, message.getHeader());
        for (final S segment : message.getSegments()) {
            writeSegment(generator, segment);
        }
        writeFooter(generator);
    }

    /** Write header for the file.
     * @param generator generator to use for producing output
     * @param header header to write (creation date and originator will be added if missing)
     * @throws IOException if the stream cannot write to stream
     */
    void writeHeader(Generator generator, H header) throws IOException;

    /** Write one segment.
     * @param generator generator to use for producing output
     * @param segment segment to write
     * @throws IOException if any buffer writing operations fails
     */
    void writeSegment(Generator generator, S segment) throws IOException;

    /** Write footer for the file.
     * @param generator generator to use for producing output
     * @throws IOException if the stream cannot write to stream
     */
    void writeFooter(Generator generator) throws IOException;

    /** Get root element for XML files.
     * @return root element for XML files
     * @since 12.0
     */
    String getRoot();

    /** Get key for format version.
     * @return key for format version
     * @since 12.0
     */
    String getFormatVersionKey();

    /** Get current format version.
     * @return current format version
     * @since 12.0
     */
    double getVersion();

}
