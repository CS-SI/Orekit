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
    public void method() {

    }

    @DefaultDataContext
    public static void staticMethod() {

    }

    @DefaultDataContext
    public Bad() {

    }

    public Bad(boolean b) {

    }

}
