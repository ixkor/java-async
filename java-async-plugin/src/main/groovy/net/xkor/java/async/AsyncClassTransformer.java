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

import net.xkor.java.async.annotations.AsyncMethodInternal;

import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.build.IClassTransformer;
import javassist.build.JavassistBuildException;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

public class AsyncClassTransformer implements IClassTransformer {
    @Override
    public void applyTransformations(CtClass ctClass) throws JavassistBuildException {
        for (CtMethod method : ctClass.getMethods()) {
            if (method.hasAnnotation(AsyncMethodInternal.class)) {
                handleMethod(method);
            }
        }
    }

    private void handleMethod(CtMethod method) throws JavassistBuildException {
        try {
            method.instrument(new ExprEditor() {
                @Override
                public void edit(MethodCall m) throws CannotCompileException {
                    if (JavaAsync.class.getCanonicalName().equals(m.getClassName()) && "fakeReturn".equals(m.getMethodName())) {
                        m.replace("{label" + m.getLineNumber() + ": $_ = $proceed($$);}");
                    }
                }
            });
        } catch (CannotCompileException e) {
            throw new JavassistBuildException(e);
        }

//        CodeIterator iterator = method.getMethodInfo().getCodeAttribute().iterator();
//        while (iterator.hasNext()) {
//            int index;
//            try {
//                index = iterator.next();
//            } catch (BadBytecode badBytecode) {
//                badBytecode.printStackTrace();
//                break;
//            }
//            int op = iterator.byteAt(index);
//            System.out.println(Mnemonic.OPCODE[op]);
//        }
    }

    @Override
    public boolean shouldTransform(CtClass ctClass) throws JavassistBuildException {
        for (CtMethod method : ctClass.getMethods()) {
            if (method.hasAnnotation(AsyncMethodInternal.class)) {
                return true;
            }
        }
        return false;
    }
}
