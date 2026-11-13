---
scope: The framework's opinions about how to compose its parts into a screen — the two layers, floating surfaces, color under cards, the remote, the spotlight and press and hold, sheets without dimming, folding, quiet defaults. Patterns store taste; they are recipes over existing components, not new widgets.
globs: []
keywords: [pattern, patterns, opinion, taste, composition, style layer, native layer, where the line falls, stateless surfaces, stateful controls, floating surfaces, page color, two-layer shadow, dropShadow, hairline, selection ring, nesting, flat card, solid controls, frosted, frosted sheet, color under cards, row-level backdrop, amArtRow, amArtRows, amPageBackdrop, amArtText, readable text over art, light mode, dark mode, appearance, AMTheme.Surface.page, AMTheme.Surface.art, status bar, amStatusBarEdge, AMLightStatusBarIcons, empty state, carousel controls, remote, main navigation, bottom bar, floating action button, action grid, tiles, AMScene, AMFloatingActions, AMKeepsPlace, scene-wide overlays, amInstantSwitch, keeps place, landing view, back order, button shape, shapes.medium, spotlight, press and hold, amHoldable, picker, date picker, menu picker, blur-behind, dim fallback, sheets, no dimming, transparent scrim, discard guard, alert, folding, AMDisclosureGroup, quiet defaults]
updated: 2026-11-11
---

# Patterns

How AndroidMtrls wants a screen put together. A pattern is an opinion over existing parts: when you build an app, follow these before inventing a layout.

## The two layers

- **The native layer is Android's.** Jetpack and Material 3 components keep their own shapes, colors, type, motion and behavior, so an AndroidMtrls app looks and feels like Android: the Material type scale in the phone's font, ripple, Android gestures, Android back.
- **The style layer goes on top.** Floating white/black surfaces on a tuned page, two-layer shadows and hairlines, cards, the remote as main navigation, the spotlight for holds and pickers, no dimming behind sheets, color under cards, frosted surfaces over art, folding, quiet defaults.
- **Where the line falls: stateless surfaces wear the style; stateful controls keep Android's look.** Cards, buttons, sheets, tiles, the floating action button and the banner get the white/black fill, the shadow and the hairline. Switches, sliders, chips, segmented buttons, text fields, checks and pickers keep Material's shapes and colors untouched; nothing recolors a native control.
- **Text and accent colors are native**, from the static Material color scheme. Corners are Android's circular corners. **Buttons are rounded rectangles** in the theme's medium shape (12 dp), filled or text, surface and ripple alike, never pills; the floating action button stays a circle and icon buttons keep Material's round ripple. Type is sentence case everywhere.
- **When in doubt, lean Android.**

## Floating surfaces

- **Pure white in light mode, pure black in dark mode, on a tuned page** (`#F2F2F7` light, `#141416` dark). The page is what's tuned, not the surfaces: shadows cast onto it, and the dark page sits far enough above black that black surfaces read as floating. Floating surfaces have no tonal elevation; they lift by shadow, never by tint.
- **Two-layer shadow, cast by the shape, never by its content**: a tight near layer plus a soft, wide far layer, as two `dropShadow`s on the surface's shape. Levels (`AMElevation`): `None` (keeps fill and shape, casts nothing: dense tile grids, horizontal card bands), `Low` (the default: cards, filled buttons, tiles, the toast), `Medium` (a sheet, which sits higher than a card), `Text` (glyph legibility over art and blur). Shadows run much stronger in dark mode so they register on the dark page.
- **Hairlines.** A 0.5 dp outline, drawn inset and never thinner than one pixel. On solid cards it shows in dark mode only, where the shadow alone can't separate black from the page; sheets, tiles, the floating action button, the banner and avatars always carry it. A solid control inside a black card or sheet takes a brighter outline in dark mode, so it still reads as raised.
- **The selection ring**: 1.5 dp, asymmetric on purpose (soft on white, bright enough to define black on the dark page), for a chosen row or tile. Chosen reads as a quiet hint, not a border that fights the card.
- **Nesting.** A card inside a card demotes to flat: no fill, corners, shadow or hairline of its own. Shadows never stack and padding never doubles.

## Color under cards

Any view with cards sits on heavily blurred pictures relevant to the app. The pictures supply the color variation.

