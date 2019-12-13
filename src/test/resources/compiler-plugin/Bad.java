import org.orekit.annotation.DefaultDataContext;

/**
 * A class that uses the default data context in many ways.
 *
 * @author Evan Ward
 */
public class Bad {

    @DefaultDataContext
    public static String staticField = "";

    @DefaultDataContext
    public String field = "";

    @DefaultDataContext
    public Bad method() {
        return this;
    }

    @DefaultDataContext
    public static String staticMethod() {
        return null;
    }

    @DefaultDataContext
    public Bad() {

    }

    public Bad(boolean b) {

    }

    @DefaultDataContext
    public static class Inner {

        public static String staticField;

        public static void staticMethod() {
        }

        public String field;

        public void method() {
        }
    }

    /* Code below checks warnings are not generated when annotations are applied. */

    @DefaultDataContext
    private void noWarnings() {
        final Bad bad = new Bad();
        bad.method();
        String a = bad.field;
        Bad.staticMethod();
        a = Bad.staticField;
        new Bad() {
        };
    }

    @DefaultDataContext
    private static class NoWarnings {
        {
            final Bad bad = new Bad();
            bad.method();
            String a = bad.field;
            Bad.staticMethod();
            a = Bad.staticField;
            new Bad() {
            };
        }
    }

    @DefaultDataContext
    private String noWarnings1 = new Bad().method().field;
    @DefaultDataContext
    private String noWarnings2 = Bad.staticField;
    @DefaultDataContext
    private String noWarnings3 = Bad.staticMethod();

}
