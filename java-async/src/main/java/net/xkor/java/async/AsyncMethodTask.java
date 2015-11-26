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

public final class AsyncMethodTask<T> extends Task<T> {
    private int step = 0;
    private Object awaitResult;
    private Throwable awaitError;
    private AsyncTask<T> asyncTask;

    public AsyncMethodTask(AsyncTask<T> asyncTask){
        this.asyncTask = asyncTask;
    }

    @Override
    public void doWork() {
        try {
            asyncTask.doStep(this);
        } catch (Throwable error) {
            if (error instanceof AsyncException) {
                fail(error.getCause());
            } else {
                fail(error);
            }
        }
    }

    public int getStep() {
        return step;
    }

    public <ST> TaskCallback<ST> nextStep() {
        step++;
        return new TaskCallback<ST>() {
            @Override
            public void onComplete(ST result) {
                setAwaitResult(result, null);
            }

            @Override
            public void onFail(Throwable error) {
                setAwaitResult(null, error);
            }
        };
    }

    public <ST> void setAwaitResult(ST result, Throwable error) {
        awaitResult = result;
        awaitError = error;
        doWork();
    }

    public <ST> ST getStepResult() throws Throwable {
        if (awaitError != null) {
            throw awaitError;
        }
        return (ST) awaitResult;
    }

    @Override
    public void complete(T result) {
        super.complete(result);
    }
}
