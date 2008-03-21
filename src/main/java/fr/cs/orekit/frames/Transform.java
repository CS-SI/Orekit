package fr.cs.orekit.frames;

import java.io.Serializable;

import org.apache.commons.math.geometry.Rotation;
import org.apache.commons.math.geometry.Vector3D;

import fr.cs.orekit.utils.PVCoordinates;

/** Transformation class in three dimensional space.
 *
 * <p>This class represents the transformation engine between {@link Frame frames}.
 * It is used both to define the relationship between each frame and its
 * parent frame and to gather all individual transforms into one
 * operation when converting between frames far away from each other.</p>
 * <p> The convention used in OREKIT is vectorial transformation. It means
 * that a transformation is defined as a transform to apply to the
 * coordinates of a vector expressed in the old frame to obtain the
 * same vector expressed in the new frame. <p>
 *
 *  <h5> Example </h5>
 *
 * <pre>
 *
 * 1 ) Example of translation from R<sub>A</sub> to R<sub>B</sub>:
 * We want to transform the {@link PVCoordinates} PV<sub>A</sub> to PV<sub>B</sub>.
 *
 * With :  PV<sub>A</sub> = ( {1,0,0} , {1,0,0} );
 * and  :  PV<sub>B</sub> = ( {0,0,0} , {0,0,0} );
 *
 * The transform to apply then is defined as follows :
 *
 * Vector3D translation = new Vector3D(-1,0,0);
 * Vector3D velocity = new Vector3D(-1,0,0);
 *
 * Transform RAtoRB = new Transform(translation , Velocity);
 *
 * PV<sub>B</sub> = R1toR2.transformPVCoordinates(PV<sub>A</sub>);
 *
 *
 * 2 ) Example of rotation from R<sub>A</sub> to R<sub>B</sub>:
 * We want to transform the {@link PVCoordinates} PV<sub>A</sub> to PV<sub>B</sub>.
 *
 * With :  PV<sub>A</sub> = ( {1,0,0} , {1,0,0} );
 * and  :  PV<sub>B</sub> = ( {0,1,0} , {-2,1,0} );
 *
 * The transform to apply then is defined as follows :
 *
 * Rotation rotation = new Roation(new Vector3D(0,0,1), Math.PI/2);
 * Vector3D rotationRate = new Vector3D(0, 0, -2));
 *
 * Transform R1toR2 = new Transform(rotation , rotationRate);
 *
 * PV<sub>B</sub> = R1toR2.transformPVCoordinates(PV<sub>A</sub>);
 *
 * </pre>
 *
 *  @author L. Maisonobe
 *  @author F. Maussion
 */
public class Transform implements Serializable {

    /** serializable UID. */
    private static final long serialVersionUID = -989814451927839248L;

    /** Global translation. */
    private Vector3D translation;

    /** First time derivative of the translation. */
    private Vector3D velocity;

    /** Global rotation. */
    private Rotation rotation;

    /** First time derivative of the rotation (norm representing angular rate). */
    private Vector3D rotationRate;

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
     * @param rotation rotation to apply ( i.e. rotation to apply to the
     * coordinates of a vector expressed in the old frame to obtain the
     * same vector expressed in the new frame )
     */
    public Transform(Rotation rotation) {
        this(new Vector3D(), new Vector3D(), rotation, new Vector3D());
    }

    /** Build a translation transform, with its first time derivative.
     * @param translation translation to apply (i.e. coordinates of
     * the transformed origin, or coordinates of the origin of the
     * old frame in the new frame)
     * @param velocity the velocity of the translation (i.e. origin
     * of the old frame velocity in the new frame)
     */
    public Transform(Vector3D translation, Vector3D velocity) {
        this(translation, velocity, new Rotation(), new Vector3D());
    }

    /** Build a rotation transform.
     * @param rotation rotation to apply ( i.e. rotation to apply to the
     * coordinates of a vector expressed in the old frame to obtain the
     * same vector expressed in the new frame )
     * @param rotationRate the axis of the instant rotation
     * expressed in the new frame. (norm representing angular rate)
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
        return first.translation.add(
                                     first.rotation.applyInverseTo(second.translation));
    }

    /** The new velocity */
    private static Vector3D compositeVelocity(Transform first, Transform second) {
        return first.velocity.add(
                                  first.rotation.applyInverseTo(second.velocity.add(
                                                                                    Vector3D.crossProduct(first.rotationRate,
                                                                                                          second.translation))));
    }

    /** The new rotation */
    private static Rotation compositeRotation(Transform first, Transform second) {
        return second.rotation.applyTo(first.rotation);
    }

    /** The new rotation rate */
    private static Vector3D compositeRotationRate(Transform first, Transform second) {
        return second.rotationRate.add(second.rotation.applyTo(first.rotationRate));
    }

    /** Get the inverse transform of the instance.
     * @return inverse transform of the instance
     */
    public Transform getInverse() {
        final Vector3D rT = rotation.applyTo(translation);
        return new Transform(rT.negate(),
                             Vector3D.crossProduct(rotationRate, rT).subtract(rotation.applyTo(velocity)),
                             rotation.revert(),
                             rotation.applyInverseTo(rotationRate.negate()));
    }

    /** Transform a position vector (including translation effects).
     * @param position vector to transform
     * @return transformed position
     */
    public Vector3D transformPosition(Vector3D position) {
        return rotation.applyTo(translation.add(position));
    }

    /** Transform a vector (ignoring translation effects).
     * @param vector vector to transform
     * @return transformed vector
     */
    public Vector3D transformVector(Vector3D vector) {
        return rotation.applyTo(vector);
    }

    /** Transform {@link PVCoordinates} including kinematic effects.
     * @param pv the couple position-velocity to transform.
     * @return transformed position/velocity
     */
    public PVCoordinates transformPVCoordinates(PVCoordinates pv) {
        final Vector3D p = pv.getPosition();
        final Vector3D v = pv.getVelocity();
        final Vector3D transformedP = rotation.applyTo(translation.add(p));
        return new PVCoordinates(transformedP,
                                 rotation.applyTo(v.add(velocity)).subtract(
                                                                            Vector3D.crossProduct(rotationRate, transformedP)));
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

}
