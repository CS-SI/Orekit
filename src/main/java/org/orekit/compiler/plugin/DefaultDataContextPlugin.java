/* Contributed in the public domain.
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
package org.orekit.compiler.plugin;

import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic;
import java.util.EnumSet;
import java.util.Set;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.Plugin;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskEvent.Kind;
import com.sun.source.util.TaskListener;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreeScanner;
import com.sun.source.util.Trees;
import org.orekit.annotation.DefaultDataContext;

/**
 * Processes {@link DefaultDataContext} to issue warnings at compile time.
 *
 * <p>To use this plugin add {@code -Xplugin:dataContextPlugin} to the javac command line.
 * Tested with OpenJDK 8 and 11.
 *
 * <p>Do not reference this class unless executing within {@code javac} or you have added
 * {@code tools.jar} to the class path. {@code tools.jar} is part of the JDK, not JRE, and
 * is typically located at {@code JAVA_HOME/../lib/tools.jar}.
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

    /** Empty constructor.
     * <p>
     * This constructor is not strictly necessary, but it prevents spurious
     * javadoc warnings with JDK 18 and later.
     * </p>
     * @since 12.0
     */
    public DefaultDataContextPlugin() {
        // nothing to do
    }

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
            check(newClassTree);
            return super.visitNewClass(newClassTree, aVoid);
        }

        /**
         * Check if this bit of code calls into the default data context from outside the
         * default data context.
         *
         * @param tree to check.
         */
        private void check(final Tree tree) {
            final Element element = trees.getElement(trees.getPath(root, tree));
            check(tree, element);
        }

        /**
         * Check tricky bits of code.
         *
         * @param tree    used to check the containing scope and for logging.
         * @param element to check for {@link #ANNOTATION}.
         */
        private void check(final Tree tree, final Element element) {
            // element and its containing scopes.
            if (isAnyElementAnnotated(element)) {
                // using code annotated with @DefaultDataContext
                // check if current scope is also annotated
                if (!isAnyElementAnnotated(trees.getPath(root, tree))) {
                    // calling the default data context from a method without an annotation
                    final String message = MESSAGE + " Used: " + element.getKind() + " " + element;
                    trees.printMessage(Diagnostic.Kind.WARNING, message, tree, root);
                }
            }
        }

        /**
         * Determine if any enclosing element has {@link #ANNOTATION}. Walks towards the
         * tree root checking each node for the annotation.
         *
         * @param element to start the search from. May be {@code null}.
         * @return {@code true} if {@code element} or any of its parents are annotated,
         * {@code false} otherwise.
         */
        private boolean isAnyElementAnnotated(final Element element) {
            Element e = element;
            while (e != null) {
                if (e.getAnnotation(ANNOTATION) != null) {
                    return true;
                }
                e = e.getEnclosingElement();
            }
            return false;
        }

        /**
         * Determine if any enclosing tree has {@link #ANNOTATION}. Walks towards the tree
         * root checking each node for the annotation.
         *
         * @param path to start the search from. May be {@code null}.
         * @return {@code true} if {@code path} or any of its parents are annotated,
         * {@code false} otherwise.
         */
        private boolean isAnyElementAnnotated(final TreePath path) {
            // Kinds of declarations which can be annotated
            final Set<Tree.Kind> toCheck = EnumSet.of(
                    Tree.Kind.METHOD, Tree.Kind.CLASS, Tree.Kind.VARIABLE,
                    Tree.Kind.INTERFACE, Tree.Kind.ENUM);
            TreePath next = path;
            while (next != null) {
                if (toCheck.contains(next.getLeaf().getKind())) {
                    if (trees.getElement(next).getAnnotation(ANNOTATION) != null) {
                        return true;
                    }
                }
                next = next.getParentPath();
            }
            return false;
        }

    }

}
