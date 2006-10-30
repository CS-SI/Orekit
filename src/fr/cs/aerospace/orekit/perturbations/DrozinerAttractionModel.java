package fr.cs.aerospace.orekit.perturbations;

import org.spaceroots.mantissa.geometry.Vector3D;

import fr.cs.aerospace.orekit.errors.OrekitException;
import fr.cs.aerospace.orekit.frames.SynchronizedFrame;
import fr.cs.aerospace.orekit.frames.Transform;
import fr.cs.aerospace.orekit.propagation.EquinoctialGaussEquations;
import fr.cs.aerospace.orekit.time.AbsoluteDate;
import fr.cs.aerospace.orekit.utils.PVCoordinates;

/** This class represents the gravitational field of a celestial body.
 * <p>The algorithm implemented in this class has been designed by
 * Andrzej Droziner (Institute of Mathematical Machines, Warsaw) in
 * his 1976 paper: <em>An algorithm for recurrent calculation of gravitational
 * acceleration</em> (artificial satellites, Vol. 12, No 2, June 1977).</p>
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
   * @param C denormalized coefficients array (cosine part)
   * @param S denormalized coefficients array (sine part)
   * @throws OrekitException 
   */
  public DrozinerAttractionModel(double mu, SynchronizedFrame centralBodyFrame, 
                                 double equatorialRadius,
                                 double[][] C, double[][] S)
    throws OrekitException {

    this.mu = mu;
    this.equatorialRadius = equatorialRadius;
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

    if (C.length == 0) {
      return;
    }

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
      throw new OrekitException("trajectory inside the Brillouin sphere (r = {0})",
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
    double sum1 = 0.0;
    double sum2 = 0.0;
    double bk1 = zOnr;
    double bk0 = aeOnr * (3 * bk1 * bk1 - 1.0);
    for (int k = 2; k < C.length; k++) {
      double bk2 = bk1;
      bk1 = bk0;
      double p = (1.0 + k) / k;
      bk0 = aeOnr * ((1 + p) * zOnr * bk1 - (k * aeOnr * bk2) / (k - 1));
      double ak0 = p * aeOnr * bk1 - zOnr * bk0;
      double jk = -C[k][0];
      sum1 += jk * ak0;
      sum2 += jk * bk0;
    }
    double p = -sum1 / (r1Onr * r1Onr);
    double aX = xDotDotk * p;
    double aY = yDotDotk * p;
    double aZ = mu * sum2 / r2;

    // Tessereal-sectorial part of acceleration
    double cosl = xBody / r1;  
    double sinl = yBody / r1;    
    double[][] A = new double[C.length][C.length];
    double[][] B = new double[C.length][C.length];
    double[] beta = new double[C.length];
    beta[1] = aeOnr;
    B[1][1] = 3 * beta[1] * zOnr * r1Onr;
    double[] sinkl = new double[C.length];
    double[] coskl = new double[C.length];
    sinkl[1] = sinl;
    coskl[1] = cosl;
    double[][] H = new double[C.length][C.length];
    double[][] Hb = new double[C.length][C.length];
    double[][] D = new double[C.length][C.length];

    double sumX = 0.0;
    double sumY = 0.0;
    double sumZ = 0.0;
    for (int k = 2; k < C.length; k++) {
      sinkl[k] = sinkl[k] * cosl + coskl[k] * sinl;
      coskl[k] = coskl[k] * cosl - sinkl[k] * sinl;
      double innerSumX = 0.0;
      double innerSumY = 0.0;
      double innerSumZ = 0.0;
      for (int j = 1; j < C[k].length; j++) {
        H[k][j] = C[k][j] * coskl[j] + S[k][j] * sinkl[j];
        Hb[k][j] = C[k][j] * sinkl[j] - S[k][j] * coskl[j];
        if ((j >= 1) && (j <= (k - 2))) {
          B[k][j] = aeOnr * (2 * k + 1) / (k - j) * zOnr * B[k - 1][j]
                  - aeOnr * (k + j) / (k - 1 - j) * B[k - 2][j];
          A[k][j] = aeOnr * (k + 1) / (k - j) * B[k - 1][j]
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

  /** First normalized potential tesseral coefficients array. */
  private double[][]   C;

  /** Second normalized potential tesseral coefficients array. */
  private double[][]   S;

  /** Frame for the central body. */  
  private SynchronizedFrame centralBodyFrame;

}
