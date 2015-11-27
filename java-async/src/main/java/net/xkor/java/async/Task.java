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

    private final Object sync = new Object();
    private volatile int state = CREATED;
    private static final int CREATED = 0;
    private static final int RUNNING = 1;
    private static final int COMPLETED = 2;
    private static final int FAULTED = 3;
    private static final int CANCELLED = 4;

    private T result;
    private Throwable error;

    public void start() {
        if (state != CREATED) {
            return;
        }

        state = RUNNING;
        doWork();
    }

    protected abstract void doWork();

    protected void complete(T result) {
        synchronized (sync) {
            if (state != RUNNING) {
                return;
            }
            this.result = result;
            state = COMPLETED;
        }
        if (callback != null) {
            callback.onComplete(result);
        }
    }

    protected void fail(Throwable error) {
        synchronized (sync) {
            if (state != RUNNING) {
                return;
            }
            this.error = error;
            state = FAULTED;
        }
        if (callback != null) {
            callback.onFail(error);
        }
    }

    public void cancel() {
        synchronized (sync) {
            if (state != RUNNING) {
                return;
            }
            state = CANCELLED;
        }
        if (callback != null) {
            callback.onFail(new TaskCanceledException());
        }
    }

    public void start(TaskCallback<T> callback) {
        this.callback = callback;
        start();
    }

    public void setCallback(TaskCallback<T> callback) {
        this.callback = callback;
    }

    public TaskCallback<T> getCallback() {
        return callback;
    }

    public static Task<Void> sleep(final long milliseconds) {
        return new Task<Void>() {
            private Thread thread;

            @Override
            protected void doWork() {
                thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(milliseconds);
                            complete(null);
                        } catch (InterruptedException e) {
                            fail(e);
                        }
                    }
                });
                thread.start();
            }

            @Override
            public void cancel() {
                if (thread != null) {
                    thread.interrupt();
                    thread = null;
                }
                super.cancel();
            }
        };
    }
}
