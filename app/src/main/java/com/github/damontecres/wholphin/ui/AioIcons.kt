package com.github.damontecres.wholphin.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.LocalTextStyle
import androidx.tv.material3.Text
import com.github.damontecres.wholphin.R

/**
 * Nigiro's icon set: BoxIcons 2.1.4 (MIT), the icons AIOStreams draws through react-icons (sidebar, item menu, actions,
 * cards). [AioIcons] are ImageVectors built from the paths (usable outside composition, like Material icons); the same
 * icons are vector drawables `res/drawable/bx_*.xml` for painterResource. They replace the Material icons and FontAwesome.
 */
object AioIcons {
    val Search: ImageVector by lazy { boxIcon("Search", "M10 18a7.952 7.952 0 0 0 4.897-1.688l4.396 4.396 1.414-1.414-4.396-4.396A7.952 7.952 0 0 0 18 10c0-4.411-3.589-8-8-8s-8 3.589-8 8 3.589 8 8 8zm0-14c3.309 0 6 2.691 6 6s-2.691 6-6 6-6-2.691-6-6 2.691-6 6-6z") }

    val Home: ImageVector by lazy { boxIcon("Home", "M12.71 2.29a1 1 0 0 0-1.42 0l-9 9a1 1 0 0 0 0 1.42A1 1 0 0 0 3 13h1v7a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-7h1a1 1 0 0 0 1-1 1 1 0 0 0-.29-.71zM6 20v-9.59l6-6 6 6V20z") }

    val Heart: ImageVector by lazy { boxIcon("Heart", "M12 4.595a5.904 5.904 0 0 0-3.996-1.558 5.942 5.942 0 0 0-4.213 1.758c-2.353 2.363-2.352 6.059.002 8.412l7.332 7.332c.17.299.498.492.875.492a.99.99 0 0 0 .792-.409l7.415-7.415c2.354-2.354 2.354-6.049-.002-8.416a5.938 5.938 0 0 0-4.209-1.754A5.906 5.906 0 0 0 12 4.595zm6.791 1.61c1.563 1.571 1.564 4.025.002 5.588L12 18.586l-6.793-6.793c-1.562-1.563-1.561-4.017-.002-5.584.76-.756 1.754-1.172 2.799-1.172s2.035.416 2.789 1.17l.5.5a.999.999 0 0 0 1.414 0l.5-.5c1.512-1.509 4.074-1.505 5.584-.002z") }

    val HeartFilled: ImageVector by lazy { boxIcon("HeartFilled", "M20.205 4.791a5.938 5.938 0 0 0-4.209-1.754A5.906 5.906 0 0 0 12 4.595a5.904 5.904 0 0 0-3.996-1.558 5.942 5.942 0 0 0-4.213 1.758c-2.353 2.363-2.352 6.059.002 8.412L12 21.414l8.207-8.207c2.354-2.353 2.355-6.049-.002-8.416z") }

    val Compass: ImageVector by lazy { boxIcon("Compass", "M12 2C6.486 2 2 6.486 2 12s4.486 10 10 10 10-4.486 10-10S17.514 2 12 2zm0 18c-4.411 0-8-3.589-8-8s3.589-8 8-8 8 3.589 8 8-3.589 8-8 8z", "m8 16 5.991-2L16 8l-6 2z") }

