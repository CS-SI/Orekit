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
package org.orekit.files.ccsds.ndm.odm.oem;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;

import org.orekit.data.DataContext;
import org.orekit.data.DataSource;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.definitions.Units;
import org.orekit.files.ccsds.ndm.ParsedUnitsBehavior;
import org.orekit.files.ccsds.ndm.odm.CartesianCovariance;
import org.orekit.files.ccsds.ndm.odm.CartesianCovarianceKey;
import org.orekit.files.ccsds.ndm.odm.OdmCommonMetadata;
import org.orekit.files.ccsds.ndm.odm.CommonMetadataKey;
import org.orekit.files.ccsds.ndm.odm.OdmHeader;
import org.orekit.files.ccsds.ndm.odm.OdmMetadataKey;
import org.orekit.files.ccsds.ndm.odm.OdmParser;
import org.orekit.files.ccsds.ndm.odm.StateVector;
import org.orekit.files.ccsds.ndm.odm.StateVectorKey;
import org.orekit.files.ccsds.section.HeaderProcessingState;
import org.orekit.files.ccsds.section.KvnStructureProcessingState;
import org.orekit.files.ccsds.section.MetadataKey;
import org.orekit.files.ccsds.section.XmlStructureProcessingState;
import org.orekit.files.ccsds.utils.ContextBinding;
import org.orekit.files.ccsds.utils.FileFormat;
import org.orekit.files.ccsds.utils.lexical.ParseToken;
import org.orekit.files.ccsds.utils.lexical.TokenType;
import org.orekit.files.ccsds.utils.parsing.ProcessingState;
import org.orekit.files.general.EphemerisFileParser;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.units.Unit;

/**
 * A parser for the CCSDS OEM (Orbit Ephemeris Message).
 * <p>
 * Note than starting with Orekit 11.0, CCSDS message parsers are
 * mutable objects that gather the data being parsed, until the
 * message is complete and the {@link #parseMessage(org.orekit.data.DataSource)
 * parseMessage} method has returned. This implies that parsers
 * should <em>not</em> be used in a multi-thread context. The recommended
 * way to use parsers is to either dedicate one parser for each message
 * and drop it afterwards, or to use a single-thread loop.
 * </p>
 * @author sports
 * @since 6.1
 */
public class OemParser extends OdmParser<Oem, OemParser> implements EphemerisFileParser<Oem> {

    /** Comment marker. */
    private static final String COMMENT = "COMMENT";

                    /** Pattern for splitting strings at blanks. */
    private static final Pattern SPLIT_AT_BLANKS = Pattern.compile("\\s+");

    /** File header. */
    private OdmHeader header;

    /** File segments. */
    private List<OemSegment> segments;

    /** Metadata for current observation block. */
    private OemMetadata metadata;

    /** Context binding valid for current metadata. */
    private ContextBinding context;

    /** Current Ephemerides block being parsed. */
    private OemData currentBlock;

    /** Indicator for covariance parsing. */
    private boolean inCovariance;

    /** Current covariance matrix being parsed. */
    private CartesianCovariance currentCovariance;

    /** Current row number in covariance matrix. */
    private int currentRow;

    /** Default interpolation degree. */
    private int defaultInterpolationDegree;

    /** Processor for global message structure. */
    private ProcessingState structureProcessor;

    /** State vector logical block being read. */
    private StateVector stateVectorBlock;

