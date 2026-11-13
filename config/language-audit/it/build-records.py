"""Reproduce Italian audit inventory and mechanical checks; linguistic verdicts are in review.md."""
from pathlib import Path
import collections
import csv
import hashlib
import json
import re
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[3]
OUT = ROOT / 'config/language-audit/it'
BASE = 'app/src/main/java/com/finnvek/knittools/'

required_receipts = [
    ROOT / f'reports/language-audit/shared/{name}/final-input-hashes.tsv'
    for name in ('20261018', '20261018-followup', '20261019-b08')
] + [ROOT / 'reports/language-audit/shared/20261019-b05/final-input-hashes.json']
missing_receipts = [str(path.relative_to(ROOT)) for path in required_receipts if not path.is_file()]
if missing_receipts:
    raise SystemExit('Missing historical audit evidence; no outputs written:\n' + '\n'.join(missing_receipts))

def read(path):
    return (ROOT / path).read_text(encoding='utf-8-sig')

def digest(path):
    return hashlib.sha256((ROOT / path).read_bytes()).hexdigest()

def dump(name, value):
    (OUT / name).write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')

def tsv(name, rows, fields):
    with (OUT / name).open('w', encoding='utf-8', newline='') as f:
        w = csv.DictWriter(f, fieldnames=fields, delimiter='\t', lineterminator='\n')
        w.writeheader()
        for row in rows:
            w.writerow({k: str(row.get(k, '')).replace('\t', '\\t').replace('\r', '').replace('\n', '\\n') for k in fields})

def resources(directory):
    result = {}
    for p in sorted((ROOT / directory).glob('*.xml')):
        for e in ET.parse(p).getroot():
            if e.tag in ('string', 'plurals', 'string-array', 'array'):
                key = e.attrib['name']
                assert key not in result, key
                result[key] = (p.relative_to(ROOT).as_posix(), e)
    return result

target = resources('app/src/main/res/values-it')
default = resources('app/src/main/res/values')
expected = {k for k, (_, e) in default.items() if e.get('translatable') != 'false'}
assert set(target) == expected, (set(target) - expected, expected - set(target))
callers = json.loads((OUT / 'caller-inventory.json').read_text('utf-8'))
corrections = json.loads((OUT / 'corrections.json').read_text('utf-8'))
start = json.loads((OUT / 'starting-state.json').read_text('utf-8'))

# Only batch membership is reused as a discovery map. No foreign review verdict,
# rendering result, source text or language approval is copied.
membership = {}
with (ROOT / 'config/language-audit/en/coverage.tsv').open(encoding='utf-8-sig') as f:
    for x in csv.DictReader(f, delimiter='\t'):
        if x['kind'] in ('string', 'plural', 'array'):
            membership.setdefault(x['key_or_member'], x['batch'])
for x in corrections:
    membership[x['key']] = x['batch']
assert expected <= membership.keys()

