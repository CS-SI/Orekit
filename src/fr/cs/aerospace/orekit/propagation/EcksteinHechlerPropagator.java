package fr.cs.aerospace.orekit.propagation;

import fr.cs.aerospace.orekit.attitudes.AttitudeProvider;
import fr.cs.aerospace.orekit.errors.PropagationException;
import fr.cs.aerospace.orekit.orbits.CircularParameters;
import fr.cs.aerospace.orekit.orbits.Orbit;
import fr.cs.aerospace.orekit.time.AbsoluteDate;

/** This class propagates an {@link fr.cs.aerospace.orekit.orbits.Orbit Orbit}
 *  using the analytical Eckstein-Hechler model.
 * <p>The Eckstein-Hechler model is suited for near circular orbits
 * (e < 0.1, with poor accuracy between 0.005 and 0.1) and inclination
 * neither equatorial (direct or retrograde) nor critical (direct or
 * retrograde).</p>
 * @see Orbit
 * @version $Id$
 * @author G. Prat
 */
public class EcksteinHechlerPropagator implements Ephemeris {

  /** Create a new instance.
   * <p>The C<sub>n,0</sub> coefficients are the denormalized zonal coefficients, they
   * are related to both the normalized coefficients <span style="text-decoration: overline">C</span><sub>n,0</sub>
   *  and the J<sub>n</sub> one
   * as follows:</p>
   * <pre>
   *   C<sub>n,0</sub> = [(2-&delta;<sub>0,m</sub>)(2n+1)(n-m)!/(n+m)!]<sup>&frac12;</sup><span style="text-decoration: overline">C</span><sub>n,0</sub>
   *   C<sub>n,0</sub> = -J<sub>n</sub>
   * </pre>
   * @param orbit initial orbit
   * @param referenceRadius reference radius of the Earth for the potential model (m)
   * @param mu central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
   * @param C<sub>2,0</sub> un-normalized zonal coefficient (about -1.08e-3 for Earth)
   * @param C<sub>3,0</sub> un-normalized zonal coefficient (about +2.53e-6 for Earth)
   * @param C<sub>4,0</sub> un-normalized zonal coefficient (about +1.62e-6 for Earth)
   * @param C<sub>5,0</sub> un-normalized zonal coefficient (about +2.28e-7 for Earth)
   * @param C<sub>6,0</sub> un-normalized zonal coefficient (about -5.41e-7 for Earth)
   * @exception PropagationException if the mean parameters cannot be computed
   */
  public EcksteinHechlerPropagator(SpacecraftState initialState, double referenceRadius, double mu,
                                   double c20, double c30, double c40,
                                   double c50, double c60)
  throws PropagationException {

    // store model coefficients
    this.referenceRadius = referenceRadius;
    this.mu  = mu;
    this.c20 = c20;
    this.c30 = c30;
    this.c40 = c40;
    this.c50 = c50;
    this.c60 = c60;

    // transformation into circular adapted parameters
    // (used by the Eckstein-Hechler model)
    CircularParameters osculating = new CircularParameters(initialState.getParameters(), mu);

    // compute mean parameters
    initialDate = initialState.getDate();
    mass = initialState.getMass();
    this.attitude = initialState.getAttitudeProvider();
    computeMeanParameters(osculating);

  }

  /** Get the state extrapolated up to a given date with an analytical model.
   * The extrapolated parameters are osculating circular parameters.
   * @param date target date for the propagation
   * @return propagated state (in circular parameters)
   */
  public SpacecraftState getSpacecraftState(AbsoluteDate date)
      throws PropagationException {
    return new SpacecraftState(new Orbit(date, propagate(date)), mass, attitude);
  }

