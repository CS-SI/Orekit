package fr.cs.aerospace.orekit.perturbations;

import fr.cs.aerospace.orekit.bodies.RotatingBody;
import fr.cs.aerospace.orekit.errors.OrekitException;
import fr.cs.aerospace.orekit.frames.Frame;
import fr.cs.aerospace.orekit.orbits.OrbitDerivativesAdder;
import fr.cs.aerospace.orekit.time.AbsoluteDate;
import fr.cs.aerospace.orekit.utils.PVCoordinates;


import org.spaceroots.mantissa.geometry.Vector3D;

/**
 * This class represents the gravitational field of a celestial body.
 * <p>
 * The gravitational field of a central body is split in two parts. The first
 * one is the central attraction which is a single coefficient. The second one
 * is the perturbing acceleration which is expressed using spherical harmonics.
 * </p>
 * @version $Id$
 * @author L. Maisonobe
 * @author E. Delente
 */

public class CunninghamAttractionModel implements ForceModel {

  /**
   * Create a new instance of CentralBodyPotential.
   * @param mu central body attraction coefficient
   * @param body rotating body
   * @param equatorialRadius reference equatorial radius of the potential
   * @param C normalized coefficients array (cosine part)
   * @param S normalized coefficients array (sine part)
   */
  public CunninghamAttractionModel(double mu, RotatingBody body,
                                   double equatorialRadius,
                                   double[][] C, double[][] S) {

    this.body = body;
    this.equatorialRadius = equatorialRadius;
    muOnAe = mu / equatorialRadius;
    this.C = C;
    this.S = S;

    V   = new double[C.length + 3 + 1][C.length + 3 + 1];
    ndd = Math.min(Math.max(ndeg + 1, 4), C.length + 2);
    nod = Math.min(Math.max(nord + 1, 4), C.length + 2);

  }

  /**
   * Compute the contribution of the central body potential to the perturbing
   * acceleration.
   * <p>
   * The central part of the acceleration (mu/r<sup>2</sup> term) is not
   * computed here, only the <em>perturbing</em> acceleration is considered.
   * </p>
   * @param date current date
   * @param pvCoordinates the position end velocity
   * @param Attitude current attitude
   * @param adder object where the contribution should be added
   */
  public void addContribution(AbsoluteDate date, PVCoordinates pvCoordinates,
		  Frame frame , OrbitDerivativesAdder adder)
      throws OrekitException {

    // Construction of the potential array V(n,m)
    buildArray(ndd, nod, date, pvCoordinates.getPosition(), V);

    double aX = 0.0;
    double aY = 0.0;
    double aZ = 0.0;
    for (int nn = 1; nn <= (ndeg + 1); nn++) {
      int n   = ndeg + 1 - nn;
      int npu = n + 1;
      int mpu = Math.min(npu, (nord + 1));
      double sumX = 0.0;
      double sumY = 0.0;
      double sumZ = 0.0;
      double[] cn = C[n];
      double[] sn = S[n];
      for (int m = 0; m < mpu; ++m) {

        // compute the first derivative of the Vnm function
        buildDerivatives(n, m, V);

        double cnm = cn[m];
        double snm = sn[m];
        sumX += realDerX * cnm + imaginaryDerX * snm;
        sumY += realDerY * cnm + imaginaryDerY * snm;
        sumZ += realDerZ * cnm + imaginaryDerZ * snm;

      }

      // Acceleration calculation
      aX += sumX * muOnAe;
      aY += sumY * muOnAe;
      aZ += sumZ * muOnAe;

    }

    // provide the perturbing acceleration to the derivatives adder
    adder.addXYZAcceleration(aX, aY, aZ);

  }

