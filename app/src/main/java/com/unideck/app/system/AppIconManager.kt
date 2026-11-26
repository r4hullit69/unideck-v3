package com.unideck.app.system

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.annotation.DrawableRes
import com.unideck.app.R

data class AppIconOption(
    val key: String,
    val label: String,
    val aliasClassName: String,
    @DrawableRes val bgDrawable: Int,
    @DrawableRes val fgDrawable: Int
)

object AppIconManager {

    val availableIcons: List<AppIconOption> = listOf(
        // Core Deck Icons
        AppIconOption(
            key = "dark",
            label = "Dark",
            aliasClassName = "com.unideck.app.IconDark",
            bgDrawable = R.drawable.ic_launcher_background,
            fgDrawable = R.drawable.ic_launcher_foreground
        ),
        AppIconOption(
            key = "scholar",
            label = "Scholar",
            aliasClassName = "com.unideck.app.IconScholar",
            bgDrawable = R.drawable.ic_bg_scholar,
            fgDrawable = R.drawable.ic_fg_scholar
        ),
        AppIconOption(
            key = "white",
            label = "White",
            aliasClassName = "com.unideck.app.IconWhite",
            bgDrawable = R.drawable.ic_bg_white,
            fgDrawable = R.drawable.ic_fg_white
        ),
        AppIconOption(
            key = "mono",
            label = "Mono",
            aliasClassName = "com.unideck.app.IconMono",
            bgDrawable = R.drawable.ic_bg_mono,
            fgDrawable = R.drawable.ic_fg_mono
        ),
        // Flat-Outlined Animal Icons (Matching Requested Grid)
        AppIconOption(
            key = "penguin",
            label = "Penguin",
            aliasClassName = "com.unideck.app.IconPenguin",
            bgDrawable = R.drawable.ic_bg_penguin,
            fgDrawable = R.drawable.ic_fg_penguin
        ),
        AppIconOption(
            key = "bee",
            label = "Bee",
            aliasClassName = "com.unideck.app.IconBee",
            bgDrawable = R.drawable.ic_bg_bee,
            fgDrawable = R.drawable.ic_fg_bee
        ),
        AppIconOption(
            key = "ladybug",
            label = "Ladybug",
            aliasClassName = "com.unideck.app.IconLadybug",
            bgDrawable = R.drawable.ic_bg_ladybug,
            fgDrawable = R.drawable.ic_fg_ladybug
        ),
        AppIconOption(
            key = "flamingo",
            label = "Flamingo",
            aliasClassName = "com.unideck.app.IconFlamingo",
            bgDrawable = R.drawable.ic_bg_flamingo,
            fgDrawable = R.drawable.ic_fg_flamingo
        ),
        AppIconOption(
            key = "giraffe",
            label = "Giraffe",
            aliasClassName = "com.unideck.app.IconGiraffe",
            bgDrawable = R.drawable.ic_bg_giraffe,
            fgDrawable = R.drawable.ic_fg_giraffe
        ),
        AppIconOption(
            key = "owl",
            label = "Owl",
            aliasClassName = "com.unideck.app.IconOwl",
            bgDrawable = R.drawable.ic_bg_owl,
            fgDrawable = R.drawable.ic_fg_owl
        ),
        AppIconOption(
            key = "panda",
            label = "Panda",
            aliasClassName = "com.unideck.app.IconPanda",
            bgDrawable = R.drawable.ic_bg_panda,
            fgDrawable = R.drawable.ic_fg_panda
        ),
        AppIconOption(
            key = "crocodile",
            label = "Crocodile",
            aliasClassName = "com.unideck.app.IconCrocodile",
            bgDrawable = R.drawable.ic_bg_crocodile,
            fgDrawable = R.drawable.ic_fg_crocodile
        ),
        AppIconOption(
            key = "turtle",
            label = "Turtle",
            aliasClassName = "com.unideck.app.IconTurtle",
            bgDrawable = R.drawable.ic_bg_turtle,
            fgDrawable = R.drawable.ic_fg_turtle
        ),
        AppIconOption(
            key = "whale",
            label = "Whale",
            aliasClassName = "com.unideck.app.IconWhale",
            bgDrawable = R.drawable.ic_bg_whale,
            fgDrawable = R.drawable.ic_fg_whale
        ),
        AppIconOption(
            key = "chameleon",
            label = "Chameleon",
            aliasClassName = "com.unideck.app.IconChameleon",
            bgDrawable = R.drawable.ic_bg_chameleon,
            fgDrawable = R.drawable.ic_fg_chameleon
        ),
        AppIconOption(
            key = "parrot",
            label = "Parrot",
            aliasClassName = "com.unideck.app.IconParrot",
            bgDrawable = R.drawable.ic_bg_parrot,
            fgDrawable = R.drawable.ic_fg_parrot
        ),
        AppIconOption(
            key = "pig",
            label = "Pig",
            aliasClassName = "com.unideck.app.IconPig",
            bgDrawable = R.drawable.ic_bg_pig,
            fgDrawable = R.drawable.ic_fg_pig
        ),
        AppIconOption(
            key = "mouse",
            label = "Mouse",
            aliasClassName = "com.unideck.app.IconMouse",
            bgDrawable = R.drawable.ic_bg_mouse,
            fgDrawable = R.drawable.ic_fg_mouse
        ),
        AppIconOption(
            key = "sheep",
            label = "Sheep",
            aliasClassName = "com.unideck.app.IconSheep",
            bgDrawable = R.drawable.ic_bg_sheep,
            fgDrawable = R.drawable.ic_fg_sheep
        ),
        AppIconOption(
            key = "clownfish",
            label = "Clownfish",
            aliasClassName = "com.unideck.app.IconClownfish",
            bgDrawable = R.drawable.ic_bg_clownfish,
            fgDrawable = R.drawable.ic_fg_clownfish
        ),
        AppIconOption(
            key = "llama",
            label = "Llama",
            aliasClassName = "com.unideck.app.IconLlama",
            bgDrawable = R.drawable.ic_bg_llama,
            fgDrawable = R.drawable.ic_fg_llama
        ),
        AppIconOption(
            key = "beetle",
            label = "Beetle",
            aliasClassName = "com.unideck.app.IconBeetle",
            bgDrawable = R.drawable.ic_bg_beetle,
            fgDrawable = R.drawable.ic_fg_beetle
        ),
        AppIconOption(
            key = "chicken",
            label = "Chicken",
            aliasClassName = "com.unideck.app.IconChicken",
            bgDrawable = R.drawable.ic_bg_chicken,
            fgDrawable = R.drawable.ic_fg_chicken
        ),
        AppIconOption(
            key = "spider",
            label = "Spider",
            aliasClassName = "com.unideck.app.IconSpider",
            bgDrawable = R.drawable.ic_bg_spider,
            fgDrawable = R.drawable.ic_fg_spider
        ),
        AppIconOption(
            key = "horse",
            label = "Horse",
            aliasClassName = "com.unideck.app.IconHorse",
            bgDrawable = R.drawable.ic_bg_horse,
            fgDrawable = R.drawable.ic_fg_horse
        ),
        AppIconOption(
            key = "bull",
            label = "Bull",
            aliasClassName = "com.unideck.app.IconBull",
            bgDrawable = R.drawable.ic_bg_bull,
            fgDrawable = R.drawable.ic_fg_bull
        ),
        AppIconOption(
            key = "platypus",
            label = "Platypus",
            aliasClassName = "com.unideck.app.IconPlatypus",
            bgDrawable = R.drawable.ic_bg_platypus,
            fgDrawable = R.drawable.ic_fg_platypus
        ),
        AppIconOption(
            key = "gorilla",
            label = "Gorilla",
            aliasClassName = "com.unideck.app.IconGorilla",
            bgDrawable = R.drawable.ic_bg_gorilla,
            fgDrawable = R.drawable.ic_fg_gorilla
        ),
        AppIconOption(
            key = "beaver",
            label = "Beaver",
            aliasClassName = "com.unideck.app.IconBeaver",
            bgDrawable = R.drawable.ic_bg_beaver,
            fgDrawable = R.drawable.ic_fg_beaver
        ),
        // Additional Classic Variants
        AppIconOption(
            key = "fox",
            label = "Fox",
            aliasClassName = "com.unideck.app.IconFox",
            bgDrawable = R.drawable.ic_bg_fox,
            fgDrawable = R.drawable.ic_fg_fox
        ),
        AppIconOption(
            key = "bear",
            label = "Bear",
            aliasClassName = "com.unideck.app.IconBear",
            bgDrawable = R.drawable.ic_bg_bear,
            fgDrawable = R.drawable.ic_fg_bear
        ),
        AppIconOption(
            key = "kitty",
            label = "Kitty",
            aliasClassName = "com.unideck.app.IconKitty",
            bgDrawable = R.drawable.ic_bg_kitty,
            fgDrawable = R.drawable.ic_fg_kitty
        ),
        AppIconOption(
            key = "doggo",
            label = "Doggo",
            aliasClassName = "com.unideck.app.IconDoggo",
            bgDrawable = R.drawable.ic_bg_doggo,
            fgDrawable = R.drawable.ic_fg_doggo
        ),
        AppIconOption(
            key = "bird",
            label = "Bird",
            aliasClassName = "com.unideck.app.IconBird",
            bgDrawable = R.drawable.ic_bg_bird,
            fgDrawable = R.drawable.ic_fg_bird
        ),
        AppIconOption(
            key = "glass",
            label = "Glass",
            aliasClassName = "com.unideck.app.IconGlass",
            bgDrawable = R.drawable.ic_bg_glass,
            fgDrawable = R.drawable.ic_fg_glass
        ),
        AppIconOption(
            key = "minimal",
            label = "Minimal",
            aliasClassName = "com.unideck.app.IconMinimal",
            bgDrawable = R.drawable.ic_bg_minimal,
            fgDrawable = R.drawable.ic_fg_minimal
        )
    )

    val allIcons: List<AppIconOption>
        get() = availableIcons

    fun switchIcon(context: Context, selectedKey: String) = applyAppIcon(context, selectedKey)

    fun applyAppIcon(context: Context, selectedKey: String) {
        val pm = context.packageManager
        val target = availableIcons.find { it.key == selectedKey } ?: availableIcons.first()

        // Enable target alias first, then disable all others so the launcher never loses an active entry
        val targetComponent = ComponentName(context, target.aliasClassName)
        try {
            pm.setComponentEnabledSetting(
                targetComponent,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
            availableIcons.filter { it.key != target.key }.forEach { option ->
                val comp = ComponentName(context, option.aliasClassName)
                pm.setComponentEnabledSetting(
                    comp,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                )
            }
        } catch (_: Exception) {
            // Safe fallback in test or preview environments
        }
    }
}