notes = {
'B01': 'Shared actions read as Italian imperatives; neutral selection heading supports both number and gender. Units and navigation retain their meanings.',
'B02': 'Project/folder creation, completion, reopening, deletion, selection and virtual filters checked; folder deletion preserves projects; inserted names remain opaque.',
'B03': 'Selected ROWS/ROUNDS/REPEATS/CUSTOM, target positions, count deltas and step size traced through MainCounterDisplayText and CounterValueFormatter; generic UI supports crochet rows.',
'B04': 'Named, linked, shaping and repeating counters distinguished; repeat index is a noun, not a command. Reminder interval and occurrence arguments checked separately.',
'B05': 'Session Start/Stop, recovery, duration and completion-event semantics read; current shared date clipping and historical unit policy reconciled. Day-count grammar does not describe the user.',
'B06': 'Yarn metadata, package counts, thickness categories and care labels read; icon label doubles as contentDescription. No inferred strand/ply count from category.',
'B07': 'Planned/allocated/used independent; remaining=allocated-used; unknown differs from zero; negative remainder means overuse. Conversion and unchanged global stash described accurately.',
'B08': 'Saved metadata, webpage and local PDF distinguished; availability, browser/download actions, duplicates and unlink/delete consequences checked against repositories.',
'B09': 'Local PDF copy/import, primary documents, camera and ordered photo import read; 20 images, 25 MiB/image and 200 MiB/session match limits; no OCR/sync promise.',
'B10': 'Page/bookmark/guide/marker/calibration meanings checked; follow tracks main counter, manual movement pauses it; document-specific state and annotation exclusion respected.',
'B11': 'Tools, layers, selection, undo/redo and marked-PDF export read; library layer read-only in project, overlay editable, export preserves original.',
'B12': 'Notes/save state, local photos, sharing and destructive confirmations checked; user content/names are inserted without grammatical assumptions.',
'B13': 'Stitch/row axes, gauge basis, canonical conversion, rounding and copy compositions checked. Neutral righe covers crochet; no claim of automatic garment redesign.',
'B14': 'Increase/decrease notation interpreted as knitting, not crochet; M1 bars, edge reservation, consumed stitches and regex-only English example syntax checked.',
'B15': 'Desired width, total edges, repeat rounding, alternative counts and displayed resulting width checked with shared calculator evidence and arithmetic.',
'B16': 'Required length, exact package estimate and whole-package purchase mass distinguished; fractional/singular output and lowercase g preserved; no universal extra-package recommendation.',
'B17': 'All table cells read with category/header mapping; body circumferences not finished garments, cm/2.54 conversion, ranges, missing em dash and non-universal needle numbering checked.',
'B18': 'All 77 English notation/Italian meaning/description triples read; tool names, slip direction, decreases, yarn position, repeats and fabrics checked with Italian craft sources.',
'B19': 'All 24 displayed glyph/name/description triples read against unchanged shared geometry evidence; right-side convention made explicit; cable widths and direction checked.',
'B20': 'Informal tu maintained; language/system default, theme, analytics opt-in, app details and unit defaults checked against settings and configuration.',
'B21': 'Backup preview counts use invariant headings; local replacement, privacy, exclusions and failure language checked against backup repository/transaction boundaries. No destructive restore run.',
'B22': '14-day trial, single active free project, retained existing content and advanced-view gates checked; reminder language is unit-neutral; debug unlock not treated as purchase.',
'B23': 'Residual resources individually read; aliases, app-owned literals and generated numeric/date/file-name output reconciled. No unassigned key or unexplained fallback remains.',
}

fields = 'item_id locale batch kind key_or_member variant resource_path context_id caller_refs text_or_case input_hash T F L C R V evidence_refs finding_ids result_note'.split()
rows = []
finding_ids = {}
seq = collections.Counter()
for x in corrections:
    if x['key'] not in finding_ids:
        seq[x['batch']] += 1
        finding_ids[x['key']] = f"LA-it-{x['batch']}-{seq[x['batch']]:02}"
    x['finding_id'] = finding_ids[x['key']]

def add(batch, kind, key, variant, path, text, note='', context='primary', refs='', findings='', resource=True):
    rows.append(dict(item_id=f'it:{kind}:{key}:{variant}:{context}', locale='it', batch=batch,
        kind=kind, key_or_member=key, variant=variant, resource_path=path,
        context_id=context, caller_refs=refs, text_or_case=text, input_hash=digest(path),
        T='PASS', F='PASS', L='PASS', C='PASS', R='PASS' if resource else 'NOT_APPLICABLE', V='',
        evidence_refs=f'READ-{batch};CALLERS;STRUCTURE' + (';PLURALS' if kind=='plural' else ''),
        finding_ids=findings, result_note=note or notes[batch]))

def content(e):
    return ''.join(e.itertext())

def variants(e):
    if e.tag == 'string':
        return [('default', content(e))]
    return [(i.get('quantity', str(n)), content(i)) for n, i in enumerate(e)]

fmt = re.compile(r'%(?:(\d+)\$)?[-#+ 0,(]*\d*(?:\.\d+)?([a-zA-Z%])')
def signature(s):
    implicit = 0
    result = []
    for m in fmt.finditer(s):
        if m[2] in ('%', 'n'): continue
        if not m[1]: implicit += 1
        result.append((int(m[1]) if m[1] else implicit, m[2]))
    return sorted(result)

