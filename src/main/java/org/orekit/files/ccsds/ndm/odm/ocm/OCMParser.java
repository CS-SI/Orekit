/* Copyright 2002-2021 CS GROUP
 * Licensed to CS Systèmes d'Information (CS) under one or more
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
import java.util.regex.Pattern;

import org.orekit.data.DataContext;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.ndm.odm.OCommonParser;
import org.orekit.files.ccsds.ndm.odm.ODMHeader;
import org.orekit.files.ccsds.ndm.odm.ODMHeaderProcessingState;
import org.orekit.files.ccsds.ndm.odm.ODMMetadataKey;
import org.orekit.files.ccsds.ndm.odm.ODMUserDefined;
import org.orekit.files.ccsds.ndm.odm.opm.OPMManeuver;
import org.orekit.files.ccsds.section.Header;
import org.orekit.files.ccsds.section.KVNStructureProcessingState;
import org.orekit.files.ccsds.section.MetadataKey;
import org.orekit.files.ccsds.section.Segment;
import org.orekit.files.ccsds.section.XMLStructureProcessingState;
import org.orekit.files.ccsds.utils.CCSDSUnit;
import org.orekit.files.ccsds.utils.FileFormat;
import org.orekit.files.ccsds.utils.ParsingContext;
import org.orekit.files.ccsds.utils.lexical.ParseToken;
import org.orekit.files.ccsds.utils.state.ProcessingState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.IERSConventions;

/** A parser for the CCSDS OCM (Orbit Comprehensive Message).
 * @author Luc Maisonobe
 * @since 11.0
 */
public class OCMParser extends OCommonParser<OCMFile, OCMParser> {

    /** Root element for XML files. */
    private static final String ROOT = "ocm";

    /** Pattern for splitting strings at blanks. */
    private static final Pattern SPLIT_AT_BLANKS = Pattern.compile("\\s+");

    /** File header. */
    private ODMHeader header;

    /** Metadata for current observation block. */
    private OCMMetadata metadata;

    /** Parsing context valid for current metadata. */
    private ParsingContext context;

    /** Orbital state histories logical blocks. */
    private List<OrbitStateHistory> orbitBlocks;

    /** Current orbit state metadata. */
    private OrbitStateHistoryMetadata currentOrbitStateHistoryMetadata;

    /** Current orbit state time history being read. */
    private List<OrbitState> currentOrbitStateHistory;

    /** Physical properties logical block. */
    private PhysicalProperties physicBlock;

    /** Covariance logical blocks. */
    private List<CovarianceHistory> covarianceBlocks;

    /** Current covariance history being read. */
    private CovarianceHistory currentCovarianceHistory;

    /** Maneuvers logical blocks. */
    private List<OPMManeuver> maneuverBlocks;

    /** current maneuver being read. */
    private OPMManeuver currentManeuver;

    /** Perturbations logical block. */
    private Perturbations perturbationsBlock;

    /** Orbit determination logical block. */
    private OrbitDetermination orbitDeterminationBlock;

    /** User defined parameters logical block. */
    private ODMUserDefined userDefinedBlock;
    /** Processor for global message structure. */
    private ProcessingState structureProcessor;

    /**
     * Complete constructor.
     * @param conventions IERS Conventions
     * @param simpleEOP if true, tidal effects are ignored when interpolating EOP
     * @param dataContext used to retrieve frames, time scales, etc.
     * @param mu gravitational coefficient
     */
    public OCMParser(final IERSConventions conventions, final boolean simpleEOP,
                     final DataContext dataContext, final double mu) {
        super(OCMFile.FORMAT_VERSION_KEY, conventions, simpleEOP, dataContext, null, mu);
    }

    /** {@inheritDoc} */
    @Override
    public Header getHeader() {
        return header;
    }

