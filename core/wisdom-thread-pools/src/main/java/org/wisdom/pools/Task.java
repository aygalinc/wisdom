package org.wisdom.pools;

import com.google.common.util.concurrent.*;
import org.jetbrains.annotations.NotNull;
import org.wisdom.api.concurrent.ExecutionContext;
import org.wisdom.api.concurrent.ManagedFutureTask;

import java.util.concurrent.*;

/**
 * Implementation of {@link org.wisdom.api.concurrent.ManagedFutureTask} to be
 * used with {@link org.wisdom.pools.ManagedExecutorServiceImpl}.
 */
public class Task<V> extends FutureTask<V> implements ListenableFuture<V>, ManagedFutureTask<V> {

    private final ListeningExecutorService executor;
    private final ExecutionContext executionContext;
    private final Callable<V> callable;
    private ListenableFuture<V> future;
    private Throwable taskRunThrowable;

    private long submissionDate;
    private long startDate;
    private long completionDate;
    private long hungTime;

    protected Task(
            ListeningExecutorService executor,
            Runnable runnable,
            V result,
            ExecutionContext executionContext, long hungTime) {
        super(runnable, result);
        this.callable = new EnhancedCallable(Executors.callable(runnable, result));
        this.executor = executor;
        this.executionContext = executionContext;
        this.hungTime = hungTime;
    }

    public Task(
            ListeningExecutorService executor,
            Callable<V> callable,
            ExecutionContext executionContext, long hungTime) {
        super(callable);
        this.callable = new EnhancedCallable(callable);
        this.executor = executor;
        this.executionContext = executionContext;
        this.hungTime = hungTime;
    }

    protected Task<V> execute() {
        ListenableFuture<V> future = executor.submit(callable);
        submitted(future);
        return this;
    }

    protected Task<V> submitted(Future<V> future) {
        this.submissionDate = System.currentTimeMillis();
        this.future = JdkFutureAdapters.listenInPoolThread(future);
        return this;
    }

    public void addListener(Runnable listener) {
        addListener(listener, executor);
    }

    @Override
    public void addListener(@NotNull Runnable listener, @NotNull Executor exec) {
        this.future.addListener(listener, exec);
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return this.future != null && this.future.cancel(mayInterruptIfRunning);
    }

    @Override
    public V get() throws InterruptedException, ExecutionException {
        return this.future.get();
    }

    @Override
    public V get(long timeout, @NotNull TimeUnit unit) throws InterruptedException, TimeoutException, ExecutionException {
        return this.future.get(timeout, unit);
    }

    @Override
    public boolean isCancelled() {
        return this.future.isCancelled();
    }

    @Override
    public boolean isDone() {
        return this.future.isDone();
    }

    public Throwable cause() {
        return taskRunThrowable;
    }

    @Override
    protected void setException(Throwable throwable) {
        taskRunThrowable = throwable;
    }

    @Override
    public Task onSuccess(final SuccessCallback<V> callback, Executor executor) {
        Futures.addCallback(future, new FutureCallback<V>() {
            @Override
            public void onSuccess(V v) {
                callback.onSuccess(Task.this, v);
            }

            @Override
            public void onFailure(@NotNull Throwable throwable) {
                // Do nothing.
            }
        }, executor);
        return this;
    }

    @Override
    public Task onSuccess(final SuccessCallback<V> callback) {
        return onSuccess(callback, executor);
    }

    @Override
    public Task onFailure(final FailureCallback callback, Executor executor) {
        Futures.addCallback(future, new FutureCallback<V>() {
            @Override
            public void onSuccess(V v) {
                // Do nothing
            }

            @Override
            public void onFailure(@NotNull Throwable throwable) {
                callback.onFailure(Task.this, throwable);
            }
        }, executor);
        return this;
    }

    @Override
    public Task onFailure(final FailureCallback callback) {
        return onFailure(callback, executor);
    }

    @Override
    public boolean isTaskHang() {
        // The task was completed.
        return completionDate == 0 && System.currentTimeMillis() - submissionDate >= hungTime;
    }

    @Override
    public boolean isShutdown() {
        return executor.isShutdown();
    }

    @Override
    public long getTaskStartTime() {
        return startDate;
    }

    @Override
    public long getTaskCompletionTime() {
        return completionDate;
    }

    @Override
    public long getTaskRunTime() {
        if (startDate == 0) {
            return 0;
        }
        if (completionDate == 0) {
            return System.currentTimeMillis() - startDate;
        }
        return completionDate - startDate;
    }


    @Override
    public long getHungTaskThreshold() {
        return hungTime;
    }

    @Override
    public ExecutionContext getExecutionContext() {
        return executionContext;
    }

    private class EnhancedCallable implements Callable<V> {

        private final Callable<V> delegate;

        private EnhancedCallable(Callable<V> delegate) {
            this.delegate = delegate;
        }

        @Override
        public V call() throws Exception {
            try {
                if (executionContext != null) {
                    executionContext.apply();
                }
                startDate = System.currentTimeMillis();
                return delegate.call();
            } catch (Throwable e) {
                setException(e);
                throw e;
            } finally {
                completionDate = System.currentTimeMillis();
                if (executionContext != null) {
                    executionContext.unapply();
                }
            }
        }
    }

}
