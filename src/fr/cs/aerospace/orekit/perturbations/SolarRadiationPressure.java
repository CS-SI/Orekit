package fr.cs.aerospace.orekit.perturbations;

import fr.cs.aerospace.orekit.bodies.OneAxisEllipsoid;
import fr.cs.aerospace.orekit.bodies.ThirdBody;
import fr.cs.aerospace.orekit.errors.OrekitException;
import fr.cs.aerospace.orekit.frames.Frame;
import fr.cs.aerospace.orekit.orbits.OrbitDerivativesAdder;
import fr.cs.aerospace.orekit.time.AbsoluteDate;
import fr.cs.aerospace.orekit.utils.PVCoordinates;
import models.satellite.SolarRadiationPressureSatellite;

import org.spaceroots.mantissa.geometry.Vector3D;

/** Solar radiation pressure force model.
 * @version $Id: SolarRadiationPressure.java 1032 2006-09-28 08:25:21 +0000 (jeu., 28 sept. 2006) fabien $
 * @author E. Delente
 */

public class SolarRadiationPressure implements ForceModel {

  /** Simple constructor.
   * <p>When this constructor is used, the reference values are:</p>
   * <ul>
   *   <li>d<sub>ref</sub> = 149597870000.0 m</li>
   *   <li>p<sub>ref</sub> = 4.56 10<sup>-6</sup> N/m<sup>2</sup></li>
   * </ul>
   * @param sun Sun model
   * @param earth Earth shape model (for umbra/penumbra computation)
   * @param spacecraft spacecraft
   */
  public SolarRadiationPressure(ThirdBody sun, OneAxisEllipsoid earth,
		  SolarRadiationPressureSatellite satellite) {
    this(149597870000.0, 4.56e-6, sun, earth, satellite);
  }

  /** Complete constructor.
   * @param dRef reference distance for the radiation pressure (m)
   * @param pRef reference radiation pressure at dRef (N/m<sup>2</sup>)
   * @param sun Sun model
   * @param earth Earth shape model (for umbra/penumbra computation)
   * @param spacecraft spacecraft
   */
  public SolarRadiationPressure(double dRef, double pRef,
                                ThirdBody sun, OneAxisEllipsoid earth,
                                SolarRadiationPressureSatellite satellite) {
    this.dRef  = dRef;
    this.pRef  = pRef;
    this.sun   = sun;
    this.earth = earth;
    this.spacecraft = satellite;
  }

  /**
   * Compute the contribution of the solar radiation pressure to the perturbing
   * acceleration.
   * @param date current date
   * @param pvCoordinates
   * @param Attitude current Attitude
   * @param adder object where the contribution should be added
   */
  public void addContribution(AbsoluteDate date, PVCoordinates pvCoordinates,           
		                    Frame frame, OrbitDerivativesAdder adder)
      throws OrekitException {

    // raw radiation pressure
    Vector3D satSunVector = Vector3D.subtract(sun.getPosition(date, Frame.getJ2000()),
                                              pvCoordinates.getPosition());
    double dRatio = dRef / satSunVector.getNorm();
    double rawP   = pRef * dRatio * dRatio
                  * getLightningRatio(date, pvCoordinates.getPosition(), satSunVector);

    // spacecraft characteristics effects
    Vector3D u = new Vector3D(satSunVector);
    u.normalizeSelf();
    double kd = (1.0 - spacecraft.getAbsCoef(u , date).getNorm())
              * (1.0 - spacecraft.getReflectionCoef(u , date).getNorm());
    double acceleration = rawP * (1 + kd * 4.0 / 9.0 )
                        * spacecraft.getSurface(u , date) / spacecraft.getMass();
 
    // provide the perturbing acceleration to the derivatives adder
    adder.addXYZAcceleration(acceleration * u.getX(),
                             acceleration * u.getY(),
                             acceleration * u.getZ());
  }

