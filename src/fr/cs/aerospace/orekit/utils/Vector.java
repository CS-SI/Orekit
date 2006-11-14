package fr.cs.aerospace.orekit.utils;

import org.spaceroots.mantissa.geometry.Vector3D;

public class Vector {

	public static String toString(Vector3D v) {
        String s = " X : " + v.getX() + "; Y :  " + v.getY() + "; Z : " + v.getZ() + ";" + " norme  : " + v.getNorm();
		return s;
	}
	
}
