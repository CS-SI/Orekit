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
package org.orekit.propagation.analytical.tle.generation;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.propagation.analytical.tle.FieldTLE;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScale;

/**
 * Utility class for TLE generation algorithm.
 * @author Bryan Cazabonne
 * @author Thomas Paulet
 * @author Mark Rutten
 */
public final class TleGenerationUtil {

    /** Private constructor.
     * <p>This class is a utility class, it should neither have a public
     * nor a default constructor. This private constructor prevents
     * the compiler from generating one automatically.</p>
     */
    private TleGenerationUtil() {
    }

    /**
     * Builds a new TLE from Keplerian parameters and a template for TLE data.
     * @param keplerianOrbit the Keplerian parameters to build the TLE from
     * @param templateTLE TLE used to get object identification
     * @param bStar TLE B* parameter
     * @param utc UTC scale
     * @return TLE with template identification and new orbital parameters
     */
    public static TLE newTLE(final KeplerianOrbit keplerianOrbit, final TLE templateTLE,
                             final double bStar, final TimeScale utc) {

        // Keplerian parameters
        final double meanMotion  = keplerianOrbit.getKeplerianMeanMotion();
        final double e           = keplerianOrbit.getE();
        final double i           = keplerianOrbit.getI();
        final double raan        = keplerianOrbit.getRightAscensionOfAscendingNode();
        final double pa          = keplerianOrbit.getPerigeeArgument();
        final double meanAnomaly = keplerianOrbit.getMeanAnomaly();

        // TLE epoch is state epoch
        final AbsoluteDate epoch = keplerianOrbit.getDate();

        // Identification
        final int satelliteNumber = templateTLE.getSatelliteNumber();
        final char classification = templateTLE.getClassification();
        final int launchYear      = templateTLE.getLaunchYear();
        final int launchNumber    = templateTLE.getLaunchNumber();
        final String launchPiece  = templateTLE.getLaunchPiece();
        final int ephemerisType   = templateTLE.getEphemerisType();
        final int elementNumber   = templateTLE.getElementNumber();

        // Updates revolutionNumberAtEpoch
        final int revolutionNumberAtEpoch = templateTLE.getRevolutionNumberAtEpoch();
        final double dt = epoch.durationFrom(templateTLE.getDate());
        final int newRevolutionNumberAtEpoch = (int) (revolutionNumberAtEpoch + FastMath.floor((MathUtils.normalizeAngle(meanAnomaly, FastMath.PI) + dt * meanMotion) / (MathUtils.TWO_PI)));

        // Gets Mean Motion derivatives
        final double meanMotionFirstDerivative  = templateTLE.getMeanMotionFirstDerivative();
        final double meanMotionSecondDerivative = templateTLE.getMeanMotionSecondDerivative();

        // Returns the new TLE
        return new TLE(satelliteNumber, classification, launchYear, launchNumber, launchPiece, ephemerisType,
                       elementNumber, epoch, meanMotion, meanMotionFirstDerivative, meanMotionSecondDerivative,
                       e, i, pa, raan, meanAnomaly, newRevolutionNumberAtEpoch, bStar, utc);

    }

    /**
     * Builds a new TLE from Keplerian parameters and a template for TLE data.
     * @param keplerianOrbit the Keplerian parameters to build the TLE from
     * @param templateTLE TLE used to get object identification
     * @param bStar TLE B* parameter
     * @param utc UTC scale
     * @param <T> type of the element
     * @return TLE with template identification and new orbital parameters
     */
    public static <T extends CalculusFieldElement<T>> FieldTLE<T> newTLE(final FieldKeplerianOrbit<T> keplerianOrbit,
                                                                         final FieldTLE<T> templateTLE, final T bStar,
                                                                         final TimeScale utc) {

        // Keplerian parameters
        final T meanMotion  = keplerianOrbit.getKeplerianMeanMotion();
        final T e           = keplerianOrbit.getE();
        final T i           = keplerianOrbit.getI();
        final T raan        = keplerianOrbit.getRightAscensionOfAscendingNode();
        final T pa          = keplerianOrbit.getPerigeeArgument();
        final T meanAnomaly = keplerianOrbit.getMeanAnomaly();

        // TLE epoch is state epoch
        final FieldAbsoluteDate<T> epoch = keplerianOrbit.getDate();

        // Identification
        final int satelliteNumber = templateTLE.getSatelliteNumber();
        final char classification = templateTLE.getClassification();
        final int launchYear      = templateTLE.getLaunchYear();
        final int launchNumber    = templateTLE.getLaunchNumber();
        final String launchPiece  = templateTLE.getLaunchPiece();
        final int ephemerisType   = templateTLE.getEphemerisType();
        final int elementNumber   = templateTLE.getElementNumber();

        // Updates revolutionNumberAtEpoch
        final int revolutionNumberAtEpoch = templateTLE.getRevolutionNumberAtEpoch();
        final T dt = epoch.durationFrom(templateTLE.getDate());
        final int newRevolutionNumberAtEpoch = (int) ((int) revolutionNumberAtEpoch + FastMath.floor(MathUtils.normalizeAngle(meanAnomaly, e.getPi()).add(dt.multiply(meanMotion)).divide(MathUtils.TWO_PI)).getReal());

        // Gets Mean Motion derivatives
        final T meanMotionFirstDerivative  = templateTLE.getMeanMotionFirstDerivative();
        final T meanMotionSecondDerivative = templateTLE.getMeanMotionSecondDerivative();

        // Returns the new TLE
        return new FieldTLE<>(satelliteNumber, classification, launchYear, launchNumber, launchPiece, ephemerisType,
                              elementNumber, epoch, meanMotion, meanMotionFirstDerivative, meanMotionSecondDerivative,
                              e, i, pa, raan, meanAnomaly, newRevolutionNumberAtEpoch, bStar.getReal(), utc);

    }

}
