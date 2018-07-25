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

package net.daverix.slingerorm.compiler

fun Throwable.getStackTraceString(indent: String = ""): String {
    val sb = StringBuilder("${toString()}\n")

    if (stackTrace != null) {
        for (stackTraceElement in stackTrace) {
            sb.append("$indent\tat $stackTraceElement\n")
        }
    }

    // Print suppressed exceptions indented one level deeper.
    if (suppressed != null) {
        suppressed.map { it.getStackTraceString("$indent\t") }
                .forEach { sb.append("$indent\tSuppressed: $it") }
    }

    val causeTrace = cause?.getStackTraceString(indent)
    if(causeTrace != null) {
        sb.append("$indent\tCaused by: $causeTrace")
    }

    return sb.toString()
}
