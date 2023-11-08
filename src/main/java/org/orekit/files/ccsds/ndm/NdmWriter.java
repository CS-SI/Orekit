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
package org.orekit.files.ccsds.ndm;

import java.io.IOException;

import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitInternalError;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.ndm.adm.acm.Acm;
import org.orekit.files.ccsds.ndm.adm.aem.Aem;
import org.orekit.files.ccsds.ndm.adm.apm.Apm;
import org.orekit.files.ccsds.ndm.odm.ocm.Ocm;
import org.orekit.files.ccsds.ndm.odm.oem.Oem;
import org.orekit.files.ccsds.ndm.odm.omm.Omm;
import org.orekit.files.ccsds.ndm.odm.opm.Opm;
import org.orekit.files.ccsds.ndm.tdm.Tdm;
import org.orekit.files.ccsds.section.Header;
import org.orekit.files.ccsds.section.Segment;
import org.orekit.files.ccsds.utils.generation.Generator;
import org.orekit.files.ccsds.utils.generation.MessageWriter;

/**
 * Writer for CCSDS Navigation Data Message.
 *
 * @author Luc Maisonobe
 * @since 11.0
 */
public class NdmWriter {

    /** Builder for the constituents writers. */
    private final WriterBuilder builder;

    /** Indicator for started message. */
    private boolean started;

    /** Number of constituents written. */
    private int count;

    /** Simple constructor.
     * <p>
     * Calling this constructor directly is not recommended. Users should rather use
     * {@link org.orekit.files.ccsds.ndm.WriterBuilder#buildNdmWriter()
     * WriterBuilder.buildNdmWriter()}.
     * </p>
     * @param builder builder for the constituents parsers
     */
    public NdmWriter(final WriterBuilder builder) {
        this.builder = builder;
        this.started = false;
        this.count   = 0;
    }

    /** Write one complete message.
     * @param generator generator to use for producing output
     * @param message message to write
     * @throws IOException if the stream cannot write to stream
     */
    public void writeMessage(final Generator generator, final Ndm message)
        throws IOException {

        // write the global comments
        for (final String comment : message.getComments()) {
            writeComment(generator, comment);
        }

        // write the constituents
        for (final NdmConstituent<?, ?> constituent : message.getConstituents()) {
            writeConstituent(generator, constituent);
        }

    }

    /** Start the composite message if needed.
     * @param generator generator to use for producing output
     * @throws IOException if the stream cannot write to stream
     */
    private void startMessageIfNeeded(final Generator generator) throws IOException {
        if (!started) {
            generator.enterSection(NdmStructureKey.ndm.name());
            started = true;
        }
    }

    /** Write a comment line.
     * <p>
     * Comments allows comments only before constituents, so attempting to
     * add comments after the first constituent has been written will
     * produce an exception.
     * </p>
     * @param generator generator to use for producing output
     * @param comment comment line to write
     * @throws IOException if the stream cannot write to stream
     */
    public void writeComment(final Generator generator, final String comment) throws IOException {

        startMessageIfNeeded(generator);

        // check we can still write comments
        if (count > 0) {
            throw new OrekitException(OrekitMessages.ATTEMPT_TO_GENERATE_MALFORMED_FILE, generator.getOutputName());
        }

        generator.writeEntry(NdmStructureKey.COMMENT.name(), comment, null, false);

    }

    /** Write a constituent.
     * @param generator generator to use for producing output
     * @param constituent constituent
     * @param <H> type of the header
     * @param <S> type of the segments
     * @param <F> type of the file
     * @throws IOException if the stream cannot write to stream
     */
    public <H extends Header, S extends Segment<?, ?>, F extends NdmConstituent<H, S>>
        void writeConstituent(final Generator generator, final F constituent) throws IOException {

        // write the root element if needed
        startMessageIfNeeded(generator);

        // write the constituent
        final MessageWriter<H, S, F> writer = buildWriter(constituent);
        writer.writeMessage(generator, constituent);

        // update count
        ++count;

    }

    /** Build writer for a constituent.
     * @param constituent constituent
     * @param <H> type of the header
     * @param <S> type of the segments
     * @param <F> type of the file
     * @return writer suited for the constituent
     * @throws IOException if the stream cannot write to stream
     */
    @SuppressWarnings("unchecked")
    private <H extends Header, S extends Segment<?, ?>, F extends NdmConstituent<H, S>>
        MessageWriter<H, S, F> buildWriter(final F constituent) throws IOException {
        if (constituent instanceof Tdm) {
            return (MessageWriter<H, S, F>) builder.buildTdmWriter();
        } else if (constituent instanceof Opm) {
            return (MessageWriter<H, S, F>) builder.buildOpmWriter();
        } else if (constituent instanceof Omm) {
            return (MessageWriter<H, S, F>) builder.buildOmmWriter();
        } else if (constituent instanceof Oem) {
            return (MessageWriter<H, S, F>) builder.buildOemWriter();
        } else if (constituent instanceof Ocm) {
            return (MessageWriter<H, S, F>) builder.buildOcmWriter();
        } else if (constituent instanceof Apm) {
            return (MessageWriter<H, S, F>) builder.buildApmWriter();
        } else if (constituent instanceof Aem) {
            return (MessageWriter<H, S, F>) builder.buildAemWriter();
        } else if (constituent instanceof Acm) {
            return (MessageWriter<H, S, F>) builder.buildAcmWriter();
        } else {
            // this should never happen
            throw new OrekitInternalError(null);
        }
    }

}
