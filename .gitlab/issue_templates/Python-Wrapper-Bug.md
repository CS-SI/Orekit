<!-- Thank you for reporting a bug in the Orekit Python wrapper!

Before submitting, please:
- If you can, confirm the issue is in the Python wrapper, not in Orekit core.
- Search existing issues in both projects to avoid duplicates:
    https://gitlab.orekit.org/orekit/orekit-python-wrapper/-/issues
    https://gitlab.orekit.org/orekit/orekit/-/issues
    If unsure, don't worry! We'll most probably know if this is a known issue.
- Consider discussing first on the forum if you are not sure: https://forum.orekit.org/
-->

## Summary

<!-- A clear and concise description of the bug. -->

## Steps to reproduce

<!--
Provide a minimal, self-contained Python script that reproduces the issue.
Include the JVM initialisation block so the issue is fully self-contained.
If the bug depends on input data, attach it or describe how to obtain it.
-->

```python
import orekit
vm = orekit.initVM()

from orekit.pyhelpers import setup_orekit_curdir
setup_orekit_curdir()

# Minimal reproducer here
```

## Expected behavior

<!-- What you expected to happen, ideally with a reference (paper, standard, analytic case). -->

## Actual behavior

<!-- What actually happens. Include the full traceback if an exception is raised. -->

```
<paste traceback or output here>
```

## Environment

- orekit Python package version (`pip show orekit-jpype`):
- Hipparchus version (bundled with the above):
- Java version (`java -version`):
- JPype version (`pip show jpype1`):
- Python version (`python --version`):
- Installation method (conda / pip / source build):
- Operating system:

## Additional context

<!-- Anything else: links to forum discussions, related issues, workarounds tried, etc. -->

/label ~bug ~"python wrapper"
