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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.section.CommentsContainer;
import org.orekit.files.ccsds.utils.FileFormat;
import org.orekit.files.ccsds.utils.lexical.ParseToken;
import org.orekit.files.ccsds.utils.lexical.XmlTokenBuilder;
import org.orekit.files.ccsds.utils.parsing.AbstractMessageParser;

/** A parser for the CCSDS NDM (Navigation Data Message).
 * @author Luc Maisonobe
 * @since 11.0
 */
public class NdmParser extends AbstractMessageParser<Ndm> {

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
     * @param filters filters to apply to parse tokens
     * @since 12.0
     */
    public NdmParser(final ParserBuilder builder,
                     final Function<ParseToken, List<ParseToken>>[] filters) {
        super(NdmStructureKey.ndm.name(), null, filters);
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
        builders.putAll(builder.buildAcmParser().getSpecialXmlElementsBuilders());
        builders.putAll(builder.buildCdmParser().getSpecialXmlElementsBuilders());

        return builders;

    }

    /** {@inheritDoc} */
    @Override
    public void reset(final FileFormat fileFormat) {
        reset(fileFormat, this::processToken);
        constituentParser = null;
        comments          = new CommentsContainer();
        constituents      = new ArrayList<>();
    }

    /** {@inheritDoc} */
    @Override
    public Ndm build() {
        // build the file from parsed comments and constituents
        return new Ndm(comments.getComments(), constituents);
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

    /** Prepare parsing of a TDM constituent.
     * @return always return true
     */
    boolean manageTdmConstituent() {
        return manageConstituent(builder::buildTdmParser);
    }

    /** Prepare parsing of an OPM constituent.
     * @return always return true
     */
    boolean manageOpmConstituent() {
        return manageConstituent(builder::buildOpmParser);
    }

    /** Prepare parsing of an OMM constituent.
     * @return always return true
     */
    boolean manageOmmConstituent() {
        return manageConstituent(builder::buildOmmParser);
    }

    /** Prepare parsing of an OEM constituent.
     * @return always return true
     */
    boolean manageOemConstituent() {
        return manageConstituent(builder::buildOemParser);
    }

    /** Prepare parsing of an OCM constituent.
     * @return always return true
     */
    boolean manageOcmConstituent() {
        return manageConstituent(builder::buildOcmParser);
    }

    /** Prepare parsing of an APM constituent.
     * @return always return true
     */
    boolean manageApmConstituent() {
        return manageConstituent(builder::buildApmParser);
    }

    /** Prepare parsing of a AEM constituent.
     * @return always return true
     */
    boolean manageAemConstituent() {
        return manageConstituent(builder::buildAemParser);
    }

    /** Prepare parsing of a ACM constituent.
     * @return always return true
     * @since 12.0
     */
    boolean manageAcmConstituent() {
        return manageConstituent(builder::buildAcmParser);
    }

    /** Prepare parsing of a CDM constituent.
     * @return always return true
     */
    boolean manageCdmConstituent() {
        return manageConstituent(builder::buildCdmParser);
    }

    /** Prepare parsing of a constituent.
     * @param parserSupplier supplier for constituent parser
     * @return always return true
     */
    boolean manageConstituent(final Supplier<AbstractMessageParser<? extends NdmConstituent<?, ?>>> parserSupplier) {

        // as we have started parsing constituents, we cannot accept any further comments
        comments.refuseFurtherComments();

        // create a parser for the constituent
        constituentParser = parserSupplier.get();
        constituentParser.reset(getFileFormat());

        return true;

    }

    /** Process one token.
     * @param token token to process
     * @return true if token was processed, false otherwise
     */
    private boolean processToken(final ParseToken token) {

        if (getFileFormat() == FileFormat.KVN) {
            // NDM combined instantiation can only be formatted as XML messages
            throw new OrekitException(OrekitMessages.UNSUPPORTED_FILE_FORMAT, token.getFileName());
        }

        if (constituentParser == null) {
            // we are in the global NDM structure
            try {
                return NdmStructureKey.valueOf(token.getName()).process(token, this);
            } catch (IllegalArgumentException iae) {
                // token has not been recognized
                return false;
            }
        } else {
            // we are inside one constituent
            constituentParser.process(token);
            if (constituentParser.wasEndTagSeen()) {
                // we have seen the end tag, we must go back global structure parsing
                constituents.add(constituentParser.build());
                constituentParser = null;
            }
            return true;
        }

    }

}
