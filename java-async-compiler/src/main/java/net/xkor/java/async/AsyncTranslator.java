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

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.Name;
import net.xkor.java.async.annotations.Async;

public class AsyncTranslator extends TreeTranslator {
    private static final String CATCH_PARAM_NAME = "error";

    private final JavacTools tools;
    private final Logger logger;
    private final TreeMaker maker;
    private final Symbol.ClassSymbol taskSymbol;
    private final Symbol.ClassSymbol asyncExceptionSymbol;
    private final Symbol.ClassSymbol throwableSymbol;

    public AsyncTranslator(JavacTools tools) {
        this.tools = tools;
        logger = tools.getLogger();
        maker = tools.getMaker();
        taskSymbol = tools.getJavacElements().getTypeElement(Task.class.getCanonicalName());
        asyncExceptionSymbol = tools.getJavacElements().getTypeElement(AsyncException.class.getCanonicalName());
        throwableSymbol = tools.getJavacElements().getTypeElement(Throwable.class.getCanonicalName());

//        tools.createParser("")
        Name exceptionName = tools.getJavacElements().getName(CATCH_PARAM_NAME);
        maker.Catch(maker.VarDef(maker.Modifiers(0), exceptionName,
                tools.qualIdent(throwableSymbol), null), );
    }

    @Override
    public void visitClassDef(JCTree.JCClassDecl jcClassDecl) {
        result = jcClassDecl;
    }

    @Override
    public void visitBlock(JCTree.JCBlock jcBlock) {
        super.visitBlock(jcBlock);
    }

    @Override
    public void visitMethodDef(JCTree.JCMethodDecl methodTree) {
        Type returnType = methodTree.getReturnType().type;
        if (returnType.asElement() != taskSymbol) {
            logger.error(methodTree.sym, "Method annotated with @%s must return a result of type %s",
                    Async.class.getSimpleName(), Task.class.getSimpleName());
            result = methodTree;
            return;
        }

        super.visitMethodDef(methodTree);

        maker.at(methodTree);
        Name exceptionName = tools.getJavacElements().getName(CATCH_PARAM_NAME);
        maker.Catch(maker.VarDef(maker.Modifiers(0), exceptionName,
                tools.qualIdent(throwableSymbol), null), );
    }
}
