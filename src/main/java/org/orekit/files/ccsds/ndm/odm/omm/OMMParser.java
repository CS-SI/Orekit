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
package org.orekit.files.ccsds.ndm.odm.omm;

import java.util.ArrayList;
import java.util.List;

import org.hipparchus.util.FastMath;
import org.orekit.data.DataContext;
import org.orekit.files.ccsds.ndm.odm.OCommonMetadata;
import org.orekit.files.ccsds.ndm.odm.OCommonMetadataKey;
import org.orekit.files.ccsds.ndm.odm.OCommonParser;
import org.orekit.files.ccsds.ndm.odm.ODMCovariance;
import org.orekit.files.ccsds.ndm.odm.ODMCovarianceKey;
import org.orekit.files.ccsds.ndm.odm.ODMHeader;
import org.orekit.files.ccsds.ndm.odm.ODMHeaderProcessingState;
import org.orekit.files.ccsds.ndm.odm.ODMKeplerianElements;
import org.orekit.files.ccsds.ndm.odm.ODMKeplerianElementsKey;
import org.orekit.files.ccsds.ndm.odm.ODMMetadataKey;
import org.orekit.files.ccsds.ndm.odm.ODMSpacecraftParameters;
import org.orekit.files.ccsds.ndm.odm.ODMSpacecraftParametersKey;
import org.orekit.files.ccsds.ndm.odm.ODMUserDefined;
import org.orekit.files.ccsds.section.CommentsContainer;
import org.orekit.files.ccsds.section.Header;
import org.orekit.files.ccsds.section.MetadataKey;
import org.orekit.files.ccsds.section.Segment;
import org.orekit.files.ccsds.section.XMLStructureProcessingState;
import org.orekit.files.ccsds.utils.FileFormat;
import org.orekit.files.ccsds.utils.ParsingContext;
import org.orekit.files.ccsds.utils.lexical.ParseToken;
import org.orekit.files.ccsds.utils.lexical.TokenType;
import org.orekit.files.ccsds.utils.state.ErrorState;
import org.orekit.files.ccsds.utils.state.ProcessingState;
import org.orekit.propagation.analytical.tle.TLEPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.IERSConventions;

/** A parser for the CCSDS OMM (Orbiter Mean-Elements Message).
 * @author sports
 * @since 6.1
 */
public class OMMParser extends OCommonParser<OMMFile, OMMParser> {

    /** Root element for XML files. */
    private static final String ROOT = "omm";

    /** Default mass to use if there are no spacecraft parameters block logical block in the file. */
    private final double defaultMass;

    /** File header. */
    private ODMHeader header;

    /** File segments. */
    private List<Segment<OMMMetadata, OMMData>> segments;

    /** OMM metadata being read. */
    private OMMMetadata metadata;

    /** Parsing context valid for current metadata. */
    private ParsingContext context;

    /** Keplerian elements logical block being read. */
    private ODMKeplerianElements keplerianElementsBlock;

    /** Spacecraft parameters logical block being read. */
    private ODMSpacecraftParameters spacecraftParametersBlock;

    /** TLE logical block being read. */
    private OMMTLE tleBlock;

    /** Covariance matrix logical block being read. */
    private ODMCovariance covarianceBlock;

    /** User defined parameters. */
    private ODMUserDefined userDefinedBlock;

    /** Processor for global message structure. */
    private ProcessingState structureProcessor;

    /** Complete constructor.
     * @param conventions IERS Conventions
     * @param simpleEOP if true, tidal effects are ignored when interpolating EOP
     * @param dataContext used to retrieve frames, time scales, etc.
     * @param missionReferenceDate reference date for Mission Elapsed Time or Mission Relative Time time systems
     * @param mu gravitational coefficient
     * @param defaultMass default mass to use if there are no spacecraft parameters block logical block in the file
     */
    public OMMParser(final IERSConventions conventions, final boolean simpleEOP,
                     final DataContext dataContext, final AbsoluteDate missionReferenceDate,
                     final double mu, final double defaultMass) {
        super(OMMFile.FORMAT_VERSION_KEY, conventions, simpleEOP, dataContext, missionReferenceDate, mu);
        this.defaultMass = defaultMass;
    }

    /** {@inheritDoc} */
    @Override
    public Header getHeader() {
        return header;
    }

