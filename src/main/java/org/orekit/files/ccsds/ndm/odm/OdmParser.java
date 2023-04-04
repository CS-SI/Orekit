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
package org.orekit.files.ccsds.ndm.odm;

import java.util.List;
import java.util.function.Function;

import org.orekit.data.DataContext;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.ndm.NdmConstituent;
import org.orekit.files.ccsds.ndm.ParsedUnitsBehavior;
import org.orekit.files.ccsds.utils.lexical.ParseToken;
import org.orekit.files.ccsds.utils.parsing.AbstractConstituentParser;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.IERSConventions;

/** Common parser for Orbit Parameter/Ephemeris/Mean/Comprehensive Messages.
 * <p>
 * Note than starting with Orekit 11.0, CCSDS message parsers are
 * mutable objects that gather the data being parsed, until the
 * message is complete and the {@link #parseMessage(org.orekit.data.DataSource)
 * parseMessage} method has returned. This implies that parsers
 * should <em>not</em> be used in a multi-thread context. The recommended
 * way to use parsers is to either dedicate one parser for each message
 * and drop it afterwards, or to use a single-thread loop.
 * </p>
 * @param <T> type of the ODM file
 * @param <P> type of the parser
 * @author Luc Maisonobe
 * @since 11.0
 */
public abstract class OdmParser<T extends NdmConstituent<OdmHeader, ?>, P extends OdmParser<T, ?>>
    extends AbstractConstituentParser<OdmHeader, T, P> {

    /** Reference date for Mission Elapsed Time or Mission Relative Time time systems. */
    private final AbsoluteDate missionReferenceDate;

    /** Gravitational coefficient set by the user in the parser. */
    private final double muSet;

    /** Gravitational coefficient parsed in the ODM File. */
    private double muParsed;

    /** Gravitational coefficient created from the knowledge of the central body. */
    private double muCreated;

    /** Complete constructor.
     * @param root root element for XML files
     * @param formatVersionKey key for format version
     * @param conventions IERS Conventions
     * @param simpleEOP if true, tidal effects are ignored when interpolating EOP
     * @param dataContext used to retrieve frames and time scales
     * @param missionReferenceDate reference date for Mission Elapsed Time or Mission Relative Time time systems
     * @param mu gravitational coefficient
     * @param parsedUnitsBehavior behavior to adopt for handling parsed units
     * @param filters filters to apply to parse tokens
     * @since 12.0
     */
    protected OdmParser(final String root, final String formatVersionKey,
                        final IERSConventions conventions, final boolean simpleEOP,
                        final DataContext dataContext, final AbsoluteDate missionReferenceDate,
                        final double mu, final ParsedUnitsBehavior parsedUnitsBehavior,
                        final Function<ParseToken, List<ParseToken>>[] filters) {
        super(root, formatVersionKey, conventions, simpleEOP, dataContext, parsedUnitsBehavior, filters);
        this.missionReferenceDate = missionReferenceDate;
        this.muSet                = mu;
        this.muParsed             = Double.NaN;
        this.muCreated            = Double.NaN;
    }

    /**
     * Get reference date for Mission Elapsed Time and Mission Relative Time time systems.
     * @return the reference date
     */
    public AbsoluteDate getMissionReferenceDate() {
        return missionReferenceDate;
    }

    /** Get the gravitational coefficient set at construction.
     * @return gravitational coefficient set at construction
     */
    protected double getMuSet() {
        return muSet;
    }

    /**
     * Set the gravitational coefficient parsed in the ODM File.
     * @param muParsed the coefficient to be set
     */
    protected void setMuParsed(final double muParsed) {
        this.muParsed = muParsed;
    }

    /**
     * Set the gravitational coefficient created from the knowledge of the central body.
     * @param muCreated the coefficient to be set
     */
    protected void setMuCreated(final double muCreated) {
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
