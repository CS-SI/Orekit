package fr.cs.aerospace.orekit.geometry;

/** Simple container for a point near a 2D body surface.
 * <p>This class is a simple container, it does not provide any processing method.</p>
 * @author Luc Maisonobe
 * $Id$
 *
 */
public class NearSurfacePoint {

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
  public NearSurfacePoint(double longitude, double latitude, double altitude) {
    this.longitude = longitude;
    this.latitude  = latitude;
    this.altitude  = altitude;
  }

}
