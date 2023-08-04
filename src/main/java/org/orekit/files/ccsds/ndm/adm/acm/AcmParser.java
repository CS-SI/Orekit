/* Copyright 2023 Luc Maisonobe
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
package org.orekit.files.ccsds.ndm.adm.acm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;

import org.orekit.data.DataContext;
import org.orekit.data.DataSource;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.ndm.ParsedUnitsBehavior;
import org.orekit.files.ccsds.ndm.adm.AdmHeader;
import org.orekit.files.ccsds.ndm.adm.AdmMetadataKey;
import org.orekit.files.ccsds.ndm.adm.AdmParser;
import org.orekit.files.ccsds.ndm.odm.UserDefined;
import org.orekit.files.ccsds.ndm.odm.ocm.Ocm;
import org.orekit.files.ccsds.section.HeaderProcessingState;
import org.orekit.files.ccsds.section.KvnStructureProcessingState;
import org.orekit.files.ccsds.section.MetadataKey;
import org.orekit.files.ccsds.section.Segment;
import org.orekit.files.ccsds.section.XmlStructureProcessingState;
import org.orekit.files.ccsds.utils.ContextBinding;
import org.orekit.files.ccsds.utils.FileFormat;
import org.orekit.files.ccsds.utils.lexical.ParseToken;
import org.orekit.files.ccsds.utils.lexical.TokenType;
import org.orekit.files.ccsds.utils.lexical.UserDefinedXmlTokenBuilder;
import org.orekit.files.ccsds.utils.lexical.XmlTokenBuilder;
import org.orekit.files.ccsds.utils.parsing.ProcessingState;
import org.orekit.files.general.AttitudeEphemerisFileParser;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.IERSConventions;

/** A parser for the CCSDS ACM (Attitude Comprehensive Message).
 * @author Luc Maisonobe
 * @since 12.0
 */
public class AcmParser extends AdmParser<Acm, AcmParser> implements AttitudeEphemerisFileParser<Acm> {

    /** Pattern for splitting strings at blanks. */
    private static final Pattern SPLIT_AT_BLANKS = Pattern.compile("\\s+");

    /** File header. */
    private AdmHeader header;

    /** Metadata for current observation block. */
    private AcmMetadata metadata;

    /** Context binding valid for current metadata. */
    private ContextBinding context;

    /** Attitude state histories logical blocks. */
    private List<AttitudeStateHistory> attitudeBlocks;

    /** Current attitude state metadata. */
    private AttitudeStateHistoryMetadata currentAttitudeStateHistoryMetadata;

    /** Current attitude state time history being read. */
    private List<AttitudeState> currentAttitudeStateHistory;

    /** Physical properties logical block. */
    private AttitudePhysicalProperties physicBlock;

    /** Covariance logical blocks. */
    private List<AttitudeCovarianceHistory> covarianceBlocks;

    /** Current covariance metadata. */
    private AttitudeCovarianceHistoryMetadata currentCovarianceHistoryMetadata;

    /** Current covariance history being read. */
    private List<AttitudeCovariance> currentCovarianceHistory;

    /** Maneuver logical blocks. */
    private List<AttitudeManeuver> maneuverBlocks;

    /** Current maneuver history being read. */
    private AttitudeManeuver currentManeuver;

    /** Attitude determination logical block. */
    private AttitudeDetermination attitudeDeterminationBlock;

    /** Attitude determination sensor logical block. */
    private AttitudeDeterminationSensor attitudeDeterminationSensorBlock;

    /** User defined parameters logical block. */
    private UserDefined userDefinedBlock;

    /** Processor for global message structure. */
    private ProcessingState structureProcessor;

    /**
     * Complete constructor.
     * <p>
     * Calling this constructor directly is not recommended. Users should rather use
     * {@link org.orekit.files.ccsds.ndm.ParserBuilder#buildAcmParser()
     * parserBuilder.buildAcmParser()}.
     * </p>
     * @param conventions IERS Conventions
     * @param simpleEOP if true, tidal effects are ignored when interpolating EOP
     * @param dataContext used to retrieve frames, time scales, etc.
     * @param parsedUnitsBehavior behavior to adopt for handling parsed units
     * @param filters filters to apply to parse tokens
     * @since 12.0
     */
    public AcmParser(final IERSConventions conventions, final boolean simpleEOP, final DataContext dataContext,
                     final ParsedUnitsBehavior parsedUnitsBehavior,
                     final Function<ParseToken, List<ParseToken>>[] filters) {
        super(Acm.ROOT, Acm.FORMAT_VERSION_KEY, conventions, simpleEOP, dataContext, null,
              parsedUnitsBehavior, filters);
    }

