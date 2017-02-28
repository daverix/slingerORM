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

class PackageProvider {
    String getPackage(String qualifiedName) {
        if(qualifiedName == null) throw new IllegalArgumentException("qualifiedName is null");

        int lastDot = qualifiedName.lastIndexOf(".");
        return qualifiedName.substring(0, lastDot);
    }
}
