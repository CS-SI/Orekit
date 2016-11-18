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
package org.orekit.forces;

import java.util.ArrayList;
import java.util.List;

import org.hipparchus.RealFieldElement;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.Precision;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitInternalError;
import org.orekit.errors.OrekitMessages;
import org.orekit.forces.drag.DragSensitive;
import org.orekit.forces.radiation.RadiationSensitive;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterObserver;

/** experimental class representing the features of a classical satellite
 * with a convex body shape and rotating flat solar arrays.
 * <p>
 * As of 5.0, this class is still considered experimental, so use it with care.
 * </p>
 * <p>
 * The body can be either a simple parallelepipedic box aligned with
 * spacecraft axes or a set of facets defined by their area and normal vector.
 * This should handle accurately most spacecraft shapes.
 * </p>
 * <p>
 * The solar array rotation with respect to satellite body can be either
 * the best lighting orientation (i.e. Sun exactly in solar array meridian
 * plane defined by solar array rotation axis and solar array normal vector)
 * or a rotation evolving linearly according to a start position and an
 * angular rate (which can be set to 0 for non-rotating panels, which may
 * occur in special modes or during contingencies).
 * </p>
 * <p>
 * This model does not take cast shadow between body and solar array into account.
 * </p>
 *
 * @author Luc Maisonobe
 * @author Pascal Parraud
 */
public class BoxAndSolarArraySpacecraft implements RadiationSensitive, DragSensitive {

    /** Parameters scaling factor.
     * <p>
     * We use a power of 2 to avoid numeric noise introduction
     * in the multiplications/divisions sequences.
     * </p>
     */
    private final double SCALE = FastMath.scalb(1.0, -3);

    /** Drivers for drag coefficient parameter. */
    private final ParameterDriver[] dragParametersDrivers;

    /** Drivers for radiation pressure coefficient parameter. */
    private final ParameterDriver[] radiationParametersDrivers;

    /** Surface vectors for body facets. */
    private final List<Facet> facets;

    /** Solar array area (m²). */
    private final double solarArrayArea;

    /** Reference date for linear rotation angle (may be null). */
    private final AbsoluteDate referenceDate;

    /** Rotation rate for linear rotation angle. */
    private final double rotationRate;

    /** Solar array reference axis in spacecraft frame (may be null). */
    private final Vector3D saX;

    /** Solar array third axis in spacecraft frame (may be null). */
    private final Vector3D saY;

    /** Solar array rotation axis in spacecraft frame. */
    private final Vector3D saZ;

    /** Drag coefficient. */
    private double dragCoeff;

    /** Absorption coefficient. */
    private double absorptionCoeff;

    /** Specular reflection coefficient. */
    private double specularReflectionCoeff;

    /** Diffuse reflection coefficient. */
    private double diffuseReflectionCoeff;

    /** Sun model. */
    private final PVCoordinatesProvider sun;

    /** Build a spacecraft model with best lighting of solar array.
     * <p>
     * Solar arrays orientation will be such that at each time the Sun direction
     * will always be in the solar array meridian plane defined by solar array
     * rotation axis and solar array normal vector.
     * </p>
     * @param xLength length of the body along its X axis (m)
     * @param yLength length of the body along its Y axis (m)
     * @param zLength length of the body along its Z axis (m)
     * @param sun sun model
     * @param solarArrayArea area of the solar array (m²)
     * @param solarArrayAxis solar array rotation axis in satellite frame
     * @param dragCoeff drag coefficient (used only for drag)
     * @param absorptionCoeff absorption coefficient between 0.0 an 1.0
     * (used only for radiation pressure)
     * @param reflectionCoeff specular reflection coefficient between 0.0 an 1.0
     * (used only for radiation pressure)
     */
    public BoxAndSolarArraySpacecraft(final double xLength, final double yLength,
                                      final double zLength,
                                      final PVCoordinatesProvider sun, final double solarArrayArea,
                                      final Vector3D solarArrayAxis,
                                      final double dragCoeff,
                                      final double absorptionCoeff,
                                      final double reflectionCoeff) {
        this(simpleBoxFacets(xLength, yLength, zLength), sun, solarArrayArea, solarArrayAxis,
             dragCoeff, absorptionCoeff, reflectionCoeff);
    }