aliases = []
for key, (path, e) in target.items():
    d = default[key][1]
    assert e.tag == d.tag, key
    if e.tag == 'plurals':
        q = [x[0] for x in variants(e)]
        assert len(q) == len(set(q)) and 'other' in q
        assert set(q) == {'one', 'many', 'other'}, (key, q)
    for variant, value in variants(e):
        if value.startswith('@string/'):
            dest = value[8:]
            assert dest in target and target[dest][1].tag == 'string'
            assert not content(target[dest][1]).startswith('@')
            aliases.append([key, dest])
        else:
            defvalue = content(d) if d.tag == 'string' else content(next(i for i in d if i.get('quantity') == 'other'))
            assert signature(value) == signature(defvalue), (key, variant, value, defvalue)
        assert '\ufffd' not in value and "l\\'ferro" not in value, key
        refs = ';'.join(callers.get(key, []))
        note = notes[membership[key]]
        if not refs:
            note += ' No direct production caller in current scan; retained resource reviewed, no active-screen claim.'
        add(membership[key], 'plural' if e.tag=='plurals' else 'string', key, variant, path, value,
            note=note, refs=refs, findings=finding_ids.get(key, ''))

resource_rows = len(rows)
# Independent extraction of every app-owned reference cell/token/glyph pair.
p = BASE+'domain/calculator/NeedleSizeData.kt'
needles = re.findall(r'NeedleSize\(([\d.]+), "([^"]+)", "([^"]+)", "([^"]+)"\)', read(p))
for mm, *regional in needles:
    for system, value in zip(('mm','US','UK/Canada','JP'), (mm,*regional)):
        shown = value.replace('.', ',') if system=='mm' else value
        add('B17','reference_data','NeedleSizeData.sizes',mm+':'+system,p,shown,
            note=notes['B17']+' Numeric cell; R not applicable (Kotlin-owned value).',resource=False)

p = BASE+'domain/calculator/SizeChartData.kt'
size_count = 0
for category, block in re.findall(r'private val (\w+Entries)\s*=\s*listOf\((.*?)(?=\n    private val|\Z)',read(p),re.S):
    for n, item in enumerate(re.split(r'SizeChartEntry\(', block)[1:]):
        keys = re.findall(r'R\.string\.(\w+)', item)
        measures = re.findall(r'\bm\(([\d.]+)(?:, ([\d.]+))?\)',item)
        if not measures: continue
        size_count += 1
        heading = ' / '.join(content(target[k][1]) for k in keys)
        for column, (low, high) in enumerate(measures):
            value = low if not high else low+'–'+high
            add('B17','reference_data','SizeChartData.'+category,f'{n+1}:{column+1}',p,
                heading+': '+value.replace('.',',')+' cm',
                note=notes['B17']+' Column order follows headers(category); R not applicable to numeric data.',resource=False)

p = BASE+'domain/calculator/AbbreviationData.kt'
abbr = re.findall(r'KnittingAbbreviation\(\s*"([^"]+)",\s*R.string.(\w+),\s*R.string.(\w+)',read(p))
for token, meaning, desc in abbr:
    add('B18','code_text','AbbreviationData.abbreviations',token,p,
        f'{token}: {content(target[meaning][1])} — {content(target[desc][1])}',
        note=notes['B18']+' English notation token retained, Italian explanation reviewed. R not applicable to literal token; paired resources checked separately.',
        findings=';'.join(finding_ids[k] for k in (meaning,desc) if k in finding_ids),resource=False)
p = BASE+'domain/model/StandardYarnWeight.kt'
weights = re.findall(r'^    ([A-Z_]+)\("([^"]+)"\)',read(p),re.M)
for ident, label in weights:
    add('B06','code_text','StandardYarnWeight',ident,p,label,
        note=notes['B06']+' Established category identifier retained; R not applicable to persisted Kotlin category label.',resource=False)
p = BASE+'domain/calculator/ChartSymbolData.kt'
symbols = re.findall(r'ChartSymbol\(\s*"([^"]+)",\s*R.string.(\w+),\s*R.string.(\w+)',read(p))
for ident, name, desc in symbols:
    add('B19','symbol','ChartSymbolData.symbols',ident,p,
        content(target[name][1])+' — '+content(target[desc][1]),
        note=notes['B19']+' SHARED: B19-01/B19-02; R not applicable to glyph definition; paired resource checks separate.',
        findings=';'.join(finding_ids[k] for k in (name,desc) if k in finding_ids),resource=False)
