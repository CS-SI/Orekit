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
package org.orekit;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.SortedSet;
import java.util.TimeZone;
import java.util.TreeSet;

import org.hipparchus.exception.DummyLocalizable;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.forces.drag.atmosphere.DTM2000InputParameters;
import org.orekit.forces.drag.atmosphere.JB2006InputParameters;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.ChronologicalComparator;
import org.orekit.time.TimeScalesFactory;
import org.orekit.time.TimeStamped;
import org.orekit.utils.Constants;


/** This class reads and provides solar activity data needed by the
 * two atmospheric models. The data are furnished at the <a
 * href="http://sol.spacenvironment.net/~JB2006/">
 * official JB2006 website.</a>
 *
 * @author Fabien Maussion
 */
public class SolarInputs97to05 implements JB2006InputParameters, DTM2000InputParameters {

    /** Serializable UID. */
    private static final long serialVersionUID = -3687601846334870069L;

    private static final double third = 1.0/3.0;

    private static final double[] kpTab = new double[] {
        0, 0+third, 1-third, 1, 1+third, 2-third, 2, 2+third,
        3-third, 3, 3+third, 4-third, 4, 4+third, 5-third, 5,
        5+third, 6-third, 6, 6+third, 7-third, 7, 7+third,
        8-third, 8, 8+third, 9-third, 9
    };

    private static final double[] apTab = new double[] {
        0, 2, 3, 4, 5, 6, 7, 9, 12, 15, 18, 22, 27, 32,
        39, 48, 56, 67, 80, 94, 111, 132, 154 , 179, 207, 236, 300, 400
    };

    /** All entries. */
    private SortedSet<TimeStamped> data;

    private LineParameters currentParam;
    private AbsoluteDate firstDate;
    private AbsoluteDate lastDate;