  /** Compute mean parameters according to the Eckstein-Hechler analytical model.
   * @param osculating osculating orbit
   * @throws PropagationException
   */
  private void computeMeanParameters(CircularParameters osculating)
    throws PropagationException {

    // sanity check
    if (osculating.getA() < referenceRadius) {
      throw new PropagationException("trajectory inside the Brillouin sphere (r = {0})",
                                     new String[] {
                                       Double.toString(osculating.getA())
                                     });
    }

    // rough initialization of the mean parameters
    mean = new CircularParameters(osculating , mu);

    // threshold for each parameter
    double epsilon         = 1.0e-13;
    double thresholdA      = epsilon * (1 + Math.abs(mean.getA()));
    double thresholdE      = epsilon * (1 + mean.getE());
    double thresholdAngles = epsilon * Math.PI;

    int i = 0;
    while (i++ < 100) {

      // preliminary processing
      q = referenceRadius / mean.getA();
      ql = q * q;
      g2 = c20 * ql;
      ql *= q;
      g3 = c30 * ql;
      ql *= q;
      g4 = c40 * ql;
      ql *= q;
      g5 = c50 * ql;
      ql *= q;
      g6 = c60 * ql;

      cosI1 = Math.cos(mean.getI());
      sinI1 = Math.sin(mean.getI());
      sinI2 = sinI1 * sinI1;
      sinI4 = sinI2 * sinI2;
      sinI6 = sinI2 * sinI4;

      // recompute the osculation parameters from the current mean parameters
      CircularParameters rebuilt = propagate(initialDate);

      // adapted parameters residuals
      double deltaA      = osculating.getA()  - rebuilt.getA();
      double deltaEx     = osculating.getCircularEx() - rebuilt.getCircularEx();
      double deltaEy     = osculating.getCircularEy() - rebuilt.getCircularEy();
      double deltaI      = osculating.getI()  - rebuilt.getI();
      double deltaRAAN   = trimAngle(osculating.getRightAscensionOfAscendingNode()
                                    - rebuilt.getRightAscensionOfAscendingNode(),
                                      0.0);
      double deltaAlphaM = trimAngle(osculating.getAlphaM() - rebuilt.getAlphaM(),
                                        0.0);

      // update mean parameters
      mean= new CircularParameters(mean.getA()          + deltaA,
                 mean.getCircularEx() + deltaEx,
                 mean.getCircularEy() + deltaEy,
                 mean.getI()          + deltaI,
                 mean.getRightAscensionOfAscendingNode() + deltaRAAN,
                 mean.getAlphaM()     + deltaAlphaM,
                 CircularParameters.MEAN_LONGITUDE_ARGUMENT,
                 mean.getFrame());

      // check convergence
      if ((Math.abs(deltaA)         < thresholdA)
          && (Math.abs(deltaEx)     < thresholdE)
          && (Math.abs(deltaEy)     < thresholdE)
          && (Math.abs(deltaI)      < thresholdAngles)
          && (Math.abs(deltaRAAN)   < thresholdAngles)
          && (Math.abs(deltaAlphaM) < thresholdAngles)) {

        // sanity checks
        double e = mean.getE();
        if (e > 0.1) {
          // if 0.005 < e < 0.1 no error is triggered, but accuracy is poor
          throw new PropagationException("too excentric orbit (e = {0})",
                                         new String[] { Double.toString(e) });
        }

        double meanI = mean.getI();
        if ((meanI < 0.) || (meanI > Math.PI)
            || (Math.abs(Math.sin(meanI)) < 1.0e-10)) {
          throw new PropagationException("almost equatorial orbit (i = {0} degrees)",
                                         new String[] {
                                           Double.toString(Math.toDegrees(meanI))
                                         });
        }

        if ((Math.abs(meanI - 1.1071487) < 1.0e-3) || (Math.abs(meanI - 2.0344439) < 1.0e-3)) {
          throw new PropagationException("almost critically inclined orbit (i = {0} degrees)",
                                         new String[] {
                                           Double.toString(Math.toDegrees(meanI))
                                         });
        }

        return;

      }

    }

    throw new PropagationException("unable to compute Eckstein-Hechler mean"
                                 + " parameters after {0} iterations",
                                   new String[] { Integer.toString(i) });

  }

