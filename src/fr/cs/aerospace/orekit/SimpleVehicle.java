package fr.cs.aerospace.orekit;

import org.spaceroots.mantissa.geometry.Vector3D;

/** This class represents the features of a simplified spacecraft.
 *
 *
 * @version $Id$
 * @author E. Delente
 */

public class SimpleVehicle implements Vehicle {

    /** Mass (Kg)*/
    private double mass;
    
    /** Surface (m^2) */
    private double surface;
    
    /** Drag coefficient */
    private double drag_coef;
    
    /** Absorption coefficient */
    private double absorb_coef;
    
    /** Specular reflection coefficient */
    private double refl_coef;
    
    /** Create a vehicle.
     * @param mass Mass (Kg)
     * @param surface Surface (m^2)
     * @param drag_coef Drag coefficient
     * @param absorp_coef coefficient Absorption coefficient
     * @param refl_coef Specular reflection coefficient
     */
    public SimpleVehicle(double mass, double surface, double drag_coef, double absorb_coef, double refl_coef) {
        this.mass = mass;
        this.surface = surface;
        this.drag_coef = drag_coef;
        this.absorb_coef = absorb_coef;
        this.refl_coef = refl_coef;
    }
    

    /** Get the mass.
    * @return mass (m)
    */
    public double getMass() {
        return this.mass;
    }

   /** Get the surface.
    * @return surface (m^2)
    */
    public double getSurface() {
        return surface;
    }
    
    /** Get the surface.
    * @return surface (m^2)
    */
    public double getSurface(Vector3D direction) {
        return surface;
    }

    /** Get the drag coefficient.
    * @return drag coefficient
    */
    public double getDragCoef() {
        return drag_coef;
    }

    /** Get the drag coefficient.
    * @return drag coefficient
    */
    public double getDragCoef(Vector3D direction) {
        return drag_coef;
    }
    
    /** Get the absorption coefficient.
    * @return absorption coefficient
    */
    public double getAbsCoef() {
        return absorb_coef;
    }
    
    /** Get the absorption coefficient.
    * @return absorption coefficient
    */
    public double getAbsCoef(Vector3D direction) {
        return absorb_coef;
    }
    
    /** Get the specular reflection coefficient.
    * @return specular reflection coefficient
    */
    public double getReflCoef() {
        return refl_coef;
    }

    /** Get the specular reflection coefficient.
    * @return specular reflection coefficient
    */
    public double getReflCoef(Vector3D direction) {
        return refl_coef;
    }
    
    /** Set the mass.
    * @param mass mass (m)
    */
    public void setMass(double mass) {
        this.mass = mass;
    }
    
    /** Set the surface.
    * @param Surface surface (m^2)
    * @param Direction Direction of the incident flux
    */
    public void setSurface(double surface, Vector3D direction) {
        this.surface = surface;
    }

    /** Set the drag coefficient.
    * @param drag coefficient drag coefficient
    * @param Direction Direction of the velocity vector
    */
    public void setDragCoef(double drag_coef, Vector3D direction) {
        this.drag_coef = drag_coef;
    }
    
    /** Set the absorption coefficient.
    * @param absorption coefficient absorption coefficient
    * @param Direction Direction of the incident flux
    */
    public void setAbsCoef(double absorb_coef, Vector3D direction) {
        this.absorb_coef = absorb_coef;
    }

    /** Set the specular reflection coefficient.
    * @param absorption coefficient specular reflection coefficient
    * @param Direction Direction of the incident flux
    */
    public void setReflCoef(double refl_coef, Vector3D direction) {
        this.refl_coef = refl_coef;
    }

}
