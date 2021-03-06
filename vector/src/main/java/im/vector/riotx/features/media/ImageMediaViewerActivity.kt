/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.riotx.features.media

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.Toolbar
import androidx.core.transition.addListener
import androidx.core.view.ViewCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.transition.Transition
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.github.piasy.biv.indicator.progresspie.ProgressPieIndicator
import com.github.piasy.biv.view.GlideImageViewFactory
import im.vector.riotx.core.di.ScreenComponent
import im.vector.riotx.core.glide.GlideApp
import im.vector.riotx.core.platform.VectorBaseActivity
import kotlinx.android.synthetic.main.activity_image_media_viewer.*
import timber.log.Timber
import javax.inject.Inject

class ImageMediaViewerActivity : VectorBaseActivity() {

    @Inject lateinit var imageContentRenderer: ImageContentRenderer

    private lateinit var mediaData: ImageContentRenderer.Data

    override fun injectWith(injector: ScreenComponent) {
        injector.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(im.vector.riotx.R.layout.activity_image_media_viewer)

        if (intent.hasExtra(EXTRA_MEDIA_DATA)) {
            mediaData = intent.getParcelableExtra(EXTRA_MEDIA_DATA)!!
        } else {
            finish()
        }

        intent.extras?.getString(EXTRA_SHARED_TRANSITION_NAME)?.let {
            ViewCompat.setTransitionName(imageTransitionView, it)
        }

        if (mediaData.url.isNullOrEmpty()) {
            supportFinishAfterTransition()
            return
        }

        configureToolbar(imageMediaViewerToolbar, mediaData)

        if (isFirstCreation() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && addTransitionListener()) {
            // Encrypted image
            imageTransitionView.isVisible = true
            imageMediaViewerImageView.isVisible = false
            encryptedImageView.isVisible = false
            // Postpone transaction a bit until thumbnail is loaded
            supportPostponeEnterTransition()
            imageContentRenderer.renderFitTarget(mediaData, ImageContentRenderer.Mode.THUMBNAIL, imageTransitionView) {
                // Proceed with transaction
                scheduleStartPostponedTransition(imageTransitionView)
            }
        } else {
            imageTransitionView.isVisible = false

            if (mediaData.elementToDecrypt != null) {
                // Encrypted image
                imageMediaViewerImageView.isVisible = false
                encryptedImageView.isVisible = true

                GlideApp
                        .with(this)
                        .load(mediaData)
                        .dontAnimate()
                        .into(encryptedImageView)
            } else {
                // Clear image
                imageMediaViewerImageView.isVisible = true
                encryptedImageView.isVisible = false

                imageMediaViewerImageView.setImageViewFactory(GlideImageViewFactory())
                imageMediaViewerImageView.setProgressIndicator(ProgressPieIndicator())
                imageContentRenderer.render(mediaData, imageMediaViewerImageView)
            }
        }
    }

    private fun configureToolbar(toolbar: Toolbar, mediaData: ImageContentRenderer.Data) {
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            title = mediaData.filename
            setHomeButtonEnabled(true)
            setDisplayHomeAsUpEnabled(true)
        }
    }

    override fun onBackPressed() {
        // show again for exit animation
        imageTransitionView.isVisible = true
        super.onBackPressed()
    }

    private fun scheduleStartPostponedTransition(sharedElement: View) {
        sharedElement.viewTreeObserver.addOnPreDrawListener(
                object : ViewTreeObserver.OnPreDrawListener {
                    override fun onPreDraw(): Boolean {
                        sharedElement.viewTreeObserver.removeOnPreDrawListener(this)
                        supportStartPostponedEnterTransition()
                        return true
                    }
                })
    }

    /**
     * Try and add a [Transition.TransitionListener] to the entering shared element
     * [Transition]. We do this so that we can load the full-size image after the transition
     * has completed.
     *
     * @return true if we were successful in adding a listener to the enter transition
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun addTransitionListener(): Boolean {
        val transition = window.sharedElementEnterTransition

        if (transition != null) {
            // There is an entering shared element transition so add a listener to it
            transition.addListener(
                    onEnd = {
                        if (mediaData.elementToDecrypt != null) {
                            // Encrypted image
                            GlideApp
                                    .with(this)
                                    .load(mediaData)
                                    .dontAnimate()
                                    .listener(object : RequestListener<Drawable> {
                                        override fun onLoadFailed(e: GlideException?,
                                                                  model: Any?,
                                                                  target: Target<Drawable>?,
                                                                  isFirstResource: Boolean): Boolean {
                                            // TODO ?
                                            Timber.e("TRANSITION onLoadFailed")
                                            imageMediaViewerImageView.isVisible = false
                                            encryptedImageView.isVisible = true
                                            return false
                                        }

                                        override fun onResourceReady(resource: Drawable?,
                                                                     model: Any?,
                                                                     target: Target<Drawable>?,
                                                                     dataSource: DataSource?,
                                                                     isFirstResource: Boolean): Boolean {
                                            Timber.e("TRANSITION onResourceReady")
                                            imageTransitionView.isInvisible = true
                                            imageMediaViewerImageView.isVisible = false
                                            encryptedImageView.isVisible = true
                                            return false
                                        }
                                    })
                                    .into(encryptedImageView)
                        } else {
                            imageTransitionView.isInvisible = true
                            // Clear image
                            imageMediaViewerImageView.isVisible = true
                            encryptedImageView.isVisible = false

                            imageMediaViewerImageView.setImageViewFactory(GlideImageViewFactory())
                            imageMediaViewerImageView.setProgressIndicator(ProgressPieIndicator())
                            imageContentRenderer.render(mediaData, imageMediaViewerImageView)
                        }
                    },
                    onCancel = {
                        // Something to do?
                    }
            )
            return true
        }

        // If we reach here then we have not added a listener
        return false
    }

    companion object {

        private const val EXTRA_MEDIA_DATA = "EXTRA_MEDIA_DATA"
        private const val EXTRA_SHARED_TRANSITION_NAME = "EXTRA_SHARED_TRANSITION_NAME"

        fun newIntent(context: Context, mediaData: ImageContentRenderer.Data, shareTransitionName: String?): Intent {
            return Intent(context, ImageMediaViewerActivity::class.java).apply {
                putExtra(EXTRA_MEDIA_DATA, mediaData)
                putExtra(EXTRA_SHARED_TRANSITION_NAME, shareTransitionName)
            }
        }
    }
}
