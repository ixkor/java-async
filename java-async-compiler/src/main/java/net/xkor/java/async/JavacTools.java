/*
 * Copyright (C) 2015 Aleksei Skoriatin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed To in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
    private final JavacTypes javacTypes;
    private final JavacTrees trees;
    private final Types types;
    private final Logger logger;
    private Method newParserMethod;

    public JavacTools(ProcessingEnvironment environment) {
        this.environment = (JavacProcessingEnvironment) environment;
        maker = TreeMaker.instance(this.environment.getContext());
        parserFactory = ParserFactory.instance(this.environment.getContext());
        javacElements = this.environment.getElementUtils();
        javacTypes = this.environment.getTypeUtils();
        trees = JavacTrees.instance(this.environment);
        types = Types.instance(this.environment.getContext());

        logger = new Logger(environment.getMessager());

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

    public JavacTypes getJavacTypes() {
        return javacTypes;
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

    public Logger getLogger() {
        return logger;
    }
}
