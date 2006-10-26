package fr.cs.aerospace.orekit.perturbations;

import org.spaceroots.mantissa.geometry.Vector3D;

import fr.cs.aerospace.orekit.errors.OrekitException;
import fr.cs.aerospace.orekit.frames.SynchronizedFrame;
import fr.cs.aerospace.orekit.frames.Transform;
import fr.cs.aerospace.orekit.propagation.EquinoctialGaussEquations;
import fr.cs.aerospace.orekit.time.AbsoluteDate;
import fr.cs.aerospace.orekit.utils.PVCoordinates;

/** This class represents the gravitational field of a celestial body.
 * <p>
 * The gravitational field of a central body is split in two parts. The first
 * one is the central attraction which is a single coefficient. The second one
 * is the perturbing acceleration which is expressed using spherical harmonics.
 * </p>
 * @version $Id$
 * @author L. Maisonobe
 * @author E. Delente
 * @author F. Maussion
 */

public class DrozinerAttractionModel implements ForceModel {

  /**
   * Creates a new instance of CentralBodyPotential.
   * @param mu central body attraction coefficient
   * @param centralBodyFrame frame for the central body
   * @param body rotating body
   * @param equatorialRadius reference equatorial radius of the potential
   * @param J normalized coefficients array (zonal part)
   * @param C normalized coefficients array (cosine part)
   * @param S normalized coefficients array (sine part)
   * @throws OrekitException 
   */
  public DrozinerAttractionModel(double mu, SynchronizedFrame centralBodyFrame, 
                                 double equatorialRadius,
                                 double[] J, double[][] C, double[][] S) throws OrekitException {

    this.mu = mu;
    this.equatorialRadius = equatorialRadius;
    this.J = J;
    this.C = C;
    this.S = S;
    this.centralBodyFrame = centralBodyFrame;
  }

  /**
   * Computes the contribution of the central body potential to the perturbing
   * acceleration, using the Drozyner algorithm. The central part of the
   * acceleration (&mu;/r<sup>2</sup> term) is not computed here, only the
   * <em>perturbing</em> acceleration is considered, not the main part.
   * @param t current date
   * @param pvCoordinates the {@link PVCoordinates}
   * @param adder object where the contribution should be added
   */

