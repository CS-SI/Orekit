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
package org.orekit.files.ccsds.ndm.odm;

import org.orekit.bodies.CelestialBodies;
import org.orekit.data.DataContext;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.utils.CcsdsTimeScale;
import org.orekit.files.ccsds.utils.CenterName;
import org.orekit.files.ccsds.utils.KeyValue;
import org.orekit.files.ccsds.utils.state.ProcessingState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.IERSConventions;

/** Common parser for Orbit Parameter/Ephemeris/Mean Message files.
 * @param <T> type of the ODM file
 * @param <P> type of the parser
 * @author Luc Maisonobe
 * @since 11.0
 */
public abstract class OCommonParser<T extends ODMFile<?>, P extends ODMParser<T, ?>> extends ODMParser<T, P> {

    /** Gravitational coefficient set by the user in the parser. */
    private double muSet;

    /** Gravitational coefficient parsed in the ODM File. */
    private double muParsed;

    /** Gravitational coefficient created from the knowledge of the central body. */
    private double muCreated;

    /** Reference date for Mission Elapsed Time or Mission Relative Time time systems. */
    private AbsoluteDate missionReferenceDate;

    /** Complete constructor.
     * @param conventions IERS Conventions
     * @param simpleEOP if true, tidal effects are ignored when interpolating EOP
     * @param dataContext used to retrieve frames and time scales
     * @param initialState initial parsing state
     * @param missionReferenceDate reference date for Mission Elapsed Time or Mission Relative Time time systems
     * @param mu gravitational coefficient
     */
    protected OCommonParser(final IERSConventions conventions, final boolean simpleEOP,
                            final DataContext dataContext, final ProcessingState initialState,
                            final AbsoluteDate missionReferenceDate, final double mu) {
        super(conventions, simpleEOP, dataContext, initialState);
        this.missionReferenceDate = missionReferenceDate;
        this.muSet                = mu;
        this.muParsed             = Double.NaN;
        this.muCreated            = Double.NaN;
    }

    /** Set initial date.
     * @param newMissionReferenceDate mission reference date to use while parsing
     * @return a new instance, with mission reference date replaced
     * @see #getMissionReferenceDate()
     */
    public P withMissionReferenceDate(final AbsoluteDate newMissionReferenceDate) {
        return create(getConventions(), isSimpleEOP(), getDataContext(), getInitialState(),
                      newMissionReferenceDate, muSet);
    }

    /**
     * Get reference date for Mission Elapsed Time and Mission Relative Time time systems.
     * @return the reference date
     */
    public AbsoluteDate getMissionReferenceDate() {
        return missionReferenceDate;
    }

    /** Set gravitational coefficient.
     * @param newMu gravitational coefficient to use while parsing
     * @return a new instance, with gravitational coefficient value replaced
     * @see #getMu()
     */
    public P withMu(final double newMu) {
        return create(getConventions(), isSimpleEOP(), getDataContext(), getInitialState(),
                      missionReferenceDate, newMu);
    }

    /** {@inheritDoc} */
    @Override
    protected P create(final IERSConventions newConventions,
                                      final boolean newSimpleEOP,
                                      final DataContext newDataContext,
                                      final ProcessingState newInitialState) {
        return create(newConventions, newSimpleEOP, newDataContext, newInitialState,
                      missionReferenceDate, muSet);
    }

    /** Build a new instance.
     * @param newConventions IERS conventions to use while parsing
     * @param newSimpleEOP if true, tidal effects are ignored when interpolating EOP
     * @param newDataContext data context used for frames, time scales, and celestial bodies
     * @param newInitialState initial parsing state
     * @param newMissionReferenceDate mission reference date to use while parsing
     * @param newMu gravitational coefficient to use while parsing
     * @return a new instance with changed parameters
     */
    protected abstract P create(IERSConventions newConventions,
                               boolean newSimpleEOP,
                               DataContext newDataContext,
                               ProcessingState newInitialState,
                               AbsoluteDate newMissionReferenceDate,
                               double newMu);

    /** Get the gravitational coefficient set at construction or by calling {@link #withMu()}.
     * @return gravitational coefficient set at construction or by calling {@link #withMu()}
     */
    protected double getMuSet() {
        return muSet;
    }

    /** {@inheritDoc} */
    @Override
    protected AbsoluteDate parseDate(final String date, final CcsdsTimeScale timeSystem,
                                     final int lineNumber, final String fileName, final String line) {
        return timeSystem.parseDate(date, getConventions(), missionReferenceDate,
                                    getDataContext().getTimeScales());
    }

