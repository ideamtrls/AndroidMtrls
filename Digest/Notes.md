---
scope: Where plans, specs, scratch notes and the published screenshots' sources go
globs: []
keywords: [plan, spec, design, scratch, notes, writeup, briefs, spikes, artifacts, screenshots, Collection, Scenes, capture, walk, publish]
updated: 2026-11-10
---

# Notes

Where plans, specs, and scratch notes go.

`.notes/` holds **personal workflow scratch** and is gitignored. Artifacts produced while working go here, such as plans, specs, and exploratory writeups.

- Plans → `.notes/plans/YYYY-MM-DD-<slug>.md` — execution outlines for multi-step work.
- Specs → `.notes/specs/YYYY-MM-DD-<slug>.md` — designs before implementation.
- Briefs → `.notes/briefs/<slug>.md` — task briefs handed to agents, with the shared `rules.md`.
- Spikes → `.notes/spikes/` — throwaway experiments and the scripts and captures they need.
- Published screenshots → `.notes/artifacts/` — the sources of the two published pages, the Collection (`collection/`: `index.html` and one picture per catalog page, light above dark) and the Scenes (`scenes/`: `index.html` and every scene screen, light and dark), with the scripts that re-capture them on the emulator (`capture/collection.sh` and `capture/build` for the Collection, `walk/<scene>.sh light|dark` for the Scenes); its `README.md` has the steps, from capture to review and publish.
- Other → `.notes/scratch/`.

Temporary files from a build or a check (screenshots, logs) go in `.tmp/`, also gitignored, and are deleted when done.
