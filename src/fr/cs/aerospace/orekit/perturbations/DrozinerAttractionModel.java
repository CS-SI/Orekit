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
    
    double sinJ, sinJminusOne;
    double cosJ, cosJminusOne;
    
    sinJ = sinl;
    cosJ = cosl;
    sinJminusOne = sinJ;
    cosJminusOne = cosJ;

    double Bkj = 0;
    double Akj = 0;
    double betaKminus1 = aeOnr;
    double Bkminus1j = 3 * betaKminus1 * zOnr * r1Onr;
    double Bkminus2j = 0;
    double Bkminus1kminus1 = 0;
    double betaK = 0;
    double sumX = 0.0;
    double sumY = 0.0;
    double sumZ = 0.0;
    double Dkj;
    double innerSumX;
    double innerSumY;
    double innerSumZ;
    
    double Gkj;
    double Hkj;
    
    for (int k = 2; k < C.length; k++) {

      innerSumX = 0.0;
      innerSumY = 0.0;
      innerSumZ = 0.0;
      
      for (int j = 1; j <= k; j++) {
        if (j<C[k].length) {
          if (j!=1) {
            sinJ = sinJminusOne * cosl + cosJminusOne * sinl;
            cosJ = cosJminusOne * cosl - sinJminusOne * sinl;
          }
          cosJminusOne = cosJ;
          sinJminusOne = sinJ;
          
          Gkj = C[k][j] * cosJ + S[k][j] * sinJ;
          Hkj = C[k][j] * sinJ - S[k][j] * cosJ;
          
          if ((j >= 1) && (j <= (k - 2))) {
            
            Bkj = aeOnr * (2.0 * k + 1) / (k - j) * zOnr * Bkminus1j
                    - aeOnr * (k + j) / (k - 1 - j) * Bkminus2j;
            Akj = aeOnr * (k + 1.0) / (k - j) * Bkminus1j
                    - zOnr * Bkj;
            
          }
          if (j == (k - 1)) {
            betaK = (2 * k - 1) * r1Onr * aeOnr * betaKminus1;
            Bkj = (2 * k + 1) * aeOnr * zOnr * Bkminus1j - betaK;
            Akj = (k + 1) * aeOnr * Bkminus1j - zOnr * Bkj;
            Bkminus1kminus1 = Bkj;
            betaKminus1 = betaK;          
          }
          if (j == k) {
            Bkj = (2 * k + 1) * aeOnr * r1Onr * Bkminus1kminus1;
            Akj = (k + 1) * r1Onr * betaK - zOnr * Bkj;
          }
          Bkminus2j = Bkminus1j;
          Bkminus1j = Bkj;        
          
          Dkj =  j / (k + 1) * (Akj + zOnr * Bkj);
          
          innerSumX += Akj * Gkj;
          innerSumY += Bkj * Gkj;
          innerSumZ += Dkj * Hkj;
        }

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