   /** Parse a meta-data key = value entry.
     * @param keyValue key = value pair
     * @param metaData instance to update with parsed entry
     * @param lineNumber number of line being parsed
     * @param fileName name of the parsed file
     * @param line full parsed line
     * @return true if the keyword was a meta-data keyword and has been parsed
     */
    protected boolean parseMetaDataEntry(final KeyValue keyValue, final OCommonMetadata metaData,
                                         final int lineNumber, final String fileName, final String line) {
        if (super.parseMetaDataEntry(keyValue, metaData, lineNumber, fileName, line)) {
            return true;
        } else {
            switch (keyValue.getKeyword()) {

                case OBJECT_ID: {
                    metaData.setObjectID(keyValue.getValue());
                    return true;
                }

                case CENTER_NAME:
                    metaData.setCenterName(keyValue.getValue());
                    final String canonicalValue;
                    if (keyValue.getValue().equals("SOLAR SYSTEM BARYCENTER") || keyValue.getValue().equals("SSB")) {
                        canonicalValue = "SOLAR_SYSTEM_BARYCENTER";
                    } else if (keyValue.getValue().equals("EARTH MOON BARYCENTER") || keyValue.getValue().equals("EARTH-MOON BARYCENTER") ||
                                    keyValue.getValue().equals("EARTH BARYCENTER") || keyValue.getValue().equals("EMB")) {
                        canonicalValue = "EARTH_MOON";
                    } else {
                        canonicalValue = keyValue.getValue();
                    }
                    final CenterName c;
                    try {
                        c = CenterName.valueOf(canonicalValue);
                        metaData.setHasCreatableBody(true);
                        final CelestialBodies celestialBodies = getDataContext().getCelestialBodies();
                        metaData.setCenterBody(c.getCelestialBody(celestialBodies));
                        setMuCreated(c.getCelestialBody(celestialBodies).getGM());
                    } catch (IllegalArgumentException n) {
                        // intentionally ignored, as there is no limit to acceptable center names
                    }
                    return true;

                case REF_FRAME:
                    metaData.setFrameString(keyValue.getValue());
                    metaData.setRefFrame(parseCCSDSFrame(keyValue.getValue())
                                         .getFrame(getConventions(), isSimpleEOP(), getDataContext()));
                    return true;

                case REF_FRAME_EPOCH:
                    // for OPM, OEM and OMM, REF_FRAME_EPOCH appears in metadata *before* TIME_SYSTEM
                    // so we can only store the string value here, it will be converted later on
                    metaData.setFrameEpochString(keyValue.getValue());
                    return true;

                case TIME_SYSTEM:
                    final CcsdsTimeScale timeSystem = CcsdsTimeScale.parse(keyValue.getValue());
                    metaData.setTimeSystem(timeSystem);
                    if (metaData.getFrameEpochString() != null) {
                        // convert ref frame epoch to a proper data now that we know the time system
                        metaData.setFrameEpoch(parseDate(metaData.getFrameEpochString(), timeSystem,
                                                         lineNumber, fileName, line));
                    }
                    return true;

                default:
                    return false;
            }
        }
    }

    /**
     * Set the gravitational coefficient parsed in the ODM File.
     * @param muParsed the coefficient to be set
     */
    void setMuParsed(final double muParsed) {
        this.muParsed = muParsed;
    }

    /**
     * Set the gravitational coefficient created from the knowledge of the central body.
     * @param muCreated the coefficient to be set
     */
    void setMuCreated(final double muCreated) {
        this.muCreated = muCreated;
    }

    /**
     * Select the gravitational coefficient to use.
     * In order of decreasing priority, finalMU is set equal to:
     * <ol>
     *   <li>the coefficient parsed in the file,</li>
     *   <li>the coefficient set by the user with the parser's method setMu,</li>
     *   <li>the coefficient created from the knowledge of the central body.</li>
     * </ol>
     * @return selected gravitational coefficient
     */
    public double getSelectedMu() {
        if (!Double.isNaN(muParsed)) {
            return muParsed;
        } else if (!Double.isNaN(muSet)) {
            return muSet;
        } else if (!Double.isNaN(muCreated)) {
            return muCreated;
        } else {
            throw new OrekitException(OrekitMessages.CCSDS_UNKNOWN_GM);
        }
    }

}
