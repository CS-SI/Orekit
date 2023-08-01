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
package org.orekit.files.ccsds.ndm.cdm;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.orekit.data.DataContext;
import org.orekit.files.ccsds.definitions.TimeSystem;
import org.orekit.files.ccsds.ndm.ParsedUnitsBehavior;
import org.orekit.files.ccsds.ndm.odm.UserDefined;
import org.orekit.files.ccsds.section.CommentsContainer;
import org.orekit.files.ccsds.section.KvnStructureProcessingState;
import org.orekit.files.ccsds.section.MetadataKey;
import org.orekit.files.ccsds.section.XmlStructureProcessingState;
import org.orekit.files.ccsds.utils.ContextBinding;
import org.orekit.files.ccsds.utils.FileFormat;
import org.orekit.files.ccsds.utils.lexical.ParseToken;
import org.orekit.files.ccsds.utils.lexical.TokenType;
import org.orekit.files.ccsds.utils.parsing.AbstractConstituentParser;
import org.orekit.files.ccsds.utils.parsing.ProcessingState;
import org.orekit.utils.IERSConventions;

/**
 * Base class for Conjunction Data Message parsers.
 * <p>
 * Note than starting with Orekit 11.0, CCSDS message parsers are
 * mutable objects that gather the data being parsed, until the
 * message is complete and the {@link #parseMessage(org.orekit.data.DataSource)
 * parseMessage} method has returned. This implies that parsers
 * should <em>not</em> be used in a multi-thread context. The recommended
 * way to use parsers is to either dedicate one parser for each message
 * and drop it afterwards, or to use a single-thread loop.
 * </p>
 * @author Melina Vanel
 * @since 11.2
 */
public class CdmParser extends AbstractConstituentParser<CdmHeader, Cdm, CdmParser> {

    /** Comment key. */
    private static String COMMENT = "COMMENT";

    /** XML relative metadata key. */
    private static String RELATIVEMETADATA = "relativeMetadataData";

    /** XML metadata key. */
    private static String METADATA = "metadata";

    /** File header. */
    private CdmHeader header;

    /** File segments. */
    private List<CdmSegment> segments;

    /** CDM metadata being read. */
    private CdmMetadata metadata;

    /** CDM relative metadata being read. */
    private CdmRelativeMetadata relativeMetadata;

    /** Context binding valid for current metadata. */
    private ContextBinding context;

    /** CDM general data comments block being read. */
    private CommentsContainer commentsBlock;

    /** CDM OD parameters logical block being read. */
    private ODParameters odParameters;

    /** CDM additional parameters logical block being read. */
    private AdditionalParameters addParameters;

    /** CDM state vector logical block being read. */
    private StateVector stateVector;

    /** CDM covariance matrix logical block being read. */
    private RTNCovariance covMatrix;

    /** CDM XYZ covariance matrix logical block being read. */
    private XYZCovariance xyzCovMatrix;

    /** CDM Sigma/Eigenvectors covariance logical block being read. */
    private SigmaEigenvectorsCovariance sig3eigvec3;

    /** CDM additional covariance metadata logical block being read. */
    private AdditionalCovarianceMetadata additionalCovMetadata;

    /** Processor for global message structure. */
    private ProcessingState structureProcessor;

    /** Flag to only compute once relative metadata. */
    private boolean doRelativeMetadata;

    /** Flag to indicate that data block parsing is finished. */
    private boolean isDatafinished;

    /** CDM user defined logical block being read. */
    private UserDefined userDefinedBlock;


    /** Complete constructor.
     * <p>
     * Calling this constructor directly is not recommended. Users should rather use
     * {@link org.orekit.files.ccsds.ndm.ParserBuilder#buildCdmParser()
     * parserBuilder.buildCdmParser()}.
     * </p>
     * @param conventions IERS Conventions
     * @param simpleEOP if true, tidal effects are ignored when interpolating EOP
     * @param dataContext used to retrieve frames, time scales, etc.
     * @param parsedUnitsBehavior behavior to adopt for handling parsed units
     * @param filters filters to apply to parse tokens
     * @since 12.0
     */
    public CdmParser(final IERSConventions conventions, final boolean simpleEOP, final DataContext dataContext,
                     final ParsedUnitsBehavior parsedUnitsBehavior,
                     final Function<ParseToken, List<ParseToken>>[] filters) {
        super(Cdm.ROOT, Cdm.FORMAT_VERSION_KEY, conventions, simpleEOP, dataContext, parsedUnitsBehavior, filters);
        this.doRelativeMetadata = true;
        this.isDatafinished = false;
    }

