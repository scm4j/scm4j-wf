All statuses are calculated by `BranchesStatuses` class

# `develop` branch
- `IGNORED`
  - Last commit has `#scm-ignore` or no commits at all
- `MODIFIED`
  - Last commit has no `#scm-ver`
- `BRANCHED`
  - Last commit has `#scm-ver`
  
# `release` branch
- `MISSING`
  - Branch does not exist
- `BRANCHED`
  - Not all mdeps have fixed version or no mdeps at all
- `MDEPS_FROZEN`
  - All mdeps (if any) have fixed version 
- `MDEPS_ACTUAL`
   - For every mdeps entry tag which corresponds to mdeps.component.version exists and points to the component-repo.branches.version.commits.head-1. Or no mdeps at all.
- `ACTUAL`
  - `MDEPS_ACTUAL` (or no mdeps) and tag which corresponds to version.minor.patch-1 exists and points to head-1 (no commits after last patch but `#scm-ver`)
  
