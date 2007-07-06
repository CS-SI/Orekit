package fr.cs.orekit.utils;

import org.apache.commons.math.geometry.Vector3D;


public class VectorFormatter {
  
  public static String toString(Vector3D v) {
    String s = " X : " + v.getX() + "; Y :  " + v.getY() + "; Z : " + v.getZ() + ";" + " norme  : " + v.getNorm();
    return s;
  }
  
  public static String toString(Vector3D v, int decimals) {
        
    double exp = Math.pow(10,decimals);
    
    double x = v.getX() * exp;
    x = Math.round(x);
    x /= exp;
    double y = v.getY() * exp;
    y = Math.round(y);
    y /= exp;
    
    double z = v.getZ() * exp;
    z = Math.round(z);
    z /= exp;
    
    double n = v.getNorm() * exp;
    n = Math.round(n);
    n /= exp;
    
    if(decimals<=0) {
      return "X: " + (int)x + "; Y: " + (int)y + "; Z: " + (int)z + ";" + " norme: " + (int)n;
    }
    else {
      return "X: " + x + "; Y: " + y + "; Z: " + z + ";" + " norme: " + n;
    }
  }
  
}
