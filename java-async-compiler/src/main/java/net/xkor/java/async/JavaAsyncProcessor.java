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

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Pair;
import net.xkor.java.async.annotations.Async;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class JavaAsyncProcessor extends AbstractProcessor {
    private JavacTools tools;
    private AsyncTranslator translator;

    @Override
    public void init(ProcessingEnvironment procEnv) {
        super.init(procEnv);
        tools = new JavacTools(procEnv);
        translator = new AsyncTranslator(tools);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (annotations == null || annotations.isEmpty()) {
            return false;
        }

        for (Element element : roundEnv.getElementsAnnotatedWith(Async.class)) {
            Pair<JCTree, JCTree.JCCompilationUnit> treeAndTopLevel = tools.getJavacElements().getTreeAndTopLevel(element, null, null);
            tools.setToplevel(treeAndTopLevel.snd);
            JCTree.JCMethodDecl methodTree = (JCTree.JCMethodDecl) treeAndTopLevel.fst;
            methodTree.accept(translator);
        }

        return true;
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return new HashSet<>(Arrays.asList(Async.class.getCanonicalName()));
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }
}