p = BASE+'ui/components/care/CareSymbol.kt'
care = re.findall(r'([A-Z_0-9]+)\(R.string.(\w+), (\d+)\)',read(p))
for ident, label, bit in care:
    add('B06','symbol','CareSymbol',ident,p,f'bit {bit}: {content(target[label][1])}',
        note=notes['B06']+' SHARED B06-04; R not applicable to enum mapping; text resource checked separately.',
        findings=finding_ids.get(label,''),resource=False)
for key in ('widget_preview_primary_count','counter_step_custom_placeholder'):
    p,e = default[key]
    add('B03','code_text',key,'nontranslatable',p,content(e),
        note='Numeric sample, intentionally nontranslatable; actual 42/10 display meaning checked. No prose or unit to translate.')
add('B23','code_text','WidgetOperators','plus-minus',BASE+'widget/CounterWidget.kt','+ / −',
    note='Arithmetic increase/decrease symbols, shared across locales and sizes; no English prose. R not applicable to literal glyphs. Animation labels resultNumber/counterImageButtonPress/digit_ are internal tooling labels, not visible text.',resource=False)

# Explicit Italian composition cases: source reasoning, not device executions.
cases = [
('B03','CounterValueFormatter','crochet-rows','domain/calculator/CounterValueFormatter.kt','CROCHET + ROWS, count 1/target 2: Riga 1 / 2; Rimanenti: 1 riga. Aggiungi riga / Togli riga.'),
('B03','CounterValueFormatter','rounds','domain/calculator/CounterValueFormatter.kt','ROUNDS 0/1/2/3, target 2: 2 giri / 1 giro remaining; Obiettivo raggiunto at 2; one giro over target at 3.'),
('B03','CounterValueFormatter','repeat-custom','domain/calculator/CounterValueFormatter.kt','REPEATS: 1 ripetizione / 2 ripetizioni; CUSTOM label Motivi preserved verbatim, no inflection guessed. Negative counts/deltas remain numeric.'),
('B03','MainCounterDisplayText','step','ui/components/MainCounterDisplayText.kt','Step 1 and 5 insert selected localized unit; 3+5=8; target 2 leaves 6 units over target. Widget uses same count semantics, generic Contatore label.'),
('B04','ReminderRepeatDescription','two-arguments','ui/components/MainCounterDisplayText.kt','Occurrence 1, interval 2 => Ripetizione #1, intervallo 2; occurrence 2, interval 1 => Ripetizione #2, intervallo 1. Quantity is interval, not occurrence.'),
('B05','ActiveSessionDescription','duration','ui/screens/counter/CounterWorkspaceSections.kt','Sessione di lavoro per Scialle, 0:01:00 trascorsi. Caller always supplies an h:mm:ss clock, not singular/mixed unit phrases. Original clock shorthand retained; a proposed agreement fix was withdrawn after tracing formatWorkSessionDuration.'),
('B05','InsightsDays','counts','ui/screens/insights/InsightsScreen.kt','0/1 => Giorni di attività: 0 su 1; 1/1 => Giorni di attività: 1 su 1; 1/2 => Giorni di attività: 1 su 2; 2/2 => Giorni di attività: 2 su 2. Quantity is daysInRange.'),
('B05','InsightsDates','zone-bounds','ui/screens/insights/InsightsSessionAccumulator.kt','Stored-zone dates clipped to current bounded calendar range, including prior B05-01 counterexamples; active days cannot exceed range denominator. All Time starts no later than today.'),
('B05','InsightsHistoricalUnit','label-change','ui/screens/insights/InsightsViewModel.kt','Session 10→20, 61 seconds: 2 display minutes and neutral Avanzamento del contatore preserved after current label edits. No historical row/pace inference from current unit.'),
('B05','InsightsCompletion','events','ui/screens/insights/InsightsScreen.kt','1 completamento / 2 completamenti count completion events, including re-completion of one project; this differs from the number of projects.'),
('B07','YarnUsagePresentation','fractional','ui/screens/counter/YarnUsagePresentation.kt','0 matasse; 0,5 matasse; 1 matassa; 1,5 matasse; 2 matasse. Rounded display is parsed with Italian locale before ICU quantity selection; 0,999999 may display 1 and select one.'),
('B07','YarnUsagePresentation','remainder','ui/screens/counter/YarnUsagePresentation.kt','Allocated 100 m, used 120 m => Utilizzato in eccesso: 20 m. Allocated/used unknown => Quantità rimanente sconosciuta; both known equal => Rimanente: 0 m.'),
('B08','SavedPatternTitle','names','ui/screens/library/SavedPatternDetailScreen.kt','User title Scialle d’inverno / 100% lino inserted as a format argument, never parsed as a new format. Unlink preserves documents; delete library master annotations differs from project overlay.'),
('B11','AnnotatedPdf','filename','data/storage/PatternPdfExporter.kt','Export suffix: <name>-annotato.pdf; source PDF preserved and project/library annotation scopes retained. Filename base modello is localized fallback, not user content translation.'),
('B13','GaugePresentation','conversion','ui/screens/gauge/GaugePresentation.kt','2,54 cm = 1 in; 1 m = 100 cm; independent unit controls preserve canonical values, output uses Italian comma and short unit symbols.'),
('B13','GaugePresentation','density','ui/screens/gauge/GaugePresentation.kt','22 stitches / 10 cm × 25 cm = 55 maglie; 30 rows / 10 cm × 20 cm = 60 righe; 22/4 in is a distinct density from 22/10 cm.'),
('B13','GaugePresentation','adjust-copy','ui/screens/gauge/GaugePresentation.kt','Original 100 stitches at 20/10 cm = 50 cm; actual 22/10 cm requires 110 stitches, while unchanged 100 stitches yield about 45,45 cm. Label: value copy composition retains colon and units.'),
('B14','IncreaseDecreaseCalculator','flat-increase','domain/calculator/IncreaseDecreaseCalculator.kt','96+12=108. Easy K5,(K7,M1)×12,K6,K1 consumes 5+84+6+1=96 stitches and 12 distinct bars; final stitch remains.'),
('B14','IncreaseDecreaseCalculator','circular-increase','domain/calculator/IncreaseDecreaseCalculator.kt','96+12=108. (K8,M1)×12 consumes96; last M1 uses the boundary between rounds.'),
('B14','IncreaseDecreaseCalculator','decrease','domain/calculator/IncreaseDecreaseCalculator.kt','96−12=84. (K6,K2tog)×12 consumes72+24=96, outputs72+12=84.'),
('B14','IncreaseDecreaseCalculator','invalid','domain/calculator/IncreaseDecreaseCalculator.kt','Flat1+3 invalid: no distinct M1 bars. Italian error asks for at least2 initial stitches and fewer increases if needed, not an impossible knitting sequence.'),
('B14','InstructionParser','english-examples','domain/calculator/InstructionParser.kt','Pasted English examples remain supported regex syntax; Italian explanatory text does not promise parsing arbitrary Italian prose. K, M1, K2tog are explained in the glossary.'),
('B15','CastOnCalculator','metric-imperial','domain/calculator/CastOnCalculator.kt','25 cm at22/10 cm =>55 maglie,25 cm. 8 in at22/4 in =>44 maglie,8 in.'),
('B15','CastOnCalculator','repeat-edges','domain/calculator/CastOnCalculator.kt','20 cm at20/10 cm, repeat6 and2 total edges =>44 maglie,22 cm; alternatives38/44. Exact12,4 at repeat8 rounds to16 rather than8; tie12 rounds to8.'),
('B16','YarnEstimator','fractional-purchase','domain/calculator/YarnEstimator.kt','450 m,200 m/50 g => Gomitoli stimati: circa 2,25; buy3; Peso dei gomitoli interi: 150 g. Exact1 gives circa 1 without a plural-noun agreement error. Zero input gives no result.'),
('B17','SizeChartData','hand-category','domain/calculator/SizeChartData.kt','Women S/M/L hand rows resolve to Donna: S / Donna: M / Donna: L; dimensions are circumference and length. Italian labels do not include the US clothing sizes from the key identifiers.'),
('B17','SizeChartData','display-rounding','domain/calculator/SizeChartData.kt','40,5 cm displays15,9 in; 71–76 cm displays28–29,9 in. Range endpoints derive from exact cm/2,54 and do not claim independent source-inch values.'),
('B23','DurationDisplayFormatter','boundaries','domain/calculator/DurationDisplayFormatter.kt','0/1/59/60/61/120 minutes =>0 min /1 min /59 min /1 h /1 h 1 min /2 h. Invariant h/min symbols need no grammatical inflection.'),
('B23','LocaleNumberFormatter','grouping','domain/calculator/LocaleNumberFormatter.kt','Italian decimal comma; grouped numeric labels may use 1.234,5 while standard US needle code1.5 stays a code. Canonical calculations never consume display rounding.'),
('B23','PatternDisplayNames','filename-cleaning','domain/model/PatternDisplayNames.kt','File-like PDF suffix/separator/all-caps cleanup only; mixed-case user titles stay untouched. No translation of a user or designer title.'),
('B23','ElapsedClock','clock','ui/screens/counter/CounterWorkspaceSections.kt','0:00:20 and signed numeric deltas are notation, not English prose; elapsed duration clock uses fixed numeric fields.'),
('B23','HistoryDateTime','date','ui/screens/counterhistory/CounterHistoryViewModel.kt','Date/time uses current locale formatter; dates remain actual session/history timestamps. 42→43 is numeric history notation, not a sentence requiring plural inflection.'),
('B21','BackupDate','preview','ui/screens/backup/BackupScreen.kt','Data del backup: <Italian localized date/time>; Progetti:1 / Progetti:2 are valid count headings, not malformed singular sentences.'),
('B21','BackupFileName','filename','ui/screens/backup/BackupScreen.kt','KnitTools-backup-YYYY-MM-DD.knittools-backup intentionally stable interchange filename; it is not user-facing English explanatory prose.'),
]
for b,k,v,p,text in cases:
    add(b,'generated_output',k,v,BASE+p,text,note=notes[b]+' Source-derived case, not executed device output. R applies to referenced composition.',context='composition')

