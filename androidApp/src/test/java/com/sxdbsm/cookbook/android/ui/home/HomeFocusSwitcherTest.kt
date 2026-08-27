package com.sxdbsm.cookbook.android.ui.home

import com.sxdbsm.cookbook.data.repository.PresentFocusSelection
import com.sxdbsm.cookbook.domain.model.FamilyMember
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeFocusSwitcherTest {

    @Test
    fun presentFocusSelectionOnlyPublishesPresentTabsAndEffectiveViewer() {
        val dad = FamilyMember(id = 2L, name = "爸", isFocus = true)
        val switcher = PresentFocusSelection(
            members = listOf(dad),
            viewing = dad,
            share = 1.0,
            requiresViewingFallback = true,
        ).toFocusSwitcher()

        assertEquals(listOf("爸"), switcher.members.map { it.name })
        assertEquals(dad.id, switcher.viewingId)
    }
}
