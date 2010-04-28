/* Copyright 2002-2010 CS Communication & Systèmes
 * Licensed to CS Communication & Systèmes (CS) under one or more
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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math.geometry.Rotation;
import org.apache.commons.math.geometry.Vector3D;
import org.apache.commons.math.util.MathUtils;
import org.orekit.errors.OrekitException;
import org.orekit.forces.drag.DragSensitive;
import org.orekit.forces.radiation.RadiationSensitive;
import org.orekit.frames.Frame;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinatesProvider;

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
 * the best lightning orientation (i.e. Sun exactly in solar array meridian
 * plane defined by solar array rotation axis and solar array normal vector)
 * or a rotation evolving linearly according to a start position and an
 * angular rate (which can be set to 0 for non-rotating panels, which may
 * occur in special modes or during contingencies).
 * </p>
 * <p>
 * This model does not take cast shadow between body and solar array into account.
 * </p>
 * <p>
 * Instances of this class are guaranteed to be immutable.
 * </p>
 *
 * @see SphericalSpacecraft
 * @author Luc Maisonobe
 * @author Pascal Parraud
 * @version $Revision$ $Date$
 */
public class BoxAndSolarArraySpacecraft implements RadiationSensitive, DragSensitive {

    /** Serializable UID. */
    private static final long serialVersionUID = -4426844682371384944L;

    /** Surface vectors for body facets. */
    private final List<Facet> facets;

    /** Solar array area (m<sup>2</sup>). */
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

    /** Build a spacecraft model with best lightning of solar array.
     * <p>
     * Solar arrays orientation will be such that at each time the Sun direction
     * will always be in the solar array meridian plane defined by solar array
     * rotation axis and solar array normal vector.
     * </p>
     * @param xLength length of the body along its X axis (m)
     * @param yLength length of the body along its Y axis (m)
     * @param zLength length of the body along its Z axis (m)
     * @param sun sun model
     * @param solarArrayArea area of the solar array (m<sup>2</sup>)
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

    /** Build a spacecraft model with best lightning of solar array.
     * <p>
     * The spacecraft body is described by an array of surface vectors. Each facet of
     * the body is describe by a vector normal to the facet (pointing outward of the spacecraft)
     * and whose norm is the surface area in m<sup>2</sup>.
     * </p>
     * <p>
     * Solar arrays orientation will be such that at each time the Sun direction
     * will always be in the solar array meridian plane defined by solar array
     * rotation axis and solar array normal vector.
     * </p>
     * @param facets body facets (only the facets with strictly positive area will be stored)
     * @param sun sun model
     * @param solarArrayArea area of the solar array (m<sup>2</sup>)
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
     * @param solarArrayArea area of the solar array (m<sup>2</sup>)
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
     * and whose norm is the surface area in m<sup>2</sup>.
     * </p>
     * <p>
     * Solar arrays orientation will be a regular rotation from the
     * reference orientation at reference date and using a constant
     * rotation rate.
     * </p>
     * @param facets body facets (only the facets with strictly positive area will be stored)
     * @param sun sun model
     * @param solarArrayArea area of the solar array (m<sup>2</sup>)
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

    /** Get solar array normal in spacecraft frame.
     * @param state current state information: date, kinematics, attitude
     * @return solar array normal in spacecraft frame
     * @exception OrekitException if sun direction cannot be computed in best lightning
     * configuration
     */
    public synchronized Vector3D getNormal(final SpacecraftState state)
        throws OrekitException {

        final AbsoluteDate date = state.getDate();

        if (referenceDate != null) {
            // use a simple rotation at fixed rate
            final double alpha = rotationRate * date.durationFrom(referenceDate);
            return new Vector3D(Math.cos(alpha), saX, Math.sin(alpha), saY);
        }

        // compute orientation for best lightning
        final Frame frame = state.getFrame();
        final Vector3D sunInert = sun.getPVCoordinates(date, frame).getPosition().normalize();
        final Vector3D sunSpacecraft = state.getAttitude().getRotation().applyTo(sunInert);
        final double d = Vector3D.dotProduct(sunSpacecraft, saZ);
        final double f = 1 - d * d;
        if (f < MathUtils.EPSILON) {
            // extremely rare case: the sun is along solar array rotation axis
            // (there will not be much output power ...)
            // we set up an arbitrary normal
            return saZ.orthogonal();
        }

        final double s = 1.0 / Math.sqrt(f);
        return new Vector3D(s, sunSpacecraft, -s * d, saZ);

    }


