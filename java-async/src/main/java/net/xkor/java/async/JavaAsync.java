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

public final class JavaAsync {
    public static <T> T await(Task<T> task) {
        throw new UnsupportedOperationException("You can not use JavaAsync.await() outside of methods annotated with @Async");
    }

    public static <T> Task<T> asResult(T result) {
        throw new UnsupportedOperationException("You can not use JavaAsync.asResult() outside of methods annotated with @Async");
    }
}
