/* Copyright 2002-2024 Thales Alenia Space
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
package org.orekit.models.earth.troposphere.iturp834;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.analysis.interpolation.GridAxis;
import org.hipparchus.util.FastMath;
import org.orekit.bodies.FieldGeodeticPoint;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.utils.units.Unit;

/** Utility class for ITU-R P.834 gravity parameters.
 * <p>
 * This class implements the gravity parts of the model,
 * i.e. equations 27g and 27j in section 6 of the recommendation.
 * </p>
 * @see ITURP834PathDelay
 * @see ITURP834MappingFunction
 * @see ITURP834WeatherParameters
 * @author Luc Maisonobe
 * @see <a href="https://www.itu.int/rec/R-REC-P.834/en">P.834 : Effects of tropospheric refraction on radiowave propagation</>
 * @since 13.0
 */
class Gravity {

    /** Name of height reference level. */
    public static final String AVERAGE_HEIGHT_REFERENCE_LEVEL_NAME = "hreflev.dat";

    /** Gravity factor for equation 27g. */
    private static final double G_27G = 9.806;

    /** Gravity latitude correction factor for equation 27g. */
    private static final double GL_27G = 0.002637;

    /** Gravity altitude correction factor for equation 27g. */
    private static final double GH_27G = Unit.parse("km⁻¹").toSI(0.00031);

    /** Gravity factor for equation 27j. */
    private static final double G_27J = 9.784;

    /** Gravity latitude correction factor for equation 27j. */
    private static final double GL_27J = 0.00266;

    /** Gravity altitude correction factor for equation 27j. */
    private static final double GH_27J = Unit.parse("km⁻¹").toSI(0.00028);

    /** Gravity at Earth surface. */
    private static final ConstantGrid GS =
        new ConstantGrid(Unit.METRE, AVERAGE_HEIGHT_REFERENCE_LEVEL_NAME).
                apply((lat, lon, h) -> G_27G * (1 - GL_27G * FastMath.cos(2 * lat) - GH_27G * h));

    /** Private constructor for a utility class.
     */
    private Gravity() {
        // nothing to do
    }

    /** Get gravity at surface.
     * @param location point location on Earth
     * @return gravity model over one cell
     */
    public static GridCell getGravityAtSurface(final GeodeticPoint location) {
        return GS.getCell(location, 0.0);
    }

    /** Get gravity at surface.
     * @param location point location on Earth
     * @return gravity model over one cell
     */
    public static <T extends CalculusFieldElement<T>> FieldGridCell<T> getGravityAtSurface(final FieldGeodeticPoint<T> location) {
        return GS.getCell(location, null);
    }

    /** Get gravity at point altitude.
     * @param location point location on Earth
     * @return gravity model over one cell
     */
    public static GridCell getGravityAtAltitude(final GeodeticPoint location) {
        final GridAxis latitudeAxis  = GS.getLatitudeAxis();
        final int      southIndex    = latitudeAxis.interpolationIndex(location.getLatitude());
        final double   northLatitude = latitudeAxis.node(southIndex + 1);
        final double   southLatitude = latitudeAxis.node(southIndex);
        final GridAxis longitudeAxis = GS.getLongitudeAxis();
        final int      westIndex     = longitudeAxis.interpolationIndex(location.getLongitude());
        final double   westLongitude = longitudeAxis.node(westIndex);
        final double   mga           = -GH_27J * location.getAltitude();
        final double   gNorth        = (mga + 1 - GL_27J * FastMath.cos(2 * northLatitude)) * G_27J;
        final double   gSouth        = (mga + 1 - GL_27J * FastMath.cos(2 * southLatitude)) * G_27J;
        return new GridCell(location.getLatitude()  - southLatitude,
                            location.getLongitude() - westLongitude,
                            GS.getSizeLat(), GS.getSizeLon(),
                            gNorth, gSouth, gSouth, gNorth);
    }

    /** Get gravity at point altitude.
     * @param location point location on Earth
     * @return gravity model over one cell
     */
    public static <T extends CalculusFieldElement<T>> FieldGridCell<T> getGravityAtAltitude(
        final FieldGeodeticPoint<T> location) {
        final GridAxis latitudeAxis  = GS.getLatitudeAxis();
        final int      southIndex    = latitudeAxis.interpolationIndex(location.getLatitude().getReal());
        final double   northLatitude = latitudeAxis.node(southIndex + 1);
        final double   southLatitude = latitudeAxis.node(southIndex);
        final GridAxis longitudeAxis = GS.getLongitudeAxis();
        final int      westIndex     = longitudeAxis.interpolationIndex(location.getLongitude().getReal());
        final double   westLongitude = longitudeAxis.node(westIndex);
        final T        mga           = location.getAltitude().multiply(GH_27J).negate();
        final T        gNorth        = mga.add(1 - GL_27J * FastMath.cos(2 * northLatitude)).multiply(G_27J);
        final T        gSouth        = mga.add(1 - GL_27J * FastMath.cos(2 * southLatitude)).multiply(G_27J);
        return new FieldGridCell<>(location.getLatitude().subtract(southLatitude),
                                   location.getLongitude().subtract(westLongitude),
                                   GS.getSizeLat(), GS.getSizeLon(), gNorth,
                                   gSouth, gSouth, gNorth);
    }

}