    /** Simple constructor.
     * Data file address is set internally, nothing to be done here.
     *
     * @exception OrekitException
     */
    private SolarInputs97to05() throws OrekitException {

        data = new TreeSet<TimeStamped>(new ChronologicalComparator());
        InputStream in = SolarInputs97to05.class.getResourceAsStream("/atmosphere/JB_All_97-05.txt");
        BufferedReader rFlux = new BufferedReader(new InputStreamReader(in));


        in = SolarInputs97to05.class.getResourceAsStream("/atmosphere/NOAA_ap_97-05.dat.txt");
        BufferedReader rAp = new BufferedReader(new InputStreamReader(in));

        try {
            read(rFlux, rAp);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /** Singleton getter.
     * @return the unique instance of this class.
     * @exception OrekitException
     */
    public static SolarInputs97to05 getInstance() throws OrekitException {
        if (LazyHolder.instance == null) {
            throw LazyHolder.orekitException;
        }
        return LazyHolder.instance;
    }

    private void read(BufferedReader rFlux, BufferedReader rAp) throws IOException, OrekitException {

        rFlux.readLine();
        rFlux.readLine();
        rFlux.readLine();
        rFlux.readLine();
        rAp.readLine();
        String lineAp;
        String[] flux;
        String[] ap;
        Calendar cal = new GregorianCalendar();
        cal.setTimeZone(TimeZone.getTimeZone("UTC"));
        cal.set(0, 0, 0, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);

        AbsoluteDate date = null;
        boolean first = true;

        for (String lineFlux = rFlux.readLine(); lineFlux != null; lineFlux = rFlux.readLine()) {

            flux = lineFlux.trim().split("\\s+");

            lineAp = rAp.readLine();
            if (lineAp == null) {
                throw new OrekitException(new DummyLocalizable("inconsistent JB2006 and geomagnetic indices files"));
            }
            ap = lineAp.trim().split("\\s+");

            int fluxYear = Integer.parseInt(flux[0]);
            int fluxDay = Integer.parseInt(flux[1]);
            int apYear  = Integer.parseInt(ap[11]);

            if (fluxDay != Integer.parseInt(ap[0])) {
                throw new OrekitException(new DummyLocalizable("inconsistent JB2006 and geomagnetic indices files"));
            }
            if (((fluxYear <  2000) && ((fluxYear - 1900) != apYear)) ||
                ((fluxYear >= 2000) && ((fluxYear - 2000) != apYear))) {
                throw new OrekitException(new DummyLocalizable("inconsistent JB2006 and geomagnetic indices files"));
            }

            cal.set(Calendar.YEAR, fluxYear);
            cal.set(Calendar.DAY_OF_YEAR, fluxDay);

            date = new AbsoluteDate(cal.getTime(), TimeScalesFactory.getUTC());

            if (first) {
                first = false;
                firstDate = date;
            }

            data.add(new LineParameters(date,
                                        new double[] {
                    Double.parseDouble(ap[3]),
                    Double.parseDouble(ap[4]),
                    Double.parseDouble(ap[5]),
                    Double.parseDouble(ap[6]),
                    Double.parseDouble(ap[7]),
                    Double.parseDouble(ap[8]),
                    Double.parseDouble(ap[9]),
                    Double.parseDouble(ap[10]),

            },
            Double.parseDouble(flux[3]),
            Double.parseDouble(flux[4]),
            Double.parseDouble(flux[5]),
            Double.parseDouble(flux[6]),
            Double.parseDouble(flux[7]),
            Double.parseDouble(flux[8])));

        }
        lastDate = date;

    }

    private void findClosestLine(AbsoluteDate date) throws OrekitException {

        if ((date.durationFrom(firstDate) < 0) || (date.durationFrom(lastDate) > Constants.JULIAN_DAY)) {
            throw new OrekitException(OrekitMessages.OUT_OF_RANGE_EPHEMERIDES_DATE, date, firstDate, lastDate);
        }

        // don't search if the cached selection is fine
        if ((currentParam != null) && (date.durationFrom(currentParam.date) >= 0) &&
                (date.durationFrom(currentParam.date) < Constants.JULIAN_DAY )) {
            return;
        }
        LineParameters before = new LineParameters(date.shiftedBy(-Constants.JULIAN_DAY), null, 0, 0, 0, 0, 0, 0);

        // search starting from entries a few steps before the target date
        SortedSet<TimeStamped> tailSet = data.tailSet(before);
        if (tailSet != null) {
            currentParam = (LineParameters) tailSet.first();
            if (currentParam.date.durationFrom(date) == -Constants.JULIAN_DAY) {
                currentParam = (LineParameters) data.tailSet(date).first();
            }
        } else {
            throw new OrekitException(new DummyLocalizable("unable to find data for date {0}"), date);
        }
    }

    /** Container class for Solar activity indexes.  */
    private static class LineParameters implements TimeStamped, Serializable {

        /** Serializable UID. */
        private static final long serialVersionUID = -1127762834954768272L;

        /** Entries */
        private  final AbsoluteDate date;
        private final double[] ap;
        private final double f10;
        private final double f10B;
        private final double s10;
        private final double s10B;
        private final double xm10;
        private final double xm10B;


        /** Simple constructor. */
        private LineParameters(AbsoluteDate date, double[]  ap, double f10,
                               double f10B, double s10, double s10B,
                               double xm10, double xm10B) {
            this.date = date;
            this.ap = ap;
            this.f10 = f10;
            this.f10B = f10B;
            this.s10 = s10;
            this.s10B = s10B;
            this.xm10 = xm10;
            this.xm10B = xm10B;

        }

        /** Get the current date */
        public AbsoluteDate getDate() {
            return date;
        }

    }

    public double getAp(AbsoluteDate date) {
        double result = Double.NaN;
        try {
            findClosestLine(date);
            Calendar cal = new GregorianCalendar();
            cal.setTimeZone(TimeZone.getTimeZone("UTC"));
            cal.setTime(date.toDate(TimeScalesFactory.getUTC()));
            int hour = cal.get(Calendar.HOUR_OF_DAY);
            for (int i= 0; i<8; i++) {
                if ((hour >= (i * 3)) && (hour < ((i + 1) * 3))) {
                    result = currentParam.ap[i];
                }
            }
        }
        catch (OrekitException e) {
            // nothing
        }
        return result;
    }

    public double getF10(AbsoluteDate date) {
        double result = Double.NaN;
        try {
            findClosestLine(date);
            result=currentParam.f10;
        }
        catch (OrekitException e) {
            // nothing
        }
        return result;
    }

    public double getF10B(AbsoluteDate date) {
        double result = Double.NaN;
        try {
            findClosestLine(date);
            result=currentParam.f10B;
        }
        catch (OrekitException e) {
            // nothing
        }
        return result;
    }

    public AbsoluteDate getMaxDate() {
        return lastDate.shiftedBy(Constants.JULIAN_DAY);
    }

    public AbsoluteDate getMinDate() {
        return firstDate;
    }

    public double getS10(AbsoluteDate date) {
        double result = Double.NaN;
        try {
            findClosestLine(date);
            result=currentParam.s10;
        }
        catch (OrekitException e) {
            // nothing
        }
        return result;
    }

    public double getS10B(AbsoluteDate date) {
        double result = Double.NaN;
        try {
            findClosestLine(date);
            result=currentParam.s10B;
        }
        catch (OrekitException e) {
            // nothing
        }
        return result;
    }

    public double getXM10(AbsoluteDate date) {
        double result = Double.NaN;
        try {
            findClosestLine(date);
            result=currentParam.xm10;
        }
        catch (OrekitException e) {
            // nothing
        }
        return result;
    }

    public double getXM10B(AbsoluteDate date) {
        double result = Double.NaN;
        try {
            findClosestLine(date);
            result=currentParam.xm10B;
        }
        catch (OrekitException e) {
            // nothing
        }
        return result;
    }

    public double get24HoursKp(AbsoluteDate date) {
        double result = 0;
        AbsoluteDate myDate = date;

        for (int i=0; i<8; i++) {
            result += getThreeHourlyKP(date);
            myDate = myDate.shiftedBy(3 * 3600);
        }

        return result/8;
    }

    public double getInstantFlux(AbsoluteDate date) {
        return getF10(date);
    }

    public double getMeanFlux(AbsoluteDate date) {
        return getF10B(date);
    }

    /** The 3-H Kp is derived from the Ap index.
     * The used method is explained on <a
     * href="http://www.ngdc.noaa.gov/stp/GEOMAG/kp_ap.shtml">
     * NOAA website.</a>. Here is the corresponding tab :
     * <pre>
     * The scale is O to 9 expressed in thirds of a unit, e.g. 5- is 4 2/3,
     * 5 is 5 and 5+ is 5 1/3.
     *
     * The 3-hourly ap (equivalent range) index is derived from the Kp index as follows:
     *
     * Kp = 0o   0+   1-   1o   1+   2-   2o   2+   3-   3o   3+   4-   4o   4+
     * ap =  0    2    3    4    5    6    7    9   12   15   18   22   27   32
     * Kp = 5-   5o   5+   6-   6o   6+   7-   7o   7+   8-   8o   8+   9-   9o
     * ap = 39   48   56   67   80   94  111  132  154  179  207  236  300  400
     *
     * </pre>
     */
    public double getThreeHourlyKP(AbsoluteDate date) {

        double ap = getAp(date);
        int i = 0;
        for ( i= 0; ap>=apTab[i]; i++) {
            if (i==apTab.length-1) {
                i++;
                break;
            }
        }
        return kpTab[i-1];
    }

    /** Holder for the singleton.
     * <p>We use the Initialization on demand holder idiom to store
     * the singleton, as it is both thread-safe, efficient (no
     * synchronization) and works with all versions of java.</p>
     */
    private static class LazyHolder {
        private static final SolarInputs97to05 instance;
        private static final OrekitException orekitException;
        static {
            SolarInputs97to05 tmpInstance = null;
            OrekitException tmpException = null;
            try {
                tmpInstance = new SolarInputs97to05();
            } catch (OrekitException oe) {
                tmpException = oe;
            }
            instance        = tmpInstance;
            orekitException = tmpException;
        }
    }

}
