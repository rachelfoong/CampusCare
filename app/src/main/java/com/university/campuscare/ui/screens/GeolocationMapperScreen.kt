package com.university.campuscare.ui.screens

import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState


@Composable
fun GeolocationMapperScreen(
    initialLatLng: LatLng,
    onLocationConfirmed: (LatLng) -> Unit,
    onBack: () -> Unit
) {
    var pickedLocation by remember { mutableStateOf(initialLatLng) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialLatLng, 16f)
    }

    Box(Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            onMapClick = { latLng ->
                pickedLocation = latLng
            }
        ) {
            Marker(
                state = MarkerState(position = pickedLocation),
                title = "Selected Location"
            )
        }

        Button(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            shape = RoundedCornerShape(8.dp),
            onClick = {
                onLocationConfirmed(pickedLocation)
            }
        ) {
            Text("Confirm Location")
        }
    }
}

