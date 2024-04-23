package com.canopas.yourspace.ui.flow.geofence.addplace.locate

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.canopas.yourspace.R
import com.canopas.yourspace.domain.utils.getAddress
import com.canopas.yourspace.ui.component.AppProgressIndicator
import com.canopas.yourspace.ui.flow.home.map.DEFAULT_CAMERA_ZOOM
import com.canopas.yourspace.ui.flow.home.map.component.MapControlBtn
import com.canopas.yourspace.ui.flow.home.map.distanceTo
import com.canopas.yourspace.ui.theme.AppTheme
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocateOnMapScreen() {
    val viewModel = hiltViewModel<LocateOnMapViewModel>()
    val state by viewModel.state.collectAsState()

    val userLocation = remember(state.defaultLocation) {
        val location = state.defaultLocation
        LatLng(location?.latitude ?: 0.0, location?.longitude ?: 0.0)
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(userLocation, DEFAULT_CAMERA_ZOOM)
    }

    LaunchedEffect(userLocation) {
        cameraPositionState.animate(
            CameraUpdateFactory.newLatLngZoom(
                userLocation,
                DEFAULT_CAMERA_ZOOM
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppTheme.colorScheme.surface),
                title = {
                    Text(
                        text = if (!state.selectedPlaceName.isNullOrEmpty()) {
                            stringResource(
                                id = R.string.locate_on_map_selected_name_title,
                                state.selectedPlaceName ?: ""
                            )
                        } else {
                            stringResource(id = R.string.locate_on_map_title)
                        },
                        style = AppTheme.appTypography.header3
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.popBackStack() }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = ""
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            viewModel.onNextClick(
                                cameraPositionState.position.target.latitude,
                                cameraPositionState.position.target.longitude
                            )
                        },
                        enabled = !state.addingPlace && !cameraPositionState.isMoving && (state.selectedPlaceName.isNullOrEmpty() || state.updatedPlaceName.isNotEmpty()),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = AppTheme.colorScheme.primary,
                            disabledContentColor = AppTheme.colorScheme.textDisabled
                        )
                    ) {
                        if (state.addingPlace) {
                            AppProgressIndicator()
                        } else {
                            Text(
                                text = if (!state.selectedPlaceName.isNullOrEmpty()) {
                                    stringResource(id = R.string.common_btn_add)
                                } else {
                                    stringResource(
                                        id = R.string.common_btn_next
                                    )
                                },
                                style = AppTheme.appTypography.subTitle1
                            )
                        }
                    }
                }
            )
        },
        contentColor = AppTheme.colorScheme.textPrimary,
        containerColor = AppTheme.colorScheme.surface
    ) {
        LocateOnMapContent(modifier = Modifier.padding(it), cameraPositionState, userLocation)
    }
}

@Composable
fun LocateOnMapContent(
    modifier: Modifier,
    cameraPositionState: CameraPositionState,
    userLocation: LatLng
) {
    val viewModel = hiltViewModel<LocateOnMapViewModel>()
    val state by viewModel.state.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.colorScheme.surface)
    ) {
        if (!state.selectedPlaceName.isNullOrEmpty()) {
            PlaceNameContent(
                state.updatedPlaceName ?: "",
                cameraPositionState,
                userLocation,
                viewModel::onPlaceNameChanged
            )
        }
        MapView(Modifier.fillMaxSize(), cameraPositionState, userLocation)
    }
}

@Composable
fun PlaceNameContent(
    placeName: String,
    cameraPositionState: CameraPositionState,
    userLocation: LatLng,
    onPlaceNameChanged: (String) -> Unit
) {
    val context = LocalContext.current
    var address by remember { mutableStateOf("") }
    Timber.d("isMoving: ${cameraPositionState.isMoving}")

    LaunchedEffect(cameraPositionState.isMoving) {
        withContext(Dispatchers.IO) {
            if (!cameraPositionState.isMoving) {
                address = ""
                address = cameraPositionState.position.target.getAddress(context) ?: ""
            }
        }
    }

    LaunchedEffect(key1 = userLocation) {
        withContext(Dispatchers.IO) {
            if (address.isEmpty()) {
                address = userLocation.getAddress(context) ?: ""
            }
        }
    }

    PlaceNameTextField(
        placeName,
        leadingIcon = R.drawable.ic_bookmark,
        onValueChange = onPlaceNameChanged
    )

    PlaceNameTextField(
        address.ifEmpty { stringResource(id = R.string.locate_on_map_hint_getting_address) },
        leadingIcon = R.drawable.ic_tab_places_filled,
        enable = false
    )

    Spacer(modifier = Modifier.height(10.dp))
}

@Composable
fun PlaceNameTextField(
    text: String,
    enable: Boolean = true,
    leadingIcon: Int,
    onValueChange: ((value: String) -> Unit) = {}

) {
    val focusManager = LocalFocusManager.current
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val outlineColor =
        if (isFocused) AppTheme.colorScheme.primary else AppTheme.colorScheme.outline

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painterResource(id = leadingIcon),
                contentDescription = null,
                tint = AppTheme.colorScheme.onDisabled,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            BasicTextField(
                value = text,
                onValueChange = { onValueChange(it) },
                maxLines = 1,
                enabled = enable,
                interactionSource = interactionSource,
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxWidth(),
                singleLine = true,
                textStyle = AppTheme.appTypography.subTitle1.copy(color = AppTheme.colorScheme.textPrimary),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = {
                    focusManager.clearFocus()
                }),
                cursorBrush = SolidColor(AppTheme.colorScheme.primary)
            )
        }

        Divider(
            Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            color = outlineColor
        )
    }
}

@Composable
private fun MapView(
    modifier: Modifier,
    cameraPositionState: CameraPositionState,
    userLocation: LatLng
) {
    val scope = rememberCoroutineScope()
    val relocate by remember {
        derivedStateOf {
            val distance = cameraPositionState.position.target.distanceTo(userLocation)
            distance > 100
        }
    }

    val isDarkMode = isSystemInDarkTheme()
    val context = LocalContext.current
    val mapProperties = remember(isDarkMode) {
        MapProperties(
            mapStyleOptions = if (isDarkMode) {
                MapStyleOptions.loadRawResourceStyle(context, R.raw.map_theme_night)
            } else {
                null
            }
        )
    }
    Box(modifier = modifier) {
        GoogleMap(
            cameraPositionState = cameraPositionState,
            properties = mapProperties,
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                tiltGesturesEnabled = false,
                myLocationButtonEnabled = false,
                compassEnabled = false,
                mapToolbarEnabled = false
            )
        )

        MapControlBtn(
            modifier = Modifier
                .padding(bottom = 10.dp, end = 10.dp)
                .align(Alignment.BottomEnd),
            icon = R.drawable.ic_relocate,
            show = relocate
        ) {
            scope.launch {
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngZoom(
                        userLocation,
                        DEFAULT_CAMERA_ZOOM
                    )
                )
            }
        }

        SmallFloatingActionButton(
            modifier = Modifier.align(Alignment.Center),
            onClick = { },
            containerColor = AppTheme.colorScheme.surface,
            contentColor = AppTheme.colorScheme.primary,
            shape = CircleShape
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_tab_places_filled),
                contentDescription = "",
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
