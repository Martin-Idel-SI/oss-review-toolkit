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

package org.ossreviewtoolkit.model.config

/**
 * A class to hold the configuration for SW360.
 */
data class Sw360Configuration(
    /**
     * The REST API URL of SW360.
     */
    val restUrl: String,

    /**
     * The authentication URL of your SW360 instance.
     */
    val authUrl: String,

    /**
     * The username for the requests to SW360.
     */
    val username: String,

    /**
     * The password of the SW360 user.
     */
    val password: String,

    /**
     * The client ID of the SW360 instance for the two step authentication.
     */
    val clientId: String,

    /**
     * The password of the client ID.
     */
    val clientPassword: String,

    /**
     * The host of the proxy URL if one is in use.
     */
    val proxyHost: String? = null,

    /**
     * THe port of the proxy URL if one is in use.
     */
    val proxyPort: Int? = 0
)
