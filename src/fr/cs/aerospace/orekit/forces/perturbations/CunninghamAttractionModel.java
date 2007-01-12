package fr.cs.aerospace.orekit.forces.perturbations;

import fr.cs.aerospace.orekit.attitudes.AttitudeKinematics;
import fr.cs.aerospace.orekit.errors.OrekitException;
import fr.cs.aerospace.orekit.forces.ForceModel;
import fr.cs.aerospace.orekit.forces.SWF;
import fr.cs.aerospace.orekit.frames.Frame;
import fr.cs.aerospace.orekit.frames.Transform;
import fr.cs.aerospace.orekit.propagation.TimeDerivativesEquations;
import fr.cs.aerospace.orekit.time.AbsoluteDate;
import fr.cs.aerospace.orekit.utils.PVCoordinates;

import org.spaceroots.mantissa.geometry.Vector3D;

/** This class represents the gravitational field of a celestial body.
 * <p>The algorithm implemented in this class has been designed by
 * Leland E. Cunningham (Lockheed Missiles and Space Company, Sunnyvale
 * and Astronomy Department University of California, Berkeley) in
 * his 1969 paper: <em>On the computation of the spherical harmonic
 * terms needed during the numerical integration of the orbital motion
 * of an artificial satellite</em> (Celestial Mechanics 2, 1970).</p>
 * 
 * @author F. Maussion
 * @author L. Maisonobe
 */

public class CunninghamAttractionModel implements ForceModel {

  /** Creates a new instance.
   * 
   * @param mu central body attraction coefficient
   * @param centralBodyFrame rotating body frame
   * @param equatorialRadius reference equatorial radius of the potential
   * @param C un-normalized coefficients array (cosine part)
   * @param S un-normalized coefficients array (sine part)
   * @throws OrekitException 
   */
  public CunninghamAttractionModel(double mu, Frame centralBodyFrame,
                                   double equatorialRadius, double[][] C, double[][] S)
  throws OrekitException {

    this.bodyFrame = centralBodyFrame;
    this.equatorialRadius = equatorialRadius;
    this.mu = mu;
    degree  = C.length - 1;
    order   = C[degree].length - 1;

    if (C.length!=S.length||C[C.length-1].length!=S[S.length-1].length) {
      throw new OrekitException("C and S should have the same size :" +
                                " (C = [{0}][{1}] ; S = [{2}][{3}])",
                                new String[] { Integer.toString(C.length) , 
                                               Integer.toString(C[degree].length) , 
                                               Integer.toString(S.length) ,           
                                               Integer.toString(S[degree].length) });
    }
    
    if(C.length<1) {
      this.C = new double[1][1];
      this.S = new double[1][1];
    }
    else {
      // invert the arrays (optimization for later "line per line" seeking) 
      this.C = new double[C[degree].length][C.length];
      this.S = new double[S[degree].length][S.length];

      for (int i=0; i<=degree; i++) {
        double[] cT = C[i];
        double[] sT = S[i];
        for (int j=0; j<cT.length; j++) {
          this.C[j][i] = cT[j];
          this.S[j][i] = sT[j];

        }
      }
    }

    // do not calculate keplerian evolution
    this.C[0][0] = 0.0;

  }
  
