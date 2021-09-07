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
package org.orekit.files.ccsds.ndm;

import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.DataContext;
import org.orekit.files.ccsds.ndm.adm.aem.AemParser;
import org.orekit.files.ccsds.ndm.adm.apm.ApmParser;
import org.orekit.files.ccsds.ndm.odm.ocm.OcmParser;
import org.orekit.files.ccsds.ndm.odm.oem.OemParser;
import org.orekit.files.ccsds.ndm.odm.omm.OmmParser;
import org.orekit.files.ccsds.ndm.odm.opm.OpmParser;
import org.orekit.files.ccsds.ndm.tdm.IdentityConverter;
import org.orekit.files.ccsds.ndm.tdm.RangeUnits;
import org.orekit.files.ccsds.ndm.tdm.RangeUnitsConverter;
import org.orekit.files.ccsds.ndm.tdm.TdmParser;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.IERSConventions;

/** Builder for all {@link NdmConstituent CCSDS Message} files parsers.
 * <p>
 * This builder can be used for building all CCSDS Messages parsers types.
 * It is particularly useful in multi-threaded context as parsers cannot
 * be shared between threads and thus several independent parsers must be
 * built in this case.
 * </p>
 * @author Luc Maisonobe
 * @since 11.0
 */
public class ParserBuilder extends AbstractBuilder<ParserBuilder> {

    /** Indicator for simple or accurate EOP interpolation. */
    private final  boolean simpleEOP;

    /** Gravitational coefficient. */
    private final double mu;

    /** Default mass. */
    private final double defaultMass;

    /** Default interpolation degree. */
    private final int defaultInterpolationDegree;

    /** Behavior adopted for units that have been parsed from a CCSDS message. */
    private final ParsedUnitsBehavior parsedUnitsBehavior;

    /**
     * Simple constructor.
     * <p>
     * This constructor creates a builder with
     * <ul>
     *   <li>{@link #getConventions() IERS conventions} set to {@link IERSConventions#IERS_2010}</li>
     *   <li>{@link #isSimpleEOP() simple EOP} set to {@code true}</li>
     *   <li>{@link #getDataContext() data context} set to {@link DataContext#getDefault() default context}</li>
     *   <li>{@link #getMissionReferenceDate() mission reference date} set to {@code null}</li>
     *   <li>{@link #getMu() gravitational coefficient} set to {@code Double.NaN}</li>
     *   <li>{@link #getDefaultMass() default mass} set to {@code Double.NaN}</li>
     *   <li>{@link #getDefaultInterpolationDegree() default interpolation degree} set to {@code 1}</li>
     *   <li>{@link #getParsedUnitsBehavior() parsed unit behavior} set to {@link ParsedUnitsBehavior#CONVERT_COMPATIBLE}</li>
     *   <li>{@link #getRangeUnitsConverter() converter for range units} set to {@link IdentityConverter}</li>
     * </ul>
     */
    @DefaultDataContext
    public ParserBuilder() {
        this(DataContext.getDefault());
    }

    /**
     * Simple constructor.
     * <p>
     * This constructor creates a builder with
     * <ul>
     *   <li>{@link #getConventions() IERS conventions} set to {@link IERSConventions#IERS_2010}</li>
     *   <li>{@link #isSimpleEOP() simple EOP} set to {@code true}</li>
     *   <li>{@link #getMissionReferenceDate() mission reference date} set to {@code null}</li>
     *   <li>{@link #getMu() gravitational coefficient} set to {@code Double.NaN}</li>
     *   <li>{@link #getDefaultMass() default mass} set to {@code Double.NaN}</li>
     *   <li>{@link #getDefaultInterpolationDegree() default interpolation degree} set to {@code 1}</li>
     *   <li>{@link #getParsedUnitsBehavior() parsed unit behavior} set to {@link ParsedUnitsBehavior#CONVERT_COMPATIBLE}</li>
     *   <li>{@link #getRangeUnitsConverter() converter for range units} set to {@link IdentityConverter}</li>
     * </ul>
     * @param dataContext data context used to retrieve frames, time scales, etc.
     */
    public ParserBuilder(final DataContext dataContext) {
        this(IERSConventions.IERS_2010, dataContext, null, new IdentityConverter(),
             true, Double.NaN, Double.NaN, 1, ParsedUnitsBehavior.CONVERT_COMPATIBLE);
    }

