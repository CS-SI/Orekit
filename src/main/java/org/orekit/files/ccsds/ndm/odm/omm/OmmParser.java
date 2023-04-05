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
package org.orekit.files.ccsds.ndm.odm.omm;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.hipparchus.util.FastMath;
import org.orekit.data.DataContext;
import org.orekit.files.ccsds.ndm.ParsedUnitsBehavior;
import org.orekit.files.ccsds.ndm.odm.CartesianCovariance;
import org.orekit.files.ccsds.ndm.odm.CartesianCovarianceKey;
import org.orekit.files.ccsds.ndm.odm.OdmCommonMetadata;
import org.orekit.files.ccsds.ndm.odm.CommonMetadataKey;
import org.orekit.files.ccsds.ndm.odm.KeplerianElements;
import org.orekit.files.ccsds.ndm.odm.KeplerianElementsKey;
import org.orekit.files.ccsds.ndm.odm.OdmHeader;
import org.orekit.files.ccsds.ndm.odm.OdmMetadataKey;
import org.orekit.files.ccsds.ndm.odm.OdmParser;
import org.orekit.files.ccsds.ndm.odm.SpacecraftParameters;
import org.orekit.files.ccsds.ndm.odm.SpacecraftParametersKey;
import org.orekit.files.ccsds.ndm.odm.UserDefined;
import org.orekit.files.ccsds.section.CommentsContainer;
import org.orekit.files.ccsds.section.HeaderProcessingState;
import org.orekit.files.ccsds.section.MetadataKey;
import org.orekit.files.ccsds.section.Segment;
import org.orekit.files.ccsds.section.XmlStructureProcessingState;
import org.orekit.files.ccsds.utils.ContextBinding;
import org.orekit.files.ccsds.utils.FileFormat;
import org.orekit.files.ccsds.utils.lexical.ParseToken;
import org.orekit.files.ccsds.utils.lexical.TokenType;
import org.orekit.files.ccsds.utils.lexical.UserDefinedXmlTokenBuilder;
import org.orekit.files.ccsds.utils.lexical.XmlTokenBuilder;
import org.orekit.files.ccsds.utils.parsing.ErrorState;
import org.orekit.files.ccsds.utils.parsing.ProcessingState;
import org.orekit.propagation.analytical.tle.TLEPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.IERSConventions;

