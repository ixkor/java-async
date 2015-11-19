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

public abstract class Task<T> {
    private TaskCallback<T> callback;

    private volatile int state;
    private static final int CREATED = 0;
    private static final int RUNNING = 1;
    private static final int COMPLETED = 2;
    private static final int FAULTED = 3;
    private static final int CANCELLED = 4;

    public abstract void start();

    public void start(TaskCallback<T> callback) {
        this.callback = callback;
        start();
    }
}