  /** Get the lightning ratio
   * @param date current date
   * @param position the satellite's position
   * @param satSunVector satellite to Sun vector
   * @exception OrekitException if the trajectory is inside the Earth
   */
  public double getLightningRatio(AbsoluteDate date, Vector3D position,
                                   Vector3D satSunVector)
   throws OrekitException {

    // Earth apparent radius
    double r = position.getNorm();
    if (r <= earth.getEquatorialRadius()) {
      throw new OrekitException("underground trajectory (r = {0})",
                                new String[] { Double.toString(r) });
    }
    double alphaEarth = earth.getEquatorialRadius() / r;

    // Definition of the Sun's apparent radius
    double alphaSun = sun.getRadius() / satSunVector.getNorm();

    // Retrieve the Sat-Sun / Sat-Central body angle
    double sunEarthAngle = Math.PI - Vector3D.angle(satSunVector, position);

    // Is the satellite is in the penumbra ?
    if ((sunEarthAngle - alphaEarth - alphaSun <= 0.0)
     && (sunEarthAngle - alphaEarth + alphaSun >= 0.0)) {

      // Is the satellite is in the umbra ?
      if (sunEarthAngle - alphaEarth < 0.0) {
        return 0.0;
      } else {
        // Compute a lightning ratio in penumbra
        // TODO check the lightning ratio model in penumbra
        double alpha1 = (sunEarthAngle * sunEarthAngle
                       - (alphaEarth - alphaSun) * (alphaSun + alphaEarth))
                      / (2 * sunEarthAngle);

        double alpha2 = (sunEarthAngle * sunEarthAngle
                       + (alphaEarth - alphaSun) * (alphaSun + alphaEarth))
                      / (2 * sunEarthAngle);

        double P1 = Math.PI * alphaSun * alphaSun
                  - alphaSun * alphaSun * Math.acos(alpha1 / alphaSun)
                  + alpha1 * Math.sqrt(alphaSun * alphaSun - alpha1 * alpha1);

        double P2 = alphaEarth * alphaEarth * Math.acos(alpha2 / alphaEarth)
                  - alpha2 * Math.sqrt(alphaEarth * alphaEarth - alpha2 * alpha2);

        return (P1 - P2) / (Math.PI * alphaSun * alphaSun);
      }
    }

    return 1.0;

  }

  /** Gets the swithching functions related to umbra and penumbra passes.
   * @return umbra/penumbra switching functions
   */
  public SWF[] getSwitchingFunctions() {
    return new SWF[] { new Umbraswitch(), new Penumbraswitch() };
  }

  /** This class defines the Umbra switching function.
   * It triggers when the satellite enters the umbra zone.
   */
  private class Umbraswitch implements SWF {

    public void eventOccurred(AbsoluteDate t, PVCoordinates pvCoordinates, Frame frame) {
      // do nothing
    }

    /** The G-function is the difference between the Sat-Sun-Sat-Earth angle and
     * the Earth's apparent radius
     */
    public double g(AbsoluteDate date, PVCoordinates pvCoordinates, Frame frame)
        throws OrekitException {
      Vector3D satSunVector = Vector3D.subtract(sun.getPosition(date, frame),
                                                pvCoordinates.getPosition());
      double sunEarthAngle = Math.PI - Vector3D.angle(satSunVector, pvCoordinates.getPosition());
      double r = pvCoordinates.getPosition().getNorm();
      if (r <= earth.getEquatorialRadius()) {
        throw new OrekitException("underground trajectory (r = {0})",
                                  new String[] { Double.toString(r) });
      }
      double alphaEarth = earth.getEquatorialRadius() / r;
      return sunEarthAngle - alphaEarth;
    }

    public double getMaxCheckInterval() {
      // we accept losing umbra passes shorter than one minute
      return 60.0;
    }

    public double getThreshold() {
      // convergence threshold in seconds for umbra events
      return 1.0e-3;
    }

  }

  /** This class defines the penumbra switching function.
   * It triggers when the satellite enters the penumbra zone.
   */
  private class Penumbraswitch implements SWF {

    public void eventOccurred(AbsoluteDate t, PVCoordinates pvCoordinates, Frame frame) {
      // do nothing
    }

    /** The G-function is the difference between the Sat-Sun-Sat-Earth angle and
     * the sum of the Earth's and Sun's apparent radius
     */
    public double g(AbsoluteDate date, PVCoordinates pvCoordinates, Frame frame)
        throws OrekitException {
      Vector3D satSunVector = Vector3D.subtract(sun.getPosition(date, frame),
                                                pvCoordinates.getPosition());
      double sunEarthAngle = Math.PI - Vector3D.angle(satSunVector, pvCoordinates.getPosition());
      double r = pvCoordinates.getPosition().getNorm();
      if (r <= earth.getEquatorialRadius()) {
        throw new OrekitException("underground trajectory (r = {0})",
                                  new String[] { Double.toString(r) });
      }
      double alphaEarth = earth.getEquatorialRadius() / r;
      double alphaSun   = sun.getRadius() / satSunVector.getNorm();
      return sunEarthAngle - alphaEarth - alphaSun;
    }

    public double getMaxCheckInterval() {
      // we accept losing penumbra passes shorter than one minute
      return 60.0;
    }

    public double getThreshold() {
      // convergence threshold in seconds for penumbra events
      return 1.0e-3;
    }

  }

  /** Reference distance (m). */
  private double dRef;

  /** Reference radiation pressure at dRef (N/m<sup>2</sup>).*/
  private double pRef;

  /** Sun model. */
  private ThirdBody sun;

  /** Earth model. */
  private OneAxisEllipsoid earth;

  /** Spacecraft. */
  private SolarRadiationPressureSatellite spacecraft;

}
