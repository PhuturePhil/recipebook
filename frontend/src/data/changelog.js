export const changelog = [
  {
    date: '27.09.2026',
    title: 'Schlauere Suche und Sortierung',
    changes: [
      'Englische Rezepte werden jetzt auch mit deutschen Begriffen gefunden, zum Beispiel „Bohnen“ für „Turkish green beans“. Die englischen Begriffe funktionieren weiterhin',
      'Akzente und ß spielen bei der Suche keine Rolle mehr: „Creme“ findet auch „Crème fraîche“, „Sosse“ auch „Soße“. Kleine Tippfehler in längeren Wörtern werden verziehen',
      'Neben dem Suchfeld lässt sich die Liste sortieren: neueste zuerst, nach Zubereitungszeit oder nach kcal pro Portion. Die Wahl bleibt auf dem Gerät gespeichert',
    ]
  },
  {
    date: '27.09.2026',
    title: 'Übersetzungen deutlich gekennzeichnet',
    changes: [
      'Zeigt die App die deutsche Übersetzung eines englischen Rezepts, steht direkt unter dem Titel ein farbiger Hinweis „Automatisch übersetzte Fassung — Original: Englisch“ mit dem Knopf „Original anzeigen“. Das gilt auch auf geteilten Seiten',
      'Im PDF steht unter dem Titel „Automatisch aus dem Englischen übersetzt“',
    ]
  },
  {
    date: '27.09.2026',
    title: 'Englische Rezepte auf Deutsch',
    changes: [
      'Englische Rezepte erscheinen jetzt auf Deutsch: Titel, Beschreibung, Zutaten und Schritte werden beim ersten Öffnen einmal übersetzt (dauert ein paar Sekunden) und danach gespeichert',
      'Amerikanische Maße werden dabei umgerechnet: cup → ml, oz und lb → g, inch → cm, °F → °C. Die Portionen-Umrechnung funktioniert mit den umgerechneten Mengen',
      'Oben auf der Rezeptseite schaltet „Deutsch / Original“ zwischen Übersetzung und englischem Original um. Die Wahl gilt für alle englischen Rezepte und bleibt auf dem Gerät gespeichert',
      'PDF und geteilte Links folgen der gewählten Sprache; auf der geteilten Seite gibt es denselben Schalter. Wird das Original bearbeitet, entsteht die Übersetzung beim nächsten Öffnen neu',
    ]
  },
  {
    date: '27.09.2026',
    title: 'Sprache pro Rezept',
    changes: [
      'Jedes Rezept kennt jetzt seine Sprache (Deutsch oder Englisch). Beim Speichern erkennt die App sie automatisch aus Titel, Zutaten und Schritten',
      'Im Formular steht unter der Zubereitungszeit ein Feld „Sprache des Rezepts“. Dort lässt sich die Erkennung korrigieren; eine von Hand gewählte Sprache bleibt beim nächsten Speichern erhalten',
    ]
  },
  {
    date: '27.09.2026',
    title: 'Rezepteingabe: Foto-Knopf, Einheiten per Tastatur, Bruch-Tasten',
    changes: [
      'Das Bildfeld hat jetzt einen Knopf „Foto aufnehmen oder auswählen“ mit Vorschau daneben. Auf dem Handy stehen weiter Kamera und Mediathek zur Wahl. Ohne eigenes Foto sucht die App beim Anlegen automatisch ein passendes Bild',
      'In der Einheiten-Liste wählt man mit ↑/↓ und Enter, Esc schließt die Liste. Enter speichert dabei nie das Rezept',
      'Ist unter dem Einheitenfeld zu wenig Platz, etwa über der Tastatur oder den Knöpfen unten, klappt die Liste nach oben auf',
      'Im Mengenfeld erscheinen die Tasten ½ ¼ ¾. Aus „1“ und ½ wird „1 1/2“, ein vorhandener Bruch wird ersetzt',
    ]
  },
  {
    date: '27.09.2026',
    title: 'Rezepteingabe: einheitliche Einheiten, Zutaten-Vorschläge, Nährwert-Hinweis',
    changes: [
      'Die Einheiten-Liste zeigt jetzt eine feste Auswahl: g, kg, ml, l, EL, TL, Prise, Stück, Zehe, Scheibe, Bund, Zweig, Handvoll, Dose, Glas, Packung, Becher, Tasse. Eigene Angaben wie „daumengroßes Stück“ lassen sich weiterhin eintippen',
      'Beim Speichern neuer oder geänderter Zutaten werden gängige Schreibweisen vereinheitlicht, z. B. „St.“ oder „pc“ zu „Stück“, „tablespoon“ zu „EL“, „Teel.“ zu „TL“, „Dosen“ zu „Dose“. Bestehende Zutaten bleiben, wie sie sind',
      'Beim Tippen im Zutatenfeld erscheinen Vorschläge aus dem Zutatenkatalog, z. B. „Kicher…“ → Kichererbsen. Mit ↑/↓ und Enter oder per Antippen übernehmen, Esc schließt die Liste',
      'Neben jeder Zutat zeigt ✓, dass Nährwerte dafür hinterlegt sind. Ein ? heißt: keine Nährwertdaten, die Zutat wird per KI geschätzt. Antippen zeigt die Details',
      'Beim Speichern prüft der Server die Angaben: Titel ist Pflicht, Portionen zwischen 1 und 100. Fehlt die Portionenzahl, gilt 4. Bei Fehlern erscheint eine verständliche Meldung',
    ]
  },
  {
    date: '27.09.2026',
    title: 'Rezepteingabe: Zutatenliste einfügen, Entwürfe, Umsortieren',
    changes: [
      'Eine kopierte Zutatenliste lässt sich in eine Zutatenzeile einfügen. Jede Zeile wird eine eigene Zutat, Menge und Einheit werden erkannt, z. B. „200 g Zwiebeln“ oder „½ TL Salz“. Zeilen ohne Menge wie „Salz und Pfeffer“ landen komplett im Namen',
      '„Als Text bearbeiten“ zeigt alle Zutaten als Text mit einer Zutat pro Zeile. Nach „Übernehmen“ werden daraus wieder einzelne Zeilen',
      'Eingaben werden laufend als Entwurf auf dem Gerät gesichert. Nach Neuladen, Absturz oder abgelaufener Anmeldung fragt das Formular „Entwurf von … wiederherstellen?“. Nach dem Speichern wird der Entwurf gelöscht',
      'Beim Neuladen oder Schließen des Tabs kommt deshalb keine Nachfrage mehr. Wer innerhalb der App weggeht, wird weiterhin gefragt, und wer bestätigt, verwirft damit auch den Entwurf',
      'Zutaten und Arbeitsschritte lassen sich mit den Pfeilen ↑/↓ verschieben, per Tastatur mit Alt+↑/↓',
      'Die Reihenfolge der Zutaten bleibt beim Speichern jetzt zuverlässig erhalten. Bisher konnten bearbeitete Zutaten ans Ende der Liste rutschen. Zwei betroffene Rezepte zeigen wieder die ursprünglich eingegebene Reihenfolge',
    ]
  },
  {
    date: '27.09.2026',
    title: 'Display bleibt beim Kochen an',
    changes: [
      'Auf der Rezeptseite und bei geteilten Rezepten geht das Display nicht mehr von selbst aus, solange das Rezept offen ist',
      'Der Knopf „Display bleibt an“ oben am Rezept schaltet das ab. Die Einstellung merkt sich jedes Gerät',
      'Im Energiesparmodus kann der Browser das Anbleiben verweigern, dann wirkt der Knopf blasser. Ältere Browser zeigen den Knopf gar nicht',
    ]
  },
  {
    date: '27.09.2026',
    title: 'Rezepteingabe: weniger Stolperfallen',
    changes: [
      'Enter speichert das Rezept nicht mehr versehentlich. In einer Zutatenzeile legt Enter eine neue Zeile darunter an und springt hinein, gespeichert wird nur über den Knopf',
      'Schlägt das Speichern fehl, bleiben alle Eingaben stehen und die Fehlermeldung erscheint direkt über den Knöpfen',
      'Leere Zutaten- oder Schrittzeilen blockieren das Speichern nicht mehr, sie werden beim Speichern einfach weggelassen',
      'Zutatenzeile in der Reihenfolge Menge, Einheit, Zutat. Die Tab-Taste springt nicht mehr auf den Löschen-Knopf oder in die Einheiten-Liste',
      '„Abbrechen“ beim Bearbeiten führt zurück zum Rezept. Bei ungespeicherten Änderungen kommt vorher eine Nachfrage, auch beim Zurück-Wischen oder Neuladen',
      'Ein Foto-Scan fragt nach, bevor er vorhandene Zutaten und Schritte ersetzt',
    ]
  },
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
