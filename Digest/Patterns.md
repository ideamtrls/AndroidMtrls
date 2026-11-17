---
scope: The framework's opinions on composing a screen from existing parts — the two layers and the one surface setting, floating surfaces, color under cards and text over art, the status bar, the remote, the spotlight and pickers, sheets without dimming, folding, quiet defaults
globs: []
keywords: [pattern, opinion, taste, composition, recipe, style layer, native layer, where the line falls, stateless surfaces, native controls, floating surfaces, page color, two-layer shadow, AMElevation, hairline, selection ring, nesting, flat card, backdrop layer, surface layer, surface setting, AMTheme(surfaceFill), AMSurfaceFill, solid, frosted, always solid, the tint, embedded, quiet chip, container colors, frosted sheet, color under cards, row-level color, amArtRow, amArtRows, amArtBackdrop, branded backdrop, amArtText, art tone, AMArtTone, AMBackdrop, AMArtRowsBackdrop, readable text over art, secondaryText, ProvideAMOnSurface, light mode, dark mode, AMTheme.Surface.page, status bar, amStatusBarEdge, AMStatusBarIcons, empty state, carousel controls, card stack controls, remote, main navigation, no bottom bar, floating action button, AMActionGrid, tiles, shortcut, AMScene, AMFloatingActions, AMKeepsPlace, overlays slot, amInstantSwitch, landing view, back order, button shape, shapes.medium, spotlight, press and hold, amHoldable, AMSpotlightState, picker, date picker, menu picker, blur-behind, wash, the remote's scrim, sheets, no dimming, transparent scrim, discard guard, alert, folding, AMDisclosureGroup, quiet defaults]
updated: 2026-11-16
---

# Patterns

How AndroidMtrls wants a screen put together. A pattern is an opinion over existing parts: when you build an app, follow these before inventing a layout.

## The two layers

- **The native layer is Android's.** Jetpack and Material 3 components keep their own shapes, outlines, type, motion and behavior, so an AndroidMtrls app looks and feels like Android: the Material type scale in the phone's font, ripple, Android gestures, Android back.
- **The style layer goes on top.** Floating surfaces on a tuned page, two-layer shadows and hairlines, cards, the remote as main navigation, the spotlight for holds and pickers, no dimming behind sheets, color under cards, frosted surfaces over art, folding, quiet defaults, and one setting for how the surfaces on the backdrop are made (solid or frosted).
- **Where the line falls: stateless surfaces wear the style; native controls keep Material's shapes, outlines and behavior and take the model's container colors.** Cards, buttons, tiles, the floating action button and the banner get the floating fill, the shadow and the hairline; sheets get a frosted background. Text fields, pickers, chips and segmented buttons keep Material's shapes and outlines, and their containers follow the app's surface setting (below; each control's rule is in `Inputs.md` and `Selection.md`). A selected chip takes `primary`. Switches, sliders and stepper buttons keep their own colors.
- **Text and accent colors are native**, from the static Material color scheme. Corners are Android's circular corners. **Buttons are rounded rectangles** in the theme's medium shape (12 dp by default), filled or text, surface and ripple alike, never pills; the floating action button stays a circle and icon buttons keep Material's round ripple. Type is sentence case everywhere.
- **When in doubt, lean Android.**

## Backdrop and surfaces

The screen is two layers, and one setting.

- **The backdrop layer** is the app's backdrop (its art or branded picture, or the page color), a sheet's background, and the blurred screen behind the remote and the spotlight. It is always frosty, and the setting never changes it.
- **The surface layer** is everything on it: cards, sections, tabs, buttons, tiles, the floating action button, the toolbar chips, fields, chips, tags, the banner and the band behind it, the toast and the alert.
- **The setting is `AMTheme(surfaceFill = …)`**, `Solid` (the library default) or `Frosted`, set once at the app's root; a nested `AMTheme(surfaceFill = …)` sets it for a subtree. In a frosted app, pieces on the backdrop are frosted and pieces embedded in a frosted surface draw the tint. In a solid app, pieces are the solid white or black surface, and unselected chips take the quiet fill, an opaque gray one step off the page. The remote, the + that opens it included, is solid in either setting (below).
- **The scene's banners lie on the art showing.** In a frosted app they (the band behind a banner included) frost the backdrop around the scene, or in a scene with none the showing root's art, or with no art at all they're frost over the page color (`Navigation.md`, `Feedback.md`). The toast and the alert, Material pieces that follow by container color alone, keep Material's container in a solid app and are frost over the page, with the frosted edge, in a frosted one, never the tint.

## Floating surfaces

- **Pure white in light mode, pure black in dark mode in a solid app, on a tuned page.** The page is what's tuned, not the surfaces: shadows cast onto it, and the dark page sits far enough above black that black surfaces read as floating. Floating surfaces have no tonal elevation; they lift by shadow, never by tint.
- **Two-layer shadow, cast by the shape, never by its content**: a tight near layer plus a soft, wide far layer. Pick the level (`AMElevation`) by what the surface is: `Low` is the default lift (cards, filled buttons, tiles); `None` keeps fill and shape and casts nothing, for dense tile grids and horizontal card bands, where per-item shadows stack into noise; `Medium` is a sheet, which sits higher than a card; `Text` is for glyphs over art and blur. Shadows run much stronger in dark mode, so they register on the dark page.
- **Hairlines.** On solid cards the hairline shows in dark mode only, where the shadow alone can't separate black from the page; frosted surfaces, sheets, tiles, the floating action button, the banner and avatars always carry one (on frost it's the light frosted edge). A solid control inside a black card or on a sheet takes a brighter outline in dark mode, so it still reads as raised.
- **The selection ring** marks a chosen row or tile, asymmetric on purpose (soft on white, bright enough to define black on the dark page). Chosen reads as a quiet hint, not a border that fights the card.
- **Nesting.** A card inside a card demotes to flat: no fill, corners, shadow or hairline of its own. Shadows never stack and padding never doubles.
- The values (the page colors, the shadow table, the hairline and ring widths) are in `Foundations.md`.

## Color under cards

Any view with cards sits on heavily blurred pictures relevant to the app. The pictures supply the color variation.

- **Color lives at the row level.** Each row has its own picture behind it, and that picture scrolls with the row (`amArtRow` / `amArtRows`). Nothing behind the page swaps as you scroll.
- **Colors blur into each other.** There's never a visible line between two rows' colors, so the pictures are laid out as one layer and blurred together.
- **The setting decides the cards.** A solid app's cards, tabs and page buttons are the solid surface; a frosted app's are frost over the backdrop. Frost is a pure Gaussian blur of what's behind under the surface color, translucent, with a light edge. Where there's no art to blur, a frosted surface is frost over the page color (the page under the fill, opaque), never unblurred translucency, because sharp art under translucent text fails contrast. A piece embedded in a frosted surface (a card on a sheet, a button in a card) draws the tint instead: the surface color, translucent, with no blur of its own, so nothing frosts through frost. A card inside a card stays flat.
- **Back over art, the picker cards and a held item follow the setting too:** solid in a solid app, frosted in a frosted one. The picker cards and a held item sit in a blurred window, so their frost lies over the window's own blur, and they draw solid where the system has cross-window blur off, never see-through. In a frosted app a reorder's lifted row is frosted even inside a card or on a sheet, never the tint (`Lists.md`).
- **Buttons on an overlay are never transparent.** The + that opens the remote, the remote's tiles and any card or button it holds, and the spotlight's tiles under a held item are solid in either setting, white in light mode and black in dark, so what you can do is always easy to see.
- **Surfaces don't mix.** An app's surfaces are all solid or all frosted, but for the buttons on an overlay, which are always solid (above). Native controls inside them take the model's container colors, not a color of their own.
- **Empty states sit on the page's own background**, never on a card: the glyph, title, description and actions lie on the raw app background (the page color or the app's backdrop), and only their buttons float.
- **Controls under a carousel or card stack get no card.** Their buttons are flat (`AMElevation.None`), set a little apart from the deck, so nothing pulls focus from the swipe, and the page indicator lies on the page.
- **It needs rows.** A screen built as a grid has no rows to carry color and doesn't use this pattern.
- **An app can use one branded backdrop instead**, the same picture or gradient behind every screen, so it always looks like itself.
- **A picture as a whole app's background** goes through `amArtBackdrop` too, washed toward the mode like all art, with adjustable blur. A heavy blur makes a picture read as its colors; a light one lets its subject show.
- **Rows go inside a card** on these views. Full-width flat rows would cut the color into strips.
- **Views with no pictures of their own still get color.** Decoration is not a substitute: use real pictures, never drawn shapes. Blurred backdrops are decoded small, blurred once and cached.
- **Text lying on art is art text** (`amArtText`): a fixed ink by the art's tone (white on dark art, black on light art), the secondary ink for supporting lines, with a two-layer shadow of the other tone drawn from the glyphs; headers over a backdrop carry the text shadow too.

