package ai.nextgpu.agent.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


enum class AppThemeMode {
    Light, Dark, System
}

// ==========================================
// PRIMITIVES (Your existing definitions)
// ==========================================

// Modern color palette
val Primary01White = Color(255,255,255)
val Primary02Purple = Color(152, 112, 227)
val Primary03Black = Color(19, 19, 19)
val Accent01Lime = Color(214, 249, 76)
val Secondary01Mint = Color(212, 255, 183)
val Secondary02Lilac = Color(219,192,247)
val Secondary03LightGray = Color(248, 248, 248)
val  Secondary04DarkGray = Color(157, 162, 179)
val Secondary05LightPurple = Color(251, 250, 255)
val BackgroundGray = Color(248,248,248)
val BackgroundDarkGray = Color(30,31,32)
val HoverBackgroundDark = Color(54, 54, 54)
val HoverBackgroundLight = Color(238, 238, 238)
val StrokeGray = Color(225, 225, 225)
val StrokeLightGray = Color(50, 50, 50)
val UrlBlue = Color(0,122,255)
val StarGolden = Color(255, 191, 0)
val OpenclawRed = Color(232, 27, 37)

// Text Primitives
val PrimaryText01 = Color(0,0,0,230)
val PrimaryText02 = Color(0,0,0,175)
val SecondaryText01 = Color(79,79,79)
val SecondaryText02 = Color(0,0,0,81)

// Functional Colors
val InfoText = UrlBlue
val WarnText = Color(245,124,0)
val ErrorText = Color(229,57,53)
val CodeGreen = Color(56, 142, 60)

// Spacing values
val SpacingMicro = 2.dp
val SpacingTiny = 4.dp
val SpacingSmall = 8.dp
val SpacingMedium = 10.dp
val SpacingLarge = 16.dp
val SpacingExtraLarge = 24.dp
val SpacingHuge = 36.dp
val SpacingMassive = 48.dp
val SpacingColossal = 64.dp
val SpacingDialog = 20.dp

// Elevation values
val ElevationNone = 0.dp
val ElevationMicro = 1.dp
val ElevationSmall = 2.dp
val ElevationMedium = 4.dp
val ElevationLarge = 8.dp
val ElevationExtraLarge = 16.dp

// Border radius values
val RadiusSmall = 6.dp
val RadiusMedium = 12.dp
val RadiusLarge = 16.dp
val RadiusExtraLarge = 25.dp
val RadiusRound = 50.dp

val BorderWidth = 0.5.dp

// Icon Sizes
val IconSizeMicro = 12.dp
val IconSizeSmall = 16.dp
val IconSizeMedium = 20.dp
val IconSizeStandard = 24.dp
val IconSizeLarge = 32.dp

// Component Dimensions
val HeightButtonCompact = 32.dp
val HeightButtonStandard = 36.dp
val HeightInputMin = 40.dp
val HeightTopBar = 52.dp
val HeightFooter = 36.dp
val HeightPrompt = 160.dp

// Sidebar Specifics
val SidebarWidth = 270.dp
val RightSidebarWidth = 350.dp
val SidebarCollapsedWidth = 55.dp
val IconSizeSidebar = 18.dp
val HeightListItem = 40.dp

// Layout Limits
val MaxContentWidth = 900.dp

// Modern gradients
val PurpleBlueGradient = Brush.linearGradient(
    0.0f to Primary02Purple,
    1.0f to Secondary04DarkGray
)

// Define Font Families
val InterFontFamily = FontFamily(
    Font("fonts/Inter-Regular.ttf", FontWeight.Normal),
    Font("fonts/Inter-Medium.ttf", FontWeight.Medium),
    Font("fonts/Inter-SemiBold.ttf", FontWeight.SemiBold)
)

val ManropeFontFamily = FontFamily(
    Font("fonts/Manrope-Regular.ttf", FontWeight.Normal),
    Font("fonts/Manrope-Medium.ttf", FontWeight.Medium),
    Font("fonts/Manrope-SemiBold.ttf", FontWeight.SemiBold)
)

val JetBrainsMono = FontFamily(
    Font("fonts/JetBrainsMono-Regular.ttf", FontWeight.Normal),
    Font("fonts/JetBrainsMono-Medium.ttf", FontWeight.Medium)
)

