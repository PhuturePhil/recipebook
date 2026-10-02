// app-icons v1.5 — Master: workspace/snippets/app-icons.js
// Gemeinsames Linien-Icon-Set für die Pastoors-PWAs (Plan: plans/design-vereinheitlichung.md).
// Pro App kopiert — Änderungen zuerst hier, dann in alle Apps übernehmen.
//
// Regeln: keine Emojis als UI-Icons, keine CDNs/Icon-Fonts. Alle Icons 24er-Raster,
// stroke = currentColor (Farbe kommt aus dem CSS, z. B. var(--akzent-text)), Linienstärke 1.9.
//
// Nutzung:
//   APP_ICONS.svg('suche')            → '<svg …>' (Standard 20 px)
//   APP_ICONS.svg('plus', 18, 'cls')  → mit Größe und Klasse
//   <span data-icon="suche"></span>   → wird von APP_ICONS.fill(root) ersetzt (app-shell.js ruft das automatisch auf)
//
// Herkunft/Lizenz: eigene, schlichte Zeichnungen; Formensprache und einzelne Grundformen
// angelehnt an Lucide (https://lucide.dev), ISC-Lizenz:
//   ISC License — Copyright (c) for portions of Lucide are held by Cole Bemis 2013-2022 as part of
//   Feather (MIT). All other copyright (c) for Lucide are held by Lucide Contributors 2022.
//   Permission to use, copy, modify, and/or distribute this software for any purpose with or without
//   fee is hereby granted, provided that the above copyright notice and this permission notice appear
//   in all copies. THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES […].
(function () {
  'use strict';
  var P = {
    // Navigation
    liste:      '<path d="M8.5 6H20M8.5 12H20M8.5 18H20"/><path d="M4 6h.01M4 12h.01M4 18h.01"/>',
    auswertung: '<path d="M12 3.5a8.5 8.5 0 1 0 8.5 8.5H12z"/><path d="M15 3.8A8.5 8.5 0 0 1 20.2 9H15z"/>',
    balken:     '<path d="M4 20h16"/><path d="M7.5 16.5v-5M12 16.5V6.5M16.5 16.5V10"/>',
    abo:        '<path d="M17 3l3.5 3.5L17 10"/><path d="M3.5 11.5v-1a4 4 0 0 1 4-4h13"/><path d="M7 21l-3.5-3.5L7 14"/><path d="M20.5 12.5v1a4 4 0 0 1-4 4h-13"/>',
    mehr:       '<circle cx="5.5" cy="12" r="1.7" fill="currentColor" stroke="none"/><circle cx="12" cy="12" r="1.7" fill="currentColor" stroke="none"/><circle cx="18.5" cy="12" r="1.7" fill="currentColor" stroke="none"/>',
    start:      '<path d="M4 10.5L12 4l8 6.5V19a1.5 1.5 0 0 1-1.5 1.5H15v-6H9v6H5.5A1.5 1.5 0 0 1 4 19z"/>',
    // Mehr-Menü
    neuerungen: '<path d="M11 3.5l1.9 5.1 5.1 1.9-5.1 1.9L11 17.5l-1.9-5.1L4 10.5l5.1-1.9z"/><path d="M18.5 15v5M16 17.5h5"/>',
    einstellungen: '<path d="M4 7h9M17 7h3M4 17h3M11 17h9"/><circle cx="15" cy="7" r="2"/><circle cx="9" cy="17" r="2"/>',
    abmelden:   '<path d="M9.5 20.5H6A1.5 1.5 0 0 1 4.5 19V5A1.5 1.5 0 0 1 6 3.5h3.5"/><path d="M15.5 16.5L20 12l-4.5-4.5"/><path d="M20 12H9.5"/>',
    benutzer:   '<circle cx="12" cy="8" r="4"/><path d="M4.5 20.5a7.5 7.5 0 0 1 15 0"/>',
    info:       '<circle cx="12" cy="12" r="8.5"/><path d="M12 11v5.5M12 7.6h.01"/>',
    // Darstellung
    auto:       '<circle cx="12" cy="12" r="8.5"/><path d="M12 3.5a8.5 8.5 0 0 1 0 17z" fill="currentColor" stroke="none"/>',
    hell:       '<circle cx="12" cy="12" r="3.8"/><path d="M12 2.5v2M12 19.5v2M2.5 12h2M19.5 12h2M5.3 5.3l1.4 1.4M17.3 17.3l1.4 1.4M5.3 18.7l1.4-1.4M17.3 6.7l1.4-1.4"/>',
    dunkel:     '<path d="M20 14.2A8.5 8.5 0 1 1 9.8 4a6.8 6.8 0 0 0 10.2 10.2z"/>',
    // Aktionen
    suche:      '<circle cx="10.8" cy="10.8" r="6.3"/><path d="M15.5 15.5L20 20"/>',
    plus:       '<path d="M12 5v14M5 12h14"/>',
    filter:     '<path d="M4 6h16M7 12h10M10 18h4"/>',
    schliessen: '<path d="M6.5 6.5l11 11M17.5 6.5l-11 11"/>',
    zurueck:    '<path d="M14.5 5.5L8 12l6.5 6.5"/>',
    weiter:     '<path d="M9.5 5.5L16 12l-6.5 6.5"/>',
    runter:     '<path d="M5.5 9.5L12 16l6.5-6.5"/>',
    haken:      '<path d="M5 12.5l4.5 4.5L19 7.5"/>',
    bearbeiten: '<path d="M4.5 19.5l1-4L16 5a2.1 2.1 0 0 1 3 3L8.5 18.5z"/><path d="M14 7l3 3"/>',
    loeschen:   '<path d="M4.5 7h15"/><path d="M9.5 7V4.5h5V7"/><path d="M6.5 7l.9 12.1A1.5 1.5 0 0 0 8.9 20.5h6.2a1.5 1.5 0 0 0 1.5-1.4L17.5 7"/>',
    aktualisieren: '<path d="M20 11.5A8 8 0 0 0 5.6 7.2L4 9"/><path d="M4 4.5V9h4.5"/><path d="M4 12.5a8 8 0 0 0 14.4 4.3L20 15"/><path d="M20 19.5V15h-4.5"/>',
    extern:     '<path d="M14 4h6v6"/><path d="M20 4l-8.5 8.5"/><path d="M18 13.5V19a1.5 1.5 0 0 1-1.5 1.5H5A1.5 1.5 0 0 1 3.5 19V7.5A1.5 1.5 0 0 1 5 6h5.5"/>',
    download:   '<path d="M12 4v11"/><path d="M7.5 10.5L12 15l4.5-4.5"/><path d="M4.5 19.5h15"/>',
    // Inhalte
    kalender:   '<rect x="3.5" y="5" width="17" height="15.5" rx="2"/><path d="M3.5 10h17M8 3v4M16 3v4"/>',
    uhr:        '<circle cx="12" cy="12" r="8.5"/><path d="M12 7.5V12l3 2"/>',
    warnung:    '<path d="M10.3 4.3L2.9 17.5A2 2 0 0 0 4.6 20.5h14.8a2 2 0 0 0 1.7-3L13.7 4.3a2 2 0 0 0-3.4 0z"/><path d="M12 9.5v4M12 16.8h.01"/>',
    tag:        '<path d="M3.5 12.2V4.5a1 1 0 0 1 1-1h7.7l8.3 8.3a1.5 1.5 0 0 1 0 2.1l-6.2 6.2a1.5 1.5 0 0 1-2.1 0z"/><path d="M8 8h.01"/>',
    geldbeutel: '<path d="M19 7.5V6a1.5 1.5 0 0 0-1.5-1.5H5.5a2 2 0 0 0 0 4h14a1 1 0 0 1 1 1v9a1.5 1.5 0 0 1-1.5 1.5h-13a2 2 0 0 1-2-2v-12"/><path d="M16.5 14.5h.01"/>',
    trend:      '<path d="M3.5 17l6-6 4 4 7-7.5"/><path d="M15 7.5h5.5V13"/>',
    stern:      '<path d="M12 3.5l2.6 5.4 5.9.8-4.3 4.1 1 5.8-5.2-2.8-5.2 2.8 1-5.8-4.3-4.1 5.9-.8z"/>',
    korb:       '<path d="M3.5 4.5h2l2.2 10.3a1.5 1.5 0 0 0 1.5 1.2h7.9a1.5 1.5 0 0 0 1.5-1.1L20.5 8H6.4"/><circle cx="9.5" cy="19.5" r="1.2"/><circle cx="17" cy="19.5" r="1.2"/>',
    buch:       '<path d="M4.5 19V5.5A2 2 0 0 1 6.5 3.5h13v14h-13a2 2 0 0 0-2 2 2 2 0 0 0 2 2h13"/>',
    glocke:     '<path d="M6 16.5V11a6 6 0 0 1 12 0v5.5l1.5 2h-15z"/><path d="M10 20.5a2 2 0 0 0 4 0"/>',
    // Finanzen (v1.1, Anlagen)
    aktentasche: '<rect x="3.5" y="7" width="17" height="12.5" rx="2"/><path d="M9 7V5.5A1.5 1.5 0 0 1 10.5 4h3A1.5 1.5 0 0 1 15 5.5V7"/><path d="M3.5 12.5h17"/>',
    muenzen:    '<ellipse cx="12" cy="6" rx="7" ry="2.5"/><path d="M5 6v4c0 1.4 3.1 2.5 7 2.5s7-1.1 7-2.5V6"/><path d="M5 10v4c0 1.4 3.1 2.5 7 2.5s7-1.1 7-2.5v-4"/><path d="M5 14v4c0 1.4 3.1 2.5 7 2.5s7-1.1 7-2.5v-4"/>',
    archiv:     '<rect x="3" y="4" width="18" height="4.5" rx="1"/><path d="M5 8.5V19a1.5 1.5 0 0 0 1.5 1.5h11A1.5 1.5 0 0 0 19 19V8.5"/><path d="M10 12.5h4"/>',
    beleg:      '<path d="M6 3.5h12v17l-2-1.3-2 1.3-2-1.3-2 1.3-2-1.3-2 1.3z"/><path d="M9 8h6M9 11.5h6M9 15h3.5"/>',
    bank:       '<path d="M3.5 9.5L12 4l8.5 5.5z"/><path d="M5.5 9.5v8M10 9.5v8M14 9.5v8M18.5 9.5v8"/><path d="M3.5 20.5h17"/>',
    sanduhr:    '<path d="M6.5 3.5h11M6.5 20.5h11"/><path d="M7.5 3.5v2.8a4.5 4.5 0 0 0 2 3.7L12 12l2.5-2a4.5 4.5 0 0 0 2-3.7V3.5"/><path d="M7.5 20.5v-2.8a4.5 4.5 0 0 1 2-3.7L12 12l2.5 2a4.5 4.5 0 0 1 2 3.7v2.8"/>',
    // Listen-Werkzeuge (v1.2, Anleihen)
    kacheln:    '<rect x="4" y="4" width="6.5" height="6.5" rx="1.5"/><rect x="13.5" y="4" width="6.5" height="6.5" rx="1.5"/><rect x="4" y="13.5" width="6.5" height="6.5" rx="1.5"/><rect x="13.5" y="13.5" width="6.5" height="6.5" rx="1.5"/>',
    gruppieren: '<path d="M12 3.5l8.5 4.3-8.5 4.3-8.5-4.3z"/><path d="M3.5 12l8.5 4.3 8.5-4.3"/><path d="M3.5 16.2l8.5 4.3 8.5-4.3"/>',
    sortieren:  '<path d="M7.5 4.5v15M4 8l3.5-3.5L11 8"/><path d="M16.5 19.5v-15M13 16l3.5 3.5L20 16"/>',
    'pfeil-hoch':   '<path d="M12 19.5v-15M6.5 10L12 4.5 17.5 10"/>',
    'pfeil-runter': '<path d="M12 4.5v15M6.5 14l5.5 5.5 5.5-5.5"/>',
    hoch:       '<path d="M5.5 14.5L12 8l6.5 6.5"/>',
    // Fantasy (v1.3)
    personen:   '<circle cx="9" cy="8" r="3.5"/><path d="M2.5 20a6.5 6.5 0 0 1 13 0"/><path d="M15.5 4.8a3.5 3.5 0 0 1 0 6.4"/><path d="M18 14.2a6.5 6.5 0 0 1 3.5 5.8"/>',
    tausch:     '<path d="M8 3.5L4 7.5l4 4"/><path d="M4 7.5h16"/><path d="M16 12.5l4 4-4 4"/><path d="M20 16.5H4"/>',
    schloss:    '<rect x="4.5" y="10.5" width="15" height="10" rx="2"/><path d="M8 10.5V7.5a4 4 0 0 1 8 0v3"/>',
    // Listen (v1.4)
    griff:      '<path d="M5 9h14M5 15h14"/>',
    verlauf:    '<path d="M3.5 12a8.5 8.5 0 1 0 2.5-6L3.5 8.5"/><path d="M3.5 4v4.5H8"/><path d="M12 8v4l3 2"/>',
    // v1.5 (Rezepte)
    karotte:    '<path d="M2.5 21.5s9.6-3.4 12.4-6.2a4.4 4.4 0 0 0-6.2-6.2C5.9 11.9 2.5 21.5 2.5 21.5z"/><path d="M8.6 14l-2-2M15.2 15l-2.4-2.4"/><path d="M21.5 9s-1.3-2-3.4-2C16.5 7 15 9 15 9s1.3 2 3.4 2 3.1-2 3.1-2z"/><path d="M15 2.5s-2 1.3-2 3.4S15 9 15 9s2-1.8 2-3.4c0-2.1-2-3.1-2-3.1z"/>',
    globus:     '<circle cx="12" cy="12" r="8.5"/><path d="M3.5 12h17"/><path d="M12 3.5a13 13 0 0 1 0 17a13 13 0 0 1 0-17z"/>',
    topf:       '<path d="M4.5 10.5h15v6a3.5 3.5 0 0 1-3.5 3.5H8a3.5 3.5 0 0 1-3.5-3.5z"/><path d="M2.5 10.5h19"/><path d="M9 7c0-1.2 1-1.5 1-2.8M13.5 7c0-1.2 1-1.5 1-2.8"/>'
  };

  function svg(name, size, cls) {
    var p = P[name];
    if (!p) return '';
    var s = size || 20;
    return '<svg class="icon' + (cls ? ' ' + cls : '') + '" width="' + s + '" height="' + s +
      '" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round"' +
      ' stroke-linejoin="round" aria-hidden="true" focusable="false">' + p + '</svg>';
  }

  // <span data-icon="name" data-size="18"></span> → Inline-SVG
  function fill(root) {
    var els = (root || document).querySelectorAll('[data-icon]:not([data-icon-done])');
    for (var i = 0; i < els.length; i++) {
      var el = els[i];
      el.innerHTML = svg(el.getAttribute('data-icon'), Number(el.getAttribute('data-size')) || 20);
      el.setAttribute('data-icon-done', '');
    }
  }

  window.APP_ICONS = { svg: svg, fill: fill, names: Object.keys(P) };
})();
