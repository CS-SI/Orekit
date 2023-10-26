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
package org.orekit.models.earth.ionosphere;

import java.util.Collections;
import java.util.List;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.FieldSinCos;
import org.hipparchus.util.MathUtils;
import org.hipparchus.util.SinCos;
import org.orekit.bodies.FieldGeodeticPoint;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.frames.TopocentricFrame;
import org.orekit.gnss.metric.messages.ssr.subtype.SsrIm201;
import org.orekit.gnss.metric.messages.ssr.subtype.SsrIm201Data;
import org.orekit.gnss.metric.messages.ssr.subtype.SsrIm201Header;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.utils.Constants;
import org.orekit.utils.FieldLegendrePolynomials;
import org.orekit.utils.LegendrePolynomials;
import org.orekit.utils.ParameterDriver;

/**
 * Ionospheric model based on SSR IM201 message.
 * <p>
 * Within this message, the ionospheric VTEC is provided
 * using spherical harmonic expansions. For a given ionospheric
 * layer, the slant TEC value is calculated using the satellite
 * elevation and the height of the corresponding layer. The
 * total slant TEC is computed by the sum of the individual slant
 * TEC for each layer.
 * </p>
 * @author Bryan Cazabonne
 * @since 11.0
 * @see "IGS State Space Representation (SSR) Format, Version 1.00, October 2020."
 */
public class SsrVtecIonosphericModel implements IonosphericModel {

    /** Earth radius in meters (see reference). */
    private static final double EARTH_RADIUS = 6370000.0;

    /** Multiplication factor for path delay computation. */
    private static final double FACTOR = 40.3e16;

    /** SSR Ionosphere VTEC Spherical Harmonics Message.. */
    private final transient SsrIm201 vtecMessage;

    /**
     * Constructor.
     * @param vtecMessage SSR Ionosphere VTEC Spherical Harmonics Message.
     */
    public SsrVtecIonosphericModel(final SsrIm201 vtecMessage) {
        this.vtecMessage = vtecMessage;
    }

    /** {@inheritDoc} */
    @Override
    public double pathDelay(final SpacecraftState state, final TopocentricFrame baseFrame,
                            final double frequency, final double[] parameters) {

        // Elevation in radians
        final Vector3D position  = state.getPosition(baseFrame);
        final double   elevation = position.getDelta();

        // Only consider measures above the horizon
        if (elevation > 0.0) {

            // Azimuth angle in radians
            double azimuth = FastMath.atan2(position.getX(), position.getY());
            if (azimuth < 0.) {
                azimuth += MathUtils.TWO_PI;
            }

            // Initialize slant TEC
            double stec = 0.0;

            // Message header
            final SsrIm201Header header = vtecMessage.getHeader();

            // Loop on ionospheric layers
            for (final SsrIm201Data data : vtecMessage.getData()) {
                stec += stecIonosphericLayer(data, header, elevation, azimuth, baseFrame.getPoint());
            }

            // Return the path delay
            return FACTOR * stec / (frequency * frequency);

        }

        // Delay is equal to 0.0
        return 0.0;

    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> T pathDelay(final FieldSpacecraftState<T> state, final TopocentricFrame baseFrame,
                                                       final double frequency, final T[] parameters) {

        // Field
        final Field<T> field = state.getDate().getField();

        // Elevation in radians
        final FieldVector3D<T> position  = state.getPosition(baseFrame);
        final T                elevation = position.getDelta();

        // Only consider measures above the horizon
        if (elevation.getReal() > 0.0) {

            // Azimuth angle in radians
            T azimuth = FastMath.atan2(position.getX(), position.getY());
            if (azimuth.getReal() < 0.) {
                azimuth = azimuth.add(MathUtils.TWO_PI);
            }

            // Initialize slant TEC
            T stec = field.getZero();

            // Message header
            final SsrIm201Header header = vtecMessage.getHeader();

            // Loop on ionospheric layers
            for (SsrIm201Data data : vtecMessage.getData()) {
                stec = stec.add(stecIonosphericLayer(data, header, elevation, azimuth, baseFrame.getPoint(field)));
            }

            // Return the path delay
            return stec.multiply(FACTOR).divide(frequency * frequency);

        }

        // Delay is equal to 0.0
        return field.getZero();

    }

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return Collections.emptyList();
    }