  /**
   * Potential builder.
   * @param ndeg maximum degree of potential to take into account
   * @param nord maximum order of the potential to take into account
   * @param date current date
   * @param position current position (m)
   * @param V matrix containing all the elementary potentials
   */
  private void buildArray(int ndeg, int nord, AbsoluteDate date, Vector3D position,
                          double[][] V) throws OrekitException {

    // Retrieval of cartesian coordinates
    Vector3D relative = body.getOrientation(date).applyTo(position);
    double x = relative.getX();
    double y = relative.getY();
    double z = relative.getZ();

    // Definition of intermediate variables
    double r = Math.sqrt(x * x + y * y + z * z);
    if (r == 0) {
      throw new OrekitException("underground trajectory (r = {0})",
                                new String[] { Double.toString(r) });
    }
    double cosphicosl = x / r;
    double cosphisinl = y / r;
    double sinphi     = z / r;
    double aeOnr      = equatorialRadius / r;
    double aeOnr2     = aeOnr * aeOnr;
    int    noo        = Math.min(C.length + 3, Math.max(2, nord + 2));

    // Construction of the two first columns of matrix V
    V[1][1] = 1.0 / r;
    for (int i = 2; i <= noo; i++) {
      V[i][1] = V[i - 1][1] * (2 * i - 3) * aeOnr;
      V[i][2] = V[i][1] * sinphi;
    }

    if (ndeg > 1) {
      int mm = Math.min((nord + 1), (ndeg - 1));
      for (int j = 1; j <= mm; j++) {
        for (int i = (j + 2); i <= (ndeg + 1); i++) {
          V[i][i - j + 1] =
            (V[i - 1][i - j] * sinphi * aeOnr * (2 * i - 3)
           - V[i - 2][i - j - 1] * aeOnr2 * (i + j - 3)
            ) / (i - j);
        }
      }
    }

    if (nord != 0) {
      double sinml[] = new double[C.length + 3];
      double cosml[] = new double[C.length + 3];
      sinml[1] = 0.0;
      cosml[1] = 1.0;
      for (int m = 1; m <= nord; m++) {
        sinml[m + 1] = sinml[m] * cosphicosl + cosml[m] * cosphisinl;
        cosml[m + 1] = cosml[m] * cosphicosl - sinml[m] * cosphisinl;
        for (int n = m + 1; n <= (ndeg + 1); n++) {
          V[n - m][n] = V[n][n - m];
          V[n][n - m] = V[n][n - m] * sinml[m + 1];
          V[n - m][n] = V[n - m][n] * cosml[m + 1];
        }
      }
    }

  }

  /** compute the derivatives.
   * @param n degree of the concerened elementary potential
   * @param m order of the concerened elementary potential
   * @param V matrix containing all the elementary potentials
   */
  private void buildDerivatives(int n, int m, double[][] V) {
    if (m == 0) {
      // zonal terms

      // real part
      realDerX = -V[n + 1][n + 2];
      realDerY = -V[n + 2][n + 1];
      realDerZ = -V[n + 2][n + 2] * (double) (n + 1);

      // no imaginary part for zonal terms
      imaginaryDerX = 0.0;
      imaginaryDerY = 0.0;
      imaginaryDerZ = 0.0;

    } else if (m == 1) {
      // sectorial terms
      double en = (double) n;
      double fn = (double) (n * (n + 1));
      double vn = fn * V[n + 2][n + 2];

      // real part
      realDerX = (-V[n][n + 2] + vn) / 2.0;
      realDerY = -V[n + 2][n] / 2.0;
      realDerZ = -en * V[n + 1][n + 2];

      // imaginary part
      imaginaryDerX = realDerY;
      imaginaryDerY = (V[n][n + 2] + vn) / 2.0;
      imaginaryDerZ = -en * V[n + 2][n + 1];

    } else {
      // tessereal terms
      double enm = (double) (n - m + 1);
      double fnm = (double) (n - m + 1) * (double) (n - m + 2);

      // real part
      realDerX = (-V[n - m + 1][n + 2] + fnm * V[n - m + 3][n + 2]) / 2.0;
      realDerY = (-V[n + 2][n - m + 1] - fnm * V[n + 2][n - m + 3]) / 2.0;
      realDerZ = -enm * V[n - m + 2][n + 2];

      // imaginary part
      imaginaryDerX = (-V[n + 2][n - m + 1] + fnm * V[n + 2][n - m + 3]) / 2.0;
      imaginaryDerY = (V[n - m + 1][n + 2] + fnm * V[n - m + 3][n + 2]) / 2.0;
      imaginaryDerZ = -enm * V[n + 2][n - m + 2];

    }
  }

  public SWF[] getSwitchingFunctions() {
    return null;
  }

  /** Initialisation of potential array. */
  private double[][]   V;

  /** Real part of potential derivatives. */
  private double realDerX;
  private double realDerY;
  private double realDerZ;

  /** Imaginary part of potential derivatives. */
  private double imaginaryDerX;
  private double imaginaryDerY;
  private double imaginaryDerZ;

  /** Initialization of the acceleration. */
  private int          ndd;

  private int          nod;

  /** Equatorial radius of the Central Body. */
  private double       equatorialRadius;

  /** Intermediate variables. */
  private double       muOnAe;

  /** First normalized potential tesseral coefficients array. */
  private double[][]   C;

  /** Second normalized potential tesseral coefficients array. */
  private double[][]   S;

  /** Definition of degree, order and maximum potential size. */
  private int          ndeg;

  private int          nord;

  /** Rotating body. */
  private RotatingBody body;

}
