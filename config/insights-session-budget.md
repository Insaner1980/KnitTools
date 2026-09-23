# Insights session processing and backup budget

Backup export and restore accept at most **10,000 sessions**. The same overflow-safe
`BackupBudget.addRow` check applies to export, table reads, preflight, shadow import,
confirmation-time revalidation and live replacement. A custom budget can tighten this
limit but cannot relax it. Direct database verification checks the same ceiling.
No oversized preview is published and confirmation revalidates before live mutation.

This is a conservative mobile processing budget, not a measured device threshold.
Two explicit Start/Stop sessions per day fit about 13.7 years; ten per day fit about
2.7 years. Sessions are user work intervals, not counter taps or background ticks.
The former 100,000-row generic table budget admitted ten times that workload.
Zone-aware daily analysis may inspect up to 366 day segments per session, so the
new limit bounds one such pass over an imported table to 3,660,000 contributions.
Ordinary sessions cover one or two days. Archive byte/field/identity budgets remain
independent additional bounds. Numeric session-value hardening (triage-015) is not
part of this change.

Native history remains complete and is never truncated by this backup budget.
Repository session writers stop adding completed rows at 100,000 in one transaction,
including Start/Stop, recovery, replacement, project completion, and direct insertion.
Existing histories above that ceiling remain intact and readable. This ceiling allows
ten sessions each day for more than 27 years; the smaller backup ceiling remains
independent. Insights reads **256 rows per Room query**, folds and releases each batch,
and publishes only the completed transaction snapshot. Memory retains aggregate
date/project/bucket facts required by existing results, not session history or
lists of batches. Total work still grows with history; cancellation is checked per
batch and per session. All-time totals, streaks and first stored-zone date include
every session. Chart axes keep their existing 12-month / 26-week / daily behavior.

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
10,000 imported sessions need at most 20,000 blocks (about 10 MiB of bit storage,
plus collection overhead), even when their dates are far apart. Streak traversal
does not expand those bits into a full date list. Chart and fabric maps retain
only their visible windows, with at most two distinct future bucket keys to preserve
the existing meaningful-comparison flag. Future sessions still contribute to all
existing totals and activity/streak semantics; they are not discarded.
