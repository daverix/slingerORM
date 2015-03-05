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
package net.daverix.slingerorm.android.model;

import net.daverix.slingerorm.annotation.DatabaseEntity;
import net.daverix.slingerorm.annotation.PrimaryKey;

@DatabaseEntity
public class SimpleEntity {
    @PrimaryKey
    public String id;
    public String message;
    public int Length; //should be upper case letter to test both upper and lower case

    public SimpleEntity(String id, String message, int length) {
        this.id = id;
        this.message = message;
        Length = length;
    }

    public SimpleEntity() {
    }
}