    /** Complete constructor.
     * @param conventions IERS Conventions
     * @param dataContext used to retrieve frames, time scales, etc.
     * @param missionReferenceDate reference date for Mission Elapsed Time or Mission Relative Time time systems
     * @param simpleEOP if true, tidal effects are ignored when interpolating EOP
     * @param mu gravitational coefficient
     * @param defaultMass default mass
     * @param defaultInterpolationDegree default interpolation degree
     * @param parsedUnitsBehavior behavior to adopt for handling parsed units
     * @param rangeUnitsConverter converter for {@link RangeUnits#RU Range Units}
     */
    private ParserBuilder(final IERSConventions conventions, final DataContext dataContext,
                          final AbsoluteDate missionReferenceDate,
                          final RangeUnitsConverter rangeUnitsConverter,
                          final boolean simpleEOP, final double mu, final double defaultMass,
                          final int defaultInterpolationDegree,
                          final ParsedUnitsBehavior parsedUnitsBehavior) {
        super(conventions, dataContext, missionReferenceDate, rangeUnitsConverter);
        this.simpleEOP                  = simpleEOP;
        this.mu                         = mu;
        this.defaultMass                = defaultMass;
        this.defaultInterpolationDegree = defaultInterpolationDegree;
        this.parsedUnitsBehavior        = parsedUnitsBehavior;
    }

    /** {@inheritDoc} */
    @Override
    protected ParserBuilder create(final IERSConventions newConventions, final DataContext newDataContext,
                                   final AbsoluteDate newMissionReferenceDate, final RangeUnitsConverter newRangeUnitsConverter) {
        return new ParserBuilder(newConventions, newDataContext, newMissionReferenceDate, newRangeUnitsConverter,
                                 simpleEOP, mu, defaultMass, defaultInterpolationDegree, parsedUnitsBehavior);
    }

    /** Set up flag for ignoring tidal effects when interpolating EOP.
     * @param newSimpleEOP true if tidal effects are ignored when interpolating EOP
     * @return a new builder with updated configuration (the instance is not changed)
     */
    public ParserBuilder withSimpleEOP(final boolean newSimpleEOP) {
        return new ParserBuilder(getConventions(), getDataContext(), getMissionReferenceDate(), getRangeUnitsConverter(),
                                 newSimpleEOP, getMu(), getDefaultMass(),
                                 getDefaultInterpolationDegree(), getParsedUnitsBehavior());
    }

    /** Check if tidal effects are ignored when interpolating EOP.
     * @return true if tidal effects are ignored when interpolating EOP
     */
    public boolean isSimpleEOP() {
        return simpleEOP;
    }

    /** Set up the gravitational coefficient.
     * @param newMu gravitational coefficient
     * @return a new builder with updated configuration (the instance is not changed)
     */
    public ParserBuilder withMu(final double newMu) {
        return new ParserBuilder(getConventions(), getDataContext(), getMissionReferenceDate(), getRangeUnitsConverter(),
                                 isSimpleEOP(), newMu, getDefaultMass(),
                                 getDefaultInterpolationDegree(), getParsedUnitsBehavior());
    }

    /** Get the gravitational coefficient.
     * @return gravitational coefficient
     */
    public double getMu() {
        return mu;
    }

    /** Set up the default mass.
     * <p>
     * The default mass is used only by {@link OpmParser}.
     * </p>
     * @param newDefaultMass default mass
     * @return a new builder with updated configuration (the instance is not changed)
     */
    public ParserBuilder withDefaultMass(final double newDefaultMass) {
        return new ParserBuilder(getConventions(), getDataContext(), getMissionReferenceDate(), getRangeUnitsConverter(),
                                 isSimpleEOP(), getMu(), newDefaultMass,
                                 getDefaultInterpolationDegree(), getParsedUnitsBehavior());
    }

    /** Get the default mass.
     * @return default mass
     */
    public double getDefaultMass() {
        return defaultMass;
    }