## Readable text over art

Art follows the mode, and text reads against the art.

- **The UI follows the system's mode entirely.** Cards, buttons, tiles, sheets, the spotlight, controls and the status bar's icons are all light in light mode and all dark in dark mode. Nothing forces part of a screen into the other mode.
- **Art follows the mode too.** The library washes every picture it draws as art (`amArtBackdrop`, art rows) toward the mode, a frost: every pixel mixed evenly toward white in light mode or toward black in dark mode, then saturated again to win back color. Light mode looks light, dark mode dark, and the art keeps its color.
- **Text on art reads in fixed inks per tone, in either mode.** At the default washes (`AMArtDefaults`) the inks hold over any art, where two bands blend too. The inks are fixed, so they never depend on the color scheme. The washes, the inks and their contrast are in `DataDisplay.md`.
- **The art's tone is the mode's**, unless an art surface (`AMBackdrop`, `AMArtRowsBackdrop`) is given one: an app that washes its art less, or shows art the library doesn't draw, says how light it is (`AMArtTone`), and everything on that surface follows that tone: art text turns white on dark art and black on light art, in either mode. Pictures are never measured.
- **Everything that lies on art reads the tone itself**, with no caller code: art text, section headers and footers, empty states, a ghost button's label (a destructive one in a red moved to hold 4.5:1 on the tone), a page header's, toolbar's and art panel's lines at rest (the bar's own colors once its frosted fill comes in), and the spotlight's title over its washed screen.
- **Text on frosted and tinted surfaces follows the mode.** Frost and the tint hold their contrast over the library's washed art; over art an app chose not to wash, the frost is the app's to check.
- **Secondary text comes from the surface it sits on**, never passed by hand: on the page or a solid card the platform's secondary color, on a sheet or on a frosted or tinted surface the frosted secondary, on art the secondary ink of its tone (`AMTheme.secondaryText`). The library's containers say their content lies on a surface; a hand-built card says so once with `ProvideAMOnSurface`, and the app's setting (`AMTheme(surfaceFill = …)`) says which fill that is.
- **No art means the page color.** Where there's no art (no picture yet, no rows, between a band's pictures, a page transition), the background is the mode's page color (`AMTheme.Surface.page`), never a fixed dark color, and a band keeps its last picture until its new one is ready.

