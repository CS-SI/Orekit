package fr.cs.orekit.frames;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import fr.cs.orekit.errors.OrekitException;
import fr.cs.orekit.iers.BulletinBFilesLoader;
import fr.cs.orekit.iers.EOPC04FilesLoader;
import fr.cs.orekit.iers.EarthOrientationParameters;
import fr.cs.orekit.time.AbsoluteDate;

/** This class holds Earth Orientation data throughout a large time range.
 * It is a singleton since it handles voluminous data.
 * @author L. Maisonobe
 * @author F. Maussion
 */
public class EarthOrientationHistory implements Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = -6983677302885110865L;

    /** Earth Orientation Parameters. */
    private TreeSet eop = null;

    /** Previous EOP entry. */
    private EarthOrientationParameters previous;

    /** Next EOP entry. */
    private EarthOrientationParameters next;

   /** Private constructor for the singleton.
     * @exception OrekitException if there is a problem while reading IERS data
     */
    private EarthOrientationHistory() throws OrekitException {

        // set up a date-ordered set, able to use either
        // EarthOrientationParameters or AbsoluteDate instances
        // (beware to use AbsoluteDate ONLY as arguments to
        // headSet or tailSet and NOT to add them in the set)
        eop = new TreeSet(new EOPComparator());

        // consider first the more accurate EOP C 04 entries
        new EOPC04FilesLoader(eop).loadEOP();

        // add the final values from bulletin B entries for new dates
        // (if duplicated dates occur, the existing data will be preserved)
        new BulletinBFilesLoader(eop).loadEOP();

        // check the continuity of the loaded data
        checkEOPContinuity(5);

    }

    /** Get the singleton instance.
     * @return the unique dated eop reader instance.
     * @exception OrekitException when there is a problem while reading IERS data
     */
    public static EarthOrientationHistory getInstance() throws OrekitException {
        if (LazyHolder.INSTANCE == null) {
            throw LazyHolder.OREKIT_EXCEPTION;
        }
        return LazyHolder.INSTANCE;
    }

    /** Check Earth orientation parameters continuity.
     * @param maxGap maximal allowed gap between entries (in days)
     * @exception OrekitException if there are holes in the data sequence
     */
    private void checkEOPContinuity(final int maxGap) throws OrekitException {
        EarthOrientationParameters current = null;
        for (final Iterator iterator = eop.iterator(); iterator.hasNext();) {
            final EarthOrientationParameters preceding = current;
            current = (EarthOrientationParameters) iterator.next();

            // compare the dates of preceding and current entries
            if ((preceding != null) && ((current.getMjd() - preceding.getMjd()) > maxGap)) {
                throw new OrekitException("missing Earth Orientation Parameters between {0} and {1}",
                                          new Object[] {
                                              preceding, current
                                          });

            }
        }
    }

    /** Get the date of the first available Earth Orientation Parameters.
     * @return the start date of the available data
     */
    public AbsoluteDate getStartDate() {
        return ((EarthOrientationParameters) eop.first()).getDate();
    }

    /** Get the date of the last available Earth Orientation Parameters.
     * @return the end date of the available data
     */
    public AbsoluteDate getEndDate() {
        return ((EarthOrientationParameters) eop.last()).getDate();
    }

    /** Get the UT1-UTC value.
     * <p>The data provided comes from the EOP C 04 files. It
     * is smoothed data.</p>
     * @param date date at which the value is desired
     * @return UT1-UTC in seconds (0 if date is outside covered range)
     */
    protected double getUT1MinusUTC(final AbsoluteDate date) {
        if (selectBracketingEntries(date)) {
            final double dtP = date.minus(previous.getDate());
            final double dtN = next.getDate().minus(date);
            return (dtP * next.getUT1MinusUTC() + dtN * previous.getUT1MinusUTC()) / (dtN + dtP);
        }
        return 0;
    }

    /** Get the pole IERS Reference Pole correction.
     * <p>The data provided comes from the EOP C 04 files. It
     * is smoothed data.</p>
     * @param date date at which the correction is desired
     * @return pole correction ({@link PoleCorrection#NULL_CORRECTION
     * PoleCorrection.NULL_CORRECTION} if date is outside covered range)
     */
    protected PoleCorrection getPoleCorrection(final AbsoluteDate date) {
        if (selectBracketingEntries(date)) {
            final double dtP    = date.minus(previous.getDate());
            final double dtN    = next.getDate().minus(date);
            final double sum    = dtN + dtP;
            final double coeffP = dtN / sum;
            final double coeffN = dtP / sum;
            return new PoleCorrection(coeffP * previous.getPoleCorrection().xp +
                                      coeffN * next.getPoleCorrection().xp,
                                      coeffP * previous.getPoleCorrection().yp +
                                      coeffN * next.getPoleCorrection().yp);
        }
        return PoleCorrection.NULL_CORRECTION;
    }

    /** Select the entries bracketing a specified date.
     * @param  date target date
     * @return true if the date was found in the tables
     */
    private boolean selectBracketingEntries(final AbsoluteDate date) {

        // don't search if the cached selection is fine
        if ((previous != null) && (date.minus(previous.getDate()) >= 0) &&
            (next != null) && (date.minus(next.getDate()) < 0)) {
            // the current selection is already good
            return true;
        }

        // select the bracketing elements (may be null)
        final SortedSet head = eop.headSet(date);
        previous = (EarthOrientationParameters) (head.isEmpty() ? null : head.last());
        final SortedSet tail = eop.tailSet(date);
        next     = (EarthOrientationParameters) (tail.isEmpty() ? null : tail.first());

        return (previous != null) && (next != null);

    }

    /** Specialized comparator handling both {@link EarthOrientationParameters}
     * and {@link AbsoluteDate} instances.
     */
    private static class EOPComparator implements Comparator, Serializable {

        /** Serializable UID. */
        private static final long serialVersionUID = -8636906467091448424L;

        /** Build a comparator for either {@link AbsoluteDate} or
         * {@link EarthOrientationParameters} instances.
         * @param o1 first object
         * @param o2 second object
         * @return a negative integer if o1 is before o2, 0 if they are
         * are the same time, a positive integer otherwise
         */
        public int compare(final Object o1, final Object o2) {
            final AbsoluteDate d1 =
                (o1 instanceof AbsoluteDate) ?
                        ((AbsoluteDate) o1) : ((EarthOrientationParameters) o1).getDate();
            final AbsoluteDate d2 =
                (o2 instanceof AbsoluteDate) ?
                        ((AbsoluteDate) o2) : ((EarthOrientationParameters) o2).getDate();
            return d1.compareTo(d2);
        }

    }

    /** Holder for the singleton.
     * <p>We use the Initialization on demand holder idiom to store
     * the singleton, as it is both thread-safe, efficient (no
     * synchronization) and works with all version of java.</p>
     */
    private static class LazyHolder {

        /** Unique instance. */
        private static final EarthOrientationHistory INSTANCE;

        /** Reason why the unique instance may be missing (i.e. null). */
        private static final OrekitException OREKIT_EXCEPTION;

        static {
            EarthOrientationHistory tmpInstance = null;
            OrekitException tmpException = null;
            try {
                tmpInstance = new EarthOrientationHistory();
            } catch (OrekitException oe) {
                tmpException = oe;
            }
            INSTANCE        = tmpInstance;
            OREKIT_EXCEPTION = tmpException;
        }

    }

}