    /** {@inheritDoc} */
    @Override
    public void reset(final FileFormat fileFormat) {
        header                  = new ODMHeader();
        metadata                = null;
        context                 = null;
        orbitBlocks             = null;
        physicBlock             = null;
        covarianceBlocks        = null;
        maneuverBlocks          = null;
        perturbationsBlock      = null;
        orbitDeterminationBlock = null;
        userDefinedBlock        = null;
        if (getFileFormat() == FileFormat.XML) {
            structureProcessor = new XMLStructureProcessingState(ROOT, this);
            reset(fileFormat, structureProcessor);
        } else {
            structureProcessor = new KVNStructureProcessingState(this);
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
        metadata  = new OCMMetadata();
        context   = new ParsingContext(this::getConventions,
                                       this::isSimpleEOP,
                                       this::getDataContext,
                                       metadata::getEpochT0,
                                       metadata::getTimeSystem);
        setFallback(this::processMetadataToken);
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean inMetadata() {
        setFallback(structureProcessor);
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean finalizeMetadata() {
        metadata.checkMandatoryEntries();
        setFallback(getFileFormat() == FileFormat.XML ? structureProcessor : this::processDataSubStructureToken);
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean prepareData() {
        setFallback(this::processOrbitStateToken);
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
        // fix gravitational parameter now that all sections have been completed
        final List<OrbitStateHistory> old = orbitBlocks;
        orbitBlocks = new ArrayList<>(old.size());
        for (final OrbitStateHistory osh : old) {
            orbitBlocks.add(new OrbitStateHistory(osh.getMetadata(), osh.getOrbitalStates(), getSelectedMu()));
        }
        return true;
    }

    /** Manage orbit state history section.
     * @param starting if true, parser is entering the section
     * otherwise it is leaving the section
     * @return always return true
     */
    boolean manageOrbitStateSection(final boolean starting) {
        if (starting) {
            if (orbitBlocks == null) {
                // this is the first orbit block, we need to allocate the container
                orbitBlocks = new ArrayList<>();
            }
            currentOrbitStateHistoryMetadata = new OrbitStateHistoryMetadata(metadata.getEpochT0(),
                                                                             getDataContext());
            currentOrbitStateHistory         = new ArrayList<>();
            setFallback(this::processOrbitStateToken);
        } else {
            if (currentOrbitStateHistoryMetadata.getCenterBody() != null) {
                setMuCreated(currentOrbitStateHistoryMetadata.getCenterBody().getGM());
            }
            // we temporarily set gravitational parameter to NaN,
            // as we may get a proper one in the perturbations section
            orbitBlocks.add(new OrbitStateHistory(currentOrbitStateHistoryMetadata,
                                                  currentOrbitStateHistory,
                                                  Double.NaN));
        }
        return true;
    }

    /** Manage physical properties section.
     * @param starting if true, parser is entering the section
     * otherwise it is leaving the section
     * @return always return true
     */
    boolean managePhysicalPropertiesSection(final boolean starting) {
        // TODO
        return true;
    }

    /** Manage covariance history section.
     * @param starting if true, parser is entering the section
     * otherwise it is leaving the section
     * @return always return true
     */
    boolean manageCovarianceHistorySection(final boolean starting) {
        // TODO
        return true;
    }

    /** Manage maneuvers section.
     * @param starting if true, parser is entering the section
     * otherwise it is leaving the section
     * @return always return true
     */
    boolean manageManeuversSection(final boolean starting) {
        // TODO
        return true;
    }

    /** Manage perturbation parameters section.
     * @param starting if true, parser is entering the section
     * otherwise it is leaving the section
     * @return always return true
     */
    boolean managePerturbationParametersSection(final boolean starting) {
        // TODO
        return true;
    }

    /** Manage orbit determination section.
     * @param starting if true, parser is entering the section
     * otherwise it is leaving the section
     * @return always return true
     */
    boolean manageOrbitDeterminationSection(final boolean starting) {
        // TODO
        return true;
    }

    /** Manage user-defined parameters section.
     * @param starting if true, parser is entering the section
     * otherwise it is leaving the section
     * @return always return true
     */
    boolean manageUserDefinedParametersSection(final boolean starting) {
        // TODO
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public OCMFile build() {
        if (userDefinedBlock != null && userDefinedBlock.getParameters().isEmpty()) {
            userDefinedBlock = null;
        }
        final OCMData data = new OCMData(orbitBlocks, physicBlock, covarianceBlocks,
                                         maneuverBlocks, perturbationsBlock,
                                         orbitDeterminationBlock, userDefinedBlock);
        data.checkMandatoryEntries();
        return new OCMFile(header, Collections.singletonList(new Segment<>(metadata, data)),
                           getConventions(), getDataContext(), getSelectedMu());
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
                    return OCMMetadataKey.valueOf(token.getName()).process(token, context, metadata);
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
                   OCMDataSubStructureKey.valueOf(token.getName()).process(token, this);
        } catch (IllegalArgumentException iae) {
            // token has not been recognized
            return false;
        }
    }

    /** Process one orbit state history data token.
     * @param token token to process
     * @return true if token was processed, false otherwise
     */
    private boolean processOrbitStateToken(final ParseToken token) {
        if (token.getName() != null) {
            // we are in the section metadata part
            try {
                return OrbitStateHistoryMetadataKey.valueOf(token.getName()).
                       process(token, context, currentOrbitStateHistoryMetadata);
            } catch (IllegalArgumentException iae) {
                // token has not been recognized
                return false;
            }
        } else {
            // we are in the section data part
            if (currentOrbitStateHistory.isEmpty()) {
                // we are starting the real data section, we can now check metadata is complete
                currentOrbitStateHistoryMetadata.checkMandatoryEntries();
                setFallback(this::processDataSubStructureToken);
            }
            try {
                final String[] fields = SPLIT_AT_BLANKS.split(token.getContent().trim());
                final List<CCSDSUnit> units = currentOrbitStateHistoryMetadata.getOrbUnits();
                if (fields.length != units.size() + 1) {
                    throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                              token.getLineNumber(), token.getFileName(), token.getContent());
                }
                final AbsoluteDate epoch;
                if (fields[0].indexOf('T') > 0) {
                    // absolute date
                    epoch = context.getTimeScale().parseDate(fields[0],
                                                             context.getConventions(),
                                                             metadata.getEpochT0(),
                                                             context.getDataContext().getTimeScales());
                } else {
                    // relative date
                    epoch = metadata.getEpochT0().shiftedBy(Double.parseDouble(fields[0]));
                }
                return currentOrbitStateHistory.add(new OrbitState(currentOrbitStateHistoryMetadata.getOrbType(),
                                                                   epoch, fields, 1, units));
            } catch (NumberFormatException nfe) {
                throw new OrekitException(nfe, OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                          token.getLineNumber(), token.getFileName(), token.getContent());
            }
        }
    }

    /** Process one physical property data token.
     * @param token token to process
     * @return true if token was processed, false otherwise
     */
    private boolean processPhysicalPropertyToken(final ParseToken token) {
        setFallback(this::processDataSubStructureToken);
        // TODO
        return false;
    }

    /** Process one covariance history history data token.
     * @param token token to process
     * @return true if token was processed, false otherwise
     */
    private boolean processCovarianceToken(final ParseToken token) {
        setFallback(this::processDataSubStructureToken);
        // TODO
        return false;
    }

    /** Process one maneuver data token.
     * @param token token to process
     * @return true if token was processed, false otherwise
     */
    private boolean processManeuverToken(final ParseToken token) {
        setFallback(this::processDataSubStructureToken);
        // TODO
        return false;
    }

    /** Process one perturbation parameter data token.
     * @param token token to process
     * @return true if token was processed, false otherwise
     */
    private boolean processPerturbationToken(final ParseToken token) {
        setFallback(this::processDataSubStructureToken);
        // TODO
        return false;
    }

    /** Process one orbit determination data token.
     * @param token token to process
     * @return true if token was processed, false otherwise
     */
    private boolean processOrbitDeterminationToken(final ParseToken token) {
        setFallback(this::processDataSubStructureToken);
        // TODO
        return false;
    }

    /** Process one user-defined parametert data token.
     * @param token token to process
     * @return true if token was processed, false otherwise
     */
    private boolean processUserDefinedToken(final ParseToken token) {
        setFallback(this::processDataSubStructureToken);
        // TODO
        return false;
    }

}
