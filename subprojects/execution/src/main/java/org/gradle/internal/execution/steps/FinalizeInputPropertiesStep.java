/*
 * Copyright 2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.internal.execution.steps;

import org.gradle.cache.Cache;
import org.gradle.internal.Deferrable;
import org.gradle.internal.Try;
import org.gradle.internal.execution.UnitOfWork;

public class FinalizeInputPropertiesStep<C extends Context, R extends Result> implements DeferredExecutionAwareStep<C, R> {
    private final DeferredExecutionAwareStep<? super C, ? extends R> delegate;

    public FinalizeInputPropertiesStep(DeferredExecutionAwareStep<? super C, ? extends R> delegate) {
        this.delegate = delegate;
    }

    @Override
    public R execute(UnitOfWork work, C context) {
        prepare(work);
        return delegate.execute(work, context);
    }

    @Override
    public <T> Deferrable<Try<T>> executeDeferred(UnitOfWork work, C context, Cache<UnitOfWork.Identity, Try<T>> cache) {
        prepare(work);
        return delegate.executeDeferred(work, context, cache);
    }

    private static void prepare(UnitOfWork work) {
        Finalizer visitor = new Finalizer();
        work.visitIdentityInputs(visitor);
        work.visitRegularInputs(visitor);
    }

    private static class Finalizer implements UnitOfWork.InputVisitor {
        @Override
        public void visitInputProperty(String propertyName, UnitOfWork.ValueSupplier value) {
            value.finalizeValue();
        }

        @Override
        public void visitInputFileProperty(String propertyName, UnitOfWork.InputBehavior behavior, UnitOfWork.InputFileValueSupplier value) {
            value.finalizeValue();
        }
    }
}
