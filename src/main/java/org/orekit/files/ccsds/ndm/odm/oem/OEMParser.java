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
package org.orekit.files.ccsds.ndm.odm.oem;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.data.DataContext;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.ndm.odm.OCommonMetadata;
import org.orekit.files.ccsds.ndm.odm.OCommonMetadataKey;
import org.orekit.files.ccsds.ndm.odm.OCommonParser;
import org.orekit.files.ccsds.ndm.odm.ODMCovariance;
import org.orekit.files.ccsds.ndm.odm.ODMCovarianceKey;
import org.orekit.files.ccsds.ndm.odm.ODMHeader;
import org.orekit.files.ccsds.ndm.odm.ODMMetadataKey;
import org.orekit.files.ccsds.section.Header;
import org.orekit.files.ccsds.section.HeaderProcessingState;
import org.orekit.files.ccsds.section.KVNStructureProcessingState;
import org.orekit.files.ccsds.section.MetadataKey;
import org.orekit.files.ccsds.section.XMLStructureProcessingState;
import org.orekit.files.ccsds.utils.ParsingContext;
import org.orekit.files.ccsds.utils.lexical.FileFormat;
import org.orekit.files.ccsds.utils.lexical.ParseToken;
import org.orekit.files.ccsds.utils.lexical.TokenType;
import org.orekit.files.ccsds.utils.state.ProcessingState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.TimeStampedPVCoordinates;

/**
 * A parser for the CCSDS OEM (Orbit Ephemeris Message).
 * @author sports
 * @since 6.1
 */
public class OEMParser extends OCommonParser<OEMFile, OEMParser> {

    /** Root element for XML files. */
    private static final String ROOT = "oem";

    /** Pattern for splitting strings at blanks. */
    private static final Pattern SPLIT_AT_BLANKS = Pattern.compile("\\s+");

    /** File header. */
    private ODMHeader header;

    /** File segments. */
    private List<OEMSegment> segments;

    /** Metadata for current observation block. */
    private OEMMetadata metadata;

    /** Parsing context valid for current metadata. */
    private ParsingContext context;

    /** Current Ephemerides block being parsed. */
    private OEMData currentBlock;

    /** Indicator for covariance parsing. */
    private boolean inCovariance;

    /** Current covariance matrix being parsed. */
    private ODMCovariance currentCovariance;

    /** Current row number in covariance matrix. */
    private int currentRow;

    /** Default interpolation degree. */
    private int defaultInterpolationDegree;

    /** Processor for global message structure. */
    private ProcessingState structureProcessor;

    /**
     * Complete constructor.
     * @param conventions IERS Conventions
     * @param simpleEOP if true, tidal effects are ignored when interpolating EOP
     * @param dataContext used to retrieve frames, time scales, etc.
     * @param missionReferenceDate reference date for Mission Elapsed Time or Mission Relative Time time systems
     * (may be null if time system is absolute)
     * @param mu gravitational coefficient
     * @param defaultInterpolationDegree default interpolation degree
     */
    public OEMParser(final IERSConventions conventions, final boolean simpleEOP,
                     final DataContext dataContext,
                     final AbsoluteDate missionReferenceDate, final double mu,
                     final int defaultInterpolationDegree) {
        super(OEMFile.FORMAT_VERSION_KEY, conventions, simpleEOP, dataContext,
              missionReferenceDate, mu);
        this.defaultInterpolationDegree  = defaultInterpolationDegree;
    }

    /** {@inheritDoc} */
    @Override
    public Header getHeader() {
        return header;
    }

    /** {@inheritDoc} */
    @Override
    public void reset(final FileFormat fileFormat) {
        header            = new ODMHeader();
        segments          = new ArrayList<>();
        metadata          = null;
        context           = null;
        currentBlock      = null;
        inCovariance      = false;
        currentCovariance = null;
        currentRow        = -1;
        if (getFileFormat() == FileFormat.XML) {
            structureProcessor = new XMLStructureProcessingState(ROOT, this);
            reset(fileFormat, structureProcessor);
        } else {
            structureProcessor = new KVNStructureProcessingState(this);
            reset(fileFormat, new HeaderProcessingState(getDataContext(), this));
        }
    }

    /** {@inheritDoc} */
    @Override
    public void prepareHeader() {
        setFallback(new HeaderProcessingState(getDataContext(), this));
    }

    /** {@inheritDoc} */
    @Override
    public void inHeader() {
        setFallback(structureProcessor);
    }

    /** {@inheritDoc} */
    @Override
    public void finalizeHeader() {
        header.checkMandatoryEntries();
    }

    /** {@inheritDoc} */
    @Override
    public void prepareMetadata() {
        if (currentBlock != null) {
            // we have started a new segment, we need to finalize the previous one
            finalizeData();
        }
        metadata = new OEMMetadata(defaultInterpolationDegree);
        context  = new ParsingContext(this::getConventions,
                                      this::isSimpleEOP,
                                      this::getDataContext,
                                      this::getMissionReferenceDate,
                                      metadata::getTimeSystem);
        setFallback(this::processMetadataToken);
    }

    /** {@inheritDoc} */
    @Override
    public void inMetadata() {
        setFallback(structureProcessor);
    }

    /** {@inheritDoc} */
    @Override
    public void finalizeMetadata() {
        metadata.finalizeMetadata(context);
        metadata.checkMandatoryEntries();
        setFallback(getFileFormat() == FileFormat.XML ? structureProcessor : this::processDataToken);
    }

