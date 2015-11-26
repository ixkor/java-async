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
import com.sun.tools.javac.util.Name;

import net.xkor.java.async.annotations.Async;


public class AsyncTranslator extends TreeTranslator {
    public static final String TASK_PARAM_NAME = "task$";
    private final JavacTools tools;
    private final Logger logger;
    private final TreeMaker maker;
    private final Symbol.ClassSymbol taskClassSymbol;
    private final Symbol.ClassSymbol taskAsyncMethodClassSymbol;
    private final Symbol.ClassSymbol taskAsyncClassSymbol;
    private final Symbol.MethodSymbol doStepMethodSymbol;
    private final Symbol.MethodSymbol setAwaitResultMethodSymbol;
    private final Symbol.MethodSymbol completeMethodSymbol;

    private JCTree.JCClassDecl taskNewClass;

    private Type methodReturnType;
    private Symbol.MethodSymbol methodSymbol;

    public AsyncTranslator(JavacTools tools) {
        this.tools = tools;
        logger = tools.getLogger();
        maker = tools.getMaker();

        taskClassSymbol = tools.getJavacElements().getTypeElement(Task.class.getCanonicalName());

        taskAsyncMethodClassSymbol = tools.getJavacElements().getTypeElement(AsyncMethodTask.class.getCanonicalName());
        setAwaitResultMethodSymbol = tools.findMethodRecursive(taskAsyncMethodClassSymbol, "setAwaitResult");
        completeMethodSymbol = tools.findMethodRecursive(taskAsyncMethodClassSymbol, "complete");

        taskAsyncClassSymbol = tools.getJavacElements().getTypeElement(AsyncTask.class.getCanonicalName());
        doStepMethodSymbol = tools.findMethodRecursive(taskAsyncClassSymbol, "doStep");
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
        methodSymbol = methodTree.sym;
        if (returnType.asElement() != taskClassSymbol) {
            logger.error(methodSymbol, "Method annotated with @%s must return a result of type %s",
                    Async.class.getSimpleName(), Task.class.getSimpleName());
            result = methodTree;
            return;
        }
        methodReturnType = returnType.getTypeArguments().get(0);

        maker.at(methodTree);
        taskNewClass = maker.AnonymousClassDef(maker.Modifiers(0), List.<JCTree>nil());

        for (JCTree.JCVariableDecl param : methodTree.params) {
            Name newName = tools.getJavacElements().getName(param.name + "$param");
            JCTree.JCVariableDecl field = maker.VarDef(
                    maker.Modifiers(Flags.PRIVATE), param.name, param.vartype, maker.Ident(newName));
            taskNewClass.defs = taskNewClass.defs.append(field);
            param.name = newName;
            param.mods.flags |= Flags.FINAL;
        }
        JCTree.JCBlock translatedBody = translate(methodTree.body);

        JCTree.JCMethodDecl doStepMethod = tools.overrideMethod(taskNewClass, doStepMethodSymbol, TASK_PARAM_NAME);
        JCTree.JCVariableDecl taskParam = doStepMethod.params.get(0);
        taskParam.vartype = maker.TypeApply(taskParam.vartype, List.of(tools.typeToTree(methodReturnType)));
        doStepMethod.body = translatedBody;

        methodTree.body = maker.Block(0, List.<JCTree.JCStatement>of(maker.Return(maker.NewClass(
                null,
                List.<JCTree.JCExpression>nil(),
                maker.TypeApply(tools.qualIdent(taskAsyncMethodClassSymbol), List.of(tools.typeToTree(methodReturnType))),
                List.<JCTree.JCExpression>of(maker.NewClass(
                        null,
                        List.<JCTree.JCExpression>nil(),
                        maker.TypeApply(tools.qualIdent(taskAsyncClassSymbol), List.of(tools.typeToTree(methodReturnType))),
                        List.<JCTree.JCExpression>nil(),
                        taskNewClass)),
                null))));

//        methodTree.body = translate(methodTree.body);
        result = methodTree;
    }

    @Override
    public void visitReturn(JCTree.JCReturn jcReturn) {
        if (jcReturn.expr instanceof JCTree.JCMethodInvocation) {
            JCTree.JCMethodInvocation methodInvocation = (JCTree.JCMethodInvocation) jcReturn.expr;
            if (methodInvocation.meth instanceof JCTree.JCFieldAccess) {
                if (methodInvocation.meth.toString().equals("JavaAsync.asResult")) {
                    result = maker.Block(0, List.of(
                            maker.Exec(maker.Apply(
                                    null,
                                    maker.Select(maker.Ident(tools.getJavacElements().getName(TASK_PARAM_NAME)), completeMethodSymbol),
                                    translate(methodInvocation.args))),
                            maker.Return(null)
                    ));
                    return;
                }
            }
        }

        logger.error(methodSymbol, "You must use method JavaAsync.asResult in return statement");
        result = jcReturn;
    }

    @Override
    public void visitVarDef(JCTree.JCVariableDecl jcVariableDecl) {
        JCTree.JCVariableDecl field = maker.VarDef(
                maker.Modifiers(Flags.PRIVATE), jcVariableDecl.name, jcVariableDecl.vartype, null);
        taskNewClass.defs = taskNewClass.defs.append(field);

        if (jcVariableDecl.init != null) {
            result = translate(maker.Exec(maker.Assign(maker.Ident(jcVariableDecl.name), jcVariableDecl.init)));
        } else {
            result = null;
        }
    }
}
