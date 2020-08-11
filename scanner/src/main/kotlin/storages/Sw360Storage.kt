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

package org.ossreviewtoolkit.scanner.storages

import com.fasterxml.jackson.module.kotlin.readValue

import java.nio.file.Path

import org.eclipse.sw360.antenna.api.service.ServiceFactory
import org.eclipse.sw360.antenna.sw360.client.adapter.AttachmentUploadRequest
import org.eclipse.sw360.antenna.sw360.client.adapter.SW360ConnectionFactory
import org.eclipse.sw360.antenna.sw360.client.config.SW360ClientConfig
import org.eclipse.sw360.antenna.sw360.client.rest.resource.attachments.SW360AttachmentType
import org.eclipse.sw360.antenna.sw360.client.rest.resource.releases.SW360Release

import org.ossreviewtoolkit.model.Failure
import org.ossreviewtoolkit.model.Identifier
import org.ossreviewtoolkit.model.Result
import org.ossreviewtoolkit.model.ScanResult
import org.ossreviewtoolkit.model.ScanResultContainer
import org.ossreviewtoolkit.model.Success
import org.ossreviewtoolkit.model.config.Sw360StorageConfiguration
import org.ossreviewtoolkit.model.yamlMapper
import org.ossreviewtoolkit.scanner.ScanResultsStorage
import org.ossreviewtoolkit.utils.log

class Sw360Storage(
    sw360Configuration: Sw360StorageConfiguration
) : ScanResultsStorage() {
    private val sw360ConnectionConfig: SW360ClientConfig = SW360ClientConfig.createConfig(
        sw360Configuration.restUrl,
        sw360Configuration.authUrl,
        sw360Configuration.username,
        sw360Configuration.password,
        sw360Configuration.clientId,
        sw360Configuration.clientPassword,
        ServiceFactory().createHttpClient(
            sw360Configuration.proxyHost.isNullOrEmpty() && sw360Configuration.proxyPort!! > 0,
            sw360Configuration.proxyHost,
            sw360Configuration.proxyPort!!
        ),
        ServiceFactory.getObjectMapper()
    )
    private val sw360ConnectionFactory = SW360ConnectionFactory().newConnection(sw360ConnectionConfig)
    private val releaseClient = sw360ConnectionFactory.releaseAdapter

    override fun readFromStorage(id: Identifier): Result<ScanResultContainer> {
        val name = listOfNotNull(id.namespace, id.name).joinToString("/")

        releaseClient.getSparseReleaseByNameAndVersion(name, id.version).takeIf { it.isPresent }?.let {
            val sparseRelease = it.get()
            val fullRelease = releaseClient.getReleaseById(sparseRelease.releaseId)

            val scanResults = mutableListOf<ScanResult>()
            if (fullRelease.isPresent) {
                getScanResultOfRelease(fullRelease.get())
                    .forEach { path -> scanResults += yamlMapper.readValue<ScanResult>(path.toFile()) }
                }

            return Success(ScanResultContainer(id, scanResults))
        }

        val message = "Could not find a corresponding release for ${id.toCoordinates()} in SW360."
        log.info { message }
        return Failure(message)
    }

    override fun addToStorage(id: Identifier, scanResult: ScanResult): Result<Unit> {
        val cachedScanResult = tmpStoragePath(id)
        yamlMapper.writeValue(cachedScanResult, scanResult)

        val name = listOfNotNull(id.namespace, id.name).joinToString("/")
        releaseClient.getSparseReleaseByNameAndVersion(name, id.version).takeIf { it.isPresent }?.let {
            val sparseRelease = it.get()
            val fullRelease = releaseClient.getReleaseById(sparseRelease.releaseId)

            if (fullRelease.isPresent) {
                val attachmentUploadRequest =
                    createAttachmentOfScanResult(fullRelease.get(), cachedScanResult.toPath())
                releaseClient.uploadAttachments(attachmentUploadRequest)

                log.debug { "Stored scan result for '${id.toCoordinates()}' in SW360." }
                return Success(Unit)
            }
        }

        val message = "Could not find a corresponding release for ${id.toCoordinates()} in SW360."
        log.info { message }
        return Failure(message)
    }

    private fun getScanResultOfRelease(release: SW360Release): List<Path> =
        release.embedded.attachments
            .filter {
                it.attachmentType == SW360AttachmentType.CLEARING_REPORT && it.filename == SCAN_RESULTS_FILE_NAME
            }
            .map { releaseClient.downloadAttachment(release, it, createTempDir().toPath()) }
            .filter { it.isPresent }
            .map { it.get() }

    private fun createAttachmentOfScanResult(release: SW360Release,
                                             cachedScanResult: Path): AttachmentUploadRequest<SW360Release> =
        AttachmentUploadRequest.builder(release)
            .addAttachment(cachedScanResult, SW360AttachmentType.CLEARING_REPORT)
            .build()

    private fun tmpStoragePath(id: Identifier) = createTempDir(id.toCoordinates()).resolve(SCAN_RESULTS_FILE_NAME)
}
