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

import net.xkor.java.async.annotations.Async;

import javassist.CtClass;
import javassist.CtMethod;
import javassist.build.IClassTransformer;
import javassist.build.JavassistBuildException;

public class AsyncClassTransformer implements IClassTransformer {
    @Override
    public void applyTransformations(CtClass ctClass) throws JavassistBuildException {
        for (CtMethod method : ctClass.getMethods()) {
            if (method.hasAnnotation(Async.class)) {
            }
        }
    }

    @Override
    public boolean shouldTransform(CtClass ctClass) throws JavassistBuildException {
        for (CtMethod method : ctClass.getMethods()) {
            if (method.hasAnnotation(Async.class)) {
                return true;
            }
        }
        return false;
    }
}
