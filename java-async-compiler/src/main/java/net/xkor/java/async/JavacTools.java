package net.xkor.java.async;

import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.model.JavacTypes;
import com.sun.tools.javac.parser.Parser;
import com.sun.tools.javac.parser.ParserFactory;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Pair;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class JavacTools {
    private final TreeMaker maker;
    private final ParserFactory parserFactory;
    private final JavacElements javacElements;
    private final JavacProcessingEnvironment environment;
    private final JavacTypes typeUtils;
    private final JavacTrees trees;
    private final Types types;
    private Method newParserMethod;

    public JavacTools(ProcessingEnvironment environment) {
        this.environment = (JavacProcessingEnvironment) environment;
        maker = TreeMaker.instance(this.environment.getContext());
        parserFactory = ParserFactory.instance(this.environment.getContext());
        javacElements = this.environment.getElementUtils();
        typeUtils = this.environment.getTypeUtils();
        trees = JavacTrees.instance(this.environment);
        types = Types.instance(this.environment.getContext());

        try {
            newParserMethod = ParserFactory.class.getMethod("newParser", CharSequence.class, Boolean.TYPE, Boolean.TYPE, Boolean.TYPE);
        } catch (NoSuchMethodException ignored) {
        }
    }

    public Pair<JCTree, JCTree.JCCompilationUnit> getTreeAndTopLevel(Element e) {
        return javacElements.getTreeAndTopLevel(e, null, null);
    }

    public JCTree getTree(Element e) {
        return javacElements.getTree(e);
    }

    public JavacElements getJavacElements() {
        return javacElements;
    }

    public JavacTypes getTypeUtils() {
        return typeUtils;
    }

    public JavacTrees getTrees() {
        return trees;
    }

    public Types getTypes() {
        return types;
    }

    public TreeMaker getMaker() {
        return maker;
    }

    public Parser createParser(String sources) {
        try {
            return (Parser) newParserMethod.invoke(parserFactory, sources, false, false, false);
        } catch (IllegalAccessException | InvocationTargetException ignored) {
        }
        return null;
    }
}
