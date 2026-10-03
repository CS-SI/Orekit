/* Copyright 2002-2025 CS GROUP
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
package org.orekit.forces.radiation;

import java.util.List;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.analysis.polynomials.PolynomialFunction;
import org.hipparchus.analysis.polynomials.PolynomialsUtils;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.FieldSinCos;
import org.hipparchus.util.MathUtils;
import org.hipparchus.util.SinCos;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.DataContext;
import org.orekit.forces.ForceModel;
import org.orekit.frames.Frame;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.utils.Constants;
import org.orekit.utils.ExtendedPositionProvider;
import org.orekit.utils.ParameterDriver;

/** The Knocke Earth Albedo and IR emission force model.
 * <p>
 * This model is based on "EARTH RADIATION PRESSURE EFFECTS ON SATELLITES", 1988, by P. C. Knocke, J. C. Ries, and B. D. Tapley.
 * </p> <p>
 * This model represents the effects of radiation pressure coming from the Earth.
 * It considers Solar radiation which has been reflected by Earth (albedo) and Earth infrared emissions.
 * The planet is considered as a sphere and is divided into elementary areas.
 * Each elementary area is considered as a plane and emits radiation according to Lambert's law.
 * The flux the satellite receives is then equal to the sum of the elementary fluxes coming from Earth.
 * </p> <p>
 * The radiative model of the satellite, and its ability to diffuse, reflect  or absorb radiation is handled
 * by a {@link RadiationSensitive radiation sensitive model}.
 * </p> <p>
 * <b>Caution:</b> This model is only suitable for Earth. Using it with another central body is prone to error..
 * </p>
 *
 * @author Thomas Paulet
 * @since 10.3
 */
public class KnockeRediffusedForceModel implements ForceModel {

    /** Earth rotation around Sun pulsation in rad/sec. */
    public static final double EARTH_AROUND_SUN_PULSATION = MathUtils.TWO_PI / Constants.JULIAN_YEAR;

    /** Coefficient for solar irradiance computation. */
    public static final double ES_COEFF = 4.5606E-6;

    /** First coefficient for albedo computation. */
    public static final double A0 = 0.34;

    /** Second coefficient for albedo computation. */
    public static final double C0 = 0.;

    /** Third coefficient for albedo computation. */
    public static final double C1 = 0.10;

    /** Fourth coefficient for albedo computation. */
    public static final double C2 = 0.;

    /** Fifth coefficient for albedo computation. */
    public static final double A2 = 0.29;

    /** First coefficient for Earth emissivity computation. */
    public static final double E0 = 0.68;

    /** Second coefficient for Earth emissivity computation. */
    public static final double K0 = 0.;

    /** Third coefficient for Earth emissivity computation. */
    public static final double K1 = -0.07;

    /** Fourth coefficient for Earth emissivity computation. */
    public static final double K2 = 0.;

    /** Fifth coefficient for Earth emissivity computation. */
    public static final double E2 = -0.18;

    /** Sun model. */
    private final ExtendedPositionProvider sun;

    /** Spacecraft. */
    private final RadiationSensitive spacecraft;

    /** Angular resolution for emissivity and albedo computation in rad. */
    private final double angularResolution;

    /** Earth equatorial radius in m. */
    private final double equatorialRadius;

    /** Reference date for periodic terms: December 22nd 1981.
     * Without more precision, the choice is to set it at midnight, UTC. */
    private final AbsoluteDate referenceEpoch;

    /** Default Constructor.
     * <p>This constructor uses the {@link DataContext#getDefault() default data context}</p>.
     * @param sun Sun model
     * @param spacecraft the object physical and geometrical information
     * @param equatorialRadius the Earth equatorial radius in m
     * @param angularResolution angular resolution in rad
     */
    @DefaultDataContext
    public KnockeRediffusedForceModel (final ExtendedPositionProvider sun,
                                       final RadiationSensitive spacecraft,
                                       final double equatorialRadius,
                                       final double angularResolution) {

        this(sun, spacecraft, equatorialRadius, angularResolution, DataContext.getDefault().getTimeScales().getUTC());
    }