    val Cog: ImageVector by lazy { boxIcon("Cog", "M12 16c2.206 0 4-1.794 4-4s-1.794-4-4-4-4 1.794-4 4 1.794 4 4 4zm0-6c1.084 0 2 .916 2 2s-.916 2-2 2-2-.916-2-2 .916-2 2-2z", "m2.845 16.136 1 1.73c.531.917 1.809 1.261 2.73.73l.529-.306A8.1 8.1 0 0 0 9 19.402V20c0 1.103.897 2 2 2h2c1.103 0 2-.897 2-2v-.598a8.132 8.132 0 0 0 1.896-1.111l.529.306c.923.53 2.198.188 2.731-.731l.999-1.729a2.001 2.001 0 0 0-.731-2.732l-.505-.292a7.718 7.718 0 0 0 0-2.224l.505-.292a2.002 2.002 0 0 0 .731-2.732l-.999-1.729c-.531-.92-1.808-1.265-2.731-.732l-.529.306A8.1 8.1 0 0 0 15 4.598V4c0-1.103-.897-2-2-2h-2c-1.103 0-2 .897-2 2v.598a8.132 8.132 0 0 0-1.896 1.111l-.529-.306c-.924-.531-2.2-.187-2.731.732l-.999 1.729a2.001 2.001 0 0 0 .731 2.732l.505.292a7.683 7.683 0 0 0 0 2.223l-.505.292a2.003 2.003 0 0 0-.731 2.733zm3.326-2.758A5.703 5.703 0 0 1 6 12c0-.462.058-.926.17-1.378a.999.999 0 0 0-.47-1.108l-1.123-.65.998-1.729 1.145.662a.997.997 0 0 0 1.188-.142 6.071 6.071 0 0 1 2.384-1.399A1 1 0 0 0 11 5.3V4h2v1.3a1 1 0 0 0 .708.956 6.083 6.083 0 0 1 2.384 1.399.999.999 0 0 0 1.188.142l1.144-.661 1 1.729-1.124.649a1 1 0 0 0-.47 1.108c.112.452.17.916.17 1.378 0 .461-.058.925-.171 1.378a1 1 0 0 0 .471 1.108l1.123.649-.998 1.729-1.145-.661a.996.996 0 0 0-1.188.142 6.071 6.071 0 0 1-2.384 1.399A1 1 0 0 0 13 18.7l.002 1.3H11v-1.3a1 1 0 0 0-.708-.956 6.083 6.083 0 0 1-2.384-1.399.992.992 0 0 0-1.188-.141l-1.144.662-1-1.729 1.124-.651a1 1 0 0 0 .471-1.108z") }

    val Play: ImageVector by lazy { boxIcon("Play", "M7 6v12l10-6z") }

    val Check: ImageVector by lazy { boxIcon("Check", "m10 15.586-3.293-3.293-1.414 1.414L10 18.414l9.707-9.707-1.414-1.414z") }

    val CheckDouble: ImageVector by lazy { boxIcon("CheckDouble", "m2.394 13.742 4.743 3.62 7.616-8.704-1.506-1.316-6.384 7.296-3.257-2.486zm19.359-5.084-1.506-1.316-6.369 7.279-.753-.602-1.25 1.562 2.247 1.798z") }

    val Trash: ImageVector by lazy { boxIcon("Trash", "M5 20a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V8h2V6h-4V4a2 2 0 0 0-2-2H9a2 2 0 0 0-2 2v2H3v2h2zM9 4h6v2H9zM8 8h9v12H7V8z", "M9 10h2v8H9zm4 0h2v8h-2z") }

    val Star: ImageVector by lazy { boxIcon("Star", "M21.947 9.179a1.001 1.001 0 0 0-.868-.676l-5.701-.453-2.467-5.461a.998.998 0 0 0-1.822-.001L8.622 8.05l-5.701.453a1 1 0 0 0-.619 1.713l4.213 4.107-1.49 6.452a1 1 0 0 0 1.53 1.057L12 18.202l5.445 3.63a1.001 1.001 0 0 0 1.517-1.106l-1.829-6.4 4.536-4.082c.297-.268.406-.686.278-1.065z") }

    val MoreVertical: ImageVector by lazy { boxIcon("MoreVertical", "M12 10c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm0-6c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm0 12c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z") }

    val MoreHorizontal: ImageVector by lazy { boxIcon("MoreHorizontal", "M12 10c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm6 0c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zM6 10c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z") }

    val ArrowRight: ImageVector by lazy { boxIcon("ArrowRight", "m11.293 17.293 1.414 1.414L19.414 12l-6.707-6.707-1.414 1.414L15.586 11H6v2h9.586z") }

    val ArrowLeft: ImageVector by lazy { boxIcon("ArrowLeft", "M12.707 17.293 8.414 13H18v-2H8.414l4.293-4.293-1.414-1.414L4.586 12l6.707 6.707z") }

    val Plus: ImageVector by lazy { boxIcon("Plus", "M19 11h-6V5h-2v6H5v2h6v6h2v-6h6z") }

