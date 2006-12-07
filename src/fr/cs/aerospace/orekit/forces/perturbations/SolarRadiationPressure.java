package fr.cs.aerospace.orekit.forces.perturbations;

import org.spaceroots.mantissa.geometry.Vector3D;

import fr.cs.aerospace.orekit.attitudes.AttitudeKinematics;
import fr.cs.aerospace.orekit.bodies.OneAxisEllipsoid;
import fr.cs.aerospace.orekit.bodies.ThirdBody;
import fr.cs.aerospace.orekit.errors.OrekitException;
import fr.cs.aerospace.orekit.forces.ForceModel;
import fr.cs.aerospace.orekit.forces.SWF;
import fr.cs.aerospace.orekit.frames.Frame;
import fr.cs.aerospace.orekit.models.spacecraft.SolarRadiationPressureSpacecraft;
import fr.cs.aerospace.orekit.propagation.TimeDerivativesEquations;
import fr.cs.aerospace.orekit.time.AbsoluteDate;
import fr.cs.aerospace.orekit.utils.PVCoordinates;

/** Solar radiation pressure force model.
 * 
 * @author F. Maussion , E.Delente
 */

public class SolarRadiationPressure implements ForceModel {

  /** Simple constructor.
   * <p>When this constructor is used, the reference values are:</p>
   * <ul>
   *   <li>d<sub>ref</sub> = 149597870000.0 m</li>
   *   <li>p<sub>ref</sub> = 4.56 10<sup>-6</sup> N/m<sup>2</sup></li>
   * </ul>
   * @param sun Sun model
   * @param centralBody centralBody shape model (for umbra/penumbra computation)
   * @param spacecraft the object which is pertubated by the pressure
   */
  public SolarRadiationPressure(ThirdBody sun, OneAxisEllipsoid centralBody,
                                SolarRadiationPressureSpacecraft spacecraft) {
    this(149597870000.0, 4.56e-6, sun, centralBody, spacecraft);
  }

  /** Complete constructor.
   * @param dRef reference distance for the radiation pressure (m)
   * @param pRef reference radiation pressure at dRef (N/m<sup>2</sup>)
   * @param sun Sun model
   * @param centralBody centralBody shape model (for umbra/penumbra computation)
   * @param spacecraft the object which is pertubated by the pressure
   */
  public SolarRadiationPressure(double dRef, double pRef, ThirdBody sun, 
                                OneAxisEllipsoid centralBody, SolarRadiationPressureSpacecraft spacecraft) {
    this.dRef  = dRef;
    this.pRef  = pRef;
    this.sun   = sun;
    this.centralBody = centralBody;
    this.spacecraft = spacecraft;
  }

  /** Compute the contribution of the solar radiation pressure to the perturbing
   * acceleration.
   * @param pvCoordinates
   * @param adder object where the contribution should be added
   * @param date current date
   */	
  public void addContribution(AbsoluteDate date, PVCoordinates pvCoordinates, 
                              Frame frame, double mass, AttitudeKinematics ak, TimeDerivativesEquations adder)
  throws OrekitException {

    // raw radiation pressure
    Vector3D satSunVector = Vector3D.subtract(sun.getPosition(date , frame),
                                              pvCoordinates.getPosition());

    double dRatio = dRef / satSunVector.getNorm();
    double rawP   = pRef * dRatio * dRatio
    * getLightningRatio(
                        pvCoordinates.getPosition(), frame, date);

    // spacecraft characteristics effects
    Vector3D u = new Vector3D(satSunVector);
    u.normalizeSelf();
    Vector3D inSpacecraft = ak.getAttitude().applyTo(u);
    double kd = (1.0 - spacecraft.getAbsCoef(inSpacecraft).getNorm())
    * (1.0 - spacecraft.getReflectionCoef(inSpacecraft).getNorm());
    double acceleration = rawP * (1 + kd * 4.0 / 9.0 )
    * spacecraft.getSurface(inSpacecraft) / mass;

    // provide the perturbing acceleration to the derivatives adder

    adder.addXYZAcceleration(acceleration * u.getX(),
                             acceleration * u.getY(),
                             acceleration * u.getZ());

  }

