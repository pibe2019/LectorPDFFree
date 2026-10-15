package com.antonio.mypdffree.domain.model.result

import com.antonio.mypdffree.domain.error.DomainError

typealias AppResult<T> = DomainResult<T, DomainError>