    /** Build a spacecraft model with best lighting of solar array.
     * <p>
     * The spacecraft body is described by an array of surface vectors. Each facet of
     * the body is describe by a vector normal to the facet (pointing outward of the spacecraft)
     * and whose norm is the surface area in m².
     * </p>
     * <p>
     * Solar arrays orientation will be such that at each time the Sun direction
     * will always be in the solar array meridian plane defined by solar array
     * rotation axis and solar array normal vector.
     * </p>
     * @param facets body facets (only the facets with strictly positive area will be stored)
     * @param sun sun model
     * @param solarArrayArea area of the solar array (m²)
     * @param solarArrayAxis solar array rotation axis in satellite frame
     * @param dragCoeff drag coefficient (used only for drag)
     * @param absorptionCoeff absorption coefficient between 0.0 an 1.0
     * (used only for radiation pressure)
     * @param reflectionCoeff specular reflection coefficient between 0.0 an 1.0
     * (used only for radiation pressure)
     */
    public BoxAndSolarArraySpacecraft(final Facet[] facets,
                                      final PVCoordinatesProvider sun, final double solarArrayArea,
                                      final Vector3D solarArrayAxis,
                                      final double dragCoeff,
                                      final double absorptionCoeff,
                                      final double reflectionCoeff) {

        this.dragParametersDrivers      = new ParameterDriver[1];
        this.radiationParametersDrivers = new ParameterDriver[2];
        try {
            dragParametersDrivers[0] = new ParameterDriver(DragSensitive.DRAG_COEFFICIENT,
                                                           dragCoeff, SCALE, 0.0, Double.POSITIVE_INFINITY);
            dragParametersDrivers[0].addObserver(new ParameterObserver() {
                /** {@inheridDoc} */
                @Override
                public void valueChanged(final double previousValue, final ParameterDriver driver) {
                    BoxAndSolarArraySpacecraft.this.dragCoeff = driver.getValue();
                }
            });
            radiationParametersDrivers[0] = new ParameterDriver(RadiationSensitive.ABSORPTION_COEFFICIENT,
                                                                absorptionCoeff, SCALE, 0.0, 1.0);
            radiationParametersDrivers[0].addObserver(new ParameterObserver() {
                /** {@inheridDoc} */
                @Override
                public void valueChanged(final double previousValue, final ParameterDriver driver) {
                    BoxAndSolarArraySpacecraft.this.absorptionCoeff = driver.getValue();
                    BoxAndSolarArraySpacecraft.this.diffuseReflectionCoeff =
                                    1 - (driver.getValue() + BoxAndSolarArraySpacecraft.this.specularReflectionCoeff);
                }
            });
            radiationParametersDrivers[1] = new ParameterDriver(RadiationSensitive.REFLECTION_COEFFICIENT,
                                                                reflectionCoeff, SCALE, 0.0, 1.0);
            radiationParametersDrivers[1].addObserver(new ParameterObserver() {
                /** {@inheridDoc} */
                @Override
                public void valueChanged(final double previousValue, final ParameterDriver driver) {
                    BoxAndSolarArraySpacecraft.this.specularReflectionCoeff = driver.getValue();
                    BoxAndSolarArraySpacecraft.this.diffuseReflectionCoeff  =
                                    1 - (BoxAndSolarArraySpacecraft.this.absorptionCoeff + driver.getValue());
                }
            });
        } catch (OrekitException oe) {
            // this should never happen
            throw new OrekitInternalError(oe);
        }

        this.facets = filter(facets);

        this.sun            = sun;
        this.solarArrayArea = solarArrayArea;
        this.referenceDate  = null;
        this.rotationRate   = 0;

        this.saZ = solarArrayAxis.normalize();
        this.saY = null;
        this.saX = null;

        this.dragCoeff               = dragCoeff;
        this.absorptionCoeff         = absorptionCoeff;
        this.specularReflectionCoeff = reflectionCoeff;
        this.diffuseReflectionCoeff  = 1 - (absorptionCoeff + reflectionCoeff);
    }

    /** Build a spacecraft model with linear rotation of solar array.
     * <p>
     * Solar arrays orientation will be a regular rotation from the
     * reference orientation at reference date and using a constant
     * rotation rate.
     * </p>
     * @param xLength length of the body along its X axis (m)
     * @param yLength length of the body along its Y axis (m)
     * @param zLength length of the body along its Z axis (m)
     * @param sun sun model
     * @param solarArrayArea area of the solar array (m²)
     * @param solarArrayAxis solar array rotation axis in satellite frame
     * @param referenceDate reference date for the solar array rotation
     * @param referenceNormal direction of the solar array normal at reference date
     * in spacecraft frame
     * @param rotationRate rotation rate of the solar array, may be 0 (rad/s)
     * @param dragCoeff drag coefficient (used only for drag)
     * @param absorptionCoeff absorption coefficient between 0.0 an 1.0
     * (used only for radiation pressure)
     * @param reflectionCoeff specular reflection coefficient between 0.0 an 1.0
     * (used only for radiation pressure)
     */
    public BoxAndSolarArraySpacecraft(final double xLength, final double yLength,
                                      final double zLength,
                                      final PVCoordinatesProvider sun, final double solarArrayArea,
                                      final Vector3D solarArrayAxis,
                                      final AbsoluteDate referenceDate,
                                      final Vector3D referenceNormal,
                                      final double rotationRate,
                                      final double dragCoeff,
                                      final double absorptionCoeff,
                                      final double reflectionCoeff) {
        this(simpleBoxFacets(xLength, yLength, zLength), sun, solarArrayArea, solarArrayAxis,
             referenceDate, referenceNormal, rotationRate,
             dragCoeff, absorptionCoeff, reflectionCoeff);
    }

