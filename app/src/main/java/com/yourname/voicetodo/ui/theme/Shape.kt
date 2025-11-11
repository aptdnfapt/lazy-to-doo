package com.yourname.voicetodo.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val Shapes = Shapes(
    // Matching HTML rounded corners
    extraSmall = RoundedCornerShape(4.dp), // default
    small = RoundedCornerShape(8.dp),      // lg
    medium = RoundedCornerShape(12.dp),    // xl
    large = RoundedCornerShape(50.dp),     // full
    extraLarge = RoundedCornerShape(16.dp)
)