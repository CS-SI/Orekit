package fr.cs.orekit.bodies;

/** Point location relative to a 2D body surface.
 * <p>This class is a simple immutable container,
 * it does not provide any processing method.</p>
 * @see BodyShape
 * @author Luc Maisonobe
 */
public class GeodeticPoint {

    /** Longitude of the point (rad). */
    public final double longitude;

    /** Latitude of the point (rad). */
    public final double latitude;

    /** Altitude of the point (m). */
    public final double altitude;

    /** Build a new instance.
     * @param longitude longitude of the point
     * @param latitude of the point
     * @param altitude altitude of the point
     */
    public GeodeticPoint(double longitude, double latitude, double altitude) {
        this.longitude = longitude;
        this.latitude  = latitude;
        this.altitude  = altitude;
    }

}