    /** Build a spacecraft model with linear rotation of solar array.
     * <p>
     * The spacecraft body is described by an array of surface vectors. Each facet of
     * the body is describe by a vector normal to the facet (pointing outward of the spacecraft)
     * and whose norm is the surface area in m².
     * </p>
     * <p>
     * Solar arrays orientation will be a regular rotation from the
     * reference orientation at reference date and using a constant
     * rotation rate.
     * </p>
     * @param facets body facets (only the facets with strictly positive area will be stored)
     * @param sun sun model
     * @param solarArrayArea area of the solar array (m²)
     * @param solarArrayAxis solar array rotation axis in satellite frame
     * @param referenceDate reference date for the solar array rotation
     * @param referenceNormal direction of the solar array normal at reference date
     * in spacecraft frame
     * @param rotationRate rotation rate of the solar array, may be 0 (rad/s)
     * @param dragCoeff drag coefficient (used only for drag)
     * @param absorptionCoeff absorption coefficient between 0.0 an 1.0
     * (used only for radiation pressure)
     * @param reflectionCoeff specular reflection coefficient between 0.0 an 1.0
     * (used only for radiation pressure)
     */
    public BoxAndSolarArraySpacecraft(final Facet[] facets,
                                      final PVCoordinatesProvider sun, final double solarArrayArea,
                                      final Vector3D solarArrayAxis,
                                      final AbsoluteDate referenceDate,
                                      final Vector3D referenceNormal,
                                      final double rotationRate,
                                      final double dragCoeff,
                                      final double absorptionCoeff,
                                      final double reflectionCoeff) {

        this.dragParametersDrivers      = new ParameterDriver[1];
        this.radiationParametersDrivers = new ParameterDriver[2];
        try {
            dragParametersDrivers[0] = new ParameterDriver(DragSensitive.DRAG_COEFFICIENT,
                                                           dragCoeff, SCALE,
                                                           Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
            dragParametersDrivers[0].addObserver(new ParameterObserver() {
                /** {@inheridDoc} */
                @Override
                public void valueChanged(final double previousValue, final ParameterDriver driver) {
                    BoxAndSolarArraySpacecraft.this.dragCoeff = driver.getValue();
                }
            });
            radiationParametersDrivers[0] = new ParameterDriver(RadiationSensitive.ABSORPTION_COEFFICIENT,
                                                                absorptionCoeff, SCALE, 0.0, 1.0);
            radiationParametersDrivers[0].addObserver(new ParameterObserver() {
                /** {@inheridDoc} */
                @Override
                public void valueChanged(final double previousValue, final ParameterDriver driver) {
                    BoxAndSolarArraySpacecraft.this.absorptionCoeff = driver.getValue();
                    BoxAndSolarArraySpacecraft.this.diffuseReflectionCoeff =
                                    1 - (driver.getValue() + BoxAndSolarArraySpacecraft.this.specularReflectionCoeff);
                }
            });
            radiationParametersDrivers[1] = new ParameterDriver(RadiationSensitive.REFLECTION_COEFFICIENT,
                                                                reflectionCoeff, SCALE, 0.0, 1.0);
            radiationParametersDrivers[1].addObserver(new ParameterObserver() {
                /** {@inheridDoc} */
                @Override
                public void valueChanged(final double previousValue, final ParameterDriver driver) {
                    BoxAndSolarArraySpacecraft.this.specularReflectionCoeff = driver.getValue();
                    BoxAndSolarArraySpacecraft.this.diffuseReflectionCoeff  =
                                    1 - (BoxAndSolarArraySpacecraft.this.absorptionCoeff + driver.getValue());
                }
            });
        } catch (OrekitException oe) {
            // this should never happen
            throw new OrekitInternalError(oe);
        }

        this.facets = filter(facets.clone());

        this.sun            = sun;
        this.solarArrayArea = solarArrayArea;
        this.referenceDate  = referenceDate;
        this.rotationRate   = rotationRate;

        this.saZ = solarArrayAxis.normalize();
        this.saY = Vector3D.crossProduct(saZ, referenceNormal).normalize();
        this.saX = Vector3D.crossProduct(saY, saZ);

        this.dragCoeff               = dragCoeff;
        this.absorptionCoeff         = absorptionCoeff;
        this.specularReflectionCoeff = reflectionCoeff;
        this.diffuseReflectionCoeff  = 1 - (absorptionCoeff + reflectionCoeff);

    }

    /** {@inheritDoc} */
    @Override
    public ParameterDriver[] getDragParametersDrivers() {
        return dragParametersDrivers.clone();
    }

    /** {@inheritDoc} */
    @Override
    public ParameterDriver[] getRadiationParametersDrivers() {
        return radiationParametersDrivers.clone();
    }

    /** Get solar array normal in spacecraft frame.
     * @param date current date
     * @param frame inertial reference frame for state (both orbit and attitude)
     * @param position position of spacecraft in reference frame
     * @param rotation orientation (attitude) of the spacecraft with respect to reference frame
     * @return solar array normal in spacecraft frame
     * @exception OrekitException if sun direction cannot be computed in best lighting
     * configuration
     */
    public synchronized Vector3D getNormal(final AbsoluteDate date, final Frame frame,
                                           final Vector3D position, final Rotation rotation)
        throws OrekitException {

        if (referenceDate != null) {
            // use a simple rotation at fixed rate
            final double alpha = rotationRate * date.durationFrom(referenceDate);
            return new Vector3D(FastMath.cos(alpha), saX, FastMath.sin(alpha), saY);
        }

        // compute orientation for best lighting
        final Vector3D sunInert = sun.getPVCoordinates(date, frame).getPosition().subtract(position).normalize();
        final Vector3D sunSpacecraft = rotation.applyTo(sunInert);
        final double d = Vector3D.dotProduct(sunSpacecraft, saZ);
        final double f = 1 - d * d;
        if (f < Precision.EPSILON) {
            // extremely rare case: the sun is along solar array rotation axis
            // (there will not be much output power ...)
            // we set up an arbitrary normal
            return saZ.orthogonal();
        }

        final double s = 1.0 / FastMath.sqrt(f);
        return new Vector3D(s, sunSpacecraft, -s * d, saZ);

    }

    /** Get solar array normal in spacecraft frame.
     * @param date current date
     * @param frame inertial reference frame for state (both orbit and attitude)
     * @param position position of spacecraft in reference frame
     * @param rotation orientation (attitude) of the spacecraft with respect to reference frame
     * @return solar array normal in spacecraft frame
     * @param <T> extends RealFieldElement
     * @exception OrekitException if sun direction cannot be computed in best lighting
     * configuration
     */
    public synchronized <T extends RealFieldElement<T>> FieldVector3D<T> getNormal(final FieldAbsoluteDate<T> date, final Frame frame,
                                           final FieldVector3D<T> position, final FieldRotation<T> rotation)
        throws OrekitException {

        if (referenceDate != null) {
            // use a simple rotation at fixed rate
            final T alpha = date.durationFrom(referenceDate).multiply(rotationRate);
            return new FieldVector3D<T>(alpha.cos(), saX, alpha.sin(), saY);
        }

        // compute orientation for best lighting
        final FieldVector3D<T> sunInert = position.subtract(sun.getPVCoordinates(date.toAbsoluteDate(), frame).getPosition()).negate().normalize();
        final FieldVector3D<T> sunSpacecraft = rotation.applyTo(sunInert);
        final T d = FieldVector3D.dotProduct(sunSpacecraft, saZ);
        final T f = d.multiply(d).subtract(1).negate();
        if (f.getReal() < Precision.EPSILON) {
            // extremely rare case: the sun is along solar array rotation axis
            // (there will not be much output power ...)
            // we set up an arbitrary normal
            return new FieldVector3D<T>(f.getField().getOne(), saZ.orthogonal());
        }

        final T s = f.sqrt().reciprocal();
        return new FieldVector3D<T>(s, sunSpacecraft).subtract(new FieldVector3D<T>(s.multiply(d), saZ));

    }

    /** Get solar array normal in spacecraft frame.
     * @param date current date
     * @param frame inertial reference frame for state (both orbit and attitude)
     * @param position position of spacecraft in reference frame
     * @param rotation orientation (attitude) of the spacecraft with respect to reference frame
     * @return solar array normal in spacecraft frame
     * @exception OrekitException if sun direction cannot be computed in best lighting
     * configuration
     */
    public synchronized FieldVector3D<DerivativeStructure> getNormal(final AbsoluteDate date, final Frame frame,
                                                                     final FieldVector3D<DerivativeStructure> position,
                                                                     final FieldRotation<DerivativeStructure> rotation)
        throws OrekitException {

        final DerivativeStructure zero = position.getX().getField().getZero();

        if (referenceDate != null) {
            // use a simple rotation at fixed rate
            final DerivativeStructure alpha = zero.add(rotationRate * date.durationFrom(referenceDate));
            return new FieldVector3D<DerivativeStructure>(alpha.cos(), saX, alpha.sin(), saY);
        }

        // compute orientation for best lighting
        final FieldVector3D<DerivativeStructure> sunInert =
                position.subtract(sun.getPVCoordinates(date, frame).getPosition()).negate().normalize();
        final FieldVector3D<DerivativeStructure> sunSpacecraft = rotation.applyTo(sunInert);
        final DerivativeStructure d = FieldVector3D.dotProduct(sunSpacecraft, saZ);
        final DerivativeStructure f = d.multiply(d).subtract(1).negate();
        if (f.getValue() < Precision.EPSILON) {
            // extremely rare case: the sun is along solar array rotation axis
            // (there will not be much output power ...)
            // we set up an arbitrary normal
            return new FieldVector3D<DerivativeStructure>(position.getX().getField().getOne(), saZ.orthogonal());
        }

        final DerivativeStructure s = f.sqrt().reciprocal();
        return new FieldVector3D<DerivativeStructure>(s, sunSpacecraft).subtract(new FieldVector3D<DerivativeStructure>(s.multiply(d), saZ));

    }


    /** {@inheritDoc} */
    public Vector3D dragAcceleration(final AbsoluteDate date, final Frame frame, final Vector3D position,
                                     final Rotation rotation, final double mass,
                                     final double density, final Vector3D relativeVelocity)
        throws OrekitException {

        // relative velocity in spacecraft frame
        final Vector3D v = rotation.applyTo(relativeVelocity);

        // solar array contribution
        final Vector3D solarArrayFacet = new Vector3D(solarArrayArea, getNormal(date, frame, position, rotation));
        double sv = FastMath.abs(Vector3D.dotProduct(solarArrayFacet, v));

        // body facets contribution
        for (final Facet facet : facets) {
            final double dot = Vector3D.dotProduct(facet.getNormal(), v);
            if (dot < 0) {
                // the facet intercepts the incoming flux
                sv -= facet.getArea() * dot;
            }
        }

        return new Vector3D(sv * density * dragCoeff / (2.0 * mass), relativeVelocity);

    }

    /** {@inheritDoc} */
    public FieldVector3D<DerivativeStructure> dragAcceleration(final AbsoluteDate date, final Frame frame,
                                                               final FieldVector3D<DerivativeStructure> position,
                                                               final FieldRotation<DerivativeStructure> rotation,
                                                               final DerivativeStructure mass,
                                                               final DerivativeStructure density,
                                                               final FieldVector3D<DerivativeStructure> relativeVelocity)
        throws OrekitException {

        // relative velocity in spacecraft frame
        final FieldVector3D<DerivativeStructure> v = rotation.applyTo(relativeVelocity);

        // solar array contribution
        final FieldVector3D<DerivativeStructure> solarArrayFacet =
                new FieldVector3D<DerivativeStructure>(solarArrayArea, getNormal(date, frame, position, rotation));
        DerivativeStructure sv = FieldVector3D.dotProduct(v, solarArrayFacet).abs();

        // body facets contribution
        for (final Facet facet : facets) {
            final DerivativeStructure dot = FieldVector3D.dotProduct(v, facet.getNormal());
            if (dot.getValue() < 0) {
                // the facet intercepts the incoming flux
                sv = sv.subtract(dot.multiply(facet.getArea()));
            }
        }

        return new FieldVector3D<DerivativeStructure>(sv.multiply(density.multiply(dragCoeff / 2.0)).divide(mass),
                                                      relativeVelocity);

    }

    /** {@inheritDoc} */
    public FieldVector3D<DerivativeStructure> dragAcceleration(final AbsoluteDate date, final Frame frame,
                                                               final Vector3D position, final Rotation rotation,
                                                               final double mass, final  double density,
                                                               final Vector3D relativeVelocity,
                                                               final String paramName)
        throws OrekitException {

        if (!DRAG_COEFFICIENT.equals(paramName)) {
            throw new OrekitException(OrekitMessages.UNSUPPORTED_PARAMETER_NAME, paramName, DRAG_COEFFICIENT);
        }

        final DerivativeStructure dragCoeffDS = new DerivativeStructure(1, 1, 0, dragCoeff);

        // relative velocity in spacecraft frame
        final Vector3D v = rotation.applyTo(relativeVelocity);

        // solar array contribution
        final Vector3D solarArrayFacet = new Vector3D(solarArrayArea, getNormal(date, frame, position, rotation));
        double sv = FastMath.abs(Vector3D.dotProduct(solarArrayFacet, v));

        // body facets contribution
        for (final Facet facet : facets) {
            final double dot = Vector3D.dotProduct(facet.getNormal(), v);
            if (dot < 0) {
                // the facet intercepts the incoming flux
                sv -= facet.getArea() * dot;
            }
        }

        return new FieldVector3D<DerivativeStructure>(dragCoeffDS.multiply(sv * density / (2.0 * mass)),
                                                      relativeVelocity);

    }

    /** {@inheritDoc} */
    public Vector3D radiationPressureAcceleration(final AbsoluteDate date, final Frame frame, final Vector3D position,
                                                  final Rotation rotation, final double mass, final Vector3D flux)
        throws OrekitException {

        if (flux.getNormSq() < Precision.SAFE_MIN) {
            // null illumination (we are probably in umbra)
            return Vector3D.ZERO;
        }

        // radiation flux in spacecraft frame
        final Vector3D fluxSat = rotation.applyTo(flux);

        // solar array contribution
        Vector3D normal = getNormal(date, frame, position, rotation);
        double dot = Vector3D.dotProduct(normal, fluxSat);
        if (dot > 0) {
            // the solar array is illuminated backward,
            // fix signs to compute contribution correctly
            dot   = -dot;
            normal = normal.negate();
        }
        Vector3D force = facetRadiationAcceleration(normal, solarArrayArea, fluxSat, dot);

        // body facets contribution
        for (final Facet bodyFacet : facets) {
            normal = bodyFacet.getNormal();
            dot = Vector3D.dotProduct(normal, fluxSat);
            if (dot < 0) {
                // the facet intercepts the incoming flux
                force = force.add(facetRadiationAcceleration(normal, bodyFacet.getArea(), fluxSat, dot));
            }
        }

        // convert to inertial frame
        return rotation.applyInverseTo(new Vector3D(1.0 / mass, force));

    }

    /** {@inheritDoc} */
    public FieldVector3D<DerivativeStructure> radiationPressureAcceleration(final AbsoluteDate date, final Frame frame,
                                                                            final FieldVector3D<DerivativeStructure> position,
                                                                            final FieldRotation<DerivativeStructure> rotation,
                                                                            final DerivativeStructure mass,
                                                                            final FieldVector3D<DerivativeStructure> flux)
        throws OrekitException {

        if (flux.getNormSq().getValue() < Precision.SAFE_MIN) {
            // null illumination (we are probably in umbra)
            return new FieldVector3D<DerivativeStructure>(0.0, flux);
        }

        // radiation flux in spacecraft frame
        final FieldVector3D<DerivativeStructure> fluxSat = rotation.applyTo(flux);

        // solar array contribution
        FieldVector3D<DerivativeStructure> normal = getNormal(date, frame, position, rotation);
        DerivativeStructure dot = FieldVector3D.dotProduct(normal, fluxSat);
        if (dot.getValue() > 0) {
            // the solar array is illuminated backward,
            // fix signs to compute contribution correctly
            dot    = dot.negate();
            normal = normal.negate();
        }
        FieldVector3D<DerivativeStructure> force = facetRadiationAcceleration(normal, solarArrayArea, fluxSat, dot);

        // body facets contribution
        for (final Facet bodyFacet : facets) {
            normal = new FieldVector3D<DerivativeStructure>(mass.getField().getOne(), bodyFacet.getNormal());
            dot = FieldVector3D.dotProduct(normal, fluxSat);
            if (dot.getValue() < 0) {
                // the facet intercepts the incoming flux
                force = force.add(facetRadiationAcceleration(normal, bodyFacet.getArea(), fluxSat, dot));
            }
        }

        // convert to inertial frame
        return rotation.applyInverseTo(new FieldVector3D<DerivativeStructure>(mass.reciprocal(), force));

    }

    /** {@inheritDoc} */
    public FieldVector3D<DerivativeStructure> radiationPressureAcceleration(final AbsoluteDate date, final Frame frame,
                                                                            final Vector3D position, final Rotation rotation,
                                                                            final double mass, final Vector3D flux,
                                                                            final String paramName)
        throws OrekitException {

        if (flux.getNormSq() < Precision.SAFE_MIN) {
            // null illumination (we are probably in umbra)
            final DerivativeStructure zero = new DerivativeStructure(1, 1, 0.0);
            return new FieldVector3D<DerivativeStructure>(zero, zero, zero);
        }

        final DerivativeStructure absorptionCoeffDS;
        final DerivativeStructure specularReflectionCoeffDS;
        if (ABSORPTION_COEFFICIENT.equals(paramName)) {
            absorptionCoeffDS         = new DerivativeStructure(1, 1, 0, absorptionCoeff);
            specularReflectionCoeffDS = new DerivativeStructure(1, 1,    specularReflectionCoeff);
        } else if (REFLECTION_COEFFICIENT.equals(paramName)) {
            absorptionCoeffDS         = new DerivativeStructure(1, 1,    absorptionCoeff);
            specularReflectionCoeffDS = new DerivativeStructure(1, 1, 0, specularReflectionCoeff);
        } else {
            throw new OrekitException(OrekitMessages.UNSUPPORTED_PARAMETER_NAME, paramName,
                                      ABSORPTION_COEFFICIENT + ", " + REFLECTION_COEFFICIENT);
        }
        final DerivativeStructure diffuseReflectionCoeffDS =
                absorptionCoeffDS.add(specularReflectionCoeffDS).subtract(1).negate();


        // radiation flux in spacecraft frame
        final Vector3D fluxSat = rotation.applyTo(flux);

        // solar array contribution
        Vector3D normal = getNormal(date, frame, position, rotation);
        double dot = Vector3D.dotProduct(normal, fluxSat);
        if (dot > 0) {
            // the solar array is illuminated backward,
            // fix signs to compute contribution correctly
            dot   = -dot;
            normal = normal.negate();
        }
        FieldVector3D<DerivativeStructure> force =
                facetRadiationAcceleration(normal, solarArrayArea, fluxSat, dot,
                                           specularReflectionCoeffDS, diffuseReflectionCoeffDS);

        // body facets contribution
        for (final Facet bodyFacet : facets) {
            normal = bodyFacet.getNormal();
            dot = Vector3D.dotProduct(normal, fluxSat);
            if (dot < 0) {
                // the facet intercepts the incoming flux
                force = force.add(facetRadiationAcceleration(normal, bodyFacet.getArea(), fluxSat, dot,
                                                             specularReflectionCoeffDS, diffuseReflectionCoeffDS));
            }
        }

        // convert to inertial
        return FieldRotation.applyInverseTo(rotation, new FieldVector3D<DerivativeStructure>(1.0 / mass, force));

    }

    /** Compute contribution of one facet to force.
     * <p>This method implements equation 8-44 from David A. Vallado's
     * Fundamentals of Astrodynamics and Applications, third edition,
     * 2007, Microcosm Press.</p>
     * @param normal facet normal
     * @param area facet area
     * @param fluxSat radiation pressure flux in spacecraft frame
     * @param dot dot product of facet and fluxSat (must be negative)
     * @return contribution of the facet to force in spacecraft frame
     */
    private Vector3D facetRadiationAcceleration(final Vector3D normal, final double area, final Vector3D fluxSat,
                                                final double dot) {
        final double psr  = fluxSat.getNorm();

        // Vallado's equation 8-44 uses different parameters which are related to our parameters as:
        // cos (phi) = -dot / (psr * area)
        // n         = facet / area
        // s         = -fluxSat / psr
        final double cN = 2 * area * dot * (diffuseReflectionCoeff / 3 - specularReflectionCoeff * dot / psr);
        final double cS = (area * dot / psr) * (specularReflectionCoeff - 1);
        return new Vector3D(cN, normal, cS, fluxSat);

    }

    /** Compute contribution of one facet to force.
     * <p>This method implements equation 8-44 from David A. Vallado's
     * Fundamentals of Astrodynamics and Applications, third edition,
     * 2007, Microcosm Press.</p>
     * @param normal facet normal
     * @param area facet area
     * @param fluxSat radiation pressure flux in spacecraft frame
     * @param dot dot product of facet and fluxSat (must be negative)
     * @return contribution of the facet to force in spacecraft frame
     */
    private FieldVector3D<DerivativeStructure> facetRadiationAcceleration(final FieldVector3D<DerivativeStructure> normal,
                                                                          final double area,
                                                                          final FieldVector3D<DerivativeStructure> fluxSat,
                                                                          final DerivativeStructure dot) {
        final DerivativeStructure psr  = fluxSat.getNorm();

        // Vallado's equation 8-44 uses different parameters which are related to our parameters as:
        // cos (phi) = -dot / (psr * area)
        // n         = facet / area
        // s         = -fluxSat / psr
        final DerivativeStructure cN = dot.multiply(-2 * area).multiply(dot.divide(psr).multiply(specularReflectionCoeff).subtract(diffuseReflectionCoeff / 3));
        final DerivativeStructure cS = dot.divide(psr).multiply(area * (specularReflectionCoeff - 1));
        return new FieldVector3D<DerivativeStructure>(cN, normal, cS, fluxSat);

    }

    /** Compute contribution of one facet to force.
     * <p>This method implements equation 8-44 from David A. Vallado's
     * Fundamentals of Astrodynamics and Applications, third edition,
     * 2007, Microcosm Press.</p>
     * @param normal facet normal
     * @param area facet area
     * @param fluxSat radiation pressure flux in spacecraft frame
     * @param dot dot product of facet and fluxSat (must be negative)
     * @param specularReflectionCoeffDS specular reflection coefficient
     * @param diffuseReflectionCoeffDS diffuse reflection coefficient
     * @return contribution of the facet to force in spacecraft frame
     */
    private FieldVector3D<DerivativeStructure> facetRadiationAcceleration(final Vector3D normal, final double area,
                                                                          final Vector3D fluxSat, final double dot,
                                                                          final DerivativeStructure specularReflectionCoeffDS,
                                                                          final DerivativeStructure diffuseReflectionCoeffDS) {
        final double psr  = fluxSat.getNorm();

        // Vallado's equation 8-44 uses different parameters which are related to our parameters as:
        // cos (phi) = -dot / (psr * area)
        // n         = facet / area
        // s         = -fluxSat / psr
        final DerivativeStructure cN =
                diffuseReflectionCoeffDS.divide(3).subtract(specularReflectionCoeffDS.multiply(dot / psr)).multiply(2 * area * dot);
        final DerivativeStructure cS = specularReflectionCoeffDS.subtract(1).multiply(area * dot / psr);

        return new FieldVector3D<DerivativeStructure>(cN, normal, cS, fluxSat);

    }

    /** Class representing a single facet of a convex spacecraft body.
     * <p>Instance of this class are guaranteed to be immutable.</p>
     * @author Luc Maisonobe
     */
    public static class Facet {

        /** Unit Normal vector, pointing outward. */
        private final Vector3D normal;

        /** Area in m². */
        private final double area;

        /** Simple constructor.
         * @param normal vector normal to the facet, pointing outward (will be normalized)
         * @param area facet area in m²
         */
        public Facet(final Vector3D normal, final double area) {
            this.normal = normal.normalize();
            this.area   = area;
        }

        /** Get unit normal vector.
         * @return unit normal vector
         */
        public Vector3D getNormal() {
            return normal;
        }

        /** Get facet area.
         * @return facet area
         */
        public double getArea() {
            return area;
        }

    }

    /** Build the surface vectors for body facets of a simple parallelepipedic box.
     * @param xLength length of the body along its X axis (m)
     * @param yLength length of the body along its Y axis (m)
     * @param zLength length of the body along its Z axis (m)
     * @return surface vectors array
     */
    private static Facet[] simpleBoxFacets(final double xLength, final double yLength, final double zLength) {
        return new Facet[] {
            new Facet(Vector3D.MINUS_I, yLength * zLength),
            new Facet(Vector3D.PLUS_I,  yLength * zLength),
            new Facet(Vector3D.MINUS_J, xLength * zLength),
            new Facet(Vector3D.PLUS_J,  xLength * zLength),
            new Facet(Vector3D.MINUS_K, xLength * yLength),
            new Facet(Vector3D.PLUS_K,  xLength * yLength)
        };
    }

    /** Filter out zero area facets.
     * @param facets original facets (may include zero area facets)
     * @return filtered array
     */
    private static List<Facet> filter(final Facet[] facets) {
        final List<Facet> filtered = new ArrayList<Facet>(facets.length);
        for (Facet facet : facets) {
            if (facet.getArea() > 0) {
                filtered.add(facet);
            }
        }
        return filtered;
    }

    @Override
    public <T extends RealFieldElement<T>> FieldVector3D<T>
        dragAcceleration(final FieldAbsoluteDate<T> date, final Frame frame,
                         final FieldVector3D<T> position, final FieldRotation<T> rotation,
                         final T mass, final  T density, final FieldVector3D<T> relativeVelocity)
            throws OrekitException {
        // relative velocity in spacecraft frame
        final FieldVector3D<T> v = rotation.applyTo(relativeVelocity);

        // solar array contribution
        final FieldVector3D<T> solarArrayFacet = new FieldVector3D<T>(solarArrayArea, getNormal(date, frame, position, rotation));
        T sv = FieldVector3D.dotProduct(solarArrayFacet, v).abs();

        // body facets contribution
        for (final Facet facet : facets) {
            final T dot = FieldVector3D.dotProduct(facet.getNormal(), v);
            if (dot.getReal() < 0) {
                // the facet intercepts the incoming flux
                sv = sv.subtract(dot.multiply(facet.getArea()));
            }
        }

        return new FieldVector3D<T>(sv.multiply(density).multiply(dragCoeff / 2.0).divide(mass), relativeVelocity);

    }

    @Override
    public <T extends RealFieldElement<T>> FieldVector3D<T>
        radiationPressureAcceleration(final FieldAbsoluteDate<T> date, final Frame frame,
                                      final FieldVector3D<T> position,
                                      final FieldRotation<T> rotation, final T mass,
                                      final FieldVector3D<T> flux)
            throws OrekitException {
        // TODO: field implementation
        throw new UnsupportedOperationException();
    }

}
