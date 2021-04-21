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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.section.CommentsContainer;
import org.orekit.files.ccsds.utils.FileFormat;
import org.orekit.files.ccsds.utils.lexical.ParseToken;
import org.orekit.files.ccsds.utils.lexical.XmlTokenBuilder;
import org.orekit.files.ccsds.utils.parsing.AbstractMessageParser;
import org.orekit.files.ccsds.utils.parsing.ErrorState;

/** A parser for the CCSDS NDM (Navigation Data Message).
 * @author Luc Maisonobe
 * @since 11.0
 */
public class NdmParser extends AbstractMessageParser<NdmFile> {

    /** Builder for the constituents parsers. */
    private final ParserBuilder builder;

    /** Current constituent parser. */
    private AbstractMessageParser<? extends NdmConstituent<?, ?>> constituentParser;

    /** Container for comments. */
    private CommentsContainer comments;

    /** Container for constituents. */
    private List<NdmConstituent<?, ?>> constituents;

    /** Simple constructor.
     * <p>
     * Calling this constructor directly is not recommended. Users should rather use
     * {@link org.orekit.files.ccsds.ndm.ParserBuilder#buildNdmParser()
     * parserBuilder.buildNdmParser()}.
     * </p>
     * @param builder builder for the constituents parsers
     */
    public NdmParser(final ParserBuilder builder) {
        super(NdmStructureKey.ndm.name(), null);
        this.builder = builder;
    }

    /** {@inheritDoc} */
    @Override
    public Map<String, XmlTokenBuilder> getSpecialXmlElementsBuilders() {
        final Map<String, XmlTokenBuilder> builders = super.getSpecialXmlElementsBuilders();

        // special handling of root elements for all constituents
        builders.putAll(builder.buildTdmParser().getSpecialXmlElementsBuilders());
        builders.putAll(builder.buildOpmParser().getSpecialXmlElementsBuilders());
        builders.putAll(builder.buildOmmParser().getSpecialXmlElementsBuilders());
        builders.putAll(builder.buildOemParser().getSpecialXmlElementsBuilders());
        builders.putAll(builder.buildOcmParser().getSpecialXmlElementsBuilders());
        builders.putAll(builder.buildApmParser().getSpecialXmlElementsBuilders());
        builders.putAll(builder.buildAemParser().getSpecialXmlElementsBuilders());

        return builders;

    }

    /** {@inheritDoc} */
    @Override
    public void reset(final FileFormat fileFormat) {
        reset(fileFormat, this::processStructureToken);
        setFallback(new ErrorState());
        constituentParser = null;
        comments          = new CommentsContainer();
        constituents      = new ArrayList<>();
    }

    /** {@inheritDoc} */
    @Override
    public NdmFile build() {

        // store the previous constituent if any
        storeLastConstituent();

        // build the file from parsed comments and constituents
        return new NdmFile(comments.getComments(), constituents);

    }

    /**
     * Add comment.
     * <p>
     * Comments are accepted only at start. Once
     * other content is stored in the same section, comments are refused.
     * </p>
     * @param comment comment line
     * @return true if comment was accepted
     */
    public boolean addComment(final String comment) {
        return comments.addComment(comment);
    }

    /** Store last parsed constituent.
     */
    private void storeLastConstituent() {
        if (constituentParser != null) {
            constituents.add(constituentParser.build());
        }
        constituentParser = null;
    }

    /** Prepare parsing of a constituent.
     * <p>
     * Returning false allows the root element of the constituent to be handled by the dedicated parser
     * </p>
     * otherwise it is leaving the section
     * @param build builder for constituent parser
     * @return always return false!
     */
    boolean manageConstituent(final Function<ParserBuilder, AbstractMessageParser<? extends NdmConstituent<?, ?>>> build) {

        // as we have started parsing constituents, we cannot accept any further comments
        comments.refuseFurtherComments();

        // store the previous constituent if any
        storeLastConstituent();

        // create a parser for the constituent
        constituentParser = build.apply(builder);
        constituentParser.reset(getFileFormat());

        // delegate to the constituent parser the parsing of upcoming tokens
        // (including the current one that will be rejected below)
        anticipateNext(constituentParser.getCurrent());
        setFallback(this::processStructureToken);

        // reject current token, so it will be parsed by the constituent just built
        return false;

    }

    /** Process one structure token.
     * @param token token to process
     * @return true if token was processed, false otherwise
     */
    private boolean processStructureToken(final ParseToken token) {

        if (getFileFormat() == FileFormat.KVN) {
            // NDM combined instantiation can only be formatted as XML messages
            throw new OrekitException(OrekitMessages.UNSUPPORTED_FILE_FORMAT, token.getFileName());
        }

        try {
            return NdmStructureKey.valueOf(token.getName()).process(token, this);
        } catch (IllegalArgumentException iae) {
            // token has not been recognized
            return false;
        }

    }

}