  /** Get the lightning ratio.
   * @param position the satellite's position in the selected frame.
   * @param frame in which is defined the position
   * @param date the date
   * @exception OrekitException if the trajectory is inside the Earth
   */
  public double getLightningRatio(Vector3D position, Frame frame, AbsoluteDate date)
  throws OrekitException {
    Vector3D satSunVector = Vector3D.subtract(sun.getPosition(date, frame),
                                              position);
    // Earth apparent radius
    double r = position.getNorm();
    if (r <= centralBody.getEquatorialRadius()) {
      throw new OrekitException("trajectory inside the Brillouin sphere (r = {0})",
                                new String[] { Double.toString(r) });
    }

    double alphaEarth = Math.atan(centralBody.getEquatorialRadius() / r);

    // Definition of the Sun's apparent radius
    double alphaSun = sun.getRadius() / satSunVector.getNorm();

    // Retrieve the Sat-Sun / Sat-Central body angle
    double sunEarthAngle = Vector3D.angle(satSunVector, Vector3D.negate(position));

    double result = 1.0;

    // Is the satellite is in complete penumbra ?
    if (sunEarthAngle - alphaEarth + alphaSun < 0.0) {
      result = 0.0;
    }
    // Compute a lightning ratio in penumbra
    if ((sunEarthAngle - alphaEarth + alphaSun >= 0.0)&&((sunEarthAngle - alphaEarth - alphaSun <= 0.0))) {

      //result = (alphaSun + sunEarthAngle - alphaEarth) / (2*alphaSun);

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

      result =  (P1 - P2) / (Math.PI * alphaSun * alphaSun);


    }

    return result;

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

    public void eventOccurred(AbsoluteDate t, PVCoordinates pvCoordinates, Frame frame, double mass, AttitudeKinematics ak) {
      // do nothing
    }

    /** The G-function is the difference between the Sat-Sun-Sat-Earth angle and
     * the Earth's apparent radius
     */
    public double g(AbsoluteDate date, PVCoordinates pvCoordinates, Frame frame, double mass, AttitudeKinematics ak)
    throws OrekitException {
      Vector3D satSunVector = Vector3D.subtract(sun.getPosition(date, frame),
                                                pvCoordinates.getPosition());
      double sunEarthAngle = Math.PI - Vector3D.angle(satSunVector, pvCoordinates.getPosition());
      double r = pvCoordinates.getPosition().getNorm();
      if (r <= centralBody.getEquatorialRadius()) {
        throw new OrekitException("trajectory inside the Brillouin sphere (r = {0})",
                                  new String[] { Double.toString(r) });
      }
      double alphaEarth = centralBody.getEquatorialRadius() / r;
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

    public void eventOccurred(AbsoluteDate t, PVCoordinates pvCoordinates, Frame frame, double mass, AttitudeKinematics ak) {
      // do nothing
    }

    /** The G-function is the difference between the Sat-Sun-Sat-Earth angle and
     * the sum of the Earth's and Sun's apparent radius
     */
    public double g(AbsoluteDate date, PVCoordinates pvCoordinates, Frame frame, double mass, AttitudeKinematics ak)
    throws OrekitException {
      Vector3D satSunVector = Vector3D.subtract(sun.getPosition(date , frame),
                                                pvCoordinates.getPosition());
      double sunEarthAngle = Math.PI - Vector3D.angle(satSunVector, pvCoordinates.getPosition());
      double r = pvCoordinates.getPosition().getNorm();
      if (r <= centralBody.getEquatorialRadius()) {
        throw new OrekitException("trajectory inside the Brillouin sphere (r = {0})",
                                  new String[] { Double.toString(r) });
      }
      double alphaEarth = centralBody.getEquatorialRadius() / r;
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
  private OneAxisEllipsoid centralBody;

  /** Spacecraft. */
  private SolarRadiationPressureSpacecraft spacecraft;
}
