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

@file:Suppress("TooManyFunctions")

package org.ossreviewtoolkit.fossid

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import kotlin.reflect.KClass

import org.ossreviewtoolkit.model.jsonMapper

import retrofit2.Call

private const val SCAN_GROUP = "scans"
private const val PROJECT_GROUP = "projects"

/**
 * This function verifies that a request to FossID server was successful
 * @param operation the label of the operation, for logging
 * @param withDataCheck if true, the payload will be checked for existence
 */
fun EntityPostResponseBody<*>?.checkResponse(operation: String, withDataCheck: Boolean = true) {
    checkNotNull(this)
    require(error == null) {
        "Could not '$operation'. Additional information : $error"
    }
    if (withDataCheck) {
        requireNotNull(data) {
            "No Payload received for '$operation'. Additional information : $error"
        }
    }
}

/**
 * The list operations in FossID have inconsistent return types depending on the amount of entities returned.
 * This function streamlines these entities to a list.
 */
fun <T : Any> EntityPostResponseBody<Any>.toList(cls: KClass<T>): List<T> {
    // the list  operation returns different json depending on the amount of scans
    return when (val data = data) {
        is List<*> -> {
            data.map { jsonMapper.convertValue(it, cls.java) }
        }
        is Map<*, *> -> {
            data.values.map { jsonMapper.convertValue(it, cls.java) }
        }
        is Boolean -> {
            emptyList()
        }
        else -> {
            error("Cannot process the returned values")
        }
    }
}

/**
 * Get a FossID project
 * @param apiKey the key to query the API
 * @param user the user querying the API
 * @param projectCode the code of the project
 */
fun FossIdRestService.getProject(apiKey: String, user: String, projectCode: String) = getProject(
        PostRequestBody("get_information", PROJECT_GROUP, apiKey, user, "project_code" to projectCode)
)

/**
 * List the scans of a FossID project
 * @param apiKey the key to query the API
 * @param user the user querying the API
 * @param projectCode the code of the project
 */
fun FossIdRestService.listScansForProject(apiKey: String, user: String, projectCode: String) = listScansForProject(
        PostRequestBody("get_all_scans", PROJECT_GROUP, apiKey, user, "project_code" to projectCode)
)

/**
 * Create a new FossID project
 * @param apiKey the key to query the API
 * @param user the user querying the API
 * @param projectCode the code of the project
 * @param projectName the name of the project
 * @param comment the comment of the project
 */
fun FossIdRestService.createProject(
        apiKey: String,
        user: String,
        projectCode: String,
        projectName: String,
        comment: String = "Created by ORT"
) = createProject(
        PostRequestBody(
                "create",
                PROJECT_GROUP,
                apiKey,
                user,
                "project_code" to projectCode,
                "project_name" to projectName,
                "comment" to comment
        )
)

/**
 * Create a new scan for a FossID project
 * @param apiKey the key to query the API
 * @param user the user querying the API
 * @param projectCode the code of the project
 * @param gitRepoUrl the URL of the GIT repository
 * @param gitBranch the branch of the GIT repository
 */
fun FossIdRestService.createScan(
        apiKey: String,
        user: String,
        projectCode: String,
        gitRepoUrl: String,
        gitBranch: String
): Call<MapResponseBody<String>> {
    val formatter = DateTimeFormatter.ofPattern("'$projectCode'_yyyyMMdd_HHmmss")
    val scanCode = formatter.format(LocalDateTime.now())
    return createScan(
            PostRequestBody(
                    "create",
                    SCAN_GROUP,
                    apiKey,
                    user,
                    "project_code" to projectCode,
                    "scan_code" to scanCode,
                    "scan_name" to scanCode,
                    "git_repo_url" to gitRepoUrl,
                    "git_branch" to gitBranch
            )
    )
}

/**
 * Trigger a scan
 * @param apiKey the key to query the API
 * @param user the user querying the API
 * @param scanCode the code of the scan
 */
fun FossIdRestService.runScan(
        apiKey: String,
        user: String,
        scanCode: String
) = runScan(PostRequestBody(
        "run",
        SCAN_GROUP,
        apiKey,
        user,
        "scan_code" to scanCode,
        "auto_identification_detect_declaration" to "1",
        "auto_identification_detect_copyright" to "1"
))

/**
 * Trigger the download of the source code for a given scan
 * @param apiKey the key to query the API
 * @param user the user querying the API
 * @param scanCode the code of the scan
 */
fun FossIdRestService.downloadFromGit(
        apiKey: String,
        user: String,
        scanCode: String
) = downloadFromGit(PostRequestBody(
        "download_content_from_git",
        SCAN_GROUP,
        apiKey,
        user,
        "scan_code" to scanCode
))

/**
 * Get the status of a scan
 * @param apiKey the key to query the API
 * @param user the user querying the API
 * @param scanCode the code of the scan
 */
fun FossIdRestService.checkScanStatus(
        apiKey: String,
        user: String,
        scanCode: String
) = checkScanStatus(PostRequestBody("check_status", SCAN_GROUP, apiKey, user, "scan_code" to scanCode))

/**
 * Get the download status of the source code for a given scan
 * @param apiKey the key to query the API
 * @param user the user querying the API
 * @param scanCode the code of the scan
 */
fun FossIdRestService.checkDownloadStatus(
        apiKey: String,
        user: String,
        scanCode: String
) = checkDownloadStatus(PostRequestBody(
        "check_status_download_content_from_git", SCAN_GROUP, apiKey, user, "scan_code" to scanCode
))

/**
 * List the results of a scan
 * @param apiKey the key to query the API
 * @param user the user querying the API
 * @param scanCode the code of the scan
 */
fun FossIdRestService.listScanResults(
        apiKey: String,
        user: String,
        scanCode: String
) = listScanResults(PostRequestBody("get_results", SCAN_GROUP, apiKey, user, "scan_code" to scanCode))

/**
 * List the files of a scan that have been manually marked as identified
 * @param apiKey the key to query the API
 * @param user the user querying the API
 * @param scanCode the code of the scan
 */
fun FossIdRestService.listMarkedAsIdentifiedFiles(
        apiKey: String,
        user: String,
        scanCode: String
) = listMarkedAsIdentifiedFiles(PostRequestBody(
        "get_marked_as_identified_files",
        SCAN_GROUP,
        apiKey,
        user,
        "scan_code" to scanCode
))

/**
 * List the files of a scan that have been identified
 * @param apiKey the key to query the API
 * @param user the user querying the API
 * @param scanCode the code of the scan
 */
fun FossIdRestService.listIdentifiedFiles(
        apiKey: String,
        user: String,
        scanCode: String
) = listIdentifiedFiles(PostRequestBody(
        "get_identified_files", SCAN_GROUP, apiKey, user, "scan_code" to scanCode
))

/**
 * List the files of a scan that have been ignored
 * @param apiKey the key to query the API
 * @param user the user querying the API
 * @param scanCode the code of the scan
 */
fun FossIdRestService.listIgnoredFiles(
        apiKey: String,
        user: String,
        scanCode: String
) = listIgnoredFiles(PostRequestBody(
        "get_ignored_files", SCAN_GROUP, apiKey, user, "scan_code" to scanCode
))
