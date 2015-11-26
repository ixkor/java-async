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
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Kinds;
import com.sun.tools.javac.code.Scope;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.model.JavacTypes;
import com.sun.tools.javac.parser.Parser;
import com.sun.tools.javac.parser.ParserFactory;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Filter;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;
import com.sun.tools.javac.util.Pair;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.type.TypeKind;

public class JavacTools {
    private static final Filter<Symbol> EMPTY_FILTER = new Filter<Symbol>() {
        public boolean accepts(Symbol var1) {
            return true;
        }
    };

    private TreeMaker maker;
    private final ParserFactory parserFactory;
    private final JavacElements javacElements;
    private final JavacProcessingEnvironment environment;
    private final JavacTypes javacTypes;
    private final JavacTrees trees;
    private final Names names;
    private final Types types;
    private final Logger logger;
    private Method newParserMethod;
    private final Symbol.PackageSymbol javaLangPackage;

    public JavacTools(ProcessingEnvironment environment) {
        this.environment = (JavacProcessingEnvironment) environment;
        Context context = this.environment.getContext();
        maker = TreeMaker.instance(context);
        parserFactory = ParserFactory.instance(context);
        javacElements = this.environment.getElementUtils();
        javacTypes = this.environment.getTypeUtils();
        trees = JavacTrees.instance(this.environment);
        types = Types.instance(context);
        names = Names.instance(context);

        logger = new Logger(environment.getMessager());

        javaLangPackage = javacElements.getPackageElement("java.lang");

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

    public JCTree.JCExpression qualIdent(Symbol sym) {
        return isUnqualifiable(sym) ? maker.Ident(sym) : maker.Select(qualIdent(sym.owner), sym);
    }

    boolean isUnqualifiable(Symbol sym) {
        if (sym.name != this.names.empty && sym.owner != null && sym.owner.name != this.names.empty
                && sym.owner.kind != Kinds.MTH && sym.owner.kind != Kinds.VAR
                && sym.owner != javaLangPackage) {
            if (sym.kind == Kinds.TYP && maker.toplevel != null) {
                Scope.Entry entry = maker.toplevel.namedImportScope.lookup(sym.name);
                if (entry.scope != null) {
                    return entry.sym == sym && entry.next().scope == null;
                }

                entry = maker.toplevel.packge.members().lookup(sym.name);
                if (entry.scope != null) {
                    return entry.sym == sym && entry.next().scope == null;
                }

                entry = maker.toplevel.starImportScope.lookup(sym.name);
                if (entry.scope != null) {
                    return entry.sym == sym && entry.next().scope == null;
                }
            }

            return false;
        } else {
            return true;
        }
    }

    public void setToplevel(JCTree.JCCompilationUnit toplevel) {
        maker = maker.forToplevel(toplevel);
    }

    public JCTree.JCExpression typeToTree(Type type) {
        return typeToTree(type.asElement());
    }

    public JCTree.JCExpression typeToTree(Symbol.TypeSymbol typeSymbol) {
        if (typeSymbol.type.getKind().ordinal() <= TypeKind.VOID.ordinal()) {
            return maker.TypeIdent(typeSymbol.type.getTag());
        } else {
            return qualIdent(typeSymbol);
        }
//        return createParser(typeSymbol.getQualifiedName().toString()).parseType();
    }

    public JCTree.JCMethodDecl overrideMethod(JCTree.JCClassDecl classTree, Symbol.MethodSymbol methodSymbol, CharSequence... paramNames) {
        JCTree.JCExpression returnType = typeToTree(methodSymbol.getReturnType());
        List<JCTree.JCVariableDecl> params = List.nil();
        int paramNum = 0;
        for (Type paramType : methodSymbol.asType().getParameterTypes()) {
            CharSequence name;
            if (paramNum < paramNames.length) {
                name = paramNames[paramNum];
            } else {
                name = "param" + paramNum;
            }
            paramNum++;
            Name paramName = javacElements.getName(name);
            JCTree.JCExpression returnTypeName = typeToTree(paramType);
            params = params.append(maker.at(classTree).VarDef(
                    maker.Modifiers(Flags.PARAMETER), paramName, returnTypeName, null));
        }
        long modifiers = methodSymbol.flags() & (Flags.PUBLIC | Flags.PRIVATE | Flags.PROTECTED);
        JCTree.JCAnnotation overrideAnnotation = maker.Annotation(maker.Ident(javacElements.getName("Override")), List.<JCTree.JCExpression>nil());

        List<JCTree.JCExpression> methodThrows = List.nil();
        for (Type throwType : methodSymbol.asType().getThrownTypes()) {
            JCTree.JCExpression throwTypeName = typeToTree(throwType);
            methodThrows = methodThrows.append(throwTypeName);
        }

        JCTree.JCMethodDecl methodDecl = maker.MethodDef(
                maker.Modifiers(modifiers, List.of(overrideAnnotation)),
                methodSymbol.getSimpleName(),
                returnType,
                List.<JCTree.JCTypeParameter>nil(),
                params,
                methodThrows,
                maker.Block(0, List.<JCTree.JCStatement>nil()),
                null);

        classTree.defs = classTree.defs.append(methodDecl);

        return methodDecl;
    }

    public Symbol findClassMember(Symbol.ClassSymbol classSymbol, String name) {
        return findClassMember(classSymbol, name, EMPTY_FILTER);
    }

    public Symbol findClassMember(Symbol.ClassSymbol classSymbol, String name, Filter<Symbol> filter) {
        Symbol member = null;
        Iterator<Symbol> iterator = classSymbol.members().getElementsByName(javacElements.getName(name), filter).iterator();
        if (iterator.hasNext()) {
            member = iterator.next();
        }
        return member;
    }

    public Symbol.MethodSymbol findMethodRecursive(Symbol.ClassSymbol classSymbol, String name) {
        return findMethodRecursive(classSymbol, name, EMPTY_FILTER);
    }

    public Symbol.MethodSymbol findMethodRecursive(Symbol.ClassSymbol classSymbol, String name, Filter<Symbol> filter) {
        Symbol.MethodSymbol methodSymbol;
        do {
            methodSymbol = (Symbol.MethodSymbol) findClassMember(classSymbol, name, filter);
            classSymbol = (Symbol.ClassSymbol) classSymbol.getSuperclass().asElement();
        } while (classSymbol != null && methodSymbol == null);
        return methodSymbol;
    }
}
