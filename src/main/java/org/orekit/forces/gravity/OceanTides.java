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
package org.orekit.forces.gravity;

import java.util.List;
import java.util.stream.Stream;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.DataContext;
import org.orekit.forces.ForceModel;
import org.orekit.forces.gravity.potential.CachedNormalizedSphericalHarmonicsProvider;
import org.orekit.forces.gravity.potential.GravityFields;
import org.orekit.forces.gravity.potential.NormalizedSphericalHarmonicsProvider;
import org.orekit.forces.gravity.potential.OceanTidesWave;
import org.orekit.frames.Frame;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.time.TimeScales;
import org.orekit.time.UT1Scale;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.OrekitConfiguration;
import org.orekit.utils.ParameterDriver;

/** Ocean tides force model.
 * @since 6.1
 * @author Luc Maisonobe
 */
public class OceanTides implements ForceModel {

    /** Default step for tides field sampling (seconds). */
    public static final double DEFAULT_STEP = 600.0;

    /** Default number of points tides field sampling. */
    public static final int DEFAULT_POINTS = 12;

    /** Underlying attraction model. */
    private final ForceModel attractionModel;

    /** Simple constructor.
     * <p>
     * This constructor uses pole tides, the default {@link #DEFAULT_STEP step} and default
     * {@link #DEFAULT_POINTS number of points} for the tides field interpolation.
     * </p>
     *
     * <p>This constructor uses the {@link DataContext#getDefault() default data context}.
     *
     * @param centralBodyFrame rotating body frame
     * @param ae central body reference radius
     * @param mu central body attraction coefficient
     * @param degree degree of the tide model to load
     * @param order order of the tide model to load
     * @param conventions IERS conventions used for loading ocean pole tide
     * @param ut1 UT1 time scale
     * @see #DEFAULT_STEP
     * @see #DEFAULT_POINTS
     * @see #OceanTides(Frame, double, double, boolean, double, int, int, int, IERSConventions, UT1Scale)
     * @see GravityFields#getOceanTidesWaves(int, int)
     * @see #OceanTides(Frame, double, double, boolean, double, int, int, int,
     * IERSConventions, UT1Scale, GravityFields)
     */
    @DefaultDataContext
    public OceanTides(final Frame centralBodyFrame, final double ae, final double mu,
                      final int degree, final int order,
                      final IERSConventions conventions, final UT1Scale ut1) {
        this(centralBodyFrame, ae, mu, true,
             DEFAULT_STEP, DEFAULT_POINTS, degree, order,
             conventions, ut1);
    }

    /** Simple constructor.
     *
     * <p>This constructor uses the {@link DataContext#getDefault() default data context}.
     *
     * @param centralBodyFrame rotating body frame
     * @param ae central body reference radius
     * @param mu central body attraction coefficient
     * @param poleTide if true, pole tide is computed
     * @param step time step between sample points for interpolation
     * @param nbPoints number of points to use for interpolation, if less than 2
     * then no interpolation is performed (thus greatly increasing computation cost)
     * @param degree degree of the tide model to load
     * @param order order of the tide model to load
     * @param conventions IERS conventions used for loading ocean pole tide
     * @param ut1 UT1 time scale
     * @see GravityFields#getOceanTidesWaves(int, int)
     * @see #OceanTides(Frame, double, double, boolean, double, int, int, int,
     * IERSConventions, UT1Scale, GravityFields)
     */
    @DefaultDataContext
    public OceanTides(final Frame centralBodyFrame, final double ae, final double mu,
                      final boolean poleTide, final double step, final int nbPoints,
                      final int degree, final int order,
                      final IERSConventions conventions, final UT1Scale ut1) {
        this(centralBodyFrame, ae, mu, poleTide, step, nbPoints, degree, order,
                conventions, ut1, DataContext.getDefault().getGravityFields());
    }

    /** Simple constructor.
     * @param centralBodyFrame rotating body frame
     * @param ae central body reference radius
     * @param mu central body attraction coefficient
     * @param poleTide if true, pole tide is computed
     * @param step time step between sample points for interpolation
     * @param nbPoints number of points to use for interpolation, if less than 2
     * then no interpolation is performed (thus greatly increasing computation cost)
     * @param degree degree of the tide model to load
     * @param order order of the tide model to load
     * @param conventions IERS conventions used for loading ocean pole tide
     * @param ut1 UT1 time scale
     * @param gravityFields used to compute ocean tides.
     * @see GravityFields#getOceanTidesWaves(int, int)
     * @since 10.1
     */
    public OceanTides(final Frame centralBodyFrame, final double ae, final double mu,
                      final boolean poleTide, final double step, final int nbPoints,
                      final int degree, final int order,
                      final IERSConventions conventions, final UT1Scale ut1,
                      final GravityFields gravityFields) {

        // load the ocean tides model
        final List<OceanTidesWave> waves = gravityFields.getOceanTidesWaves(degree, order);

        final TimeScales timeScales = ut1.getEOPHistory().getTimeScales();
        final OceanTidesField raw =
                new OceanTidesField(ae, mu, waves,
                                    conventions.getNutationArguments(ut1, timeScales),
                                    poleTide ? conventions.getOceanPoleTide(ut1.getEOPHistory()) : null);

        final NormalizedSphericalHarmonicsProvider provider;
        if (nbPoints < 2) {
            provider = raw;
        } else {
            provider =
                new CachedNormalizedSphericalHarmonicsProvider(raw, step, nbPoints,
                                                               OrekitConfiguration.getCacheSlotsNumber(),
                                                               7 * Constants.JULIAN_DAY,
                                                               0.5 * Constants.JULIAN_DAY);
        }

        attractionModel = new HolmesFeatherstoneAttractionModel(centralBodyFrame, provider);

    }

    /** {@inheritDoc} */
    @Override
    public boolean dependsOnPositionOnly() {
        return attractionModel.dependsOnPositionOnly();
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D acceleration(final SpacecraftState s, final double[] parameters) {
        // delegate to underlying model
        return attractionModel.acceleration(s, parameters);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldVector3D<T> acceleration(final FieldSpacecraftState<T> s,
                                                                         final T[] parameters) {
        // delegate to underlying model
        return attractionModel.acceleration(s, parameters);
    }


    /** {@inheritDoc} */
    @Override
    public Stream<EventDetector> getEventDetectors() {
        // delegate to underlying attraction model
        return attractionModel.getEventDetectors();
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> Stream<FieldEventDetector<T>> getFieldEventDetectors(final Field<T> field) {
        // delegate to underlying attraction model
        return attractionModel.getFieldEventDetectors(field);
    }

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        // delegate to underlying attraction model
        return attractionModel.getParametersDrivers();
    }

}