    /** {@inheritDoc} */
    @Override
    public CdmHeader getHeader() {
        return header;
    }

    /** {@inheritDoc} */
    @Override
    public void reset(final FileFormat fileFormat) {
        header                    = new CdmHeader();
        segments                  = new ArrayList<>();
        metadata                  = null;
        relativeMetadata          = null;
        context                   = null;
        odParameters              = null;
        addParameters             = null;
        stateVector               = null;
        covMatrix                 = null;
        xyzCovMatrix              = null;
        sig3eigvec3               = null;
        additionalCovMetadata     = null;
        userDefinedBlock          = null;
        commentsBlock             = null;
        if (fileFormat == FileFormat.XML) {
            structureProcessor = new XmlStructureProcessingState(Cdm.ROOT, this);
            reset(fileFormat, structureProcessor);
        } else {
            structureProcessor = new KvnStructureProcessingState(this);
            reset(fileFormat, new CdmHeaderProcessingState(this));
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean prepareHeader() {
        anticipateNext(new CdmHeaderProcessingState(this));
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean inHeader() {
        anticipateNext(getFileFormat() == FileFormat.XML ? structureProcessor : this::processMetadataToken);
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean finalizeHeader() {
        anticipateNext(getFileFormat() == FileFormat.XML ? this::processXmlSubStructureToken : structureProcessor);
        header.validate(header.getFormatVersion());
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean prepareMetadata() {
        if (metadata != null) {
            return false;
        }
        if (doRelativeMetadata) {
            // if parser is just after header it is time to create / read relative metadata,
            // their are only initialized once and then shared between metadata for object 1 and 2
            relativeMetadata = new CdmRelativeMetadata();
            relativeMetadata.setTimeSystem(TimeSystem.UTC);
        }
        metadata  = new CdmMetadata(getDataContext());
        metadata.setRelativeMetadata(relativeMetadata);

        // As no time system is defined in CDM because all dates are given in UTC,
        // time system is set here to UTC, we use relative metadata and not metadata
        // because setting time system on metadata implies refusingfurthercomments
        // witch would be a problem as metadata comments have not been read yet.
        context   = new ContextBinding(this::getConventions, this::isSimpleEOP,
                                       this::getDataContext, this::getParsedUnitsBehavior,
                                       () -> null, relativeMetadata::getTimeSystem,
                                       () -> 0.0, () -> 1.0);
        anticipateNext(this::processMetadataToken);
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean inMetadata() {
        anticipateNext(getFileFormat() == FileFormat.XML ? this::processXmlSubStructureToken : this::processGeneralCommentToken);
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean finalizeMetadata() {
        metadata.validate(header.getFormatVersion());
        relativeMetadata.validate();
        anticipateNext(structureProcessor);
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean prepareData() {
        // stateVector and RTNCovariance blocks are 2 mandatory data blocks
        stateVector = new StateVector();
        covMatrix = new RTNCovariance();

        // initialize comments block for general data comments
        commentsBlock = new CommentsContainer();
        anticipateNext(getFileFormat() == FileFormat.XML ? this::processXmlSubStructureToken : this::processGeneralCommentToken);
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
        // call at the and of data block for object 1 or 2
        if (metadata != null) {

            CdmData data = new CdmData(commentsBlock, odParameters, addParameters,
                                             stateVector, covMatrix, additionalCovMetadata);

            if (metadata.getAltCovType() != null && metadata.getAltCovType() == AltCovarianceType.XYZ) {
                data = new CdmData(commentsBlock, odParameters, addParameters,
                                             stateVector, covMatrix, xyzCovMatrix, additionalCovMetadata);
            } else if (metadata.getAltCovType() != null && metadata.getAltCovType() == AltCovarianceType.CSIG3EIGVEC3) {
                data = new CdmData(commentsBlock, odParameters, addParameters,
                                             stateVector, covMatrix, sig3eigvec3, additionalCovMetadata);
            }

            data.validate(header.getFormatVersion());
            segments.add(new CdmSegment(metadata, data));

            // Add the user defined block to both Objects data sections
            if (userDefinedBlock != null && !userDefinedBlock.getParameters().isEmpty()) {
                for (int i = 0; i < segments.size(); i++) {
                    segments.get(i).getData().setUserDefinedBlock(userDefinedBlock);
                }
            }
        }
        metadata                  = null;
        context                   = null;
        odParameters              = null;
        addParameters             = null;
        stateVector               = null;
        covMatrix                 = null;
        xyzCovMatrix              = null;
        sig3eigvec3               = null;
        additionalCovMetadata     = null;
        userDefinedBlock          = null;
        commentsBlock             = null;
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public Cdm build() {
        // CDM KVN file lack a DATA_STOP keyword, hence we can't call finalizeData()
        // automatically before the end of the file
        finalizeData();
        if (userDefinedBlock != null && userDefinedBlock.getParameters().isEmpty()) {
            userDefinedBlock = null;
        }
        final Cdm file = new Cdm(header, segments, getConventions(), getDataContext());
        return file;
    }

    /** Add a general comment.
     * @param comment comment to add
     * @return always return true
     */
    boolean addGeneralComment(final String comment) {
        return commentsBlock.addComment(comment);

    }

    /** Manage relative metadata section.
     * @param starting if true, parser is entering the section
     * otherwise it is leaving the section
     * @return always return true
     */
    boolean manageRelativeMetadataSection(final boolean starting) {
        anticipateNext(starting ? this::processMetadataToken : structureProcessor);
        return true;
    }

    /** Manage relative metadata state vector section.
     * @param starting if true, parser is entering the section
     * otherwise it is leaving the section
     * @return always return true
     */
    boolean manageRelativeStateVectorSection(final boolean starting) {
        anticipateNext(this::processMetadataToken);
        return true;
    }

    /** Manage OD parameters section.
     * @param starting if true, parser is entering the section
     * otherwise it is leaving the section
     * @return always return true
     */
    boolean manageODParametersSection(final boolean starting) {
        commentsBlock.refuseFurtherComments();
        anticipateNext(starting ? this::processODParamToken : structureProcessor);
        return true;
    }

    /** Manage additional parameters section.
     * @param starting if true, parser is entering the section
     * otherwise it is leaving the section
     * @return always return true
     */
    boolean manageAdditionalParametersSection(final boolean starting) {
        commentsBlock.refuseFurtherComments();
        anticipateNext(starting ? this::processAdditionalParametersToken : structureProcessor);
        return true;
    }

    /** Manage state vector section.
     * @param starting if true, parser is entering the section
     * otherwise it is leaving the section
     * @return always return true
     */
    boolean manageStateVectorSection(final boolean starting) {
        commentsBlock.refuseFurtherComments();
        anticipateNext(starting ? this::processStateVectorToken : structureProcessor);
        return true;
    }

    /** Manage general covariance section in XML file.
     * @param starting if true, parser is entering the section
     * otherwise it is leaving the section
     * @return always return true
     */
    boolean manageXmlGeneralCovarianceSection(final boolean starting) {
        commentsBlock.refuseFurtherComments();

        if (starting) {
            if (metadata.getAltCovType() == null) {
                anticipateNext(this::processCovMatrixToken);
            } else {
                if (Double.isNaN(covMatrix.getCrr())) {
                    // First, handle mandatory RTN covariance section
                    anticipateNext(this::processCovMatrixToken);
                } else if ( metadata.getAltCovType() == AltCovarianceType.XYZ && xyzCovMatrix == null ||
                                metadata.getAltCovType() == AltCovarianceType.CSIG3EIGVEC3 && sig3eigvec3 == null ) {
                    // Second, add the alternate covariance if provided
                    anticipateNext(this::processAltCovarianceToken);
                } else if (additionalCovMetadata == null) {
                    // Third, process the additional covariance metadata
                    anticipateNext(this::processAdditionalCovMetadataToken);
                }
            }
        } else {
            anticipateNext(structureProcessor);
        }

        return true;
    }

    /** Manage user-defined parameters section.
     * @param starting if true, parser is entering the section
     * otherwise it is leaving the section
     * @return always return true
     */
    boolean manageUserDefinedParametersSection(final boolean starting) {
        commentsBlock.refuseFurtherComments();
        if (starting) {
            if (userDefinedBlock == null) {
                // this is the first (and unique) user-defined parameters block, we need to allocate the container
                userDefinedBlock = new UserDefined();
            }
            anticipateNext(this::processUserDefinedToken);
        } else {
            anticipateNext(structureProcessor);
        }
        return true;
    }

    /** Process one metadata token.
     * @param token token to process
     * @return true if token was processed, false otherwise
     */
    private boolean processMetadataToken(final ParseToken token) {
        if (isDatafinished && getFileFormat() != FileFormat.XML) {
            finalizeData();
            isDatafinished = false;
        }
        if (metadata == null) {
            // CDM KVN file lack a META_START keyword, hence we can't call prepareMetadata()
            // automatically before the first metadata token arrives
            prepareMetadata();
        }
        inMetadata();

        // There can be a COMMENT key at the beginning of relative metadata, but as the relative
        // metadata are processed in the same try and catch loop than metadata because Orekit is
        // build to read metadata and then data (and not relative metadata), it would be problematic
        // to make relative metadata extends comments container(because of the COMMENTS in the middle
        // of relativemetadata and metadata section. Indeed, as said in {@link
        // #CommentsContainer} COMMENT should only be at the beginning of sections but in this case
        // there is a comment at the beginning corresponding to the relative metadata comment
        // and 1 in the middle for object 1 metadata and one further for object 2 metadata. That
        // is why this special syntax was used and initializes the relative metadata COMMENT once
        // at the beginning as relative metadata is not a comment container
        if (COMMENT.equals(token.getName()) && doRelativeMetadata ) {
            if (token.getType() == TokenType.ENTRY) {
                relativeMetadata.addComment(token.getContentAsNormalizedString());
                return true;
            }
        }
        doRelativeMetadata = false;

        try {
            return token.getName() != null &&
                   CdmRelativeMetadataKey.valueOf(token.getName()).process(token, context, relativeMetadata);
        } catch (IllegalArgumentException iaeM) {
            try {
                return MetadataKey.valueOf(token.getName()).process(token, context, metadata);
            } catch (IllegalArgumentException iaeD) {
                try {
                    return CdmMetadataKey.valueOf(token.getName()).process(token, context, metadata);
                } catch (IllegalArgumentException iaeC) {
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

        // As no relativemetadata token exists in the structure processor and as RelativeMetadata keys are
        // processed in the same try and catch loop in processMetadatatoken as CdmMetadata keys, if the relativemetadata
        // token is read it should be as if the token was equal to metadata to start to initialize relative metadata
        // and metadata and to go in the processMetadataToken try and catch loop. The following relativemetadata
        // stop should be ignored to stay in the processMetadataToken try and catch loop and the following metadata
        // start also ignored to stay in the processMetadataToken try and catch loop. Then arrives the end of metadata
        // so we call structure processor with metadata stop. This distinction of cases is useful for relativemetadata
        // block followed by metadata block for object 1 and also useful to only close metadata block for object 2.
        // The metadata start for object 2 is processed by structureProcessor
        if (METADATA.equals(token.getName()) && TokenType.START.equals(token.getType()) ||
            RELATIVEMETADATA.equals(token.getName()) && TokenType.STOP.equals(token.getType())) {
            anticipateNext(this::processMetadataToken);
            return true;

        } else if (RELATIVEMETADATA.equals(token.getName()) && TokenType.START.equals(token.getType()) ||
                   METADATA.equals(token.getName()) && TokenType.STOP.equals(token.getType())) {
            final ParseToken replaceToken = new ParseToken(token.getType(), METADATA,
                                      null, token.getUnits(), token.getLineNumber(), token.getFileName());

            return structureProcessor.processToken(replaceToken);

        } else {

            // Relative metadata COMMENT and metadata COMMENT should not be read by XmlSubStructureKey that
            // is why 2 cases are distinguished here : the COMMENT for relative metadata and the COMMENT
            // for metadata.
            if (commentsBlock == null && COMMENT.equals(token.getName())) {

                // COMMENT adding for Relative Metadata in XML
                if (doRelativeMetadata) {
                    if (token.getType() == TokenType.ENTRY) {
                        relativeMetadata.addComment(token.getContentAsNormalizedString());
                        doRelativeMetadata = false;
                        return true;

                    } else {
                        // if the token Type is still not ENTRY we return true as at the next step
                        // it will be ENTRY ad we will be able to store the comment (similar treatment
                        // as OD parameter or Additional parameter or State Vector ... COMMENT treatment.)
                        return true;
                    }
                }

                // COMMENT adding for Metadata in XML
                if (!doRelativeMetadata) {
                    if (token.getType() == TokenType.ENTRY) {
                        metadata.addComment(token.getContentAsNormalizedString());
                        return true;

                    } else {
                        // same as above
                        return true;
                    }
                }
            }

            // to treat XmlSubStructureKey keys ( OD parameters, relative Metadata ...)
            try {
                return token.getName() != null && !doRelativeMetadata &&
                       XmlSubStructureKey.valueOf(token.getName()).process(token, this);
            } catch (IllegalArgumentException iae) {
                // token has not been recognized
                return false;
            }
        }
    }

    /** Process one comment token.
     * @param token token to process
     * @return true if token was processed, false otherwise
     */
    private boolean processGeneralCommentToken(final ParseToken token) {
        if (commentsBlock == null) {
            // CDM KVN file lack a META_STOP keyword, hence we can't call finalizeMetadata()
            // automatically before the first data token arrives
            finalizeMetadata();
            // CDM KVN file lack a DATA_START keyword, hence we can't call prepareData()
            // automatically before the first data token arrives
            prepareData();
        }
        anticipateNext(getFileFormat() == FileFormat.XML ? this::processXmlSubStructureToken : this::processODParamToken);
        if (COMMENT.equals(token.getName()) && commentsBlock.acceptComments()) {
            if (token.getType() == TokenType.ENTRY) {
                commentsBlock.addComment(token.getContentAsNormalizedString());
            }
            // in order to be able to differentiate general data comments and next block comment (OD parameters if not empty)
            // only 1 line comment is allowed for general data comment.
            commentsBlock.refuseFurtherComments();
            return true;
        } else {
            return false;
        }
    }

    /** Process one od parameter data token.
     * @param token token to process
     * @return true if token was processed, false otherwise
     */
    private boolean processODParamToken(final ParseToken token) {
        if (odParameters == null) {
            odParameters = new ODParameters();
        }
        anticipateNext(getFileFormat() == FileFormat.XML ? this::processXmlSubStructureToken : this::processAdditionalParametersToken);
        try {
            return token.getName() != null &&
                   ODParametersKey.valueOf(token.getName()).process(token, context, odParameters);
        } catch (IllegalArgumentException iae) {
            // token has not been recognized
            return false;
        }
    }

    /** Process one additional parameter data token.
     * @param token token to process
     * @return true if token was processed, false otherwise
     */
    private boolean processAdditionalParametersToken(final ParseToken token) {
        if (addParameters == null) {
            addParameters = new AdditionalParameters();
        }
        if (moveCommentsIfEmpty(odParameters, addParameters)) {
            // get rid of the empty logical block
            odParameters = null;
        }
        anticipateNext(getFileFormat() == FileFormat.XML ? this::processXmlSubStructureToken : this::processStateVectorToken);
        try {
            return token.getName() != null &&
                   AdditionalParametersKey.valueOf(token.getName()).process(token, context, addParameters);
        } catch (IllegalArgumentException iae) {
            // token has not been recognized
            return false;
        }
    }

    /** Process one state vector data token.
     * @param token token to process
     * @return true if token was processed, false otherwise
     */
    private boolean processStateVectorToken(final ParseToken token) {
        if (moveCommentsIfEmpty(addParameters, stateVector)) {
            // get rid of the empty logical block
            addParameters = null;
        }
        anticipateNext(getFileFormat() == FileFormat.XML ? this::processXmlSubStructureToken : this::processCovMatrixToken);
        try {
            return token.getName() != null &&
                   StateVectorKey.valueOf(token.getName()).process(token, context, stateVector);
        } catch (IllegalArgumentException iae) {
            // token has not been recognized
            return false;
        }
    }

    /** Process covariance matrix data token.
     * @param token token to process
     * @return true if token was processed, false otherwise
     */
    private boolean processCovMatrixToken(final ParseToken token) {

        if (moveCommentsIfEmpty(stateVector, covMatrix)) {
            // get rid of the empty logical block
            stateVector = null;
        }

        if (metadata.getAltCovType() == null) {
            anticipateNext(getFileFormat() == FileFormat.XML ? this::processXmlSubStructureToken : this::processMetadataToken);
        } else {
            anticipateNext(getFileFormat() == FileFormat.XML ? this::processXmlSubStructureToken : this::processAltCovarianceToken);
        }

        isDatafinished = true;
        try {
            return token.getName() != null &&
                   RTNCovarianceKey.valueOf(token.getName()).process(token, context, covMatrix);
        } catch (IllegalArgumentException iae) {
            // token has not been recognized
            return false;
        }
    }

    /** Process alternate covariance data token.
     * @param token token to process
     * @return true if token was processed, false otherwise
     */
    private boolean processAltCovarianceToken(final ParseToken token) {

        // Covariance is provided in XYZ
        if (metadata.getAltCovType() == AltCovarianceType.XYZ && xyzCovMatrix == null) {
            xyzCovMatrix = new XYZCovariance(true);

            if (moveCommentsIfEmpty(covMatrix, xyzCovMatrix)) {
                // get rid of the empty logical block
                covMatrix = null;
            }
        }
        // Covariance is provided in CSIG3EIGVEC3 format
        if (metadata.getAltCovType() == AltCovarianceType.CSIG3EIGVEC3 && sig3eigvec3 == null) {
            sig3eigvec3 = new SigmaEigenvectorsCovariance(true);

            if (moveCommentsIfEmpty(covMatrix, sig3eigvec3)) {
                // get rid of the empty logical block
                covMatrix = null;
            }
        }


        anticipateNext(getFileFormat() == FileFormat.XML ? this::processXmlSubStructureToken : this::processAdditionalCovMetadataToken);
        try {

            if (metadata.getAltCovType() != null && metadata.getAltCovType() == AltCovarianceType.XYZ) {

                return token.getName() != null &&
                           XYZCovarianceKey.valueOf(token.getName()).process(token, context, xyzCovMatrix);

            } else if (metadata.getAltCovType() != null && metadata.getAltCovType() == AltCovarianceType.CSIG3EIGVEC3) {

                return token.getName() != null &&
                           SigmaEigenvectorsCovarianceKey.valueOf(token.getName()).process(token, context, sig3eigvec3);

            } else {

                // token has not been recognized
                return false;

            }

        } catch (IllegalArgumentException iae) {
            // token has not been recognized
            return false;
        }
    }

    /** Process additional covariance metadata token.
     * @param token token to process
     * @return true if token was processed, false otherwise
     */
    private boolean processAdditionalCovMetadataToken(final ParseToken token) {

        // Additional covariance metadata
        if ( additionalCovMetadata == null) {
            additionalCovMetadata = new AdditionalCovarianceMetadata();
        }

        if (moveCommentsIfEmpty(xyzCovMatrix, additionalCovMetadata)) {
            // get rid of the empty logical block
            xyzCovMatrix = null;
        } else if (moveCommentsIfEmpty(sig3eigvec3, additionalCovMetadata)) {
            // get rid of the empty logical block
            sig3eigvec3 = null;
        }

        anticipateNext(getFileFormat() == FileFormat.XML ? this::processXmlSubStructureToken : this::processUserDefinedToken);
        try {
            return token.getName() != null &&
                           AdditionalCovarianceMetadataKey.valueOf(token.getName()).process(token, context, additionalCovMetadata);
        } catch (IllegalArgumentException iae) {
            // token has not been recognized
            return false;
        }
    }

    /** Process one user-defined parameter data token.
     * @param token token to process
     * @return true if token was processed, false otherwise
     */
    private boolean processUserDefinedToken(final ParseToken token) {

        if (userDefinedBlock == null) {
            userDefinedBlock = new UserDefined();
        }

        if (moveCommentsIfEmpty(additionalCovMetadata, userDefinedBlock)) {
            // get rid of the empty logical block
            additionalCovMetadata = null;
        }

        anticipateNext(getFileFormat() == FileFormat.XML ? this::processXmlSubStructureToken : this::processMetadataToken);

        if (COMMENT.equals(token.getName())) {
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


    /** Move comments from one empty logical block to another logical block.
     * @param origin origin block
     * @param destination destination block
     * @return true if origin block was empty
     */
    private boolean moveCommentsIfEmpty(final CommentsContainer origin, final CommentsContainer destination) {
        if (origin != null && origin.acceptComments()) {
            // origin block is empty, move the existing comments
            for (final String comment : origin.getComments()) {
                destination.addComment(comment);
            }
            return true;
        } else {
            return false;
        }
    }

}

