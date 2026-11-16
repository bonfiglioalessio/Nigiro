package com.github.damontecres.wholphin.util

import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.UUID

class PlatformItemsRequestTest {
    private fun item(name: String) = BaseItemDto(id = UUID.nameUUIDFromBytes(name.toByteArray()), name = name, type = BaseItemKind.MOVIE)

    @Test
    fun `movies and series alternate, the longer list finishing alone`() {
        val result = interleaveDistinct(listOf(item("m1"), item("m2"), item("m3")), listOf(item("s1")))
        assertEquals(listOf("m1", "s1", "m2", "m3"), result.map { it.name })
    }

    @Test
    fun `a title in both lists appears once`() {
        val result = interleaveDistinct(listOf(item("a"), item("b")), listOf(item("a"), item("c")))
        assertEquals(listOf("a", "b", "c"), result.map { it.name })
    }

    @Test
    fun `an empty side gives the other list`() {
        assertEquals(listOf("s1"), interleaveDistinct(emptyList(), listOf(item("s1"))).map { it.name })
    }
}
