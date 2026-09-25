#!/usr/bin/env python3
"""Konvertiert die offizielle BLS-4.0-Excel-Datei in die kompakte CSV, die das Backend importiert.

Quelle: Max Rubner-Institut (2025): Bundeslebensmittelschlüssel (BLS), Version 4.0 —
Deutsche Nährstoffdatenbank. Karlsruhe. DOI: 10.25826/Data20251217-134202-0
Download: https://blsdb.de/download (Lizenz CC BY 4.0)

Aufruf:
  python3 -m venv .venv && .venv/bin/pip install openpyxl
  .venv/bin/python3 tools/bls/convert_bls.py BLS_4_0_Daten_2025_DE.xlsx \
      backend/src/main/resources/nutrition/bls_4_0.csv.gz

Werte sind pro 100 g essbarer Anteil. "-" (kein Wert) wird leer, "<LOD", "<LOQ" und "TR"
(Spuren) werden als 0 übernommen.
"""
import csv
import gzip
import sys

import openpyxl

MACROS = [
    ('kcal', 'ENERCC'),
    ('kj', 'ENERCJ'),
    ('water', 'WATER'),
    ('protein', 'PROT625'),
    ('fat', 'FAT'),
    ('carbs', 'CHO'),
    ('fiber', 'FIBT'),
    ('sugar', 'SUGAR'),
    ('salt', 'NACL'),
    ('alcohol', 'ALC'),
    ('saturated_fat', 'FASAT'),
]

MICROS = [
    'VITA', 'VITD', 'VITE', 'VITK', 'THIA', 'RIBF', 'NIAEQ', 'VITB6', 'FOL', 'VITB12', 'VITC',
    'NA', 'K', 'CA', 'MG', 'P', 'FE', 'ZN', 'ID', 'CHORL', 'FAPUN3',
]

TRACE = {'<LOD', '<LOQ', '<LOD or <LOQ', 'TR'}


def value(raw):
    if raw is None or raw == '-' or raw == '':
        return ''
    if isinstance(raw, str):
        if raw.strip() in TRACE:
            return '0'
        raw = raw.replace(',', '.')
    number = float(raw)
    return ('%.4f' % number).rstrip('0').rstrip('.')


def main(source, target):
    workbook = openpyxl.load_workbook(source, read_only=True)
    rows = workbook.active.iter_rows(values_only=True)
    header = next(rows)
    index = {h.split(' ')[0]: i for i, h in enumerate(header) if h and '[' in h}
    columns = ['code', 'name_de', 'name_en'] + [m[0] for m in MACROS] + MICROS
    count = 0
    with gzip.open(target, 'wt', encoding='utf-8', newline='') as out:
        writer = csv.writer(out, delimiter=';', lineterminator='\n')
        writer.writerow(columns)
        for row in rows:
            if not row[0]:
                continue
            record = [row[0], row[1], row[2]]
            record += [value(row[index[code]]) for _, code in MACROS]
            record += [value(row[index[code]]) for code in MICROS]
            writer.writerow(record)
            count += 1
    print('%d Lebensmittel geschrieben nach %s' % (count, target))


if __name__ == '__main__':
    main(sys.argv[1], sys.argv[2])
