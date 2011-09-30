package org.orekit.propagation.semianalytical.dsst.dsstforcemodel;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math.fraction.BigFraction;

public class ModifiedNewcombOperators {

    private final static List<PolynomialFunction> modifiedNewcombOperators = new ArrayList<PolynomialFunction>();
    
    
    private static final List<PolynomialFunction> JACOBI_COEFFICIENTS = null;

    /**
     * @param rho
     * @param sigma
     * @param orderMax
     */
    public static List<PolynomialFunction> CreateModifiedNewcombOperator(final int rho,
                                                                 final int sigma,
                                                                 final int orderMax) {
//        initializeJacobiCoefficient();
        
        

        return null;
    }
    
    
//    /**
//     * Initialize the recursion formula for the Modified Newcomb Operator
//     * @param v first exponent
//     * @param w second exponent
//     * @return coefficient of polynoms of degree 0 (coefficients[0]) and degree 1 ((coefficients[1] et (coefficients[2]))  
//     */
//    private static void initializeJacobiCoefficient(final double v, final double w){
//        JACOBI_COEFFICIENTS.clear();
//        // P0(x) = 1;
//        JACOBI_COEFFICIENTS.add(BigFraction.ONE);
//        // P1(x) = (v - w) / 2 + (2 + v + w) * X / 2
//        final BigFraction constantTerm = new BigFraction((v - w) / 2d);
//        final BigFraction xTerm = new BigFraction((2 + v + w) / 2d);
//        // (v - w) / 2
//        JACOBI_COEFFICIENTS.add(constantTerm);
//        // 0 + (2 + v + w) / 2 * X
//        JACOBI_COEFFICIENTS.add(xTerm);
//        JACOBI_V_COEFFICIENT = v;
//        JACOBI_W_COEFFICIENT = w;
//    }
    
    
}