# Reconcile every declared category against the actual AOSP quantity selector.
for key,(path,e) in target.items():
    if e.tag!='plurals': continue
    note='AOSP getQuantityText selects ICU category from the integer quantity and falls back to other. Italian: 1=one; 0,2,21=other; modern ICU exact nonzero millions=many; older ICU other gives the same plural wording. No rendered/spoken execution claimed.'
    if key=='yarn_usage_skeins_format': note+=' Fractional display uses skeinPluralQuantity; 0,5/1,5=other,1=one.'
    if key=='insights_days_meta_format': note+=' Caller quantity=daysInRange; args=activeDays,daysInRange; both counts independently reviewed.'
    if key=='insights_project_fabric_a11y_summary': note+=' Caller quantity=activeDays; args=rangeLabel,activeDays.'
    if key=='reminder_repeat_occurrence_format': note+=' Caller quantity=intervalRows; args=occurrence,interval; all text forms deliberately invariant.'
    add(membership[key],'generated_output',key,'quantity-selection',path,'; '.join(q+': '+s for q,s in variants(e)),
        note=note,context='plural-matrix',refs=';'.join(callers.get(key,[])),findings=finding_ids.get(key,''))

assert len({x['item_id'] for x in rows}) == len(rows)
tsv('coverage.tsv',rows,fields)

