import org.orekit.annotation.DefaultDataContext;

/** All methods/constructors should have warnings unless otherwise stated. */
public class User extends Bad {

    private final Inner inner = null;

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

    void staticFieldAccess3() {
        String a = Inner.staticField;
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

    void fieldAccess4(Inner i) {
        String a = i.field;
    }

    void fieldAccess5() {
        String a = inner.field;
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

    void methodCall5(Inner i) {
        i.method();
    }

    void methodCall6() {
        inner.method();
    }

    void staticMethodCall() {
        Bad.staticMethod();
    }

    void staticMethodCall2() {
        staticMethod();
    }

    void staticMethodCall3() {
        Inner.staticMethod();
    }

    void constructor() {
        new Bad();
    }

    void constructor2() {
        new Inner();
    }

    void anonymous() {
        new Bad() {
        };
    }

    enum Enum {

        A() {
            @Override
            public void method() {
                // no error
            }
        },
        B() {
            @Override
            public void method() {
                Object a = Bad.staticField;
            }
        };

        public abstract void method();

    }

}
