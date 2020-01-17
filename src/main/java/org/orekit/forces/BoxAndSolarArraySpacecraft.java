/* Copyright 2002-2020 CS Group
 * Licensed to CS Group (CS) under one or more
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

import org.hipparchus.Field;
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
import org.orekit.forces.drag.DragSensitive;
import org.orekit.forces.radiation.RadiationSensitive;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.ParameterDriver;

/** Class representing the features of a classical satellite
 * with a convex body shape and rotating flat solar arrays.
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
 * The lift component of the drag force can be optionally considered. It should
 * probably only be used for reentry computation, with much denser atmosphere
 * than in regular orbit propagation. The lift component is computed using a
 * ratio of molecules that experience specular reflection instead of diffuse
 * reflection (absorption followed by outgassing at negligible velocity).
 * Without lift (i.e. when the lift ratio is set to 0), drag force is along
 * atmosphere relative velocity. With lift (i.e. when the lift ratio is set to any
 * value between 0 and 1), the drag force depends on both relative velocity direction
 * and facets normal orientation. For a single panel, if the relative velocity is
 * head-on (i.e. aligned with the panel normal), the force will be in the same
 * direction with and without lift, but the magnitude with lift ratio set to 1.0 will
 * be twice the magnitude with lift ratio set to 0.0 (because atmosphere molecules
 * bounces backward at same velocity in case of specular reflection).
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

    /** Driver for drag coefficient parameter. */
    private final ParameterDriver dragParameterDriver;

    /** Driver for lift ratio parameter (may be null is lift is ignored). */
    private final ParameterDriver liftParameterDriver;

    /** Driver for radiation pressure absorption coefficient parameter. */
    private final ParameterDriver absorptionParameterDriver;

    /** Driver for radiation pressure reflection coefficient parameter. */
    private final ParameterDriver reflectionParameterDriver;

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

    /** Sun model. */
    private final PVCoordinatesProvider sun;

    /** Build a spacecraft model with best lighting of solar array.
     * <p>
     * This constructor builds an instance that completely ignores lift
     * in atmospheric drag (the value of lift coefficient is set to zero,
     * and there are no {@link ParameterDriver drivers} to change it).
     * </p>
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
             dragCoeff, 0.0, false,
             absorptionCoeff, reflectionCoeff);
    }

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
     * @param liftRatio lift ratio (proportion between 0 and 1 of atmosphere modecules
     * that will experience specular reflection when hitting spacecraft instead
     * of experiencing diffuse reflection, hence producing lift)
     * @param absorptionCoeff absorption coefficient between 0.0 an 1.0
     * (used only for radiation pressure)
     * @param reflectionCoeff specular reflection coefficient between 0.0 an 1.0
     * (used only for radiation pressure)
     * @since 9.0
     */
    public BoxAndSolarArraySpacecraft(final double xLength, final double yLength,
                                      final double zLength,
                                      final PVCoordinatesProvider sun, final double solarArrayArea,
                                      final Vector3D solarArrayAxis,
                                      final double dragCoeff, final double liftRatio,
                                      final double absorptionCoeff,
                                      final double reflectionCoeff) {
        this(simpleBoxFacets(xLength, yLength, zLength), sun, solarArrayArea, solarArrayAxis,
             dragCoeff, liftRatio,
             absorptionCoeff, reflectionCoeff);
    }

    /** Build a spacecraft model with best lighting of solar array.
     * <p>
     * The spacecraft body is described by an array of surface vectors. Each facet of
     * the body is described by a vector normal to the facet (pointing outward of the spacecraft)
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
        this(facets, sun, solarArrayArea, solarArrayAxis,
             dragCoeff, 0.0, false,
             absorptionCoeff, reflectionCoeff);
    }

    /** Build a spacecraft model with best lighting of solar array.
     * <p>
     * The spacecraft body is described by an array of surface vectors. Each facet of
     * the body is described by a vector normal to the facet (pointing outward of the spacecraft)
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
     * @param liftRatio lift ratio (proportion between 0 and 1 of atmosphere modecules
     * that will experience specular reflection when hitting spacecraft instead
     * of experiencing diffuse reflection, hence producing lift)
     * @param absorptionCoeff absorption coefficient between 0.0 an 1.0
     * (used only for radiation pressure)
     * @param reflectionCoeff specular reflection coefficient between 0.0 an 1.0
     * (used only for radiation pressure)
     * @since 9.0
     */
    public BoxAndSolarArraySpacecraft(final Facet[] facets,
                                      final PVCoordinatesProvider sun, final double solarArrayArea,
                                      final Vector3D solarArrayAxis,
                                      final double dragCoeff, final double liftRatio,
                                      final double absorptionCoeff,
                                      final double reflectionCoeff) {
        this(facets, sun, solarArrayArea, solarArrayAxis,
             dragCoeff, liftRatio, true,
             absorptionCoeff, reflectionCoeff);
    }

    /** Build a spacecraft model with best lighting of solar array.
     * <p>
     * The spacecraft body is described by an array of surface vectors. Each facet of
     * the body is described by a vector normal to the facet (pointing outward of the spacecraft)
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
     * @param liftRatio lift ratio (proportion between 0 and 1 of atmosphere modecules
     * that will experience specular reflection when hitting spacecraft instead
     * of experiencing diffuse reflection, hence producing lift)
     * @param useLift if true, lift should be used, otherwise it is completely ignored
     * @param absorptionCoeff absorption coefficient between 0.0 an 1.0
     * (used only for radiation pressure)
     * @param reflectionCoeff specular reflection coefficient between 0.0 an 1.0
     * (used only for radiation pressure)
     * @since 9.0
     */
    private BoxAndSolarArraySpacecraft(final Facet[] facets,
                                       final PVCoordinatesProvider sun, final double solarArrayArea,
                                       final Vector3D solarArrayAxis,
                                       final double dragCoeff, final double liftRatio, final boolean useLift,
                                       final double absorptionCoeff,
                                       final double reflectionCoeff) {

        // drag
        dragParameterDriver = buildDragParameterDriver(dragCoeff);
        liftParameterDriver = useLift ? buildLiftParameterDriver(liftRatio) : null;

        // radiation pressure
        absorptionParameterDriver = buildAbsorptionParameterDriver(absorptionCoeff);
        reflectionParameterDriver = buildReflectionParameterDriver(reflectionCoeff);

        this.facets = filter(facets);

        this.sun            = sun;
        this.solarArrayArea = solarArrayArea;
        this.referenceDate  = null;
        this.rotationRate   = 0;

        this.saZ = solarArrayAxis.normalize();
        this.saY = null;
        this.saX = null;

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
             dragCoeff, 0.0, false,
             absorptionCoeff, reflectionCoeff);
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
     * @param liftRatio lift ratio (proportion between 0 and 1 of atmosphere modecules
     * that will experience specular reflection when hitting spacecraft instead
     * of experiencing diffuse reflection, hence producing lift)
     * @param absorptionCoeff absorption coefficient between 0.0 an 1.0
     * (used only for radiation pressure)
     * @param reflectionCoeff specular reflection coefficient between 0.0 an 1.0
     * (used only for radiation pressure)
     * @since 9.0
     */
    public BoxAndSolarArraySpacecraft(final double xLength, final double yLength,
                                      final double zLength,
                                      final PVCoordinatesProvider sun, final double solarArrayArea,
                                      final Vector3D solarArrayAxis,
                                      final AbsoluteDate referenceDate,
                                      final Vector3D referenceNormal,
                                      final double rotationRate,
                                      final double dragCoeff, final double liftRatio,
                                      final double absorptionCoeff,
                                      final double reflectionCoeff) {
        this(simpleBoxFacets(xLength, yLength, zLength), sun, solarArrayArea, solarArrayAxis,
             referenceDate, referenceNormal, rotationRate,
             dragCoeff, liftRatio, true,
             absorptionCoeff, reflectionCoeff);
    }

    /** Build a spacecraft model with linear rotation of solar array.
     * <p>
     * The spacecraft body is described by an array of surface vectors. Each facet of
     * the body is described by a vector normal to the facet (pointing outward of the spacecraft)
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
        this(facets, sun, solarArrayArea, solarArrayAxis, referenceDate, referenceNormal, rotationRate,
             dragCoeff, 0.0, false,
             absorptionCoeff, reflectionCoeff);
    }

    /** Build a spacecraft model with linear rotation of solar array.
     * <p>
     * The spacecraft body is described by an array of surface vectors. Each facet of
     * the body is described by a vector normal to the facet (pointing outward of the spacecraft)
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
     * @param liftRatio lift ratio (proportion between 0 and 1 of atmosphere modecules
     * that will experience specular reflection when hitting spacecraft instead
     * of experiencing diffuse reflection, hence producing lift)
     * @param absorptionCoeff absorption coefficient between 0.0 an 1.0
     * (used only for radiation pressure)
     * @param reflectionCoeff specular reflection coefficient between 0.0 an 1.0
     * (used only for radiation pressure)
     * @since 9.0
     */
    public BoxAndSolarArraySpacecraft(final Facet[] facets,
                                      final PVCoordinatesProvider sun, final double solarArrayArea,
                                      final Vector3D solarArrayAxis,
                                      final AbsoluteDate referenceDate,
                                      final Vector3D referenceNormal,
                                      final double rotationRate,
                                      final double dragCoeff, final double liftRatio,
                                      final double absorptionCoeff,
                                      final double reflectionCoeff) {
        this(facets, sun, solarArrayArea, solarArrayAxis, referenceDate, referenceNormal, rotationRate,
             dragCoeff, liftRatio, true,
             absorptionCoeff, reflectionCoeff);
    }

    /** Build a spacecraft model with linear rotation of solar array.
     * <p>
     * The spacecraft body is described by an array of surface vectors. Each facet of
     * the body is described by a vector normal to the facet (pointing outward of the spacecraft)
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
     * @param liftRatio lift ratio (proportion between 0 and 1 of atmosphere modecules
     * that will experience specular reflection when hitting spacecraft instead
     * of experiencing diffuse reflection, hence producing lift)
     * @param useLift if true, lift should be used, otherwise it is completely ignored
     * @param absorptionCoeff absorption coefficient between 0.0 an 1.0
     * (used only for radiation pressure)
     * @param reflectionCoeff specular reflection coefficient between 0.0 an 1.0
     * (used only for radiation pressure)
     * @since 9.0
     */
    private BoxAndSolarArraySpacecraft(final Facet[] facets,
                                       final PVCoordinatesProvider sun, final double solarArrayArea,
                                       final Vector3D solarArrayAxis,
                                       final AbsoluteDate referenceDate,
                                       final Vector3D referenceNormal,
                                       final double rotationRate,
                                       final double dragCoeff, final double liftRatio, final boolean useLift,
                                       final double absorptionCoeff,
                                       final double reflectionCoeff) {

        // drag
        dragParameterDriver = buildDragParameterDriver(dragCoeff);
        liftParameterDriver = useLift ? buildLiftParameterDriver(liftRatio) : null;

        // radiation pressure
        absorptionParameterDriver = buildAbsorptionParameterDriver(absorptionCoeff);
        reflectionParameterDriver = buildReflectionParameterDriver(reflectionCoeff);

        this.facets = filter(facets.clone());

        this.sun            = sun;
        this.solarArrayArea = solarArrayArea;
        this.referenceDate  = referenceDate;
        this.rotationRate   = rotationRate;

        this.saZ = solarArrayAxis.normalize();
        this.saY = Vector3D.crossProduct(saZ, referenceNormal).normalize();
        this.saX = Vector3D.crossProduct(saY, saZ);

    }

    /** Build the parameter driver for drag coefficient.
     * @param coeff drag coefficient
     * @return parameter driver for drag coefficient
     * @since 9.0
     */
    private ParameterDriver buildDragParameterDriver(final double coeff) {
        try {
            return new ParameterDriver(DragSensitive.DRAG_COEFFICIENT,
                                       coeff, SCALE, 0.0, Double.POSITIVE_INFINITY);
        } catch (OrekitException oe) {
            // this should never happen
            throw new OrekitInternalError(oe);
        }
    }

    /** Build the parameter driver for lift coefficient.
     * @param coeff lift coefficient
     * @return parameter driver for lift coefficient
     * @since 9.0
     */
    private ParameterDriver buildLiftParameterDriver(final double coeff) {
        try {
            return new ParameterDriver(DragSensitive.LIFT_RATIO, coeff, SCALE, 0.0, 1.0);
        } catch (OrekitException oe) {
            // this should never happen
            throw new OrekitInternalError(oe);
        }
    }

    /** Build the parameter driver for absorption coefficient.
     * @param coeff absorption coefficient
     * @return parameter driver for absorption coefficient
     * @since 9.0
     */
    private ParameterDriver buildAbsorptionParameterDriver(final double coeff) {
        try {
            return new ParameterDriver(RadiationSensitive.ABSORPTION_COEFFICIENT, coeff, SCALE, 0.0, 1.0);
        } catch (OrekitException oe) {
            // this should never happen
            throw new OrekitInternalError(oe);
        }
    }

    /** Build the parameter driver for reflection coefficient.
     * @param coeff absorption coefficient
     * @return parameter driver for reflection coefficient
     * @since 9.0
     */
    private ParameterDriver buildReflectionParameterDriver(final double coeff) {
        try {
            return new ParameterDriver(RadiationSensitive.REFLECTION_COEFFICIENT, coeff, SCALE, 0.0, 1.0);
        } catch (OrekitException oe) {
            // this should never happen
            throw new OrekitInternalError(oe);
        }
    }

    /** {@inheritDoc} */
    @Override
    public ParameterDriver[] getDragParametersDrivers() {
        return liftParameterDriver == null ?
               new ParameterDriver[] {
                   dragParameterDriver
               } : new ParameterDriver[] {
                   dragParameterDriver, liftParameterDriver
               };
    }

    /** {@inheritDoc} */
    @Override
    public ParameterDriver[] getRadiationParametersDrivers() {
        return new ParameterDriver[] {
            absorptionParameterDriver, reflectionParameterDriver
        };
    }

    /** Get solar array normal in spacecraft frame.
     * @param date current date
     * @param frame inertial reference frame for state (both orbit and attitude)
     * @param position position of spacecraft in reference frame
     * @param rotation orientation (attitude) of the spacecraft with respect to reference frame
     * @return solar array normal in spacecraft frame
     */
    public synchronized Vector3D getNormal(final AbsoluteDate date, final Frame frame,
                                           final Vector3D position, final Rotation rotation) {

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
     * @param <T> type of the field elements
     */
    public synchronized <T extends RealFieldElement<T>> FieldVector3D<T> getNormal(final FieldAbsoluteDate<T> date,
                                                                                   final Frame frame,
                                                                                   final FieldVector3D<T> position,
                                                                                   final FieldRotation<T> rotation) {

        if (referenceDate != null) {
            // use a simple rotation at fixed rate
            final T alpha = date.durationFrom(referenceDate).multiply(rotationRate);
            return new FieldVector3D<>(alpha.cos(), saX, alpha.sin(), saY);
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
            return new FieldVector3D<>(f.getField(), saZ.orthogonal());
        }

        final T s = f.sqrt().reciprocal();
        return new FieldVector3D<>(s, sunSpacecraft,
                                   s.multiply(d).negate(), new FieldVector3D<>(date.getField(), saZ));

    }

    /** Get solar array normal in spacecraft frame.
     * @param date current date
     * @param frame inertial reference frame for state (both orbit and attitude)
     * @param position position of spacecraft in reference frame
     * @param rotation orientation (attitude) of the spacecraft with respect to reference frame
     * @return solar array normal in spacecraft frame
     * @deprecated Method not used anymore, should have been deleted in 9.0 but was left over. To be deleted in the next major version.
     */
    @Deprecated
    public synchronized FieldVector3D<DerivativeStructure> getNormal(final AbsoluteDate date, final Frame frame,
                                                                     final FieldVector3D<DerivativeStructure> position,
                                                                     final FieldRotation<DerivativeStructure> rotation) {

        final DerivativeStructure zero = position.getX().getField().getZero();

        if (referenceDate != null) {
            // use a simple rotation at fixed rate
            final DerivativeStructure alpha = zero.add(rotationRate * date.durationFrom(referenceDate));
            return new FieldVector3D<>(alpha.cos(), saX, alpha.sin(), saY);
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
            return new FieldVector3D<>(position.getX().getField(), saZ.orthogonal());
        }

        final DerivativeStructure s = f.sqrt().reciprocal();
        return new FieldVector3D<>(s, sunSpacecraft,
                                   s.multiply(d).negate(), new FieldVector3D<>(zero.getField(), saZ));

    }


    /** {@inheritDoc} */
    @Override
    public Vector3D dragAcceleration(final AbsoluteDate date, final Frame frame, final Vector3D position,
                                     final Rotation rotation, final double mass,
                                     final double density, final Vector3D relativeVelocity,
                                     final double[] parameters) {

        final double dragCoeff = parameters[0];
        final double liftRatio = liftParameterDriver == null ? 0.0 : parameters[1];

        // relative velocity in spacecraft frame
        final double   vNorm2 = relativeVelocity.getNormSq();
        final double   vNorm  = FastMath.sqrt(vNorm2);
        final Vector3D vDir   = rotation.applyTo(relativeVelocity.scalarMultiply(1.0 / vNorm));
        final double   coeff  = density * dragCoeff * vNorm2 / (2.0 * mass);
        final double   oMr    = 1 - liftRatio;

        // solar array contribution
        final Vector3D frontNormal = getNormal(date, frame, position, rotation);
        final double   s           = coeff * solarArrayArea * Vector3D.dotProduct(frontNormal, vDir);
        Vector3D acceleration = new Vector3D(oMr * FastMath.abs(s), vDir,
                                             liftRatio * s * 2,     frontNormal);

        // body facets contribution
        for (final Facet facet : facets) {
            final double dot = Vector3D.dotProduct(facet.getNormal(), vDir);
            if (dot < 0) {
                // the facet intercepts the incoming flux
                final double f = coeff * facet.getArea() * dot;
                acceleration = new Vector3D(1,                     acceleration,
                                            oMr * FastMath.abs(f), vDir,
                                            liftRatio * f * 2,     facet.getNormal());
            }
        }

        // convert back to inertial frame
        return rotation.applyInverseTo(acceleration);

    }

    /** {@inheritDoc} */
    @Override
    public Vector3D radiationPressureAcceleration(final AbsoluteDate date, final Frame frame, final Vector3D position,
                                                  final Rotation rotation, final double mass, final Vector3D flux,
                                                  final double[] parameters) {

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
        Vector3D force = facetRadiationAcceleration(normal, solarArrayArea, fluxSat, dot, parameters);

        // body facets contribution
        for (final Facet bodyFacet : facets) {
            normal = bodyFacet.getNormal();
            dot = Vector3D.dotProduct(normal, fluxSat);
            if (dot < 0) {
                // the facet intercepts the incoming flux
                force = force.add(facetRadiationAcceleration(normal, bodyFacet.getArea(), fluxSat, dot, parameters));
            }
        }

        // convert to inertial frame
        return rotation.applyInverseTo(new Vector3D(1.0 / mass, force));

    }

    /** {@inheritDoc} */
    @Override
    public <T extends RealFieldElement<T>> FieldVector3D<T>
        dragAcceleration(final FieldAbsoluteDate<T> date, final Frame frame,
                         final FieldVector3D<T> position, final FieldRotation<T> rotation,
                         final T mass, final  T density, final FieldVector3D<T> relativeVelocity,
                         final T[] parameters) {

        final T dragCoeff = parameters[0];
        final T liftRatio = liftParameterDriver == null ? dragCoeff.getField().getZero() : parameters[1];

        // relative velocity in spacecraft frame
        final T                vNorm2 = relativeVelocity.getNormSq();
        final T                vNorm  = vNorm2.sqrt();
        final FieldVector3D<T> vDir   = rotation.applyTo(relativeVelocity.scalarMultiply(vNorm.reciprocal()));
        final T                coeff  = density.multiply(0.5).multiply(dragCoeff).multiply(vNorm2).divide(mass);
        final T                oMr    = liftRatio.negate().add(1);

        // solar array facet contribution
        final FieldVector3D<T> frontNormal = getNormal(date, frame, position, rotation);
        final T                s           = coeff.
                                             multiply(solarArrayArea).
                                             multiply(FieldVector3D.dotProduct(frontNormal, vDir));
        FieldVector3D<T> acceleration = new FieldVector3D<>(s.abs().multiply(oMr), vDir,
                                                            s.multiply(liftRatio).multiply(2), frontNormal);

        // body facets contribution
        final Field<T> field = coeff.getField();
        for (final Facet facet : facets) {
            final T dot = FieldVector3D.dotProduct(facet.getNormal(), vDir);
            if (dot.getReal() < 0) {
                // the facet intercepts the incoming flux
                final T f = coeff.multiply(facet.getArea()).multiply(dot);
                acceleration = new FieldVector3D<>(field.getOne(),        acceleration,
                                                   f.abs().multiply(oMr), vDir,
                                                   f.multiply(liftRatio).multiply(2), new FieldVector3D<>(field, facet.getNormal()));
            }
        }

        // convert back to inertial frame
        return rotation.applyInverseTo(acceleration);

    }

    /** {@inheritDoc} */
    @Override
    public <T extends RealFieldElement<T>> FieldVector3D<T>
        radiationPressureAcceleration(final FieldAbsoluteDate<T> date, final Frame frame,
                                      final FieldVector3D<T> position,
                                      final FieldRotation<T> rotation, final T mass,
                                      final FieldVector3D<T> flux,
                                      final T[] parameters) {

        if (flux.getNormSq().getReal() < Precision.SAFE_MIN) {
            // null illumination (we are probably in umbra)
            return FieldVector3D.getZero(date.getField());
        }

        // radiation flux in spacecraft frame
        final FieldVector3D<T> fluxSat = rotation.applyTo(flux);

        // solar array contribution
        FieldVector3D<T> normal = getNormal(date, frame, position, rotation);
        T dot = FieldVector3D.dotProduct(normal, fluxSat);
        if (dot.getReal() > 0) {
            // the solar array is illuminated backward,
            // fix signs to compute contribution correctly
            dot    = dot.negate();
            normal = normal.negate();
        }
        FieldVector3D<T> force = facetRadiationAcceleration(normal, solarArrayArea, fluxSat, dot, parameters);

        // body facets contribution
        for (final Facet bodyFacet : facets) {
            normal = new FieldVector3D<>(date.getField(), bodyFacet.getNormal());
            dot = FieldVector3D.dotProduct(fluxSat, normal);
            if (dot.getReal() < 0) {
                // the facet intercepts the incoming flux
                force = force.add(facetRadiationAcceleration(normal, bodyFacet.getArea(), fluxSat, dot, parameters));
            }
        }

        // convert to inertial frame
        return rotation.applyInverseTo(new FieldVector3D<>(mass.reciprocal(), force));

    }

    /** Compute contribution of one facet to force.
     * <p>This method implements equation 8-44 from David A. Vallado's
     * Fundamentals of Astrodynamics and Applications, third edition,
     * 2007, Microcosm Press.</p>
     * @param normal facet normal
     * @param area facet area
     * @param fluxSat radiation pressure flux in spacecraft frame
     * @param dot dot product of facet and fluxSat (must be negative)
     * @param parameters values of the force model parameters
     * @return contribution of the facet to force in spacecraft frame
     */
    private Vector3D facetRadiationAcceleration(final Vector3D normal, final double area, final Vector3D fluxSat,
                                                final double dot, final double[] parameters) {

        final double absorptionCoeff         = parameters[0];
        final double specularReflectionCoeff = parameters[1];
        final double diffuseReflectionCoeff  = 1 - (absorptionCoeff + specularReflectionCoeff);

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
     * @param parameters values of the force model parameters
     * @param <T> type of the field elements
     * @return contribution of the facet to force in spacecraft frame
     */
    private <T extends RealFieldElement<T>> FieldVector3D<T>
        facetRadiationAcceleration(final FieldVector3D<T> normal, final double area, final FieldVector3D<T> fluxSat,
                                   final T dot, final T[] parameters) {

        final T absorptionCoeff         = parameters[0];
        final T specularReflectionCoeff = parameters[1];
        final T diffuseReflectionCoeff  = absorptionCoeff.add(specularReflectionCoeff).negate().add(1);

        final T psr  = fluxSat.getNorm();

        // Vallado's equation 8-44 uses different parameters which are related to our parameters as:
        // cos (phi) = -dot / (psr * area)
        // n         = facet / area
        // s         = -fluxSat / psr
        final T cN = dot.multiply(-2 * area).multiply(dot.multiply(specularReflectionCoeff).divide(psr).subtract(diffuseReflectionCoeff.divide(3)));
        final T cS = dot.multiply(area).multiply(specularReflectionCoeff.subtract(1)).divide(psr);
        return new FieldVector3D<>(cN, normal, cS, fluxSat);

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

}
