/* Copyright 2002-2024 CS GROUP
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
package org.orekit.propagation.conversion.osc2mean;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.DataContext;
import org.orekit.frames.Frame;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.propagation.analytical.tle.FieldTLE;
import org.orekit.propagation.analytical.tle.FieldTLEPropagator;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.propagation.analytical.tle.TLEConstants;
import org.orekit.propagation.analytical.tle.TLEPropagator;
import org.orekit.propagation.analytical.tle.generation.TleGenerationUtil;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScale;

/**
 * TLE, i.e. SGP4/SDP4, theory for osculating to mean orbit conversion.
 *
 * @author Pascal Parraud
 * @since 13.0
 */
public class TLETheory implements MeanTheory {

    /** Theory used for converting from osculating to mean orbit. */
    private static final String THEORY = "TLE";

    /** First line of arbitrary TLE. Should not impact conversion. */
    private static final String TMP_L1 = "1 00000U 00000A   00001.00000000  .00000000  00000+0  00000+0 0    02";
    /** Second line of arbitrary TLE. Should not impact conversion. */
    private static final String TMP_L2 = "2 00000   0.0000   0.0000 0000000   0.0000   0.0000  0.00000000    02";

    /** Template TLE. */
    private final TLE tmpTle;

    /** UTC scale. */
    private final TimeScale utc;

    /** TEME frame. */
    private final Frame teme;

    /**
     * Constructor.
     */
    @DefaultDataContext
    public TLETheory() {
        this(DataContext.getDefault().getTimeScales().getUTC(),
             DataContext.getDefault().getFrames().getTEME());
    }

    /**
     * Constructor.
     * @param template template TLE
     */
    @DefaultDataContext
    public TLETheory(final TLE template) {
        this(template,
             DataContext.getDefault().getTimeScales().getUTC(),
             DataContext.getDefault().getFrames().getTEME());
    }

    /**
     * Constructor.
     * @param utc  UTC time scale
     * @param teme TEME frame
     */
    public TLETheory(final TimeScale utc,
                     final Frame teme) {
        this.tmpTle = new TLE(TMP_L1, TMP_L2, utc);
        this.utc    = utc;
        this.teme   = teme;
    }

    /**
     * Constructor.
     * @param template template TLE
     * @param utc      UTC time scale
     * @param teme     TEME frame
     */
    public TLETheory(final TLE template,
                     final TimeScale utc,
                     final Frame teme) {
        this.tmpTle = template;
        this.utc    = utc;
        this.teme   = teme;
    }

    /** {@inheritDoc} */
    @Override
    public String getTheoryName() {
        return THEORY;
    }

    /** {@inheritDoc} */
    @Override
    public double getReferenceRadius() {
        return 1000 * TLEConstants.EARTH_RADIUS;
    };

    /** Pre-treatment of the osculating orbit to be converted.
     * <p>The osculating orbit is transformed to TEME frame.</p>
     */
    @Override
    public Orbit preprocessing(final Orbit osculating) {
        return new KeplerianOrbit(osculating.getPVCoordinates(teme), teme, TLEPropagator.getMU());
    }

    /** {@inheritDoc} */
    @Override
    public Orbit meanToOsculating(final Orbit mean) {
        // Build TLE from mean and template
        final KeplerianOrbit meanKepl = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(mean);
        final TLE meanTle = TleGenerationUtil.newTLE(meanKepl, tmpTle, tmpTle.getBStar(mean.getDate()), utc);
        final TLEPropagator propagator = TLEPropagator.selectExtrapolator(meanTle, teme);
        return propagator.getInitialState().getOrbit();
    }

    /** Post-treatment of the converted mean orbit.
     * <p>The mean orbit returned is a Keplerian orbit in TEME frame.</p>
     */
    @Override
    public Orbit postprocessing(final Orbit osculating, final Orbit mean) {
        return OrbitType.KEPLERIAN.convertType(mean);
    }

    /** Pre-treatment of the osculating orbit to be converted.
     * <p>The osculating orbit is transformed to TEME frame.</p>
     */
    @Override
    public <T extends CalculusFieldElement<T>> FieldOrbit<T> preprocessing(final FieldOrbit<T> osculating) {
        final T mu = osculating.getDate().getField().getZero().newInstance(TLEConstants.MU);
        return new FieldKeplerianOrbit<T>(osculating.getPVCoordinates(teme), teme, mu);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldOrbit<T> meanToOsculating(final FieldOrbit<T> mean) {
        final FieldAbsoluteDate<T> date = mean.getDate();
        final Field<T> field = date.getField();
        final FieldTLE<T> fieldTmpTle = new FieldTLE<T>(field, tmpTle.getLine1(), tmpTle.getLine2(), utc);
        final T bStar = field.getZero().newInstance(fieldTmpTle.getBStar());
        // Build TLE from mean and template
        final FieldKeplerianOrbit<T> meanKepl = (FieldKeplerianOrbit<T>) OrbitType.KEPLERIAN.convertType(mean);
        final FieldTLE<T> meanTle = TleGenerationUtil.newTLE(meanKepl, fieldTmpTle, bStar, utc);
        final FieldTLEPropagator<T> propagator = FieldTLEPropagator.selectExtrapolator(meanTle, teme, meanTle.getParameters(field));
        return propagator.getInitialState().getOrbit();
    }

    /** Post-treatment of the converted mean orbit.
     * <p>The mean orbit returned is a Keplerian orbit in TEME frame.</p>
     */
    @Override
    public <T extends CalculusFieldElement<T>> FieldOrbit<T> postprocessing(final FieldOrbit<T> osculating,
                                                                            final FieldOrbit<T> mean) {
        return OrbitType.KEPLERIAN.convertType(mean);
    }

}