    /** {@inheritDoc} */
    @Override
    public void reset(final FileFormat fileFormat) {
        header                    = new ODMHeader();
        segments                  = new ArrayList<>();
        metadata                  = null;
        context                   = null;
        keplerianElementsBlock    = null;
        spacecraftParametersBlock = null;
        tleBlock                  = null;
        covarianceBlock           = null;
        userDefinedBlock          = null;
        if (fileFormat == FileFormat.XML) {
            structureProcessor = new XMLStructureProcessingState(ROOT, this);
            reset(fileFormat, structureProcessor);
        } else {
            structureProcessor = new ErrorState(); // should never be called
            reset(fileFormat, new ODMHeaderProcessingState(getDataContext(), this, header));
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean prepareHeader() {
        setFallback(new ODMHeaderProcessingState(getDataContext(), this, header));
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean inHeader() {
        setFallback(getFileFormat() == FileFormat.XML ? structureProcessor : this::processMetadataToken);
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
        metadata  = new OMMMetadata();
        context   = new ParsingContext(this::getConventions,
                                       this::isSimpleEOP,
                                       this::getDataContext,
                                       this::getMissionReferenceDate,
                                       metadata::getTimeSystem);
        setFallback(this::processMetadataToken);
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean inMetadata() {
        setFallback(getFileFormat() == FileFormat.XML ? structureProcessor : this::processKeplerianElementsToken);
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean finalizeMetadata() {
        metadata.finalizeMetadata(context);
        metadata.checkMandatoryEntries();
        if (metadata.getCenterBody() != null) {
            setMuCreated(metadata.getCenterBody().getGM());
        }
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean prepareData() {
        keplerianElementsBlock = new ODMKeplerianElements();
        setFallback(this::processKeplerianElementsToken);
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
        if (metadata != null) {
            if (userDefinedBlock != null && userDefinedBlock.getParameters().isEmpty()) {
                userDefinedBlock = null;
            }
            if (tleBlock != null) {
                if (Double.isNaN(keplerianElementsBlock.getMu())) {
                    keplerianElementsBlock.setMu(TLEPropagator.getMU());
                }
                final double mu = keplerianElementsBlock.getMu();
                final double n  = keplerianElementsBlock.getMeanMotion();
                keplerianElementsBlock.setA(FastMath.cbrt(mu / (n * n)));
                setMuParsed(mu);
            }
            final double  mass = spacecraftParametersBlock == null ?
                                 defaultMass : spacecraftParametersBlock.getMass();
            final OMMData data = new OMMData(keplerianElementsBlock, spacecraftParametersBlock,
                                             tleBlock, covarianceBlock, userDefinedBlock, mass);
            data.checkMandatoryEntries();
            segments.add(new Segment<>(metadata, data));
        }
        metadata                  = null;
        context                   = null;
        keplerianElementsBlock    = null;
        spacecraftParametersBlock = null;
        tleBlock                  = null;
        covarianceBlock           = null;
        userDefinedBlock          = null;
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public OMMFile build() {
        // OMM KVN file lack a DATA_STOP keyword, hence we can't call finalizeData()
        // automatically before the end of the file
        finalizeData();
        return new OMMFile(header, segments, getConventions(), getDataContext());
    }

    /** Process one metadata token.
     * @param token token to process
     * @return true if token was processed, false otherwise
     */
    private boolean processMetadataToken(final ParseToken token) {
        if (metadata == null) {
            // OMM KVN file lack a META_START keyword, hence we can't call prepareMetadata()
            // automatically before the first metadata token arrives
            prepareMetadata();
        }
        inMetadata();
        try {
            return token.getName() != null &&
                   MetadataKey.valueOf(token.getName()).process(token, context, metadata);
        } catch (IllegalArgumentException iaeG) {
            try {
                return ODMMetadataKey.valueOf(token.getName()).process(token, context, metadata);
            } catch (IllegalArgumentException iaeD) {
                try {
                    return OCommonMetadataKey.valueOf(token.getName()).process(token, context, metadata);
                } catch (IllegalArgumentException iaeC) {
                    try {
                        return OMMMetadataKey.valueOf(token.getName()).process(token, context, metadata);
                    } catch (IllegalArgumentException iaeM) {
                        // token has not been recognized
                        return false;
                    }
                }
            }
        }
    }

    /** Process one Keplerian elements data token.
     * @param token token to process
     * @return true if token was processed, false otherwise
     */
    private boolean processKeplerianElementsToken(final ParseToken token) {
        if (keplerianElementsBlock == null) {
            // OMM KVN file lack a META_STOP keyword, hence we can't call finalizeMetadata()
            // automatically before the first data token arrives
            finalizeMetadata();
            // OMM KVN file lack a DATA_START keyword, hence we can't call prepareData()
            // automatically before the first data token arrives
            prepareData();
        }
        setFallback(getFileFormat() == FileFormat.XML ? structureProcessor : this::processSpacecraftParametersToken);
        inData();
        try {
            return token.getName() != null &&
                   ODMKeplerianElementsKey.valueOf(token.getName()).process(token, context, keplerianElementsBlock);
        } catch (IllegalArgumentException iae) {
            // token has not been recognized
            return false;
        }
    }

    /** Process one spacecraft parameters data token.
     * @param token token to process
     * @return true if token was processed, false otherwise
     */
    private boolean processSpacecraftParametersToken(final ParseToken token) {
        if (spacecraftParametersBlock == null) {
            spacecraftParametersBlock = new ODMSpacecraftParameters();
            if (moveCommentsIfEmpty(keplerianElementsBlock, spacecraftParametersBlock)) {
                // get rid of the empty logical block
                keplerianElementsBlock = null;
            }
        }
        setFallback(getFileFormat() == FileFormat.XML ? structureProcessor : this::processTLEToken);
        try {
            return token.getName() != null &&
                   ODMSpacecraftParametersKey.valueOf(token.getName()).process(token, context, spacecraftParametersBlock);
        } catch (IllegalArgumentException iae) {
            // token has not been recognized
            return false;
        }
    }

    /** Process one TLE data token.
     * @param token token to process
     * @return true if token was processed, false otherwise
     */
    private boolean processTLEToken(final ParseToken token) {
        if (tleBlock == null) {
            tleBlock = new OMMTLE();
            if (moveCommentsIfEmpty(spacecraftParametersBlock, tleBlock)) {
                // get rid of the empty logical block
                spacecraftParametersBlock = null;
            }
        }
        setFallback(getFileFormat() == FileFormat.XML ? structureProcessor : this::processCovarianceToken);
        try {
            return token.getName() != null &&
                   OMMTLEKey.valueOf(token.getName()).process(token, context, tleBlock);
        } catch (IllegalArgumentException iae) {
            // token has not been recognized
            return false;
        }
    }

    /** Process one covariance matrix data token.
     * @param token token to process
     * @return true if token was processed, false otherwise
     */
    private boolean processCovarianceToken(final ParseToken token) {
        if (covarianceBlock == null) {
            // save the current metadata for later retrieval of reference frame
            final OCommonMetadata savedMetadata = metadata;
            covarianceBlock = new ODMCovariance(() -> savedMetadata.getFrame(), () -> savedMetadata.getRefCCSDSFrame());
            if (moveCommentsIfEmpty(tleBlock, covarianceBlock)) {
                // get rid of the empty logical block
                tleBlock = null;
            }
        }
        setFallback(getFileFormat() == FileFormat.XML ? structureProcessor : this::processUserDefinedToken);
        try {
            return token.getName() != null &&
                   ODMCovarianceKey.valueOf(token.getName()).process(token, context, covarianceBlock);
        } catch (IllegalArgumentException iae) {
            // token has not been recognized
            return false;
        }
    }

    /** Process one maneuver data token.
     * @param token token to process
     * @return true if token was processed, false otherwise
     */
    private boolean processUserDefinedToken(final ParseToken token) {
        if (userDefinedBlock == null) {
            userDefinedBlock = new ODMUserDefined();
            if (moveCommentsIfEmpty(covarianceBlock, userDefinedBlock)) {
                // get rid of the empty logical block
                covarianceBlock = null;
            }
        }
        setFallback(getFileFormat() == FileFormat.XML ? structureProcessor : new ErrorState());
        if (token.getType() == TokenType.ENTRY &&
            token.getName().startsWith(ODMUserDefined.USER_DEFINED_PREFIX)) {
            userDefinedBlock.addEntry(token.getName().substring(ODMUserDefined.USER_DEFINED_PREFIX.length()),
                                      token.getContent());
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
        if (origin.acceptComments()) {
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