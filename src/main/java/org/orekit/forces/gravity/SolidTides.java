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

import java.util.stream.Stream;

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.bodies.CelestialBody;
import org.orekit.errors.OrekitException;
import org.orekit.forces.AbstractForceModel;
import org.orekit.forces.ForceModel;
import org.orekit.forces.gravity.potential.CachedNormalizedSphericalHarmonicsProvider;
import org.orekit.forces.gravity.potential.NormalizedSphericalHarmonicsProvider;
import org.orekit.forces.gravity.potential.TideSystem;
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

/** Solid tides force model.
 * @since 6.1
 * @author Luc Maisonobe
 */
public class SolidTides extends AbstractForceModel {

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
     * @param centralTideSystem tide system used in the central attraction model
     * @param conventions IERS conventions used for loading Love numbers
     * @param ut1 UT1 time scale
     * @param bodies tide generating bodies (typically Sun and Moon)
     * @exception OrekitException if the Love numbers embedded in the
     * library cannot be read
     * @see #DEFAULT_STEP
     * @see #DEFAULT_POINTS
     * @see #SolidTides(Frame, double, double, TideSystem, boolean, double, int, IERSConventions, UT1Scale, CelestialBody...)
     */
    public SolidTides(final Frame centralBodyFrame, final double ae, final double mu,
                      final TideSystem centralTideSystem,
                      final IERSConventions conventions, final UT1Scale ut1,
                      final CelestialBody... bodies)
        throws OrekitException {
        this(centralBodyFrame, ae, mu, centralTideSystem, true,
             DEFAULT_STEP, DEFAULT_POINTS, conventions, ut1, bodies);
    }

    /** Simple constructor.
     * @param centralBodyFrame rotating body frame
     * @param ae central body reference radius
     * @param mu central body attraction coefficient
     * @param centralTideSystem tide system used in the central attraction model
     * @param poleTide if true, pole tide is computed
     * @param step time step between sample points for interpolation
     * @param nbPoints number of points to use for interpolation, if less than 2
     * then no interpolation is performed (thus greatly increasing computation cost)
     * @param conventions IERS conventions used for loading Love numbers
     * @param ut1 UT1 time scale
     * @param bodies tide generating bodies (typically Sun and Moon)
     * @exception OrekitException if the Love numbers embedded in the
     * library cannot be read
     */
    public SolidTides(final Frame centralBodyFrame, final double ae, final double mu,
                      final TideSystem centralTideSystem, final boolean poleTide,
                      final double step, final int nbPoints,
                      final IERSConventions conventions, final UT1Scale ut1,
                      final CelestialBody... bodies)
        throws OrekitException {
        final SolidTidesField raw =
                new SolidTidesField(conventions.getLoveNumbers(),
                                    conventions.getTideFrequencyDependenceFunction(ut1),
                                    conventions.getPermanentTide(),
                                    poleTide ? conventions.getSolidPoleTide(ut1.getEOPHistory()) : null,
                                             centralBodyFrame, ae, mu, centralTideSystem, bodies);
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
        // delegate to underlying attraction model
        return attractionModel.acceleration(s, parameters);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends RealFieldElement<T>> FieldVector3D<T> acceleration(final FieldSpacecraftState<T> s,
                                                                         final T[] parameters)
        throws OrekitException {
        // delegate to underlying attraction model
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
