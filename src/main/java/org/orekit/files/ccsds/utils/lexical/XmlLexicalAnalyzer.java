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
package org.orekit.files.ccsds.utils.lexical;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.hipparchus.exception.DummyLocalizable;
import org.orekit.data.DataSource;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.utils.FileFormat;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/** Lexical analyzer for XML CCSDS messages.
 * @author Maxime Journot
 * @author Luc Maisonobe
 * @since 11.0
 */
public class XmlLexicalAnalyzer implements LexicalAnalyzer {

    /** Source providing the data to analyze. */
    private final DataSource source;

    /** Simple constructor.
     * @param source source providing the data to parse
     */
    public XmlLexicalAnalyzer(final DataSource source) {
        this.source = source;
    }

    /** {@inheritDoc} */
    @Override
    public <T> T accept(final MessageParser<T> messageParser) {
        try {
            // Create the handler
            final DefaultHandler handler = new XMLHandler(messageParser);

            // Create the XML SAX parser factory
            final SAXParserFactory factory = SAXParserFactory.newInstance();

            // Build the parser
            final SAXParser saxParser = factory.newSAXParser();

            // Read the xml file
            messageParser.reset(FileFormat.XML);
            final DataSource.Opener opener = source.getOpener();
            if (opener.rawDataIsBinary()) {
                try (InputStream is = opener.openStreamOnce()) {
                    if (is == null) {
                        throw new OrekitException(OrekitMessages.UNABLE_TO_FIND_FILE, source.getName());
                    }
                    saxParser.parse(new InputSource(is), handler);
                }
            } else {
                try (Reader reader = opener.openReaderOnce()) {
                    if (reader == null) {
                        throw new OrekitException(OrekitMessages.UNABLE_TO_FIND_FILE, source.getName());
                    }
                    saxParser.parse(new InputSource(reader), handler);
                }
            }

            // Get the content of the file
            return messageParser.build();

        } catch (SAXException | ParserConfigurationException | IOException e) {
            // throw caught exception as an OrekitException
            throw new OrekitException(e, new DummyLocalizable(e.getMessage()));
        }
    }

    /** Handler for parsing XML file formats.
     */
    private class XMLHandler extends DefaultHandler {

        /** CCSDS Message parser to use. */
        private final MessageParser<?> messageParser;

        /** Builder for regular elements. */
        private final XmlTokenBuilder regularBuilder;

        /** Builders for special elements. */
        private Map<String, XmlTokenBuilder> specialElements;

        /** Locator used to get current line number. */
        private Locator locator;

        /** Name of the current element. */
        private String currentElementName;

        /** Line number of the current entry. */
        private int currentLineNumber;

        /** Content of the current entry. */
        private String currentContent;

        /** Attributes of the current element. */
        private Map<String, String> currentAttributes;

        /** Last processed token qualified name.
         * @since 12.0
         */
        private String lastQname;

        /** Last processed token start/end indicator.
         * @since 12.0
         */
        private boolean lastWasStart;

        /** Simple constructor.
         * @param messageParser CCSDS Message parser to use
         */
        XMLHandler(final MessageParser<?> messageParser) {
            this.messageParser   = messageParser;
            this.regularBuilder  = new RegularXmlTokenBuilder();
            this.specialElements = messageParser.getSpecialXmlElementsBuilders();
            this.lastQname       = "";
            this.lastWasStart    = false;
        }

        /** Get a builder for the current element.
         * @param qName XML element ualified name
         * @return builder for this element
         */
        private XmlTokenBuilder getBuilder(final String qName) {
            final XmlTokenBuilder specialBuilder = specialElements.get(qName);
            return (specialBuilder != null) ? specialBuilder : regularBuilder;
        }

        /** {@inheritDoc} */
        @Override
        public void setDocumentLocator(final Locator documentLocator) {
            this.locator = documentLocator;
        }

        /** {@inheritDoc} */
        @Override
        public void characters(final char[] ch, final int start, final int length) throws SAXException {
            // we are only interested in leaf elements between one start and one end tag
            // when nested elements occur, this method is called with the spurious whitespace
            // characters (space, tab, end of line) that occur between two successive start
            // tags, two successive end tags, or one end tag and the following start tag of
            // next element at same level.
            // We need to identify the characters we want and the characters we drop.

            // check if we are after a start tag (thus already dropping the characters
            // between and end tag and a following start or end tag)
            if (currentElementName != null) {
                // we are after a start tag, we don't know yet if the next tag will be
                // another start tag (in which case we ignore the characters) or if
                // it is the end tag of a leaf element, so we just store the characters
                // and will either use them or drop them when this next tag is seen
                currentLineNumber = locator.getLineNumber();
                this.currentContent = this.currentContent + new String(ch, start, length);
            }
        }

        /** {@inheritDoc} */
        @Override
        public void startElement(final String uri, final String localName, final String qName, final Attributes attributes) {

            currentElementName = qName;
            currentLineNumber  = locator.getLineNumber();
            currentContent     = "";

            // save attributes in separate map, to avoid overriding during parsing
            if (attributes.getLength() == 0) {
                currentAttributes  = Collections.emptyMap();
            } else {
                currentAttributes = new HashMap<>(attributes.getLength());
                for (int i = 0; i < attributes.getLength(); ++i) {
                    currentAttributes.put(attributes.getQName(i), attributes.getValue(i));
                }
            }

            for (final ParseToken token : getBuilder(qName).
                                          buildTokens(true, false, qName, getContent(), currentAttributes,
                                                      currentLineNumber, source.getName())) {
                messageParser.process(token);
            }
            lastQname    = qName;
            lastWasStart = true;

        }

        private String getContent() {
            return currentContent.isEmpty() ? null : currentContent;
        }

        /** {@inheritDoc} */
        @Override
        public void endElement(final String uri, final String localName, final String qName) {

            if (currentContent == null || currentContent.isEmpty()) {
                // for an end tag without content, we keep the line number of the end tag itself
                currentLineNumber = locator.getLineNumber();
            }

            // check if we are parsing the end tag of a leaf element
            final boolean isLeaf = lastWasStart && qName.equals(lastQname);

            for (final ParseToken token : getBuilder(qName).
                                          buildTokens(false, isLeaf, qName, getContent(), currentAttributes,
                                                      currentLineNumber, source.getName())) {
                messageParser.process(token);
            }
            lastQname    = qName;
            lastWasStart = true;

            currentElementName = null;
            currentAttributes  = null;
            currentLineNumber  = -1;
            currentContent     = "";

        }

        /** {@inheritDoc} */
        @Override
        public InputSource resolveEntity(final String publicId, final String systemId) {
            // disable external entities
            return new InputSource();
        }

    }

}
