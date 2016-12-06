package com.proxerme.app.task

import com.proxerme.app.task.CachedTask.CacheStrategy
import com.proxerme.app.task.framework.BaseTask
import com.proxerme.app.task.framework.Task

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class CachedTask<O>(private val task: Task<O>,
                    cacheStrategy: CacheStrategy = CacheStrategy.FULL,
                    successCallback: ((O) -> Unit)? = null,
                    exceptionCallback: ((Exception) -> Unit)? = null) :
        BaseTask<O>(successCallback, exceptionCallback) {

    override val isWorking: Boolean
        get() = task.isWorking

    private val shouldCachedResult = cacheStrategy == CacheStrategy.FULL ||
            cacheStrategy == CacheStrategy.RESULT
    private val shouldCacheException = cacheStrategy == CacheStrategy.FULL ||
            cacheStrategy == CacheStrategy.EXCEPTION

    private var cachedResult: O? = null
    private var cachedException: Exception? = null

    override fun execute() {
        if (shouldCachedResult) {
            cachedResult?.let {
                successCallback?.invoke(it)

                return
            }

            if (isWorking) {
                return
            }
        }

        if (shouldCacheException) {
            cachedException?.let {
                exceptionCallback?.invoke(it)

                return
            }

            if (isWorking) {
                return
            }
        }

        delegatedExecute(task, {
            cachedResult = if (shouldCachedResult) it else null

            successCallback?.invoke(it)
        }, {
            cachedException = if (shouldCacheException) it else null

            exceptionCallback?.invoke(it)
        })
    }

    override fun cancel() {
        task.cancel()
    }

    override fun reset() {
        cachedResult = null
        cachedException = null

        task.reset()
    }

    override fun destroy() {
        cachedResult = null
        cachedException = null

        task.destroy()
        super.destroy()
    }

    enum class CacheStrategy {FULL, RESULT, EXCEPTION }
}