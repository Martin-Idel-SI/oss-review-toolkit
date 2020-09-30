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

package org.ossreviewtoolkit.analyzer.curation

import java.io.File

import org.eclipse.sw360.antenna.api.service.ServiceFactory
import org.eclipse.sw360.antenna.sw360.client.adapter.SW360ConnectionFactory
import org.eclipse.sw360.antenna.sw360.client.config.SW360ClientConfig
import org.eclipse.sw360.antenna.sw360.client.rest.resource.attachments.SW360AttachmentType
import org.eclipse.sw360.antenna.sw360.client.rest.resource.releases.SW360SparseRelease

import org.ossreviewtoolkit.analyzer.PackageCurationProvider
import org.ossreviewtoolkit.model.Hash
import org.ossreviewtoolkit.model.HashAlgorithm
import org.ossreviewtoolkit.model.Identifier
import org.ossreviewtoolkit.model.PackageCuration
import org.ossreviewtoolkit.model.PackageCurationData
import org.ossreviewtoolkit.model.RemoteArtifact
import org.ossreviewtoolkit.model.config.Sw360Configuration

/**
 * A [PackageCurationProvider] for curated package meta-data from the configured SW360 instance using the REST API.
 */
class Sw360PackageCurationProvider(sw360Configuration: Sw360Configuration?) : PackageCurationProvider {

    init {
        requireNotNull(sw360Configuration) {
            "SW360 is not configured. To use curations from SW360 configure it in the ORT configuration file."
        }
    }

    private val sw360ConnectionConfig: SW360ClientConfig = SW360ClientConfig.createConfig(
        sw360Configuration?.restUrl,
        sw360Configuration?.authUrl,
        sw360Configuration?.username,
        sw360Configuration?.password,
        sw360Configuration?.clientId,
        sw360Configuration?.clientPassword,
        ServiceFactory().createHttpClient(
            sw360Configuration?.proxyHost.isNullOrEmpty() && sw360Configuration?.proxyPort!! > 0,
            sw360Configuration?.proxyHost,
            sw360Configuration?.proxyPort!!
        ),
        ServiceFactory.getObjectMapper()
    )
    private val sw360ConnectionFactory = SW360ConnectionFactory().newConnection(sw360ConnectionConfig)
    private val sw360ReleaseClient = sw360ConnectionFactory.releaseAdapter

    override fun getCurationsFor(pkgId: Identifier): List<PackageCuration> {
        val name = listOfNotNull(pkgId.namespace, pkgId.name).joinToString("/")

        return sw360ReleaseClient.getSparseReleaseByNameAndVersion(name, pkgId.version).takeIf { it.isPresent }?.let {
            val curation = it.get()
            val attachmentsDownloadPath = createTempDir()

            listOf(
                PackageCuration(
                    id = pkgId,
                    data = PackageCurationData(
                        declaredLicenses = curation.mainLicenseIds?.toSortedSet() ?: sortedSetOf(),
                        homepageUrl = "", // Not provided from SW360.
                        sourceArtifact = getSourceArtifactFromRelease(curation, attachmentsDownloadPath),
                        vcs = null,
                        comment = "Provided by SW360."
                    )
                )
            )
        } ?: emptyList()
    }

    private fun getSourceArtifactFromRelease(release: SW360SparseRelease, downloadPath: File): RemoteArtifact? {
        val releaseById = sw360ReleaseClient.getReleaseById(release.releaseId)
        if (releaseById.isPresent) {
            val fullRelease = releaseById.get()
            val attachments = fullRelease.embedded?.attachments.orEmpty()
            attachments
                .filter { it.attachmentType == SW360AttachmentType.SOURCE }
                .forEach { attachment ->
                    val downloadedAttachment =
                        sw360ReleaseClient.downloadAttachment(fullRelease, attachment, downloadPath.toPath())
                    if (downloadedAttachment.isPresent) {
                        return RemoteArtifact(
                            url = downloadedAttachment.toString(),
                            hash = Hash(
                                value = attachment.sha1,
                                algorithm = HashAlgorithm.SHA1
                            )
                        )
                }
            }
        }

        return RemoteArtifact.EMPTY
    }
}