    /** General constructor.
     * @param sun Sun model
     * @param spacecraft the object physical and geometrical information
     * @param equatorialRadius the Earth equatorial radius in m
     * @param angularResolution angular resolution in rad
     * @param utc the UTC time scale to define reference epoch
     */
    public KnockeRediffusedForceModel (final ExtendedPositionProvider sun,
                                       final RadiationSensitive spacecraft,
                                       final double equatorialRadius,
                                       final double angularResolution,
                                       final TimeScale utc) {
        this.sun               = sun;
        this.spacecraft        = spacecraft;
        this.equatorialRadius  = equatorialRadius;
        this.angularResolution = angularResolution;
        this.referenceEpoch    = new AbsoluteDate(1981, 12, 22, 0, 0, 0.0, utc);
    }


    /** {@inheritDoc} */
    @Override
    public boolean dependsOnPositionOnly() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D acceleration(final SpacecraftState s,
                                 final double[] parameters) {

        // Get date
        final AbsoluteDate date = s.getDate();

        // Get frame
        final Frame frame = s.getFrame();

        // Get satellite position
        final Vector3D satellitePosition = s.getPosition();

        // Get Sun position
        final Vector3D sunPosition = sun.getPosition(date, frame);

        // Project satellite on Earth as vector
        final Vector3D projectedToGround = satellitePosition.normalize().scalarMultiply(equatorialRadius);

        // Get an axis orthogonal to the satellite position for Earth browsing using rotations
        final Vector3D offsetAxis = satellitePosition.orthogonal();

        // Get satellite distance and horizon angle
        final double r       = satellitePosition.getNorm();
        final double horizon = FastMath.acos(equatorialRadius / r);

        // Split each ring into equal azimuth cells covering exactly the full circle
        final int    azimuthCells = FastMath.max(1, (int) FastMath.round(MathUtils.TWO_PI / angularResolution));
        final double azimuthStep  = MathUtils.TWO_PI / azimuthCells;

        // Grid the part of Earth which is seen by the satellite into concentric rings around nadir,
        // browsed outward from nadir to the horizon with constant angular resolution
        Vector3D rediffusedFlux = Vector3D.ZERO;
        double innerAngle = 0.0;
        while (innerAngle < horizon) {

            // Get current ring angles from nadir
            // The last ring extends to the horizon and is 0.5 to 1.5 angular resolution wide
            final double outerAngle = innerAngle + 1.5 * angularResolution < horizon ?
                                      innerAngle + angularResolution : horizon;
            final double centerAngle = 0.5 * (innerAngle + outerAngle);

            // Build rotation transformations to get first cell center of the ring
            final Rotation centerRotation = new Rotation(offsetAxis, centerAngle, RotationConvention.VECTOR_OPERATOR);
            final Vector3D firstCellCenter = centerRotation.applyTo(projectedToGround);

            // Compute cell area, the same for all cells of the ring
            final double cellArea = computeCellArea(r, innerAngle, outerAngle, centerAngle, azimuthCells);

            // Browse all cells of the ring
            for (int i = 0; i < azimuthCells; ++i) {

                // Build rotation transformations to get elementary area center
                final Rotation radialRotation  = new Rotation(projectedToGround, (i + 0.5) * azimuthStep,
                                                              RotationConvention.VECTOR_OPERATOR);

                // Get current grid cell center
                final Vector3D currentCenter = radialRotation.applyTo(firstCellCenter);

                // Add current cell contribution to total rediffused flux
                rediffusedFlux = rediffusedFlux.add(computeElementaryFlux(s, currentCenter, sunPosition, cellArea));
            }

            innerAngle = outerAngle;
        }

        return spacecraft.radiationPressureAcceleration(s, rediffusedFlux, parameters);
    }


    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldVector3D<T> acceleration(final FieldSpacecraftState<T> s,
                                                                             final T[] parameters) {
        // Get date
        final FieldAbsoluteDate<T> date = s.getDate();

        // Get frame
        final Frame frame = s.getFrame();

        // Get zero
        final T zero = date.getField().getZero();

        // Get satellite position
        final FieldVector3D<T> satellitePosition = s.getPosition();

        // Get Sun position
        final FieldVector3D<T> sunPosition = sun.getPosition(date, frame);

        // Project satellite on Earth as vector
        final FieldVector3D<T> projectedToGround = satellitePosition.normalize().scalarMultiply(equatorialRadius);

        // Get an axis orthogonal to the satellite position for Earth browsing using rotations
        final FieldVector3D<T> offsetAxis = satellitePosition.orthogonal();

        // Get satellite distance and horizon angle
        final T r       = satellitePosition.getNorm();
        final T horizon = FastMath.acos(r.reciprocal().multiply(equatorialRadius));

        // Split each ring into equal azimuth cells covering exactly the full circle
        final int    azimuthCells = FastMath.max(1, (int) FastMath.round(MathUtils.TWO_PI / angularResolution));
        final double azimuthStep  = MathUtils.TWO_PI / azimuthCells;

        // Grid the part of Earth which is seen by the satellite into concentric rings around nadir,
        // browsed outward from nadir to the horizon with constant angular resolution
        FieldVector3D<T> rediffusedFlux = FieldVector3D.getZero(date.getField());
        T innerAngle = zero;
        while (innerAngle.getReal() < horizon.getReal()) {

            // Get current ring angles from nadir
            // The last ring extends to the horizon and is 0.5 to 1.5 angular resolution wide
            final T outerAngle = innerAngle.getReal() + 1.5 * angularResolution < horizon.getReal() ?
                                 innerAngle.add(angularResolution) : horizon;
            final T centerAngle = innerAngle.add(outerAngle).multiply(0.5);

            // Build rotation transformations to get first cell center of the ring
            final FieldRotation<T> centerRotation = new FieldRotation<>(offsetAxis, centerAngle,
                                                                      RotationConvention.VECTOR_OPERATOR);
            final FieldVector3D<T> firstCellCenter = centerRotation.applyTo(projectedToGround);

            // Compute cell area, the same for all cells of the ring
            final T cellArea = computeCellArea(r, innerAngle, outerAngle, centerAngle, azimuthCells);

            // Browse all cells of the ring
            for (int i = 0; i < azimuthCells; ++i) {

                // Build rotation transformations to get elementary area center
                final FieldRotation<T> radialRotation  = new FieldRotation<>(projectedToGround,
                                                                             zero.newInstance((i + 0.5) * azimuthStep),
                                                                             RotationConvention.VECTOR_OPERATOR);

                // Get current grid cell center
                final FieldVector3D<T> currentCenter = radialRotation.applyTo(firstCellCenter);

                // Add current cell contribution to total rediffused flux
                rediffusedFlux = rediffusedFlux.add(computeElementaryFlux(s, currentCenter, sunPosition, cellArea));
            }

            innerAngle = outerAngle;
        }

        return spacecraft.radiationPressureAcceleration(s, rediffusedFlux, parameters);
    }


    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return spacecraft.getRadiationParametersDrivers();
    }

    /** Compute Earth albedo.
     * Albedo value represents the fraction of solar radiative flux that is reflected by Earth.
     * Its value is in [0;1].
     * @param date the date
     * @param phi the equatorial latitude in rad
     * @return the albedo in [0;1]
     */
    public double computeAlbedo(final AbsoluteDate date, final double phi) {

        // Get duration since coefficient reference epoch
        final double deltaT = date.durationFrom(referenceEpoch);

        // Compute 1rst Legendre polynomial coeficient
        final SinCos sc = FastMath.sinCos(EARTH_AROUND_SUN_PULSATION * deltaT);
        final double A1 = C0 +
                          C1 * sc.cos() +
                          C2 * sc.sin();

        // Get 1rst and 2nd order Legendre polynomials
        final PolynomialFunction firstLegendrePolynomial  = PolynomialsUtils.createLegendrePolynomial(1);
        final PolynomialFunction secondLegendrePolynomial = PolynomialsUtils.createLegendrePolynomial(2);

        // Get latitude sinus
        final double sinPhi = FastMath.sin(phi);

        // Compute albedo
        return A0 +
               A1 * firstLegendrePolynomial.value(sinPhi) +
               A2 * secondLegendrePolynomial.value(sinPhi);

    }


    /** Compute Earth albedo.
     * Albedo value represents the fraction of solar radiative flux that is reflected by Earth.
     * Its value is in [0;1].
     * @param date the date
     * @param phi the equatorial latitude in rad
     * @param <T> extends CalculusFieldElement
     * @return the albedo in [0;1]
     */
    public <T extends CalculusFieldElement<T>> T computeAlbedo(final FieldAbsoluteDate<T> date, final T phi) {

        // Get duration since coefficient reference epoch
        final T deltaT = date.durationFrom(referenceEpoch);

        // Compute 1rst Legendre polynomial coeficient
        final FieldSinCos<T> sc = FastMath.sinCos(deltaT.multiply(EARTH_AROUND_SUN_PULSATION));
        final T A1 = sc.cos().multiply(C1).add(
                     sc.sin().multiply(C2)).add(C0);

        // Get 1rst and 2nd order Legendre polynomials
        final PolynomialFunction firstLegendrePolynomial  = PolynomialsUtils.createLegendrePolynomial(1);
        final PolynomialFunction secondLegendrePolynomial = PolynomialsUtils.createLegendrePolynomial(2);

        // Get latitude sinus
        final T sinPhi = FastMath.sin(phi);

        // Compute albedo
        return firstLegendrePolynomial.value(sinPhi).multiply(A1).add(
               secondLegendrePolynomial.value(sinPhi).multiply(A2)).add(A0);

    }

    /** Compute Earth emisivity.
     * Emissivity is used to compute the infrared flux that is emitted by Earth.
     * Its value is in [0;1].
     * @param date the date
     * @param phi the equatorial latitude in rad
     * @return the emissivity in [0;1]
     */
    public double computeEmissivity(final AbsoluteDate date, final double phi) {

        // Get duration since coefficient reference epoch
        final double deltaT = date.durationFrom(referenceEpoch);

        // Compute 1rst Legendre polynomial coefficient
        final SinCos sc = FastMath.sinCos(EARTH_AROUND_SUN_PULSATION * deltaT);
        final double E1 = K0 +
                          K1 * sc.cos() +
                          K2 * sc.sin();

        // Get 1rst and 2nd order Legendre polynomials
        final PolynomialFunction firstLegendrePolynomial  = PolynomialsUtils.createLegendrePolynomial(1);
        final PolynomialFunction secondLegendrePolynomial = PolynomialsUtils.createLegendrePolynomial(2);

        // Get latitude sinus
        final double sinPhi = FastMath.sin(phi);

        // Compute the emissivity
        return E0 +
               E1 * firstLegendrePolynomial.value(sinPhi) +
               E2 * secondLegendrePolynomial.value(sinPhi);

    }


    /** Compute Earth emisivity.
     * Emissivity is used to compute the infrared flux that is emitted by Earth.
     * Its value is in [0;1].
     * @param date the date
     * @param phi the equatorial latitude in rad
     * @param <T> extends CalculusFieldElement
     * @return the emissivity in [0;1]
     */
    public <T extends CalculusFieldElement<T>> T computeEmissivity(final FieldAbsoluteDate<T> date, final T phi) {

        // Get duration since coefficient reference epoch
        final T deltaT = date.durationFrom(referenceEpoch);

        // Compute 1rst Legendre polynomial coeficient
        final FieldSinCos<T> sc = FastMath.sinCos(deltaT.multiply(EARTH_AROUND_SUN_PULSATION));
        final T E1 = sc.cos().multiply(K1).add(
                     sc.sin().multiply(K2)).add(K0);

        // Get 1rst and 2nd order Legendre polynomials
        final PolynomialFunction firstLegendrePolynomial  = PolynomialsUtils.createLegendrePolynomial(1);
        final PolynomialFunction secondLegendrePolynomial = PolynomialsUtils.createLegendrePolynomial(2);

        // Get latitude sinus
        final T sinPhi = FastMath.sin(phi);

        // Compute the emissivity
        return firstLegendrePolynomial.value(sinPhi).multiply(E1).add(
               secondLegendrePolynomial.value(sinPhi).multiply(E2)).add(E0);

    }

    /** Compute total solar flux impacting Earth.
     * @param sunPosition the Sun position in an Earth centered frame
     * @return the total solar flux impacting Earth in J/m^3
     */
    public double computeSolarFlux(final Vector3D sunPosition) {

        // Compute Earth - Sun distance in UA
        final double earthSunDistance = sunPosition.getNorm() / Constants.JPL_SSD_ASTRONOMICAL_UNIT;

        // Compute Solar flux
        return ES_COEFF * Constants.SPEED_OF_LIGHT / (earthSunDistance * earthSunDistance);
    }


    /** Compute total solar flux impacting Earth.
     * @param sunPosition the Sun position in an Earth centered frame
     * @param <T> extends CalculusFieldElement
     * @return the total solar flux impacting Earth in J/m^3
     */
    public <T extends CalculusFieldElement<T>> T computeSolarFlux(final FieldVector3D<T> sunPosition) {

        // Compute Earth - Sun distance in UA
        final T earthSunDistance = sunPosition.getNorm().divide(Constants.JPL_SSD_ASTRONOMICAL_UNIT);

        // Compute Solar flux
        return earthSunDistance.multiply(earthSunDistance).reciprocal().multiply(ES_COEFF * Constants.SPEED_OF_LIGHT);
    }


    /** Compute the area of a grid cell giving, when lumped at its center, its exact flux along nadir.
     * @param r satellite distance to Earth center
     * @param innerAngle cell inner angle from nadir, seen from Earth center
     * @param outerAngle cell outer angle from nadir, seen from Earth center, not beyond the horizon
     * @param centerAngle cell center angle from nadir, seen from Earth center
     * @param azimuthCells number of cells in azimuth
     * @return cell area
     * @since 13.1.9
     */
    private double computeCellArea(final double r, final double innerAngle, final double outerAngle,
                                   final double centerAngle, final int azimuthCells) {
        final double h  = r - equatorialRadius;
        final double sc = FastMath.sin(0.5 * centerAngle);
        final double xc = 2.0 * sc * sc;
        final double d2 = h * h + 2.0 * equatorialRadius * r * xc;
        return FastMath.PI * d2 * d2 *
               (computeNadirViewFactor(r, outerAngle) - computeNadirViewFactor(r, innerAngle)) /
               ((h - r * xc) * (h + equatorialRadius * xc) * azimuthCells);
    }

    /** Compute the area of a grid cell giving, when lumped at its center, its exact flux along nadir.
     * @param <T> type of the field elements
     * @param r satellite distance to Earth center
     * @param innerAngle cell inner angle from nadir, seen from Earth center
     * @param outerAngle cell outer angle from nadir, seen from Earth center, not beyond the horizon
     * @param centerAngle cell center angle from nadir, seen from Earth center
     * @param azimuthCells number of cells in azimuth
     * @return cell area
     * @since 13.1.9
     */
    private <T extends CalculusFieldElement<T>> T computeCellArea(final T r, final T innerAngle, final T outerAngle,
                                                                  final T centerAngle, final int azimuthCells) {
        final T h  = r.subtract(equatorialRadius);
        final T sc = FastMath.sin(centerAngle.multiply(0.5));
        final T xc = sc.multiply(2.0).multiply(sc);
        final T d2 = h.square().add(r.multiply(2.0 * equatorialRadius).multiply(xc));
        return d2.multiply(FastMath.PI).multiply(d2).
               multiply(computeNadirViewFactor(r, outerAngle).subtract(computeNadirViewFactor(r, innerAngle))).
               divide(h.subtract(r.multiply(xc)).multiply(h.add(xc.multiply(equatorialRadius))).multiply(azimuthCells));
    }

    /** Compute the view factor along nadir of the Earth within given angle from nadir.
     * <p>
     * For a Lambertian sphere, it is (Re * sin(angle) / d)^2, with d the distance between the satellite
     * and the points at this angle from nadir: d^2 = h^2 + 2 * Re * r * (1 - cos(angle)), h = r - Re
     * being the satellite altitude.
     * </p>
     * @param r satellite distance to Earth center
     * @param angle angle from nadir, seen from Earth center, not beyond the horizon
     * @return view factor
     * @since 13.1.9
     */
    private double computeNadirViewFactor(final double r, final double angle) {
        final double h = r - equatorialRadius;
        final double s = FastMath.sin(0.5 * angle);
        final double x = 2.0 * s * s;
        return equatorialRadius * equatorialRadius * x * (2.0 - x) / (h * h + 2.0 * equatorialRadius * r * x);
    }

    /** Compute the view factor along nadir of the Earth within given angle from nadir.
     * @param <T> type of the field elements
     * @param r satellite distance to Earth center
     * @param angle angle from nadir, seen from Earth center, not beyond the horizon
     * @return view factor
     * @since 13.1.9
     */
    private <T extends CalculusFieldElement<T>> T computeNadirViewFactor(final T r, final T angle) {
        final T h = r.subtract(equatorialRadius);
        final T s = FastMath.sin(angle.multiply(0.5));
        final T x = s.multiply(2.0).multiply(s);
        return x.multiply(equatorialRadius * equatorialRadius).multiply(x.negate().add(2.0)).
               divide(h.square().add(r.multiply(2.0 * equatorialRadius).multiply(x)));
    }

    /** Compute elementary rediffused flux on satellite.
     * @param state the current spacecraft state
     * @param elementCenter the position of the considered area center
     * @param sunPosition the position of the Sun in the spacecraft frame
     * @param elementArea the area of the current element
     * @return the rediffused flux from considered element on the spacecraft
     */
    public Vector3D computeElementaryFlux(final SpacecraftState state,
                                          final Vector3D elementCenter,
                                          final Vector3D sunPosition,
                                          final double elementArea) {

        // Get satellite position
        final Vector3D satellitePosition = state.getPosition();

        // Get current date
        final AbsoluteDate date = state.getDate();

        // Get solar flux impacting Earth
        final double solarFlux = computeSolarFlux(sunPosition);

        // Compute elementary area - satellite vector and distance
        final Vector3D r = satellitePosition.subtract(elementCenter);
        final double rNorm = r.getNorm();

        // Get satellite viewing angle as seen from current elementary area
        final double centerNorm = elementCenter.getNorm();
        final double cosAlpha   = Vector3D.dotProduct(elementCenter, r) /
                                  (centerNorm * rNorm);

        // Check that satellite sees the current area
        if (cosAlpha > 0) {

            // Get current elementary area center latitude
            final double currentLatitude = elementCenter.getDelta();

            // Compute Earth emissivity value
            final double e = computeEmissivity(date, currentLatitude);

            // Initialize albedo
            double a = 0.0;

            // Check if elementary area is in daylight
            final double cosSunAngle = Vector3D.dotProduct(elementCenter, sunPosition) /
                                       (centerNorm * sunPosition.getNorm());

            if (cosSunAngle > 0) {
                // Elementary area is in daylight, compute albedo value
                a = computeAlbedo(date, currentLatitude);
            }

            // Compute elementary area contribution to rediffused flux
            final double albedoAndIR = a * solarFlux * cosSunAngle + e * solarFlux * 0.25;

            // Compute attenuated projected elementary area vector
            final Vector3D projectedAreaVector = r.scalarMultiply(elementArea * cosAlpha /
                                                                 (FastMath.PI * rNorm * rNorm * rNorm));

            // Compute elementary radiation flux from current elementary area
            return projectedAreaVector.scalarMultiply(albedoAndIR / Constants.SPEED_OF_LIGHT);

        } else {

            // Spacecraft does not see the elementary area
            return new Vector3D(0.0, 0.0, 0.0);
        }

    }


    /** Compute elementary rediffused flux on satellite.
     * @param state the current spacecraft state
     * @param elementCenter the position of the considered area center
     * @param sunPosition the position of the Sun in the spacecraft frame
     * @param elementArea the area of the current element
     * @param <T> extends CalculusFieldElement
     * @return the rediffused flux from considered element on the spacecraft
     */
    public <T extends CalculusFieldElement<T>> FieldVector3D<T> computeElementaryFlux(final FieldSpacecraftState<T> state,
                                                                                      final FieldVector3D<T> elementCenter,
                                                                                      final FieldVector3D<T> sunPosition,
                                                                                      final T elementArea) {

        // Get satellite position
        final FieldVector3D<T> satellitePosition = state.getPosition();

        // Get current date
        final FieldAbsoluteDate<T> date = state.getDate();

        // Get zero
        final T zero = date.getField().getZero();

        // Get solar flux impacting Earth
        final T solarFlux = computeSolarFlux(sunPosition);

        // Compute elementary area - satellite vector and distance
        final FieldVector3D<T> r = satellitePosition.subtract(elementCenter);
        final T rNorm = r.getNorm();

        // Get satellite viewing angle as seen from current elementary area
        final T centerNorm = elementCenter.getNorm();
        final T cosAlpha   = FieldVector3D.dotProduct(elementCenter, r).
                             divide(centerNorm.multiply(rNorm));

        // Check that satellite sees the current area
        if (cosAlpha.getReal() > 0) {

            // Get current elementary area center latitude
            final T currentLatitude = elementCenter.getDelta();

            // Compute Earth emissivity value
            final T e = computeEmissivity(date, currentLatitude);

            // Initialize albedo
            T a = zero;

            // Check if elementary area is in daylight
            final T cosSunAngle = FieldVector3D.dotProduct(elementCenter, sunPosition).
                                  divide(centerNorm.multiply(sunPosition.getNorm()));

            if (cosSunAngle.getReal() > 0) {
                // Elementary area is in daylight, compute albedo value
                a = computeAlbedo(date, currentLatitude);
            }

            // Compute elementary area contribution to rediffused flux
            final T albedoAndIR = a.multiply(solarFlux).multiply(cosSunAngle).
                                  add(e.multiply(solarFlux).multiply(0.25));

            // Compute attenuated projected elementary area vector
            final FieldVector3D<T> projectedAreaVector = r.scalarMultiply(elementArea.multiply(cosAlpha).
                                                                          divide(rNorm.square().multiply(rNorm).multiply(zero.getPi())));

            // Compute elementary radiation flux from current elementary area
            return projectedAreaVector.scalarMultiply(albedoAndIR.divide(Constants.SPEED_OF_LIGHT));

        } else {

            // Spacecraft does not see the elementary area
            return new FieldVector3D<>(zero, zero, zero);
        }

    }

}
