/*
 * Copyright 2015 David Laurell
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

package net.daverix.slingerorm.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.TYPE})
public @interface DatabaseStorage {

    /**
     * <p>Annotated on interfaces of storage builders. A storage builder is required in order to
     * pass the SQLiteDatabase instance to storage class.</p>
     * <p>Current types are supported in the builder:<br/>
     * <ul>
     *     <li>SQLiteDatabase</li>
     *     <li>SQLiteOpenHelper</li>
     *     <li>Types that has non-static {@code @SerializeType} or {@code @DeserializeType} methods.</li>
     * </ul>
     * </p>
     *
     * <p>Example of how the builder would look if you would provide the SQLiteDatabase type and a
     * type that serializes date types:</p>
     * <pre>
     *{@code @Storage(serializers} = { DateSerializer.class })
     * public interface MyStorage {
     *    {@code @Insert}
     *     void insert(MyObject obj);
     *
     *    {@code @Select}
     *     MyObject getObject(String id);
     *
     *    {@code @Storage.Builder}
     *     interface Builder {
     *         Builder database(SQLiteDatabase db);
     *         Builder customSerializer(DateSerializer serializer);
     *         MyStorage build();
     *     }
     * }
     * </pre>
     */
    @Retention(RetentionPolicy.SOURCE)
    @Target({ElementType.TYPE})
    @interface Builder {

    }
}