    /**
     * Complete constructor.
     * <p>
     * Calling this constructor directly is not recommended. Users should rather use
     * {@link org.orekit.files.ccsds.ndm.ParserBuilder#buildOemParser()
     * parserBuilder.buildOemParser()}.
     * </p>
     * @param conventions IERS Conventions
     * @param simpleEOP if true, tidal effects are ignored when interpolating EOP
     * @param dataContext used to retrieve frames, time scales, etc.
     * @param missionReferenceDate reference date for Mission Elapsed Time or Mission Relative Time time systems
     * (may be null if time system is absolute)
     * @param mu gravitational coefficient
     * @param defaultInterpolationDegree default interpolation degree
     * @param parsedUnitsBehavior behavior to adopt for handling parsed units
     * @param filters filters to apply to parse tokens
     * @since 12.0
     */
    public OemParser(final IERSConventions conventions, final boolean simpleEOP,
                     final DataContext dataContext,
                     final AbsoluteDate missionReferenceDate, final double mu,
                     final int defaultInterpolationDegree, final ParsedUnitsBehavior parsedUnitsBehavior,
                     final Function<ParseToken, List<ParseToken>>[] filters) {
        super(Oem.ROOT, Oem.FORMAT_VERSION_KEY, conventions, simpleEOP, dataContext,
              missionReferenceDate, mu, parsedUnitsBehavior, filters);
        this.defaultInterpolationDegree  = defaultInterpolationDegree;
    }

    /** {@inheritDoc} */
    @Override
    public Oem parse(final DataSource source) {
        return parseMessage(source);
    }

    /** {@inheritDoc} */
    @Override
    public OdmHeader getHeader() {
        return header;
    }

