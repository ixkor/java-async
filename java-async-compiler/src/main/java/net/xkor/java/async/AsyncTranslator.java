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
import net.xkor.java.async.annotations.AsyncMethodInternal;

import java.util.Stack;


public class AsyncTranslator extends TreeTranslator {
    private final Name taskParamName;

    private final JavacTools tools;
    private final Logger logger;
    private final TreeMaker maker;
    private final Symbol.ClassSymbol taskClassSymbol;
    private final Symbol.ClassSymbol taskAsyncMethodClassSymbol;
    private final Symbol.ClassSymbol taskAsyncClassSymbol;
    private final Symbol.MethodSymbol doStepMethodSymbol;
    private final Symbol.MethodSymbol getStepResultMethodSymbol;
    private final Symbol.MethodSymbol getStepMethodSymbol;
    private final Symbol.MethodSymbol completeMethodSymbol;
    private final Symbol.MethodSymbol startNextStepMethodSymbol;

    private JCTree.JCClassDecl taskNewClass;

    private Type methodReturnType;
    private Symbol.MethodSymbol methodSymbol;
    private Stack<JCTree> treeStack = new Stack<>();
    private int awaitNum;

    public AsyncTranslator(JavacTools tools) {
        this.tools = tools;
        logger = tools.getLogger();
        maker = tools.getMaker();

        taskClassSymbol = tools.getJavacElements().getTypeElement(Task.class.getCanonicalName());

        taskAsyncMethodClassSymbol = tools.getJavacElements().getTypeElement(AsyncMethodTask.class.getCanonicalName());
        getStepResultMethodSymbol = tools.findMethodRecursive(taskAsyncMethodClassSymbol, "getStepResult");
        getStepMethodSymbol = tools.findMethodRecursive(taskAsyncMethodClassSymbol, "getStep");
        completeMethodSymbol = tools.findMethodRecursive(taskAsyncMethodClassSymbol, "complete");
        startNextStepMethodSymbol = tools.findMethodRecursive(taskAsyncMethodClassSymbol, "startNextStep");

        taskAsyncClassSymbol = tools.getJavacElements().getTypeElement(AsyncTask.class.getCanonicalName());
        doStepMethodSymbol = tools.findMethodRecursive(taskAsyncClassSymbol, "doStep");

        taskParamName = tools.getJavacElements().getName("task$");
    }

    @Override
    public void visitClassDef(JCTree.JCClassDecl jcClassDecl) {
        result = jcClassDecl;
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
        treeStack.clear();
        awaitNum = 0;

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
        maker.at(methodTree);

        translatedBody.stats = translatedBody.stats.prepend(maker.Switch(maker.Apply(
                null,
                maker.Select(maker.Ident(taskParamName), getStepMethodSymbol),
                List.<JCTree.JCExpression>nil()
        ), List.<JCTree.JCCase>nil()));

        JCTree.JCMethodDecl doStepMethod = tools.overrideMethod(taskNewClass, doStepMethodSymbol, taskParamName);
        JCTree.JCVariableDecl taskParam = doStepMethod.params.get(0);
        taskParam.vartype = maker.TypeApply(taskParam.vartype, List.of(tools.typeToTree(methodReturnType)));
        doStepMethod.body = translatedBody;
        doStepMethod.mods.annotations = doStepMethod.mods.annotations.append(
                tools.createAnnotation(AsyncMethodInternal.class));

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

        result = methodTree;
    }

    @Override
    public void visitReturn(JCTree.JCReturn jcReturn) {
        if (jcReturn.expr instanceof JCTree.JCMethodInvocation) {
            JCTree.JCMethodInvocation methodInvocation = (JCTree.JCMethodInvocation) jcReturn.expr;
            if (methodInvocation.meth instanceof JCTree.JCFieldAccess) {
                if (methodInvocation.meth.toString().equals("JavaAsync.asResult")) {
                    List<JCTree.JCExpression> translated = translate(methodInvocation.args);
                    maker.at(jcReturn);
                    result = maker.Block(0, List.of(
                            maker.Exec(maker.Apply(
                                    null,
                                    maker.Select(maker.Ident(taskParamName), completeMethodSymbol),
                                    translated
                            )),
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

    @Override
    public void visitApply(JCTree.JCMethodInvocation methodInvocation) {
        if (methodInvocation.meth instanceof JCTree.JCFieldAccess) {
            if (methodInvocation.meth.toString().equals("JavaAsync.await")) {
                // TODO update message
                logger.error(methodSymbol, "You can use method JavaAsync.await only in ...");
            }
        }
        super.visitApply(methodInvocation);
    }

    @Override
    public void visitExec(JCTree.JCExpressionStatement expressionStatement) {
        JCTree.JCAssign assign = null;
        JCTree.JCExpression expression = expressionStatement.expr;
        if (expression instanceof JCTree.JCAssign) {
            assign = (JCTree.JCAssign) expressionStatement.expr;
            expression = assign.rhs;
        }
        if (expression instanceof JCTree.JCMethodInvocation) {
            JCTree.JCMethodInvocation methodInvocation = (JCTree.JCMethodInvocation) expression;
            if (methodInvocation.meth instanceof JCTree.JCFieldAccess) {
                if (methodInvocation.meth.toString().equals("JavaAsync.await")) {
                    JCTree.JCExpression translated = translate(methodInvocation.args.head);
                    maker.at(expressionStatement);
                    expression = maker.Apply(
                            null,
                            maker.Select(maker.Ident(taskParamName), getStepResultMethodSymbol),
                            List.<JCTree.JCExpression>nil()
                    );
                    result = maker.Block(0, List.of(
                            maker.Exec(maker.Apply(
                                    null,
                                    maker.Select(maker.Ident(taskParamName), startNextStepMethodSymbol),
                                    List.of(translated)
                            )),
                            maker.Exec(maker.Apply(
                                    null,
                                    maker.Select(maker.Ident(tools.getJavacElements().getName("JavaAsync")), tools.getJavacElements().getName("fakeReturn")),
                                    List.<JCTree.JCExpression>nil()
                            )),
//                            maker.If(maker.Literal(true), maker.Return(null), null),
                            maker.Labelled(tools.getJavacElements().getName("$await" + awaitNum), expressionStatement)
                    ));
                    awaitNum++;
                    if (assign != null) {
                        assign.rhs = expression;
                    } else {
                        expressionStatement.expr = expression;
                    }
                    return;
                }
            }
        }
        super.visitExec(expressionStatement);
    }

    @Override
    public <T extends JCTree> T translate(T t) {
        treeStack.push(t);
        T translated = super.translate(t);
        treeStack.pop();
        return translated;
    }

    private JCTree getParentTree() {
        return treeStack.elementAt(treeStack.size() - 2);
    }

    private JCTree getParentTree(JCTree child) {
        int index = treeStack.lastIndexOf(child) - 1;
        if (index >= 0) {
            return treeStack.elementAt(index);
        }
        return null;
    }
}
