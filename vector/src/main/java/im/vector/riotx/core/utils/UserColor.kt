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

package im.vector.riotx.core.utils

import androidx.annotation.ColorRes
import im.vector.riotx.R
import im.vector.riotx.core.resources.ColorProvider
import org.billcarsonfr.jsonviewer.JSonViewerStyleProvider
import kotlin.math.abs

@ColorRes
fun getColorFromUserId(userId: String?): Int {
    var hash = 0

    userId?.toList()?.map { chr -> hash = (hash shl 5) - hash + chr.toInt() }

    return when (abs(hash) % 8) {
        1    -> R.color.riotx_username_2
        2    -> R.color.riotx_username_3
        3    -> R.color.riotx_username_4
        4    -> R.color.riotx_username_5
        5    -> R.color.riotx_username_6
        6    -> R.color.riotx_username_7
        7    -> R.color.riotx_username_8
        else -> R.color.riotx_username_1
    }
}

fun jsonViewerStyler(colorProvider: ColorProvider): JSonViewerStyleProvider {
    return JSonViewerStyleProvider(
            keyColor = colorProvider.getColor(R.color.riotx_accent),
            secondaryColor = colorProvider.getColorFromAttribute(R.attr.riotx_text_secondary),
            stringColor = colorProvider.getColorFromAttribute(R.attr.vctr_notice_text_color),
            baseColor = colorProvider.getColorFromAttribute(R.attr.riotx_text_primary),
            booleanColor = colorProvider.getColorFromAttribute(R.attr.vctr_notice_text_color),
            numberColor = colorProvider.getColorFromAttribute(R.attr.vctr_notice_text_color)
    )
}
