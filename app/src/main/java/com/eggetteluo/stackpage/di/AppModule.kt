package com.eggetteluo.stackpage.di

import com.eggetteluo.stackpage.data.entity.AppDatabase
import com.eggetteluo.stackpage.ui.library.LibraryViewModel
import com.eggetteluo.stackpage.ui.reader.ReaderViewModel
import com.eggetteluo.stackpage.util.DatabaseProvider
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { DatabaseProvider.getDatabase(androidContext()) }

    single { get<AppDatabase>().readingDao() }

    viewModel {
        LibraryViewModel(get())
    }

    viewModel { (bookId: Long) ->
        ReaderViewModel(dao = get(), bookId = bookId)
    }
}