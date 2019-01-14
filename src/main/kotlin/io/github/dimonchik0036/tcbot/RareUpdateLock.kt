package io.github.dimonchik0036.tcbot

import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.reflect.KProperty

class RareUpdateLock<T>(
    private var field: T,
    private val lock: ReentrantReadWriteLock = ReentrantReadWriteLock()
) {
    operator fun getValue(thisRef: Any?, prop: KProperty<*>): T = lock.read { field }
    operator fun setValue(thisRef: Any?, prop: KProperty<*>, value: T) = lock.write { field = value }
}