// Define Typography
val NextGpuTypography = Typography(
    h1 = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 96.sp
    ),
    h2 = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 60.sp
    ),
    h3 = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 48.sp
    ),
    h4 = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 34.sp
    ),
    h5 = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 24.sp
    ),
    h6 = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp
    ),
    subtitle1 = TextStyle(
        fontFamily = ManropeFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp
    ),
    subtitle2 = TextStyle(
        fontFamily = ManropeFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp
    ),
    body1 = TextStyle(
        fontFamily = ManropeFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    ),
    body2 = TextStyle(
        fontFamily = ManropeFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp
    ),
    button = TextStyle(
        fontFamily = ManropeFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp
    ),
    caption = TextStyle(
        fontFamily = ManropeFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp
    ),
    overline = TextStyle(
        fontFamily = ManropeFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp
    )
)

// ==========================================
// EXTENSIBLE COLOR SYSTEM
// ==========================================

/**
 * Our custom color contract.
 * Add new semantic color slots here (e.g., sidebarBackground, success, warning).
 */
@Immutable
data class NextGpuColors(
    val primary: Color,
    val primaryVariant: Color,
    val secondary: Color,
    val secondaryVariant: Color,
    val hoverBackground: Color,
    val background: Color,
    val backgroundVariant: Color,
    val surface: Color,
    val textPrimary: Color,
    val textPrimaryVariant: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val error: Color,
    val success: Color,
    val border: Color,
    val isDark: Boolean
)

// Light Theme Definition
private val LightNextGpuColors = NextGpuColors(
    primary = Accent01Lime,
    primaryVariant = Primary02Purple,
    secondary = Secondary05LightPurple,
    secondaryVariant = Secondary01Mint,
    hoverBackground = HoverBackgroundLight,
    background = Primary01White,
    backgroundVariant = BackgroundGray,
    surface = Secondary03LightGray,
    textPrimary = PrimaryText01,
    textPrimaryVariant = PrimaryText02,
    textSecondary = SecondaryText01,
    textTertiary = SecondaryText02,
    error = ErrorText,
    success = CodeGreen,
    border = StrokeGray,
    isDark = false
)

// Dark Theme Definition
private val DarkNextGpuColors = NextGpuColors(
    primary = Accent01Lime,
    primaryVariant = Primary02Purple,
    secondary = Secondary05LightPurple,
    secondaryVariant = Secondary03LightGray,
    hoverBackground = HoverBackgroundDark,
    background = Primary03Black,
    backgroundVariant = BackgroundDarkGray,
    surface = Color(30, 30, 30), // Slightly lighter than black for cards
    textPrimary = Primary01White,
    textPrimaryVariant = PrimaryText02,
    textSecondary = Secondary04DarkGray,
    textTertiary = Color.Gray,
    error = ErrorText,
    success = Secondary01Mint,
    border = StrokeLightGray,
    isDark = true
)

// CompositionLocal to pass our colors down the tree
val LocalNextGpuColors = staticCompositionLocalOf {
    LightNextGpuColors
}

// ==========================================
// THEME ENTRY POINT
// ==========================================

@Composable
fun NextGpuTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Determine which color set to use
    val colors = if (darkTheme) DarkNextGpuColors else LightNextGpuColors

    // Bridge to Material Colors
    // This maps our custom colors to Material slots so standard components (Buttons, etc.) still work.
    val materialColors = if (darkTheme) {
        darkColors(
            primary = colors.primary,
            primaryVariant = colors.primaryVariant,
            secondary = colors.secondary,
            background = colors.background,
            surface = colors.surface,
            error = colors.error,
            onPrimary = Secondary03LightGray,
            onSecondary = Color.Black,
            onBackground = colors.textPrimary,
            onSurface = colors.textPrimary,
            onError = Color.Black
        )
    } else {
        lightColors(
            primary = colors.primary,
            primaryVariant = colors.primaryVariant,
            secondary = colors.secondary,
            background = colors.background,
            surface = colors.surface,
            error = colors.error,
            onPrimary = Color.White,
            onSecondary = Color.Black,
            onBackground = colors.textPrimary,
            onSurface = colors.textPrimary,
            onError = Color.White
        )
    }

    // Provide the custom colors via CompositionLocal
    CompositionLocalProvider(
        LocalNextGpuColors provides colors
    ) {
        MaterialTheme(
            colors = materialColors,
            typography = NextGpuTypography,
            shapes = Shapes(
                small = MaterialTheme.shapes.small,
                medium = MaterialTheme.shapes.medium,
                large = MaterialTheme.shapes.large
            ),
            content = content
        )
    }
}

/**
 * Accessor Object
 * Usage: NextGpuTheme.colors.textPrimary
 */
object NextGpuTheme {
    val colors: NextGpuColors
        @Composable
        get() = LocalNextGpuColors.current

    val typography: Typography
        @Composable
        get() = MaterialTheme.typography

    val shapes: Shapes
        @Composable
        get() = MaterialTheme.shapes
}
