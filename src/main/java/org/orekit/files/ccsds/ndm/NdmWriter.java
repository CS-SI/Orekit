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
package org.orekit.files.ccsds.ndm;

import java.io.IOException;

import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.ndm.adm.aem.AemFile;
import org.orekit.files.ccsds.ndm.adm.apm.ApmFile;
import org.orekit.files.ccsds.ndm.odm.ocm.OcmFile;
import org.orekit.files.ccsds.ndm.odm.oem.OemFile;
import org.orekit.files.ccsds.ndm.odm.omm.OmmFile;
import org.orekit.files.ccsds.ndm.odm.opm.OpmFile;
import org.orekit.files.ccsds.ndm.tdm.TdmFile;
import org.orekit.files.ccsds.utils.generation.Generator;

/**
 * Writer for CCSDS Navigation Data Message.
 *
 * @author Luc Maisonobe
 * @since 11.0
 */
public class NdmWriter {

    /** Builder for the constituents writers. */
    private final WriterBuilder builder;

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
        this.count   = 0;
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

        // check we can still write comments
        if (count > 0) {
            throw new OrekitException(OrekitMessages.ATTEMPT_TO_GENERATE_MALFORMED_FILE, generator.getOutputName());
        }

        generator.writeEntry(NdmStructureKey.COMMENT.name(), comment, null, false);

    }

    /** Write a TDM constituent.
     * @param generator generator to use for producing output
     * @param tdmConstituent TDM constituent
     */
    public void writeTdmConstituent(final Generator generator, final TdmFile tdmConstituent) throws IOException {
        builder.buildTdmWriter().writeMessage(generator, tdmConstituent);
    }

    /** Write an OPM constituent.
     * @param generator generator to use for producing output
     * @param opmConstituent OPM constituent
     */
    public void writeOpmConstituent(final Generator generator, final OpmFile opmConstituent) throws IOException {
        builder.buildOpmWriter().writeMessage(generator, opmConstituent);
    }

    /** Write an OMM constituent.
     * @param generator generator to use for producing output
     * @param ommConstituent OMM constituent
     */
    public void writeOmmConstituent(final Generator generator, final OmmFile ommConstituent) throws IOException {
        builder.buildOmmWriter().writeMessage(generator, ommConstituent);
    }

    /** Write an OEM constituent.
     * @param generator generator to use for producing output
     * @param oemConstituent TDM constituent
     */
    public void writeOemConstituent(final Generator generator, final OemFile oemConstituent) throws IOException {
        builder.buildOemWriter().writeMessage(generator, oemConstituent);
    }

    /** Write an OCM constituent.
     * @param generator generator to use for producing output
     * @param ocmConstituent OCM constituent
     */
    public void writeOcmConstituent(final Generator generator, final OcmFile ocmConstituent) throws IOException {
        builder.buildOcmWriter().writeMessage(generator, ocmConstituent);
    }

    /** Write an APM constituent.
     * @param generator generator to use for producing output
     * @param apmConstituent APM constituent
     */
    public void writeApmConstituent(final Generator generator, final ApmFile apmConstituent) throws IOException {
        builder.buildApmWriter().writeMessage(generator, apmConstituent);
    }

    /** Write an AEM constituent.
     * @param generator generator to use for producing output
     * @param aemConstituent AEM constituent
     */
    public void writeAemConstituent(final Generator generator, final AemFile aemConstituent) throws IOException {
        builder.buildAemWriter().writeMessage(generator, aemConstituent);
    }

}