- **Color lives at the row level.** Each row has its own picture behind it, and that picture scrolls with the row (`amArtRow` / `amArtRows`). Nothing behind the page swaps as you scroll.
- **Colors blur into each other.** There's never a visible line between two rows' colors, so the pictures are laid out as one layer and blurred together.
- **Solid or frosted cards are each app's choice**, not part of the pattern. Frosted is a pure Gaussian blur of what's behind under the surface color, translucent (white 0.6 light, black 0.5 dark), with a light edge. Where blur isn't available a frosted surface falls back to solid, never to unblurred translucency, because sharp art under translucent text fails contrast.
- **Crucial controls are always solid, so they're always obvious:** the +, back over art, the remote's tiles, and a held item, over any backdrop in any app.
- **Everything else on a page is one or the other.** An app picks solid or frosted for its cards, tabs and page buttons, and doesn't mix them.
- **Empty states sit on the page's own background**, never on a card: the glyph, title, description and actions lie on the raw app background (the page color or the app's backdrop), and only their buttons float.
- **Controls under a carousel or card stack get no card.** Their buttons are flat (`AMElevation.None`), set a little apart from the deck, so nothing pulls focus from the swipe, and the page indicator lies on the page.
- **Sheets follow the app.** An app with frosted cards presents frosted sheets; a solid app's sheets are solid. What a sheet holds is always solid, frosted sheet or not: its rows sit in a solid card, and its buttons and fields are solid.
- **It needs rows.** A screen built as a grid has no rows to carry color and doesn't use this pattern.
- **An app can use one branded backdrop instead**, the same picture or gradient behind every screen, so it always looks like itself. The solid-control rule still applies.
- **A picture as a whole app's background** goes through `amPageBackdrop`, lighter in light mode and darker in dark mode, with adjustable blur. A heavy blur makes a picture read as its colors; a light one lets its subject show.
- **Rows go inside a card** on these views. Full-width flat rows would cut the color into strips.
- **Views with no pictures of their own still get color.** Decoration is not a substitute: use real pictures, never drawn shapes. Blurred backdrops are decoded small, blurred once and cached.
- **Text on darkened pictures is white art text** (`amArtText`), and art text and headers over a backdrop carry the two-layer text shadow (the text drawn twice).
- **The status bar's icons turn light over dark art**: an art panel's bar does it at rest, and any other view whose top lies on dark art (a header at rest on its art rows, a full-screen player) says so with `AMLightStatusBarIcons`. The claims count together, so the icons follow whatever is on top.

## Readable text over art

A background generated from row colors can be light or dark in either mode, so the text on it varies with the art, not the mode.

- **The UI follows the system's mode entirely.** Cards, buttons, tiles, sheets, the spotlight, controls and the status bar's icons are all light in light mode and all dark in dark mode. Nothing forces part of a screen into the other mode.
- **Backgrounds vary.** A background generated from row colors (art rows) takes its lightness from the art, so it can be light or dark in either mode.
- **Text adapts.** Text that lies right on that background, not on a card, uses light or dark text, whichever reads against the art behind it, so it can differ from the mode. Text on cards follows the mode.
- **No art means the page color.** Where there's no art (an empty state, art still loading, a page transition), the background is the mode's page color (`AMTheme.Surface.page`), never a fixed dark color.
- **Where the library stands.** `Modifier.amArtText()` is white only. Art rows and the art backdrop wash the art with black in both modes, and `AMTheme.Surface.art` (`#333333`) shows until a picture arrives. That reads over dark art and fails over light art: a mostly white cover in light mode leaves white text unreadable. `AMLightStatusBarIcons` turns the status bar's icons light over dark art in light mode too. `Modifier.amPageBackdrop` follows the mode, a light background in light mode and a dark one in dark mode.

## Under the status bar

Edge to edge, content runs behind the status bar, and scrolled content never reads through it. A screen with a top bar gets that from the bar's fill. A scrolling view with no top bar (a header that scrolls away, a list that opens on its first section) puts `Modifier.amStatusBarEdge()` on its scrolling container, so its content fades out under the bar and the page or the art behind shows through; it still pads the status bar, so nothing changes at rest.

## The remote

The actions overlay (the floating action button and its grid of tiles) IS the app's main navigation, like a TV remote with big buttons for the views and actions the app needs. There's no bottom bar: an app has as many views as it needs, not as many as fit on a bar.

