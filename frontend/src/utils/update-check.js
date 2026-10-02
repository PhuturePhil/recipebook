// update-check v2 — Master: workspace/snippets/update-check.js
// Gemeinsames Auto-Update + „Was ist neu“ für die Pastoors-PWAs (Plan: plans/pwa-auto-update-changelog.md).
// Pro App kopiert — Änderungen zuerst hier, dann in alle Apps übernehmen.
//
// Nutzung:
//   initUpdateCheck({
//     versionUrl:     '/api/version',    // {"version":"2026-10-02.1"}, Cache-Control: no-store
//     changelogUrl:   '/api/changelog',  // [{version,datum,titel,punkte:[…]}], neueste zuerst
//     isBusy:         () => bool,        // App-spezifisch: Dialog offen, Kochmodus …
//     builtinVersion: '2026-10-02.1',    // optional (Rezepte): ins Bundle eingebaute Version
//     versionOf:      (data) => string,  // optional: Version aus der versionUrl-Antwort lesen
//     openPopup:      (title, entries, onClose) => void,  // optional: eigener Renderer (React/Vue)
//     changelogPage:  () => void,        // optional (v2): Menüpunkt öffnet eine eigene Seite statt des Popups
//   });
//   window.__showChangelog()             // Menüpunkt „Neuerungen“ (Popup, bzw. changelogPage falls gesetzt)
//   window.__ucChangelog()               // v2: Promise mit der kompletten Historie (für die Seite „Neuerungen“)
//   window.__ucMarkSeen(version?)        // v2: als gesehen merken (Seite geöffnet) → Event 'uc:seen'
//   window.__ucState()                   // v2: {seen, current} für den NEU-Badge; Event 'uc:version' sobald bekannt
//   window.__appBusy.add('grund') / .delete('grund')   // laufende Arbeit melden
//
// Verhalten: Versionscheck beim Start, beim Zurückholen (visibilitychange/pageshow) und alle 5 Min.
// Neue Version + App frei → still neu laden; App beschäftigt → nachholen, sobald frei.
// Höchstens 1 Auto-Reload pro 2 Min, sonst Banner „Neue Version verfügbar – Aktualisieren“.
// Nach dem Laden zeigt ein Popup alle Einträge seit der zuletzt gesehenen Version — nur bei neuer Version.
// v2 ist abwärtskompatibel: ohne changelogPage verhält sich alles wie v1.
// Kein Tracking: nur anonyme GETs, gemerkt wird ausschließlich lokal (localStorage/sessionStorage).
(function () {
  'use strict';
  window.__appBusy = window.__appBusy || new Set();

  var LS_SEEN = 'uc:lastSeen';
  var SS_RELOAD = 'uc:lastReload';
  var GUARD_MS = 2 * 60 * 1000;
  var started = false;

  function lsGet(k) { try { return localStorage.getItem(k); } catch (e) { return null; } }
  function lsSet(k, v) { try { localStorage.setItem(k, v); } catch (e) {} }

  window.initUpdateCheck = function initUpdateCheck(cfg) {
    if (started) return;   // nur einmal pro Seite (React StrictMode, mehrfaches Einbinden)
    started = true;
    cfg = cfg || {};
    var versionUrl = cfg.versionUrl || '/api/version';
    var changelogUrl = cfg.changelogUrl || '/api/changelog';
    var loadedVersion = cfg.builtinVersion || null;
    var reloadPending = false;
    var pendingTimer = null;
    var popupShownThisLoad = false;

    // ---------- Busy-Erkennung ----------
    var NO_TEXT = /^(button|submit|reset|checkbox|radio|range|color|hidden|image)$/i;
    function dirtyInput() {
      var el = document.activeElement;
      if (!el || !/^(INPUT|TEXTAREA|SELECT)$/.test(el.tagName)) return false;
      if (el.tagName === 'INPUT' && NO_TEXT.test(el.type || '')) return false;
      return String(el.value || '') !== '';
    }
    function busy() {
      if (window.__appBusy.size > 0 || dirtyInput()) return true;
      try { return cfg.isBusy ? !!cfg.isBusy() : false; } catch (e) { return false; }
    }

    // ---------- Netz ----------
    function fetchJson(url) {
      return fetch(url, { cache: 'no-store', credentials: 'same-origin' })
        .then(function (r) { return r.ok ? r.json() : null; })
        .catch(function () { return null; });   // offline, Login-Redirect (HTML) … → keine Info
    }
    function fetchVersion() {
      return fetchJson(versionUrl).then(function (d) {
        if (!d) return null;
        if (cfg.versionOf) { try { return cfg.versionOf(d) || null; } catch (e) { return null; } }
        if (Array.isArray(d)) return d.length ? d[0].version || null : null;
        return d.version || null;
      });
    }
    function fetchChangelog() {
      return fetchJson(changelogUrl).then(function (d) { return Array.isArray(d) && d.length ? d : null; });
    }

    function emit(name) { try { window.dispatchEvent(new Event(name)); } catch (e) {} }

    // ---------- Reload ----------
    function doReload() {
      try { sessionStorage.setItem(SS_RELOAD, String(Date.now())); } catch (e) {}
      var upd = (navigator.serviceWorker && navigator.serviceWorker.getRegistration)
        ? navigator.serviceWorker.getRegistration().then(function (r) { return r && r.update(); }).catch(function () {})
        : Promise.resolve();
      var done = false;
      function go() { if (!done) { done = true; location.reload(); } }
      upd.then(go, go);
      setTimeout(go, 2000);   // nicht am SW-Update hängen bleiben
    }
    function stopPending() {
      reloadPending = false;
      if (pendingTimer) { clearInterval(pendingTimer); pendingTimer = null; }
    }
    function tryReload() {
      if (busy()) {
        reloadPending = true;
        if (!pendingTimer) {
          pendingTimer = setInterval(function () {
            if (document.visibilityState === 'visible' && !busy()) tryReload();
          }, 5000);
        }
        return;
      }
      stopPending();
      var last = 0;
      try { last = Number(sessionStorage.getItem(SS_RELOAD) || 0); } catch (e) {}
      if (Date.now() - last < GUARD_MS) { showBanner(); return; }   // Schleifen-Schutz
      doReload();
    }
    function checkVersion() {
      return fetchVersion().then(function (v) {
        if (!v) return;
        if (!loadedVersion) { loadedVersion = v; emit('uc:version'); maybePopup(v); return; }
        if (v !== loadedVersion) tryReload();
        else { if (reloadPending) stopPending(); maybePopup(v); }
      });
    }

    // ---------- Banner (nur noch Fallback des Schleifen-Schutzes) ----------
    function showBanner() { whenBody(renderBanner); }
    function renderBanner() {
      if (document.getElementById('ucBanner')) return;
      ensureStyles();
      var el = document.createElement('div');
      el.id = 'ucBanner';
      el.className = 'update-banner uc-banner';
      el.setAttribute('role', 'status');
      el.innerHTML = '<span>Neue Version verfügbar</span><button type="button">Aktualisieren</button>';
      el.querySelector('button').addEventListener('click', function () { location.reload(); });
      document.body.appendChild(el);
    }

    // ---------- „Was ist neu“ ----------
    function markSeen(v) { if (v) { lsSet(LS_SEEN, v); emit('uc:seen'); } }
    function maybePopup(current) {
      if (popupShownThisLoad) return;
      var seen = lsGet(LS_SEEN);
      if (!seen) { markSeen(current); return; }   // allererster Start: nur merken
      if (seen === current) return;
      popupShownThisLoad = true;
      fetchChangelog().then(function (log) {
        if (!log) { popupShownThisLoad = false; return; }
        var idx = -1;
        for (var i = 0; i < log.length; i++) if (log[i].version === seen) { idx = i; break; }
        var entries = idx > 0 ? log.slice(0, idx) : log.slice(0, 5);
        showWhenFree(function () {
          if (lsGet(LS_SEEN) === current) return;   // inzwischen gesehen (z. B. Seite „Neuerungen“ offen)
          open('Was ist neu', entries, function () { markSeen(current); });
        }, 0);
      });
    }
    function showWhenFree(fn, waited) {   // Popup nie über einen offenen Dialog/eine Eingabe legen
      if (!busy()) { fn(); return; }
      if (waited >= 60000) { popupShownThisLoad = false; return; }   // beim nächsten Check erneut
      setTimeout(function () { showWhenFree(fn, waited + 2000); }, 2000);
    }
    function open(title, entries, onClose) {
      if (cfg.openPopup) cfg.openPopup(title, entries, onClose);
      else whenBody(function () { openPopup(title, entries, onClose); });
    }

    window.__showChangelog = function () {
      if (cfg.changelogPage) { cfg.changelogPage(); return Promise.resolve(); }
      return fetchChangelog().then(function (log) {
        if (!log) return;
        open('Neuerungen', log, function () { markSeen(loadedVersion || log[0].version); });
      });
    };

    window.__ucChangelog = fetchChangelog;
    window.__ucMarkSeen = function (v) { markSeen(v || loadedVersion); };
    window.__ucState = function () { return { seen: lsGet(LS_SEEN), current: loadedVersion }; };

    // ---------- Start ----------
    checkVersion();
    document.addEventListener('visibilitychange', function () {
      if (document.visibilityState === 'visible') checkVersion();
    });
    window.addEventListener('pageshow', function (e) { if (e.persisted) checkVersion(); });   // iOS bfcache
    setInterval(function () { if (document.visibilityState === 'visible') checkVersion(); }, 5 * 60 * 1000);
  };

  // ---------- Standard-Popup (Vanilla-Apps) ----------
  // Farben über CSS-Variablen pro App: --uc-surface, --uc-text, --uc-muted, --uc-border, --uc-accent, --uc-accent-text
  function ensureStyles() {
    if (document.getElementById('ucStyles')) return;
    var s = document.createElement('style');
    s.id = 'ucStyles';
    // :where() = Spezifität 0 → App-CSS kann alles überschreiben; Buttons mit Klassen-Spezifität,
    // damit generische button-Regeln der Apps sie nicht plätten
    s.textContent =
      ':where(.uc-banner){position:fixed;left:50%;transform:translateX(-50%);top:calc(env(safe-area-inset-top,0px) + 10px);' +
      'background:var(--uc-surface,#fff);color:var(--uc-text,#1a1a1a);border:1px solid var(--uc-border,#ddd);border-radius:10px;' +
      'padding:10px 14px 10px 18px;font-size:14px;display:flex;align-items:center;gap:12px;box-shadow:0 6px 20px rgba(0,0,0,.3);' +
      'z-index:10001;max-width:90%;white-space:nowrap}' +
      '.uc-banner button{background:var(--uc-accent,#2d6a4f);color:var(--uc-accent-text,#fff);border:none;border-radius:6px;padding:6px 12px;' +
      'font-size:13px;font-weight:600;cursor:pointer;font-family:inherit}' +
      ':where(.uc-overlay){position:fixed;inset:0;background:rgba(0,0,0,.5);z-index:10000;display:flex;align-items:center;' +
      'justify-content:center;padding:16px}' +
      ':where(.uc-dialog){background:var(--uc-surface,#fff);color:var(--uc-text,#1a1a1a);border:1px solid var(--uc-border,#ddd);' +
      'border-radius:14px;width:100%;max-width:460px;max-height:min(80vh,640px);display:flex;flex-direction:column;' +
      'box-shadow:0 10px 40px rgba(0,0,0,.35);font-family:inherit}' +
      ':where(.uc-dialog h2){margin:0;padding:18px 20px 8px;font-size:19px}' +
      ':where(.uc-body){overflow-y:auto;padding:4px 20px 4px;-webkit-overflow-scrolling:touch}' +
      ':where(.uc-entry){margin:0 0 16px}' +
      ':where(.uc-entry h3){margin:0 0 2px;font-size:15px}' +
      ':where(.uc-date){margin:0 0 6px;font-size:12px;color:var(--uc-muted,#666)}' +
      ':where(.uc-entry ul){margin:0;padding-left:18px;font-size:14px;line-height:1.45}' +
      ':where(.uc-entry li){margin:3px 0}' +
      ':where(.uc-foot){padding:10px 20px 18px}' +
      '.uc-dialog .uc-close{display:block;width:100%;background:var(--uc-accent,#2d6a4f);color:var(--uc-accent-text,#fff);border:none;border-radius:8px;' +
      'padding:11px;font-size:15px;font-weight:600;cursor:pointer;font-family:inherit}';
    document.head.appendChild(s);
  }

  function whenBody(fn) {
    if (document.body) fn();
    else document.addEventListener('DOMContentLoaded', fn, { once: true });
  }

  function el(tag, cls, text) {
    var e = document.createElement(tag);
    if (cls) e.className = cls;
    if (text != null) e.textContent = text;
    return e;
  }

  function openPopup(title, entries, onClose) {
    if (document.getElementById('ucOverlay')) return;
    ensureStyles();
    var prevFocus = document.activeElement;
    var prevOverflow = document.body.style.overflow;
    var ov = el('div', 'uc-overlay');
    ov.id = 'ucOverlay';
    var dlg = el('div', 'uc-dialog');
    dlg.setAttribute('role', 'dialog');
    dlg.setAttribute('aria-modal', 'true');
    dlg.setAttribute('aria-labelledby', 'ucTitle');
    var h = el('h2', null, title);
    h.id = 'ucTitle';
    dlg.appendChild(h);
    var body = el('div', 'uc-body');
    entries.forEach(function (e) {
      var box = el('section', 'uc-entry');
      box.appendChild(el('h3', null, e.titel || e.version));
      if (e.datum) box.appendChild(el('p', 'uc-date', e.datum));
      var ul = el('ul');
      (e.punkte || []).forEach(function (p) { ul.appendChild(el('li', null, p)); });
      box.appendChild(ul);
      body.appendChild(box);
    });
    dlg.appendChild(body);
    var foot = el('div', 'uc-foot');
    var btn = el('button', 'uc-close', 'Alles klar');
    btn.type = 'button';
    foot.appendChild(btn);
    dlg.appendChild(foot);
    ov.appendChild(dlg);

    function onKey(ev) {
      if (ev.key === 'Escape') { ev.preventDefault(); close(); }
      else if (ev.key === 'Tab') { ev.preventDefault(); btn.focus(); }   // einziges Bedienelement
    }
    function close() {
      document.removeEventListener('keydown', onKey, true);
      ov.remove();
      document.body.style.overflow = prevOverflow;
      if (prevFocus && prevFocus.focus && document.contains(prevFocus)) { try { prevFocus.focus(); } catch (e) {} }
      if (onClose) onClose();
    }
    btn.addEventListener('click', close);
    ov.addEventListener('click', function (ev) { if (ev.target === ov) close(); });
    document.addEventListener('keydown', onKey, true);
    document.body.style.overflow = 'hidden';
    document.body.appendChild(ov);
    btn.focus();
  }
})();