    val Refresh: ImageVector by lazy { boxIcon("Refresh", "M10 11H7.101l.001-.009a4.956 4.956 0 0 1 .752-1.787 5.054 5.054 0 0 1 2.2-1.811c.302-.128.617-.226.938-.291a5.078 5.078 0 0 1 2.018 0 4.978 4.978 0 0 1 2.525 1.361l1.416-1.412a7.036 7.036 0 0 0-2.224-1.501 6.921 6.921 0 0 0-1.315-.408 7.079 7.079 0 0 0-2.819 0 6.94 6.94 0 0 0-1.316.409 7.04 7.04 0 0 0-3.08 2.534 6.978 6.978 0 0 0-1.054 2.505c-.028.135-.043.273-.063.41H2l4 4 4-4zm4 2h2.899l-.001.008a4.976 4.976 0 0 1-2.103 3.138 4.943 4.943 0 0 1-1.787.752 5.073 5.073 0 0 1-2.017 0 4.956 4.956 0 0 1-1.787-.752 5.072 5.072 0 0 1-.74-.61L7.05 16.95a7.032 7.032 0 0 0 2.225 1.5c.424.18.867.317 1.315.408a7.07 7.07 0 0 0 2.818 0 7.031 7.031 0 0 0 4.395-2.945 6.974 6.974 0 0 0 1.053-2.503c.027-.135.043-.273.063-.41H22l-4-4-4 4z") }

    val Reset: ImageVector by lazy { boxIcon("Reset", "M12 16c1.671 0 3-1.331 3-3s-1.329-3-3-3-3 1.331-3 3 1.329 3 3 3z", "M20.817 11.186a8.94 8.94 0 0 0-1.355-3.219 9.053 9.053 0 0 0-2.43-2.43 8.95 8.95 0 0 0-3.219-1.355 9.028 9.028 0 0 0-1.838-.18V2L8 5l3.975 3V6.002c.484-.002.968.044 1.435.14a6.961 6.961 0 0 1 2.502 1.053 7.005 7.005 0 0 1 1.892 1.892A6.967 6.967 0 0 1 19 13a7.032 7.032 0 0 1-.55 2.725 7.11 7.11 0 0 1-.644 1.188 7.2 7.2 0 0 1-.858 1.039 7.028 7.028 0 0 1-3.536 1.907 7.13 7.13 0 0 1-2.822 0 6.961 6.961 0 0 1-2.503-1.054 7.002 7.002 0 0 1-1.89-1.89A6.996 6.996 0 0 1 5 13H3a9.02 9.02 0 0 0 1.539 5.034 9.096 9.096 0 0 0 2.428 2.428A8.95 8.95 0 0 0 12 22a9.09 9.09 0 0 0 1.814-.183 9.014 9.014 0 0 0 3.218-1.355 8.886 8.886 0 0 0 1.331-1.099 9.228 9.228 0 0 0 1.1-1.332A8.952 8.952 0 0 0 21 13a9.09 9.09 0 0 0-.183-1.814z") }

    val Info: ImageVector by lazy { boxIcon("Info", "M12 2C6.486 2 2 6.486 2 12s4.486 10 10 10 10-4.486 10-10S17.514 2 12 2zm0 18c-4.411 0-8-3.589-8-8s3.589-8 8-8 8 3.589 8 8-3.589 8-8 8z", "M11 11h2v6h-2zm0-4h2v2h-2z") }

    val Error: ImageVector by lazy { boxIcon("Error", "M11.001 10h2v5h-2zM11 16h2v2h-2z", "M13.768 4.2C13.42 3.545 12.742 3.138 12 3.138s-1.42.407-1.768 1.063L2.894 18.064a1.986 1.986 0 0 0 .054 1.968A1.984 1.984 0 0 0 4.661 21h14.678c.708 0 1.349-.362 1.714-.968a1.989 1.989 0 0 0 .054-1.968L13.768 4.2zM4.661 19 12 5.137 19.344 19H4.661z") }

    val Calendar: ImageVector by lazy { boxIcon("Calendar", "M7 11h2v2H7zm0 4h2v2H7zm4-4h2v2h-2zm0 4h2v2h-2zm4-4h2v2h-2zm0 4h2v2h-2z", "M5 22h14c1.103 0 2-.897 2-2V6c0-1.103-.897-2-2-2h-2V2h-2v2H9V2H7v2H5c-1.103 0-2 .897-2 2v14c0 1.103.897 2 2 2zM19 8l.001 12H5V8h14z") }

