// Hell/Dunkel vor dem ersten Zeichnen setzen (kein Aufblitzen) — Logik wie workspace/snippets/app-shell.js.
// Eigene Datei statt Inline-Skript, weil die CSP (script-src 'self') Inline-Skripte blockt.
(function () {
  try {
    var t = localStorage.getItem('theme');
    var d = t === 'dark' || (t !== 'light' && window.matchMedia && matchMedia('(prefers-color-scheme: dark)').matches);
    var r = document.documentElement;
    r.setAttribute('data-theme', d ? 'dark' : 'light');
    r.style.background = d ? '#14161D' : '#F2F2F7';
    if (window.navigator.standalone === true) r.classList.add('is-standalone');
    var m = document.querySelectorAll('meta[name="theme-color"]');
    for (var i = 0; i < m.length; i++) m[i].setAttribute('content', d ? '#14161D' : '#F2F2F7');
  } catch (e) {}
})();
