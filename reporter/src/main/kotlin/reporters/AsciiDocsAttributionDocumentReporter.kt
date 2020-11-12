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

package org.ossreviewtoolkit.reporter.reporters

import java.io.File

import org.asciidoctor.Asciidoctor.Factory.create
import org.asciidoctor.AttributesBuilder
import org.asciidoctor.OptionsBuilder
import org.asciidoctor.SafeMode
import org.ossreviewtoolkit.reporter.Reporter
import org.ossreviewtoolkit.reporter.ReporterInput
import org.ossreviewtoolkit.reporter.utils.FreemarkerTemplateGenerator

class AsciiDocsAttributionDocumentReporter : Reporter {
    companion object {
        private const val ASCIIDOCS_FILE_PREFIX = "Asciidocs_"
        private const val ASCIIDOCS_DEFAULT_TEMPLATE = "asciidoc"

        private const val OPTION_PDF_THEME_PATH = "pdf-theme.path"
    }

    val templateGenerator = FreemarkerTemplateGenerator(ASCIIDOCS_FILE_PREFIX, ASCIIDOCS_DEFAULT_TEMPLATE)

    override val reporterName = "Asciidocs"

    override fun generateReport(input: ReporterInput, outputDir: File, options: Map<String, String>): List<File> {
        val asciidocsInput = templateGenerator.generateTemplate(input, outputDir, options)

        val asciidoctor = create()
        val outputFiles = mutableListOf<File>()

        val asciidoctorAttributes = AttributesBuilder.attributes()
        val themePath = options[OPTION_PDF_THEME_PATH]

        if (themePath != null) {
            File(themePath).also {
                require(it.exists()) { "Could not find theme file at ${it.absolutePath}." }
            }
            asciidoctorAttributes.attribute("pdf-theme", themePath)
        }

        asciidocsInput.forEach { file ->
            val outputFile = outputDir.resolve("${file.nameWithoutExtension}.pdf")
            outputFiles += outputFile

            val asciidoctorOptions = OptionsBuilder.options()
                .backend("pdf")
                .toFile(outputFile)
                .attributes(asciidoctorAttributes)
                .safe(SafeMode.UNSAFE)

            asciidoctor.convertFile(file, asciidoctorOptions)
        }

        return outputFiles
    }
}
