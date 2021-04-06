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
package org.orekit.files.ccsds.ndm.adm.aem;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.orekit.data.DataContext;
import org.orekit.data.DataSource;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.ndm.adm.AdmMetadataKey;
import org.orekit.files.ccsds.ndm.adm.AdmParser;
import org.orekit.files.ccsds.section.Header;
import org.orekit.files.ccsds.section.HeaderProcessingState;
import org.orekit.files.ccsds.section.KvnStructureProcessingState;
import org.orekit.files.ccsds.section.MetadataKey;
import org.orekit.files.ccsds.section.XmlStructureProcessingState;
import org.orekit.files.ccsds.utils.ContextBinding;
import org.orekit.files.ccsds.utils.FileFormat;
import org.orekit.files.ccsds.utils.lexical.ParseToken;
import org.orekit.files.ccsds.utils.lexical.TokenType;
import org.orekit.files.ccsds.utils.parsing.ProcessingState;
import org.orekit.files.general.AttitudeEphemerisFileParser;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.IERSConventions;

/**
 * A parser for the CCSDS AEM (Attitude Ephemeris Message).
 * <p>
 * Note than starting with Orekit 11.0, CCSDS message parsers are
 * mutable objects that gather the data being parsed, until the
 * message is complete and the {@link #parseMessage(org.orekit.data.DataSource)
 * parseMessage} method has returned. This implies that parsers
 * should <em>not</em> be used in a multi-thread context. The recommended
 * way to use parsers is to either dedicate one parser for each message
 * and drop it afterwards, or to use a single-thread loop.
 * </p>
 * @author Bryan Cazabonne
 * @since 10.2
 */
public class AemParser extends AdmParser<AemFile, AemParser> implements AttitudeEphemerisFileParser<AemFile> {

    /** Pattern for splitting strings at blanks. */
    private static final Pattern SPLIT_AT_BLANKS = Pattern.compile("\\s+");

    /** File header. */
    private Header header;

    /** File segments. */
    private List<AemSegment> segments;

    /** Metadata for current observation block. */
    private AemMetadata metadata;

    /** Context binding valid for current metadata. */
    private ContextBinding context;

    /** Current Ephemerides block being parsed. */
    private AemData currentBlock;

    /** Default interpolation degree. */
    private int defaultInterpolationDegree;

    /** Processor for global message structure. */
    private ProcessingState structureProcessor;

    /** Current attitude entry. */
    private AttitudeEntry currentEntry;

    /**Complete constructor.
     * <p>
     * Calling this constructor directly is not recommended. Users should rather use
     * {@link org.orekit.files.ccsds.ndm.ParserBuilder#buildAemParser()
     * parserBuilder.buildAemParser()}.
     * </p>
     * @param conventions IERS Conventions
     * @param simpleEOP if true, tidal effects are ignored when interpolating EOP
     * @param dataContext used to retrieve frames, time scales, etc.
     * @param missionReferenceDate reference date for Mission Elapsed Time or Mission Relative Time time systems
     * (may be null if time system is absolute)
     * @param defaultInterpolationDegree default interpolation degree
     */
    public AemParser(final IERSConventions conventions, final boolean simpleEOP,
                     final DataContext dataContext,
                     final AbsoluteDate missionReferenceDate, final int defaultInterpolationDegree) {
        super(AemFile.ROOT, AemFile.FORMAT_VERSION_KEY, conventions, simpleEOP, dataContext, missionReferenceDate);
        this.defaultInterpolationDegree  = defaultInterpolationDegree;
    }

    /** {@inheritDoc} */
    @Override
    public AemFile parse(final DataSource source) {
        return parseMessage(source);
    }

    /** {@inheritDoc} */
    @Override
    public Header getHeader() {
        return header;
    }