# Every resource diff must be individually justified by the correction register.
actual_changes=[]
for path, original in start['italian_before'].items():
    before={e.attrib['name']:e for e in ET.fromstring(original)}
    after={e.attrib['name']:e for e in ET.parse(ROOT/path).getroot()}
    assert before.keys()==after.keys()
    for key,e in before.items():
        for (v,old),(v2,new) in zip(variants(e),variants(after[key]), strict=True):
            assert v==v2
            if old!=new: actual_changes.append((key,v,old,new))
recorded={(x['key'],x['variant'],x['original'],x['final']) for x in corrections}
assert set(actual_changes)==recorded, (set(actual_changes)-recorded,recorded-set(actual_changes))
serious = {'abbr_ssk_desc','abbr_s2kp_meaning','abbr_wyif_desc','abbr_wyib_desc'}
tsv('corrections.tsv', [{**x,'classification':'CONFIRMED_DEFECT','severity':'S1' if x['key'] in serious else ('S2' if x['category'] in ('FACT','CONTEXT','TERM') else 'S3'),'state':'VERIFIED','why_minimal':'Only the named semantic/grammar problem is changed; valid alternatives elsewhere retained.'} for x in corrections],
    ['finding_id','batch','key','variant','file','classification','category','severity','state','original','problem','final','why_minimal'])