/** A parser for the CCSDS OMM (Orbiter Mean-Elements Message).
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
public class OmmParser extends OdmParser<Omm, OmmParser> {

    /** Default mass to use if there are no spacecraft parameters block logical block in the file. */
    private final double defaultMass;

    /** File header. */
    private OdmHeader header;

    /** File segments. */
    private List<Segment<OmmMetadata, OmmData>> segments;

    /** OMM metadata being read. */
    private OmmMetadata metadata;

    /** Context binding valid for current metadata. */
    private ContextBinding context;

    /** Keplerian elements logical block being read. */
    private KeplerianElements keplerianElementsBlock;

    /** Spacecraft parameters logical block being read. */
    private SpacecraftParameters spacecraftParametersBlock;

    /** TLE logical block being read. */
    private OmmTle tleBlock;

    /** Covariance matrix logical block being read. */
    private CartesianCovariance covarianceBlock;

    /** User defined parameters. */
    private UserDefined userDefinedBlock;

    /** Processor for global message structure. */
    private ProcessingState structureProcessor;

    /** Complete constructor.
     * <p>
     * Calling this constructor directly is not recommended. Users should rather use
     * {@link org.orekit.files.ccsds.ndm.ParserBuilder#buildOmmParser()
     * parserBuilder.buildOmmParser()}.
     * </p>
     * @param conventions IERS Conventions
     * @param simpleEOP if true, tidal effects are ignored when interpolating EOP
     * @param dataContext used to retrieve frames, time scales, etc.
     * @param missionReferenceDate reference date for Mission Elapsed Time or Mission Relative Time time systems
     * @param mu gravitational coefficient
     * @param defaultMass default mass to use if there are no spacecraft parameters block logical block in the file
     * @param parsedUnitsBehavior behavior to adopt for handling parsed units
     * @param filters filters to apply to parse tokens
     * @since 12.0
     */
    public OmmParser(final IERSConventions conventions, final boolean simpleEOP,
                     final DataContext dataContext, final AbsoluteDate missionReferenceDate,
                     final double mu, final double defaultMass, final ParsedUnitsBehavior parsedUnitsBehavior,
                     final Function<ParseToken, List<ParseToken>>[] filters) {
        super(Omm.ROOT, Omm.FORMAT_VERSION_KEY, conventions, simpleEOP, dataContext,
              missionReferenceDate, mu, parsedUnitsBehavior, filters);
        this.defaultMass = defaultMass;
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
    public OdmHeader getHeader() {
        return header;
    }

    /** {@inheritDoc} */
    @Override
    public void reset(final FileFormat fileFormat) {
        header                    = new OdmHeader();
        segments                  = new ArrayList<>();
        metadata                  = null;
        context                   = null;
        keplerianElementsBlock    = null;
        spacecraftParametersBlock = null;
        tleBlock                  = null;
        covarianceBlock           = null;
        userDefinedBlock          = null;
        if (fileFormat == FileFormat.XML) {
            structureProcessor = new XmlStructureProcessingState(Omm.ROOT, this);
            reset(fileFormat, structureProcessor);
        } else {
            structureProcessor = new ErrorState(); // should never be called
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
        anticipateNext(getFileFormat() == FileFormat.XML ? structureProcessor : this::processMetadataToken);
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
        metadata  = new OmmMetadata();
        context   = new ContextBinding(this::getConventions, this::isSimpleEOP,
                                       this::getDataContext, this::getParsedUnitsBehavior,
                                       this::getMissionReferenceDate,
                                       metadata::getTimeSystem, () -> 0.0, () -> 1.0);
        anticipateNext(this::processMetadataToken);
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean inMetadata() {
        anticipateNext(getFileFormat() == FileFormat.XML ? structureProcessor : this::processKeplerianElementsToken);
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
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean prepareData() {
        keplerianElementsBlock = new KeplerianElements();
        anticipateNext(getFileFormat() == FileFormat.XML ? this::processXmlSubStructureToken : this::processKeplerianElementsToken);
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
            final OmmData data = new OmmData(keplerianElementsBlock, spacecraftParametersBlock,
                                             tleBlock, covarianceBlock, userDefinedBlock, mass);
            data.validate(header.getFormatVersion());
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
    public Omm build() {
        // OMM KVN file lack a DATA_STOP keyword, hence we can't call finalizeData()
        // automatically before the end of the file
        finalizeData();
        return new Omm(header, segments, getConventions(), getDataContext());
    }

    /** Manage Keplerian elements section.
     * @param starting if true, parser is entering the section
     * otherwise it is leaving the section
     * @return always return true
     */
    boolean manageKeplerianElementsSection(final boolean starting) {
        anticipateNext(starting ? this::processKeplerianElementsToken : structureProcessor);
        return true;
    }

    /** Manage spacecraft parameters section.
     * @param starting if true, parser is entering the section
     * otherwise it is leaving the section
     * @return always return true
     */
    boolean manageSpacecraftParametersSection(final boolean starting) {
        anticipateNext(starting ? this::processSpacecraftParametersToken : structureProcessor);
        return true;
    }

    /** Manage TLE parameters section.
     * @param starting if true, parser is entering the section
     * otherwise it is leaving the section
     * @return always return true
     */
    boolean manageTleParametersSection(final boolean starting) {
        anticipateNext(starting ? this::processTLEToken : structureProcessor);
        return true;
    }

        /** Manage covariance matrix section.
     * @param starting if true, parser is entering the section
     * otherwise it is leaving the section
     * @return always return true
     */
    boolean manageCovarianceSection(final boolean starting) {
        anticipateNext(starting ? this::processCovarianceToken : structureProcessor);
        return true;
    }

    /** Manage user-defined parameters section.
     * @param starting if true, parser is entering the section
     * otherwise it is leaving the section
     * @return always return true
     */
    boolean manageUserDefinedParametersSection(final boolean starting) {
        anticipateNext(starting ? this::processUserDefinedToken : structureProcessor);
        return true;
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
                return OdmMetadataKey.valueOf(token.getName()).process(token, context, metadata);
            } catch (IllegalArgumentException iaeD) {
                try {
                    return CommonMetadataKey.valueOf(token.getName()).process(token, context, metadata);
                } catch (IllegalArgumentException iaeC) {
                    try {
                        return OmmMetadataKey.valueOf(token.getName()).process(token, context, metadata);
                    } catch (IllegalArgumentException iaeM) {
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
        try {
            return token.getName() != null &&
                   XmlSubStructureKey.valueOf(token.getName()).process(token, this);
        } catch (IllegalArgumentException iae) {
            // token has not been recognized
            return false;
        }
    }

    /** Process one mean Keplerian elements data token.
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
        anticipateNext(getFileFormat() == FileFormat.XML ? this::processXmlSubStructureToken : this::processSpacecraftParametersToken);
        try {
            return token.getName() != null &&
                   KeplerianElementsKey.valueOf(token.getName()).process(token, context, keplerianElementsBlock);
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
            spacecraftParametersBlock = new SpacecraftParameters();
            if (moveCommentsIfEmpty(keplerianElementsBlock, spacecraftParametersBlock)) {
                // get rid of the empty logical block
                keplerianElementsBlock = null;
            }
        }
        anticipateNext(getFileFormat() == FileFormat.XML ? this::processXmlSubStructureToken : this::processTLEToken);
        try {
            return token.getName() != null &&
                   SpacecraftParametersKey.valueOf(token.getName()).process(token, context, spacecraftParametersBlock);
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
            tleBlock = new OmmTle();
            if (moveCommentsIfEmpty(spacecraftParametersBlock, tleBlock)) {
                // get rid of the empty logical block
                spacecraftParametersBlock = null;
            }
        }
        anticipateNext(getFileFormat() == FileFormat.XML ? this::processXmlSubStructureToken : this::processCovarianceToken);
        try {
            return token.getName() != null &&
                   OmmTleKey.valueOf(token.getName()).process(token, context, tleBlock);
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
            final OdmCommonMetadata savedMetadata = metadata;
            covarianceBlock = new CartesianCovariance(() -> savedMetadata.getReferenceFrame());
            if (moveCommentsIfEmpty(tleBlock, covarianceBlock)) {
                // get rid of the empty logical block
                tleBlock = null;
            }
        }
        anticipateNext(getFileFormat() == FileFormat.XML ? this::processXmlSubStructureToken : this::processUserDefinedToken);
        try {
            return token.getName() != null &&
                   CartesianCovarianceKey.valueOf(token.getName()).process(token, context, covarianceBlock);
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
            userDefinedBlock = new UserDefined();
            if (moveCommentsIfEmpty(covarianceBlock, userDefinedBlock)) {
                // get rid of the empty logical block
                covarianceBlock = null;
            }
        }
        anticipateNext(getFileFormat() == FileFormat.XML ? this::processXmlSubStructureToken : new ErrorState());
        if (token.getName().startsWith(UserDefined.USER_DEFINED_PREFIX)) {
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
