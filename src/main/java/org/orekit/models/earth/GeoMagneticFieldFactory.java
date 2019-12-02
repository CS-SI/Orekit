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

import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.DataContext;

/** Factory for different {@link GeoMagneticField} models.
 * <p>
 * This is a utility class, so its constructor is private.
 * </p>
 * @author Thomas Neidhart
 * @author Evan Ward
 * @see GeoMagneticFields
 * @see LazyLoadedGeoMagneticFields
 * @see DataContext#getGeoMagneticFields()
 */
public class GeoMagneticFieldFactory {

    /** The currently supported geomagnetic field models. */
    public enum FieldModel {
        /** World Magnetic Model. */
        WMM,
        /** International Geomagnetic Reference Field. */
        IGRF
    }

    /** Private constructor.
     * <p>
     * This class is a utility class, it should neither have a public nor a
     * default constructor. This private constructor prevents the compiler from
     * generating one automatically.
     * </p>
     */
    private GeoMagneticFieldFactory() {
    }

    /**
     * Get the instance of {@link GeoMagneticFields} that is called by methods in this
     * class.
     *
     * @return the geomagnetic fields used by this factory.
     * @since 10.1
     */
    @DefaultDataContext
    public static LazyLoadedGeoMagneticFields getGeoMagneticFields() {
        return DataContext.getDefault().getGeoMagneticFields();
    }

    /** Get the {@link GeoMagneticField} for the given model type and year.
     * @param type the field model type
     * @param year the decimal year
     * @return a {@link GeoMagneticField} for the given year and model
     * @see GeoMagneticField#getDecimalYear(int, int, int)
     */
    @DefaultDataContext
    public static GeoMagneticField getField(final FieldModel type, final double year) {
        return getGeoMagneticFields().getField(type, year);
    }

    /** Get the IGRF model for the given year.
     * @param year the decimal year
     * @return a {@link GeoMagneticField} for the given year
     * @see GeoMagneticField#getDecimalYear(int, int, int)
     */
    @DefaultDataContext
    public static GeoMagneticField getIGRF(final double year) {
        return getGeoMagneticFields().getIGRF(year);
    }

    /** Get the WMM model for the given year.
     * @param year the decimal year
     * @return a {@link GeoMagneticField} for the given year
     * @see GeoMagneticField#getDecimalYear(int, int, int)
     */
    @DefaultDataContext
    public static GeoMagneticField getWMM(final double year) {
        return getGeoMagneticFields().getWMM(year);
    }

}
