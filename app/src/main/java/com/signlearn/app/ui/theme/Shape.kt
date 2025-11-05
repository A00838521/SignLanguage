package com.signlearn.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val Shapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(12.dp),
    extraLarge = RoundedCornerShape(16.dp),
)

object SignLearnShapes {
    val PhoneContainer = RoundedCornerShape(40.dp)
    val PhoneNotch = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
    val CardElevated = RoundedCornerShape(12.dp)
    val Circle = RoundedCornerShape(50)
    val Pill = RoundedCornerShape(100.dp)
    val BottomBar = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    val TopBar = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
    val CameraOverlay = RoundedCornerShape(8.dp)
    val ProgressCard = RoundedCornerShape(16.dp)
    val AchievementBadge = RoundedCornerShape(12.dp)
    val LessonCard = RoundedCornerShape(12.dp)
    val DictionaryCard = RoundedCornerShape(10.dp)
    val CategoryButton = RoundedCornerShape(8.dp)
    val VideoContainer = RoundedCornerShape(12.dp)
    val ChartCard = RoundedCornerShape(16.dp)
    val InputField = RoundedCornerShape(8.dp)
    val Dialog = RoundedCornerShape(24.dp)
    val ExtendedFab = RoundedCornerShape(16.dp)
    val None = RoundedCornerShape(0.dp)
    val BottomSheet = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 0.dp, bottomEnd = 0.dp)
    val StickyHeader = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
}

object BorderWidths { val Thin = 1.dp; val Medium = 2.dp; val Thick = 4.dp; val ExtraThick = 8.dp }
object Elevations { val None = 0.dp; val ExtraSmall = 1.dp; val Small = 2.dp; val Medium = 4.dp; val Large = 8.dp; val ExtraLarge = 16.dp }