## Under the status bar

Edge to edge, content runs behind the status bar, and scrolled content never reads through it. A screen with a top bar gets that from the bar's fill. A scrolling view with no top bar (a header that scrolls away, a list that opens on its first section) puts `Modifier.amStatusBarEdge()` on its scrolling container, so its content fades out under the bar and the page or the art behind shows through. The edge adds no inset: the container still pads the status bar itself, so nothing changes at rest.

**The icons follow what's under them.** On the library's art, washed toward the mode, they're the mode's, as everywhere. An art surface given the other tone (`AMBackdrop`, `AMArtRowsBackdrop`) turns them over while it reaches the top of the screen, until a bar's frosted fill covers them; content the library doesn't draw (a raw photo, video) says so with `AMStatusBarIcons(tone)`. Art panels and scenes never force light icons. How the claims combine is in `Navigation.md`.

## The remote

The actions overlay (the floating action button and its grid of tiles) IS the app's main navigation, like a TV remote with big buttons for the views and actions the app needs. There's no bottom bar: an app has as many views as it needs, not as many as fit on a bar.

- **The button**: a movable circle with a +, centered at the bottom above the navigation bar, solid in either surface setting (white in light mode, black in dark, with a hairline in both), so it's always easy to see. A long press lifts it to move it.
- **The grid**: it opens over a blurred screen (a window with the platform's blur-behind; the blur bare in light mode and dimmed by a black scrim in dark mode, since the tiles bring their own solid surfaces) as rows of big tiles, exactly 2, 3 or 4 per row, low where a thumb reaches them. The current view's tile is ringed. Sizes are in `Actions.md`.
- **It sits on top and goes away.** Opening and closing it changes nothing underneath.
- **A view tile switches to that view and closes the remote.** Every view's place is saved when you leave it and restored when you come back, page and scroll position included, the Android way: saved state, not a view kept running. Each root view's back stack is the app's own state.
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

The remote for one specific thing. Holding an item blurs the screen and raises a card of it, low on the screen where the remote's tiles sit, with a row of up to four tiles of things to do with it below.

- **The library owns it**: `Modifier.amHoldable` is the hold (the system's long-press timeout) and its haptic; `AMSpotlight` is the presentation. The held item comes up on one card in the app's setting, whatever it was on the screen (a frosted card on a backdrop, a translucent row over art); its tiles below are always solid. Over the blurred, washed screen the thing in focus is the clearest thing there.
- **It's a window** with the platform's blur-behind, so it sits above sheets. The screen blurs under the mode's wash, so the paused screen follows the mode and the spotlight's title holds over any screen, sharp art included. Where the system has cross-window blur off (battery saver, some devices), the wash alone stills the screen. The blur and the wash are in `Overlays.md`.
- A tap still does the item's main thing, and a drag that starts on it still scrolls.
- TalkBack users get the spotlight's tiles directly on the item, as custom actions, from the same definition (`AMSpotlightState`, or the hold's `actions`), with no second list to keep in step. On a row that also swipes or reorders, all of it is one list: the hold's tiles, then the moves, then the swipes.
- Nothing underneath changes unless you pick a tile.
- A visible button opens it only where people wouldn't otherwise find it.

**Every picker works the same way**: the same spotlight, opened by a tap instead of a hold, with a title above the card (a field needs naming where a held item doesn't) and no tiles. Nothing raises the keyboard. A date or time picker applies changes as you make them and stays up until you tap away; a menu applies the option you pick and closes. What each one shows is in `Inputs.md` (`AMDatePicker`) and `Selection.md` (`AMMenuPicker`).

## Sheets — no dimming behind them

- A sheet is Material's `ModalBottomSheet` wearing the style: a frosted background in either setting, the screen under it blurred so its cards and text show through as soft shapes (what it holds follows the setting: solid pieces in a solid app, the tint in a frosted one), Material's sheet shape with its rounded top corners, the frosted edge and the `Medium` shadow, over a **transparent scrim**. The screen behind stays as it is.
- It keeps Android's behavior: the drag handle, drag to dismiss, predictive back.
- **The discard guard**: while a sheet holds edits, dismissing it (a drag, back, a tap outside) asks "Discard changes?" first.
- **Alerts keep Android's shape, motion and dim**: an alert is Material's `AlertDialog`, its container in the app's surface setting (frost over the page color, outlined by the frosted edge, in a frosted app). Back and a tap outside dismiss it, running its cancel action if it has one; dismissing never runs a destructive action.

## Folding

Secondary content folds into single rows that open in place, so you never leave what you were looking at: `AMDisclosureGroup`, a card that expands. Any page can use it.

## Quiet defaults

- Autofocus is off.
- Overlays (banners, toasts, sheets, the remote, the spotlight) appear only when state says so.
- Nothing moves on its own.

## See also
- `Foundations.md` — the values behind floating surfaces, frost and the tint, and the surface setting's mechanics.
- `Navigation.md` — `AMScene`, `AMKeepsPlace` and the status bar's icons, under the remote recipe.
- `Overlays.md` — the windows, blur and wash under the remote, the spotlight, sheets and alerts.
- `DataDisplay.md` — the art recipe: washes, inks, art rows and art tone.
