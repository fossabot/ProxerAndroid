package com.proxerme.app.task.framework

import android.support.annotation.CallSuper

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
abstract class BaseListenableTask<O>(successCallback: ((O) -> Unit)? = null,
                                     exceptionCallback: ((Exception) -> Unit)? = null) :
        BaseTask<O>(successCallback, exceptionCallback), ListenableTask<O> {

    private var onStartCallback: (() -> Unit)? = null
    private var onSuccessCallback: (() -> Unit)? = null
    private var onExceptionCallback: (() -> Unit)? = null
    private var onFinishCallback: (() -> Unit)? = null

    override fun onStart(callback: () -> Unit): ListenableTask<O> {
        return this.apply { onStartCallback = callback }
    }

    override fun onSuccess(callback: () -> Unit): ListenableTask<O> {
        return this.apply { onSuccessCallback = callback }
    }

    override fun onException(callback: () -> Unit): ListenableTask<O> {
        return this.apply { onExceptionCallback = callback }
    }

    override fun onFinish(callback: () -> Unit): ListenableTask<O> {
        return this.apply { onFinishCallback = callback }
    }

    @CallSuper
    override fun destroy() {
        onStartCallback = null
        onSuccessCallback = null
        onExceptionCallback = null
        onFinishCallback = null

        super.destroy()
    }

    protected fun start(action: () -> Unit) {
        onStartCallback?.invoke()

        action.invoke()
    }

    protected fun finishSuccessful(result: O, successCallback: ((O) -> Unit)?) {
        successCallback?.invoke(result)

        onSuccessCallback?.invoke()
        onFinishCallback?.invoke()
    }

    protected fun finishWithException(result: Exception, exceptionCallback: ((Exception) -> Unit)?) {
        exceptionCallback?.invoke(result)

        onExceptionCallback?.invoke()
        onFinishCallback?.invoke()
    }
}