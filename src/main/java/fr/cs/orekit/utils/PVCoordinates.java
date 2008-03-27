package fr.cs.orekit.utils;

import java.io.Serializable;

import org.apache.commons.math.geometry.Vector3D;

/** Simple container for Position/Velocity pairs.
 * <p>Instances of this class are guaranteed to be immutable.</p>
 * @author Fabien Maussion
 * @author Luc Maisonobe
 */
public class PVCoordinates implements Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = 8581359579182947710L;

    /** The position. */
    private final Vector3D position;

    /** The velocity. */
    private final Vector3D velocity;

    /** Simple constructor.
     * <p> Sets the Coordinates to default : (0 0 0) (0 0 0).
     */
    public PVCoordinates() {
        position = Vector3D.zero;
        velocity = Vector3D.zero;
    }

    /** Builds a PVCoordinates couple.
     * @param position the position vector (m)
     * @param velocity the velocity vector (m/s)
     */
    public PVCoordinates(Vector3D position, Vector3D velocity) {
        this.position = position;
        this.velocity = velocity;
    }

    /** Multiplicative constructor
     * Build a PVCoordinates from another one and a scale factor. 
     * The PVCoordinates built will be a * pv
     * @param a scale factor
     * @param pv base (unscaled) PVCoordinates
     */
    public PVCoordinates(double a, PVCoordinates pv) {
      this(new Vector3D(a, pv.position), new Vector3D(a, pv.velocity));
    }

    /** Linear constructor
     * Build a PVCoordinates from two other ones and corresponding scale factors.
     * The PVCoordinates built will be a1 * u1 + a2 * u2
     * @param a1 first scale factor
     * @param pv1 first base (unscaled) PVCoordinates
     * @param a2 second scale factor
     * @param pv2 second base (unscaled) PVCoordinates
     */
    public PVCoordinates(double a1, PVCoordinates pv1, double a2, PVCoordinates pv2) {
        this(new Vector3D(a1, pv1.position, a2, pv2.position),
             new Vector3D(a1, pv1.velocity, a2, pv2.velocity));
    }

    /** Linear constructor
     * Build a PVCoordinates from three other ones and corresponding scale factors.
     * The PVCoordinates built will be a1 * u1 + a2 * u2 + a3 * u3
     * @param a1 first scale factor
     * @param pv1 first base (unscaled) PVCoordinates
     * @param a2 second scale factor
     * @param pv2 second base (unscaled) PVCoordinates
     * @param a3 third scale factor
     * @param pv3 third base (unscaled) PVCoordinates
     */
    public PVCoordinates(double a1, PVCoordinates pv1, double a2, PVCoordinates pv2,
                         double a3, PVCoordinates pv3) {
        this(new Vector3D(a1, pv1.position, a2, pv2.position, a3, pv3.position),
             new Vector3D(a1, pv1.velocity, a2, pv2.velocity, a3, pv3.velocity));
    }

    /** Linear constructor
     * Build a PVCoordinates from four other ones and corresponding scale factors.
     * The PVCoordinates built will be a1 * u1 + a2 * u2 + a3 * u3 + a4 * u4
     * @param a1 first scale factor
     * @param pv1 first base (unscaled) PVCoordinates
     * @param a2 second scale factor
     * @param pv2 second base (unscaled) PVCoordinates
     * @param a3 third scale factor
     * @param pv3 third base (unscaled) PVCoordinates
     * @param a4 fourth scale factor
     * @param pv4 fourth base (unscaled) PVCoordinates
     */
    public PVCoordinates(double a1, PVCoordinates pv1, double a2, PVCoordinates pv2,
                         double a3, PVCoordinates pv3, double a4, PVCoordinates pv4) {
        this(new Vector3D(a1, pv1.position, a2, pv2.position,
                          a3, pv3.position, a4, pv4.position),
             new Vector3D(a1, pv1.velocity, a2, pv2.velocity,
                          a3, pv3.velocity, a4, pv4.velocity));
    }

    /** Gets the position.
     * @return the position vector (m).
     */
    public Vector3D getPosition() {
        return position;
    }

    /** Gets the velocity
     * @return the velocity vector (m/s).
     */
    public Vector3D getVelocity() {
        return velocity;
    }

    public String toString() {
        return new StringBuffer().append('{').append("P(").
                                  append(position.getX()).append(", ").
                                  append(position.getY()).append(", ").
                                  append(position.getZ()).append("), V(").
                                  append(velocity.getX()).append(", ").
                                  append(velocity.getY()).append(", ").
                                  append(velocity.getZ()).append(")}").toString();
    }

}
