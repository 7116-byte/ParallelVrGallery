v2.41

- Restored the v2.32-style source queue behavior for image VR preloading: All, Album, and Generated run as separate sources.
- Switching sources now auto-pauses other sources and only auto-resumes jobs that were paused by source switching.
- Generated page resumes only generated-source auto-paused jobs; failed jobs, manually paused jobs, and persisted PAUSED jobs are not resumed by entering Generated.
- Viewer browsing and preloading stay scoped to the page that opened the viewer: All order, current Album order, or selected generated version.
- Adjusted the top bar so content starts below it and the bar becomes translucent only after scrolled content overlaps the top area.