   /** Compute the contribution of the central body potential to the perturbing
   * acceleration.
   * <p>
   * The central part of the acceleration (mu/r<sup>2</sup> term) is not
   * computed here, only the <em>perturbing</em> acceleration is considered.
   * </p>
   * @param date current date
   * @param pvCoordinates the position and velocity
   * @param frame in which are defined the coordinates
   * @param mass the current mass (kg)
   * @param ak the attitude representation
   * @param adder object where the contribution should be added
   * @throws OrekitException if some specific error occurs
   */
  public void addContribution(AbsoluteDate date, PVCoordinates pvCoordinates,
                              Frame frame, double mass, AttitudeKinematics ak, TimeDerivativesEquations adder)
  throws OrekitException {

    // get the position in body frame
    Transform fromBodyFrame = bodyFrame.getTransformTo(frame, date);
    Transform toBodyFrame   = fromBodyFrame.getInverse();
    Vector3D relative = toBodyFrame.transformPosition(pvCoordinates.getPosition());

    double x = relative.getX();
    double y = relative.getY();
    double z = relative.getZ();

    double x2 = x * x;
    double y2 = y * y;
    double z2 = z * z;
    double r2 = x2 + y2 + z2;
    double r = Math.sqrt(r2);
    if (r <= equatorialRadius) {
      throw new OrekitException("trajectory inside the Brillouin sphere (r = {0})",
                                new String[] { Double.toString(r) });
    }

    // define of some intermediate variables
    double onR2 = 1 / r2;
    double onR3 = onR2 / r;
    double onR4 = onR2 * onR2;

    double cmx   = -x * onR2; 
    double cmy   = -y * onR2;
    double cmz   = -z * onR2;

    double dx   = -2 * cmx;
    double dy   = -2 * cmy;
    double dz   = -2 * cmz;

    // intermediate variables gradients
    // since dcy/dx = dcx/dy, dcz/dx = dcx/dz and dcz/dy = dcy/dz,
    // we reuse the existing variables

    double dcmxdx = (x2 - y2 - z2) * onR4;
    double dcmxdy =  dx * y * onR2;
    double dcmxdz =  dx * z * onR2;
    double dcmydy = (y2 - x2 - z2) * onR4;
    double dcmydz =  dy * z * onR2;
    double dcmzdz = (z2 - x2 - y2) * onR4;

    double ddxdx = -2 * dcmxdx;
    double ddxdy = -2 * dcmxdy;
    double ddxdz = -2 * dcmxdz;
    double ddydy = -2 * dcmydy;
    double ddydz = -2 * dcmydz;
    double ddzdz = -2 * dcmzdz;

    double donr2dx = -dx * onR2;
    double donr2dy = -dy * onR2;
    double donr2dz = -dz * onR2;

    // potential coefficients (4 per matrix)
    double Vrn  = 0.0;
    double Vin  = 0.0;
    double Vrd  = 1.0 / r;;
    double Vid  = 0.0;
    double Vrn1 = 0.0;
    double Vin1 = 0.0;
    double Vrn2 = 0.0;
    double Vin2 = 0.0;

    // gradient coefficients (4 per matrix)
    double gradXVrn  = 0.0;
    double gradXVin  = 0.0;
    double gradXVrd  = -x * onR3;
    double gradXVid  = 0.0;
    double gradXVrn1 = 0.0;
    double gradXVin1 = 0.0;
    double gradXVrn2 = 0.0;
    double gradXVin2 = 0.0;

    double gradYVrn  = 0.0;
    double gradYVin  = 0.0;
    double gradYVrd  = -y * onR3;
    double gradYVid  = 0.0;
    double gradYVrn1 = 0.0;
    double gradYVin1 = 0.0;
    double gradYVrn2 = 0.0;
    double gradYVin2 = 0.0;

    double gradZVrn  = 0.0;
    double gradZVin  = 0.0;
    double gradZVrd  = -z * onR3;
    double gradZVid  = 0.0;
    double gradZVrn1 = 0.0;
    double gradZVin1 = 0.0;
    double gradZVrn2 = 0.0;
    double gradZVin2 = 0.0;

    // acceleration coefficients
    double vdX = 0.0;
    double vdY = 0.0;
    double vdZ = 0.0;
    double rm  = 1.0;

    // start calculating
    for (int m = 0; m <= order; m++) {

      // intermediate variables to compute incrementation
      double[] Cm = C[m];
      double[] Sm = S[m];

      double rn = rm;
      double cx = cmx; 
      double cy = cmy;
      double cz = cmz;

      double dcxdx = dcmxdx;
      double dcxdy = dcmxdy;
      double dcxdz = dcmxdz;
      double dcydy = dcmydy;
      double dcydz = dcmydz;
      double dczdz = dcmzdz;

      Vrn1 = Vrd;
      Vin1 = Vid;

      gradXVrn1 = gradXVrd;
      gradXVin1 = gradXVid;
      gradYVrn1 = gradYVrd;
      gradYVin1 = gradYVid;
      gradZVrn1 = gradZVrd;
      gradZVin1 = gradZVid;

      for (int n = m; n <= degree; n++) {

        if(n==m) {
          // calculate the first element of the next column
          Vrd = (cx + dx) * Vrn1 - (cy + dy) * Vin1; 
          Vid = (cy + dy) * Vrn1 + (cx + dx) * Vin1;     

          gradXVrd = (cx + dx) * gradXVrn1 - (cy + dy) * gradXVin1 + (dcxdx + ddxdx) * Vrn1 - (dcxdy + ddxdy) * Vin1;
          gradXVid = (cy + dy) * gradXVrn1 + (cx + dx) * gradXVin1 + (dcxdy + ddxdy) * Vrn1 + (dcxdx + ddxdx) * Vin1;

          gradYVrd = (cx + dx) * gradYVrn1 - (cy + dy) * gradYVin1 + (dcxdy + ddxdy) * Vrn1 - (dcydy + ddydy) * Vin1;
          gradYVid = (cy + dy) * gradYVrn1 + (cx + dx) * gradYVin1 + (dcydy + ddydy) * Vrn1 + (dcxdy + ddxdy) * Vin1;

          gradZVrd = (cx + dx) * gradZVrn1 - (cy + dy) * gradZVin1 + (dcxdz + ddxdz) * Vrn1 - (dcydz + ddydz) * Vin1;
          gradZVid = (cy + dy) * gradZVrn1 + (cx + dx) * gradZVin1 + (dcydz + ddydz) * Vrn1 + (dcxdz + ddxdz) * Vin1;
          // initialize the current column 
          Vrn = Vrn1;
          Vin = Vin1;

          gradXVrn = gradXVrn1;
          gradXVin = gradXVin1;
          gradYVrn = gradYVrn1;
          gradYVin = gradYVin1;
          gradZVrn = gradZVrn1;
          gradZVin = gradZVin1;

        }        

        if(n==m+1) {
          // calculate the second element of the column
          Vrn = cz*Vrn1;
          Vin = cz*Vin1;          

          gradXVrn = cz * gradXVrn1 + dcxdz * Vrn1;
          gradXVin = cz * gradXVin1 + dcxdz * Vin1;

          gradYVrn = cz * gradYVrn1 + dcydz * Vrn1;
          gradYVin = cz * gradYVin1 + dcydz * Vin1;

          gradZVrn = cz * gradZVrn1 + dczdz * Vrn1;
          gradZVin = cz * gradZVin1 + dczdz * Vin1;

        }        

        if(n>=m+2) {
          // calculate the other elements of the column
          double inv   = 1.0 / (n - m);
          double coeff = n + m - 1.0;

          Vrn = (cz * Vrn1 - coeff * onR2 * Vrn2) * inv;
          Vin = (cz * Vin1 - coeff * onR2 * Vin2) * inv;

          gradXVrn = (cz * gradXVrn1 - coeff * onR2 * gradXVrn2 + dcxdz * Vrn1 - coeff * donr2dx * Vrn2) * inv;
          gradXVin = (cz * gradXVin1 - coeff * onR2 * gradXVin2 + dcxdz * Vin1 - coeff * donr2dx * Vin2) * inv;
          gradYVrn = (cz * gradYVrn1 - coeff * onR2 * gradYVrn2 + dcydz * Vrn1 - coeff * donr2dy * Vrn2) * inv;
          gradYVin = (cz * gradYVin1 - coeff * onR2 * gradYVin2 + dcydz * Vin1 - coeff * donr2dy * Vin2) * inv;
          gradZVrn = (cz * gradZVrn1 - coeff * onR2 * gradZVrn2 + dczdz * Vrn1 - coeff * donr2dz * Vrn2) * inv;
          gradZVin = (cz * gradZVin1 - coeff * onR2 * gradZVin2 + dczdz * Vin1 - coeff * donr2dz * Vin2) * inv;
        }

        // increment variables
        cx += dx;
        cy += dy;
        cz += dz;

        dcxdx += ddxdx;
        dcxdy += ddxdy;
        dcxdz += ddxdz;
        dcydy += ddydy;
        dcydz += ddydz;
        dczdz += ddzdz;

        Vrn2 = Vrn1;
        Vin2 = Vin1;
        gradXVrn2 = gradXVrn1;
        gradXVin2 = gradXVin1;
        gradYVrn2 = gradYVrn1;
        gradYVin2 = gradYVin1;
        gradZVrn2 = gradZVrn1;
        gradZVin2 = gradZVin1;

        Vrn1 = Vrn;
        Vin1 = Vin;
        gradXVrn1 = gradXVrn;
        gradXVin1 = gradXVin;
        gradYVrn1 = gradYVrn;
        gradYVin1 = gradYVin;
        gradZVrn1 = gradZVrn;
        gradZVin1 = gradZVin;     

        // calculate the acceleration due to the Cnm and Snm coefficients
        // ( as the matrix is inversed, Cnm actually is Cmn )

        if (Cm[n]!=0.0||Sm[n]!=0.0) { // avoid doing the calcul if not necessary
          vdX += rn * (Cm[n] * gradXVrn + Sm[n] * gradXVin);
          vdY += rn * (Cm[n] * gradYVrn + Sm[n] * gradYVin);
          vdZ += rn * (Cm[n] * gradZVrn + Sm[n] * gradZVin);
        }

        rn *= equatorialRadius;

      }

      // increment variables
      rm *= equatorialRadius;

      cmx += dx; 
      cmy += dy;
      cmz += dz;

      dcmxdx += ddxdx;
      dcmxdy += ddxdy;
      dcmxdz += ddxdz;
      dcmydy += ddydy;
      dcmydz += ddydz;
      dcmzdz += ddzdz;

    }

    // compute acceleration in inertial frame
    Vector3D acceleration =
      fromBodyFrame.transformVector(new Vector3D(mu * vdX, mu * vdY, mu * vdZ));
    adder.addXYZAcceleration(acceleration.getX(), acceleration.getY(), acceleration.getZ());

  }

  public SWF[] getSwitchingFunctions() {
    return new SWF[0];
  }

  /** Equatorial radius of the Central Body. */
  private double equatorialRadius;

  /** Central attraction. */
  private double mu;

  /** First normalized potential tesseral coefficients array. */
  private double[][] C;

  /** Second normalized potential tesseral coefficients array. */
  private double[][] S;

  /** Degree of potential. */
  private int degree;

  /** Order of potential. */
  private int order;

  /** Rotating body. */
  private Frame bodyFrame;

}