    /** {@inheritDoc} */
    @Override
    public void prepareData() {
        currentBlock = new OEMData();
        setFallback(getFileFormat() == FileFormat.XML ? structureProcessor : this::processMetadataToken);
    }

    /** {@inheritDoc} */
    @Override
    public void inData() {
        setFallback(getFileFormat() == FileFormat.XML ? structureProcessor : this::processCovarianceToken);
    }

    /** {@inheritDoc} */
    @Override
    public void finalizeData() {
        if (metadata != null) {
            currentBlock.checkMandatoryEntries();
            segments.add(new OEMSegment(metadata, currentBlock, getSelectedMu()));
        }
        metadata          = null;
        context           = null;
        currentBlock      = null;
        inCovariance      = false;
        currentCovariance = null;
        currentRow        = -1;
    }

    /** {@inheritDoc} */
    @Override
    public OEMFile build() {
        // OEM KVN file lack a DATA_STOP keyword, hence we can't call finalizeData()
        // automatically before the end of the file
        finalizeData();
        final OEMFile file = new OEMFile(header, segments, getConventions(), getDataContext(), getSelectedMu());
        file.checkTimeSystems();
        return file;
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
                return ODMMetadataKey.valueOf(token.getName()).process(token, context, metadata);
            } catch (IllegalArgumentException iaeD) {
                try {
                    return OCommonMetadataKey.valueOf(token.getName()).process(token, context, metadata);
                } catch (IllegalArgumentException iaeC) {
                    try {
                        return OEMMetadataKey.valueOf(token.getName()).process(token, context, metadata);
                    } catch (IllegalArgumentException iaeE) {
                        // token has not been recognized
                        return false;
                    }
                }
            }
        }
    }

    /** Process one data token.
     * @param token token to process
     * @return true if token was processed, false otherwise
     */
    private boolean processDataToken(final ParseToken token) {
        if (currentBlock == null) {
            // OEM KVN file lack a DATA_START keyword, hence we can't call prepareData()
            // automatically before the first data token arrives
            prepareData();
        }
        inData();
        if ("COMMENT".equals(token.getName())) {
            return token.getType() == TokenType.ENTRY ? currentBlock.addComment(token.getContent()) : true;
        } else if (token.getType() == TokenType.RAW_LINE) {
            try {
                final String[] fields = SPLIT_AT_BLANKS.split(token.getContent().trim());
                if (fields.length != 7 && fields.length != 10) {
                    throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                              token.getLineNumber(), token.getFileName(), token.getContent());
                }
                final boolean hasAcceleration = fields.length == 10;
                final AbsoluteDate epoch = context.getTimeScale().parseDate(fields[0],
                                                                            context.getConventions(),
                                                                            context.getMissionReferenceDate(),
                                                                            context.getDataContext().getTimeScales());
                final Vector3D position = new Vector3D(Double.parseDouble(fields[1]) * 1000,
                                                       Double.parseDouble(fields[2]) * 1000,
                                                       Double.parseDouble(fields[3]) * 1000);
                final Vector3D velocity = new Vector3D(Double.parseDouble(fields[4]) * 1000,
                                                       Double.parseDouble(fields[5]) * 1000,
                                                       Double.parseDouble(fields[6]) * 1000);
                final Vector3D acceleration = hasAcceleration ?
                                              new Vector3D(Double.parseDouble(fields[7]) * 1000,
                                                           Double.parseDouble(fields[8]) * 1000,
                                                           Double.parseDouble(fields[9]) * 1000) :
                                              Vector3D.ZERO;
                return currentBlock.addData(new TimeStampedPVCoordinates(epoch, position, velocity, acceleration),
                                            hasAcceleration);
            } catch (NumberFormatException nfe) {
                throw new OrekitException(nfe, OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                          token.getLineNumber(), token.getFileName(), token.getContent());
            }
        } else {
            // not a raw line, it is most probably either the end of the data section or a covariance section
            return false;
        }
    }

    /** Process one covariance token.
     * @param token token to process
     * @return true if token was processed, false otherwise
     */
    private boolean processCovarianceToken(final ParseToken token) {
        setFallback(getFileFormat() == FileFormat.XML ? structureProcessor : this::processMetadataToken);
        if (token.getName() != null) {
            if (OEMFile.COVARIANCE_KVN.equals(token.getName()) ||
                OEMFile.COVARIANCE_XML.equals(token.getName())) {
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
                    final OCommonMetadata savedMetadata = metadata;
                    currentCovariance = new ODMCovariance(() -> savedMetadata.getFrame(),  () -> savedMetadata.getRefCCSDSFrame());
                    currentRow        = 0;
                }

                // parse the token
                try {
                    return ODMCovarianceKey.valueOf(token.getName()).
                           process(token, context, currentCovariance);
                } catch (IllegalArgumentException iae) {
                    // token not recognized
                    return false;
                }

            }
        } else {
            // this is a raw line
            try {
                final String[] fields = SPLIT_AT_BLANKS.split(token.getContent().trim());
                if (fields.length != currentRow + 1) {
                    throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                              token.getLineNumber(), token.getFileName(), token.getContent());
                }
                for (int j = 0; j < fields.length; ++j) {
                    currentCovariance.setCovarianceMatrixEntry(currentRow, j, Double.parseDouble(fields[j]));
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
                                          token.getLineNumber(), token.getFileName(), token.getContent());
            }
        }
    }

}
