/* Copyright 2013 Applied Defense Solutions, Inc.
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

import org.hipparchus.util.FastMath;
import org.orekit.models.AtmosphericRefractionModel;

/** Implementation of refraction model for Earth standard atmosphere.
 * <p>Refraction angle is 0 at zenith, about 1 arcminute at 45°, and 34 arcminutes at the
 *  horizon for optical wavelengths.</p>
 * <p>Refraction angle is computed according to Saemundssen formula quoted by Meeus.
 *  For reference, see <b>Astronomical Algorithms</b> (1998), 2nd ed,
 *  (ISBN 0-943396-61-1), chap. 15.</p>
 * <p>This formula is about 30 arcseconds of accuracy very close to the horizon, as
 *  variable atmospheric effects become very important.</p>
 * <p>Local pressure and temperature can be set to correct refraction at the viewpoint.</p>
 * @since 6.1
 */
public class EarthStandardAtmosphereRefraction implements AtmosphericRefractionModel {


    /** Default correction factor value. */
    public static final double DEFAULT_CORRECTION_FACTOR = 1.0;

    /** Default local pressure at viewpoint (Pa). */
    public static final double DEFAULT_PRESSURE = 101000.0;

    /** Default local temperature at viewpoint (K). */
    public static final double DEFAULT_TEMPERATURE = 283.0;

    /** NIST standard atmospheric pressure (Pa). */
    public static final double STANDARD_ATM_PRESSURE = 101325.0;

    /** NIST standard atmospheric temperature (K). */
    public static final double STANDARD_ATM_TEMPERATURE = 293.15;

    /** Elevation min value to compute refraction (under the horizon). */
    private static final double MIN_ELEVATION = -2.0;

    /** Elevation max value to compute refraction (zenithal). */
    private static final double MAX_ELEVATION = 89.89;

    /** Serializable UID. */
    private static final long serialVersionUID = 6001744143210742620L;

    /** Refraction correction from local pressure and temperature. */
    private double correfrac;

    /** Local pressure. */
    private double pressure;

    /** Local temperature. */
    private double temperature;

    /**
     * Creates a new default instance.
     */
    public EarthStandardAtmosphereRefraction() {
        correfrac   = DEFAULT_CORRECTION_FACTOR;
        pressure    = DEFAULT_PRESSURE;
        temperature = DEFAULT_TEMPERATURE;
    }

    /**
     * Creates an instance given a specific pressure and temperature.
     * @param pressure in Pascals (Pa)
     * @param temperature in Kelvin (K)
     */
    public EarthStandardAtmosphereRefraction(final double pressure, final double temperature) {
        setTemperature(temperature);
        setPressure(pressure);
    }

    /** Get the local pressure at the evaluation location.
     * @return the pressure (Pa)
     */
    public double getPressure() {
        return pressure;
    }

    /** Set the local pressure at the evaluation location
     * <p>Otherwise the default value for the local pressure is set to {@link #DEFAULT_PRESSURE}.</p>
     * @param pressure the pressure to set (Pa)
     */
    public void setPressure(final double pressure) {
        this.pressure = pressure;
        this.correfrac = (pressure / DEFAULT_PRESSURE) * (DEFAULT_TEMPERATURE / temperature);
    }

    /** Get the local temperature at the evaluation location.
     * @return the temperature (K)
     */
    public double getTemperature() {
        return temperature;
    }

    /** Set the local temperature at the evaluation location
     * <p>Otherwise the default value for the local temperature is set to {@link #DEFAULT_TEMPERATURE}.</p>
     * @param temperature the temperature to set (K)
     */
    public void setTemperature(final double temperature) {
        this.temperature = temperature;
        this.correfrac = (pressure / DEFAULT_PRESSURE) * (DEFAULT_TEMPERATURE / temperature);
    }

    @Override
    /** {@inheritDoc} */
    public double getRefraction(final double trueElevation) {
        double refraction = 0.0;
        final double eld = FastMath.toDegrees(trueElevation);
        if (eld > MIN_ELEVATION && eld < MAX_ELEVATION) {
            final double tmp = eld + 10.3 / (eld + 5.11);
            final double ref = 1.02 / FastMath.tan(FastMath.toRadians(tmp)) / 60.;
            refraction = FastMath.toRadians(correfrac * ref);
        }
        return refraction;
    }
}
