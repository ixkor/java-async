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
import com.sun.tools.javac.util.List;

import net.xkor.java.async.annotations.Async;

import java.util.HashMap;

import javax.lang.model.element.Name;

public class AsyncTranslator extends TreeTranslator {
    private final JavacTools tools;
    private final Logger logger;
    private final TreeMaker maker;
    private final Symbol.ClassSymbol taskClassSymbol;
    private final Symbol.ClassSymbol taskAsyncMethodClassSymbol;
    private final Symbol.MethodSymbol doStepMethodSymbol;

    private JCTree.JCClassDecl taskNewClass;
    private HashMap<Name, Name> namesMap = new HashMap<>();

    public AsyncTranslator(JavacTools tools) {
        this.tools = tools;
        logger = tools.getLogger();
        maker = tools.getMaker();
        taskClassSymbol = tools.getJavacElements().getTypeElement(Task.class.getCanonicalName());
        taskAsyncMethodClassSymbol = tools.getJavacElements().getTypeElement(AsyncMethodTask.class.getCanonicalName());
        doStepMethodSymbol = tools.findMethodRecursive(taskAsyncMethodClassSymbol, "doStep");
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
        if (returnType.asElement() != taskClassSymbol) {
            logger.error(methodTree.sym, "Method annotated with @%s must return a result of type %s",
                    Async.class.getSimpleName(), Task.class.getSimpleName());
            result = methodTree;
            return;
        }

        namesMap.clear();

        maker.at(methodTree);
        taskNewClass = maker.AnonymousClassDef(maker.Modifiers(0), List.<JCTree>nil());
//        taskNewClass.extending = tools.qualIdent(taskAsyncMethodClassSymbol);

        for (JCTree.JCVariableDecl param : methodTree.params) {
            JCTree.JCVariableDecl field = maker.VarDef(maker.Modifiers(Flags.PRIVATE), param.name, param.vartype, null);
            taskNewClass.defs = taskNewClass.defs.append(field);
        }

        JCTree.JCMethodDecl doStepMethod = tools.overrideMethod(taskNewClass, doStepMethodSymbol);
        doStepMethod.body.stats = methodTree.body.stats;

        methodTree.body.stats = List.<JCTree.JCStatement>of(maker.Return(maker.NewClass(
                null,
                List.<JCTree.JCExpression>nil(),
                tools.qualIdent(taskAsyncMethodClassSymbol),
                List.<JCTree.JCExpression>nil(),
                taskNewClass)));

        super.visitMethodDef(methodTree);
    }
}
