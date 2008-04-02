package fr.cs.orekit;

public class Utils {

    // epsilon for tests
    public static final double epsilonTest  = 1.e-12;

    // epsilon for eccentricity
    public static final double epsilonE     = 1.e+5 * epsilonTest;

    // epsilon for circular eccentricity
    public static final double epsilonEcir  = 1.e+8 * epsilonTest;

    // epsilon for angles
    public static final double epsilonAngle = 1.e+5 * epsilonTest;

    public static final double ae =  6378136.460;
    public static final double mu =  3.986004415e+14;


}