    /** {@inheritDoc} */
    @Override
    public Map<String, XmlTokenBuilder> getSpecialXmlElementsBuilders() {

        final Map<String, XmlTokenBuilder> builders = super.getSpecialXmlElementsBuilders();

        // special handling of user-defined parameters
        builders.put(UserDefined.USER_DEFINED_XML_TAG, new UserDefinedXmlTokenBuilder());

        return builders;

    }

    /** {@inheritDoc} */
    @Override
    public Acm parse(final DataSource source) {
        return parseMessage(source);
    }

    /** {@inheritDoc} */
    @Override
    public AdmHeader getHeader() {
        return header;
    }

    /** {@inheritDoc} */
    @Override
    public void reset(final FileFormat fileFormat) {
        header                     = new AdmHeader();
        metadata                   = null;
        context                    = null;
        attitudeBlocks             = null;
        physicBlock                = null;
        covarianceBlocks           = null;
        maneuverBlocks             = null;
        attitudeDeterminationBlock = null;
        userDefinedBlock           = null;
        if (fileFormat == FileFormat.XML) {
            structureProcessor = new XmlStructureProcessingState(Acm.ROOT, this);
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
        if (metadata != null) {
            return false;
        }
        metadata  = new AcmMetadata(getDataContext());
        context   = new ContextBinding(this::getConventions, this::isSimpleEOP, this::getDataContext,
                                       this::getParsedUnitsBehavior, metadata::getEpochT0, metadata::getTimeSystem,
                                       () -> 0.0, () -> 1.0);
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
        metadata.validate(header.getFormatVersion());
        anticipateNext(this::processDataSubStructureToken);
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean prepareData() {
        anticipateNext(this::processDataSubStructureToken);
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean inData() {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean finalizeData() {
        return true;
    }

    /** Manage attitude state history section.
     * @param starting if true, parser is entering the section
     * otherwise it is leaving the section
     * @return always return true
     */
    boolean manageAttitudeStateSection(final boolean starting) {
        if (starting) {
            if (attitudeBlocks == null) {
                // this is the first attitude block, we need to allocate the container
                attitudeBlocks = new ArrayList<>();
            }
            currentAttitudeStateHistoryMetadata = new AttitudeStateHistoryMetadata();
            currentAttitudeStateHistory         = new ArrayList<>();
            anticipateNext(this::processAttitudeStateToken);
        } else {
            anticipateNext(structureProcessor);
            attitudeBlocks.add(new AttitudeStateHistory(currentAttitudeStateHistoryMetadata,
                                                        currentAttitudeStateHistory));
        }
        return true;
    }

    /** Manage physical properties section.
     * @param starting if true, parser is entering the section
     * otherwise it is leaving the section
     * @return always return true
     */
    boolean managePhysicalPropertiesSection(final boolean starting) {
        if (starting) {
            physicBlock = new AttitudePhysicalProperties(metadata.getEpochT0());
            anticipateNext(this::processPhysicalPropertyToken);
        } else {
            anticipateNext(structureProcessor);
        }
        return true;
    }

    /** Manage covariance history section.
     * @param starting if true, parser is entering the section
     * otherwise it is leaving the section
     * @return always return true
     */
    boolean manageCovarianceHistorySection(final boolean starting) {
        if (starting) {
            if (covarianceBlocks == null) {
                // this is the first covariance block, we need to allocate the container
                covarianceBlocks = new ArrayList<>();
            }
            currentCovarianceHistoryMetadata = new AttitudeCovarianceHistoryMetadata();
            currentCovarianceHistory         = new ArrayList<>();
            anticipateNext(this::processCovarianceToken);
        } else {
            anticipateNext(structureProcessor);
            covarianceBlocks.add(new AttitudeCovarianceHistory(currentCovarianceHistoryMetadata,
                                                               currentCovarianceHistory));
            currentCovarianceHistoryMetadata = null;
            currentCovarianceHistory         = null;
        }
        return true;
    }

    /** Manage maneuvers section.
     * @param starting if true, parser is entering the section
     * otherwise it is leaving the section
     * @return always return true
     */
    boolean manageManeuversSection(final boolean starting) {
        if (starting) {
            if (maneuverBlocks == null) {
                // this is the first maneuver block, we need to allocate the container
                maneuverBlocks = new ArrayList<>();
            }
            currentManeuver = new AttitudeManeuver();
            anticipateNext(this::processManeuverToken);
        } else {
            anticipateNext(structureProcessor);
            maneuverBlocks.add(currentManeuver);
            currentManeuver = null;
        }
        return true;
    }

    /** Manage attitude determination section.
     * @param starting if true, parser is entering the section
     * otherwise it is leaving the section
     * @return always return true
     */
    boolean manageAttitudeDeterminationSection(final boolean starting) {
        if (starting) {
            attitudeDeterminationBlock = new AttitudeDetermination();
            anticipateNext(this::processAttitudeDeterminationToken);
        } else {
            anticipateNext(structureProcessor);
        }
        return true;
    }

    /** Manage attitude determination sensor section.
     * @param starting if true, parser is entering the section
     * otherwise it is leaving the section
     * @return always return true
     */
    boolean manageAttitudeDeterminationSensorSection(final boolean starting) {
        if (starting) {
            attitudeDeterminationSensorBlock = new AttitudeDeterminationSensor();
            anticipateNext(this::processAttitudeDeterminationSensorToken);
        } else {
            anticipateNext(this::processDataSubStructureToken);
        }
        return true;
    }

    /** Manage user-defined parameters section.
     * @param starting if true, parser is entering the section
     * otherwise it is leaving the section
     * @return always return true
     */
    boolean manageUserDefinedParametersSection(final boolean starting) {
        if (starting) {
            userDefinedBlock = new UserDefined();
            anticipateNext(this::processUserDefinedToken);
        } else {
            anticipateNext(structureProcessor);
        }
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public Acm build() {

        if (userDefinedBlock != null && userDefinedBlock.getParameters().isEmpty()) {
            userDefinedBlock = null;
        }

        final AcmData data = new AcmData(attitudeBlocks, physicBlock, covarianceBlocks,
                                         maneuverBlocks, attitudeDeterminationBlock, userDefinedBlock);
        data.validate(header.getFormatVersion());

        return new Acm(header, Collections.singletonList(new Segment<>(metadata, data)),
                           getConventions(), getDataContext());

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
                    return AcmMetadataKey.valueOf(token.getName()).process(token, context, metadata);
                } catch (IllegalArgumentException iaeC) {
                    // token has not been recognized
                    return false;
                }
            }
        }
    }

    /** Process one data substructure token.
     * @param token token to process
     * @return true if token was processed, false otherwise
     */
    private boolean processDataSubStructureToken(final ParseToken token) {
        try {
            return token.getName() != null &&
                   AcmDataSubStructureKey.valueOf(token.getName()).process(token, this);
        } catch (IllegalArgumentException iae) {
            // token has not been recognized
            return false;
        }
    }

    /** Process one attitude state history data token.
     * @param token token to process
     * @return true if token was processed, false otherwise
     */
    private boolean processAttitudeStateToken(final ParseToken token) {
        if (token.getName() != null && !token.getName().equals(Acm.ATT_LINE)) {
            // we are in the section metadata part
            try {
                return AttitudeStateHistoryMetadataKey.valueOf(token.getName()).
                       process(token, context, currentAttitudeStateHistoryMetadata);
            } catch (IllegalArgumentException iae) {
                // token has not been recognized
                return false;
            }
        } else {
            // we are in the section data part
            if (currentAttitudeStateHistory.isEmpty()) {
                // we are starting the real data section, we can now check metadata is complete
                currentAttitudeStateHistoryMetadata.validate(header.getFormatVersion());
                anticipateNext(this::processDataSubStructureToken);
            }
            if (token.getType() == TokenType.START || token.getType() == TokenType.STOP) {
                return true;
            }
            try {
                final String[] fields = SPLIT_AT_BLANKS.split(token.getRawContent().trim());
                final AbsoluteDate epoch = context.getTimeSystem().getConverter(context).parse(fields[0]);
                return currentAttitudeStateHistory.add(new AttitudeState(currentAttitudeStateHistoryMetadata.getAttitudeType(),
                                                                         currentAttitudeStateHistoryMetadata.getRateType(),
                                                                         epoch, fields, 1));
            } catch (NumberFormatException | OrekitIllegalArgumentException e) {
                throw new OrekitException(e, OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                          token.getLineNumber(), token.getFileName(), token.getContentAsNormalizedString());
            }
        }
    }

    /** Process one physical property data token.
     * @param token token to process
     * @return true if token was processed, false otherwise
     */
    private boolean processPhysicalPropertyToken(final ParseToken token) {
        anticipateNext(this::processDataSubStructureToken);
        try {
            return token.getName() != null &&
                   AttitudePhysicalPropertiesKey.valueOf(token.getName()).process(token, context, physicBlock);
        } catch (IllegalArgumentException iae) {
            // token has not been recognized
            return false;
        }
    }

    /** Process one covariance history history data token.
     * @param token token to process
     * @return true if token was processed, false otherwise
     */
    private boolean processCovarianceToken(final ParseToken token) {
        if (token.getName() != null && !token.getName().equals(Ocm.COV_LINE)) {
            // we are in the section metadata part
            try {
                return AttitudeCovarianceHistoryMetadataKey.valueOf(token.getName()).
                       process(token, context, currentCovarianceHistoryMetadata);
            } catch (IllegalArgumentException iae) {
                // token has not been recognized
                return false;
            }
        } else {
            // we are in the section data part
            if (currentCovarianceHistory.isEmpty()) {
                // we are starting the real data section, we can now check metadata is complete
                currentCovarianceHistoryMetadata.validate(header.getFormatVersion());
                anticipateNext(this::processDataSubStructureToken);
            }
            if (token.getType() == TokenType.START || token.getType() == TokenType.STOP) {
                return true;
            }
            try {
                final String[] fields = SPLIT_AT_BLANKS.split(token.getRawContent().trim());
                currentCovarianceHistory.add(new AttitudeCovariance(currentCovarianceHistoryMetadata.getCovType(),
                                                                    context.getTimeSystem().getConverter(context).parse(fields[0]),
                                                                    fields, 1));
                return true;
            } catch (NumberFormatException nfe) {
                throw new OrekitException(nfe, OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                          token.getLineNumber(), token.getFileName(), token.getContentAsNormalizedString());
            }
        }
    }

    /** Process one maneuver data token.
     * @param token token to process
     * @return true if token was processed, false otherwise
     */
    private boolean processManeuverToken(final ParseToken token) {
        anticipateNext(this::processDataSubStructureToken);
        try {
            return token.getName() != null &&
                   AttitudeManeuverKey.valueOf(token.getName()).process(token, context, currentManeuver);
        } catch (IllegalArgumentException iae) {
            // token has not been recognized
            return false;
        }
    }

    /** Process one attitude determination data token.
     * @param token token to process
     * @return true if token was processed, false otherwise
     */
    private boolean processAttitudeDeterminationToken(final ParseToken token) {
        anticipateNext(attitudeDeterminationSensorBlock != null ?
                       this::processAttitudeDeterminationSensorToken :
                       this::processDataSubStructureToken);
        if (token.getName() == null) {
            return false;
        }
        try {
            return AttitudeDeterminationKey.valueOf(token.getName()).process(token, this, context, attitudeDeterminationBlock);
        } catch (IllegalArgumentException iae1) {
            // token has not been recognized
            return false;
        }
    }

    /** Process one attitude determination sensor data token.
     * @param token token to process
     * @return true if token was processed, false otherwise
     */
    private boolean processAttitudeDeterminationSensorToken(final ParseToken token) {
        anticipateNext(this::processAttitudeDeterminationToken);
        if (token.getName() == null) {
            return false;
        }
        try {
            return AttitudeDeterminationSensorKey.valueOf(token.getName()).process(token, context, attitudeDeterminationSensorBlock);
        } catch (IllegalArgumentException iae1) {
            // token has not been recognized
            attitudeDeterminationBlock.addSensor(attitudeDeterminationSensorBlock);
            attitudeDeterminationSensorBlock = null;
            return false;
        }
    }

    /** Process one user-defined parameter data token.
     * @param token token to process
     * @return true if token was processed, false otherwise
     */
    private boolean processUserDefinedToken(final ParseToken token) {
        anticipateNext(this::processDataSubStructureToken);
        if ("COMMENT".equals(token.getName())) {
            return token.getType() == TokenType.ENTRY ? userDefinedBlock.addComment(token.getContentAsNormalizedString()) : true;
        } else if (token.getName().startsWith(UserDefined.USER_DEFINED_PREFIX)) {
            if (token.getType() == TokenType.ENTRY) {
                userDefinedBlock.addEntry(token.getName().substring(UserDefined.USER_DEFINED_PREFIX.length()),
                                          token.getContentAsNormalizedString());
            }
            return true;
        } else {
            // the token was not processed
            return false;
        }
    }

}
