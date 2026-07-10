package io.github.jmsdnns.androidmtrls.demo.pages.content

import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentEntry

/**
 * All Content-section entries in iOS catalog order.
 *
 * Order mirrors `ContentPages.entries` in iOSMtrls
 * `Demo/Sources/Catalog/Pages/Content.swift`:
 * Avatar, Timeline, Calendar, Tag, Badge, Divider, Labeled Content,
 * Progress, Remote Image, Backdrop Blur, Skeleton, Empty State.
 *
 * PageHeader and SectionHeader have no page in this section — the
 * AMPageHeader and AMSectionHeader components are showcased on the
 * Foundations Typography page instead.
 */
val contentEntries: List<ComponentEntry> = listOf(
    avatarEntry,
    timelineEntry,
    calendarEntry,
    tagEntry,
    badgeEntry,
    dividerEntry,
    labeledContentEntry,
    progressEntry,
    remoteImageEntry,
    backdropBlurEntry,
    skeletonEntry,
    emptyStateEntry,
)