    val Close: ImageVector by lazy { boxIcon("Close", "m16.192 6.344-4.243 4.242-4.242-4.242-1.414 1.414L10.535 12l-4.242 4.242 1.414 1.414 4.242-4.242 4.243 4.242 1.414-1.414L13.364 12l4.242-4.242z") }

    val ChevronUp: ImageVector by lazy { boxIcon("ChevronUp", "m6.293 13.293 1.414 1.414L12 10.414l4.293 4.293 1.414-1.414L12 7.586z") }

    val ChevronDown: ImageVector by lazy { boxIcon("ChevronDown", "M16.293 9.293 12 13.586 7.707 9.293l-1.414 1.414L12 16.414l5.707-5.707z") }

    val ChevronLeft: ImageVector by lazy { boxIcon("ChevronLeft", "M13.293 6.293 7.586 12l5.707 5.707 1.414-1.414L10.414 12l4.293-4.293z") }

    val ChevronRight: ImageVector by lazy { boxIcon("ChevronRight", "M10.707 17.707 16.414 12l-5.707-5.707-1.414 1.414L13.586 12l-4.293 4.293z") }

    val Shuffle: ImageVector by lazy { boxIcon("Shuffle", "M17 17h-1.559l-9.7-10.673A1 1 0 0 0 5.001 6H2v2h2.559l4.09 4.5-4.09 4.501H2v2h3.001a1 1 0 0 0 .74-.327L10 13.987l4.259 4.686a1 1 0 0 0 .74.327H17v3l5-4-5-4v3z", "M15.441 8H17v3l5-3.938L17 3v3h-2.001a1 1 0 0 0-.74.327l-3.368 3.707 1.48 1.346L15.441 8z") }

    val Dice: ImageVector by lazy { boxIcon("Dice", "M19 3H5c-1.103 0-2 .897-2 2v14c0 1.103.897 2 2 2h14c1.103 0 2-.897 2-2V5c0-1.103-.897-2-2-2zM8 17.5a1.5 1.5 0 1 1 .001-3.001A1.5 1.5 0 0 1 8 17.5zm0-8a1.5 1.5 0 1 1 .001-3.001A1.5 1.5 0 0 1 8 9.5zm4 4a1.5 1.5 0 1 1 .001-3.001A1.5 1.5 0 0 1 12 13.5zm4 4a1.5 1.5 0 1 1 .001-3.001A1.5 1.5 0 0 1 16 17.5zm0-8a1.5 1.5 0 1 1 .001-3.001A1.5 1.5 0 0 1 16 9.5z") }

    val Microphone: ImageVector by lazy { boxIcon("Microphone", "M16 12V6c0-2.217-1.785-4.021-3.979-4.021a.933.933 0 0 0-.209.025A4.006 4.006 0 0 0 8 6v6c0 2.206 1.794 4 4 4s4-1.794 4-4zm-6 0V6c0-1.103.897-2 2-2a.89.89 0 0 0 .163-.015C13.188 4.06 14 4.935 14 6v6c0 1.103-.897 2-2 2s-2-.897-2-2z", "M6 12H4c0 4.072 3.061 7.436 7 7.931V22h2v-2.069c3.939-.495 7-3.858 7-7.931h-2c0 3.309-2.691 6-6 6s-6-2.691-6-6z") }

    val Hdd: ImageVector by lazy { boxIcon("Hdd", "M20 13H4a2 2 0 0 0-2 2v4a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2v-4a2 2 0 0 0-2-2zm-4 5h-2v-2h2v2zm4 0h-2v-2h2v2zm.775-7H21l-1.652-7.434A2 2 0 0 0 17.396 2H6.604a2 2 0 0 0-1.952 1.566L3 11h17.775z") }

    val ListPlus: ImageVector by lazy { boxIcon("ListPlus", "M19 15v-3h-2v3h-3v2h3v3h2v-3h3v-2h-.937zM4 7h11v2H4zm0 4h11v2H4zm0 4h8v2H4z") }

