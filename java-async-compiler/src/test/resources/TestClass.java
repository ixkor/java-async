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

import net.xkor.java.async.JavaAsync;
import net.xkor.java.async.*;
import net.xkor.java.async.annotations.Async;

public class TestClass {

    @Async
    public Task<Integer> method(int prm) {
        int x = JavaAsync.await(new Task<Integer>() {
            @Override
            protected void doWork() {
            }
        });
        for (int i = 0; i < 4; i++) {
            JavaAsync.await(Task.sleep(1000));
        }
        return JavaAsync.asResult(x);
    }
}