    /** {@inheritDoc} */
    @Override
    public void reset(final FileFormat fileFormat) {
        header   = new Header();
        segments = new ArrayList<>();
        metadata = null;
        context  = null;
        if (fileFormat == FileFormat.XML) {
            structureProcessor = new XmlStructureProcessingState(AemFile.ROOT, this);
            reset(fileFormat, structureProcessor);
        } else {
            structureProcessor = new KvnStructureProcessingState(this);
            reset(fileFormat, new HeaderProcessingState(getDataContext(), this));
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean prepareHeader() {
        setFallback(new HeaderProcessingState(getDataContext(), this));
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean inHeader() {
        setFallback(structureProcessor);
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean finalizeHeader() {
        header.checkMandatoryEntries();
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean prepareMetadata() {
        if (metadata != null) {
            return false;
        }
        metadata  = new AemMetadata(defaultInterpolationDegree);
        context   = new ContextBinding(this::getConventions, this::isSimpleEOP,
                                       this::getDataContext, this::getMissionReferenceDate,
                                       metadata::getTimeSystem, () -> 0.0, () -> 1.0);
        setFallback(this::processMetadataToken);
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean inMetadata() {
        setFallback(getFileFormat() == FileFormat.XML ? structureProcessor : this::processKvnDataToken);
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean finalizeMetadata() {
        metadata.checkMandatoryEntries();
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean prepareData() {
        currentBlock = new AemData();
        setFallback(getFileFormat() == FileFormat.XML ? this::processXmlSubStructureToken : this::processMetadataToken);
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean inData() {
        setFallback(structureProcessor);
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean finalizeData() {
        if (metadata != null) {
            currentBlock.checkMandatoryEntries();
            segments.add(new AemSegment(metadata, currentBlock));
        }
        metadata = null;
        context  = null;
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public AemFile build() {
        final AemFile file = new AemFile(header, segments, getConventions(), getDataContext());
        file.checkTimeSystems();
        return file;
    }

    /** Manage attitude state section in a XML message.
     * @param starting if true, parser is entering the section
     * otherwise it is leaving the section
     * @return always return true
     */
    boolean manageXmlAttitudeStateSection(final boolean starting) {
        if (starting) {
            currentEntry = new AttitudeEntry(metadata);
            setFallback(this::processXmlDataToken);
        } else {
            currentBlock.addData(currentEntry.getCoordinates());
            currentEntry = null;
            setFallback(structureProcessor);
        }
        return true;
    }

    /** Process one metadata token.
     * @param token token to process
     * @return true if token was processed, false otherwise
     */
    private boolean processMetadataToken(final ParseToken token) {
        inMetadata();
        try {
            return token.getName() != null &&
                   MetadataKey.valueOf(token.getName()).process(token, context, metadata);
        } catch (IllegalArgumentException iaeM) {
            try {
                return AdmMetadataKey.valueOf(token.getName()).process(token, context, metadata);
            } catch (IllegalArgumentException iaeD) {
                try {
                    return AemMetadataKey.valueOf(token.getName()).process(token, context, metadata);
                } catch (IllegalArgumentException iaeE) {
                    // token has not been recognized
                    return false;
                }
            }
        }
    }

    /** Process one XML data substructure token.
     * @param token token to process
     * @return true if token was processed, false otherwise
     */
    private boolean processXmlSubStructureToken(final ParseToken token) {
        try {
            return token.getName() != null &&
                   XmlSubStructureKey.valueOf(token.getName()).process(token, this);
        } catch (IllegalArgumentException iae) {
            // token has not been recognized
            return false;
        }
    }

    /** Process one data token in a KVN message.
     * @param token token to process
     * @return true if token was processed, false otherwise
     */
    private boolean processKvnDataToken(final ParseToken token) {
        inData();
        if ("COMMENT".equals(token.getName())) {
            return token.getType() == TokenType.ENTRY ? currentBlock.addComment(token.getContentAsNormalizedString()) : true;
        } else if (token.getType() == TokenType.RAW_LINE) {
            try {
                if (metadata.getAttitudeType() == null) {
                    throw new OrekitException(OrekitMessages.CCSDS_MISSING_KEYWORD,
                                              AemMetadataKey.ATTITUDE_TYPE.name(), token.getFileName());
                }
                return currentBlock.addData(metadata.getAttitudeType().parse(metadata.isFirst(),
                                                                             metadata.getEndpoints().isExternal2SpacecraftBody(),
                                                                             metadata.getEulerRotSeq(),
                                                                             metadata.isSpacecraftBodyRate(),
                                                                             context,
                                                                             SPLIT_AT_BLANKS.split(token.getRawContent().trim())));
            } catch (NumberFormatException nfe) {
                throw new OrekitException(nfe, OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                          token.getLineNumber(), token.getFileName(), token.getRawContent());
            }
        } else {
            // not a raw line, it is most probably the end of the data section
            return false;
        }
    }

    /** Process one data token in a XML message.
     * @param token token to process
     * @return true if token was processed, false otherwise
     */
    private boolean processXmlDataToken(final ParseToken token) {
        setFallback(this::processXmlSubStructureToken);
        try {
            return token.getName() != null &&
                   AttitudeEntryKey.valueOf(token.getName()).process(token, context, currentEntry);
        } catch (IllegalArgumentException iae) {
            // token has not been recognized
            return false;
        }
    }

}
