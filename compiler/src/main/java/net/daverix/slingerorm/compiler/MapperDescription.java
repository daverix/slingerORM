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

package net.daverix.slingerorm.compiler;

class MapperDescription {
    private String qualifiedName;
    private String simpleName;
    private boolean emptyConstructor;

    MapperDescription(String qualifiedName, String simpleName, boolean emptyConstructor) {
        this.qualifiedName = qualifiedName;
        this.simpleName = simpleName;
        this.emptyConstructor = emptyConstructor;
    }

    public String getQualifiedName() {
        return qualifiedName + "Mapper";
    }

    public String getEntityName() {
        return simpleName;
    }

    public boolean hasEmptyConstructor() {
        return emptyConstructor;
    }

    public String getVariableName() {
        String firstCharacterLowerCaseName = simpleName.substring(0, 1).toLowerCase() + simpleName.substring(1);
        return firstCharacterLowerCaseName + "Mapper";
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MapperDescription)) return false;

        MapperDescription that = (MapperDescription) o;

        return qualifiedName.equals(that.qualifiedName);

    }

    @Override
    public int hashCode() {
        return qualifiedName.hashCode();
    }
}
