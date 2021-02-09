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
import org.orekit.files.ccsds.ndm.odm.OCommonParser;
import org.orekit.files.ccsds.ndm.odm.ODMHeader;
import org.orekit.files.ccsds.ndm.odm.ODMHeaderProcessingState;
import org.orekit.files.ccsds.ndm.odm.ODMUserDefined;
import org.orekit.files.ccsds.ndm.odm.opm.OPMManeuver;
import org.orekit.files.ccsds.section.Header;
import org.orekit.files.ccsds.section.Segment;
import org.orekit.files.ccsds.section.XMLStructureProcessingState;
import org.orekit.files.ccsds.utils.ParsingContext;
import org.orekit.files.ccsds.utils.lexical.FileFormat;
import org.orekit.files.ccsds.utils.state.ErrorState;
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
    private List<OrbitStateHistory> currentOrbitStateHistory;

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
     * @param missionReferenceDate reference date for Mission Elapsed Time or Mission Relative Time time systems
     * (may be null if time system is absolute)
     * @param mu gravitational coefficient
     */
    public OCMParser(final IERSConventions conventions, final boolean simpleEOP,
                     final DataContext dataContext,
                     final AbsoluteDate missionReferenceDate, final double mu) {
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
            structureProcessor = new ErrorState(); // should never be called
            reset(fileFormat, new ODMHeaderProcessingState(getDataContext(), this, header));
        }
    }

    /** {@inheritDoc} */
    @Override
    public void prepareHeader() {
        setFallback(new ODMHeaderProcessingState(getDataContext(), this, header));
    }

    /** {@inheritDoc} */
    @Override
    public void inHeader() {
        setFallback(getFileFormat() == FileFormat.XML ? structureProcessor : this::processMetadataToken);
    }

    /** {@inheritDoc} */
    @Override
    public void finalizeHeader() {
        header.checkMandatoryEntries();
    }

    /** {@inheritDoc} */
    @Override
    public void prepareMetadata() {
        metadata  = new OCMMetadata();
        context   = new ParsingContext(this::getConventions,
                                       this::isSimpleEOP,
                                       this::getDataContext,
                                       this::getMissionReferenceDate,
                                       metadata::getTimeSystem);
        setFallback(this::processMetadataToken);
    }

    /** {@inheritDoc} */
    @Override
    public void inMetadata() {
        setFallback(getFileFormat() == FileFormat.XML ? structureProcessor : this::processStateVectorToken);
    }

    /** {@inheritDoc} */
    @Override
    public void finalizeMetadata() {
        metadata.checkMandatoryEntries();
        // TODO: move after orbit state history metadara parsing
        if (metadata.getCenterBody() != null) {
            setMuCreated(metadata.getCenterBody().getGM());
        }
    }

    /** {@inheritDoc} */
    @Override
    public void prepareData() {
        orbitBlocks = new ArrayList<>();
        currentOrbitStateHistoryMetadata =
                        new OrbitStateHistoryMetadata(metadata.getEpochT0(), getDataContext());
        setFallback(this::processStateVectorToken);
    }

    /** {@inheritDoc} */
    @Override
    public void inData() {
        // nothing to do
    }

    /** {@inheritDoc} */
    @Override
    public void finalizeData() {
        // nothing to do
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

}
