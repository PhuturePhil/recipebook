export const changelog = [
  {
    date: '25.09.2026',
    title: 'Schneller laden, Suche beim Tippen, Mengen mit Brüchen',
    changes: [
      'Die Rezeptliste lädt deutlich schneller, vor allem am Handy: Bilder kommen erst, wenn die Karte ins Bild scrollt, und werden danach zwischengespeichert',
      'Neue Fotos werden beim Hochladen automatisch verkleinert, vorhandene Bilder wurden einmalig verkleinert',
      'Mengen wie „1/2“, „½“, „1,5“ oder „200-250“ werden richtig erkannt und beim Umrechnen der Portionen mitgerechnet',
      'Die Suche filtert schon beim Tippen, Enter ist nicht mehr nötig. Mit Enter oder Komma wird ein Begriff wie bisher als Filter festgehalten',
      '„Bearbeiten“ und „Löschen“ erscheinen nur noch bei eigenen Rezepten (Admins sehen sie überall). Der Knopf „Abbrechen“ auf der Rezeptseite heißt jetzt „Zurück“',
      'Nicht vorhandene Rezepte und unbekannte Adressen zeigen eine verständliche Seite statt einer Fehlermeldung oder einer leeren Seite',
    ]
  },
  {
    date: '25.09.2026',
    title: 'Sicherheit und klarere Fehlermeldungen',
    changes: [
      'Fehlermeldungen des Servers erscheinen jetzt im Klartext, z. B. „Das Rezept wurde nicht gefunden.“ oder „Diese E-Mail-Adresse ist bereits vergeben.“',
      'Wenn ein Admin ein Rezept bearbeitet, bleibt der ursprüngliche Ersteller eingetragen',
      'Passwörter müssen jetzt mindestens 8 Zeichen lang sein. Nach einem Passwortwechsel werden alle anderen Anmeldungen dieses Kontos abgemeldet',
      'Nach vielen Fehlversuchen beim Anmelden gibt es eine kurze Wartezeit',
      'Ein Benutzer, der noch Rezepte hat, lässt sich nicht löschen. Stattdessen erscheint ein Hinweis',
    ]
  },
  {
    date: '25.09.2026',
    title: 'Nährwerttabelle pro 100 g und pro Portion',
    changes: [
      'Die Nährwerttabelle zeigt links die Werte pro 100 g und rechts pro Portion, auch auf dem Handy',
      'Die Spalte mit der Summe für alle Portionen entfällt',
      'Unter der Tabelle steht, auf welches Gesamtgewicht der rohen Zutaten sich „pro 100 g“ bezieht; fehlt für eine Zutat die Grammangabe, gibt es einen Hinweis',
    ]
  },
  {
    date: '25.09.2026',
    title: 'Länger angemeldet bleiben',
    changes: [
      'Nach dem Neuladen der Seite oder beim Öffnen eines Rezept-Links bleibt man jetzt angemeldet',
      'Eine Anmeldung gilt jetzt 90 Tage statt 24 Stunden',
      'Ist die Anmeldung abgelaufen, geht es mit einem kurzen Hinweis zur Anmeldeseite und nach dem Anmelden zurück zur aufgerufenen Seite',
    ]
  },
  {
    date: '25.09.2026',
    title: 'Nährwerte neu berechnet',
    changes: [
      'Die Nährwerte stammen jetzt aus dem Bundeslebensmittelschlüssel (BLS), der offiziellen deutschen Nährwertdatenbank, statt aus KI-Schätzungen',
      'Im Rezeptkopf steht die Kalorienzahl pro Portion, in der Nährwerttabelle zusätzlich Kilojoule, Zucker und Salz',
      'Unter der Tabelle steht, aus wie vielen Zutaten gerechnet wurde und welche fehlen; unvollständige Werte werden ausgegraut',
      'Pro Zutat lässt sich aufklappen, wie viel Gramm angenommen wurden und woher der Wert stammt',
      'Vitamine und Mineralstoffe lassen sich unter „Mikronährstoffe anzeigen“ einblenden',
      'Die Badges „Energiearm“, „Fettarm“, „Proteinreich“ und „Ballaststoffreich“ folgen jetzt festen Grenzen aus dem EU-Recht; eine Erklärung gibt es im Menü unter „Nährwerte & Badges erklärt“',
      'Mengen wie „200–250 g“, „½ TL“ oder „8–10 Äpfel“ werden jetzt richtig mitgerechnet',
    ]
  },
  {
    date: '25.09.2026',
    title: 'Anmelden mit pastoors.cloud',
    changes: [
      'Auf der Anmeldeseite gibt es jetzt den Button "Mit pastoors.cloud anmelden" — ein Konto für alle Familienseiten',
      'Bestehende Konten werden über die E-Mail-Adresse automatisch verknüpft; Rezepte und Rechte bleiben erhalten',
      'Die Anmeldung mit E-Mail und Passwort funktioniert weiterhin wie gewohnt',
    ]
  },
  {
    date: '24.09.2026',
    title: 'Rezepte teilen & als PDF speichern',
    changes: [
      'Rezepte lassen sich jetzt per Link mit anderen teilen — auch mit Personen ohne Konto',
      'Geteilte Links zeigen nur Zutaten, Zubereitung und Quelle; Beschreibung und persönliche Notizen bleiben privat',
      'Ein Link ist 30 Tage gültig und kann jederzeit widerrufen oder neu erzeugt werden',
      'Über "Als PDF speichern" lässt sich ein Rezept vollständig drucken oder als PDF sichern',
    ]
  },
  {
    date: '05.03.2026',
    title: 'Zutaten nach Einheit gruppiert',
    changes: [
      'Die Zutatenseite zeigt Zutaten jetzt nach Einheit gruppiert an',
    ]
  },
  {
    date: '02.03.2026',
    title: 'Zuverlässigere Nährwertberechnung',
    changes: [
      'Die Nährwertberechnung erkennt Zutaten jetzt zuverlässiger — auch wenn die KI intern nach einer anderen Schreibweise sucht',
      'Zutaten, für die keine Nährwerte ermittelt werden konnten, werden nun protokolliert',
    ]
  },
  {
    date: '02.03.2026',
    title: 'Nährwerte der Zutaten',
    changes: [
      'Im Nährwerte-Tab eines Rezepts gibt es jetzt einen Link zur Zutatenseite — dort werden alle Zutaten des Rezepts mit ihren Nährwerten angezeigt, sortiert nach Kalorien',
    ]
  },
  {
    date: '02.03.2026',
    title: 'Rezeptanzahl',
    changes: [
      'Die Anzahl der Rezepte wird jetzt auf der Hauptseite angezeigt',
      'Bei aktiver Suche wird angezeigt, wie viele Rezepte gefunden wurden',
    ]
  },
  {
    date: '02.03.2026',
    title: 'Ladeanimation',
    changes: [
      'Beim Speichern eines Rezepts und beim Analysieren eines Rezeptfotos erscheint jetzt eine Ladeanimation',
      'Während die Animation läuft, ist die Seite gesperrt — so gibt es keine versehentlichen Doppelklicks',
    ]
  },
  {
    date: '02.03.2026',
    title: 'Badges',
    changes: [
      'Rezepte werden jetzt mit Badges ausgezeichnet — z.B. "Schnell", "Proteinreich" oder "Kalorienarm"',
      'Die Badges werden automatisch vergeben und zeigen, welche Rezepte in ihrer Kategorie besonders herausstechen',
      'Über die Suche kann direkt nach Badges gefiltert werden',
    ]
  },
  {
    date: '01.03.2026',
    title: 'Zutaten & Nährwerte',
    changes: [
      'Neue Seite "Zutaten" im Menü: alle Zutaten mit ihren Nährwerten auf einen Blick',
      'Klick auf eine Zutat zeigt die genauen Nährwerte pro Einheit',
      'Spalten lassen sich sortieren — nach Name, Einheit oder Nährwerten',
      'Admins können Nährwerte direkt in der Tabelle pflegen und neue Zutaten anlegen',
      'Beim Anlegen eines Rezepts werden bekannte Zutaten aus dem Katalog verwendet — spart Zeit und verbessert die Genauigkeit',
    ]
  },
  {
    date: '01.03.2026',
    title: 'Schnellere Ladezeiten',
    changes: [
      'Rezepte ohne Bild laden jetzt schneller — externe Platzhalterbilder werden nicht mehr nachgeladen',
    ]
  },
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