- **The button**: a movable 56 dp circle with a +, in the surface color, centered at the bottom above the navigation bar. A long press lifts it to move it.
- **The grid**: it opens over a blurred screen (a window with the platform's blur-behind) as rows of 76 dp tiles, exactly 2, 3 or 4 per row. The current view's tile is ringed.
- **It sits on top and goes away.** Opening and closing it changes nothing underneath.
- **A view tile switches to that view and closes the remote.** Every view's place is saved and restored when you leave it, page and scroll position included, the Android way: saved state, not a view kept running. Each root view's back stack is the app's own state.
- **Views and actions share the remote.** It can hold live controls that work without closing it, in the same place they sit on the page.
- **The same buttons stay in the same places** wherever you are.
- **A page may carry a shortcut to a view** as long as the remote reaches that view too. The shortcut pushes the view as a panel whose back returns to where it was pressed; the remote's tile swaps the root instead.
- **Back order**: the topmost window first, so an alert, then the spotlight or the remote, then a sheet, then the panel stack, then the landing view (an instant swap back to it), then out of the app.

**The recipe.** Every scene's root is `AMScene`; the remote is `AMFloatingActions` in its `floatingActions` slot; the menu is an `AMActionGrid` of `AMActionGridRow`s of `AMActionGridButton`s; the views are the roots of `AMScene`'s roots form (an `AMKeepsPlace`); what the whole scene presents (its sheets, alerts, spotlight, loading overlay and toasts) goes in that form's `overlays` slot, beside the roots and never inside one. A scene on one backdrop wraps the whole `AMScene` in its `AMBackdrop` and passes `containerColor = Color.Transparent`.

```kotlin
enum class View { Inbox, Archive }

var view by rememberSaveable { mutableStateOf(View.Inbox) }
var showsRemote by rememberSaveable { mutableStateOf(false) }
var toast by remember { mutableStateOf<AMToastItem?>(null) }
fun open(target: View) {
    showsRemote = false                    // the remote closes outside the switch, so its fade plays over the new view
    amInstantSwitch { view = target }      // the new root appears at once, restored where it was left
}

AMScene(
    current = view,
    landing = View.Inbox,
    onCurrentChange = { view = it },
    floatingActions = {
        AMFloatingActions(showsRemote, onExpandedChange = { showsRemote = it }) {
            AMActionGrid {
                AMActionGridRow(                                   // actions: each closes the remote, then acts
                    { AMActionGridButton(composeIcon, "Compose", onClick = { showsRemote = false; compose() }) },
                    { AMActionGridButton(searchIcon, "Search", onClick = { showsRemote = false; search() }) },
                )
                AMActionGridRow(                                   // views: the current one ringed
                    { AMActionGridButton(inboxIcon, "Inbox", onClick = { open(View.Inbox) }, active = view == View.Inbox) },
                    { AMActionGridButton(archiveIcon, "Archive", onClick = { open(View.Archive) }, active = view == View.Archive) },
                )
            }
        }
    },
    overlays = { AMToast(toast, onDismissRequest = { toast = null }) },   // up whichever view is showing
    backStack = { root -> if (root == View.Inbox) inbox else emptyList() },
) { root ->
    when (root) {
        View.Inbox -> NavDisplay(inbox, onBack = { inbox.removeLastOrNull() }) { … }   // its panels are its own back stack
        View.Archive -> ArchiveView()
    }
}
```

- The scene fades the button while any overlay inside it is up (its own menu, a sheet, an alert, the spotlight, the loading overlay), and shows the scene's toasts above the button.
- A live control (a play button) in the menu acts without setting `showsRemote` false.
- Back needs no code: the windows, the root's `NavDisplay` and the scene's landing step take it in order.

## Press and hold — the spotlight

The remote for one specific thing. Holding an item blurs the screen and raises a solid card of it, low on the screen where the remote's tiles sit, with 2 or 3 tiles of things to do with it below.

- **The library owns it**: `Modifier.amHoldable` is the hold (the system's long-press timeout) and its haptic; `AMSpotlight` is the presentation. The held item always comes up solid, whatever it was on the screen (a frosted card on a backdrop, a translucent row over art), so the thing in focus is the clearest thing on the screen.
- **It's a window** with the platform's blur-behind, so it sits above sheets. A blur of 34 dp (Android's radius for a Gaussian sigma of 20), with a 0.4 black dim in dark mode only. Where the system has cross-window blur off (battery saver, some devices), it falls back to a dim, the platform's own behavior.
- A tap still does the item's main thing, and a vertical drag that starts on it still scrolls.
- The hold gives the long-press haptic. TalkBack users get the same actions directly on the item, as custom actions.
- Nothing underneath changes unless you pick a tile.
- A visible button opens it only where people wouldn't otherwise find it.

**Every picker works the same way**: the same spotlight, opened by a tap instead of a hold, with a title above the card (a field needs naming where a held item doesn't) and no tiles. A date opens Material's calendar (without its header), with a row under it that switches to the clock dial when the field holds a time too, or the dial alone for a time-only field; nothing raises the keyboard; a menu row opens its options as list items with radio buttons, the current one chosen. Dates stay open until you tap away, and changes apply as you make them; picking a menu option closes it.

## Sheets — no dimming behind them

- A sheet is Material's `ModalBottomSheet` wearing the style: the floating surface (or the page color, or frosted in a frosted app; its contents stay solid), Material's sheet shape with its rounded top corners, the hairline and the `medium` shadow, over a **transparent scrim**. The screen behind stays as it is.
- It keeps Android's behavior: the drag handle, drag to dismiss, predictive back.
- **The discard guard**: while a sheet holds edits, dismissing it (a drag, back, a tap outside) asks "Discard changes?" first.
- **Alerts keep Android's look and dim**: an alert is Material's `AlertDialog`. Back and a tap outside dismiss it, running its cancel action if it has one; dismissing never runs a destructive action.

## Folding

Secondary content folds into single rows that open in place, so you never leave what you were looking at: `AMDisclosureGroup`, a card that expands. Any page can use it.

## Quiet defaults

- Autofocus is off.
- Overlays (banners, toasts, sheets, the remote, the spotlight) appear only when state says so.
- Nothing moves on its own.