    /**
     * Calculates the slant TEC for a given ionospheric layer.
     * @param im201Data ionospheric data for the current layer
     * @param im201Header container for data contained in the header
     * @param elevation satellite elevation angle [rad]
     * @param azimuth satellite azimuth angle [rad]
     * @param point geodetic point
     * @return the slant TEC for the current ionospheric layer
     */
    private static double stecIonosphericLayer(final SsrIm201Data im201Data, final SsrIm201Header im201Header,
                                               final double elevation, final double azimuth,
                                               final GeodeticPoint point) {

        // Geodetic point data
        final double phiR    = point.getLatitude();
        final double lambdaR = point.getLongitude();
        final double hR      = point.getAltitude();

        // Data contained in the message
        final double     hI     = im201Data.getHeightIonosphericLayer();
        final int        degree = im201Data.getSphericalHarmonicsDegree();
        final int        order  = im201Data.getSphericalHarmonicsOrder();
        final double[][] cnm    = im201Data.getCnm();
        final double[][] snm    = im201Data.getSnm();

        // Spherical Earth's central angle
        final double psiPP = calculatePsi(hR, hI, elevation);

        // Sine and cosine of useful angles
        final SinCos scA    = FastMath.sinCos(azimuth);
        final SinCos scPhiR = FastMath.sinCos(phiR);
        final SinCos scPsi  = FastMath.sinCos(psiPP);

        // Pierce point latitude and longitude
        final double phiPP    = calculatePiercePointLatitude(scPhiR, scPsi, scA);
        final double lambdaPP = calculatePiercePointLongitude(scA, phiPP, psiPP, phiR, lambdaR);

        // Mean sun fixed longitude (modulo 2pi)
        final double lambdaS = calculateSunLongitude(im201Header, lambdaPP);

        // VTEC
        // According to the documentation, negative VTEC values must be ignored and shall be replaced by 0.0
        final double vtec = FastMath.max(0.0, calculateVTEC(degree, order, cnm, snm, phiPP, lambdaS));

        // Return STEC for the current ionospheric layer
        return vtec / FastMath.sin(elevation + psiPP);

    }

    /**
     * Calculates the slant TEC for a given ionospheric layer.
     * @param im201Data ionospheric data for the current layer
     * @param im201Header container for data contained in the header
     * @param elevation satellite elevation angle [rad]
     * @param azimuth satellite azimuth angle [rad]
     * @param point geodetic point
     * @param <T> type of the elements
     * @return the slant TEC for the current ionospheric layer
     */
    private static <T extends CalculusFieldElement<T>> T stecIonosphericLayer(final SsrIm201Data im201Data, final SsrIm201Header im201Header,
                                                                          final T elevation, final T azimuth,
                                                                          final FieldGeodeticPoint<T> point) {

        // Geodetic point data
        final T phiR    = point.getLatitude();
        final T lambdaR = point.getLongitude();
        final T hR      = point.getAltitude();

        // Data contained in the message
        final double     hI     = im201Data.getHeightIonosphericLayer();
        final int        degree = im201Data.getSphericalHarmonicsDegree();
        final int        order  = im201Data.getSphericalHarmonicsOrder();
        final double[][] cnm    = im201Data.getCnm();
        final double[][] snm    = im201Data.getSnm();

        // Spherical Earth's central angle
        final T psiPP = calculatePsi(hR, hI, elevation);

        // Sine and cosine of useful angles
        final FieldSinCos<T> scA    = FastMath.sinCos(azimuth);
        final FieldSinCos<T> scPhiR = FastMath.sinCos(phiR);
        final FieldSinCos<T> scPsi  = FastMath.sinCos(psiPP);

        // Pierce point latitude and longitude
        final T phiPP    = calculatePiercePointLatitude(scPhiR, scPsi, scA);
        final T lambdaPP = calculatePiercePointLongitude(scA, phiPP, psiPP, phiR, lambdaR);

        // Mean sun fixed longitude (modulo 2pi)
        final T lambdaS = calculateSunLongitude(im201Header, lambdaPP);

        // VTEC
        // According to the documentation, negative VTEC values must be ignored and shall be replaced by 0.0
        final T vtec = FastMath.max(phiR.getField().getZero(), calculateVTEC(degree, order, cnm, snm, phiPP, lambdaS));

        // Return STEC for the current ionospheric layer
        return vtec.divide(FastMath.sin(elevation.add(psiPP)));

    }

    /**
     * Calculates the spherical Earth’s central angle between station position and
     * the projection of the pierce point to the spherical Earth’s surface.
     * @param hR height of station position in meters
     * @param hI height of ionospheric layer in meters
     * @param elevation satellite elevation angle in radians
     * @return the spherical Earth’s central angle in radians
     */
    private static double calculatePsi(final double hR, final double hI,
                                       final double elevation) {
        final double ratio = (EARTH_RADIUS + hR) / (EARTH_RADIUS + hI);
        return MathUtils.SEMI_PI - elevation - FastMath.asin(ratio * FastMath.cos(elevation));
    }