    val Show: ImageVector by lazy { boxIcon("Show", "M12 9a3.02 3.02 0 0 0-3 3c0 1.642 1.358 3 3 3 1.641 0 3-1.358 3-3 0-1.641-1.359-3-3-3z", "M12 5c-7.633 0-9.927 6.617-9.948 6.684L1.946 12l.105.316C2.073 12.383 4.367 19 12 19s9.927-6.617 9.948-6.684l.106-.316-.105-.316C21.927 11.617 19.633 5 12 5zm0 12c-5.351 0-7.424-3.846-7.926-5C4.578 10.842 6.652 7 12 7c5.351 0 7.424 3.846 7.926 5-.504 1.158-2.578 5-7.926 5z") }

    val Hide: ImageVector by lazy { boxIcon("Hide", "M12 19c.946 0 1.81-.103 2.598-.281l-1.757-1.757c-.273.021-.55.038-.841.038-5.351 0-7.424-3.846-7.926-5a8.642 8.642 0 0 1 1.508-2.297L4.184 8.305c-1.538 1.667-2.121 3.346-2.132 3.379a.994.994 0 0 0 0 .633C2.073 12.383 4.367 19 12 19zm0-14c-1.837 0-3.346.396-4.604.981L3.707 2.293 2.293 3.707l18 18 1.414-1.414-3.319-3.319c2.614-1.951 3.547-4.615 3.561-4.657a.994.994 0 0 0 0-.633C21.927 11.617 19.633 5 12 5zm4.972 10.558-2.28-2.28c.19-.39.308-.819.308-1.278 0-1.641-1.359-3-3-3-.459 0-.888.118-1.277.309L8.915 7.501A9.26 9.26 0 0 1 12 7c5.351 0 7.424 3.846 7.926 5-.302.692-1.166 2.342-2.954 3.558z") }

    val Download: ImageVector by lazy { boxIcon("Download", "m12 16 4-5h-3V4h-2v7H8z", "M20 18H4v-7H2v7c0 1.103.897 2 2 2h16c1.103 0 2-.897 2-2v-7h-2v7z") }

    // Paths drawn with the even-odd rule (the converter keeps the SVG fill-rule)
    private class EvenOdd(
        val path: String,
    )

    private fun evenOdd(path: String) = EvenOdd(path)

