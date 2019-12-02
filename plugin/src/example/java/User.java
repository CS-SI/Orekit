import org.orekit.annotation.DefaultDataContext;

/** All methods/constructors should have warnings unless otherwise stated. */
public class User extends Bad {

    User() {
    }

    User(String s) {
        super();
    }

    @DefaultDataContext
    User(long l) { // no error
    }

    User(int i) {
        this((long) i);
    }

    void staticFieldAccess() {
        String a = Bad.staticField;
    }

    void staticFieldAccess2() {
        String a = staticField;
    }

    void fieldAccess() {
        String a = this.field;
    }

    void fieldAccess2() {
        String a = field;
    }

    void fieldAccess3() {
        String a = super.field;
    }

    void methodCall() {
        this.method();
    }

    void methodCall2() {
        method();
    }

    void methodCall3() {
        new Bad(true).method();
    }

    void methodCall4() {
        super.method();
    }

    void staticMethodCall() {
        Bad.staticMethod();
    }

    void staticMethodCall2() {
        staticMethod();
    }

    void constructor() {
        new Bad();
    }

    void anonymous() {
        new Bad() {
        };
    }

}