    /** {@inheritDoc} */
    public Vector3D dragAcceleration(final SpacecraftState state, final double density,
                                     final Vector3D relativeVelocity)
        throws OrekitException {

        // relative velocity in spacecraft frame
        final Vector3D v = state.getAttitude().getRotation().applyTo(relativeVelocity);

        // solar array contribution
        final Vector3D solarArrayFacet = new Vector3D(solarArrayArea, getNormal(state));
        double sv = Math.abs(Vector3D.dotProduct(solarArrayFacet, v));

        // body facets contribution
        for (final Facet facet : facets) {
            final double dot = Vector3D.dotProduct(facet.getNormal(), v);
            if (dot < 0) {
                // the facet intercepts the incoming flux
                sv -= facet.getArea() * dot;
            }
        }

        return new Vector3D(density * sv * dragCoeff / (2.0 * state.getMass()), relativeVelocity);

    }

    /** {@inheritDoc} */
    public Vector3D radiationPressureAcceleration(final SpacecraftState state, final Vector3D flux)
        throws OrekitException {

        // radiation flux in spacecraft frame
        final Rotation r = state.getAttitude().getRotation();
        final Vector3D fluxSat = r.applyTo(flux);

        // solar array contribution
        Facet facet = new Facet(getNormal(state), solarArrayArea);
        double dot = Vector3D.dotProduct(facet.getNormal(), fluxSat);
        if (dot > 0) {
            // the solar array is illuminated backward,
            // fix signs to compute contribution correctly
            dot   = -dot;
            facet = new Facet(facet.getNormal().negate(), solarArrayArea);
        }
        Vector3D force = facetRadiationAcceleration(facet, fluxSat, dot);

        // body facets contribution
        for (final Facet bodyFacet : facets) {
            dot = Vector3D.dotProduct(bodyFacet.getNormal(), fluxSat);
            if (dot < 0) {
                // the facet intercepts the incoming flux
                force = force.add(facetRadiationAcceleration(bodyFacet, fluxSat, dot));
            }
        }

        // convert to inertial frame
        return r.applyInverseTo(new Vector3D(1.0 / state.getMass(), force));

    }

    /** Compute contribution of one facet to force.
     * <p>This method implements equation 8-44 from David A. Vallado's
     * Fundamentals of Astrodynamics and Applications, third edition,
     * 2007, Microcosm Press.</p>
     * @param facet facet definition
     * @param fluxSat radiation pressure flux in spacecraft frame
     * @param dot dot product of facet and fluxSat (must be negative)
     * @return contribution of the facet to force in spacecraft frame
     */
    private Vector3D facetRadiationAcceleration(final Facet facet, final Vector3D fluxSat,
                                                final double dot) {
        final double area = facet.getArea();
        final double dOa  = dot / area;
        final double psr  = fluxSat.getNorm();

        // Vallado's equation 8-44 uses different parameters which are related to our parameters as:
        // cos (phi) = -dot / (psr * area)
        // n         = facet / area
        // s         = -fluxSat / psr
        final double cN = 2 * dOa * (diffuseReflectionCoeff / 3 - specularReflectionCoeff * dOa / psr);
        final double cS = (dot / psr) * (specularReflectionCoeff - 1);
        return new Vector3D(cN, facet.getNormal(), cS, fluxSat);

    }

    /** Class representing a single facet of a convex spacecraft body.
     * <p>Instance of this class are guaranteed to be immutable.</p>
     * @author Luc Maisonobe
     * @version $Revision$ $Date$
     */
    public static class Facet implements Serializable {

        /** Serializble UID. */
        private static final long serialVersionUID = -1743508315029520059L;

        /** Unit Normal vector, pointing outward. */
        private final Vector3D normal;

        /** Area in m<sup>2</sup>. */
        private final double area;

        /** Simple constructor.
         * @param normal vector normal to the facet, pointing outward (will be normalized)
         * @param area facet area in m<sup>2</sup>
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

    /** {@inheritDoc} */
    public void setAbsorptionCoefficient(final double value) {
        absorptionCoeff = value;
        diffuseReflectionCoeff = 1 - (absorptionCoeff + specularReflectionCoeff);
    }

    /** {@inheritDoc} */
    public double getAbsorptionCoefficient() {
        return absorptionCoeff;
    }

    /** {@inheritDoc} */
    public void setReflectionCoefficient(final double value) {
        specularReflectionCoeff = value;
        diffuseReflectionCoeff  = 1 - (absorptionCoeff + specularReflectionCoeff);
    }

    /** {@inheritDoc} */
    public double getReflectionCoefficient() {
        return specularReflectionCoeff;
    }

    /** {@inheritDoc} */
    public void setDragCoefficient(final double value) {
        dragCoeff = value;
    }

    /** {@inheritDoc} */
    public double getDragCoefficient() {
        return dragCoeff;
    }

}
