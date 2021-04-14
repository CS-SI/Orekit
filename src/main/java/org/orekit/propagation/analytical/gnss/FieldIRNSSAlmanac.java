/* Copyright 2002-2021 CS GROUP
 * Licensed to CS GROUP (CS) under one or more
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
package org.orekit.propagation.analytical.gnss;

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.util.FastMath;
import org.orekit.time.FieldAbsoluteDate;

/**
 * Class for IRNSS almanac.
 *
 * @see "Indian Regiona Navigation Satellite System, Signal In Space ICD for
 *      standard positioning service, version 1.1 - Table 28"
 *
 * @author Bryan Cazabonne
 * @author Nicolas Fialton (field translation)
 *
 */
public class FieldIRNSSAlmanac<T extends RealFieldElement<T>> implements FieldIRNSSOrbitalElements<T> {

	private final T zero;
	/** PRN number. */
	private final int prn;

	/** IRNSS week. */
	private final int week;

	/** Time of applicability. */
	private final T toa;

	/** Semi-major axis. */
	private final T sma;

	/** Eccentricity. */
	private final T ecc;

	/** Inclination. */
	private final T inc;

	/** Longitude of Orbital Plane. */
	private final T om0;

	/** Rate of Right Ascension. */
	private final T dom;

	/** Argument of perigee. */
	private final T aop;

	/** Mean anomaly. */
	private final T anom;

	/** Zeroth order clock correction. */
	private final T af0;

	/** First order clock correction. */
	private final T af1;

	/** Date of aplicability. */
	private final FieldAbsoluteDate<T> date;

	/**
	 * Constructor.
	 * 
	 * @param prn  the PRN number
	 * @param week the GPS week
	 * @param toa  the Time of Applicability
	 * @param sqa  the Square Root of Semi-Major Axis (m^1/2)
	 * @param ecc  the eccentricity
	 * @param inc  the inclination (rad)
	 * @param om0  the geographic longitude of the orbital plane at the weekly epoch
	 *             (rad)
	 * @param dom  the Rate of Right Ascension (rad/s)
	 * @param aop  the Argument of Perigee (rad)
	 * @param anom the Mean Anomaly (rad)
	 * @param af0  the Zeroth Order Clock Correction (s)
	 * @param af1  the First Order Clock Correction (s/s)
	 * @param date of applicability corresponding to {@code toa}.
	 */
	public FieldIRNSSAlmanac(final int prn, final int week, final T toa, final T sqa, final T ecc, final T inc,
			final T om0, final T dom, final T aop, final T anom, final T af0, final T af1,
			final FieldAbsoluteDate<T> date) {
		this.zero = toa.getField().getZero();
		this.prn = prn;
		this.week = week;
		this.toa = toa;
		this.sma = sqa.multiply(sqa);
		this.ecc = ecc;
		this.inc = inc;
		this.om0 = om0;
		this.dom = dom;
		this.aop = aop;
		this.anom = anom;
		this.af0 = af0;
		this.af1 = af1;
		this.date = date;
	}

	/**
	 * Constructor
	 * 
	 * This constructor converts an IRNSSOrbitalElements (almanac) into a FieldIRNSSAlmanac
	 * 
	 * @param field
	 * @param almanac
	 */
	public FieldIRNSSAlmanac(Field<T> field, IRNSSOrbitalElements almanac) {
		this.zero = field.getZero();
		this.prn = almanac.getPRN();
		this.week = almanac.getWeek();
		this.toa = zero.add(almanac.getTime());
		this.sma = zero.add(almanac.getSma());
		this.ecc = zero.add(almanac.getE());
		this.inc = zero.add(almanac.getI0());
		this.om0 = zero.add(almanac.getOmega0());
		this.dom = zero.add(almanac.getOmegaDot());
		this.aop = zero.add(almanac.getPa());
		this.anom = zero.add(almanac.getM0());
		this.af0 = zero.add(almanac.getAf0());
		this.af1 = zero.add(almanac.getAf1());
		this.date = new FieldAbsoluteDate<>(field, almanac.getDate());
	}

	@Override
	public FieldAbsoluteDate<T> getDate() {
		return date;
	}

	@Override
	public int getPRN() {
		return prn;
	}

	@Override
	public int getWeek() {
		return week;
	}

	@Override
	public T getTime() {
		return toa;
	}

	@Override
	public T getSma() {
		return sma;
	}

	@Override
	public T getMeanMotion() {
		final T absA = FastMath.abs(sma);
		return FastMath.sqrt(absA.getField().getZero().add(IRNSS_MU).divide(absA)).divide(absA);
	}

	@Override
	public T getE() {
		return ecc;
	}

	@Override
	public T getI0() {
		return inc;
	}

	@Override
	public T getIDot() {
		return zero;
	}

	@Override
	public T getOmega0() {
		return om0;
	}

	@Override
	public T getOmegaDot() {
		return dom;
	}

	@Override
	public T getPa() {
		return aop;
	}

	@Override
	public T getM0() {
		return anom;
	}

	@Override
	public T getCuc() {
		return zero;
	}

	@Override
	public T getCus() {
		return zero;
	}

	@Override
	public T getCrc() {
		return zero;
	}

	@Override
	public T getCrs() {
		return zero;
	}

	@Override
	public T getCic() {
		return zero;
	}

	@Override
	public T getCis() {
		return zero;
	}

	@Override
	public T getAf0() {
		return af0;
	}

	@Override
	public T getAf1() {
		return af1;
	}

	@Override
	public T getAf2() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public T getToc() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getIODEC() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public T getTGD() {
		// TODO Auto-generated method stub
		return null;
	}

}
