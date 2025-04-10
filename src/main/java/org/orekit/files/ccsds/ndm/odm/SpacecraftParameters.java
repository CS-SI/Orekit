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

package org.orekit.files.ccsds.ndm.odm;

import org.orekit.files.ccsds.section.CommentsContainer;
import org.orekit.files.ccsds.section.Data;

/** Container for spacecraft parameters.
 * <p>
 * Beware that the Orekit getters and setters all rely on SI units. The parsers
 * and writers take care of converting these SI units into CCSDS mandatory units.
 * The {@link org.orekit.utils.units.Unit Unit} class provides useful
 * {@link org.orekit.utils.units.Unit#fromSI(double) fromSi} and
 * {@link org.orekit.utils.units.Unit#toSI(double) toSI} methods in case the callers
 * already use CCSDS units instead of the API SI units. The general-purpose
 * {@link org.orekit.utils.units.Unit Unit} class (without an 's') and the
 * CCSDS-specific {@link org.orekit.files.ccsds.definitions.Units Units} class
 * (with an 's') also provide some predefined units. These predefined units and the
 * {@link org.orekit.utils.units.Unit#fromSI(double) fromSi} and
 * {@link org.orekit.utils.units.Unit#toSI(double) toSI} conversion methods are indeed
 * what the parsers and writers use for the conversions.
 * </p>
 * @author sports
 * @since 6.1
 */
public class SpacecraftParameters extends CommentsContainer implements Data {

    /** Spacecraft mass. */
    private double mass;

    /** Solar radiation pressure area (m^2). */
    private double solarRadArea;

    /** Solar radiation pressure coefficient. */
    private double solarRadCoeff;

    /** Drag area (m^2). */
    private double dragArea;

    /** Drag coefficient. */
    private double dragCoeff;

    /** Create an empty state data set.
     */
    public SpacecraftParameters() {
        mass          = Double.NaN;
        solarRadArea  = Double.NaN;
        solarRadCoeff = Double.NaN;
        dragArea      = Double.NaN;
        dragCoeff     = Double.NaN;
    }

    /** {@inheritDoc} */
    @Override
    public void validate(final double version) {
        checkNotNaN(mass, SpacecraftParametersKey.MASS.name());
    }

    /** Get the spacecraft mass.
     * @return the spacecraft mass
     */
    public double getMass() {
        return mass;
    }

    /** Set the spacecraft mass.
     * @param mass the spacecraft mass to be set
     */
    public void setMass(final double mass) {
        refuseFurtherComments();
        this.mass = mass;
    }

    /** Get the solar radiation pressure area.
     * @return the solar radiation pressure area
     */
    public double getSolarRadArea() {
        return solarRadArea;
    }

    /** Set the solar radiation pressure area.
     * @param solarRadArea the area to be set
     */
    public void setSolarRadArea(final double solarRadArea) {
        refuseFurtherComments();
        this.solarRadArea = solarRadArea;
    }

    /** Get the solar radiation pressure coefficient.
     * @return the solar radiation pressure coefficient
     */
    public double getSolarRadCoeff() {
        return solarRadCoeff;
    }

    /** Get the solar radiation pressure coefficient.
     * @param solarRadCoeff the coefficient to be set
     */
    public void setSolarRadCoeff(final double solarRadCoeff) {
        refuseFurtherComments();
        this.solarRadCoeff = solarRadCoeff;
    }

    /** Get the drag area.
     * @return the drag area
     */
    public double getDragArea() {
        return dragArea;
    }

    /** Set the drag area.
     * @param dragArea the area to be set
     */
    public void setDragArea(final double dragArea) {
        refuseFurtherComments();
        this.dragArea = dragArea;
    }

    /** Get the drag coefficient.
     * @return the drag coefficient
     */
    public double getDragCoeff() {
        return dragCoeff;
    }

    /** Set the drag coefficient.
     * @param dragCoeff the coefficient to be set
     */
    public void setDragCoeff(final double dragCoeff) {
        refuseFurtherComments();
        this.dragCoeff = dragCoeff;
    }

}
