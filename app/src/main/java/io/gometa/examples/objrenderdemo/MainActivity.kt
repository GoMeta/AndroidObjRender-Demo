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

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.graphics.Color
import android.graphics.PointF
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.view.MotionEvent
import android.view.View
import de.javagl.obj.Rect3D
import io.gometa.support.obj.LightingParameters
import io.gometa.support.obj.ObjRenderer
import io.gometa.support.obj.Vec4
import io.gometa.support.obj.VirtualObject
import kotlinx.android.synthetic.main.activity_main.*
import timber.log.Timber
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class MainActivity : AppCompatActivity(), GLSurfaceView.Renderer {

    companion object {
        private const val NEAR_PLANE = 1f
        private const val DEFAULT_FAR_PLANE = 7f
    }

    private val viewModel by lazy {
        ViewModelProviders.of(this)[MainActivityViewModel::class.java]
    }

    private val objSelectorAdapter = ObjSelectorAdapter()
    private var projectionRatio: Float = 1f
    private val anchorMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)

    private val lightingParameters = LightingParameters(1f,
        Vec4(0.250f, 0.866f, 0.433f, 0.0f))

    private var tumbStonedRenderers: ObjRenderer? = null
    private var renderer: ObjRenderer? = null
    private var rect3D: Rect3D? = null

    private var lastTouchPoint: PointF? = null
    private val rotationMatrix = FloatArray(16)
    private val cameraPosition = FloatArray(3)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        obj_selector.apply {
            layoutManager = LinearLayoutManager(this@MainActivity,
                LinearLayoutManager.HORIZONTAL, false)
            adapter = objSelectorAdapter
        }
        gl_surface.apply {
            preserveEGLContextOnPause = true
            setEGLContextClientVersion(2)
            setEGLConfigChooser(8, 8, 8, 8, 16, 0)
            setRenderer(this@MainActivity)
            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
            setOnTouchListener { _, motionEvent -> rotateOnTouch(motionEvent) }
        }
        Matrix.setIdentityM(anchorMatrix, 0)
        objSelectorAdapter.setAssetDirectories(listOf("astronaut", "campfire", "dumptruck",
            "flower", "littleman", "minotaur", "panda", "skeleton", "soundwave", "xwing"))
        objSelectorAdapter.onSelectedAssetChanged = { assetDirectory ->
            viewModel.loadModel(assetDirectory)
        }
        viewModel.renderer.observe(this, Observer {
            Matrix.setIdentityM(anchorMatrix, 0)
            progress.visibility = if (it?.isLoading == true) View.VISIBLE else View.GONE
            tumbStonedRenderers = renderer
            renderer = it?.renderer
            rect3D = it?.renderer?.bounds
            rect3D?.let {
                updateCamera(0f, .1f, -.8f)
                Matrix.translateM(anchorMatrix, 0, -it.centerX, -it.centerY, -it.centerZ)
            }
            updateProjectionMatrix()
            Timber.d("rect3D = $rect3D")
        })
        Matrix.setIdentityM(rotationMatrix, 0)
    }

    override fun onResume() {
        super.onResume()
        gl_surface.onResume()
    }

    override fun onPause() {
        gl_surface.onPause()
        super.onPause()
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // GLSurfaceView.Renderer

    override fun onDrawFrame(p0: GL10?) {
        tumbStonedRenderers?.destroy()
        tumbStonedRenderers = null

        GLES20.glClearColor(Color.red(objSelectorAdapter.selectedItemBackgroundColor) / 255f,
            Color.green(objSelectorAdapter.selectedItemBackgroundColor) / 255f,
            Color.blue(objSelectorAdapter.selectedItemBackgroundColor) / 255f,
            1f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        val renderer = renderer ?: return

        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glDepthFunc(GLES20.GL_LEQUAL)
        GLES20.glDepthMask( true)
        renderer.apply {
            updateModelMatrix(anchorMatrix, 1f)
            draw(viewMatrix, projectionMatrix, lightingParameters)
        }
    }

    override fun onSurfaceChanged(p0: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        projectionRatio = width.toFloat() / height
        updateProjectionMatrix()
    }

    override fun onSurfaceCreated(p0: GL10?, p1: EGLConfig?) {
        GLES20.glClearColor(1f, 1f, 1f, 1f)
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Private helpers

    private fun updateCamera(x: Float, y: Float, z: Float) {
        val objWidth = renderer?.bounds?.diagonalLength ?: DEFAULT_FAR_PLANE
        cameraPosition[0] = x * objWidth
        cameraPosition[1] = y * objWidth
        cameraPosition[2] = z * objWidth
        Matrix.setLookAtM(viewMatrix, 0,
            cameraPosition[0], cameraPosition[1], cameraPosition[2],
            0f, 0f, 0f,
            0f, 1f, 0f)
        lightingParameters.lightDirection.x = cameraPosition[0]
        lightingParameters.lightDirection.y = cameraPosition[1]
        lightingParameters.lightDirection.z = cameraPosition[2]
        VirtualObject.normalizeVec3(lightingParameters.lightDirection.array)
    }

    private fun rotateOnTouch(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchPoint = PointF(event.x, event.y)
            }
            MotionEvent.ACTION_MOVE -> {
                lastTouchPoint?.let {
                    val dx = (event.x - it.x) * Math.PI * 2 / gl_surface.width
                    val dy = (event.y - it.y) * Math.PI * 2 / gl_surface.height
                    updateCamera(Math.sin(dx).toFloat(), Math.sin(dy).toFloat(), Math.cos(dx).toFloat())
                }
            }
            MotionEvent.ACTION_UP -> {
                lastTouchPoint = null
            }
            else -> return false
        }
        return true
    }

    private fun updateProjectionMatrix() {
        val farPlane = rect3D?.diagonalLength?.let {
            NEAR_PLANE + it * 2f
        } ?: DEFAULT_FAR_PLANE
        Matrix.frustumM(projectionMatrix, 0, -projectionRatio, projectionRatio,
            -1f, 1f,
            NEAR_PLANE, farPlane)
    }
}
