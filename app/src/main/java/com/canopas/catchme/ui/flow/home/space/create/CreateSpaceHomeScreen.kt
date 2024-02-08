package com.canopas.catchme.ui.flow.home.space.create

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.canopas.catchme.ui.component.AppBanner
import com.canopas.catchme.ui.component.CreateSpace
import com.canopas.catchme.ui.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateSpaceHomeScreen() {
    val viewModel = hiltViewModel<CreateSpaceHomeViewModel>()
    val state by viewModel.state.collectAsState()

    if (state.error != null) {
        AppBanner(msg = state.error!!) {
            viewModel.resetErrorState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppTheme.colorScheme.surface),
                title = {
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = ""
                        )
                    }
                }
            )
        }
    ) {
        CreateSpace(
            modifier = Modifier.padding(it),
            spaceName = state.spaceName,
            showLoader = state.creatingSpace,
            onSpaceNameChanged = { viewModel.onSpaceNameChange(it) },
            onNext = {
                viewModel.createSpace()
            }
        )
    }
}