  public void addContribution(AbsoluteDate t, PVCoordinates pvCoordinates, 
                              EquinoctialGaussEquations adder)
      throws OrekitException {

    // Coordinates in centralBodyFrame
    Transform bodyToInertial = centralBodyFrame.getTransformTo(adder.getFrame(), t);
    Vector3D posInBody =
      bodyToInertial.getInverse().transformVector(pvCoordinates.getPosition());
    double xBody = posInBody.getX();
    double yBody = posInBody.getY();
    double zBody = posInBody.getZ();
    
    // Computation of intermediate variables
    double r12 = xBody * xBody + yBody * yBody;
    double r1 = Math.sqrt(r12);
    if (r1 <= 10e-2) {
      throw new OrekitException("polar trajectory (r1 = {0})",
                                new String[] { Double.toString(r1) });
    }

    double r2 = r12 + zBody * zBody;
    double r  = Math.sqrt(r2);
    if (r <= equatorialRadius) {
      throw new OrekitException("underground trajectory (r = {0})",
                                new String[] { Double.toString(r) });
    }
    double r3    = r2  * r;
    double aeOnr = equatorialRadius / r;
    double zOnr  = zBody/ r;
    double r1Onr = r1 / r;

    // Definition of the first acceleration terms
    double mMuOnr3  = -mu / r3;
    double xDotDotk = xBody * mMuOnr3;
    double yDotDotk = yBody * mMuOnr3;
    
    // Zonal part of acceleration
    double aX = 0.0;
    double aY = 0.0;
    double aZ = 0.0;
    if (J.length != 0) {
      double sum1 = 0.0;
      double sum2 = 0.0;
      double bk1 = zOnr;
      double bk0 = aeOnr * (3 * bk1 * bk1 - 1.0);
      for (int k = 2; k <= J.length; k++) {
        double bk2 = bk1;
        bk1 = bk0;
        double p = (1.0 + k) / k;
        bk0 = aeOnr * ((1 + p) * zOnr * bk1 - (k * aeOnr * bk2) / (k - 1));
        double ak0 = p * aeOnr * bk1 - zOnr * bk0;
        double jk = J[k-1];
        sum1 += jk * ak0;
        sum2 += jk * bk0;
      }
      double p = -sum1 / (r1Onr * r1Onr);
      aX = xDotDotk * p;
      aY = yDotDotk * p;
      aZ = mu * sum2 / r2;
    }

    // Tessereal-sectorial part of acceleration
    if (C.length != 0) {
      // Determine the longitude
      double cosl = xBody / r1;  
      double sinl = yBody / r1;    
      double[][] A = new double[C.length + 1][C.length + 1];
      double[][] B = new double[C.length + 1][C.length + 1];
      double[] beta = new double[C.length + 1];
      beta[1] = aeOnr;
      B[1][1] = 3 * beta[1] * zOnr * r1Onr;
      double[] sinkl = new double[C.length + 1];
      double[] coskl = new double[C.length + 1];
      sinkl[1] = sinl;
      coskl[1] = cosl;
      double[][] H = new double[C.length + 1][C.length + 1];
      double[][] Hb = new double[C.length + 1][C.length + 1];
      double[][] D = new double[C.length + 1][C.length + 1];

      double sumX = 0.0;
      double sumY = 0.0;
      double sumZ = 0.0;
      for (int k = 2; k <= C.length; k++) {
        sinkl[k] = sinkl[k - 1] * cosl + coskl[k - 1] * sinl;
        coskl[k] = coskl[k - 1] * cosl - sinkl[k - 1] * sinl;
        double innerSumX = 0.0;
        double innerSumY = 0.0;
        double innerSumZ = 0.0;
        for (int j = 1; j <= k; j++) {
          H[k][j] = C[k-1][j-1] * coskl[j] + S[k-1][j-1] * sinkl[j];
          Hb[k][j] = C[k-1][j-1] * sinkl[j] - S[k-1][j-1] * coskl[j];
          if ((j >= 1) && (j <= (k - 2))) {
            B[k][j] =
              aeOnr * (2 * k + 1) / (k - j) * zOnr * B[k - 1][j]
            - aeOnr * (k + j) / (k - 1 - j) * B[k - 2][j];
            A[k][j] =
              aeOnr * (k + 1) / (k - j) * B[k - 1][j]
            - zOnr * B[k][j];
          }
          if (j == (k - 1)) {
            beta[k] = (2 * k - 1) * r1Onr * aeOnr * beta[k - 1];
            B[k][k - 1] = (2 * k + 1) * aeOnr * zOnr * B[k - 1][k - 1]
                        - beta[k];
            A[k][k - 1] = (k + 1) * aeOnr * B[k - 1][k - 1]
                        - zOnr * B[k][k - 1];
          }
          if (j == k) {
            B[k][k] = (2 * k + 1) * aeOnr * r1Onr * B[k - 1][k - 1];
            A[k][k] = (k + 1) * r1Onr * beta[k] - zOnr * B[k][k];
          }
          D[k][j] =  j / (k + 1) * (A[k][j] + zOnr * B[k][j]);
          innerSumX += A[k][j] * H[k][j];
          innerSumY += B[k][j] * H[k][j];
          innerSumZ += D[k][j] * Hb[k][j];
        }
        sumX += innerSumX;
        sumY += innerSumY;
        sumZ += innerSumZ;
      }
      double r2Onr12 = r2 / (r1 * r1);
      double p1 = r2Onr12 * xDotDotk;
      double p2 = r2Onr12 * yDotDotk;
      aX += p1 * sumX - p2 * sumY;
      aY += p2 * sumX + p1 * sumY;
      aZ -= mu * sumZ / r2;
    }

    // provide the perturbing acceleration to the derivatives adder
    Vector3D accInInert = bodyToInertial.transformVector(new Vector3D(aX, aY, aZ));
    adder.addXYZAcceleration(accInInert.getX(), accInInert.getY(), accInInert.getZ());

  }

  public SWF[] getSwitchingFunctions() {
    return null;
  }

  /** Central body attraction coefficient. */
  private double mu;

  /** Reference equatorial radius of the potential. */
  private double equatorialRadius;

  /** First normalized potential zonal coefficients array. */
  private double[]     J;

  /** First normalized potential tesseral coefficients array. */
  private double[][]   C;

  /** Second normalized potential tesseral coefficients array. */
  private double[][]   S;

  /** Frame for the central body. */  
  private SynchronizedFrame centralBodyFrame;

}
