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

package net.xkor.java.async

import com.android.build.gradle.AppPlugin
import com.github.stephanenicolas.morpheus.AbstractMorpheusPlugin
import javassist.build.IClassTransformer
import org.gradle.api.Project

public class JavaAsyncPlugin extends AbstractMorpheusPlugin {

    @Override
    public void apply(Project project) {
        super.apply(project);

        def hasApp = project.plugins.withType(AppPlugin)
        final def variants
        if (hasApp) {
            variants = project.android.applicationVariants
        } else {
            variants = project.android.libraryVariants
        }

        variants.all { variant ->
            def transformerClassName = AsyncClassTransformer.class.getSimpleName()
            def transformTask = "transform${transformerClassName}${variant.name.capitalize()}"
            def copyTransformedTask = "copyTransformed${transformerClassName}${variant.name.capitalize()}"
            def bundleTask = "bundle${variant.name.capitalize()}"
            project.tasks.getByName(bundleTask).dependsOn(transformTask, copyTransformedTask)
        }
    }

    @Override
    protected Class getPluginExtension() {
        return null;
    }

    @Override
    protected String getExtension() {
        return null;
    }

    @Override
    public IClassTransformer[] getTransformers(Project project) {
        return [new AsyncClassTransformer()];
    }
}
