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
package net.daverix.slingerorm;

import net.daverix.slingerorm.annotation.DatabaseEntity;
import net.daverix.slingerorm.exception.FetchMappingException;
import net.daverix.slingerorm.mapping.Mapping;

public interface MappingFetcher {
    /**
     * Does a lazy call to get the mapping for a specific entity
     *
     * @param entityClass the entity class
     * @param <T> a type annotated with {@link DatabaseEntity}
     * @return mapping for entity
     * @throws FetchMappingException if mapping can't fetched
     */
    <T> Mapping<T> fetchMapping(Class<T> entityClass) throws FetchMappingException;

    void registerTypeSerializer(Object typeSerializer);
}
