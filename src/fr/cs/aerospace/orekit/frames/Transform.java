package fr.cs.aerospace.orekit.frames;

import java.io.Serializable;

import org.spaceroots.mantissa.geometry.Rotation;
import org.spaceroots.mantissa.geometry.Vector3D;

import fr.cs.aerospace.orekit.utils.PVCoordinates;

/** Transformation class in three dimensional space.
 * <p>This class represents the transformation engine between {@link Frame frames}.
 * It is used both to define the relationship between each frame and its
 * parent frame and to gather all individual transforms into one
 * operation when converting between frames far away from each other.</p>
 *  @author L. Maisonobe
 *  @author F. Maussion
 */
public class Transform 
 implements Serializable {

  /** Build a transform from its primitive operations.
   * @param translation first primitive operation to apply
   * @param velocity first time derivative of the translation
   * @param rotation second primitive operation to apply
   * @param rotationRate first time derivative of the rotation (norm representing angular rate)
   */
  private Transform(Vector3D translation, Vector3D velocity,
                    Rotation rotation, Vector3D rotationRate) {
    this.translation  = translation;
    this.rotation     = rotation;
    this.velocity     = velocity;
    this.rotationRate = rotationRate;
  }

  /** Build an identity transform.
   */
  public Transform() {
    this(new Vector3D(), new Vector3D(), new Rotation(), new Vector3D());
  }

  /** Build a translation transform.
   * @param translation translation to apply (i.e. coordinates of
   * the transformed origin, or coordinates of the origin of the
   * old frame in the new frame)
   */
  public Transform(Vector3D translation) {
    this(translation, new Vector3D(), new Rotation(), new Vector3D());
  }

  /** Build a rotation transform.
   * @param rotation rotation to apply
   */
  public Transform(Rotation rotation) {
    this(new Vector3D(), new Vector3D(), rotation, new Vector3D());
  }

  /** Build a translation transform, with its first time derivative.
   * @param translation translation to apply (i.e. coordinates of
   * the transformed origin, or coordinates of the origin of the
   * old frame in the new frame) 
   * @param velocity the velocity of the translation (i.e. velocity 
   * of the transformed origin)
   */
  public Transform(Vector3D translation, Vector3D velocity) {
	  this(translation, velocity, new Rotation(), new Vector3D());
  }
  
  /** Build a rotation transform.
   * @param rotation rotation to apply
   * @param rotationRate the axis of the instant rotation
   *  expressed in the new frame. (norm representing angular rate) 
   */
  public Transform(Rotation rotation, Vector3D rotationRate) {
	  this(new Vector3D(), new Vector3D(), rotation, rotationRate);
  }
  
  /** Build a transform by combining two existing ones.
   * @param first first transform applied
   * @param second second transform applied
   */
  public Transform(Transform first, Transform second) {
    this(compositeTranslation(first, second), compositeVelocity(first, second),
         compositeRotation(first, second), compositeRotationRate(first, second));
  }
  
  /** The new translation */ 
  private static Vector3D compositeTranslation(Transform first, Transform second) {
    return Vector3D.add(first.translation,
                        first.rotation.applyInverseTo(second.translation));
  }
  
  /** The new velocity */ 
  private static Vector3D compositeVelocity(Transform first, Transform second) {
    return Vector3D.add(first.velocity,
                        first.rotation.applyInverseTo(Vector3D.subtract(second.velocity,
                                                                        Vector3D.crossProduct(first.rotationRate,
                                                                                              second.translation))));
  }
  
  /** The new rotation */ 
  private static Rotation compositeRotation(Transform first, Transform second) {
    return second.rotation.applyTo(first.rotation);
  }
  
  /** The new rotation rate */ 
  private static Vector3D compositeRotationRate(Transform first, Transform second) {
    return Vector3D.add(second.rotationRate, second.rotation.applyTo(first.rotationRate));
  }
  
  /** Get the inverse transform of the instance.
   * @return inverse transform of the instance
   */
  public Transform getInverse() {
	Vector3D reversedTranslation = rotation.applyTo(Vector3D.negate(translation));
    return new Transform(reversedTranslation, 
                         Vector3D.subtract(Vector3D.crossProduct(rotationRate, reversedTranslation),
                                           rotation.applyTo(velocity)),
                         rotation.revert(),
                         rotation.applyInverseTo(Vector3D.negate(rotationRate)));
  }

  /** Transform a position vector (including translation effects).
   * @param position vector to transform
   */
  public Vector3D transformPosition(Vector3D position) {
    return rotation.applyTo(Vector3D.add(translation, position));
  }

  /** Transform a vector (ignoring translation effects).
   * @param vector vector to transform
   */
  public Vector3D transformVector(Vector3D vector) {
    return rotation.applyTo(vector);
  } 
  
  /** Transform {@link PVCoordinates} including cinematic effects.
   * @param pv the couple position-velocity to tranform.
   */
  public PVCoordinates transformPVCoordinates(PVCoordinates pv) {
    Vector3D p = pv.getPosition();
    Vector3D v = pv.getVelocity();
    Vector3D transformedP = rotation.applyTo(Vector3D.add(translation, p));
    return new PVCoordinates(transformedP,
                             Vector3D.add(Vector3D.crossProduct(rotationRate, transformedP),
                                          rotation.applyTo(Vector3D.add(v, velocity))));
  }

  /** Get the underlying elementary translation.
   * <p>A transform can be uniquely represented as an elementary
   * translation followed by an elementary rotation. This method
   * returns this unique elementary translation.</p>
   * @return underlying elementary translation
   * @see #getRotation()
   */
  public Vector3D getTranslation() {
    return translation;
  }

  /** Get the first time derivative of the translation.
   * @return first time derivative of the translation
   */
  public Vector3D getVelocity() {
    return velocity;
  }

  /** Get the underlying elementary rotation.
   * <p>A transform can be uniquely represented as an elementary
   * translation followed by an elementary rotation. This method
   * returns this unique elementary rotation.</p>
   * @return underlying elementary rotation
   * @see #getTranslation()
   */
  public Rotation getRotation() {
    return rotation;
  }

  /** Get the first time derivative of the rotation.
   * <p>The norm represents the angular rate.</p>
   * @return First time derivative of the rotation
   */
  public Vector3D getRotAxis() {
  	return rotationRate;
  }

  /** Global translation. */
  private Vector3D translation;

  /** First time derivative of the translation. */
  private Vector3D velocity;
  
  /** Global rotation. */
  private Rotation rotation;

  /** First time derivative of the rotation (norm representing angular rate). */
  private Vector3D rotationRate;

  private static final long serialVersionUID = -4819571244806665519L;
}
