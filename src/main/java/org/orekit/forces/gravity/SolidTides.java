/* Copyright 2002-2026 CS GROUP
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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.util.MathArrays;
import org.orekit.bodies.CelestialBody;
import org.orekit.forces.ForceModel;
import org.orekit.forces.ForceModelModifier;
import org.orekit.forces.gravity.potential.CachedNormalizedSphericalHarmonicsProvider;
import org.orekit.forces.gravity.potential.NormalizedSphericalHarmonicsProvider;
import org.orekit.forces.gravity.potential.TideSystem;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeVectorFunction;
import org.orekit.time.UT1Scale;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.LoveNumbers;
import org.orekit.utils.OrekitConfiguration;

/** Solid tides force model.
 * @since 6.1
 * @author Luc Maisonobe
 * @author Rafael Ayala
 */
public class SolidTides implements ForceModelModifier {

    /**
     * Default step for tides field sampling (seconds).
     */
    public static final double DEFAULT_STEP = 600.0;

    /**
     * Default number of points tides field sampling.
     */
    public static final int DEFAULT_POINTS = 12;

    /**
     * Zero frequency-dependent corrections function for bodies without
     * frequency-dependent tidal data.
     *
     * @since 13.1.7
     */
    private static final TimeVectorFunction ZERO_FREQUENCY_FUNCTION = new TimeVectorFunction() {
        @Override
        public double[] value(final AbsoluteDate date) {
            return new double[5];
        }
        @Override
        public <T extends CalculusFieldElement<T>> T[] value(final FieldAbsoluteDate<T> date) {
            return MathArrays.buildArray(date.getField(), 5);
        }
    };

    /**
     * Underlying attraction model.
     */
    private final ForceModel attractionModel;

    /**
     * Private constructor with the force model only.
     * @param attractionModel underlying attraction model
     * @since 13.1.7
     */
    private SolidTides(final ForceModel attractionModel) {
        this.attractionModel = attractionModel;
    }

    /**
     * Simple constructor.
     * <p>
     * This constructor uses pole tides, the default {@link #DEFAULT_STEP step} and default
     * {@link #DEFAULT_POINTS number of points} for the tides field interpolation.
     * </p>
     *
     * @param centralBodyFrame  rotating body frame
     * @param ae                central body reference radius
     * @param mu                central body attraction coefficient
     * @param centralTideSystem tide system used in the central attraction model
     * @param conventions       IERS conventions used for loading Love numbers
     * @param ut1               UT1 time scale
     * @param bodies            tide generating bodies (typically Sun and Moon)
     * @see #DEFAULT_STEP
     * @see #DEFAULT_POINTS
     * @see #SolidTides(Frame, double, double, TideSystem, boolean, double, int, IERSConventions, UT1Scale, CelestialBody...)
     */
    public SolidTides(final Frame centralBodyFrame, final double ae, final double mu,
                      final TideSystem centralTideSystem,
                      final IERSConventions conventions, final UT1Scale ut1,
                      final CelestialBody... bodies) {
        this(centralBodyFrame, ae, mu, centralTideSystem, true,
                DEFAULT_STEP, DEFAULT_POINTS, conventions, ut1, bodies);
    }

    /**
     * Simple constructor.
     *
     * @param centralBodyFrame  rotating body frame
     * @param ae                central body reference radius
     * @param mu                central body attraction coefficient
     * @param centralTideSystem tide system used in the central attraction model
     * @param poleTide          if true, pole tide is computed
     * @param step              time step between sample points for interpolation
     * @param nbPoints          number of points to use for interpolation, if less than 2
     *                          then no interpolation is performed (thus greatly increasing computation cost)
     * @param conventions       IERS conventions used for loading Love numbers
     * @param ut1               UT1 time scale
     * @param bodies            tide generating bodies (typically Sun and Moon)
     */
    public SolidTides(final Frame centralBodyFrame, final double ae, final double mu,
                      final TideSystem centralTideSystem, final boolean poleTide,
                      final double step, final int nbPoints,
                      final IERSConventions conventions, final UT1Scale ut1,
                      final CelestialBody... bodies) {
        this(buildAttractionModel(centralBodyFrame,
                new SolidTidesField(conventions.getLoveNumbers(),
                        conventions.getTideFrequencyDependenceFunction(ut1, ut1.getEOPHistory().getTimeScales()),
                        conventions.getPermanentTide(),
                        poleTide ? conventions.getSolidPoleTide(ut1.getEOPHistory()) : null,
                        centralBodyFrame, ae, mu, centralTideSystem, bodies),
                step, nbPoints));
    }

