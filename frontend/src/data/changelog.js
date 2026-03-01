export const changelog = [
  {
    date: '01.03.2026',
    title: 'Einladungslink',
    changes: [
      'Admins können in der Benutzerverwaltung einen Einladungslink generieren',
      'Der Link ist 24 Stunden gültig und kann nur einmal verwendet werden',
      'Wer den Link öffnet, kann sich direkt registrieren — ohne dass der Admin eine E-Mail-Adresse kennen muss',
    ]
  },
  {
    date: '01.03.2026',
    title: 'Erweiterte Suche',
    changes: [
      'Suche jetzt auch nach Zutaten, Autor und Kochbuch',
      'Mehrere Suchbegriffe mit Komma kombinieren — alle müssen passen',
      'Zeitfilter mit < oder > (z.B. "< 30" findet Rezepte unter 30 Minuten)',
      'Suchbegriffe erscheinen als Badges und lassen sich einzeln entfernen',
    ]
  },
  {
    date: '28.02.2026',
    title: 'Fehlerbehebungen',
    changes: [
      'Bei gescannten Rezepten passen sich die Textfelder jetzt korrekt an die Länge des Inhalts an',
      'Hat ein Autor mehrere Bücher, werden beim Auswählen des Autors jetzt nur noch seine Bücher vorgeschlagen',
      'Die Vorschlagsliste beim Autor- und Buchfeld bleibt beim Wechsel zwischen den Feldern geöffnet',
    ]
  },
  {
    date: '28.02.2026',
    title: 'Kochbuch-Vorschläge',
    changes: [
      'Beim Eingeben eines Autors oder Buches werden passende Vorschläge aus vorhandenen Rezepten angezeigt',
      'Wird ein Buch ausgewählt, wird der dazugehörige Autor automatisch eingetragen — und umgekehrt',
      'Ein kleines rotes × im Feld löscht den Eintrag mit einem Klick',
    ]
  },
  {
    date: '28.02.2026',
    title: 'Schnelleres Laden der Übersicht',
    changes: [
      'Die Übersichtsseite lädt deutlich schneller, weil jetzt nur noch die für die Karten benötigten Daten übertragen werden',
      'Bilder werden erst geladen, wenn sie auf dem Bildschirm sichtbar werden — nicht alle auf einmal',
      'Wechselt man zwischen Seiten hin und her, werden die Rezepte nicht unnötig neu geladen',
      'Kehrt man zur App zurück (z.B. nach dem Wechsel in eine andere App), werden die Rezepte automatisch aktualisiert',
    ]
  },
  {
    date: '28.02.2026',
    title: 'Mobile Optimierung',
    changes: [
      'Der Rezepttitel erscheint in der Navigationsleiste, sobald er beim Scrollen nicht mehr sichtbar ist — auf der Anzeige- und der Bearbeitungsseite',
      'Beim Erstellen eines Rezepts wird der eingetippte Titel sofort in der Navigationsleiste angezeigt',
      'Auf dem Smartphone wird dabei der Website-Name ausgeblendet, damit der Titel besser lesbar ist',
      'Die Aktions-Buttons bleiben beim Scrollen immer am unteren Bildschirmrand sichtbar — auf der Anzeige- und der Bearbeitungsseite',
      'Auf der Rezeptseite gibt es jetzt Abbrechen-, Löschen- und Bearbeiten-Buttons am unteren Rand',
      'Das Burger-Menü ist auf dem Smartphone jetzt immer oben rechts sichtbar — auch auf der Übersichtsseite',
      'Zutaten und Nährwerte sind jetzt als klickbare Überschriften dargestellt statt als separate Buttons',
      'In der Bearbeitungsmaske heben sich die Zutaten durch Trennlinien besser voneinander ab',
      'Auf dem Smartphone werden Zutat, Menge und Einheit übersichtlicher untereinander angezeigt',
      'Löschen-Buttons bei Zutaten und Arbeitsschritten sind jetzt als rotes Mülleimer-Symbol dargestellt',
      'Textfelder für Beschreibung und Arbeitsschritte passen ihre Höhe automatisch an den Inhalt an',
    ]
  },
  {
    date: '28.02.2026',
    title: 'App-Icon',
    changes: [
      'Neues App-Icon mit "PR"-Monogramm und Buchsymbol für Browser-Tab und Homescreen',
      'Die App kann jetzt auf dem iPhone-Homescreen installiert werden und zeigt das eigene Icon an',
    ]
  },
  {
    date: '28.02.2026',
    title: 'Navigation',
    changes: [
      'Neues Burger-Menü in der Navigation für Neuerungen, Benutzerverwaltung, persönliche Daten und Ausloggen',
    ]
  },
  {
    date: '27.02.2026',
    title: 'Nährwerte',
    changes: [
      'Automatische Nährwertberechnung via KI beim Anlegen und Bearbeiten von Rezepten',
      'Nährwerttabelle in der Rezeptansicht mit Werten pro Portion und gesamt',
      'Tabelle passt sich dynamisch an die gewählte Personenanzahl an',
    ]
  },
  {
    date: '25.02.2026',
    title: 'Automatische Rezeptbilder',
    changes: [
      'Beim Anlegen eines Rezepts ohne eigenes Bild wird automatisch ein passendes Foto geladen',
    ]
  },
  {
    date: '25.02.2026',
    title: 'Benutzerverwaltung',
    changes: [
      'Admins können Benutzer anlegen, bearbeiten und löschen',
      'Benutzer können ihr Profil (Name, E-Mail, Passwort) selbst bearbeiten',
      'Passwort-Reset per E-Mail',
      'Neue Benutzer müssen ihr Passwort beim ersten Login ändern',
    ]
  },
  {
    date: '24.02.2026',
    title: 'Rezepterfassung per Foto',
    changes: [
      'Rezepte können aus Fotos per KI automatisch erfasst werden',
      'Mehrere Bilder gleichzeitig für den Rezeptscan hochladbar',
      'Zubereitungszeit wird beim Scan erkannt und gespeichert',
    ]
  },
  {
    date: '24.02.2026',
    title: 'Verbesserungen beim Rezept anlegen',
    changes: [
      'Zubereitungszeit (Minuten) kann angegeben werden',
      'Personenanzahl als Bereich (z.B. 4–6 Personen) möglich',
      'Einheiten-Autocomplete schlägt bekannte Einheiten vor',
      'Neue unbekannte Einheiten können direkt hinzugefügt werden',
    ]
  },
  {
    date: '23.02.2026',
    title: 'HTTPS & SSL',
    changes: [
      'Die Seite ist nun über https://pastoors.cloud erreichbar',
      'Automatische Weiterleitung von HTTP auf HTTPS',
    ]
  },
  {
    date: '22.02.2026',
    title: 'Start',
    changes: [
      'Rezepte anlegen, bearbeiten und löschen',
      'Rezeptsuche nach Titel und Beschreibung',
      'Zutaten mit Menge und Einheit',
      'Zubereitungsschritte',
      'Portionsrechner in der Rezeptansicht',
      'Bild-Upload für Rezepte',
    ]
  },
]
