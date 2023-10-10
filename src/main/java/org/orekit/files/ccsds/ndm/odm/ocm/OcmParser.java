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
package org.orekit.files.ccsds.ndm.odm.ocm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;

import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.data.DataContext;
import org.orekit.data.DataSource;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.ndm.ParsedUnitsBehavior;
import org.orekit.files.ccsds.ndm.odm.OdmHeader;
import org.orekit.files.ccsds.ndm.odm.OdmMetadataKey;
import org.orekit.files.ccsds.ndm.odm.OdmParser;
import org.orekit.files.ccsds.ndm.odm.UserDefined;
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
import org.orekit.files.general.EphemerisFileParser;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.units.Unit;

/** A parser for the CCSDS OCM (Orbit Comprehensive Message).
 * <p>
 * Note than starting with Orekit 11.0, CCSDS message parsers are
 * mutable objects that gather the data being parsed, until the
 * message is complete and the {@link #parseMessage(org.orekit.data.DataSource)
 * parseMessage} method has returned. This implies that parsers
 * should <em>not</em> be used in a multi-thread context. The recommended
 * way to use parsers is to either dedicate one parser for each message
 * and drop it afterwards, or to use a single-thread loop.
 * </p>
 * @author Luc Maisonobe
 * @since 11.0
 */
public class OcmParser extends OdmParser<Ocm, OcmParser> implements EphemerisFileParser<Ocm> {

    /** Pattern for splitting strings at blanks. */
    private static final Pattern SPLIT_AT_BLANKS = Pattern.compile("\\s+");

    /** File header. */
    private OdmHeader header;

    /** Metadata for current observation block. */
    private OcmMetadata metadata;

    /** Central body equatorial radius.
     * @since 12.0
     */
    private final double equatorialRadius;

    /** Central body flattening.
     * @since 12.0
     */
    private final double flattening;

    /** Context binding valid for current metadata. */
    private ContextBinding context;

    /** Trajectory state histories logical blocks. */
    private List<TrajectoryStateHistory> trajectoryBlocks;

    /** Current trajectory state metadata. */
    private TrajectoryStateHistoryMetadata currentTrajectoryStateHistoryMetadata;

    /** Current trajectory state time history being read. */
    private List<TrajectoryState> currentTrajectoryStateHistory;

    /** Physical properties logical block. */
    private OrbitPhysicalProperties physicBlock;

    /** Covariance logical blocks. */
    private List<OrbitCovarianceHistory> covarianceBlocks;

    /** Current covariance metadata. */
    private OrbitCovarianceHistoryMetadata currentCovarianceHistoryMetadata;

    /** Current covariance history being read. */
    private List<OrbitCovariance> currentCovarianceHistory;

    /** Maneuver logical blocks. */
    private List<OrbitManeuverHistory> maneuverBlocks;

    /** Current maneuver metadata. */
    private OrbitManeuverHistoryMetadata currentManeuverHistoryMetadata;

    /** Current maneuver history being read. */
    private List<OrbitManeuver> currentManeuverHistory;

    /** Perturbations logical block. */
    private Perturbations perturbationsBlock;

    /** Orbit determination logical block. */
    private OrbitDetermination orbitDeterminationBlock;

    /** User defined parameters logical block. */
    private UserDefined userDefinedBlock;

    /** Processor for global message structure. */
    private ProcessingState structureProcessor;

