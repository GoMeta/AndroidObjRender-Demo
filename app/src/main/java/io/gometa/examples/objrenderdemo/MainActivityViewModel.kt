/*
 * Copyright (c) 2018 GoMeta Inc. All Rights Reserver
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.gometa.examples.objrenderdemo

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.os.Handler
import android.os.HandlerThread
import io.gometa.support.obj.ObjExtractor
import io.gometa.support.obj.ObjRenderer
import java.io.File

/**
 *
 */
class MainActivityViewModel(
    application: Application
) : AndroidViewModel(application), ObjExtractor.OnExtractionListener {

    companion object {
        private val diskIoHandler = HandlerThread("DiskIO").let {
            it.start()
            Handler(it.looper)
        }
    }

    data class State(
        val renderer: ObjRenderer? = null,
        val isLoading: Boolean = false)

    private val _renderer = MutableLiveData<State>()
    val renderer: LiveData<State>
        get() = _renderer

    fun loadModel(assetDirectory: String?) {
        _renderer.value = _renderer.value?.copy(isLoading = true)
        assetDirectory?.let {
            val inputStream = getApplication<Application>().assets
                .open("$assetDirectory${File.separator}model.zip")
            ObjExtractor(getApplication(), assetDirectory, inputStream, this, diskIoHandler)
        } ?: run {
            _renderer.value = State(null, false)
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // ObjExtractor.OnExtractionListener

    override fun onExtractionFinished(renderer: ObjRenderer?) {
        _renderer.value = State(renderer, false)
    }

    init {
        _renderer.value = State()
    }
}