v2.40

- Restored album/all viewer scope as an ordered list, so swiping and image VR preloading follow the visible page order.
- Fixed album preloading to use the current album's before/after order instead of falling back to global photo order.
- Remembered each generated version detail grid position when returning from the viewer and reopening that version.
- Changed the gallery top bar from fixed translucency to scroll-driven translucency: it is more solid at the top and becomes transparent as content slides underneath.
- Kept generated manager queue isolation, GPU behavior, model downloads, and SBS generation unchanged.
