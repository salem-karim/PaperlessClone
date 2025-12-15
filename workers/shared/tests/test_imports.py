"""Test that basic imports work."""

import paperless_shared


def test_package_imports():
    """Test that package can be imported."""
    assert paperless_shared is not None


def test_module_structure():
    """Check what's available in the package."""
    import paperless_shared.config
    import paperless_shared.models

    # Print what's available for debugging
    print("Config attributes:", dir(paperless_shared.config))
    print("Models attributes:", dir(paperless_shared.models))
