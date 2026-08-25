package com.riddle.diary.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.riddle.diary.R

val Parchment = Color(0xFFF3E6C8)
val ParchmentDark = Color(0xFFE4D2A8)
val ParchmentStain = Color(0xFFD9C49A)
val InkBlack = Color(0xFF1A120B)
val InkSepia = Color(0xFF3B2A1A)
val Leather = Color(0xFF2C1810)
val GoldFiligree = Color(0xFFB08D3A)
val BloodInk = Color(0xFF4A1515)
val SoftShadow = Color(0x331A0F08)

val CursiveFamily = FontFamily(Font(R.font.great_vibes))
val HandFamily = FontFamily(Font(R.font.caveat))

private val DiaryColors = darkColorScheme(
    primary = GoldFiligree,
    onPrimary = Leather,
    secondary = BloodInk,
    onSecondary = Parchment,
    background = Parchment,
    onBackground = InkBlack,
    surface = ParchmentDark,
    onSurface = InkSepia,
    outline = GoldFiligree.copy(alpha = 0.45f)
)

val DiaryTypography = androidx.compose.material3.Typography(
    displayLarge = TextStyle(
        fontFamily = CursiveFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 54.sp,
        color = InkBlack,
        lineHeight = 58.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = CursiveFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 36.sp,
        color = InkBlack
    ),
    titleLarge = TextStyle(
        fontFamily = HandFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 28.sp,
        color = InkSepia
    ),
    bodyLarge = TextStyle(
        fontFamily = HandFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        color = InkBlack,
        lineHeight = 32.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = HandFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 20.sp,
        color = InkSepia,
        lineHeight = 28.sp
    ),
    labelLarge = TextStyle(
        fontFamily = HandFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        color = GoldFiligree
    )
)

@Composable
fun DiaryTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DiaryColors,
        typography = DiaryTypography,
        content = content
    )
}