    /**
     * Calculates the spherical Earth’s central angle between station position and
     * the projection of the pierce point to the spherical Earth’s surface.
     * @param hR height of station position in meters
     * @param hI height of ionospheric layer in meters
     * @param elevation satellite elevation angle in radians
     * @param <T> type of the elements
     * @return the spherical Earth’s central angle in radians
     */
    private static <T extends CalculusFieldElement<T>> T calculatePsi(final T hR, final double hI,
                                                                  final T elevation) {
        final T ratio = hR.add(EARTH_RADIUS).divide(EARTH_RADIUS + hI);
        return hR.getPi().multiply(0.5).subtract(elevation).subtract(FastMath.asin(ratio.multiply(FastMath.cos(elevation))));
    }

    /**
     * Calculates the latitude of the pierce point in the spherical Earth model.
     * @param scPhiR sine and cosine of the geocentric latitude of the station
     * @param scPsi sine and cosine of the spherical Earth's central angle
     * @param scA sine and cosine of the azimuth angle
     * @return the latitude of the pierce point in the spherical Earth model in radians
     */
    private static double calculatePiercePointLatitude(final SinCos scPhiR, final SinCos scPsi, final SinCos scA) {
        return FastMath.asin(scPhiR.sin() * scPsi.cos() + scPhiR.cos() * scPsi.sin() * scA.cos());
    }

    /**
     * Calculates the latitude of the pierce point in the spherical Earth model.
     * @param scPhiR sine and cosine of the geocentric latitude of the station
     * @param scPsi sine and cosine of the spherical Earth's central angle
     * @param scA sine and cosine of the azimuth angle
     * @param <T> type of the elements
     * @return the latitude of the pierce point in the spherical Earth model in radians
     */
    private static <T extends CalculusFieldElement<T>> T calculatePiercePointLatitude(final FieldSinCos<T> scPhiR,
                                                                                  final FieldSinCos<T> scPsi,
                                                                                  final FieldSinCos<T> scA) {
        return FastMath.asin(scPhiR.sin().multiply(scPsi.cos()).add(scPhiR.cos().multiply(scPsi.sin()).multiply(scA.cos())));
    }

    /**
     * Calculates the longitude of the pierce point in the spherical Earth model.
     * @param scA sine and cosine of the azimuth angle
     * @param phiPP the latitude of the pierce point in the spherical Earth model in radians
     * @param psiPP the spherical Earth’s central angle in radians
     * @param phiR the geocentric latitude of the station in radians
     * @param lambdaR the geocentric longitude of the station
     * @return the longitude of the pierce point in the spherical Earth model in radians
     */
    private static double calculatePiercePointLongitude(final SinCos scA,
                                                        final double phiPP, final double psiPP,
                                                        final double phiR, final double lambdaR) {

        // arcSin(sin(PsiPP) * sin(Azimuth) / cos(PhiPP))
        final double arcSin = FastMath.asin(FastMath.sin(psiPP) * scA.sin() / FastMath.cos(phiPP));

        // Return
        return verifyCondition(scA.cos(), psiPP, phiR) ? lambdaR + FastMath.PI - arcSin : lambdaR + arcSin;

    }

    /**
     * Calculates the longitude of the pierce point in the spherical Earth model.
     * @param scA sine and cosine of the azimuth angle
     * @param phiPP the latitude of the pierce point in the spherical Earth model in radians
     * @param psiPP the spherical Earth’s central angle in radians
     * @param phiR the geocentric latitude of the station in radians
     * @param lambdaR the geocentric longitude of the station
     * @param <T> type of the elements
     * @return the longitude of the pierce point in the spherical Earth model in radians
     */
    private static <T extends CalculusFieldElement<T>> T calculatePiercePointLongitude(final FieldSinCos<T> scA,
                                                                                   final T phiPP, final T psiPP,
                                                                                   final T phiR, final T lambdaR) {

        // arcSin(sin(PsiPP) * sin(Azimuth) / cos(PhiPP))
        final T arcSin = FastMath.asin(FastMath.sin(psiPP).multiply(scA.sin()).divide(FastMath.cos(phiPP)));

        // Return
        return verifyCondition(scA.cos().getReal(), psiPP.getReal(), phiR.getReal()) ?
                                               lambdaR.add(arcSin.getPi()).subtract(arcSin) : lambdaR.add(arcSin);

    }

    /**
     * Calculate the mean sun fixed longitude phase.
     * @param im201Header header of the IM201 message
     * @param lambdaPP the longitude of the pierce point in the spherical Earth model in radians
     * @return the mean sun fixed longitude phase in radians
     */
    private static double calculateSunLongitude(final SsrIm201Header im201Header, final double lambdaPP) {
        final double t = getTime(im201Header);
        return MathUtils.normalizeAngle(lambdaPP + (t - 50400.0) * FastMath.PI / 43200.0, FastMath.PI);
    }

