package com.ahmedyejam.mks.data.importer.parser

import com.ahmedyejam.mks.data.importer.model.ImportFormat

/**
 * Android stub — the Android app still uses the legacy CompilerViewModel
 * in feature/ui for interactive spreadsheet editing. This stub satisfies
 * the Koin graph so CompilerViewModel in shared/commonMain can be resolved.
 */
class AndroidSpreadsheetDataProviderFactory : SpreadsheetDataProviderFactory {
    override fun open(filePath: String, format: ImportFormat): SpreadsheetOpenResult? = null
}