    /**
     * Complete constructor.
     * <p>
     * Calling this constructor directly is not recommended. Users should rather use
     * {@link org.orekit.files.ccsds.ndm.ParserBuilder#buildOcmParser()
     * parserBuilder.buildOcmParser()}.
     * </p>
     * @param conventions IERS Conventions
     * @param equatorialRadius central body equatorial radius
     * @param flattening central body flattening
     * @param simpleEOP if true, tidal effects are ignored when interpolating EOP
     * @param dataContext used to retrieve frames, time scales, etc.
     * @param mu gravitational coefficient
     * @param parsedUnitsBehavior behavior to adopt for handling parsed units
     * @param filters filters to apply to parse tokens
     * @since 12.0
     */
    public OcmParser(final IERSConventions conventions,
                     final double equatorialRadius, final double flattening,
                     final boolean simpleEOP, final DataContext dataContext,
                     final double mu, final ParsedUnitsBehavior parsedUnitsBehavior,
                     final Function<ParseToken, List<ParseToken>>[] filters) {
        super(Ocm.ROOT, Ocm.FORMAT_VERSION_KEY, conventions, simpleEOP, dataContext, null,
              mu, parsedUnitsBehavior, filters);
        this.equatorialRadius = equatorialRadius;
        this.flattening       = flattening;
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
    public Ocm parse(final DataSource source) {
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
        header                  = new OdmHeader();
        metadata                = null;
        context                 = null;
        trajectoryBlocks        = null;
        physicBlock             = null;
        covarianceBlocks        = null;
        maneuverBlocks          = null;
        perturbationsBlock      = null;
        orbitDeterminationBlock = null;
        userDefinedBlock        = null;
        if (fileFormat == FileFormat.XML) {
            structureProcessor = new XmlStructureProcessingState(Ocm.ROOT, this);
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
        metadata  = new OcmMetadata(getDataContext());
        context   = new ContextBinding(this::getConventions, this::isSimpleEOP, this::getDataContext,
                                       this::getParsedUnitsBehavior, metadata::getEpochT0, metadata::getTimeSystem,
                                       metadata::getSclkOffsetAtEpoch, metadata::getSclkSecPerSISec);
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
        final List<TrajectoryStateHistory> old = trajectoryBlocks;
        if (old != null) {
            final OneAxisEllipsoid body =
                            currentTrajectoryStateHistoryMetadata.getTrajType() == OrbitElementsType.GEODETIC ?
                            new OneAxisEllipsoid(equatorialRadius, flattening,
                                                 currentTrajectoryStateHistoryMetadata.getTrajReferenceFrame().asFrame()) :
                            null;
            trajectoryBlocks = new ArrayList<>(old.size());
            for (final TrajectoryStateHistory osh : old) {
                trajectoryBlocks.add(new TrajectoryStateHistory(osh.getMetadata(), osh.getTrajectoryStates(),
                                                                body, getSelectedMu()));
            }
        }
        return true;
    }

    /** Manage trajectory state history section.
     * @param starting if true, parser is entering the section
     * otherwise it is leaving the section
     * @return always return true
     */
    boolean manageTrajectoryStateSection(final boolean starting) {
        if (starting) {
            if (trajectoryBlocks == null) {
                // this is the first trajectory block, we need to allocate the container
                trajectoryBlocks = new ArrayList<>();
            }
            currentTrajectoryStateHistoryMetadata = new TrajectoryStateHistoryMetadata(metadata.getEpochT0(),
                                                                                       getDataContext());
            currentTrajectoryStateHistory         = new ArrayList<>();
            anticipateNext(this::processTrajectoryStateToken);
        } else {
            final OneAxisEllipsoid body =
                            currentTrajectoryStateHistoryMetadata.getTrajType() == OrbitElementsType.GEODETIC ?
                            new OneAxisEllipsoid(equatorialRadius, flattening,
                                                 currentTrajectoryStateHistoryMetadata.getTrajReferenceFrame().asFrame()) :
                            null;
            anticipateNext(structureProcessor);
            if (currentTrajectoryStateHistoryMetadata.getCenter().getBody() != null) {
                setMuCreated(currentTrajectoryStateHistoryMetadata.getCenter().getBody().getGM());
            }
            // we temporarily set gravitational parameter to NaN,
            // as we may get a proper one in the perturbations section
            trajectoryBlocks.add(new TrajectoryStateHistory(currentTrajectoryStateHistoryMetadata,
                                                            currentTrajectoryStateHistory,
                                                            body, Double.NaN));
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
            if (physicBlock == null) {
                // this is the first (and unique) physical properties block, we need to allocate the container
                physicBlock = new OrbitPhysicalProperties(metadata.getEpochT0());
            }
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
            currentCovarianceHistoryMetadata = new OrbitCovarianceHistoryMetadata(metadata.getEpochT0());
            currentCovarianceHistory         = new ArrayList<>();
            anticipateNext(this::processCovarianceToken);
        } else {
            anticipateNext(structureProcessor);
            covarianceBlocks.add(new OrbitCovarianceHistory(currentCovarianceHistoryMetadata,
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
            currentManeuverHistoryMetadata = new OrbitManeuverHistoryMetadata(metadata.getEpochT0());
            currentManeuverHistory         = new ArrayList<>();
            anticipateNext(this::processManeuverToken);
        } else {
            anticipateNext(structureProcessor);
            maneuverBlocks.add(new OrbitManeuverHistory(currentManeuverHistoryMetadata,
                                                        currentManeuverHistory));
            currentManeuverHistoryMetadata = null;
            currentManeuverHistory         = null;
        }
        return true;
    }

    /** Manage perturbation parameters section.
     * @param starting if true, parser is entering the section
     * otherwise it is leaving the section
     * @return always return true
     */
    boolean managePerturbationParametersSection(final boolean starting) {
        if (starting) {
            if (perturbationsBlock == null) {
                // this is the first (and unique) perturbations parameters block, we need to allocate the container
                perturbationsBlock = new Perturbations(context.getDataContext().getCelestialBodies());
            }
            anticipateNext(this::processPerturbationToken);
        } else {
            anticipateNext(structureProcessor);
        }
        return true;
    }

    /** Manage orbit determination section.
     * @param starting if true, parser is entering the section
     * otherwise it is leaving the section
     * @return always return true
     */
    boolean manageOrbitDeterminationSection(final boolean starting) {
        if (starting) {
            if (orbitDeterminationBlock == null) {
                // this is the first (and unique) orbit determination block, we need to allocate the container
                orbitDeterminationBlock = new OrbitDetermination();
            }
            anticipateNext(this::processOrbitDeterminationToken);
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

    /** {@inheritDoc} */
    @Override
    public Ocm build() {

        // OCM KVN file lack a DATA_STOP keyword, hence we can't call finalizeData()
        // automatically before the end of the file
        finalizeData();
        if (userDefinedBlock != null && userDefinedBlock.getParameters().isEmpty()) {
            userDefinedBlock = null;
        }

        // the mu is needed only if there are trajectories
        final double mu;
        if (trajectoryBlocks == null) {
            mu = Double.NaN;
        } else {
            if (perturbationsBlock != null) {
                // this may be Double.NaN, but it will be handled correctly
                setMuParsed(perturbationsBlock.getGm());
            }
            mu = getSelectedMu();
        }

        final OcmData data = new OcmData(trajectoryBlocks, physicBlock, covarianceBlocks,
                                         maneuverBlocks, perturbationsBlock,
                                         orbitDeterminationBlock, userDefinedBlock);
        data.validate(header.getFormatVersion());

        return new Ocm(header, Collections.singletonList(new Segment<>(metadata, data)),
                           getConventions(), getDataContext(), mu);

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
                    return OcmMetadataKey.valueOf(token.getName()).process(token, context, metadata);
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
                   OcmDataSubStructureKey.valueOf(token.getName()).process(token, this);
        } catch (IllegalArgumentException iae) {
            // token has not been recognized
            return false;
        }
    }

    /** Process one trajectory state history data token.
     * @param token token to process
     * @return true if token was processed, false otherwise
     */
    private boolean processTrajectoryStateToken(final ParseToken token) {
        if (token.getName() != null && !token.getName().equals(Ocm.TRAJ_LINE)) {
            // we are in the section metadata part
            try {
                return TrajectoryStateHistoryMetadataKey.valueOf(token.getName()).
                       process(token, context, currentTrajectoryStateHistoryMetadata);
            } catch (IllegalArgumentException iae) {
                // token has not been recognized
                return false;
            }
        } else {
            // we are in the section data part
            if (currentTrajectoryStateHistory.isEmpty()) {
                // we are starting the real data section, we can now check metadata is complete
                currentTrajectoryStateHistoryMetadata.validate(header.getFormatVersion());
                anticipateNext(this::processDataSubStructureToken);
            }
            if (token.getType() == TokenType.START || token.getType() == TokenType.STOP) {
                return true;
            }
            try {
                final String[] fields = SPLIT_AT_BLANKS.split(token.getRawContent().trim());
                // as TRAJ_UNITS is optional and indeed MUST match type, get them directly from type
                final List<Unit> units = currentTrajectoryStateHistoryMetadata.getTrajType().getUnits();
                if (fields.length != units.size() + 1) {
                    throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                              token.getLineNumber(), token.getFileName(), token.getContentAsNormalizedString());
                }
                final AbsoluteDate epoch = context.getTimeSystem().getConverter(context).parse(fields[0]);
                return currentTrajectoryStateHistory.add(new TrajectoryState(currentTrajectoryStateHistoryMetadata.getTrajType(),
                                                                             epoch, fields, 1, units));
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
        if (physicBlock == null) {
            physicBlock = new OrbitPhysicalProperties(metadata.getEpochT0());
        }
        anticipateNext(this::processDataSubStructureToken);
        try {
            return token.getName() != null &&
                   OrbitPhysicalPropertiesKey.valueOf(token.getName()).process(token, context, physicBlock);
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
                return OrbitCovarianceHistoryMetadataKey.valueOf(token.getName()).
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
                final int n = currentCovarianceHistoryMetadata.getCovType().getUnits().size();
                if (fields.length - 1 != currentCovarianceHistoryMetadata.getCovOrdering().nbElements(n)) {
                    throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                              token.getLineNumber(), token.getFileName(), token.getContentAsNormalizedString());
                }
                currentCovarianceHistory.add(new OrbitCovariance(currentCovarianceHistoryMetadata.getCovType(),
                                                            currentCovarianceHistoryMetadata.getCovOrdering(),
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
        if (token.getName() != null && !token.getName().equals(Ocm.MAN_LINE)) {
            // we are in the section metadata part
            try {
                return OrbitManeuverHistoryMetadataKey.valueOf(token.getName()).
                       process(token, context, currentManeuverHistoryMetadata);
            } catch (IllegalArgumentException iae) {
                // token has not been recognized
                return false;
            }
        } else {
            // we are in the section data part
            if (currentManeuverHistory.isEmpty()) {
                // we are starting the real data section, we can now check metadata is complete
                currentManeuverHistoryMetadata.validate(header.getFormatVersion());
                anticipateNext(this::processDataSubStructureToken);
            }
            if (token.getType() == TokenType.START || token.getType() == TokenType.STOP) {
                return true;
            }
            try {
                final String[] fields = SPLIT_AT_BLANKS.split(token.getRawContent().trim());
                final List<ManeuverFieldType> types = currentManeuverHistoryMetadata.getManComposition();
                if (fields.length != types.size()) {
                    throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                              token.getLineNumber(), token.getFileName(), token.getContentAsNormalizedString());
                }
                final OrbitManeuver maneuver = new OrbitManeuver();
                for (int i = 0; i < fields.length; ++i) {
                    types.get(i).process(fields[i], context, maneuver, token.getLineNumber(), token.getFileName());
                }
                currentManeuverHistory.add(maneuver);
                return true;
            } catch (NumberFormatException nfe) {
                throw new OrekitException(nfe, OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                          token.getLineNumber(), token.getFileName(), token.getContentAsNormalizedString());
            }
        }
    }

    /** Process one perturbation parameter data token.
     * @param token token to process
     * @return true if token was processed, false otherwise
     */
    private boolean processPerturbationToken(final ParseToken token) {
        anticipateNext(this::processDataSubStructureToken);
        try {
            return token.getName() != null &&
                   PerturbationsKey.valueOf(token.getName()).process(token, context, perturbationsBlock);
        } catch (IllegalArgumentException iae) {
            // token has not been recognized
            return false;
        }
    }

    /** Process one orbit determination data token.
     * @param token token to process
     * @return true if token was processed, false otherwise
     */
    private boolean processOrbitDeterminationToken(final ParseToken token) {
        if (orbitDeterminationBlock == null) {
            orbitDeterminationBlock = new OrbitDetermination();
        }
        anticipateNext(this::processDataSubStructureToken);
        try {
            return token.getName() != null &&
                   OrbitDeterminationKey.valueOf(token.getName()).process(token, context, orbitDeterminationBlock);
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
