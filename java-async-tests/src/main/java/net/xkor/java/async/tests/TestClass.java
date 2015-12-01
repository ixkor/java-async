package net.xkor.java.async.tests;

import net.xkor.java.async.JavaAsync;
import net.xkor.java.async.Task;
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
