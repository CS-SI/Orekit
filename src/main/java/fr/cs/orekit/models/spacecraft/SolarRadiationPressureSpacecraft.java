package fr.cs.orekit.models.spacecraft;

import java.io.Serializable;

import org.apache.commons.math.geometry.Vector3D;

/** Adapted container for the SolarRadiationPressure force model.
 *
 * @author F. Maussion
 */
public interface SolarRadiationPressureSpacecraft extends Serializable {

    /** Get the surface.
     * @param direction direction of the light flux in the spacecraft frame
     * @return surface (m<sup>2</sup>)
     */
    public double getSurface(Vector3D direction);

    /** Get the absorption coefficients vector.
     * @param direction direction of the light flux in the spacecraft frame
     * @return absorption coefficients vector in the spacecraft frame
     */
    public Vector3D getAbsCoef(Vector3D direction);

    /** Get the specular reflection coefficients vector.
     * @param direction direction of the light flux in the spacecraft frame
     * @return specular reflection coefficients vector in the spacecraft frame
     */
    public Vector3D getReflectionCoef(Vector3D direction);

}
