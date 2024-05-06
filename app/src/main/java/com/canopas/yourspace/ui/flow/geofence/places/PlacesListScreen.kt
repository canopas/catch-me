package com.canopas.yourspace.ui.flow.geofence.places

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.canopas.yourspace.R
import com.canopas.yourspace.data.models.place.ApiPlace
import com.canopas.yourspace.ui.component.AppAlertDialog
import com.canopas.yourspace.ui.component.AppBanner
import com.canopas.yourspace.ui.component.AppProgressIndicator
import com.canopas.yourspace.ui.component.motionClickEvent
import com.canopas.yourspace.ui.flow.geofence.add.components.PlaceAddedPopup
import com.canopas.yourspace.ui.theme.AppTheme

const val EXTRA_RESULT_PLACE_LATITUDE = "place_latitude"
const val EXTRA_RESULT_PLACE_LONGITUDE = "place_longitude"
const val EXTRA_RESULT_PLACE_NAME = "place_name"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlacesListScreen() {
    val viewModel = hiltViewModel<PlacesListViewModel>()
    val state by viewModel.state.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppTheme.colorScheme.surface),
                title = {
                    Text(
                        text = stringResource(id = R.string.places_list_title),
                        style = AppTheme.appTypography.header3
                    )
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
        },
        contentColor = AppTheme.colorScheme.textPrimary,
        containerColor = AppTheme.colorScheme.surface
    ) {
        PlacesListContent(modifier = Modifier.padding(it))
    }

    if (state.placeAdded) {
        val lat = state.addedPlaceLat
        val lng = state.addedPlaceLng
        val name = state.addedPlaceName

        PlaceAddedPopup(lat, lng, name) {
            viewModel.dismissPlaceAddedPopup()
        }
    }

    if (state.placeToDelete != null) {
        AppAlertDialog(
            subTitle = stringResource(id = R.string.places_list_delete_dialogue_message_text),
            confirmBtnText = stringResource(id = R.string.common_btn_delete),
            dismissBtnText = stringResource(id = R.string.common_btn_cancel),
            isConfirmDestructive = true,
            onConfirmClick = {
                viewModel.onDeletePlace()
            },
            onDismissClick = {
                viewModel.dismissDeletePlaceConfirmation()
            },
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
        )
    }

    if (state.error != null) {
        AppBanner(msg = state.error!!) {
            viewModel.resetErrorState()
        }
    }
}

@Composable
fun PlacesListContent(modifier: Modifier) {
    val context = LocalContext.current
    val viewModel = hiltViewModel<PlacesListViewModel>()
    val state by viewModel.state.collectAsState()

    val predefinedSuggestions = remember { mutableStateListOf<String>() }

    LaunchedEffect(key1 = state.places) {
        predefinedSuggestions.clear()
        val suggestions = context.resources.getStringArray(R.array.add_places_suggestions)

        suggestions.filter {
            !state.places.any { place -> place.name.contains(it, true) }
        }.let {
            predefinedSuggestions.addAll(it)
        }
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item { AddPlaceButton() { viewModel.navigateToAddPlace() } }

            if (!state.placesLoading) {
                items(state.places) { place ->
                    PlaceItem(
                        place,
                        isDeleting = state.deletingPlaces.contains(place),
                        allowDelete = state.currentUser?.id == place.created_by,
                        onClick = { viewModel.navigateToEditPlace(place) },
                        onDeleteClick = { viewModel.showDeletePlaceConfirmation(place) }
                    )
                }

                items(predefinedSuggestions) {
                    PlaceSuggestionItem(it) {
                        viewModel.selectedSuggestion(it)
                    }
                }
            }
        }

        if (state.placesLoading) AppProgressIndicator()
    }
}

@Composable
fun PlacesListItem(
    name: String,
    icon: Painter,
    isSuggestion: Boolean = false,
    showLoader: Boolean = false,
    allowDelete: Boolean = false,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .clickable { onClick() }
            .padding(vertical = 10.dp)
            .padding(start = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    if (isSuggestion) AppTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent,
                    shape = CircleShape
                )
                .border(
                    0.5.dp,
                    if (!isSuggestion) AppTheme.colorScheme.primary else Color.Transparent,
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = AppTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }

        Text(
            text = name,
            style = AppTheme.appTypography.subTitle1,
            modifier = Modifier
                .padding(start = 10.dp)
                .weight(1f)
        )

        if (!isSuggestion && allowDelete) {
            IconButton(onClick = onDeleteClick, enabled = !showLoader) {
                if (showLoader) {
                    AppProgressIndicator(
                        color = AppTheme.colorScheme.textDisabled,
                        strokeWidth = 1.dp
                    )
                } else {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = null,
                        tint = AppTheme.colorScheme.textDisabled
                    )
                }
            }
        }
    }

    Divider(
        thickness = 1.dp,
        modifier = Modifier.fillMaxWidth(),
        color = AppTheme.colorScheme.outline
    )
}

@Composable
fun PlaceItem(
    place: ApiPlace,
    isDeleting: Boolean,
    allowDelete: Boolean,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val icon =
        when {
            place.name.contains("Home", true) -> painterResource(id = R.drawable.ic_place_home)
            place.name.contains("Work", true) -> painterResource(id = R.drawable.ic_place_work)
            place.name.contains("School", true) -> painterResource(id = R.drawable.ic_place_school)
            place.name.contains("Gym", true) -> painterResource(id = R.drawable.ic_place_gym)
            place.name.contains("Library", true) ->
                painterResource(id = R.drawable.ic_place_library)

            place.name.contains("Local Park", true) ->
                painterResource(id = R.drawable.ic_place_park)

            else -> painterResource(id = R.drawable.ic_tab_places_filled)
        }

    PlacesListItem(
        name = place.name,
        icon = icon,
        isSuggestion = false,
        showLoader = isDeleting,
        allowDelete = allowDelete,
        onClick = onClick,
        onDeleteClick = onDeleteClick
    )
}

@Composable
fun PlaceSuggestionItem(suggestion: String, onClick: () -> Unit) {
    val icon =
        when (suggestion) {
            "Home" -> painterResource(id = R.drawable.ic_place_home)
            "Work" -> painterResource(id = R.drawable.ic_place_work)
            "School" -> painterResource(id = R.drawable.ic_place_school)
            "Gym" -> painterResource(id = R.drawable.ic_place_gym)
            "Library" -> painterResource(id = R.drawable.ic_place_library)
            "Local Park" -> painterResource(id = R.drawable.ic_place_park)
            else -> painterResource(id = R.drawable.ic_tab_places_filled)
        }

    PlacesListItem(
        name = stringResource(
            id = R.string.places_list_suggestion_add_your_place,
            suggestion
        ),
        icon = icon,
        isSuggestion = true,
        onClick = onClick
    )
}

@Composable
fun AddPlaceButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .motionClickEvent { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(AppTheme.colorScheme.primary, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = null,
                tint = AppTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(4.dp)
            )
        }

        Text(
            text = stringResource(id = R.string.places_list_add_new_place_btn),
            style = AppTheme.appTypography.subTitle1,
            color = AppTheme.colorScheme.primary,
            modifier = Modifier
                .padding(start = 10.dp)
                .weight(1f)
        )
    }

    Divider(
        thickness = 1.dp,
        modifier = Modifier.fillMaxWidth(),
        color = AppTheme.colorScheme.outline
    )
}
