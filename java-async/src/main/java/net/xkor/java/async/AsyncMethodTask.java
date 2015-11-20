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

public abstract class AsyncMethodTask<T> extends Task<T> {
    private int step = 0;
    Object awaitResult;
    Throwable awaitError;

    @Override
    public void doWork() {
        try {
            doStep();
        } catch (Throwable error) {
            if (error instanceof AsyncException) {
                fail(error.getCause());
            } else {
                fail(error);
            }
        }
    }

    protected abstract void doStep() throws Throwable;

    protected int getStep() {
        return step;
    }

    protected <ST> TaskCallback<ST> nextStep() {
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

    protected <ST> void setAwaitResult(ST result, Throwable error) {
        awaitResult = result;
        awaitError = error;
        doWork();
    }

    protected <ST> ST getStepResult() throws Throwable {
        if (awaitError != null) {
            throw awaitError;
        }
        return (ST) awaitResult;
    }

}
