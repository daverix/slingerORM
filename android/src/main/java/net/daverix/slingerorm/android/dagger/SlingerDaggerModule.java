/*
 * Copyright 2014 David Laurell
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.daverix.slingerorm.android.dagger;

import net.daverix.slingerorm.StorageFactory;
import net.daverix.slingerorm.android.internal.CursorResultsFactory;
import net.daverix.slingerorm.android.internal.CursorRowResultFactory;
import net.daverix.slingerorm.android.internal.ResultRowFactory;
import net.daverix.slingerorm.android.internal.ResultRowsFactory;
import net.daverix.slingerorm.GeneratedStorageFactory;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module(library = true)
public class SlingerDaggerModule {
    @Singleton @Provides
    public ResultRowFactory provideResultRowFactory(CursorRowResultFactory factory) {
        return factory;
    }

    @Singleton @Provides
    public ResultRowsFactory provideResultRowCollectionFactory(CursorResultsFactory cursorRowResultFactory) {
        return cursorRowResultFactory;
    }

    @Singleton @Provides
    public StorageFactory provideStorageFactory(GeneratedStorageFactory factory) {
        return factory;
    }
}
