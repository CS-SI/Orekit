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
import org.orekit.files.ccsds.ndm.adm.aem.AEMParser;
import org.orekit.files.ccsds.ndm.adm.apm.APMParser;
import org.orekit.files.ccsds.ndm.odm.ocm.OCMParser;
import org.orekit.files.ccsds.ndm.odm.oem.OEMParser;
import org.orekit.files.ccsds.ndm.odm.omm.OMMParser;
import org.orekit.files.ccsds.ndm.odm.opm.OPMParser;
import org.orekit.files.ccsds.ndm.tdm.TDMParser;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.IERSConventions;

/** Build for all {@link NDMFile CCSDS Message} files.
 * <p>
 * This builder can be used for building all CCSDS Messages parsers types.
 * It is particularly useful in multi-threaded context as parsers cannot
 * be shared between threads and thus several independant parsers must be
 * built in this case.
 * </p>
 * @author Luc Maisonobe
 * @since 11.0
 */
public class ParserBuilder {

    /** IERS conventions used. */
    private final IERSConventions conventions;

    /** Indicator for simple or accurate EOP interpolation. */
    private final  boolean simpleEOP;

    /** Data context. */
    private final DataContext dataContext;

    /** Reference date for Mission Elapsed Time or Mission Relative Time time systems. */
    private final AbsoluteDate missionReferenceDate;

    /** Gravitational coefficient. */
    private final double mu;

    /** Default mass. */
    private final double defaultMass;

    /** Default interpolation degree. */
    private final int defaultInterpolationDegree;

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
     * </ul>
     * </p>
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
     * </ul>
     * </p>
     * @param dataContext data context used to retrieve frames, time scales, etc.
     */
    public ParserBuilder(final DataContext dataContext) {
        this(IERSConventions.IERS_2010, true, dataContext, null, Double.NaN, Double.NaN, 1);
    }

    /**
     * Complete constructor.
     * @param conventions IERS Conventions
     * @param simpleEOP if true, tidal effects are ignored when interpolating EOP
     * @param dataContext used to retrieve frames, time scales, etc.
     * @param missionReferenceDate reference date for Mission Elapsed Time or Mission Relative Time time systems
     * @param mu gravitational coefficient
     * @param defaultMass default mass
     * @param defaultInterpolationDegree default interpolation degree
     */
    private ParserBuilder(final IERSConventions conventions, final boolean simpleEOP, final DataContext dataContext,
                          final AbsoluteDate missionReferenceDate, final double mu, final double defaultMass,
                          final int defaultInterpolationDegree) {
        this.conventions                = conventions;
        this.simpleEOP                  = simpleEOP;
        this.dataContext                = dataContext;
        this.missionReferenceDate       = missionReferenceDate;
        this.mu                         = mu;
        this.defaultMass                = defaultMass;
        this.defaultInterpolationDegree = defaultInterpolationDegree;
    }

    /** Set up IERS conventions.
     * @param newConventions IERS Conventions
     * @return a new builder with updated configuration (the instance is not changed)
     */
    public ParserBuilder withConventions(final IERSConventions newConventions) {
        return new ParserBuilder(newConventions, isSimpleEOP(), getDataContext(),
                                 getMissionReferenceDate(), getMu(), getDefaultMass(),
                                 getDefaultInterpolationDegree());
    }

    /** Get the IERS conventions.
     * @return IERS conventions
     */
    public IERSConventions getConventions() {
        return conventions;
    }

    /** Set up flag for ignoring tidal effects when interpolating EOP.
     * @param newSimpleEOP true if tidal effects are ignored when interpolating EOP
     * @return a new builder with updated configuration (the instance is not changed)
     */
    public ParserBuilder withSimpleEOP(final boolean newSimpleEOP) {
        return new ParserBuilder(getConventions(), newSimpleEOP, getDataContext(),
                                 getMissionReferenceDate(), getMu(), getDefaultMass(),
                                 getDefaultInterpolationDegree());
    }

    /** Check if tidal effects are ignored when interpolating EOP.
     * @return true if tidal effects are ignored when interpolating EOP
     */
    public boolean isSimpleEOP() {
        return simpleEOP;
    }

    /** Set up data context used to retrieve frames, time scales, etc..
     * @param newDataContext data context used to retrieve frames, time scales, etc.
     * @return a new builder with updated configuration (the instance is not changed)
     */
    public ParserBuilder withDataContext(final DataContext newDataContext) {
        return new ParserBuilder(getConventions(), isSimpleEOP(), newDataContext,
                                 getMissionReferenceDate(), getMu(), getDefaultMass(),
                                 getDefaultInterpolationDegree());
    }

    /** Get the data context.
     * @return data context used to retrieve frames, time scales, etc.
     */
    public DataContext getDataContext() {
        return dataContext;
    }

