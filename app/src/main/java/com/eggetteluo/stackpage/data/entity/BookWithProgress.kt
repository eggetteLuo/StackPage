package com.eggetteluo.stackpage.data.entity

import androidx.room.Embedded
import androidx.room.Relation

data class BookWithProgress(
    @Embedded val book: BookEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "bookId"
    )
    val progress: ProgressEntity?
)