package org.orekit.annotation;

import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Scope;
import com.sun.source.tree.Tree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.Plugin;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskEvent.Kind;
import com.sun.source.util.TaskListener;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreeScanner;
import com.sun.source.util.Trees;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCNewClass;
import com.sun.tools.javac.tree.TreeInfo;

/**
 * Processes {@link DefaultDataContext} to issue warnings at compile time.
 *
 * @author Evan Ward
 * @since 10.1
 */
@SupportedAnnotationTypes("org.orekit.annotation.DefaultDataContext")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class DefaultDataContextPlugin implements Plugin, TaskListener {

    /** Warning message. */
    static final String MESSAGE = "Use of the default data context from a scope not " +
            "annotated with @DefaultDataContext. This code may be unintentionally " +
            "using the default data context.";
    /** Annotation to search for. */
    private static final Class<DefaultDataContext> ANNOTATION = DefaultDataContext.class;

    /** Compiler Trees. */
    private Trees trees;

    @Override
    public String getName() {
        return "dataContextPlugin";
    }

    @Override
    public synchronized void init(final JavacTask javacTask, final String... args) {
        javacTask.addTaskListener(this);
        trees = Trees.instance(javacTask);
    }

    @Override
    public void started(final TaskEvent taskEvent) {
    }

    @Override
    public void finished(final TaskEvent taskEvent) {
        if (taskEvent.getKind() == Kind.ANALYZE) {
            final CompilationUnitTree root = taskEvent.getCompilationUnit();
            root.accept(new AnnotationTreeScanner(root), null);
        }
    }

    /** Finds when an annotation is used and checks the scope has the same annotation. */
    private class AnnotationTreeScanner extends TreeScanner<Void, Void> {

        /** Compilation root. */
        private final CompilationUnitTree root;

        /**
         * Create a scanner.
         *
         * @param root of the compilation.
         */
        AnnotationTreeScanner(final CompilationUnitTree root) {
            this.root = root;
        }

        @Override
        public Void visitIdentifier(final IdentifierTree identifierTree,
                                    final Void aVoid) {
            check(identifierTree);
            return super.visitIdentifier(identifierTree, aVoid);
        }

        @Override
        public Void visitMemberSelect(final MemberSelectTree memberSelectTree,
                                      final Void aVoid) {
            check(memberSelectTree);
            return super.visitMemberSelect(memberSelectTree, aVoid);
        }

        @Override
        public Void visitNewClass(final NewClassTree newClassTree, final Void aVoid) {
            // need a hack to get the constructor
            // constructor has no associated Tree
            final Symbol constructorSymbol = ((JCNewClass) newClassTree).constructor;
            check(newClassTree, constructorSymbol);
            return super.visitNewClass(newClassTree, aVoid);
        }

        @Override
        public Void visitMethodInvocation(final MethodInvocationTree methodInvocationTree,
                                          final Void aVoid) {
            // member select covers this case
            // check(methodInvocationTree.getMethodSelect());
            return super.visitMethodInvocation(methodInvocationTree, aVoid);
        }

        /**
         * Print diagnostic information.
         *
         * @param tree to log.
         */
        private void log(final Tree tree) {
            final Tree parent = trees.getPath(root, tree).getParentPath().getLeaf();
            System.out.printf("%s %s %s %s %s\n", tree.getKind(), tree,
                    tree instanceof IdentifierTree ? ((IdentifierTree) tree).getName() : "",
                    parent.getKind(), parent);
        }

        /**
         * Check if this bit of code calls into the default data context from outside the
         * default data context.
         *
         * @param tree to check.
         */
        private void check(final Tree tree) {
            final Element element = TreeInfo.symbol((JCTree) tree);
            check(tree, element);
        }

        /**
         * Check tricky bits of code.
         *
         * @param tree    used to check the containing scope and for logging.
         * @param element to check for {@link #ANNOTATION}.
         */
        private void check(final Tree tree, final Element element) {
            if (element != null && element.getAnnotation(ANNOTATION) != null) {
                // calling a method annotated with @DefaultDataContext
                // check if current scope is also annotated
                final TreePath path = trees.getPath(root, tree);
                final Scope scope = trees.getScope(path);
                final TypeElement enclosingClass = scope.getEnclosingClass();
                final ExecutableElement enclosingMethod = scope.getEnclosingMethod();
                final boolean isClassAnnotated =
                        enclosingClass.getAnnotation(ANNOTATION) != null;
                final boolean isMethodAnnotated = enclosingMethod != null &&
                        enclosingMethod.getAnnotation(ANNOTATION) != null;
                boolean isFieldAnnotated = false;
                if (enclosingMethod == null) {
                    // not in a method, check field
                    // iterate towards the root of the tree until we find a field
                    TreePath next = path;
                    while (next != null) {
                        final Tree leaf = next.getLeaf();
                        if (leaf.getKind() == Tree.Kind.VARIABLE) {
                            isFieldAnnotated =
                                    trees.getElement(next).getAnnotation(ANNOTATION) != null;
                            break;
                        }
                        next = next.getParentPath();
                    }
                }
                if (!(isClassAnnotated || isMethodAnnotated || isFieldAnnotated)) {
                    // calling the default data context from a method without an annotation
                    final String message = MESSAGE + " Used: " + element.getKind() + " " + element;
                    trees.printMessage(Diagnostic.Kind.WARNING, message, tree, root);
                }
            }
        }

    }

}
