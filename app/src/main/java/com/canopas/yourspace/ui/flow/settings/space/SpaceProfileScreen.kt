package com.canopas.yourspace.ui.flow.settings.space

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.canopas.yourspace.R
import com.canopas.yourspace.data.models.user.UserInfo
import com.canopas.yourspace.domain.utils.ConnectivityObserver
import com.canopas.yourspace.ui.component.AppAlertDialog
import com.canopas.yourspace.ui.component.AppBanner
import com.canopas.yourspace.ui.component.AppProgressIndicator
import com.canopas.yourspace.ui.component.NoInternetScreen
import com.canopas.yourspace.ui.component.PrimaryTextButton
import com.canopas.yourspace.ui.component.UserProfile
import com.canopas.yourspace.ui.flow.settings.profile.UserTextField
import com.canopas.yourspace.ui.theme.AppTheme

@Composable
fun SpaceProfileScreen() {
    val viewModel = hiltViewModel<SpaceProfileViewModel>()
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            SpaceProfileToolbar()
        }
    ) {
        Box(
            modifier = Modifier
                .padding(it)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (state.connectivityStatus == ConnectivityObserver.Status.Available) {
                SpaceProfileContent()
                if (state.isLoading) {
                    AppProgressIndicator()
                }
            } else {
                NoInternetScreen(viewModel::checkInternetConnection)
            }
        }
    }
    if (state.error != null) {
        AppBanner(msg = state.error!!) {
            viewModel.resetErrorState()
        }
    }

    if (state.showDeleteSpaceConfirmation) {
        AppAlertDialog(
            title = stringResource(id = R.string.space_settings_btn_delete_space),
            subTitle = stringResource(id = R.string.space_settings_delete_confrim_message),
            confirmBtnText = stringResource(id = R.string.space_settings_delete_confrim_btn),
            dismissBtnText = stringResource(id = R.string.common_btn_cancel),
            isConfirmDestructive = true,
            onDismissClick = {
                viewModel.showDeleteSpaceConfirmation(false)
            },
            onConfirmClick = {
                viewModel.deleteSpace()
            }
        )
    }

    if (state.showLeaveSpaceConfirmation) {
        AppAlertDialog(
            title = stringResource(id = R.string.space_settings_btn_leave_space),
            subTitle = stringResource(id = R.string.space_settings_leave_confrim_message),
            confirmBtnText = stringResource(id = R.string.space_settings_leave_confrim_btn),
            dismissBtnText = stringResource(id = R.string.common_btn_cancel),
            isConfirmDestructive = true,
            onDismissClick = {
                viewModel.showLeaveSpaceConfirmation(false)
            },
            onConfirmClick = {
                viewModel.leaveSpace()
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpaceProfileToolbar() {
    val viewModel = hiltViewModel<SpaceProfileViewModel>()
    val state by viewModel.state.collectAsState()
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(containerColor = AppTheme.colorScheme.surface),
        title = {
            Text(
                text = stringResource(
                    id = R.string.setting_space_settings,
                    state.spaceInfo?.space?.name ?: ""
                ),
                style = AppTheme.appTypography.subTitle1
            )
        },
        navigationIcon = {
            IconButton(onClick = { viewModel.popBackStack() }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_nav_back_arrow_icon),
                    contentDescription = ""
                )
            }
        },
        actions = {
            Text(
                text = stringResource(id = R.string.edit_profile_toolbar_save_text),
                color = if (state.allowSave) AppTheme.colorScheme.primary else AppTheme.colorScheme.textDisabled,
                style = AppTheme.appTypography.button,
                modifier = Modifier
                    .padding(end = 8.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = false),
                        enabled = state.allowSave,
                        onClick = {
                            viewModel.saveSpace()
                        }
                    )
            )
        }
    )
}

