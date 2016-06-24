/**
 * Copyright 2016 Netflix, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */

package io.reactivex.internal.operators.observable;

import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.internal.disposables.DisposableHelper;

public final class ObserverResourceWrapper<T, R> extends AtomicReference<Object> implements Observer<T>, Disposable {
    /** */
    private static final long serialVersionUID = -8612022020200669122L;

    final Observer<? super T> actual;
    final Consumer<? super R> disposer;
    
    final AtomicReference<Disposable> subscription = new AtomicReference<Disposable>();
    
    private static final Object TERMINATED = new Object();
    
    public ObserverResourceWrapper(Observer<? super T> actual, Consumer<? super R> disposer) {
        this.actual = actual;
        this.disposer = disposer;
    }
    
    @Override
    public void onSubscribe(Disposable s) {
        if (DisposableHelper.setOnce(subscription, s)) {
            actual.onSubscribe(this);
        }
    }
    
    @Override
    public void onNext(T t) {
        actual.onNext(t);
    }
    
    @Override
    public void onError(Throwable t) {
        dispose();
        actual.onError(t);
    }
    
    @Override
    public void onComplete() {
        dispose();
        actual.onComplete();
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public void dispose() {
        DisposableHelper.dispose(subscription);

        Object o = get();
        if (o != TERMINATED) {
            o = getAndSet(TERMINATED);
            if (o != TERMINATED && o != null) {
                disposer.accept((R)o);
            }
        }
    }

    @Override
    public boolean isDisposed() {
        return subscription.get() == DisposableHelper.DISPOSED;
    }

    @SuppressWarnings("unchecked")
    public void setResource(R resource) {
        for (;;) {
            Object r = get();
            if (r == TERMINATED) {
                disposer.accept(resource);
                return;
            }
            if (compareAndSet(r, resource)) {
                if (r != null) {
                    disposer.accept((R)r);
                }
                return;
            }
        }
    }
}
