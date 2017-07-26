/* Copyright 2002-2017 CS Systèmes d'Information
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
package org.orekit.forces.gravity;

import java.util.List;
import java.util.stream.Stream;

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.errors.OrekitException;
import org.orekit.forces.AbstractForceModel;
import org.orekit.forces.ForceModel;
import org.orekit.forces.gravity.potential.CachedNormalizedSphericalHarmonicsProvider;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.NormalizedSphericalHarmonicsProvider;
import org.orekit.forces.gravity.potential.OceanTidesWave;
import org.orekit.frames.Frame;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.time.UT1Scale;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.OrekitConfiguration;
import org.orekit.utils.ParameterDriver;

/** Ocean tides force model.
 * @since 6.1
 * @author Luc Maisonobe
 */
public class OceanTides extends AbstractForceModel {

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
     * @param centralBodyFrame rotating body frame
     * @param ae central body reference radius
     * @param mu central body attraction coefficient
     * @param degree degree of the tide model to load
     * @param order order of the tide model to load
     * @param conventions IERS conventions used for loading ocean pole tide
     * @param ut1 UT1 time scale
     * @exception OrekitException if the ocean tides model cannot be read or the
     * model does not support requested degree or order
     * @see #DEFAULT_STEP
     * @see #DEFAULT_POINTS
     * @see #OceanTides(Frame, double, double, boolean, double, int, int, int, IERSConventions, UT1Scale)
     * @see GravityFieldFactory#getOceanTidesWaves(int, int)
     */
    public OceanTides(final Frame centralBodyFrame, final double ae, final double mu,
                      final int degree, final int order,
                      final IERSConventions conventions, final UT1Scale ut1)
        throws OrekitException {
        this(centralBodyFrame, ae, mu, true,
             DEFAULT_STEP, DEFAULT_POINTS, degree, order,
             conventions, ut1);
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
     * @exception OrekitException if the ocean tides model cannot be read or the
     * model does not support requested degree or order
     * @see GravityFieldFactory#getOceanTidesWaves(int, int)
     */
    public OceanTides(final Frame centralBodyFrame, final double ae, final double mu,
                      final boolean poleTide, final double step, final int nbPoints,
                      final int degree, final int order,
                      final IERSConventions conventions, final UT1Scale ut1)
        throws OrekitException {

        // load the ocean tides model
        final List<OceanTidesWave> waves = GravityFieldFactory.getOceanTidesWaves(degree, order);

        final OceanTidesField raw =
                new OceanTidesField(ae, mu, waves,
                                    conventions.getNutationArguments(ut1),
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
    public Vector3D acceleration(final SpacecraftState s, final double[] parameters)
        throws OrekitException {
        // delegate to underlying model
        return attractionModel.acceleration(s, parameters);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends RealFieldElement<T>> FieldVector3D<T> acceleration(final FieldSpacecraftState<T> s,
                                                                         final T[] parameters)
        throws OrekitException {
        // delegate to underlying model
        return attractionModel.acceleration(s, parameters);
    }


    /** {@inheritDoc} */
    @Override
    public Stream<EventDetector> getEventsDetectors() {
        // delegate to underlying attraction model
        return attractionModel.getEventsDetectors();
    }

    /** {@inheritDoc} */
    @Override
    public <T extends RealFieldElement<T>> Stream<FieldEventDetector<T>> getFieldEventsDetectors(final Field<T> field) {
        // delegate to underlying attraction model
        return attractionModel.getFieldEventsDetectors(field);
    }

    /** {@inheritDoc} */
    @Override
    public ParameterDriver[] getParametersDrivers() {
        // delegate to underlying attraction model
        return attractionModel.getParametersDrivers();
    }

}