    /** Set up the default interpolation degree.
     * <p>
     * The default interpolation degree is used only by {@link AemParser}
     * and {@link OemParser}.
     * </p>
     * @param newDefaultInterpolationDegree default interpolation degree
     * @return a new builder with updated configuration (the instance is not changed)
     */
    public ParserBuilder withDefaultInterpolationDegree(final int newDefaultInterpolationDegree) {
        return new ParserBuilder(getConventions(), getDataContext(), getMissionReferenceDate(), getRangeUnitsConverter(),
                                 isSimpleEOP(), getMu(), getDefaultMass(),
                                 newDefaultInterpolationDegree, getParsedUnitsBehavior());
    }

    /** Get the default interpolation degree.
     * @return default interpolation degree
     */
    public int getDefaultInterpolationDegree() {
        return defaultInterpolationDegree;
    }

    /** Set up the behavior to adopt for handling parsed units.
     * @param newParsedUnitsBehavior behavior to adopt for handling parsed units
     * @return a new builder with updated configuration (the instance is not changed)
     */
    public ParserBuilder withParsedUnitsBehavior(final ParsedUnitsBehavior newParsedUnitsBehavior) {
        return new ParserBuilder(getConventions(), getDataContext(), getMissionReferenceDate(), getRangeUnitsConverter(),
                                 isSimpleEOP(), getMu(), getDefaultMass(),
                                 getDefaultInterpolationDegree(), newParsedUnitsBehavior);
    }

    /** Get the behavior to adopt for handling parsed units.
     * @return behavior to adopt for handling parsed units
     */
    public ParsedUnitsBehavior getParsedUnitsBehavior() {
        return parsedUnitsBehavior;
    }

    /** Build a parser for {@link org.orekit.files.ccsds.ndm.Ndm Navigation Data Messages}.
     * @return a new parser
     */
    public NdmParser buildNdmParser() {
        return new NdmParser(this);
    }

    /** Build a parser for {@link org.orekit.files.ccsds.ndm.odm.opm.Opm Orbit Parameters Messages}.
     * @return a new parser
     */
    public OpmParser buildOpmParser() {
        return new OpmParser(getConventions(), isSimpleEOP(), getDataContext(), getMissionReferenceDate(),
                             getMu(), getDefaultMass(), getParsedUnitsBehavior());
    }

    /** Build a parser for {@link org.orekit.files.ccsds.ndm.odm.omm.Omm Orbit Mean elements Messages}.
     * @return a new parser
     */
    public OmmParser buildOmmParser() {
        return new OmmParser(getConventions(), isSimpleEOP(), getDataContext(), getMissionReferenceDate(),
                             getMu(), getDefaultMass(), getParsedUnitsBehavior());
    }

    /** Build a parser for {@link org.orekit.files.ccsds.ndm.odm.oem.Oem Orbit Ephemeris Messages}.
     * @return a new parser
     */
    public OemParser buildOemParser() {
        return new OemParser(getConventions(), isSimpleEOP(), getDataContext(), getMissionReferenceDate(),
                             getMu(), getDefaultInterpolationDegree(), getParsedUnitsBehavior());
    }

    /** Build a parser for {@link org.orekit.files.ccsds.ndm.odm.ocm.Ocm Orbit Comprehensive Messages}.
     * @return a new parser
     */
    public OcmParser buildOcmParser() {
        return new OcmParser(getConventions(), isSimpleEOP(), getDataContext(), getMu(), getParsedUnitsBehavior());
    }

    /** Build a parser for {@link org.orekit.files.ccsds.ndm.adm.apm.Apm Attitude Parameters Messages}.
     * @return a new parser
     */
    public ApmParser buildApmParser() {
        return new ApmParser(getConventions(), isSimpleEOP(), getDataContext(),
                             getMissionReferenceDate(), getParsedUnitsBehavior());
    }

    /** Build a parser for {@link org.orekit.files.ccsds.ndm.adm.aem.Aem Attitude Ephemeris Messages}.
     * @return a new parser
     */
    public AemParser buildAemParser() {
        return new AemParser(getConventions(), isSimpleEOP(), getDataContext(), getMissionReferenceDate(),
                             getDefaultInterpolationDegree(), getParsedUnitsBehavior());
    }

    /** Build a parser for {@link org.orekit.files.ccsds.ndm.tdm.Tdm Tracking Data Messages}.
     * @return a new parser
     */
    public TdmParser buildTdmParser() {
        return new TdmParser(getConventions(), isSimpleEOP(), getDataContext(),
                             getParsedUnitsBehavior(), getRangeUnitsConverter());
    }

}
