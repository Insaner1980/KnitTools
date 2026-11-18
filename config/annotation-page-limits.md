# Annotation page limits

triage-015: one page in one master or project layer accepts at most 256 annotations,
16,384 geometry work units and 2 MiB of encoded payloads. Freehand/highlighter work
is the point count (at most 2,048 per stroke); an empty legacy stroke uses its path
character count, at least one. Simple annotations cost eight units, covering their
constant hit-test work. The existing chart-cell and PDF-export budgets still apply.

These are conservative interaction ceilings, not measured frame-time guarantees.
256 supports hundreds of short notes, shapes or highlights on a pattern page;
16,384 supports eight maximum-length strokes or more shorter strokes. Selection
checks at most two layers, hence 512 annotations and 32,768 units. Erasing checks
only the editable layer. This is deliberately below the PDF exporter’s batch
limits (2,000 annotations, 250,000 render units, 16 MiB). The 2 MiB page payload
ceiling also bounds decoding before interaction. No claim of device latency is
made by the deterministic tests.

DAO insert, update and REPLACE batches validate their final page states in the
same Room transaction as the write. Raw DAO mutation methods are protected.
REPLACE keeps the last occurrence of an ID and retains auto-generated ID behavior.
Backup export, preflight, preview, confirmation revalidation, shadow import,
replacement export and live import use the same budget, including passes that do
not track embedded identities. Live import and its final database validation run
inside the existing restore transaction.

Page reads check SQL count/payload statistics over at most 257 indexed rows before
materializing entities, then validate geometry. Over-limit legacy data is retained
unchanged and produces an explicit page error with no rendered prefix. Switching
to a valid page remains possible. Migrations preserve existing rows; no schema or
ownership changes are needed. Existing master/project ownership, document keys,
detach/reattach, zIndex/ID ordering and export rendering semantics are unchanged
for pages within the limits. An unsafe write or export fails as a whole.

Hit testing accepts any input order and retains the greatest (zIndex, ID) hit in
one traversal. Equal keys preserve the first input hit. Segment testing uses an
indexed loop, with no sort or input-sized pair/list allocation. The UI computes
chart highlight maps only when annotations or counters change, not on selection.