    /** Set up mission reference date or Mission Elapsed Time or Mission Relative Time time systems.
     * <p>
     * The mission reference date is used only by {@link AEMParser} and {@link APMParser},
     * and by {@link OPMParser}, {@link OMMParser} and {@link OEMParser} up to version 2.0
     * of ODM (starting with version 3.0of ODM, both MET and MRT time system have been
     * withdrawn from the standard).
     * </p>
     * @param newMissionReferenceDate mission reference date or Mission Elapsed Time or Mission Relative Time time systems
     * @return a new builder with updated configuration (the instance is not changed)
     */
    public ParserBuilder withMissionReferenceDate(final AbsoluteDate newMissionReferenceDate) {
        return new ParserBuilder(getConventions(), isSimpleEOP(), getDataContext(),
                                 newMissionReferenceDate, getMu(), getDefaultMass(),
                                 getDefaultInterpolationDegree());
    }

    /** Get the mission reference date or Mission Elapsed Time or Mission Relative Time time systems.
     * @return mission reference date
     */
    public AbsoluteDate getMissionReferenceDate() {
        return missionReferenceDate;
    }

    /** Set up the gravitational coefficient.
     * @param newMu gravitational coefficient
     * @return a new builder with updated configuration (the instance is not changed)
     */
    public ParserBuilder withMu(final double newMu) {
        return new ParserBuilder(getConventions(), isSimpleEOP(), getDataContext(),
                                 getMissionReferenceDate(), newMu, getDefaultMass(),
                                 getDefaultInterpolationDegree());
    }

    /** Get the gravitational coefficient.
     * @return gravitational coefficient
     */
    public double getMu() {
        return mu;
    }

    /** Set up the default mass.
     * <p>
     * The default mass is used only by {@link OPMParser}.
     * </p>
     * @param newDefaultMass default mass
     * @return a new builder with updated configuration (the instance is not changed)
     */
    public ParserBuilder withDefaultMass(final double newDefaultMass) {
        return new ParserBuilder(getConventions(), isSimpleEOP(), getDataContext(),
                                 getMissionReferenceDate(), getMu(), newDefaultMass,
                                 getDefaultInterpolationDegree());
    }

    /** Get the default mass.
     * @return default mass
     */
    public double getDefaultMass() {
        return defaultMass;
    }

    /** Set up the default interpolation degree.
     * <p>
     * The default interpolation degree is used only by {@link AEMParser}
     * and {@link OEMParser}.
     * </p>
     * @param newDefaultInterpolationDegree default interpolation degree
     * @return a new builder with updated configuration (the instance is not changed)
     */
    public ParserBuilder withDefaultInterpolationDegree(final int newDefaultInterpolationDegree) {
        return new ParserBuilder(getConventions(), isSimpleEOP(), getDataContext(),
                                 getMissionReferenceDate(), getMu(), getDefaultMass(),
                                 newDefaultInterpolationDegree);
    }

    /** Get the default interpolation degree.
     * @return default interpolation degree
     */
    public int getDefaultInterpolationDegree() {
        return defaultInterpolationDegree;
    }

    /** Build a parser for {@link org.orekit.files.ccsds.ndm.tdm.TDMFile Tracking Data Messages}.
     * @return a new parser
     */
    public TDMParser buildTDMParser() {
        return new TDMParser(getConventions(), isSimpleEOP(), getDataContext());
    }

    /** Build a parser for {@link org.orekit.files.ccsds.ndm.odm.opm.OPMFile Orbit Parameters Messages}.
     * @return a new parser
     */
    public OPMParser buildOPMParser() {
        return new OPMParser(getConventions(), isSimpleEOP(), getDataContext(), getMissionReferenceDate(),
                             getMu(), getDefaultMass());
    }

    /** Build a parser for {@link org.orekit.files.ccsds.ndm.odm.opm.OMMFile Orbit Mean elements Messages}.
     * @return a new parser
     */
    public OMMParser buildOMMParser() {
        return new OMMParser(getConventions(), isSimpleEOP(), getDataContext(), getMissionReferenceDate(),
                             getMu(), getDefaultMass());
    }

    /** Build a parser for {@link org.orekit.files.ccsds.ndm.odm.oem.OEMFile Orbit Ephemeris Messages}.
     * @return a new parser
     */
    public OEMParser buildOEMParser() {
        return new OEMParser(getConventions(), isSimpleEOP(), getDataContext(), getMissionReferenceDate(),
                             getMu(), getDefaultInterpolationDegree());
    }

    /** Build a parser for {@link org.orekit.files.ccsds.ndm.odm.ocm.OCMFile Orbit Comprehensive Messages}.
     * @return a new parser
     */
    public OCMParser buildOCMParser() {
        return new OCMParser(getConventions(), isSimpleEOP(), getDataContext(), getMu());
    }

    /** Build a parser for {@link org.orekit.files.ccsds.ndm.adm.apm.APMFile Attitude Parameters Messages}.
     * @return a new parser
     */
    public APMParser buildAPMParser() {
        return new APMParser(getConventions(), isSimpleEOP(), getDataContext(), getMissionReferenceDate());
    }

    /** Build a parser for {@link org.orekit.files.ccsds.ndm.adm.aem.AEMFile Attitude Ephemeris Messages}.
     * @return a new parser
     */
    public AEMParser buildAEMParser() {
        return new AEMParser(getConventions(), isSimpleEOP(), getDataContext(), getMissionReferenceDate(),
                             getDefaultInterpolationDegree());
    }

}
