package fr.cs.aerospace.orekit;

import org.spaceroots.mantissa.geometry.Vector3D;

/** This interface represents the vehicle features.
 *
 *
 * @version $Id$
 * @author E. Delente
 */

public interface Vehicle {
    
    /** Get the mass.
    * @return mass (m)
    */
    public double getMass();

    /** Get the surface.
    * @return surface (m^2)
    */
    public double getSurface();
    
    /** Get the surface.
    * @return surface (m^2)
    */
    public double getSurface(Vector3D direction);

    /** Get the drag coefficient.
    * @return drag coefficient
    */
    public double getDragCoef();
    
    /** Get the drag coefficient.
    * @return drag coefficient
    */
    public double getDragCoef(Vector3D direction);
    
    /** Get the absorption coefficient.
    * @return absorption coefficient
    */
    public double getAbsCoef();

    /** Get the absorption coefficient.
    * @return absorption coefficient
    */
    public double getAbsCoef(Vector3D direction);
    
    /** Get the specular reflection coefficient.
    * @return specular reflection coefficient
    */
    public double getReflCoef();

    /** Get the specular reflection coefficient.
    * @return specular reflection coefficient
    */
    public double getReflCoef(Vector3D direction);
    
    /** Set the mass.
    * @param mass mass (m)
    */
    public void setMass(double mass);
    
    /** Set the surface.
    * @param Surface surface (m^2)
    * @param Direction Direction of the incident flux
    */
    public void setSurface(double surface, Vector3D direction);

    /** Set the drag coefficient.
    * @param drag coefficient drag coefficient
    * @param Direction Direction of the velocity vector
    */
    public void setDragCoef(double drag_coef, Vector3D direction);
    
    /** Set the absorption coefficient.
    * @param absorption coefficient absorption coefficient
    * @param Direction Direction of the incident flux
    */
    public void setAbsCoef(double absorb_coef, Vector3D direction);

    /** Set the specular reflection coefficient.
    * @param absorption coefficient specular reflection coefficient
    * @param Direction Direction of the incident flux
    */
    public void setReflCoef(double refl_coef, Vector3D direction);

}
