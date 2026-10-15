package com.antonio.mypdffree.data.mapper

import com.antonio.mypdffree.data.local.room.entities.RecentDocumentEntity
import com.antonio.mypdffree.domain.model.RecentDocument

fun RecentDocumentEntity.toDomain(): RecentDocument = RecentDocument(
    uri = uri,
    displayName = displayName,
    sizeBytes = sizeBytes,
    pageCount = pageCount,
    lastPageIndex = lastPageIndex,
    lastOpenedAtMillis = lastOpenedAt,
)

fun RecentDocument.toEntity(): RecentDocumentEntity = RecentDocumentEntity(
    uri = uri,
    displayName = displayName,
    sizeBytes = sizeBytes,
    pageCount = pageCount,
    lastPageIndex = lastPageIndex,
    lastOpenedAt = lastOpenedAtMillis,
)
