package fr.cs.aerospace.orekit.frames;

import org.spaceroots.mantissa.geometry.Rotation;
import org.spaceroots.mantissa.geometry.Vector3D;

/** Transformation class in three dimensional space.
 * <p>This class represent the transformation engine between {@link Frame frames}.
 * It is used both to define the relationship between each frame and its
 * parent frame and to compute the gather all individual transforms into one
 * operation when converting between frames far away from each other tree-wise.</p>
 *  @author Luc Maisonobe
 */
public class Transform {

  /** Build a transform from its primitive operations.
   * @param translation first primitive operation to apply
   * @param rotation second primitive operation to apply
   */
  private Transform(Vector3D translation, Rotation rotation) {
    this.translation = translation;
    this.rotation    = rotation;
  }

  /** Build an identity transform.
   */
  public Transform() {
    this(new Vector3D(), new Rotation());
  }

  /** Build a translation transform.
   * @param translation translation to apply (i.e. coordinates of
   * the transformed origin, or coordinates of the origin of the
   * new frame in the old frame)
   */
  public Transform(Vector3D translation) {
    this(translation, new Rotation());
  }

  /** Build a rotation transform.
   * @param rotation rotation to apply
   */
  public Transform(Rotation rotation) {
    this(new Vector3D(), rotation);
  }

  /** Build a transform by combining two existing ones.
   * @param first first transform applied
   * @param second second transform applied
   */
  public Transform(Transform first, Transform second) {
    this(Vector3D.add(first.translation,
                      first.rotation.applyInverseTo(second.translation)),
         second.rotation.applyTo(first.rotation));
  }
  
  /** Get the inverse transform of the instance.
   * @return inverse transform of the instance
   */
  public Transform getInverse() {
    return new Transform(rotation.applyTo(Vector3D.negate(translation)),
                         rotation.revert());
  }

  /** Transform a position vector (including translation effects).
   * @param position vector to transform
   */
  public Vector3D transformPosition(Vector3D position) {
    return rotation.applyTo(Vector3D.add(translation, position));
  }

  /** Transform a direction vector (ignoring translation effects).
   * @param direction vector to transform
   */
  public Vector3D transformDirection(Vector3D direction) {
    return rotation.applyTo(direction);
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

  /** Global translation. */
  private Vector3D translation;

  /** Global rotation. */
  private Rotation rotation;

}
