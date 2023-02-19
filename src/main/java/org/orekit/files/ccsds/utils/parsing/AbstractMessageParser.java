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
package org.orekit.files.ccsds.utils.parsing;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.hipparchus.exception.LocalizedCoreFormats;
import org.orekit.data.DataSource;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitInternalError;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.utils.FileFormat;
import org.orekit.files.ccsds.utils.lexical.LexicalAnalyzerSelector;
import org.orekit.files.ccsds.utils.lexical.MessageParser;
import org.orekit.files.ccsds.utils.lexical.MessageVersionXmlTokenBuilder;
import org.orekit.files.ccsds.utils.lexical.ParseToken;
import org.orekit.files.ccsds.utils.lexical.TokenType;
import org.orekit.files.ccsds.utils.lexical.XmlTokenBuilder;

/** Parser for CCSDS messages.
 * <p>
 * Note than starting with Orekit 11.0, CCSDS message parsers are
 * mutable objects that gather the data being parsed, until the
 * message is complete and the {@link #parseMessage(org.orekit.data.DataSource)
 * parseMessage} method has returned. This implies that parsers
 * should <em>not</em> be used in a multi-thread context. The recommended
 * way to use parsers is to either dedicate one parser for each message
 * and drop it afterwards, or to use a single-thread loop.
 * </p>
 * @param <T> type of the file
 * @author Luc Maisonobe
 * @since 11.0
 */
public abstract class AbstractMessageParser<T> implements MessageParser<T> {

    /** Comment key.
     * @since 12.0
     */
    private static final String COMMENT = "COMMENT";

    /** Safety limit for loop over processing states. */
    private static final int MAX_LOOP = 100;

    /** Root element for XML files. */
    private final String root;

    /** Key for format version. */
    private final String formatVersionKey;

    /** Filters for parse tokens. */
    private final Function<ParseToken, List<ParseToken>>[] filters;

    /** Anticipated next processing state. */
    private ProcessingState next;

    /** Current processing state. */
    private ProcessingState current;

    /** Fallback processing state. */
    private ProcessingState fallback;

    /** Format of the file ready to be parsed. */
    private FileFormat format;

    /** Flag for XML end tag. */
    private boolean endTagSeen;

    /** Simple constructor.
     * @param root root element for XML files
     * @param formatVersionKey key for format version
     * @param filters filters to apply to parse tokens
     * @since 12.0
     */
    protected AbstractMessageParser(final String root, final String formatVersionKey,
                                    final Function<ParseToken, List<ParseToken>>[] filters) {
        this.root             = root;
        this.formatVersionKey = formatVersionKey;
        this.filters          = filters.clone();
        this.current          = null;
        setFallback(new ErrorState());
    }

    /** Set fallback processing state.
     * <p>
     * The fallback processing state is used if anticipated state fails
     * to parse the token.
     * </p>
     * @param fallback processing state to use if anticipated state does not work
     */
    public void setFallback(final ProcessingState fallback) {
        this.fallback = fallback;
    }

    /** Reset parser to initial state before parsing.
     * @param fileFormat format of the file ready to be parsed
     * @param initialState initial processing state
     */
    protected void reset(final FileFormat fileFormat, final ProcessingState initialState) {
        format     = fileFormat;
        current    = initialState;
        endTagSeen = false;
        anticipateNext(fallback);
    }

    /** Set the flag for XML end tag.
     * @param endTagSeen if true, the XML end tag has been seen
     */
    public void setEndTagSeen(final boolean endTagSeen) {
        this.endTagSeen = endTagSeen;
    }

    /** Check if XML end tag has been seen.
     * @return true if XML end tag has been seen
     */
    public boolean wasEndTagSeen() {
        return endTagSeen;
    }

    /** Get the current processing state.
     * @return current processing state
     */
    public ProcessingState getCurrent() {
        return current;
    }

    /** {@inheritDoc} */
    @Override
    public FileFormat getFileFormat() {
        return format;
    }

    /** {@inheritDoc} */
    @Override
    public T parseMessage(final DataSource source) {
        try {
            return LexicalAnalyzerSelector.select(source).accept(this);
        } catch (IOException ioe) {
            throw new OrekitException(ioe, LocalizedCoreFormats.SIMPLE_MESSAGE,
                                      ioe.getLocalizedMessage());
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getFormatVersionKey() {
        return formatVersionKey;
    }

    /** {@inheritDoc} */
    @Override
    public Map<String, XmlTokenBuilder> getSpecialXmlElementsBuilders() {

        final HashMap<String, XmlTokenBuilder> builders = new HashMap<>();

        if (formatVersionKey != null) {
            // special handling of root tag that contains the format version
            builders.put(root, new MessageVersionXmlTokenBuilder());
        }

        return builders;

    }

    /** Anticipate what next processing state should be.
     * @param anticipated anticipated next processing state
     */
    public void anticipateNext(final ProcessingState anticipated) {
        this.next = anticipated;
    }

    /** {@inheritDoc} */
    @Override
    public void process(final ParseToken token) {

        // loop over the filters
        List<ParseToken> filtered = Collections.singletonList(token);
        for (Function<ParseToken, List<ParseToken>> filter : filters) {
            final ArrayList<ParseToken> newFiltered = new ArrayList<>();
            for (final ParseToken original : filtered) {
                newFiltered.addAll(filter.apply(original));
            }
            filtered = newFiltered;
        }
        if (filtered.isEmpty()) {
            return;
        }

        int remaining = filtered.size();
        for (final ParseToken filteredToken : filtered) {

            if (filteredToken.getType() == TokenType.ENTRY &&
                !COMMENT.equals(filteredToken.getName()) &&
                (filteredToken.getRawContent() == null || filteredToken.getRawContent().isEmpty())) {
                // value is empty, which is forbidden by CCSDS standards
                throw new OrekitException(OrekitMessages.UNINITIALIZED_VALUE_FOR_KEY, filteredToken.getName());
            }

            // loop over the various states until one really processes the token
            for (int i = 0; i < MAX_LOOP; ++i) {
                if (current.processToken(filteredToken)) {
                    // filtered token was properly processed
                    if (--remaining == 0) {
                        // we have processed all filtered tokens
                        return;
                    } else {
                        // we need to continue processing the remaining filtered tokens
                        break;
                    }
                } else {
                    // filtered token was not processed by current processing state, switch to next one
                    current = next;
                    next    = fallback;
                }
            }

        }

        // this should never happen
        throw new OrekitInternalError(null);

    }

}
