/* Copyright 2002-2008 CS Communication & Systèmes
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

import org.apache.commons.math.geometry.Vector3D;
import org.orekit.forces.drag.DragSensitive;
import org.orekit.forces.radiation.RadiationSensitive;

/** This class represents the features of a simplified spacecraft.
 * <p>The model of this spacecraft is a simple spherical model, this
 * means that all coefficients are constant and do not depend of
 * the direction. As such, it is a simple container that returns the
 * values set in the constructor.</p>
 *
 * @author &Eacute;douard Delente
 * @author Fabien Maussion
 * @version $Revision:1665 $ $Date:2008-06-11 12:12:59 +0200 (mer., 11 juin 2008) $
 */
public class SphericalSpacecraft implements RadiationSensitive, DragSensitive {

    /** Serializable UID. */
    private static final long serialVersionUID = 7948570591152935928L;

    /** Cross section (m<sup>2</sup>). */
    private double crossSection;

    /** Drag coefficient. */
    private double dragCoeff;

    /** Absorption coefficient. */
    private double absorptionCoeff;

    /** Specular reflection coefficient. */
    private double reflectionCoeff;

    /** Simple constructor.
     * @param crossSection Surface (m<sup>2</sup>)
     * @param dragCoeff Drag coefficient
     * @param absorptionCoeff coefficient Absorption coefficient
     * @param reflectionCoeff Specular reflection coefficient
     */
    public SphericalSpacecraft(final double crossSection, final double dragCoeff,
                               final double absorptionCoeff, final double reflectionCoeff) {

        this.crossSection = crossSection;
        this.dragCoeff = dragCoeff;
        this.absorptionCoeff = absorptionCoeff;
        this.reflectionCoeff = reflectionCoeff;
    }

    /** {@inheritDoc} */
    public double getDragCrossSection(final Vector3D direction) {
        return crossSection;
    }

    /** {@inheritDoc} */
    public Vector3D getDragCoef(final Vector3D direction) {
        return new Vector3D(dragCoeff, direction);
    }

    /** {@inheritDoc} */
    public Vector3D getAbsorptionCoef(final Vector3D direction) {
        return new Vector3D(absorptionCoeff, direction);
    }

    /** {@inheritDoc} */
    public double getRadiationCrossSection(final Vector3D direction) {
        return crossSection;
    }

    /** {@inheritDoc} */
    public Vector3D getReflectionCoef(final Vector3D direction) {
        return new Vector3D(reflectionCoeff, direction);
    }

    /** Set the cross section.
     * @param crossSection crossSection (m<sup>2</sup>)
     */
    public void setCrossSection(final double crossSection) {
        this.crossSection = crossSection;
    }

    /** Set the drag coefficient.
     * @param dragCoeff coefficient drag coefficient
     */
    public void setDragCoeff(final double dragCoeff) {
        this.dragCoeff = dragCoeff;
    }

    /** Set the absorption coefficient.
     * @param absorptionCoeff coefficient absorption coefficient
     */
    public void setAbsorptionCoeff(final double absorptionCoeff) {
        this.absorptionCoeff = absorptionCoeff;
    }

    /** Set the specular reflection coefficient.
     * @param reflectionCoeff coefficient specular reflection coefficient
     */
    public void setReflectionCoeff(final double reflectionCoeff) {
        this.reflectionCoeff = reflectionCoeff;
    }

}
