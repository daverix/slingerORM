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
package net.daverix.slingerorm.mapping;

public interface InsertableValues {
    public void put(String fieldName, byte[] bytes);
    public void put(String fieldName, String value);

    public void put(String fieldName, double value);
    public void put(String fieldName, float value);

    public void put(String fieldName, int value);
    public void put(String fieldName, boolean value);
    public void put(String fieldName, short value);
    public void put(String fieldName, long value);

    public Object getData();
}
