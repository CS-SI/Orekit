/* Copyright 2022-2025 Thales Alenia Space
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
package org.orekit.propagation.analytical.tle;

import org.hipparchus.util.FastMath;
import org.orekit.frames.Frame;
import org.orekit.orbits.AbstractOrbitalParameterFactory;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList;

import java.util.List;

/**
 * Factory for TLE orbital elements.
 * @author Luc Maisonobe
 * @since 14.0
 */
public class TleParametersFactory extends AbstractOrbitalParameterFactory<TLE> {

    /** Name for mean motion. */
    public static final String MEAN_MOTION = "TleMeanMotion";

    /** Name for eccentricity. */
    public static final String ECCENTRICITY   = "TleEccentricity";

    /** Name for inclination. */
    public static final String INCLINATION   = "TleInclination";

    /** Name for perigee argument. */
    public static final String PERIGEE_ARGUMENT = "TlePerigeeArgument";

    /** Name for right ascension of ascending node. */
    public static final String RAAN    = "TleRighAscensionAscendingNode";

    /** Name for mean anomaly. */
    public static final String MEAN_ANOM = "TleMeanAnomaly";

    /** Template TLE. */
    private final TLE templateTLE;

    /** Default constructor.
     * @param templateTLE template TLE
     * @param teme teme frame
     */
    public TleParametersFactory(final TLE templateTLE,  final Frame teme) {
        super(createDrivers(templateTLE), teme, PositionAngleType.MEAN,
              templateTLE.getDate(), TLEConstants.MU);
        this.templateTLE = templateTLE;
    }

    /** Create parameter drivers.
     * @param tle reference TLE
     * @return drivers
     */
    private static ParameterDriversList createDrivers(final TLE tle) {
        final ParameterDriversList drivers = new ParameterDriversList();
        drivers.add(new ParameterDriver(MEAN_MOTION, tle.getMeanMotion(),
                                        FastMath.scalb(1.0, -32),
                                        0, Double.POSITIVE_INFINITY));
        drivers.add(new ParameterDriver(ECCENTRICITY, tle.getE(),
                                        FastMath.scalb(1.0, -22),
                                        0.0, 1.0));
        drivers.add(new ParameterDriver(INCLINATION, tle.getI(),
                                        FastMath.scalb(1.0, -22),
                                        0, FastMath.PI));
        drivers.add(new ParameterDriver(PERIGEE_ARGUMENT, tle.getPerigeeArgument(),
                                        FastMath.scalb(1.0, -22),
                                        Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY));
        drivers.add(new ParameterDriver(RAAN, tle.getRaan(),
                                        FastMath.scalb(1.0, -22),
                                        Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY));
        drivers.add(new ParameterDriver(MEAN_ANOM, tle.getMeanAnomaly(),
                                        FastMath.scalb(1.0, -22),
                                        Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY));
        return drivers;
    }

    /** {@inheritDoc} */
    @Override
    protected double[] toArray(final Orbit orbit) {

        // retrieve Keplerian orbital parameters
        final double[] stateVector = super.toArray(orbit);

        // TLE uses mean motion as first parameter, not semi major axis as Keplerian orbit
        stateVector[0] = orbit.getKeplerianMeanMotion();

        return stateVector;

    }

    /** {@inheritDoc} */
    @Override
    public TLE createFromDrivers() {
        final List<ParameterDriversList.DelegatingDriver> drivers = getOrbitalParametersDrivers().getDrivers();
        return new TLE(templateTLE.getSatelliteNumber(), templateTLE.getClassification(),
                       templateTLE.getLaunchYear(), templateTLE.getLaunchNumber(), templateTLE.getLaunchPiece(),
                       templateTLE.getEphemerisType(), templateTLE.getElementNumber(),
                       getDate(),
                       drivers.get(0).getValue(),
                       templateTLE.getMeanMotionFirstDerivative(), templateTLE.getMeanMotionSecondDerivative(),
                       drivers.get(1).getValue(),
                       drivers.get(2).getValue(),
                       drivers.get(3).getValue(),
                       drivers.get(4).getValue(),
                       drivers.get(5).getValue(),
                       templateTLE.getRevolutionNumberAtEpoch(), templateTLE.getBStar(), templateTLE.getUtc());
    }

}