@Composable
private fun SpaceProfileContent() {
    val viewModel = hiltViewModel<SpaceProfileViewModel>()
    val state by viewModel.state.collectAsState()
    val scrollState = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(bottom = 80.dp)
        ) {
            UserTextField(
                label = stringResource(R.string.space_setting_hint_space_name),
                text = state.spaceName ?: "",
                enabled = state.isAdmin,
                onValueChange = {
                    viewModel.onNameChanged(it.trimStart())
                }
            )

            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                color = AppTheme.colorScheme.outline
            )

            Header(title = stringResource(id = R.string.space_setting_title_your_location))

            state.spaceInfo?.members?.firstOrNull { it.user.id == state.currentUserId }?.let {
                UserItem(
                    userInfo = it,
                    isChecked = state.locationEnabled,
                    enable = true,
                    onCheckedChange = {
                        viewModel.onLocationEnabledChanged(it)
                    }
                )
            }

            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                color = AppTheme.colorScheme.outline
            )

            Header(title = stringResource(id = R.string.space_setting_title_members_location_status))

            val others =
                state.spaceInfo?.members?.filter { it.user.id != state.currentUserId }
                    ?: emptyList()

            if (others.isNotEmpty()) {
                others.forEach {
                    UserItem(
                        userInfo = it,
                        isChecked = it.isLocationEnable,
                        enable = false,
                        onCheckedChange = {
                        }
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = stringResource(
                        id = R.string.space_setting_no_members,
                        state.spaceInfo?.space?.name ?: ""
                    ),
                    style = AppTheme.appTypography.body1,
                    color = AppTheme.colorScheme.textPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
            }
        }
        if (state.spaceInfo != null && state.currentUserId == state.spaceInfo?.space?.admin_id) {
            FooterButton(
                title = stringResource(id = R.string.space_settings_btn_delete_space),
                onClick = {
                    viewModel.showDeleteSpaceConfirmation(true)
                },
                showLoader = state.deletingSpace,
                icon = Icons.Default.Delete
            )
        }

        if (state.spaceInfo != null && state.currentUserId != state.spaceInfo?.space?.admin_id) {
            FooterButton(
                title = stringResource(id = R.string.space_settings_btn_leave_space),
                onClick = {
                    viewModel.showLeaveSpaceConfirmation(true)
                },
                showLoader = state.leavingSpace,
                icon = Icons.AutoMirrored.Filled.ExitToApp
            )
        }
    }
}

@Composable
private fun BoxScope.FooterButton(
    title: String,
    icon: ImageVector,
    showLoader: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        AppTheme.colorScheme.surface.copy(alpha = 0.1f),
                        AppTheme.colorScheme.surface.copy(alpha = 0.9f),
                        AppTheme.colorScheme.surface,
                        AppTheme.colorScheme.surface
                    )
                )
            )
            .padding(bottom = 16.dp, top = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        PrimaryTextButton(
            label = title,
            onClick = onClick,
            containerColor = AppTheme.colorScheme.containerLow,
            contentColor = AppTheme.colorScheme.alertColor,
            showLoader = showLoader,
            icon = {
                Icon(
                    imageVector = icon,
                    contentDescription = "",
                    modifier = Modifier.size(20.dp)
                )
            }
        )
    }
}

@Composable
private fun UserItem(
    userInfo: UserInfo,
    isChecked: Boolean,
    enable: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UserProfile(modifier = Modifier.size(40.dp), user = userInfo.user)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = userInfo.user.fullName,
            style = AppTheme.appTypography.subTitle2,
            color = AppTheme.colorScheme.textPrimary,
            textAlign = TextAlign.Start,
            modifier = Modifier.weight(1f)
        )

        Switch(
            checked = isChecked,
            enabled = enable,
            colors = SwitchDefaults.colors(
                checkedThumbColor = AppTheme.colorScheme.onPrimary,
                uncheckedThumbColor = AppTheme.colorScheme.onPrimary,
                uncheckedTrackColor = AppTheme.colorScheme.containerHigh
            ),
            onCheckedChange = {
                onCheckedChange(it)
            }
        )
    }
}

@Composable
private fun Header(title: String) {
    Text(
        text = title,
        style = AppTheme.appTypography.subTitle1,
        color = AppTheme.colorScheme.textDisabled,
        textAlign = TextAlign.Start,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, bottom = 8.dp)
    )
}