    /** {@inheritDoc} */
    @Override
    public void reset(final FileFormat fileFormat) {
        header            = new OdmHeader();
        segments          = new ArrayList<>();
        metadata          = null;
        context           = null;
        currentBlock      = null;
        inCovariance      = false;
        currentCovariance = null;
        currentRow        = -1;
        if (fileFormat == FileFormat.XML) {
            structureProcessor = new XmlStructureProcessingState(Oem.ROOT, this);
            reset(fileFormat, structureProcessor);
        } else {
            structureProcessor = new KvnStructureProcessingState(this);
            reset(fileFormat, new HeaderProcessingState(this));
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean prepareHeader() {
        anticipateNext(new HeaderProcessingState(this));
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean inHeader() {
        anticipateNext(structureProcessor);
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean finalizeHeader() {
        header.validate(header.getFormatVersion());
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean prepareMetadata() {
        if (currentBlock != null) {
            // we have started a new segment, we need to finalize the previous one
            finalizeData();
        }
        metadata = new OemMetadata(defaultInterpolationDegree);
        context  = new ContextBinding(this::getConventions, this::isSimpleEOP,
                                      this::getDataContext, this::getParsedUnitsBehavior,
                                      this::getMissionReferenceDate,
                                      metadata::getTimeSystem, () -> 0.0, () -> 1.0);
        anticipateNext(this::processMetadataToken);
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean inMetadata() {
        anticipateNext(structureProcessor);
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean finalizeMetadata() {
        metadata.finalizeMetadata(context);
        metadata.validate(header.getFormatVersion());
        if (metadata.getCenter().getBody() != null) {
            setMuCreated(metadata.getCenter().getBody().getGM());
        }
        anticipateNext(getFileFormat() == FileFormat.XML ? structureProcessor : this::processKvnDataToken);
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean prepareData() {
        currentBlock = new OemData();
        anticipateNext(getFileFormat() == FileFormat.XML ? this::processXmlSubStructureToken : this::processMetadataToken);
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean inData() {
        anticipateNext(getFileFormat() == FileFormat.XML ? structureProcessor : this::processKvnCovarianceToken);
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean finalizeData() {
        if (metadata != null) {
            currentBlock.validate(header.getFormatVersion());
            segments.add(new OemSegment(metadata, currentBlock, getSelectedMu()));
        }
        metadata          = null;
        context           = null;
        currentBlock      = null;
        inCovariance      = false;
        currentCovariance = null;
        currentRow        = -1;
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public Oem build() {
        // OEM KVN file lack a DATA_STOP keyword, hence we can't call finalizeData()
        // automatically before the end of the file
        finalizeData();
        final Oem file = new Oem(header, segments, getConventions(), getDataContext(), getSelectedMu());
        file.checkTimeSystems();
        return file;
    }

    /** Manage state vector section in a XML message.
     * @param starting if true, parser is entering the section
     * otherwise it is leaving the section
     * @return always return true
     */
    boolean manageXmlStateVectorSection(final boolean starting) {
        if (starting) {
            stateVectorBlock = new StateVector();
            anticipateNext(this::processXmlStateVectorToken);
        } else {
            currentBlock.addData(stateVectorBlock.toTimeStampedPVCoordinates(),
                                 stateVectorBlock.hasAcceleration());
            stateVectorBlock = null;
            anticipateNext(structureProcessor);
        }
        return true;
    }

    /** Manage covariance matrix section.
     * @param starting if true, parser is entering the section
     * otherwise it is leaving the section
     * @return always return true
     */
    boolean manageCovarianceSection(final boolean starting) {
        if (starting) {
            // save the current metadata for later retrieval of reference frame
            final OdmCommonMetadata savedMetadata = metadata;
            currentCovariance = new CartesianCovariance(() -> savedMetadata.getReferenceFrame());
            anticipateNext(getFileFormat() == FileFormat.XML ?
                        this::processXmlCovarianceToken :
                        this::processKvnCovarianceToken);
        } else {
            currentBlock.addCovarianceMatrix(currentCovariance);
            currentCovariance = null;
            anticipateNext(structureProcessor);
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
                return OdmMetadataKey.valueOf(token.getName()).process(token, context, metadata);
            } catch (IllegalArgumentException iaeD) {
                try {
                    return CommonMetadataKey.valueOf(token.getName()).process(token, context, metadata);
                } catch (IllegalArgumentException iaeC) {
                    try {
                        return OemMetadataKey.valueOf(token.getName()).process(token, context, metadata);
                    } catch (IllegalArgumentException iaeE) {
                        // token has not been recognized
                        return false;
                    }
                }
            }
        }
    }

    /** Process one XML data substructure token.
     * @param token token to process
     * @return true if token was processed, false otherwise
     */
    private boolean processXmlSubStructureToken(final ParseToken token) {
        if (COMMENT.equals(token.getName())) {
            return token.getType() == TokenType.ENTRY ? currentBlock.addComment(token.getContentAsNormalizedString()) : true;
        } else {
            try {
                return token.getName() != null &&
                                OemDataSubStructureKey.valueOf(token.getName()).process(token, this);
            } catch (IllegalArgumentException iae) {
                // token has not been recognized
                return false;
            }
        }
    }

    /** Process one data token in a KVN message.
     * @param token token to process
     * @return true if token was processed, false otherwise
     */
    private boolean processKvnDataToken(final ParseToken token) {
        if (currentBlock == null) {
            // OEM KVN file lack a DATA_START keyword, hence we can't call prepareData()
            // automatically before the first data token arrives
            prepareData();
        }
        inData();
        if (COMMENT.equals(token.getName())) {
            return token.getType() == TokenType.ENTRY ? currentBlock.addComment(token.getContentAsNormalizedString()) : true;
        } else if (token.getType() == TokenType.RAW_LINE) {
            try {
                final String[] fields = SPLIT_AT_BLANKS.split(token.getRawContent().trim());
                if (fields.length != 7 && fields.length != 10) {
                    throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                              token.getLineNumber(), token.getFileName(), token.getContentAsNormalizedString());
                }
                stateVectorBlock = new StateVector();
                stateVectorBlock.setEpoch(context.getTimeSystem().getConverter(context).parse(fields[0]));
                stateVectorBlock.setP(0, Unit.KILOMETRE.toSI(Double.parseDouble(fields[1])));
                stateVectorBlock.setP(1, Unit.KILOMETRE.toSI(Double.parseDouble(fields[2])));
                stateVectorBlock.setP(2, Unit.KILOMETRE.toSI(Double.parseDouble(fields[3])));
                stateVectorBlock.setV(0, Units.KM_PER_S.toSI(Double.parseDouble(fields[4])));
                stateVectorBlock.setV(1, Units.KM_PER_S.toSI(Double.parseDouble(fields[5])));
                stateVectorBlock.setV(2, Units.KM_PER_S.toSI(Double.parseDouble(fields[6])));
                if (fields.length == 10) {
                    stateVectorBlock.setA(0, Units.KM_PER_S2.toSI(Double.parseDouble(fields[7])));
                    stateVectorBlock.setA(1, Units.KM_PER_S2.toSI(Double.parseDouble(fields[8])));
                    stateVectorBlock.setA(2, Units.KM_PER_S2.toSI(Double.parseDouble(fields[9])));
                }
                return currentBlock.addData(stateVectorBlock.toTimeStampedPVCoordinates(),
                                            stateVectorBlock.hasAcceleration());
            } catch (NumberFormatException nfe) {
                throw new OrekitException(nfe, OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                          token.getLineNumber(), token.getFileName(), token.getRawContent());
            }
        } else {
            // not a raw line, it is most probably either the end of the data section or a covariance section
            return false;
        }
    }

    /** Process one state vector data token in a XML message.
     * @param token token to process
     * @return true if token was processed, false otherwise
     */
    private boolean processXmlStateVectorToken(final ParseToken token) {
        anticipateNext(this::processXmlSubStructureToken);
        try {
            return token.getName() != null &&
                   StateVectorKey.valueOf(token.getName()).process(token, context, stateVectorBlock);
        } catch (IllegalArgumentException iae) {
            // token has not been recognized
            return false;
        }
    }

    /** Process one covariance token in a KVN message.
     * @param token token to process
     * @return true if token was processed, false otherwise
     */
    private boolean processKvnCovarianceToken(final ParseToken token) {
        anticipateNext(getFileFormat() == FileFormat.XML ? structureProcessor : this::processMetadataToken);
        if (token.getName() != null) {
            if (OemDataSubStructureKey.COVARIANCE.name().equals(token.getName()) ||
                OemDataSubStructureKey.covarianceMatrix.name().equals(token.getName())) {
                // we are entering/leaving covariance section
                inCovariance = token.getType() == TokenType.START;
                return true;
            } else if (!inCovariance) {
                // this is not a covariance token
                return false;
            } else {
                // named tokens in covariance section must be at the start, before the raw lines
                if (currentRow > 0) {
                    // the previous covariance matrix was not completed
                    throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_ELEMENT_IN_FILE,
                                              token.getName(), token.getLineNumber(), token.getFileName());
                }

                if (currentCovariance == null) {
                    // save the current metadata for later retrieval of reference frame
                    final OdmCommonMetadata savedMetadata = metadata;
                    currentCovariance = new CartesianCovariance(() -> savedMetadata.getReferenceFrame());
                    currentRow        = 0;
                }

                // parse the token
                try {
                    return CartesianCovarianceKey.valueOf(token.getName()).
                           process(token, context, currentCovariance);
                } catch (IllegalArgumentException iae) {
                    // token not recognized
                    return false;
                }

            }
        } else {
            // this is a raw line
            try {
                final String[] fields = SPLIT_AT_BLANKS.split(token.getContentAsNormalizedString().trim());
                if (fields.length != currentRow + 1) {
                    throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                              token.getLineNumber(), token.getFileName(), token.getContentAsNormalizedString());
                }
                for (int j = 0; j < fields.length; ++j) {
                    currentCovariance.setCovarianceMatrixEntry(currentRow, j, 1.0e6 * Double.parseDouble(fields[j]));
                }
                if (++currentRow == 6) {
                    // this was the last row
                    currentBlock.addCovarianceMatrix(currentCovariance);
                    currentCovariance = null;
                    currentRow        = -1;
                }
                return true;
            } catch (NumberFormatException nfe) {
                throw new OrekitException(nfe, OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                          token.getLineNumber(), token.getFileName(), token.getContentAsNormalizedString());
            }
        }
    }

    /** Process one covariance matrix data token in a XML message.
     * @param token token to process
     * @return true if token was processed, false otherwise
     */
    private boolean processXmlCovarianceToken(final ParseToken token) {
        anticipateNext(this::processXmlSubStructureToken);
        try {
            return token.getName() != null &&
                   CartesianCovarianceKey.valueOf(token.getName()).process(token, context, currentCovariance);
        } catch (IllegalArgumentException iae) {
            // token has not been recognized
            return false;
        }
    }

}
