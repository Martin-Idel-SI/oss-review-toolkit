/*
 * Copyright (C) 2020 Bosch.IO GmbH
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
 *
 * SPDX-License-Identifier: Apache-2.0
 * License-Filename: LICENSE
 */

package org.ossreviewtoolkit.fossid.api.identification.markedAsIdentified

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonUnwrapped

import org.ossreviewtoolkit.fossid.api.identification.common.Component
import org.ossreviewtoolkit.fossid.api.summary.License
import org.ossreviewtoolkit.fossid.api.summary.Summarizable
import org.ossreviewtoolkit.fossid.api.summary.SummaryIdentifiedFile

data class MarkedAsIdentifiedFile(
    val comment: String?,

    @JsonProperty("identification_id")
    val identificationId: Int,

    @JsonProperty("identification_copyright")
    val identificationCopyright: String,

    @JsonProperty("is_distributed")
    val isDistributed: Int,

    @JsonProperty("row_id")
    val rowId: Int
) : Summarizable {
    @JsonUnwrapped(prefix = "component_")
    lateinit var component: Component // TODO how to get the additional components ? Only one is returned

    @JsonUnwrapped(prefix = "file_")
    lateinit var file: File

    override fun toSummary(): SummaryIdentifiedFile {
        val licenses = file.licenses?.let { licenses ->
            licenses.values.map {
                License(
                    identifier = it.file.licenseIdentifier!!,
                    name = it.file.licenseName,
                    origin = it.type
                )
            }
        }.orEmpty()

        return SummaryIdentifiedFile(
            path = getFileName(),
            comment = comment,
            licences = licenses
        )
    }

    override fun getFileName(): String = file.path!!

    override fun getCopyright(): String? = identificationCopyright
}
