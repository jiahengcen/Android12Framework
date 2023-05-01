/*
 * Copyright (C) 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.controls.dagger

import android.testing.AndroidTestingRunner
import android.provider.Settings
import androidx.test.filters.SmallTest
import com.android.internal.widget.LockPatternUtils
import com.android.internal.widget.LockPatternUtils.StrongAuthTracker.STRONG_AUTH_NOT_REQUIRED
import com.android.internal.widget.LockPatternUtils.StrongAuthTracker.STRONG_AUTH_REQUIRED_AFTER_BOOT
import com.android.systemui.SysuiTestCase
import com.android.systemui.controls.controller.ControlsController
import com.android.systemui.controls.management.ControlsListingController
import com.android.systemui.controls.ui.ControlsUiController
import com.android.systemui.settings.UserTracker
import com.android.systemui.statusbar.policy.KeyguardStateController
import com.android.systemui.util.settings.SecureSettings
import dagger.Lazy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Answers
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.anyInt
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations

@SmallTest
@RunWith(AndroidTestingRunner::class)
class ControlsComponentTest : SysuiTestCase() {

    @Mock
    private lateinit var controller: ControlsController
    @Mock
    private lateinit var uiController: ControlsUiController
    @Mock
    private lateinit var listingController: ControlsListingController
    @Mock
    private lateinit var keyguardStateController: KeyguardStateController
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private lateinit var userTracker: UserTracker
    @Mock
    private lateinit var lockPatternUtils: LockPatternUtils
    @Mock
    private lateinit var secureSettings: SecureSettings

    companion object {
        fun <T> eq(value: T): T = Mockito.eq(value) ?: value
    }

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)

        `when`(userTracker.userHandle.identifier).thenReturn(0)
    }

    @Test
    fun testFeatureEnabled() {
        val component = setupComponent(true)

        assertTrue(component.getControlsController().isPresent)
        assertEquals(controller, component.getControlsController().get())
        assertTrue(component.getControlsUiController().isPresent)
        assertEquals(uiController, component.getControlsUiController().get())
        assertTrue(component.getControlsListingController().isPresent)
        assertEquals(listingController, component.getControlsListingController().get())
    }

    @Test
    fun testFeatureDisabled() {
        val component = setupComponent(false)

        assertFalse(component.getControlsController().isPresent)
        assertFalse(component.getControlsUiController().isPresent)
        assertFalse(component.getControlsListingController().isPresent)
    }

    @Test
    fun testFeatureDisabledVisibility() {
        val component = setupComponent(false)

        assertEquals(ControlsComponent.Visibility.UNAVAILABLE, component.getVisibility())
    }

    @Test
    fun testFeatureEnabledAfterBootVisibility() {
        `when`(lockPatternUtils.getStrongAuthForUser(anyInt()))
            .thenReturn(STRONG_AUTH_REQUIRED_AFTER_BOOT)
        val component = setupComponent(true)

        assertEquals(ControlsComponent.Visibility.AVAILABLE_AFTER_UNLOCK, component.getVisibility())
    }

    @Test
    fun testFeatureEnabledAndCannotShowOnLockScreenVisibility() {
        `when`(lockPatternUtils.getStrongAuthForUser(anyInt()))
            .thenReturn(STRONG_AUTH_NOT_REQUIRED)
        `when`(keyguardStateController.isUnlocked()).thenReturn(false)
        `when`(secureSettings.getInt(eq(Settings.Secure.LOCKSCREEN_SHOW_CONTROLS), anyInt()))
            .thenReturn(0)
        val component = setupComponent(true)

        assertEquals(ControlsComponent.Visibility.AVAILABLE_AFTER_UNLOCK, component.getVisibility())
    }

    @Test
    fun testFeatureEnabledAndCanShowOnLockScreenVisibility() {
        `when`(lockPatternUtils.getStrongAuthForUser(anyInt()))
            .thenReturn(STRONG_AUTH_NOT_REQUIRED)
        `when`(keyguardStateController.isUnlocked()).thenReturn(false)
        `when`(secureSettings.getInt(eq(Settings.Secure.LOCKSCREEN_SHOW_CONTROLS), anyInt()))
            .thenReturn(1)
        val component = setupComponent(true)

        assertEquals(ControlsComponent.Visibility.AVAILABLE, component.getVisibility())
    }

    @Test
    fun testFeatureEnabledAndCanShowWhileUnlockedVisibility() {
        `when`(secureSettings.getInt(eq(Settings.Secure.LOCKSCREEN_SHOW_CONTROLS), anyInt()))
            .thenReturn(0)
        `when`(lockPatternUtils.getStrongAuthForUser(anyInt()))
            .thenReturn(STRONG_AUTH_NOT_REQUIRED)
        `when`(keyguardStateController.isUnlocked()).thenReturn(true)
        val component = setupComponent(true)

        assertEquals(ControlsComponent.Visibility.AVAILABLE, component.getVisibility())
    }

    private fun setupComponent(enabled: Boolean): ControlsComponent {
        return ControlsComponent(
            enabled,
            mContext,
            Lazy { controller },
            Lazy { uiController },
            Lazy { listingController },
            lockPatternUtils,
            keyguardStateController,
            userTracker,
            secureSettings
        )
    }
}
