package org.orekit.models.earth;

import org.orekit.models.earth.GeoMagneticFieldFactory.FieldModel;

/**
 * Methods for obtaining geomagnetic fields.
 *
 * @author Evan Ward
 * @author Thomas Neidhart
 * @see GeoMagneticFieldFactory
 * @see LazyLoadedGeoMagneticFields
 * @since 10.1
 */
public interface GeoMagneticFields {

    /**
     * Get the {@link GeoMagneticField} for the given model type and year.
     *
     * @param type the field model type
     * @param year the decimal year
     * @return a {@link GeoMagneticField} for the given year and model
     * @see GeoMagneticField#getDecimalYear(int, int, int)
     */
    GeoMagneticField getField(FieldModel type, double year);

    /**
     * Get the IGRF model for the given year.
     *
     * @param year the decimal year
     * @return a {@link GeoMagneticField} for the given year
     * @see GeoMagneticField#getDecimalYear(int, int, int)
     */
    GeoMagneticField getIGRF(double year);

    /**
     * Get the WMM model for the given year.
     *
     * @param year the decimal year
     * @return a {@link GeoMagneticField} for the given year
     * @see GeoMagneticField#getDecimalYear(int, int, int)
     */
    GeoMagneticField getWMM(double year);

}
