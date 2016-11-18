/* Copyright 2002-2016 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * CS licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.orekit.utils;

import org.hipparchus.Field;
import org.hipparchus.FieldElement;
import org.hipparchus.RealFieldElement;
import org.hipparchus.exception.MathIllegalArgumentException;
import org.hipparchus.exception.MathRuntimeException;
import org.hipparchus.exception.NullArgumentException;
import org.hipparchus.util.FastMath;

public class FieldTimeDerivative<T extends RealFieldElement<T>> implements RealFieldElement<FieldTimeDerivative<T>> {
    /** Value. */
    private final T f0;
    /** First derivative with respect to time.*/
    private final T f1;
    /** Second derivative with respect to time.*/
    private final T f2;
    /**Instance of one.*/
    private final T one;
    /**Instance of one.*/
    private final T zero;

    /**Build an instance of position velocity and acceleration of a RealFieldElement.
     * @param position value
     * @param velocity first derivative with respect of time
     * @param acceleration second derivative with respect of time
     * */
    public FieldTimeDerivative(final T position, final T velocity, final T acceleration ) {
        f0 = position;
        f1 = velocity;
        f2 = acceleration;
        one = f0.getField().getOne();
        zero = f0.getField().getZero();
    }
    @Override
    public FieldTimeDerivative<T> add(final FieldTimeDerivative<T> a)
        throws NullArgumentException {
        return new FieldTimeDerivative<T>(f0.add(a.f0), f1.add(a.f1), f2.add(a.f2));
    }
    @Override
    public FieldTimeDerivative<T> subtract(final FieldTimeDerivative<T> a)
        throws NullArgumentException {
        return new FieldTimeDerivative<T>(f0.subtract(a.f0), f1.subtract(a.f1), f2.subtract(a.f2));
    }
    /** '-' operator.
     * @param a right hand side parameter of the operator
     * @return this - a
     * @throws NullArgumentException in case of Null agrument
     */
    public FieldTimeDerivative<T> subtract(final T a)
        throws NullArgumentException {
        return new FieldTimeDerivative<T>(f0.subtract(a), f1, f2);
    }
    @Override
    public FieldTimeDerivative<T> negate() {
        return new FieldTimeDerivative<T>(f0.negate(), f1.negate(), f2.negate());
    }

    @Override
    public FieldTimeDerivative<T> multiply(final int n) {
        return new FieldTimeDerivative<T>(f0.multiply(n), f1.multiply(n), f2.multiply(n));
    }

    /** Compute this &times; n.
    * @param n a element to multiply
    * @return a new element representing this &times; n
    */
    public FieldTimeDerivative<T> multiply(final T n) {
        return new FieldTimeDerivative<T>(f0.multiply(n), f1.multiply(n), f2.multiply(n));
    }
    @Override
    public FieldTimeDerivative<T> multiply(final FieldTimeDerivative<T> a)
        throws NullArgumentException {
        return new FieldTimeDerivative<T>(f0.multiply(a.f0),
                                          f0.multiply(a.f1).add(a.f0.multiply(f1)),
                                          f0.multiply(a.f2).add(f1.multiply(a.f1).multiply(2)).add(f2.multiply(a.f0)));
    }

    @Override
    public FieldTimeDerivative<T> divide(final FieldTimeDerivative<T> a)
        throws NullArgumentException, MathRuntimeException {
        return new FieldTimeDerivative<T>(f0.divide(a.f0),
                                          (f1.multiply(a.f0).subtract(f0.multiply(a.f1))).divide(a.f0.multiply(a.f0)),
                                          ((f2.multiply(a.f0).subtract(a.f2.multiply(f0))).multiply(a.f0).subtract((f1.multiply(a.f0).subtract(f0.multiply(a.f1))).multiply(2).multiply(a.f1))).divide(a.f0.multiply(a.f0).multiply(a.f0)));
    }

    @Override
    public Field<FieldTimeDerivative<T>> getField() {
        return new Field<FieldTimeDerivative<T>>() {

            /** {@inheritDoc} */
            @Override
            public FieldTimeDerivative<T> getZero() {
                return new FieldTimeDerivative<T>(f0.getField().getZero(), f0.getField().getZero(), f0.getField().getZero());
            }

            /** {@inheritDoc} */
            @Override
            public FieldTimeDerivative<T> getOne() {
                return new FieldTimeDerivative<T>(f0.getField().getOne(), f0.getField().getZero(), f0.getField().getZero());
            }

            /** {@inheritDoc} */
            @SuppressWarnings("unchecked")
            @Override
            public Class<? extends FieldElement<FieldTimeDerivative<T>>> getRuntimeClass() {
                return (Class<? extends FieldElement<FieldTimeDerivative<T>>>) FieldTimeDerivative.class;
            }

        };
    }

    /**The free parameters of the FieldTimeDerivative are the time (if it was a DerivativeStructure).
     * @return number of free parameters (1, which is the time)
     */
    public int getFreeParameters() {
        return 1;
    }

    /**The order of the FieldTimeDerivative is 2  (if it was a DerivativeStructure).
     * @return Order of the DerivativeStructure
     */
    public int getOrder() {
        return 2;
    }

    @Override
    public double getReal() {
        return f0.getReal();
    }
    public T getPosition() {
        return f0;
    }
    public T getVelocity() {
        return f1;
    }
    public T getAcceleration() {
        return f2;
    }

    @Override
    public FieldTimeDerivative<T> add(final double a) {
        return  new FieldTimeDerivative<T>(f0.add(a), f1, f2);
    }

    /** '+' operator.
     * @param a right hand side parameter of the operator
     * @return this+a
     */
    public FieldTimeDerivative<T> add(final T a) {
        return  new FieldTimeDerivative<T>(f0.add(a), f1, f2);
    }

    @Override
    public FieldTimeDerivative<T> subtract(final double a) {
        return  new FieldTimeDerivative<T>(f0.subtract(a), f1, f2);
    }

    @Override
    public FieldTimeDerivative<T> multiply(final double a) {
        return  new FieldTimeDerivative<T>(f0.multiply(a), f1.multiply(a), f2.multiply(a));
    }

    @Override
    public FieldTimeDerivative<T> divide(final double a) {
        return  new FieldTimeDerivative<T>(f0.divide(a), f1.divide(a), f2.divide(a));
    }

    @Override
    public FieldTimeDerivative<T> remainder(final double a) {
        return  new FieldTimeDerivative<T>(f0.remainder(a), f1, f2);
    }

    @Override
    public FieldTimeDerivative<T> remainder(final FieldTimeDerivative<T> a)
        throws MathIllegalArgumentException {
        return  new FieldTimeDerivative<T>(f0.remainder(a.f0), f1.remainder(a.f1), f2.remainder(a.f2)); //TODO
    }

    @Override
    public FieldTimeDerivative<T> abs() {
        if (f0.getReal() < 0) {

            return  new FieldTimeDerivative<T>(f0.abs(), f1.negate(), f2.negate());
        } else {
            return new FieldTimeDerivative<T>(f0.abs(), f1, f2);
        }
    }

    @Override
    public FieldTimeDerivative<T> ceil() {
        return  new FieldTimeDerivative<T>(f0.ceil(), zero, zero);
    }

    @Override
    public FieldTimeDerivative<T> floor() {
        return  new FieldTimeDerivative<T>(f0.floor(), zero, zero);
    }

    @Override
    public FieldTimeDerivative<T> rint() {
        return  new FieldTimeDerivative<T>(f0.rint(), zero, zero);
    }

    @Override
    public long round() {
        return f0.round();
    }

    @Override
    public FieldTimeDerivative<T> signum() {
        return  new FieldTimeDerivative<T>(f0.signum(), zero, zero);
    }

    @Override
    public FieldTimeDerivative<T> copySign(final FieldTimeDerivative<T> sign) {
        final long m = Double.doubleToLongBits(f0.getReal());
        final long s = Double.doubleToLongBits(sign.f0.getReal());
        if ((m >= 0 && s >= 0) || (m < 0 && s < 0)) { // Sign is currently OK
            return this;
        }
        return negate();
    }

    @Override
    public FieldTimeDerivative<T> copySign(final double sign) {
        final long m = Double.doubleToLongBits(f0.getReal());
        final long s = Double.doubleToLongBits(sign);
        if ((m >= 0 && s >= 0) || (m < 0 && s < 0)) { // Sign is currently OK
            return this;
        }
        return negate();
    }

    @Override
    public FieldTimeDerivative<T> scalb(final int n) {
        return multiply(FastMath.pow(2, n));
    }

    @Override
    public FieldTimeDerivative<T> hypot(final FieldTimeDerivative<T> y)
        throws MathIllegalArgumentException {
        return (multiply(this).add(y.multiply(y))).sqrt();
    }

    @Override
    public FieldTimeDerivative<T> reciprocal() {
        return getField().getOne().divide(this);
    }

    @Override
    public FieldTimeDerivative<T> sqrt() {
        return pow(0.5);
    }

    @Override
    public FieldTimeDerivative<T> cbrt() {
        return pow(1.0 / 3);
    }

    @Override
    public FieldTimeDerivative<T> rootN(final int n) {
        return pow(1.0 / n);
    }

    @Override
    public FieldTimeDerivative<T> pow(final double p) {
        if (f0.abs().getReal() == 0.0) {
            if (p == 0.0) return new FieldTimeDerivative<T>(one, zero, zero);
            return new FieldTimeDerivative<T>(zero, zero, zero);
        }
        if (Double.isInfinite(f0.pow(p).getReal()) || Double.isNaN(f0.pow(p).getReal()) ) {
            return new FieldTimeDerivative<T>(zero.add(Double.NaN), zero.add(Double.NaN), zero.add(Double.NaN));
        } else {
            return new FieldTimeDerivative<T>( f0.pow(p),
                      f0.pow(p - 1).multiply(p).multiply(f1),
                      f0.pow(p - 2).multiply(p * (p - 1.0)).multiply(f1.pow(2.0)).add(f0.pow(p - 1.0).multiply(p).multiply(f2))
                      );

        }
//        if ((signum().getReal()<=0 && FastMath.abs(p - FastMath.floor(p))> 1e-15 ) || (abs().getReal()<1e-15 && p < 1e-15)){
//            return new FieldTimeDerivative<T>(zero.add(Double.NaN),zero.add(Double.NaN),zero.add(Double.NaN));
//       }else if(FastMath.abs(p) < 1e-15){
//           return new FieldTimeDerivative<T>(one,zero,zero);
//        }
//        return new FieldTimeDerivative<T>( f0.pow(p),
//                        f0.pow(p-1).multiply(p).multiply(f1),
//                        f0.pow(p-2).multiply(p*(p-1.0)).multiply(f1.pow(2.0)).add(f0.pow(p-1.0).multiply(p).multiply(f2))
//                        );
    }

    @Override
    public FieldTimeDerivative<T> pow (final int p) {
        if (f0.abs().getReal() == 0.0) {
            if (p == 0.0 ) return new FieldTimeDerivative<T>(one, zero, zero);
            return new FieldTimeDerivative<T>(zero, zero, zero);
        }
        if (Double.isInfinite(f0.pow(p).getReal()) || Double.isNaN(f0.pow(p).getReal())) {
            return new FieldTimeDerivative<T>(zero.add(Double.NaN), zero.add(Double.NaN), zero.add(Double.NaN));
        } else {
            return new FieldTimeDerivative<T>( f0.pow(p),
                      f0.pow(p - 1).multiply(p).multiply(f1),
                      f0.pow(p - 2).multiply(p * (p - 1.0)).multiply(f1.pow(2.0)).add(f0.pow(p - 1.0).multiply(p).multiply(f2))
                      );

        }
    }


    @Override
    public FieldTimeDerivative<T> pow(final FieldTimeDerivative<T> e)
        throws MathIllegalArgumentException {
        if (Double.isInfinite(f0.pow(e.f0).getReal()) || Double.isNaN(f0.pow(e.f0).getReal())) {
            return new FieldTimeDerivative<T>(zero.add(Double.NaN), zero.add(Double.NaN), zero.add(Double.NaN));
        }
        return new FieldTimeDerivative<T>(f0.pow(e.f0),
                        f0.pow(e.f0).multiply(e.f1.multiply(f0.log()).add(e.f0.multiply(f1).divide(f0))),
                        f0.pow(e.f0.subtract(1)).multiply((e.f0.subtract(1).multiply(f1).divide(f0).subtract(f0.log().multiply(e.f1))).multiply(e.f0.multiply(f1).add( f0.multiply(f0.log()).multiply(e.f1))).add(
                        e.f0.multiply(f2).add(f1.multiply(2).multiply(e.f1.pow(2.0))).add(f1.multiply(f0.log()).multiply(e.f1)).add(f0.multiply(f0.log()).multiply(e.f2)) ))
                        );
    }

    @Override
    public FieldTimeDerivative<T> exp() {
        return new FieldTimeDerivative<T>(
                        f0.exp(),
                        f0.exp().multiply(f1),
                        f0.exp().multiply(f1.pow(2)).add(f0.exp().multiply(f2))
                        );
    }

    @Override
    public FieldTimeDerivative<T> expm1() {
        return exp().subtract(getField().getOne());
    }

    @Override
    public FieldTimeDerivative<T> log() {
        if (Double.isInfinite(f0.log().getReal()))
            return new FieldTimeDerivative<T>(
                            f0.log(),
                            f1.divide(f0),
                            zero.add(Double.NaN));
        return new FieldTimeDerivative<T>(
                        f0.log(),
                        f1.divide(f0),
                        f0.multiply(f2).subtract(f1.multiply(f1)).divide(f0.multiply(f0))
                        );
    }

    @Override
    public FieldTimeDerivative<T> log1p() {
        return add(getField().getOne()).log();
    }

    @Override
    public FieldTimeDerivative<T> log10() {
        return log().multiply(1.0 / FastMath.log(10));
    }

    @Override
    public FieldTimeDerivative<T> cos() {
        return new FieldTimeDerivative<T>( f0.cos(),
                        f1.multiply(f0.sin().multiply(-1)),
                        f2.multiply(f0.sin().multiply(-1)).subtract(f1.multiply(f1).multiply(f0.cos()))
                        );
    }

    @Override
    public FieldTimeDerivative<T> sin() {
        return new FieldTimeDerivative<T>( f0.sin(),
                        f1.multiply(f0.cos()),
                        f2.multiply(f0.cos()).subtract(f1.multiply(f1).multiply(f0.sin()))
                        );
    }

    @Override
    public FieldTimeDerivative<T> tan() {
        return sin().divide(cos());
    }

    @Override
    public FieldTimeDerivative<T> acos() {
        return new FieldTimeDerivative<T>( f0.acos(),
                        f1.multiply(-1).divide(one.subtract(f0.pow(2)).sqrt()),
                       (f2.subtract(f0.multiply(f0).multiply(f2)).add(f0.multiply(f1.multiply(f1)))).multiply(-1).divide(one.subtract(f0.multiply(f0)).pow(1.5)) );
    }

    @Override
    public FieldTimeDerivative<T> asin() {
        return new FieldTimeDerivative<T>(f0.asin(),
                        f1.divide(one.subtract(f0.pow(2)).sqrt()),
                        (f2.subtract(f0.pow(2).multiply(f2)).add(f0.multiply(f1.pow(2)))).divide(one.subtract(f0.multiply(f0)).pow(1.5))
                        );
    }

    @Override
    public FieldTimeDerivative<T> atan() {
        return new FieldTimeDerivative<T>(f0.atan(),
                        f1.divide(f0.multiply(f0).add(1)),
                        f2.multiply(f0.multiply(f0).add(1)).subtract(f1.multiply(f1).multiply(f0).multiply(2)).divide(f0.multiply(f0).add(1).multiply(f0.multiply(f0).add(1)))
                        );
    }

    @Override
    public FieldTimeDerivative<T> atan2(final FieldTimeDerivative<T> x)
        throws MathIllegalArgumentException {
        if (f0.getReal() == 0.0) {
            return new FieldTimeDerivative<T> (zero, zero.add(Double.NaN), zero.add(Double.NaN));
        }
        if (x.signum().getReal() >= 0) {
            return divide(x).atan();
        }
        if (signum().getReal() >= 0 && x.signum().getReal() < 0) {
            return divide(x).atan().add(FastMath.PI);
        }
        return divide(x).atan().add(FastMath.PI);

    }


    @Override
    public FieldTimeDerivative<T> cosh() {
        return new FieldTimeDerivative<T>(f0.cosh(),
                                       f1.multiply(f0.sinh()),
                                       f2.multiply(f0.sinh()).add(f1.multiply(f1).multiply(f0.cosh()))
                        );
    }

    @Override
    public FieldTimeDerivative<T> sinh() {
        return new FieldTimeDerivative<T>(f0.sinh(),
                                          f1.multiply(f0.cosh()),
                                          f2.multiply(f0.cosh()).add(f1.multiply(f1).multiply(f0.sinh()))
                                          );
    }

    @Override
    public FieldTimeDerivative<T> tanh() {
        return sinh().divide(cosh());
    }

    @Override
    public FieldTimeDerivative<T> acosh() {
        return multiply(this).subtract(1).sqrt().add(this).log();
    }

    @Override
    public FieldTimeDerivative<T> asinh() {
        return multiply(this).add(1).sqrt().add(this).log();
    }

    @Override
    public FieldTimeDerivative<T> atanh() {
        return this.add(1).divide(negate().add(1)).log().multiply(0.5);
    }

    @Override
    public FieldTimeDerivative<T> linearCombination(final FieldTimeDerivative<T>[] a,
                                                    final FieldTimeDerivative<T>[] b)
        throws MathIllegalArgumentException {
        FieldTimeDerivative<T> result = getField().getZero();
        for (int i = 0; i < a.length; i++) {
            result = result.add(a[i].multiply(b[i]));
        }
        return result;
    }

    @Override
    public FieldTimeDerivative<T> linearCombination(final double[] a,
                                                    final FieldTimeDerivative<T>[] b)
        throws MathIllegalArgumentException {
        FieldTimeDerivative<T> result = getField().getZero();
        for (int i = 0; i < a.length; i++) {
            result = result.add(b[i].multiply(a[i]));
        }
        return result;
    }

    @Override
    public FieldTimeDerivative<T> linearCombination(final FieldTimeDerivative<T> a1,
                                                    final FieldTimeDerivative<T> b1,
                                                    final FieldTimeDerivative<T> a2,
                                                    final FieldTimeDerivative<T> b2) {
        return a1.multiply(b1).add(a2.multiply(b2));
    }

    @Override
    public FieldTimeDerivative<T>
        linearCombination(final double a1, final FieldTimeDerivative<T> b1, final double a2,
                          final FieldTimeDerivative<T> b2) {
        return b1.multiply(a1).add(b2.multiply(a2));
    }

    @Override
    public FieldTimeDerivative<T>
        linearCombination(final FieldTimeDerivative<T> a1, final FieldTimeDerivative<T> b1,
                          final FieldTimeDerivative<T> a2, final FieldTimeDerivative<T> b2,
                          final FieldTimeDerivative<T> a3,
                          final FieldTimeDerivative<T> b3) {
        return b1.multiply(a1).add(b2.multiply(a2)).add(b3.multiply(a3));
    }

    @Override
    public FieldTimeDerivative<T>
        linearCombination(final double a1, final FieldTimeDerivative<T> b1, final double a2,
                          final FieldTimeDerivative<T> b2, final double a3,
                          final FieldTimeDerivative<T> b3) {
        return b1.multiply(a1).add(b2.multiply(a2)).add(b3.multiply(a3));
    }

    @Override
    public FieldTimeDerivative<T>
        linearCombination(final FieldTimeDerivative<T> a1, final FieldTimeDerivative<T> b1,
                          final FieldTimeDerivative<T> a2, final FieldTimeDerivative<T> b2,
                          final FieldTimeDerivative<T> a3, final FieldTimeDerivative<T> b3,
                          final FieldTimeDerivative<T> a4,
                          final FieldTimeDerivative<T> b4) {
        return b1.multiply(a1).add(b2.multiply(a2)).add(b3.multiply(a3)).add(b4.multiply(a4));
    }

    @Override
    public FieldTimeDerivative<T>
        linearCombination(final double a1, final FieldTimeDerivative<T> b1, final double a2,
                          final FieldTimeDerivative<T> b2, final double a3,
                          final FieldTimeDerivative<T> b3, final double a4,
                          final FieldTimeDerivative<T> b4) {
        return b1.multiply(a1).add(b2.multiply(a2)).add(b3.multiply(a3)).add(b4.multiply(a4));
    }

}
