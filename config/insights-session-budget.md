# Insights session processing and backup budget

Backup export and restore accept at most **100,000 sessions**, using the native
completed-session writer's shared limit. The same overflow-safe
`BackupBudget.addRow` check applies to export, table reads, preflight, shadow import,
confirmation-time revalidation and live replacement. A custom budget can tighten this
limit but cannot relax it. Direct database verification checks the same ceiling.
No oversized preview is published and confirmation revalidates before live mutation.

The former 10,000-session backup ceiling rejected valid native history. Matching
the native ceiling removes that mismatch without truncating history. This is not
a measured device performance threshold: archive byte/field/identity budgets
remain independent additional bounds. In particular, 150,000 identities must cover
sessions and all other identity-bearing tables together. Sessions are user work
intervals, not counter taps or background ticks. Zone-aware daily analysis may
inspect up to 366 day segments per session, or 36,600,000 contributions for a full
history; ordinary sessions cover one or two days.

The archive remains format/data version 1, schema 25. Existing accepted archives
remain readable, but older app versions with the 10,000-session ceiling reject
larger archives. The full 100,000-session restore still needs on-device resource
verification; JVM boundary checks do not establish device memory or latency.

Native history remains complete and is never truncated by this backup budget.
Repository session writers stop adding completed rows at 100,000 in one transaction,
including Start/Stop, recovery, replacement, project completion, and direct insertion.
Existing histories above that ceiling remain intact and readable. This ceiling allows
ten sessions each day for more than 27 years. Insights reads **256 rows per Room query**,
folds and releases each batch, and publishes the completed session snapshot only
after its Room transaction ends. That single transaction covers existence and
project-activity facts, the first local date, and every history batch. Memory retains aggregate
date/project/bucket facts required by existing results, not session history or
lists of batches. Total work still grows with history; cancellation is checked per
batch and per session. All-time totals, streaks and first stored-zone date include
every session. Chart axes keep their existing 12-month / 26-week / daily behavior.

Room invalidation starts a replacement calculation; cancellation discards an
unfinished fold. It is the transaction, not invalidation delivery timing, that
prevents mixing history before and after a write. The transaction remains open
during accumulation and can delay writers at large history sizes; device latency
has not been measured. Cancellation releases it at the existing checkpoints.
Projects, completion events, and feature gates are still observed independently,
so the combined Insights UI state does not represent one database-wide snapshot.

Week/month queries include only rows overlapping the preceding comparison period
or later, with the selected project applied in SQL. Their lower bound uses midnight
at UTC+18 to retain every accepted stored-zone local date. SQL includes the existing
duration-derived end fallback; Kotlin retains exact zone-aware splitting and rounding.
Global existence and project last-session timestamps use SQL facts rather than full
history objects. Existing primary-key/project/time indexes suffice; no schema changes.

The first All-time local date is resolved before the fold so the existing chart's
leading-edge clipping is unchanged. Only starts within 36 hours of the earliest
UTC timestamp can win across accepted -18..+18 offsets; these candidate projections
are also read in 256-row batches. The minimum timestamp is read once per snapshot
using existing indexes. Preexisting native history is read in full, even when it
predates the writer ceiling.

Active days use sparse 4,096-day bit blocks instead of one date object per active
day. A session's existing 366-day analysis window touches at most two blocks, so
100,000 imported sessions need at most 200,000 blocks (about 100 MiB of bit storage,
plus collection overhead), even when their dates are far apart. Streak traversal
does not expand those bits into a full date list. Chart and fabric maps retain
only their visible windows, with at most two distinct future bucket keys to preserve
the existing meaningful-comparison flag. Future sessions still contribute to all
existing totals and activity/streak semantics; they are not discarded.
