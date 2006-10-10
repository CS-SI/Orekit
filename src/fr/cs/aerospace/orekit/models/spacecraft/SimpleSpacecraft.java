package fr.cs.aerospace.orekit.models.spacecraft;

import org.spaceroots.mantissa.geometry.Vector3D;

import fr.cs.aerospace.orekit.time.AbsoluteDate;

/** This class represents the features of a simplified spacecraft.
 * <p>The model of this spacecraft is a simple spherical model, this
 * means that all coefficients are constant and do not depend of
 * the direction. As such, it is a simple container that returns the
 * values set in the constructor.</p>
 * @version $Id: SimpleVehicle.java 831 2003-05-12 16:03:04Z internMS $
 * @author E. Delente
 */

public class SimpleSpacecraft implements SolarRadiationPressureSatellite, AtmosphereDragSatellite, KeplerianSatellite {

  /** Simple constructor.
   * @param mass Mass (kg)
   * @param surface Surface (m<sup>2</sup>)
   * @param dragCoeff Drag coefficient
   * @param absorptionCoeff coefficient Absorption coefficient
   * @param reflectionCoeff Specular reflection coefficient
   */
  public SimpleSpacecraft(double mass, double surface, double dragCoeff,
                          double absorptionCoeff, double reflectionCoeff) {

    this.mass = mass;
    this.surface = surface;
    this.dragCoeff = dragCoeff;
    this.absorptionCoeff = absorptionCoeff;
    this.reflectionCoeff = reflectionCoeff;
  }

  /** Get the mass.
   * @return mass (kg)
   */
  public double getMass() {
    return this.mass;
  }

  /** Get the surface.
   * @param direction direction of the flux
   * (ignored in this implementation)
   * @return surface (m<sup>2</sup>)
   */
  public double getSurface(Vector3D direction, AbsoluteDate t) {
    return surface;
  }

  /** Get the drag coefficients vector.
   * @param direction direction of the atmospheric flux
   * @return drag coefficients vector
   */
  public Vector3D getDragCoef(Vector3D direction, AbsoluteDate t) {
    return new Vector3D(dragCoeff, direction);
  }

  /** Get the absorption coefficients vector.
   * @param direction direction of the light flux
   * @return absorption coefficients vector
   */
  public Vector3D getAbsCoef(Vector3D direction, AbsoluteDate t) {
    return new Vector3D(absorptionCoeff, direction);
  }

  /** Get the specular reflection coefficients vector.
   * @param direction direction of the light flux
   * @return specular reflection coefficients vector
   */
  public Vector3D getReflectionCoef(Vector3D direction, AbsoluteDate t) {
    return new Vector3D(reflectionCoeff, direction);
  }

  /** Set the mass.
   * @param mass new mass (kg)
   */
  public void setMass(double mass) {
    this.mass = mass;
  }

  /** Set the surface.
   * @param surface surface (m<sup>2</sup>)
   */
  public void setSurface(double surface) {
    this.surface = surface;
  }

  /** Set the drag coefficient.
   * @param dragCoeff coefficient drag coefficient
   */
  public void setDragCoef(double dragCoeff) {
    this.dragCoeff = dragCoeff;
  }

  /** Set the absorption coefficient.
   * @param absorptionCoeff coefficient absorption coefficient
   */
  public void setAbsCoef(double absorptionCoeff) {
    this.absorptionCoeff = absorptionCoeff;
  }

  /** Set the specular reflection coefficient.
   * @param reflectionCoeff coefficient specular reflection coefficient
   */
  public void setReflectionCoef(double reflectionCoeff) {
    this.reflectionCoeff = reflectionCoeff;
  }

  /** Mass (kg). */
  private double mass;

  /** Surface (m<sup>2</sup>). */
  private double surface;

  /** Drag coefficient. */
  private double dragCoeff;

  /** Absorption coefficient. */
  private double absorptionCoeff;

  /** Specular reflection coefficient. */
  private double reflectionCoeff;

}
