package org.orekit.forces.gravity.potential;

import java.util.List;

/**
 * Defines methods for obtaining gravity fields.
 *
 * @author Evan Ward
 * @author Fabien Maussion
 * @author Pascal Parraud
 * @author Luc Maisonobe
 * @see GravityFieldFactory
 * @since 10.1
 */
public interface GravityFields {

    /** Get the constant gravity field coefficients provider from the first supported file.
     *
     * @param degree maximal degree
     * @param order maximal order
     * @return a gravity field coefficients provider containing already loaded data
     * @since 6.0
     * @see #getNormalizedProvider(int, int)
     */
    NormalizedSphericalHarmonicsProvider getConstantNormalizedProvider(int degree,
                                                                       int order);

    /** Get the gravity field coefficients provider from the first supported file.
     *
     * @param degree maximal degree
     * @param order maximal order
     * @return a gravity field coefficients provider containing already loaded data
     * @since 6.0
     * @see #getConstantNormalizedProvider(int, int)
     */
    NormalizedSphericalHarmonicsProvider getNormalizedProvider(int degree,
                                                               int order);

    /** Get the constant gravity field coefficients provider from the first supported file.
     *
     * @param degree maximal degree
     * @param order maximal order
     * @return a gravity field coefficients provider containing already loaded data
     * @since 6.0
     * @see #getUnnormalizedProvider(int, int)
     */
    UnnormalizedSphericalHarmonicsProvider getConstantUnnormalizedProvider(int degree,
                                                                           int order);

    /** Get the gravity field coefficients provider from the first supported file.
     *
     * @param degree maximal degree
     * @param order maximal order
     * @return a gravity field coefficients provider containing already loaded data
     * @since 6.0
     * @see #getConstantUnnormalizedProvider(int, int)
     */
    UnnormalizedSphericalHarmonicsProvider getUnnormalizedProvider(int degree,
                                                                   int order);

    /** Get the ocean tides waves from the first supported file.
     *
     * <p><span style="color:red">
     * WARNING: as of 2013-11-17, there seem to be an inconsistency when loading
     * one or the other file, for wave Sa (Doodson number 56.554) and P1 (Doodson
     * number 163.555). The sign of the coefficients are different. We think the
     * problem lies in the input files from IERS and not in the conversion (which
     * works for all other waves), but cannot be sure. For this reason, ocean
     * tides are still considered experimental at this date.
     * </span></p>
     * @param degree maximal degree
     * @param order maximal order
     * @return list of tides waves containing already loaded data
     * @since 6.1
     */
    List<OceanTidesWave> getOceanTidesWaves(int degree, int order);
}
