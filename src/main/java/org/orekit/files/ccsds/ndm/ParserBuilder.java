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
package org.orekit.files.ccsds.ndm;

import java.lang.reflect.Array;
import java.util.List;
import java.util.function.Function;

import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.DataContext;
import org.orekit.files.ccsds.ndm.adm.acm.AcmParser;
import org.orekit.files.ccsds.ndm.adm.aem.AemParser;
import org.orekit.files.ccsds.ndm.adm.apm.ApmParser;
import org.orekit.files.ccsds.ndm.cdm.CdmParser;
import org.orekit.files.ccsds.ndm.odm.ocm.OcmParser;
import org.orekit.files.ccsds.ndm.odm.oem.OemParser;
import org.orekit.files.ccsds.ndm.odm.omm.OmmParser;
import org.orekit.files.ccsds.ndm.odm.opm.OpmParser;
import org.orekit.files.ccsds.ndm.tdm.IdentityConverter;
import org.orekit.files.ccsds.ndm.tdm.RangeUnits;
import org.orekit.files.ccsds.ndm.tdm.RangeUnitsConverter;
import org.orekit.files.ccsds.ndm.tdm.TdmParser;
import org.orekit.files.ccsds.utils.lexical.ParseToken;
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

    /** Filters for parse tokens.
     * @since 12.0
     */
    private final Function<ParseToken, List<ParseToken>>[] filters;

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
     *   <li>{@link #getEquatorialRadius() central body equatorial radius} set to {@code Double.NaN}</li>
     *   <li>{@link #getFlattening() central body flattening} set to {@code Double.NaN}</li>
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
     *   <li>{@link #getEquatorialRadius() central body equatorial radius} set to {@code Double.NaN}</li>
     *   <li>{@link #getFlattening() central body flattening} set to {@code Double.NaN}</li>
     *   <li>{@link #getDefaultMass() default mass} set to {@code Double.NaN}</li>
     *   <li>{@link #getDefaultInterpolationDegree() default interpolation degree} set to {@code 1}</li>
     *   <li>{@link #getParsedUnitsBehavior() parsed unit behavior} set to {@link ParsedUnitsBehavior#CONVERT_COMPATIBLE}</li>
     *   <li>{@link #getRangeUnitsConverter() converter for range units} set to {@link IdentityConverter}</li>
     * </ul>
     * @param dataContext data context used to retrieve frames, time scales, etc.
     */
    @SuppressWarnings("unchecked")
    public ParserBuilder(final DataContext dataContext) {
        this(IERSConventions.IERS_2010, Double.NaN, Double.NaN, dataContext,
             null, new IdentityConverter(), true, Double.NaN, Double.NaN,
             1, ParsedUnitsBehavior.CONVERT_COMPATIBLE,
             (Function<ParseToken, List<ParseToken>>[]) Array.newInstance(Function.class, 0));
    }

    /** Complete constructor.
     * @param conventions IERS Conventions
     * @param equatorialRadius central body equatorial radius
     * @param flattening central body flattening
     * @param dataContext used to retrieve frames, time scales, etc.
     * @param missionReferenceDate reference date for Mission Elapsed Time or Mission Relative Time time systems
     * @param rangeUnitsConverter converter for {@link RangeUnits#RU Range Units}
     * @param simpleEOP if true, tidal effects are ignored when interpolating EOP
     * @param mu gravitational coefficient
     * @param defaultMass default mass
     * @param defaultInterpolationDegree default interpolation degree
     * @param parsedUnitsBehavior behavior to adopt for handling parsed units
     * @param filters filters to apply to parse tokens
     * @since 12.0
     */
    private ParserBuilder(final IERSConventions conventions,
                          final double equatorialRadius, final double flattening,
                          final DataContext dataContext, final AbsoluteDate missionReferenceDate,
                          final RangeUnitsConverter rangeUnitsConverter,
                          final boolean simpleEOP, final double mu,
                          final double defaultMass, final int defaultInterpolationDegree,
                          final ParsedUnitsBehavior parsedUnitsBehavior,
                          final Function<ParseToken, List<ParseToken>>[] filters) {
        super(conventions, equatorialRadius, flattening, dataContext, missionReferenceDate, rangeUnitsConverter);
        this.simpleEOP                  = simpleEOP;
        this.mu                         = mu;
        this.defaultMass                = defaultMass;
        this.defaultInterpolationDegree = defaultInterpolationDegree;
        this.parsedUnitsBehavior        = parsedUnitsBehavior;
        this.filters                    = filters.clone();
    }

    /** {@inheritDoc} */
    @Override
    protected ParserBuilder create(final IERSConventions newConventions,
                                   final double newEquatorialRadius, final double newFlattening,
                                   final DataContext newDataContext,
                                   final AbsoluteDate newMissionReferenceDate, final RangeUnitsConverter newRangeUnitsConverter) {
        return new ParserBuilder(newConventions, newEquatorialRadius, newFlattening, newDataContext,
                                 newMissionReferenceDate, newRangeUnitsConverter, simpleEOP, mu,
                                 defaultMass, defaultInterpolationDegree, parsedUnitsBehavior, filters);
    }

    /** Set up flag for ignoring tidal effects when interpolating EOP.
     * @param newSimpleEOP true if tidal effects are ignored when interpolating EOP
     * @return a new builder with updated configuration (the instance is not changed)
     */
    public ParserBuilder withSimpleEOP(final boolean newSimpleEOP) {
        return new ParserBuilder(getConventions(), getEquatorialRadius(), getFlattening(), getDataContext(),
                                 getMissionReferenceDate(), getRangeUnitsConverter(), newSimpleEOP, getMu(), getDefaultMass(),
                                 getDefaultInterpolationDegree(), getParsedUnitsBehavior(), getFilters());
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
        return new ParserBuilder(getConventions(), getEquatorialRadius(), getFlattening(), getDataContext(),
                                 getMissionReferenceDate(), getRangeUnitsConverter(), isSimpleEOP(), newMu, getDefaultMass(),
                                 getDefaultInterpolationDegree(), getParsedUnitsBehavior(), getFilters());
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
        return new ParserBuilder(getConventions(), getEquatorialRadius(), getFlattening(), getDataContext(),
                                 getMissionReferenceDate(), getRangeUnitsConverter(), isSimpleEOP(), getMu(), newDefaultMass,
                                 getDefaultInterpolationDegree(), getParsedUnitsBehavior(), getFilters());
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
        return new ParserBuilder(getConventions(), getEquatorialRadius(), getFlattening(), getDataContext(),
                                 getMissionReferenceDate(), getRangeUnitsConverter(), isSimpleEOP(), getMu(), getDefaultMass(),
                                 newDefaultInterpolationDegree, getParsedUnitsBehavior(), getFilters());
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
        return new ParserBuilder(getConventions(), getEquatorialRadius(), getFlattening(), getDataContext(),
                                 getMissionReferenceDate(), getRangeUnitsConverter(), isSimpleEOP(), getMu(), getDefaultMass(),
                                 getDefaultInterpolationDegree(), newParsedUnitsBehavior, getFilters());
    }

    /** Get the behavior to adopt for handling parsed units.
     * @return behavior to adopt for handling parsed units
     */
    public ParsedUnitsBehavior getParsedUnitsBehavior() {
        return parsedUnitsBehavior;
    }

    /** Add a filter for parsed tokens.
     * <p>
     * This filter allows to change parsed tokens. This method can be called several times,
     * once for each filter to set up. The filters are always applied in the order they were set.
     * There are several use cases for this feature.
     * </p>
     * <p>
     * The first use case is to allow parsing malformed CCSDS messages with some known
     * discrepancies that can be fixed. One real life example (the one that motivated the
     * development of this feature) is OMM files in XML format that add an empty
     * OBJECT_ID. This could be fixed by setting a filter as follows:
     * </p>
     * <pre>{@code
     * Omm omm = new ParserBuilder().
     *           withFilter(token -> {
     *                          if ("OBJECT_ID".equals(token.getName()) &&
     *                              (token.getRawContent() == null || token.getRawContent().isEmpty())) {
     *                              // replace null/empty entries with "unknown"
     *                              return Collections.singletonList(new ParseToken(token.getType(), token.getName(),
     *                                                                              "unknown", token.getUnits(),
     *                                                                              token.getLineNumber(), token.getFileName()));
     *                          } else {
     *                              return Collections.singletonList(token);
     *                          }
     *                     }).
     *           buildOmmParser().
     *           parseMessage(message);
     * }</pre>
     * <p>
     * A second use case is to remove unwanted data. For example in order to remove all user-defined data
     * one could use:
     * </p>
     * <pre>{@code
     * Omm omm = new ParserBuilder().
     *           withFilter(token -> {
     *                          if (token.getName().startsWith("USER_DEFINED")) {
     *                              return Collections.emptyList();
     *                          } else {
     *                              return Collections.singletonList(token);
     *                          }
     *                     }).
     *           buildOmmmParser().
     *           parseMessage(message);
     * }</pre>
     * <p>
     * A third use case is to add data not originally present in the file. For example in order
     * to add a generated ODM V3 message id to an ODM V2 message that lacks it, one could do:
     * </p>
     * <pre>{@code
     * final String myMessageId = ...; // this could be computed from a counter, or a SHA256 digest, or some metadata
     * Omm omm = new ParserBuilder()
     *           withFilter(token -> {
     *                          if ("CCSDS_OMM_VERS".equals(token.getName())) {
     *                              // enforce ODM V3
     *                              return Collections.singletonList(new ParseToken(token.getType(), token.getName(),
     *                                                                              "3.0", token.getUnits(),
     *                                                                              token.getLineNumber(), token.getFileName()));
     *                          } else {
     *                              return Collections.singletonList(token);
     *                          }
     *                      }).
     *           withFilter(token -> {
     *                          if ("ORIGINATOR".equals(token.getName())) {
     *                              // add generated message ID after ORIGINATOR entry
     *                              return Arrays.asList(token,
     *                                                   new ParseToken(TokenType.ENTRY, "MESSAGE_ID",
     *                                                                  myMessageId, null,
     *                                                                  -1, token.getFileName()));
     *                          } else {
     *                              return Collections.singletonList(token);
     *                          }
     *                      }).
     *           buildOmmmParser().
     *           parseMessage(message);
     * }</pre>
     * @param filter token filter to add
     * @return a new builder with updated configuration (the instance is not changed)
     * @since 12.0
     */
    public ParserBuilder withFilter(final Function<ParseToken, List<ParseToken>> filter) {

        // populate new filters array
        @SuppressWarnings("unchecked")
        final Function<ParseToken, List<ParseToken>>[] newFilters =
                        (Function<ParseToken, List<ParseToken>>[]) Array.newInstance(Function.class, filters.length + 1);
        System.arraycopy(filters, 0, newFilters, 0, filters.length);
        newFilters[filters.length] = filter;

        return new ParserBuilder(getConventions(), getEquatorialRadius(), getFlattening(), getDataContext(),
                                 getMissionReferenceDate(), getRangeUnitsConverter(), isSimpleEOP(), getMu(), getDefaultMass(),
                                 getDefaultInterpolationDegree(), getParsedUnitsBehavior(),
                                 newFilters);

    }

    /** Get the filters to apply to parse tokens.
     * @return filters to apply to parse tokens
     * @since 12.0
     */
    public Function<ParseToken, List<ParseToken>>[] getFilters() {
        return filters.clone();
    }

    /** Build a parser for {@link org.orekit.files.ccsds.ndm.Ndm Navigation Data Messages}.
     * @return a new parser
     */
    public NdmParser buildNdmParser() {
        return new NdmParser(this, getFilters());
    }

    /** Build a parser for {@link org.orekit.files.ccsds.ndm.odm.opm.Opm Orbit Parameters Messages}.
     * @return a new parser
     */
    public OpmParser buildOpmParser() {
        return new OpmParser(getConventions(), isSimpleEOP(), getDataContext(), getMissionReferenceDate(),
                             getMu(), getDefaultMass(), getParsedUnitsBehavior(), getFilters());
    }

    /** Build a parser for {@link org.orekit.files.ccsds.ndm.odm.omm.Omm Orbit Mean elements Messages}.
     * @return a new parser
     */
    public OmmParser buildOmmParser() {
        return new OmmParser(getConventions(), isSimpleEOP(), getDataContext(), getMissionReferenceDate(),
                             getMu(), getDefaultMass(), getParsedUnitsBehavior(), getFilters());
    }

    /** Build a parser for {@link org.orekit.files.ccsds.ndm.odm.oem.Oem Orbit Ephemeris Messages}.
     * @return a new parser
     */
    public OemParser buildOemParser() {
        return new OemParser(getConventions(), isSimpleEOP(), getDataContext(), getMissionReferenceDate(),
                             getMu(), getDefaultInterpolationDegree(), getParsedUnitsBehavior(), getFilters());
    }

    /** Build a parser for {@link org.orekit.files.ccsds.ndm.odm.ocm.Ocm Orbit Comprehensive Messages}.
     * @return a new parser
     */
    public OcmParser buildOcmParser() {
        return new OcmParser(getConventions(), getEquatorialRadius(), getFlattening(),
                             isSimpleEOP(), getDataContext(), getMu(),
                             getParsedUnitsBehavior(), getFilters());
    }

    /** Build a parser for {@link org.orekit.files.ccsds.ndm.adm.apm.Apm Attitude Parameters Messages}.
     * @return a new parser
     */
    public ApmParser buildApmParser() {
        return new ApmParser(getConventions(), isSimpleEOP(), getDataContext(),
                             getMissionReferenceDate(), getParsedUnitsBehavior(), getFilters());
    }

    /** Build a parser for {@link org.orekit.files.ccsds.ndm.adm.aem.Aem Attitude Ephemeris Messages}.
     * @return a new parser
     */
    public AemParser buildAemParser() {
        return new AemParser(getConventions(), isSimpleEOP(), getDataContext(), getMissionReferenceDate(),
                             getDefaultInterpolationDegree(), getParsedUnitsBehavior(), getFilters());
    }

    /** Build a parser for {@link org.orekit.files.ccsds.ndm.adm.acm.Acm Attitude Comprehensive Messages}.
     * @return a new parser
     * @since 12.0
     */
    public AcmParser buildAcmParser() {
        return new AcmParser(getConventions(), isSimpleEOP(), getDataContext(),
                             getParsedUnitsBehavior(), getFilters());
    }

    /** Build a parser for {@link org.orekit.files.ccsds.ndm.tdm.Tdm Tracking Data Messages}.
     * @return a new parser
     */
    public TdmParser buildTdmParser() {
        return new TdmParser(getConventions(), isSimpleEOP(), getDataContext(),
                             getParsedUnitsBehavior(), getRangeUnitsConverter(), getFilters());
    }

    /** Build a parser for {@link org.orekit.files.ccsds.ndm.cdm.Cdm Conjunction Data Messages}.
     * @return a new parser
     */
    public CdmParser buildCdmParser() {
        return new CdmParser(getConventions(), isSimpleEOP(), getDataContext(),
                             getParsedUnitsBehavior(), getFilters());
    }

}