    /**
     * Calculate the mean sun fixed longitude phase.
     * @param im201Header header of the IM201 message
     * @param lambdaPP the longitude of the pierce point in the spherical Earth model in radians
     * @param <T> type of the elements
     * @return the mean sun fixed longitude phase in radians
     */
    private static <T extends CalculusFieldElement<T>> T calculateSunLongitude(final SsrIm201Header im201Header, final T lambdaPP) {
        final double t = getTime(im201Header);
        return MathUtils.normalizeAngle(lambdaPP.add(lambdaPP.getPi().multiply(t - 50400.0).divide(43200.0)), lambdaPP.getPi());
    }

    /**
     * Calculate the VTEC contribution for a given ionospheric layer.
     * @param degree degree of spherical expansion
     * @param order order of spherical expansion
     * @param cnm cosine coefficients for the layer in TECU
     * @param snm sine coefficients for the layer in TECU
     * @param phiPP geocentric latitude of ionospheric pierce point for the layer in radians
     * @param lambdaS mean sun fixed and phase shifted longitude of ionospheric pierce point
     * @return the VTEC contribution for the current ionospheric layer in TECU
     */
    private static double calculateVTEC(final int degree, final int order,
                                        final double[][] cnm, final double[][] snm,
                                        final double phiPP, final double lambdaS) {

        // Initialize VTEC value
        double vtec = 0.0;

        // Compute Legendre Polynomials Pnm(sin(phiPP))
        final LegendrePolynomials p = new LegendrePolynomials(degree, order, FastMath.sin(phiPP));

        // Calculate VTEC
        for (int n = 0; n <= degree; n++) {

            for (int m = 0; m <= FastMath.min(n, order); m++) {

                // Legendre coefficients
                final SinCos sc = FastMath.sinCos(m * lambdaS);
                final double pCosmLambda = p.getPnm(n, m) * sc.cos();
                final double pSinmLambda = p.getPnm(n, m) * sc.sin();

                // Update VTEC value
                vtec += cnm[n][m] * pCosmLambda + snm[n][m] * pSinmLambda;

            }

        }

        // Return the VTEC
        return vtec;

    }

    /**
     * Calculate the VTEC contribution for a given ionospheric layer.
     * @param degree degree of spherical expansion
     * @param order order of spherical expansion
     * @param cnm cosine coefficients for the layer in TECU
     * @param snm sine coefficients for the layer in TECU
     * @param phiPP geocentric latitude of ionospheric pierce point for the layer in radians
     * @param lambdaS mean sun fixed and phase shifted longitude of ionospheric pierce point
     * @param <T> type of the elements
     * @return the VTEC contribution for the current ionospheric layer in TECU
     */
    private static <T extends CalculusFieldElement<T>> T calculateVTEC(final int degree, final int order,
                                                                   final double[][] cnm, final double[][] snm,
                                                                   final T phiPP, final T lambdaS) {

        // Initialize VTEC value
        T vtec = phiPP.getField().getZero();

        // Compute Legendre Polynomials Pnm(sin(phiPP))
        final FieldLegendrePolynomials<T> p = new FieldLegendrePolynomials<>(degree, order, FastMath.sin(phiPP));

        // Calculate VTEC
        for (int n = 0; n <= degree; n++) {

            for (int m = 0; m <= FastMath.min(n, order); m++) {

                // Legendre coefficients
                final FieldSinCos<T> sc = FastMath.sinCos(lambdaS.multiply(m));
                final T pCosmLambda = p.getPnm(n, m).multiply(sc.cos());
                final T pSinmLambda = p.getPnm(n, m).multiply(sc.sin());

                // Update VTEC value
                vtec = vtec.add(pCosmLambda.multiply(cnm[n][m]).add(pSinmLambda.multiply(snm[n][m])));

            }

        }

        // Return the VTEC
        return vtec;

    }

    /**
     * Get the SSR epoch time of computation modulo 86400 seconds.
     * @param im201Header header data
     * @return the SSR epoch time of computation modulo 86400 seconds
     */
    private static double getTime(final SsrIm201Header im201Header) {
        final double ssrEpochTime = im201Header.getSsrEpoch1s();
        return ssrEpochTime - FastMath.floor(ssrEpochTime / Constants.JULIAN_DAY) * Constants.JULIAN_DAY;
    }

    /**
     * Verify the condition for the calculation of the pierce point longitude.
     * @param scACos cosine of the azimuth angle
     * @param psiPP the spherical Earth’s central angle in radians
     * @param phiR the geocentric latitude of the station in radians
     * @return true if the condition is respected
     */
    private static boolean verifyCondition(final double scACos, final double psiPP,
                                           final double phiR) {

        // tan(PsiPP) * cos(Azimuth)
        final double tanPsiCosA = FastMath.tan(psiPP) * scACos;

        // Verify condition
        return phiR >= 0 && tanPsiCosA > FastMath.tan(MathUtils.SEMI_PI - phiR) ||
                        phiR < 0 && -tanPsiCosA > FastMath.tan(MathUtils.SEMI_PI + phiR);

    }

}
