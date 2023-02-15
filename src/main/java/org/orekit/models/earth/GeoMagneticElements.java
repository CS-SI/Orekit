/* Copyright 2011-2012 Space Applications Services
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
package org.orekit.models.earth;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.utils.units.UnitsConverter;

/** Contains the elements to represent a magnetic field at a single point.
 * @author Thomas Neidhart
 */
public class GeoMagneticElements implements Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = 1881493738280586855L;

    /** The magnetic field vector (East=X, North=Y, Nadir=Z). */
    private Vector3D b;

    /** The magnetic inclination in radians. */
    private double inclination;

    /** The magnetic declination in radians. */
    private double declination;

    /** The magnetic total intensity, in Teslas. */
    private double totalIntensity;

    /** The magnetic horizontal intensity, in Teslas. */
    private double horizontalIntensity;

    /** Construct a new element with the given field vector. The other elements
     * of the magnetic field are calculated from the field vector.
     * @param b the magnetic field vector
     */
    public GeoMagneticElements(final Vector3D b) {
        this.b = new Vector3D(UnitsConverter.NANO_TESLAS_TO_TESLAS.getFrom().getScale(), b);

        final double intensityNanoTesla = FastMath.hypot(b.getX(), b.getY());
        horizontalIntensity = UnitsConverter.NANO_TESLAS_TO_TESLAS.convert(intensityNanoTesla);
        totalIntensity = UnitsConverter.NANO_TESLAS_TO_TESLAS.convert(b.getNorm());
        declination = FastMath.atan2(b.getY(), b.getX());
        inclination = FastMath.atan2(b.getZ(), intensityNanoTesla);
    }

    /** Returns the magnetic field vector in Tesla.
     * @return the magnetic field vector in Tesla
     */
    public Vector3D getFieldVector() {
        return b;
    }

    /** Returns the inclination of the magnetic field in radians.
     * @return the inclination (dip) in radians
     */
    public double getInclination() {
        return inclination;
    }

    /** Returns the declination of the magnetic field in radians.
     * @return the declination (dec) in radians
     */
    public double getDeclination() {
        return declination;
    }

    /** Returns the total intensity of the magnetic field (= norm of the field vector).
     * @return the total intensity in Tesla
     */
    public double getTotalIntensity() {
        return totalIntensity;
    }

    /** Returns the horizontal intensity of the magnetic field (= norm of the
     * vector in the plane spanned by the x/y components of the field vector).
     * @return the horizontal intensity in Tesla
     */
    public double getHorizontalIntensity() {
        return horizontalIntensity;
    }

    @Override
    public String toString() {
        final NumberFormat f = NumberFormat.getInstance();
        final DecimalFormat d = new DecimalFormat("0.######E0");
        final StringBuilder sb = new StringBuilder();
        sb.append("MagneticField[");
        sb.append("B=");
        sb.append(b.toString(d));
        sb.append(",H=");
        sb.append(d.format(getHorizontalIntensity()));
        sb.append(",F=");
        sb.append(d.format(getTotalIntensity()));
        sb.append(",I=");
        sb.append(f.format(getInclination()));
        sb.append(",D=");
        sb.append(f.format(getDeclination()));
        sb.append("]");
        return sb.toString();
    }
}