  /** Extrapolate an orbit up to a specific target date.
   * @param targetDate target date for the orbit
   * @exception PropagationException if some parameters are out of bounds
   */
  private CircularParameters propagate(AbsoluteDate date)
    throws PropagationException {

    // keplerian evolution
    double xnot = date.minus(initialDate) * Math.sqrt(mu / mean.getA()) / mean.getA();

    // secular effects

    // eccentricity
    double rdpom = -0.75 * g2 * (4.0 - 5.0 * sinI2);
    double rdpomp = 7.5 * g4 * (1.0 - 31.0 / 8.0 * sinI2 + 49.0 / 16.0 * sinI4)
        - 13.125 * g6
        * (1.0 - 8.0 * sinI2 + 129.0 / 8.0 * sinI4 - 297.0 / 32.0 * sinI6);
    double x = (rdpom + rdpomp) * xnot;
    double cx = Math.cos(x);
    double sx = Math.sin(x);
    q = 3.0 / (32.0 * rdpom);
    double eps1 = q * g4 * sinI2 * (30.0 - 35.0 * sinI2) - 175.0 * q * g6
        * sinI2 * (1.0 - 3.0 * sinI2 + 2.0625 * sinI4);
    q = 3.0 * sinI1 / (8.0 * rdpom);
    double eps2 = q * g3 * (4.0 - 5.0 * sinI2) - q * g5
        * (10.0 - 35.0 * sinI2 + 26.25 * sinI4);
    double exm = mean.getCircularEx() * cx - (1.0 - eps1) * mean.getCircularEy() * sx + eps2 * sx;
    double eym = (1.0 + eps1) * mean.getCircularEx() * sx + (mean.getCircularEy() - eps2) * cx + eps2;

    // inclination
    double xim = mean.getI();

    // right ascension of ascending node
    q = 1.50 * g2 - 2.25 * g2 * g2 * (2.5 - 19.0 / 6.0 * sinI2) + 0.9375 * g4
        * (7.0 * sinI2 - 4.0) + 3.28125 * g6
        * (2.0 - 9.0 * sinI2 + 8.25 * sinI4);
    double omm = trimAngle(mean.getRightAscensionOfAscendingNode() + q * cosI1 * xnot,
                           Math.PI);

    // latitude argument
    double rdl = 1.0 - 1.50 * g2 * (3.0 - 4.0 * sinI2);
    q = rdl + 2.25 * g2 * g2
        * (9.0 - 263.0 / 12.0 * sinI2 + 341.0 / 24.0 * sinI4) + 15.0 / 16.0
        * g4 * (8.0 - 31.0 * sinI2 + 24.5 * sinI4) + 105.0 / 32.0 * g6
        * (-10.0 / 3.0 + 25.0 * sinI2 - 48.75 * sinI4 + 27.5 * sinI6);
    double xlm = trimAngle(mean.getAlphaM()+ q * xnot, Math.PI);

    // periodical terms
    double cl1 = Math.cos(xlm);
    double sl1 = Math.sin(xlm);
    double cl2 = cl1 * cl1 - sl1 * sl1;
    double sl2 = cl1 * sl1 + sl1 * cl1;
    double cl3 = cl2 * cl1 - sl2 * sl1;
    double sl3 = cl2 * sl1 + sl2 * cl1;
    double cl4 = cl3 * cl1 - sl3 * sl1;
    double sl4 = cl3 * sl1 + sl3 * cl1;
    double cl5 = cl4 * cl1 - sl4 * sl1;
    double sl5 = cl4 * sl1 + sl4 * cl1;
    double cl6 = cl5 * cl1 - sl5 * sl1;

    double qq = -1.5 * g2 / rdl;
    double qh = 0.375 * (eym - eps2) / rdpom;
    ql = 0.375 * exm / (sinI1 * rdpom);

    // semi major axis
    double f = (2.0 - 3.5 * sinI2) * exm * cl1 + (2.0 - 2.5 * sinI2) * eym
        * sl1 + sinI2 * cl2 + 3.5 * sinI2 * (exm * cl3 + eym * sl3);
    double rda = qq * f;

    q = 0.75 * g2 * g2 * sinI2;
    f = 7.0 * (2.0 - 3.0 * sinI2) * cl2 + sinI2 * cl4;
    rda += q * f;

    q = -0.75 * g3 * sinI1;
    f = (4.0 - 5.0 * sinI2) * sl1 + 5.0 / 3.0 * sinI2 * sl3;
    rda += q * f;

    q = 0.25 * g4 * sinI2;
    f = (15.0 - 17.5 * sinI2) * cl2 + 4.375 * sinI2 * cl4;
    rda += q * f;

    q = 3.75 * g5 * sinI1;
    f = (2.625 * sinI4 - 3.5 * sinI2 + 1.0) * sl1 + 7.0 / 6.0 * sinI2
        * (1.0 - 1.125 * sinI2) * sl3 + 21.0 / 80.0 * sinI4 * sl5;
    rda += q * f;

    q = 105.0 / 16.0 * g6 * sinI2;
    f = (3.0 * sinI2 - 1.0 - 33.0 / 16.0 * sinI4) * cl2 + 0.75
        * (1.1 * sinI4 - sinI2) * cl4 - 11.0 / 80.0 * sinI4 * cl6;
    rda += q * f;

    // eccentricity
    f = (1.0 - 1.25 * sinI2) * cl1 + 0.5 * (3.0 - 5.0 * sinI2) * exm * cl2
        + (2.0 - 1.5 * sinI2) * eym * sl2 + 7.0 / 12.0 * sinI2 * cl3 + 17.0
        / 8.0 * sinI2 * (exm * cl4 + eym * sl4);
    double rdex = qq * f;

    f = (1.0 - 1.75 * sinI2) * sl1 + (1.0 - 3.0 * sinI2) * exm * sl2
        + (2.0 * sinI2 - 1.5) * eym * cl2 + 7.0 / 12.0 * sinI2 * sl3 + 17.0
        / 8.0 * sinI2 * (exm * sl4 - eym * cl4);
    double rdey = qq * f;

    // ascending node
    q = -qq * cosI1;
    f = 3.5 * exm * sl1 - 2.5 * eym * cl1 - 0.5 * sl2 + 7.0 / 6.0
        * (eym * cl3 - exm * sl3);
    double rdom = q * f;

    f = g3 * cosI1 * (4.0 - 15.0 * sinI2);
    rdom += ql * f;

    f = 2.5 * g5 * cosI1 * (4.0 - 42.0 * sinI2 + 52.5 * sinI4);
    rdom -= ql * f;

    // inclination
    q = 0.5 * qq * sinI1 * cosI1;
    f = eym * sl1 - exm * cl1 + cl2 + 7.0 / 3.0 * (exm * cl3 + eym * sl3);
    double rdxi = q * f;

    f = g3 * cosI1 * (4.0 - 5.0 * sinI2);
    rdxi -= qh * f;

    f = 2.5 * g5 * cosI1 * (4.0 - 14.0 * sinI2 + 10.5 * sinI4);
    rdxi += qh * f;

    // latitude argument
    f = (7.0 - 77.0 / 8.0 * sinI2) * exm * sl1 + (55.0 / 8.0 * sinI2 - 7.50)
        * eym * cl1 + (1.25 * sinI2 - 0.5) * sl2
        + (77.0 / 24.0 * sinI2 - 7.0 / 6.0) * (exm * sl3 - eym * cl3);
    double rdxl = qq * f;

    f = g3 * (53.0 * sinI2 - 4.0 - 57.5 * sinI4);
    rdxl += ql * f;

    f = 2.5 * g5 * (4.0 - 96.0 * sinI2 + 269.5 * sinI4 - 183.75 * sinI6);
    rdxl += ql * f;

    // osculating parameters
    return new CircularParameters(mean.getA() * (1.0 + rda), exm + rdex, eym + rdey,
                                  xim + rdxi, trimAngle(omm + rdom, Math.PI),
                                  trimAngle(xlm + rdxl, Math.PI),
                                  CircularParameters.MEAN_LONGITUDE_ARGUMENT, mean.getFrame());

  }

  // trim an angle between ref - PI and ref + PI
  private static double trimAngle(double a, double ref) {
    double twoPi = 2 * Math.PI;
    return a - twoPi * Math.floor((a + Math.PI - ref) / twoPi);
  }

  /** Initial date. */
  private AbsoluteDate initialDate;

  /** Mean parameters at the initial date. */
  private CircularParameters mean;

  /** Attitude. */
  private AttitudeProvider attitude;

  /** Preprocessed values. */
  private double q;
  private double ql;
  private double g2;
  private double g3;
  private double g4;
  private double g5;
  private double g6;
  private double cosI1;
  private double sinI1;
  private double sinI2;
  private double sinI4;
  private double sinI6;

  /** Model parameters. */
  private double referenceRadius;
  private double mu;
  private double c20, c30, c40, c50, c60;
  private double mass;

}