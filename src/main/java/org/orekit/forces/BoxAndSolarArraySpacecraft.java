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
package org.orekit.forces;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.Precision;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitInternalError;
import org.orekit.forces.drag.DragSensitive;
import org.orekit.forces.radiation.RadiationSensitive;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.utils.ExtendedPVCoordinatesProvider;
import org.orekit.utils.ParameterDriver;

/** Class representing the features of a classical satellite with a convex body shape.
 * <p>
 * The body can be either a simple parallelepipedic box aligned with
 * spacecraft axes or a set of panels defined by their area and normal vector.
 * Some panels may be moving to model solar arrays (or antennas that could
 * point anywhere). This should handle accurately most spacecraft shapes. This model
 * does not take cast shadows into account.
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
 * and panels normal orientation. For a single panel, if the relative velocity is
 * head-on (i.e. aligned with the panel normal), the force will be in the same
 * direction with and without lift, but the magnitude with lift ratio set to 1.0 will
 * be twice the magnitude with lift ratio set to 0.0 (because atmosphere molecules
 * bounces backward at same velocity in case of specular reflection).
 * </p>
 * <p>
 * Each {@link Panel panel} has its own set of radiation and drag coefficients. In
 * orbit determination context, it would not be possible to estimate each panel
 * individually, therefore {@link #getDragParametersDrivers()} returns a single
 * {@link ParameterDriver parameter driver} representing a {@link DragSensitive#GLOBAL_DRAG_FACTOR
 * global drag multiplicative factor} that applies to all panels drag coefficients
 * and the {@link #getRadiationParametersDrivers()} returns a single
 * {@link ParameterDriver parameter driver} representing a
 * {@link RadiationSensitive#GLOBAL_RADIATION_FACTOR global radiation multiplicative factor}
 * that applies to all panels radiation coefficients.
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

    /** Driver for drag multiplicative factor parameter. */
    private final ParameterDriver dragFactorParameterDriver;

    /** Driver for radiation pressure multiplicative factor parameter. */
    private final ParameterDriver radiationFactorParameterDriver;

    /** Panels composing the spacecraft. */
    private final List<Panel> panels;

    /** Build a spacecraft model.
     * @param panels panels composing the body, solar arrays and antennas
     * (only the panels with strictly positive area will be stored)
     * @since 12.0
     */
    public BoxAndSolarArraySpacecraft(final List<Panel> panels) {

        try {
            dragFactorParameterDriver      = new ParameterDriver(DragSensitive.GLOBAL_DRAG_FACTOR,
                                                                 1.0, SCALE, 0.0, Double.POSITIVE_INFINITY);
            radiationFactorParameterDriver = new ParameterDriver(RadiationSensitive.GLOBAL_RADIATION_FACTOR,
                                                                 1.0, SCALE, 0.0, Double.POSITIVE_INFINITY);
        } catch (OrekitException oe) {
            // this should never happen
            throw new OrekitInternalError(oe);
        }

        // remove spurious panels
        this.panels = panels.stream().filter(p -> p.getArea() > 0).collect(Collectors.toList());

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
     * @since 12.0
     */
    public BoxAndSolarArraySpacecraft(final double xLength, final double yLength, final double zLength,
                                      final ExtendedPVCoordinatesProvider sun,
                                      final double solarArrayArea, final Vector3D solarArrayAxis,
                                      final double dragCoeff, final double liftRatio,
                                      final double absorptionCoeff, final double reflectionCoeff) {
        this(buildPanels(xLength, yLength, zLength,
                         sun, solarArrayArea, solarArrayAxis,
                         dragCoeff, liftRatio, absorptionCoeff, reflectionCoeff));
    }

    /** Get the panels composing the body.
     * @return unmodifiable view of the panels composing the body
     * @since 12.0
     */
    public List<Panel> getPanels() {
        return Collections.unmodifiableList(panels);
    }

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getDragParametersDrivers() {
        return Collections.singletonList(dragFactorParameterDriver);
    }

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getRadiationParametersDrivers() {
        return Collections.singletonList(radiationFactorParameterDriver);
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D dragAcceleration(final SpacecraftState state,
                                     final double density, final Vector3D relativeVelocity,
                                     final double[] parameters) {

        final double dragFactor = parameters[0];

        // relative velocity in spacecraft frame
        final double   vNorm2 = relativeVelocity.getNormSq();
        final double   vNorm  = FastMath.sqrt(vNorm2);
        final Vector3D vDir   = state.getAttitude().getRotation().applyTo(relativeVelocity.scalarMultiply(1.0 / vNorm));
        final double   coeff  = density * dragFactor * vNorm2 / (2.0 * state.getMass());

        // panels contribution
        Vector3D acceleration = Vector3D.ZERO;
        for (final Panel panel : panels) {
            Vector3D normal = panel.getNormal(state);
            double dot = Vector3D.dotProduct(normal, vDir);
            if (panel.isDoubleSided() && dot > 0) {
                // the flux comes from the back side
                normal = normal.negate();
                dot    = -dot;
            }
            if (dot < 0) {
                // the panel intercepts the incoming flux
                final double f         = coeff * panel.getDrag() * panel.getArea() * dot;
                final double liftRatio = panel.getLiftRatio();
                acceleration = new Vector3D(1,                                 acceleration,
                                            (1 - liftRatio) * FastMath.abs(f), vDir,
                                            liftRatio * f * 2,                 normal);
            }
        }

        // convert back to inertial frame
        return state.getAttitude().getRotation().applyInverseTo(acceleration);

    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldVector3D<T>
        dragAcceleration(final FieldSpacecraftState<T> state,
                         final  T density, final FieldVector3D<T> relativeVelocity,
                         final T[] parameters) {

        final Field<T> field = state.getDate().getField();
        final T dragFactor = parameters[0];

        // relative velocity in spacecraft frame
        final T                vNorm2 = relativeVelocity.getNormSq();
        final T                vNorm  = FastMath.sqrt(vNorm2);
        final FieldVector3D<T> vDir   = state.getAttitude().getRotation().applyTo(relativeVelocity.scalarMultiply(vNorm.reciprocal()));
        final T                coeff  = density.multiply(dragFactor).multiply(vNorm2).divide(state.getMass().multiply(2.0));

        // panels contribution
        FieldVector3D<T> acceleration = FieldVector3D.getZero(field);
        for (final Panel panel : panels) {
            FieldVector3D<T> normal = panel.getNormal(state);
            T dot = FieldVector3D.dotProduct(normal, vDir);
            if (panel.isDoubleSided() && dot.getReal() > 0) {
                // the flux comes from the back side
                normal = normal.negate();
                dot    = dot.negate();
            }
            if (panel.isDoubleSided() || dot.getReal() < 0) {
                // the panel intercepts the incoming flux
                final T      f         = coeff.multiply(panel.getDrag() * panel.getArea()).multiply(dot);
                final double liftRatio = panel.getLiftRatio();
                acceleration = new FieldVector3D<>(field.getOne(),                         acceleration,
                                                  FastMath.abs(f).multiply(1 - liftRatio), vDir,
                                                  f.multiply(2 * liftRatio),               normal);
            }
        }

        // convert back to inertial frame
        return state.getAttitude().getRotation().applyInverseTo(acceleration);

    }

    /** {@inheritDoc} */
    @Override
    public Vector3D radiationPressureAcceleration(final SpacecraftState state,
                                                  final Vector3D flux,
                                                  final double[] parameters) {

        if (flux.getNormSq() < Precision.SAFE_MIN) {
            // null illumination (we are probably in umbra)
            return Vector3D.ZERO;
        }

        // radiation flux in spacecraft frame
        final double   radiationFactor = parameters[0];
        final Vector3D fluxSat         = state.getAttitude().getRotation().applyTo(flux).
                                         scalarMultiply(radiationFactor);

        // panels contribution
        Vector3D force = Vector3D.ZERO;
        for (final Panel panel : panels) {
            Vector3D normal = panel.getNormal(state);
            double dot = Vector3D.dotProduct(normal, fluxSat);
            if (panel.isDoubleSided() && dot > 0) {
                // the flux comes from the back side
                normal = normal.negate();
                dot    = -dot;
            }
            if (dot < 0) {
                // the panel intercepts the incoming flux

                final double absorptionCoeff         = panel.getAbsorption();
                final double specularReflectionCoeff = panel.getReflection();
                final double diffuseReflectionCoeff  = 1 - (absorptionCoeff + specularReflectionCoeff);
                final double psr                     = fluxSat.getNorm();

                // Vallado's equation 8-44 uses different parameters which are related to our parameters as:
                // cos (phi) = -dot / (psr * area)
                // n         = panel / area
                // s         = -fluxSat / psr
                final double cN = 2 * panel.getArea() * dot * (diffuseReflectionCoeff / 3 - specularReflectionCoeff * dot / psr);
                final double cS = (panel.getArea() * dot / psr) * (specularReflectionCoeff - 1);
                force = new Vector3D(1, force, cN, normal, cS, fluxSat);

            }
        }

        // convert to inertial frame
        return state.getAttitude().getRotation().applyInverseTo(new Vector3D(1.0 / state.getMass(), force));

    }

    /** {@inheritDoc}
     * <p>This method implements equation 8-44 from David A. Vallado's
     * Fundamentals of Astrodynamics and Applications, third edition,
     * 2007, Microcosm Press.</p>
     */
    @Override
    public <T extends CalculusFieldElement<T>> FieldVector3D<T>
        radiationPressureAcceleration(final FieldSpacecraftState<T> state,
                                      final FieldVector3D<T> flux,
                                      final T[] parameters) {

        final Field<T> field = state.getDate().getField();
        if (flux.getNormSq().getReal() < Precision.SAFE_MIN) {
            // null illumination (we are probably in umbra)
            return FieldVector3D.getZero(field);
        }

        // radiation flux in spacecraft frame
        final T                radiationFactor = parameters[0];
        final FieldVector3D<T> fluxSat         = state.getAttitude().getRotation().applyTo(flux).
                                                 scalarMultiply(radiationFactor);

        // panels contribution
        FieldVector3D<T> force = FieldVector3D.getZero(field);
        for (final Panel panel : panels) {
            FieldVector3D<T> normal = panel.getNormal(state);
            T dot = FieldVector3D.dotProduct(normal, fluxSat);
            if (panel.isDoubleSided() && dot.getReal() > 0) {
                // the flux comes from the back side
                normal = normal.negate();
                dot    = dot.negate();
            }
            if (dot.getReal() < 0) {
                // the panel intercepts the incoming flux

                final double absorptionCoeff         = panel.getAbsorption();
                final double specularReflectionCoeff = panel.getReflection();
                final double diffuseReflectionCoeff  = 1 - (absorptionCoeff + specularReflectionCoeff);
                final T      psr                     = fluxSat.getNorm();

                // Vallado's equation 8-44 uses different parameters which are related to our parameters as:
                // cos (phi) = -dot / (psr * area)
                // n         = panel / area
                // s         = -fluxSat / psr
                final T cN = dot.multiply(-2 * panel.getArea()).multiply(dot.multiply(specularReflectionCoeff).divide(psr).subtract(diffuseReflectionCoeff / 3));
                final T cS = dot.multiply(panel.getArea()).multiply(specularReflectionCoeff - 1).divide(psr);
                force = new FieldVector3D<>(field.getOne(), force, cN, normal, cS, fluxSat);
            }
        }

        // convert to inertial frame
        return state.getAttitude().getRotation().applyInverseTo(new FieldVector3D<>(state.getMass().reciprocal(), force));

    }

    /** Build the panels of a simple parallelepipedic box.
     * @param xLength length of the body along its X axis (m)
     * @param yLength length of the body along its Y axis (m)
     * @param zLength length of the body along its Z axis (m)
     * @param drag drag coefficient
     * @param liftRatio drag lift ratio (proportion between 0 and 1 of atmosphere modecules
     * that will experience specular reflection when hitting spacecraft instead
     * of experiencing diffuse reflection, hence producing lift)
     * @param absorption radiation pressure absorption coefficient (between 0 and 1)
     * @param reflection radiation pressure specular reflection coefficient (between 0 and 1)
     * @return surface vectors array
     * @since 12.0
     */
    public static List<Panel> buildBox(final double xLength, final double yLength, final double zLength,
                                       final double drag, final double liftRatio,
                                       final double absorption, final double reflection) {

        final List<Panel> panels = new ArrayList<>(6);

        // spacecraft body, composed of single-sided panels
        panels.add(new FixedPanel(Vector3D.MINUS_I, yLength * zLength, false, drag, liftRatio, absorption, reflection));
        panels.add(new FixedPanel(Vector3D.PLUS_I,  yLength * zLength, false, drag, liftRatio, absorption, reflection));
        panels.add(new FixedPanel(Vector3D.MINUS_J, xLength * zLength, false, drag, liftRatio, absorption, reflection));
        panels.add(new FixedPanel(Vector3D.PLUS_J,  xLength * zLength, false, drag, liftRatio, absorption, reflection));
        panels.add(new FixedPanel(Vector3D.MINUS_K, xLength * yLength, false, drag, liftRatio, absorption, reflection));
        panels.add(new FixedPanel(Vector3D.PLUS_K,  xLength * yLength, false, drag, liftRatio, absorption, reflection));

        return panels;

    }

    /** Build the panels of a simple parallelepiped box plus one solar array panel.
     * @param xLength length of the body along its X axis (m)
     * @param yLength length of the body along its Y axis (m)
     * @param zLength length of the body along its Z axis (m)
     * @param sun sun model
     * @param solarArrayArea area of the solar array (m²)
     * @param solarArrayAxis solar array rotation axis in satellite frame
     * @param drag drag coefficient
     * @param liftRatio drag lift ratio (proportion between 0 and 1 of atmosphere modecules
     * that will experience specular reflection when hitting spacecraft instead
     * of experiencing diffuse reflection, hence producing lift)
     * @param absorption radiation pressure absorption coefficient (between 0 and 1)
     * @param reflection radiation pressure specular reflection coefficient (between 0 and 1)
     * @return surface vectors array
     * @since 12.0
     */
    public static List<Panel> buildPanels(final double xLength, final double yLength, final double zLength,
                                          final ExtendedPVCoordinatesProvider sun,
                                          final double solarArrayArea, final Vector3D solarArrayAxis,
                                          final double drag, final double liftRatio,
                                          final double absorption, final double reflection) {

        // spacecraft body
        final List<Panel> panels = buildBox(xLength, yLength, zLength, drag, liftRatio, absorption, reflection);

        // solar array
        panels.add(new PointingPanel(solarArrayAxis, sun, solarArrayArea, drag, liftRatio, absorption, reflection));

        return panels;

    }

}
