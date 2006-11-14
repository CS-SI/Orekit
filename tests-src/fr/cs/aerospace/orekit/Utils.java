package fr.cs.aerospace.orekit;


public class Utils {
  
  // epsilon for tests
  public static final double epsilonTest  = 1.e-12;
  
  // epsilon for eccentricity
  public static final double epsilonE     = 1.e+5 * epsilonTest;
  
  // epsilon for circular eccentricity
  public static final double epsilonEcir  = 1.e+8 * epsilonTest;

  // epsilon for angles
  public static final double epsilonAngle = 1.e+5 * epsilonTest;

  
  //trim an angle between ref - PI and ref + PI
  public static double trimAngle (double a, double ref) {
    double twoPi = 2 * Math.PI;
    return a - twoPi * Math.floor ((a + Math.PI - ref) / twoPi);
  }
  
  public static double ae =  6378136.460;
  public static double mu =  3.986004415e+14;
  

}
