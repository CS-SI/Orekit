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
package org.orekit.files.ccsds.utils.lexical;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.hipparchus.exception.DummyLocalizable;
import org.orekit.data.DataSource;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.ndm.NdmFile;
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
class XmlLexicalAnalyzer implements LexicalAnalyzer {

    /** Attribute name for id. */
    private static final String ID = "id";

    /** Attribute name for version. */
    private static final String VERSION = "version";

    /** Attribute name for units. */
    private static final String UNITS = "units";

    /** Source providing the data to analyze. */
    private final DataSource source;

    /** Simple constructor.
     * @param source source providing the data to parse
     */
    XmlLexicalAnalyzer(final DataSource source) {
        this.source = source;
    }

    /** {@inheritDoc} */
    @Override
    public <T extends NdmFile<?, ?>> T accept(final MessageParser<T> messageParser) {
        try {
            // Create the handler
            final DefaultHandler handler = new XMLHandler(messageParser);

            // Create the XML SAX parser factory
            final SAXParserFactory factory = SAXParserFactory.newInstance();

            // Build the parser
            final SAXParser saxParser = factory.newSAXParser();

            // Read the xml file
            messageParser.reset(FileFormat.XML);
            final InputStream is = source.getStreamOpener().openStream();
            if (is == null) {
                throw new OrekitException(OrekitMessages.UNABLE_TO_FIND_FILE, source.getName());
            }
            saxParser.parse(is, handler);

            // Get the content of the file
            return messageParser.build();

        } catch (SAXException se) {
            final OrekitException oe;
            if (se.getException() != null && se.getException() instanceof OrekitException) {
                oe = (OrekitException) se.getException();
            } else {
                oe = new OrekitException(se, new DummyLocalizable(se.getMessage()));
            }
            throw oe;
        } catch (ParserConfigurationException | IOException e) {
            // throw caught exception as an OrekitException
            throw new OrekitException(e, new DummyLocalizable(e.getMessage()));
        }
    }

    /** Handler for parsing XML file formats.
     */
    private class XMLHandler extends DefaultHandler {

        /** CCSDS Message parser to use. */
        private final MessageParser<?> messageParser;

        /** Flag for special processing of first element. */
        private boolean first;

        /** Locator used to get current line number. */
        private Locator locator;

        /** Name of the current element. */
        private String currentElementName;

        /** Line number of the current entry. */
        private int currentLineNumber;

        /** Content of the current entry. */
        private String currentContent;

        /** Units of the current element, if any. */
        private String currentUnits;

        /** Simple constructor.
         * @param messageParser CCSDS Message parser to use
         */
        XMLHandler(final MessageParser<?> messageParser) {
            this.messageParser = messageParser;
            this.first         = true;
        }

        /** {@inheritDoc} */
        @Override
        public void setDocumentLocator(final Locator documentLocator) {
            this.locator = documentLocator;
        }

        /** {@inheritDoc} */
        @Override
        public void characters(final char[] ch, final int start, final int length) throws SAXException {
            if (currentElementName != null) {
                // we are only interested in leaf elements between one start and one end tag
                // when nested elements occur, this method is called with the spurious whitespace
                // characters (space, tab, end of line) that occur between two successive start tags.
                // We don't want these characters, so we delay the processing of the content until
                // the closing tag is found, thus allowing to drop them by resetting content
                // each time a start tag is found
                currentLineNumber = locator.getLineNumber();
                currentContent    = new String(ch, start, length);
            }
        }

        /** {@inheritDoc} */
        @Override
        public void startElement(final String uri, final String localName, final String qName, final Attributes attributes) {

            // process start tag
            messageParser.process(new ParseToken(TokenType.START,
                                                 qName, null, null,
                                                 locator.getLineNumber(), source.getName()));

            if (first) {
                // this is the first element in the file, it must contains the format file version
                if (messageParser.getFormatVersionKey().equals(attributes.getValue(ID))) {
                    // generate a parse token for the file format version
                    messageParser.process(new ParseToken(TokenType.ENTRY,
                                                         messageParser.getFormatVersionKey(),
                                                         attributes.getValue(VERSION),
                                                         null,
                                                         locator.getLineNumber(), source.getName()));
                    first = false;
                } else {
                    throw new OrekitException(OrekitMessages.UNSUPPORTED_FILE_FORMAT, source.getName());
                }
            }

            currentElementName = qName;
            currentUnits       = attributes.getValue(UNITS);
            currentLineNumber  = -1;
            currentContent     = null;

        }

        /** {@inheritDoc} */
        @Override
        public void endElement(final String uri, final String localName, final String qName) {

            if (currentContent != null) {
                // we have found in succession a start tag, some content, and an end tag
                // we are sure we have found a leaf element, so we can now process its content
                // which was delayed until now
                messageParser.process(new ParseToken(TokenType.ENTRY,
                                                     currentElementName, currentContent, currentUnits,
                                                     currentLineNumber, source.getName()));
            }

            // process end tag
            messageParser.process(new ParseToken(TokenType.STOP,
                                                 qName, null, null,
                                                 locator.getLineNumber(), source.getName()));

            currentElementName = null;
            currentUnits       = null;
            currentLineNumber  = -1;
            currentContent     = null;

        }

        /** {@inheritDoc} */
        @Override
        public InputSource resolveEntity(final String publicId, final String systemId) {
            // disable external entities
            return new InputSource();
        }

    }

}