# Latest applicable raw shared receipts, not other locales' approval decisions.
receipts={}
for name in ('20261018','20261018-followup','20261019-b08'):
    p=f'reports/language-audit/shared/{name}/final-input-hashes.tsv'
    with (ROOT/p).open(encoding='utf-8-sig') as f:
        for x in csv.DictReader(f,delimiter='\t'):
            receipts[x['path']]=(x['sha256'],p)
p='reports/language-audit/shared/20261019-b05/final-input-hashes.json'
for k,v in json.loads(read(p)).items(): receipts[k]=(v,p)
shared=[]
for p,(prior,receipt) in receipts.items():
    if not p.startswith('app/src/main/java/'): continue
    current=digest(p)
    shared.append(dict(path=p,prior_sha256=prior,current_sha256=current,result='MATCH' if prior==current else 'CHANGED',receipt=receipt))
tsv('shared-evidence-reconciliation.tsv',shared,['path','prior_sha256','current_sha256','result','receipt'])

inputs={x['resource_path'] for x in rows}
for refs in callers.values():
    inputs.update(x.rsplit(':',1)[0] for x in refs)
inputs.update(['AGENTS.md','CODEX.md','CLAUDE.md','LANGUAGE_AUDIT.md','config/language-audit/shared-fixes.md','app/build.gradle.kts','gradle/libs.versions.toml','gradle/gradle-daemon-jvm.properties','config/posthog.md'])
inputs.update(p.relative_to(ROOT).as_posix() for p in (ROOT/'app/src/main/res/values').glob('*.xml'))
inputs.update(x['path'] for x in shared)
inputs.update([BASE+'repository/CounterRepository.kt',BASE+'repository/ProjectFolderRepository.kt',BASE+'repository/BackupRepository.kt',BASE+'repository/SavedPatternRepository.kt',BASE+'repository/ProjectDocumentRepository.kt',BASE+'pro/ProState.kt',BASE+'pro/TrialManager.kt',BASE+'ui/screens/counter/YarnUsagePresentation.kt'])
inputs.update(p.relative_to(ROOT).as_posix() for p in (ROOT/'app/src/test').rglob('*SourceTest.kt') if p.name in ('CountPluralResourcesSourceTest.kt',))
inputs.add('app/src/test/java/com/finnvek/knittools/ReferenceResourceLocalizationTest.kt')
dump('input-hashes.json',{p:digest(p) for p in sorted(inputs)})
changed_unrelated=[p for p,h in start['files'].items() if not p.startswith(('app/src/main/res/values-it/','config/language-audit/it/')) and (not (ROOT/p).is_file() or digest(p)!=h)]
summary=dict(keys=len(target),strings=sum(e.tag=='string' for _,e in target.values()),plurals=sum(e.tag=='plurals' for _,e in target.values()),resource_variants=resource_rows,
    rows=len(rows),by_kind=dict(collections.Counter(x['kind'] for x in rows)),by_batch=dict(sorted(collections.Counter(x['batch'] for x in rows).items())),
    changed_variants=len(actual_changes),changed_keys=len({x[0] for x in actual_changes}),changed_files=sorted({x['file'] for x in corrections}),
    aliases=aliases,needle_rows=len(needles),size_rows=size_count,abbreviation_tokens=len(abbr),yarn_categories=len(weights),chart_symbols=len(symbols),care_symbols=len(care),
    shared_inputs=len(shared),shared_changes=[x['path'] for x in shared if x['result']!='MATCH'],unrelated_changes_since_start=changed_unrelated,input_files=len(inputs),structural_result='PASS')
dump('inventory-summary.json',summary)
print(json.dumps(summary,ensure_ascii=False,indent=2))
