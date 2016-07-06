/* Copyright 2002-2016 CS Systèmes d'Information
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
package org.orekit.propagation.conversion;

import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitInternalError;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.propagation.analytical.tle.TLEPropagator;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterObserver;

/** Builder for TLEPropagator.
 * @author Pascal Parraud
 * @since 6.0
 */
public class TLEPropagatorBuilder extends AbstractPropagatorBuilder {

    /** Parameter name for B* coefficient. */
    public static final String B_STAR = "BSTAR";

    /** B* scaling factor.
     * <p>
     * We use a power of 2 to avoid numeric noise introduction
     * in the multiplications/divisions sequences.
     * </p>
     */
    private static final double B_STAR_SCALE = FastMath.scalb(1.0, -20);

    /** Satellite number. */
    private final int satelliteNumber;

    /** Classification (U for unclassified). */
    private final char classification;

    /** Launch year (all digits). */
    private final int launchYear;

    /** Launch number. */
    private final int launchNumber;

    /** Launch piece. */
    private final String launchPiece;

    /** Element number. */
    private final int elementNumber;

    /** Revolution number at epoch. */
    private final int revolutionNumberAtEpoch;

    /** Ballistic coefficient. */
    private double bStar;

    /** Build a new instance.
     * <p>
     * The template TLE is used as a model to {@link
     * #createInitialOrbit() create initial orbit}. It defines the
     * inertial frame, the central attraction coefficient, orbit type, satellite number,
     * classification, .... and is also used together with the {@code positionScale} to
     * convert from the {@link ParameterDriver#setNormalizedValue(double) normalized}
     * parameters used by the callers of this builder to the real orbital parameters.
     * </p>
     * @param templateTLE reference TLE from which real orbits will be built
     * @param positionAngle position angle type to use
     * @param positionScale scaling factor used for orbital parameters normalization
     * (typically set to the expected standard deviation of the position)
     * @throws OrekitException if the TEME frame cannot be set
     * @since 7.1
     */
    public TLEPropagatorBuilder(final TLE templateTLE, final PositionAngle positionAngle,
                                final double positionScale)
        throws OrekitException {
        super(TLEPropagator.selectExtrapolator(templateTLE).getInitialState().getOrbit(),
              positionAngle, positionScale);
        this.satelliteNumber         = templateTLE.getSatelliteNumber();
        this.classification          = templateTLE.getClassification();
        this.launchYear              = templateTLE.getLaunchYear();
        this.launchNumber            = templateTLE.getLaunchNumber();
        this.launchPiece             = templateTLE.getLaunchPiece();
        this.elementNumber           = templateTLE.getElementNumber();
        this.revolutionNumberAtEpoch = templateTLE.getRevolutionNumberAtEpoch();
        this.bStar                   = 0.0;
        try {
            final ParameterDriver driver = new ParameterDriver(B_STAR, bStar, B_STAR_SCALE,
                                                               Double.NEGATIVE_INFINITY,
                                                               Double.POSITIVE_INFINITY);
            driver.addObserver(new ParameterObserver() {
                /** {@inheritDoc} */
                @Override
                public void valueChanged(final double previousValue, final ParameterDriver driver) {
                    TLEPropagatorBuilder.this.bStar = driver.getValue();
                }
            });
            addSupportedParameter(driver);
        } catch (OrekitException oe) {
            // this should never happen
            throw new OrekitInternalError(oe);
        }

    }

    /** {@inheritDoc} */
    public Propagator buildPropagator(final double[] normalizedParameters)
        throws OrekitException {

        // create the orbit
        setParameters(normalizedParameters);
        final Orbit orbit = createInitialOrbit();

        // we really need a Keplerian orbit type
        final KeplerianOrbit kep = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(orbit);

        final TLE tle = new TLE(satelliteNumber, classification, launchYear, launchNumber, launchPiece,
                                TLE.DEFAULT, elementNumber, orbit.getDate(),
                                kep.getKeplerianMeanMotion(), 0.0, 0.0,
                                kep.getE(), MathUtils.normalizeAngle(orbit.getI(), FastMath.PI),
                                MathUtils.normalizeAngle(kep.getPerigeeArgument(), FastMath.PI),
                                MathUtils.normalizeAngle(kep.getRightAscensionOfAscendingNode(), FastMath.PI),
                                MathUtils.normalizeAngle(kep.getMeanAnomaly(), FastMath.PI),
                                revolutionNumberAtEpoch, bStar);

        return TLEPropagator.selectExtrapolator(tle);
    }

}