    private fun boxIcon(
        name: String,
        vararg paths: Any,
    ): ImageVector =
        ImageVector
            .Builder(name = "bx.$name", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
            .apply {
                paths.forEach { path ->
                    val (data, rule) =
                        if (path is EvenOdd) path.path to PathFillType.EvenOdd else path as String to PathFillType.NonZero
                    addPath(pathData = addPathNodes(data), pathFillType = rule, fill = SolidColor(Color.Black))
                }
            }.build()
}

// FontAwesome glyph (the character of an R.string.fa_* resource) -> the BoxIcons drawable drawn in its place
private val GlyphIcons: Map<String, Int> =
    mapOf(
        "\uf107" to R.drawable.bx_chevron_down, // fa_angle_down
        "\uf106" to R.drawable.bx_chevron_up, // fa_angle_up
        "\uf103" to R.drawable.bx_chevrons_down, // fa_angles_down
        "\uf102" to R.drawable.bx_chevrons_up, // fa_angles_up
        "\uf175" to R.drawable.bx_down_arrow_alt, // fa_arrow_down_long
        "\uf0ec" to R.drawable.bx_transfer_alt, // fa_arrow_left_arrow_right
        "\uf177" to R.drawable.bx_left_arrow_alt, // fa_arrow_left_long
        "\uf178" to R.drawable.bx_right_arrow_alt, // fa_arrow_right_long
        "\uf176" to R.drawable.bx_up_arrow_alt, // fa_arrow_up_long
        "\uf021" to R.drawable.bx_refresh, // fa_arrows_rotate
        "\uf0f3" to R.drawable.bx_bell, // fa_bell
        "\uf0d7" to R.drawable.bx_chevron_down, // fa_caret_down
        "\uf0d8" to R.drawable.bx_chevron_up, // fa_caret_up
        "\uf00c" to R.drawable.bx_check, // fa_check
        "\uf057" to R.drawable.bx_x_circle, // fa_circle_xmark
        "\uf017" to R.drawable.bx_time, // fa_clock
        "\uf20a" to R.drawable.bx_captions, // fa_closed_captioning
        "\uf0ed" to R.drawable.bx_cloud_download, // fa_cloud_arrow_down
        "\uf0ee" to R.drawable.bx_cloud_upload, // fa_cloud_arrow_up
        "\uf51f" to R.drawable.bx_disc, // fa_compact_disc
        "\uf14e" to R.drawable.bx_compass, // fa_compass
        "\uf522" to R.drawable.bx_s_dice_5, // fa_dice
        "\uf019" to R.drawable.bx_download, // fa_download
        "\uf141" to R.drawable.bx_dots_horizontal_rounded, // fa_ellipsis
        "\uf142" to R.drawable.bx_dots_vertical_rounded, // fa_ellipsis_vertical
        "\uf06e" to R.drawable.bx_show, // fa_eye
        "\uf070" to R.drawable.bx_hide, // fa_eye_slash
        "\uf1c8" to R.drawable.bx_video, // fa_file_video
        "\uf008" to R.drawable.bx_movie, // fa_film
        "\uf0b0" to R.drawable.bx_filter_alt, // fa_filter
        "\uf0a0" to R.drawable.bx_s_hdd, // fa_hard_drive
        "\uf004" to R.drawable.bx_s_heart, // fa_heart
        "\uf0ca" to R.drawable.bx_list_ul, // fa_list_ul
        "\uf002" to R.drawable.bx_search, // fa_magnifying_glass
        "\uf00e" to R.drawable.bx_search_alt, // fa_magnifying_glass_plus
        "\uf130" to R.drawable.bx_microphone, // fa_microphone
        "\uf001" to R.drawable.bx_music, // fa_music
        "\uf07c" to R.drawable.bx_folder_open, // fa_open_folder
        "\uf04b" to R.drawable.bx_play, // fa_play
        "\uf074" to R.drawable.bx_shuffle, // fa_shuffle
        "\uf1de" to R.drawable.bx_slider_alt, // fa_sliders
        "\uf02b" to R.drawable.bx_purchase_tag, // fa_tag
        "\uf26c" to R.drawable.bx_tv, // fa_tv
        "\uf007" to R.drawable.bx_user, // fa_user
        "\uf03d" to R.drawable.bx_video, // fa_video
        "\uf028" to R.drawable.bx_volume_full, // fa_volume_high
        "\uf027" to R.drawable.bx_volume_low, // fa_volume_low
        "\uf00d" to R.drawable.bx_x, // fa_xmark
    )

/** The BoxIcons drawable standing for a FontAwesome glyph, or null if there is none yet */
@DrawableRes
fun glyphIcon(glyph: String): Int? = GlyphIcons[glyph]

/**
 * Drop-in for `Text(glyph, fontFamily = FontAwesome)`: draws the matching BoxIcons icon, sized from the font size
 * (BoxIcons leave a little padding in their 24 px box), and falls back to the font for glyphs without one.
 */
@Composable
fun FaGlyph(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    @Suppress("UNUSED_PARAMETER") fontFamily: FontFamily? = null,
    fontWeight: FontWeight? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    textAlign: TextAlign? = null,
    style: TextStyle? = null,
    maxLines: Int = Int.MAX_VALUE,
) {
    val icon = glyphIcon(text)
    if (icon == null) {
        Text(
            text = text,
            modifier = modifier,
            color = color,
            fontSize = fontSize,
            fontFamily = FontAwesome,
            fontWeight = fontWeight,
            lineHeight = lineHeight,
            textAlign = textAlign,
            style = style ?: LocalTextStyle.current,
            maxLines = maxLines,
        )
        return
    }
    val size =
        when {
            fontSize.isSpecified -> fontSize
            style != null && style.fontSize.isSpecified -> style.fontSize
            else -> LocalTextStyle.current.fontSize.takeIf { it.isSpecified } ?: 16.sp
        }
    val sizeDp = with(LocalDensity.current) { size.toDp() } * GLYPH_SCALE
    Icon(
        painter = painterResource(icon),
        contentDescription = null,
        tint = if (color.isSpecified) color else LocalContentColor.current,
        modifier = modifier.size(sizeDp),
    )
}

// A FontAwesome glyph fills its em box; a BoxIcons icon covers about 80% of its 24 px box
private const val GLYPH_SCALE = 1.25f
