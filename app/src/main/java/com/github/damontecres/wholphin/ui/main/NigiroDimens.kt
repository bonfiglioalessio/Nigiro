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
    val HeroHeight = 276.dp
    val HeroBottomPadding = 16.dp
    val SectionGap = 16.dp
    val TitleGap = 10.dp
    val CardGap = 14.dp
    // AIOStreams lg row cards (1 css px = 0.75 dp): posters 168x252 px, wide cards 304x171 px
    val PosterHeight = 189.dp

    // AIOStreams-style home: continue watching cards (16:9) and the fixed height of the focused row
    val ContinueCardHeight = 128.dp
    val FocusedRowTop = 120.dp

    // AIOStreams hero: sm:h-[30rem] minus the top bar on the 960x540 dp TV screen
    val AioHeroHeight = 430.dp
    val WideCardHeight = 124.dp
    val PlatformTileWidth = 120.dp
    val PlatformTileHeight = 68.dp
    val ButtonHeight = 44.dp

    // Inset the hero is stretched by to reach the sidebar and the right edge of the screen
    // Under the transparent sidebar too: 60 dp collapsed sidebar + 12 dp page padding
    val HeroBleedStart = 72.dp
    val HeroBleedEnd = 16.dp

    val RowTitle = TextStyle(fontFamily = com.github.damontecres.wholphin.ui.theme.InterFontFamily, fontSize = 19.sp, lineHeight = 23.sp, fontWeight = FontWeight.Bold)
    val Meta = TextStyle(fontFamily = com.github.damontecres.wholphin.ui.theme.InterFontFamily, fontSize = 15.sp, lineHeight = 20.sp)
    val Body = TextStyle(fontFamily = com.github.damontecres.wholphin.ui.theme.InterFontFamily, fontSize = 15.sp, lineHeight = 22.sp)
    val Badge = TextStyle(fontFamily = com.github.damontecres.wholphin.ui.theme.InterFontFamily, fontSize = 13.sp, lineHeight = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
}