    /**
     * Constructor with custom Love numbers for any central body.
     * This constructor allows using body-specific Love numbers (e.g. for the Moon)
     * instead of IERS Earth conventions. Note that frequency-dependent corrections and pole
     * tide are not applied, and only the frequency-independent tidal deformation
     * (IERS 2010 equations 6.6 and 6.7) is computed.
     *
     * @param centralBodyFrame  rotating body frame
     * @param ae                central body reference radius
     * @param mu                central body attraction coefficient
     * @param centralTideSystem tide system used in the central attraction model
     * @param loveNumbers       body-specific Love numbers
     * @param step              time step between sample points for interpolation
     * @param nbPoints          number of points to use for interpolation, if less than 2
     *                          then no interpolation is performed (thus greatly increasing computation cost)
     * @param bodies            tide generating bodies (typically Sun and Moon)
     * @since 13.1.7
     */
    public SolidTides(final Frame centralBodyFrame, final double ae, final double mu,
                      final TideSystem centralTideSystem,
                      final LoveNumbers loveNumbers,
                      final double step, final int nbPoints,
                      final CelestialBody... bodies) {
        this(buildAttractionModel(centralBodyFrame,
                new SolidTidesField(loveNumbers, ZERO_FREQUENCY_FUNCTION,
                        0.0, null,
                        centralBodyFrame, ae, mu, centralTideSystem, bodies),
                step, nbPoints));
    }

    /**
     * Constructor with custom Love numbers using default interpolation settings.
     * <p>
     * This constructor uses the default {@link #DEFAULT_STEP step} and default
     * {@link #DEFAULT_POINTS number of points} for the tides field interpolation.
     * </p>
     *
     * @param centralBodyFrame  rotating body frame
     * @param ae                central body reference radius
     * @param mu                central body attraction coefficient
     * @param centralTideSystem tide system used in the central attraction model
     * @param loveNumbers       body-specific Love numbers
     * @param bodies            tide generating bodies (typically Sun and Moon)
     * @see #DEFAULT_STEP
     * @see #DEFAULT_POINTS
     * @see #SolidTides(Frame, double, double, TideSystem, LoveNumbers, double, int, CelestialBody...)
     * @since 13.1.7
     */
    public SolidTides(final Frame centralBodyFrame, final double ae, final double mu,
                      final TideSystem centralTideSystem,
                      final LoveNumbers loveNumbers,
                      final CelestialBody... bodies) {
        this(centralBodyFrame, ae, mu, centralTideSystem, loveNumbers,
                DEFAULT_STEP, DEFAULT_POINTS, bodies);
    }

    /** Build the attraction model from a raw provider.
     * @param centralBodyFrame rotating body frame
     * @param rawProvider      raw spherical harmonics provider
     * @param step             time step between sample points for interpolation
     * @param nbPoints         number of points to use for interpolation
     * @return the attraction model
     */
    private static ForceModel buildAttractionModel(final Frame centralBodyFrame,
                                                   final NormalizedSphericalHarmonicsProvider rawProvider,
                                                   final double step, final int nbPoints) {
        final NormalizedSphericalHarmonicsProvider provider;
        if (nbPoints < 2) {
            provider = rawProvider;
        } else {
            provider =
                    new CachedNormalizedSphericalHarmonicsProvider(rawProvider, step, nbPoints,
                            OrekitConfiguration.getCacheSlotsNumber(),
                            7 * Constants.JULIAN_DAY,
                            0.5 * Constants.JULIAN_DAY);
        }
        return new HolmesFeatherstoneAttractionModel(centralBodyFrame, provider);
    }

    @Override
    public ForceModel getUnderlyingModel() {
        return attractionModel;
    }
}
