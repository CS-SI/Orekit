package fr.cs.aerospace.orekit.utils;


public class Angle {

  // trim an angle between ref - PI and ref + PI
  public static double trim(double a, double ref) {
    double twoPi = 2 * Math.PI;
    return a - twoPi * Math.floor((a + Math.PI - ref) / twoPi);
  }
  
}
