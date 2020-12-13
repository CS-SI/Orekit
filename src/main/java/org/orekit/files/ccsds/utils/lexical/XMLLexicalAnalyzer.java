/* Copyright 2002-2020 CS GROUP
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
import org.orekit.errors.OrekitException;
import org.orekit.files.ccsds.ndm.NDMFile;
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
public class XMLLexicalAnalyzer implements LexicalAnalyzer {

    /** Stream containing message. */
    private final InputStream stream;

    /** Name of the file containing the message (for error messages). */
    private final String fileName;

    /** Simple constructor.
     * @param stream stream containing message
     * @param fileName name of the file containing the message (for error messages)
     */
    public XMLLexicalAnalyzer(final InputStream stream, final String fileName) {
        this.stream   = stream;
        this.fileName = fileName;
    }

    /** {@inheritDoc} */
    @Override
    public <T extends NDMFile> T parse(final MessageParser<T> messageParser) {
        try {
            // Create the handler
            final XMLHandler<T> handler = new XMLHandler<T>(messageParser);

            // Create the XML SAX parser factory
            final SAXParserFactory factory = SAXParserFactory.newInstance();

            // Build the parser
            final SAXParser saxParser = factory.newSAXParser();

            // Read the xml file
            saxParser.parse(stream, handler);

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
     * @param <T> type of the CCSDS Message file
     */
    private class XMLHandler<T extends NDMFile> extends DefaultHandler {

        /** CCSDS Message parser to use. */
        private final MessageParser<T> messageParser;

        /** Locator used to get current line number. */
        private Locator locator;

        /** Name of the current element. */
        private String currentElementName;

        /** Simple constructor.
         * @param messageParser CCSDS Message parser to use
         */
        XMLHandler(final MessageParser<T> messageParser) {
            this.messageParser = messageParser;
        }

        /** {@inheritDoc} */
        @Override
        public void setDocumentLocator(final Locator documentLocator) {
            this.locator = documentLocator;
        }

        /** {@inheritDoc} */
        @Override
        public void characters(final char[] ch, final int start, final int length) throws SAXException {
            // currentElementName is set to null in function endElement every time an end tag is parsed.
            // Thus only the characters between a start and an end tags are parsed.
            if (currentElementName != null) {
                final String content = new String(ch, start, length);
                messageParser.entry(new XMLEntry(currentElementName, content, locator, fileName));
            }
        }

        /** {@inheritDoc} */
        @Override
        public void startElement(final String uri, final String localName, final String qName, final Attributes attributes) {
            currentElementName = qName;
            messageParser.start(currentElementName);
        }

        /** {@inheritDoc} */
        @Override
        public void endElement(final String uri, final String localName, final String qName) {
            currentElementName = null;
            messageParser.end(currentElementName);
        }

        /** {@inheritDoc} */
        @Override
        public InputSource resolveEntity(final String publicId, final String systemId) {
            // disable external entities
            return new InputSource();
        }

    }

}
