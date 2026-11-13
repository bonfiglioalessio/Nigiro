package com.github.damontecres.wholphin.ui.main

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Fixed layout values of the Nigiro home/search design (screen is 960x540 dp on the TV).
 *
 * Page content already starts 72 dp from the screen edge (collapsed drawer + 12 dp), so [ContentStart] puts text and
 * cards 48 dp right of the 60 dp sidebar.
 */
object NigiroDimens {
    val ContentStart = 36.dp
    val ContentEnd = 32.dp
    val HeroHeight = 340.dp
    val HeroBottomPadding = 24.dp
    val SectionGap = 20.dp
    val TitleGap = 10.dp
    val CardGap = 14.dp
    val PosterHeight = 180.dp
    val WideCardHeight = 124.dp
    val PlatformTileWidth = 120.dp
    val PlatformTileHeight = 68.dp
    val ButtonHeight = 46.dp

    // Inset the hero is stretched by to reach the sidebar and the right edge of the screen
    val HeroBleedStart = 12.dp
    val HeroBleedEnd = 15.dp

    val RowTitle = TextStyle(fontSize = 19.sp, lineHeight = 23.sp, fontWeight = FontWeight.Bold)
    val Meta = TextStyle(fontSize = 15.sp, lineHeight = 20.sp)
    val Body = TextStyle(fontSize = 15.sp, lineHeight = 22.sp)
    val Badge = TextStyle(fontSize = 13.sp, lineHeight = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
}
